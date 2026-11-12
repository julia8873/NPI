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
 * Detecta cuando el móvil se coloca boca abajo sobre una superficie (Z <= -8.0 m/s²).
 * Si el usuario se encuentra en horario de clase, activa el MODO SILENCIO REAL del sistema
 * para todo el teléfono (llamadas, notificaciones, sonidos del sistema).
 * Al levantar el móvil boca arriba, restaura el sonido normal del sistema.
 */
public class DetectorBocaAbajo implements SensorEventListener {

    private static final float ALFA = 0.8f;
    private static final float UMBRAL_BOCA_ABAJO_Z = -8.0f; // Eje Z negativo = boca abajo
    private static final float UMBRAL_FLAT_XY = 3.5f;        // Plano horizontal
    private static final long TIEMPO_ESTABLE_MS = 800;      // Tiempo continuo boca abajo para confirmar
    private static final long ENFRIAMIENTO_MS = 5000;       // Enfriamiento entre avisos

    private final Context context;
    private final SensorManager sensorManager;
    private final Sensor acelerometro;

    private float zFiltrado;
    private float xFiltrado;
    private float yFiltrado;
    private boolean primeraLectura;

    private long tiempoInicioBocaAbajo = 0;
    private boolean activadoModoSilencio = false;
    private long ultimoDisparo = 0;

    public DetectorBocaAbajo(Context context) {
        this.context = context.getApplicationContext();
        this.sensorManager = (SensorManager) this.context.getSystemService(Context.SENSOR_SERVICE);
        this.acelerometro = sensorManager != null ? sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) : null;
    }

    public boolean disponible() {
        return acelerometro != null;
    }

    public void iniciar() {
        if (acelerometro == null || sensorManager == null) return;
        primeraLectura = true;
        activadoModoSilencio = false;
        sensorManager.registerListener(this, acelerometro, SensorManager.SENSOR_DELAY_UI);
    }

    public void parar() {
        if (acelerometro != null && sensorManager != null) {
            sensorManager.unregisterListener(this);
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
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

        boolean estaBocaAbajo = (zFiltrado <= UMBRAL_BOCA_ABAJO_Z)
                && (Math.abs(xFiltrado) <= UMBRAL_FLAT_XY)
                && (Math.abs(yFiltrado) <= UMBRAL_FLAT_XY);

        long ahora = SystemClock.elapsedRealtime();

        if (estaBocaAbajo) {
            if (tiempoInicioBocaAbajo == 0) {
                tiempoInicioBocaAbajo = ahora;
            } else if (!activadoModoSilencio && (ahora - tiempoInicioBocaAbajo >= TIEMPO_ESTABLE_MS)
                    && (ahora - ultimoDisparo > ENFRIAMIENTO_MS)) {

                // Comprobar si el usuario está en clase según su horario
                String correo = context.getSharedPreferences("SesionUsuario", Context.MODE_PRIVATE)
                        .getString("CORREO", null);
                LocalizadorUsuario.SesionActual sesion = LocalizadorUsuario.sesionEnCurso(
                        context, correo, Calendar.getInstance());

                if (sesion != null) {
                    activadoModoSilencio = true;
                    ultimoDisparo = ahora;
                    activarModoSilencioSistema(sesion);
                }
            }
        } else {
            if (activadoModoSilencio) {
                activadoModoSilencio = false;
            }
            tiempoInicioBocaAbajo = 0;
        }
    }

    private void activarModoSilencioSistema(LocalizadorUsuario.SesionActual sesion) {
        AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        if (audioManager != null) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (notificationManager != null && notificationManager.isNotificationPolicyAccessGranted()) {
                        notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_NONE);
                        audioManager.setRingerMode(AudioManager.RINGER_MODE_SILENT);
                    } else {
                        // Muta los canales del sistema directamente
                        audioManager.setRingerMode(AudioManager.RINGER_MODE_SILENT);
                        audioManager.adjustStreamVolume(AudioManager.STREAM_RING, AudioManager.ADJUST_MUTE, 0);
                        audioManager.adjustStreamVolume(AudioManager.STREAM_NOTIFICATION, AudioManager.ADJUST_MUTE, 0);
                        audioManager.adjustStreamVolume(AudioManager.STREAM_SYSTEM, AudioManager.ADJUST_MUTE, 0);

                        // Abrir ajuste de No Molestar para conceder acceso permanente si no se ha otorgado
                        Intent intent = new Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        context.startActivity(intent);
                        Toast.makeText(context, R.string.boca_abajo_permiso_dnd, Toast.LENGTH_LONG).show();
                    }
                } else {
                    audioManager.setRingerMode(AudioManager.RINGER_MODE_SILENT);
                }
            } catch (Exception e) {
                try {
                    audioManager.setStreamVolume(AudioManager.STREAM_RING, 0, 0);
                    audioManager.setStreamVolume(AudioManager.STREAM_NOTIFICATION, 0, 0);
                    audioManager.setStreamVolume(AudioManager.STREAM_SYSTEM, 0, 0);
                } catch (Exception ignored) {}
            }
        }

        String asignaturaConAula = sesion.nombreAsignatura + (sesion.aula != null ? (" (" + sesion.aula + ")") : "");
        String msg = context.getString(R.string.boca_abajo_silenciado_fmt, asignaturaConAula);
        Toast.makeText(context, msg, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}
}