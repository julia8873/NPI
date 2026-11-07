package com.example.nuietsiit.activities.secciones.biblioteca;

import com.example.nuietsiit.data.modelo.Libro;

/**
 * Lo que se nota al apuntar a un libro: cada estado tiene su color y su vibración,
 * para enterarse sin mirar la pantalla.
 */
public enum SenalLibro {
    DISPONIBLE("#2E7D32", new long[]{0, 70}),                 // 1 corto
    DE_TU_ASIGNATURA("#BF4A00", new long[]{0, 70, 110, 70}),  // 2 cortos
    SIN_EJEMPLARES("#C62828", new long[]{0, 450});            // 1 largo

    public final String color;
    public final long[] vibracion;

    SenalLibro(String color, long[] vibracion) {
        this.color = color;
        this.vibracion = vibracion;
    }

    // sin ejemplares manda: aunque sea de tu asignatura, no te lo puedes llevar
    public static SenalLibro de(Libro libro, boolean deMiAsignatura) {
        if (!libro.disponible()) return SIN_EJEMPLARES;
        return deMiAsignatura ? DE_TU_ASIGNATURA : DISPONIBLE;
    }
}
