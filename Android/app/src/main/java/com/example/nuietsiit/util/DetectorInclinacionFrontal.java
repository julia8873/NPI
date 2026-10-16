package com.example.nuietsiit.util;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.SystemClock;

/**
 * Detecta cuando el usuario pasa el móvil de posición vertical (sosteniéndolo para leer)
 * a horizontal (tumbándolo hacia adelante) usando el acelerómetro (eje Y y Z).
 */
public class DetectorInclinacionFrontal implements SensorEventListener {

    public interface OnInclinacionFrontal {
        void alTumbarHorizontal();
    }

    private static final float ALFA = 0.8f;          // Filtro paso bajo
    private static final float UMBRAL_VERTICAL = 7.0f;   // Y > 7.0 m/s² -> Móvil vertical (esperandoInclinacion)
    private static final float UMBRAL_HORIZONTAL = 3.0f; // Y < 3.0 m/s² -> Móvil tumbado hacia adelante
    private static final long ENFRIAMIENTO_MS = 1200;    // Tiempo mínimo entre disparos

    private final SensorManager sensorManager;
    private final Sensor acelerometro;
    private final OnInclinacionFrontal listener;

    private float yFiltrado;
    private boolean primeraLectura;
    private boolean esperandoInclinacion;
    private long ultimoDisparo;

    public DetectorInclinacionFrontal(Context context, OnInclinacionFrontal listener) {
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
        esperandoInclinacion = false;
        sensorManager.registerListener(this, acelerometro, SensorManager.SENSOR_DELAY_UI);
    }

    public void parar() {
        if (acelerometro != null && sensorManager != null) {
            sensorManager.unregisterListener(this);
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        float yActual = event.values[1]; // Eje Y: vertical a lo largo de la pantalla

        if (primeraLectura) {
            yFiltrado = yActual;
            primeraLectura = false;
        } else {
            yFiltrado = ALFA * yFiltrado + (1 - ALFA) * yActual;
        }

        // Si el móvil está en posición vertical normal (sosteniéndolo para leer), queda listo para detectar
        if (yFiltrado > UMBRAL_VERTICAL) {
            esperandoInclinacion = true;
            return;
        }

        // Si previamente estaba vertical y ahora se tumba hacia adelante a posición horizontal
        long ahora = SystemClock.elapsedRealtime();
        if (esperandoInclinacion && yFiltrado < UMBRAL_HORIZONTAL && (ahora - ultimoDisparo > ENFRIAMIENTO_MS)) {
            esperandoInclinacion = false; // Se reinicia hasta que el usuario vuelva a ponerlo en vertical
            ultimoDisparo = ahora;
            if (listener != null) {
                listener.alTumbarHorizontal();
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}
}
