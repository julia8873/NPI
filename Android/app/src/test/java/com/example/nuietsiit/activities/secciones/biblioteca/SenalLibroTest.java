package com.example.nuietsiit.activities.secciones.biblioteca;

import static org.junit.Assert.assertEquals;

import com.example.nuietsiit.data.modelo.Libro;

import org.junit.Test;

public class SenalLibroTest {

    private static Libro libro(int disponibles) {
        return new Libro(1, "T", "A", "9780000000000", null, null, null, null, 3, disponibles, null, null, null, null);
    }

    @Test
    public void disponibleSinSerDeMiAsignatura() {
        assertEquals(SenalLibro.DISPONIBLE, SenalLibro.de(libro(1), false));
    }

    @Test
    public void disponibleDeMiAsignatura() {
        assertEquals(SenalLibro.DE_TU_ASIGNATURA, SenalLibro.de(libro(1), true));
    }

    @Test
    public void sinEjemplaresMandaAunqueSeaDeMiAsignatura() {
        assertEquals(SenalLibro.SIN_EJEMPLARES, SenalLibro.de(libro(0), true));
        assertEquals(SenalLibro.SIN_EJEMPLARES, SenalLibro.de(libro(0), false));
    }
}
