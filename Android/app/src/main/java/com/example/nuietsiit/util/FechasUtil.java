package com.example.nuietsiit.util;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.TextStyle;
import java.time.temporal.ChronoUnit;
import java.util.Locale;

/**
 * Utilidades de fechas para Actividades y Noticias.
 * En la BD las fechas se guardan como texto 'YYYY-MM-DD HH:MM' (o 'YYYY-MM-DD').
 */
public final class FechasUtil {

    private FechasUtil() {}

    private static final Locale ES = Locale.forLanguageTag("es-ES");
    private static final DateTimeFormatter FORMATO_BD = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    // 'YYYY-MM-DD HH:MM' o 'YYYY-MM-DD' -> LocalDateTime (null si viene vacío o mal formado)
    public static LocalDateTime parsear(String texto) {
        if (texto == null || texto.trim().isEmpty()) return null;
        String s = texto.trim().replace(' ', 'T');
        if (s.length() == 10) s += "T00:00";
        try {
            return LocalDateTime.parse(s);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    public static String aBD(LocalDateTime fecha) {
        return fecha.format(FORMATO_BD);
    }

    /** Ahora en formato de la BD: 'YYYY-MM-DD HH:MM' */
    public static String ahoraBD() {
        return aBD(LocalDateTime.now());
    }

    /** Hoy en formato de la BD: 'YYYY-MM-DD' */
    public static String hoyBD() {
        return LocalDate.now().toString();
    }

    public static long millis(LocalDateTime fecha) {
        return fecha.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }

    public static long diasHasta(LocalDate fecha) {
        return ChronoUnit.DAYS.between(LocalDate.now(), fecha);
    }

    // "12:00"
    public static String hora(LocalDateTime fecha) {
        return fecha.format(DateTimeFormatter.ofPattern("HH:mm"));
    }

    // "jueves 22 de octubre"
    public static String diaLargo(LocalDateTime fecha) {
        return fecha.format(DateTimeFormatter.ofPattern("EEEE d 'de' MMMM", ES));
    }

    // "22 oct"
    public static String diaCorto(LocalDate fecha) {
        return fecha.format(DateTimeFormatter.ofPattern("d MMM", ES)).replace(".", "");
    }

    // "OCT"
    public static String mesCorto(LocalDateTime fecha) {
        return fecha.format(DateTimeFormatter.ofPattern("MMM", ES)).replace(".", "").toUpperCase(ES);
    }

    // "Hoy", "Mañana", "Jueves" (si es esta semana) o "22 oct"
    public static String cuando(LocalDateTime fecha) {
        long dias = diasHasta(fecha.toLocalDate());
        if (dias == 0) return "Hoy";
        if (dias == 1) return "Mañana";
        if (dias > 1 && dias < 7) {
            return capitalizar(fecha.getDayOfWeek().getDisplayName(TextStyle.FULL, ES));
        }
        return diaCorto(fecha.toLocalDate());
    }

    // "ahora mismo", "hace 5 min", "hace 3 h", "ayer", "hace 4 días" o "22 oct"
    public static String hace(LocalDateTime fecha) {
        Duration d = Duration.between(fecha, LocalDateTime.now());
        if (d.toMinutes() < 1) return "ahora mismo";
        if (d.toMinutes() < 60) return "hace " + d.toMinutes() + " min";
        if (d.toHours() < 24) return "hace " + d.toHours() + " h";
        long dias = d.toDays();
        if (dias == 1) return "ayer";
        if (dias < 7) return "hace " + dias + " días";
        return diaCorto(fecha.toLocalDate());
    }

    public static String capitalizar(String s) {
        if (s == null || s.isEmpty()) return s;
        return s.substring(0, 1).toUpperCase(ES) + s.substring(1);
    }
}
