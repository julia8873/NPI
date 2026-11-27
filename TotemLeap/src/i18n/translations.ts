export type Language = "es" | "en";
export type FontScale = "normal" | "large" | "xlarge";

export type SectionId =
  | "actividades"
  | "biblioteca"
  | "mapa"
  | "profesorado"
  | "asignaturas"
  | "secretaria"
  | "comedor"
  | "faq";

export interface NewsSlide {
  id: string;
  badge: string;
  title: string;
  subtitle: string;
  qrLabel: string;
  qrValue: string;
}

/** Textos mínimos de cada sección: título visible en la tarjeta y placeholder temporal. */
export interface SectionTranslation {
  title: string;
  subtitle: string;
  placeholderText: string;
}

export interface GestureTutorialItem {
  title: string;
  howTo: string;
  actionHint: string;
  shortcut: string;
}

export interface TutorialTranslation {
  buttonLabel: string;
  title: string;
  subtitle: string;
  resetProgress: string;
  completedBadge: string;
  completedBanner: string;
  waveInterceptNote: string;
  swipeCardLabel: string;
  swipeCards: string[];
  targetButton: string;
  targetActivated: string;
  pinchCounter: string;
  gestures: {
    hover: GestureTutorialItem;
    fist: GestureTutorialItem;
    pinch: GestureTutorialItem;
    wave: GestureTutorialItem;
    swipe: GestureTutorialItem;
  };
}

export interface TranslationDictionary {
  brandSubtitle: string;
  accessibility: {
    languageLabel: string;
    fontScaleLabel: string;
    fontScaleNames: Record<FontScale, string>;
    sensorConnected: string;
    sensorWaiting: string;
    sensorSimulated: string;
  };
  gestures: {
    moveHand: string;
    closeFist: string;
    waveBack: string;
    fistBadge: string;
    waveDetected: string;
    fistDetected: string;
    waveProgress: string;
    backToMenu: string;
  };
  simulator: {
    title: string;
    statusConnected: string;
    statusDisconnected: string;
    handsLabel: string;
    toggleLabel: string;
    hint: string;
    simulateFist: string;
    simulateWave: string;
    simulatePinch: string;
    simulateSwipeLeft: string;
    simulateSwipeRight: string;
  };
  tutorial: TutorialTranslation;
  newsSlides: NewsSlide[];
  sections: Record<SectionId, SectionTranslation>;
}

export const TRANSLATIONS: Record<Language, TranslationDictionary> = {
  es: {
    brandSubtitle: "TÓTEM INTERACTIVO",
    accessibility: {
      languageLabel: "Cambiar idioma",
      fontScaleLabel: "Tamaño de letra",
      fontScaleNames: {
        normal: "Normal",
        large: "Grande",
        xlarge: "Muy grande",
      },
      sensorConnected: "Leap conectado",
      sensorWaiting: "Esperando Leap",
      sensorSimulated: "Modo simulación",
    },
    gestures: {
      moveHand: "Mueve la mano para elegir",
      closeFist: "Cierra el puño para entrar",
      waveBack: "Adiós para volver al menú",
      fistBadge: "Puño",
      waveDetected: "Volviendo al menú...",
      fistDetected: "Selección activada",
      waveProgress: "Adiós",
      backToMenu: "Volver al menú",
    },
    simulator: {
      title: "Estado Leap Motion",
      statusConnected: "Conectado (puerto 6437)",
      statusDisconnected: "Sin sensor físico",
      handsLabel: "Manos",
      toggleLabel: "Simular mano con ratón",
      hint: "Atajos: F = Puño · W = Adiós ×3 · P = Pellizco · ← → = Barrido 2 manos",
      simulateFist: "Simular Puño (F)",
      simulateWave: "Simular Adiós ×3 (W)",
      simulatePinch: "Simular Pellizco (P)",
      simulateSwipeLeft: "Barrido ← (flecha izq.)",
      simulateSwipeRight: "Barrido → (flecha dcha.)",
    },
    tutorial: {
      buttonLabel: "Probar gestos",
      title: "Tutorial y Zona de Pruebas",
      subtitle:
        "Aprende y verifica todos los gestos de Leap Motion en tiempo real",
      resetProgress: "Reiniciar pruebas",
      completedBadge: "Completado",
      completedBanner: "¡Has completado con éxito los 5 gestos del tótem!",
      waveInterceptNote:
        "En esta zona el gesto Adiós ×3 suma progreso aquí sin sacarte al menú.",
      swipeCardLabel: "Página de prueba",
      swipeCards: [
        "Tarjeta 1 · Horarios y Aulas",
        "Tarjeta 2 · Menú del Comedor",
        "Tarjeta 3 · Plano 3D ETSIIT",
      ],
      targetButton: "Apunta aquí y cierra el puño",
      targetActivated: "¡Puño detectado!",
      pinchCounter: "Pellizcos detectados",
      gestures: {
        hover: {
          title: "1. Apuntar con la mano",
          howTo:
            "Coloca una mano abierta sobre el sensor y muévela suavemente.",
          actionHint: "Pasa el cursor naranja por encima de esta tarjeta.",
          shortcut: "Ratón en modo simulación",
        },
        fist: {
          title: "2. Cerrar puño (Seleccionar)",
          howTo:
            "Con el cursor sobre un botón, cierra todos los dedos formando un puño.",
          actionHint: "Llena la barra de fuerza hasta el 100 % para activar.",
          shortcut: "Tecla F",
        },
        pinch: {
          title: "3. Pellizco (Índice + Pulgar)",
          howTo:
            "Junta las yemas de los dedos índice y pulgar manteniendo el resto relajados.",
          actionHint: "Realiza un pellizco en cualquier punto de la pantalla.",
          shortcut: "Tecla P",
        },
        wave: {
          title: "4. Adiós ×3 (Volver atrás)",
          howTo:
            "Con la mano abierta, saluda de izquierda a derecha 3 veces seguidas.",
          actionHint: "Completa las 3 oscilaciones seguidas sin pausar.",
          shortcut: "Tecla W",
        },
        swipe: {
          title: "5. Barrido con dos manos",
          howTo:
            "Sitúa ambas manos abiertas sobre el sensor y deslízalas a la vez a izquierda o derecha.",
          actionHint: "Cambia de tarjeta en el carrusel inferior.",
          shortcut: "Flechas ← / →",
        },
      },
    },
    newsSlides: [
      {
        id: "gamejam",
        badge: "NOTICIAS",
        title: "GameJam ETSIIT: ¡crea videojuegos en la UGR!",
        subtitle: "Fin de semana de creatividad y desarrollo · Inscríbete ya",
        qrLabel: "Escanea",
        qrValue: "https://etsiit.ugr.es/gamejam",
      },
      {
        id: "hackaton",
        badge: "EVENTOS",
        title: "Jornadas de Software Libre e IA en la ETSIIT",
        subtitle: "Charlas abiertas y talleres prácticos en el Salón de Actos",
        qrLabel: "Escanea",
        qrValue: "https://etsiit.ugr.es/jornadas",
      },
      {
        id: "becas",
        badge: "AVISOS",
        title: "Abierto el plazo de prácticas en empresa 2026/27",
        subtitle: "Consulta ofertas disponibles y fechas clave en Secretaría",
        qrLabel: "Escanea",
        qrValue: "https://etsiit.ugr.es/practicas",
      },
    ],
    sections: {
      actividades: {
        title: "Tablón de actividades",
        subtitle: "Eventos, talleres y charlas de hoy",
        placeholderText: "Contenido en desarrollo por el equipo.",
      },
      biblioteca: {
        title: "Biblioteca",
        subtitle: "Reserva aula de estudio y libros destacados",
        placeholderText: "Contenido en desarrollo por el equipo.",
      },
      mapa: {
        title: "Mapa",
        subtitle: "Modelo 3D, plantas y rutas guiadas",
        placeholderText: "Contenido en desarrollo por el equipo.",
      },
      profesorado: {
        title: "Profesorado",
        subtitle: "Fichas, disponibilidad y tutorías",
        placeholderText: "Contenido en desarrollo por el equipo.",
      },
      asignaturas: {
        title: "Asignaturas",
        subtitle: "Busca por grado y curso",
        placeholderText: "Contenido en desarrollo por el equipo.",
      },
      secretaria: {
        title: "Secretaría",
        subtitle: "Pide cita y consulta trámites",
        placeholderText: "Contenido en desarrollo por el equipo.",
      },
      comedor: {
        title: "Comedor",
        subtitle: "Menú semanal y opción vegetariana",
        placeholderText: "Contenido en desarrollo por el equipo.",
      },
      faq: {
        title: "Preguntas frecuentes",
        subtitle: "Respuestas rápidas a lo más común",
        placeholderText: "Contenido en desarrollo por el equipo.",
      },
    },
  },
  en: {
    brandSubtitle: "INTERACTIVE KIOSK",
    accessibility: {
      languageLabel: "Switch language",
      fontScaleLabel: "Text size",
      fontScaleNames: {
        normal: "Normal",
        large: "Large",
        xlarge: "Extra large",
      },
      sensorConnected: "Leap connected",
      sensorWaiting: "Waiting for Leap",
      sensorSimulated: "Simulation mode",
    },
    gestures: {
      moveHand: "Move hand to choose",
      closeFist: "Close fist to enter",
      waveBack: "Wave to return to menu",
      fistBadge: "Fist",
      waveDetected: "Returning to menu...",
      fistDetected: "Selection triggered",
      waveProgress: "Wave",
      backToMenu: "Back to menu",
    },
    simulator: {
      title: "Leap Motion Status",
      statusConnected: "Connected (port 6437)",
      statusDisconnected: "No physical sensor",
      handsLabel: "Hands",
      toggleLabel: "Simulate hand with mouse",
      hint: "Shortcuts: F = Fist · W = Wave ×3 · P = Pinch · ← → = Two-hand swipe",
      simulateFist: "Simulate Fist (F)",
      simulateWave: "Simulate Wave ×3 (W)",
      simulatePinch: "Simulate Pinch (P)",
      simulateSwipeLeft: "Swipe ← (left arrow)",
      simulateSwipeRight: "Swipe → (right arrow)",
    },
    tutorial: {
      buttonLabel: "Test gestures",
      title: "Gesture Tutorial & Sandbox",
      subtitle: "Learn and test every Leap Motion gesture in real time",
      resetProgress: "Reset progress",
      completedBadge: "Completed",
      completedBanner: "You have successfully tested all 5 kiosk gestures!",
      waveInterceptNote:
        "Inside this sandbox, Wave ×3 registers here without exiting to the main menu.",
      swipeCardLabel: "Sample page",
      swipeCards: [
        "Card 1 · Schedules & Rooms",
        "Card 2 · Dining Hall Menu",
        "Card 3 · ETSIIT 3D Map",
      ],
      targetButton: "Hover here and close fist",
      targetActivated: "Fist detected!",
      pinchCounter: "Pinches detected",
      gestures: {
        hover: {
          title: "1. Point with your hand",
          howTo: "Hold one open hand above the sensor and move it smoothly.",
          actionHint: "Move the orange cursor over this card.",
          shortcut: "Mouse in simulation mode",
        },
        fist: {
          title: "2. Close fist (Select)",
          howTo:
            "While pointing at a button, curl all fingers into a closed fist.",
          actionHint: "Fill the grab strength bar to 100% to trigger.",
          shortcut: "Key F",
        },
        pinch: {
          title: "3. Pinch (Index + Thumb)",
          howTo: "Bring your index finger and thumb tips together.",
          actionHint: "Perform a pinch anywhere over the screen.",
          shortcut: "Key P",
        },
        wave: {
          title: "4. Wave ×3 (Go back)",
          howTo: "With an open hand, wave side-to-side 3 times in a row.",
          actionHint: "Complete 3 consecutive swings without pausing.",
          shortcut: "Key W",
        },
        swipe: {
          title: "5. Two-hand swipe",
          howTo:
            "Place both open hands over the sensor and sweep them left or right together.",
          actionHint: "Switch pages in the carousel below.",
          shortcut: "Arrows ← / →",
        },
      },
    },
    newsSlides: [
      {
        id: "gamejam",
        badge: "NEWS",
        title: "ETSIIT GameJam: build video games at UGR!",
        subtitle: "Weekend of creativity & development · Register now",
        qrLabel: "Scan QR",
        qrValue: "https://etsiit.ugr.es/gamejam",
      },
      {
        id: "hackaton",
        badge: "EVENTS",
        title: "Open Source & AI Symposium at ETSIIT",
        subtitle: "Open talks and hands-on workshops at the Main Hall",
        qrLabel: "Scan QR",
        qrValue: "https://etsiit.ugr.es/jornadas",
      },
      {
        id: "becas",
        badge: "NOTICES",
        title: "2026/27 Company Internships now open",
        subtitle: "Check available positions and deadlines at Registrar Office",
        qrLabel: "Scan QR",
        qrValue: "https://etsiit.ugr.es/practicas",
      },
    ],
    sections: {
      actividades: {
        title: "Activity Board",
        subtitle: "Today's events, workshops and talks",
        placeholderText: "Content under development by the team.",
      },
      biblioteca: {
        title: "Library",
        subtitle: "Book study rooms and featured books",
        placeholderText: "Content under development by the team.",
      },
      mapa: {
        title: "Campus Map",
        subtitle: "3D model, floors and guided routes",
        placeholderText: "Content under development by the team.",
      },
      profesorado: {
        title: "Faculty",
        subtitle: "Profiles, availability and office hours",
        placeholderText: "Content under development by the team.",
      },
      asignaturas: {
        title: "Courses",
        subtitle: "Search by degree and academic year",
        placeholderText: "Content under development by the team.",
      },
      secretaria: {
        title: "Secretary  Office",
        subtitle: "Book an appointment and check procedures",
        placeholderText: "Content under development by the team.",
      },
      comedor: {
        title: "Dining Hall",
        subtitle: "Weekly menu and vegetarian option",
        placeholderText: "Content under development by the team.",
      },
      faq: {
        title: "FAQ",
        subtitle: "Quick answers to common questions",
        placeholderText: "Content under development by the team.",
      },
    },
  },
};
