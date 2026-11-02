package com.example.nuietsiit.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

public class BuscadorToleranteTest {

    // { nombre, palabrasClave + descripción } como los trámites de ejemplo
    private static final List<String[]> TRAMITES = Arrays.asList(
            new String[]{"Automatrícula", "matricularme, inscribirme, asignaturas, oficina virtual"},
            new String[]{"Anulación de matrícula", "anular, darme de baja, baja, dejar el grado, devolución"},
            new String[]{"Solicitud del título de grado y SET", "título, graduarme, suplemento europeo"},
            new String[]{"Reconocimiento y transferencia de créditos",
                    "convalidar, convalidación, reconocer creditos, ciclo superior"},
            new String[]{"Evaluación única final", "examen único, no puedo ir a clase, evaluación única"},
            new String[]{"Certificado académico personal", "certificado de notas, expediente, nota media, calificaciones"},
            new String[]{"Cambio de grupo", "grupo, horario, cambiar de grupo, permuta"},
            new String[]{"Beca de colaboración", "beca, ayuda, colaborar"});

    private static String primero(String consulta) {
        BuscadorTolerante.Busqueda<String[]> b = buscar(consulta);
        return b.elementos.isEmpty() ? null : b.elementos.get(0)[0];
    }

    private static BuscadorTolerante.Busqueda<String[]> buscar(String consulta) {
        return BuscadorTolerante.buscar(consulta, TRAMITES, t -> t[0], t -> t[0] + " " + t[1]);
    }

    @Test
    public void tildesYMayusculas() {
        assertEquals("Automatrícula", primero("AUTOMATRICULA"));
        assertEquals("Evaluación única final", primero("evaluacion unica"));
    }

    @Test
    public void faltasDeOrtografia() {
        assertEquals("Reconocimiento y transferencia de créditos", primero("convalidasion"));
        assertEquals("Reconocimiento y transferencia de créditos", primero("rekonocimiento"));
        assertEquals("Evaluación única final", primero("ivaluacion"));
    }

    @Test
    public void letrasRepetidasDeMasOCambiadas() {
        assertEquals("Anulación de matrícula", primero("matriiicula"));
        assertEquals("Anulación de matrícula", primero("matricla"));
        assertEquals("Anulación de matrícula", primero("matirucla"));
        assertEquals("Certificado académico personal", primero("certifcado notas"));
    }

    @Test
    public void palabrasAMedias() {
        assertEquals("Anulación de matrícula", primero("matri"));
        assertEquals(2, buscar("matri").elementos.size());
        assertEquals("Reconocimiento y transferencia de créditos", primero("reco cred"));
    }

    @Test
    public void primerasLetrasYaFiltran() {
        // con 1–2 letras salen solo los que tienen una palabra que empieza así
        BuscadorTolerante.Busqueda<String[]> b = buscar("ma");
        assertEquals(2, b.elementos.size());
        assertNull(b.mostrandoPara);
        assertEquals("Beca de colaboración", primero("be"));
        assertEquals(1, buscar("be").elementos.size());
    }

    @Test
    public void sinonimosDePalabrasClave() {
        assertEquals("Reconocimiento y transferencia de créditos", primero("convalidar"));
        assertEquals("Cambio de grupo", primero("permuta"));
        assertEquals("Anulación de matrícula", primero("darme de baja"));
    }

    @Test
    public void becaNoCasaConBaja() {
        BuscadorTolerante.Busqueda<String[]> b = buscar("beca");
        assertEquals(1, b.elementos.size());
        assertEquals("Beca de colaboración", b.elementos.get(0)[0]);
    }

    @Test
    public void todasLasPalabrasTienenPrioridad() {
        BuscadorTolerante.Busqueda<String[]> b = buscar("anulacion matricula");
        assertEquals(1, b.elementos.size());
        assertEquals("Anulación de matrícula", b.elementos.get(0)[0]);
    }

    @Test
    public void consultaVaciaDevuelveTodos() {
        assertEquals(TRAMITES.size(), buscar("").elementos.size());
        assertEquals(TRAMITES.size(), buscar("  de la ").elementos.size());
    }

    @Test
    public void mensajesDeCorreccionYSugerencia() {
        BuscadorTolerante.Busqueda<String[]> aprox = buscar("matricla");
        assertNotNull(aprox.mostrandoPara);
        assertTrue(aprox.mostrandoPara.contains("matricula"));

        assertNull(buscar("matricula").mostrandoPara);

        BuscadorTolerante.Busqueda<String[]> nada = buscar("xyzxyz");
        assertTrue(nada.elementos.isEmpty());
        assertNull(nada.sugerencia);
    }

    @Test
    public void claveFonetica() {
        assertEquals(BuscadorTolerante.clavefonetica("convalidacion"),
                BuscadorTolerante.clavefonetica("conbalidasion"));
        assertEquals(BuscadorTolerante.clavefonetica("queso"), BuscadorTolerante.clavefonetica("keso"));
        assertEquals(BuscadorTolerante.clavefonetica("hola"), BuscadorTolerante.clavefonetica("ola"));
        // "gue/gui" es g suave, "ge/gi" suena a j
        assertEquals("gion", BuscadorTolerante.clavefonetica("guion"));
        assertEquals(BuscadorTolerante.clavefonetica("jion"), BuscadorTolerante.clavefonetica("gion"));
        assertTrue(!BuscadorTolerante.clavefonetica("guion").equals(BuscadorTolerante.clavefonetica("jion")));
    }
}
