package com.example.nuietsiit.activities.secciones.mapa;

import java.util.List;
import java.util.Locale;

/** Coordenada (lat, lon) en Java puro, con distancia y rumbo. */
public final class Punto {

    private static final double RADIO_TIERRA = 6371000.0;

    public final double lat;
    public final double lon;

    public Punto(double lat, double lon) {
        this.lat = lat;
        this.lon = lon;
    }

    /** Distancia en metros (haversine). */
    public double distanciaA(Punto otro) {
        double f1 = Math.toRadians(lat), f2 = Math.toRadians(otro.lat);
        double df = f2 - f1, dl = Math.toRadians(otro.lon - lon);
        double a = Math.sin(df / 2) * Math.sin(df / 2)
                + Math.cos(f1) * Math.cos(f2) * Math.sin(dl / 2) * Math.sin(dl / 2);
        return 2 * RADIO_TIERRA * Math.asin(Math.min(1, Math.sqrt(a)));
    }

    /** Rumbo en grados (0 = norte, 90 = este) hacia otro punto. */
    public double rumboA(Punto otro) {
        double f1 = Math.toRadians(lat), f2 = Math.toRadians(otro.lat);
        double dl = Math.toRadians(otro.lon - lon);
        double y = Math.sin(dl) * Math.cos(f2);
        double x = Math.cos(f1) * Math.sin(f2) - Math.sin(f1) * Math.cos(f2) * Math.cos(dl);
        return (Math.toDegrees(Math.atan2(y, x)) + 360) % 360;
    }

    /** ¿Está dentro del polígono? (ray casting; vale para contornos pequeños). */
    public boolean dentroDe(List<Punto> poligono) {
        boolean dentro = false;
        for (int i = 0, j = poligono.size() - 1; i < poligono.size(); j = i++) {
            Punto a = poligono.get(i), b = poligono.get(j);
            if ((a.lat > lat) != (b.lat > lat)
                    && lon < (b.lon - a.lon) * (lat - a.lat) / (b.lat - a.lat) + a.lon) {
                dentro = !dentro;
            }
        }
        return dentro;
    }

    @Override
    public String toString() {
        return String.format(Locale.US, "(%.6f, %.6f)", lat, lon);
    }
}
