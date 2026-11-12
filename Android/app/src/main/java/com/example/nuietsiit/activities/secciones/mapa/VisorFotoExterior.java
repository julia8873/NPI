package com.example.nuietsiit.activities.secciones.mapa;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.graphics.drawable.ColorDrawable;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatImageView;

import com.example.nuietsiit.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Fotos de la ruta exterior a pantalla completa (diálogo, no hace falta tocar el Manifest).
 * Gestos: pellizcar = zoom, doble toque = ampliar/volver, deslizar izquierda/derecha = otra foto,
 * deslizar hacia abajo = cerrar, un toque = esconder/enseñar los textos.
 */
public final class VisorFotoExterior {

    private static final float ZOOM_MAX = 5f, ZOOM_DOBLE_TOQUE = 2.5f;

    private final Context ctx;
    private final List<String> claves;
    private final Dialog dialogo;
    private final FrameLayout raiz;
    private final FotoZoom foto;
    private final TextView tvContador, tvTexto;
    private final LinearLayout llPuntos;
    private final View[] capas;
    private int actual;

    /** Abre el visor en la foto `inicial` de la lista de claves (ver {@link FotoLugar#deClave}). */
    public static void abrir(Context ctx, List<String> claves, int inicial) {
        if (claves == null || claves.isEmpty()) return;
        new VisorFotoExterior(ctx, claves, Math.max(0, Math.min(claves.size() - 1, inicial)));
    }

    @SuppressLint("ClickableViewAccessibility")
    private VisorFotoExterior(Context ctx, List<String> claves, int inicial) {
        this.ctx = ctx;
        this.claves = new ArrayList<>(claves);
        float dp = ctx.getResources().getDisplayMetrics().density;
        dialogo = new Dialog(ctx, android.R.style.Theme_Black_NoTitleBar_Fullscreen);

        raiz = new FrameLayout(ctx);
        raiz.setBackgroundColor(Color.BLACK);
        foto = new FotoZoom(ctx);
        raiz.addView(foto, new FrameLayout.LayoutParams(-1, -1));
        // Los gestos se leen en el fondo, que no se mueve (la foto sí, al arrastrarla)
        raiz.setOnTouchListener((v, e) -> foto.tocar(e));

        // Arriba: contador y cerrar
        LinearLayout arriba = new LinearLayout(ctx);
        arriba.setGravity(Gravity.CENTER_VERTICAL);
        arriba.setPadding((int) (16 * dp), (int) (16 * dp), (int) (8 * dp), 0);
        tvContador = new TextView(ctx);
        tvContador.setTextColor(Color.WHITE);
        tvContador.setTypeface(Typeface.DEFAULT_BOLD);
        tvContador.setTextSize(15);
        tvContador.setPadding((int) (12 * dp), (int) (6 * dp), (int) (12 * dp), (int) (6 * dp));
        tvContador.setBackground(pastilla(Color.argb(120, 0, 0, 0), 16 * dp));
        arriba.addView(tvContador, new LinearLayout.LayoutParams(-2, -2));
        arriba.addView(new View(ctx), new LinearLayout.LayoutParams(0, 1, 1f));
        TextView cerrar = new TextView(ctx);
        cerrar.setText("✕");
        cerrar.setTextColor(Color.WHITE);
        cerrar.setTextSize(22);
        cerrar.setGravity(Gravity.CENTER);
        cerrar.setBackground(pastilla(Color.argb(120, 0, 0, 0), 24 * dp));
        cerrar.setContentDescription(ctx.getString(R.string.cerrar));
        cerrar.setOnClickListener(v -> dialogo.dismiss());
        arriba.addView(cerrar, new LinearLayout.LayoutParams((int) (48 * dp), (int) (48 * dp)));
        raiz.addView(arriba, new FrameLayout.LayoutParams(-1, -2, Gravity.TOP));

        // Abajo: instrucción sobre degradado + puntitos + pista de los gestos
        LinearLayout abajo = new LinearLayout(ctx);
        abajo.setOrientation(LinearLayout.VERTICAL);
        abajo.setGravity(Gravity.CENTER_HORIZONTAL);
        abajo.setPadding((int) (20 * dp), (int) (48 * dp), (int) (20 * dp), (int) (28 * dp));
        abajo.setBackground(new GradientDrawable(GradientDrawable.Orientation.BOTTOM_TOP,
                new int[]{Color.argb(220, 0, 0, 0), Color.TRANSPARENT}));
        tvTexto = new TextView(ctx);
        tvTexto.setTextColor(Color.WHITE);
        tvTexto.setTypeface(Typeface.DEFAULT_BOLD);
        tvTexto.setTextSize(22);
        tvTexto.setGravity(Gravity.CENTER);
        abajo.addView(tvTexto, new LinearLayout.LayoutParams(-1, -2));
        llPuntos = new LinearLayout(ctx);
        llPuntos.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams lpPuntos = new LinearLayout.LayoutParams(-1, -2);
        lpPuntos.topMargin = (int) (12 * dp);
        abajo.addView(llPuntos, lpPuntos);
        TextView pista = new TextView(ctx);
        pista.setText(claves.size() > 1
                ? ctx.getString(R.string.mapa_pista_visor_mas)
                : ctx.getString(R.string.mapa_pista_visor_solo));
        pista.setTextColor(Color.argb(170, 255, 255, 255));
        pista.setTextSize(12);
        pista.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams lpPista = new LinearLayout.LayoutParams(-1, -2);
        lpPista.topMargin = (int) (10 * dp);
        abajo.addView(pista, lpPista);
        raiz.addView(abajo, new FrameLayout.LayoutParams(-1, -2, Gravity.BOTTOM));

        capas = new View[]{arriba, abajo};
        dialogo.setContentView(raiz);
        // fondo transparente: al bajar la foto para cerrar se ve el mapa detrás
        if (dialogo.getWindow() != null) dialogo.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        actual = inicial;
        mostrar();
        dialogo.show();
    }

    private void mostrar() {
        String clave = claves.get(actual);
        foto.poner(FotoLugar.deClave(ctx, clave, FotoLugar.ANCHO));
        tvContador.setText((actual + 1) + " / " + claves.size());
        tvContador.setVisibility(claves.size() > 1 ? View.VISIBLE : View.INVISIBLE);
        tvTexto.setText(FotoLugar.textoDe(ctx, clave));
        pintarPuntos(llPuntos, claves.size(), actual);
    }

    /** Pasa a la foto siguiente (+1) o anterior (-1) con un deslizamiento. */
    private void cambiar(int delta) {
        int nueva = actual + delta;
        if (nueva < 0 || nueva >= claves.size()) {
            foto.animate().translationX(0).setDuration(150).start();
            return;
        }
        foto.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
        float ancho = foto.getWidth();
        foto.animate().translationX(-delta * ancho).alpha(0f).setDuration(150).withEndAction(() -> {
            actual = nueva;
            mostrar();
            foto.setTranslationX(delta * ancho);
            foto.animate().translationX(0).alpha(1f).setDuration(180).start();
        }).start();
    }

    /** Arrastre hacia abajo: la foto baja y el fondo se aclara; al soltar, cierra o vuelve. */
    private void arrastrar(float dy, boolean soltar) {
        float alto = Math.max(1, raiz.getHeight());
        if (soltar) {
            if (dy > alto / 6) {
                dialogo.dismiss();
            } else {
                foto.animate().translationY(0).setDuration(150).start();
                raiz.getBackground().setAlpha(255);
            }
            return;
        }
        foto.setTranslationY(Math.max(0, dy));
        raiz.getBackground().setAlpha((int) (255 * (1 - Math.min(0.7f, Math.max(0, dy) / alto))));
    }

    private void alternarTextos() {
        boolean ver = capas[0].getAlpha() < 0.5f;
        for (View v : capas) v.animate().alpha(ver ? 1f : 0f).setDuration(150).start();
    }

    // ---------- Utilidades (también las usa la guía) ----------

    /** Puntitos de página: el actual más ancho y en blanco. */
    static void pintarPuntos(LinearLayout ll, int total, int actual) {
        ll.removeAllViews();
        if (total < 2) return;
        float dp = ll.getResources().getDisplayMetrics().density;
        for (int i = 0; i < total; i++) {
            View punto = new View(ll.getContext());
            punto.setBackground(pastilla(i == actual ? Color.WHITE : Color.argb(110, 255, 255, 255), 3 * dp));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    (int) ((i == actual ? 16 : 6) * dp), (int) (6 * dp));
            lp.setMargins((int) (2 * dp), 0, (int) (2 * dp), 0);
            ll.addView(punto, lp);
        }
    }

    static GradientDrawable pastilla(int color, float radio) {
        GradientDrawable fondo = new GradientDrawable();
        fondo.setColor(color);
        fondo.setCornerRadius(radio);
        return fondo;
    }

    // ---------- Imagen con zoom ----------

    /** ImageView con matriz: pellizcar, doble toque y mover si está ampliada; si no, pasa los gestos al visor. */
    private final class FotoZoom extends AppCompatImageView {

        private final Matrix matriz = new Matrix();
        private final ScaleGestureDetector pellizco;
        private final GestureDetector gestos;
        private float zoom = 1f, arrastreX, arrastreY;
        private boolean escalando;

        FotoZoom(Context ctx) {
            super(ctx);
            setScaleType(ScaleType.MATRIX);
            pellizco = new ScaleGestureDetector(ctx, new ScaleGestureDetector.SimpleOnScaleGestureListener() {
                @Override public boolean onScale(@NonNull ScaleGestureDetector d) {
                    float nuevo = Math.max(1f, Math.min(ZOOM_MAX, zoom * d.getScaleFactor()));
                    matriz.postScale(nuevo / zoom, nuevo / zoom, d.getFocusX(), d.getFocusY());
                    zoom = nuevo;
                    ajustar();
                    return true;
                }
            });
            gestos = new GestureDetector(ctx, new GestureDetector.SimpleOnGestureListener() {
                @Override public boolean onDown(@NonNull MotionEvent e) { return true; }

                @Override public boolean onSingleTapConfirmed(@NonNull MotionEvent e) {
                    alternarTextos();
                    return true;
                }

                @Override public boolean onDoubleTap(@NonNull MotionEvent e) {
                    float nuevo = zoom > 1.1f ? 1f : ZOOM_DOBLE_TOQUE;
                    matriz.postScale(nuevo / zoom, nuevo / zoom, e.getX(), e.getY());
                    zoom = nuevo;
                    ajustar();
                    return true;
                }

                @Override public boolean onScroll(MotionEvent e1, @NonNull MotionEvent e2, float dx, float dy) {
                    if (escalando) return true;
                    if (zoom > 1.01f) {
                        matriz.postTranslate(-dx, -dy);
                        ajustar();
                    } else {
                        arrastreX -= dx;
                        arrastreY -= dy;
                        if (Math.abs(arrastreY) > Math.abs(arrastreX)) {
                            setTranslationX(0);
                            arrastrar(arrastreY, false);
                        } else {
                            setTranslationX(arrastreX);
                        }
                    }
                    return true;
                }

                @Override public boolean onFling(MotionEvent e1, @NonNull MotionEvent e2, float vx, float vy) {
                    if (zoom > 1.01f || escalando) return false;
                    if (Math.abs(vx) > Math.abs(vy) && Math.abs(vx) > 800) {
                        soltar(vx < 0 ? 1 : -1);
                    } else if (vy > 1500 && Math.abs(vy) > Math.abs(vx)) {
                        dialogo.dismiss();
                    } else {
                        soltar(0);
                    }
                    arrastreX = arrastreY = 0;
                    return true;
                }
            });
        }

        void poner(Drawable d) {
            setImageDrawable(d);
            zoom = 1f;
            if (getWidth() > 0) encajar();
        }

        @Override
        protected void onSizeChanged(int w, int h, int ow, int oh) {
            super.onSizeChanged(w, h, ow, oh);
            zoom = 1f;
            encajar();
        }

        boolean tocar(MotionEvent e) {
            pellizco.onTouchEvent(e);
            if (e.getPointerCount() > 1) escalando = true;
            gestos.onTouchEvent(e);
            if (e.getActionMasked() == MotionEvent.ACTION_UP || e.getActionMasked() == MotionEvent.ACTION_CANCEL) {
                // soltar sin fling: decide por la distancia arrastrada
                if (zoom <= 1.01f && !escalando && (arrastreX != 0 || arrastreY != 0)) {
                    if (Math.abs(arrastreY) > Math.abs(arrastreX)) {
                        arrastrar(arrastreY, true);
                    } else {
                        soltar(Math.abs(arrastreX) > getWidth() / 4f ? (arrastreX < 0 ? 1 : -1) : 0);
                    }
                }
                arrastreX = arrastreY = 0;
                escalando = false;
            }
            return true;
        }

        private void soltar(int delta) {
            arrastrar(0, true);
            if (delta != 0) cambiar(delta);
            else animate().translationX(0).setDuration(150).start();
        }

        /** Foto entera y centrada (zoom 1). */
        private void encajar() {
            Drawable d = getDrawable();
            if (d == null || getWidth() == 0) return;
            float escala = Math.min(getWidth() / (float) d.getIntrinsicWidth(),
                    getHeight() / (float) d.getIntrinsicHeight());
            matriz.setScale(escala, escala);
            matriz.postTranslate((getWidth() - d.getIntrinsicWidth() * escala) / 2,
                    (getHeight() - d.getIntrinsicHeight() * escala) / 2);
            setImageMatrix(matriz);
        }

        /** Que la foto no se salga: centrada si cabe, bordes pegados si no. */
        private void ajustar() {
            Drawable d = getDrawable();
            if (d == null) return;
            if (zoom <= 1.01f) {
                zoom = 1f;
                encajar();
                return;
            }
            RectF r = new RectF(0, 0, d.getIntrinsicWidth(), d.getIntrinsicHeight());
            matriz.mapRect(r);
            float dx = r.width() <= getWidth() ? (getWidth() - r.width()) / 2 - r.left
                    : Math.max(Math.min(0, -r.left), getWidth() - r.right);
            float dy = r.height() <= getHeight() ? (getHeight() - r.height()) / 2 - r.top
                    : Math.max(Math.min(0, -r.top), getHeight() - r.bottom);
            matriz.postTranslate(dx, dy);
            setImageMatrix(matriz);
        }
    }
}
