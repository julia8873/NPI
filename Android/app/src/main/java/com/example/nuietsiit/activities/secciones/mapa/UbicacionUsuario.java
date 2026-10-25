package com.example.nuietsiit.activities.secciones.mapa;

import java.text.Normalizer;
import java.util.Locale;

/**
 * Posición del usuario dentro de la ETSIIT: en qué planta está y en qué punto del plano.
 *
 * Las coordenadas pueden estar en dos espacios:
 *  - píxeles del plano (igual que los {@link Marcador}), o
 *  - normalizadas (0..1 sobre el ancho/alto del plano). Las usamos para los puntos nuevos
 *    (entrada, QR) porque no dependen de la resolución/densidad de la pantalla.
 */
public class UbicacionUsuario {

    public enum Metodo { HORARIO, GPS, ENTRADA, QR, MANUAL }

    /** Clave en minúsculas (a-z, 0-9, _). Sirve para buscar las fotos de ruta: ruta_<idPunto>_<grados>. */
    public final String idPunto;
    public final String nombre;
    public final String planta;   // "sotano", "planta0", "planta1"...
    public final Metodo metodo;

    private final float x;
    private final float y;
    private final boolean normalizada;

    private UbicacionUsuario(String idPunto, String nombre, String planta, float x, float y,
                             boolean normalizada, Metodo metodo) {
        this.idPunto = idPunto;
        this.nombre = nombre;
        this.planta = planta;
        this.x = x;
        this.y = y;
        this.normalizada = normalizada;
        this.metodo = metodo;
    }

    public static UbicacionUsuario enPixeles(String idPunto, String nombre, String planta,
                                             float xPx, float yPx, Metodo metodo) {
        return new UbicacionUsuario(idPunto, nombre, planta, xPx, yPx, false, metodo);
    }

    public static UbicacionUsuario normalizada(String idPunto, String nombre, String planta,
                                               float nx, float ny, Metodo metodo) {
        return new UbicacionUsuario(idPunto, nombre, planta, nx, ny, true, metodo);
    }

    /** X en píxeles del plano, dado el ancho del drawable que se está mostrando. */
    public float xPx(int anchoPlano) {
        return normalizada ? x * anchoPlano : x;
    }

    /** Y en píxeles del plano, dado el alto del drawable que se está mostrando. */
    public float yPx(int altoPlano) {
        return normalizada ? y * altoPlano : y;
    }

    public String descripcionMetodo() {
        switch (metodo) {
            case HORARIO: return "según tu horario";
            case ENTRADA: return "en la entrada";
            case QR:      return "por código QR";
            case GPS:     return "por GPS";
            case MANUAL:  return "elegido por ti";
            default:      return "";
        }
    }

    /** "Aula 0.5" -> "aula_0_5"; quita emojis, tildes y símbolos. */
    public static String idDesdeNombre(String nombre) {
        String s = Normalizer.normalize(nombre, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "_")
                .replaceAll("^_+|_+$", "");
        return s.isEmpty() ? "punto" : s;
    }
}
