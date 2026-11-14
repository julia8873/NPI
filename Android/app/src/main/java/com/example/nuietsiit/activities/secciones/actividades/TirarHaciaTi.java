package com.example.nuietsiit.activities.secciones.actividades;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.SystemClock;

/**
 * Inclinar el móvil hacia ti (la parte de arriba hacia la cara, como quien se acerca un cartel para guardárselo):
 * Actividades lo usa para "guardar para luego". Con el acelerómetro filtrado se calcula el cabeceo
 * (ángulo = atan2(y, z): 0° tumbado boca arriba, ~50° leyendo, 90° en vertical, más de 90° pasado hacia ti).
 * Cuenta al pasar de MIN_GRADOS y mantenerse SOSTENER_MS, con el móvil derecho (sin torcerlo a los lados, que es
 * inclinar para pasar) y sin llegar a boca abajo. Para repetirlo hay que volver por debajo de REARME_GRADOS.
 */
public class TirarHaciaTi implements SensorEventListener {

    private static final float ALFA = 0.8f;            // filtro paso bajo (peso del valor anterior)
    private static final float MIN_GRADOS = 110f;      // pasado de la vertical hacia ti
    private static final float MAX_GRADOS = 145f;      // más allá ya es ponerlo boca abajo
    private static final float REARME_GRADOS = 85f;    // hay que volver a la postura de leer (histéresis)
    private static final float MAX_LATERAL = 4f;       // m/s² en X: si se tuerce de lado, es inclinar para pasar
    private static final long SOSTENER_MS = 250;       // un movimiento de paso no cuenta

    public interface Oyente {
        void alTirarHaciaTi();
    }

    private final SensorManager sensores;
    private final Sensor acelerometro;
    private final Oyente oyente;
    private final float[] g = new float[3];
    private boolean escuchando, armado, hayValor;
    private long desde;

    public TirarHaciaTi(Context ctx, Oyente oyente) {
        this.oyente = oyente;
        sensores = (SensorManager) ctx.getSystemService(Context.SENSOR_SERVICE);
        acelerometro = sensores == null ? null : sensores.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
    }

    public boolean disponible() {
        return acelerometro != null;
    }

    public void iniciar() {
        if (acelerometro == null || escuchando) return;
        escuchando = sensores.registerListener(this, acelerometro, SensorManager.SENSOR_DELAY_GAME);
        armado = false; // si ya empieza inclinado hacia ti, no salta hasta ponerlo para leer
        hayValor = false;
        desde = 0;
    }

    public void parar() {
        if (!escuchando) return;
        sensores.unregisterListener(this);
        escuchando = false;
    }

    @Override
    public void onSensorChanged(SensorEvent e) {
        for (int i = 0; i < 3; i++) g[i] = hayValor ? ALFA * g[i] + (1 - ALFA) * e.values[i] : e.values[i];
        hayValor = true;
        double grados = Math.toDegrees(Math.atan2(g[1], g[2]));
        if (grados < REARME_GRADOS && grados > 0) {
            armado = true;
            desde = 0;
            return;
        }
        boolean haciaTi = armado && grados > MIN_GRADOS && grados < MAX_GRADOS && Math.abs(g[0]) < MAX_LATERAL;
        if (!haciaTi) {
            desde = 0;
            return;
        }
        long ahora = SystemClock.elapsedRealtime();
        if (desde == 0) desde = ahora;
        if (ahora - desde >= SOSTENER_MS) {
            armado = false;
            desde = 0;
            oyente.alTirarHaciaTi();
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int precision) {}
}
