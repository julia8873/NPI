package com.example.nuietsiit.data;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.nuietsiit.data.modelo.AsignaturaCatalogo;
import com.example.nuietsiit.data.modelo.Libro;
import com.example.nuietsiit.util.ProximaClase;
import com.example.nuietsiit.util.TextoUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Acceso a datos de Biblioteca: LIBRO, ASIGNATURA_LIBRO.
 */
public class BibliotecaDB {

    private final DBHelper dbHelper;

    public BibliotecaDB(DBHelper dbHelper) {
        this.dbHelper = dbHelper;
    }

    // texto null/vacío = todos; cada palabra (sin tildes) debe aparecer en título, autores, editorial o ISBN
    public List<Libro> buscarLibros(String texto) {
        StringBuilder where = new StringBuilder("1=1");
        List<String> args = new ArrayList<>();
        for (String palabra : TextoUtil.normalizar(texto).split("\\s+")) {
            if (palabra.isEmpty()) continue;
            where.append(" AND textoBusqueda LIKE ?");
            args.add("%" + palabra + "%");
        }
        return consultar("SELECT *, NULL AS tipoBibliografia FROM LIBRO WHERE " + where +
                " ORDER BY titulo", args.toArray(new String[0]));
    }

    // null si no existe
    public Libro getLibro(int idLibro) {
        List<Libro> libros = consultar("SELECT *, NULL AS tipoBibliografia FROM LIBRO WHERE idLibro = ?",
                new String[]{String.valueOf(idLibro)});
        return libros.isEmpty() ? null : libros.get(0);
    }

    // isbn = ISBN-13 sin guiones (convertir ISBN-10 antes de llamar); null si no existe
    public Libro getLibroPorIsbn(String isbn) {
        List<Libro> libros = consultar("SELECT *, NULL AS tipoBibliografia FROM LIBRO WHERE isbn = ?",
                new String[]{isbn});
        return libros.isEmpty() ? null : libros.get(0);
    }

    // bibliografía de una asignatura: primero la básica, luego la complementaria
    public List<Libro> getLibrosDeAsignatura(String codigoAsignatura) {
        return consultar(
                "SELECT L.*, AL.tipoBibliografia FROM LIBRO L " +
                "JOIN ASIGNATURA_LIBRO AL ON L.idLibro = AL.idLibro " +
                "WHERE AL.codigoAsignatura = ? " +
                "ORDER BY AL.tipoBibliografia, L.titulo",
                new String[]{codigoAsignatura});
    }

    // códigos de las asignaturas que usan un libro (para la ficha tras escanear)
    public List<String> getAsignaturasDeLibro(int idLibro) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        List<String> codigos = new ArrayList<>();
        try (Cursor c = db.rawQuery(
                "SELECT codigoAsignatura FROM ASIGNATURA_LIBRO WHERE idLibro = ? ORDER BY codigoAsignatura",
                new String[]{String.valueOf(idLibro)})) {
            while (c.moveToNext()) codigos.add(c.getString(0));
        }
        return codigos;
    }

    // de cada libro, sus asignaturas y el tipo de bibliografía (filtros y marca "Básica" de la balda)
    public Map<Integer, Map<String, String>> getBibliografiaPorLibro() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Map<Integer, Map<String, String>> porLibro = new HashMap<>();
        try (Cursor c = db.rawQuery("SELECT idLibro, codigoAsignatura, tipoBibliografia FROM ASIGNATURA_LIBRO", null)) {
            while (c.moveToNext()) {
                porLibro.computeIfAbsent(c.getInt(0), k -> new HashMap<>()).put(c.getString(1), c.getString(2));
            }
        }
        return porLibro;
    }

    // asignaturas con bibliografía, una fila por grado en el que se imparten (panel Explorar)
    public List<AsignaturaCatalogo> getCatalogoAsignaturas() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        List<AsignaturaCatalogo> catalogo = new ArrayList<>();
        try (Cursor c = db.rawQuery(
                "SELECT DISTINCT A.codigoAsignatura, A.nombreAsignatura, G.idGrado, G.nombreGrado, AG.curso, AG.rama " +
                "FROM ASIGNATURA A " +
                "JOIN ASIGNATURA_LIBRO AL ON AL.codigoAsignatura = A.codigoAsignatura " +
                "JOIN ASIGNATURA_GRADO AG ON AG.codigoAsignatura = A.codigoAsignatura " +
                "JOIN GRADO G ON G.idGrado = AG.idGrado " +
                "ORDER BY A.nombreAsignatura, G.idGrado", null)) {
            while (c.moveToNext()) {
                catalogo.add(new AsignaturaCatalogo(c.getString(0), c.getString(1), c.getInt(2), c.getString(3),
                        c.getInt(4), c.isNull(5) ? null : c.getString(5)));
            }
        }
        return catalogo;
    }

    // horario del usuario según su matrícula (para saber cuál es su próxima clase)
    public List<ProximaClase.Sesion> getClasesMatriculadas(String correo) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        List<ProximaClase.Sesion> sesiones = new ArrayList<>();
        try (Cursor c = db.rawQuery(
                "SELECT SC.codigoAsignatura, SC.diaSemana, SC.horaInicio, SC.horaFin FROM SESION_CLASE SC " +
                "JOIN MATRICULA M ON SC.codigoAsignatura = M.codigoAsignatura " +
                "  AND SC.grupo = M.grupo AND SC.cursoAcademico = M.cursoAcademico " +
                "WHERE M.correoEstudiante = ?", new String[]{correo.toLowerCase()})) {
            while (c.moveToNext()) {
                sesiones.add(new ProximaClase.Sesion(c.getString(0), c.getInt(1), c.getString(2), c.getString(3)));
            }
        }
        return sesiones;
    }

    // asignaturas en las que está matriculado un usuario (filtro "Mis asignaturas")
    public List<String> getAsignaturasMatriculadas(String correo) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        List<String> codigos = new ArrayList<>();
        try (Cursor c = db.rawQuery(
                "SELECT DISTINCT codigoAsignatura FROM MATRICULA WHERE correoEstudiante = ?", new String[]{correo.toLowerCase()})) {
            while (c.moveToNext()) codigos.add(c.getString(0));
        }
        return codigos;
    }

    private List<Libro> consultar(String sql, String[] args) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        List<Libro> libros = new ArrayList<>();
        try (Cursor c = db.rawQuery(sql, args)) {
            while (c.moveToNext()) libros.add(leerLibro(c));
        }
        return libros;
    }

    private static Libro leerLibro(Cursor c) {
        int iAnio = c.getColumnIndexOrThrow("anioPublicacion");
        return new Libro(
                c.getInt(c.getColumnIndexOrThrow("idLibro")),
                c.getString(c.getColumnIndexOrThrow("titulo")),
                c.getString(c.getColumnIndexOrThrow("autores")),
                c.getString(c.getColumnIndexOrThrow("isbn")),
                c.getString(c.getColumnIndexOrThrow("editorial")),
                c.isNull(iAnio) ? null : c.getInt(iAnio),
                c.getString(c.getColumnIndexOrThrow("edicion")),
                c.getString(c.getColumnIndexOrThrow("enlaceCatalogo")),
                c.getInt(c.getColumnIndexOrThrow("ejemplaresTotales")),
                c.getInt(c.getColumnIndexOrThrow("ejemplaresDisponibles")),
                c.getString(c.getColumnIndexOrThrow("signatura")),
                c.getString(c.getColumnIndexOrThrow("ubicacion")),
                c.getString(c.getColumnIndexOrThrow("portada")),
                c.getString(c.getColumnIndexOrThrow("tipoBibliografia")));
    }
}
