package com.example.nuietsiit.data;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.HashSet;
import java.util.Set;

/**
 * Avisos "guardados" en Noticias (inclinar el móvil hacia ti o tocar el marcapáginas), para tenerlos a mano en la
 * pestaña «Guardados»: un plazo, una beca… Igual que ActividadesGuardadas: en el móvil y por correo (si entra
 * otra cuenta, no ve los del anterior). Sin sesión también se puede guardar (cuenta "").
 */
public final class AnunciosGuardados {

    private AnunciosGuardados() {}

    private static final String PREFS = "anuncios_guardados";

    private static SharedPreferences prefs(Context c) {
        return c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    private static String clave(String correo) {
        return "guardados_" + (correo == null ? "" : correo.toLowerCase());
    }

    /** Ids (como texto) de los avisos guardados por ese usuario. */
    public static Set<String> guardados(Context c, String correo) {
        return new HashSet<>(prefs(c).getStringSet(clave(correo), new HashSet<>()));
    }

    public static void poner(Context c, String correo, int idAnuncio, boolean guardado) {
        Set<String> s = guardados(c, correo);
        if (guardado) s.add(String.valueOf(idAnuncio));
        else s.remove(String.valueOf(idAnuncio));
        prefs(c).edit().putStringSet(clave(correo), s).apply();
    }
}
