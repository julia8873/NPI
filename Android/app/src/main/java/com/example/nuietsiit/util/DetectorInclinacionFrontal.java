package com.example.nuietsiit.util;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.SystemClock;

/**
 * Detecta cuando el usuario parte de una posición natural de lectura erguida
 * y tumba el móvil hacia atrás (hacia la posición horizontal boca arriba).
 */
public class DetectorInclinacionFrontal implements SensorEventListener {

    public interface OnInclinacionFrontal {
        void alInclinarAtras();
    }

    private static final float ALFA = 0.8f;                 // Filtro paso bajo para suavizar aceleraciones bruscas
    // Para considerarse en posición erguida o de lectura natural:
    // El usuario sostiene el móvil de frente (Y >= 4.0 m/s², Z <= 7.5 m/s²)
    private static final float UMBRAL_REPOSO_Y_MIN = 4.0f;
    private static final float UMBRAL_REPOSO_Z_MAX = 7.5f;

    // Para considerarse inclinado hacia atrás (hacia la horizontal con pantalla arriba o volteado):
    // Z aumenta (>= 8.8 m/s²) o Y cae (<= 1.5 m/s²)
    private static final float UMBRAL_INCLINADO_Z_MIN = 8.8f;
    private static final float UMBRAL_INCLINADO_Y_MAX = 1.5f;

    private static final long ENFRIAMIENTO_MS = 2000;       // 2s de enfriamiento para evitar cambios accidentales

    private final SensorManager sensorManager;
    private final Sensor acelerometro;
    private final OnInclinacionFrontal listener;

    private float yFiltrado;
    private float zFiltrado;
    private boolean primeraLectura;
    private boolean armado;
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
        armado = false;
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
        float zActual = event.values[2]; // Eje Z: perpendicular a la pantalla hacia afuera

        if (primeraLectura) {
            yFiltrado = yActual;
            zFiltrado = zActual;
            primeraLectura = false;
        } else {
            yFiltrado = ALFA * yFiltrado + (1 - ALFA) * yActual;
            zFiltrado = ALFA * zFiltrado + (1 - ALFA) * zActual;
        }

        // 1. Armado: el usuario sostiene el móvil en posición de lectura normal
        if (yFiltrado >= UMBRAL_REPOSO_Y_MIN && zFiltrado <= UMBRAL_REPOSO_Z_MAX) {
            armado = true;
            return;
        }

        // 2. Disparo: si estaba armado y el usuario lo inclina hacia atrás
        long ahora = SystemClock.elapsedRealtime();
        boolean inclinadoAtras = (zFiltrado >= UMBRAL_INCLINADO_Z_MIN || yFiltrado <= UMBRAL_INCLINADO_Y_MAX)
                || (zFiltrado < -7.0f) // Volteado hacia abajo
                || (yFiltrado < -5.0f); // Invertido

        if (armado && inclinadoAtras && (ahora - ultimoDisparo > ENFRIAMIENTO_MS)) {
            armado = false; // Se desarma hasta que el usuario vuelva a colocarlo en posición de lectura
            ultimoDisparo = ahora;
            if (listener != null) {
                listener.alInclinarAtras();
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}
}