package com.example.nuietsiit.activities.secciones.mapa;

import android.content.Context;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Fotos de ruta para el visor ("realidad virtual" casera).
 *
 * CONVENCIÓN: las fotos son drawables llamados   ruta_<clave>_<grados>
 *   - <clave>:  idPunto de donde se hace la foto (ver UbicacionUsuario.idPunto), p. ej. "entrada", "p0_hall".
 *   - <grados>: hacia dónde miraba la cámara respecto al norte, MÚLTIPLO DE 15 (0 = N, 90 = E, 180 = S, 270 = O).
 *   Ejemplo: res/drawable-nodpi/ruta_entrada_0.jpg  (mirando al norte desde la entrada)
 *            res/drawable-nodpi/ruta_entrada_90.jpg (mirando al este desde la entrada)
 *
 * El visor muestra la foto cuyo rumbo esté más cerca del rumbo del usuario, si está a menos de
 * TOLERANCIA_GRADOS. Si no hay ninguna, pantalla negra. Para añadir fotos no hay que tocar código.
 *
 * Mientras no haya fotos propias del punto se usa la clave "demo" (ruta_demo_0 = foto de prueba).
 */
public final class FotosRuta {

    public static final float TOLERANCIA_GRADOS = 30f;
    public static final String CLAVE_DEMO = "demo";

    private FotosRuta() {}

    public static final class Foto {
        public final int grados;
        public final int idRecurso;

        Foto(int grados, int idRecurso) {
            this.grados = grados;
            this.idRecurso = idRecurso;
        }
    }

    /** Todas las fotos existentes para esa clave (ruta_<clave>_0, _15, _30 ... _345). */
    public static List<Foto> cargar(Context ctx, String clave) {
        List<Foto> fotos = new ArrayList<>();
        String base = "ruta_" + clave.toLowerCase(Locale.ROOT) + "_";
        for (int g = 0; g < 360; g += 15) {
            int id = ctx.getResources().getIdentifier(base + g, "drawable", ctx.getPackageName());
            if (id != 0) fotos.add(new Foto(g, id));
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
