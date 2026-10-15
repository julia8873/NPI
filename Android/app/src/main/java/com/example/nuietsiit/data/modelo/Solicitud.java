package com.example.nuietsiit.data.modelo;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Locale;

/**
 * Solicitud de un trámite hecha desde la app (tabla SOLICITUD).
 * El estado es simulado: se calcula según el tiempo desde el envío.
 */
public class Solicitud {
    public static final String ENVIADA = "Enviada";
    public static final String EN_REVISION = "En revisión";
    public static final String RESUELTA = "Resuelta";

    public final int id;
    public final String correoUsuario;
    public final int idProcedimiento;
    public final String nombreTramite;
    public final String fechaEnvio;    // ISO yyyy-MM-ddTHH:mm:ss
    public final String observaciones; // puede ser null
    public final String rutaFirma;     // puede ser null

    public Solicitud(int id, String correoUsuario, int idProcedimiento, String nombreTramite,
                     String fechaEnvio, String observaciones, String rutaFirma) {
        this.id = id;
        this.correoUsuario = correoUsuario;
        this.idProcedimiento = idProcedimiento;
        this.nombreTramite = nombreTramite;
        this.fechaEnvio = fechaEnvio;
        this.observaciones = observaciones;
        this.rutaFirma = rutaFirma;
    }

    // código del justificante y del QR: "SOL-0012"
    public String codigo() {
        return String.format(Locale.ROOT, "SOL-%04d", id);
    }

    // < 2 min Enviada, < 10 min En revisión, después Resuelta
    public String estado() {
        long minutos = Duration.between(LocalDateTime.parse(fechaEnvio), LocalDateTime.now()).toMinutes();
        if (minutos < 2) return ENVIADA;
        if (minutos < 10) return EN_REVISION;
        return RESUELTA;
    }
}
