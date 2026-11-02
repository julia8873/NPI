package com.example.nuietsiit.util;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.provider.CalendarContract;

import java.time.LocalDateTime;

/**
 * Abre la app de calendario con un evento ya rellenado para que el usuario lo confirme.
 * Sin permisos de calendario (no se escribe directamente en él).
 * Mismo patrón que Actividades.anadirACalendario.
 */
public class CalendarioUtil {

    // false si no hay ninguna app de calendario
    public static boolean anadirEvento(Context ctx, String titulo, String descripcion, String lugar,
                                       LocalDateTime inicio, LocalDateTime fin) {
        Intent intent = new Intent(Intent.ACTION_INSERT)
                .setData(CalendarContract.Events.CONTENT_URI)
                .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, FechasUtil.millis(inicio))
                .putExtra(CalendarContract.EXTRA_EVENT_END_TIME, FechasUtil.millis(fin))
                .putExtra(CalendarContract.Events.TITLE, titulo)
                .putExtra(CalendarContract.Events.DESCRIPTION, descripcion)
                .putExtra(CalendarContract.Events.EVENT_LOCATION, lugar);
        try {
            ctx.startActivity(intent);
            return true;
        } catch (ActivityNotFoundException e) {
            return false;
        }
    }
}
