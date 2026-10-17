package com.example.nuietsiit.util;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.SystemClock;

/**
 * Detecta cuando se pasa la mano por encima del móvil con el sensor de proximidad
 * (el que apaga la pantalla al acercarte el móvil a la oreja en una llamada).
 * Registrar con iniciar() en onResume y parar() en onPause.
 */
public class DetectorProximidad implements SensorEventListener {

    public interface OnPasarMano {
        void alPasarMano();
    }

    private static final float DISTANCIA_CERCA_CM = 5f;  // por debajo de esto hay "algo encima"
    private static final long ENFRIAMIENTO_MS = 800;     // evita que un solo gesto cuente dos veces

    private final SensorManager sensorManager;
    private final Sensor proximidad;
    private final OnPasarMano listener;

    private boolean armado;      // solo dispara si antes estaba "lejos" (evita disparos al empezar tapado)
    private long ultimoDisparo;

    public DetectorProximidad(Context context, OnPasarMano listener) {
        this.sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        this.proximidad = sensorManager != null ? sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY) : null;
        this.listener = listener;
    }

    public boolean disponible() {
        return proximidad != null;
    }

    public void iniciar() {
        if (proximidad == null) return;
        armado = false;
        sensorManager.registerListener(this, proximidad, SensorManager.SENSOR_DELAY_UI);
    }

    public void parar() {
        if (proximidad != null) sensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        // Muchos sensores solo devuelven 0 (cerca) o su valor máximo (lejos)
        float umbral = Math.min(DISTANCIA_CERCA_CM, proximidad.getMaximumRange());
        boolean cerca = event.values[0] < umbral;

        if (!cerca) {
            armado = true;
            return;
        }
        long ahora = SystemClock.elapsedRealtime();
        if (armado && ahora - ultimoDisparo > ENFRIAMIENTO_MS) {
            armado = false;
            ultimoDisparo = ahora;
            listener.alPasarMano();
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}
}
