package com.example.nuietsiit.data.modelo;

import android.content.Context;

import androidx.annotation.ColorRes;
import androidx.annotation.DrawableRes;

import com.example.nuietsiit.R;

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
    // Filtros en la seccion
    public static final String[][] TIPOS = {
            {"charla", "Charlas"},
            {"bibliomaker", "Bibliomaker"},
            {"abiertaugr", "AbiertaUGR"},
            {"dia_etsiit", "Día ETSIIT"},
            {"otro", "Otras"}
    };

    // Columnas de tabla
    public int id;
    public String titulo;           // en el idioma de la app (traducido si hace falta)
    public String tituloEs;         // título original en español: es la clave de los QR de los carteles
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


    //No son columnas, se calcula
    public int inscritos;           // personas apuntadas
    public boolean inscrito;        // ¿está apuntado el usuario actual?


    // Metodos que responden preguntas
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
    /** true si el plazo de inscripción acaba en 3 días o menos (se pinta    en rojo) */
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
        if (dias == 0) return "¡Hoy es el último día para inscribirte!";
        if (dias <= 3) return "Quedan " + dias + (dias == 1 ? " día" : " días") + " para inscribirte";
        return "Inscripción abierta hasta el " + FechasUtil.diaCorto(plazoFin);
    }

    /** Estado de inscripción en el idioma de la app (pantallas). */
    public String estadoInscripcion(Context ctx) {
        if (!esAbiertaUGR()) return "";
        LocalDate hoy = LocalDate.now();
        if (hoy.isBefore(plazoInicio)) return ctx.getString(R.string.nui_insc_desde_fmt, FechasUtil.diaCorto(plazoInicio));
        if (hoy.isAfter(plazoFin)) return ctx.getString(R.string.nui_insc_cerrada);
        if (completa()) return ctx.getString(R.string.nui_act_btn_agotadas);
        long dias = FechasUtil.diasHasta(plazoFin);
        if (dias == 0) return ctx.getString(R.string.nui_insc_hoy);
        if (dias <= 3) return ctx.getResources().getQuantityString(R.plurals.nui_insc_quedan, (int) dias, (int) dias);
        return ctx.getString(R.string.nui_insc_abierta_hasta_fmt, FechasUtil.diaCorto(plazoFin));
    }

    // Pone el texto de creditos bonito
    public String creditosTexto() {
        if (creditos == null) return "";
        double c = creditos;
        return (c == Math.floor(c) ? String.valueOf((int) c) : String.valueOf(c)) + " ECTS";
    }

    // Sirve para buscar
    public String textoBusqueda() {
        return TextoUtil.textoBusqueda(titulo, descripcion, nombreTipo(tipo), lugar);
    }

    // -------------------- Aspecto según el tipo --------------------


    /** Nombre del tipo en el idioma de la app (pantallas). */
    public static String nombreTipo(Context ctx, String tipo) {
        switch (tipo) {
            case "charla": return ctx.getString(R.string.nui_tipo_charla);
            case "bibliomaker": return ctx.getString(R.string.nui_tipo_bibliomaker);
            case "abiertaugr": return ctx.getString(R.string.nui_tipo_abiertaugr);
            case "dia_etsiit": return ctx.getString(R.string.nui_tipo_dia_etsiit);
            default: return ctx.getString(R.string.nui_tipo_otro);
        }
    }

    /** Texto de la pestaña de filtro del tipo, en el idioma de la app. */
    public static String nombreFiltro(Context ctx, String tipo) {
        switch (tipo) {
            case "charla": return ctx.getString(R.string.nui_tipo_f_charla);
            case "bibliomaker": return ctx.getString(R.string.nui_tipo_f_bibliomaker);
            case "abiertaugr": return ctx.getString(R.string.nui_tipo_f_abiertaugr);
            case "dia_etsiit": return ctx.getString(R.string.nui_tipo_f_dia_etsiit);
            default: return ctx.getString(R.string.nui_tipo_f_otro);
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

    /**
     * Sitio de la ETSIIT donde se hace, con el mismo nombre que un marcador del mapa interior
     * (para el botón "Llévame"). null = la escuela en general.
     */
    public String sitioEnMapa() {
        if (lugar != null) return lugar;
        switch (tipo) {
            case "bibliomaker": return "Biblioteca";   // el Bibliomaker está en la biblioteca
            case "charla": return "Aula 0.4";
            case "abiertaugr": return "Aula 2.1";
            default: return null;                      // Día de la ETSIIT, torneos…: toda la escuela
        }
    }

    // Colores e iconos de cada tipo (colores_secciones.xml)

    @ColorRes
    public static int colorRes(String tipo) {
        switch (tipo) {
            case "charla": return R.color.sec_tipo_charla;
            case "bibliomaker": return R.color.sec_tipo_bibliomaker;
            case "abiertaugr": return R.color.sec_tipo_abiertaugr;
            case "dia_etsiit": return R.color.nui_acento;
            default: return R.color.sec_tipo_otro;
        }
    }

    @ColorRes
    public static int tinteRes(String tipo) {
        switch (tipo) {
            case "charla": return R.color.sec_tipo_charla_tinte;
            case "bibliomaker": return R.color.sec_tipo_bibliomaker_tinte;
            case "abiertaugr": return R.color.sec_tipo_abiertaugr_tinte;
            case "dia_etsiit": return R.color.nui_tinte;
            default: return R.color.sec_tipo_otro_tinte;
        }
    }

    @DrawableRes
    public static int iconoRes(String tipo) {
        switch (tipo) {
            case "charla": return R.drawable.nui_ic_consulta;
            case "bibliomaker": return R.drawable.nui_ic_hoja;
            case "abiertaugr": return R.drawable.nui_ic_creditos;
            case "dia_etsiit": return R.drawable.nui_ic_granada;
            default: return R.drawable.ic_nui_actividades;
        }
    }
}
