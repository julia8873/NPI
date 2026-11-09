package com.example.nuietsiit.activities.secciones.mapa;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Resultado del planificador: a pie, a pie + bus + a pie, o "lejos" (usar Google Maps).
 * Los tramos van en orden y encadenados en el tiempo.
 */
public final class PlanRuta {

    public enum Tipo { A_PIE, BUS, LEJOS }

    public enum TipoTramo { A_PIE, ESPERA, BUS }

    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("H:mm");

    public static final class Tramo {
        public final TipoTramo tipo;
        public final List<Punto> geometria;
        public final LocalDateTime inicio, fin;
        public final String instruccion;
        /** Id del lugar o parada (o "bus_<línea>") para buscar la foto del tramo. */
        public final String idFoto;
        public final double metros;
        // solo en ESPERA y BUS (null en A_PIE)
        public final RedTransporte.Linea linea;
        public final RedTransporte.Sentido sentido;
        public final RedTransporte.Parada subida, bajada;

        Tramo(TipoTramo tipo, List<Punto> geometria, LocalDateTime inicio, LocalDateTime fin, String instruccion,
              String idFoto, double metros, RedTransporte.Linea linea, RedTransporte.Sentido sentido,
              RedTransporte.Parada subida, RedTransporte.Parada bajada) {
            this.tipo = tipo;
            this.geometria = Collections.unmodifiableList(geometria);
            this.inicio = inicio;
            this.fin = fin;
            this.instruccion = instruccion;
            this.idFoto = idFoto;
            this.metros = metros;
            this.linea = linea;
            this.sentido = sentido;
            this.subida = subida;
            this.bajada = bajada;
        }

        /** Duración redondeada hacia arriba (mínimo 1 min si dura algo). */
        public long minutos() {
            return minutosArriba(Duration.between(inicio, fin));
        }
    }

    public final Tipo tipo;
    public final Punto origen;
    public final RedTransporte.Lugar destino;
    public final List<Tramo> tramos;
    /** Hora a la que hay que salir del origen. */
    public final LocalDateTime salida;
    public final LocalDateTime llegada;
    public final double metrosAPie;
    public final double metrosRecta;

    PlanRuta(Tipo tipo, Punto origen, RedTransporte.Lugar destino, List<Tramo> tramos,
             LocalDateTime salida, LocalDateTime llegada) {
        this.tipo = tipo;
        this.origen = origen;
        this.destino = destino;
        this.tramos = Collections.unmodifiableList(tramos);
        this.salida = salida;
        this.llegada = llegada;
        double aPie = 0;
        for (Tramo t : tramos) {
            if (t.tipo == TipoTramo.A_PIE) aPie += t.metros;
        }
        this.metrosAPie = aPie;
        this.metrosRecta = origen.distanciaA(destino.punto);
    }

    /** El tramo en bus, o null si el plan no lleva bus. */
    public Tramo tramoBus() {
        for (Tramo t : tramos) {
            if (t.tipo == TipoTramo.BUS) return t;
        }
        return null;
    }

    public long minutosTotales() {
        return minutosArriba(Duration.between(salida, llegada));
    }

    /** "Sal a las 8:33 · 3 min a pie · 🚌 9 de las 8:37 en … · 6 min · bájate en … · 7 min a pie · llegas 8:50". */
    public String resumen() {
        if (tipo == Tipo.LEJOS) {
            return String.format(Locale.getDefault(), "%s está a %.1f km: mejor con Google Maps",
                    destino.corto, metrosRecta / 1000);
        }
        StringBuilder sb = new StringBuilder("Sal a las ").append(hora(salida));
        for (Tramo t : tramos) {
            switch (t.tipo) {
                case A_PIE:
                    sb.append(" · ").append(t.minutos()).append(" min a pie");
                    break;
                case BUS:
                    sb.append(" · 🚌 ").append(t.linea.id).append(" de las ").append(hora(t.inicio))
                            .append(" en ").append(t.subida.nombre)
                            .append(" · ").append(t.minutos()).append(" min")
                            .append(" · bájate en ").append(t.bajada.nombre);
                    break;
                default:
                    break;
            }
        }
        return sb.append(" · llegas ").append(horaArriba(llegada)).toString();
    }

    /** Hora truncada al minuto ("8:39"). */
    public static String hora(LocalDateTime t) {
        return t.format(HORA);
    }

    /** Hora redondeada al minuto siguiente si tiene segundos (para llegadas). */
    public static String horaArriba(LocalDateTime t) {
        LocalDateTime truncada = t.withSecond(0).withNano(0);
        return hora(truncada.equals(t) ? t : truncada.plusMinutes(1));
    }

    static long minutosArriba(Duration d) {
        long s = d.getSeconds();
        return s <= 0 ? 0 : (s + 59) / 60;
    }
}
