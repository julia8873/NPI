package com.example.nuietsiit.util.tutorial;

import android.view.View;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/** Un paso del tutorial: qué enseñar (foco), qué gesto pedir y el texto del bocadillo. */
public final class Paso {
    /** Vista sobre la que va el foco (la primera que se vea con ese id); 0 = sin foco (gestos de sensor). */
    public final int idVista;
    /** Gesto que hay que hacer; null = solo información (avanza con "Entendido"). */
    public final Gesto gesto;
    public final String texto;
    /** Pista propia de la pantalla (recibe la vista del foco); null = la mano o el móvil animados. */
    Consumer<View> pista;
    /** Si sale o se salta, mirado al llegar al paso; null = siempre. */
    BooleanSupplier condicion;
    /** Preparar la pantalla al llegar al paso (datos de práctica…); null = nada. */
    Runnable alLlegar;

    public Paso(int idVista, Gesto gesto, String texto) {
        this.idVista = idVista;
        this.gesto = gesto;
        this.texto = texto;
    }

    public boolean esInfo() {
        return gesto == null;
    }

    boolean aplica() {
        return condicion == null || condicion.getAsBoolean();
    }
}
