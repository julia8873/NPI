package com.example.nuietsiit.activities.secciones.mapa;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

/**
 * Rumbo hacia el que mira el usuario, en grados respecto al norte magnético (0 = N, 90 = E, ...).
 *
 * Pensado para llevar el móvil en vertical, como si se hiciera una foto: el rumbo es hacia donde
 * apunta la cámara trasera. Si el móvil está tumbado, se usa la parte de arriba del móvil.
 */
public class Brujula implements SensorEventListener {

    public interface Listener {
        void onRumbo(float grados);
    }

    private static final float SUAVIZADO = 0.15f; // 0..1, más alto = reacciona antes pero tiembla más

    private final SensorManager sensorManager;
    private final Sensor vectorRotacion;
    private final Listener listener;

    private final float[] matriz = new float[9];
    private final float[] matrizRemapeada = new float[9];
    private final float[] orientacion = new float[3];

    private float seno;
    private float coseno;
    private boolean iniciado = false;

    public Brujula(Context contexto, Listener listener) {
        this.sensorManager = (SensorManager) contexto.getSystemService(Context.SENSOR_SERVICE);
        this.vectorRotacion = sensorManager == null ? null
                : sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
        this.listener = listener;
    }

    public boolean disponible() {
        return vectorRotacion != null;
    }

    public void iniciar() {
        if (vectorRotacion != null) {
            iniciado = false;
            sensorManager.registerListener(this, vectorRotacion, SensorManager.SENSOR_DELAY_UI);
        }
    }

    public void parar() {
        if (sensorManager != null) sensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() != Sensor.TYPE_ROTATION_VECTOR) return;

        SensorManager.getRotationMatrixFromVector(matriz, event.values);

        // matriz[8] = cuánto mira la pantalla hacia el cielo. Cerca de ±1 = móvil tumbado.
        float azimut;
        if (Math.abs(matriz[8]) > 0.7f) {
            SensorManager.getOrientation(matriz, orientacion);
        } else {
            SensorManager.remapCoordinateSystem(matriz, SensorManager.AXIS_X, SensorManager.AXIS_Z,
                    matrizRemapeada);
            SensorManager.getOrientation(matrizRemapeada, orientacion);
        }
        azimut = orientacion[0]; // radianes, -PI..PI

        // Filtro paso bajo sobre seno/coseno para no tener saltos en 359° -> 0°
        float s = (float) Math.sin(azimut);
        float c = (float) Math.cos(azimut);
        if (!iniciado) {
            seno = s;
            coseno = c;
            iniciado = true;
        } else {
            seno += SUAVIZADO * (s - seno);
            coseno += SUAVIZADO * (c - coseno);
        }

        float grados = (float) Math.toDegrees(Math.atan2(seno, coseno));
        grados = (grados + 360f) % 360f;
        if (listener != null) listener.onRumbo(grados);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // no hacemos nada: el sistema ya fusiona acelerómetro + giroscopio + magnetómetro
    }
}
