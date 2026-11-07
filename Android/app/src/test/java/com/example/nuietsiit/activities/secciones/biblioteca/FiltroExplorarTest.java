package com.example.nuietsiit.activities.secciones.biblioteca;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.example.nuietsiit.data.modelo.AsignaturaCatalogo;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class FiltroExplorarTest {

    private static final String GII = "Grado en Ingeniería Informática";
    private static final String DGIIM = "Doble Grado en Ingeniería Informática y Matemáticas";

    private final List<AsignaturaCatalogo> catalogo = Arrays.asList(
            new AsignaturaCatalogo("ALG", "Algorítmica", 1, GII, 2, null),
            new AsignaturaCatalogo("ALG", "Algorítmica", 2, DGIIM, 3, null),
            new AsignaturaCatalogo("NPI", "Nuevos Paradigmas", 1, GII, 4, "CSI"));

    private final FiltroExplorar filtro = new FiltroExplorar(catalogo);

    @Test
    public void gradoYCursoDebenCoincidirEnLaMismaFila() {
        filtro.idGrado = 1;
        filtro.curso = 3; // ALG es 3º solo en el doble grado
        assertFalse(filtro.admiteLibro(Collections.singletonList("ALG"), null));
        filtro.idGrado = 2;
        assertTrue(filtro.admiteLibro(Collections.singletonList("ALG"), null));
    }

    @Test
    public void ramaDejaFueraLasDeFormacionComun() {
        filtro.rama = "CSI";
        assertFalse(filtro.admiteLibro(Collections.singletonList("ALG"), null));
        assertTrue(filtro.admiteLibro(Arrays.asList("ALG", "NPI"), null));
    }

    @Test
    public void soloPlantaAdmiteCualquierAsignatura() {
        filtro.planta = "Planta 1";
        assertTrue(filtro.admiteLibro(Collections.emptyList(), "Planta 1"));
        assertFalse(filtro.admiteLibro(Collections.singletonList("ALG"), "Planta 2"));
    }

    @Test
    public void soloPlantaNoFiltraAsignaturas() {
        filtro.planta = "Planta 1";
        assertTrue(filtro.activo());
        assertFalse(filtro.filtraAsignaturas());
        filtro.curso = 2;
        assertTrue(filtro.filtraAsignaturas());
    }

    @Test
    public void comprobadorCombinaPlantaYAsignatura() {
        filtro.asignatura = "NPI";
        filtro.planta = "Planta 1";
        assertTrue(filtro.comprobador().test(Collections.singletonList("NPI"), "Planta 1"));
        assertFalse(filtro.comprobador().test(Collections.singletonList("NPI"), "Planta 2"));
        assertFalse(filtro.comprobador().test(Collections.singletonList("ALG"), "Planta 1"));
    }

    @Test
    public void resumenUsaGradoCorto() {
        assertFalse(filtro.activo());
        filtro.idGrado = 2;
        filtro.curso = 3;
        assertTrue(filtro.activo());
        assertEquals("DGIIM · 3º", filtro.resumen());
    }

    @Test
    public void limpiarDejaElFiltroInactivo() {
        filtro.asignatura = "NPI";
        filtro.limpiar();
        assertFalse(filtro.activo());
    }
}
