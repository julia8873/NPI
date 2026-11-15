package com.example.nuietsiit.util;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.example.nuietsiit.R;
import com.example.nuietsiit.activities.secciones.Actividades;
import com.example.nuietsiit.activities.secciones.mapa.Mapa;
import com.example.nuietsiit.data.ActividadesDB;
import com.example.nuietsiit.data.DBHelper;
import com.example.nuietsiit.data.modelo.Actividad;

import java.time.LocalDateTime;

/**
 * Recordatorio de una actividad a la que te has apuntado: ANTES_MIN minutos antes salta una notificación
 * con el botón "Llévame", que abre el mapa con la ruta hasta el sitio (igual que "Llévame" del detalle).
 * Usa AlarmManager, así que avisa aunque la app esté cerrada (como AvisoSalidaClase).
 * - Al apuntarse: programar(). Al desapuntarse: cancelar().
 * - Al abrir la app (Notificador.revisarAvisos): reprogramarTodas(), porque al reiniciar el móvil se borran.
 */
public final class RecordatorioActividad {

    private RecordatorioActividad() {}

    static final int ANTES_MIN = 30;
    private static final String EXTRA_ID = "ID_ACTIVIDAD";
    private static final int BASE_CODIGO = 5000; // códigos de alarma y de notificación: 5000 + id

    /** Programa el aviso. Si faltan menos de ANTES_MIN minutos, avisa ya; si ya ha empezado, nada. */
    public static void programar(Context ctx, Actividad a) {
        if (a.inicio == null || !a.inicio.isAfter(LocalDateTime.now())) return;
        AlarmManager am = ctx.getSystemService(AlarmManager.class);
        if (am == null) return;
        LocalDateTime cuando = a.inicio.minusMinutes(ANTES_MIN);
        am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, FechasUtil.millis(cuando), alarma(ctx, a.id));
    }

    public static void cancelar(Context ctx, int idActividad) {
        AlarmManager am = ctx.getSystemService(AlarmManager.class);
        if (am != null) am.cancel(alarma(ctx, idActividad));
    }

    /** Vuelve a programar las de todas las actividades futuras a las que está apuntado el usuario. */
    public static void reprogramarTodas(Context ctx) {
        String correo = correoSesion(ctx);
        if (correo == null) return;
        for (Actividad a : new ActividadesDB(DBHelper.getInstance(ctx))
                .listarActividadesConDetalles(correo, FechasUtil.hoyBD())) {
            if (a.inscrito) programar(ctx, a);
        }
    }

    private static PendingIntent alarma(Context ctx, int idActividad) {
        Intent intent = new Intent(ctx, Receptor.class).putExtra(EXTRA_ID, idActividad);
        return PendingIntent.getBroadcast(ctx, BASE_CODIGO + idActividad, intent,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
    }

    private static String correoSesion(Context ctx) {
        String correo = ctx.getSharedPreferences("SesionUsuario", Context.MODE_PRIVATE).getString("CORREO", null);
        return correo == null || correo.isEmpty() ? null : correo;
    }

    /** Salta con la alarma: comprueba que sigue apuntado y enseña la notificación. */
    public static class Receptor extends BroadcastReceiver {
        @Override
        public void onReceive(Context ctx, Intent intent) {
            int id = intent.getIntExtra(EXTRA_ID, -1);
            String correo = correoSesion(ctx);
            if (id < 0 || correo == null) return;

            Actividad a = null;
            for (Actividad x : new ActividadesDB(DBHelper.getInstance(ctx))
                    .listarActividadesConDetalles(correo, FechasUtil.hoyBD())) {
                if (x.id == id) a = x;
            }
            // se desapuntó, cerró sesión o ya ha empezado: no se avisa
            if (a == null || !a.inscrito || !a.inicio.isAfter(LocalDateTime.now())) return;

            long minutos = Math.max(1, java.time.Duration.between(LocalDateTime.now(), a.inicio).toMinutes());
            String sitio = a.sitioEnMapa();
            String titulo = ctx.getString(R.string.nui_rec_titulo_fmt, (int) minutos, a.titulo);
            String texto = ctx.getString(R.string.nui_rec_texto_fmt, FechasUtil.hora(a.inicio),
                    sitio != null ? sitio : ctx.getString(R.string.nui_rec_escuela));

            Intent abrir = new Intent(ctx, Actividades.class);
            Intent llevame = new Intent(ctx, Mapa.class).putExtra("CORREO", correo);
            if (sitio != null) llevame.putExtra("RUTA_A_MARCADOR", sitio);

            Notificador.crearCanal(ctx);
            Notificador.mostrar(ctx, BASE_CODIGO + id, titulo, texto, abrir, ctx.getString(R.string.nui_rec_llevame), llevame);
        }
    }
}
