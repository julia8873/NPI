import {
  useCallback,
  useEffect,
  useMemo,
  useState,
  type ReactNode,
} from "react";
import {
  TRANSLATIONS,
  type FontScale,
  type Language,
} from "../i18n/translations";
import {
  AccessibilityContext,
  type AccessibilityContextValue,
} from "./AccessibilityContext";

const SCALE_ORDER: FontScale[] = ["normal", "large", "xlarge"];

const SCALE_MULTIPLIER: Record<FontScale, number> = {
  normal: 1,
  large: 1.14,
  xlarge: 1.28,
};

/**
 * Proveedor global de accesibilidad.
 * Gestiona el idioma activo (`es` / `en`) y el factor de escala tipográfica (`--font-scale`),
 * sincronizándolos con el elemento raíz del documento HTML.
 */
export function AccessibilityProvider({ children }: { children: ReactNode }) {
  const [language, setLanguage] = useState<Language>("es");
  const [fontScale, setFontScale] = useState<FontScale>("normal");

  // Alterna entre español e inglés
  const toggleLanguage = useCallback(() => {
    setLanguage((prev) => (prev === "es" ? "en" : "es"));
  }, []);

  // Rota entre los tres niveles de tamaño de letra: normal -> grande -> muy grande
  const cycleFontScale = useCallback(() => {
    setFontScale((prev) => {
      const idx = SCALE_ORDER.indexOf(prev);
      return SCALE_ORDER[(idx + 1) % SCALE_ORDER.length];
    });
  }, []);

  useEffect(() => {
    document.documentElement.lang = language;
    document.documentElement.style.setProperty(
      "--font-scale",
      String(SCALE_MULTIPLIER[fontScale]),
    );
    document.documentElement.setAttribute("data-font-scale", fontScale);
  }, [fontScale, language]);

  const value = useMemo<AccessibilityContextValue>(
    () => ({
      language,
      setLanguage,
      toggleLanguage,
      fontScale,
      cycleFontScale,
      t: TRANSLATIONS[language],
    }),
    [language, toggleLanguage, fontScale, cycleFontScale],
  );

  return (
    <AccessibilityContext.Provider value={value}>
      {children}
    </AccessibilityContext.Provider>
  );
}
