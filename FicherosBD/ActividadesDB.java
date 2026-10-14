package com.example.pruebanpi;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

/**
 * Acceso a datos del módulo de Actividades:
 * ACTIVIDAD, CURSO_ABIERTAUGR, INSCRIPCION_ACTIVIDAD, COMUNIDAD_CHAT
 */
public class ActividadesDB {

    private final DBHelper dbHelper;

    public ActividadesDB(DBHelper dbHelper) {
        this.dbHelper = dbHelper;
    }

    // --------------------------------------------------------------
    // ACTIVIDAD
    // --------------------------------------------------------------

    /**
     * Inserta una actividad.
     * fechaFin, idEspacio, enlaceInfo, correoAdmin y correoProfesor pueden ser null.
     * Devuelve el idActividad generado o -1 si falla.
     */
    public long insertarActividad(String titulo, String descripcion, String tipoActividad,
                                  String fechaInicio, String fechaFin, Integer idEspacio,
                                  String enlaceInfo, String correoAdmin, String correoProfesor) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("titulo", titulo);
        cv.put("descripcion", descripcion);
        cv.put("tipoActividad", tipoActividad); // 'bibliomaker'|'dia_etsiit'|'abiertaugr'|'charla'|'otro'
        cv.put("fechaInicio", fechaInicio);     // 'YYYY-MM-DD HH:MM'
        cv.put("fechaFin", fechaFin);           // puede ser null
        if (idEspacio != null) cv.put("idEspacio", idEspacio); else cv.putNull("idEspacio");
        cv.put("enlaceInfo", enlaceInfo);       // puede ser null
        if (correoAdmin != null) cv.put("correoAdmin", correoAdmin.toLowerCase());
        else cv.putNull("correoAdmin");
        if (correoProfesor != null) cv.put("correoProfesor", correoProfesor.toLowerCase());
        else cv.putNull("correoProfesor");
        return db.insert("ACTIVIDAD", null, cv);
    }

    // Obtiene una actividad por id.
    public Cursor obtenerActividad(int idActividad) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        return db.rawQuery("SELECT * FROM ACTIVIDAD WHERE idActividad = ?",
                new String[]{String.valueOf(idActividad)});
    }

    // Lista todas las actividades a partir de hoy, ordenadas por fechaInicio.
    public Cursor listarActividadesFuturas(String fechaHoyISO) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        return db.rawQuery(
                "SELECT * FROM ACTIVIDAD WHERE fechaInicio >= ? ORDER BY fechaInicio",
                new String[]{fechaHoyISO});
    }

    //Lista las actividades de un tipo concreto.
    public Cursor listarActividadesPorTipo(String tipoActividad) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        return db.rawQuery(
                "SELECT * FROM ACTIVIDAD WHERE tipoActividad = ? ORDER BY fechaInicio",
                new String[]{tipoActividad});
    }

    // --------------------------------------------------------------
    // CURSO_ABIERTAUGR
    // --------------------------------------------------------------

    /**
     * Inserta los datos extra de un curso AbiertaUGR.
     * La actividad ya debe existir y ser de tipoActividad='abiertaugr'.
     * plazasTotales puede ser null (sin límite).
     */
    public boolean insertarCursoAbiertaUGR(int idActividad, double creditos,
                                            String plazoInicioInscripcion,
                                            String plazoFinInscripcion,
                                            Integer plazasTotales) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("idActividad", idActividad);
        cv.put("creditos", creditos);
        cv.put("plazoInicioInscripcion", plazoInicioInscripcion); // 'YYYY-MM-DD'
        cv.put("plazoFinInscripcion", plazoFinInscripcion);
        if (plazasTotales != null) cv.put("plazasTotales", plazasTotales);
        else cv.putNull("plazasTotales");
        long resultado = db.insert("CURSO_ABIERTAUGR", null, cv);
        return resultado != -1;
    }

    /**
     * Lista los cursos AbiertaUGR con el plazo de inscripción aún abierto,
     * ordenados por créditos descendentes.
     */
    public Cursor listarCursosAbiertaUGRDisponibles(String fechaHoyISO) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        return db.rawQuery(
                "SELECT A.*, C.creditos, C.plazoInicioInscripcion, " +
                "       C.plazoFinInscripcion, C.plazasTotales " +
                "FROM ACTIVIDAD A " +
                "JOIN CURSO_ABIERTAUGR C ON A.idActividad = C.idActividad " +
                "WHERE C.plazoInicioInscripcion <= ? AND C.plazoFinInscripcion >= ? " +
                "ORDER BY C.creditos DESC",
                new String[]{fechaHoyISO, fechaHoyISO});
    }

    // --------------------------------------------------------------
    // INSCRIPCION_ACTIVIDAD
    // --------------------------------------------------------------

    /**
     * Inscribe a un estudiante en una actividad.
     * fechaInscripcion: 'YYYY-MM-DD HH:MM'
     */
    public boolean insertarInscripcion(String correoEstudiante, int idActividad,
                                       String fechaInscripcion) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("correoEstudiante", correoEstudiante.toLowerCase());
        cv.put("idActividad", idActividad);
        cv.put("fechaInscripcion", fechaInscripcion);
        long resultado = db.insert("INSCRIPCION_ACTIVIDAD", null, cv);
        return resultado != -1;
    }

    /** Comprueba si un estudiante ya está inscrito en una actividad. */
    public boolean estaInscrito(String correoEstudiante, int idActividad) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        try (Cursor cursor = db.rawQuery(
                "SELECT 1 FROM INSCRIPCION_ACTIVIDAD " +
                "WHERE correoEstudiante = ? AND idActividad = ?",
                new String[]{correoEstudiante.toLowerCase(), String.valueOf(idActividad)})) {
            return cursor.getCount() > 0;
        }
    }

    /** Devuelve las actividades en las que está inscrito un estudiante. */
    public Cursor listarInscripcionesDe(String correoEstudiante) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        return db.rawQuery(
                "SELECT A.*, IA.fechaInscripcion FROM ACTIVIDAD A " +
                "JOIN INSCRIPCION_ACTIVIDAD IA ON A.idActividad = IA.idActividad " +
                "WHERE IA.correoEstudiante = ? ORDER BY A.fechaInicio",
                new String[]{correoEstudiante.toLowerCase()});
    }

    // --------------------------------------------------------------
    // COMUNIDAD_CHAT
    // --------------------------------------------------------------

    /**
     * Inserta una comunidad de chat.
     * descripcion e idGrado pueden ser null.
     * idGrado null = comunidad general (para todos los grados).
     */
    public long insertarComunidad(String nombreComunidad, String plataforma,
                                  String enlaceAcceso, String descripcion, Integer idGrado) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("nombreComunidad", nombreComunidad);
        cv.put("plataforma", plataforma);   // 'whatsapp'|'telegram'|'discord'
        cv.put("enlaceAcceso", enlaceAcceso);
        cv.put("descripcion", descripcion); // puede ser null
        if (idGrado != null) cv.put("idGrado", idGrado); else cv.putNull("idGrado");
        return db.insert("COMUNIDAD_CHAT", null, cv);
    }

    /** Lista las comunidades de chat disponibles para un grado (+ las generales). */
    public Cursor listarComunidadesDe(int idGrado) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        return db.rawQuery(
                "SELECT * FROM COMUNIDAD_CHAT " +
                "WHERE idGrado = ? OR idGrado IS NULL " +
                "ORDER BY nombreComunidad",
                new String[]{String.valueOf(idGrado)});
    }
}
