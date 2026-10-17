package com.example.nuietsiit.data;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.HashSet;
import java.util.Set;

/**
 * Guarda en el móvil qué anuncios se han leído (SharedPreferences).
 * No hace falta tocar la BD: "leído" es algo de cada dispositivo.
 */
public final class AnunciosLeidos {

    private AnunciosLeidos() {}

    private static final String PREFS = "anuncios";
    private static final String CLAVE = "leidos";

    private static SharedPreferences prefs(Context c) {
        return c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    /** Copia del conjunto de ids leídos (como texto). */
    public static Set<String> leidos(Context c) {
        return new HashSet<>(prefs(c).getStringSet(CLAVE, new HashSet<>()));
    }

    public static void marcar(Context c, int idAnuncio) {
        Set<String> s = leidos(c);
        s.add(String.valueOf(idAnuncio));
        prefs(c).edit().putStringSet(CLAVE, s).apply();
    }

    public static void marcarVarios(Context c, Iterable<Integer> ids) {
        Set<String> s = leidos(c);
        for (int id : ids) s.add(String.valueOf(id));
        prefs(c).edit().putStringSet(CLAVE, s).apply();
    }
}
