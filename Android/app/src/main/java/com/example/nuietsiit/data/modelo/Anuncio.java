package com.example.nuietsiit.data.modelo;

import android.content.Context;

import androidx.annotation.ColorRes;
import androidx.annotation.DrawableRes;

import com.example.nuietsiit.R;

import java.time.LocalDateTime;

/** Un anuncio general (tabla ANUNCIO_GENERAL). */
public class Anuncio {

    // {valor en la BD, texto del chip}
    public static final String[][] CATEGORIAS = {
            {"plazo", "Plazos"},
            {"beca", "Becas"},
            {"transporte", "Transporte"},
            {"general", "General"}
    };

    public int id;
    public String titulo;
    public String contenido;
    public String categoria;   // 'beca'|'plazo'|'transporte'|'general'
    public String prioridad;   // 'baja'|'normal'|'alta'
    public LocalDateTime publicacion;
    public LocalDateTime caducidad; // puede ser null
    public String enlace;           // puede ser null

    public boolean leido;      // se guarda en el móvil (AnunciosLeidos), no en la BD

    public boolean esUrgente() {
        return "alta".equals(prioridad);
    }

    // Colores e iconos de cada categoría (colores_secciones.xml)

    @ColorRes
    public static int colorRes(String categoria) {
        switch (categoria) {
            case "beca": return R.color.sec_ok;
            case "plazo": return R.color.sec_error;
            case "transporte": return R.color.sec_info;
            default: return R.color.nui_acento;
        }
    }

    @ColorRes
    public static int tinteRes(String categoria) {
        switch (categoria) {
            case "beca": return R.color.sec_ok_tinte;
            case "plazo": return R.color.sec_error_tinte;
            case "transporte": return R.color.sec_tipo_abiertaugr_tinte;
            default: return R.color.nui_tinte;
        }
    }

    @DrawableRes
    public static int iconoRes(String categoria) {
        switch (categoria) {
            case "beca": return R.drawable.nui_ic_creditos;
            case "plazo": return R.drawable.nui_ic_reloj_arena;
            case "transporte": return R.drawable.ic_nui_mapa;
            default: return R.drawable.nui_ic_sobre;
        }
    }

    /** Nombre de la categoría en el idioma de la app (pantallas). */
    public static String nombreCategoria(Context ctx, String categoria) {
        switch (categoria) {
            case "beca": return ctx.getString(R.string.nui_cat_beca);
            case "plazo": return ctx.getString(R.string.nui_cat_plazo);
            case "transporte": return ctx.getString(R.string.nui_cat_transporte);
            default: return ctx.getString(R.string.nui_cat_general);
        }
    }

    /** Texto de la pestaña de filtro de la categoría, en el idioma de la app. */
    public static String nombreFiltro(Context ctx, String categoria) {
        switch (categoria) {
            case "beca": return ctx.getString(R.string.nui_cat_f_beca);
            case "plazo": return ctx.getString(R.string.nui_cat_f_plazo);
            case "transporte": return ctx.getString(R.string.nui_cat_f_transporte);
            default: return ctx.getString(R.string.nui_cat_f_general);
        }
    }

    public static String nombreCategoria(String categoria) {
        switch (categoria) {
            case "beca": return "Beca";
            case "plazo": return "Plazo";
            case "transporte": return "Transporte";
            default: return "General";
        }
    }
}
