import { createContext, useContext } from "react";
import type {
  FontScale,
  Language,
  TranslationDictionary,
} from "../i18n/translations";

/** Estado y acciones globales de accesibilidad (idioma y tamaño de texto). */
export interface AccessibilityContextValue {
  language: Language;
  setLanguage: (lang: Language) => void;
  toggleLanguage: () => void;
  fontScale: FontScale;
  cycleFontScale: () => void;
  t: TranslationDictionary;
}

/** Contexto React para compartir la configuración de accesibilidad en toda la app. */
export const AccessibilityContext =
  createContext<AccessibilityContextValue | null>(null);

/**
 * Hook para leer y modificar el idioma actual, la escala tipográfica y el diccionario `t`.
 */
export function useAccessibility() {
  const ctx = useContext(AccessibilityContext);
  if (!ctx) {
    throw new Error(
      "useAccessibility debe usarse dentro de un <AccessibilityProvider>",
    );
  }
  return ctx;
}
