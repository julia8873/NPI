import { useEffect, useState } from "react";
import { useAccessibility } from "../context/AccessibilityContext";
import { useGestureTarget, useLeap } from "../gestures/hooks";
import { GlobeGlyph } from "./SectionIcons";

/**
 * Cabecera del tótem con marca "ETSIIT Go", reloj/fecha en directo,
 * botones de accesibilidad (idioma y tamaño de letra) e indicador de sensor Leap Motion.
 */
export function TotemHeader() {
  const { language, toggleLanguage, fontScale, cycleFontScale, t } =
    useAccessibility();
  const {
    connected,
    handsCount,
    simulatorEnabled,
    setSimulatorEnabled,
    triggerSimulatedFist,
    triggerSimulatedWave,
    triggerSimulatedPinch,
    triggerSimulatedSwipe,
    telemetry,
  } = useLeap();

  const [now, setNow] = useState(() => new Date());
  const [popoverOpen, setPopoverOpen] = useState(false);

  // Actualiza el reloj cada 15 segundos
  useEffect(() => {
    const id = window.setInterval(() => setNow(new Date()), 15_000);
    return () => window.clearInterval(id);
  }, []);

  // Botón de idioma controlable también mediante gesto de mano + puño
  const { ref: langRef, isHovered: langHovered } =
    useGestureTarget<HTMLButtonElement>({
      id: "header-lang-toggle",
      onSelect: toggleLanguage,
      priority: 2,
    });

  // Botón de tamaño de letra controlable también mediante gesto de mano + puño
  const { ref: fontRef, isHovered: fontHovered } =
    useGestureTarget<HTMLButtonElement>({
      id: "header-font-toggle",
      onSelect: cycleFontScale,
      priority: 2,
    });

  const locale = language === "es" ? "es-ES" : "en-GB";
  const timeText = new Intl.DateTimeFormat(locale, {
    hour: "2-digit",
    minute: "2-digit",
    hour12: false,
  }).format(now);

  const dateText = new Intl.DateTimeFormat(locale, {
    weekday: "long",
    day: "numeric",
    month: "long",
  }).format(now);

  const fontBadgeLabel =
    fontScale === "normal" ? "Aa" : fontScale === "large" ? "Aa+" : "Aa++";

  const sensorStatusTitle = connected
    ? t.accessibility.sensorConnected
    : simulatorEnabled
      ? t.accessibility.sensorSimulated
      : t.accessibility.sensorWaiting;

  return (
    <header className="totem-header">
      <div className="brand-block">
        <div className="brand-title">
          <span className="brand-etsiit">ETSIIT</span>{" "}
          <span className="brand-go">Go</span>
        </div>
        <div className="brand-subtitle">{t.brandSubtitle}</div>
      </div>

      <div className="clock-block" aria-live="off">
        <div className="clock-time">{timeText}</div>
        <div className="clock-date">{dateText}</div>
      </div>

      <div className="accessibility-controls">
        <button
          ref={langRef}
          type="button"
          className={`pill-btn lang-btn ${langHovered ? "gesture-hovered" : ""}`}
          onClick={toggleLanguage}
          aria-label={t.accessibility.languageLabel}
          title={t.accessibility.languageLabel}
        >
          <GlobeGlyph className="pill-globe-svg" />
          <span className="pill-label">{language.toUpperCase()}</span>
        </button>

        <button
          ref={fontRef}
          type="button"
          className={`pill-btn font-btn ${fontScale !== "normal" ? "active-scale" : ""} ${fontHovered ? "gesture-hovered" : ""}`}
          onClick={cycleFontScale}
          aria-label={`${t.accessibility.fontScaleLabel}: ${t.accessibility.fontScaleNames[fontScale]}`}
          title={`${t.accessibility.fontScaleLabel}: ${t.accessibility.fontScaleNames[fontScale]}`}
        >
          <span className="pill-label">{fontBadgeLabel}</span>
        </button>

        <div className="sensor-popover-anchor">
          <button
            type="button"
            className={`pill-btn sensor-btn ${connected ? "online" : simulatorEnabled ? "sim" : "waiting"}`}
            onClick={() => setPopoverOpen((v) => !v)}
            aria-label={sensorStatusTitle}
            title={sensorStatusTitle}
            aria-expanded={popoverOpen}
          >
            <span className="sensor-dot" />
          </button>

          {popoverOpen && (
            <div className="sensor-popover" role="dialog" aria-label={t.simulator.title}>
              <div className="sensor-popover-header">
                <strong>{t.simulator.title}</strong>
                <button
                  type="button"
                  className="popover-close"
                  onClick={() => setPopoverOpen(false)}
                  aria-label="Cerrar"
                >
                  ×
                </button>
              </div>
              <p className="sensor-status-line">
                {connected
                  ? `● ${t.simulator.statusConnected}`
                  : `○ ${t.simulator.statusDisconnected}`}
              </p>
              <p className="sensor-status-line">
                {t.simulator.handsLabel}: <strong>{handsCount}</strong>
                {connected && (
                  <>
                    {" "}
                    · Puño: <strong>{telemetry.grab}</strong>
                  </>
                )}
              </p>

              <label className="sim-toggle-row">
                <input
                  type="checkbox"
                  checked={simulatorEnabled}
                  onChange={(e) => setSimulatorEnabled(e.target.checked)}
                />
                <span>{t.simulator.toggleLabel}</span>
              </label>

              <p className="sim-hint">{t.simulator.hint}</p>

              <div className="sim-actions">
                <button
                  type="button"
                  className="sim-action-btn"
                  onClick={triggerSimulatedFist}
                >
                  {t.simulator.simulateFist}
                </button>
                <button
                  type="button"
                  className="sim-action-btn"
                  onClick={triggerSimulatedWave}
                >
                  {t.simulator.simulateWave}
                </button>
                <button
                  type="button"
                  className="sim-action-btn"
                  onClick={triggerSimulatedPinch}
                >
                  {t.simulator.simulatePinch}
                </button>
                <button
                  type="button"
                  className="sim-action-btn"
                  onClick={() => triggerSimulatedSwipe("left")}
                >
                  {t.simulator.simulateSwipeLeft}
                </button>
                <button
                  type="button"
                  className="sim-action-btn"
                  onClick={() => triggerSimulatedSwipe("right")}
                >
                  {t.simulator.simulateSwipeRight}
                </button>
              </div>
            </div>
          )}
        </div>
      </div>
    </header>
  );
}
