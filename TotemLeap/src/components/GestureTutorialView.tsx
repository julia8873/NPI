import { useEffect, useState } from "react";
import { useAccessibility } from "../context/AccessibilityContext";
import {
  useFistListener,
  useGestureTarget,
  useLeap,
  usePinch,
  useTwoHandSwipe,
  useWaveGoodbye,
} from "../gestures/hooks";
import { FistGlyph, HandGlyph, WaveGlyph } from "./SectionIcons";

interface GestureTutorialViewProps {
  onBack: () => void;
}

type GestureKey = "hover" | "fist" | "pinch" | "wave" | "swipe";

/**
 * Zona de tutorial y laboratorio interactivo para explicar y probar
 * en tiempo real todos los gestos implementados en Leap Motion.
 */
export function GestureTutorialView({ onBack }: GestureTutorialViewProps) {
  const { t } = useAccessibility();
  const copy = t.tutorial;

  const {
    connected,
    handsCount,
    simulatorEnabled,
    setSimulatorEnabled,
    fistProgress,
    pinchProgress,
    waveProgress,
    telemetry,
    lastGestureFeedback,
  } = useLeap();

  const [completed, setCompleted] = useState<Record<GestureKey, boolean>>({
    hover: false,
    fist: false,
    pinch: false,
    wave: false,
    swipe: false,
  });

  const [pinchCount, setPinchCount] = useState(0);
  const [waveCount, setWaveCount] = useState(0);
  const [fistPulse, setFistPulse] = useState(false);
  const [swipeIndex, setSwipeIndex] = useState(0);
  const [lastSwipeDir, setLastSwipeDir] = useState<"left" | "right" | null>(
    null,
  );

  const markDone = (key: GestureKey) => {
    setCompleted((prev) => (prev[key] ? prev : { ...prev, [key]: true }));
  };

  // Botón de regreso al menú principal
  const { ref: backRef, isHovered: backHovered } =
    useGestureTarget<HTMLButtonElement>({
      id: "tutorial-back-btn",
      onSelect: onBack,
      priority: 2,
    });

  // 1. Prueba de Hover con mano
  const { ref: hoverCardRef, isHovered: hoverTestHovered } =
    useGestureTarget<HTMLDivElement>({
      id: "tutorial-hover-zone",
      onSelect: () => markDone("hover"),
    });

  useEffect(() => {
    if (hoverTestHovered) markDone("hover");
  }, [hoverTestHovered]);

  // 2. Prueba de Cerrar Puño sobre botón diana
  const triggerFistTest = () => {
    markDone("fist");
    setFistPulse(true);
    window.setTimeout(() => setFistPulse(false), 900);
  };

  const {
    ref: fistBtnRef,
    isHovered: fistBtnHovered,
    fistProgress: localFistProgress,
  } = useGestureTarget<HTMLButtonElement>({
    id: "tutorial-fist-target",
    onSelect: triggerFistTest,
    priority: 2,
  });

  useFistListener(() => {
    markDone("fist");
  });

  // 3. Prueba de Pellizco (índice + pulgar)
  usePinch(() => {
    markDone("pinch");
    setPinchCount((c) => c + 1);
  });

  // 4. Prueba de Adiós ×3 (interceptado aquí para practicar sin salir del tutorial)
  useWaveGoodbye(() => {
    markDone("wave");
    setWaveCount((c) => c + 1);
  });

  // 5. Prueba de Barrido con dos manos
  useTwoHandSwipe((direction) => {
    markDone("swipe");
    setLastSwipeDir(direction);
    setSwipeIndex((prev) => {
      const total = copy.swipeCards.length;
      return direction === "right"
        ? (prev + 1) % total
        : (prev - 1 + total) % total;
    });
  });

  const resetAll = () => {
    setCompleted({
      hover: false,
      fist: false,
      pinch: false,
      wave: false,
      swipe: false,
    });
    setPinchCount(0);
    setWaveCount(0);
    setLastSwipeDir(null);
  };

  const completedCount = Object.values(completed).filter(Boolean).length;
  const allCompleted = completedCount === 5;

  return (
    <main className="section-view tutorial-view">
      {/* Barra superior */}
      <div className="section-view-topbar">
        <button
          ref={backRef}
          type="button"
          className={`back-pill-btn ${backHovered ? "is-hovered" : ""}`}
          onClick={onBack}
        >
          <span aria-hidden="true">←</span>
          <span>{t.gestures.backToMenu}</span>
        </button>

        <div className="tutorial-topbar-actions">
          <span className="tutorial-score-pill">
            {completedCount} / 5 {copy.completedBadge}
          </span>
          <button
            type="button"
            className="tutorial-reset-btn"
            onClick={resetAll}
          >
            {copy.resetProgress}
          </button>
        </div>
      </div>

      {/* Cabecera descriptiva y activación rápida del simulador si no hay Leap físico */}
      <div className="tutorial-hero">
        <div className="tutorial-hero-text">
          <h1 className="tutorial-title">{copy.title}</h1>
          <p className="tutorial-subtitle">{copy.subtitle}</p>
        </div>

        {!connected && (
          <label className="tutorial-sim-chip">
            <input
              type="checkbox"
              checked={simulatorEnabled}
              onChange={(e) => setSimulatorEnabled(e.target.checked)}
            />
            <span>{t.simulator.toggleLabel}</span>
          </label>
        )}
      </div>

      {allCompleted && (
        <div className="tutorial-congrats-banner" role="status">
          {copy.completedBanner}
        </div>
      )}

      {/* Cuadrícula interactiva de los 5 gestos */}
      <div className="tutorial-grid">
        {/* 1. HOVER / APUNTAR */}
        <div
          ref={hoverCardRef}
          className={`tutorial-card ${hoverTestHovered ? "is-hovered" : ""} ${
            completed.hover ? "is-done" : ""
          }`}
        >
          <div className="tutorial-card-header">
            <span className="tutorial-card-icon">
              <HandGlyph className="gesture-svg" />
            </span>
            <h2 className="tutorial-card-title">{copy.gestures.hover.title}</h2>
            {completed.hover && (
              <span className="tutorial-check">✓ {copy.completedBadge}</span>
            )}
          </div>
          <p className="tutorial-card-desc">{copy.gestures.hover.howTo}</p>
          <div className="tutorial-live-box">
            <span>{copy.gestures.hover.actionHint}</span>
            <small className="tutorial-shortcut">
              {copy.gestures.hover.shortcut} · X:{telemetry.x} Y:{telemetry.y}
            </small>
          </div>
        </div>

        {/* 2. CERRAR PUÑO */}
        <div
          className={`tutorial-card ${
            lastGestureFeedback === "fist" ? "is-active-flash" : ""
          } ${completed.fist ? "is-done" : ""}`}
        >
          <div className="tutorial-card-header">
            <span className="tutorial-card-icon">
              <FistGlyph className="gesture-svg" />
            </span>
            <h2 className="tutorial-card-title">{copy.gestures.fist.title}</h2>
            {completed.fist && (
              <span className="tutorial-check">✓ {copy.completedBadge}</span>
            )}
          </div>
          <p className="tutorial-card-desc">{copy.gestures.fist.howTo}</p>

          <div className="tutorial-meter">
            <div
              className="tutorial-meter-fill fist"
              style={{ width: `${Math.round(fistProgress * 100)}%` }}
            />
          </div>

          <button
            ref={fistBtnRef}
            type="button"
            onClick={triggerFistTest}
            className={`tutorial-target-btn ${fistBtnHovered ? "is-hovered" : ""} ${
              fistPulse ? "triggered" : ""
            }`}
          >
            {fistPulse
              ? copy.targetActivated
              : `${copy.targetButton} (${Math.round(
                  (fistBtnHovered ? localFistProgress : fistProgress) * 100,
                )}%)`}
          </button>
          <small className="tutorial-shortcut">
            {copy.gestures.fist.shortcut}
          </small>
        </div>

        {/* 3. PELLIZCO */}
        <div
          className={`tutorial-card ${
            lastGestureFeedback === "pinch" ? "is-active-flash" : ""
          } ${completed.pinch ? "is-done" : ""}`}
        >
          <div className="tutorial-card-header">
            <span className="tutorial-card-icon">🤏</span>
            <h2 className="tutorial-card-title">{copy.gestures.pinch.title}</h2>
            {completed.pinch && (
              <span className="tutorial-check">✓ {copy.completedBadge}</span>
            )}
          </div>
          <p className="tutorial-card-desc">{copy.gestures.pinch.howTo}</p>

          <div className="tutorial-meter">
            <div
              className="tutorial-meter-fill pinch"
              style={{ width: `${Math.round(pinchProgress * 100)}%` }}
            />
          </div>

          <div className="tutorial-live-box row">
            <span>
              {copy.pinchCounter}: <strong>{pinchCount}</strong>
            </span>
            <small className="tutorial-shortcut">
              {copy.gestures.pinch.shortcut} · {Math.round(pinchProgress * 100)}
              %
            </small>
          </div>
        </div>

        {/* 4. ADIÓS ×3 */}
        <div
          className={`tutorial-card ${
            waveProgress > 0 || lastGestureFeedback === "wave"
              ? "is-active-flash"
              : ""
          } ${completed.wave ? "is-done" : ""}`}
        >
          <div className="tutorial-card-header">
            <span className="tutorial-card-icon">
              <WaveGlyph className="gesture-svg" />
            </span>
            <h2 className="tutorial-card-title">{copy.gestures.wave.title}</h2>
            {completed.wave && (
              <span className="tutorial-check">✓ {copy.completedBadge}</span>
            )}
          </div>
          <p className="tutorial-card-desc">{copy.gestures.wave.howTo}</p>

          <div className="tutorial-wave-steps" aria-label="Progreso de saludo">
            {[1, 2, 3].map((step) => (
              <span
                key={step}
                className={`wave-step-dot ${
                  waveProgress >= step || waveCount > 0 ? "filled" : ""
                }`}
              >
                {step}/3
              </span>
            ))}
          </div>
          <small className="tutorial-shortcut">
            {copy.gestures.wave.shortcut} · {copy.waveInterceptNote}
          </small>
        </div>

        {/* 5. BARRIDO CON DOS MANOS */}
        <div
          className={`tutorial-card full-span ${
            lastGestureFeedback === "swipe" ? "is-active-flash" : ""
          } ${completed.swipe ? "is-done" : ""}`}
        >
          <div className="tutorial-card-header">
            <span className="tutorial-card-icon">🙌</span>
            <h2 className="tutorial-card-title">{copy.gestures.swipe.title}</h2>
            {completed.swipe && (
              <span className="tutorial-check">✓ {copy.completedBadge}</span>
            )}
          </div>
          <p className="tutorial-card-desc">{copy.gestures.swipe.howTo}</p>

          <div className="tutorial-swipe-carousel">
            <span className="swipe-arrow">
              {lastSwipeDir === "left" ? "◀" : "◁"}
            </span>
            <div className="swipe-card-display">
              <span className="swipe-page-tag">
                {copy.swipeCardLabel} {swipeIndex + 1}/{copy.swipeCards.length}
              </span>
              <strong>{copy.swipeCards[swipeIndex]}</strong>
            </div>
            <span className="swipe-arrow">
              {lastSwipeDir === "right" ? "▶" : "▷"}
            </span>
          </div>
          <small className="tutorial-shortcut">
            {copy.gestures.swipe.shortcut} · {t.simulator.handsLabel}:{" "}
            {handsCount}/2
          </small>
        </div>
      </div>
    </main>
  );
}
