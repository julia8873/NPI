import {
  useCallback,
  useEffect,
  useMemo,
  useRef,
  useState,
  type ReactNode,
} from "react";
import {
  FistDetector,
  WaveGoodbyeDetector,
  mapLeapToScreen,
  type ScreenPoint,
} from "./detectors";
import { LeapContext, type LeapContextValue } from "./LeapContext";
import type {
  FistListener,
  GestureTargetEntry,
  LeapFrame,
  LeapTelemetry,
  WaveListener,
} from "./types";

const LEAP_WS_URL = "ws://127.0.0.1:6437/v6.json";

const INITIAL_TELEMETRY: LeapTelemetry = {
  x: 0,
  y: 0,
  z: 0,
  pinch: 0,
  grab: 0,
  vx: 0,
};

/**
 * Proveedor global de interacción gestual con Leap Motion.
 * Gestiona la conexión WebSocket, el cursor suavizado a 60+ FPS,
 * la detección de hover sobre objetivos registrados, el gesto de cerrar puño,
 * el gesto de despedida ×3 y un modo de simulación opcional con ratón/teclado.
 */
export function LeapProvider({ children }: { children: ReactNode }) {
  const [connected, setConnected] = useState(false);
  const [handsCount, setHandsCount] = useState(0);
  const [hoveredTargetId, setHoveredTargetId] = useState<string | null>(null);
  const [fistProgress, setFistProgress] = useState(0);
  const [waveProgress, setWaveProgress] = useState(0);
  const [lastGestureFeedback, setLastGestureFeedback] = useState<
    "fist" | "wave" | null
  >(null);
  const [telemetry, setTelemetry] = useState<LeapTelemetry>(INITIAL_TELEMETRY);
  const [simulatorEnabled, setSimulatorEnabled] = useState(false);

  const cursorRef = useRef<HTMLDivElement | null>(null);
  const targetsRef = useRef<Map<string, GestureTargetEntry>>(new Map());
  const waveListenersRef = useRef<Set<WaveListener>>(new Set());
  const fistListenersRef = useRef<Set<FistListener>>(new Set());

  const smoothedPosRef = useRef<ScreenPoint>({
    x: typeof window !== "undefined" ? window.innerWidth / 2 : 500,
    y: typeof window !== "undefined" ? window.innerHeight / 2 : 500,
  });
  const hoveredIdRef = useRef<string | null>(null);
  const lastFistBucketRef = useRef(0);
  const lastWaveProgressRef = useRef(0);
  const lastTelemetryUpdateRef = useRef(0);
  const feedbackTimerRef = useRef<number | undefined>(undefined);

  const fistDetectorRef = useRef(new FistDetector());
  const waveDetectorRef = useRef(new WaveGoodbyeDetector());

  // Muestra un destello visual breve cuando se reconoce un gesto discreto
  const flashGesture = useCallback((kind: "fist" | "wave") => {
    setLastGestureFeedback(kind);
    if (feedbackTimerRef.current) {
      window.clearTimeout(feedbackTimerRef.current);
    }
    feedbackTimerRef.current = window.setTimeout(() => {
      setLastGestureFeedback(null);
    }, 650);
  }, []);

  // Comprueba qué elemento registrado está bajo las coordenadas (cx, cy) del cursor
  const hitTestTargets = useCallback((cx: number, cy: number): string | null => {
    let matched: GestureTargetEntry | null = null;
    for (const entry of targetsRef.current.values()) {
      if (entry.disabled) continue;
      const rect = entry.element.getBoundingClientRect();
      if (rect.width === 0 || rect.height === 0) continue;
      const inside =
        cx >= rect.left &&
        cx <= rect.right &&
        cy >= rect.top &&
        cy <= rect.bottom;
      if (inside) {
        if (!matched || entry.priority >= matched.priority) {
          matched = entry;
        }
      }
    }
    return matched ? matched.id : null;
  }, []);

  // Solo actualiza el estado de React cuando cambia el elemento bajo el cursor
  const updateHoveredTarget = useCallback((nextId: string | null) => {
    if (hoveredIdRef.current !== nextId) {
      hoveredIdRef.current = nextId;
      setHoveredTargetId(nextId);
    }
  }, []);

  // Ejecuta la acción del elemento señalado al detectar "Cerrar puño"
  const fireFistSelection = useCallback(() => {
    const currentId = hoveredIdRef.current;
    flashGesture("fist");
    if (currentId) {
      const target = targetsRef.current.get(currentId);
      if (target && !target.disabled) {
        target.onSelect();
      }
    }
    for (const listener of fistListenersRef.current) {
      listener(currentId);
    }
  }, [flashGesture]);

  // Notifica a las vistas suscritas que se ha completado el gesto "Adiós ×3"
  const fireWaveGoodbye = useCallback(() => {
    flashGesture("wave");
    lastWaveProgressRef.current = 0;
    setWaveProgress(0);
    for (const listener of waveListenersRef.current) {
      listener();
    }
  }, [flashGesture]);

  // Conexión WebSocket con el servicio local de Leap Motion y reconexión automática
  useEffect(() => {
    let ws: WebSocket | null = null;
    let reconnectTimer: number | undefined;
    let unmounted = false;

    const connect = () => {
      if (unmounted) return;
      try {
        ws = new WebSocket(LEAP_WS_URL);
      } catch {
        reconnectTimer = window.setTimeout(connect, 2500);
        return;
      }

      ws.onopen = () => {
        if (unmounted) return;
        setConnected(true);
        // Permite recibir datos aunque la ventana no tenga el foco activo
        ws?.send(JSON.stringify({ background: true }));
      };

      ws.onclose = () => {
        if (unmounted) return;
        setConnected(false);
        setHandsCount(0);
        if (!simulatorEnabled) {
          updateHoveredTarget(null);
        }
        reconnectTimer = window.setTimeout(connect, 2500);
      };

      ws.onerror = () => {
        ws?.close();
      };

      ws.onmessage = (event) => {
        const frame: LeapFrame = JSON.parse(event.data);
        const hands = frame.hands ?? [];
        const count = hands.length;
        setHandsCount(count);

        if (count === 0) {
          fistDetectorRef.current.reset();
          waveDetectorRef.current.resetSequence();
          if (!simulatorEnabled) {
            updateHoveredTarget(null);
          }
          if (lastFistBucketRef.current !== 0) {
            lastFistBucketRef.current = 0;
            setFistProgress(0);
          }
          if (lastWaveProgressRef.current !== 0) {
            lastWaveProgressRef.current = 0;
            setWaveProgress(0);
          }
          return;
        }

        const hand = hands[0];
        const now = performance.now();

        // 1. Mueve el cursor virtual suavizado directamente sobre el DOM
        const nextPos = mapLeapToScreen(
          hand.palmPosition,
          smoothedPosRef.current,
          window.innerWidth,
          window.innerHeight,
        );
        smoothedPosRef.current = nextPos;

        if (cursorRef.current) {
          cursorRef.current.style.transform = `translate3d(${nextPos.x}px, ${nextPos.y}px, 0)`;
        }

        // 2. Comprueba colisión (hover) con los elementos interactivos registrados
        const hitId = hitTestTargets(nextPos.x, nextPos.y);
        updateHoveredTarget(hitId);

        // 3. Detección del gesto "Cerrar puño"
        const fist = fistDetectorRef.current.update(hand.grabStrength, now);
        const fistBucket = Math.round(fist.progress * 20) / 20;
        if (fistBucket !== lastFistBucketRef.current) {
          lastFistBucketRef.current = fistBucket;
          setFistProgress(fistBucket);
        }
        if (fist.triggered) {
          fireFistSelection();
        }

        // 4. Detección del gesto "Adiós (3 veces)"
        const wave = waveDetectorRef.current.update(
          hand.palmPosition,
          hand.palmVelocity,
          hand.grabStrength,
          now,
        );
        if (wave.triggered) {
          fireWaveGoodbye();
        } else if (wave.progress !== lastWaveProgressRef.current) {
          lastWaveProgressRef.current = wave.progress;
          setWaveProgress(wave.progress);
        }

        // 5. Actualización periódica de telemetría (cada 120 ms para no saturar renders)
        if (now - lastTelemetryUpdateRef.current > 120) {
          lastTelemetryUpdateRef.current = now;
          setTelemetry({
            x: Math.round(hand.palmPosition[0]),
            y: Math.round(hand.palmPosition[1]),
            z: Math.round(hand.palmPosition[2]),
            pinch: Number(hand.pinchStrength.toFixed(2)),
            grab: Number(hand.grabStrength.toFixed(2)),
            vx: Math.round(hand.palmVelocity[0]),
          });
        }
      };
    };

    connect();

    return () => {
      unmounted = true;
      if (reconnectTimer) window.clearTimeout(reconnectTimer);
      ws?.close();
    };
  }, [
    fireFistSelection,
    fireWaveGoodbye,
    hitTestTargets,
    simulatorEnabled,
    updateHoveredTarget,
  ]);

  // Modo simulación opcional: mueve el cursor virtual con el ratón y usa F (puño) / W (adiós ×3)
  useEffect(() => {
    if (!simulatorEnabled) {
      if (handsCount === 0) {
        updateHoveredTarget(null);
      }
      return;
    }

    const handleMouseMove = (e: MouseEvent) => {
      smoothedPosRef.current = { x: e.clientX, y: e.clientY };
      if (cursorRef.current) {
        cursorRef.current.style.transform = `translate3d(${e.clientX}px, ${e.clientY}px, 0)`;
      }
      const hitId = hitTestTargets(e.clientX, e.clientY);
      updateHoveredTarget(hitId);
    };

    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.repeat) return;
      if (e.key.toLowerCase() === "f") {
        fireFistSelection();
      } else if (e.key.toLowerCase() === "w") {
        fireWaveGoodbye();
      }
    };

    window.addEventListener("mousemove", handleMouseMove);
    window.addEventListener("keydown", handleKeyDown);
    return () => {
      window.removeEventListener("mousemove", handleMouseMove);
      window.removeEventListener("keydown", handleKeyDown);
    };
  }, [
    fireFistSelection,
    fireWaveGoodbye,
    handsCount,
    hitTestTargets,
    simulatorEnabled,
    updateHoveredTarget,
  ]);

  // Registra un elemento DOM interactivo y devuelve su función de limpieza
  const registerTarget = useCallback((entry: GestureTargetEntry) => {
    targetsRef.current.set(entry.id, entry);
    return () => {
      targetsRef.current.delete(entry.id);
      if (hoveredIdRef.current === entry.id) {
        hoveredIdRef.current = null;
        setHoveredTargetId(null);
      }
    };
  }, []);

  // Suscribe un listener al gesto global "Adiós ×3"
  const subscribeWave = useCallback((listener: WaveListener) => {
    waveListenersRef.current.add(listener);
    return () => {
      waveListenersRef.current.delete(listener);
    };
  }, []);

  // Suscribe un listener al gesto global "Cerrar puño"
  const subscribeFist = useCallback((listener: FistListener) => {
    fistListenersRef.current.add(listener);
    return () => {
      fistListenersRef.current.delete(listener);
    };
  }, []);

  const triggerSimulatedFist = useCallback(() => {
    fireFistSelection();
  }, [fireFistSelection]);

  const triggerSimulatedWave = useCallback(() => {
    // Simula la progresión visual 1/3 -> 2/3 -> 3/3 antes de disparar el retorno
    setWaveProgress(1);
    window.setTimeout(() => setWaveProgress(2), 140);
    window.setTimeout(() => {
      fireWaveGoodbye();
    }, 280);
  }, [fireWaveGoodbye]);

  const value = useMemo<LeapContextValue>(
    () => ({
      connected,
      handsCount,
      handActive: handsCount > 0 || simulatorEnabled,
      hoveredTargetId,
      fistProgress,
      waveProgress,
      lastGestureFeedback,
      telemetry,
      cursorRef,
      simulatorEnabled,
      setSimulatorEnabled,
      registerTarget,
      subscribeWave,
      subscribeFist,
      triggerSimulatedFist,
      triggerSimulatedWave,
    }),
    [
      connected,
      handsCount,
      simulatorEnabled,
      hoveredTargetId,
      fistProgress,
      waveProgress,
      lastGestureFeedback,
      telemetry,
      registerTarget,
      subscribeWave,
      subscribeFist,
      triggerSimulatedFist,
      triggerSimulatedWave,
    ],
  );

  return <LeapContext.Provider value={value}>{children}</LeapContext.Provider>;
}
