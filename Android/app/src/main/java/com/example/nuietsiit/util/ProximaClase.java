package com.example.nuietsiit.util;

import android.content.Context;

import com.example.nuietsiit.R;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;

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

    private static Sesion buscarMejor(List<Sesion> sesiones, LocalDateTime ahora, int[] outDiasMejor) {
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
        outDiasMejor[0] = diasMejor;
        return mejor;
    }

    // Versión localizada usando Context y Locale del dispositivo
    public static ProximaClase de(Context ctx, List<Sesion> sesiones, LocalDateTime ahora) {
        int[] diasMejor = new int[1];
        Sesion mejor = buscarMejor(sesiones, ahora, diasMejor);
        if (mejor == null) return null;

        LocalTime hora = ahora.toLocalTime();
        String cuando;
        if (diasMejor[0] == 0 && !hora.isBefore(mejor.inicio)) {
            cuando = ctx.getString(R.string.proxima_clase_ahora);
        } else if (diasMejor[0] == 0) {
            cuando = ctx.getString(R.string.proxima_clase_hoy_fmt, mejor.inicio.toString());
        } else if (diasMejor[0] == 1) {
            cuando = ctx.getString(R.string.proxima_clase_manana_fmt, mejor.inicio.toString());
        } else {
            String diaCorto = DayOfWeek.of(mejor.diaSemana)
                    .getDisplayName(TextStyle.SHORT, Locale.getDefault())
                    .replace(".", "");
            cuando = ctx.getString(R.string.proxima_clase_dia_fmt, diaCorto, mejor.inicio.toString());
        }
        return new ProximaClase(mejor.codigo, cuando);
    }

    // Versión Java puro (sin Context, útil para pruebas unitarias JUnit)
    public static ProximaClase de(List<Sesion> sesiones, LocalDateTime ahora) {
        int[] diasMejor = new int[1];
        Sesion mejor = buscarMejor(sesiones, ahora, diasMejor);
        if (mejor == null) return null;

        LocalTime hora = ahora.toLocalTime();
        String cuando;
        if (diasMejor[0] == 0 && !hora.isBefore(mejor.inicio)) cuando = "ahora";
        else if (diasMejor[0] == 0) cuando = "hoy " + mejor.inicio;
        else if (diasMejor[0] == 1) cuando = "mañana " + mejor.inicio;
        else cuando = DIAS[mejor.diaSemana - 1] + " " + mejor.inicio;
        return new ProximaClase(mejor.codigo, cuando);
    }
}