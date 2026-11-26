import { useState } from "react";
import "./App.css";
import { GestureCursor } from "./components/GestureCursor";
import { GestureGuideBar } from "./components/GestureGuideBar";
import { NewsBanner } from "./components/NewsBanner";
import { SectionCard } from "./components/SectionCard";
import { SectionView } from "./components/SectionView";
import { TotemHeader } from "./components/TotemHeader";
import { AccessibilityProvider } from "./context/AccessibilityProvider";
import { SECTIONS } from "./data/sections";
import { LeapProvider } from "./gestures/LeapProvider";
import type { SectionId } from "./i18n/translations";

/**
 * Carcasa principal del tótem que alterna entre el menú de 8 secciones
 * y la vista interna de la sección seleccionada.
 */
function TotemShell() {
  const [activeSection, setActiveSection] = useState<SectionId | null>(null);

  return (
    <div className="totem-viewport">
      <div className="totem-frame">
        <TotemHeader />

        {activeSection === null ? (
          <div className="main-menu-content">
            <NewsBanner />
            <main className="sections-grid" aria-label="Secciones principales">
              {SECTIONS.map((theme) => (
                <SectionCard
                  key={theme.id}
                  theme={theme}
                  onEnter={setActiveSection}
                />
              ))}
            </main>
          </div>
        ) : (
          <SectionView
            sectionId={activeSection}
            onBack={() => setActiveSection(null)}
          />
        )}

        <GestureGuideBar inSection={activeSection !== null} />
      </div>

      <GestureCursor />
    </div>
  );
}

/**
 * Componente raíz de la aplicación: envuelve la interfaz con los proveedores
 * globales de accesibilidad (idioma y fuente) y de gestos (Leap Motion).
 */
export default function App() {
  return (
    <AccessibilityProvider>
      <LeapProvider>
        <TotemShell />
      </LeapProvider>
    </AccessibilityProvider>
  );
}
