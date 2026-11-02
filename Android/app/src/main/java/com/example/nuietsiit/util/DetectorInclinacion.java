package com.example.nuietsiit.util;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.SystemClock;

/**
 * Detecta inclinaciones claras del móvil hacia la derecha o la izquierda con el acelerómetro (eje X).
 * Registrar con iniciar() en onResume y parar() en onPause.
 * Usa el eje X del dispositivo: la Activity que lo use debe estar fijada en vertical.
 */
public class DetectorInclinacion implements SensorEventListener {

    public static final int DERECHA = 1;
    public static final int IZQUIERDA = -1;

    public interface OnInclinacion {
        void alInclinar(int direccion);
    }

    private static final float ALFA = 0.8f;          // filtro paso bajo: peso del valor anterior
    private static final float UMBRAL = 4f;          // m/s² para considerar que está inclinado
    private static final float REPOSO = 1.5f;        // hay que volver por debajo para rearmar (histéresis)
    private static final long ENFRIAMIENTO_MS = 700;

    private final SensorManager sensorManager;
    private final Sensor acelerometro;
    private final OnInclinacion listener;

    private float x;
    private boolean primeraLectura;
    private boolean armado;
    private long ultimoDisparo;

    public DetectorInclinacion(Context context, OnInclinacion listener) {
        this.sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        this.acelerometro = sensorManager != null ? sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) : null;
        this.listener = listener;
    }

    public boolean disponible() {
        return acelerometro != null;
    }

    public void iniciar() {
        if (acelerometro == null) return;
        primeraLectura = true;
        armado = false; // si ya empieza inclinado, no salta hasta volver a reposo
        sensorManager.registerListener(this, acelerometro, SensorManager.SENSOR_DELAY_GAME);
    }

    public void parar() {
        if (acelerometro != null) sensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        // el filtro arranca en la primera lectura (no en 0): si no, al empezar inclinado pasaría por
        // "reposo" mientras sube y se dispararía solo
        if (primeraLectura) {
            x = event.values[0];
            primeraLectura = false;
        } else {
            x = ALFA * x + (1 - ALFA) * event.values[0];
        }

        if (Math.abs(x) < REPOSO) {
            armado = true;
            return;
        }
        long ahora = SystemClock.elapsedRealtime();
        if (armado && Math.abs(x) > UMBRAL && ahora - ultimoDisparo > ENFRIAMIENTO_MS) {
            armado = false;
            ultimoDisparo = ahora;
            // inclinar el borde derecho hacia abajo da X negativa
            listener.alInclinar(x < 0 ? DERECHA : IZQUIERDA);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }
}
