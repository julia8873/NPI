package com.example.nuietsiit.activities.secciones.mapa;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

public class DestinoViajeTest {

    @Test
    public void guardarYLeer() {
        DestinoViaje d = DestinoViaje.leer(new DestinoViaje("etsiit", "Biblioteca", "📚 Biblioteca", "10:30").guardar());
        assertEquals("etsiit", d.idEdificio);
        assertEquals("Biblioteca", d.interior);
        assertEquals("📚 Biblioteca", d.etiqueta);
        assertNull(d.horaLimite);

        DestinoViaje sinDentro = DestinoViaje.leer(new DestinoViaje("edificio_auxiliar", null, "🏢 Auxiliar", null).guardar());
        assertNull(sinDentro.interior);
        assertNull(DestinoViaje.leer("basura"));
    }

    @Test
    public void recientesSinRepetirYConLimite() {
        DestinoViaje bib = new DestinoViaje("etsiit", "Biblioteca", "📚 Biblioteca", null);
        DestinoViaje com = new DestinoViaje("etsiit", "Comedor", "☕ Comedor", null);
        DestinoViaje aux = new DestinoViaje("edificio_auxiliar", null, "🏢 Auxiliar", null);
        List<String> r = DestinoViaje.anadirReciente(Arrays.asList(com.guardar(), bib.guardar(), aux.guardar()),
                new DestinoViaje("etsiit", "Biblioteca", "📚 Biblioteca", "9:00"), 3);
        assertEquals(Arrays.asList(bib.guardar(), com.guardar(), aux.guardar()), r);
        assertEquals(2, DestinoViaje.anadirReciente(r, aux, 2).size());
    }
}
