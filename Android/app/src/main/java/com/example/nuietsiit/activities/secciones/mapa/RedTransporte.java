package com.example.nuietsiit.activities.secciones.mapa;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Red de transporte exterior: lugares (residencia, ETSIIT, Auxiliar), líneas de bus con sus
 * sentidos y paradas, y tramos a pie precalculados. Se carga de
 * assets/rutas_exteriores/red_transporte.json (generado fuera del repo).
 */
public final class RedTransporte {

    public static final String RUTA_ASSET = "rutas_exteriores/red_transporte.json";
    /** Distancia máxima para reconocer un tramo precalculado por sus extremos. */
    private static final double TOLERANCIA_TRAMO_M = 25;

    public final List<Lugar> lugares;
    public final List<Linea> lineas;
    public final List<RutaAPie.Camino> tramosAPie;
    private final List<Punto[]> extremosTramos;

    private RedTransporte(List<Lugar> lugares, List<Linea> lineas,
                          List<RutaAPie.Camino> tramosAPie, List<Punto[]> extremosTramos) {
        this.lugares = Collections.unmodifiableList(lugares);
        this.lineas = Collections.unmodifiableList(lineas);
        this.tramosAPie = Collections.unmodifiableList(tramosAPie);
        this.extremosTramos = extremosTramos;
    }

    // ---------- Modelo ----------

    public static final class Lugar {
        public final String id, nombre, corto, icono;
        public final Punto punto;
        public final boolean ejemplo, destacado;
        /** Contorno del recinto (vacío si no hay). */
        public final List<Punto> recinto;
        /** Contornos de los edificios (vacío si no hay). */
        public final List<List<Punto>> edificios;

        Lugar(String id, String nombre, String corto, String icono, Punto punto, boolean ejemplo,
              boolean destacado, List<Punto> recinto, List<List<Punto>> edificios) {
            this.id = id;
            this.nombre = nombre;
            this.corto = corto;
            this.icono = icono;
            this.punto = punto;
            this.ejemplo = ejemplo;
            this.destacado = destacado;
            this.recinto = recinto;
            this.edificios = edificios;
        }

        /** ¿El punto está dentro del recinto o de alguno de sus edificios? */
        public boolean contiene(Punto p) {
            if (!recinto.isEmpty() && p.dentroDe(recinto)) return true;
            for (List<Punto> edificio : edificios) {
                if (p.dentroDe(edificio)) return true;
            }
            return false;
        }
    }

    public static final class Linea {
        public final String id, nombre, color;
        public final List<Franja> laborable, finde;
        public final List<Sentido> sentidos;

        Linea(String id, String nombre, String color, List<Franja> laborable, List<Franja> finde,
              List<Sentido> sentidos) {
            this.id = id;
            this.nombre = nombre;
            this.color = color;
            this.laborable = laborable;
            this.finde = finde;
            this.sentidos = sentidos;
        }
    }

    /** Frecuencia de paso (min) para las salidas de cabecera en [desde, hasta). */
    public static final class Franja {
        public final LocalTime desde, hasta;
        public final int minutos;

        Franja(LocalTime desde, LocalTime hasta, int minutos) {
            this.desde = desde;
            this.hasta = hasta;
            this.minutos = minutos;
        }
    }

    public static final class Sentido {
        public final String id, nombre, hacia;
        public final LocalTime primera, ultima;
        public final List<Parada> paradas;
        public final List<Punto> forma;

        Sentido(String id, String nombre, String hacia, LocalTime primera, LocalTime ultima,
                List<Parada> paradas, List<Punto> forma) {
            this.id = id;
            this.nombre = nombre;
            this.hacia = hacia;
            this.primera = primera;
            this.ultima = ultima;
            this.paradas = paradas;
            this.forma = forma;
        }

        /** Trazado entre dos paradas del sentido (ambas incluidas). */
        public List<Punto> formaEntre(Parada subida, Parada bajada) {
            return forma.subList(subida.iforma, Math.min(bajada.iforma + 1, forma.size()));
        }
    }

    public static final class Parada {
        public final String id, nombre;
        public final Punto punto;
        /** Minutos desde la salida de cabecera. */
        public final double minuto;
        /** Índice del punto de la forma más cercano. */
        public final int iforma;

        Parada(String id, String nombre, Punto punto, double minuto, int iforma) {
            this.id = id;
            this.nombre = nombre;
            this.punto = punto;
            this.minuto = minuto;
            this.iforma = iforma;
        }
    }

    // ---------- Consultas ----------

    public Lugar lugar(String id) {
        for (Lugar l : lugares) {
            if (l.id.equals(id)) return l;
        }
        return null;
    }

    public Linea linea(String id) {
        for (Linea l : lineas) {
            if (l.id.equals(id)) return l;
        }
        return null;
    }

    /** Tramo a pie precalculado entre esos extremos, o null. */
    public RutaAPie.Camino tramoAPie(Punto desde, Punto hasta) {
        for (int i = 0; i < tramosAPie.size(); i++) {
            Punto[] ext = extremosTramos.get(i);
            if (ext[0].distanciaA(desde) <= TOLERANCIA_TRAMO_M && ext[1].distanciaA(hasta) <= TOLERANCIA_TRAMO_M) {
                return tramosAPie.get(i);
            }
        }
        return null;
    }

    // ---------- Carga ----------

    public static RedTransporte cargar(InputStream entrada) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int n;
        while ((n = entrada.read(buffer)) > 0) bytes.write(buffer, 0, n);
        return desdeJson(new String(bytes.toByteArray(), StandardCharsets.UTF_8));
    }

    @SuppressWarnings("unchecked")
    static RedTransporte desdeJson(String json) {
        Map<String, Object> raiz = (Map<String, Object>) JsonLigero.leer(json);

        List<Lugar> lugares = new ArrayList<>();
        for (Object o : (List<Object>) raiz.get("lugares")) {
            Map<String, Object> m = (Map<String, Object>) o;
            List<List<Punto>> edificios = new ArrayList<>();
            if (m.containsKey("edificios")) {
                for (Object e : (List<Object>) m.get("edificios")) edificios.add(puntos(e));
            }
            lugares.add(new Lugar(texto(m, "id"), texto(m, "nombre"), texto(m, "corto"), texto(m, "icono"),
                    new Punto(num(m, "lat"), num(m, "lon")),
                    Boolean.TRUE.equals(m.get("ejemplo")), Boolean.TRUE.equals(m.get("destacado")),
                    m.containsKey("recinto") ? puntos(m.get("recinto")) : Collections.emptyList(),
                    edificios));
        }

        List<Linea> lineas = new ArrayList<>();
        for (Object o : (List<Object>) raiz.get("lineas")) {
            Map<String, Object> m = (Map<String, Object>) o;
            Map<String, Object> frec = (Map<String, Object>) m.get("frecuencias");
            List<Sentido> sentidos = new ArrayList<>();
            for (Object os : (List<Object>) m.get("sentidos")) {
                Map<String, Object> s = (Map<String, Object>) os;
                List<Parada> paradas = new ArrayList<>();
                for (Object op : (List<Object>) s.get("paradas")) {
                    Map<String, Object> p = (Map<String, Object>) op;
                    paradas.add(new Parada(texto(p, "id"), texto(p, "nombre"),
                            new Punto(num(p, "lat"), num(p, "lon")), num(p, "minuto"), (int) num(p, "iforma")));
                }
                sentidos.add(new Sentido(texto(s, "id"), texto(s, "nombre"), texto(s, "hacia"),
                        LocalTime.parse(texto(s, "primera")), LocalTime.parse(texto(s, "ultima")),
                        Collections.unmodifiableList(paradas), Collections.unmodifiableList(puntos(s.get("forma")))));
            }
            lineas.add(new Linea(texto(m, "id"), texto(m, "nombre"), texto(m, "color"),
                    franjas(frec.get("laborable")), franjas(frec.get("finde")),
                    Collections.unmodifiableList(sentidos)));
        }

        List<RutaAPie.Camino> tramos = new ArrayList<>();
        List<Punto[]> extremos = new ArrayList<>();
        for (Object o : (List<Object>) raiz.get("tramosAPie")) {
            Map<String, Object> m = (Map<String, Object>) o;
            extremos.add(new Punto[]{punto(m.get("desde")), punto(m.get("hasta"))});
            tramos.add(new RutaAPie.Camino(num(m, "metros"), puntos(m.get("puntos")), true));
        }
        return new RedTransporte(lugares, lineas, tramos, extremos);
    }

    @SuppressWarnings("unchecked")
    private static List<Franja> franjas(Object o) {
        List<Franja> franjas = new ArrayList<>();
        for (Object of : (List<Object>) o) {
            List<Object> f = (List<Object>) of;
            franjas.add(new Franja(LocalTime.parse((String) f.get(0)), hora((String) f.get(1)),
                    ((Double) f.get(2)).intValue()));
        }
        return Collections.unmodifiableList(franjas);
    }

    /** "24:00" no lo admite LocalTime: se toma como fin del día. */
    private static LocalTime hora(String texto) {
        return texto.startsWith("24") ? LocalTime.MAX : LocalTime.parse(texto);
    }

    @SuppressWarnings("unchecked")
    private static Punto punto(Object o) {
        List<Object> par = (List<Object>) o;
        return new Punto((Double) par.get(0), (Double) par.get(1));
    }

    @SuppressWarnings("unchecked")
    private static List<Punto> puntos(Object o) {
        List<Punto> lista = new ArrayList<>();
        for (Object p : (List<Object>) o) lista.add(punto(p));
        return lista;
    }

    private static String texto(Map<String, Object> m, String clave) {
        Object v = m.get(clave);
        return v == null ? "" : (String) v;
    }

    private static double num(Map<String, Object> m, String clave) {
        return (Double) m.get(clave);
    }
}
