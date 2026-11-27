import { useState } from "react";
import "./App.css";
import { GestureCursor } from "./components/GestureCursor";
import { GestureGuideBar } from "./components/GestureGuideBar";
import { GestureTutorialView } from "./components/GestureTutorialView";
import { NewsBanner } from "./components/NewsBanner";
import { SectionCard } from "./components/SectionCard";
import { SectionView } from "./components/SectionView";
import { TotemHeader } from "./components/TotemHeader";
import { AccessibilityProvider } from "./context/AccessibilityProvider";
import { SECTIONS } from "./data/sections";
import { LeapProvider } from "./gestures/LeapProvider";
import type { SectionId } from "./i18n/translations";

/**
 * Carcasa principal del tótem que alterna entre el menú de 8 secciones,
 * la vista interna de la sección seleccionada y la zona de tutorial de gestos.
 */
function TotemShell() {
  const [activeSection, setActiveSection] = useState<SectionId | null>(null);
  const [tutorialOpen, setTutorialOpen] = useState(false);

  const handleEnterSection = (id: SectionId) => {
    setTutorialOpen(false);
    setActiveSection(id);
  };

  const handleToggleTutorial = () => {
    setTutorialOpen((prev) => {
      const next = !prev;
      if (next) setActiveSection(null);
      return next;
    });
  };

  return (
    <div className="totem-viewport">
      <div className="totem-frame">
        <TotemHeader
          tutorialOpen={tutorialOpen}
          onToggleTutorial={handleToggleTutorial}
        />

        {tutorialOpen ? (
          <GestureTutorialView onBack={() => setTutorialOpen(false)} />
        ) : activeSection === null ? (
          <div className="main-menu-content">
            <NewsBanner />
            <main className="sections-grid" aria-label="Secciones principales">
              {SECTIONS.map((theme) => (
                <SectionCard
                  key={theme.id}
                  theme={theme}
                  onEnter={handleEnterSection}
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

        <GestureGuideBar inSection={activeSection !== null || tutorialOpen} />
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
