import { createContext, type RefObject } from "react";
import type {
  FistListener,
  GestureTargetEntry,
  LeapTelemetry,
  WaveListener,
} from "./types";

/** Estado y operaciones compartidas por el proveedor de gestos Leap Motion. */
export interface LeapContextValue {
  connected: boolean;
  handsCount: number;
  handActive: boolean;
  hoveredTargetId: string | null;
  fistProgress: number;
  waveProgress: number;
  lastGestureFeedback: "fist" | "wave" | null;
  telemetry: LeapTelemetry;
  cursorRef: RefObject<HTMLDivElement | null>;
  simulatorEnabled: boolean;
  setSimulatorEnabled: (enabled: boolean) => void;
  registerTarget: (entry: GestureTargetEntry) => () => void;
  subscribeWave: (listener: WaveListener) => () => void;
  subscribeFist: (listener: FistListener) => () => void;
  triggerSimulatedFist: () => void;
  triggerSimulatedWave: () => void;
}

/** Contexto React que distribuye el estado del sensor y el registro de objetivos gestuales. */
export const LeapContext = createContext<LeapContextValue | null>(null);
