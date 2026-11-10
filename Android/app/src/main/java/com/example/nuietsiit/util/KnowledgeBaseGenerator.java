package com.example.nuietsiit.util;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.nuietsiit.data.DBHelper;
import com.example.nuietsiit.data.Comedores.TablasComedores;
import com.example.nuietsiit.data.TablasAcademico;
import com.example.nuietsiit.data.TablasActividades;
import com.example.nuietsiit.data.TablasBiblioteca;
import com.example.nuietsiit.data.TablasSecretaria;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Genera un archivo de texto plano (etsiit_knowledge_base.txt) resumido y optimizado con todos los datos institucionales
 * de la base de datos para servir de base de conocimiento robusta al Asistente IA de la ETSIIT.
 */
public final class KnowledgeBaseGenerator {

    private KnowledgeBaseGenerator() {}

    public static String generarYObtenerContenido(Context context) {
        StringBuilder sb = new StringBuilder();
        sb.append("=== BASE DE CONOCIMIENTO OFICIAL DE LA ETSIIT - UGR ===\n\n");

        try {
            DBHelper dbHelper = DBHelper.getInstance(context);
            SQLiteDatabase db = dbHelper.getWritableDatabase();

            TablasComedores.poblar(db);
            TablasActividades.poblar(db);
            TablasSecretaria.poblar(db);
            TablasBiblioteca.poblar(db);
            TablasAcademico.poblar(db);

            // 1. Asignaturas y Profesores
            sb.append("[ASIGNATURAS Y PROFESORES]\n");
            try (Cursor c = db.rawQuery("SELECT A.codigoAsignatura, A.nombreAsignatura, A.cuatrimestre, A.creditos, P.nombreProfesor FROM ASIGNATURA A LEFT JOIN IMPARTE I ON A.codigoAsignatura = I.codigoAsignatura LEFT JOIN PROFESOR P ON I.correoProfesor = P.correoProfesor", null)) {
                while (c != null && c.moveToNext()) {
                    sb.append("- Asignatura [").append(c.getString(0)).append("]: ").append(c.getString(1))
                      .append(" (Cuatrimestre: ").append(c.getInt(2)).append("º, ECTS: ").append(c.getDouble(3))
                      .append(", Profesor: ").append(c.getString(4) != null ? c.getString(4) : "Varios").append(")\n");
                }
            }

            // 2. Actividades
            sb.append("\n[ACTIVIDADES Y CURSOS ABIERTAUGR]\n");
            try (Cursor c = db.rawQuery("SELECT titulo, tipoActividad, fechaInicio, lugar FROM ACTIVIDAD", null)) {
                while (c != null && c.moveToNext()) {
                    sb.append("- Actividad: ").append(c.getString(0))
                      .append(" [Tipo: ").append(c.getString(1))
                      .append(", Fecha: ").append(c.getString(2))
                      .append(", Lugar: ").append(c.getString(3)).append("]\n");
                }
            }

            // 3. Comedores
            sb.append("\n[COMEDORES UNIVERSITARIOS UGR]\n");
            try (Cursor c = db.rawQuery("SELECT DISTINCT nombreComedor, ubicacion FROM COMEDOR", null)) {
                while (c != null && c.moveToNext()) {
                    sb.append("- ").append(c.getString(0)).append(" (Ubicación: ").append(c.getString(1)).append(")\n");
                }
            }

            // 4. Secretaría y Biblioteca
            sb.append("\n[TRÁMITES Y BIBLIOTECA]\n");
            try (Cursor c = db.rawQuery("SELECT nombre FROM TRAMITE", null)) {
                while (c != null && c.moveToNext()) {
                    sb.append("- Trámite: ").append(c.getString(0)).append("\n");
                }
            }
            try (Cursor c = db.rawQuery("SELECT titulo, autores FROM LIBRO LIMIT 10", null)) {
                while (c != null && c.moveToNext()) {
                    sb.append("- Libro: ").append(c.getString(0)).append(" de ").append(c.getString(1)).append("\n");
                }
            }

            try {
                File scratchDir = new File(context.getFilesDir(), "scratch");
                if (!scratchDir.exists()) scratchDir.mkdirs();
                File txtFile = new File(scratchDir, "etsiit_knowledge_base.txt");
                try (FileWriter writer = new FileWriter(txtFile)) {
                    writer.write(sb.toString());
                }
            } catch (IOException ignored) {}

        } catch (Exception e) {
            e.printStackTrace();
        }

        return sb.toString();
    }
}
