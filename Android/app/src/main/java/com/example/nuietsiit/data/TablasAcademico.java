package com.example.nuietsiit.data;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;

/**
 * Datos de ejemplo del módulo Académico.
 * Asignaturas, asignación a grados, matrículas y horarios de clase.
 * Se llama desde DBHelper en el onCreate.
 */
public final class TablasAcademico {

    private TablasAcademico() {}

    public static void poblar(SQLiteDatabase db) {
        // Usamos el mismo correo que ya se define en TablasActividades
        String correoInvitado = TablasActividades.CORREO_INVITADO;
        String cursoAcademico = "2026-27";

        // -------------------------------------------------
        // 1. ASIGNATURAS (Catálogo general)
        // -------------------------------------------------
        asignatura(db, "FP", "Fundamentos de Programación", 1, 6.0, "https://grados.ugr.es/informatica/pages/infoacademica/guias");
        asignatura(db, "NPI", "Nuevos Paradigmas de Interacción", 1, 6.0, "https://grados.ugr.es/informatica/pages/infoacademica/guias");
        asignatura(db, "PL", "Procesadores del Lenguaje", 1, 6.0, "https://grados.ugr.es/informatica/pages/infoacademica/guias");
        asignatura(db, "VC", "Visión por Computador", 1, 6.0, "https://grados.ugr.es/informatica/pages/infoacademica/guias");
        asignatura(db, "EDI", "Ecuaciones Diferenciales I", 1, 6.0, "https://grados.ugr.es/matematicas/pages/infoacademica/guias");
        asignatura(db, "AF", "Análisis de Fourier", 1, 6.0, "https://grados.ugr.es/matematicas/pages/infoacademica/guias");

        // -------------------------------------------------
        // 2. ASIGNACIÓN A GRADOS (idGrado 1 = GII, idGrado 2 = DGIIM)
        // -------------------------------------------------
        // FP: 1º curso de GII y 1º curso de DGIIM
        asignaturaGrado(db, "FP", 1, 1, "obligatoria");
        asignaturaGrado(db, "FP", 2, 1, "obligatoria");

        // EDI: 3º curso de DGIIM
        asignaturaGrado(db, "EDI", 2, 3, "obligatoria");
        // AF: 5º curso de DGIIM
        asignaturaGrado(db, "AF", 2, 5, "obligatoria");

        // NPI, PL, VC: 4º curso de GII
        asignaturaGrado(db, "NPI", 1, 4, "optativa");
        asignaturaGrado(db, "PL", 1, 4, "obligatoria");
        asignaturaGrado(db, "VC", 1, 4, "optativa");

        // -------------------------------------------------
        // 3. MATRÍCULAS DEL USUARIO INVITADO
        // -------------------------------------------------
        // Matriculamos al invitado en NPI y PL en el Grupo A
        matricula(db, correoInvitado, "NPI", cursoAcademico, "A");
        matricula(db, correoInvitado, "PL", cursoAcademico, "A");

        // -------------------------------------------------
        // 4. ESPACIOS Y EDIFICIO (Despachos y Aulas)
        // -------------------------------------------------
        db.execSQL("INSERT OR IGNORE INTO EDIFICIO (idEdificio, nombreEdificio, direccion, latitud, longitud) " +
                "VALUES (1, 'Edificio Principal ETSIIT', 'Calle Periodista Daniel Saucedo Aranda, s/n', 37.197050, -3.624260)");

        db.execSQL("INSERT OR IGNORE INTO PLANTA (idPlanta, idEdificio, numeroPlanta) VALUES (1, 1, 1)");
        db.execSQL("INSERT OR IGNORE INTO PLANTA (idPlanta, idEdificio, numeroPlanta) VALUES (2, 1, 2)");
        db.execSQL("INSERT OR IGNORE INTO PLANTA (idPlanta, idEdificio, numeroPlanta) VALUES (3, 1, 3)");

        long d215 = espacio(db, 2, "Despacho 2.15", "despacho", 1, 10.0, 20.0, "Despacho Dr. Juan Pérez Ruiz");
        long d308 = espacio(db, 3, "Despacho 3.08", "despacho", 1, 15.0, 25.0, "Despacho Dra. María García López");
        long d110 = espacio(db, 1, "Despacho 1.10", "despacho", 1, 5.0, 12.0, "Despacho Dr. Carlos Rodríguez Martín");
        long d204 = espacio(db, 2, "Despacho 2.04", "despacho", 1, 8.0, 18.0, "Despacho Dra. Ana Martínez Sánchez");

        // -------------------------------------------------
        // 5. PROFESORES
        // -------------------------------------------------
        profesor(db, "juan.perez@ugr.es", "Dr. Juan Pérez Ruiz", "pass123", "https://etsiit.ugr.es/profesores/juanperez");
        profesor(db, "maria.garcia@ugr.es", "Dra. María García López", "pass123", "https://etsiit.ugr.es/profesores/mariagarcia");
        profesor(db, "carlos.rodriguez@ugr.es", "Dr. Carlos Rodríguez Martín", "pass123", "https://etsiit.ugr.es/profesores/carlosrodriguez");
        profesor(db, "ana.martinez@ugr.es", "Dra. Ana Martínez Sánchez", "pass123", "https://etsiit.ugr.es/profesores/anamartinez");

        // -------------------------------------------------
        // 6. DESPACHOS DE PROFESORES
        // -------------------------------------------------
        despacho(db, "juan.perez@ugr.es", (int) d215);
        despacho(db, "maria.garcia@ugr.es", (int) d308);
        despacho(db, "carlos.rodriguez@ugr.es", (int) d110);
        despacho(db, "ana.martinez@ugr.es", (int) d204);

        // -------------------------------------------------
        // 7. ASIGNATURAS IMPARTIDAS (IMPARTE)
        // -------------------------------------------------
        imparte(db, "FP", "juan.perez@ugr.es", cursoAcademico);
        imparte(db, "NPI", "juan.perez@ugr.es", cursoAcademico);
        imparte(db, "PL", "maria.garcia@ugr.es", cursoAcademico);
        imparte(db, "VC", "maria.garcia@ugr.es", cursoAcademico);
        imparte(db, "EDI", "carlos.rodriguez@ugr.es", cursoAcademico);
        imparte(db, "AF", "ana.martinez@ugr.es", cursoAcademico);

        // -------------------------------------------------
        // 8. HORARIO DE CLASES
        // -------------------------------------------------
        // NPI: Lunes de 08:30 a 10:30 (Teoría) y Miércoles de 10:30 a 12:30 (Prácticas)
        sesionClase(db, "NPI", 1, "08:30", "10:30", "A", "teoria", "juan.perez@ugr.es", cursoAcademico);
        sesionClase(db, "NPI", 3, "10:30", "12:30", "A", "practicas", "juan.perez@ugr.es", cursoAcademico);

        // PL: Lunes de 10:30 a 12:30 (Teoría) y Jueves de 08:30 a 10:30 (Prácticas)
        sesionClase(db, "PL", 1, "10:30", "12:30", "A", "teoria", "maria.garcia@ugr.es", cursoAcademico);
        sesionClase(db, "PL", 4, "08:30", "10:30", "A", "practicas", "maria.garcia@ugr.es", cursoAcademico);

        // FP, VC, EDI, AF
        sesionClase(db, "FP", 2, "08:30", "10:30", "A", "teoria", "juan.perez@ugr.es", cursoAcademico);
        sesionClase(db, "VC", 2, "10:30", "12:30", "A", "teoria", "maria.garcia@ugr.es", cursoAcademico);
        sesionClase(db, "EDI", 3, "08:30", "10:30", "A", "teoria", "carlos.rodriguez@ugr.es", cursoAcademico);
        sesionClase(db, "AF", 5, "08:30", "10:30", "A", "teoria", "ana.martinez@ugr.es", cursoAcademico);

        // -------------------------------------------------
        // 9. HORARIOS DE TUTORÍA (TUTORIA_HORARIO)
        // -------------------------------------------------
        // Juan Pérez: Lunes (1) 10:30-12:30 Presencial, Miércoles (3) 16:30-18:30 Online
        tutoria(db, "juan.perez@ugr.es", 1, "10:30", "12:30", "presencial", (int) d215, null);
        tutoria(db, "juan.perez@ugr.es", 3, "16:30", "18:30", "online", null, "https://meet.google.com/abc-defg-hij");

        // María García: Martes (2) 11:30-13:30 Presencial, Jueves (4) 17:30-19:30 Mixta
        tutoria(db, "maria.garcia@ugr.es", 2, "11:30", "13:30", "presencial", (int) d308, null);
        tutoria(db, "maria.garcia@ugr.es", 4, "17:30", "19:30", "mixta", (int) d308, "https://meet.google.com/xyz-uvwx-rst");

        // Carlos Rodríguez: Miércoles (3) 09:30-11:30 Presencial
        tutoria(db, "carlos.rodriguez@ugr.es", 3, "09:30", "11:30", "presencial", (int) d110, null);

        // Ana Martínez: Viernes (5) 10:30-12:30 Online
        tutoria(db, "ana.martinez@ugr.es", 5, "10:30", "12:30", "online", null, "https://meet.google.com/ana-tutorias");
    }

    // -------------------------------------------------
    // MÉTODOS AUXILIARES DE INSERCIÓN
    // -------------------------------------------------

    private static void asignatura(SQLiteDatabase db, String codigo, String nombre, int cuatrimestre, double creditos, String enlace) {
        ContentValues cv = new ContentValues();
        cv.put("codigoAsignatura", codigo);
        cv.put("nombreAsignatura", nombre);
        cv.put("cuatrimestre", cuatrimestre);
        cv.put("creditosECTS", creditos);
        cv.put("enlaceGuiaDocente", enlace);
        db.insert("ASIGNATURA", null, cv);
    }

    private static void asignaturaGrado(SQLiteDatabase db, String codigo, int idGrado, int curso, String tipo) {
        ContentValues cv = new ContentValues();
        cv.put("codigoAsignatura", codigo);
        cv.put("idGrado", idGrado);
        cv.put("curso", curso);
        cv.put("tipo", tipo);
        db.insert("ASIGNATURA_GRADO", null, cv);
    }

    private static void matricula(SQLiteDatabase db, String correo, String codigo, String cursoAcademico, String grupo) {
        ContentValues cv = new ContentValues();
        cv.put("correoEstudiante", correo);
        cv.put("codigoAsignatura", codigo);
        cv.put("cursoAcademico", cursoAcademico);
        cv.put("grupo", grupo);
        db.insert("MATRICULA", null, cv);
    }

    /**
     * @param diaSemana 1=Lunes, 2=Martes, 3=Miércoles, 4=Jueves, 5=Viernes
     */
    private static void sesionClase(SQLiteDatabase db, String codigo, int diaSemana, String horaInicio, String horaFin, String grupo, String tipo, String cursoAcademico) {
        sesionClase(db, codigo, diaSemana, horaInicio, horaFin, grupo, tipo, null, cursoAcademico);
    }

    private static void sesionClase(SQLiteDatabase db, String codigo, int diaSemana, String horaInicio, String horaFin, String grupo, String tipo, String correoProfesor, String cursoAcademico) {
        ContentValues cv = new ContentValues();
        cv.put("codigoAsignatura", codigo);
        cv.put("diaSemana", diaSemana);
        cv.put("horaInicio", horaInicio);
        cv.put("horaFin", horaFin);
        cv.put("grupo", grupo);
        cv.put("tipoSesion", tipo);
        if (correoProfesor != null) cv.put("correoProfesor", correoProfesor.toLowerCase());
        cv.put("cursoAcademico", cursoAcademico);
        db.insert("SESION_CLASE", null, cv);
    }

    private static long espacio(SQLiteDatabase db, int idPlanta, String nombre, String tipo, int cap, double x, double y, String desc) {
        ContentValues cv = new ContentValues();
        cv.put("idPlanta", idPlanta);
        cv.put("nombreEspacio", nombre);
        cv.put("tipoEspacio", tipo);
        cv.put("capacidad", cap);
        cv.put("coordX", x);
        cv.put("coordY", y);
        cv.put("descripcion", desc);
        return db.insertWithOnConflict("ESPACIO", null, cv, SQLiteDatabase.CONFLICT_IGNORE);
    }

    private static void profesor(SQLiteDatabase db, String correo, String nombre, String passHash, String web) {
        ContentValues cv = new ContentValues();
        cv.put("correoProfesor", correo.toLowerCase());
        cv.put("nombreProfesor", nombre);
        cv.put("contraseniaHash", passHash);
        cv.put("enlacePaginaWeb", web);
        db.insertWithOnConflict("PROFESOR", null, cv, SQLiteDatabase.CONFLICT_IGNORE);
    }

    private static void despacho(SQLiteDatabase db, String correoProfesor, int idEspacio) {
        ContentValues cv = new ContentValues();
        cv.put("correoProfesor", correoProfesor.toLowerCase());
        cv.put("idEspacio", idEspacio);
        db.insertWithOnConflict("DESPACHO_PROFESOR", null, cv, SQLiteDatabase.CONFLICT_IGNORE);
    }

    private static void imparte(SQLiteDatabase db, String codigoAsignatura, String correoProfesor, String cursoAcademico) {
        ContentValues cv = new ContentValues();
        cv.put("codigoAsignatura", codigoAsignatura);
        cv.put("correoProfesor", correoProfesor.toLowerCase());
        cv.put("cursoAcademico", cursoAcademico);
        db.insertWithOnConflict("IMPARTE", null, cv, SQLiteDatabase.CONFLICT_IGNORE);
    }

    private static void tutoria(SQLiteDatabase db, String correoProfesor, int diaSemana, String horaInicio, String horaFin, String modalidad, Integer idEspacio, String enlaceOnline) {
        ContentValues cv = new ContentValues();
        cv.put("correoProfesor", correoProfesor.toLowerCase());
        cv.put("diaSemana", diaSemana);
        cv.put("horaInicio", horaInicio);
        cv.put("horaFin", horaFin);
        cv.put("modalidad", modalidad);
        if (idEspacio != null) cv.put("idEspacio", idEspacio); else cv.putNull("idEspacio");
        cv.put("enlaceOnline", enlaceOnline);
        db.insertWithOnConflict("TUTORIA_HORARIO", null, cv, SQLiteDatabase.CONFLICT_IGNORE);
    }
}