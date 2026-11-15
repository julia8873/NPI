package com.example.nuietsiit.data.modelo;

import com.example.nuietsiit.R;

import java.time.LocalDate;

/**
 * Trámite de secretaría (tabla PROCEDIMIENTO).
 */
public class Tramite {
    // {clave en BD, etiqueta}: fuente única para los chips de la lista y las etiquetas
    public static final String[][] CATEGORIAS = {
            {"matricula", "Matrícula"},
            {"titulos", "Títulos"},
            {"reconocimiento_creditos", "Reconocimiento de créditos"},
            {"convocatorias", "Convocatorias"},
            {"certificados", "Certificados"},
            {"otro", "Otros"}};

    public final int id;
    public final String nombre;
    public final String categoria;       // 'matricula'|'titulos'|'reconocimiento_creditos'|'convocatorias'|'certificados'|'otro'
    public final String descripcion;
    public final String requisitos;      // puede ser null
    public final String plazosInfo;      // puede ser null
    public final String plazoInicio;     // ISO yyyy-MM-dd, puede ser null
    public final String plazoFin;        // ISO yyyy-MM-dd, puede ser null
    public final boolean solicitudOnline; // false = solo presencial (con cita previa)
    public final String palabrasClave;   // puede ser null

    public Tramite(int id, String nombre, String categoria, String descripcion,
                   String requisitos, String plazosInfo, String plazoInicio, String plazoFin,
                   boolean solicitudOnline, String palabrasClave) {
        this.id = id;
        this.nombre = nombre;
        this.categoria = categoria;
        this.descripcion = descripcion;
        this.requisitos = requisitos;
        this.plazosInfo = plazosInfo;
        this.plazoInicio = plazoInicio;
        this.plazoFin = plazoFin;
        this.solicitudOnline = solicitudOnline;
        this.palabrasClave = palabrasClave;
    }

    // texto sobre el que busca BuscadorTolerante (equivale a la columna textoBusqueda)
    public String textoBusqueda() {
        return nombre + " " + (palabrasClave != null ? palabrasClave : "") + " " + (descripcion != null ? descripcion : "");
    }

    // color de la categoría (tokens nui_tema_*): etiqueta de la ficha y tinta del catálogo
    public int colorCategoria() {
        return colorCategoria(categoria);
    }

    // también para las pestañas del fichero, que no tienen un trámite concreto
    public static int colorCategoria(String categoria) {
        switch (categoria) {
            case "matricula": return R.color.nui_tema_matricula;
            case "titulos": return R.color.nui_tema_titulos;
            case "reconocimiento_creditos": return R.color.nui_tema_creditos;
            case "convocatorias": return R.color.nui_tema_examenes;
            case "certificados": return R.color.nui_tema_certificados;
            default: return R.color.nui_tema_otros;
        }
    }

    public String etiquetaCategoria() {
        for (String[] c : CATEGORIAS) {
            if (c[0].equals(categoria)) return c[1];
        }
        return "Otros";
    }

    public boolean tienePlazo() {
        return plazoInicio != null && plazoFin != null;
    }

    // true si hoy está dentro de [plazoInicio, plazoFin] (para el chip "Plazo abierto")
    public boolean plazoAbierto() {
        if (!tienePlazo()) return false;
        LocalDate hoy = LocalDate.now();
        return !hoy.isBefore(LocalDate.parse(plazoInicio)) && !hoy.isAfter(LocalDate.parse(plazoFin));
    }

    // botón "Solicitar" habilitado: online y con plazo abierto o sin plazo
    public boolean sePuedeSolicitar() {
        return solicitudOnline && (!tienePlazo() || plazoAbierto());
    }

    // pastilla de estado (Detalle y "¿Qué necesitas?"): qué se puede hacer hoy
    public String textoEstado() {
        if (!solicitudOnline) return "En ventanilla, con cita";
        if (!tienePlazo()) return "Puedes solicitarlo ya";
        if (plazoAbierto()) return "Plazo abierto";
        return LocalDate.now().isBefore(LocalDate.parse(plazoInicio)) ? "Plazo aún no abierto" : "Plazo cerrado";
    }

    // recurso de color: nui_ok = se puede solicitar, nui_info = ventanilla, nui_apagado = fuera de plazo
    public int colorEstado() {
        if (sePuedeSolicitar()) return R.color.nui_ok;
        if (!solicitudOnline) return R.color.nui_info;
        return R.color.nui_apagado;
    }
}
