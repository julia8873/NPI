package com.example.nuietsiit.util;

import android.content.Intent;

import com.example.nuietsiit.data.TablasActividades;

/**
 * Sesión del usuario entre Activities (extra "CORREO").
 */
public final class SesionUtil {

    private SesionUtil() {
    }

    // correo del extra "CORREO"; sin sesión (null o vacío) = usuario invitado
    public static String correo(Intent intent) {
        String correo = intent.getStringExtra("CORREO");
        return correo == null || correo.isEmpty() ? TablasActividades.CORREO_INVITADO : correo;
    }
}
