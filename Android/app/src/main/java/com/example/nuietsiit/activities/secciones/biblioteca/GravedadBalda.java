package com.example.nuietsiit.activities.secciones.biblioteca;

import android.animation.ValueAnimator;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.view.Choreographer;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.WeakHashMap;

/**
 * Balda con gravedad: al inclinar el móvil a un lado, los libros se tuercen hacia ese lado apoyándose en
 * su esquina de abajo y se deslizan un poco, con efecto muelle (cada libro con su propia rigidez, para que
 * no se muevan todos a la vez). Con el móvil plano, o mientras se toca la balda, se quedan derechos.
 * Si se inclina más, InclinarParaPasar pasa de estante: la gravedad es el aviso de que eso va a pasar.
 * Iniciar en onResume y parar en onPause.
 */
public class GravedadBalda implements SensorEventListener, Choreographer.FrameCallback {

    private static final float ZONA_MUERTA = 0.7f;      // m/s²: por debajo, el móvil está "plano" a los lados
    private static final float GRADOS_POR_MS2 = 1.6f;   // cuánto se tuerce el libro por cada m/s² de inclinación
    private static final float GIRO_MAX = 5f;           // grados
    private static final float DESLIZ_MAX_DP = 2f;      // lo que resbala el libro hacia el lado bajo
    private static final float PLANO_Z = 9.0f;          // gravedad en Z por encima de esto = móvil sobre la mesa
    private static final float CONTRA_PARED = 0.35f;    // el libro pegado al lateral apenas puede inclinarse
    private static final float RIGIDEZ = 140f;          // muelle (1/s²)
    private static final float AMORTIGUAMIENTO = 0.3f;  // < 1: rebota un poco antes de quedarse quieto
    private static final float FILTRO = 0.85f;          // paso bajo si solo hay acelerómetro

    private final RecyclerView balda;
    private final SensorManager sensores;
    private final Sensor sensor;
    private final boolean filtrar;
    private final float deslizMax;
    private final boolean animaciones;

    private final WeakHashMap<View, float[]> estados = new WeakHashMap<>(); // libro -> {giro, velocidad}
    private float gx;
    private boolean primeraLectura;
    private float objetivo;      // giro al que tienden los libros según la inclinación
    private boolean pausada;     // tocando la balda u ojeando: derechos
    private boolean enMarcha;    // hay un frame pedido
    private long ultimoFrame;

    public GravedadBalda(RecyclerView balda) {
        this.balda = balda;
        Context ctx = balda.getContext();
        sensores = (SensorManager) ctx.getSystemService(Context.SENSOR_SERVICE);
        Sensor gravedad = sensores != null ? sensores.getDefaultSensor(Sensor.TYPE_GRAVITY) : null;
        filtrar = gravedad == null;
        sensor = gravedad != null ? gravedad
                : sensores != null ? sensores.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) : null;
        deslizMax = DESLIZ_MAX_DP * ctx.getResources().getDisplayMetrics().density;
        // con las animaciones del sistema quitadas (accesibilidad), los libros no se mueven solos
        animaciones = ValueAnimator.areAnimatorsEnabled();

        balda.addOnChildAttachStateChangeListener(new RecyclerView.OnChildAttachStateChangeListener() {
            @Override
            public void onChildViewAttachedToWindow(@NonNull View libro) {
                arrancar(); // un libro nuevo en la balda (otro estante) también se inclina
            }

            @Override
            public void onChildViewDetachedFromWindow(@NonNull View libro) {
                libro.setRotation(0f);
                libro.setTranslationX(0f);
                estados.remove(libro);
            }
        });
    }

    public void iniciar() {
        if (sensor == null || !animaciones) return;
        primeraLectura = true;
        sensores.registerListener(this, sensor, SensorManager.SENSOR_DELAY_GAME);
    }

    public void parar() {
        if (sensor != null) sensores.unregisterListener(this);
        objetivo = 0f;
        enderezar();
    }

    /** Mientras se toca la balda o se ojea un libro, los libros vuelven a su sitio (con muelle). */
    public void setPausada(boolean pausada) {
        if (this.pausada == pausada) return;
        this.pausada = pausada;
        arrancar();
    }

    /** Pone todos los libros derechos de golpe: antes de medir dónde está una portada. */
    public void enderezar() {
        for (int i = 0; i < balda.getChildCount(); i++) {
            View libro = balda.getChildAt(i);
            libro.setRotation(0f);
            if (!balda.isAnimating()) libro.setTranslationX(0f);
            float[] e = estados.get(libro);
            if (e != null) {
                e[0] = 0f;
                e[1] = 0f;
            }
        }
    }

    /** Leve balanceo al entrar: enseña que los libros están sueltos y se mueven. */
    public void balancear() {
        if (!animaciones) return;
        for (int i = 0; i < balda.getChildCount(); i++) {
            float[] e = estado(balda.getChildAt(i));
            e[1] += (i % 2 == 0 ? 1 : -1) * (40f + 10f * (i % 3)); // grados/s, alternando el sentido
        }
        arrancar();
    }

    private float[] estado(View libro) {
        float[] e = estados.get(libro);
        if (e == null) {
            e = new float[]{libro.getRotation(), 0f};
            estados.put(libro, e);
        }
        return e;
    }

    private void arrancar() {
        if (enMarcha || !animaciones) return;
        enMarcha = true;
        ultimoFrame = 0;
        Choreographer.getInstance().postFrameCallback(this);
    }

    @Override
    public void doFrame(long ahoraNanos) {
        enMarcha = false;
        float dt = ultimoFrame == 0 ? 0.016f : Math.min(0.033f, (ahoraNanos - ultimoFrame) / 1e9f);
        ultimoFrame = ahoraNanos;
        float meta = pausada ? 0f : objetivo;
        boolean quietos = true;
        boolean recolocando = balda.isAnimating(); // la animación de recolocar de la rejilla usa translationX

        int columnas = balda.getLayoutManager() instanceof GridLayoutManager
                ? ((GridLayoutManager) balda.getLayoutManager()).getSpanCount() : 1;
        for (int i = 0; i < balda.getChildCount(); i++) {
            View libro = balda.getChildAt(i);
            float[] e = estado(libro);
            float metaLibro = contraLaPared(libro, meta, columnas) ? meta * CONTRA_PARED : meta;
            // cada libro con su rigidez: unos llegan antes que otros, como libros de verdad
            float k = RIGIDEZ * (0.8f + 0.08f * ((i * 3) % 6));
            float c = 2f * AMORTIGUAMIENTO * (float) Math.sqrt(k);
            e[1] += (k * (metaLibro - e[0]) - c * e[1]) * dt;
            e[0] += e[1] * dt;
            if (Math.abs(metaLibro - e[0]) > 0.05f || Math.abs(e[1]) > 0.05f) quietos = false;
            else {
                e[0] = metaLibro;
                e[1] = 0f;
            }
            // se apoya en la esquina de abajo del lado hacia el que cae (el giro es 0 al cambiar de lado)
            libro.setPivotX(e[0] >= 0 ? libro.getWidth() - libro.getPaddingRight() : libro.getPaddingLeft());
            libro.setPivotY(libro.getHeight());
            libro.setRotation(e[0]);
            if (!recolocando) libro.setTranslationX(e[0] / GIRO_MAX * deslizMax);
        }
        if (!quietos || recolocando) arrancar();
    }

    // el primer libro de la fila si caen a la izquierda, el último si caen a la derecha
    private static boolean contraLaPared(View libro, float giro, int columnas) {
        if (giro == 0f || !(libro.getLayoutParams() instanceof GridLayoutManager.LayoutParams)) return false;
        int columna = ((GridLayoutManager.LayoutParams) libro.getLayoutParams()).getSpanIndex();
        return giro > 0 ? columna == columnas - 1 : columna == 0;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        float x = event.values[0];
        if (filtrar && !primeraLectura) x = FILTRO * gx + (1f - FILTRO) * x;
        primeraLectura = false;
        gx = x;

        float nuevo = 0f;
        // boca arriba sobre la mesa la X oscila sin sentido: libros derechos
        if (Math.abs(event.values[2]) < PLANO_Z && Math.abs(gx) > ZONA_MUERTA) {
            float inclinacion = gx - Math.signum(gx) * ZONA_MUERTA;
            // inclinar el borde derecho hacia abajo da X negativa: los libros caen a la derecha (giro positivo)
            nuevo = Math.max(-GIRO_MAX, Math.min(GIRO_MAX, -inclinacion * GRADOS_POR_MS2));
        }
        if (Math.abs(nuevo - objetivo) > 0.1f) {
            objetivo = nuevo;
            arrancar();
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }
}