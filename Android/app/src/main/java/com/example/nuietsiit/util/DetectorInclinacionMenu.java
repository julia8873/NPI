package com.example.nuietsiit.util;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.SystemClock;

/**
 * Gestos de inclinación del menú principal (acelerómetro).
 *
 * <ul>
 *    <li><b>Selección</b>:
 *       Izquierda/Derecha (roll): elige columna (0: izq, 1: centro, 2: der).
 *       Adelante/Hacia mí (pitch): inclinar el móvil hacia uno mismo selecciona abajo (fila 2);
 *         inclinarlo hacia adelante selecciona arriba (fila 0).
 *       Una zona sólo se confirma si se mantiene {@link #DWELL_MS} ms.</li>
 *    <li><b>Entrar</b>: se realiza agitando el móvil (manejado por ShakeDetector en BaseActivity / MenuPrincipal).
 *       Se ha eliminado el gesto de entrar al pasar de vertical a horizontal.</li>
 *  </ul>
 */
public class DetectorInclinacionMenu implements SensorEventListener {

    public interface Listener {
        /** Posición de la bola, de -1 a 1 (x: derecha +, y: abajo +). Se llama en cada lectura. */
        void alMoverBola(float x, float y);

        /** La selección ha cambiado de zona (fila y columna de 0 a 2). */
        void alCambiarSeleccion(int fila, int columna);

        /** Gancho opcional para indicar estado listo. */
        void alEstarListo(boolean listo);

        /** Gancho opcional para confirmar. */
        void alConfirmar();
    }

    // ---- Parámetros ajustables (grados / milisegundos) ----
    private static final float ALFA = 0.8f;                 // filtro paso bajo (peso del valor anterior)
    private static final float FILA_1 = 36f, FILA_2 = 58f;  // <36 fila 0 (arriba) · 36-58 fila 1 (centro) · >58 fila 2 (abajo)
    private static final float COL_1 = -12f, COL_2 = 12f;   // <-12 col 0 · -12..12 col 1 · >12 col 2
    private static final float HISTERESIS_PITCH = 3f, HISTERESIS_ROLL = 2f;
    private static final long DWELL_MS = 220;

    // Escalas de la bola en el aro
    private static final float ROLL_BOLA = 24f;
    private static final float PITCH_CENTRO_BOLA = 47f;
    private static final float PITCH_BOLA = 22f;

    private final SensorManager sensorManager;
    private final Sensor acelerometro;
    private final Listener listener;

    private boolean primeraLectura;
    private float ax, ay, az;

    // Selección confirmada (se conserva entre onPause y onResume)
    private int fila = 1, columna = 1;
    private int filaPend = -1, columnaPend = -1;
    private long tPend;

    public DetectorInclinacionMenu(Context context, Listener listener) {
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
        filaPend = columnaPend = -1;
        sensorManager.registerListener(this, acelerometro, SensorManager.SENSOR_DELAY_GAME);
    }

    public void parar() {
        if (acelerometro != null) sensorManager.unregisterListener(this);
    }

    /** Fuerza la selección (por ejemplo, tras un toque en pantalla). */
    public void fijarSeleccion(int nuevaFila, int nuevaColumna) {
        fila = nuevaFila;
        columna = nuevaColumna;
        filaPend = columnaPend = -1;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        float[] v = event.values;
        if (primeraLectura) {
            ax = v[0];
            ay = v[1];
            az = v[2];
            primeraLectura = false;
        } else {
            ax = ALFA * ax + (1 - ALFA) * v[0];
            ay = ALFA * ay + (1 - ALFA) * v[1];
            az = ALFA * az + (1 - ALFA) * v[2];
        }

        float pitch = (float) Math.toDegrees(Math.atan2(ay, az));
        float roll = (float) Math.toDegrees(Math.atan2(-ax, Math.hypot(ay, az)));
        long ahora = SystemClock.elapsedRealtime();

        // Bola en el aro:
        // roll > 0: inclinación a la derecha -> bola a la derecha (+X)
        // pitch > PITCH_CENTRO_BOLA: inclinación hacia el usuario -> bola abajo (+Y)
        listener.alMoverBola(
                limitar(roll / ROLL_BOLA),
                limitar((pitch - PITCH_CENTRO_BOLA) / PITCH_BOLA));

        actualizarZona(pitch, roll, ahora);
    }

    private void actualizarZona(float pitch, float roll, long ahora) {
        // Al inclinar el móvil hacia el usuario (pitch > FILA_2), va hacia abajo en el menú (fila 2).
        // Al inclinarlo hacia adelante / alejarlo (pitch < FILA_1), va hacia arriba en el menú (fila 0).
        int filaCand = zona(pitch, fila, FILA_1, FILA_2, HISTERESIS_PITCH);
        int columnaCand = zona(roll, columna, COL_1, COL_2, HISTERESIS_ROLL);

        if (filaCand == fila && columnaCand == columna) {
            filaPend = columnaPend = -1;
            return;
        }
        if (filaCand != filaPend || columnaCand != columnaPend) {
            filaPend = filaCand;
            columnaPend = columnaCand;
            tPend = ahora;
        } else if (ahora - tPend >= DWELL_MS) {
            fila = filaCand;
            columna = columnaCand;
            filaPend = columnaPend = -1;
            listener.alCambiarSeleccion(fila, columna);
        }
    }

    /** Zona 0/1/2 de un valor con histéresis respecto a la zona actual. */
    private static int zona(float valor, int actual, float t1, float t2, float h) {
        switch (actual) {
            case 0:
                if (valor > t1 + h) return valor > t2 + h ? 2 : 1;
                return 0;
            case 1:
                if (valor < t1 - h) return 0;
                if (valor > t2 + h) return 2;
                return 1;
            default:
                if (valor < t2 - h) return valor < t1 - h ? 0 : 1;
                return 2;
        }
    }

    private static float limitar(float v) {
        return Math.max(-1f, Math.min(1f, v));
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }
}