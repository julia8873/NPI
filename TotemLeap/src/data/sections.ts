import type { SectionId } from "../i18n/translations";

/** Paleta de colores y degradados visuales asociados a cada sección del menú. */
export interface SectionTheme {
  id: SectionId;
  cardBg: string;
  cardHoverBg: string;
  iconBg: string;
  accentColor: string;
  borderColor: string;
  glowColor: string;
}

/** Lista ordenada de las 8 secciones principales del tótem ETSIIT Go. */
export const SECTIONS: SectionTheme[] = [
  {
    id: "actividades",
    cardBg: "linear-gradient(145deg, #f4edff 0%, #efe6ff 100%)",
    cardHoverBg: "linear-gradient(145deg, #efe4ff 0%, #e4d4ff 100%)",
    iconBg: "linear-gradient(135deg, #a855f7 0%, #7c3aed 100%)",
    accentColor: "#7c3aed",
    borderColor: "#a855f7",
    glowColor: "rgba(168, 85, 247, 0.24)",
  },
  {
    id: "biblioteca",
    cardBg: "linear-gradient(145deg, #e7f8f5 0%, #dcf4ef 100%)",
    cardHoverBg: "linear-gradient(145deg, #dbf5f0 0%, #c6ede4 100%)",
    iconBg: "linear-gradient(135deg, #2dd4bf 0%, #0d9488 100%)",
    accentColor: "#0f766e",
    borderColor: "#14b8a6",
    glowColor: "rgba(20, 184, 166, 0.24)",
  },
  {
    id: "mapa",
    cardBg: "linear-gradient(145deg, #ebf3ff 0%, #e1ecff 100%)",
    cardHoverBg: "linear-gradient(145deg, #e0ecff 0%, #cfe1ff 100%)",
    iconBg: "linear-gradient(135deg, #60a5fa 0%, #2563eb 100%)",
    accentColor: "#1d4ed8",
    borderColor: "#3b82f6",
    glowColor: "rgba(59, 130, 246, 0.24)",
  },
  {
    id: "profesorado",
    cardBg: "linear-gradient(145deg, #fdecf4 0%, #fae2ee 100%)",
    cardHoverBg: "linear-gradient(145deg, #fce2ee 0%, #f7cfe2 100%)",
    iconBg: "linear-gradient(135deg, #f472b6 0%, #db2777 100%)",
    accentColor: "#be185d",
    borderColor: "#ec4899",
    glowColor: "rgba(236, 72, 153, 0.24)",
  },
  {
    id: "asignaturas",
    cardBg: "linear-gradient(145deg, #e8f9ef 0%, #dcf5e6 100%)",
    cardHoverBg: "linear-gradient(145deg, #dcf5e6 0%, #c8eed8 100%)",
    iconBg: "linear-gradient(135deg, #4ade80 0%, #16a34a 100%)",
    accentColor: "#15803d",
    borderColor: "#22c55e",
    glowColor: "rgba(34, 197, 94, 0.24)",
  },
  {
    id: "secretaria",
    cardBg: "linear-gradient(145deg, #fef5dc 0%, #fdecc6 100%)",
    cardHoverBg: "linear-gradient(145deg, #fdeec5 0%, #fcdf9c 100%)",
    iconBg: "linear-gradient(135deg, #f59e0b 0%, #d97706 100%)",
    accentColor: "#b45309",
    borderColor: "#f59e0b",
    glowColor: "rgba(245, 158, 11, 0.28)",
  },
  {
    id: "comedor",
    cardBg: "linear-gradient(145deg, #fdedf0 0%, #fae1e6 100%)",
    cardHoverBg: "linear-gradient(145deg, #fce1e6 0%, #f7cdd5 100%)",
    iconBg: "linear-gradient(135deg, #fb7185 0%, #e11d48 100%)",
    accentColor: "#be123c",
    borderColor: "#f43f5e",
    glowColor: "rgba(244, 63, 94, 0.24)",
  },
  {
    id: "faq",
    cardBg: "linear-gradient(145deg, #edf3fb 0%, #e3ecf8 100%)",
    cardHoverBg: "linear-gradient(145deg, #e2ebf8 0%, #cfe0f5 100%)",
    iconBg: "linear-gradient(135deg, #38bdf8 0%, #0284c7 100%)",
    accentColor: "#0369a1",
    borderColor: "#0ea5e9",
    glowColor: "rgba(14, 165, 233, 0.24)",
  },
];

/** Mapa de acceso rápido por identificador de sección para reutilizar estilos en vistas internas. */
export const SECTION_THEME_MAP: Record<SectionId, SectionTheme> =
  Object.fromEntries(SECTIONS.map((s) => [s.id, s])) as Record<
    SectionId,
    SectionTheme
  >;
