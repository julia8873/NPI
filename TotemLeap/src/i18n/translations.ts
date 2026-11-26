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

export interface SectionTranslation {
  title: string;
  subtitle: string;
  previewHeadline: string;
  previewNote: string;
  sampleItems: Array<{ label: string; detail: string }>;
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
  };
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
      waveBack: "Adiós ×3: menú",
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
      hint: "Atajos: F = Puño · W = Adiós ×3",
      simulateFist: "Simular Puño (F)",
      simulateWave: "Simular Adiós ×3 (W)",
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
        previewHeadline: "Agenda de actividades de la escuela",
        previewNote:
          "Usa tu mano para explorar las actividades o haz el gesto Adiós ×3 para volver al menú principal.",
        sampleItems: [
          { label: "Taller de Git y CI/CD", detail: "17:00 · Aula 2.4" },
          { label: "Charla Ciberseguridad UGR", detail: "18:30 · Salón de Actos" },
          { label: "Club de Robótica ETSIIT", detail: "19:15 · Lab. -1.2" },
        ],
      },
      biblioteca: {
        title: "Biblioteca",
        subtitle: "Reserva aula de estudio y libros destacados",
        previewHeadline: "Puestos y salas de trabajo disponibles",
        previewNote:
          "Selecciona cualquier elemento cerrando el puño o realiza Adiós ×3 para regresar.",
        sampleItems: [
          { label: "Sala de trabajo en grupo A", detail: "Libre de 16:30 a 18:30" },
          { label: "Puestos individuales planta 1", detail: "24 plazas libres ahora" },
          { label: "Préstamo de portátiles", detail: "Mostrador principal" },
        ],
      },
      mapa: {
        title: "Mapa",
        subtitle: "Modelo 3D, plantas y rutas guiadas",
        previewHeadline: "Orientación y planos del edificio ETSIIT",
        previewNote:
          "Elige una planta con el puño o vuelve al menú con Adiós ×3.",
        sampleItems: [
          { label: "Planta Baja · Conserjería y Secretaría", detail: "Acceso principal" },
          { label: "Planta 1 · Aulas teóricas 1.1 – 1.8", detail: "Ala Norte" },
          { label: "Planta -1 · Laboratorios de Informática", detail: "Ala Sur" },
        ],
      },
      profesorado: {
        title: "Profesorado",
        subtitle: "Fichas, disponibilidad y tutorías",
        previewHeadline: "Directorio docente y horarios de tutoría",
        previewNote:
          "Desplaza la mano sobre un departamento y cierra el puño para consultar.",
        sampleItems: [
          { label: "Dpto. Ciencias de la Computación e IA", detail: "Planta 3 · Despachos 1-14" },
          { label: "Dpto. Lenguajes y Sistemas Informáticos", detail: "Planta 2 · Despachos 1-18" },
          { label: "Dpto. Arquitectura y Tecnología de Computadores", detail: "Planta 1 · Ala Oeste" },
        ],
      },
      asignaturas: {
        title: "Asignaturas",
        subtitle: "Busca por grado y curso",
        previewHeadline: "Guías docentes y aulas asignadas",
        previewNote:
          "Selecciona una titulación cerrando el puño o vuelve con Adiós ×3.",
        sampleItems: [
          { label: "Grado en Ingeniería Informática", detail: "1.º a 4.º curso · Menciones" },
          { label: "Grado en Ing. de Tecnologías de Telecomunicación", detail: "1.º a 4.º curso" },
          { label: "Doble Grado Informática y Matemáticas", detail: "1.º a 5.º curso" },
        ],
      },
      secretaria: {
        title: "Secretaría",
        subtitle: "Pide cita y consulta trámites",
        previewHeadline: "Atención al estudiante y cita previa",
        previewNote:
          "Cierra el puño sobre un trámite para ver los detalles o usa Adiós ×3 para volver.",
        sampleItems: [
          { label: "Horario de ventanilla", detail: "Lunes a viernes · 09:00 a 14:00" },
          { label: "Solicitud de certificados académicos", detail: "Cita previa o Sede UGR" },
          { label: "Entrega de documentación de becas", detail: "Planta baja · Ventanilla 2" },
        ],
      },
      comedor: {
        title: "Comedor",
        subtitle: "Menú semanal y opción vegetariana",
        previewHeadline: "Menú Universitario UGR · Hoy",
        previewNote:
          "Consulta las opciones del día o realiza Adiós ×3 para regresar.",
        sampleItems: [
          { label: "Primer plato", detail: "Crema de calabaza con picatostes" },
          { label: "Segundo plato", detail: "Pollo asado con patatas / Tofu marinado (Veg)" },
          { label: "Horario de servicio", detail: "13:00 a 15:30 · Comedor Fuentenueva" },
        ],
      },
      faq: {
        title: "Preguntas frecuentes",
        subtitle: "Respuestas rápidas a lo más común",
        previewHeadline: "Ayuda rápida sobre la ETSIIT",
        previewNote:
          "Elige una pregunta frecuente con el puño o haz Adiós ×3 para volver.",
        sampleItems: [
          { label: "¿Cómo conectarme a la Wi-Fi eduroam?", detail: "Usa tu correo @correo.ugr.es" },
          { label: "¿Dónde obtengo la TUI universitaria?", detail: "Punto de atención en Planta Baja" },
          { label: "¿Cómo reservo un aula de estudio?", detail: "Desde la sección Biblioteca o app UGR" },
        ],
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
      waveBack: "Wave ×3: menu",
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
      hint: "Shortcuts: F = Fist · W = Wave ×3",
      simulateFist: "Simulate Fist (F)",
      simulateWave: "Simulate Wave ×3 (W)",
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
        previewHeadline: "School activity schedule",
        previewNote:
          "Use your hand to browse activities or wave goodbye ×3 to return to the main menu.",
        sampleItems: [
          { label: "Git & CI/CD Workshop", detail: "17:00 · Room 2.4" },
          { label: "UGR Cybersecurity Talk", detail: "18:30 · Main Auditorium" },
          { label: "ETSIIT Robotics Club", detail: "19:15 · Lab -1.2" },
        ],
      },
      biblioteca: {
        title: "Library",
        subtitle: "Book study rooms and featured books",
        previewHeadline: "Available seats and study rooms",
        previewNote:
          "Close your fist over any item to select it, or wave ×3 to go back.",
        sampleItems: [
          { label: "Group Study Room A", detail: "Available 16:30 – 18:30" },
          { label: "Individual desks · Floor 1", detail: "24 seats free right now" },
          { label: "Laptop lending desk", detail: "Main library counter" },
        ],
      },
      mapa: {
        title: "Campus Map",
        subtitle: "3D model, floors and guided routes",
        previewHeadline: "ETSIIT building floor plans & navigation",
        previewNote:
          "Select a floor by closing your fist, or wave ×3 to return to menu.",
        sampleItems: [
          { label: "Ground Floor · Reception & Registrar", detail: "Main entrance" },
          { label: "Floor 1 · Lecture Rooms 1.1 – 1.8", detail: "North Wing" },
          { label: "Floor -1 · Computer Science Labs", detail: "South Wing" },
        ],
      },
      profesorado: {
        title: "Faculty",
        subtitle: "Profiles, availability and office hours",
        previewHeadline: "Faculty directory & tutoring schedule",
        previewNote:
          "Hover over a department and close your fist to view office details.",
        sampleItems: [
          { label: "Dept. Computer Science & AI", detail: "Floor 3 · Offices 1-14" },
          { label: "Dept. Software Engineering", detail: "Floor 2 · Offices 1-18" },
          { label: "Dept. Computer Architecture", detail: "Floor 1 · West Wing" },
        ],
      },
      asignaturas: {
        title: "Courses",
        subtitle: "Search by degree and academic year",
        previewHeadline: "Course syllabi & assigned classrooms",
        previewNote:
          "Select a degree by closing your fist or wave ×3 to return.",
        sampleItems: [
          { label: "BSc in Computer Engineering", detail: "Years 1–4 · Specializations" },
          { label: "BSc in Telecommunication Technologies", detail: "Years 1–4" },
          { label: "Double Degree CS & Mathematics", detail: "Years 1–5" },
        ],
      },
      secretaria: {
        title: "Registrar Office",
        subtitle: "Book an appointment and check procedures",
        previewHeadline: "Student services & appointment booking",
        previewNote:
          "Close your fist on a procedure for details, or wave ×3 to return.",
        sampleItems: [
          { label: "Desk opening hours", detail: "Monday to Friday · 09:00 – 14:00" },
          { label: "Academic transcript requests", detail: "Appointment or UGR Portal" },
          { label: "Scholarship documentation", detail: "Ground Floor · Desk 2" },
        ],
      },
      comedor: {
        title: "Dining Hall",
        subtitle: "Weekly menu and vegetarian option",
        previewHeadline: "UGR University Dining · Today's Menu",
        previewNote:
          "Check today's dishes or wave goodbye ×3 to return to the main menu.",
        sampleItems: [
          { label: "Starter", detail: "Roasted pumpkin cream with croutons" },
          { label: "Main course", detail: "Roast chicken & potatoes / Marinated tofu (Veg)" },
          { label: "Serving hours", detail: "13:00 – 15:30 · Fuentenueva Dining Hall" },
        ],
      },
      faq: {
        title: "FAQ",
        subtitle: "Quick answers to common questions",
        previewHeadline: "Quick help about ETSIIT",
        previewNote:
          "Choose a question with your fist or wave ×3 to go back.",
        sampleItems: [
          { label: "How do I connect to eduroam Wi-Fi?", detail: "Sign in with @correo.ugr.es" },
          { label: "Where do I get my UGR student ID card?", detail: "Ground Floor Help Point" },
          { label: "How can I book a study room?", detail: "Via Library section or UGR App" },
        ],
      },
    },
  },
};
