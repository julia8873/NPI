/** Coordenada 2D en píxeles de pantalla. */
export interface ScreenPoint {
  x: number;
  y: number;
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
  // Caja de interacción útil de Leap Motion: X en [-180, 180] mm, Y en [100, 390] mm
  const normX = Math.min(1, Math.max(0, (rawX + 180) / 360));
  const normY = Math.min(1, Math.max(0, (rawY - 100) / 290));

  const targetX = normX * viewportWidth;
  // En Leap, el eje Y crece hacia arriba; en pantalla crece hacia abajo (se invierte)
  const targetY = (1 - normY) * viewportHeight;

  return {
    x: alpha * targetX + (1 - alpha) * previous.x,
    y: alpha * targetY + (1 - alpha) * previous.y,
  };
}

// ─────────────────────────────────────────────────────────────────────────────
// DETECTOR DE PUÑO  ("Cerrar puño" = seleccionar)
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Detector con estado para el gesto de selección "Cerrar puño".
 * Exige que la mano se abra por debajo de `releaseThreshold` antes de volver
 * a cerrarse por encima de `triggerThreshold`, evitando múltiples activaciones
 * mientras se mantiene el puño cerrado.
 */
export class FistDetector {
  private armed = true;
  private cooldownUntil = 0;
  private readonly triggerThreshold: number;
  private readonly releaseThreshold: number;
  private readonly cooldownMs: number;

  constructor(
    triggerThreshold = 0.78,
    releaseThreshold = 0.45,
    cooldownMs = 700,
  ) {
    this.triggerThreshold = triggerThreshold;
    this.releaseThreshold = releaseThreshold;
    this.cooldownMs = cooldownMs;
  }

  /**
   * Evalúa un nuevo valor de `grabStrength` en el instante `now` (ms).
   * Devuelve si se acaba de disparar el gesto, el progreso normalizado (0..1)
   * y si el puño está cerrado actualmente.
   */
  update(
    grabStrength: number,
    now: number,
  ): { triggered: boolean; progress: number; isClosed: boolean } {
    const progress = Math.min(
      1,
      Math.max(
        0,
        (grabStrength - this.releaseThreshold) /
          (this.triggerThreshold - this.releaseThreshold),
      ),
    );

    if (grabStrength < this.releaseThreshold) {
      this.armed = true;
    }

    let triggered = false;
    if (
      this.armed &&
      grabStrength >= this.triggerThreshold &&
      now >= this.cooldownUntil
    ) {
      this.armed = false;
      this.cooldownUntil = now + this.cooldownMs;
      triggered = true;
    }

    return { triggered, progress, isClosed: grabStrength >= this.triggerThreshold };
  }

  /** Rearma el detector (por ejemplo, cuando deja de detectarse la mano). */
  reset(): void {
    this.armed = true;
  }
}

// ─────────────────────────────────────────────────────────────────────────────
// DETECTOR DE PELLIZCO  ("Pellizcar" = índice + pulgar juntos)
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Detector con estado para el gesto "Pellizco" (pinchStrength).
 * Funciona igual que FistDetector pero sobre el valor `pinchStrength` del frame.
 *
 * Actualmente no está conectado a ninguna acción predeterminada.
 * Los compañeros de equipo pueden usarlo a través del hook `usePinch()`.
 */
export class PinchDetector {
  private armed = true;
  private cooldownUntil = 0;
  private readonly triggerThreshold: number;
  private readonly releaseThreshold: number;
  private readonly cooldownMs: number;

  constructor(
    triggerThreshold = 0.82,
    releaseThreshold = 0.50,
    cooldownMs = 600,
  ) {
    this.triggerThreshold = triggerThreshold;
    this.releaseThreshold = releaseThreshold;
    this.cooldownMs = cooldownMs;
  }

  /**
   * Evalúa un nuevo valor de `pinchStrength` en el instante `now` (ms).
   * Devuelve si se acaba de disparar el gesto, el progreso normalizado (0..1)
   * y si el pellizco está activo actualmente.
   */
  update(
    pinchStrength: number,
    now: number,
  ): { triggered: boolean; progress: number; isPinching: boolean } {
    const progress = Math.min(
      1,
      Math.max(
        0,
        (pinchStrength - this.releaseThreshold) /
          (this.triggerThreshold - this.releaseThreshold),
      ),
    );

    if (pinchStrength < this.releaseThreshold) {
      this.armed = true;
    }

    let triggered = false;
    if (
      this.armed &&
      pinchStrength >= this.triggerThreshold &&
      now >= this.cooldownUntil
    ) {
      this.armed = false;
      this.cooldownUntil = now + this.cooldownMs;
      triggered = true;
    }

    return { triggered, progress, isPinching: pinchStrength >= this.triggerThreshold };
  }

  /** Rearma el detector (por ejemplo, cuando deja de detectarse la mano). */
  reset(): void {
    this.armed = true;
  }
}

// ─────────────────────────────────────────────────────────────────────────────
// DETECTOR DE BARRIDO CON DOS MANOS  ("Deslizar" = retroceder / avanzar página)
// ─────────────────────────────────────────────────────────────────────────────

import type { LeapHand, SwipeDirection } from "./types";

/**
 * Detector con estado para el gesto de navegación "Barrido con dos manos".
 * Exige que ambas manos estén presentes y se desplacen en la misma dirección
 * horizontal con suficiente velocidad y con las manos abiertas.
 *
 * - Ambas manos hacia la izquierda  → `direction = "left"`  (retroceder página)
 * - Ambas manos hacia la derecha   → `direction = "right"` (avanzar página)
 */
export class TwoHandSwipeDetector {
  private cooldownUntil = 0;
  private gestureStartTime: number | null = null;
  private gestureDirection: SwipeDirection | null = null;

  private readonly minVelocityX = 220;    // Velocidad horizontal mínima de cada mano (mm/s)
  private readonly maxGrabStrength = 0.5; // Ambas manos deben estar abiertas
  private readonly minDurationMs = 60;    // Duración mínima del barrido (ms)
  private readonly maxDurationMs = 600;   // Duración máxima antes de cancelar
  private readonly cooldownMs = 1200;     // Tiempo de espera tras disparar el gesto

  /**
   * Evalúa las dos manos del frame actual.
   * Solo se llama cuando el frame contiene exactamente 2 manos.
   */
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

    // Ambas manos deben estar abiertas y moverse con velocidad suficiente
    const hand1Valid =
      Math.abs(vx1) >= this.minVelocityX && grab1 <= this.maxGrabStrength;
    const hand2Valid =
      Math.abs(vx2) >= this.minVelocityX && grab2 <= this.maxGrabStrength;

    if (!hand1Valid || !hand2Valid) {
      this.gestureStartTime = null;
      this.gestureDirection = null;
      return { triggered: false, direction: null };
    }

    // Ambas manos deben ir en el mismo sentido horizontal
    const dir1: SwipeDirection = vx1 > 0 ? "right" : "left";
    const dir2: SwipeDirection = vx2 > 0 ? "right" : "left";

    if (dir1 !== dir2) {
      this.gestureStartTime = null;
      this.gestureDirection = null;
      return { triggered: false, direction: null };
    }

    const currentDir = dir1;

    if (this.gestureStartTime === null || this.gestureDirection !== currentDir) {
      // Inicio de un nuevo barrido en esta dirección
      this.gestureStartTime = now;
      this.gestureDirection = currentDir;
      return { triggered: false, direction: currentDir };
    }

    const elapsed = now - this.gestureStartTime;

    if (elapsed > this.maxDurationMs) {
      // Barrido demasiado lento: cancelar y esperar el siguiente
      this.gestureStartTime = null;
      this.gestureDirection = null;
      return { triggered: false, direction: null };
    }

    if (elapsed >= this.minDurationMs) {
      // ¡Barrido válido detectado!
      this.gestureStartTime = null;
      this.gestureDirection = null;
      this.cooldownUntil = now + this.cooldownMs;
      return { triggered: true, direction: currentDir };
    }

    return { triggered: false, direction: currentDir };
  }

  /** Reinicia el estado del detector (cuando el número de manos cambia). */
  reset(): void {
    this.gestureStartTime = null;
    this.gestureDirection = null;
  }
}

// ─────────────────────────────────────────────────────────────────────────────
// DETECTOR DE ADIÓS ×3  ("Wave goodbye" = volver al menú principal)
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Detector con estado para el gesto "Adiós (3 veces)" de vuelta al menú principal.
 * Rastrea oscilaciones laterales rápidas en el eje X con la mano abierta.
 * Secuencia: un primer barrido lateral arma el detector; cada cambio de sentido
 * válido suma 1/3 -> 2/3 -> 3/3 y dispara la acción de retorno.
 */
export class WaveGoodbyeDetector {
  private lastDirection: -1 | 0 | 1 = 0;
  private anchorX: number | null = null;
  private lastSwingTime = 0;
  private reversals = 0;
  private cooldownUntil = 0;

  private readonly requiredWaves = 3;
  private readonly minVelocityX = 230; // Velocidad horizontal mínima (mm/s)
  private readonly minTravelMm = 36;   // Recorrido mínimo entre cambios de sentido (mm)
  private readonly maxPauseMs = 800;   // Pausa máxima permitida entre sacudidas (ms)
  private readonly maxGrabStrength = 0.55; // La mano debe permanecer abierta
  private readonly cooldownMs = 1500;  // Tiempo de espera tras disparar el gesto

  /**
   * Procesa la posición y velocidad actual de la palma para detectar el saludo ×3.
   */
  update(
    palmPosition: [number, number, number],
    palmVelocity: [number, number, number],
    grabStrength: number,
    now: number,
  ): { triggered: boolean; progress: number } {
    if (now < this.cooldownUntil) {
      return { triggered: false, progress: 0 };
    }

    // Caduca la secuencia parcial si el usuario deja de mover la mano
    if (this.lastSwingTime > 0 && now - this.lastSwingTime > this.maxPauseMs) {
      this.resetSequence();
    }

    // Ignorar si el usuario tiene el puño cerrado
    if (grabStrength > this.maxGrabStrength) {
      return { triggered: false, progress: this.reversals };
    }

    const [x] = palmPosition;
    const [vx, vy] = palmVelocity;

    // Exigir movimiento predominantemente horizontal y suficientemente rápido
    if (Math.abs(vx) < this.minVelocityX || Math.abs(vx) <= Math.abs(vy) * 1.05) {
      return { triggered: false, progress: this.reversals };
    }

    const dir: -1 | 1 = vx > 0 ? 1 : -1;

    if (this.lastDirection === 0) {
      this.lastDirection = dir;
      this.anchorX = x;
      this.lastSwingTime = now;
      return { triggered: false, progress: 0 };
    }

    // Mientras sigue en el mismo sentido, actualizamos el extremo alcanzado
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

    // Cambio de sentido: comprobamos que el desplazamiento lateral haya sido suficiente
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

  /** Reinicia el conteo de oscilaciones del saludo. */
  resetSequence(): void {
    this.lastDirection = 0;
    this.anchorX = null;
    this.lastSwingTime = 0;
    this.reversals = 0;
  }
}
