package com.example.nuietsiit.activities.main;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;

/**
 * Detector de agitación lateral (de lado a lado).
 *
 * Detecta un movimiento rápido de vaivén horizontal (sacudida lateral de muñeca,
 * alternando aceleración positiva y negativa en el eje X) en una ventana de 500 ms.
 * Es completamente inmune a movimientos verticales, inclinaciones o pasar de vertical a horizontal.
 */
public class ShakeDetector implements SensorEventListener {

    // Umbral de aceleración lateral en m/s² para detectar un golpe a un lado (aumentado para reducir sensibilidad)
    private static final float UMBRAL_LATERAL = 13.5f;
    // Ventana mínima y máxima entre el golpe a un lado y el rebote al lado contrario
    private static final int TIEMPO_MIN_REBOTE_MS = 80;
    private static final int TIEMPO_MAX_REBOTE_MS = 400;
    private static final int ENFRIAMIENTO_DISPARO_MS = 1200;

    private OnShakeListener mListener;

    private int mDireccionPrevia = 0; // +1: derecha, -1: izquierda
    private long mTiempoPrimerGolpe = 0;
    private long mUltimoDisparo = 0;

    public interface OnShakeListener {
        void onShake();
    }

    public void setOnShakeListener(OnShakeListener listener) {
        this.mListener = listener;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (mListener == null) return;

        long now = System.currentTimeMillis();

        // Enfriamiento tras haber disparado para evitar aperturas dobles
        if (now - mUltimoDisparo < ENFRIAMIENTO_DISPARO_MS) {
            return;
        }

        float ax = event.values[0]; // Eje X: aceleración lateral (de lado a lado)

        if (mDireccionPrevia == 0 || (now - mTiempoPrimerGolpe > TIEMPO_MAX_REBOTE_MS)) {
            // Esperando primer golpe hacia un lado
            if (ax > UMBRAL_LATERAL) {
                mDireccionPrevia = 1; // Primer golpe hacia la derecha
                mTiempoPrimerGolpe = now;
            } else if (ax < -UMBRAL_LATERAL) {
                mDireccionPrevia = -1; // Primer golpe hacia la izquierda
                mTiempoPrimerGolpe = now;
            }
        } else {
            // Ya tenemos un golpe hacia un lado, comprobamos el rebote hacia el lado contrario
            long tiempoTranscurrido = now - mTiempoPrimerGolpe;

            if (tiempoTranscurrido >= TIEMPO_MIN_REBOTE_MS && tiempoTranscurrido <= TIEMPO_MAX_REBOTE_MS) {
                if (mDireccionPrevia == 1 && ax < -UMBRAL_LATERAL) {
                    // Derecha -> Izquierda: ¡sacudida lateral confirmada!
                    disparar(now);
                } else if (mDireccionPrevia == -1 && ax > UMBRAL_LATERAL) {
                    // Izquierda -> Derecha: ¡sacudida lateral confirmada!
                    disparar(now);
                }
            }
        }
    }

    private void disparar(long now) {
        mDireccionPrevia = 0;
        mTiempoPrimerGolpe = 0;
        mUltimoDisparo = now;
        if (mListener != null) {
            mListener.onShake();
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }
}
