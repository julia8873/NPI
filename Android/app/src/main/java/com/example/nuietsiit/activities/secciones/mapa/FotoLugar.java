package com.example.nuietsiit.activities.secciones.mapa;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.text.TextPaint;
import android.text.TextUtils;

import androidx.core.content.ContextCompat;

import java.util.Locale;

/**
 * Foto de un lugar, parada o bus de la ruta exterior: drawable `ruta_ext_<id>_<n>` (n desde 1).
 * Si todavía no existe, genera una tarjeta con icono + nombre + "Foto pendiente".
 * La lista de fotos que faltan está en `assets/rutas_exteriores/README.md`.
 */
public final class FotoLugar {

    public static final String PREFIJO = "ruta_ext_";

    private FotoLugar() {}

    /** Nombre del drawable para ese id y número de foto. */
    public static String nombreRecurso(String id, int n) {
        return PREFIJO + id.toLowerCase(Locale.ROOT) + "_" + n;
    }

    /** ¿Ya hay foto real para ese id y número? */
    public static boolean existe(Context ctx, String id, int n) {
        return idRecurso(ctx, id, n) != 0;
    }

    public static Drawable para(Context ctx, String id, int n) {
        int recurso = idRecurso(ctx, id, n);
        if (recurso != 0) {
            Drawable foto = ContextCompat.getDrawable(ctx, recurso);
            if (foto != null) return foto;
        }
        String[] nombre = nombreEIcono(ctx, id);
        return new BitmapDrawable(ctx.getResources(), tarjeta(ctx, nombre[1], nombre[0]));
    }

    private static int idRecurso(Context ctx, String id, int n) {
        if (id == null) return 0;
        return ctx.getResources().getIdentifier(nombreRecurso(id, n), "drawable", ctx.getPackageName());
    }

    /** {nombre, icono} del lugar, parada o bus ("bus_<línea>") según la red cargada. */
    private static String[] nombreEIcono(Context ctx, String id) {
        if (id == null) return new String[]{"Ruta", "📍"};
        RedTransporte red = CapaLugares.cargarRed(ctx);
        if (red != null) {
            RedTransporte.Lugar lugar = red.lugar(id);
            if (lugar != null) return new String[]{lugar.corto, lugar.icono};
            for (RedTransporte.Linea linea : red.lineas) {
                if (id.equals("bus_" + linea.id)) return new String[]{linea.nombre, "🚌"};
                for (RedTransporte.Sentido sentido : linea.sentidos) {
                    for (RedTransporte.Parada parada : sentido.paradas) {
                        if (parada.id.equals(id)) return new String[]{"Parada " + parada.nombre, "🚏"};
                    }
                }
            }
        }
        return new String[]{id, "📍"};
    }

    /** Tarjeta 4:3 con degradado, icono grande, nombre y "Foto pendiente". */
    private static Bitmap tarjeta(Context ctx, String icono, String nombre) {
        int ancho = 800, alto = 600;
        Bitmap bmp = Bitmap.createBitmap(ancho, alto, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(bmp);
        Paint fondo = new Paint();
        fondo.setShader(new LinearGradient(0, 0, ancho, alto,
                Color.rgb(25, 118, 210), Color.rgb(13, 71, 161), Shader.TileMode.CLAMP));
        c.drawRect(0, 0, ancho, alto, fondo);

        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setTextAlign(Paint.Align.CENTER);
        p.setTextSize(170);
        c.drawText(icono, ancho / 2f, 270, p);

        p.setColor(Color.WHITE);
        p.setTypeface(Typeface.DEFAULT_BOLD);
        p.setTextSize(46);
        String texto = TextUtils.ellipsize(nombre, new TextPaint(p), ancho - 60,
                TextUtils.TruncateAt.END).toString();
        c.drawText(texto, ancho / 2f, 390, p);

        p.setTypeface(Typeface.DEFAULT);
        p.setTextSize(32);
        p.setColor(Color.argb(200, 255, 255, 255));
        c.drawText("📷 Foto pendiente", ancho / 2f, 460, p);
        return bmp;
    }
}
