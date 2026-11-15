package com.example.nuietsiit.activities.secciones.mapa;

import android.content.Context;
import android.content.SharedPreferences;
import android.speech.tts.TextToSpeech;

import java.util.Locale;

/**
 * Voz de la guía exterior (TextToSpeech, no necesita permisos): lee los avisos y las instrucciones.
 * Encendida o apagada se recuerda entre usos (prefs "guia_voz").
 */
public final class VozGuia {

    private static final String PREFS = "guia_voz", ACTIVA = "activa";

    private final SharedPreferences prefs;
    private TextToSpeech tts;
    private boolean lista, activa;
    private String ultima = "";

    public VozGuia(Context ctx) {
        prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        activa = prefs.getBoolean(ACTIVA, true);
        tts = new TextToSpeech(ctx.getApplicationContext(), estado -> {
            if (estado != TextToSpeech.SUCCESS || tts == null) return;
            int r = tts.setLanguage(new Locale("es", "ES"));
            lista = r != TextToSpeech.LANG_MISSING_DATA && r != TextToSpeech.LANG_NOT_SUPPORTED;
        });
    }

    public boolean activa() {
        return activa;
    }

    public void activar(boolean si) {
        activa = si;
        prefs.edit().putBoolean(ACTIVA, si).apply();
        if (!si) callar();
    }

    /** Dice el texto (corta lo que estuviera diciendo). No repite la misma frase seguida. */
    public void decir(String texto) {
        if (!activa || !lista || texto == null || texto.isEmpty() || texto.equals(ultima)) return;
        ultima = texto;
        tts.speak(limpiar(texto), TextToSpeech.QUEUE_FLUSH, null, "guia");
    }

    /** Como decir, pero espera a que acabe lo anterior y sí repite (avisos importantes). */
    public void avisar(String texto) {
        if (!activa || !lista || texto == null || texto.isEmpty()) return;
        ultima = texto;
        tts.speak(limpiar(texto), TextToSpeech.QUEUE_ADD, null, "aviso");
    }

    public void callar() {
        if (tts != null) tts.stop();
        ultima = "";
    }

    public void cerrar() {
        if (tts == null) return;
        tts.shutdown();
        tts = null;
        lista = false;
    }

    /** Sin emojis ni flechas, que el TTS los lee por su nombre ("bandera a cuadros"). */
    static String limpiar(String texto) {
        return texto.replaceAll("[^\\p{L}\\p{N}\\p{P}\\s]", "").replaceAll("\\s+", " ").trim();
    }
}
