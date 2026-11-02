package com.example.nuietsiit.data;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.nuietsiit.data.modelo.Libro;
import com.example.nuietsiit.util.TextoUtil;

import java.util.ArrayList;
import java.util.List;

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

    // todos los códigos con bibliografía (para el selector de asignatura mientras no haya MATRICULA)
    public List<String> getCodigosAsignatura() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        List<String> codigos = new ArrayList<>();
        try (Cursor c = db.rawQuery(
                "SELECT DISTINCT codigoAsignatura FROM ASIGNATURA_LIBRO ORDER BY codigoAsignatura", null)) {
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
