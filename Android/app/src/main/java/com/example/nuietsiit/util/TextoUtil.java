package com.example.nuietsiit.util;

import java.text.Normalizer;
import java.util.Locale;

/**
 * Utilidades de texto reutilizables (búsquedas en BD y reconocimiento de voz).
 */
public final class TextoUtil {

    private TextoUtil() {}

    // pasa a minúsculas y quita tildes/diéresis: "Matrícula" -> "matricula"
    // (LIKE de SQLite no ignora tildes, por eso se guarda y se busca el texto normalizado)
    public static String normalizar(String texto) {
        if (texto == null) return "";
        String sinDiacriticos = Normalizer.normalize(texto, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return sinDiacriticos.toLowerCase(Locale.ROOT).trim();
    }

    // une varios campos en un único texto normalizado para la columna textoBusqueda
    public static String textoBusqueda(String... campos) {
        StringBuilder sb = new StringBuilder();
        for (String campo : campos) {
            if (campo != null && !campo.isEmpty()) {
                sb.append(normalizar(campo)).append(' ');
            }
        }
        return sb.toString().trim();
    }
}
