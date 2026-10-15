package com.example.nuietsiit.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

/**
 * Búsqueda tolerante a tildes, mayúsculas, faltas de ortografía, letras repetidas/cambiadas
 * y palabras a medias. Java puro (sin Android) para poder probarla con JUnit.
 * Sirve igual para texto escrito y para voz, y se reutiliza en Biblioteca.
 */
public final class BuscadorTolerante {

    private BuscadorTolerante() {}

    private static final Set<String> VACIAS = new HashSet<>(Arrays.asList(
            "de", "del", "la", "las", "el", "los", "lo", "al", "para", "por", "con", "en", "y", "o", "a",
            "mi", "mis", "un", "una", "unos", "unas", "que", "se", "me", "es",
            "quiero", "necesito", "como", "puedo", "hacer"));

    /** Palabra normalizada y su clave fonética. */
    private static final class Palabra {
        final String raw;
        final String clave;

        Palabra(String raw) {
            this.raw = raw;
            this.clave = clavefonetica(raw);
        }
    }

    /** Resultado de buscar: elementos ordenados y mensajes opcionales para la pantalla. */
    public static final class Busqueda<T> {
        public final List<T> elementos;
        public final String mostrandoPara; // corrección aplicada si solo hubo coincidencias aproximadas
        public final String sugerencia;    // "¿Quisiste decir …?" si no hubo ningún resultado

        Busqueda(List<T> elementos, String mostrandoPara, String sugerencia) {
            this.elementos = elementos;
            this.mostrandoPara = mostrandoPara;
            this.sugerencia = sugerencia;
        }
    }

    /** Puntuación de un elemento frente a una consulta. */
    public static final class Puntuacion {
        public final int puntos;
        public final int palabrasCoincidentes;
        public final int palabrasExactas; // coincidencias sin recurrir a la clave fonética ni a la distancia

        Puntuacion(int puntos, int palabrasCoincidentes, int palabrasExactas) {
            this.puntos = puntos;
            this.palabrasCoincidentes = palabrasCoincidentes;
            this.palabrasExactas = palabrasExactas;
        }
    }

    // ---------------------------------------------------------------------------------
    //                                 Normalización
    // ---------------------------------------------------------------------------------

    // minúsculas, sin tildes ni signos, 3 o más letras iguales seguidas -> 1, sin palabras vacías
    public static List<String> palabras(String texto) {
        String limpio = TextoUtil.normalizar(texto)
                .replaceAll("[^a-z0-9]+", " ")
                .replaceAll("([a-z])\\1{2,}", "$1");
        List<String> resultado = new ArrayList<>();
        for (String p : limpio.split(" ")) {
            if (!p.isEmpty() && !VACIAS.contains(p)) resultado.add(p);
        }
        return resultado;
    }

    // clave fonética del español: "convalidasion" y "convalidacion" dan la misma clave
    public static String clavefonetica(String p) {
        return p.replace("ch", "ç")
                .replace("ll", "y")
                .replace("h", "")
                .replace("qu", "k")
                .replaceAll("c(?=[ei])", "s")
                .replace("c", "k")
                .replace("z", "s")
                .replace("v", "b")
                .replace("w", "b")
                .replaceAll("gu(?=[ei])", "g")
                .replaceAll("g(?=[ei])", "j")
                .replace("x", "ks");
    }

    // ---------------------------------------------------------------------------------
    //                                  Puntuación
    // ---------------------------------------------------------------------------------

    // distancia máxima permitida según la longitud de la palabra buscada
    private static int toleranciaDistancia(int longitud) {
        if (longitud >= 8) return 2;
        if (longitud >= 4) return 1;
        return 0;
    }

    // Damerau-Levenshtein (con intercambio de letras adyacentes)
    static int distancia(String a, String b) {
        int[][] d = new int[a.length() + 1][b.length() + 1];
        for (int i = 0; i <= a.length(); i++) d[i][0] = i;
        for (int j = 0; j <= b.length(); j++) d[0][j] = j;
        for (int i = 1; i <= a.length(); i++) {
            for (int j = 1; j <= b.length(); j++) {
                int coste = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                d[i][j] = Math.min(Math.min(d[i - 1][j] + 1, d[i][j - 1] + 1), d[i - 1][j - 1] + coste);
                if (i > 1 && j > 1 && a.charAt(i - 1) == b.charAt(j - 2) && a.charAt(i - 2) == b.charAt(j - 1)) {
                    d[i][j] = Math.min(d[i][j], d[i - 2][j - 2] + 1);
                }
            }
        }
        return d[a.length()][b.length()];
    }

    // 3 exacta · 2 prefijo (>=3 letras) · 1 aproximada; los dos primeros también sobre la clave fonética
    // devuelve {puntos, exacta(0/1)}
    private static int[] puntuarPalabra(Palabra q, Palabra d) {
        if (q.raw.equals(d.raw)) return new int[]{3, 1};
        if (q.raw.length() >= 3 && d.raw.startsWith(q.raw)) return new int[]{2, 1};
        if (q.clave.equals(d.clave)) return new int[]{3, 0};
        if (q.clave.length() >= 3 && d.clave.startsWith(q.clave)) return new int[]{2, 0};
        if (distancia(q.clave, d.clave) <= toleranciaDistancia(q.clave.length())) return new int[]{1, 0};
        return new int[]{0, 0};
    }

    private static List<Palabra> comoPalabras(List<String> lista) {
        List<Palabra> r = new ArrayList<>();
        for (String s : lista) r.add(new Palabra(s));
        return r;
    }

    private static int[] mejor(Palabra q, List<Palabra> doc) {
        int[] mejor = {0, 0};
        for (Palabra d : doc) {
            int[] p = puntuarPalabra(q, d);
            if (p[0] > mejor[0] || (p[0] == mejor[0] && p[1] > mejor[1])) mejor = p;
        }
        return mejor;
    }

    private static Puntuacion puntuar(List<Palabra> consulta, List<Palabra> enNombre, List<Palabra> enTexto) {
        int puntos = 0, coincidentes = 0, exactas = 0;
        for (Palabra q : consulta) {
            int[] n = mejor(q, enNombre);
            int[] t = mejor(q, enTexto);
            int[] elegido = n[0] * 2 >= t[0] ? new int[]{n[0] * 2, n[1]} : t; // x2 si está en el nombre
            if (elegido[0] > 0) {
                puntos += elegido[0];
                coincidentes++;
                exactas += elegido[1];
            }
        }
        return new Puntuacion(puntos, coincidentes, exactas);
    }

    public static Puntuacion puntuar(String consulta, String nombre, String textoBusqueda) {
        return puntuar(comoPalabras(palabras(consulta)), comoPalabras(palabras(nombre)),
                comoPalabras(palabras(textoBusqueda)));
    }

    // ---------------------------------------------------------------------------------
    //                                 Sugerencias
    // ---------------------------------------------------------------------------------

    // Sustituye cada palabra sin coincidencia exacta por la más cercana del vocabulario (distancia <= tolerancia + extra
    // sobre la clave fonética). null si no cambia nada.
    static String sugerir(List<String> consulta, Set<String> vocabulario, int extra) {
        List<Palabra> vocab = comoPalabras(new ArrayList<>(vocabulario));
        List<String> resultado = new ArrayList<>();
        boolean cambia = false;
        for (String q : consulta) {
            if (q.length() < 3) { resultado.add(q); continue; } // demasiado corta para corregirla
            Palabra pq = new Palabra(q);
            int maxDist = toleranciaDistancia(pq.clave.length()) + extra;
            String elegida = q;
            int mejorDist = Integer.MAX_VALUE;
            boolean exacta = false;
            for (Palabra v : vocab) {
                if (v.raw.startsWith(q) && q.length() >= 3) { exacta = true; break; }
                int dist = distancia(pq.clave, v.clave);
                if (dist < mejorDist) {
                    mejorDist = dist;
                    elegida = v.raw;
                }
            }
            if (exacta || mejorDist > maxDist) elegida = q;
            if (!elegida.equals(q)) cambia = true;
            resultado.add(elegida);
        }
        return cambia ? String.join(" ", resultado) : null;
    }

    // ---------------------------------------------------------------------------------
    //                                  Búsqueda
    // ---------------------------------------------------------------------------------

    /** Palabras y claves fonéticas de los elementos, calculadas una sola vez (la búsqueda se repite en cada pulsación). */
    public static final class Indice<T> {
        private final List<T> elementos;
        private final List<List<Palabra>> nombres = new ArrayList<>();
        private final List<List<Palabra>> textos = new ArrayList<>();
        private final Set<String> vocabulario = new LinkedHashSet<>();

        Indice(List<T> elementos, Function<T, String> nombre, Function<T, String> texto) {
            this.elementos = elementos;
            for (T e : elementos) {
                List<String> pn = palabras(nombre.apply(e));
                List<String> pt = palabras(texto.apply(e));
                vocabulario.addAll(pn);
                vocabulario.addAll(pt);
                nombres.add(comoPalabras(pn));
                textos.add(comoPalabras(pt));
            }
        }

        /**
         * Filtra y ordena los elementos según la consulta. Consulta vacía = todos, en su orden.
         * Si algún elemento coincide con TODAS las palabras, solo se devuelven esos.
         */
        public Busqueda<T> buscar(String consulta) {
            List<String> palabrasConsulta = palabras(consulta);
            if (palabrasConsulta.isEmpty()) {
                return new Busqueda<>(new ArrayList<>(elementos), null, null);
            }
            List<Palabra> pq = comoPalabras(palabrasConsulta);

            List<Integer> candidatos = new ArrayList<>();
            List<Puntuacion> puntuaciones = new ArrayList<>();
            boolean hayCompletos = false;
            for (int i = 0; i < elementos.size(); i++) {
                Puntuacion p = puntuar(pq, nombres.get(i), textos.get(i));
                if (p.puntos > 0) {
                    candidatos.add(i);
                    puntuaciones.add(p);
                    if (p.palabrasCoincidentes == palabrasConsulta.size()) hayCompletos = true;
                }
            }

            List<Integer> indices = new ArrayList<>(); // posiciones dentro de candidatos
            for (int k = 0; k < candidatos.size(); k++) {
                if (!hayCompletos || puntuaciones.get(k).palabrasCoincidentes == palabrasConsulta.size()) {
                    indices.add(k);
                }
            }
            // orden estable por puntos descendentes
            Collections.sort(indices, (a, b) -> puntuaciones.get(b).puntos - puntuaciones.get(a).puntos);

            List<T> ordenados = new ArrayList<>();
            int exactasTotales = 0;
            for (int k : indices) {
                ordenados.add(elementos.get(candidatos.get(k)));
                exactasTotales += puntuaciones.get(k).palabrasExactas;
            }

            if (ordenados.isEmpty()) {
                return new Busqueda<>(ordenados, null, sugerir(palabrasConsulta, vocabulario, 1));
            }
            String mostrandoPara = exactasTotales == 0 ? sugerir(palabrasConsulta, vocabulario, 0) : null;
            return new Busqueda<>(ordenados, mostrandoPara, null);
        }
    }

    public static <T> Indice<T> crearIndice(List<T> elementos, Function<T, String> nombre,
                                            Function<T, String> texto) {
        return new Indice<>(elementos, nombre, texto);
    }

    /** Atajo para una sola búsqueda; para búsquedas repetidas usar {@link #crearIndice}. */
    public static <T> Busqueda<T> buscar(String consulta, List<T> elementos,
                                         Function<T, String> nombre, Function<T, String> texto) {
        return crearIndice(elementos, nombre, texto).buscar(consulta);
    }
}
