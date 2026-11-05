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

        // Poblar fechas específicas de simulación (del martes 3 al viernes 6 de noviembre)
        poblarDiasSimulacion(db);
    }

    /**
     * Inserta menús guardados para la simulación desde el martes 3 de noviembre hasta el viernes 6 de noviembre,
     * incluyendo tanto la opción normal como la vegetariana.
     */
    public static void poblarDiasSimulacion(SQLiteDatabase db) {
        // =========================================================
        // Martes 3 de noviembre de 2026
        // =========================================================
        // Menús normales
        insertarMenuConPlatos(db, 1, "2026-11-03", "en_comedor",
                "Salmorejo cordobés con huevo duro y jamón ibérico",
                "Pechuga de pollo a la plancha con patatas a lo pobre",
                "Fruta de temporada (Manzana / Naranja)");
        insertarMenuConPlatos(db, 2, "2026-11-03", "en_comedor",
                "Ensalada campera de patata, atún y huevo cocido",
                "Lomo adobado a la plancha con verduras salteadas",
                "Flan casero con nata");
        insertarMenuConPlatos(db, 3, "2026-11-03", "en_comedor",
                "Crema de calabaza y zanahoria con pipas tostadas",
                "Merluza a la romana con alioli y ensalada verde",
                "Tarrina de helado de vainilla y chocolate");
        insertarMenuConPlatos(db, 4, "2026-11-03", "en_comedor",
                "Pasta penne con salsa boloñesa y orégano",
                "Filete de ternera a la plancha con patatas fritas",
                "Yogur griego con arándanos frescos");

        // Menús vegetarianos
        insertarMenuConPlatos(db, 1, "2026-11-03", "vegetariano",
                "Salmorejo cordobés vegano con tofu y picatostes",
                "Hamburguesa de lentejas y quinoa con verduras asadas",
                "Fruta de temporada (Manzana / Pera)");
        insertarMenuConPlatos(db, 2, "2026-11-03", "vegetariano",
                "Ensalada campera vegetal con patata, maíz y aguacate",
                "Tofu marinado a la plancha con verduras salteadas",
                "Macedonia de frutas naturales");
        insertarMenuConPlatos(db, 3, "2026-11-03", "vegetariano",
                "Crema de calabaza y zanahoria con pipas tostadas",
                "Berenjenas rellenas de soja texturizada y verduras",
                "Fruta fresca de temporada");
        insertarMenuConPlatos(db, 4, "2026-11-03", "vegetariano",
                "Pasta penne con boloñesa de lentejas y albahaca",
                "Filete de seitán a la plancha con patatas asadas",
                "Yogur de soja con arándanos");

        // =========================================================
        // Miércoles 4 de noviembre de 2026
        // =========================================================
        // Menús normales
        insertarMenuConPlatos(db, 1, "2026-11-04", "en_comedor",
                "Lentejas pardinas estilo tradicional con verduras y chorizo",
                "San Jacobo de pavo y queso con tomate provenzal",
                "Natillas caseras con galleta María");
        insertarMenuConPlatos(db, 2, "2026-11-04", "en_comedor",
                "Garbanzos estofados con espinacas y berenjena",
                "Albóndigas de ternera en salsa castellana",
                "Mandarina de temporada");
        insertarMenuConPlatos(db, 3, "2026-11-04", "en_comedor",
                "Arroz tres delicias con verduras y tortilla",
                "Escalope de pollo empanado con patatas fritas",
                "Macedonia de frutas frescas");
        insertarMenuConPlatos(db, 4, "2026-11-04", "en_comedor",
                "Pisto manchego casero con huevo escalfado",
                "Lomo de cerdo con salsa cremosa de champiñones",
                "Kiwi o fruta de temporada");

        // Menús vegetarianos
        insertarMenuConPlatos(db, 1, "2026-11-04", "vegetariano",
                "Lentejas pardinas con verduras de la huerta (100% vegetal)",
                "Tortilla vegana de patatas y cebolla con ensalada",
                "Compota de manzana casera con canela");
        insertarMenuConPlatos(db, 2, "2026-11-04", "vegetariano",
                "Garbanzos estofados con espinacas y pimientos",
                "Albóndigas de espinacas y garbanzos con salsa de tomate",
                "Mandarina de temporada");
        insertarMenuConPlatos(db, 3, "2026-11-04", "vegetariano",
                "Arroz con verduras variadas y setas salteadas",
                "Escalope de seitán empanado con patatas fritas",
                "Rodajas de piña al natural");
        insertarMenuConPlatos(db, 4, "2026-11-04", "vegetariano",
                "Pisto manchego tradicional con garbanzos tostados",
                "Medallones de calabacín y queso vegano al horno",
                "Kiwi de temporada");

        // =========================================================
        // Jueves 5 de noviembre de 2026
        // =========================================================
        // Menús normales
        insertarMenuConPlatos(db, 1, "2026-11-05", "en_comedor",
                "Paella mixta de pollo, magro y verduras de la huerta",
                "Cazuela de fideos con pescado y marisco",
                "Plátano de Canarias");
        insertarMenuConPlatos(db, 2, "2026-11-05", "en_comedor",
                "Arroz a la cubana con salsa de tomate y huevo frito",
                "Merluza fresca al horno con limón y perejil",
                "Yogur griego con miel");
        insertarMenuConPlatos(db, 3, "2026-11-05", "en_comedor",
                "Gazpacho andaluz tradicional con picadillo de verdura",
                "Solomillo de cerdo al whisky con patatas duquesa",
                "Arroz con leche estilo asturiano con canela");
        insertarMenuConPlatos(db, 4, "2026-11-05", "en_comedor",
                "Consomé de ave con fideos finos y picatostes",
                "Emperador a la plancha con aliño de ajo y perejil",
                "Mousse de chocolate artesana");

        // Menús vegetarianos
        insertarMenuConPlatos(db, 1, "2026-11-05", "vegetariano",
                "Paella vegetal con alcachofas, judías y pimientos",
                "Cazuela de fideos con setas silvestres y espárragos",
                "Plátano de Canarias");
        insertarMenuConPlatos(db, 2, "2026-11-05", "vegetariano",
                "Arroz con tomate casero, pisto y plátano frito",
                "Filete de tofu a las finas hierbas con ensalada verde",
                "Naranja con canela");
        insertarMenuConPlatos(db, 3, "2026-11-05", "vegetariano",
                "Gazpacho andaluz tradicional con picadillo vegetal",
                "Falafel casero de garbanzos con salsa tahini y patatas",
                "Yogur vegetal de vainilla");
        insertarMenuConPlatos(db, 4, "2026-11-05", "vegetariano",
                "Crema de verduras y puerros con picatostes",
                "Hamburguesa de garbanzos y espinacas con ensalada",
                "Fruta variada de temporada");

        // =========================================================
        // Viernes 6 de noviembre de 2026
        // =========================================================
        // Menús normales
        insertarMenuConPlatos(db, 1, "2026-11-06", "en_comedor",
                "Macarrones a la boloñesa con queso parmesano",
                "Bacalao a la vizcaína con pimientos asados",
                "Yogur natural azucarado");
        insertarMenuConPlatos(db, 2, "2026-11-06", "en_comedor",
                "Sopa de picadillo con fideos y tacos de jamón",
                "Hamburguesa completa de buey con patatas fritas",
                "Peras al vino tinto o al natural");
        insertarMenuConPlatos(db, 3, "2026-11-06", "en_comedor",
                "Guiso de patatas con ternera de la sierra",
                "Calamares en su tinta con guarnición de arroz blanco",
                "Naranja de mesa pelada y picada con miel");
        insertarMenuConPlatos(db, 4, "2026-11-06", "en_comedor",
                "Alubias blancas guisadas con calabaza y verduras",
                "Muslo de pavo asado al horno en su jugo",
                "Melón de piel de sapo");

        // Menús vegetarianos
        insertarMenuConPlatos(db, 1, "2026-11-06", "vegetariano",
                "Macarrones con pisto de verduras y orégano",
                "Croquetas veganas de boletus con ensalada de tomate",
                "Manzana asada con canela");
        insertarMenuConPlatos(db, 2, "2026-11-06", "vegetariano",
                "Sopa de verduras con fideos finos y garbanzos",
                "Hamburguesa de remolacha y avena con patatas rústicas",
                "Peras al natural");
        insertarMenuConPlatos(db, 3, "2026-11-06", "vegetariano",
                "Guiso marinero vegetal de patatas con algas y alcachofas",
                "Nuggets de tofu crujientes con salsa barbacoa",
                "Naranja de mesa pelada con nueces");
        insertarMenuConPlatos(db, 4, "2026-11-06", "vegetariano",
                "Alubias blancas estofadas con calabaza y zanahorias",
                "Salteado de seitán con champiñones y pimientos",
                "Melón de temporada");
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
