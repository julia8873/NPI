package com.example.nuietsiit.activities.secciones.asignaturas;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

/**
 * Detector de giro para la ruleta de cursos basado en el acelerómetro.
 * Detecta la inclinación/rotación física del teléfono en el plano frontal (como un volante de coche)
 * y notifica los cambios de ángulo para hacer girar la ruleta en tiempo real.
 */
public class DetectorGiroRuleta implements SensorEventListener {

    public interface Oyente {
        /**
         * Notifica la variación de ángulo (en grados) producida por el giro del teléfono.
         * @param deltaAngulo Cambio en grados (positivo = sentido horario, negativo = antihorario).
         */
        void onGiro(float deltaAngulo);
    }

    private static final float FILTRO_G = 0.88f;       // Factor de suavizado del filtro pasa-bajo (amortigua temblor)
    private static final float UMBRAL_PLANO = 1.8f;    // Umbral mínimo de fuerza gravitatoria en X/Y
    private static final float UMBRAL_DELTA_MIN = 0.55f;// Zona muerta equilibrada para congelar en reposo (sin lag)
    private static final float SENSIBILIDAD = 1.8f;    // Multiplicador de sensibilidad para mayor agilidad al girar la muñeca

    private final SensorManager sensorManager;
    private final Sensor acelerometro;
    private final Oyente oyente;

    private final float[] g = new float[3];
    private boolean primeraLectura = true;
    private float ultimoAnguloFisico = 0f;

    public DetectorGiroRuleta(Context context, Oyente oyente) {
        this.sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        this.acelerometro = sensorManager != null ? sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) : null;
        this.oyente = oyente;
    }

    public boolean disponible() {
        return acelerometro != null;
    }

    public void iniciar() {
        if (acelerometro == null) return;
        primeraLectura = true;
        sensorManager.registerListener(this, acelerometro, SensorManager.SENSOR_DELAY_GAME);
    }

    public void parar() {
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() != Sensor.TYPE_ACCELEROMETER || oyente == null) return;

        float[] a = event.values;

        if (primeraLectura) {
            System.arraycopy(a, 0, g, 0, 3);
            primeraLectura = false;
            float gx = g[0];
            float gy = g[1];
            if (Math.hypot(gx, gy) > UMBRAL_PLANO) {
                ultimoAnguloFisico = (float) Math.toDegrees(Math.atan2(-gx, gy));
            } else {
                ultimoAnguloFisico = 0f;
            }
            return;
        }

        // Aplicar filtro pasa-bajo para suavizar
        g[0] = FILTRO_G * g[0] + (1f - FILTRO_G) * a[0];
        g[1] = FILTRO_G * g[1] + (1f - FILTRO_G) * a[1];
        g[2] = FILTRO_G * g[2] + (1f - FILTRO_G) * a[2];

        float gx = g[0];
        float gy = g[1];

        // Verificar que el teléfono no esté apoyado horizontalmente sobre una mesa
        if (Math.hypot(gx, gy) < UMBRAL_PLANO) {
            return;
        }

        // Calcular ángulo de inclinación lateral (tipo volante): -gx hace que el giro horario sea positivo
        float anguloFisicoActual = (float) Math.toDegrees(Math.atan2(-gx, gy));

        float delta = anguloFisicoActual - ultimoAnguloFisico;

        // Normalizar el delta en el rango [-180, 180] para evitar saltos al cruzar los ±180°
        while (delta > 180f) delta -= 360f;
        while (delta < -180f) delta += 360f;

        // Si la variación supera el umbral mínimo de temblor, notificar multiplicando por la sensibilidad
        if (Math.abs(delta) >= UMBRAL_DELTA_MIN) {
            oyente.onGiro(delta * SENSIBILIDAD);
            ultimoAnguloFisico = anguloFisicoActual;
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }
}
