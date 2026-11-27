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
  PinchDetector,
  TwoHandSwipeDetector,
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
  PinchListener,
  SwipeDirection,
  TwoHandSwipeListener,
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
 * la detección de hover sobre objetivos registrados y los cuatro gestos:
 *   1. Cerrar puño     → seleccionar elemento
 *   2. Pellizco        → disponible para el equipo (sin acción predeterminada)
 *   3. Adiós ×3        → volver al menú principal
 *   4. Barrido 2 manos → retroceder / avanzar página
 */
export function LeapProvider({ children }: { children: ReactNode }) {
  const [connected, setConnected] = useState(false);
  const [handsCount, setHandsCount] = useState(0);
  const [hoveredTargetId, setHoveredTargetId] = useState<string | null>(null);
  const [fistProgress, setFistProgress] = useState(0);
  const [pinchProgress, setPinchProgress] = useState(0);
  const [waveProgress, setWaveProgress] = useState(0);
  const [lastGestureFeedback, setLastGestureFeedback] = useState<
    "fist" | "pinch" | "wave" | "swipe" | null
  >(null);
  const [telemetry, setTelemetry] = useState<LeapTelemetry>(INITIAL_TELEMETRY);
  const [simulatorEnabled, setSimulatorEnabled] = useState(false);

  const cursorRef = useRef<HTMLDivElement | null>(null);
  const targetsRef = useRef<Map<string, GestureTargetEntry>>(new Map());
  const waveListenersRef = useRef<Set<WaveListener>>(new Set());
  const fistListenersRef = useRef<Set<FistListener>>(new Set());
  const pinchListenersRef = useRef<Set<PinchListener>>(new Set());
  const swipeListenersRef = useRef<Set<TwoHandSwipeListener>>(new Set());

  const smoothedPosRef = useRef<ScreenPoint>({
    x: typeof window !== "undefined" ? window.innerWidth / 2 : 500,
    y: typeof window !== "undefined" ? window.innerHeight / 2 : 500,
  });
  const hoveredIdRef = useRef<string | null>(null);
  const lastFistBucketRef = useRef(0);
  const lastPinchBucketRef = useRef(0);
  const lastWaveProgressRef = useRef(0);
  const lastTelemetryUpdateRef = useRef(0);
  const feedbackTimerRef = useRef<number | undefined>(undefined);

  // Instancias de los cuatro detectores (viven durante todo el ciclo de vida del proveedor)
  const fistDetectorRef = useRef(new FistDetector());
  const pinchDetectorRef = useRef(new PinchDetector());
  const waveDetectorRef = useRef(new WaveGoodbyeDetector());
  const swipeDetectorRef = useRef(new TwoHandSwipeDetector());

  // Muestra un destello visual breve cuando se reconoce un gesto discreto
  const flashGesture = useCallback(
    (kind: "fist" | "pinch" | "wave" | "swipe") => {
      setLastGestureFeedback(kind);
      if (feedbackTimerRef.current) {
        window.clearTimeout(feedbackTimerRef.current);
      }
      feedbackTimerRef.current = window.setTimeout(() => {
        setLastGestureFeedback(null);
      }, 650);
    },
    [],
  );

  // Comprueba qué elemento registrado está bajo las coordenadas (cx, cy) del cursor
  const hitTestTargets = useCallback(
    (cx: number, cy: number): string | null => {
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
        if (inside && (!matched || entry.priority >= matched.priority)) {
          matched = entry;
        }
      }
      return matched ? matched.id : null;
    },
    [],
  );

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
      if (target && !target.disabled) target.onSelect();
    }
    for (const listener of fistListenersRef.current) listener(currentId);
  }, [flashGesture]);

  // Notifica a los listeners suscritos que se ha detectado un pellizco
  const firePinch = useCallback(() => {
    const currentId = hoveredIdRef.current;
    flashGesture("pinch");
    for (const listener of pinchListenersRef.current) listener(currentId);
  }, [flashGesture]);

  // Notifica a las vistas suscritas que se ha completado el gesto "Adiós ×3"
  const fireWaveGoodbye = useCallback(() => {
    flashGesture("wave");
    lastWaveProgressRef.current = 0;
    setWaveProgress(0);
    for (const listener of waveListenersRef.current) listener();
  }, [flashGesture]);

  // Notifica a los listeners suscritos que se ha detectado un barrido con dos manos
  const fireTwoHandSwipe = useCallback(
    (direction: SwipeDirection) => {
      flashGesture("swipe");
      for (const listener of swipeListenersRef.current) listener(direction);
    },
    [flashGesture],
  );

  // ─── Conexión WebSocket con reconexión automática ─────────────────────────
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
        if (!simulatorEnabled) updateHoveredTarget(null);
        reconnectTimer = window.setTimeout(connect, 2500);
      };

      ws.onerror = () => ws?.close();

      ws.onmessage = (event) => {
        const frame: LeapFrame = JSON.parse(event.data);
        const hands = frame.hands ?? [];
        const count = hands.length;
        setHandsCount(count);

        // Sin manos: reiniciar todos los detectores
        if (count === 0) {
          fistDetectorRef.current.reset();
          pinchDetectorRef.current.reset();
          waveDetectorRef.current.resetSequence();
          swipeDetectorRef.current.reset();
          if (!simulatorEnabled) updateHoveredTarget(null);
          if (lastFistBucketRef.current !== 0) {
            lastFistBucketRef.current = 0;
            setFistProgress(0);
          }
          if (lastPinchBucketRef.current !== 0) {
            lastPinchBucketRef.current = 0;
            setPinchProgress(0);
          }
          if (lastWaveProgressRef.current !== 0) {
            lastWaveProgressRef.current = 0;
            setWaveProgress(0);
          }
          return;
        }

        const hand = hands[0];
        const handFingers = (frame.pointables ?? []).filter(
          (p) => p.handId === hand.id,
        );
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

        // 3. Detección del gesto "Cerrar puño" (mano principal)
        const fist = fistDetectorRef.current.update(hand, handFingers, now);
        const fistBucket = Math.round(fist.progress * 20) / 20;
        if (fistBucket !== lastFistBucketRef.current) {
          lastFistBucketRef.current = fistBucket;
          setFistProgress(fistBucket);
        }
        if (fist.triggered) fireFistSelection();

        // 4. Detección del gesto "Pellizco" (mano principal)
        const pinch = pinchDetectorRef.current.update(hand, handFingers, now);
        const pinchBucket = Math.round(pinch.progress * 20) / 20;
        if (pinchBucket !== lastPinchBucketRef.current) {
          lastPinchBucketRef.current = pinchBucket;
          setPinchProgress(pinchBucket);
        }
        if (pinch.triggered && !fist.isClosed) firePinch();

        // 5. Detección del gesto "Adiós ×3" (mano principal)
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

        // 6. Detección del gesto "Barrido con dos manos" (requiere exactamente 2 manos)
        if (count === 2) {
          const swipe = swipeDetectorRef.current.update(
            hands[0],
            hands[1],
            now,
          );
          if (swipe.triggered && swipe.direction) {
            fireTwoHandSwipe(swipe.direction);
          }
        } else {
          swipeDetectorRef.current.reset();
        }

        // 7. Actualización periódica de telemetría (cada 120 ms para no saturar renders)
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
    firePinch,
    fireTwoHandSwipe,
    fireWaveGoodbye,
    hitTestTargets,
    simulatorEnabled,
    updateHoveredTarget,
  ]);

  // ─── Modo simulación: ratón para mover cursor, teclado F/W/P/←/→ ─────────
  useEffect(() => {
    if (!simulatorEnabled) {
      if (handsCount === 0) updateHoveredTarget(null);
      return;
    }

    const handleMouseMove = (e: MouseEvent) => {
      smoothedPosRef.current = { x: e.clientX, y: e.clientY };
      if (cursorRef.current) {
        cursorRef.current.style.transform = `translate3d(${e.clientX}px, ${e.clientY}px, 0)`;
      }
      updateHoveredTarget(hitTestTargets(e.clientX, e.clientY));
    };

    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.repeat) return;
      switch (e.key.toLowerCase()) {
        case "f":
          fireFistSelection();
          break;
        case "w":
          fireWaveGoodbye();
          break;
        case "p":
          firePinch();
          break;
        case "arrowleft":
          fireTwoHandSwipe("left");
          break;
        case "arrowright":
          fireTwoHandSwipe("right");
          break;
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
    firePinch,
    fireTwoHandSwipe,
    fireWaveGoodbye,
    handsCount,
    hitTestTargets,
    simulatorEnabled,
    updateHoveredTarget,
  ]);

  // ─── API pública del proveedor ────────────────────────────────────────────

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

  const subscribeWave = useCallback((listener: WaveListener) => {
    waveListenersRef.current.add(listener);
    return () => {
      waveListenersRef.current.delete(listener);
    };
  }, []);

  const subscribeFist = useCallback((listener: FistListener) => {
    fistListenersRef.current.add(listener);
    return () => {
      fistListenersRef.current.delete(listener);
    };
  }, []);

  const subscribePinch = useCallback((listener: PinchListener) => {
    pinchListenersRef.current.add(listener);
    return () => {
      pinchListenersRef.current.delete(listener);
    };
  }, []);

  const subscribeSwipe = useCallback((listener: TwoHandSwipeListener) => {
    swipeListenersRef.current.add(listener);
    return () => {
      swipeListenersRef.current.delete(listener);
    };
  }, []);

  // Disparadores del simulador de gestos
  const triggerSimulatedFist = useCallback(
    () => fireFistSelection(),
    [fireFistSelection],
  );
  const triggerSimulatedWave = useCallback(() => {
    // Anima progresión visual 1/3 → 2/3 → 3/3 antes de disparar
    setWaveProgress(1);
    window.setTimeout(() => setWaveProgress(2), 140);
    window.setTimeout(() => {
      fireWaveGoodbye();
    }, 280);
  }, [fireWaveGoodbye]);
  const triggerSimulatedPinch = useCallback(() => firePinch(), [firePinch]);
  const triggerSimulatedSwipe = useCallback(
    (direction: SwipeDirection) => fireTwoHandSwipe(direction),
    [fireTwoHandSwipe],
  );

  const value = useMemo<LeapContextValue>(
    () => ({
      connected,
      handsCount,
      handActive: handsCount > 0 || simulatorEnabled,
      hoveredTargetId,
      fistProgress,
      pinchProgress,
      waveProgress,
      lastGestureFeedback,
      telemetry,
      cursorRef,
      simulatorEnabled,
      setSimulatorEnabled,
      registerTarget,
      subscribeWave,
      subscribeFist,
      subscribePinch,
      subscribeSwipe,
      triggerSimulatedFist,
      triggerSimulatedWave,
      triggerSimulatedPinch,
      triggerSimulatedSwipe,
    }),
    [
      connected,
      handsCount,
      simulatorEnabled,
      hoveredTargetId,
      fistProgress,
      pinchProgress,
      waveProgress,
      lastGestureFeedback,
      telemetry,
      registerTarget,
      subscribeWave,
      subscribeFist,
      subscribePinch,
      subscribeSwipe,
      triggerSimulatedFist,
      triggerSimulatedWave,
      triggerSimulatedPinch,
      triggerSimulatedSwipe,
    ],
  );

  return <LeapContext.Provider value={value}>{children}</LeapContext.Provider>;
}
