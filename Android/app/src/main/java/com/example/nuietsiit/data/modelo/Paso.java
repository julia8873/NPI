package com.example.nuietsiit.data.modelo;

/**
 * Paso de un trámite (tabla PROCEDIMIENTO_PASO).
 */
public class Paso {
    public final int orden;
    public final String titulo;
    public final String descripcion;

    public Paso(int orden, String titulo, String descripcion) {
        this.orden = orden;
        this.titulo = titulo;
        this.descripcion = descripcion;
    }
}
