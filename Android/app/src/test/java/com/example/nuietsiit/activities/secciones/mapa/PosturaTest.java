package com.example.nuietsiit.activities.secciones.mapa;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class PosturaTest {

    /** Mide la misma gravedad cada 50 ms durante ms milisegundos; devuelve el último cambio (o null). */
    private static Postura.Tipo durante(Postura p, long[] reloj, float x, float y, float z, long ms) {
        Postura.Tipo cambio = null;
        for (long fin = reloj[0] + ms; reloj[0] < fin; reloj[0] += 50) {
            Postura.Tipo c = p.medir(x, y, z, reloj[0]);
            if (c != null) cambio = c;
        }
        return cambio;
    }

    @Test
    public void levantarHaceFaltaUnSegundo() {
        Postura p = new Postura();
        long[] t = {0};
        // En la mano, inclinado unos 40°: normal
        assertNull(durante(p, t, 0, 6.3f, 7.5f, 2000));
        // Vertical medio segundo: todavía no
        assertNull(durante(p, t, 0, 9.8f, 0.5f, 500));
        assertEquals(Postura.Tipo.NORMAL, p.actual());
        assertEquals(Postura.Tipo.LEVANTADO, durante(p, t, 0, 9.8f, 0.5f, 1200));
    }

    @Test
    public void histeresisAlAndar() {
        Postura p = new Postura();
        long[] t = {0};
        durante(p, t, 0, 9.8f, 0, 2000);
        assertEquals(Postura.Tipo.LEVANTADO, p.actual());
        // Algo menos vertical (7 m/s² en Y) sigue contando como levantado
        assertNull(durante(p, t, 0, 7f, 6.8f, 3000));
        // Tumbarlo de verdad vuelve al mapa
        assertEquals(Postura.Tipo.NORMAL, durante(p, t, 0, 3f, 9.3f, 2000));
    }

    @Test
    public void bocaAbajoYGolpesSueltos() {
        Postura p = new Postura();
        long[] t = {0};
        durante(p, t, 0, 5f, 8.4f, 1000);
        // Un golpe de un paso no cambia nada (filtro paso bajo)
        assertNull(p.medir(0, 30f, 0, t[0]));
        assertNull(durante(p, t, 0, 5f, 8.4f, 1000));
        assertEquals(Postura.Tipo.BOCA_ABAJO, durante(p, t, 0, 0, -9.8f, 2000));
    }
}
