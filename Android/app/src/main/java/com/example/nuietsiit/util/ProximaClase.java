package com.example.nuietsiit.util;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * Cuál es la siguiente clase de un horario semanal y cómo decirlo en pocas palabras ("hoy 10:30").
 */
public final class ProximaClase {

    public static final class Sesion {
        public final String codigo;
        final int diaSemana; // 1=lunes..7=domingo
        final LocalTime inicio;
        final LocalTime fin;

        public Sesion(String codigo, int diaSemana, String horaInicio, String horaFin) {
            this.codigo = codigo;
            this.diaSemana = diaSemana;
            this.inicio = LocalTime.parse(horaInicio);
            this.fin = LocalTime.parse(horaFin);
        }
    }

    private static final String[] DIAS = {"lun", "mar", "mié", "jue", "vie", "sáb", "dom"};

    public final String codigo;
    public final String cuando;

    private ProximaClase(String codigo, String cuando) {
        this.codigo = codigo;
        this.cuando = cuando;
    }

    // la clase en curso cuenta como la próxima; null si el horario está vacío
    public static ProximaClase de(List<Sesion> sesiones, LocalDateTime ahora) {
        int hoy = ahora.getDayOfWeek().getValue();
        LocalTime hora = ahora.toLocalTime();
        Sesion mejor = null;
        int diasMejor = 0;
        for (Sesion s : sesiones) {
            int dias = (s.diaSemana - hoy + 7) % 7;
            if (dias == 0 && !hora.isBefore(s.fin)) dias = 7; // la de hoy ya ha terminado: la semana que viene
            if (mejor == null || dias < diasMejor || (dias == diasMejor && s.inicio.isBefore(mejor.inicio))) {
                mejor = s;
                diasMejor = dias;
            }
        }
        if (mejor == null) return null;

        String cuando;
        if (diasMejor == 0 && !hora.isBefore(mejor.inicio)) cuando = "ahora";
        else if (diasMejor == 0) cuando = "hoy " + mejor.inicio;
        else if (diasMejor == 1) cuando = "mañana " + mejor.inicio;
        else cuando = DIAS[mejor.diaSemana - 1] + " " + mejor.inicio;
        return new ProximaClase(mejor.codigo, cuando);
    }
}
