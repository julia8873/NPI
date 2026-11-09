package com.example.nuietsiit.activities.secciones.mapa;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * A dónde vas: edificio de la red (ETSIIT / Auxiliar) + sitio de dentro opcional (parte del título de un marcador
 * de los planos: "Biblioteca", "Aula 2.1") + hora límite opcional (inicio de la clase, "HH:mm").
 * Lo crea "¿A dónde vas?" (InicioMapa) y lo lleva el mapa durante todo el viaje.
 */
public final class DestinoViaje {

    public final String idEdificio;
    public final String interior;   // null = el edificio sin más
    public final String etiqueta;   // texto para la UI: "📚 Biblioteca"
    public final String horaLimite; // null si no hay prisa

    public DestinoViaje(String idEdificio, String interior, String etiqueta, String horaLimite) {
        this.idEdificio = idEdificio;
        this.interior = interior;
        this.etiqueta = etiqueta;
        this.horaLimite = horaLimite;
    }

    /** Una línea para guardarlo en "Recientes" (sin hora: la clase de hoy no vale mañana). */
    public String guardar() {
        return idEdificio + "|" + (interior != null ? interior : "") + "|" + etiqueta;
    }

    /** Inverso de {@link #guardar()}; null si la línea no es válida. */
    public static DestinoViaje leer(String linea) {
        if (linea == null) return null;
        String[] p = linea.split("\\|", -1);
        if (p.length != 3 || p[0].isEmpty() || p[2].isEmpty()) return null;
        return new DestinoViaje(p[0], p[1].isEmpty() ? null : p[1], p[2], null);
    }

    /** Pone {@code nuevo} el primero de los recientes, sin repetir y como mucho {@code max}. */
    public static List<String> anadirReciente(List<String> recientes, DestinoViaje nuevo, int max) {
        List<String> lista = new ArrayList<>();
        lista.add(nuevo.guardar());
        for (String r : recientes) {
            DestinoViaje d = leer(r);
            if (d != null && !d.mismoSitio(nuevo) && lista.size() < max) lista.add(r);
        }
        return lista;
    }

    public boolean mismoSitio(DestinoViaje otro) {
        return idEdificio.equals(otro.idEdificio) && Objects.equals(interior, otro.interior);
    }
}
