package com.example.nuietsiit.activities.secciones.biblioteca;

import com.example.nuietsiit.data.modelo.AsignaturaCatalogo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiPredicate;

/**
 * Lo elegido en el panel Explorar: grado, curso, rama, asignatura y planta (null = sin elegir).
 */
public final class FiltroExplorar {

    public Integer idGrado;
    public Integer curso;
    public String rama;
    public String asignatura;
    public String planta;

    private final List<AsignaturaCatalogo> catalogo;

    public FiltroExplorar(List<AsignaturaCatalogo> catalogo) {
        this.catalogo = catalogo;
    }

    public FiltroExplorar copia() {
        FiltroExplorar c = new FiltroExplorar(catalogo);
        c.copiarDe(this);
        return c;
    }

    public void copiarDe(FiltroExplorar otro) {
        idGrado = otro.idGrado;
        curso = otro.curso;
        rama = otro.rama;
        asignatura = otro.asignatura;
        planta = otro.planta;
    }

    public void limpiar() {
        idGrado = null;
        curso = null;
        rama = null;
        asignatura = null;
        planta = null;
    }

    public boolean activo() {
        return filtraAsignaturas() || planta != null;
    }

    // la planta sola no dice nada de las asignaturas
    public boolean filtraAsignaturas() {
        return idGrado != null || curso != null || rama != null || asignatura != null;
    }

    private boolean admite(AsignaturaCatalogo a) {
        return (idGrado == null || idGrado == a.idGrado)
                && (curso == null || curso == a.curso)
                && (rama == null || rama.equals(a.rama))
                && (asignatura == null || asignatura.equals(a.codigo));
    }

    // hay que comprobarlo fila a fila: "GII + 3º" no debe admitir una asignatura que es 3º solo en otro grado
    public Set<String> codigos() {
        Set<String> codigos = new LinkedHashSet<>();
        for (AsignaturaCatalogo a : catalogo) {
            if (admite(a)) codigos.add(a.codigo);
        }
        return codigos;
    }

    // comprobador(asignaturas del libro, planta del libro); las asignaturas admitidas se calculan una vez, no por libro
    public BiPredicate<Collection<String>, String> comprobador() {
        String plantaElegida = planta;
        Set<String> admitidas = filtraAsignaturas() ? codigos() : null;
        return (codigosLibro, plantaLibro) -> (plantaElegida == null || plantaElegida.equals(plantaLibro))
                && (admitidas == null || !Collections.disjoint(admitidas, codigosLibro));
    }

    public boolean admiteLibro(Collection<String> codigosLibro, String plantaLibro) {
        return comprobador().test(codigosLibro, plantaLibro);
    }

    // para el chip de Explorar: "GII · 2º"
    public String resumen() {
        List<String> partes = new ArrayList<>();
        for (AsignaturaCatalogo a : catalogo) {
            if (Objects.equals(idGrado, a.idGrado)) {
                partes.add(a.gradoCorto());
                break;
            }
        }
        if (curso != null) partes.add(curso + "º");
        if (rama != null) partes.add(rama);
        if (asignatura != null) partes.add(asignatura);
        if (planta != null) partes.add(planta);
        return String.join(" · ", partes);
    }
}
