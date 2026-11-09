package com.example.nuietsiit.activities.secciones.mapa;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.time.LocalDateTime;

public class PuntualidadTest {

    private static LocalDateTime a(int h, int m, int s) {
        return LocalDateTime.of(2026, 11, 9, h, m, s);
    }

    @Test
    public void verdeAmbarRojo() {
        Puntualidad verde = Puntualidad.de(a(10, 24, 0), "10:30");
        assertEquals(Puntualidad.Nivel.VERDE, verde.nivel);
        assertEquals(6, verde.margen);
        assertEquals("Llegas 6 min antes", verde.texto());

        Puntualidad justo = Puntualidad.de(a(10, 28, 30), "10:30");
        assertEquals(Puntualidad.Nivel.AMBAR, justo.nivel);
        assertEquals(1, justo.margen);
        assertEquals("Llegas justo", justo.texto());

        Puntualidad tarde = Puntualidad.de(a(10, 33, 20), "10:30");
        assertEquals(Puntualidad.Nivel.ROJO, tarde.nivel);
        assertEquals(-4, tarde.margen);
        assertEquals("Llegarás 4 min tarde", tarde.texto());
    }

    @Test
    public void unosSegundosTardeYaEsRojo() {
        assertEquals(Puntualidad.Nivel.ROJO, Puntualidad.de(a(10, 30, 10), "10:30").nivel);
        assertEquals(Puntualidad.Nivel.AMBAR, Puntualidad.de(a(10, 30, 0), "10:30").nivel);
    }

    @Test
    public void sinHoraLimite() {
        assertNull(Puntualidad.de(a(10, 0, 0), null));
        assertNull(Puntualidad.de(null, "10:30"));
    }

    @Test
    public void mejoraSoloSiLlegaATiempo() {
        Puntualidad tarde = Puntualidad.de(a(10, 34, 0), "10:30");
        assertTrue(Puntualidad.mejora(Puntualidad.de(a(10, 26, 0), "10:30"), tarde));
        assertFalse(Puntualidad.mejora(Puntualidad.de(a(10, 31, 0), "10:30"), tarde));
        assertFalse(Puntualidad.mejora(null, tarde));
    }
}
