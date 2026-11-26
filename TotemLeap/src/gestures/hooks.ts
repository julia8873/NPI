import { useContext, useEffect, useRef } from "react";
import { LeapContext } from "./LeapContext";
import type { FistListener } from "./types";

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
  useEffect(() => {
    onSelectRef.current = onSelect;
  }, [onSelect]);

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

  return {
    ref,
    isHovered,
    fistProgress: isHovered ? fistProgress : 0,
  };
}

/**
 * Hook reutilizable para reaccionar al gesto "Adiós (3 veces)" desde cualquier pantalla
 * (por ejemplo, para volver al menú principal desde dentro de una sección).
 */
export function useWaveGoodbye(onWave: () => void, enabled = true) {
  const { subscribeWave, waveProgress } = useLeap();
  const onWaveRef = useRef(onWave);
  useEffect(() => {
    onWaveRef.current = onWave;
  }, [onWave]);

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
  useEffect(() => {
    onFistRef.current = onFist;
  }, [onFist]);

  useEffect(() => {
    if (!enabled) return;
    return subscribeFist((targetId) => onFistRef.current(targetId));
  }, [enabled, subscribeFist]);
}
