package com.example.nuietsiit.data.modelo;

/** Una asignatura con bibliografía en un grado concreto (una misma asignatura puede estar en varios). */
public class AsignaturaCatalogo {

    public final String codigo;
    public final String nombre;
    public final int idGrado;
    public final String nombreGrado;
    public final int curso;
    public final String rama; // null = formación común

    public AsignaturaCatalogo(String codigo, String nombre, int idGrado, String nombreGrado, int curso, String rama) {
        this.codigo = codigo;
        this.nombre = nombre;
        this.idGrado = idGrado;
        this.nombreGrado = nombreGrado;
        this.curso = curso;
        this.rama = rama;
    }

    // las iniciales en mayúscula: "Grado en Ingeniería Informática" = GII
    public String gradoCorto() {
        StringBuilder corto = new StringBuilder();
        for (String palabra : nombreGrado.split(" ")) {
            if (!palabra.isEmpty() && Character.isUpperCase(palabra.charAt(0))) corto.append(palabra.charAt(0));
        }
        return corto.toString();
    }
}
