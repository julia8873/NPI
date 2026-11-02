package com.example.nuietsiit.data.modelo;

/**
 * Libro de la biblioteca (tabla LIBRO).
 */
public class Libro {
    public final int id;
    public final String titulo;
    public final String autores;
    public final String isbn;               // ISBN-13 sin guiones
    public final String editorial;          // puede ser null
    public final Integer anioPublicacion;   // puede ser null
    public final String edicion;            // puede ser null
    public final String enlaceCatalogo;     // puede ser null
    public final int ejemplaresTotales;
    public final int ejemplaresDisponibles;
    public final String signatura;          // puede ser null
    public final String ubicacion;          // puede ser null
    public final String portada;            // nombre del drawable, puede ser null
    public final String tipoBibliografia;   // 'basica'|'complementaria', solo al listar por asignatura

    public Libro(int id, String titulo, String autores, String isbn, String editorial,
                 Integer anioPublicacion, String edicion, String enlaceCatalogo,
                 int ejemplaresTotales, int ejemplaresDisponibles, String signatura,
                 String ubicacion, String portada, String tipoBibliografia) {
        this.id = id;
        this.titulo = titulo;
        this.autores = autores;
        this.isbn = isbn;
        this.editorial = editorial;
        this.anioPublicacion = anioPublicacion;
        this.edicion = edicion;
        this.enlaceCatalogo = enlaceCatalogo;
        this.ejemplaresTotales = ejemplaresTotales;
        this.ejemplaresDisponibles = ejemplaresDisponibles;
        this.signatura = signatura;
        this.ubicacion = ubicacion;
        this.portada = portada;
        this.tipoBibliografia = tipoBibliografia;
    }

    public boolean disponible() {
        return ejemplaresDisponibles > 0;
    }
}
