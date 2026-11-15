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
        cv.put("modalidad", modalidad); // 'en_comedor'|'para_llevar'|'vegetariano'
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
     * Asegura que los menús guardados de la simulación (del martes 3 al viernes 6 de noviembre)
     * estén presentes en la base de datos SQLite.
     */
    public void asegurarMenusSimulacion() {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        TablasComedores.poblar(db);
    }

    /**
     * Obtiene la información del menú tradicional (en_comedor).
     */
    public MenuInfo obtenerMenuParaComedorYFecha(int idComedor, String fecha) {
        return obtenerMenuParaComedorYFecha(idComedor, fecha, false);
    }

    /**
     * Obtiene la información del menú para un comedor y fecha específica (yyyy-MM-dd),
     * permitiendo seleccionar entre menú tradicional o vegetariano.
     */
    public MenuInfo obtenerMenuParaComedorYFecha(int idComedor, String fecha, boolean esVegetariano) {
        String modalidad = esVegetariano ? "vegetariano" : "en_comedor";
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        long idMenu = -1;
        try (Cursor c = db.rawQuery(
                "SELECT idMenu FROM MENU WHERE idComedor = ? AND fecha = ? AND modalidad = ?",
                new String[]{String.valueOf(idComedor), fecha, modalidad})) {
            if (c != null && c.moveToFirst()) {
                idMenu = c.getInt(0);
            }
        } catch (Exception e) {
            e.printStackTrace();
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
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // Si la base de datos no contiene los platos o están incompletos, recurrir al catálogo simulado
        if ("No disponible".equals(primero) || "No disponible".equals(segundo)) {
            return generarMenuSimulado(idComedor, fecha, esVegetariano);
        }

        return new MenuInfo(primero, segundo, postre);
    }

    /**
     * Genera datos de menú variados y realistas (tanto vegetarianos como normales) para evitar que la vista quede vacía.
     */
    public MenuInfo generarMenuSimulado(int idComedor, String fecha, boolean esVegetariano) {
        if (esVegetariano) {
            if (fecha != null && fecha.endsWith("11-03")) {
                switch (idComedor) {
                    case 2:
                        return new MenuInfo("Ensalada campera vegetal con patata, maíz y aguacate",
                                "Tofu marinado a la plancha con verduras salteadas",
                                "Macedonia de frutas naturales");
                    case 3:
                        return new MenuInfo("Crema de calabaza y zanahoria con pipas tostadas",
                                "Berenjenas rellenas de soja texturizada y verduras",
                                "Fruta fresca de temporada");
                    case 4:
                        return new MenuInfo("Pasta penne con boloñesa de lentejas y albahaca",
                                "Filete de seitán a la plancha con patatas asadas",
                                "Yogur de soja con arándanos");
                    default:
                        return new MenuInfo("Salmorejo cordobés vegano con tofu y picatostes",
                                "Hamburguesa de lentejas y quinoa con verduras asadas",
                                "Fruta de temporada (Manzana / Pera)");
                }
            } else if (fecha != null && fecha.endsWith("11-04")) {
                switch (idComedor) {
                    case 2:
                        return new MenuInfo("Garbanzos estofados con espinacas y pimientos",
                                "Albóndigas de espinacas y garbanzos con salsa de tomate",
                                "Mandarina de temporada");
                    case 3:
                        return new MenuInfo("Arroz con verduras variadas y setas salteadas",
                                "Escalope de seitán empanado con patatas fritas",
                                "Rodajas de piña al natural");
                    case 4:
                        return new MenuInfo("Pisto manchego tradicional con garbanzos tostados",
                                "Medallones de calabacín y queso vegano al horno",
                                "Kiwi de temporada");
                    default:
                        return new MenuInfo("Lentejas pardinas con verduras de la huerta (100% vegetal)",
                                "Tortilla vegana de patatas y cebolla con ensalada",
                                "Compota de manzana casera con canela");
                }
            } else if (fecha != null && fecha.endsWith("11-05")) {
                switch (idComedor) {
                    case 2:
                        return new MenuInfo("Arroz con tomate casero, pisto y plátano frito",
                                "Filete de tofu a las finas hierbas con ensalada verde",
                                "Naranja con canela");
                    case 3:
                        return new MenuInfo("Gazpacho andaluz tradicional con picadillo vegetal",
                                "Falafel casero de garbanzos con salsa tahini y patatas",
                                "Yogur vegetal de vainilla");
                    case 4:
                        return new MenuInfo("Crema de verduras y puerros con picatostes",
                                "Hamburguesa de garbanzos y espinacas con ensalada",
                                "Fruta variada de temporada");
                    default:
                        return new MenuInfo("Paella vegetal con alcachofas, judías y pimientos",
                                "Cazuela de fideos con setas silvestres y espárragos",
                                "Plátano de Canarias");
                }
            } else if (fecha != null && fecha.endsWith("11-06")) {
                switch (idComedor) {
                    case 2:
                        return new MenuInfo("Sopa de verduras con fideos finos y garbanzos",
                                "Hamburguesa de remolacha y avena con patatas rústicas",
                                "Peras al natural");
                    case 3:
                        return new MenuInfo("Guiso marinero vegetal de patatas con algas y alcachofas",
                                "Nuggets de tofu crujientes con salsa barbacoa",
                                "Naranja de mesa pelada con nueces");
                    case 4:
                        return new MenuInfo("Alubias blancas estofadas con calabaza y zanahorias",
                                "Salteado de seitán con champiñones y pimientos",
                                "Melón de temporada");
                    default:
                        return new MenuInfo("Macarrones con pisto de verduras y orégano",
                                "Croquetas veganas de boletus con ensalada de tomate",
                                "Manzana asada con canela");
                }
            } else {
                return new MenuInfo("Crema de calabacín y puerros con picatostes",
                        "Hamburguesa de lentejas y quinoa con verduras asadas",
                        "Fruta fresca de temporada");
            }
        } else {
            if (fecha != null && fecha.endsWith("11-03")) {
                return new MenuInfo("Salmorejo cordobés con huevo duro y jamón ibérico",
                        "Pechuga de pollo a la plancha con patatas a lo pobre",
                        "Fruta de temporada (Manzana / Naranja)");
            } else if (fecha != null && fecha.endsWith("11-04")) {
                return new MenuInfo("Lentejas pardinas con verduras y chorizo",
                        "San Jacobo de pavo y queso con patatas fritas",
                        "Natillas caseras con galleta");
            } else if (fecha != null && fecha.endsWith("11-05")) {
                return new MenuInfo("Paella mixta de pollo, magro y verduras",
                        "Merluza al horno con limón y perejil",
                        "Plátano de Canarias");
            } else if (fecha != null && fecha.endsWith("11-06")) {
                return new MenuInfo("Macarrones a la boloñesa con queso gratinado",
                        "Bacalao a la vizcaína con pimientos asados",
                        "Yogur natural azucarado");
            } else {
                return new MenuInfo("Primer plato tradicional del día",
                        "Segundo plato tradicional del día",
                        "Postre casero");
            }
        }
    }
}
