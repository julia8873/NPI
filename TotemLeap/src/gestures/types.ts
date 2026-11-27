/** Mano detectada por el servicio WebSocket de Leap Motion (formato v6.json). */
export interface LeapHand {
  id: number;
  type: "left" | "right";
  /** Posición de la palma [X, Y, Z] en milímetros respecto al sensor. */
  palmPosition: [number, number, number];
  /** Velocidad de la palma [vX, vY, vZ] en mm/s. */
  palmVelocity: [number, number, number];
  /** Intensidad del gesto de pinza (0 = abierta, 1 = cerrada). */
  pinchStrength: number;
  /** Intensidad del gesto de puño (0 = mano abierta, 1 = puño cerrado). */
  grabStrength: number;
}

/** Frame enviado por Leap Motion en cada muestreo. */
export interface LeapFrame {
  hands?: LeapHand[];
}

/** Resumen de telemetría de la mano principal para depuración o diagnóstico. */
export interface LeapTelemetry {
  x: number;
  y: number;
  z: number;
  pinch: number;
  grab: number;
  vx: number;
}

/** Elemento interactivo registrado para responder a hover de mano y selección por puño. */
export interface GestureTargetEntry {
  id: string;
  element: HTMLElement;
  onSelect: () => void;
  disabled: boolean;
  priority: number;
}

/** Callback disparado al completar el gesto "Adiós (3 veces)". */
export type WaveListener = () => void;

/** Callback disparado al cerrar el puño (recibe el id del objetivo bajo el cursor, si existe). */
export type FistListener = (hoveredTargetId: string | null) => void;

/**
 * Callback disparado al detectar un pellizco (índice + pulgar).
 * Recibe el id del objetivo bajo el cursor, si existe.
 * No está conectado a ninguna acción por defecto; disponible para que el equipo lo use.
 */
export type PinchListener = (hoveredTargetId: string | null) => void;

/**
 * Dirección del barrido detectado con dos manos.
 * - `"left"`:  ambas manos se desplazan hacia la izquierda  → retroceder página.
 * - `"right"`: ambas manos se desplazan hacia la derecha     → avanzar página.
 */
export type SwipeDirection = "left" | "right";

/**
 * Callback disparado al detectar un barrido simultáneo con dos manos.
 * Recibe la dirección del barrido.
 */
export type TwoHandSwipeListener = (direction: SwipeDirection) => void;

/** Dedo o herramienta detectada en el frame de Leap Motion (v6.json). */
export interface LeapPointable {
  id: number;
  handId: number;
  /** 0 = pulgar, 1 = índice, 2 = corazón, 3 = anular, 4 = meñique. */
  type: 0 | 1 | 2 | 3 | 4;
  /** true si el dedo está estirado; false si está encogido hacia la palma. */
  extended: boolean;
  /** Posición 3D de la punta del dedo [X, Y, Z] en mm. */
  tipPosition: [number, number, number];
}

/** Mano detectada por el servicio WebSocket de Leap Motion (formato v6.json). */
export interface LeapHand {
  id: number;
  type: "left" | "right";
  /** Posición de la palma [X, Y, Z] en milímetros respecto al sensor. */
  palmPosition: [number, number, number];
  /** Velocidad de la palma [vX, vY, vZ] en mm/s. */
  palmVelocity: [number, number, number];
  /** Intensidad del gesto de pinza (0 = abierta, 1 = cerrada). */
  pinchStrength: number;
  /** Intensidad del gesto de puño (0 = mano abierta, 1 = puño cerrado). */
  grabStrength: number;
}

/** Frame enviado por Leap Motion en cada muestreo. */
export interface LeapFrame {
  hands?: LeapHand[];
  pointables?: LeapPointable[];
}
