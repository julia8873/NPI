package com.example.pruebanpi;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

/**
 * Acceso a datos del módulo de Secretaría:
 * PROCEDIMIENTO, PROCEDIMIENTO_PASO, PROCEDIMIENTO_DOCUMENTO
 */
public class SecretariaDB {

    private final DBHelper dbHelper;

    public SecretariaDB(DBHelper dbHelper) {
        this.dbHelper = dbHelper;
    }

    // --------------------------------------------------------------
    // PROCEDIMIENTO
    // --------------------------------------------------------------

    /**
     * Inserta un procedimiento de secretaría.
     * requisitos, plazosInfo, enlaceSolicitud y enlaceCita pueden ser null.
     * Devuelve el idProcedimiento generado o -1 si falla.
     */
    public long insertarProcedimiento(String nombreProcedimiento, String categoria,
                                      String descripcion, String requisitos,
                                      String plazosInfo, String enlaceSolicitud,
                                      String enlaceCita) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("nombreProcedimiento", nombreProcedimiento);
        cv.put("categoria", categoria);   // 'matricula'|'titulos'|'reconocimiento_creditos'|...
        cv.put("descripcion", descripcion);
        cv.put("requisitos", requisitos);         // puede ser null
        cv.put("plazosInfo", plazosInfo);         // puede ser null
        cv.put("enlaceSolicitud", enlaceSolicitud); // puede ser null (mostrar como QR)
        cv.put("enlaceCita", enlaceCita);         // puede ser null (mostrar como QR)
        return db.insert("PROCEDIMIENTO", null, cv);
    }

    /** Obtiene un procedimiento por id. */
    public Cursor obtenerProcedimiento(int idProcedimiento) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        return db.rawQuery("SELECT * FROM PROCEDIMIENTO WHERE idProcedimiento = ?",
                new String[]{String.valueOf(idProcedimiento)});
    }

    /** Lista todos los procedimientos, opcionalmente filtrados por categoría. */
    public Cursor listarProcedimientos(String categoria) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        if (categoria == null) {
            return db.rawQuery(
                    "SELECT * FROM PROCEDIMIENTO ORDER BY categoria, nombreProcedimiento", null);
        } else {
            return db.rawQuery(
                    "SELECT * FROM PROCEDIMIENTO WHERE categoria = ? " +
                    "ORDER BY nombreProcedimiento",
                    new String[]{categoria});
        }
    }

    // --------------------------------------------------------------
    // PROCEDIMIENTO_PASO
    // --------------------------------------------------------------

    /**
     * Inserta un paso de un procedimiento.
     * orden determina el número de paso (1, 2, 3...).
     */
    public boolean insertarPaso(int idProcedimiento, int orden, String descripcionPaso) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("idProcedimiento", idProcedimiento);
        cv.put("orden", orden);
        cv.put("descripcionPaso", descripcionPaso);
        long resultado = db.insert("PROCEDIMIENTO_PASO", null, cv);
        return resultado != -1;
    }

    /** Devuelve los pasos de un procedimiento ordenados. */
    public Cursor listarPasosDe(int idProcedimiento) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        return db.rawQuery(
                "SELECT * FROM PROCEDIMIENTO_PASO WHERE idProcedimiento = ? ORDER BY orden",
                new String[]{String.valueOf(idProcedimiento)});
    }

    // --------------------------------------------------------------
    // PROCEDIMIENTO_DOCUMENTO
    // --------------------------------------------------------------

    /**
     * Inserta un documento asociado a un procedimiento.
     * Devuelve el idDocumento generado o -1 si falla.
     */
    public long insertarDocumento(int idProcedimiento, String nombreDocumento, String url) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("idProcedimiento", idProcedimiento);
        cv.put("nombreDocumento", nombreDocumento);
        cv.put("url", url);
        return db.insert("PROCEDIMIENTO_DOCUMENTO", null, cv);
    }

    /** Devuelve los documentos de un procedimiento. */
    public Cursor listarDocumentosDe(int idProcedimiento) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        return db.rawQuery(
                "SELECT * FROM PROCEDIMIENTO_DOCUMENTO WHERE idProcedimiento = ? " +
                "ORDER BY nombreDocumento",
                new String[]{String.valueOf(idProcedimiento)});
    }
}
