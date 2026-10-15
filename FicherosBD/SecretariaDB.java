package com.example.pruebanpi;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

/**
 * Acceso a datos del módulo de Secretaría:
 * PROCEDIMIENTO, PROCEDIMIENTO_PASO, PROCEDIMIENTO_DOCUMENTO,
 * SOLICITUD, SOLICITUD_DOCUMENTO, CITA (solicitudes y citas simuladas dentro de la app)
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
     * requisitos, plazosInfo, plazoInicio, plazoFin y palabrasClave pueden ser null.
     * Plazos en formato ISO yyyy-MM-dd. solicitudOnline = false: solo presencial (con cita previa).
     * Devuelve el idProcedimiento generado o -1 si falla.
     */
    public long insertarProcedimiento(String nombreProcedimiento, String categoria,
                                      String descripcion, String requisitos,
                                      String plazosInfo, String plazoInicio, String plazoFin,
                                      boolean solicitudOnline, String palabrasClave) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("nombreProcedimiento", nombreProcedimiento);
        cv.put("categoria", categoria);   // 'matricula'|'titulos'|'reconocimiento_creditos'|'certificados'|...
        cv.put("descripcion", descripcion);
        cv.put("requisitos", requisitos);         // puede ser null
        cv.put("plazosInfo", plazosInfo);         // puede ser null
        cv.put("plazoInicio", plazoInicio);       // puede ser null
        cv.put("plazoFin", plazoFin);             // puede ser null
        cv.put("solicitudOnline", solicitudOnline ? 1 : 0);
        cv.put("palabrasClave", palabrasClave);   // sinónimos (búsqueda por texto y por voz)
        // minúsculas sin tildes para buscar con LIKE
        String texto = nombreProcedimiento + " " + (palabrasClave == null ? "" : palabrasClave) + " " + descripcion;
        cv.put("textoBusqueda", java.text.Normalizer.normalize(texto, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").toLowerCase());
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
    public boolean insertarPaso(int idProcedimiento, int orden, String tituloPaso,
                                String descripcionPaso) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("idProcedimiento", idProcedimiento);
        cv.put("orden", orden);
        cv.put("tituloPaso", tituloPaso);
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
     * Inserta un documento que hay que fotografiar al solicitar el procedimiento.
     * descripcion puede ser null. Devuelve el idDocumento generado o -1 si falla.
     */
    public long insertarDocumento(int idProcedimiento, String nombreDocumento,
                                  String descripcion, boolean obligatorio) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("idProcedimiento", idProcedimiento);
        cv.put("nombreDocumento", nombreDocumento);
        cv.put("descripcion", descripcion);
        cv.put("obligatorio", obligatorio ? 1 : 0);
        return db.insert("PROCEDIMIENTO_DOCUMENTO", null, cv);
    }

    /** Devuelve los documentos de un procedimiento. */
    public Cursor listarDocumentosDe(int idProcedimiento) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        return db.rawQuery(
                "SELECT * FROM PROCEDIMIENTO_DOCUMENTO WHERE idProcedimiento = ? " +
                "ORDER BY obligatorio DESC, idDocumento",
                new String[]{String.valueOf(idProcedimiento)});
    }

    // --------------------------------------------------------------
    // SOLICITUD + SOLICITUD_DOCUMENTO
    // Estado (Enviada / En revisión / Resuelta) derivado de fechaEnvio; código = "SOL-" + id.
    // --------------------------------------------------------------

    /**
     * Inserta una solicitud con la fecha actual (ISO yyyy-MM-ddTHH:mm:ss).
     * observaciones y rutaFirma pueden ser null. Devuelve el idSolicitud o -1 si falla.
     */
    public long insertarSolicitud(String correoUsuario, int idProcedimiento,
                                  String observaciones, String rutaFirma) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("correoUsuario", correoUsuario);
        cv.put("idProcedimiento", idProcedimiento);
        cv.put("fechaEnvio", java.time.LocalDateTime.now()
                .truncatedTo(java.time.temporal.ChronoUnit.SECONDS).toString());
        cv.put("observaciones", observaciones);
        cv.put("rutaFirma", rutaFirma);
        return db.insert("SOLICITUD", null, cv);
    }

    /** Guarda la foto de un documento de la solicitud. */
    public boolean insertarFotoSolicitud(long idSolicitud, int idDocumento, String rutaFoto) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("idSolicitud", idSolicitud);
        cv.put("idDocumento", idDocumento);
        cv.put("rutaFoto", rutaFoto);
        return db.insert("SOLICITUD_DOCUMENTO", null, cv) != -1;
    }

    /** Solicitudes de un usuario (más recientes primero) con el nombre del procedimiento. */
    public Cursor listarSolicitudesDe(String correoUsuario) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        return db.rawQuery(
                "SELECT s.*, p.nombreProcedimiento FROM SOLICITUD s " +
                "JOIN PROCEDIMIENTO p ON p.idProcedimiento = s.idProcedimiento " +
                "WHERE s.correoUsuario = ? ORDER BY s.fechaEnvio DESC",
                new String[]{correoUsuario});
    }

    // --------------------------------------------------------------
    // CITA  (código = "CITA-" + id)
    // --------------------------------------------------------------

    /**
     * Inserta una cita activa. idProcedimiento null = consulta general.
     * fecha ISO yyyy-MM-dd, hora HH:mm. Devuelve el idCita o -1 si falla.
     */
    public long insertarCita(String correoUsuario, Integer idProcedimiento, String fecha, String hora) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("correoUsuario", correoUsuario);
        cv.put("idProcedimiento", idProcedimiento);
        cv.put("fecha", fecha);
        cv.put("hora", hora);
        cv.put("estado", "activa");
        return db.insert("CITA", null, cv);
    }

    /** Cambia el estado de una cita: 'activa' | 'cancelada'. */
    public boolean cambiarEstadoCita(int idCita, String estado) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("estado", estado);
        return db.update("CITA", cv, "idCita = ?", new String[]{String.valueOf(idCita)}) > 0;
    }

    /** Citas activas de un usuario con el nombre del procedimiento (null = consulta general). */
    public Cursor listarCitasDe(String correoUsuario) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        return db.rawQuery(
                "SELECT c.*, p.nombreProcedimiento FROM CITA c " +
                "LEFT JOIN PROCEDIMIENTO p ON p.idProcedimiento = c.idProcedimiento " +
                "WHERE c.correoUsuario = ? AND c.estado = 'activa' ORDER BY c.fecha, c.hora",
                new String[]{correoUsuario});
    }

    /** Horas (HH:mm) con cita activa en una fecha. */
    public Cursor listarHorasOcupadas(String fecha) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        return db.rawQuery("SELECT hora FROM CITA WHERE fecha = ? AND estado = 'activa'",
                new String[]{fecha});
    }
}
