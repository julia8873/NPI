package com.example.nuietsiit.util;

import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.os.Build;
import android.os.SystemClock;
import android.provider.Settings;
import android.widget.Toast;

import com.example.nuietsiit.R;
import com.example.nuietsiit.activities.secciones.mapa.LocalizadorUsuario;

import java.util.Calendar;

/**
 * Detecta cuando el móvil se coloca boca abajo o en la oscuridad (nivel de luz < 5 lux,
 * como en un bolsillo o sobre la mesa boca abajo).
 * Si el usuario se encuentra en horario de clase, activa el MODO SILENCIO REAL del sistema
 * para todo el teléfono. Al levantar el móvil o sacarlo a la luz, restaura el sonido anterior.
 */
public class DetectorBocaAbajo implements SensorEventListener {

    private static final float ALFA = 0.8f;
    private static final float UMBRAL_BOCA_ABAJO_Z = -8.0f; // Eje Z negativo = boca abajo
    private static final float UMBRAL_FLAT_XY = 3.5f;        // Plano horizontal
    private static final float UMBRAL_LUX_OSCURIDAD = 5.0f;  // Menos de 5 lux = oscuridad (bolsillo / boca abajo)
    private static final long TIEMPO_ESTABLE_MS = 800;      // Tiempo continuo para confirmar
    private static final long ENFRIAMIENTO_MS = 5000;       // Enfriamiento entre avisos

    private final Context context;
    private final SensorManager sensorManager;
    private final Sensor acelerometro;
    private final Sensor luzSensor;

    private float zFiltrado;
    private float xFiltrado;
    private float yFiltrado;
    private float luzActual = 100f; // Por defecto luz ambiente normal
    private boolean primeraLectura;

    private long tiempoInicioDisparo = 0;
    private boolean activadoModoSilencio = false;
    private long ultimoDisparo = 0;

    // Estado previo del teléfono para restaurarlo al levantarlo
    private int ringerModePrevio = AudioManager.RINGER_MODE_NORMAL;
    private int interruptionFilterPrevio = NotificationManager.INTERRUPTION_FILTER_ALL;

    public DetectorBocaAbajo(Context context) {
        this.context = context.getApplicationContext();
        this.sensorManager = (SensorManager) this.context.getSystemService(Context.SENSOR_SERVICE);
        this.acelerometro = sensorManager != null ? sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) : null;
        this.luzSensor = sensorManager != null ? sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT) : null;
    }

    public boolean disponible() {
        return acelerometro != null || luzSensor != null;
    }

    public void iniciar() {
        if (sensorManager == null) return;
        primeraLectura = true;
        if (acelerometro != null) {
            sensorManager.registerListener(this, acelerometro, SensorManager.SENSOR_DELAY_UI);
        }
        if (luzSensor != null) {
            sensorManager.registerListener(this, luzSensor, SensorManager.SENSOR_DELAY_UI);
        }
    }

    public void parar() {
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            float x = event.values[0];
            float y = event.values[1];
            float z = event.values[2];

            if (primeraLectura) {
                xFiltrado = x;
                yFiltrado = y;
                zFiltrado = z;
                primeraLectura = false;
            } else {
                xFiltrado = ALFA * xFiltrado + (1 - ALFA) * x;
                yFiltrado = ALFA * yFiltrado + (1 - ALFA) * y;
                zFiltrado = ALFA * zFiltrado + (1 - ALFA) * z;
            }
        } else if (event.sensor.getType() == Sensor.TYPE_LIGHT) {
            luzActual = event.values[0];
        }

        boolean estaBocaAbajo = (zFiltrado <= UMBRAL_BOCA_ABAJO_Z)
                && (Math.abs(xFiltrado) <= UMBRAL_FLAT_XY)
                && (Math.abs(yFiltrado) <= UMBRAL_FLAT_XY);

        boolean estaOscuro = (luzActual < UMBRAL_LUX_OSCURIDAD);

        // Se activa si está boca abajo O si está en completa oscuridad (en el bolsillo / bolso durante clase)
        boolean condicionActivacion = estaBocaAbajo || estaOscuro;

        long ahora = SystemClock.elapsedRealtime();

        if (condicionActivacion) {
            if (tiempoInicioDisparo == 0) {
                tiempoInicioDisparo = ahora;
            } else if (!activadoModoSilencio && (ahora - tiempoInicioDisparo >= TIEMPO_ESTABLE_MS)
                    && (ahora - ultimoDisparo > ENFRIAMIENTO_MS)) {

                // Comprobar si el usuario está en clase según su horario
                String correo = context.getSharedPreferences("SesionUsuario", Context.MODE_PRIVATE)
                        .getString("CORREO", null);
                LocalizadorUsuario.SesionActual sesion = LocalizadorUsuario.sesionEnCurso(
                        context, correo, Calendar.getInstance());

                if (sesion != null) {
                    if (activarModoSilencioSistema(sesion)) {
                        activadoModoSilencio = true;
                        ultimoDisparo = ahora;
                    } else {
                        ultimoDisparo = ahora;
                        tiempoInicioDisparo = 0;
                    }
                }
            }
        } else {
            // Si estaba silenciado y el usuario levanta el móvil o lo saca a la luz
            if (activadoModoSilencio && (zFiltrado > 2.0f || luzActual > 20.0f)) {
                activadoModoSilencio = false;
                restaurarModoSonidoSistema();
            }
            tiempoInicioDisparo = 0;
        }
    }

    private boolean activarModoSilencioSistema(LocalizadorUsuario.SesionActual sesion) {
        AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        if (audioManager == null) return false;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (notificationManager == null || !notificationManager.isNotificationPolicyAccessGranted()) {
                Intent intent = new Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
                Toast.makeText(context, R.string.boca_abajo_permiso_dnd, Toast.LENGTH_LONG).show();
                return false;
            }

            ringerModePrevio = audioManager.getRingerMode();
            interruptionFilterPrevio = notificationManager.getCurrentInterruptionFilter();

            notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_NONE);
            audioManager.setRingerMode(AudioManager.RINGER_MODE_SILENT);
        } else {
            ringerModePrevio = audioManager.getRingerMode();
            audioManager.setRingerMode(AudioManager.RINGER_MODE_SILENT);
        }

        String asignaturaConAula = sesion.nombreAsignatura + (sesion.aula != null ? (" (" + sesion.aula + ")") : "");
        String msg = context.getString(R.string.boca_abajo_silenciado_fmt, asignaturaConAula);
        Toast.makeText(context, msg, Toast.LENGTH_LONG).show();
        return true;
    }

    private void restaurarModoSonidoSistema() {
        AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        if (audioManager == null) return;

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && notificationManager != null
                    && notificationManager.isNotificationPolicyAccessGranted()) {
                notificationManager.setInterruptionFilter(interruptionFilterPrevio);
            }
            audioManager.setRingerMode(ringerModePrevio);
            Toast.makeText(context, R.string.boca_abajo_restaurado, Toast.LENGTH_SHORT).show();
        } catch (SecurityException ignored) {
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}
}
