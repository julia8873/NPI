package com.example.nuietsiit.ui;

import android.content.Context;
import android.content.SharedPreferences;
import android.view.View;
import android.view.ViewTreeObserver;

import com.example.nuietsiit.util.DetectorInclinacion;
import com.example.nuietsiit.util.tutorial.Gesto;
import com.example.nuietsiit.util.tutorial.Tutorial;

import java.util.function.BooleanSupplier;
import java.util.function.IntConsumer;

/**
 * Inclinar el móvil para pasar de página (la misma interacción que los pasos de un trámite), atada a una vista:
 * escucha el acelerómetro solo mientras la vista está en una ventana con el foco y hay a dónde pasar.
 * Agitar sigue volviendo al menú: el giro para pasar es suave y la sacudida, fuerte.
 */
public class InclinarParaPasar implements View.OnAttachStateChangeListener, ViewTreeObserver.OnWindowFocusChangeListener {

    private static final String PREFS = "secretaria";
    private static final String PREF_INCLINAR = "inclinarPasos";

    private final View vista;
    private final DetectorInclinacion detector;
    private final BooleanSupplier hayPaginas;
    private boolean activo, adjunta;

    /** pasar recibe +1 (inclinar a la derecha) o -1 (a la izquierda). */
    public InclinarParaPasar(View vista, IntConsumer pasar, BooleanSupplier hayPaginas) {
        this.vista = vista;
        this.hayPaginas = hayPaginas;
        detector = new DetectorInclinacion(vista.getContext(), direccion -> {
            pasar.accept(direccion == DetectorInclinacion.DERECHA ? 1 : -1);
            Tutorial.avisar(vista.getContext(), Gesto.INCLINAR);
        });
        vista.addOnAttachStateChangeListener(this);
        if (vista.isAttachedToWindow()) onViewAttachedToWindow(vista);
    }

    /** Preferencia compartida con el interruptor de los pasos de un trámite. Por defecto, activado. */
    public static boolean activado(Context ctx) {
        return prefs(ctx).getBoolean(PREF_INCLINAR, true);
    }

    public static void setActivado(Context ctx, boolean si) {
        prefs(ctx).edit().putBoolean(PREF_INCLINAR, si).apply();
    }

    private static SharedPreferences prefs(Context ctx) {
        return ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    /** Si ahora mismo inclinar pasaría páginas (para que el tutorial solo lo enseñe entonces). */
    public boolean escuchando() {
        return activo;
    }

    /** Llamar cuando cambie el número de páginas. */
    public void actualizar() {
        boolean debe = adjunta && vista.hasWindowFocus() && vista.isShown()
                && detector.disponible() && hayPaginas.getAsBoolean() && activado(vista.getContext());
        if (debe == activo) return;
        activo = debe;
        if (debe) {
            detector.iniciar();
        } else {
            detector.parar();
        }
    }

    @Override
    public void onViewAttachedToWindow(View v) {
        adjunta = true;
        v.getViewTreeObserver().addOnWindowFocusChangeListener(this);
        actualizar();
    }

    @Override
    public void onViewDetachedFromWindow(View v) {
        adjunta = false;
        v.getViewTreeObserver().removeOnWindowFocusChangeListener(this);
        actualizar();
    }

    // otra Activity o un diálogo encima: deja de escuchar
    @Override
    public void onWindowFocusChanged(boolean tieneFoco) {
        actualizar();
    }
}
