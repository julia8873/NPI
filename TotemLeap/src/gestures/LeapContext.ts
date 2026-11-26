import { createContext, type RefObject } from "react";
import type {
  FistListener,
  GestureTargetEntry,
  LeapTelemetry,
  PinchListener,
  SwipeDirection,
  TwoHandSwipeListener,
  WaveListener,
} from "./types";

/** Estado y operaciones compartidas por el proveedor de gestos Leap Motion. */
export interface LeapContextValue {
  connected: boolean;
  handsCount: number;
  handActive: boolean;
  hoveredTargetId: string | null;

  // Progreso en tiempo real de cada gesto (0..1 ó 0..N)
  fistProgress: number;
  pinchProgress: number;
  waveProgress: number;

  /** Último gesto discreto reconocido (para efectos de retroalimentación visual). */
  lastGestureFeedback: "fist" | "pinch" | "wave" | "swipe" | null;

  telemetry: LeapTelemetry;
  cursorRef: RefObject<HTMLDivElement | null>;

  simulatorEnabled: boolean;
  setSimulatorEnabled: (enabled: boolean) => void;

  // Registro de objetivos gestuales
  registerTarget: (entry: GestureTargetEntry) => () => void;

  // Suscripciones a gestos globales
  subscribeWave: (listener: WaveListener) => () => void;
  subscribeFist: (listener: FistListener) => () => void;
  subscribePinch: (listener: PinchListener) => () => void;
  subscribeSwipe: (listener: TwoHandSwipeListener) => () => void;

  // Disparadores del simulador de gestos
  triggerSimulatedFist: () => void;
  triggerSimulatedWave: () => void;
  triggerSimulatedPinch: () => void;
  triggerSimulatedSwipe: (direction: SwipeDirection) => void;
}

/** Contexto React que distribuye el estado del sensor y el registro de objetivos gestuales. */
export const LeapContext = createContext<LeapContextValue | null>(null);
