package com.example.nuietsiit.data;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;

import com.example.nuietsiit.util.TextoUtil;

/**
 * Tablas de Biblioteca: LIBRO, ASIGNATURA_LIBRO.
 * Las llama DBHelper desde onCreate / onUpgrade.
 */
public final class TablasBiblioteca {

    private TablasBiblioteca() {}

    // -------------------------------------------------
    //              CREAR / BORRAR
    // -------------------------------------------------
    public static void crear(SQLiteDatabase db) {
        db.execSQL(
            "CREATE TABLE LIBRO (" +
            "  idLibro               INTEGER PRIMARY KEY AUTOINCREMENT," +
            "  titulo                TEXT NOT NULL," +
            "  autores               TEXT NOT NULL," +
            "  isbn                  TEXT NOT NULL UNIQUE," +   // ISBN-13 sin guiones
            "  editorial             TEXT," +
            "  anioPublicacion       INTEGER," +
            "  edicion               TEXT," +
            "  enlaceCatalogo        TEXT," +
            "  ejemplaresTotales     INTEGER NOT NULL CHECK (ejemplaresTotales >= 0)," +
            "  ejemplaresDisponibles INTEGER NOT NULL CHECK (ejemplaresDisponibles BETWEEN 0 AND ejemplaresTotales)," +
            "  signatura             TEXT," +
            "  ubicacion             TEXT," +
            "  portada               TEXT," +                   // nombre de drawable
            "  textoBusqueda         TEXT NOT NULL" +            // título+autores+editorial normalizados
            ")"
        );

        // codigoAsignatura sin FOREIGN KEY de momento: ASIGNATURA aún no está en la app
        db.execSQL(
            "CREATE TABLE ASIGNATURA_LIBRO (" +
            "  codigoAsignatura TEXT NOT NULL," +
            "  idLibro          INTEGER NOT NULL," +
            "  tipoBibliografia TEXT NOT NULL CHECK (tipoBibliografia IN ('basica','complementaria'))," +
            "  PRIMARY KEY (codigoAsignatura, idLibro)," +
            "  FOREIGN KEY (idLibro) REFERENCES LIBRO(idLibro)" +
            ")"
        );
    }

    public static void borrar(SQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS ASIGNATURA_LIBRO");
        db.execSQL("DROP TABLE IF EXISTS LIBRO");
    }

    // -------------------------------------------------
    //              DATOS DE EJEMPLO
    // -------------------------------------------------
    // Códigos de asignatura PROVISIONALES: sustituir por los que use la tabla ASIGNATURA cuando exista
    private static final String MP = "MP";     // Metodología de la Programación
    private static final String ED = "ED";     // Estructuras de Datos
    private static final String ALG = "ALG";   // Algorítmica
    private static final String EC = "EC";     // Estructura de Computadores
    private static final String SO = "SO";     // Sistemas Operativos
    private static final String FR = "FR";     // Fundamentos de Redes
    private static final String FBD = "FBD";   // Fundamentos de Bases de Datos
    private static final String FIS = "FIS";   // Fundamentos de Ingeniería del Software
    private static final String IA = "IA";     // Inteligencia Artificial
    private static final String MC = "MC";     // Modelos de Computación
    private static final String PL = "PL";     // Procesadores de Lenguajes
    private static final String AA = "AA";     // Aprendizaje Automático
    private static final String SPSI = "SPSI"; // Seguridad y Protección de Sistemas Informáticos

    public static void poblar(SQLiteDatabase db) {
        long id;

        id = libro(db, "Introduction to Algorithms", "Thomas H. Cormen; Charles E. Leiserson; Ronald L. Rivest; Clifford Stein",
                "9780262033848", "MIT Press", 2009, "3ª ed.", 6, 2, "004.421 COR int", "Planta 1 · Estantería 12");
        asignaturaLibro(db, ALG, id, "basica");
        asignaturaLibro(db, ED, id, "complementaria");

        id = libro(db, "Algorithms", "Robert Sedgewick; Kevin Wayne",
                "9780321573513", "Addison-Wesley", 2011, "4ª ed.", 4, 0, "004.421 SED alg", "Planta 1 · Estantería 12");
        asignaturaLibro(db, ED, id, "basica");
        asignaturaLibro(db, ALG, id, "complementaria");

        id = libro(db, "The Art of Computer Programming, Vol. 1: Fundamental Algorithms", "Donald E. Knuth",
                "9780201896831", "Addison-Wesley", 1997, "3ª ed.", 2, 1, "004.421 KNU art", "Planta 1 · Estantería 12");
        asignaturaLibro(db, ALG, id, "complementaria");

        id = libro(db, "The C Programming Language", "Brian W. Kernighan; Dennis M. Ritchie",
                "9780131103627", "Prentice Hall", 1988, "2ª ed.", 5, 3, "004.438C KER cpr", "Planta 1 · Estantería 14");
        asignaturaLibro(db, MP, id, "complementaria");
        asignaturaLibro(db, SO, id, "complementaria");

        id = libro(db, "The C++ Programming Language", "Bjarne Stroustrup",
                "9780321563842", "Addison-Wesley", 2013, "4ª ed.", 5, 2, "004.438C++ STR cpp", "Planta 1 · Estantería 14");
        asignaturaLibro(db, MP, id, "basica");
        asignaturaLibro(db, ED, id, "complementaria");

        id = libro(db, "Effective Java", "Joshua Bloch",
                "9780134685991", "Addison-Wesley", 2018, "3ª ed.", 3, 3, "004.438JAV BLO eff", "Planta 1 · Estantería 14");
        asignaturaLibro(db, FIS, id, "complementaria");

        id = libro(db, "Clean Code: A Handbook of Agile Software Craftsmanship", "Robert C. Martin",
                "9780132350884", "Prentice Hall", 2008, "1ª ed.", 4, 1, "004.41 MAR cle", "Planta 1 · Estantería 15");
        asignaturaLibro(db, FIS, id, "complementaria");
        asignaturaLibro(db, MP, id, "complementaria");

        id = libro(db, "Design Patterns: Elements of Reusable Object-Oriented Software", "Erich Gamma; Richard Helm; Ralph Johnson; John Vlissides",
                "9780201633610", "Addison-Wesley", 1994, "1ª ed.", 3, 0, "004.41 GAM des", "Planta 1 · Estantería 15");
        asignaturaLibro(db, FIS, id, "complementaria");

        id = libro(db, "Software Engineering", "Ian Sommerville",
                "9780133943030", "Pearson", 2015, "10ª ed.", 6, 4, "004.41 SOM sof", "Planta 1 · Estantería 15");
        asignaturaLibro(db, FIS, id, "basica");

        id = libro(db, "Modern Operating Systems", "Andrew S. Tanenbaum; Herbert Bos",
                "9780133591620", "Pearson", 2014, "4ª ed.", 5, 2, "004.451 TAN mod", "Planta 1 · Estantería 16");
        asignaturaLibro(db, SO, id, "basica");

        id = libro(db, "Operating System Concepts", "Abraham Silberschatz; Peter B. Galvin; Greg Gagne",
                "9781118063330", "Wiley", 2012, "9ª ed.", 4, 1, "004.451 SIL ope", "Planta 1 · Estantería 16");
        asignaturaLibro(db, SO, id, "basica");

        id = libro(db, "Computer Organization and Design: The Hardware/Software Interface", "David A. Patterson; John L. Hennessy",
                "9780124077263", "Morgan Kaufmann", 2013, "5ª ed.", 5, 3, "004.2 PAT com", "Planta 1 · Estantería 17");
        asignaturaLibro(db, EC, id, "basica");

        id = libro(db, "Computer Networking: A Top-Down Approach", "James F. Kurose; Keith W. Ross",
                "9780133594140", "Pearson", 2016, "7ª ed.", 6, 0, "004.7 KUR com", "Planta 1 · Estantería 18");
        asignaturaLibro(db, FR, id, "basica");

        id = libro(db, "Cryptography and Network Security: Principles and Practice", "William Stallings",
                "9780134444284", "Pearson", 2017, "7ª ed.", 3, 2, "004.056 STA cry", "Planta 1 · Estantería 18");
        asignaturaLibro(db, SPSI, id, "basica");
        asignaturaLibro(db, FR, id, "complementaria");

        id = libro(db, "Fundamentals of Database Systems", "Ramez Elmasri; Shamkant B. Navathe",
                "9780133970777", "Pearson", 2016, "7ª ed.", 5, 2, "004.65 ELM fun", "Planta 1 · Estantería 19");
        asignaturaLibro(db, FBD, id, "basica");

        id = libro(db, "Artificial Intelligence: A Modern Approach", "Stuart Russell; Peter Norvig",
                "9780136042594", "Prentice Hall", 2010, "3ª ed.", 6, 1, "004.8 RUS art", "Planta 2 · Estantería 3");
        asignaturaLibro(db, IA, id, "basica");

        id = libro(db, "Pattern Recognition and Machine Learning", "Christopher M. Bishop",
                "9780387310732", "Springer", 2006, "1ª ed.", 3, 1, "004.85 BIS pat", "Planta 2 · Estantería 3");
        asignaturaLibro(db, AA, id, "basica");
        asignaturaLibro(db, IA, id, "complementaria");

        id = libro(db, "Deep Learning", "Ian Goodfellow; Yoshua Bengio; Aaron Courville",
                "9780262035613", "MIT Press", 2016, "1ª ed.", 3, 0, "004.85 GOO dee", "Planta 2 · Estantería 3");
        asignaturaLibro(db, AA, id, "complementaria");

        id = libro(db, "Introduction to Automata Theory, Languages, and Computation", "John E. Hopcroft; Rajeev Motwani; Jeffrey D. Ullman",
                "9780321455369", "Pearson", 2006, "3ª ed.", 4, 2, "004.4 HOP int", "Planta 2 · Estantería 5");
        asignaturaLibro(db, MC, id, "basica");
        asignaturaLibro(db, PL, id, "complementaria");

        id = libro(db, "Compilers: Principles, Techniques, and Tools", "Alfred V. Aho; Monica S. Lam; Ravi Sethi; Jeffrey D. Ullman",
                "9780321486813", "Pearson", 2006, "2ª ed.", 4, 3, "004.4 AHO com", "Planta 2 · Estantería 5");
        asignaturaLibro(db, PL, id, "basica");
    }

    // -------------------------------------------------
    //              INSERCIONES (con db de onCreate)
    // -------------------------------------------------
    private static long libro(SQLiteDatabase db, String titulo, String autores, String isbn,
                              String editorial, int anio, String edicion, int totales, int disponibles,
                              String signatura, String ubicacion) {
        ContentValues cv = new ContentValues();
        cv.put("titulo", titulo);
        cv.put("autores", autores);
        cv.put("isbn", isbn);
        cv.put("editorial", editorial);
        cv.put("anioPublicacion", anio);
        cv.put("edicion", edicion);
        // búsqueda por ISBN en Granatensis (catálogo de la Biblioteca UGR)
        cv.put("enlaceCatalogo", "https://granatensis.ugr.es/discovery/search?vid=34CBUA_UGR:VU1&query=any,contains," + isbn);
        cv.put("ejemplaresTotales", totales);
        cv.put("ejemplaresDisponibles", disponibles);
        cv.put("signatura", signatura);
        cv.put("ubicacion", "Biblioteca ETSIIT · " + ubicacion);
        cv.put("textoBusqueda", TextoUtil.textoBusqueda(titulo, autores, editorial, isbn));
        return db.insert("LIBRO", null, cv);
    }

    private static void asignaturaLibro(SQLiteDatabase db, String codigoAsignatura, long idLibro, String tipo) {
        ContentValues cv = new ContentValues();
        cv.put("codigoAsignatura", codigoAsignatura);
        cv.put("idLibro", idLibro);
        cv.put("tipoBibliografia", tipo);
        db.insert("ASIGNATURA_LIBRO", null, cv);
    }
}
