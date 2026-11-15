package com.example.nuietsiit.activities.secciones.mapa;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.BeforeClass;
import org.junit.Test;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

public class PlanificadorRutaTest {

    private static RedTransporte red;
    private static PlanificadorRuta planificador;
    private static RedTransporte.Lugar residencia, etsiit, auxiliar;

    @BeforeClass
    public static void cargar() throws Exception {
        red = HorarioBusTest.cargarRed();
        planificador = new PlanificadorRuta(red);
        residencia = red.lugar("livensa_fuentenueva");
        etsiit = red.lugar("etsiit");
        auxiliar = red.lugar("edificio_auxiliar");
    }

    private static LocalDateTime lunes(int h, int m) {
        return LocalDateTime.of(2026, 11, 9, h, m);
    }

    private static void tramosEncadenados(PlanRuta plan) {
        assertEquals(plan.salida, plan.tramos.get(0).inicio);
        for (int i = 1; i < plan.tramos.size(); i++) {
            assertEquals(plan.tramos.get(i - 1).fin, plan.tramos.get(i).inicio);
        }
        assertEquals(plan.llegada, plan.tramos.get(plan.tramos.size() - 1).fin);
    }

    @Test
    public void residenciaAEtsiitEnEl9BajandoEnBellasArtes() {
        PlanRuta plan = planificador.planificar(residencia, etsiit, lunes(8, 30));
        assertEquals(PlanRuta.Tipo.BUS, plan.tipo);
        PlanRuta.Tramo bus = plan.tramoBus();
        assertEquals("9", bus.linea.id);
        assertEquals("chana", bus.sentido.id);
        assertEquals("Camino de Ronda 210", bus.subida.nombre);
        assertEquals("Avenida de Andalucía - Facultad Bellas Artes", bus.bajada.nombre);
        // bus de las 8:12 desde cabecera: Ronda 210 a las 8:37:30, Bellas Artes a las 8:43
        assertEquals(lunes(8, 37).plusSeconds(30), bus.inicio);
        assertEquals(lunes(8, 43), bus.fin);
        // sal = paso − 187 m a pie (2:30) − 2 min; llega = 8:43 + 465 m (6:12)
        assertEquals(lunes(8, 33), plan.salida);
        assertEquals(lunes(8, 49).plusSeconds(12), plan.llegada);
        assertEquals(187 + 465, plan.metrosAPie, 0.5);

        assertEquals(4, plan.tramos.size());
        assertEquals(PlanRuta.TipoTramo.ESPERA, plan.tramos.get(1).tipo);
        assertEquals(bus.subida.id, plan.tramos.get(0).idFoto);
        assertEquals("bus_9", bus.idFoto);
        assertEquals("etsiit", plan.tramos.get(3).idFoto);
        assertTrue(plan.tramos.get(0).geometria.size() > 2);   // tramo precalculado, no recta
        tramosEncadenados(plan);
        assertEquals("Sal a las 8:33 · 3 min a pie · 🚌 9 de las 8:37 en Camino de Ronda 210 · 6 min"
                + " · bájate en Avenida de Andalucía - Facultad Bellas Artes · 7 min a pie · llegas 8:50",
                plan.resumen());
    }

    @Test
    public void enFindeTambienVaEnBus() {
        PlanRuta plan = planificador.planificar(residencia, etsiit, LocalDateTime.of(2026, 11, 14, 8, 30));
        assertEquals(PlanRuta.Tipo.BUS, plan.tipo);
        assertEquals(LocalDateTime.of(2026, 11, 14, 8, 40, 30), plan.tramoBus().inicio);   // 8:15 + 25,5
    }

    @Test
    public void etsiitYAuxiliarAPie() {
        PlanRuta ida = planificador.planificar(etsiit, auxiliar, lunes(10, 0));
        assertEquals(PlanRuta.Tipo.A_PIE, ida.tipo);
        assertEquals(578, ida.metrosAPie, 0.5);
        assertEquals(lunes(10, 0), ida.salida);
        assertEquals(8, ida.minutosTotales());
        assertNull(ida.tramoBus());
        assertEquals("edificio_auxiliar", ida.tramos.get(0).idFoto);
        tramosEncadenados(ida);

        PlanRuta vuelta = planificador.planificar(auxiliar, etsiit, lunes(10, 0));
        assertEquals(PlanRuta.Tipo.A_PIE, vuelta.tipo);
        assertEquals(578, vuelta.metrosAPie, 0.5);
    }

    @Test
    public void vueltaALaResidenciaEnEl9HaciaLosRebites() {
        LocalDateTime ahora = lunes(14, 0);
        PlanRuta plan = planificador.planificar(etsiit, residencia, ahora);
        assertEquals(PlanRuta.Tipo.BUS, plan.tipo);
        PlanRuta.Tramo bus = plan.tramoBus();
        assertEquals("rebites", bus.sentido.id);
        assertEquals("Periodista Rafael Gómez Montero 3", bus.subida.nombre);
        assertEquals("Camino de Ronda 177 - Estadio de la Juventud", bus.bajada.nombre);
        assertEquals(125 + 157, plan.metrosAPie, 0.5);
        // salir a las = paso − 125 m a pie (1:40) − 2 min, y nunca antes de "ahora"
        assertEquals(bus.inicio.minusSeconds(100).minusMinutes(2), plan.salida);
        assertFalse(plan.salida.isBefore(ahora));
        tramosEncadenados(plan);
    }

    @Test
    public void sinBusCercaVaAPie() {
        // ~1,1 km al norte de la ETSIIT, sin paradas del 9 a menos de 700 m
        Punto norte = new Punto(etsiit.punto.lat + 0.010, etsiit.punto.lon + 0.002);
        PlanRuta plan = planificador.planificar(norte, etsiit, lunes(9, 0));
        assertEquals(PlanRuta.Tipo.A_PIE, plan.tipo);
        assertEquals(1, plan.tramos.size());
        assertEquals(2, plan.tramos.get(0).geometria.size());   // línea recta
        assertEquals(norte.distanciaA(etsiit.punto) * RutaAPie.FACTOR_RECTA, plan.metrosAPie, 0.5);
        assertTrue(planificador.alternativas(norte, etsiit, lunes(9, 0)).isEmpty());
    }

    @Test
    public void lejosDeTodoProponeGoogleMaps() {
        Punto madrid = new Punto(40.4168, -3.7038);
        PlanRuta plan = planificador.planificar(madrid, etsiit, lunes(9, 0));
        assertEquals(PlanRuta.Tipo.LEJOS, plan.tipo);
        assertTrue(plan.tramos.isEmpty());
        assertTrue(plan.resumen().contains("Google Maps"));
    }

    @Test
    public void alternativasBusAnteriorYSiguientes() {
        // a las 8:28 el bus de las 8:30:30 aún se alcanza sin margen; el elegido es el de las 8:37:30
        LocalDateTime ahora = lunes(8, 28);
        assertEquals(lunes(8, 37).plusSeconds(30), planificador.planificar(residencia, etsiit, ahora).tramoBus().inicio);
        List<PlanRuta> alt = planificador.alternativas(residencia, etsiit, ahora);
        assertEquals(4, alt.size());
        assertEquals(lunes(8, 30).plusSeconds(30), alt.get(0).tramoBus().inicio);
        assertTrue(alt.get(0).salida.isBefore(ahora));
        for (int i = 1; i < alt.size(); i++) {
            Duration hueco = Duration.between(alt.get(i - 1).tramoBus().inicio, alt.get(i).tramoBus().inicio);
            assertEquals(Duration.ofMinutes(7), hueco);
        }

        // a las 8:30 ese ya no se alcanza: elegido + 2 siguientes
        List<PlanRuta> sinAnterior = planificador.alternativas(residencia, etsiit, lunes(8, 30));
        assertEquals(3, sinAnterior.size());
        assertEquals(lunes(8, 37).plusSeconds(30), sinAnterior.get(0).tramoBus().inicio);
    }

    @Test
    public void geocercaDeLosEdificios() {
        assertTrue(etsiit.contiene(etsiit.punto));
        assertTrue(auxiliar.contiene(auxiliar.punto));
        assertFalse(etsiit.contiene(residencia.punto));
        assertFalse(auxiliar.contiene(etsiit.punto));
    }
}
