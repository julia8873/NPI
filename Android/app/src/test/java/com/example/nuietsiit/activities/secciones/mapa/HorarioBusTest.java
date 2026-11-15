package com.example.nuietsiit.activities.secciones.mapa;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.BeforeClass;
import org.junit.Test;

import java.io.FileInputStream;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class HorarioBusTest {

    static final String JSON = "src/main/assets/" + RedTransporte.RUTA_ASSET;

    private static RedTransporte red;
    private static HorarioBus horario;
    private static RedTransporte.Sentido haciaChana, haciaRebites;

    @BeforeClass
    public static void cargar() throws Exception {
        red = cargarRed();
        RedTransporte.Linea l9 = red.linea("9");
        horario = new HorarioBus(l9);
        haciaChana = l9.sentidos.get(0);
        haciaRebites = l9.sentidos.get(1);
    }

    static RedTransporte cargarRed() throws Exception {
        try (InputStream in = new FileInputStream(JSON)) {
            return RedTransporte.cargar(in);
        }
    }

    static RedTransporte.Parada parada(RedTransporte.Sentido s, String nombre) {
        for (RedTransporte.Parada p : s.paradas) {
            if (p.nombre.equals(nombre)) return p;
        }
        throw new AssertionError("no está la parada " + nombre);
    }

    // lunes 9 y martes 10 de noviembre de 2026; sábado 14
    private static LocalDateTime lunes(int h, int m) {
        return LocalDateTime.of(2026, 11, 9, h, m);
    }

    @Test
    public void cargaLaRed() {
        assertEquals(3, red.lugares.size());
        assertEquals("chana", haciaChana.id);
        assertEquals(33, haciaChana.paradas.size());
        assertEquals(35, haciaRebites.paradas.size());
        assertEquals(6, red.tramosAPie.size());
    }

    @Test
    public void laborableSigueLasFranjas() {
        List<LocalDateTime> s = horario.salidas(haciaChana, LocalDate.of(2026, 11, 9));
        assertEquals(lunes(6, 30), s.get(0));
        assertEquals(lunes(6, 42), s.get(1));            // 06:30–07:30 cada 12
        assertEquals(lunes(23, 24), s.get(s.size() - 1)); // la última no pasa de 23:30
        assertEquals(true, s.contains(lunes(8, 5)));
        assertEquals(true, s.contains(lunes(8, 12)));     // 07:30–09:30 cada 7
    }

    @Test
    public void elOtroSentidoEmpiezaMasTarde() {
        List<LocalDateTime> s = horario.salidas(haciaRebites, LocalDate.of(2026, 11, 9));
        assertEquals(lunes(6, 50), s.get(0));
        assertEquals(lunes(23, 26), s.get(s.size() - 1));
    }

    @Test
    public void findeCadaCuartoDeHora() {
        LocalDate sabado = LocalDate.of(2026, 11, 14);
        List<LocalDateTime> s = horario.salidas(haciaChana, sabado);
        assertEquals(sabado.atTime(6, 45), s.get(1));
        assertEquals(sabado.atTime(23, 30), s.get(s.size() - 1));
        assertEquals(69, s.size());
    }

    @Test
    public void proximoPasoSumaElMinutoDeLaParada() {
        RedTransporte.Parada ronda210 = parada(haciaChana, "Camino de Ronda 210");
        HorarioBus.Paso p = horario.proximoPaso(haciaChana, ronda210, lunes(8, 30));
        assertEquals(lunes(8, 30).plusSeconds(30), p.hora);  // salida 8:05 + 25,5 min
        assertEquals(lunes(8, 5), p.salidaCabecera);
        RedTransporte.Parada bellasArtes = parada(haciaChana, "Avenida de Andalucía - Facultad Bellas Artes");
        assertEquals(lunes(8, 36), p.horaEn(bellasArtes));
    }

    @Test
    public void trasElUltimoBusVieneElDeLaManana() {
        RedTransporte.Parada ronda210 = parada(haciaChana, "Camino de Ronda 210");
        HorarioBus.Paso p = horario.proximoPaso(haciaChana, ronda210, lunes(0, 30).plusDays(1));
        assertEquals(lunes(6, 55).plusDays(1).plusSeconds(30), p.hora);
    }

    @Test
    public void elUltimoBusCruzaLaMedianoche() {
        RedTransporte.Parada rebites = parada(haciaRebites, "Los Rebites");
        HorarioBus.Paso p = horario.proximoPaso(haciaRebites, rebites, lunes(23, 59));
        assertEquals(lunes(23, 26), p.salidaCabecera);
        assertEquals(lunes(0, 3).plusDays(1).plusSeconds(6), p.hora);   // + 37,1 min
    }

    @Test
    public void viernesNocheSaltaAlHorarioDeFinde() {
        RedTransporte.Parada ronda210 = parada(haciaChana, "Camino de Ronda 210");
        LocalDateTime viernes = LocalDateTime.of(2026, 11, 13, 23, 59);
        List<HorarioBus.Paso> pasos = horario.proximosPasos(haciaChana, ronda210, viernes, 2);
        assertEquals(LocalDateTime.of(2026, 11, 14, 6, 55, 30), pasos.get(0).hora);
        assertEquals(LocalDateTime.of(2026, 11, 14, 7, 10, 30), pasos.get(1).hora);
    }

    @Test
    public void pasoAnterior() {
        RedTransporte.Parada ronda210 = parada(haciaChana, "Camino de Ronda 210");
        HorarioBus.Paso p = horario.pasoAnterior(haciaChana, ronda210, lunes(8, 37).plusSeconds(30));
        assertNotNull(p);
        assertEquals(lunes(8, 30).plusSeconds(30), p.hora);
    }
}
