package com.example.nuietsiit.util;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.SystemClock;

/**
 * Detecta cuando se gira el móvil para enseñar la pantalla a quien está delante, y cuando se vuelve a mirar.
 * Con el vector de rotación se sabe hacia dónde mira la pantalla en horizontal: mientras la miras tú se guarda esa
 * dirección como "hacia mí"; si pasa a mirar en sentido contrario (girarlo como una carta o volcarlo hacia delante)
 * y se mantiene, es enseñar. Boca abajo en la mesa no cuenta: la pantalla mira al suelo, no a nadie.
 * Registrar con iniciar() en onResume y parar() en onPause.
 */
public class DetectorEnsenar implements SensorEventListener {

    public interface OnEnsenar {
        void alEnsenar();
        void alVolver();
    }

    private static final float HORIZONTAL_MIN = 0.3f;  // por debajo, la pantalla mira casi arriba/abajo: sin dirección
    private static final float HORIZONTAL_ENSENAR = 0.7f; // al enseñar, la pantalla está casi en vertical
    private static final float SIGUE_HACIA_MI = 0.7f;  // coseno: dentro de ~45° se sigue la dirección poco a poco
    private static final float SEGUIR = 0.03f;         // cuánto se acerca la dirección guardada a la actual
    private static final float COS_ENSENAR = -0.5f;    // más de ~120° respecto a "hacia mí"
    private static final float COS_VOLVER = 0.3f;      // otra vez de cara (histéresis)
    private static final float ARRIBA_VOLVER = 0.7f;   // o tumbado boca arriba
    private static final long SOSTENER_MS = 300;       // un giro de paso no cuenta

    private final SensorManager sensorManager;
    private final Sensor rotacion;
    private final OnEnsenar listener;
    private final float[] matriz = new float[9];

    private boolean hayReferencia;
    private float refX, refY;        // dirección horizontal (unitaria) de la pantalla cuando la miras tú
    private boolean ensenando;
    private long inicioCruce;

    public DetectorEnsenar(Context context, OnEnsenar listener) {
        this.sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        Sensor s = null;
        if (sensorManager != null) {
            s = sensorManager.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR);
            if (s == null) s = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
        }
        this.rotacion = s;
        this.listener = listener;
    }

    public void iniciar() {
        if (rotacion == null) return;
        hayReferencia = false; // si ya empieza girado, no salta hasta mirarlo de cara
        ensenando = false;
        inicioCruce = 0;
        sensorManager.registerListener(this, rotacion, SensorManager.SENSOR_DELAY_GAME);
    }

    public void parar() {
        if (rotacion != null) sensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        SensorManager.getRotationMatrixFromVector(matriz, event.values);
        // eje Z del móvil (perpendicular a la pantalla) en coordenadas del mundo
        float nx = matriz[2], ny = matriz[5], nz = matriz[8];
        float horizontal = (float) Math.sqrt(nx * nx + ny * ny);
        float cos = 1f; // coseno entre la dirección actual y "hacia mí"
        if (horizontal > HORIZONTAL_MIN) {
            nx /= horizontal;
            ny /= horizontal;
            if (hayReferencia) cos = nx * refX + ny * refY;
        }

        if (!ensenando) {
            // seguir despacio la dirección "hacia mí" (por si te giras), pero no mientras se está girando el móvil
            if (nz > -HORIZONTAL_MIN && horizontal > HORIZONTAL_MIN) {
                if (!hayReferencia) {
                    refX = nx;
                    refY = ny;
                    hayReferencia = true;
                } else if (cos > SIGUE_HACIA_MI) {
                    float x = refX + SEGUIR * (nx - refX), y = refY + SEGUIR * (ny - refY);
                    float m = (float) Math.sqrt(x * x + y * y);
                    refX = x / m;
                    refY = y / m;
                }
            }
            if (!hayReferencia) return;
        }

        boolean cruzado = ensenando
                ? (horizontal > HORIZONTAL_MIN && cos > COS_VOLVER) || nz > ARRIBA_VOLVER
                : horizontal > HORIZONTAL_ENSENAR && cos < COS_ENSENAR;
        if (!cruzado) {
            inicioCruce = 0;
            return;
        }
        long ahora = SystemClock.elapsedRealtime();
        if (inicioCruce == 0) inicioCruce = ahora;
        if (ahora - inicioCruce < SOSTENER_MS) return;

        inicioCruce = 0;
        ensenando = !ensenando;
        if (ensenando) listener.alEnsenar();
        else listener.alVolver();
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }
}
