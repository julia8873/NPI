package com.example.nuietsiit.activities.secciones.mapa;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Horario simulado de una línea: salidas de cabecera según la frecuencia de cada franja
 * (laborable o fin de semana) y paso por cada parada = salida + minuto de la parada.
 */
public final class HorarioBus {

    private final RedTransporte.Linea linea;

    public HorarioBus(RedTransporte.Linea linea) {
        this.linea = linea;
    }

    /** Un paso del bus por una parada. */
    public static final class Paso {
        public final RedTransporte.Sentido sentido;
        public final LocalDateTime salidaCabecera;
        public final LocalDateTime hora;

        Paso(RedTransporte.Sentido sentido, LocalDateTime salidaCabecera, LocalDateTime hora) {
            this.sentido = sentido;
            this.salidaCabecera = salidaCabecera;
            this.hora = hora;
        }

        /** Hora a la que este mismo bus pasa por otra parada del sentido. */
        public LocalDateTime horaEn(RedTransporte.Parada parada) {
            return salidaCabecera.plus(desfase(parada));
        }
    }

    public static boolean esFinde(LocalDate dia) {
        DayOfWeek d = dia.getDayOfWeek();
        return d == DayOfWeek.SATURDAY || d == DayOfWeek.SUNDAY;
    }

    /** Salidas de cabecera de un día. */
    public List<LocalDateTime> salidas(RedTransporte.Sentido sentido, LocalDate dia) {
        List<RedTransporte.Franja> franjas = esFinde(dia) ? linea.finde : linea.laborable;
        List<LocalDateTime> salidas = new ArrayList<>();
        LocalTime t = sentido.primera;
        while (!t.isAfter(sentido.ultima)) {
            salidas.add(dia.atTime(t));
            LocalTime siguiente = t.plusMinutes(frecuencia(franjas, t));
            if (siguiente.isBefore(t)) break;   // pasaría de medianoche
            t = siguiente;
        }
        return salidas;
    }

    /** Los n próximos pasos por la parada a partir de "desde" (incluido). */
    public List<Paso> proximosPasos(RedTransporte.Sentido sentido, RedTransporte.Parada parada,
                                    LocalDateTime desde, int n) {
        List<Paso> pasos = new ArrayList<>();
        Duration desfase = desfase(parada);
        // el día anterior cubre los buses de después de medianoche; si hoy no quedan, mañana...
        for (LocalDate dia = desde.toLocalDate().minusDays(1);
             pasos.size() < n && !dia.isAfter(desde.toLocalDate().plusDays(7)); dia = dia.plusDays(1)) {
            for (LocalDateTime salida : salidas(sentido, dia)) {
                LocalDateTime hora = salida.plus(desfase);
                if (!hora.isBefore(desde)) {
                    pasos.add(new Paso(sentido, salida, hora));
                    if (pasos.size() == n) break;
                }
            }
        }
        return pasos;
    }

    public Paso proximoPaso(RedTransporte.Sentido sentido, RedTransporte.Parada parada, LocalDateTime desde) {
        List<Paso> pasos = proximosPasos(sentido, parada, desde, 1);
        return pasos.isEmpty() ? null : pasos.get(0);
    }

    /** Último paso por la parada estrictamente antes de "antesDe", o null. */
    public Paso pasoAnterior(RedTransporte.Sentido sentido, RedTransporte.Parada parada, LocalDateTime antesDe) {
        Duration desfase = desfase(parada);
        Paso ultimo = null;
        for (LocalDate dia = antesDe.toLocalDate().minusDays(1);
             !dia.isAfter(antesDe.toLocalDate()); dia = dia.plusDays(1)) {
            for (LocalDateTime salida : salidas(sentido, dia)) {
                LocalDateTime hora = salida.plus(desfase);
                if (hora.isBefore(antesDe)) ultimo = new Paso(sentido, salida, hora);
            }
        }
        return ultimo;
    }

    private static int frecuencia(List<RedTransporte.Franja> franjas, LocalTime t) {
        for (RedTransporte.Franja f : franjas) {
            if (!t.isBefore(f.desde) && t.isBefore(f.hasta)) return f.minutos;
        }
        // fuera de franjas: la más cercana (antes de la primera o después de la última)
        return t.isBefore(franjas.get(0).desde) ? franjas.get(0).minutos : franjas.get(franjas.size() - 1).minutos;
    }

    private static Duration desfase(RedTransporte.Parada parada) {
        return Duration.ofSeconds(Math.round(parada.minuto * 60));
    }
}
