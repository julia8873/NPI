package com.example.nuietsiit.data.modelo;

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

    // Colores e iconos del design system NUI (DisenoNUI/DESIGN.md)

    @ColorRes
    public static int colorRes(String categoria) {
        switch (categoria) {
            case "beca": return R.color.nui_arrayan;
            case "plazo": return R.color.nui_granada;      // plazos y convocatorias = granada
            case "transporte": return R.color.nui_cobalto;
            default: return R.color.nui_tema_certificados; // madera
        }
    }

    @ColorRes
    public static int tinteRes(String categoria) {
        switch (categoria) {
            case "beca": return R.color.nui_arrayan_tinte;
            case "plazo": return R.color.nui_granada_tinte;
            case "transporte": return R.color.nui_cobalto_tinte;
            default: return R.color.nui_tema_certificados_tinte;
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

    public static String nombreCategoria(String categoria) {
        switch (categoria) {
            case "beca": return "Beca";
            case "plazo": return "Plazo";
            case "transporte": return "Transporte";
            default: return "General";
        }
    }
}
