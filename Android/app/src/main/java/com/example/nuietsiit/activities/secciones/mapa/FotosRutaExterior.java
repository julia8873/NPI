package com.example.nuietsiit.activities.secciones.mapa;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Puntos de foto de los tramos a pie (`assets/rutas_exteriores/fotos_ruta.json`, generado por
 * `osm/puntos_foto.py`): uno al salir, en cada giro, cada ~60 m y en la llegada. Java puro.
 */
public final class FotosRutaExterior {

    /** Ruta de la tabla dentro de assets. */
    public static final String ASSET = "rutas_exteriores/fotos_ruta.json";

    /**
     * Metros que hay que dejar atrás un punto para pasar a la foto del siguiente. Antes bastaba con estar
     * a 15 m: con puntos cerca (en la rotonda hay giros cada 20-50 m) la foto y su "gira a la derecha"
     * cambiaban antes de llegar al giro y la de la salida no se llegaba a ver (Miguel, probando la ruta).
     */
    static final double PASADO_M = 8;
    /** Margen para casar los extremos de un tramo del plan con los de la tabla. */
    private static final double TOLERANCIA_TRAMO_M = 25;

    public static final class PuntoFoto {
        /** `<tramo>_<n>`; la foto real es el drawable `ruta_ext_<id>`. */
        public final String id;
        public final Punto punto;
        /** Hacia dónde mira la foto (grados, 0 = norte): dirección de avance. */
        public final double rumbo;
        /** Giro en ese punto (grados, + = derecha, 0 = recto). */
        public final double giro;
        /** Metros desde el inicio del tramo. */
        public final double metros;
        public final String instruccion;

        PuntoFoto(String id, Punto punto, double rumbo, double giro, double metros, String instruccion) {
            this.id = id;
            this.punto = punto;
            this.rumbo = rumbo;
            this.giro = giro;
            this.metros = metros;
            this.instruccion = instruccion;
        }
    }

    public static final class TramoFotos {
        public final String id;
        /** En orden de marcha; el primero y el último están en los extremos de la geometría del tramo. */
        public final List<PuntoFoto> puntos;

        TramoFotos(String id, List<PuntoFoto> puntos) {
            this.id = id;
            this.puntos = Collections.unmodifiableList(puntos);
        }
    }

    public final List<TramoFotos> tramos;

    private FotosRutaExterior(List<TramoFotos> tramos) {
        this.tramos = Collections.unmodifiableList(tramos);
    }

    // ---------- Consultas ----------

    /** Fotos de ese tramo del plan en orden de marcha; vacía si no es a pie o no es un tramo conocido. */
    public List<PuntoFoto> delTramo(PlanRuta.Tramo tramo) {
        if (tramo == null || tramo.tipo != PlanRuta.TipoTramo.A_PIE || tramo.geometria.size() < 2) {
            return Collections.emptyList();
        }
        Punto ini = tramo.geometria.get(0), fin = tramo.geometria.get(tramo.geometria.size() - 1);
        for (TramoFotos t : tramos) {
            Punto desde = t.puntos.get(0).punto, hasta = t.puntos.get(t.puntos.size() - 1).punto;
            if (desde.distanciaA(ini) <= TOLERANCIA_TRAMO_M && hasta.distanciaA(fin) <= TOLERANCIA_TRAMO_M) {
                return t.puntos;
            }
        }
        return Collections.emptyList();
    }

    /** Tramo de la tabla al que pertenece ese id de punto; null si no es un punto del camino. */
    public TramoFotos tramoDe(String idPunto) {
        if (idPunto == null) return null;
        for (TramoFotos t : tramos) {
            for (PuntoFoto p : t.puntos) {
                if (p.id.equals(idPunto)) return t;
            }
        }
        return null;
    }

    /**
     * Índice del primer punto que aún no se ha dejado atrás (PASADO_M) sobre la línea que forman los puntos:
     * en la salida, la foto de la salida; en un giro, la foto de ese giro hasta haberlo hecho; entre dos
     * puntos, la del siguiente. Si ya se han pasado todos, el último; -1 si la lista está vacía.
     */
    public static int siguiente(List<PuntoFoto> lista, Punto posicion) {
        if (lista.isEmpty()) return -1;
        double avance = avanceSobreLinea(lista, posicion);
        for (int i = 0; i < lista.size(); i++) {
            if (lista.get(i).metros + PASADO_M > avance) return i;
        }
        return lista.size() - 1;
    }

    /** Metros recorridos: proyección de la posición sobre el segmento más cercano de la línea de puntos. */
    static double avanceSobreLinea(List<PuntoFoto> lista, Punto posicion) {
        if (lista.size() == 1) return 0;
        double mejorDist = Double.MAX_VALUE, mejorAvance = 0;
        double coslat = Math.cos(Math.toRadians(posicion.lat));
        for (int i = 0; i + 1 < lista.size(); i++) {
            PuntoFoto a = lista.get(i), b = lista.get(i + 1);
            // coordenadas locales en metros con origen en a
            double bx = (b.punto.lon - a.punto.lon) * coslat * 111320, by = (b.punto.lat - a.punto.lat) * 110540;
            double px = (posicion.lon - a.punto.lon) * coslat * 111320, py = (posicion.lat - a.punto.lat) * 110540;
            double largo2 = bx * bx + by * by;
            double t = largo2 == 0 ? 0 : Math.max(0, Math.min(1, (px * bx + py * by) / largo2));
            double dx = px - t * bx, dy = py - t * by;
            double dist = Math.sqrt(dx * dx + dy * dy);
            if (dist < mejorDist) {
                mejorDist = dist;
                mejorAvance = a.metros + t * (b.metros - a.metros);
            }
        }
        return mejorAvance;
    }

    // ---------- Carga ----------

    public static FotosRutaExterior cargar(InputStream entrada) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int n;
        while ((n = entrada.read(buffer)) > 0) bytes.write(buffer, 0, n);
        return desdeJson(new String(bytes.toByteArray(), StandardCharsets.UTF_8));
    }

    @SuppressWarnings("unchecked")
    static FotosRutaExterior desdeJson(String json) {
        Map<String, Object> raiz = (Map<String, Object>) JsonLigero.leer(json);
        List<TramoFotos> tramos = new ArrayList<>();
        for (Object o : (List<Object>) raiz.get("tramos")) {
            Map<String, Object> t = (Map<String, Object>) o;
            List<PuntoFoto> puntos = new ArrayList<>();
            for (Object op : (List<Object>) t.get("puntos")) {
                Map<String, Object> p = (Map<String, Object>) op;
                puntos.add(new PuntoFoto((String) p.get("id"), new Punto(num(p, "lat"), num(p, "lon")),
                        num(p, "rumbo"), num(p, "giro"), num(p, "metros"), (String) p.get("instruccion")));
            }
            if (!puntos.isEmpty()) tramos.add(new TramoFotos((String) t.get("id"), puntos));
        }
        return new FotosRutaExterior(tramos);
    }

    private static double num(Map<String, Object> m, String clave) {
        return ((Number) m.get(clave)).doubleValue();
    }
}
