package com.example.nuietsiit.activities.secciones.mapa;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Puntos de la ETSIIT donde hay (o habrá) un código QR de ubicación, más la entrada principal.
 *
 * Contenido de cada QR:   NPI-ETSIIT:<id>      (por ejemplo  NPI-ETSIIT:p0_hall)
 *
 * IMPORTANTE: las coordenadas son normalizadas (0..1 sobre el plano) y son PROVISIONALES.
 * Cuando peguéis los QR reales, ajustad aquí la posición de cada uno.
 * En Documentacion/QR_ubicacion/ están los PNG para imprimir.
 */
public final class RegistroPuntosQR {

    public static final String PREFIJO = "NPI-ETSIIT:";

    private static final List<UbicacionUsuario> PUNTOS_QR = new ArrayList<>();

    static {
        // id, nombre, planta, x (0..1), y (0..1)
        qr("sotano_hall",     "Hall del sótano",             "sotano",  0.50f, 0.50f);
        qr("p0_hall",         "Hall de la planta baja",      "planta0", 0.49f, 0.50f);
        qr("p0_pasillo_este", "Pasillo este (planta baja)",  "planta0", 0.62f, 0.45f);
        qr("p1_hall",         "Hall de la planta 1",         "planta1", 0.50f, 0.50f);
        qr("p2_hall",         "Hall de la planta 2",         "planta2", 0.50f, 0.50f);
        qr("p3_hall",         "Hall de la planta 3",         "planta3", 0.50f, 0.50f);
        qr("p4_hall",         "Hall de la planta 4",         "planta4", 0.50f, 0.50f);
        qr("p5_hall",         "Hall de la planta 5",         "planta5", 0.50f, 0.50f);
    }

    private RegistroPuntosQR() {}

    private static void qr(String id, String nombre, String planta, float nx, float ny) {
        PUNTOS_QR.add(UbicacionUsuario.normalizada(id, nombre, planta, nx, ny, UbicacionUsuario.Metodo.QR));
    }

    /** Entrada principal de la ETSIIT (escalinata frontal, planta baja). */
    public static UbicacionUsuario entrada() {
        return UbicacionUsuario.normalizada("entrada", "Entrada de la ETSIIT", "planta0",
                0.515f, 0.62f, UbicacionUsuario.Metodo.ENTRADA);
    }

    public static List<UbicacionUsuario> todosLosQR() {
        return Collections.unmodifiableList(PUNTOS_QR);
    }

    /** Devuelve el punto de un QR escaneado, o null si no es un QR de la ETSIIT / no existe. */
    public static UbicacionUsuario desdeContenido(String contenido) {
        if (contenido == null) return null;
        String c = contenido.trim();
        if (!c.regionMatches(true, 0, PREFIJO, 0, PREFIJO.length())) return null;
        String id = c.substring(PREFIJO.length()).trim().toLowerCase(Locale.ROOT);
        for (UbicacionUsuario p : PUNTOS_QR) {
            if (p.idPunto.equals(id)) return p;
        }
        return null;
    }

    public static String contenidoDe(UbicacionUsuario p) {
        return PREFIJO + p.idPunto;
    }
}
