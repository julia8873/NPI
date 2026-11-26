import { useCallback, useEffect, useState } from "react";
import { useAccessibility } from "../context/AccessibilityContext";
import { useGestureTarget } from "../gestures/hooks";

/**
 * Genera una matriz QR determinista en SVG para el banner de noticias
 * sin necesidad de librerías externas.
 */
function QrPatternSvg({ seed }: { seed: string }) {
  const size = 21;
  const cells: Array<{ x: number; y: number }> = [];

  // Comprueba si la celda pertenece a los tres patrones de esquina del QR
  const isFinder = (r: number, c: number) => {
    const inTopLeft = r < 7 && c < 7;
    const inTopRight = r < 7 && c >= size - 7;
    const inBottomLeft = r >= size - 7 && c < 7;
    return inTopLeft || inTopRight || inBottomLeft;
  };

  let hash = 2166136261;
  for (let i = 0; i < seed.length; i++) {
    hash ^= seed.charCodeAt(i);
    hash = Math.imul(hash, 16777619);
  }

  for (let r = 0; r < size; r++) {
    for (let c = 0; c < size; c++) {
      if (isFinder(r, c)) {
        const lr = r >= size - 7 ? r - (size - 7) : r;
        const lc = c >= size - 7 ? c - (size - 7) : c;
        const border = lr === 0 || lr === 6 || lc === 0 || lc === 6;
        const core = lr >= 2 && lr <= 4 && lc >= 2 && lc <= 4;
        if (border || core) cells.push({ x: c, y: r });
      } else {
        const bit = ((hash >>> ((r * 7 + c) % 24)) ^ (r * 13 + c * 7)) & 1;
        if (bit === 1 && (r + c) % 3 !== 0) {
          cells.push({ x: c, y: r });
        }
      }
    }
  }

  return (
    <svg viewBox={`0 0 ${size} ${size}`} className="qr-svg" aria-hidden="true">
      {cells.map((cell) => (
        <rect
          key={`${cell.x}-${cell.y}`}
          x={cell.x}
          y={cell.y}
          width="0.92"
          height="0.92"
          rx="0.14"
          fill="#111827"
        />
      ))}
    </svg>
  );
}

/**
 * Banner superior de noticias destacadas con carrusel automático y código QR.
 * También es interactivo mediante desplazamiento de mano + gesto de cerrar puño.
 */
export function NewsBanner() {
  const { t } = useAccessibility();
  const slides = t.newsSlides;
  const [index, setIndex] = useState(0);

  // Avanza a la siguiente noticia del carrusel
  const nextSlide = useCallback(() => {
    setIndex((prev) => (prev + 1) % slides.length);
  }, [slides.length]);

  useEffect(() => {
    const id = window.setInterval(nextSlide, 7500);
    return () => window.clearInterval(id);
  }, [nextSlide]);

  const { ref: bannerRef, isHovered: bannerHovered } =
    useGestureTarget<HTMLDivElement>({
      id: "news-banner",
      onSelect: nextSlide,
    });

  const activeSlide = slides[index % slides.length];

  return (
    <section
      ref={bannerRef}
      className={`news-banner ${bannerHovered ? "gesture-hovered" : ""}`}
      onClick={nextSlide}
      aria-label={activeSlide.badge}
    >
      <div className="news-content">
        <span className="news-badge">{activeSlide.badge}</span>
        <h2 className="news-title">{activeSlide.title}</h2>
        <p className="news-subtitle">{activeSlide.subtitle}</p>

        <div className="news-dots" role="tablist" aria-label="Noticias">
          {slides.map((slide, i) => (
            <span
              key={slide.id}
              className={`news-dot ${i === index ? "active" : ""}`}
            />
          ))}
        </div>
      </div>

      <div className="news-qr-wrapper">
        <div className="news-qr-box">
          <QrPatternSvg seed={activeSlide.qrValue} />
        </div>
        <span className="news-qr-label">{activeSlide.qrLabel}</span>
      </div>
    </section>
  );
}
