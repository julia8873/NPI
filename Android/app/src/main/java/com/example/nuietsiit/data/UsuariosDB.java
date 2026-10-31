package com.example.nuietsiit.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;
import java.util.List;

/**
 * Acceso a datos del módulo de Usuarios:
 * ESTUDIANTE, PROFESOR, ADMINISTRADOR, DESPACHO_PROFESOR, GRADO
 */
public class UsuariosDB {

    private final DBHelper dbHelper;

    public UsuariosDB(DBHelper dbHelper) {
        this.dbHelper = dbHelper;
    }

    // constructor con Context
    public UsuariosDB(Context context) {
        this(DBHelper.getInstance(context));
    }

    // --------------------------------------------------------------
    // ESTUDIANTE
    // --------------------------------------------------------------

    // Inserta un nuevo estudiante con valores por defecto para grado (1) e idioma ('es')
    public boolean insertarEstudiante(String correo, String nombre, String contraseniaHash) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        db.execSQL("INSERT OR IGNORE INTO IDIOMA (codigoIdioma, nombreIdioma) VALUES ('es', 'Español'), ('en', 'English')");
        db.execSQL("INSERT OR IGNORE INTO GRADO (idGrado, nombreGrado) VALUES (0, '-- Ninguno --'), (1, 'Grado en Ingeniería Informática')");

        ContentValues cv = new ContentValues();
        cv.put("correoEstudiante", correo.toLowerCase());
        cv.put("nombreEstudiante", nombre);
        cv.put("contraseniaHash", contraseniaHash);
        cv.put("enlaceCalendarioGoogle", (String) null);
        cv.put("idGrado", 0);
        cv.put("codigoIdioma", "es");
        cv.put("tokenNotificacion", (String) null);

        long resultado = db.insert("ESTUDIANTE", null, cv);
        return resultado != -1;
    }

    // Comprueba si ya existe un estudiante con ese correo.
    public boolean existeEstudiante(String correo) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        try (Cursor cursor = db.rawQuery(
                "SELECT 1 FROM ESTUDIANTE WHERE correoEstudiante = ?",
                new String[]{correo.toLowerCase()})) {
            return cursor != null && cursor.getCount() > 0;
        }
    }

    // Comprueba si el correo y la contraseña (hash) coinciden.
    public boolean autenticarEstudiante(String correo, String contraseniaHash) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        try (Cursor cursor = db.rawQuery(
                "SELECT 1 FROM ESTUDIANTE WHERE correoEstudiante = ? AND contraseniaHash = ?",
                new String[]{correo.toLowerCase(), contraseniaHash})) {
            return cursor != null && cursor.getCount() > 0;
        }
    }

    // Obtiene todos los datos de un estudiante por correo.
    public Cursor obtenerEstudiante(String correo) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        return db.rawQuery(
                "SELECT * FROM ESTUDIANTE WHERE correoEstudiante = ?",
                new String[]{correo.toLowerCase()});
    }

    // Obtiene el nombre de un usuario por correo.
    public String obtenerNombreUsuario(String correo) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String correoLower = correo.toLowerCase();

        try (Cursor cursor = db.rawQuery(
                "SELECT nombreEstudiante FROM ESTUDIANTE WHERE correoEstudiante = ?",
                new String[]{correoLower})) {
            if (cursor != null && cursor.moveToFirst()) {
                return cursor.getString(cursor.getColumnIndexOrThrow("nombreEstudiante"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    // Obtiene el id del grado de un usuario por correo.
    public Integer obtenerIdGradoUsuario(String correo) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String query = "SELECT idGrado FROM ESTUDIANTE WHERE correoEstudiante = ?";
        try (Cursor cursor = db.rawQuery(query, new String[]{correo.toLowerCase()})) {
            if (cursor != null && cursor.moveToFirst()) {
                int colIndex = cursor.getColumnIndexOrThrow("idGrado");
                if (!cursor.isNull(colIndex)) {
                    return cursor.getInt(colIndex);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    // Obtiene el nombre del grado de un usuario por correo.
    public String obtenerGradoUsuario(String correo) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String query = "SELECT G.nombreGrado FROM ESTUDIANTE E " +
                "LEFT JOIN GRADO G ON E.idGrado = G.idGrado " +
                "WHERE E.correoEstudiante = ?";
        try (Cursor cursor = db.rawQuery(query, new String[]{correo.toLowerCase()})) {
            if (cursor != null && cursor.moveToFirst()) {
                String grado = cursor.getString(cursor.getColumnIndexOrThrow("nombreGrado"));
                return grado != null ? grado : "";
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return "";
    }

    // Obtiene el código del idioma de preferencia de un usuario por correo.
    public String obtenerCodigoIdiomaUsuario(String correo) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String query = "SELECT codigoIdioma FROM ESTUDIANTE WHERE correoEstudiante = ?";
        try (Cursor cursor = db.rawQuery(query, new String[]{correo.toLowerCase()})) {
            if (cursor != null && cursor.moveToFirst()) {
                int colIndex = cursor.getColumnIndexOrThrow("codigoIdioma");
                if (!cursor.isNull(colIndex)) {
                    return cursor.getString(colIndex);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    // Obtiene el idioma de preferencia de un usuario por correo.
    public String obtenerIdiomaUsuario(String correo) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String query = "SELECT I.nombreIdioma FROM ESTUDIANTE E " +
                "LEFT JOIN IDIOMA I ON E.codigoIdioma = I.codigoIdioma " +
                "WHERE E.correoEstudiante = ?";
        try (Cursor cursor = db.rawQuery(query, new String[]{correo.toLowerCase()})) {
            if (cursor != null && cursor.moveToFirst()) {
                String idioma = cursor.getString(cursor.getColumnIndexOrThrow("nombreIdioma"));
                return idioma != null ? idioma : "";
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return "";
    }

    // Actualiza el enlace al calendario de Google de un estudiante.
    public boolean actualizarCalendarioEstudiante(String correo, String enlaceCalendario) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("enlaceCalendarioGoogle", enlaceCalendario);
        int filas = db.update("ESTUDIANTE", cv,
                "correoEstudiante = ?", new String[]{correo.toLowerCase()});
        return filas > 0;
    }

    // Actualiza los datos de un estudiante. Solo modifica los campos no nulos.
    public boolean actualizarAjustesEstudiante(String correo, String nuevoNombre, String nuevaPassword,
                                               Integer nuevoIdGrado, String nuevoCodigoIdioma,
                                               String nuevoCalendario) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues cv = new ContentValues();

        if (nuevoNombre != null && !nuevoNombre.isEmpty()) {
            cv.put("nombreEstudiante", nuevoNombre);
        }
        if (nuevaPassword != null && !nuevaPassword.isEmpty()) {
            cv.put("contraseniaHash", nuevaPassword);
        }
        if (nuevoIdGrado != null) {
            cv.put("idGrado", nuevoIdGrado);
        } else {
            cv.put("idGrado", 0);
        }
        if (nuevoCodigoIdioma != null && !nuevoCodigoIdioma.isEmpty()) {
            cv.put("codigoIdioma", nuevoCodigoIdioma);
        } else {
            cv.put("codigoIdioma", "es");
        }
        if (nuevoCalendario != null) {
            cv.put("enlaceCalendarioGoogle", nuevoCalendario);
        }

        if (cv.size() == 0) {
            return false; // No había campos que actualizar
        }

        int filas = db.update("ESTUDIANTE", cv,
                "correoEstudiante = ?", new String[]{correo.toLowerCase()});
        return filas > 0;
    }

    // --------------------------------------------------------------
    // PROFESOR
    // --------------------------------------------------------------

    public boolean insertarProfesor(String correo, String nombre, String contraseniaHash,
                                    String enlacePaginaWeb, String tokenNotificacion) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("correoProfesor", correo.toLowerCase());
        cv.put("nombreProfesor", nombre);
        cv.put("contraseniaHash", contraseniaHash);
        cv.put("enlacePaginaWeb", enlacePaginaWeb);
        cv.put("tokenNotificacion", tokenNotificacion);
        long resultado = db.insert("PROFESOR", null, cv);
        return resultado != -1;
    }

    public boolean existeProfesor(String correo) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        try (Cursor cursor = db.rawQuery(
                "SELECT 1 FROM PROFESOR WHERE correoProfesor = ?",
                new String[]{correo.toLowerCase()})) {
            return cursor != null && cursor.getCount() > 0;
        }
    }

    public boolean autenticarProfesor(String correo, String contraseniaHash) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        try (Cursor cursor = db.rawQuery(
                "SELECT 1 FROM PROFESOR WHERE correoProfesor = ? AND contraseniaHash = ?",
                new String[]{correo.toLowerCase(), contraseniaHash})) {
            return cursor != null && cursor.getCount() > 0;
        }
    }

    public Cursor obtenerProfesor(String correo) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        return db.rawQuery(
                "SELECT * FROM PROFESOR WHERE correoProfesor = ?",
                new String[]{correo.toLowerCase()});
    }

    public boolean actualizarTokenProfesor(String correo, String nuevoToken) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("tokenNotificacion", nuevoToken);
        int filas = db.update("PROFESOR", cv,
                "correoProfesor = ?", new String[]{correo.toLowerCase()});
        return filas > 0;
    }

    public Cursor listarProfesores() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        return db.rawQuery("SELECT * FROM PROFESOR ORDER BY nombreProfesor", null);
    }

    public Cursor buscarProfesoresPorTexto(String textoBusqueda) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String param = "%" + textoBusqueda.trim() + "%";
        String query = "SELECT DISTINCT P.correoProfesor, P.nombreProfesor, P.enlacePaginaWeb " +
                "FROM PROFESOR P " +
                "LEFT JOIN IMPARTE I ON P.correoProfesor = I.correoProfesor " +
                "LEFT JOIN ASIGNATURA A ON I.codigoAsignatura = A.codigoAsignatura " +
                "WHERE P.nombreProfesor LIKE ? OR P.correoProfesor LIKE ? OR A.nombreAsignatura LIKE ? " +
                "ORDER BY P.nombreProfesor ASC";
        return db.rawQuery(query, new String[]{param, param, param});
    }

    public Cursor obtenerProfesoresDeAsignatura(String codigoAsignatura) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String query = "SELECT DISTINCT P.* FROM PROFESOR P " +
                "JOIN IMPARTE I ON P.correoProfesor = I.correoProfesor " +
                "WHERE I.codigoAsignatura = ? " +
                "ORDER BY P.nombreProfesor ASC";
        return db.rawQuery(query, new String[]{codigoAsignatura});
    }

    public Cursor obtenerProfesoresPorGradoYCurso(int idGrado, int curso) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String query = "SELECT DISTINCT P.correoProfesor, P.nombreProfesor, P.enlacePaginaWeb, A.nombreAsignatura, A.codigoAsignatura " +
                "FROM PROFESOR P " +
                "JOIN IMPARTE I ON P.correoProfesor = I.correoProfesor " +
                "JOIN ASIGNATURA_GRADO AG ON I.codigoAsignatura = AG.codigoAsignatura " +
                "JOIN ASIGNATURA A ON AG.codigoAsignatura = A.codigoAsignatura " +
                "WHERE AG.idGrado = ? AND AG.curso = ? " +
                "ORDER BY P.nombreProfesor ASC";
        return db.rawQuery(query, new String[]{String.valueOf(idGrado), String.valueOf(curso)});
    }

    public Cursor listarSesionesDocentesDeProfesor(String correoProfesor) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String query = "SELECT S.*, A.nombreAsignatura, E.nombreEspacio " +
                "FROM SESION_CLASE S " +
                "JOIN ASIGNATURA A ON S.codigoAsignatura = A.codigoAsignatura " +
                "LEFT JOIN ESPACIO E ON S.idEspacio = E.idEspacio " +
                "WHERE S.correoProfesor = ? " +
                "ORDER BY S.diaSemana, S.horaInicio";
        return db.rawQuery(query, new String[]{correoProfesor.toLowerCase()});
    }

    // --------------------------------------------------------------
    // ADMINISTRADOR
    // --------------------------------------------------------------

    public boolean insertarAdministrador(String correo, String nombre,
                                         String contraseniaHash, String rol) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("correoAdmin", correo.toLowerCase());
        cv.put("nombreAdmin", nombre);
        cv.put("contraseniaHash", contraseniaHash);
        cv.put("rol", rol);
        long resultado = db.insert("ADMINISTRADOR", null, cv);
        return resultado != -1;
    }

    public boolean autenticarAdministrador(String correo, String contraseniaHash) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        try (Cursor cursor = db.rawQuery(
                "SELECT 1 FROM ADMINISTRADOR WHERE correoAdmin = ? AND contraseniaHash = ?",
                new String[]{correo.toLowerCase(), contraseniaHash})) {
            return cursor != null && cursor.getCount() > 0;
        }
    }

    // --------------------------------------------------------------
    // DESPACHO_PROFESOR
    // --------------------------------------------------------------

    public boolean insertarDespachoProfesor(String correoProfesor, int idEspacio) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("correoProfesor", correoProfesor.toLowerCase());
        cv.put("idEspacio", idEspacio);
        long resultado = db.insert("DESPACHO_PROFESOR", null, cv);
        return resultado != -1;
    }

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

    public long insertarGrado(String nombreGrado, String enlaceWeb) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("nombreGrado", nombreGrado);
        cv.put("enlaceWeb", enlaceWeb);
        return db.insert("GRADO", null, cv);
    }

    /** Lista todos los IDs de los grados disponibles. */
    public List<Integer> listarIdGrados() {
        List<Integer> listaGrados = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        try (Cursor cursor = db.rawQuery("SELECT idGrado FROM GRADO ORDER BY nombreGrado", null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int indexId = cursor.getColumnIndexOrThrow("idGrado");
                do {
                    listaGrados.add(cursor.getInt(indexId));
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return listaGrados;
    }

    /** Lista todos los nombres de los grados disponibles. */
    public List<String> listarGrados() {
        List<String> listaGrados = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        try (Cursor cursor = db.rawQuery("SELECT nombreGrado FROM GRADO ORDER BY nombreGrado", null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int indexNombre = cursor.getColumnIndexOrThrow("nombreGrado");
                do {
                    listaGrados.add(cursor.getString(indexNombre));
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return listaGrados;
    }

    public Cursor obtenerGrado(int idGrado) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        return db.rawQuery("SELECT * FROM GRADO WHERE idGrado = ?",
                new String[]{String.valueOf(idGrado)});
    }

    // --------------------------------------------------------------
    // FAVORITOS
    // --------------------------------------------------------------

    public boolean esFavorito(String correo, String seccion) {
        if (correo == null || correo.isEmpty()) return false;
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        try (Cursor c = db.rawQuery(
                "SELECT 1 FROM FAVORITO WHERE correoEstudiante = ? AND nombreSeccion = ?",
                new String[]{correo.toLowerCase(), seccion})) {
            return c != null && c.moveToFirst();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean marcarFavorito(String correo, String seccion, boolean favorito) {
        if (correo == null || correo.isEmpty()) return false;
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        try {
            if (favorito) {
                ContentValues cv = new ContentValues();
                cv.put("correoEstudiante", correo.toLowerCase());
                cv.put("nombreSeccion", seccion);
                long res = db.insertWithOnConflict("FAVORITO", null, cv, SQLiteDatabase.CONFLICT_IGNORE);
                return res != -1;
            } else {
                int filas = db.delete("FAVORITO", "correoEstudiante = ? AND nombreSeccion = ?",
                        new String[]{correo.toLowerCase(), seccion});
                return filas > 0;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public List<String> obtenerFavoritos(String correo) {
        List<String> lista = new ArrayList<>();
        if (correo == null || correo.isEmpty()) return lista;
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        try (Cursor c = db.rawQuery(
                "SELECT nombreSeccion FROM FAVORITO WHERE correoEstudiante = ? ORDER BY nombreSeccion",
                new String[]{correo.toLowerCase()})) {
            if (c != null && c.moveToFirst()) {
                do {
                    lista.add(c.getString(c.getColumnIndexOrThrow("nombreSeccion")));
                } while (c.moveToNext());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return lista;
    }
}

//