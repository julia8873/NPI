package com.example.nuietsiit.activities.secciones.mapa;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Trayecto de demostración ("Simular trayecto" de la guía): dónde estaría el usuario en cada momento si
 * sigue el plan al pie de la letra. Antes de salir está en el origen; en la espera, quieto en la parada.
 */
public final class TrayectoSimulado {

    public static final class Muestra {
        public final Punto posicion;
        /** m/s */
        public final double velocidad;

        Muestra(Punto posicion, double velocidad) {
            this.posicion = posicion;
            this.velocidad = velocidad;
        }
    }

    private final PlanRuta plan;

    public TrayectoSimulado(PlanRuta plan) {
        this.plan = plan;
    }

    /** Hora a la que conviene empezar la simulación: un poco antes de salir. */
    public LocalDateTime inicio() {
        return plan.salida.minusSeconds(30);
    }

    public Muestra en(LocalDateTime t) {
        Punto ultimo = plan.origen;
        for (PlanRuta.Tramo tramo : plan.tramos) {
            List<Punto> g = tramo.geometria;
            if (tramo.tipo == PlanRuta.TipoTramo.ESPERA || g.size() < 2) continue;
            if (t.isBefore(tramo.inicio)) return new Muestra(ultimo, 0);
            double segundos = Duration.between(tramo.inicio, tramo.fin).toMillis() / 1000.0;
            if (t.isBefore(tramo.fin) && segundos > 0) {
                double total = longitud(g);
                double hecho = total * Duration.between(tramo.inicio, t).toMillis() / 1000.0 / segundos;
                return new Muestra(puntoA(g, hecho), total / segundos);
            }
            ultimo = g.get(g.size() - 1);
        }
        return new Muestra(ultimo, 0);
    }

    private static double longitud(List<Punto> g) {
        double m = 0;
        for (int i = 1; i < g.size(); i++) m += g.get(i - 1).distanciaA(g.get(i));
        return m;
    }

    /** Punto a {@code metros} del comienzo de la línea (interpolación lineal en cada segmento). */
    private static Punto puntoA(List<Punto> g, double metros) {
        for (int i = 1; i < g.size(); i++) {
            Punto a = g.get(i - 1), b = g.get(i);
            double d = a.distanciaA(b);
            if (metros <= d && d > 0) {
                double f = metros / d;
                return new Punto(a.lat + (b.lat - a.lat) * f, a.lon + (b.lon - a.lon) * f);
            }
            metros -= d;
        }
        return g.get(g.size() - 1);
    }
}
