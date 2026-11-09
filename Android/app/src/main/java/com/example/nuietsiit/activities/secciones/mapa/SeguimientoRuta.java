package com.example.nuietsiit.activities.secciones.mapa;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Sigue un {@link PlanRuta} con la posición, la velocidad y la hora: en qué etapa está el usuario,
 * cuenta atrás, llegada recalculada, rumbo al siguiente objetivo y avisos (cada uno una sola vez).
 * Java puro: la guía de Android (F4) solo le pasa el GPS y pinta el {@link Estado}.
 */
public final class SeguimientoRuta {

    /** Por encima de 12 km/h y sobre la línea = en el bus. */
    public static final double VELOCIDAD_BUS_MS = 12 / 3.6;
    static final double CERCA_ORIGEN_M = 50;
    static final double EN_PARADA_M = 40;
    static final double SOBRE_LINEA_M = 50;
    static final double BAJATE_M = 150;
    static final double LLEGADO_M = 25;
    /** Margen para dar el bus por perdido (retrasos, carrera final...). */
    static final Duration MARGEN_BUS = Duration.ofMinutes(1);

    public enum Etapa {
        POR_SALIR("Prepárate para salir"),
        A_LA_PARADA("Andando a la parada"),
        ESPERANDO("Esperando el bus"),
        EN_BUS("En el bus"),
        BAJATE("Bájate"),
        AL_DESTINO("Andando al destino"),
        LLEGADO("Has llegado");

        public final String texto;

        Etapa(String texto) {
            this.texto = texto;
        }
    }

    public static final class Evento {
        public enum Tipo { SAL_YA, BUS_EN_1_MIN, BUS_PERDIDO, SIGUIENTE_ES_TU_PARADA, BAJATE_AHORA, LLEGADO }

        public final Tipo tipo;
        public final String texto;

        Evento(Tipo tipo, String texto) {
            this.tipo = tipo;
            this.texto = texto;
        }

        @Override
        public String toString() {
            return tipo + ": " + texto;
        }
    }

    /** Foto de un momento de la ruta. */
    public static final class Estado {
        public final Etapa etapa;
        /** Tramo del plan en curso (para la foto: {@code tramo.idFoto}). */
        public final PlanRuta.Tramo tramo;
        public final String instruccion;
        /** Parada, bajada o entrada del destino; null al llegar. */
        public final Punto objetivo;
        public final double metrosAlObjetivo;
        /** Rumbo al objetivo (0 = norte), NaN al llegar. Para la brújula: rumbo − orientación del móvil. */
        public final double rumbo;
        /** Hora del siguiente hito (salir, paso del bus, bajada o llegada); null al llegar. */
        public final LocalDateTime hito;
        /** "Sal en 3 min", "Tu bus pasa en 4 min", "Bajas en 5 min", "Quedan 9 min". */
        public final String cuentaAtras;
        public final long segundosCuentaAtras;
        public final LocalDateTime llegada;
        /** Bus esperado o en el que va (null si el plan es a pie o ya no hay buses). */
        public final HorarioBus.Paso bus;
        /** Paradas hasta la bajada (incluida), solo en el bus. */
        public final int paradasRestantes;
        public final List<Evento> eventos;

        Estado(Etapa etapa, PlanRuta.Tramo tramo, String instruccion, Punto objetivo, Punto posicion,
               LocalDateTime hito, String cuentaAtras, LocalDateTime ahora, LocalDateTime llegada,
               HorarioBus.Paso bus, int paradasRestantes, List<Evento> eventos) {
            this.etapa = etapa;
            this.tramo = tramo;
            this.instruccion = instruccion;
            this.objetivo = objetivo;
            this.metrosAlObjetivo = objetivo == null ? 0 : posicion.distanciaA(objetivo);
            this.rumbo = objetivo == null ? Double.NaN : posicion.rumboA(objetivo);
            this.hito = hito;
            this.cuentaAtras = cuentaAtras;
            this.segundosCuentaAtras = hito == null ? 0 : Math.max(0, Duration.between(ahora, hito).getSeconds());
            this.llegada = llegada;
            this.bus = bus;
            this.paradasRestantes = paradasRestantes;
            this.eventos = Collections.unmodifiableList(eventos);
        }

        /** Minutos que quedan hasta llegar (redondeado hacia arriba). */
        public long minutosRestantes(LocalDateTime ahora) {
            return PlanRuta.minutosArriba(Duration.between(ahora, llegada));
        }
    }

    private final PlanRuta plan;
    private final PlanRuta.Tramo aLaParada, espera, enBus, alDestino;
    private final HorarioBus horario;
    private final Punto entrada;

    private Etapa etapa;
    private HorarioBus.Paso paso;
    private final Set<Evento.Tipo> avisados = EnumSet.noneOf(Evento.Tipo.class);
    private Punto anterior;
    private LocalDateTime horaAnterior, horaLlegado;

    public SeguimientoRuta(PlanRuta plan) {
        if (plan.tipo == PlanRuta.Tipo.LEJOS || plan.tramos.isEmpty()) {
            throw new IllegalArgumentException("Un plan LEJOS no se sigue: se abre Google Maps");
        }
        this.plan = plan;
        enBus = plan.tramoBus();
        PlanRuta.Tramo antes = null, espera = null, despues = null;
        for (PlanRuta.Tramo t : plan.tramos) {
            if (t.tipo == PlanRuta.TipoTramo.ESPERA) espera = t;
            else if (t.tipo == PlanRuta.TipoTramo.A_PIE) {
                if (enBus == null || t.fin.isAfter(enBus.inicio)) despues = t;
                else antes = t;
            }
        }
        this.aLaParada = antes;
        this.espera = espera;
        this.alDestino = despues;
        if (enBus != null) {
            horario = new HorarioBus(enBus.linea);
            paso = horario.proximoPaso(enBus.sentido, enBus.subida, enBus.inicio);
        } else {
            horario = null;
        }
        PlanRuta.Tramo ultimo = plan.tramos.get(plan.tramos.size() - 1);
        entrada = ultimo.tipo == PlanRuta.TipoTramo.A_PIE && !ultimo.geometria.isEmpty()
                ? ultimo.geometria.get(ultimo.geometria.size() - 1) : plan.destino.punto;
        etapa = Etapa.POR_SALIR;
    }

    public PlanRuta plan() {
        return plan;
    }

    public Etapa etapa() {
        return etapa;
    }

    /**
     * Avanza con una lectura del GPS. Las etapas solo van hacia delante.
     *
     * @param velocidad m/s del GPS; si es negativa o NaN se estima con la lectura anterior.
     */
    public Estado actualizar(Punto posicion, double velocidad, LocalDateTime ahora) {
        if (!(velocidad >= 0)) velocidad = velocidadEstimada(posicion, ahora);
        anterior = posicion;
        horaAnterior = ahora;
        List<Evento> eventos = new ArrayList<>();
        boolean rapido = velocidad > VELOCIDAD_BUS_MS;

        // ---------- etapa ----------
        Proyeccion enLinea = enBus == null ? null : proyectarEnLinea(posicion);
        if (etapa != Etapa.LLEGADO && !rapido
                && (plan.destino.contiene(posicion) || posicion.distanciaA(entrada) <= LLEGADO_M)) {
            avanzar(Etapa.LLEGADO);
            horaLlegado = ahora;
        } else if (enBus == null) {
            if (etapa == Etapa.POR_SALIR && (!ahora.isBefore(plan.salida) || lejosDelOrigen(posicion))) {
                avanzar(Etapa.AL_DESTINO);
            }
        } else {
            if (etapa == Etapa.POR_SALIR && (!ahora.isBefore(plan.salida) || lejosDelOrigen(posicion))) {
                avanzar(Etapa.A_LA_PARADA);
            }
            if (etapa == Etapa.A_LA_PARADA && posicion.distanciaA(enBus.subida.punto) <= EN_PARADA_M) {
                avanzar(Etapa.ESPERANDO);
            }
            if (antesDe(Etapa.EN_BUS) && rapido && enLinea.distancia <= SOBRE_LINEA_M) {
                avanzar(Etapa.EN_BUS);
            }
            if (etapa == Etapa.EN_BUS && (posicion.distanciaA(enBus.bajada.punto) <= BAJATE_M
                    || enLinea.indice >= enBus.bajada.iforma)) {
                avanzar(Etapa.BAJATE);
            }
            if (etapa == Etapa.BAJATE && !rapido && posicion.distanciaA(enBus.bajada.punto) > EN_PARADA_M) {
                avanzar(Etapa.AL_DESTINO);
            }
        }

        // ---------- bus esperado (¿se pierde?) ----------
        if (antesDe(Etapa.EN_BUS) && paso != null) {
            LocalDateTime enLaParada = etapa == Etapa.ESPERANDO ? ahora : ahora.plus(aPie(aLaParada, posicion));
            if (enLaParada.isAfter(paso.hora.plus(MARGEN_BUS))) {
                HorarioBus.Paso perdido = paso;
                paso = horario.proximoPaso(enBus.sentido, enBus.subida, enLaParada);
                avisados.remove(Evento.Tipo.BUS_EN_1_MIN);
                eventos.add(new Evento(Evento.Tipo.BUS_PERDIDO, paso == null
                        ? "Has perdido el bus de las " + PlanRuta.hora(perdido.hora) + " y no hay más hoy"
                        : "Has perdido el bus de las " + PlanRuta.hora(perdido.hora) + ": el siguiente pasa a las "
                        + PlanRuta.hora(paso.hora)));
            }
        }

        // ---------- avisos ----------
        if (enBus != null && antesDe(Etapa.ESPERANDO) && !ahora.isBefore(plan.salida) && !lejosDelOrigen(posicion)) {
            avisar(eventos, Evento.Tipo.SAL_YA, "Sal ya hacia " + enBus.subida.nombre);
        }
        if (antesDe(Etapa.EN_BUS) && paso != null && etapa != Etapa.POR_SALIR
                && !ahora.isBefore(paso.hora.minusMinutes(1))) {
            avisar(eventos, Evento.Tipo.BUS_EN_1_MIN, "El " + enBus.linea.id + " llega en 1 min a " + enBus.subida.nombre);
        }
        int paradas = enBus == null ? 0 : paradasHastaBajada(enLinea);
        if (etapa == Etapa.EN_BUS && paradas <= 1) {
            avisar(eventos, Evento.Tipo.SIGUIENTE_ES_TU_PARADA, "La siguiente es tu parada: " + enBus.bajada.nombre);
        }
        if (etapa == Etapa.BAJATE) {
            avisar(eventos, Evento.Tipo.BAJATE_AHORA, "Bájate ahora en " + enBus.bajada.nombre);
        }
        if (etapa == Etapa.LLEGADO) {
            avisar(eventos, Evento.Tipo.LLEGADO, "Has llegado a " + plan.destino.corto);
        }

        return estado(posicion, ahora, enLinea, paradas, eventos);
    }

    private Estado estado(Punto pos, LocalDateTime ahora, Proyeccion enLinea, int paradas, List<Evento> eventos) {
        Duration finalAPie = alDestino == null ? Duration.ZERO : Duration.between(alDestino.inicio, alDestino.fin);
        LocalDateTime enBajada = null;
        if (paso != null && antesDe(Etapa.EN_BUS)) enBajada = paso.horaEn(enBus.bajada);
        else if (etapa == Etapa.EN_BUS || etapa == Etapa.BAJATE) enBajada = ahora.plus(enRuta(enLinea));

        switch (etapa) {
            case POR_SALIR:
            case A_LA_PARADA: {
                PlanRuta.Tramo tramo = aLaParada != null ? aLaParada : plan.tramos.get(0);
                Punto objetivo = enBus != null ? enBus.subida.punto : entrada;
                double metros = enBus != null ? aPieMetros(aLaParada, pos) : aPieMetros(alDestino, pos);
                LocalDateTime llegada = enBajada != null ? enBajada.plus(finalAPie)
                        : ahora.plus(aPie(alDestino, pos));
                String destino = enBus != null ? enBus.subida.nombre : plan.destino.corto;
                if (etapa == Etapa.POR_SALIR) {
                    return new Estado(etapa, tramo, "Sal a las " + PlanRuta.hora(plan.salida) + " hacia " + destino,
                            objetivo, pos, plan.salida, "Sal en " + minutos(ahora, plan.salida), ahora, llegada,
                            paso, 0, eventos);
                }
                LocalDateTime hito = paso != null ? paso.hora : llegada;
                return new Estado(etapa, tramo, "Camina hasta " + destino + " (" + metrosTexto(metros) + ")",
                        objetivo, pos, hito, busPasa(ahora), ahora, llegada, paso, 0, eventos);
            }
            case ESPERANDO: {
                LocalDateTime llegada = enBajada != null ? enBajada.plus(finalAPie) : ahora.plus(aPie(alDestino, pos));
                return new Estado(etapa, espera != null ? espera : enBus, "Espera el " + enBus.linea.id + " hacia "
                        + enBus.sentido.hacia, enBus.subida.punto, pos, paso != null ? paso.hora : null,
                        busPasa(ahora), ahora, llegada, paso, 0, eventos);
            }
            case EN_BUS:
            case BAJATE: {
                String instruccion = etapa == Etapa.BAJATE ? "Bájate en " + enBus.bajada.nombre
                        : "Bájate en " + enBus.bajada.nombre + " (" + paradas + (paradas == 1 ? " parada)" : " paradas)");
                return new Estado(etapa, enBus, instruccion, enBus.bajada.punto, pos, enBajada,
                        "Bajas en " + minutos(ahora, enBajada), ahora, enBajada.plus(finalAPie), paso, paradas, eventos);
            }
            case AL_DESTINO: {
                LocalDateTime llegada = ahora.plus(aPie(alDestino, pos));
                double metros = aPieMetros(alDestino, pos);
                return new Estado(etapa, alDestino != null ? alDestino : plan.tramos.get(plan.tramos.size() - 1),
                        "Camina hasta " + plan.destino.corto + " (" + metrosTexto(metros) + ")", entrada, pos,
                        llegada, "Quedan " + minutos(ahora, llegada), ahora, llegada, paso, 0, eventos);
            }
            default:
                return new Estado(etapa, plan.tramos.get(plan.tramos.size() - 1), "Has llegado a " + plan.destino.nombre,
                        null, pos, null, "Has llegado", ahora, horaLlegado, paso, 0, eventos);
        }
    }

    // ---------- etapas y avisos ----------

    private void avanzar(Etapa nueva) {
        if (nueva.ordinal() > etapa.ordinal()) etapa = nueva;
    }

    private boolean antesDe(Etapa e) {
        return etapa.ordinal() < e.ordinal();
    }

    private void avisar(List<Evento> eventos, Evento.Tipo tipo, String texto) {
        if (avisados.add(tipo)) eventos.add(new Evento(tipo, texto));
    }

    private boolean lejosDelOrigen(Punto p) {
        return p.distanciaA(plan.origen) > CERCA_ORIGEN_M;
    }

    private double velocidadEstimada(Punto p, LocalDateTime ahora) {
        if (anterior == null) return 0;
        double s = Duration.between(horaAnterior, ahora).toMillis() / 1000.0;
        return s > 0 ? anterior.distanciaA(p) / s : 0;
    }

    private String busPasa(LocalDateTime ahora) {
        if (paso == null) return "No quedan buses hoy";
        return ahora.isBefore(paso.hora) ? "Tu bus pasa en " + minutos(ahora, paso.hora) : "Tu bus está llegando";
    }

    private static String minutos(LocalDateTime ahora, LocalDateTime hito) {
        long min = PlanRuta.minutosArriba(Duration.between(ahora, hito));
        return min == 0 ? "ya" : min + " min";
    }

    private static String metrosTexto(double metros) {
        return metros >= 1000 ? String.format(Locale.getDefault(), "%.1f km", metros / 1000)
                : Math.round(metros / 10) * 10 + " m";
    }

    // ---------- tramos a pie ----------

    /** Metros que quedan del tramo desde la posición (sobre su geometría, con el factor del tramo). */
    private static double aPieMetros(PlanRuta.Tramo tramo, Punto p) {
        if (tramo == null || tramo.geometria.isEmpty()) return 0;
        List<Punto> g = tramo.geometria;
        if (g.size() == 1) return p.distanciaA(g.get(0));
        double longitud = longitud(g, 0);
        double factor = longitud > 0 ? tramo.metros / longitud : 1;
        Proyeccion pr = proyectar(g, p, 0, g.size() - 1);
        double resto = (1 - pr.fraccion) * g.get(pr.segmento).distanciaA(g.get(pr.segmento + 1))
                + longitud(g, pr.segmento + 1);
        return pr.distancia + resto * factor;
    }

    private static Duration aPie(PlanRuta.Tramo tramo, Punto p) {
        return Duration.ofSeconds(Math.round(aPieMetros(tramo, p) / RutaAPie.METROS_POR_MINUTO * 60));
    }

    private static double longitud(List<Punto> g, int desde) {
        double m = 0;
        for (int i = desde; i + 1 < g.size(); i++) m += g.get(i).distanciaA(g.get(i + 1));
        return m;
    }

    // ---------- en el bus ----------

    /** Posición sobre el trazado del sentido, de la subida a un poco más allá de la bajada. */
    private Proyeccion proyectarEnLinea(Punto p) {
        List<Punto> forma = enBus.sentido.forma;
        int hasta = Math.min(forma.size() - 1, enBus.bajada.iforma + 20);
        return proyectar(forma, p, enBus.subida.iforma, Math.max(hasta, enBus.subida.iforma + 1));
    }

    private int paradasHastaBajada(Proyeccion enLinea) {
        int n = 0;
        for (RedTransporte.Parada parada : enBus.sentido.paradas) {
            if (parada.iforma > enLinea.indice && parada.iforma <= enBus.bajada.iforma) n++;
            if (parada == enBus.bajada) break;
        }
        return n;
    }

    /** Tiempo de bus que falta hasta la bajada, interpolando el minuto de paso entre paradas. */
    private Duration enRuta(Proyeccion enLinea) {
        double indice = enLinea.indice;
        double minuto = enBus.subida.minuto;
        RedTransporte.Parada previa = null;
        for (RedTransporte.Parada parada : enBus.sentido.paradas) {
            if (previa != null && previa.iforma <= indice && indice <= parada.iforma) {
                double tramo = parada.iforma - previa.iforma;
                minuto = previa.minuto + (tramo > 0 ? (parada.minuto - previa.minuto) * (indice - previa.iforma) / tramo : 0);
                break;
            }
            if (parada.iforma <= indice) minuto = parada.minuto;
            previa = parada;
        }
        double faltan = Math.max(0, enBus.bajada.minuto - minuto);
        return Duration.ofSeconds(Math.round(faltan * 60));
    }

    // ---------- geometría ----------

    private static final class Proyeccion {
        final int segmento;
        final double fraccion, distancia;
        /** segmento + fracción: posición continua sobre la polilínea. */
        final double indice;

        Proyeccion(int segmento, double fraccion, double distancia) {
            this.segmento = segmento;
            this.fraccion = fraccion;
            this.distancia = distancia;
            this.indice = segmento + fraccion;
        }
    }

    /** Punto más cercano de la polilínea entre los vértices desde..hasta (en metros, proyección local plana). */
    private static Proyeccion proyectar(List<Punto> linea, Punto p, int desde, int hasta) {
        double cosLat = Math.cos(Math.toRadians(p.lat));
        Proyeccion mejor = null;
        for (int i = desde; i < hasta; i++) {
            Punto a = linea.get(i), b = linea.get(i + 1);
            double ax = (a.lon - p.lon) * cosLat, ay = a.lat - p.lat;
            double bx = (b.lon - p.lon) * cosLat, by = b.lat - p.lat;
            double dx = bx - ax, dy = by - ay, l2 = dx * dx + dy * dy;
            double t = l2 == 0 ? 0 : Math.max(0, Math.min(1, -(ax * dx + ay * dy) / l2));
            Punto q = new Punto(a.lat + t * (b.lat - a.lat), a.lon + t * (b.lon - a.lon));
            double d = p.distanciaA(q);
            if (mejor == null || d < mejor.distancia) mejor = new Proyeccion(i, t, d);
        }
        return mejor;
    }
}
