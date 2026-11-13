package com.example.nuietsiit.util;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.nuietsiit.R;

/**
 * Utilidad para guardar y recuperar exactamente la última subpágina/pantalla visitada por el usuario.
 */
public final class RecienteUtil {
    private static final String PREF = "pagina_reciente_pref";
    private static final String KEY_CLASS_NAME = "class_name";
    private static final String KEY_TITULO = "titulo_pagina";
    private static final String KEY_SUBTITULO = "subtitulo_pagina";
    private static final String KEY_EXTRA_KEY = "extra_key";
    private static final String KEY_EXTRA_VAL = "extra_val";
    private static final String KEY_SECCION_INDEX = "seccion_index";

    private RecienteUtil() {}

    public static void guardar(Context context, Class<?> activityClass, String titulo, String subtitulo, String extraKey, String extraVal, int seccionIndex) {
        if (context == null) return;
        SharedPreferences prefs = context.getSharedPreferences(PREF, Context.MODE_PRIVATE);
        prefs.edit()
                .putString(KEY_CLASS_NAME, activityClass.getName())
                .putString(KEY_TITULO, titulo != null ? titulo : context.getString(R.string.reciente_titulo_defecto))
                .putString(KEY_SUBTITULO, subtitulo != null ? subtitulo : context.getString(R.string.reciente_subtitulo_defecto))
                .putString(KEY_EXTRA_KEY, extraKey != null ? extraKey : "")
                .putString(KEY_EXTRA_VAL, extraVal != null ? extraVal : "")
                .putInt(KEY_SECCION_INDEX, seccionIndex)
                .apply();
    }

    public static class DatosRecientes {
        public String className;
        public String titulo;
        public String subtitulo;
        public String extraKey;
        public String extraVal;
        public int seccionIndex;

        public DatosRecientes(String className, String titulo, String subtitulo, String extraKey, String extraVal, int seccionIndex) {
            this.className = className;
            this.titulo = titulo;
            this.subtitulo = subtitulo;
            this.extraKey = extraKey;
            this.extraVal = extraVal;
            this.seccionIndex = seccionIndex;
        }
    }

    public static DatosRecientes obtener(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF, Context.MODE_PRIVATE);
        String className = prefs.getString(KEY_CLASS_NAME, null);
        String titulo = prefs.getString(KEY_TITULO, context.getString(R.string.reciente_asignaturas_defecto));
        String subtitulo = prefs.getString(KEY_SUBTITULO, context.getString(R.string.reciente_subtitulo_defecto));
        String extraKey = prefs.getString(KEY_EXTRA_KEY, "");
        String extraVal = prefs.getString(KEY_EXTRA_VAL, "");
        int seccionIndex = prefs.getInt(KEY_SECCION_INDEX, 2); // Asignaturas por defecto
        return new DatosRecientes(className, titulo, subtitulo, extraKey, extraVal, seccionIndex);
    }
}