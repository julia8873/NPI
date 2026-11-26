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
