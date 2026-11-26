import { useState, type CSSProperties } from "react";
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
 * Elemento de ejemplo interactivo dentro de una sección.
 * Demuestra cómo reutilizar `useGestureTarget` en vistas internas.
 */
function SectionSampleCard({
  id,
  label,
  detail,
  selected,
  onSelect,
  fistBadge,
}: {
  id: string;
  label: string;
  detail: string;
  selected: boolean;
  onSelect: () => void;
  fistBadge: string;
}) {
  const { ref, isHovered } = useGestureTarget<HTMLButtonElement>({
    id,
    onSelect,
  });

  return (
    <button
      ref={ref}
      type="button"
      className={`section-sample-item ${isHovered ? "is-hovered" : ""} ${
        selected ? "is-selected" : ""
      }`}
      onClick={onSelect}
    >
      <div className="sample-item-text">
        <strong>{label}</strong>
        <span>{detail}</span>
      </div>
      {isHovered && <span className="fist-prompt-pill">{fistBadge}</span>}
    </button>
  );
}

/**
 * Contenedor reutilizable para las vistas internas de cada sección.
 * Suscribe el gesto "Adiós (3 veces)" mediante `useWaveGoodbye` para permitir
 * volver al menú principal desde cualquier sección.
 */
export function SectionView({ sectionId, onBack }: SectionViewProps) {
  const { t } = useAccessibility();
  const theme = SECTION_THEME_MAP[sectionId];
  const copy = t.sections[sectionId];
  const [selectedIdx, setSelectedIdx] = useState<number | null>(null);

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

      <div className="section-hero-card">
        <div className="section-icon-badge large">
          <SectionIcon id={sectionId} />
        </div>
        <div>
          <h1 className="section-hero-title">{copy.title}</h1>
          <p className="section-hero-subtitle">{copy.subtitle}</p>
        </div>
      </div>

      <div className="section-content-panel">
        <h2 className="section-content-heading">{copy.previewHeadline}</h2>
        <p className="section-content-note">{copy.previewNote}</p>

        <div className="section-sample-list">
          {copy.sampleItems.map((item, idx) => (
            <SectionSampleCard
              key={item.label}
              id={`sample-${sectionId}-${idx}`}
              label={item.label}
              detail={item.detail}
              selected={selectedIdx === idx}
              onSelect={() => setSelectedIdx(idx)}
              fistBadge={t.gestures.fistBadge}
            />
          ))}
        </div>
      </div>
    </main>
  );
}
