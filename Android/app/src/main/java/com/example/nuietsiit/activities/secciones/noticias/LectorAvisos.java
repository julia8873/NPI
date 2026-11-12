package com.example.nuietsiit.activities.secciones.noticias;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioAttributes;
import android.media.AudioDeviceInfo;
import android.media.AudioManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.os.SystemClock;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Lee los avisos en voz alta (TextToSpeech, no necesita permisos) usando dos sensores:
 *  - PROXIMIDAD: al acercarte el móvil a la oreja empieza a leer (o sigue) por el AURICULAR, como una
 *    llamada o un audio de WhatsApp, y la pantalla se apaga para no tocar nada con la cara.
 *    Al separarlo, si había empezado por la oreja se para; si no, sigue por el altavoz.
 *  - ACELERÓMETRO: si pones el móvil boca abajo, se calla.
 * Quien lo usa (Noticias) le pasa los textos y se entera de qué aviso está leyendo (Oyente).
 */
public final class LectorAvisos implements SensorEventListener {

    public interface Oyente {
        /** Empieza a leer el aviso número indice (de la lista que se pasó a leer). */
        void alLeer(int indice);

        /** Ha terminado o se ha parado. */
        void alTerminar();

        /** Se ha acercado a la oreja sin estar leyendo: quien lo usa decide qué leer (llamando a leer). */
        void alAcercarOreja();

        /** Se ha puesto boca abajo mientras leía (ya se ha callado). */
        void alBocaAbajo();

        /** Se ha acercado (true) o separado (false) de la oreja, esté leyendo o no. */
        default void alCambiarOreja(boolean cerca) {}
    }

    private static final float BOCA_ABAJO_Z = -8f;   // gravedad contra la pantalla
    private static final long BOCA_ABAJO_MS = 600;   // tiempo seguido boca abajo para que cuente

    private final Context ctx;
    private final Oyente oyente;
    private final AudioManager audio;
    private final SensorManager sensores;
    private final Sensor proximidad, acelerometro;
    private final PowerManager.WakeLock apagarPantalla;
    private final Handler hilo = new Handler(Looper.getMainLooper());

    private TextToSpeech tts;
    private boolean listo;
    private List<String> textos = new ArrayList<>();
    private int actual = -1;
    private boolean leyendo, enOreja, empezoPorOreja;
    private long bocaAbajoDesde;

    public LectorAvisos(Context context, Oyente oyente) {
        this.ctx = context.getApplicationContext();
        this.oyente = oyente;
        audio = ctx.getSystemService(AudioManager.class);
        sensores = ctx.getSystemService(SensorManager.class);
        proximidad = sensores == null ? null : sensores.getDefaultSensor(Sensor.TYPE_PROXIMITY);
        acelerometro = sensores == null ? null : sensores.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        PowerManager energia = ctx.getSystemService(PowerManager.class);
        apagarPantalla = energia != null && energia.isWakeLockLevelSupported(PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK)
                ? energia.newWakeLock(PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK, "nuietsiit:avisosOreja")
                : null;

        tts = new TextToSpeech(ctx, estado -> {
            if (estado != TextToSpeech.SUCCESS || tts == null) return;
            int r = tts.setLanguage(new Locale("es", "ES"));
            listo = r != TextToSpeech.LANG_MISSING_DATA && r != TextToSpeech.LANG_NOT_SUPPORTED;
            tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                @Override public void onStart(String id) {
                    int i = indice(id);
                    if (i >= 0) hilo.post(() -> { actual = i; oyente.alLeer(i); });
                }

                @Override public void onDone(String id) {
                    if (indice(id) == textos.size() - 1) hilo.post(LectorAvisos.this::terminar);
                }

                @Override public void onError(String id) {
                    hilo.post(LectorAvisos.this::terminar);
                }
            });
        });
    }

    public boolean disponible() {
        return listo;
    }

    public boolean leyendo() {
        return leyendo;
    }

    public boolean hayProximidad() {
        return proximidad != null;
    }

    /** Lee los textos uno detrás de otro, desde el primero. */
    public void leer(List<String> nuevos) {
        if (!listo || nuevos.isEmpty()) return;
        textos = new ArrayList<>(nuevos);
        leyendo = true;
        if (enOreja && apagarPantalla != null && !apagarPantalla.isHeld()) apagarPantalla.acquire(10 * 60 * 1000L);
        hablarDesde(0);
    }

    public void parar() {
        if (!leyendo) return;
        if (tts != null) tts.stop();
        terminar();
    }

    /** Llamar en onResume / onPause de la pantalla. */
    public void iniciarSensores() {
        if (proximidad != null) sensores.registerListener(this, proximidad, SensorManager.SENSOR_DELAY_NORMAL);
        if (acelerometro != null) sensores.registerListener(this, acelerometro, SensorManager.SENSOR_DELAY_UI);
    }

    public void pararSensores() {
        if (sensores != null) sensores.unregisterListener(this);
        parar();
        enOreja = false;
        usarAuricular(false);
    }

    public void cerrar() {
        pararSensores();
        if (tts != null) tts.shutdown();
        tts = null;
        listo = false;
    }

    // ---------- sensores ----------

    @Override
    public void onSensorChanged(SensorEvent e) {
        if (e.sensor.getType() == Sensor.TYPE_PROXIMITY) {
            // "cerca" = menos que el alcance máximo (muchos sensores solo dan 0 o el máximo)
            boolean cerca = e.values[0] < Math.min(5f, e.sensor.getMaximumRange());
            if (cerca != enOreja) alCambiarOreja(cerca);
        } else if (e.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            if (e.values[2] > BOCA_ABAJO_Z) {
                bocaAbajoDesde = 0;
            } else if (bocaAbajoDesde == 0) {
                bocaAbajoDesde = SystemClock.elapsedRealtime();
            } else if (leyendo && SystemClock.elapsedRealtime() - bocaAbajoDesde > BOCA_ABAJO_MS) {
                parar();
                oyente.alBocaAbajo();
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    private void alCambiarOreja(boolean cerca) {
        enOreja = cerca;
        usarAuricular(cerca);
        oyente.alCambiarOreja(cerca);
        if (cerca) {
            if (leyendo) {
                // sigue por el auricular desde el aviso que iba
                if (apagarPantalla != null && !apagarPantalla.isHeld()) apagarPantalla.acquire(10 * 60 * 1000L);
                hablarDesde(Math.max(actual, 0));
            } else {
                empezoPorOreja = true;
                oyente.alAcercarOreja();
            }
        } else if (leyendo) {
            if (empezoPorOreja) parar();           // como un audio de WhatsApp: al separarlo se para
            else hablarDesde(Math.max(actual, 0)); // empezó con el botón: sigue por el altavoz
        }
    }

    // ---------- voz ----------

    private void hablarDesde(int desde) {
        if (tts == null) return;
        tts.stop();
        tts.setAudioAttributes(new AudioAttributes.Builder()
                .setUsage(enOreja ? AudioAttributes.USAGE_VOICE_COMMUNICATION : AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build());
        for (int i = desde; i < textos.size(); i++) {
            tts.speak(limpiar(textos.get(i)), TextToSpeech.QUEUE_ADD, null, "aviso_" + i);
            tts.playSilentUtterance(600, TextToSpeech.QUEUE_ADD, "pausa_" + i);
        }
    }

    private void terminar() {
        boolean estaba = leyendo;
        leyendo = false;
        empezoPorOreja = false;
        actual = -1;
        if (apagarPantalla != null && apagarPantalla.isHeld()) apagarPantalla.release();
        if (estaba) oyente.alTerminar();
    }

    /** Auricular (oreja) o altavoz. */
    private void usarAuricular(boolean si) {
        if (audio == null) return;
        try {
            audio.setMode(si ? AudioManager.MODE_IN_COMMUNICATION : AudioManager.MODE_NORMAL);
            if (Build.VERSION.SDK_INT >= 31) {
                if (si) {
                    for (AudioDeviceInfo d : audio.getAvailableCommunicationDevices()) {
                        if (d.getType() == AudioDeviceInfo.TYPE_BUILTIN_EARPIECE) {
                            audio.setCommunicationDevice(d);
                            break;
                        }
                    }
                } else {
                    audio.clearCommunicationDevice();
                }
            } else {
                audio.setSpeakerphoneOn(!si);
            }
        } catch (SecurityException ignorada) {
            // sin permiso para cambiar el audio: se oye por el altavoz, que también vale
        }
    }

    private static int indice(String id) {
        if (id == null || !id.startsWith("aviso_")) return -1;
        try {
            return Integer.parseInt(id.substring(6));
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    /** Sin emojis ni símbolos raros, que el TTS los lee por su nombre. */
    private static String limpiar(String texto) {
        return texto.replaceAll("[^\\p{L}\\p{N}\\p{P}\\s]", "").replaceAll("\\s+", " ").trim();
    }
}
