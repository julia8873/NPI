package com.example.nuietsiit.activities.secciones.mapa;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.util.Log;

import org.osmdroid.events.MapListener;
import org.osmdroid.events.ScrollEvent;
import org.osmdroid.events.ZoomEvent;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.FolderOverlay;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polygon;

import java.io.InputStream;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Capa del mapa exterior: contornos de la ETSIIT y del Edificio Auxiliar con su etiqueta,
 * recinto, lugares de ejemplo (residencia) y paradas del bus (con zoom ≥ 16; al tocarlas
 * dicen cuándo pasan los próximos buses).
 */
public final class CapaLugares {

    private static final String TAG = "CapaLugares";
    public static final double ZOOM_PARADAS = 16.0;
    private static final int PROXIMOS_BUSES = 3;

    private static RedTransporte redCargada;

    private CapaLugares() {}

    /** Carga (una sola vez) la red de `assets/`; null si falla. */
    public static synchronized RedTransporte cargarRed(Context ctx) {
        if (redCargada == null) {
            try (InputStream entrada = ctx.getAssets().open(RedTransporte.RUTA_ASSET)) {
                redCargada = RedTransporte.cargar(entrada);
            } catch (Exception e) {
                Log.e(TAG, "No se pudo cargar la red de transporte", e);
            }
        }
        return redCargada;
    }

    /** Añade al mapa los lugares y las paradas. */
    public static void instalar(MapView mapa, RedTransporte red) {
        Context ctx = mapa.getContext();
        FolderOverlay contornos = new FolderOverlay();
        FolderOverlay etiquetas = new FolderOverlay();

        for (RedTransporte.Lugar lugar : red.lugares) {
            if (!lugar.recinto.isEmpty()) {
                Polygon recinto = poligono(mapa, lugar.recinto,
                        Color.argb(30, 25, 118, 210), Color.argb(90, 25, 118, 210), 2f);
                recinto.setInfoWindow(null);
                contornos.add(recinto);
            }
            for (List<Punto> edificio : lugar.edificios) {
                Polygon p = poligono(mapa, edificio,
                        Color.argb(110, 25, 118, 210), Color.rgb(13, 71, 161), 3f);
                p.setTitle(lugar.nombre);
                contornos.add(p);
            }
            Marker etiqueta = new Marker(mapa);
            etiqueta.setPosition(geo(lugar.punto));
            etiqueta.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER);
            etiqueta.setIcon(new BitmapDrawable(ctx.getResources(),
                    etiquetaBitmap(ctx, lugar.icono + " " + lugar.corto, lugar.destacado)));
            etiqueta.setTitle(lugar.nombre);
            if (lugar.ejemplo) etiqueta.setSnippet("Ubicación de ejemplo");
            etiquetas.add(etiqueta);
        }

        FolderOverlay paradas = new FolderOverlay();
        for (RedTransporte.Linea linea : red.lineas) {
            HorarioBus horario = new HorarioBus(linea);
            int color = colorDe(linea.color);
            BitmapDrawable icono = new BitmapDrawable(ctx.getResources(),
                    iconoParada(ctx, linea.id, color));
            for (RedTransporte.Sentido sentido : linea.sentidos) {
                for (RedTransporte.Parada parada : sentido.paradas) {
                    Marker m = new Marker(mapa);
                    m.setPosition(geo(parada.punto));
                    m.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER);
                    m.setIcon(icono);
                    m.setTitle("🚌 " + linea.id + " · " + parada.nombre);
                    m.setOnMarkerClickListener((marcador, vista) -> {
                        marcador.setSnippet("Hacia " + sentido.hacia + ": "
                                + proximosBuses(horario, sentido, parada, LocalDateTime.now()));
                        marcador.showInfoWindow();
                        return true;
                    });
                    paradas.add(m);
                }
            }
        }

        // Debajo de lo que ya hubiera (ubicación del usuario, rutas…), salvo las etiquetas
        mapa.getOverlays().add(0, contornos);
        mapa.getOverlays().add(1, paradas);
        mapa.getOverlays().add(etiquetas);

        paradas.setEnabled(mapa.getZoomLevelDouble() >= ZOOM_PARADAS);
        mapa.addMapListener(new MapListener() {
            @Override public boolean onScroll(ScrollEvent event) { return false; }

            @Override public boolean onZoom(ZoomEvent event) {
                boolean visibles = event.getZoomLevel() >= ZOOM_PARADAS;
                if (visibles != paradas.isEnabled()) {
                    paradas.setEnabled(visibles);
                    if (!visibles) paradas.closeAllInfoWindows();
                    mapa.invalidate();
                }
                return false;
            }
        });
        mapa.invalidate();
    }

    /** "3 min, 11 min, 19 min" o "no hay más buses hoy". */
    static String proximosBuses(HorarioBus horario, RedTransporte.Sentido sentido,
                                RedTransporte.Parada parada, LocalDateTime ahora) {
        List<HorarioBus.Paso> pasos = horario.proximosPasos(sentido, parada, ahora, PROXIMOS_BUSES);
        if (pasos.isEmpty()) return "no hay más buses hoy";
        List<String> textos = new ArrayList<>();
        for (HorarioBus.Paso paso : pasos) {
            long min = Duration.between(ahora, paso.hora).toMinutes();
            textos.add(min <= 0 ? "llegando" : min + " min");
        }
        return String.join(", ", textos);
    }

    private static Polygon poligono(MapView mapa, List<Punto> puntos, int relleno, int borde, float grosor) {
        Polygon p = new Polygon(mapa);
        List<GeoPoint> geo = new ArrayList<>();
        for (Punto punto : puntos) geo.add(geo(punto));
        p.setPoints(geo);
        p.getFillPaint().setColor(relleno);
        p.getOutlinePaint().setColor(borde);
        p.getOutlinePaint().setStrokeWidth(grosor * mapa.getResources().getDisplayMetrics().density);
        return p;
    }

    private static GeoPoint geo(Punto p) {
        return new GeoPoint(p.lat, p.lon);
    }

    private static int colorDe(String hex) {
        try {
            return Color.parseColor(hex);
        } catch (Exception e) {
            return Color.rgb(194, 24, 91);
        }
    }

    /** Píldora blanca con borde y texto (azul si es un destino destacado). */
    private static Bitmap etiquetaBitmap(Context ctx, String texto, boolean destacado) {
        float d = ctx.getResources().getDisplayMetrics().density;
        Paint letra = new Paint(Paint.ANTI_ALIAS_FLAG);
        letra.setTextSize((destacado ? 14 : 12) * d);
        letra.setTypeface(Typeface.DEFAULT_BOLD);
        letra.setColor(destacado ? Color.WHITE : Color.rgb(33, 33, 33));
        float ancho = letra.measureText(texto) + 20 * d;
        float alto = letra.getTextSize() + 14 * d;

        Bitmap bmp = Bitmap.createBitmap((int) Math.ceil(ancho), (int) Math.ceil(alto), Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(bmp);
        RectF caja = new RectF(d, d, ancho - d, alto - d);
        Paint fondo = new Paint(Paint.ANTI_ALIAS_FLAG);
        fondo.setColor(destacado ? Color.rgb(13, 71, 161) : Color.WHITE);
        c.drawRoundRect(caja, alto / 2, alto / 2, fondo);
        Paint borde = new Paint(Paint.ANTI_ALIAS_FLAG);
        borde.setStyle(Paint.Style.STROKE);
        borde.setStrokeWidth(1.5f * d);
        borde.setColor(destacado ? Color.WHITE : Color.rgb(13, 71, 161));
        c.drawRoundRect(caja, alto / 2, alto / 2, borde);
        c.drawText(texto, 10 * d, alto / 2 - (letra.descent() + letra.ascent()) / 2, letra);
        return bmp;
    }

    /** Círculo del color de la línea con su número. */
    private static Bitmap iconoParada(Context ctx, String linea, int color) {
        float d = ctx.getResources().getDisplayMetrics().density;
        int lado = (int) (26 * d);
        Bitmap bmp = Bitmap.createBitmap(lado, lado, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(bmp);
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setColor(Color.WHITE);
        c.drawCircle(lado / 2f, lado / 2f, lado / 2f, p);
        p.setColor(color);
        c.drawCircle(lado / 2f, lado / 2f, lado / 2f - 2.5f * d, p);
        p.setColor(Color.WHITE);
        p.setTextSize(13 * d);
        p.setTypeface(Typeface.DEFAULT_BOLD);
        p.setTextAlign(Paint.Align.CENTER);
        c.drawText(linea, lado / 2f, lado / 2f - (p.descent() + p.ascent()) / 2, p);
        return bmp;
    }
}
