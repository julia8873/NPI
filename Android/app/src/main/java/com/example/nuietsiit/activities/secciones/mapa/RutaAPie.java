package com.example.nuietsiit.activities.secciones.mapa;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Cálculo de tramos a pie. La versión local usa el tramo precalculado del JSON si existe
 * y, si no, la línea recta × 1,3. (F3 puede poner aquí una versión online con caída a esta.)
 */
public interface RutaAPie {

    double METROS_POR_MINUTO = 75;   // como en Mapa (min a pie = m / 75)
    double FACTOR_RECTA = 1.3;

    Camino calcular(Punto desde, Punto hasta);

    static RutaAPie local(RedTransporte red) {
        return (desde, hasta) -> {
            Camino precalculado = red.tramoAPie(desde, hasta);
            return precalculado != null ? precalculado : Camino.recto(desde, hasta);
        };
    }

    /** Camino a pie: metros y geometría. */
    final class Camino {
        public final double metros;
        public final List<Punto> puntos;
        public final boolean precalculado;

        public Camino(double metros, List<Punto> puntos, boolean precalculado) {
            this.metros = metros;
            this.puntos = Collections.unmodifiableList(puntos);
            this.precalculado = precalculado;
        }

        public static Camino recto(Punto desde, Punto hasta) {
            return new Camino(desde.distanciaA(hasta) * FACTOR_RECTA, Arrays.asList(desde, hasta), false);
        }

        public double minutos() {
            return metros / METROS_POR_MINUTO;
        }
    }
}
