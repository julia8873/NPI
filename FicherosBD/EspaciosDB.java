package com.example.pruebanpi;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

/**
 * Acceso a datos del módulo de Espacios y Mapa:
 * EDIFICIO, PLANTA, ESPACIO, FOTO_ESPACIO,
 * NODO_NAVEGACION, ARISTA_NAVEGACION
 */
public class EspaciosDB {

    private final DBHelper dbHelper;

    public EspaciosDB(DBHelper dbHelper) {
        this.dbHelper = dbHelper;
    }

    // --------------------------------------------------------------
    // EDIFICIO
    // --------------------------------------------------------------

    // Inserta un edificio.
    // Devuelve el id generado o -1 si ha fallado.
    public long insertarEdificio(String nombreEdificio, String direccion,
                                 double latitud, double longitud) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("nombreEdificio", nombreEdificio);
        cv.put("direccion", direccion);
        cv.put("latitud", latitud);
        cv.put("longitud", longitud);
        return db.insert("EDIFICIO", null, cv);
    }

    // Lista todos los edificios ordenados por nombre.
    public Cursor listarEdificios() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        return db.rawQuery("SELECT * FROM EDIFICIO ORDER BY nombreEdificio", null);
    }

    // Obtiene un edificio por id.
    public Cursor obtenerEdificio(int idEdificio) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        return db.rawQuery("SELECT * FROM EDIFICIO WHERE idEdificio = ?",
                new String[]{String.valueOf(idEdificio)});
    }

    // --------------------------------------------------------------
    // PLANTA
    // --------------------------------------------------------------

    /**
     * Inserta una planta de un edificio.
     * altitudMetros, presionReferenciaHpa, imagenPlanoUrl, orientacionNorteGrados pueden ser null.
     */
    public long insertarPlanta(int idEdificio, int numeroPlanta,
                               Double altitudMetros, Double presionReferenciaHpa,
                               String imagenPlanoUrl, Double orientacionNorteGrados) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("idEdificio", idEdificio);
        cv.put("numeroPlanta", numeroPlanta);  // puede ser negativo (sótano)
        if (altitudMetros != null) cv.put("altitudMetros", altitudMetros);
        if (presionReferenciaHpa != null) cv.put("presionReferenciaHpa", presionReferenciaHpa);
        cv.put("imagenPlanoUrl", imagenPlanoUrl);         // puede ser null
        if (orientacionNorteGrados != null) cv.put("orientacionNorteGrados", orientacionNorteGrados);
        return db.insert("PLANTA", null, cv);
    }

    // Lista las plantas de un edificio ordenadas por número.
    public Cursor listarPlantasDe(int idEdificio) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        return db.rawQuery(
                "SELECT * FROM PLANTA WHERE idEdificio = ? ORDER BY numeroPlanta",
                new String[]{String.valueOf(idEdificio)});
    }

    // Obtiene una planta por id.
    public Cursor obtenerPlanta(int idPlanta) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        return db.rawQuery("SELECT * FROM PLANTA WHERE idPlanta = ?",
                new String[]{String.valueOf(idPlanta)});
    }

    // --------------------------------------------------------------
    // ESPACIO
    // --------------------------------------------------------------

    /**
     * Inserta un espacio (aula, despacho, aseo, etc.).
     * capacidad y descripcion pueden ser null.
     */
    public long insertarEspacio(int idPlanta, String nombreEspacio, String tipoEspacio,
                                Integer capacidad, double coordX, double coordY,
                                String descripcion) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("idPlanta", idPlanta);
        cv.put("nombreEspacio", nombreEspacio);
        cv.put("tipoEspacio", tipoEspacio);  // 'aula'|'despacho'|'secretaria'|...
        if (capacidad != null) cv.put("capacidad", capacidad); else cv.putNull("capacidad");
        cv.put("coordX", coordX);
        cv.put("coordY", coordY);
        cv.put("descripcion", descripcion);  // puede ser null
        return db.insert("ESPACIO", null, cv);
    }

    // Obtiene un espacio por id.
    public Cursor obtenerEspacio(int idEspacio) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        return db.rawQuery("SELECT * FROM ESPACIO WHERE idEspacio = ?",
                new String[]{String.valueOf(idEspacio)});
    }

    // Lista todos los espacios de una planta ordenados por nombre.
    public Cursor listarEspaciosDe(int idPlanta) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        return db.rawQuery(
                "SELECT * FROM ESPACIO WHERE idPlanta = ? ORDER BY nombreEspacio",
                new String[]{String.valueOf(idPlanta)});
    }

    // Lista los espacios de un tipo concreto (ej: 'aula', 'despacho').
    public Cursor listarEspaciosPorTipo(String tipoEspacio) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        return db.rawQuery(
                "SELECT E.*, P.numeroPlanta, P.idEdificio FROM ESPACIO E " +
                "JOIN PLANTA P ON E.idPlanta = P.idPlanta " +
                "WHERE E.tipoEspacio = ? ORDER BY P.idEdificio, P.numeroPlanta, E.nombreEspacio",
                new String[]{tipoEspacio});
    }

    // --------------------------------------------------------------
    // FOTO_ESPACIO
    // --------------------------------------------------------------

    // Añade una foto a un espacio.
    // Devuelve true si se ha insertado correctamente.
    public boolean insertarFotoEspacio(int idEspacio, String url) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("idEspacio", idEspacio);
        cv.put("url", url);
        long resultado = db.insert("FOTO_ESPACIO", null, cv);
        return resultado != -1;
    }

    // Lista las fotos de un espacio.
    public Cursor listarFotosDe(int idEspacio) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        return db.rawQuery("SELECT * FROM FOTO_ESPACIO WHERE idEspacio = ?",
                new String[]{String.valueOf(idEspacio)});
    }

    // --------------------------------------------------------------
    // NODO_NAVEGACION
    // --------------------------------------------------------------

    // Inserta un nodo del grafo de rutas.
    // idEspacio puede ser null si es un nodo de pasillo sin espacio asociado.
    // Devuelve el id generado o -1 si ha fallado.
    public long insertarNodo(int idPlanta, double coordX, double coordY, Integer idEspacio) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("idPlanta", idPlanta);
        cv.put("coordX", coordX);
        cv.put("coordY", coordY);
        if (idEspacio != null) cv.put("idEspacio", idEspacio); else cv.putNull("idEspacio");
        return db.insert("NODO_NAVEGACION", null, cv);
    }

    // Lista todos los nodos de una planta.
    public Cursor listarNodosDe(int idPlanta) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        return db.rawQuery("SELECT * FROM NODO_NAVEGACION WHERE idPlanta = ?",
                new String[]{String.valueOf(idPlanta)});
    }

    // Obtiene el nodo asociado a un espacio concreto.
    // Útil para calcular la ruta hasta un aula o despacho.
    public Cursor obtenerNodoDeEspacio(int idEspacio) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        return db.rawQuery("SELECT * FROM NODO_NAVEGACION WHERE idEspacio = ?",
                new String[]{String.valueOf(idEspacio)});
    }

    // --------------------------------------------------------------
    // ARISTA_NAVEGACION
    // --------------------------------------------------------------

    /**
     * Inserta una arista (tramo de ruta) entre dos nodos.
     * accesible: true si es accesible para sillas de ruedas.
     */
    public boolean insertarArista(int idNodoOrigen, int idNodoDestino,
                                  double distanciaMetros, String tipoTramo,
                                  boolean accesible) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("idNodoOrigen", idNodoOrigen);
        cv.put("idNodoDestino", idNodoDestino);
        cv.put("distanciaMetros", distanciaMetros);
        cv.put("tipoTramo", tipoTramo);  // 'pasillo'|'escalera'|'ascensor'
        cv.put("accesible", accesible ? 1 : 0);
        long resultado = db.insert("ARISTA_NAVEGACION", null, cv);
        return resultado != -1;
    }

    // Devuelve todas las aristas que parten de un nodo.
    public Cursor listarAristasDe(int idNodoOrigen) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        return db.rawQuery(
                "SELECT * FROM ARISTA_NAVEGACION WHERE idNodoOrigen = ?",
                new String[]{String.valueOf(idNodoOrigen)});
    }

    // Devuelve todas las aristas de una planta para cargar el grafo completo.
    // Si soloAccesible es true, solo incluye los tramos accesibles.
    public Cursor listarAristasPlanta(int idPlanta, boolean soloAccesible) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String filtroAccesible = soloAccesible ? " AND AR.accesible = 1" : "";
        return db.rawQuery(
                "SELECT AR.* FROM ARISTA_NAVEGACION AR " +
                "JOIN NODO_NAVEGACION N ON AR.idNodoOrigen = N.idNodo " +
                "WHERE N.idPlanta = ?" + filtroAccesible,
                new String[]{String.valueOf(idPlanta)});
    }
}
