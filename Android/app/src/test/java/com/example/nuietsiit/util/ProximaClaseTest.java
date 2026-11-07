package com.example.nuietsiit.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class ProximaClaseTest {

    // lunes: NPI 08:30-10:30 y PL 10:30-12:30; jueves: PL 08:30-10:30
    private final List<ProximaClase.Sesion> horario = Arrays.asList(
            new ProximaClase.Sesion("PL", 4, "08:30", "10:30"),
            new ProximaClase.Sesion("PL", 1, "10:30", "12:30"),
            new ProximaClase.Sesion("NPI", 1, "08:30", "10:30"));

    private static LocalDateTime lunesA(int hora, int minuto) {
        return LocalDateTime.of(2026, 11, 9, hora, minuto);
    }

    private void comprobar(LocalDateTime ahora, String codigo, String cuando) {
        ProximaClase p = ProximaClase.de(horario, ahora);
        assertEquals(codigo, p.codigo);
        assertEquals(cuando, p.cuando);
    }

    @Test public void antesDeLaPrimeraDelDia() { comprobar(lunesA(7, 0), "NPI", "hoy 08:30"); }
    @Test public void clasEnCursoEsAhora() { comprobar(lunesA(9, 0), "NPI", "ahora"); }
    @Test public void alTerminarPasaALaSiguiente() { comprobar(lunesA(10, 30), "PL", "ahora"); }
    @Test public void acabadasLasDeHoyVaAOtroDia() { comprobar(lunesA(13, 0), "PL", "jue 08:30"); }
    @Test public void diaAnteriorEsManana() { comprobar(lunesA(13, 0).plusDays(2), "PL", "mañana 08:30"); }
    @Test public void finDeSemanaDaLaVueltaAlLunes() { comprobar(lunesA(13, 0).minusDays(2), "NPI", "lun 08:30"); }
    @Test public void horarioVacioEsNull() { assertNull(ProximaClase.de(Collections.emptyList(), lunesA(9, 0))); }

    @Test
    public void unaSolaClaseYaPasadaHoyVuelveLaSemanaQueViene() {
        List<ProximaClase.Sesion> una = Collections.singletonList(new ProximaClase.Sesion("NPI", 1, "08:30", "10:30"));
        assertEquals("lun 08:30", ProximaClase.de(una, lunesA(11, 0)).cuando);
    }
}
