package com.example.nuietsiit.data.modelo;

/**
 * Documento que hay que fotografiar al solicitar un trámite (tabla PROCEDIMIENTO_DOCUMENTO).
 */
public class Documento {
    public final int id;
    public final String nombre;
    public final String descripcion; // puede ser null
    public final boolean obligatorio;

    public Documento(int id, String nombre, String descripcion, boolean obligatorio) {
        this.id = id;
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.obligatorio = obligatorio;
    }

    // "• DNI (obligatorio)" + descripción sangrada debajo (DetalleTramite y SolicitudTramite)
    public String textoLista() {
        StringBuilder sb = new StringBuilder("• ").append(nombre);
        sb.append(obligatorio ? " (obligatorio)" : " (opcional)");
        if (descripcion != null && !descripcion.isEmpty()) sb.append("\n   ").append(descripcion);
        return sb.toString();
    }
}
