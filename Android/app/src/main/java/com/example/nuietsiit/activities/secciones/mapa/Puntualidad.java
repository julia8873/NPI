package com.example.nuietsiit.activities.secciones.mapa;

import android.content.Context;

import com.example.nuietsiit.R;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * ¿Llego a tiempo? Compara la llegada prevista con la hora límite del viaje (inicio de la clase, "HH:mm").
 * Verde = llegas con margen, ámbar = justo, rojo = tarde.
 */
public final class Puntualidad {

    public enum Nivel { VERDE, AMBAR, ROJO }

    /** Con menos de este margen (y sin llegar tarde) se avisa de que vas justo. */
    static final int MARGEN_JUSTO_MIN = 3;

    public final Nivel nivel;
    /** Minutos de margen: positivo = antes, negativo = tarde. */
    public final long margen;

    private Puntualidad(Nivel nivel, long margen) {
        this.nivel = nivel;
        this.margen = margen;
    }

    /** null si no hay hora límite. La hora límite es del mismo día que la llegada. */
    public static Puntualidad de(LocalDateTime llegada, String horaLimite) {
        if (llegada == null || horaLimite == null) return null;
        LocalDateTime limite = llegada.toLocalDate().atTime(LocalTime.parse(horaLimite));
        // Redondeo hacia abajo a minutos: 59 s de margen no son "1 min antes"
        long margen = Math.floorDiv(Duration.between(llegada, limite).getSeconds(), 60);
        Nivel nivel = margen < 0 ? Nivel.ROJO : margen < MARGEN_JUSTO_MIN ? Nivel.AMBAR : Nivel.VERDE;
        return new Puntualidad(nivel, margen);
    }

    public String texto(Context ctx) {
        if (ctx == null) return texto();
        switch (nivel) {
            case VERDE: return ctx.getString(R.string.mapa_llegas_min_antes, margen);
            case AMBAR: return ctx.getString(R.string.mapa_llegas_justo);
            default: return ctx.getString(R.string.mapa_llegaras_min_tarde, -margen);
        }
    }

    public String texto() {
        switch (nivel) {
            case VERDE: return "Llegas " + margen + " min antes";
            case AMBAR: return "Llegas justo";
            default: return "Llegarás " + (-margen) + " min tarde";
        }
    }

    /** ¿Vale la pena cambiar a la opción nueva? Solo si llega antes y a tiempo (no roja). */
    public static boolean mejora(Puntualidad nueva, Puntualidad actual) {
        return nueva != null && actual != null && nueva.margen > actual.margen && nueva.nivel != Nivel.ROJO;
    }
}
