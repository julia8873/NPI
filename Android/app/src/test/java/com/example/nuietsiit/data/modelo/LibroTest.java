package com.example.nuietsiit.data.modelo;

import static org.junit.Assert.assertEquals;

import com.example.nuietsiit.util.BuscadorTolerante;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

public class LibroTest {

    private static Libro libro(int id, String titulo, String autores, String isbn) {
        return new Libro(id, titulo, autores, isbn, "MIT Press", 2009, null, null, 2, 1, null, null, null, null);
    }

    private static final List<Libro> LIBROS = Arrays.asList(
            libro(1, "Introduction to Algorithms", "Cormen", "9780262033848"),
            libro(2, "Algorithms", "Sedgewick", "9780321573513"),
            libro(3, "The C Programming Language", "Kernighan", "9780131103627"));

    private static List<Libro> buscar(String consulta) {
        return BuscadorTolerante.buscar(Libro.consultaSinSeparadoresIsbn(consulta), LIBROS,
                l -> l.titulo, Libro::textoBusqueda).elementos;
    }

    @Test
    public void quitaSeparadoresSoloSiParecenIsbn() {
        assertEquals("9780262033848", Libro.consultaSinSeparadoresIsbn("978-0-262-03384-8"));
        assertEquals("9780262033848", Libro.consultaSinSeparadoresIsbn(" 978 0 262 03384 8 "));
        assertEquals("978", Libro.consultaSinSeparadoresIsbn("978-"));
        assertEquals("c-programming 2", Libro.consultaSinSeparadoresIsbn("c-programming 2"));
    }

    @Test
    public void isbnConGuionesEncuentraSoloEseLibro() {
        List<Libro> r = buscar("978-0-262-03384-8");
        assertEquals(1, r.size());
        assertEquals(1, r.get(0).id);
    }

    @Test
    public void buscaPorAutorYConSignosDeLike() {
        assertEquals(2, buscar("sedgewick").get(0).id);
        assertEquals(LIBROS.size(), buscar("%_%").size()); // solo signos = consulta vacía
    }
}
