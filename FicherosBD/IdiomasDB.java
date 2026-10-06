package com.example.pruebanpi;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

/**
 * Acceso a datos del módulo de Idiomas:
 * IDIOMA, TRADUCCION
 *
 * El sistema de traducción permite internacionalizar textos de la BD
 * (ej: nombre de platos del comedor) sin cambiar el idioma de la interfaz
 * (que se gestiona desde Android Studio con recursos de strings).
 */
public class IdiomasDB {

    private final DBHelper dbHelper;

    public IdiomasDB(DBHelper dbHelper) {
        this.dbHelper = dbHelper;
    }

    // --------------------------------------------------------------
    // IDIOMA
    // --------------------------------------------------------------

    /** Inserta un idioma. Ej: ('es', 'Español'), ('en', 'English'). */
    public boolean insertarIdioma(String codigoIdioma, String nombreIdioma) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("codigoIdioma", codigoIdioma);
        cv.put("nombreIdioma", nombreIdioma);
        long resultado = db.insert("IDIOMA", null, cv);
        return resultado != -1;
    }

    /** Lista todos los idiomas disponibles. */
    public Cursor listarIdiomas() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        return db.rawQuery("SELECT * FROM IDIOMA ORDER BY nombreIdioma", null);
    }

    // --------------------------------------------------------------
    // TRADUCCION
    // --------------------------------------------------------------

    /**
     * Inserta o actualiza una traducción.
     * Ejemplo: ('PLATO', '12', 'nombrePlato', 'en', 'Grilled chicken')
     *
     * entidad:      nombre de la tabla (ej: 'PLATO', 'ALERGENO')
     * idEntidad:    valor de la PK de esa fila (TEXT porque algunas son texto)
     * campo:        nombre del campo a traducir (ej: 'nombrePlato')
     * codigoIdioma: ej: 'en', 'fr'
     * texto:        texto traducido
     */
    public boolean insertarTraduccion(String entidad, String idEntidad, String campo,
                                      String codigoIdioma, String texto) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("entidad", entidad);
        cv.put("idEntidad", idEntidad);
        cv.put("campo", campo);
        cv.put("codigoIdioma", codigoIdioma);
        cv.put("texto", texto);
        long resultado = db.insertWithOnConflict("TRADUCCION", null, cv,
                SQLiteDatabase.CONFLICT_REPLACE);
        return resultado != -1;
    }

    /**
     * Obtiene la traducción de un campo concreto de una entidad para un idioma.
     * Devuelve null (cursor vacío) si no hay traducción disponible.
     *
     * Ejemplo de uso:
     *   obtenerTraduccion("PLATO", "12", "nombrePlato", "en")
     *   → "Grilled chicken"
     */
    public Cursor obtenerTraduccion(String entidad, String idEntidad,
                                    String campo, String codigoIdioma) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        return db.rawQuery(
                "SELECT texto FROM TRADUCCION " +
                "WHERE entidad = ? AND idEntidad = ? AND campo = ? AND codigoIdioma = ?",
                new String[]{entidad, idEntidad, campo, codigoIdioma});
    }

    /**
     * Obtiene todas las traducciones de una entidad completa para un idioma.
     * Útil para cargar de golpe todas las traducciones de un plato o alérgeno.
     *
     * Ejemplo: obtenerTraducciones("PLATO", "12", "en")
     */
    public Cursor obtenerTraducciones(String entidad, String idEntidad, String codigoIdioma) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        return db.rawQuery(
                "SELECT campo, texto FROM TRADUCCION " +
                "WHERE entidad = ? AND idEntidad = ? AND codigoIdioma = ?",
                new String[]{entidad, idEntidad, codigoIdioma});
    }
}
