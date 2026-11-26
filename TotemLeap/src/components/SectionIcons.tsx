import type { SectionId } from "../i18n/translations";

/** Icono vectorial personalizado para cada una de las 8 secciones principales. */
export function SectionIcon({ id }: { id: SectionId }) {
  switch (id) {
    case "actividades":
      return (
        <svg viewBox="0 0 24 24" className="section-svg-icon" aria-hidden="true">
          <path
            d="M12 2.8l2.68 5.43 5.99.87-4.33 4.22 1.02 5.97L12 16.47l-5.36 2.82 1.02-5.97L3.33 9.1l5.99-.87L12 2.8z"
            fill="#fde047"
            stroke="#fef08a"
            strokeWidth="1.2"
            strokeLinejoin="round"
          />
        </svg>
      );
    case "biblioteca":
      return (
        <svg viewBox="0 0 24 24" className="section-svg-icon" aria-hidden="true">
          <rect x="4" y="14.5" width="16" height="4.2" rx="1.2" fill="#f8fafc" />
          <rect x="5" y="10" width="14" height="4" rx="1.2" fill="#bae6fd" />
          <rect x="4.5" y="5.5" width="15" height="4" rx="1.2" fill="#fecdd3" />
          <path d="M7.5 10h2.5v4H7.5zM14 5.5h2.2v4H14z" fill="#0f766e" opacity="0.55" />
        </svg>
      );
    case "mapa":
      return (
        <svg viewBox="0 0 24 24" className="section-svg-icon" aria-hidden="true">
          <path
            d="M12 12.5v8.5"
            stroke="#e2e8f0"
            strokeWidth="2.2"
            strokeLinecap="round"
          />
          <circle cx="12" cy="8.2" r="4.6" fill="#f43f5e" />
          <circle cx="10.4" cy="6.8" r="1.4" fill="#fda4af" />
        </svg>
      );
    case "profesorado":
      return (
        <svg viewBox="0 0 24 24" className="section-svg-icon" aria-hidden="true">
          <circle cx="12" cy="8.2" r="3.6" fill="#fde68a" />
          <path
            d="M8.5 7.4c.5-2.1 2-3.2 3.5-3.2 1.8 0 3.2 1.1 3.6 3.2"
            fill="#78350f"
          />
          <path
            d="M5.5 19.2c0-3.2 2.9-5.2 6.5-5.2s6.5 2 6.5 5.2"
            fill="#f8fafc"
          />
          <path d="M10.2 14.3l1.8 3 1.8-3" fill="#0284c7" />
        </svg>
      );
    case "asignaturas":
      return (
        <svg viewBox="0 0 24 24" className="section-svg-icon" aria-hidden="true">
          <path
            d="M3.8 6.4C6.5 5.3 9.4 5.6 12 7.2c2.6-1.6 5.5-1.9 8.2-.8v11.4c-2.7-1-5.6-.8-8.2.8-2.6-1.6-5.5-1.8-8.2-.8V6.4z"
            fill="#f8fafc"
            stroke="#dcfce7"
            strokeWidth="1"
          />
          <path d="M12 7.2v11.4" stroke="#86efac" strokeWidth="1.5" />
          <path
            d="M6.2 9.5h3.5M6.2 12.2h3.5M14.3 9.5h3.5M14.3 12.2h3.5"
            stroke="#16a34a"
            strokeWidth="1.4"
            strokeLinecap="round"
            opacity="0.6"
          />
        </svg>
      );
    case "secretaria":
      return (
        <svg viewBox="0 0 24 24" className="section-svg-icon" aria-hidden="true">
          <path
            d="M3.5 7.2A1.7 1.7 0 0 1 5.2 5.5h4.3l1.7 1.8h7.6a1.7 1.7 0 0 1 1.7 1.7v8.3a1.7 1.7 0 0 1-1.7 1.7H5.2a1.7 1.7 0 0 1-1.7-1.7V7.2z"
            fill="#fde047"
          />
          <rect x="6" y="7.8" width="12" height="2.4" rx="0.8" fill="#fef9c3" />
          <path d="M3.5 10.2h17v7.1a1.7 1.7 0 0 1-1.7 1.7H5.2a1.7 1.7 0 0 1-1.7-1.7v-7.1z" fill="#facc15" />
        </svg>
      );
    case "comedor":
      return (
        <svg viewBox="0 0 24 24" className="section-svg-icon" aria-hidden="true">
          <circle cx="12" cy="12" r="5.2" fill="#f8fafc" />
          <circle cx="12" cy="12" r="3.2" fill="none" stroke="#fda4af" strokeWidth="1.2" />
          <path
            d="M5 6.5v4.2m1.6-4.2v4.2M5.8 10.7v6.8M18.6 6.5v11"
            stroke="#ffffff"
            strokeWidth="1.7"
            strokeLinecap="round"
          />
        </svg>
      );
    case "faq":
      return (
        <svg viewBox="0 0 24 24" className="section-svg-icon" aria-hidden="true">
          <path
            d="M12 4.2a5.2 5.2 0 0 0-3.3 9.2c.8.7 1.3 1.5 1.4 2.4h3.8c.1-.9.6-1.7 1.4-2.4A5.2 5.2 0 0 0 12 4.2z"
            fill="#fef08a"
          />
          <rect x="9.8" y="16.8" width="4.4" height="1.8" rx="0.9" fill="#e2e8f0" />
          <rect x="10.4" y="19" width="3.2" height="1.4" rx="0.7" fill="#cbd5e1" />
        </svg>
      );
  }
}

/** Icono vectorial de mano abierta para indicar hover/desplazamiento. */
export function HandGlyph({ className = "" }: { className?: string }) {
  return (
    <svg viewBox="0 0 24 24" className={className} aria-hidden="true">
      <path
        d="M12 3.2c.7 0 1.2.5 1.2 1.2v6.1h.8V5.2c0-.7.5-1.2 1.2-1.2s1.2.5 1.2 1.2v5.8h.8V7.2c0-.7.5-1.2 1.2-1.2s1.2.5 1.2 1.2v6.6c0 4-2.7 7-6.6 7-2.6 0-4.7-1.3-5.9-3.6l-1.9-3.5c-.3-.6-.1-1.3.5-1.6.6-.3 1.3-.1 1.7.4l1.3 1.9V6.1c0-.7.5-1.2 1.2-1.2s1.2.5 1.2 1.2v4.4h.7V4.4c0-.7.5-1.2 1.2-1.2z"
        fill="#f59e0b"
        stroke="#b45309"
        strokeWidth="0.9"
        strokeLinejoin="round"
      />
    </svg>
  );
}

/** Icono vectorial de puño cerrado para indicar el gesto de entrar/seleccionar. */
export function FistGlyph({ className = "" }: { className?: string }) {
  return (
    <svg viewBox="0 0 24 24" className={className} aria-hidden="true">
      <path
        d="M7.5 10.2V8.5a1.5 1.5 0 0 1 3 0v1.2h.7V7.8a1.5 1.5 0 0 1 3 0v1.9h.7V8.4a1.5 1.5 0 0 1 3 0v2.1h.6a1.4 1.4 0 0 1 1.4 1.4v2.6c0 3.4-2.5 6.1-6 6.1h-2.8c-3.1 0-5.4-2.1-5.8-5.1l-.4-3.1a1.5 1.5 0 0 1 1.5-1.7h1.1z"
        fill="#fbbf24"
        stroke="#b45309"
        strokeWidth="1"
        strokeLinejoin="round"
      />
      <path
        d="M7.5 12.2h8.2"
        stroke="#b45309"
        strokeWidth="1.2"
        strokeLinecap="round"
      />
    </svg>
  );
}

/** Icono vectorial de mano saludando para indicar el gesto "Adiós ×3". */
export function WaveGlyph({ className = "" }: { className?: string }) {
  return (
    <svg viewBox="0 0 24 24" className={className} aria-hidden="true">
      <path
        d="M12.2 4.5c.6 0 1.1.5 1.1 1.1v5.1h.8V6.3c0-.6.5-1.1 1.1-1.1s1.1.5 1.1 1.1v5h.8V8.1c0-.6.5-1.1 1.1-1.1s1.1.5 1.1 1.1v5.8c0 3.6-2.4 6.2-6 6.2-2.4 0-4.3-1.2-5.4-3.3l-1.6-3.1a1.1 1.1 0 0 1 1.9-1.1l1 1.5V7c0-.6.5-1.1 1.1-1.1s1.1.5 1.1 1.1v3.7h.7V5.6c0-.6.5-1.1 1.1-1.1z"
        fill="#fbbf24"
        stroke="#b45309"
        strokeWidth="0.9"
      />
      <path
        d="M4.2 5.8c-.9 1.2-1.2 2.7-.8 4.1M19.8 5.2c1.1 1 1.6 2.5 1.3 4"
        fill="none"
        stroke="#f59e0b"
        strokeWidth="1.6"
        strokeLinecap="round"
      />
    </svg>
  );
}

/** Icono vectorial de globo terráqueo para el botón de cambio de idioma. */
export function GlobeGlyph({ className = "" }: { className?: string }) {
  return (
    <svg viewBox="0 0 24 24" className={className} aria-hidden="true">
      <circle cx="12" cy="12" r="8.5" fill="#e0f2fe" stroke="#0284c7" strokeWidth="1.6" />
      <ellipse cx="12" cy="12" rx="3.8" ry="8.5" fill="none" stroke="#0284c7" strokeWidth="1.4" />
      <path d="M3.8 12h16.4M5.2 8.2h13.6M5.2 15.8h13.6" stroke="#0284c7" strokeWidth="1.3" />
    </svg>
  );
}
