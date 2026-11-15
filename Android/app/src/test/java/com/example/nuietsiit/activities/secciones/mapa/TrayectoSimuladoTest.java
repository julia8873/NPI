package com.example.nuietsiit.activities.secciones.mapa;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.BeforeClass;
import org.junit.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.example.nuietsiit.activities.secciones.mapa.SeguimientoRuta.Etapa;
import com.example.nuietsiit.activities.secciones.mapa.SeguimientoRuta.Estado;
import com.example.nuietsiit.activities.secciones.mapa.SeguimientoRuta.Evento;

/** La simulación de la guía (×10 en la app) recorre todas las etapas sin perder el bus. */
public class TrayectoSimuladoTest {

    private static RedTransporte red;
    private static PlanificadorRuta planificador;

    @BeforeClass
    public static void cargar() throws Exception {
        red = HorarioBusTest.cargarRed();
        planificador = new PlanificadorRuta(red);
    }

    /** Simula cada 5 s (lo que hace la app cada 0,5 s a ×10) y devuelve los eventos en orden. */
    private static List<Evento.Tipo> simular(PlanRuta plan, List<Etapa> etapas) {
        TrayectoSimulado trayecto = new TrayectoSimulado(plan);
        SeguimientoRuta seg = new SeguimientoRuta(plan);
        List<Evento.Tipo> eventos = new ArrayList<>();
        LocalDateTime t = trayecto.inicio();
        while (seg.etapa() != Etapa.LLEGADO && t.isBefore(plan.llegada.plusMinutes(2))) {
            TrayectoSimulado.Muestra m = trayecto.en(t);
            Estado e = seg.actualizar(m.posicion, m.velocidad, t);
            for (Evento ev : e.eventos) eventos.add(ev.tipo);
            if (etapas.isEmpty() || etapas.get(etapas.size() - 1) != e.etapa) etapas.add(e.etapa);
            t = t.plusSeconds(5);
        }
        return eventos;
    }

    @Test
    public void residenciaAEtsiitRecorreTodasLasEtapas() {
        PlanRuta plan = planificador.planificar(red.lugar("livensa_fuentenueva"), red.lugar("etsiit"),
                LocalDateTime.of(2026, 11, 9, 8, 30));
        assertEquals(PlanRuta.Tipo.BUS, plan.tipo);
        List<Etapa> etapas = new ArrayList<>();
        List<Evento.Tipo> eventos = simular(plan, etapas);

        assertTrue(etapas.toString(), etapas.contains(Etapa.EN_BUS));
        assertTrue(etapas.toString(), etapas.contains(Etapa.BAJATE));
        assertTrue(etapas.toString(), etapas.contains(Etapa.AL_DESTINO));
        assertEquals(Etapa.LLEGADO, etapas.get(etapas.size() - 1));
        assertTrue(eventos.toString(), eventos.contains(Evento.Tipo.SAL_YA));
        assertTrue(eventos.toString(), eventos.contains(Evento.Tipo.BAJATE_AHORA));
        assertFalse(eventos.toString(), eventos.contains(Evento.Tipo.BUS_PERDIDO));
        assertEquals(Evento.Tipo.LLEGADO, eventos.get(eventos.size() - 1));
    }

    @Test
    public void etsiitAlAuxiliarAPie() {
        PlanRuta plan = planificador.planificar(red.lugar("etsiit"), red.lugar("edificio_auxiliar"),
                LocalDateTime.of(2026, 11, 9, 10, 0));
        List<Etapa> etapas = new ArrayList<>();
        List<Evento.Tipo> eventos = simular(plan, etapas);
        assertEquals(Etapa.LLEGADO, etapas.get(etapas.size() - 1));
        assertEquals(List.of(Evento.Tipo.LLEGADO), eventos);
    }

    @Test
    public void antesDeSalirEstaEnElOrigenYQuieto() {
        PlanRuta plan = planificador.planificar(red.lugar("livensa_fuentenueva"), red.lugar("etsiit"),
                LocalDateTime.of(2026, 11, 9, 8, 30));
        TrayectoSimulado.Muestra m = new TrayectoSimulado(plan).en(plan.salida.minusMinutes(1));
        assertEquals(plan.origen, m.posicion);
        assertEquals(0, m.velocidad, 0);
    }
}
