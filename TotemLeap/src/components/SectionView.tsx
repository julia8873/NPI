import type { CSSProperties } from "react";
import { useAccessibility } from "../context/AccessibilityContext";
import { SECTION_THEME_MAP } from "../data/sections";
import { useGestureTarget, useWaveGoodbye } from "../gestures/hooks";
import type { SectionId } from "../i18n/translations";
import { SectionIcon, WaveGlyph } from "./SectionIcons";

interface SectionViewProps {
  sectionId: SectionId;
  onBack: () => void;
}

/**
 * Vista de placeholder para cada sección.
 * Los compañeros de equipo añadirán aquí el contenido real.
 * Ya incluye el gesto "Adiós ×3" para volver al menú y el botón de regreso.
 */
export function SectionView({ sectionId, onBack }: SectionViewProps) {
  const { t } = useAccessibility();
  const theme = SECTION_THEME_MAP[sectionId];
  const copy = t.sections[sectionId];

  // Gesto reutilizable "Adiós ×3" para volver al menú principal
  const { waveProgress } = useWaveGoodbye(onBack);

  const { ref: backRef, isHovered: backHovered } =
    useGestureTarget<HTMLButtonElement>({
      id: "section-back-btn",
      onSelect: onBack,
      priority: 2,
    });

  const style = {
    "--card-bg": theme.cardBg,
    "--icon-bg": theme.iconBg,
    "--accent-color": theme.accentColor,
    "--border-color": theme.borderColor,
    "--glow-color": theme.glowColor,
  } as CSSProperties;

  return (
    <main className="section-view" style={style}>
      {/* Barra superior con botón de regreso e indicador del gesto Adiós ×3 */}
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

        <div
          className={`wave-return-chip ${waveProgress > 0 ? "wave-active" : ""}`}
        >
          <WaveGlyph className="wave-chip-svg" />
          <span>
            {waveProgress > 0
              ? `${t.gestures.waveProgress} ${waveProgress}/3`
              : t.gestures.waveBack}
          </span>
        </div>
      </div>

      {/* Cabecera de la sección con icono y título */}
      <div className="section-hero-card">
        <div className="section-icon-badge large">
          <SectionIcon id={sectionId} />
        </div>
        <div>
          <h1 className="section-hero-title">{copy.title}</h1>
          <p className="section-hero-subtitle">{copy.subtitle}</p>
        </div>
      </div>

      {/* Área vacía reservada para el contenido que añadirá el equipo */}
      <div className="section-placeholder">
        <div className="placeholder-icon" aria-hidden="true">🚧</div>
        <p className="placeholder-text">{t.sections[sectionId].placeholderText}</p>
      </div>
    </main>
  );
}
