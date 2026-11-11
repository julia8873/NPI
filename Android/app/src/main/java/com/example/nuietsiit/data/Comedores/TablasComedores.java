package com.example.nuietsiit.data.Comedores;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.time.LocalDate;

/**
 * Datos de ejemplo para los Menús de Comedores (PLATO, MENU, MENU_PLATO).
 * Genera tuplas variadas para los 4 comedores universitarios de la UGR
 * (ETSIIT, Bellas Artes, Fuentenueva y Cartuja) tanto normales como vegetarianos.
 */
public final class TablasComedores {

    private TablasComedores() {}

    public static void poblar(SQLiteDatabase db) {
        LocalDate hoy = LocalDate.now();

        // 1. COMEDOR ETSIIT (idComedor = 1)
        insertarMenuConPlatos(db, 1, hoy.toString(), "en_comedor",
                "Salmorejo cordobés con huevo duro y jamón ibérico",
                "Pechuga de pollo a la plancha con patatas a lo pobre",
                "Fruta de temporada (Manzana / Naranja)");

        insertarMenuConPlatos(db, 1, hoy.plusDays(1).toString(), "en_comedor",
                "Lentejas pardinas estilo tradicional con verduras y chorizo",
                "San Jacobo de pavo y queso con tomate provenzal",
                "Natillas caseras con galleta María");

        insertarMenuConPlatos(db, 1, hoy.plusDays(2).toString(), "en_comedor",
                "Paella mixta de pollo, magro y verduras de la huerta",
                "Cazuela de fideos con pescado y marisco",
                "Plátano de Canarias");

        insertarMenuConPlatos(db, 1, hoy.plusDays(3).toString(), "en_comedor",
                "Macarrones a la boloñesa con queso parmesano",
                "Bacalao a la vizcaína con pimientos asados",
                "Yogur natural azucarado");

        insertarMenuConPlatos(db, 1, hoy.plusDays(4).toString(), "en_comedor",
                "Crema de calabacín con picatostes crujientes",
                "Filete de ternera a la pimienta con guarnición",
                "Flan de huevo casero");

        // Menú para llevar ETSIIT
        insertarMenuConPlatos(db, 1, hoy.toString(), "para_llevar",
                "Ensalada de pasta tricolor con atún y pavo",
                "Bocadillo de pechuga empanada con alioli",
                "Fruta de temporada");

        // 2. COMEDOR BELLAS ARTES (idComedor = 2)
        insertarMenuConPlatos(db, 2, hoy.toString(), "en_comedor",
                "Ensalada campera de patata, atún y huevo cocido",
                "Lomo adobado a la plancha con verduras salteadas",
                "Flan casero con nata");

        insertarMenuConPlatos(db, 2, hoy.plusDays(1).toString(), "en_comedor",
                "Garbanzos estofados con espinacas y berenjena",
                "Albóndigas de ternera en salsa castellana",
                "Mandarina de temporada");

        insertarMenuConPlatos(db, 2, hoy.plusDays(2).toString(), "en_comedor",
                "Arroz a la cubana con salsa de tomate y huevo frito",
                "Merluza fresca al horno con limón y perejil",
                "Yogur griego con miel");

        insertarMenuConPlatos(db, 2, hoy.plusDays(3).toString(), "en_comedor",
                "Sopa de picadillo con fideos y tacos de jamón",
                "Hamburguesa completa de buey con patatas fritas",
                "Peras al vino tinto o al natural");

        insertarMenuConPlatos(db, 2, hoy.plusDays(4).toString(), "en_comedor",
                "Espaguetis carbonara con beicon ahumado",
                "Pollo asado al romero con cebollitas caramelizadas",
                "Cuajada con miel de la Alpujarra");

        // 3. COMEDOR FUENTENUEVA (CIENCIAS) (idComedor = 3)
        insertarMenuConPlatos(db, 3, hoy.toString(), "en_comedor",
                "Crema de calabaza y zanahoria con pipas tostadas",
                "Merluza a la romana con alioli y ensalada verde",
                "Tarrina de helado de vainilla y chocolate");

        insertarMenuConPlatos(db, 3, hoy.plusDays(1).toString(), "en_comedor",
                "Arroz tres delicias con verduras y tortilla",
                "Escalope de pollo empanado con patatas fritas",
                "Macedonia de frutas frescas");

        insertarMenuConPlatos(db, 3, hoy.plusDays(2).toString(), "en_comedor",
                "Gazpacho andaluz tradicional con picadillo de verdura",
                "Solomillo de cerdo al whisky con patatas duquesa",
                "Arroz con leche estilo asturiano con canela");

        insertarMenuConPlatos(db, 3, hoy.plusDays(3).toString(), "en_comedor",
                "Guiso de patatas con ternera de la sierra",
                "Calamares en su tinta con guarnición de arroz blanco",
                "Naranja de mesa pelada y picada con miel");

        insertarMenuConPlatos(db, 3, hoy.plusDays(4).toString(), "en_comedor",
                "Ensalada César con pollo crujiente y picatostes",
                "Trucha a la navarra con loncha de jamón",
                "Tarta de queso casera con mermelada de arándanos");

        // 4. COMEDOR CARTUJA (idComedor = 4)
        insertarMenuConPlatos(db, 4, hoy.toString(), "en_comedor",
                "Pasta penne con salsa boloñesa y orégano",
                "Filete de ternera a la plancha con patatas fritas",
                "Yogur griego con arándanos frescos");

        insertarMenuConPlatos(db, 4, hoy.plusDays(1).toString(), "en_comedor",
                "Pisto manchego casero con huevo escalfado",
                "Lomo de cerdo con salsa cremosa de champiñones",
                "Kiwi o fruta de temporada");

        insertarMenuConPlatos(db, 4, hoy.plusDays(2).toString(), "en_comedor",
                "Consomé de ave con fideos finos y picatostes",
                "Emperador a la plancha con aliño de ajo y perejil",
                "Mousse de chocolate artesana");

        insertarMenuConPlatos(db, 4, hoy.plusDays(3).toString(), "en_comedor",
                "Alubias blancas guisadas con calabaza y verduras",
                "Muslo de pavo asado al horno en su jugo",
                "Melón de piel de sapo");

        insertarMenuConPlatos(db, 4, hoy.plusDays(4).toString(), "en_comedor",
                "Ensalada de pasta tricolor con atún, maíz y pavo",
                "Cachopo de pollo relleno de jamón y queso",
                "Corte de helado tres gustos");
    }


    private static void insertarMenuConPlatos(SQLiteDatabase db, int idComedor, String fecha, String modalidad,
                                               String primero, String segundo, String postre) {
        ContentValues cvMenu = new ContentValues();
        cvMenu.put("idComedor", idComedor);
        cvMenu.put("fecha", fecha);
        cvMenu.put("modalidad", modalidad);
        long idMenu = db.insertWithOnConflict("MENU", null, cvMenu, SQLiteDatabase.CONFLICT_IGNORE);

        if (idMenu == -1) {
            try (Cursor c = db.rawQuery("SELECT idMenu FROM MENU WHERE idComedor = ? AND fecha = ? AND modalidad = ?",
                    new String[]{String.valueOf(idComedor), fecha, modalidad})) {
                if (c != null && c.moveToFirst()) {
                    idMenu = c.getInt(0);
                }
            }
        }

        if (idMenu != -1) {
            long p1 = insertarPlato(db, primero, "Primero");
            long p2 = insertarPlato(db, segundo, "Segundo");
            long p3 = insertarPlato(db, postre, "Postre");

            insertarRelacionMenuPlato(db, (int) idMenu, (int) p1, "primero");
            insertarRelacionMenuPlato(db, (int) idMenu, (int) p2, "segundo");
            insertarRelacionMenuPlato(db, (int) idMenu, (int) p3, "postre");
        }
    }

    private static long insertarPlato(SQLiteDatabase db, String nombre, String categoria) {
        ContentValues cv = new ContentValues();
        cv.put("nombrePlato", nombre);
        cv.put("descripcion", categoria);
        return db.insertWithOnConflict("PLATO", null, cv, SQLiteDatabase.CONFLICT_IGNORE);
    }

    private static void insertarRelacionMenuPlato(SQLiteDatabase db, int idMenu, int idPlato, String categoria) {
        ContentValues cv = new ContentValues();
        cv.put("idMenu", idMenu);
        cv.put("idPlato", idPlato);
        cv.put("categoria", categoria);
        db.insertWithOnConflict("MENU_PLATO", null, cv, SQLiteDatabase.CONFLICT_IGNORE);
    }
}
