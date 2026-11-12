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
 * Al levantar el móvil boca arriba, restaura el estado de sonido anterior del sistema.
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

    // Estado previo del teléfono para restaurarlo al levantarlo
    private int ringerModePrevio = AudioManager.RINGER_MODE_NORMAL;
    private int interruptionFilterPrevio = NotificationManager.INTERRUPTION_FILTER_ALL;

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
                    if (activarModoSilencioSistema(sesion)) {
                        activadoModoSilencio = true;
                        ultimoDisparo = ahora;
                    } else {
                        // Evitar abrir en bucle los ajustes si no tiene permiso
                        ultimoDisparo = ahora;
                        tiempoInicioBocaAbajo = 0;
                    }
                }
            }
        } else {
            // Si estaba silenciado por este detector y el usuario levanta el móvil (Z > 0)
            if (activadoModoSilencio && zFiltrado > 2.0f) {
                activadoModoSilencio = false;
                restaurarModoSonidoSistema();
            }
            tiempoInicioBocaAbajo = 0;
        }
    }

    /**
     * Silencia por completo llamadas, notificaciones y sistema.
     * @return true si se pudo silenciar, false si falta permiso de No Molestar.
     */
    private boolean activarModoSilencioSistema(LocalizadorUsuario.SesionActual sesion) {
        AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        if (audioManager == null) return false;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (notificationManager == null || !notificationManager.isNotificationPolicyAccessGranted()) {
                // Pedir permiso de Acceso a No Molestar SIN llamar antes a setRingerMode (que lanzaría SecurityException)
                Intent intent = new Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
                Toast.makeText(context, R.string.boca_abajo_permiso_dnd, Toast.LENGTH_LONG).show();
                return false;
            }

            // Guardar estado previo para poder restaurarlo después
            ringerModePrevio = audioManager.getRingerMode();
            interruptionFilterPrevio = notificationManager.getCurrentInterruptionFilter();

            // Silencio total del sistema (corta llamadas, tono y vibración)
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