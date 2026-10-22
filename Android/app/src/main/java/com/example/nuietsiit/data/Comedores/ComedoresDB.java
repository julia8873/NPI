package com.example.nuietsiit.data.Comedores;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.nuietsiit.data.DBHelper;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

/**
 * Gestor de acceso a datos para el módulo de Comedores (SQLite).
 * Maneja la persistencia y consultas de comedores, horarios, tarifas y menús diarios.
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
    // GESTIÓN DE COMEDORES
    // --------------------------------------------------------------

    /**
     * Inserta un nuevo comedor en la base de datos.
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

    /**
     * Obtiene todos los comedores registrados como una lista de objetos Comedor.
     */
    public List<Comedor> obtenerListaComedores() {
        List<Comedor> lista = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        try (Cursor c = db.rawQuery("SELECT * FROM COMEDOR ORDER BY nombreComedor", null)) {
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

    // --------------------------------------------------------------
    // GESTIÓN DE HORARIOS Y TARIFAS
    // --------------------------------------------------------------

    /**
     * Inserta o actualiza el horario de apertura y cierre de un comedor para un día de la semana.
     */
    public boolean insertarHorarioComedor(int idComedor, int diaSemana,
                                          String horaApertura, String horaCierre) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("idComedor", idComedor);
        cv.put("diaSemana", diaSemana);   // 1=Lunes .. 7=Domingo
        cv.put("horaApertura", horaApertura);
        cv.put("horaCierre", horaCierre);
        long resultado = db.insertWithOnConflict("COMEDOR_HORARIO", null, cv,
                SQLiteDatabase.CONFLICT_REPLACE);
        return resultado != -1;
    }

    /**
     * Inserta o actualiza una tarifa de precio para un usuario y modalidad en un comedor.
     */
    public boolean insertarTarifa(int idComedor, String tipoUsuario,
                                  String modalidad, int precio) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("idComedor", idComedor);
        cv.put("tipoUsuario", tipoUsuario); // 'estudiante', 'personal', 'externo'
        cv.put("modalidad", modalidad);     // 'en_comedor', 'para_llevar'
        cv.put("precio", precio);           // en céntimos
        long resultado = db.insertWithOnConflict("TARIFA_COMEDOR", null, cv,
                SQLiteDatabase.CONFLICT_REPLACE);
        return resultado != -1;
    }

    // --------------------------------------------------------------
    // GESTIÓN DE MENÚS Y PLATOS
    // --------------------------------------------------------------

    /**
     * Inserta un plato disponible en la base de datos.
     */
    public long insertarPlato(String nombrePlato, String descripcion, String fotoUrl) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("nombrePlato", nombrePlato);
        cv.put("descripcion", descripcion);
        cv.put("fotoUrl", fotoUrl);
        return db.insert("PLATO", null, cv);
    }

    /**
     * Inserta un menú para un comedor en una fecha y modalidad concretas.
     */
    public long insertarMenu(int idComedor, String fecha, String modalidad) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("idComedor", idComedor);
        cv.put("fecha", fecha);       // 'YYYY-MM-DD'
        cv.put("modalidad", modalidad); // 'en_comedor'|'para_llevar'
        return db.insert("MENU", null, cv);
    }

    /**
     * Asocia un plato a un menú indicando su categoría (primero, segundo, postre).
     */
    public boolean insertarMenuPlato(int idMenu, int idPlato, String categoria) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("idMenu", idMenu);
        cv.put("idPlato", idPlato);
        cv.put("categoria", categoria); // 'primero', 'segundo', 'postre'
        long resultado = db.insert("MENU_PLATO", null, cv);
        return resultado != -1;
    }

    /**
     * Devuelve los platos de un menú ordenados por categoría (primero, segundo, postre).
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

    // --------------------------------------------------------------
    // CONSULTAS DINÁMICAS (Horarios, Precios y Menús por Fecha)
    // --------------------------------------------------------------

    /**
     * Comprueba si el comedor está cerrado en la fecha dada (fin de semana o sin horario).
     */
    public boolean esDiaCerrado(int idComedor, Calendar calendar) {
        int calDay = calendar.get(Calendar.DAY_OF_WEEK);
        if (calDay == Calendar.SATURDAY || calDay == Calendar.SUNDAY) {
            return true;
        }
        int diaSemana = calDay - Calendar.MONDAY + 1;

        SQLiteDatabase db = dbHelper.getReadableDatabase();
        try (Cursor c = db.rawQuery(
                "SELECT 1 FROM COMEDOR_HORARIO WHERE idComedor = ? AND diaSemana = ?",
                new String[]{String.valueOf(idComedor), String.valueOf(diaSemana)})) {
            return !c.moveToFirst();
        }
    }

    /**
     * Obtiene el horario de apertura y cierre para un comedor en una fecha dada.
     */
    public String obtenerHorarioParaFecha(int idComedor, Calendar calendar) {
        int calDay = calendar.get(Calendar.DAY_OF_WEEK);
        if (calDay == Calendar.SATURDAY || calDay == Calendar.SUNDAY) {
            return "Horario: Cerrado (Fin de semana)";
        }
        int diaSemana = calDay - Calendar.MONDAY + 1;

        SQLiteDatabase db = dbHelper.getReadableDatabase();
        try (Cursor c = db.rawQuery(
                "SELECT horaApertura, horaCierre FROM COMEDOR_HORARIO WHERE idComedor = ? AND diaSemana = ?",
                new String[]{String.valueOf(idComedor), String.valueOf(diaSemana)})) {
            if (c != null && c.moveToFirst()) {
                String apertura = c.getString(0);
                String cierre = c.getString(1);
                return "Horario: " + apertura + " - " + cierre;
            }
        }
        return "Horario: Cerrado / Festivo";
    }

    /**
     * Obtiene el precio formateado para estudiantes (en comedor).
     */
    public String obtenerPrecioEstudiante(int idComedor) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        try (Cursor c = db.rawQuery(
                "SELECT precio FROM TARIFA_COMEDOR WHERE idComedor = ? AND tipoUsuario = 'estudiante' AND modalidad = 'en_comedor'",
                new String[]{String.valueOf(idComedor)})) {
            if (c != null && c.moveToFirst()) {
                int precioCentimos = c.getInt(0);
                double precioEuros = precioCentimos / 100.0;
                return String.format(Locale.getDefault(), "Precio: %.2f € (estudiante)", precioEuros);
            }
        }
        return "Precio: 3,50 € (estudiante)";
    }

    /**
     * Obtiene la información del menú para un comedor y fecha específica (yyyy-MM-dd).
     * Si no existe en la base de datos, genera y registra automáticamente un menú de ejemplo.
     */
    public MenuInfo obtenerMenuParaComedorYFecha(int idComedor, String fecha) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        long idMenu = -1;
        try (Cursor c = db.rawQuery(
                "SELECT idMenu FROM MENU WHERE idComedor = ? AND fecha = ? AND modalidad = 'en_comedor'",
                new String[]{String.valueOf(idComedor), fecha})) {
            if (c != null && c.moveToFirst()) {
                idMenu = c.getInt(0);
            }
        }

        // Si no hay menú registrado para esa fecha, insertamos datos de ejemplo
        if (idMenu == -1) {
            idMenu = insertarMenu(idComedor, fecha, "en_comedor");
            if (idMenu != -1) {
                String plato1 = "Salmorejo cordobés o Sopa de picadillo";
                String plato2 = "Pechuga de pollo a la plancha con patatas";
                String plato3 = "Fruta de temporada o Yogur";

                if (idComedor == 2) {
                    plato1 = "Ensalada mixta"; plato2 = "Lomo adobado con verduras"; plato3 = "Flan casero";
                } else if (idComedor == 3) {
                    plato1 = "Crema de verduras"; plato2 = "Merluza a la romana"; plato3 = "Fruta o Helado";
                } else if (idComedor == 4) {
                    plato1 = "Pasta boloñesa"; plato2 = "Filete de ternera con patatas"; plato3 = "Yogur griego";
                }

                long p1 = insertarPlato(plato1 + " (" + fecha + ")", "Primer plato", null);
                long p2 = insertarPlato(plato2 + " (" + fecha + ")", "Segundo plato", null);
                long p3 = insertarPlato(plato3, "Postre", null);

                insertarMenuPlato((int) idMenu, (int) p1, "primero");
                insertarMenuPlato((int) idMenu, (int) p2, "segundo");
                insertarMenuPlato((int) idMenu, (int) p3, "postre");
            }
        }

        String primero = "No disponible";
        String segundo = "No disponible";
        String postre = "No disponible";

        if (idMenu != -1) {
            try (Cursor c = listarPlatosDeMenu((int) idMenu)) {
                if (c != null && c.moveToFirst()) {
                    do {
                        String nombrePlato = c.getString(c.getColumnIndexOrThrow("nombrePlato"));
                        String categoria = c.getString(c.getColumnIndexOrThrow("categoria"));
                        if ("primero".equalsIgnoreCase(categoria)) {
                            primero = nombrePlato;
                        } else if ("segundo".equalsIgnoreCase(categoria)) {
                            segundo = nombrePlato;
                        } else if ("postre".equalsIgnoreCase(categoria)) {
                            postre = nombrePlato;
                        }
                    } while (c.moveToNext());
                }
            }
        }

        return new MenuInfo(primero, segundo, postre);
    }
}
