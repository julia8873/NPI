import { useAccessibility } from "../context/AccessibilityContext";
import { useLeap } from "../gestures/hooks";
import { FistGlyph, HandGlyph, WaveGlyph } from "./SectionIcons";

interface GestureGuideBarProps {
  /** Indica si el usuario está dentro de una sección para destacar el gesto de regreso. */
  inSection?: boolean;
}

/**
 * Barra inferior reutilizable que muestra los gestos disponibles y la
 * retroalimentación en tiempo real (por ejemplo, progreso del saludo Adiós 1/3 -> 3/3).
 */
export function GestureGuideBar({ inSection = false }: GestureGuideBarProps) {
  const { t } = useAccessibility();
  const { waveProgress, lastGestureFeedback } = useLeap();

  return (
    <footer className="gesture-guide-bar" aria-live="polite">
      <div className="gesture-hint">
        <span className="gesture-icon-wrap">
          <HandGlyph className="gesture-svg" />
        </span>
        <span>{t.gestures.moveHand}</span>
      </div>

      <div
        className={`gesture-hint ${
          lastGestureFeedback === "fist" ? "hint-active" : ""
        }`}
      >
        <span className="gesture-icon-wrap">
          <FistGlyph className="gesture-svg" />
        </span>
        <span>{t.gestures.closeFist}</span>
      </div>

      <div
        className={`gesture-hint ${inSection ? "hint-emphasized" : ""} ${
          waveProgress > 0 || lastGestureFeedback === "wave"
            ? "hint-wave-active"
            : ""
        }`}
      >
        <span className="gesture-icon-wrap">
          <WaveGlyph className="gesture-svg" />
        </span>
        <span>
          {lastGestureFeedback === "wave"
            ? t.gestures.waveDetected
            : waveProgress > 0
              ? `${t.gestures.waveProgress} ${waveProgress}/3`
              : t.gestures.waveBack}
        </span>
      </div>
    </footer>
  );
}
