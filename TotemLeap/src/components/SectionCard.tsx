import type { CSSProperties } from "react";
import { useAccessibility } from "../context/AccessibilityContext";
import type { SectionTheme } from "../data/sections";
import { useGestureTarget } from "../gestures/hooks";
import type { SectionId } from "../i18n/translations";
import { HandGlyph, SectionIcon } from "./SectionIcons";

interface SectionCardProps {
  theme: SectionTheme;
  onEnter: (id: SectionId) => void;
}

/**
 * Tarjeta individual de una sección del menú principal.
 * Se registra como objetivo gestual para ampliarse al pasar la mano encima
 * y abrir la sección al realizar el gesto "Cerrar puño".
 */
export function SectionCard({ theme, onEnter }: SectionCardProps) {
  const { t } = useAccessibility();
  const copy = t.sections[theme.id];

  const { ref, isHovered, fistProgress } = useGestureTarget<HTMLButtonElement>({
    id: `section-${theme.id}`,
    onSelect: () => onEnter(theme.id),
  });

  const style = {
    "--card-bg": theme.cardBg,
    "--card-hover-bg": theme.cardHoverBg,
    "--icon-bg": theme.iconBg,
    "--accent-color": theme.accentColor,
    "--border-color": theme.borderColor,
    "--glow-color": theme.glowColor,
  } as CSSProperties;

  return (
    <button
      ref={ref}
      type="button"
      style={style}
      className={`section-card ${isHovered ? "is-hovered" : ""}`}
      onClick={() => onEnter(theme.id)}
    >
      <div className="section-card-top">
        <div className="section-icon-badge">
          <SectionIcon id={theme.id} />
        </div>

        {isHovered && (
          <div className="section-hover-indicators" aria-hidden="true">
            <div
              className="hand-ring-indicator"
              style={{
                transform: `scale(${1 + fistProgress * 0.14})`,
              }}
            >
              <HandGlyph className="hand-ring-svg" />
            </div>
            <span className="fist-prompt-pill">{t.gestures.fistBadge}</span>
          </div>
        )}
      </div>

      <div className="section-card-body">
        <h3 className="section-card-title">{copy.title}</h3>
        <p className="section-card-subtitle">{copy.subtitle}</p>
      </div>
    </button>
  );
}
