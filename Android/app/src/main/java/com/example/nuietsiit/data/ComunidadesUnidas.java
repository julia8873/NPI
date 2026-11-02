package com.example.nuietsiit.data;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.HashSet;
import java.util.Set;

/**
 * Guarda en el móvil a qué comunidades de chat se ha unido el usuario (simulado).
 * Funciona igual que AnunciosLeidos.
 */
public final class ComunidadesUnidas {

    private ComunidadesUnidas() {}

    private static final String PREFS = "comunidades";
    private static final String CLAVE = "unidas";

    private static SharedPreferences prefs(Context c) {
        return c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public static Set<String> unidas(Context c) {
        return new HashSet<>(prefs(c).getStringSet(CLAVE, new HashSet<>()));
    }

    public static boolean estaUnido(Context c, int idComunidad) {
        return unidas(c).contains(String.valueOf(idComunidad));
    }

    /** Si no estaba unido, le une; si ya lo estaba, le saca. */
    public static void alternar(Context c, int idComunidad) {
        Set<String> s = unidas(c);
        String id = String.valueOf(idComunidad);
        if (!s.remove(id)) s.add(id);
        prefs(c).edit().putStringSet(CLAVE, s).apply();
    }
}
