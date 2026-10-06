package com.example.pruebanpi;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

/**
 * Acceso a datos del módulo de Usuarios:
 * ESTUDIANTE, PROFESOR, ADMINISTRADOR, DESPACHO_PROFESOR, GRADO
 */
public class UsuariosDB {

    private final DBHelper dbHelper;

    public UsuariosDB(DBHelper dbHelper) {
        this.dbHelper = dbHelper;
    }

    // --------------------------------------------------------------
    // ESTUDIANTE
    // --------------------------------------------------------------

    // Inserta un nuevo estudiante.
    // Devuelve true si se ha insertado correctamente.
    public boolean insertarEstudiante(String correo, String nombre, String contraseniaHash,
                                      String enlaceCalendario, int idGrado,
                                      String codigoIdioma, String tokenNotificacion) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("correoEstudiante", correo.toLowerCase());
        cv.put("nombreEstudiante", nombre);
        cv.put("contraseniaHash", contraseniaHash);
        cv.put("enlaceCalendarioGoogle", enlaceCalendario); // puede ser null
        cv.put("idGrado", idGrado);
        cv.put("codigoIdioma", codigoIdioma);
        cv.put("tokenNotificacion", tokenNotificacion);     // puede ser null
        long resultado = db.insert("ESTUDIANTE", null, cv);
        return resultado != -1;
    }

    // Comprueba si ya existe un estudiante con ese correo.
    public boolean existeEstudiante(String correo) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        try (Cursor cursor = db.rawQuery(
                "SELECT 1 FROM ESTUDIANTE WHERE correoEstudiante = ?",
                new String[]{correo.toLowerCase()})) {
            return cursor.getCount() > 0;
        }
    }

    // Comprueba si el correo y la contraseña (hash) coinciden.
    public boolean autenticarEstudiante(String correo, String contraseniaHash) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        try (Cursor cursor = db.rawQuery(
                "SELECT 1 FROM ESTUDIANTE WHERE correoEstudiante = ? AND contraseniaHash = ?",
                new String[]{correo.toLowerCase(), contraseniaHash})) {
            return cursor.getCount() > 0;
        }
    }

    // Obtiene todos los datos de un estudiante por correo.
    public Cursor obtenerEstudiante(String correo) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        return db.rawQuery(
                "SELECT * FROM ESTUDIANTE WHERE correoEstudiante = ?",
                new String[]{correo.toLowerCase()});
    }

    // Actualiza el token de notificación de un estudiante.
    // Devuelve true si se ha actualizado correctamente.
    public boolean actualizarTokenEstudiante(String correo, String nuevoToken) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("tokenNotificacion", nuevoToken);
        int filas = db.update("ESTUDIANTE", cv,
                "correoEstudiante = ?", new String[]{correo.toLowerCase()});
        return filas > 0;
    }

    // Actualiza el idioma preferido de un estudiante.
    // Devuelve true si se ha actualizado correctamente.
    public boolean actualizarIdiomaEstudiante(String correo, String codigoIdioma) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("codigoIdioma", codigoIdioma);
        int filas = db.update("ESTUDIANTE", cv,
                "correoEstudiante = ?", new String[]{correo.toLowerCase()});
        return filas > 0;
    }

    // --------------------------------------------------------------
    // PROFESOR
    // --------------------------------------------------------------

    // Inserta un nuevo profesor.
    // Devuelve true si se ha insertado correctamente.
    public boolean insertarProfesor(String correo, String nombre, String contraseniaHash,
                                    String enlacePaginaWeb, String tokenNotificacion) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("correoProfesor", correo.toLowerCase());
        cv.put("nombreProfesor", nombre);
        cv.put("contraseniaHash", contraseniaHash);
        cv.put("enlacePaginaWeb", enlacePaginaWeb);       // puede ser null
        cv.put("tokenNotificacion", tokenNotificacion);   // puede ser null
        long resultado = db.insert("PROFESOR", null, cv);
        return resultado != -1;
    }

    // Comprueba si ya existe un profesor con ese correo.
    public boolean existeProfesor(String correo) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        try (Cursor cursor = db.rawQuery(
                "SELECT 1 FROM PROFESOR WHERE correoProfesor = ?",
                new String[]{correo.toLowerCase()})) {
            return cursor.getCount() > 0;
        }
    }

    // Comprueba si el correo y la contraseña (hash) coinciden.
    public boolean autenticarProfesor(String correo, String contraseniaHash) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        try (Cursor cursor = db.rawQuery(
                "SELECT 1 FROM PROFESOR WHERE correoProfesor = ? AND contraseniaHash = ?",
                new String[]{correo.toLowerCase(), contraseniaHash})) {
            return cursor.getCount() > 0;
        }
    }

    // Obtiene todos los datos de un profesor por correo.
    public Cursor obtenerProfesor(String correo) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        return db.rawQuery(
                "SELECT * FROM PROFESOR WHERE correoProfesor = ?",
                new String[]{correo.toLowerCase()});
    }

    // Actualiza el token de notificación de un profesor.
    // Devuelve true si se ha actualizado correctamente.
    public boolean actualizarTokenProfesor(String correo, String nuevoToken) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("tokenNotificacion", nuevoToken);
        int filas = db.update("PROFESOR", cv,
                "correoProfesor = ?", new String[]{correo.toLowerCase()});
        return filas > 0;
    }

    // Lista todos los profesores ordenados por nombre.
    public Cursor listarProfesores() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        return db.rawQuery("SELECT * FROM PROFESOR ORDER BY nombreProfesor", null);
    }

    // --------------------------------------------------------------
    // ADMINISTRADOR
    // --------------------------------------------------------------

    // Inserta un nuevo administrador.
    // Devuelve true si se ha insertado correctamente.
    public boolean insertarAdministrador(String correo, String nombre,
                                         String contraseniaHash, String rol) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("correoAdmin", correo.toLowerCase());
        cv.put("nombreAdmin", nombre);
        cv.put("contraseniaHash", contraseniaHash);
        cv.put("rol", rol); // 'secretaria' | 'direccion' | 'comunicacion'
        long resultado = db.insert("ADMINISTRADOR", null, cv);
        return resultado != -1;
    }

    // Comprueba si el correo y la contraseña (hash) coinciden para un administrador.
    public boolean autenticarAdministrador(String correo, String contraseniaHash) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        try (Cursor cursor = db.rawQuery(
                "SELECT 1 FROM ADMINISTRADOR WHERE correoAdmin = ? AND contraseniaHash = ?",
                new String[]{correo.toLowerCase(), contraseniaHash})) {
            return cursor.getCount() > 0;
        }
    }

    // --------------------------------------------------------------
    // DESPACHO_PROFESOR
    // --------------------------------------------------------------

    // Asocia un profesor a un espacio (despacho).
    // Un profesor puede tener varios despachos asociados.
    public boolean insertarDespachoProfesor(String correoProfesor, int idEspacio) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("correoProfesor", correoProfesor.toLowerCase());
        cv.put("idEspacio", idEspacio);
        long resultado = db.insert("DESPACHO_PROFESOR", null, cv);
        return resultado != -1;
    }

    // Devuelve todos los espacios (despachos) de un profesor.
    public Cursor obtenerDespachosDe(String correoProfesor) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        return db.rawQuery(
                "SELECT E.* FROM ESPACIO E " +
                "JOIN DESPACHO_PROFESOR D ON E.idEspacio = D.idEspacio " +
                "WHERE D.correoProfesor = ?",
                new String[]{correoProfesor.toLowerCase()});
    }

    // --------------------------------------------------------------
    // GRADO
    // --------------------------------------------------------------

    // Inserta un nuevo grado.
    // Devuelve el id generado o -1 si ha fallado.
    public long insertarGrado(String nombreGrado, String enlaceWeb) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("nombreGrado", nombreGrado);
        cv.put("enlaceWeb", enlaceWeb); // puede ser null
        return db.insert("GRADO", null, cv);
    }

    // Lista todos los grados ordenados por nombre.
    public Cursor listarGrados() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        return db.rawQuery("SELECT * FROM GRADO ORDER BY nombreGrado", null);
    }

    // Obtiene un grado por su id.
    public Cursor obtenerGrado(int idGrado) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        return db.rawQuery("SELECT * FROM GRADO WHERE idGrado = ?",
                new String[]{String.valueOf(idGrado)});
    }
}
