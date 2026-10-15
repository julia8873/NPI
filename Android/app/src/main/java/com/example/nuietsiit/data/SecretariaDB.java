package com.example.nuietsiit.data;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.nuietsiit.data.modelo.Cita;
import com.example.nuietsiit.data.modelo.Documento;
import com.example.nuietsiit.data.modelo.Paso;
import com.example.nuietsiit.data.modelo.Solicitud;
import com.example.nuietsiit.data.modelo.Tramite;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Acceso a datos de Secretaría: trámites (PROCEDIMIENTO, PROCEDIMIENTO_PASO, PROCEDIMIENTO_DOCUMENTO)
 * y gestiones del usuario hechas en la app (SOLICITUD, SOLICITUD_DOCUMENTO, CITA).
 */
public class SecretariaDB {

    private final DBHelper dbHelper;

    public SecretariaDB(DBHelper dbHelper) {
        this.dbHelper = dbHelper;
    }

    // categoria null = todas. El filtro de texto se hace en Java con BuscadorTolerante
    public List<Tramite> getTramites(String categoria) {
        String where = categoria != null ? " WHERE categoria = ?" : "";
        String[] args = categoria != null ? new String[]{categoria} : new String[0];

        SQLiteDatabase db = dbHelper.getReadableDatabase();
        List<Tramite> tramites = new ArrayList<>();
        try (Cursor c = db.rawQuery("SELECT * FROM PROCEDIMIENTO" + where +
                " ORDER BY nombreProcedimiento", args)) {
            while (c.moveToNext()) tramites.add(leerTramite(c));
        }
        return tramites;
    }

    // null si no existe
    public Tramite getTramite(int idProcedimiento) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        try (Cursor c = db.rawQuery("SELECT * FROM PROCEDIMIENTO WHERE idProcedimiento = ?",
                new String[]{String.valueOf(idProcedimiento)})) {
            return c.moveToFirst() ? leerTramite(c) : null;
        }
    }

    public List<Paso> getPasos(int idProcedimiento) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        List<Paso> pasos = new ArrayList<>();
        try (Cursor c = db.rawQuery(
                "SELECT orden, tituloPaso, descripcionPaso FROM PROCEDIMIENTO_PASO " +
                "WHERE idProcedimiento = ? ORDER BY orden",
                new String[]{String.valueOf(idProcedimiento)})) {
            while (c.moveToNext()) {
                pasos.add(new Paso(c.getInt(0), c.getString(1), c.getString(2)));
            }
        }
        return pasos;
    }

    public List<Documento> getDocumentos(int idProcedimiento) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        List<Documento> documentos = new ArrayList<>();
        try (Cursor c = db.rawQuery(
                "SELECT idDocumento, nombreDocumento, descripcion, obligatorio FROM PROCEDIMIENTO_DOCUMENTO " +
                "WHERE idProcedimiento = ? ORDER BY obligatorio DESC, idDocumento",
                new String[]{String.valueOf(idProcedimiento)})) {
            while (c.moveToNext()) {
                documentos.add(new Documento(c.getInt(0), c.getString(1), c.getString(2), c.getInt(3) == 1));
            }
        }
        return documentos;
    }

    // -------------------------------------------------
    //              SOLICITUDES
    // -------------------------------------------------

    // fotos: idDocumento -> ruta de la foto (null = sin fotos). Devuelve el idSolicitud o -1 si falla.
    public long insertarSolicitud(String correoUsuario, int idProcedimiento, String observaciones,
                                  String rutaFirma, Map<Integer, String> fotos) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.beginTransaction();
        try {
            ContentValues cv = new ContentValues();
            cv.put("correoUsuario", correoUsuario);
            cv.put("idProcedimiento", idProcedimiento);
            cv.put("fechaEnvio", LocalDateTime.now().format(FORMATO_FECHA_ENVIO));
            cv.put("observaciones", observaciones);
            cv.put("rutaFirma", rutaFirma);
            long idSolicitud = db.insert("SOLICITUD", null, cv);
            if (idSolicitud == -1) return -1;

            if (fotos != null) for (Map.Entry<Integer, String> foto : fotos.entrySet()) {
                ContentValues cvFoto = new ContentValues();
                cvFoto.put("idSolicitud", idSolicitud);
                cvFoto.put("idDocumento", foto.getKey());
                cvFoto.put("rutaFoto", foto.getValue());
                if (db.insert("SOLICITUD_DOCUMENTO", null, cvFoto) == -1) return -1;
            }
            db.setTransactionSuccessful();
            return idSolicitud;
        } finally {
            db.endTransaction();
        }
    }

    // más recientes primero
    public List<Solicitud> getSolicitudes(String correoUsuario) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        List<Solicitud> solicitudes = new ArrayList<>();
        try (Cursor c = db.rawQuery(SELECT_SOLICITUD + "WHERE s.correoUsuario = ? ORDER BY s.fechaEnvio DESC",
                new String[]{correoUsuario})) {
            while (c.moveToNext()) solicitudes.add(leerSolicitud(c));
        }
        return solicitudes;
    }

    // null si no existe
    public Solicitud getSolicitud(int idSolicitud) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        try (Cursor c = db.rawQuery(SELECT_SOLICITUD + "WHERE s.idSolicitud = ?",
                new String[]{String.valueOf(idSolicitud)})) {
            return c.moveToFirst() ? leerSolicitud(c) : null;
        }
    }

    // -------------------------------------------------
    //              CITAS
    // -------------------------------------------------

    // idProcedimiento null = consulta general. Devuelve el idCita o -1 si falla.
    public long insertarCita(String correoUsuario, Integer idProcedimiento, String fecha, String hora) {
        ContentValues cv = new ContentValues();
        cv.put("correoUsuario", correoUsuario);
        cv.put("idProcedimiento", idProcedimiento);
        cv.put("fecha", fecha);
        cv.put("hora", hora);
        cv.put("estado", Cita.ACTIVA);
        return dbHelper.getWritableDatabase().insert("CITA", null, cv);
    }

    // para "ya tienes una cita para este motivo: ¿cambiarla?"
    public boolean cambiarCita(int idCita, String fecha, String hora) {
        ContentValues cv = new ContentValues();
        cv.put("fecha", fecha);
        cv.put("hora", hora);
        return actualizarCita(idCita, cv);
    }

    public boolean cancelarCita(int idCita) {
        return cambiarEstadoCita(idCita, Cita.CANCELADA);
    }

    // "Deshacer" tras cancelar deslizando
    public boolean reactivarCita(int idCita) {
        return cambiarEstadoCita(idCita, Cita.ACTIVA);
    }

    // citas activas del usuario: futuras primero (la más próxima arriba), después las pasadas
    public List<Cita> getCitas(String correoUsuario) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        List<Cita> citas = new ArrayList<>();
        try (Cursor c = db.rawQuery(SELECT_CITA +
                "WHERE c.correoUsuario = ? AND c.estado = ? " +
                "ORDER BY (c.fecha || ' ' || c.hora) < ?, " +
                "CASE WHEN (c.fecha || ' ' || c.hora) >= ? THEN c.fecha || ' ' || c.hora END, " +
                "c.fecha DESC, c.hora DESC",
                new String[]{correoUsuario, Cita.ACTIVA, ahora(), ahora()})) {
            while (c.moveToNext()) citas.add(leerCita(c));
        }
        return citas;
    }

    // null si no existe
    public Cita getCita(int idCita) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        try (Cursor c = db.rawQuery(SELECT_CITA + "WHERE c.idCita = ?",
                new String[]{String.valueOf(idCita)})) {
            return c.moveToFirst() ? leerCita(c) : null;
        }
    }

    // cita activa y futura del usuario para ese motivo (null = consulta general); null si no hay
    public Cita getCitaActiva(String correoUsuario, Integer idProcedimiento) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String motivo = idProcedimiento == null ? "c.idProcedimiento IS NULL" : "c.idProcedimiento = " + idProcedimiento;
        try (Cursor c = db.rawQuery(SELECT_CITA +
                "WHERE c.correoUsuario = ? AND c.estado = ? AND " + motivo +
                " AND (c.fecha || ' ' || c.hora) >= ? ORDER BY c.fecha, c.hora LIMIT 1",
                new String[]{correoUsuario, Cita.ACTIVA, ahora()})) {
            return c.moveToFirst() ? leerCita(c) : null;
        }
    }

    // nº de citas activas y futuras (contador del acceso "Mis gestiones")
    public int contarCitasActivas(String correoUsuario) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        try (Cursor c = db.rawQuery(
                "SELECT COUNT(*) FROM CITA WHERE correoUsuario = ? AND estado = ? " +
                "AND (fecha || ' ' || hora) >= ?",
                new String[]{correoUsuario, Cita.ACTIVA, ahora()})) {
            return c.moveToFirst() ? c.getInt(0) : 0;
        }
    }

    // horas (HH:mm) con cita activa ese día, de cualquier usuario.
    // Las ocupadas "de relleno" se generan en CitaPrevia con Random(semilla = fecha).
    public Set<String> getHorasOcupadas(String fecha) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Set<String> horas = new HashSet<>();
        try (Cursor c = db.rawQuery("SELECT hora FROM CITA WHERE fecha = ? AND estado = ?",
                new String[]{fecha, Cita.ACTIVA})) {
            while (c.moveToNext()) horas.add(c.getString(0));
        }
        return horas;
    }

    private boolean cambiarEstadoCita(int idCita, String estado) {
        ContentValues cv = new ContentValues();
        cv.put("estado", estado);
        return actualizarCita(idCita, cv);
    }

    private boolean actualizarCita(int idCita, ContentValues cv) {
        return dbHelper.getWritableDatabase().update("CITA", cv, "idCita = ?",
                new String[]{String.valueOf(idCita)}) > 0;
    }

    // -------------------------------------------------
    //              LECTURA DE CURSORES
    // -------------------------------------------------
    private static final DateTimeFormatter FORMATO_FECHA_HORA = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    // siempre con segundos (toString() los omite cuando valen 0)
    private static final DateTimeFormatter FORMATO_FECHA_ENVIO = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    // mismo formato que fecha || ' ' || hora para comparar en SQL
    private static String ahora() {
        return LocalDateTime.now().format(FORMATO_FECHA_HORA);
    }

    private static final String SELECT_SOLICITUD =
            "SELECT s.*, p.nombreProcedimiento FROM SOLICITUD s " +
            "JOIN PROCEDIMIENTO p ON p.idProcedimiento = s.idProcedimiento ";

    private static final String SELECT_CITA =
            "SELECT c.*, p.nombreProcedimiento FROM CITA c " +
            "LEFT JOIN PROCEDIMIENTO p ON p.idProcedimiento = c.idProcedimiento ";

    private static Solicitud leerSolicitud(Cursor c) {
        return new Solicitud(
                c.getInt(c.getColumnIndexOrThrow("idSolicitud")),
                c.getString(c.getColumnIndexOrThrow("correoUsuario")),
                c.getInt(c.getColumnIndexOrThrow("idProcedimiento")),
                c.getString(c.getColumnIndexOrThrow("nombreProcedimiento")),
                c.getString(c.getColumnIndexOrThrow("fechaEnvio")),
                c.getString(c.getColumnIndexOrThrow("observaciones")),
                c.getString(c.getColumnIndexOrThrow("rutaFirma")));
    }

    private static Cita leerCita(Cursor c) {
        int iProc = c.getColumnIndexOrThrow("idProcedimiento");
        return new Cita(
                c.getInt(c.getColumnIndexOrThrow("idCita")),
                c.getString(c.getColumnIndexOrThrow("correoUsuario")),
                c.isNull(iProc) ? null : c.getInt(iProc),
                c.getString(c.getColumnIndexOrThrow("nombreProcedimiento")),
                c.getString(c.getColumnIndexOrThrow("fecha")),
                c.getString(c.getColumnIndexOrThrow("hora")),
                c.getString(c.getColumnIndexOrThrow("estado")));
    }

    private static Tramite leerTramite(Cursor c) {
        return new Tramite(
                c.getInt(c.getColumnIndexOrThrow("idProcedimiento")),
                c.getString(c.getColumnIndexOrThrow("nombreProcedimiento")),
                c.getString(c.getColumnIndexOrThrow("categoria")),
                c.getString(c.getColumnIndexOrThrow("descripcion")),
                c.getString(c.getColumnIndexOrThrow("requisitos")),
                c.getString(c.getColumnIndexOrThrow("plazosInfo")),
                c.getString(c.getColumnIndexOrThrow("plazoInicio")),
                c.getString(c.getColumnIndexOrThrow("plazoFin")),
                c.getInt(c.getColumnIndexOrThrow("solicitudOnline")) == 1,
                c.getString(c.getColumnIndexOrThrow("palabrasClave")));
    }
}
