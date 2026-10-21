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
        // 4. HORARIO DE CLASES (Para que la tabla cobre vida)
        // -------------------------------------------------
        // NPI: Lunes de 08:30 a 10:30 (Teoría) y Miércoles de 10:30 a 12:30 (Prácticas)
        sesionClase(db, "NPI", 1, "08:30", "10:30", "A", "teoria", cursoAcademico);
        sesionClase(db, "NPI", 3, "10:30", "12:30", "A", "practicas", cursoAcademico);

        // PL: Lunes de 10:30 a 12:30 (Teoría) y Jueves de 08:30 a 10:30 (Prácticas)
        sesionClase(db, "PL", 1, "10:30", "12:30", "A", "teoria", cursoAcademico);
        sesionClase(db, "PL", 4, "08:30", "10:30", "A", "practicas", cursoAcademico);
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
        ContentValues cv = new ContentValues();
        cv.put("codigoAsignatura", codigo);
        cv.put("diaSemana", diaSemana);
        cv.put("horaInicio", horaInicio);
        cv.put("horaFin", horaFin);
        cv.put("grupo", grupo);
        cv.put("tipoSesion", tipo);
        cv.put("cursoAcademico", cursoAcademico);
        db.insert("SESION_CLASE", null, cv);
    }
}