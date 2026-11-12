package com.example.nuietsiit.util.tutorial;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.SharedPreferences;
import android.view.View;
import android.view.ViewGroup;

import com.example.nuietsiit.R;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/**
 * Tutorial de práctica guiada de una pantalla: cada paso pide hacer el gesto de verdad
 * y solo avanza cuando se hace (o se salta). Sale solo la primera vez (prefs "tutoriales",
 * clave + versión: si cambian los pasos, se sube la versión y vuelve a salir).
 *
 * <pre>
 *  new Tutorial(this, "biblioteca", 1)
 *     .tocar(R.id.portada, "Toca una portada")
 *     .gesto(R.id.balda, Gesto.PELLIZCAR, "Pellizca para ver más libros")
 *     .sensor(Gesto.INCLINAR, "Inclina el móvil para pasar")
 *     .info(R.id.chips, "Aquí se filtra")
 *     .mostrarSiPrimeraVez();
 * </pre>
 */
public class Tutorial {

    public static final String PREFS = "tutoriales";
    /** Paso global "agita para volver al menú": sale una sola vez, en el primer tutorial que se vea. */
    private static final String CLAVE_AGITAR = "global_agitar";

    private final Activity actividad;
    private final String clave;
    private final int version;
    private final List<Paso> pasos = new ArrayList<>();

    private CapaTutorial capa;
    private int actual;
    private boolean conPasoAgitar;
    private Runnable alEmpezar, alTerminar;
    private boolean pasoMostrado; // en esta vuelta ya salió alguno (si no, no hay "¡Listo!")

    public Tutorial(Activity actividad, String clave, int version) {
        this.actividad = actividad;
        this.clave = clave;
        this.version = version;
    }

    // ---------- pasos ----------

    public Tutorial tocar(int idVista, String texto) {
        return gesto(idVista, Gesto.TOCAR, texto);
    }

    public Tutorial gesto(int idVista, Gesto gesto, String texto) {
        pasos.add(new Paso(idVista, gesto, texto));
        return this;
    }

    public Tutorial sensor(Gesto gesto, String texto) {
        return gesto(0, gesto, texto);
    }

    /** Solo lectura: se señala la vista y se avanza con "Entendido". */
    public Tutorial info(int idVista, String texto) {
        pasos.add(new Paso(idVista, null, texto));
        return this;
    }

    /** Pista propia para el último paso añadido (p. ej. las tijeras del resguardo) en vez de la mano. */
    public Tutorial conPista(Consumer<View> pista) {
        if (!pasos.isEmpty()) pasos.get(pasos.size() - 1).pista = pista;
        return this;
    }

    /** El último paso añadido solo sale si, al llegar a él, se cumple la condición (p. ej. "hay más de una hoja"). */
    public Tutorial soloSi(BooleanSupplier condicion) {
        if (!pasos.isEmpty()) pasos.get(pasos.size() - 1).condicion = condicion;
        return this;
    }

    /** El último paso añadido solo sale si su vista se ve al llegar (no está oculta, ni en otra hoja). */
    public Tutorial soloSiSeVe() {
        if (pasos.isEmpty()) return this;
        int id = pasos.get(pasos.size() - 1).idVista;
        return soloSi(() -> CapaTutorial.buscarVisible(actividad.findViewById(android.R.id.content), id) != null);
    }

    /** Se ejecuta al llegar al último paso añadido, antes de enseñarlo (p. ej. poner datos de práctica). */
    public Tutorial alLlegar(Runnable preparar) {
        if (!pasos.isEmpty()) pasos.get(pasos.size() - 1).alLlegar = preparar;
        return this;
    }

    /** Se ejecuta cada vez que empieza (también al repetirlo con "?"): deja la pantalla como la esperan los pasos. */
    public Tutorial alEmpezar(Runnable preparar) {
        alEmpezar = preparar;
        return this;
    }

    /** Se ejecuta cuando se va la capa, por lo que sea (acabado, saltado, pantalla cerrada): quitar lo de práctica. */
    public Tutorial alTerminar(Runnable limpiar) {
        alTerminar = limpiar;
        return this;
    }

    /**
     * Para vistas reutilizables (hojas, fichero, inclinar para pasar) que no saben en qué pantalla están:
     * avisa al tutorial de su Activity, si es una {@link ConTutorial} (BaseActivity lo es).
     */
    public static void avisar(Context ctx, Gesto gesto) {
        while (!(ctx instanceof ConTutorial) && ctx instanceof ContextWrapper) {
            Context base = ((ContextWrapper) ctx).getBaseContext();
            if (base == ctx) break;
            ctx = base;
        }
        if (ctx instanceof ConTutorial) ((ConTutorial) ctx).avisarTutorial(gesto);
    }

    /** Pantalla que puede tener tutorial activo. */
    public interface ConTutorial {
        void avisarTutorial(Gesto gesto);
    }

    // ---------- mostrar ----------

    public boolean yaVisto() {
        return prefs().getBoolean(claveVisto(), false);
    }

    /** Lo muestra si esta versión no se ha visto aún. Devuelve si se mostró. */
    public boolean mostrarSiPrimeraVez() {
        if (yaVisto()) return false;
        mostrar();
        return true;
    }

    /** Lo muestra siempre (botón "?"). */
    public void mostrar() {
        if (activo() || pasos.isEmpty() || actividad.isFinishing()) return;
        boolean agitarVisto = prefs().getBoolean(CLAVE_AGITAR, false);
        if (conPasoAgitar && agitarVisto) { // se añadió la otra vez y ya se vio
            pasos.remove(pasos.size() - 1);
            conPasoAgitar = false;
        } else if (!conPasoAgitar && !agitarVisto) {
            pasos.add(new Paso(0, Gesto.AGITAR, actividad.getString(R.string.tut_global_agitar)));
            conPasoAgitar = true;
        }
        if (alEmpezar != null) alEmpezar.run();
        ViewGroup contenido = actividad.findViewById(android.R.id.content);
        capa = new CapaTutorial(actividad, this);
        contenido.addView(capa, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        actual = -1;
        pasoMostrado = false;
        siguiente();
    }

    public boolean activo() {
        return capa != null;
    }

    /** La pantalla reconoció un gesto: si es el que pide el paso actual, cuenta como acierto. */
    public void avisar(Gesto gesto) {
        if (capa == null || actual < 0 || actual >= pasos.size()) return;
        if (pasos.get(actual).gesto == gesto) capa.acierto();
    }

    /** Saca la capa sin marcar nada (p. ej. la Activity se destruye). */
    public void cerrar() {
        if (capa == null) return;
        CapaTutorial c = capa;
        capa = null;
        c.quitar();
        terminar();
    }

    // ---------- avance (lo llama la capa) ----------

    void siguiente() {
        if (capa == null) return;
        actual++;
        // los pasos que ahora no tocan (sin hojas que pasar, sin datos…) se saltan solos
        while (actual < pasos.size() && !pasos.get(actual).aplica()) actual++;
        if (actual < pasos.size()) {
            Paso p = pasos.get(actual);
            if (p.alLlegar != null) p.alLlegar.run();
            if (capa == null) return; // alLlegar pudo cerrar la pantalla
            capa.mostrarPaso(p, actual, pasos.size());
            pasoMostrado = true;
        } else if (!pasoMostrado) {
            cerrar(); // ahora no tocaba ninguno: sin "¡Listo!" y sin marcarlo, saldrá cuando sí toquen
        } else {
            marcarVisto();
            capa.despedirse();
        }
    }

    void saltarTodo() {
        marcarVisto();
        cerrar();
    }

    /** La capa terminó de despedirse y se quitó. */
    void alQuitarCapa(CapaTutorial quitada) {
        if (capa != quitada) return;
        capa = null;
        terminar();
    }

    private void terminar() {
        if (alTerminar != null) alTerminar.run();
    }

    private void marcarVisto() {
        SharedPreferences.Editor e = prefs().edit().putBoolean(claveVisto(), true);
        if (conPasoAgitar) e.putBoolean(CLAVE_AGITAR, true);
        e.apply();
    }

    private String claveVisto() {
        return clave + "_v" + version;
    }

    private SharedPreferences prefs() {
        return actividad.getSharedPreferences(PREFS, Activity.MODE_PRIVATE);
    }
}