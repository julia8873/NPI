package com.example.nuietsiit.activities.secciones.biblioteca;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.SystemClock;

/**
 * Sabe si el móvil está sobre la mesa: boca arriba, casi plano y quieto (acelerómetro).
 * Para dejarlo hay que ponerlo plano y quieto; para "levantarlo" hay que inclinarlo bastante o moverlo
 * un rato (histéresis): tocar la pantalla o un golpe en la mesa no cuentan.
 * Registrar con iniciar() en onResume y parar() en onPause.
 */
public class DetectorMesa implements SensorEventListener {

    public interface Oyente {
        void alCambiar(boolean enMesa);
    }

    private static final float COS_PLANO = 0.94f;      // ~20°: para considerarlo dejado
    private static final float COS_LEVANTADO = 0.82f;  // ~35°: para considerarlo levantado
    private static final float QUIETO = 0.25f;         // m/s² de movimiento (media) para considerarlo quieto
    private static final float MOVIDO = 1.2f;          // m/s² de movimiento sostenido = levantado
    private static final long MOVIDO_MS = 250;
    private static final float FILTRO_G = 0.9f;
    private static final float FILTRO_MOV = 0.85f;

    private final SensorManager sensores;
    private final Sensor acelerometro;
    private final Oyente oyente;

    private final float[] g = new float[3];
    private float movimiento;
    private boolean primeraLectura, enMesa;
    private long movidoDesde;

    public DetectorMesa(Context context, Oyente oyente) {
        sensores = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        acelerometro = sensores != null ? sensores.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) : null;
        this.oyente = oyente;
    }

    public boolean disponible() {
        return acelerometro != null;
    }

    public boolean enMesa() {
        return enMesa;
    }

    public void iniciar() {
        if (acelerometro == null) return;
        primeraLectura = true;
        movimiento = 0f;
        movidoDesde = 0;
        sensores.registerListener(this, acelerometro, SensorManager.SENSOR_DELAY_GAME);
    }

    /** Al parar se da por levantado: al volver hay que dejarlo otra vez. */
    public void parar() {
        if (acelerometro != null) sensores.unregisterListener(this);
        enMesa = false;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        float[] a = event.values;
        if (primeraLectura) {
            System.arraycopy(a, 0, g, 0, 3);
            primeraLectura = false;
            return;
        }
        for (int i = 0; i < 3; i++) g[i] = FILTRO_G * g[i] + (1 - FILTRO_G) * a[i];
        float dx = a[0] - g[0], dy = a[1] - g[1], dz = a[2] - g[2];
        movimiento = FILTRO_MOV * movimiento + (1 - FILTRO_MOV) * (float) Math.sqrt(dx * dx + dy * dy + dz * dz);

        float norma = (float) Math.sqrt(g[0] * g[0] + g[1] * g[1] + g[2] * g[2]);
        float cos = norma > 0 ? g[2] / norma : 0f; // 1 = boca arriba y plano

        boolean nuevo;
        if (enMesa) {
            long ahora = SystemClock.elapsedRealtime();
            if (movimiento > MOVIDO) {
                if (movidoDesde == 0) movidoDesde = ahora;
            } else {
                movidoDesde = 0;
            }
            boolean movido = movidoDesde != 0 && ahora - movidoDesde > MOVIDO_MS;
            nuevo = cos > COS_LEVANTADO && !movido;
        } else {
            nuevo = cos > COS_PLANO && movimiento < QUIETO;
        }
        if (nuevo != enMesa) {
            enMesa = nuevo;
            movidoDesde = 0;
            oyente.alCambiar(nuevo);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }
}
