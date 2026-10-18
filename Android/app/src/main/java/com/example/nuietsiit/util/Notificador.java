package com.example.nuietsiit.util;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import com.example.nuietsiit.activities.secciones.Actividades;
import com.example.nuietsiit.activities.secciones.Noticias;
import com.example.nuietsiit.data.ActividadesDB;
import com.example.nuietsiit.data.AnunciosDB;
import com.example.nuietsiit.data.AnunciosLeidos;
import com.example.nuietsiit.data.DBHelper;
import com.example.nuietsiit.data.TablasActividades;
import com.example.nuietsiit.data.modelo.Actividad;
import com.example.nuietsiit.data.modelo.Anuncio;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * Notificaciones reales del sistema (las que salen arriba en el móvil):
 *  - Avisos urgentes que aún no has leído.
 *  - Actividades a las que estás apuntado que son hoy o mañana.
 * Cada aviso se notifica una sola vez. Se llama desde MenuPrincipal al abrir la app.
 */
public final class Notificador {

    private Notificador() {}

    private static final String CANAL = "avisos_etsiit";
    public static final int CODIGO_PERMISO = 4321;
    private static final String PREFS = "notificaciones";
    private static final String CLAVE = "enviadas";

    public static void revisarAvisos(Activity act, String correo) {
        crearCanal(act);

        // Android 13+ pide permiso para mostrar notificaciones
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(act,
                Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(act,
                    new String[]{Manifest.permission.POST_NOTIFICATIONS}, CODIGO_PERMISO);
            return; // se vuelve a llamar desde onRequestPermissionsResult si lo acepta
        }

        if (correo == null || correo.isEmpty()) correo = TablasActividades.CORREO_INVITADO;
        DBHelper bd = DBHelper.getInstance(act);
        SharedPreferences prefs = act.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        Set<String> enviadas = new HashSet<>(prefs.getStringSet(CLAVE, new HashSet<>()));
        Set<String> leidos = AnunciosLeidos.leidos(act);

        // 1) Anuncios urgentes sin leer
        for (Anuncio a : new AnunciosDB(bd).obtenerAnunciosGenerales(FechasUtil.ahoraBD())) {
            String clave = "A" + a.id;
            if (!a.esUrgente() || leidos.contains(String.valueOf(a.id)) || enviadas.contains(clave)) continue;
            Intent abrir = new Intent(act, Noticias.class).putExtra("CORREO", correo);
            mostrar(act, 1000 + a.id, "🚨 " + a.titulo, a.contenido, abrir);
            enviadas.add(clave);
        }

        // 2) Actividades apuntadas que son hoy o mañana
        LocalDate manana = LocalDate.now().plusDays(1);
        LocalDateTime ahora = LocalDateTime.now();
        for (Actividad x : new ActividadesDB(bd).listarActividadesConDetalles(correo, FechasUtil.hoyBD())) {
            String clave = "C" + x.id;
            if (!x.inscrito || enviadas.contains(clave)) continue;
            if (x.inicio.isBefore(ahora) || x.inicio.toLocalDate().isAfter(manana)) continue;
            Intent abrir = new Intent(act, Actividades.class).putExtra("CORREO", correo);
            mostrar(act, 2000 + x.id,
                    "⏰ " + FechasUtil.cuando(x.inicio) + " a las " + FechasUtil.hora(x.inicio) + ": " + x.titulo,
                    "Te apuntaste a esta actividad. ¡Que no se te olvide!", abrir);
            enviadas.add(clave);
        }

        prefs.edit().putStringSet(CLAVE, enviadas).apply();
    }

    /** Llamar desde onRequestPermissionsResult de la Activity. */
    public static void alResponderPermiso(Activity act, int codigo, int[] resultados, String correo) {
        if (codigo == CODIGO_PERMISO && resultados.length > 0
                && resultados[0] == PackageManager.PERMISSION_GRANTED) {
            revisarAvisos(act, correo);
        }
    }

    private static void crearCanal(Context c) {
        NotificationChannel canal = new NotificationChannel(CANAL, "Avisos de la ETSIIT",
                NotificationManager.IMPORTANCE_HIGH);
        canal.setDescription("Avisos urgentes y recordatorios de actividades");
        c.getSystemService(NotificationManager.class).createNotificationChannel(canal);
    }

    @SuppressLint("MissingPermission") // el permiso se comprueba en revisarAvisos
    private static void mostrar(Context c, int id, String titulo, String texto, Intent intent) {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pi = PendingIntent.getActivity(c, id, intent,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder n = new NotificationCompat.Builder(c, CANAL)
                .setSmallIcon(android.R.drawable.ic_popup_reminder)
                .setColor(0xFFFF8C42)
                .setContentTitle(titulo)
                .setContentText(texto)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(texto))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pi);
        try {
            NotificationManagerCompat.from(c).notify(id, n.build());
        } catch (SecurityException ignorada) {
            // sin permiso: no se muestra
        }
    }
}
