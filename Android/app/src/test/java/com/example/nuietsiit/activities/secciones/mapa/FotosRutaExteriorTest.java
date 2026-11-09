package com.example.nuietsiit.activities.secciones.mapa;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.BeforeClass;
import org.junit.Test;

import java.io.FileInputStream;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

public class FotosRutaExteriorTest {

    private static FotosRutaExterior fotos;
    private static PlanificadorRuta planificador;
    private static RedTransporte red;

    @BeforeClass
    public static void cargar() throws Exception {
        try (InputStream in = new FileInputStream("src/main/assets/" + FotosRutaExterior.ASSET)) {
            fotos = FotosRutaExterior.cargar(in);
        }
        red = HorarioBusTest.cargarRed();
        planificador = new PlanificadorRuta(red);
    }

    private static List<FotosRutaExterior.PuntoFoto> tramo(String id) {
        for (FotosRutaExterior.TramoFotos t : fotos.tramos) {
            if (t.id.equals(id)) return t.puntos;
        }
        throw new AssertionError("No hay tramo " + id);
    }

    @Test
    public void cadaTramoAPieTieneFotosEnOrdenYCercanas() {
        assertEquals(red.tramosAPie.size(), fotos.tramos.size());
        for (FotosRutaExterior.TramoFotos t : fotos.tramos) {
            assertTrue(t.id, t.puntos.size() >= 3);
            assertEquals(t.id + "_01", t.puntos.get(0).id);
            for (int i = 1; i < t.puntos.size(); i++) {
                FotosRutaExterior.PuntoFoto a = t.puntos.get(i - 1), b = t.puntos.get(i);
                assertTrue(b.id, b.metros > a.metros);
                assertTrue(b.id, b.metros - a.metros <= 61);
                assertFalse(b.id, b.instruccion.isEmpty());
            }
        }
    }

    @Test
    public void elPlanEnBusEncuentraLasFotosDeSusTramosAPie() {
        PlanRuta plan = planificador.planificar(red.lugar("livensa_fuentenueva"), red.lugar("etsiit"),
                LocalDateTime.of(2026, 11, 9, 8, 30));
        assertEquals(PlanRuta.Tipo.BUS, plan.tipo);
        assertEquals(tramo("residencia_ronda210"), fotos.delTramo(plan.tramos.get(0)));
        assertEquals(tramo("bellasartes_etsiit"), fotos.delTramo(plan.tramos.get(plan.tramos.size() - 1)));
        assertTrue(fotos.delTramo(plan.tramoBus()).isEmpty());
        assertTrue(fotos.delTramo(null).isEmpty());
    }

    @Test
    public void siguienteAvanzaAlAlcanzarOPasarCadaPunto() {
        List<FotosRutaExterior.PuntoFoto> lista = tramo("bellasartes_etsiit");
        // en la salida: el 1.º ya está alcanzado, toca el 2.º
        assertEquals(1, FotosRutaExterior.siguiente(lista, lista.get(0).punto));
        // a mitad entre el 4.º y el 5.º: toca el 5.º
        Punto a = lista.get(3).punto, b = lista.get(4).punto;
        Punto medio = new Punto((a.lat + b.lat) / 2, (a.lon + b.lon) / 2);
        assertEquals(4, FotosRutaExterior.siguiente(lista, medio));
        // en la llegada: se queda en la última
        assertEquals(lista.size() - 1, FotosRutaExterior.siguiente(lista, lista.get(lista.size() - 1).punto));
        assertEquals(-1, FotosRutaExterior.siguiente(Collections.emptyList(), medio));
    }

    @Test
    public void tramoDeEncuentraElTramoDeCadaPunto() {
        FotosRutaExterior.TramoFotos t = fotos.tramos.get(0);
        assertEquals(t, fotos.tramoDe(t.puntos.get(t.puntos.size() - 1).id));
        assertEquals(null, fotos.tramoDe("etsiit"));
        assertEquals(null, fotos.tramoDe(null));
    }
}
