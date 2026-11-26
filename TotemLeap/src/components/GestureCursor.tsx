import { useLeap } from "../gestures/hooks";

/**
 * Cursor virtual flotante controlado por la mano en Leap Motion (o por el simulador).
 * Se mueve directamente mediante `transform` en el DOM para mantener 60+ FPS sin re-renders.
 */
export function GestureCursor() {
  const {
    cursorRef,
    handActive,
    fistProgress,
    hoveredTargetId,
    lastGestureFeedback,
  } = useLeap();

  const ringScale = 1 + fistProgress * 0.25;
  const ringOpacity = hoveredTargetId ? 0.95 : 0.45;

  return (
    <div
      ref={cursorRef}
      className={`leap-cursor ${handActive ? "visible" : ""} ${
        hoveredTargetId ? "hovering" : ""
      } ${lastGestureFeedback === "fist" ? "fist-flash" : ""}`}
      aria-hidden="true"
    >
      <div
        className="leap-cursor-ring"
        style={{
          transform: `scale(${ringScale})`,
          opacity: ringOpacity,
        }}
      />
      <div className="leap-cursor-core" />
    </div>
  );
}
