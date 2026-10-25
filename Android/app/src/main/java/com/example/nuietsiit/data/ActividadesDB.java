package com.example.nuietsiit.data;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.nuietsiit.data.modelo.Actividad;
import com.example.nuietsiit.data.modelo.Comunidad;
import com.example.nuietsiit.data.modelo.EventoCalendario;
import com.example.nuietsiit.util.FechasUtil;

import java.util.ArrayList;
import java.util.List;

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

    /**
     * Obtiene las actividades de la base de datos para una fecha específica (YYYY-MM-DD),
     * devueltas como objetos EventoCalendario para mostrarlas automáticamente en el Calendario.
     */
    public List<EventoCalendario> obtenerActividadesPorFecha(String fechaYYYYMMDD) {
        List<EventoCalendario> lista = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        try (Cursor cursor = db.rawQuery(
                "SELECT idActividad, titulo, descripcion, tipoActividad, fechaInicio " +
                "FROM ACTIVIDAD " +
                "WHERE fechaInicio LIKE ? || '%' OR substr(fechaInicio, 1, 10) = ?",
                new String[]{fechaYYYYMMDD, fechaYYYYMMDD})) {
            while (cursor.moveToNext()) {
                int id = cursor.getInt(cursor.getColumnIndexOrThrow("idActividad"));
                String titulo = cursor.getString(cursor.getColumnIndexOrThrow("titulo"));
                String desc = cursor.getString(cursor.getColumnIndexOrThrow("descripcion"));
                String fechaInicio = cursor.getString(cursor.getColumnIndexOrThrow("fechaInicio"));

                String hora = "";
                if (fechaInicio != null && fechaInicio.length() >= 16) {
                    hora = fechaInicio.substring(11, 16);
                }

                // ID negativo para diferenciar actividades del sistema de eventos de usuario
                lista.add(new EventoCalendario(-id, "", titulo, "Actividad", fechaYYYYMMDD, hora, desc));
            }
        } catch (Exception ignored) {}
        return lista;
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

    // --------------------------------------------------------------
    // PANTALLA DE ACTIVIDADES (Miguel)
    // --------------------------------------------------------------

    /**
     * Devuelve las actividades desde hoy, con los datos del curso AbiertaUGR (si lo es),
     * el lugar, cuántas personas hay apuntadas y si correoEstudiante está apuntado.
     */
    public List<Actividad> listarActividadesConDetalles(String correoEstudiante, String fechaHoyISO) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String correo = correoEstudiante != null ? correoEstudiante.toLowerCase() : "";
        List<Actividad> lista = new ArrayList<>();
        try (Cursor c = db.rawQuery(
                "SELECT A.*, C.creditos, C.plazoInicioInscripcion, C.plazoFinInscripcion, " +
                "       C.plazasTotales, E.nombreEspacio, " +
                "       (SELECT COUNT(*) FROM INSCRIPCION_ACTIVIDAD I " +
                "         WHERE I.idActividad = A.idActividad) AS inscritos, " +
                "       EXISTS (SELECT 1 FROM INSCRIPCION_ACTIVIDAD I " +
                "         WHERE I.idActividad = A.idActividad AND I.correoEstudiante = ?) AS inscrito " +
                "FROM ACTIVIDAD A " +
                "LEFT JOIN CURSO_ABIERTAUGR C ON A.idActividad = C.idActividad " +
                "LEFT JOIN ESPACIO E ON A.idEspacio = E.idEspacio " +
                "WHERE A.fechaInicio >= ? " +
                "ORDER BY A.fechaInicio",
                new String[]{correo, fechaHoyISO})) {
            while (c.moveToNext()) {
                lista.add(leerActividad(c));
            }
        }
        return lista;
    }

    /**
     * Comunidades de chat como objetos Comunidad.
     * Con grado: las de ese grado primero y luego las generales.
     * Sin grado (null o 0 = "Ninguno"): todas.
     */
    public List<Comunidad> obtenerComunidades(Integer idGrado) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor c = (idGrado == null || idGrado == 0)
                ? db.rawQuery("SELECT * FROM COMUNIDAD_CHAT " +
                        "ORDER BY idGrado IS NULL, nombreComunidad", null)
                : db.rawQuery("SELECT * FROM COMUNIDAD_CHAT " +
                        "WHERE idGrado = ? OR idGrado IS NULL " +
                        "ORDER BY idGrado IS NULL, nombreComunidad",
                        new String[]{String.valueOf(idGrado)});
        List<Comunidad> lista = new ArrayList<>();
        try (Cursor cursor = c) {
            while (cursor.moveToNext()) {
                Comunidad com = new Comunidad();
                com.id = cursor.getInt(cursor.getColumnIndexOrThrow("idComunidad"));
                com.nombre = cursor.getString(cursor.getColumnIndexOrThrow("nombreComunidad"));
                com.plataforma = cursor.getString(cursor.getColumnIndexOrThrow("plataforma"));
                com.enlace = cursor.getString(cursor.getColumnIndexOrThrow("enlaceAcceso"));
                com.descripcion = cursor.getString(cursor.getColumnIndexOrThrow("descripcion"));
                int iGrado = cursor.getColumnIndexOrThrow("idGrado");
                com.idGrado = cursor.isNull(iGrado) ? null : cursor.getInt(iGrado);
                lista.add(com);
            }
        }
        return lista;
    }

    /** Desapunta a un estudiante de una actividad. */
    public boolean borrarInscripcion(String correoEstudiante, int idActividad) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        return db.delete("INSCRIPCION_ACTIVIDAD",
                "correoEstudiante = ? AND idActividad = ?",
                new String[]{correoEstudiante.toLowerCase(), String.valueOf(idActividad)}) > 0;
    }

    // Convierte la fila actual del cursor en un objeto Actividad
    private Actividad leerActividad(Cursor c) {
        Actividad a = new Actividad();
        a.id = c.getInt(c.getColumnIndexOrThrow("idActividad"));
        a.titulo = c.getString(c.getColumnIndexOrThrow("titulo"));
        a.descripcion = c.getString(c.getColumnIndexOrThrow("descripcion"));
        a.tipo = c.getString(c.getColumnIndexOrThrow("tipoActividad"));
        a.inicio = FechasUtil.parsear(c.getString(c.getColumnIndexOrThrow("fechaInicio")));
        a.fin = FechasUtil.parsear(c.getString(c.getColumnIndexOrThrow("fechaFin")));
        a.enlace = c.getString(c.getColumnIndexOrThrow("enlaceInfo"));
        a.lugar = c.getString(c.getColumnIndexOrThrow("nombreEspacio"));

        int iCreditos = c.getColumnIndexOrThrow("creditos");
        if (!c.isNull(iCreditos)) {
            a.creditos = c.getDouble(iCreditos);
            a.plazoInicio = FechasUtil.parsear(c.getString(c.getColumnIndexOrThrow("plazoInicioInscripcion"))).toLocalDate();
            a.plazoFin = FechasUtil.parsear(c.getString(c.getColumnIndexOrThrow("plazoFinInscripcion"))).toLocalDate();
            int iPlazas = c.getColumnIndexOrThrow("plazasTotales");
            a.plazasTotales = c.isNull(iPlazas) ? null : c.getInt(iPlazas);
        }
        a.inscritos = c.getInt(c.getColumnIndexOrThrow("inscritos"));
        a.inscrito = c.getInt(c.getColumnIndexOrThrow("inscrito")) == 1;
        return a;
    }
}
