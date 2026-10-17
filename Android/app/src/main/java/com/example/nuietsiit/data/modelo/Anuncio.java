package com.example.nuietsiit.data.modelo;

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

    public static String emoji(String categoria) {
        switch (categoria) {
            case "beca": return "💰";
            case "plazo": return "⏰";
            case "transporte": return "🚌";
            default: return "📢";
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
