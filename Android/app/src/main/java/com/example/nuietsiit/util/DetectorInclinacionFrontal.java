package com.example.nuietsiit.util;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.SystemClock;

/**
 * Detecta cuando el usuario parte de una posición COMPLETAMENTE VERTICAL
 * (sosteniendo el móvil erguido de frente) y lo tumba del todo hacia atrás
 * (dejándolo en posición completamente horizontal con la pantalla hacia arriba).
 */
public class DetectorInclinacionFrontal implements SensorEventListener {

    public interface OnInclinacionFrontal {
        void alInclinarAtras();
    }

    private static final float ALFA = 0.8f;                 // Filtro paso bajo para suavizar aceleraciones bruscas
    // Para considerarse completamente vertical: Y elevado (cerca de 9.8) y Z bajo (cerca de 0)
    private static final float UMBRAL_VERTICAL_Y = 8.2f;    // Y > 8.2 m/s²
    private static final float UMBRAL_VERTICAL_Z_MAX = 4.0f; // |Z| < 4.0 m/s²

    // Para considerarse tumbado del todo (horizontal boca arriba): Z elevado y Y cerca de 0
    private static final float UMBRAL_TUMBADO_Z = 8.3f;     // Z > 8.3 m/s² (pantalla mirando al techo)
    private static final float UMBRAL_TUMBADO_Y_MAX = 3.0f; // |Y| < 3.0 m/s² (sin inclinación vertical)

    private static final long ENFRIAMIENTO_MS = 1200;       // Tiempo mínimo entre detecciones

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

        // 1. Condición de partida estricta: posición completamente vertical
        if (yFiltrado > UMBRAL_VERTICAL_Y && Math.abs(zFiltrado) < UMBRAL_VERTICAL_Z_MAX) {
            armado = true;
            return;
        }

        // 2. Condición de disparo: habiendo partido de vertical, que se tumbe del todo (horizontal boca arriba o hacia abajo)
        long ahora = SystemClock.elapsedRealtime();
        boolean tumbadoDelTodo = (zFiltrado > UMBRAL_TUMBADO_Z && Math.abs(yFiltrado) < UMBRAL_TUMBADO_Y_MAX)
                || (zFiltrado < -7.8f && Math.abs(yFiltrado) < 3.5f) // Tumbado hacia abajo
                || (yFiltrado < -6.5f); // Volteado completamente hacia abajo

        if (armado && tumbadoDelTodo && (ahora - ultimoDisparo > ENFRIAMIENTO_MS)) {
            armado = false; // Se desarma hasta que el usuario vuelva a colocarlo completamente vertical
            ultimoDisparo = ahora;
            if (listener != null) {
                listener.alInclinarAtras();
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}
}
