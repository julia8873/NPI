package com.example.nuietsiit.data.modelo;

public class EventoCalendario {
    public long id;
    public String correoUsuario;
    public String titulo;
    public String tipo; // "Examen", "Entrega", "Actividad", "Otro"
    public String fecha; // YYYY-MM-DD
    public String hora; // HH:mm
    public String descripcion;

    public EventoCalendario(long id, String correoUsuario, String titulo, String tipo, String fecha, String hora, String descripcion) {
        this.id = id;
        this.correoUsuario = correoUsuario;
        this.titulo = titulo;
        this.tipo = tipo;
        this.fecha = fecha;
        this.hora = hora;
        this.descripcion = descripcion;
    }
}
