package com.example.nuietsiit.data.modelo;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Locale;

/**
 * Cita previa en la secretaría hecha desde la app (tabla CITA).
 */
public class Cita {
    public static final String ACTIVA = "activa";
    public static final String CANCELADA = "cancelada";

    public final int id;
    public final String correoUsuario;
    public final Integer idProcedimiento; // null = consulta general
    public final String nombreTramite;    // null = consulta general
    public final String fecha;            // ISO yyyy-MM-dd
    public final String hora;             // HH:mm
    public final String estado;           // ACTIVA | CANCELADA

    public Cita(int id, String correoUsuario, Integer idProcedimiento, String nombreTramite,
                String fecha, String hora, String estado) {
        this.id = id;
        this.correoUsuario = correoUsuario;
        this.idProcedimiento = idProcedimiento;
        this.nombreTramite = nombreTramite;
        this.fecha = fecha;
        this.hora = hora;
        this.estado = estado;
    }

    // código del justificante y del QR: "CITA-0007"
    public String codigo() {
        return String.format(Locale.ROOT, "CITA-%04d", id);
    }

    public String motivo() {
        return nombreTramite != null ? nombreTramite : "Consulta general";
    }

    public LocalDateTime fechaHora() {
        return LocalDateTime.of(LocalDate.parse(fecha), LocalTime.parse(hora));
    }

    public boolean esFutura() {
        return fechaHora().isAfter(LocalDateTime.now());
    }
}
