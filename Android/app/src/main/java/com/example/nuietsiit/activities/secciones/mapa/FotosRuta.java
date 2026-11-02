package com.example.nuietsiit.activities.secciones.mapa;

import android.content.Context;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Fotos de ruta para el visor ("realidad virtual" casera).
 *
 * CONVENCIÓN: las fotos son drawables llamados   ruta_<clave>_<grados>
 * o secuenciales por pasos: ruta_<clave>_1, ruta_<clave>_2, ...
 */
public final class FotosRuta {

    public static final float TOLERANCIA_GRADOS = 30f;
    public static final String CLAVE_DEMO = "demo";

    private FotosRuta() {}

    public static final class Foto {
        public final int grados;
        public final int idRecurso;
        public final int pasoIndex;

        Foto(int grados, int idRecurso) {
            this.grados = grados;
            this.idRecurso = idRecurso;
            this.pasoIndex = 0;
        }

        Foto(int grados, int idRecurso, int pasoIndex) {
            this.grados = grados;
            this.idRecurso = idRecurso;
            this.pasoIndex = pasoIndex;
        }
    }

    /** Todas las fotos existentes para esa clave. */
    public static List<Foto> cargar(Context ctx, String clave) {
        List<Foto> fotos = new ArrayList<>();
        String base = "ruta_" + clave.toLowerCase(Locale.ROOT) + "_";
        
        // 1. Probar formato por grados (ruta_clave_0, ruta_clave_15...)
        for (int g = 0; g < 360; g += 15) {
            int id = ctx.getResources().getIdentifier(base + g, "drawable", ctx.getPackageName());
            if (id != 0) fotos.add(new Foto(g, id));
        }

        // 2. Si no hay por grados, probar formato secuencial/pasos (ruta_comedor_1, ruta_comedor_2...)
        if (fotos.isEmpty()) {
            for (int i = 1; i <= 50; i++) {
                int id = ctx.getResources().getIdentifier(base + i, "drawable", ctx.getPackageName());
                if (id != 0) {
                    fotos.add(new Foto(i * (360 / 24), id, i));
                }
            }
        }
        return fotos;
    }

    /** Foto del punto; si no tiene ninguna, las de demo. */
    public static List<Foto> cargarConFallback(Context ctx, String clave) {
        List<Foto> fotos = cargar(ctx, clave);
        return fotos.isEmpty() ? cargar(ctx, CLAVE_DEMO) : fotos;
    }

    /** La foto más cercana al rumbo, o null si ninguna está dentro de la tolerancia. */
    public static Foto elegir(List<Foto> fotos, float rumbo) {
        if (fotos.isEmpty()) return null;
        Foto mejor = null;
        float mejorDif = Float.MAX_VALUE;
        for (Foto f : fotos) {
            float dif = diferencia(f.grados, rumbo);
            if (dif < mejorDif) {
                mejorDif = dif;
                mejor = f;
            }
        }
        return (mejor != null && mejorDif <= TOLERANCIA_GRADOS) ? mejor : null;
    }

    /** Diferencia angular mínima entre dos rumbos (0..180). */
    public static float diferencia(float a, float b) {
        float d = Math.abs(a - b) % 360f;
        return d > 180f ? 360f - d : d;
    }
}