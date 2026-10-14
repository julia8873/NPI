package com.example.pruebanpi;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

/**
 * Acceso a datos del módulo Académico:
 * ASIGNATURA, ASIGNATURA_GRADO, IMPARTE, MATRICULA,
 * SESION_CLASE, EXAMEN, LIBRO, ASIGNATURA_LIBRO
 */
public class AcademicoDB {

    private final DBHelper dbHelper;

    public AcademicoDB(DBHelper dbHelper) {
        this.dbHelper = dbHelper;
    }

    // --------------------------------------------------------------
    // ASIGNATURA
    // --------------------------------------------------------------

    // Inserta una asignatura. 
    // Devuelve true si se ha insertado correctamente.
    public boolean insertarAsignatura(String codigoAsignatura, String nombreAsignatura,
                                      int cuatrimestre, double creditosECTS,
                                      String enlaceGuiaDocente) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("codigoAsignatura", codigoAsignatura);
        cv.put("nombreAsignatura", nombreAsignatura);
        cv.put("cuatrimestre", cuatrimestre);       // 1 o 2
        cv.put("creditosECTS", creditosECTS);
        cv.put("enlaceGuiaDocente", enlaceGuiaDocente);
        long resultado = db.insert("ASIGNATURA", null, cv);
        return resultado != -1;
    }

    // Obtiene una asignatura por su código. 
    public Cursor obtenerAsignatura(String codigoAsignatura) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        return db.rawQuery("SELECT * FROM ASIGNATURA WHERE codigoAsignatura = ?",
                new String[]{codigoAsignatura});
    }

    // Lista todas las asignaturas de un grado concreto. 
    public Cursor listarAsignaturasDeGrado(int idGrado) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        return db.rawQuery(
                "SELECT A.*, AG.curso, AG.tipo FROM ASIGNATURA A " +
                "JOIN ASIGNATURA_GRADO AG ON A.codigoAsignatura = AG.codigoAsignatura " +
                "WHERE AG.idGrado = ? " +
                "ORDER BY AG.curso, A.nombreAsignatura",
                new String[]{String.valueOf(idGrado)});
    }

    // --------------------------------------------------------------
    // ASIGNATURA_GRADO
    // --------------------------------------------------------------

    // Asocia una asignatura a un grado con su curso y tipo. 
    public boolean insertarAsignaturaGrado(String codigoAsignatura, int idGrado,
                                           int curso, String tipo) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("codigoAsignatura", codigoAsignatura);
        cv.put("idGrado", idGrado);
        cv.put("curso", curso);  // 1..6
        cv.put("tipo", tipo);    // 'obligatoria' | 'optativa'
        long resultado = db.insert("ASIGNATURA_GRADO", null, cv);
        return resultado != -1;
    }

    // --------------------------------------------------------------
    // IMPARTE
    // --------------------------------------------------------------

    // Registra que un profesor imparte una asignatura en un curso académico. 
    public boolean insertarImparte(String codigoAsignatura, String correoProfesor,
                                   String cursoAcademico) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("codigoAsignatura", codigoAsignatura);
        cv.put("correoProfesor", correoProfesor.toLowerCase());
        cv.put("cursoAcademico", cursoAcademico); // formato '2026-27'
        long resultado = db.insert("IMPARTE", null, cv);
        return resultado != -1;
    }

    // Devuelve las asignaturas que imparte un profesor en un curso académico dado. 
    public Cursor listarAsignaturasDe(String correoProfesor, String cursoAcademico) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        return db.rawQuery(
                "SELECT A.* FROM ASIGNATURA A " +
                "JOIN IMPARTE I ON A.codigoAsignatura = I.codigoAsignatura " +
                "WHERE I.correoProfesor = ? AND I.cursoAcademico = ?",
                new String[]{correoProfesor.toLowerCase(), cursoAcademico});
    }

    // --------------------------------------------------------------
    // MATRICULA
    // --------------------------------------------------------------

    // Matricula a un estudiante en una asignatura. 
    public boolean insertarMatricula(String correoEstudiante, String codigoAsignatura,
                                     String cursoAcademico, String grupo) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("correoEstudiante", correoEstudiante.toLowerCase());
        cv.put("codigoAsignatura", codigoAsignatura);
        cv.put("cursoAcademico", cursoAcademico);
        cv.put("grupo", grupo);
        long resultado = db.insert("MATRICULA", null, cv);
        return resultado != -1;
    }

    // Devuelve las asignaturas en las que está matriculado un estudiante. 
    public Cursor listarMatriculasDe(String correoEstudiante, String cursoAcademico) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        return db.rawQuery(
                "SELECT A.*, M.grupo FROM ASIGNATURA A " +
                "JOIN MATRICULA M ON A.codigoAsignatura = M.codigoAsignatura " +
                "WHERE M.correoEstudiante = ? AND M.cursoAcademico = ?",
                new String[]{correoEstudiante.toLowerCase(), cursoAcademico});
    }

    // --------------------------------------------------------------
    // SESION_CLASE
    // --------------------------------------------------------------

    /**
     * Inserta una sesión de clase.
     * idEspacio puede ser null si aún no tiene aula asignada.
     * correoProfesor puede ser null.
     */
    public boolean insertarSesionClase(String codigoAsignatura, Integer idEspacio,
                                       int diaSemana, String horaInicio, String horaFin,
                                       String grupo, String tipoSesion,
                                       String correoProfesor, String cursoAcademico) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("codigoAsignatura", codigoAsignatura);
        if (idEspacio != null) cv.put("idEspacio", idEspacio); else cv.putNull("idEspacio");
        cv.put("diaSemana", diaSemana);   // 1=lunes..7=domingo
        cv.put("horaInicio", horaInicio); // 'HH:MM'
        cv.put("horaFin", horaFin);
        cv.put("grupo", grupo);
        cv.put("tipoSesion", tipoSesion); // 'teoria'|'practicas'|'seminario'
        if (correoProfesor != null) cv.put("correoProfesor", correoProfesor.toLowerCase());
        else cv.putNull("correoProfesor");
        cv.put("cursoAcademico", cursoAcademico);
        long resultado = db.insert("SESION_CLASE", null, cv);
        return resultado != -1;
    }

    /**
     * Devuelve la sesión (o sesiones) que se imparten en un espacio en un momento concreto.
     * Útil para mostrar "la clase que se imparte ahora" en cada aula.
     * diaSemana: 1-7. horaActual: 'HH:MM'.
     */
    public Cursor obtenerSesionActualEnEspacio(int idEspacio, int diaSemana,
                                               String horaActual, String cursoAcademico) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        return db.rawQuery(
                "SELECT SC.*, A.nombreAsignatura FROM SESION_CLASE SC " +
                "JOIN ASIGNATURA A ON SC.codigoAsignatura = A.codigoAsignatura " +
                "WHERE SC.idEspacio = ? " +
                "  AND SC.diaSemana = ? " +
                "  AND SC.horaInicio <= ? AND SC.horaFin > ? " +
                "  AND SC.cursoAcademico = ?",
                new String[]{
                    String.valueOf(idEspacio),
                    String.valueOf(diaSemana),
                    horaActual, horaActual,
                    cursoAcademico
                });
    }

    // Devuelve las sesiones de clase de un estudiante (a través de su matrícula). 
    public Cursor listarSesionesDe(String correoEstudiante, String cursoAcademico) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        return db.rawQuery(
                "SELECT SC.*, A.nombreAsignatura FROM SESION_CLASE SC " +
                "JOIN MATRICULA M ON SC.codigoAsignatura = M.codigoAsignatura " +
                "  AND SC.grupo = M.grupo " +
                "  AND SC.cursoAcademico = M.cursoAcademico " +
                "JOIN ASIGNATURA A ON A.codigoAsignatura = SC.codigoAsignatura " +
                "WHERE M.correoEstudiante = ? AND M.cursoAcademico = ? " +
                "ORDER BY SC.diaSemana, SC.horaInicio",
                new String[]{correoEstudiante.toLowerCase(), cursoAcademico});
    }

    // --------------------------------------------------------------
    // EXAMEN
    // --------------------------------------------------------------

    /**
     * Inserta un examen.
     * idEspacio puede ser null si aún no tiene aula asignada.
     */
    public boolean insertarExamen(String codigoAsignatura, Integer idEspacio,
                                  String fecha, String horaInicio, int duracion,
                                  String comentariosProfesores, String cursoAcademico,
                                  String convocatoria) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("codigoAsignatura", codigoAsignatura);
        if (idEspacio != null) cv.put("idEspacio", idEspacio); else cv.putNull("idEspacio");
        cv.put("fecha", fecha);           // 'YYYY-MM-DD'
        cv.put("horaInicio", horaInicio); // 'HH:MM'
        cv.put("duracion", duracion);     // minutos
        cv.put("comentariosProfesores", comentariosProfesores); // puede ser null
        cv.put("cursoAcademico", cursoAcademico);
        cv.put("convocatoria", convocatoria); // 'ordinaria'|'extraordinaria'
        long resultado = db.insert("EXAMEN", null, cv);
        return resultado != -1;
    }

    // Devuelve los exámenes de las asignaturas en las que está matriculado un estudiante. 
    public Cursor listarExamenesDe(String correoEstudiante, String cursoAcademico) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        return db.rawQuery(
                "SELECT E.*, A.nombreAsignatura FROM EXAMEN E " +
                "JOIN MATRICULA M ON E.codigoAsignatura = M.codigoAsignatura " +
                "  AND E.cursoAcademico = M.cursoAcademico " +
                "JOIN ASIGNATURA A ON A.codigoAsignatura = E.codigoAsignatura " +
                "WHERE M.correoEstudiante = ? AND M.cursoAcademico = ? " +
                "ORDER BY E.fecha, E.horaInicio",
                new String[]{correoEstudiante.toLowerCase(), cursoAcademico});
    }

    // --------------------------------------------------------------
    // LIBRO
    // --------------------------------------------------------------

    //Inserta un libro. Devuelve el id generado o -1 si falla.
    public long insertarLibro(String titulo, String autores, String isbn,
                              String enlaceCatalogo, boolean disponibleBiblioteca,
                              String ubicacionBiblioteca) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("titulo", titulo);
        cv.put("autores", autores);
        cv.put("isbn", isbn);
        cv.put("enlaceCatalogo", enlaceCatalogo);              // puede ser null
        cv.put("disponibleBiblioteca", disponibleBiblioteca ? 1 : 0);
        cv.put("ubicacionBiblioteca", ubicacionBiblioteca);    // puede ser null
        return db.insert("LIBRO", null, cv);
    }

    // Obtiene un libro por su id.
    public Cursor obtenerLibro(int idLibro) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        return db.rawQuery("SELECT * FROM LIBRO WHERE idLibro = ?",
                new String[]{String.valueOf(idLibro)});
    }

    // --------------------------------------------------------------
    // ASIGNATURA_LIBRO
    // --------------------------------------------------------------

    // Asocia un libro a una asignatura con su tipo de bibliografía.
    public boolean insertarAsignaturaLibro(String codigoAsignatura, int idLibro,
                                           String tipoBibliografia) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("codigoAsignatura", codigoAsignatura);
        cv.put("idLibro", idLibro);
        cv.put("tipoBibliografia", tipoBibliografia); // 'basica'|'complementaria'
        long resultado = db.insert("ASIGNATURA_LIBRO", null, cv);
        return resultado != -1;
    }

    // Devuelve los libros (con su tipo) de una asignatura.
    public Cursor listarLibrosDeAsignatura(String codigoAsignatura) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        return db.rawQuery(
                "SELECT L.*, AL.tipoBibliografia FROM LIBRO L " +
                "JOIN ASIGNATURA_LIBRO AL ON L.idLibro = AL.idLibro " +
                "WHERE AL.codigoAsignatura = ? " +
                "ORDER BY AL.tipoBibliografia, L.titulo",
                new String[]{codigoAsignatura});
    }
}
