package com.example.nuietsiit.activities.secciones.mapa;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.BeforeClass;
import org.junit.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.example.nuietsiit.activities.secciones.mapa.SeguimientoRuta.Etapa;
import com.example.nuietsiit.activities.secciones.mapa.SeguimientoRuta.Estado;
import com.example.nuietsiit.activities.secciones.mapa.SeguimientoRuta.Evento;

public class SeguimientoRutaTest {

    private static final double ANDANDO = 1.25, BUS = 8;

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

    private static LocalDateTime lunes(int h, int m, int s) {
        return LocalDateTime.of(2026, 11, 9, h, m, s);
    }

    private static List<Evento.Tipo> tipos(Estado e) {
        List<Evento.Tipo> tipos = new ArrayList<>();
        for (Evento ev : e.eventos) tipos.add(ev.tipo);
        return tipos;
    }

    private static Punto mitad(List<Punto> g) {
        return g.get(g.size() / 2);
    }

    private static Punto ultimo(List<Punto> g) {
        return g.get(g.size() - 1);
    }

    @Test
    public void residenciaAEtsiitPasoAPaso() {
        PlanRuta plan = planificador.planificar(residencia, etsiit, lunes(8, 30, 0));
        PlanRuta.Tramo aParada = plan.tramos.get(0), bus = plan.tramoBus(), aEtsiit = plan.tramos.get(3);
        SeguimientoRuta seg = new SeguimientoRuta(plan);

        // 8:30 en casa: aún no toca salir (sale 8:33, bus 8:37:30)
        Estado e = seg.actualizar(plan.origen, 0, lunes(8, 30, 0));
        assertEquals(Etapa.POR_SALIR, e.etapa);
        assertEquals("Sal en 3 min", e.cuentaAtras);
        assertEquals(180, e.segundosCuentaAtras);
        assertEquals(plan.llegada, e.llegada);
        assertTrue(e.eventos.isEmpty());

        // 8:33 sigue en casa → "Sal ya", una sola vez
        e = seg.actualizar(plan.origen, 0, lunes(8, 33, 0));
        assertEquals(Etapa.A_LA_PARADA, e.etapa);
        assertEquals(List.of(Evento.Tipo.SAL_YA), tipos(e));
        assertEquals("Tu bus pasa en 5 min", e.cuentaAtras);
        assertTrue(seg.actualizar(plan.origen, 0, lunes(8, 33, 5)).eventos.isEmpty());

        // a mitad de camino: rumbo a la parada y misma llegada
        Punto medio = mitad(aParada.geometria);
        e = seg.actualizar(medio, ANDANDO, lunes(8, 34, 15));
        assertEquals(Etapa.A_LA_PARADA, e.etapa);
        assertEquals(bus.subida.punto, e.objetivo);
        assertEquals(medio.rumboA(bus.subida.punto), e.rumbo, 1e-9);
        assertEquals(plan.llegada, e.llegada);
        assertEquals(aParada.idFoto, e.tramo.idFoto);
        assertTrue(e.instruccion.startsWith("Camina hasta Camino de Ronda 210"));

        // en la parada
        e = seg.actualizar(bus.subida.punto, 0, lunes(8, 35, 30));
        assertEquals(Etapa.ESPERANDO, e.etapa);
        assertEquals("Tu bus pasa en 2 min", e.cuentaAtras);
        assertEquals(lunes(8, 37, 30), e.bus.hora);
        e = seg.actualizar(bus.subida.punto, 0, lunes(8, 36, 30));
        assertEquals(List.of(Evento.Tipo.BUS_EN_1_MIN), tipos(e));
        assertTrue(seg.actualizar(bus.subida.punto, 0, lunes(8, 36, 45)).eventos.isEmpty());

        // sube: rápido y sobre la línea
        e = seg.actualizar(bus.geometria.get(2), BUS, lunes(8, 37, 45));
        assertEquals(Etapa.EN_BUS, e.etapa);
        assertEquals("bus_9", e.tramo.idFoto);
        assertTrue(e.paradasRestantes > 1);
        assertTrue(e.instruccion.startsWith("Bájate en Avenida de Andalucía - Facultad Bellas Artes"));

        // en la parada anterior a la bajada (parado un momento: sigue en el bus)
        List<RedTransporte.Parada> paradas = bus.sentido.paradas;
        RedTransporte.Parada previa = paradas.get(paradas.indexOf(bus.bajada) - 1);
        e = seg.actualizar(previa.punto, 0, lunes(8, 41, 30));
        assertEquals(Etapa.EN_BUS, e.etapa);
        assertEquals(1, e.paradasRestantes);
        assertEquals(List.of(Evento.Tipo.SIGUIENTE_ES_TU_PARADA), tipos(e));
        // llegada recalculada con el minuto de paso de las paradas
        long bajaEn = Math.round((bus.bajada.minuto - previa.minuto) * 60);
        // (la parada no cae justo en un vértice del trazado: ±5 s)
        assertEquals(bajaEn, java.time.Duration.between(lunes(8, 41, 30), e.hito).getSeconds(), 5);

        // llegando a la bajada
        e = seg.actualizar(bus.bajada.punto, BUS, lunes(8, 42, 55));
        assertEquals(Etapa.BAJATE, e.etapa);
        assertEquals(List.of(Evento.Tipo.BAJATE_AHORA), tipos(e));
        e = seg.actualizar(bus.bajada.punto, 0, lunes(8, 43, 5));
        assertEquals(Etapa.BAJATE, e.etapa);
        assertTrue(e.eventos.isEmpty());

        // andando a la ETSIIT
        Punto haciaEtsiit = mitad(aEtsiit.geometria);
        e = seg.actualizar(haciaEtsiit, ANDANDO, lunes(8, 46, 0));
        assertEquals(Etapa.AL_DESTINO, e.etapa);
        assertEquals("etsiit", e.tramo.idFoto);
        assertEquals(ultimo(aEtsiit.geometria), e.objetivo);
        assertTrue(e.cuentaAtras.startsWith("Quedan "));
        assertTrue(e.llegada.isAfter(lunes(8, 46, 0)) && e.llegada.isBefore(lunes(8, 51, 0)));

        // llegada
        e = seg.actualizar(ultimo(aEtsiit.geometria), 0, lunes(8, 49, 10));
        assertEquals(Etapa.LLEGADO, e.etapa);
        assertEquals(List.of(Evento.Tipo.LLEGADO), tipos(e));
        assertNull(e.objetivo);
        assertEquals(lunes(8, 49, 10), e.llegada);
        assertTrue(seg.actualizar(ultimo(aEtsiit.geometria), 0, lunes(8, 49, 20)).eventos.isEmpty());
    }

    @Test
    public void siLlegaTardeALaParadaCambiaAlSiguienteBus() {
        PlanRuta plan = planificador.planificar(residencia, etsiit, lunes(8, 30, 0));
        PlanRuta.Tramo bus = plan.tramoBus();
        SeguimientoRuta seg = new SeguimientoRuta(plan);
        seg.actualizar(plan.origen, 0, lunes(8, 33, 0));
        // 8:39 aún en casa: no llega al de las 8:37:30
        Estado e = seg.actualizar(plan.origen, 0, lunes(8, 39, 0));
        assertEquals(List.of(Evento.Tipo.BUS_PERDIDO), tipos(e));
        assertTrue(e.bus.hora.isAfter(lunes(8, 41, 0)));
        assertTrue(e.llegada.isAfter(plan.llegada));
        assertEquals(e.bus.horaEn(bus.bajada).plus(java.time.Duration.between(
                plan.tramos.get(3).inicio, plan.tramos.get(3).fin)), e.llegada);
        // el aviso de 1 min vale para el bus nuevo
        e = seg.actualizar(bus.subida.punto, 0, e.bus.hora.minusSeconds(50));
        assertEquals(List.of(Evento.Tipo.BUS_EN_1_MIN), tipos(e));
    }

    @Test
    public void sinVelocidadDelGpsLaEstimaConLaLecturaAnterior() {
        PlanRuta plan = planificador.planificar(residencia, etsiit, lunes(8, 30, 0));
        PlanRuta.Tramo bus = plan.tramoBus();
        SeguimientoRuta seg = new SeguimientoRuta(plan);
        seg.actualizar(bus.subida.punto, Double.NaN, lunes(8, 37, 30));
        assertEquals(Etapa.ESPERANDO, seg.etapa());
        // ~10 vértices de trazado en 10 s: mucho más de 12 km/h
        seg.actualizar(bus.geometria.get(Math.min(10, bus.geometria.size() - 1)), Double.NaN, lunes(8, 37, 40));
        assertEquals(Etapa.EN_BUS, seg.etapa());
    }

    @Test
    public void etsiitAlAuxiliarAPie() {
        PlanRuta plan = planificador.planificar(etsiit, auxiliar, lunes(10, 0, 0));
        assertEquals(PlanRuta.Tipo.A_PIE, plan.tipo);
        SeguimientoRuta seg = new SeguimientoRuta(plan);
        PlanRuta.Tramo tramo = plan.tramos.get(0);
        Estado e = seg.actualizar(mitad(tramo.geometria), ANDANDO, lunes(10, 3, 0));
        assertEquals(Etapa.AL_DESTINO, e.etapa);
        assertTrue(e.eventos.isEmpty());
        assertNull(e.bus);
        assertEquals(ultimo(tramo.geometria), e.objetivo);
        e = seg.actualizar(auxiliar.punto, 0, lunes(10, 8, 0));
        assertEquals(Etapa.LLEGADO, e.etapa);
        assertEquals("Has llegado a " + auxiliar.corto, e.eventos.get(0).texto);
    }

    @Test(expected = IllegalArgumentException.class)
    public void unPlanLejosNoSeSigue() {
        PlanRuta plan = planificador.planificar(new Punto(37.0, -3.4), etsiit, lunes(10, 0, 0));
        assertEquals(PlanRuta.Tipo.LEJOS, plan.tipo);
        new SeguimientoRuta(plan);
    }
}
