package com.example.pruebanpi;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

/**
 * Acceso a datos del módulo de Anuncios:
 * ANUNCIO_GENERAL, ANUNCIO_ASIGNATURA
 */
public class AnunciosDB {

    private final DBHelper dbHelper;

    public AnunciosDB(DBHelper dbHelper) {
        this.dbHelper = dbHelper;
    }

    // --------------------------------------------------------------
    // ANUNCIO_GENERAL
    // --------------------------------------------------------------

    /**
     * Inserta un anuncio general.
     * fechaCaducidad, enlace e idGrado pueden ser null.
     * idGrado null = anuncio para todos los grados.
     * Devuelve el idAnuncioGeneral generado o -1 si falla.
     */
    public long insertarAnuncioGeneral(String titulo, String contenido,
                                       String categoria, String prioridad,
                                       String fechaPublicacion, String fechaCaducidad,
                                       String enlace, Integer idGrado,
                                       String correoAdmin) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("titulo", titulo);
        cv.put("contenido", contenido);
        cv.put("categoria", categoria);   // 'beca'|'plazo'|'transporte'|'general'
        cv.put("prioridad", prioridad);   // 'baja'|'normal'|'alta'
        cv.put("fechaPublicacion", fechaPublicacion); // 'YYYY-MM-DD HH:MM'
        cv.put("fechaCaducidad", fechaCaducidad);     // puede ser null
        cv.put("enlace", enlace);                     // puede ser null
        if (idGrado != null) cv.put("idGrado", idGrado); else cv.putNull("idGrado");
        cv.put("correoAdmin", correoAdmin.toLowerCase());
        return db.insert("ANUNCIO_GENERAL", null, cv);
    }

    /**
     * Lista los anuncios generales visibles para un grado concreto
     * (los del grado + los generales para todos), no caducados y ordenados
     * por prioridad (alta primero) y fecha de publicación.
     */
    public Cursor listarAnunciosGeneralesDe(int idGrado, String fechaActualISO) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        return db.rawQuery(
                "SELECT * FROM ANUNCIO_GENERAL " +
                "WHERE (idGrado = ? OR idGrado IS NULL) " +
                "  AND (fechaCaducidad IS NULL OR fechaCaducidad >= ?) " +
                "ORDER BY " +
                "  CASE prioridad WHEN 'alta' THEN 1 WHEN 'normal' THEN 2 ELSE 3 END, " +
                "  fechaPublicacion DESC",
                new String[]{String.valueOf(idGrado), fechaActualISO});
    }

    /** Lista todos los anuncios generales no caducados (sin filtro de grado). */
    public Cursor listarTodosAnunciosGenerales(String fechaActualISO) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        return db.rawQuery(
                "SELECT * FROM ANUNCIO_GENERAL " +
                "WHERE (fechaCaducidad IS NULL OR fechaCaducidad >= ?) " +
                "ORDER BY " +
                "  CASE prioridad WHEN 'alta' THEN 1 WHEN 'normal' THEN 2 ELSE 3 END, " +
                "  fechaPublicacion DESC",
                new String[]{fechaActualISO});
    }

    // --------------------------------------------------------------
    // ANUNCIO_ASIGNATURA
    // --------------------------------------------------------------

    /**
     * Inserta un anuncio de asignatura.
     * fechaCaducidad, idSesion, fechaAfectada e idEspacioNuevo pueden ser null.
     *
     * tipoAviso 'cambio_aula'   → rellenar idSesion, fechaAfectada, idEspacioNuevo
     * tipoAviso 'clase_anulada' → rellenar idSesion y fechaAfectada
     * tipoAviso 'otro'          → solo titulo + contenido (el resto null)
     *
     * Devuelve el idAnuncioAsignatura generado o -1 si falla.
     */
    public long insertarAnuncioAsignatura(String titulo, String contenido,
                                          String tipoAviso, String fechaPublicacion,
                                          String fechaCaducidad,
                                          String codigoAsignatura, String correoProfesor,
                                          Integer idSesion, String fechaAfectada,
                                          Integer idEspacioNuevo) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("titulo", titulo);
        cv.put("contenido", contenido);
        cv.put("tipoAviso", tipoAviso);           // 'cambio_aula'|'clase_anulada'|'otro'
        cv.put("fechaPublicacion", fechaPublicacion);
        cv.put("fechaCaducidad", fechaCaducidad); // puede ser null
        cv.put("codigoAsignatura", codigoAsignatura);
        cv.put("correoProfesor", correoProfesor.toLowerCase());
        if (idSesion != null) cv.put("idSesion", idSesion); else cv.putNull("idSesion");
        cv.put("fechaAfectada", fechaAfectada);   // puede ser null ('YYYY-MM-DD')
        if (idEspacioNuevo != null) cv.put("idEspacioNuevo", idEspacioNuevo);
        else cv.putNull("idEspacioNuevo");
        return db.insert("ANUNCIO_ASIGNATURA", null, cv);
    }

    /**
     * Lista los anuncios de asignatura visibles para un estudiante:
     * los de las asignaturas en las que está matriculado en el curso dado,
     * no caducados, ordenados por fecha de publicación descendente.
     */
    public Cursor listarAnunciosAsignaturaDe(String correoEstudiante,
                                             String cursoAcademico,
                                             String fechaActualISO) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        return db.rawQuery(
                "SELECT AA.*, A.nombreAsignatura FROM ANUNCIO_ASIGNATURA AA " +
                "JOIN ASIGNATURA A ON AA.codigoAsignatura = A.codigoAsignatura " +
                "WHERE AA.codigoAsignatura IN (" +
                "  SELECT codigoAsignatura FROM MATRICULA " +
                "  WHERE correoEstudiante = ? AND cursoAcademico = ?" +
                ") AND (AA.fechaCaducidad IS NULL OR AA.fechaCaducidad >= ?) " +
                "ORDER BY AA.fechaPublicacion DESC",
                new String[]{correoEstudiante.toLowerCase(), cursoAcademico, fechaActualISO});
    }

    /**
     * Lista los anuncios de tipo 'cambio_aula' o 'clase_anulada' para una sesión
     * y fecha concreta. Útil para mostrar en el horario si hay cambio ese día.
     */
    public Cursor listarAnunciosDeSesion(int idSesion, String fechaAfectada) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        return db.rawQuery(
                "SELECT * FROM ANUNCIO_ASIGNATURA " +
                "WHERE idSesion = ? AND fechaAfectada = ? " +
                "  AND tipoAviso IN ('cambio_aula','clase_anulada')",
                new String[]{String.valueOf(idSesion), fechaAfectada});
    }
}
