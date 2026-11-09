package com.example.nuietsiit.activities.secciones.biblioteca;

import androidx.annotation.ColorRes;

import com.example.nuietsiit.R;
import com.example.nuietsiit.data.modelo.Libro;

/**
 * Lo que se nota al apuntar a un libro: cada estado tiene su color y su vibración,
 * para enterarse sin mirar la pantalla. Colores del design system NUI (la leyenda de
 * apuntar_libros.xml usa los mismos).
 */
public enum SenalLibro {
    DISPONIBLE(R.color.nui_arrayan, new long[]{0, 70}),                 // 1 corto
    DE_TU_ASIGNATURA(R.color.nui_resaltado, new long[]{0, 70, 110, 70}),  // 2 cortos
    SIN_EJEMPLARES(R.color.nui_granada, new long[]{0, 450});            // 1 largo

    @ColorRes
    public final int color;
    public final long[] vibracion;

    SenalLibro(@ColorRes int color, long[] vibracion) {
        this.color = color;
        this.vibracion = vibracion;
    }

    // sin ejemplares manda: aunque sea de tu asignatura, no te lo puedes llevar
    public static SenalLibro de(Libro libro, boolean deMiAsignatura) {
        if (!libro.disponible()) return SIN_EJEMPLARES;
        return deMiAsignatura ? DE_TU_ASIGNATURA : DISPONIBLE;
    }
}
