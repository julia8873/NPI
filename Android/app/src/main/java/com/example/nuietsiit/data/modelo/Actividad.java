package com.example.nuietsiit.data.modelo;

import com.example.nuietsiit.util.FechasUtil;
import com.example.nuietsiit.util.TextoUtil;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Una actividad (tabla ACTIVIDAD) con los datos extra de CURSO_ABIERTAUGR si es un curso,
 * y si el estudiante actual está inscrito.
 */
public class Actividad {

    // {valor en la BD, texto del chip}
    public static final String[][] TIPOS = {
            {"charla", "Charlas"},
            {"bibliomaker", "Bibliomaker"},
            {"abiertaugr", "AbiertaUGR"},
            {"dia_etsiit", "Día ETSIIT"},
            {"otro", "Otras"}
    };

    public int id;
    public String titulo;
    public String descripcion;
    public String tipo;
    public LocalDateTime inicio;
    public LocalDateTime fin;       // puede ser null
    public String enlace;           // puede ser null
    public String lugar;            // nombre del ESPACIO, puede ser null

    // Solo cursos AbiertaUGR (null en el resto)
    public Double creditos;
    public LocalDate plazoInicio;
    public LocalDate plazoFin;
    public Integer plazasTotales;   // null = sin límite

    public int inscritos;           // personas apuntadas
    public boolean inscrito;        // ¿está apuntado el usuario actual?

    public boolean esAbiertaUGR() {
        return creditos != null && plazoInicio != null && plazoFin != null;
    }

    public boolean plazoAbierto() {
        if (!esAbiertaUGR()) return true;
        LocalDate hoy = LocalDate.now();
        return !hoy.isBefore(plazoInicio) && !hoy.isAfter(plazoFin);
    }

    public boolean completa() {
        return plazasTotales != null && inscritos >= plazasTotales;
    }

    /** true si el plazo de inscripción acaba en 3 días o menos (se pinta en rojo) */
    public boolean inscripcionUrgente() {
        return esAbiertaUGR() && plazoAbierto() && !completa() && FechasUtil.diasHasta(plazoFin) <= 3;
    }

    /** Texto del estado de inscripción de un curso AbiertaUGR */
    public String estadoInscripcion() {
        if (!esAbiertaUGR()) return "";
        LocalDate hoy = LocalDate.now();
        if (hoy.isBefore(plazoInicio)) return "Inscripción desde el " + FechasUtil.diaCorto(plazoInicio);
        if (hoy.isAfter(plazoFin)) return "Inscripción cerrada";
        if (completa()) return "Plazas agotadas";
        long dias = FechasUtil.diasHasta(plazoFin);
        if (dias == 0) return "⏳ ¡Hoy es el último día para inscribirte!";
        if (dias <= 3) return "⏳ Quedan " + dias + (dias == 1 ? " día" : " días") + " para inscribirte";
        return "Inscripción abierta hasta el " + FechasUtil.diaCorto(plazoFin);
    }

    public String creditosTexto() {
        if (creditos == null) return "";
        double c = creditos;
        return (c == Math.floor(c) ? String.valueOf((int) c) : String.valueOf(c)) + " ECTS";
    }

    public String textoBusqueda() {
        return TextoUtil.textoBusqueda(titulo, descripcion, nombreTipo(tipo), lugar);
    }

    // -------------------- Aspecto según el tipo --------------------

    public static String emoji(String tipo) {
        switch (tipo) {
            case "charla": return "🎤";
            case "bibliomaker": return "🛠️";
            case "abiertaugr": return "🎓";
            case "dia_etsiit": return "🎉";
            default: return "✨";
        }
    }

    public static String nombreTipo(String tipo) {
        switch (tipo) {
            case "charla": return "Charla";
            case "bibliomaker": return "Bibliomaker";
            case "abiertaugr": return "Curso AbiertaUGR";
            case "dia_etsiit": return "Día de la ETSIIT";
            default: return "Otra actividad";
        }
    }

    public static int color(String tipo) {
        switch (tipo) {
            case "charla": return 0xFF8E24AA;      // morado
            case "bibliomaker": return 0xFF2E7D32; // verde
            case "abiertaugr": return 0xFF1565C0;  // azul
            case "dia_etsiit": return 0xFFFF8C42;  // naranja de la app
            default: return 0xFF546E7A;            // gris azulado
        }
    }
}
