package com.example.nuietsiit.activities.secciones.mapa;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.text.TextUtils;

import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Foto de un lugar, parada o bus de la ruta exterior: drawable `ruta_ext_<id>_<n>` (n desde 1),
 * o de un punto del camino a pie: `ruta_ext_<id del punto>` (ver {@link FotosRutaExterior}).
 * Si todavía no existe, genera una tarjeta provisional (icono + nombre, o instrucción + flecha del giro).
 * La lista de fotos que faltan está en `assets/rutas_exteriores/README.md`.
 * <p>
 * Guía, panel y {@link VisorFotoExterior} manejan las fotos por "clave": el id de un punto del camino,
 * el id de un lugar ("etsiit") o "id#n" para su foto n.
 */
public final class FotoLugar {

    public static final String PREFIJO = "ruta_ext_";
    /** Ancho de las tarjetas provisionales a tamaño completo (4:3); se dibujan a 800 y se escalan. */
    public static final int ANCHO = 800;

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
        return new BitmapDrawable(ctx.getResources(), tarjeta(nombre[1], nombre[0], ANCHO));
    }

    /** Foto del punto del camino, o tarjeta con su instrucción, la flecha del giro y "numero/total". */
    public static Drawable dePunto(Context ctx, FotosRutaExterior.PuntoFoto punto, int numero, int total) {
        int recurso = ctx.getResources().getIdentifier(PREFIJO + punto.id.toLowerCase(Locale.ROOT),
                "drawable", ctx.getPackageName());
        if (recurso != 0) {
            Drawable foto = ContextCompat.getDrawable(ctx, recurso);
            if (foto != null) return foto;
        }
        return new BitmapDrawable(ctx.getResources(),
                tarjetaPunto(punto.instruccion, punto.giro, numero == total, numero + "/" + total, ANCHO));
    }

    // ---------- Claves ----------

    /** Claves de las fotos de un tramo: los puntos del camino a pie o, si no hay, las del sitio (idFoto). */
    public static List<String> delTramo(Context ctx, PlanRuta.Tramo tramo) {
        List<String> claves = new ArrayList<>();
        if (tramo == null) return claves;
        FotosRutaExterior fotos = CapaLugares.cargarFotos(ctx);
        if (fotos != null) {
            for (FotosRutaExterior.PuntoFoto p : fotos.delTramo(tramo)) claves.add(p.id);
        }
        return claves.isEmpty() ? deLugar(ctx, tramo.idFoto) : claves;
    }

    /** "id" y, si hay más fotos reales del sitio, "id#2", "id#3"… */
    public static List<String> deLugar(Context ctx, String id) {
        if (id == null) return Collections.emptyList();
        List<String> claves = new ArrayList<>();
        claves.add(id);
        for (int n = 2; existe(ctx, id, n); n++) claves.add(id + "#" + n);
        return claves;
    }

    /**
     * Foto de la clave a unos `anchoPx` (las reales se reducen al cargar; las tarjetas provisionales de un punto
     * llevan solo la flecha: el texto y el contador los pone encima quien la enseña).
     */
    public static Drawable deClave(Context ctx, String clave, int anchoPx) {
        FotosRutaExterior.PuntoFoto punto = punto(ctx, clave);
        int recurso = punto != null
                ? ctx.getResources().getIdentifier(PREFIJO + punto.id.toLowerCase(Locale.ROOT), "drawable",
                ctx.getPackageName())
                : idRecurso(ctx, idDe(clave), numeroDe(clave));
        if (recurso != 0) {
            Bitmap foto = reducida(ctx, recurso, anchoPx);
            if (foto != null) return new BitmapDrawable(ctx.getResources(), foto);
        }
        if (punto != null) {
            List<FotosRutaExterior.PuntoFoto> puntos = CapaLugares.cargarFotos(ctx).tramoDe(clave).puntos;
            boolean ultimo = puntos.get(puntos.size() - 1) == punto;
            return new BitmapDrawable(ctx.getResources(), tarjetaPunto(null, punto.giro, ultimo, null, anchoPx));
        }
        String[] nombre = nombreEIcono(ctx, idDe(clave));
        return new BitmapDrawable(ctx.getResources(), tarjeta(nombre[1], nombre[0], anchoPx));
    }

    /** Texto de la foto: instrucción del punto o nombre del sitio. */
    public static String textoDe(Context ctx, String clave) {
        FotosRutaExterior.PuntoFoto punto = punto(ctx, clave);
        return punto != null ? punto.instruccion : nombreEIcono(ctx, idDe(clave))[0];
    }

    private static FotosRutaExterior.PuntoFoto punto(Context ctx, String clave) {
        FotosRutaExterior fotos = CapaLugares.cargarFotos(ctx);
        FotosRutaExterior.TramoFotos tramo = fotos == null ? null : fotos.tramoDe(clave);
        if (tramo == null) return null;
        for (FotosRutaExterior.PuntoFoto p : tramo.puntos) {
            if (p.id.equals(clave)) return p;
        }
        return null;
    }

    private static String idDe(String clave) {
        int almohadilla = clave.indexOf('#');
        return almohadilla < 0 ? clave : clave.substring(0, almohadilla);
    }

    private static int numeroDe(String clave) {
        int almohadilla = clave.indexOf('#');
        return almohadilla < 0 ? 1 : Integer.parseInt(clave.substring(almohadilla + 1));
    }

    /** Decodifica el drawable reducido a potencias de 2 hasta ~anchoPx (las miniaturas no gastan memoria). */
    private static Bitmap reducida(Context ctx, int recurso, int anchoPx) {
        BitmapFactory.Options op = new BitmapFactory.Options();
        op.inScaled = false;
        op.inJustDecodeBounds = true;
        BitmapFactory.decodeResource(ctx.getResources(), recurso, op);
        int muestra = 1;
        while (op.outWidth / (muestra * 2) >= anchoPx) muestra *= 2;
        op.inJustDecodeBounds = false;
        op.inSampleSize = muestra;
        return BitmapFactory.decodeResource(ctx.getResources(), recurso, op);
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

    /** Lienzo 4:3 de `anchoPx` en el que se dibuja como si midiera 800 × 600. */
    private static Canvas lienzo(Bitmap bmp, int anchoPx) {
        Canvas c = new Canvas(bmp);
        c.scale(anchoPx / 800f, anchoPx / 800f);
        return c;
    }

    /** Tarjeta 4:3 con degradado, icono grande y nombre. */
    private static Bitmap tarjeta(String icono, String nombre, int anchoPx) {
        int ancho = 800, alto = 600;
        Bitmap bmp = Bitmap.createBitmap(anchoPx, anchoPx * 3 / 4, Bitmap.Config.ARGB_8888);
        Canvas c = lienzo(bmp, anchoPx);
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

        marcaProvisional(c, ancho, alto);
        return bmp;
    }

    /**
     * Tarjeta 4:3 de un punto del camino: flecha del giro (o bandera al llegar), instrucción y contador
     * (instruccion == null: solo la flecha, centrada).
     */
    private static Bitmap tarjetaPunto(String instruccion, double giro, boolean llegada, String contador,
                                       int anchoPx) {
        int ancho = 800, alto = 600;
        Bitmap bmp = Bitmap.createBitmap(anchoPx, anchoPx * 3 / 4, Bitmap.Config.ARGB_8888);
        Canvas c = lienzo(bmp, anchoPx);
        Paint fondo = new Paint();
        fondo.setShader(new LinearGradient(0, 0, 0, alto,
                Color.rgb(38, 50, 56), Color.rgb(21, 101, 192), Shader.TileMode.CLAMP));
        c.drawRect(0, 0, ancho, alto, fondo);

        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        float dy = instruccion == null ? 50 : 0;
        if (llegada) {
            p.setTextAlign(Paint.Align.CENTER);
            p.setTextSize(190);
            c.drawText("🏁", ancho / 2f, 290 + dy, p);
        } else {
            flecha(c, ancho / 2f, 210 + dy, Math.max(-150, Math.min(150, giro)));
        }
        if (instruccion == null) {
            marcaProvisional(c, ancho, alto);
            return bmp;
        }

        // contador "3/8" arriba a la izquierda
        p.setTextAlign(Paint.Align.LEFT);
        p.setTypeface(Typeface.DEFAULT_BOLD);
        p.setTextSize(34);
        float anchoContador = p.measureText(contador);
        Paint chip = new Paint(Paint.ANTI_ALIAS_FLAG);
        chip.setColor(Color.argb(90, 0, 0, 0));
        c.drawRoundRect(new RectF(24, 24, 24 + anchoContador + 36, 80), 28, 28, chip);
        p.setColor(Color.WHITE);
        c.drawText(contador, 42, 64, p);

        // instrucción grande, hasta 2 líneas
        TextPaint tp = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        tp.setColor(Color.WHITE);
        tp.setTypeface(Typeface.DEFAULT_BOLD);
        tp.setTextSize(54);
        StaticLayout texto = StaticLayout.Builder.obtain(instruccion, 0, instruccion.length(), tp, ancho - 80)
                .setAlignment(Layout.Alignment.ALIGN_CENTER)
                .setMaxLines(2)
                .setEllipsize(TextUtils.TruncateAt.END)
                .build();
        c.save();
        c.translate(40, 420 - texto.getHeight() / 2f);
        texto.draw(c);
        c.restore();

        marcaProvisional(c, ancho, alto);
        return bmp;
    }

    /** Flecha gruesa que sale hacia arriba y gira `grados` (+ = derecha), como en las señales de tráfico. */
    private static void flecha(Canvas c, float cx, float cy, double grados) {
        Paint trazo = new Paint(Paint.ANTI_ALIAS_FLAG);
        trazo.setColor(Color.WHITE);
        trazo.setStyle(Paint.Style.STROKE);
        trazo.setStrokeWidth(34);
        trazo.setStrokeCap(Paint.Cap.ROUND);
        trazo.setStrokeJoin(Paint.Join.ROUND);
        float base = cy + 140, codo = cy + 10, largo = 110;
        double r = Math.toRadians(grados);
        float px = cx + (float) (Math.sin(r) * largo), py = codo - (float) (Math.cos(r) * largo);
        Path camino = new Path();
        camino.moveTo(cx, base);
        camino.lineTo(cx, codo);
        camino.lineTo(px, py);
        c.drawPath(camino, trazo);

        // punta triangular orientada según el giro
        Paint relleno = new Paint(Paint.ANTI_ALIAS_FLAG);
        relleno.setColor(Color.WHITE);
        Path punta = new Path();
        punta.moveTo(0, -60);
        punta.lineTo(52, 10);
        punta.lineTo(-52, 10);
        punta.close();
        c.save();
        c.translate(px, py);
        c.rotate((float) grados);
        c.drawPath(punta, relleno);
        c.restore();
    }

    /** Marca pequeña "provisional" abajo a la derecha (hasta que haya foto real). */
    private static void marcaProvisional(Canvas c, int ancho, int alto) {
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setTextAlign(Paint.Align.RIGHT);
        p.setTextSize(24);
        p.setColor(Color.argb(150, 255, 255, 255));
        c.drawText("provisional", ancho - 24, alto - 22, p);
    }
}
