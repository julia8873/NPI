import { useEffect, useRef, useState } from 'react';
import './App.css';

// Tipos de datos que envía el WebSocket de Leap Motion (v6.json)
interface LeapHand {
  id: number;
  type: 'left' | 'right';
  palmPosition: [number, number, number]; // [X, Y, Z] en mm
  palmVelocity: [number, number, number]; // [vX, vY, vZ] en mm/s
  pinchStrength: number; // 0 (abierta) a 1 (pinza cerrada)
  grabStrength: number;  // 0 (abierta) a 1 (puño cerrado)
}

interface LeapFrame {
  hands: LeapHand[];
}

export default function App() {
  // Estados de interfaz (solo se actualizan en eventos discretos)
  const [conectado, setConectado] = useState(false);
  const [manosDetectadas, setManosDetectadas] = useState(0);
  const [telemetria, setTelemetria] = useState({ x: 0, y: 0, z: 0, pinch: 0, grab: 0 });
  const [sobreBoton, setSobreBoton] = useState(false);
  const [progresoDwell, setProgresoDwell] = useState(0); // 0 a 100%
  const [contadorClicks, setContadorClicks] = useState(0);
  const [gestoCustomActivo, setGestoCustomActivo] = useState(false);

  // Referencias DOM para mover el cursor a 60+ FPS sin re-renderizar React
  const cursorRef = useRef<HTMLDivElement | null>(null);
  const botonPruebaRef = useRef<HTMLButtonElement | null>(null);

  // Referencias internas para filtros y máquinas de estado de gestos
  const posSuavizada = useRef({ x: window.innerWidth / 2, y: window.innerHeight / 2 });
  const inicioHoverMs = useRef<number | null>(null);
  const pinzaBloqueada = useRef(false);
  const cooldownGestoCustom = useRef(0);
  const ultimoUpdateTelemetria = useRef(0);

  useEffect(() => {
    let ws: WebSocket | null = null;
    let timerReconexion: number | undefined;

    const conectarLeap = () => {
      ws = new WebSocket('ws://127.0.0.1:6437/v6.json');

      ws.onopen = () => {
        setConectado(true);
        // Permite recibir datos incluso si la ventana pierde el foco momentáneamente
        ws?.send(JSON.stringify({ background: true }));
      };

      ws.onclose = () => {
        setConectado(false);
        setManosDetectadas(0);
        // Reintentar conexión cada 2 segundos si el servicio no estaba listo
        timerReconexion = window.setTimeout(conectarLeap, 2000);
      };

      ws.onerror = () => {
        ws?.close();
      };

      ws.onmessage = (evento) => {
        const frame: LeapFrame = JSON.parse(evento.data);
        const numManos = frame.hands ? frame.hands.length : 0;
        setManosDetectadas(numManos);

        if (numManos === 0) {
          inicioHoverMs.current = null;
          setProgresoDwell(0);
          setSobreBoton(false);
          return;
        }

        // Usamos la mano principal (la primera detectada)
        const mano = frame.hands[0];
        const [rawX, rawY, rawZ] = mano.palmPosition;
        const [, , velZ] = mano.palmVelocity;

        // 1. MAPEO 3D (mm) -> 2D PANTALLA (px)
        // Rango útil aproximado del sensor: X en [-180, 180] mm, Y en [110, 400] mm
        const normX = Math.min(1, Math.max(0, (rawX + 180) / 360));
        const normY = Math.min(1, Math.max(0, (rawY - 110) / 290));

        const targetX = normX * window.innerWidth;
        // En Leap, Y crece hacia arriba; en pantalla, Y crece hacia abajo (invertimos)
        const targetY = (1 - normY) * window.innerHeight;

        // 2. FILTRO PASO BAJO EXPONENCIAL (anti-temblor)
        const alpha = 0.35;
        posSuavizada.current.x = alpha * targetX + (1 - alpha) * posSuavizada.current.x;
        posSuavizada.current.y = alpha * targetY + (1 - alpha) * posSuavizada.current.y;

        const cx = posSuavizada.current.x;
        const cy = posSuavizada.current.y;

        // Mover cursor directamente en el DOM (máximo rendimiento)
        if (cursorRef.current) {
          cursorRef.current.style.transform = `translate3d(${cx}px, ${cy}px, 0)`;
        }

        // 3. DETECCIÓN DE COLISIÓN CON EL BOTÓN DE PRUEBA (Hover + Dwell + Pinza)
        if (botonPruebaRef.current) {
          const rect = botonPruebaRef.current.getBoundingClientRect();
          const estaEncima =
            cx >= rect.left && cx <= rect.right && cy >= rect.top && cy <= rect.bottom;

          setSobreBoton(estaEncima);

          if (estaEncima) {
            const ahora = performance.now();
            if (inicioHoverMs.current === null) {
              inicioHoverMs.current = ahora;
            }

            // Selección A: Permanencia (Dwell) de 800 ms
            const transcurrido = ahora - inicioHoverMs.current;
            const porcentaje = Math.min(100, (transcurrido / 800) * 100);
            setProgresoDwell(porcentaje);

            if (transcurrido >= 800) {
              setContadorClicks((c) => c + 1);
              inicioHoverMs.current = ahora + 600; // Cooldown para evitar doble disparo
              setProgresoDwell(0);
            }

            // Selección B: Gesto de Pinza (índice + pulgar)
            if (mano.pinchStrength > 0.82 && !pinzaBloqueada.current) {
              pinzaBloqueada.current = true;
              setContadorClicks((c) => c + 1);
              inicioHoverMs.current = ahora + 600;
              setProgresoDwell(0);
            } else if (mano.pinchStrength < 0.5) {
              pinzaBloqueada.current = false;
            }
          } else {
            inicioHoverMs.current = null;
            setProgresoDwell(0);
          }
        }

        // 4. GESTO PERSONALIZADO: "Empujar hacia la pantalla + Cerrar puño" (Lanzar QR)
        const ahora = performance.now();
        if (
          velZ < -500 &&
          rawZ < -40 &&
          mano.grabStrength > 0.8 &&
          ahora > cooldownGestoCustom.current
        ) {
          cooldownGestoCustom.current = ahora + 1500;
          setGestoCustomActivo(true);
          setTimeout(() => setGestoCustomActivo(false), 2000);
        }

        // 5. ACTUALIZAR TELEMETRÍA EN PANTALLA (cada 100 ms para no saturar)
        if (ahora - ultimoUpdateTelemetria.current > 100) {
          ultimoUpdateTelemetria.current = ahora;
          setTelemetria({
            x: Math.round(rawX),
            y: Math.round(rawY),
            z: Math.round(rawZ),
            pinch: Number(mano.pinchStrength.toFixed(2)),
            grab: Number(mano.grabStrength.toFixed(2)),
          });
        }
      };
    };

    conectarLeap();

    return () => {
      if (timerReconexion) clearTimeout(timerReconexion);
      ws?.close();
    };
  }, []);

  return (
    <div className="totem-container">
      {/* Cabecera de estado del hardware */}
      <header className="status-bar">
        <span className={`badge ${conectado ? 'ok' : 'error'}`}>
          {conectado ? '● Leap Motion Conectado' : '○ Esperando servicio Leap (puerto 6437)...'}
        </span>
        <span className="badge info">Manos detectadas: {manosDetectadas}</span>
      </header>

      {/* Panel de telemetría para calibración */}
      <section className="telemetry-card">
        <h2>Prueba de Integración Gestual - Tótem ETSIIT</h2>
        <p>
          Coordenadas 3D (mm): <strong>X: {telemetria.x}</strong> |{' '}
          <strong>Y: {telemetria.y}</strong> | <strong>Z: {telemetria.z}</strong>
        </p>
        <p>
          Pinza: <strong>{telemetria.pinch}</strong> | Puño: <strong>{telemetria.grab}</strong>
        </p>
      </section>

      {/* Zona interactiva sin ratón ni pantalla táctil */}
      <main className="interactive-zone">
        <button
          ref={botonPruebaRef}
          className={`totem-button ${sobreBoton ? 'hovered' : ''}`}
          onClick={() => setContadorClicks((c) => c + 1)}
        >
          <span>Apunta aquí (800 ms) o haz Pinza</span>
          <strong>Activaciones: {contadorClicks}</strong>
        </button>

        <div className={`custom-gesture-banner ${gestoCustomActivo ? 'active' : ''}`}>
          {gestoCustomActivo
            ? '¡Gesto personalizado detectado! (Empuje -Z + Puño -> Mostrar QR)'
            : 'Prueba el gesto personalizado: empuja la mano hacia la pantalla y cierra el puño'}
        </div>
      </main>

      {/* Cursor virtual controlado por la mano */}
      <div
        ref={cursorRef}
        className="leap-cursor"
        style={{ opacity: manosDetectadas > 0 ? 1 : 0 }}
      >
        <div
          className="dwell-ring"
          style={{
            transform: `scale(${0.4 + (progresoDwell / 100) * 0.8})`,
            opacity: progresoDwell > 0 ? 0.9 : 0.3,
          }}
        />
      </div>
    </div>
  );
}