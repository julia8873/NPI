package com.example.nuietsiit.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.nuietsiit.data.modelo.Comedor;

import java.util.ArrayList;
import java.util.List;

/**
 * Acceso a datos del módulo de Comedores:
 * COMEDOR, COMEDOR_HORARIO, TARIFA_COMEDOR,
 * PLATO, MENU, MENU_PLATO
 */
public class ComedoresDB {

    private final DBHelper dbHelper;

    public ComedoresDB(DBHelper dbHelper) {
        this.dbHelper = dbHelper;
    }

    public ComedoresDB(Context context) {
        this(DBHelper.getInstance(context));
    }

    // --------------------------------------------------------------
    // COMEDOR
    // --------------------------------------------------------------

    /**
     * Inserta un comedor.
     * idEdificio puede ser null si el comedor no está en un edificio catalogado.
     */
    public long insertarComedor(String nombreComedor, Integer idEdificio,
                                String direccion, double latitud, double longitud) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("nombreComedor", nombreComedor);
        if (idEdificio != null) cv.put("idEdificio", idEdificio); else cv.putNull("idEdificio");
        cv.put("direccion", direccion);
        cv.put("latitud", latitud);
        cv.put("longitud", longitud);
        return db.insert("COMEDOR", null, cv);
    }

    // Lista todos los comedores ordenados por nombre.
    public Cursor listarComedores() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        return db.rawQuery("SELECT * FROM COMEDOR ORDER BY nombreComedor", null);
    }

    // Devuelve todos los comedores como una lista de objetos Comedor
    public List<Comedor> obtenerListaComedores() {
        List<Comedor> lista = new ArrayList<>();
        try (Cursor c = listarComedores()) {
            if (c != null && c.moveToFirst()) {
                do {
                    int id = c.getInt(c.getColumnIndexOrThrow("idComedor"));
                    String nombre = c.getString(c.getColumnIndexOrThrow("nombreComedor"));
                    String direccion = c.getString(c.getColumnIndexOrThrow("direccion"));
                    double lat = c.getDouble(c.getColumnIndexOrThrow("latitud"));
                    double lon = c.getDouble(c.getColumnIndexOrThrow("longitud"));
                    Integer idEdificio = c.isNull(c.getColumnIndexOrThrow("idEdificio")) ? null : c.getInt(c.getColumnIndexOrThrow("idEdificio"));
                    lista.add(new Comedor(id, nombre, idEdificio, direccion, lat, lon));
                } while (c.moveToNext());
            }
        }
        return lista;
    }

    // Obtiene un comedor por id.
    public Cursor obtenerComedor(int idComedor) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        return db.rawQuery("SELECT * FROM COMEDOR WHERE idComedor = ?",
                new String[]{String.valueOf(idComedor)});
    }

    // --------------------------------------------------------------
    // COMEDOR_HORARIO
    // --------------------------------------------------------------

    // Inserta o actualiza el horario de un comedor para un día.
    // Devuelve true si se ha insertado o actualizado correctamente.
    public boolean insertarHorarioComedor(int idComedor, int diaSemana,
                                          String horaApertura, String horaCierre) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("idComedor", idComedor);
        cv.put("diaSemana", diaSemana);   // 1=lunes..7=domingo
        cv.put("horaApertura", horaApertura); // 'HH:MM'
        cv.put("horaCierre", horaCierre);
        long resultado = db.insertWithOnConflict("COMEDOR_HORARIO", null, cv,
                SQLiteDatabase.CONFLICT_REPLACE);
        return resultado != -1;
    }

    // Devuelve los horarios de un comedor ordenados por día.
    public Cursor listarHorariosDe(int idComedor) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        return db.rawQuery(
                "SELECT * FROM COMEDOR_HORARIO WHERE idComedor = ? ORDER BY diaSemana",
                new String[]{String.valueOf(idComedor)});
    }

    // --------------------------------------------------------------
    // TARIFA_COMEDOR
    // --------------------------------------------------------------

    // Inserta o actualiza una tarifa.
    // precio en céntimos (ej: 350 = 3,50 euros).
    // Devuelve true si se ha insertado o actualizado correctamente.
    public boolean insertarTarifa(int idComedor, String tipoUsuario,
                                  String modalidad, int precio) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("idComedor", idComedor);
        cv.put("tipoUsuario", tipoUsuario); // 'estudiante'|'personal'|'externo'
        cv.put("modalidad", modalidad);     // 'en_comedor'|'para_llevar'
        cv.put("precio", precio);
        long resultado = db.insertWithOnConflict("TARIFA_COMEDOR", null, cv,
                SQLiteDatabase.CONFLICT_REPLACE);
        return resultado != -1;
    }

    // Devuelve todas las tarifas de un comedor.
    public Cursor listarTarifasDe(int idComedor) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        return db.rawQuery(
                "SELECT * FROM TARIFA_COMEDOR WHERE idComedor = ?",
                new String[]{String.valueOf(idComedor)});
    }

    // --------------------------------------------------------------
    // PLATO
    // --------------------------------------------------------------

    /**
     * Inserta un plato.
     * descripcion y fotoUrl pueden ser null.
     */
    public long insertarPlato(String nombrePlato, String descripcion, String fotoUrl) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("nombrePlato", nombrePlato);
        cv.put("descripcion", descripcion); // puede ser null
        cv.put("fotoUrl", fotoUrl);         // puede ser null
        return db.insert("PLATO", null, cv);
    }

    // Obtiene un plato por id.
    public Cursor obtenerPlato(int idPlato) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        return db.rawQuery("SELECT * FROM PLATO WHERE idPlato = ?",
                new String[]{String.valueOf(idPlato)});
    }

    // --------------------------------------------------------------
    // MENU
    // --------------------------------------------------------------

    /**
     * Inserta un menú para un comedor, fecha y modalidad.
     * Restricción UNIQUE (idComedor, fecha, modalidad) garantizada por la BD.
     * Devuelve el idMenu generado o -1 si ya existe.
     */
    public long insertarMenu(int idComedor, String fecha, String modalidad) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("idComedor", idComedor);
        cv.put("fecha", fecha);       // 'YYYY-MM-DD'
        cv.put("modalidad", modalidad); // 'en_comedor'|'para_llevar'
        return db.insert("MENU", null, cv);
    }

    // Obtiene el menú de un comedor para una fecha y modalidad concretas.
    public Cursor obtenerMenu(int idComedor, String fecha, String modalidad) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        return db.rawQuery(
                "SELECT * FROM MENU WHERE idComedor = ? AND fecha = ? AND modalidad = ?",
                new String[]{String.valueOf(idComedor), fecha, modalidad});
    }

    // --------------------------------------------------------------
    // MENU_PLATO
    // --------------------------------------------------------------

    // Añade un plato a un menú con su categoría
    public boolean insertarMenuPlato(int idMenu, int idPlato, String categoria) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("idMenu", idMenu);
        cv.put("idPlato", idPlato);
        cv.put("categoria", categoria); // 'primero'|'segundo'|'postre'
        long resultado = db.insert("MENU_PLATO", null, cv);
        return resultado != -1;
    }

    /**
     * Devuelve los platos de un menú agrupados por su categoría (primero, segundo, postre).
     * Útil para mostrar el menú del día completo.
     */
    public Cursor listarPlatosDeMenu(int idMenu) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        return db.rawQuery(
                "SELECT P.*, MP.categoria FROM PLATO P " +
                "JOIN MENU_PLATO MP ON P.idPlato = MP.idPlato " +
                "WHERE MP.idMenu = ? " +
                "ORDER BY CASE MP.categoria " +
                "  WHEN 'primero' THEN 1 " +
                "  WHEN 'segundo' THEN 2 " +
                "  WHEN 'postre'  THEN 3 " +
                "  ELSE 4 END",
                new String[]{String.valueOf(idMenu)});
    }
}
