import type { LeapHand, LeapPointable, SwipeDirection } from "./types";

/** Coordenada 2D en píxeles de pantalla. */
export interface ScreenPoint {
  x: number;
  y: number;
}

function distance3D(
  a: [number, number, number],
  b: [number, number, number],
): number {
  const dx = a[0] - b[0];
  const dy = a[1] - b[1];
  const dz = a[2] - b[2];
  return Math.sqrt(dx * dx + dy * dy + dz * dz);
}

/**
 * Convierte las coordenadas 3D en milímetros de Leap Motion a píxeles 2D de pantalla
 * y aplica un filtro paso bajo exponencial para eliminar el micro-temblor de la mano.
 */
export function mapLeapToScreen(
  palmPosition: [number, number, number],
  previous: ScreenPoint,
  viewportWidth: number,
  viewportHeight: number,
  alpha = 0.32,
): ScreenPoint {
  const [rawX, rawY] = palmPosition;
  const normX = Math.min(1, Math.max(0, (rawX + 180) / 360));
  const normY = Math.min(1, Math.max(0, (rawY - 100) / 290));

  const targetX = normX * viewportWidth;
  const targetY = (1 - normY) * viewportHeight;

  return {
    x: alpha * targetX + (1 - alpha) * previous.x,
    y: alpha * targetY + (1 - alpha) * previous.y,
  };
}

// ─────────────────────────────────────────────────────────────────────────────
// DETECTOR DE PUÑO COMPLETO  ("Cerrar puño por completo" = seleccionar)
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Exige cierre TOTAL de la mano:
 *  1. grabStrength >= 0.99
 *  2. Ningún dedo extendido (extended === false en los 5 dedos)
 *  3. Distancia media de las puntas de los dedos (índice a meñique) al centro
 *     de la palma <= maxTipToPalmMm (52 mm), evitando falsos disparos con la mano
 *     en garra o medio cerrada.
 */
export class FistDetector {
  private armed = true;
  private cooldownUntil = 0;
  private closedSince: number | null = null;

  private readonly triggerThreshold = 0.99;
  private readonly releaseThreshold = 0.65;
  private readonly maxTipToPalmMm = 52;
  private readonly holdMs = 150;
  private readonly cooldownMs = 750;

  update(
    hand: LeapHand,
    fingers: LeapPointable[],
    now: number,
  ): { triggered: boolean; progress: number; isClosed: boolean } {
    const grabStrength = hand.grabStrength;

    // Comprobación geométrica con los dedos de la mano
    let fingersFullyCurled = true;
    let tipDistanceFactor = 1;

    if (fingers.length >= 4) {
      const anyExtended = fingers.some((f) => f.extended);
      const nonThumbFingers = fingers.filter((f) => f.type !== 0);
      const avgTipDist =
        nonThumbFingers.reduce(
          (acc, f) => acc + distance3D(f.tipPosition, hand.palmPosition),
          0,
        ) / Math.max(1, nonThumbFingers.length);

      fingersFullyCurled = !anyExtended && avgTipDist <= this.maxTipToPalmMm;
      // Factor 0..1 según cuánto se acercan las puntas a la palma (95 mm abierta -> 52 mm cerrada)
      tipDistanceFactor = Math.min(
        1,
        Math.max(0, (95 - avgTipDist) / (95 - this.maxTipToPalmMm)),
      );
    }

    const grabFactor = Math.min(
      1,
      Math.max(
        0,
        (grabStrength - this.releaseThreshold) /
          (this.triggerThreshold - this.releaseThreshold),
      ),
    );
    const progress = Math.min(grabFactor, tipDistanceFactor);

    if (grabStrength < this.releaseThreshold) {
      this.armed = true;
      this.closedSince = null;
    }

    const isClosed =
      grabStrength >= this.triggerThreshold && fingersFullyCurled;
    if (!isClosed) {
      this.closedSince = null;
      return { triggered: false, progress, isClosed: false };
    }

    if (this.closedSince === null) {
      this.closedSince = now;
    }

    let triggered = false;
    if (
      this.armed &&
      now - this.closedSince >= this.holdMs &&
      now >= this.cooldownUntil
    ) {
      this.armed = false;
      this.cooldownUntil = now + this.cooldownMs;
      triggered = true;
    }

    return { triggered, progress: 1, isClosed: true };
  }

  reset(): void {
    this.armed = true;
    this.closedSince = null;
  }
}

// ─────────────────────────────────────────────────────────────────────────────
// DETECTOR DE PELLIZCO  ("Pellizcar" = solo índice + pulgar juntos)
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Detecta el pellizco real (índice + pulgar juntos con el resto de la mano abierta):
 *  - Si hay datos de dedos (`pointables`):
 *      * Al menos 2 de los otros 3 dedos (corazón, anular, meñique) deben estar
 *        extendidos (`extended === true`), lo que distingue claramente el pellizco
 *        de un puño cerrado incluso si un dedo se dobla por simpatía.
 *      * Distancia entre punta del pulgar (type 0) y punta del índice (type 1)
 *        <= 32 mm (o `pinchStrength >= 0.85`).
 *  - Si no hay `pointables`, usa respaldo `pinchStrength >= 0.85` y `grabStrength <= 0.72`.
 */
export class PinchDetector {
  private armed = true;
  private cooldownUntil = 0;
  private pinchSince: number | null = null;

  private readonly triggerThreshold = 0.85;
  private readonly releaseThreshold = 0.45;
  private readonly maxThumbIndexMm = 32;
  private readonly holdMs = 80;
  private readonly cooldownMs = 550;

  update(
    hand: LeapHand,
    fingers: LeapPointable[],
    now: number,
  ): { triggered: boolean; progress: number; isPinching: boolean } {
    const thumb = fingers.find((f) => f.type === 0);
    const index = fingers.find((f) => f.type === 1);
    const otherFingers = fingers.filter((f) => f.type >= 2);

    let otherFingersOpen = true;
    let pinchMetric = hand.pinchStrength;

    if (thumb && index && otherFingers.length >= 2) {
      const extendedOthers = otherFingers.filter((f) => f.extended).length;
      // Al menos 2 de los 3 dedos restantes (corazón, anular, meñique) deben estar abiertos
      otherFingersOpen = extendedOthers >= 2;

      const tipDist = distance3D(thumb.tipPosition, index.tipPosition);
      // Convertimos distancia (70 mm abierta -> 26 mm tocándose) a escala 0..1
      const distStrength = Math.min(1, Math.max(0, (70 - tipDist) / (70 - 26)));
      pinchMetric = Math.max(hand.pinchStrength, distStrength);

      if (!otherFingersOpen) {
        this.pinchSince = null;
        return { triggered: false, progress: 0, isPinching: false };
      }

      if (tipDist > 52 && hand.pinchStrength < this.releaseThreshold) {
        this.armed = true;
        this.pinchSince = null;
      }

      const isPinching =
        tipDist <= this.maxThumbIndexMm || pinchMetric >= this.triggerThreshold;

      const progress = Math.min(
        1,
        Math.max(
          0,
          (pinchMetric - this.releaseThreshold) /
            (this.triggerThreshold - this.releaseThreshold),
        ),
      );

      if (!isPinching) {
        this.pinchSince = null;
        return { triggered: false, progress, isPinching: false };
      }

      if (this.pinchSince === null) {
        this.pinchSince = now;
      }

      let triggered = false;
      if (
        this.armed &&
        now - this.pinchSince >= this.holdMs &&
        now >= this.cooldownUntil
      ) {
        this.armed = false;
        this.cooldownUntil = now + this.cooldownMs;
        triggered = true;
      }

      return { triggered, progress: 1, isPinching: true };
    }

    // Respaldo si el frame no incluye pointables
    if (hand.grabStrength > 0.72) {
      this.pinchSince = null;
      return { triggered: false, progress: 0, isPinching: false };
    }

    if (hand.pinchStrength < this.releaseThreshold) {
      this.armed = true;
      this.pinchSince = null;
    }

    const progress = Math.min(
      1,
      Math.max(
        0,
        (hand.pinchStrength - this.releaseThreshold) /
          (this.triggerThreshold - this.releaseThreshold),
      ),
    );

    const isPinching = hand.pinchStrength >= this.triggerThreshold;
    if (!isPinching) {
      this.pinchSince = null;
      return { triggered: false, progress, isPinching: false };
    }

    if (this.pinchSince === null) {
      this.pinchSince = now;
    }

    let triggered = false;
    if (
      this.armed &&
      now - this.pinchSince >= this.holdMs &&
      now >= this.cooldownUntil
    ) {
      this.armed = false;
      this.cooldownUntil = now + this.cooldownMs;
      triggered = true;
    }

    return { triggered, progress: 1, isPinching: true };
  }

  reset(): void {
    this.armed = true;
    this.pinchSince = null;
  }
}

// ─────────────────────────────────────────────────────────────────────────────
// DETECTOR DE BARRIDO CON DOS MANOS
// ─────────────────────────────────────────────────────────────────────────────

export class TwoHandSwipeDetector {
  private cooldownUntil = 0;
  private gestureStartTime: number | null = null;
  private gestureDirection: SwipeDirection | null = null;

  private readonly minVelocityX = 220;
  private readonly maxGrabStrength = 0.5;
  private readonly minDurationMs = 60;
  private readonly maxDurationMs = 600;
  private readonly cooldownMs = 1200;

  update(
    hand1: LeapHand,
    hand2: LeapHand,
    now: number,
  ): { triggered: boolean; direction: SwipeDirection | null } {
    if (now < this.cooldownUntil) {
      return { triggered: false, direction: null };
    }

    const vx1 = hand1.palmVelocity[0];
    const vx2 = hand2.palmVelocity[0];
    const grab1 = hand1.grabStrength;
    const grab2 = hand2.grabStrength;

    const hand1Valid =
      Math.abs(vx1) >= this.minVelocityX && grab1 <= this.maxGrabStrength;
    const hand2Valid =
      Math.abs(vx2) >= this.minVelocityX && grab2 <= this.maxGrabStrength;

    if (!hand1Valid || !hand2Valid) {
      this.gestureStartTime = null;
      this.gestureDirection = null;
      return { triggered: false, direction: null };
    }

    const dir1: SwipeDirection = vx1 > 0 ? "right" : "left";
    const dir2: SwipeDirection = vx2 > 0 ? "right" : "left";

    if (dir1 !== dir2) {
      this.gestureStartTime = null;
      this.gestureDirection = null;
      return { triggered: false, direction: null };
    }

    const currentDir = dir1;

    if (
      this.gestureStartTime === null ||
      this.gestureDirection !== currentDir
    ) {
      this.gestureStartTime = now;
      this.gestureDirection = currentDir;
      return { triggered: false, direction: currentDir };
    }

    const elapsed = now - this.gestureStartTime;

    if (elapsed > this.maxDurationMs) {
      this.gestureStartTime = null;
      this.gestureDirection = null;
      return { triggered: false, direction: null };
    }

    if (elapsed >= this.minDurationMs) {
      this.gestureStartTime = null;
      this.gestureDirection = null;
      this.cooldownUntil = now + this.cooldownMs;
      return { triggered: true, direction: currentDir };
    }

    return { triggered: false, direction: currentDir };
  }

  reset(): void {
    this.gestureStartTime = null;
    this.gestureDirection = null;
  }
}

// ─────────────────────────────────────────────────────────────────────────────
// DETECTOR DE ADIÓS ×3
// ─────────────────────────────────────────────────────────────────────────────

export class WaveGoodbyeDetector {
  private lastDirection: -1 | 0 | 1 = 0;
  private anchorX: number | null = null;
  private lastSwingTime = 0;
  private reversals = 0;
  private cooldownUntil = 0;

  private readonly requiredWaves = 3;
  private readonly minVelocityX = 230;
  private readonly minTravelMm = 36;
  private readonly maxPauseMs = 800;
  private readonly maxGrabStrength = 0.55;
  private readonly cooldownMs = 1500;

  update(
    palmPosition: [number, number, number],
    palmVelocity: [number, number, number],
    grabStrength: number,
    now: number,
  ): { triggered: boolean; progress: number } {
    if (now < this.cooldownUntil) {
      return { triggered: false, progress: 0 };
    }

    if (this.lastSwingTime > 0 && now - this.lastSwingTime > this.maxPauseMs) {
      this.resetSequence();
    }

    if (grabStrength > this.maxGrabStrength) {
      return { triggered: false, progress: this.reversals };
    }

    const [x] = palmPosition;
    const [vx, vy] = palmVelocity;

    if (
      Math.abs(vx) < this.minVelocityX ||
      Math.abs(vx) <= Math.abs(vy) * 1.05
    ) {
      return { triggered: false, progress: this.reversals };
    }

    const dir: -1 | 1 = vx > 0 ? 1 : -1;

    if (this.lastDirection === 0) {
      this.lastDirection = dir;
      this.anchorX = x;
      this.lastSwingTime = now;
      return { triggered: false, progress: 0 };
    }

    if (dir === this.lastDirection) {
      if (
        this.anchorX === null ||
        (dir === 1 && x > this.anchorX) ||
        (dir === -1 && x < this.anchorX)
      ) {
        this.anchorX = x;
      }
      return { triggered: false, progress: this.reversals };
    }

    const travel = this.anchorX !== null ? Math.abs(x - this.anchorX) : 0;
    if (travel >= this.minTravelMm) {
      this.reversals += 1;
      this.lastDirection = dir;
      this.anchorX = x;
      this.lastSwingTime = now;

      if (this.reversals >= this.requiredWaves) {
        this.resetSequence();
        this.cooldownUntil = now + this.cooldownMs;
        return { triggered: true, progress: this.requiredWaves };
      }
    }

    return { triggered: false, progress: this.reversals };
  }

  resetSequence(): void {
    this.lastDirection = 0;
    this.anchorX = null;
    this.lastSwingTime = 0;
    this.reversals = 0;
  }
}
