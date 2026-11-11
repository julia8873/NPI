package com.example.nuietsiit.activities.secciones.secretaria;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.nuietsiit.data.SecretariaDB;
import com.example.nuietsiit.data.modelo.Tramite;
import com.example.nuietsiit.util.tutorial.Tutorial;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

/**
 * Cita y solicitud de mentira para poder practicar el tutorial cuando el usuario aún no tiene ninguna.
 * Sus ids se apuntan en las preferencias del tutorial: así se borran aunque la app se cierre a mitad.
 */
final class DatosPractica {

    private static final String CITA = "practica_cita", SOLICITUD = "practica_solicitud";
    static final String NOTA = "Solicitud de práctica del tutorial";

    private DatosPractica() {}

    /** Crea una cita futura (que se puede rasgar) si no hay ninguna que cancelar. */
    static void crearCita(Context ctx, SecretariaDB db, String correo) {
        if (prefs(ctx).getInt(CITA, -1) != -1) return;
        LocalDate dia = LocalDate.now().plusDays(7);
        while (dia.getDayOfWeek() == DayOfWeek.SATURDAY || dia.getDayOfWeek() == DayOfWeek.SUNDAY) dia = dia.plusDays(1);
        Set<String> ocupadas = db.getHorasOcupadas(dia.toString());
        String hora = "09:00";
        for (int m = 9 * 60; m < 14 * 60; m += 10) {
            hora = String.format(java.util.Locale.ROOT, "%02d:%02d", m / 60, m % 60);
            if (!ocupadas.contains(hora)) break;
        }
        long id = db.insertarCita(correo, null, dia.toString(), hora);
        if (id != -1) prefs(ctx).edit().putInt(CITA, (int) id).apply();
    }

    /** Crea una solicitud ya enviada (con su justificante) si no hay ninguna. */
    static void crearSolicitud(Context ctx, SecretariaDB db, String correo) {
        if (prefs(ctx).getInt(SOLICITUD, -1) != -1) return;
        List<Tramite> tramites = db.getTramites(null);
        if (tramites.isEmpty()) return;
        Tramite elegido = tramites.get(0);
        for (Tramite t : tramites) if (t.sePuedeSolicitar()) { elegido = t; break; }
        long id = db.insertarSolicitud(correo, elegido.id, NOTA, null, null);
        if (id != -1) prefs(ctx).edit().putInt(SOLICITUD, (int) id).apply();
    }

    static boolean hay(Context ctx) {
        return prefs(ctx).getInt(CITA, -1) != -1 || prefs(ctx).getInt(SOLICITUD, -1) != -1;
    }

    static boolean esPractica(Context ctx, boolean esCita, int id) {
        return prefs(ctx).getInt(esCita ? CITA : SOLICITUD, -1) == id;
    }

    /** Quita lo que se creó para practicar (si no hay nada, no hace nada). */
    static void borrar(Context ctx, SecretariaDB db) {
        SharedPreferences p = prefs(ctx);
        int cita = p.getInt(CITA, -1), solicitud = p.getInt(SOLICITUD, -1);
        if (cita != -1) db.borrarCita(cita);
        if (solicitud != -1) db.borrarSolicitud(solicitud);
        p.edit().remove(CITA).remove(SOLICITUD).apply();
    }

    private static SharedPreferences prefs(Context ctx) {
        return ctx.getSharedPreferences(Tutorial.PREFS, Context.MODE_PRIVATE);
    }
}
