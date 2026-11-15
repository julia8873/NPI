package com.example.nuietsiit.data;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.HashSet;
import java.util.Set;

/**
 * Actividades "guardadas para luego" (inclinar el móvil hacia ti en Actividades): sin apuntarse, solo para
 * tenerlas a mano en la pestaña «Guardadas». Se guardan en el móvil por correo, como ComunidadesUnidas:
 * si entra otra cuenta, no ve las del anterior. Sin sesión también se puede guardar (cuenta "").
 */
public final class ActividadesGuardadas {

    private ActividadesGuardadas() {}

    private static final String PREFS = "actividades_guardadas";

    private static SharedPreferences prefs(Context c) {
        return c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    private static String clave(String correo) {
        return "guardadas_" + (correo == null ? "" : correo.toLowerCase());
    }

    /** Ids (como texto) de las actividades guardadas por ese usuario. */
    public static Set<String> guardadas(Context c, String correo) {
        return new HashSet<>(prefs(c).getStringSet(clave(correo), new HashSet<>()));
    }

    public static void poner(Context c, String correo, int idActividad, boolean guardada) {
        Set<String> s = guardadas(c, correo);
        if (guardada) s.add(String.valueOf(idActividad));
        else s.remove(String.valueOf(idActividad));
        prefs(c).edit().putStringSet(clave(correo), s).apply();
    }
}
