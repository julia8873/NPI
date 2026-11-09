package com.example.nuietsiit.activities.secciones.mapa;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Lector JSON mínimo en Java puro (org.json de Android no funciona en los tests JVM).
 * Devuelve Map, List, Double, String, Boolean o null.
 */
final class JsonLigero {

    private final String texto;
    private int pos;

    private JsonLigero(String texto) {
        this.texto = texto;
    }

    static Object leer(String texto) {
        JsonLigero j = new JsonLigero(texto);
        Object valor = j.valor();
        j.saltarEspacios();
        if (j.pos != texto.length()) throw j.error("sobra texto");
        return valor;
    }

    private Object valor() {
        saltarEspacios();
        if (pos >= texto.length()) throw error("fin inesperado");
        char c = texto.charAt(pos);
        switch (c) {
            case '{': return objeto();
            case '[': return lista();
            case '"': return cadena();
            case 't': literal("true"); return Boolean.TRUE;
            case 'f': literal("false"); return Boolean.FALSE;
            case 'n': literal("null"); return null;
            default: return numero();
        }
    }

    private Map<String, Object> objeto() {
        Map<String, Object> mapa = new LinkedHashMap<>();
        pos++;
        saltarEspacios();
        if (texto.charAt(pos) == '}') { pos++; return mapa; }
        while (true) {
            saltarEspacios();
            String clave = cadena();
            saltarEspacios();
            esperar(':');
            mapa.put(clave, valor());
            saltarEspacios();
            if (texto.charAt(pos) == ',') { pos++; continue; }
            esperar('}');
            return mapa;
        }
    }

    private List<Object> lista() {
        List<Object> lista = new ArrayList<>();
        pos++;
        saltarEspacios();
        if (texto.charAt(pos) == ']') { pos++; return lista; }
        while (true) {
            lista.add(valor());
            saltarEspacios();
            if (texto.charAt(pos) == ',') { pos++; continue; }
            esperar(']');
            return lista;
        }
    }

    private String cadena() {
        esperar('"');
        StringBuilder sb = new StringBuilder();
        while (true) {
            char c = texto.charAt(pos++);
            if (c == '"') return sb.toString();
            if (c != '\\') { sb.append(c); continue; }
            char e = texto.charAt(pos++);
            switch (e) {
                case 'n': sb.append('\n'); break;
                case 't': sb.append('\t'); break;
                case 'r': sb.append('\r'); break;
                case 'b': sb.append('\b'); break;
                case 'f': sb.append('\f'); break;
                case 'u':
                    sb.append((char) Integer.parseInt(texto.substring(pos, pos + 4), 16));
                    pos += 4;
                    break;
                default: sb.append(e);
            }
        }
    }

    private Double numero() {
        int inicio = pos;
        while (pos < texto.length() && "+-0123456789.eE".indexOf(texto.charAt(pos)) >= 0) pos++;
        if (inicio == pos) throw error("valor no válido");
        return Double.parseDouble(texto.substring(inicio, pos));
    }

    private void literal(String palabra) {
        if (!texto.startsWith(palabra, pos)) throw error("se esperaba " + palabra);
        pos += palabra.length();
    }

    private void esperar(char c) {
        if (pos >= texto.length() || texto.charAt(pos) != c) throw error("se esperaba '" + c + "'");
        pos++;
    }

    private void saltarEspacios() {
        while (pos < texto.length() && Character.isWhitespace(texto.charAt(pos))) pos++;
    }

    private IllegalArgumentException error(String que) {
        return new IllegalArgumentException("JSON: " + que + " en la posición " + pos);
    }
}
