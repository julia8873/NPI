package com.example.nuietsiit.activities.secciones.noticias;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.SystemClock;

/**
 * Pasar la mano por delante del móvil (sensor de PROXIMIDAD, el de arriba de la pantalla) sin tocarla:
 * Noticias lo usa para pasar al siguiente aviso con las manos ocupadas o el móvil apoyado en la mesa.
 * Cuenta como gesto tapar el sensor y destaparlo enseguida (menos de MAX_TAPADO_MS): si se queda tapado más
 * tiempo (bolsillo, funda, el móvil contra la cara) no hace nada. Tras un gesto, ENFRIAMIENTO_MS sin otro,
 * para que un vaivén de la mano no pase dos avisos. Si el móvil no tiene el sensor (muchas tablets), no hace nada.
 */
public class GestoMano implements SensorEventListener {

    /** Más cerca que esto (cm) cuenta como tapado; muchos sensores solo dicen "cerca" (0) o "lejos" (el máximo). */
    private static final float CERCA_CM = 5f;
    private static final long MAX_TAPADO_MS = 1500;
    private static final long ENFRIAMIENTO_MS = 700;

    public interface Oyente {
        void alPasarLaMano();
    }

    private final SensorManager sensores;
    private final Sensor proximidad;
    private final Oyente oyente;
    private boolean escuchando, tapado;
    private long tapadoDesde, ultimoGesto;

    public GestoMano(Context ctx, Oyente oyente) {
        this.oyente = oyente;
        sensores = (SensorManager) ctx.getSystemService(Context.SENSOR_SERVICE);
        proximidad = sensores == null ? null : sensores.getDefaultSensor(Sensor.TYPE_PROXIMITY);
    }

    /** Si el móvil tiene sensor de proximidad (para que el tutorial solo enseñe el gesto entonces). */
    public boolean disponible() {
        return proximidad != null;
    }

    public void iniciar() {
        if (proximidad == null || escuchando) return;
        escuchando = sensores.registerListener(this, proximidad, SensorManager.SENSOR_DELAY_UI);
        tapado = false;
    }

    public void parar() {
        if (!escuchando) return;
        sensores.unregisterListener(this);
        escuchando = false;
    }

    @Override
    public void onSensorChanged(SensorEvent e) {
        boolean cerca = e.values[0] < Math.min(CERCA_CM, e.sensor.getMaximumRange());
        long ahora = SystemClock.elapsedRealtime();
        if (cerca && !tapado) {
            tapado = true;
            tapadoDesde = ahora;
        } else if (!cerca && tapado) {
            tapado = false;
            boolean rapido = ahora - tapadoDesde <= MAX_TAPADO_MS;
            if (rapido && ahora - ultimoGesto > ENFRIAMIENTO_MS) {
                ultimoGesto = ahora;
                oyente.alPasarLaMano();
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int precision) {}
}
