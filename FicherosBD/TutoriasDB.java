package com.example.pruebanpi;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

/**
 * Acceso a datos del módulo de Tutorías:
 * TUTORIA_HORARIO, TUTORIA_RESERVA
 *
 * Notificaciones:
 * - Al reservar, se puede obtener el token del estudiante + enlace online con
 *   obtenerDatosNotificacionReserva(idReserva).
 * - El enlace que se usará: si TUTORIA_RESERVA.enlaceOnline no es null, ese;
 *   si no, TUTORIA_HORARIO.enlaceOnline.
 */
public class TutoriasDB {

    private final DBHelper dbHelper;

    public TutoriasDB(DBHelper dbHelper) {
        this.dbHelper = dbHelper;
    }

    // --------------------------------------------------------------
    // TUTORIA_HORARIO
    // --------------------------------------------------------------

    /**
     * Inserta un horario de tutoría para un profesor.
     * idEspacio puede ser null (se usará el despacho del profesor).
     * enlaceOnline puede ser null si es presencial.
     */
    public long insertarHorario(String correoProfesor, int diaSemana,
                                String horaInicio, String horaFin,
                                String modalidad, Integer idEspacio,
                                String enlaceOnline) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("correoProfesor", correoProfesor.toLowerCase());
        cv.put("diaSemana", diaSemana);   // 1=lunes..7=domingo
        cv.put("horaInicio", horaInicio); // 'HH:MM'
        cv.put("horaFin", horaFin);
        cv.put("modalidad", modalidad);   // 'presencial'|'online'|'mixta'
        if (idEspacio != null) cv.put("idEspacio", idEspacio); else cv.putNull("idEspacio");
        cv.put("enlaceOnline", enlaceOnline); // puede ser null
        return db.insert("TUTORIA_HORARIO", null, cv);
    }

    // Devuelve todos los horarios de tutoría de un profesor.
    public Cursor listarHorariosDe(String correoProfesor) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        return db.rawQuery(
                "SELECT * FROM TUTORIA_HORARIO WHERE correoProfesor = ? " +
                "ORDER BY diaSemana, horaInicio",
                new String[]{correoProfesor.toLowerCase()});
    }

    // Obtiene un horario por su id.
    public Cursor obtenerHorario(int idHorario) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        return db.rawQuery("SELECT * FROM TUTORIA_HORARIO WHERE idHorario = ?",
                new String[]{String.valueOf(idHorario)});
    }

    // Elimina un horario de tutoría.
    // Devuelve true si se ha eliminado correctamente.
    public boolean eliminarHorario(int idHorario) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        int filas = db.delete("TUTORIA_HORARIO", "idHorario = ?",
                new String[]{String.valueOf(idHorario)});
        return filas > 0;
    }

    // --------------------------------------------------------------
    // TUTORIA_RESERVA
    // --------------------------------------------------------------

    /**
     * Crea una reserva de tutoría para un estudiante.
     * estado inicial: 'pendiente'.
     * motivo y enlaceOnline pueden ser null.
     * Devuelve el idReserva generado o -1 si falla.
     * NOTA: el índice único parcial (idHorario, fecha) WHERE estado <> 'cancelada'
     * garantiza que no haya dos reservas activas para el mismo slot.
     */
    public long insertarReserva(int idHorario, String correoEstudiante,
                                String fecha, String motivo, String enlaceOnline) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("idHorario", idHorario);
        cv.put("correoEstudiante", correoEstudiante.toLowerCase());
        cv.put("fecha", fecha);           // 'YYYY-MM-DD'
        cv.put("motivo", motivo);         // puede ser null
        cv.put("estado", "pendiente");
        cv.put("enlaceOnline", enlaceOnline); // puede ser null
        return db.insert("TUTORIA_RESERVA", null, cv);
    }

    /**
     * Cambia el estado de una reserva.
     * estado: 'pendiente' | 'confirmada' | 'cancelada'
     */
    public boolean actualizarEstadoReserva(int idReserva, String nuevoEstado) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("estado", nuevoEstado);
        int filas = db.update("TUTORIA_RESERVA", cv,
                "idReserva = ?", new String[]{String.valueOf(idReserva)});
        return filas > 0;
    }

    // Devuelve todas las reservas de un estudiante ordenadas por fecha descendente.
    public Cursor listarReservasDe(String correoEstudiante) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        return db.rawQuery(
                "SELECT TR.*, TH.diaSemana, TH.horaInicio, TH.horaFin, " +
                "       TH.modalidad, TH.correoProfesor, " +
                "       P.nombreProfesor " +
                "FROM TUTORIA_RESERVA TR " +
                "JOIN TUTORIA_HORARIO TH ON TR.idHorario = TH.idHorario " +
                "JOIN PROFESOR P ON TH.correoProfesor = P.correoProfesor " +
                "WHERE TR.correoEstudiante = ? " +
                "ORDER BY TR.fecha DESC",
                new String[]{correoEstudiante.toLowerCase()});
    }

    // Devuelve todas las reservas pendientes o confirmadas de un profesor.
    public Cursor listarReservasParaProfesor(String correoProfesor) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        return db.rawQuery(
                "SELECT TR.*, E.nombreEstudiante FROM TUTORIA_RESERVA TR " +
                "JOIN TUTORIA_HORARIO TH ON TR.idHorario = TH.idHorario " +
                "JOIN ESTUDIANTE E ON TR.correoEstudiante = E.correoEstudiante " +
                "WHERE TH.correoProfesor = ? AND TR.estado <> 'cancelada' " +
                "ORDER BY TR.fecha, TH.horaInicio",
                new String[]{correoProfesor.toLowerCase()});
    }

    /**
     * Obtiene todos los datos necesarios para enviar la notificación al estudiante
     * tras crear o confirmar una reserva.
     *
     * Devuelve una fila con:
     *  - tokenNotificacion (del estudiante)
     *  - correoEstudiante, nombreEstudiante
     *  - correoProfesor, nombreProfesor
     *  - fecha, horaInicio, horaFin, modalidad
     *  - enlaceEfectivo: TUTORIA_RESERVA.enlaceOnline si no es NULL,
     *    si no TUTORIA_HORARIO.enlaceOnline (puede seguir siendo NULL si es presencial)
     *  - idEspacio (del horario, para calcular la ruta si es presencial)
     */
    public Cursor obtenerDatosNotificacionReserva(int idReserva) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        return db.rawQuery(
                "SELECT " +
                "  E.tokenNotificacion, " +
                "  E.correoEstudiante, " +
                "  E.nombreEstudiante, " +
                "  P.correoProfesor, " +
                "  P.nombreProfesor, " +
                "  TR.fecha, " +
                "  TH.horaInicio, " +
                "  TH.horaFin, " +
                "  TH.modalidad, " +
                "  TH.idEspacio, " +
                "  TR.estado, " +
                "  TR.motivo, " +
                // enlaceOnline de la reserva tiene prioridad sobre el del horario
                // coalesce devuelve el primer valor que no sea null
                "  COALESCE(TR.enlaceOnline, TH.enlaceOnline) AS enlaceEfectivo " +
                "FROM TUTORIA_RESERVA TR " +
                "JOIN TUTORIA_HORARIO TH ON TR.idHorario = TH.idHorario " +
                "JOIN ESTUDIANTE E ON TR.correoEstudiante = E.correoEstudiante " +
                "JOIN PROFESOR P ON TH.correoProfesor = P.correoProfesor " +
                "WHERE TR.idReserva = ?",
                new String[]{String.valueOf(idReserva)});
    }

    // Comprueba si un slot (idHorario + fecha) ya está reservado
    // con estado distinto de 'cancelada'.
    public boolean slotOcupado(int idHorario, String fecha) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        try (Cursor cursor = db.rawQuery(
                "SELECT 1 FROM TUTORIA_RESERVA " +
                "WHERE idHorario = ? AND fecha = ? AND estado <> 'cancelada'",
                new String[]{String.valueOf(idHorario), fecha})) {
            return cursor.getCount() > 0;
        }
    }
}
