package com.example.nuietsiit.util;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.util.Log;

import androidx.core.content.ContextCompat;

import com.example.nuietsiit.activities.secciones.mapa.CapaLugares;
import com.example.nuietsiit.activities.secciones.mapa.Mapa;
import com.example.nuietsiit.activities.secciones.mapa.PlanRuta;
import com.example.nuietsiit.activities.secciones.mapa.PlanificadorRuta;
import com.example.nuietsiit.activities.secciones.mapa.ProximaClase;
import com.example.nuietsiit.activities.secciones.mapa.Punto;
import com.example.nuietsiit.activities.secciones.mapa.RedTransporte;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Calendar;

/**
 * "Sal ya para llegar a clase": notificación que avisa de cuándo hay que salir para llegar a la próxima
 * clase de hoy (ETSIIT o Edificio Auxiliar), usando el planificador del mapa exterior (sin cambiarlo).
 * <ul>
 *   <li>Al abrir la app (lo llama Notificador) mira la próxima clase, calcula la ruta y programa una alarma
 *       {@link #AVISAR_ANTES_MIN} minutos antes de la hora a la que hay que salir. Si ya toca, avisa en el momento.</li>
 *   <li>Cuando salta la alarma ({@link Receptor}) vuelve a calcular con la ubicación de ese momento y avisa.</li>
 *   <li>Al pulsar la notificación se abre el mapa, que propone ir a esa clase ("¿A dónde vas?").</li>
 * </ul>
 * Cada clase se avisa una sola vez. Solo con sesión iniciada.
 */
public final class AvisoSalidaClase {

    private AvisoSalidaClase() {}

    private static final String TAG = "AvisoSalidaClase";

    /** Cuántos minutos antes de la hora de salida se avisa. */
    static final int AVISAR_ANTES_MIN = 10;
    /** Margen para no llegar justo a la hora de la clase. */
    static final int MARGEN_LLEGADA_MIN = 5;
    /** Si no se sabe dónde está el usuario, se supone que sale de "casa" (lugar de ejemplo del mapa). */
    private static final String ORIGEN_POR_DEFECTO = "livensa_fuentenueva";
    /** Una ubicación más antigua que esto no se tiene en cuenta. */
    private static final long UBICACION_VALIDA_MS = 2 * 60 * 60 * 1000L;

    private static final String PREFS = "aviso_salida";
    private static final String CLAVE_AVISADA = "ultima_avisada";
    private static final int ID_NOTIFICACION = 3000;

    /** Resultado del cálculo: la ruta y la hora a la que hay que salir como muy tarde. */
    private static final class Calculo {
        final PlanRuta plan;
        final LocalDateTime salirComoTarde;

        Calculo(PlanRuta plan, LocalDateTime salirComoTarde) {
            this.plan = plan;
            this.salirComoTarde = salirComoTarde;
        }
    }

    // -------------------------------------------------
    //              PUNTO DE ENTRADA
    // -------------------------------------------------

    /** Mira la próxima clase y avisa ahora o programa el aviso para luego. */
    public static void revisar(Context ctx) {
        String correo = correoSesion(ctx);
        if (correo == null) return; // sin sesión no hay horario

        ProximaClase clase = ProximaClase.de(ctx, correo, Calendar.getInstance());
        if (clase == null || clase.enCurso) return; // hoy no quedan clases o ya ha empezado

        // Esta clase ya se avisó
        String clave = LocalDate.now() + "|" + clase.codigoAsignatura + "|" + clase.horaInicio;
        if (clave.equals(prefs(ctx).getString(CLAVE_AVISADA, null))) return;

        Calculo calculo = calcular(ctx, clase);
        if (calculo == null) return; // ya está allí o no hay ruta

        LocalDateTime cuandoAvisar = calculo.salirComoTarde.minusMinutes(AVISAR_ANTES_MIN);
        if (LocalDateTime.now().isBefore(cuandoAvisar)) {
            programarAlarma(ctx, cuandoAvisar);
        } else {
            avisar(ctx, correo, clase, calculo, clave);
        }
    }

    // -------------------------------------------------
    //              CÁLCULO DE LA RUTA
    // -------------------------------------------------

    private static Calculo calcular(Context ctx, ProximaClase clase) {
        RedTransporte red = CapaLugares.cargarRed(ctx);
        if (red == null) return null;
        RedTransporte.Lugar destino = red.lugar(clase.idEdificio);
        RedTransporte.Lugar casa = red.lugar(ORIGEN_POR_DEFECTO);
        if (destino == null || casa == null) return null;

        PlanificadorRuta planificador = new PlanificadorRuta(red);
        LocalDateTime ahora = LocalDateTime.now();

        Punto origen = ubicacion(ctx);
        if (origen != null && destino.contiene(origen)) return null; // ya está en el edificio

        PlanRuta plan = origen != null ? planificador.planificar(origen, destino, ahora) : null;
        // Sin ubicación, o demasiado lejos para el planificador (p. ej. el emulador está en EE. UU.):
        // se calcula desde "casa"
        if (plan == null || plan.tipo == PlanRuta.Tipo.LEJOS) {
            plan = planificador.planificar(casa.punto, destino, ahora);
        }
        if (plan.tipo == PlanRuta.Tipo.LEJOS) return null;

        LocalDateTime inicioClase = LocalDate.now().atTime(LocalTime.parse(clase.horaInicio));
        LocalDateTime salirComoTarde = inicioClase.minusMinutes(plan.minutosTotales() + MARGEN_LLEGADA_MIN);
        return new Calculo(plan, salirComoTarde);
    }

    /** Última ubicación conocida (GPS o red) si hay permiso y es reciente; si no, null. */
    @SuppressLint("MissingPermission") // el permiso se comprueba justo antes
    private static Punto ubicacion(Context ctx) {
        boolean permiso = ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
        if (!permiso) return null;

        LocationManager lm = (LocationManager) ctx.getSystemService(Context.LOCATION_SERVICE);
        if (lm == null) return null;
        Location mejor = null;
        for (String proveedor : new String[]{LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER}) {
            try {
                Location l = lm.getLastKnownLocation(proveedor);
                if (l != null && (mejor == null || l.getTime() > mejor.getTime())) mejor = l;
            } catch (SecurityException | IllegalArgumentException ignorada) {
                // proveedor no disponible
            }
        }
        if (mejor == null || System.currentTimeMillis() - mejor.getTime() > UBICACION_VALIDA_MS) return null;
        return new Punto(mejor.getLatitude(), mejor.getLongitude());
    }

    // -------------------------------------------------
    //              NOTIFICACIÓN Y ALARMA
    // -------------------------------------------------

    private static void avisar(Context ctx, String correo, ProximaClase clase, Calculo calculo, String clave) {
        long faltan = Duration.between(LocalDateTime.now(), calculo.salirComoTarde).toMinutes();
        String titulo;
        if (faltan > 0) {
            titulo = "🚌 Sal en " + faltan + " min para llegar a " + clase.codigoAsignatura
                    + " (" + clase.horaInicio + ")";
        } else if (faltan == 0) {
            titulo = "🏃 ¡Sal ya! " + clase.codigoAsignatura + " empieza a las " + clase.horaInicio;
        } else {
            titulo = "⚠️ Vas justo: " + clase.codigoAsignatura + " empieza a las " + clase.horaInicio;
        }

        String edificio = ProximaClase.AUXILIAR.equals(clase.idEdificio) ? "Edificio Auxiliar" : "ETSIIT";
        String texto = clase.nombreAsignatura + (clase.aula != null ? " · " + clase.aula : "") + " · " + edificio
                + "\n" + calculo.plan.resumen();

        // Al pulsarla se abre el mapa, que propone ir a la clase de ahora
        Intent abrir = new Intent(ctx, Mapa.class).putExtra("CORREO", correo);
        Notificador.crearCanal(ctx);
        Notificador.mostrar(ctx, ID_NOTIFICACION, titulo, texto, abrir);
        prefs(ctx).edit().putString(CLAVE_AVISADA, clave).apply();
    }

    /** Alarma que despierta a la app para avisar (aunque esté cerrada). */
    private static void programarAlarma(Context ctx, LocalDateTime cuando) {
        AlarmManager am = ctx.getSystemService(AlarmManager.class);
        if (am == null) return;
        PendingIntent pi = PendingIntent.getBroadcast(ctx, 0, new Intent(ctx, Receptor.class),
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        // No exacta (no necesita permiso especial): puede retrasarse unos minutos
        am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, FechasUtil.millis(cuando), pi);
    }

    /** Se ejecuta cuando salta la alarma. Registrado en el AndroidManifest. */
    public static class Receptor extends BroadcastReceiver {
        @Override
        public void onReceive(Context ctx, Intent intent) {
            try {
                revisar(ctx);
            } catch (Exception e) {
                Log.e(TAG, "No se pudo revisar la salida a clase", e);
            }
        }
    }

    // -------------------------------------------------
    //              AUXILIARES
    // -------------------------------------------------

    private static String correoSesion(Context ctx) {
        String correo = ctx.getSharedPreferences("SesionUsuario", Context.MODE_PRIVATE).getString("CORREO", null);
        return correo == null || correo.isEmpty() ? null : correo;
    }

    private static SharedPreferences prefs(Context ctx) {
        return ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }
}
