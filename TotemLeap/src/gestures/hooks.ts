import { useContext, useEffect, useRef } from "react";
import { LeapContext } from "./LeapContext";
import type { FistListener, PinchListener, SwipeDirection, TwoHandSwipeListener } from "./types";

/**
 * Hook de acceso directo al contexto de Leap Motion y al estado del simulador.
 */
export function useLeap() {
  const ctx = useContext(LeapContext);
  if (!ctx) {
    throw new Error("useLeap debe usarse dentro de un <LeapProvider>");
  }
  return ctx;
}

interface UseGestureTargetOptions {
  id: string;
  onSelect: () => void;
  disabled?: boolean;
  priority?: number;
}

/**
 * Hook reutilizable para que cualquier elemento DOM (tarjeta, botón, elemento de lista)
 * responda al desplazamiento de la mano (hover) y se active con el gesto "Cerrar puño".
 */
export function useGestureTarget<T extends HTMLElement>({
  id,
  onSelect,
  disabled = false,
  priority = 1,
}: UseGestureTargetOptions) {
  const ref = useRef<T | null>(null);
  const { registerTarget, hoveredTargetId, fistProgress } = useLeap();

  const onSelectRef = useRef(onSelect);
  useEffect(() => { onSelectRef.current = onSelect; }, [onSelect]);

  useEffect(() => {
    const element = ref.current;
    if (!element) return;
    return registerTarget({
      id,
      element,
      onSelect: () => onSelectRef.current(),
      disabled,
      priority,
    });
  }, [id, disabled, priority, registerTarget]);

  const isHovered = hoveredTargetId === id && !disabled;
  return { ref, isHovered, fistProgress: isHovered ? fistProgress : 0 };
}

/**
 * Hook reutilizable para reaccionar al gesto "Adiós (3 veces)" desde cualquier pantalla
 * (por ejemplo, para volver al menú principal desde dentro de una sección).
 */
export function useWaveGoodbye(onWave: () => void, enabled = true) {
  const { subscribeWave, waveProgress } = useLeap();
  const onWaveRef = useRef(onWave);
  useEffect(() => { onWaveRef.current = onWave; }, [onWave]);

  useEffect(() => {
    if (!enabled) return;
    return subscribeWave(() => onWaveRef.current());
  }, [enabled, subscribeWave]);

  return { waveProgress };
}

/**
 * Hook reutilizable para escuchar cierres de puño globales en cualquier componente,
 * incluso cuando no hay un objetivo concreto bajo el cursor.
 */
export function useFistListener(onFist: FistListener, enabled = true) {
  const { subscribeFist } = useLeap();
  const onFistRef = useRef(onFist);
  useEffect(() => { onFistRef.current = onFist; }, [onFist]);

  useEffect(() => {
    if (!enabled) return;
    return subscribeFist((targetId) => onFistRef.current(targetId));
  }, [enabled, subscribeFist]);
}

/**
 * Hook reutilizable para escuchar el gesto "Pellizco" (índice + pulgar) desde
 * cualquier pantalla o sección. No tiene acción predeterminada en el menú principal;
 * el equipo puede usarlo para acciones dentro de las vistas de sección.
 *
 * @example
 * usePinch((targetId) => {
 *   console.log("Pellizco detectado sobre:", targetId);
 * });
 */
export function usePinch(onPinch: PinchListener, enabled = true) {
  const { subscribePinch, pinchProgress } = useLeap();
  const onPinchRef = useRef(onPinch);
  useEffect(() => { onPinchRef.current = onPinch; }, [onPinch]);

  useEffect(() => {
    if (!enabled) return;
    return subscribePinch((targetId) => onPinchRef.current(targetId));
  }, [enabled, subscribePinch]);

  return { pinchProgress };
}

/**
 * Hook reutilizable para el gesto "Barrido con dos manos" de navegación de páginas.
 * - `direction = "left"`  → retroceder página / sección anterior.
 * - `direction = "right"` → avanzar página / sección siguiente.
 *
 * @example
 * useTwoHandSwipe((direction) => {
 *   if (direction === "left") irPaginaAnterior();
 *   if (direction === "right") irPaginaSiguiente();
 * });
 */
export function useTwoHandSwipe(
  onSwipe: TwoHandSwipeListener,
  enabled = true,
) {
  const { subscribeSwipe } = useLeap();
  const onSwipeRef = useRef(onSwipe);
  useEffect(() => { onSwipeRef.current = onSwipe; }, [onSwipe]);

  useEffect(() => {
    if (!enabled) return;
    return subscribeSwipe((direction: SwipeDirection) =>
      onSwipeRef.current(direction),
    );
  }, [enabled, subscribeSwipe]);
}
