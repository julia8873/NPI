package com.example.nuietsiit.data;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.HashSet;
import java.util.Set;

/**
 * Guarda en el móvil a qué comunidades de chat se ha unido cada usuario (simulado).
 * Va por correo: si en el móvil entra otra cuenta, no ve las comunidades del anterior.
 */
public final class ComunidadesUnidas {

    private ComunidadesUnidas() {}

    private static final String PREFS = "comunidades";
    private static final String CLAVE = "unidas";

    private static SharedPreferences prefs(Context c) {
        return c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    private static String clave(String correo) {
        return CLAVE + "_" + (correo == null ? "" : correo.toLowerCase());
    }

    public static Set<String> unidas(Context c, String correo) {
        return new HashSet<>(prefs(c).getStringSet(clave(correo), new HashSet<>()));
    }

    public static boolean estaUnido(Context c, String correo, int idComunidad) {
        return unidas(c, correo).contains(String.valueOf(idComunidad));
    }

    /** Si no estaba unido, le une; si ya lo estaba, le saca. */
    public static void alternar(Context c, String correo, int idComunidad) {
        Set<String> s = unidas(c, correo);
        String id = String.valueOf(idComunidad);
        if (!s.remove(id)) s.add(id);
        prefs(c).edit().putStringSet(clave(correo), s).apply();
    }
}
