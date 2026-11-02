package com.example.nuietsiit.data;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.nuietsiit.data.modelo.EventoCalendario;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

/**
 * Acceso a datos para el Calendario de Actividades, Exámenes y Entregas.
 */
public class CalendarioDB {

    private final DBHelper dbHelper;

    public CalendarioDB(DBHelper dbHelper) {
        this.dbHelper = dbHelper;
        crearTablaSiNoExiste();
        poblarEventosDeEjemploSiVacio();
    }

    private void crearTablaSiNoExiste() {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS EVENTO_CALENDARIO (" +
            "  idEvento INTEGER PRIMARY KEY AUTOINCREMENT," +
            "  correoUsuario TEXT," +
            "  titulo TEXT NOT NULL," +
            "  tipo TEXT NOT NULL," +
            "  fecha TEXT NOT NULL," +
            "  hora TEXT," +
            "  descripcion TEXT" +
            ")"
        );
    }

    public long insertarEvento(String correoUsuario, String titulo, String tipo, String fecha, String hora, String descripcion) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("correoUsuario", correoUsuario != null ? correoUsuario.toLowerCase() : "");
        cv.put("titulo", titulo);
        cv.put("tipo", tipo != null ? tipo : "Examen");
        cv.put("fecha", fecha);
        cv.put("hora", hora != null ? hora : "");
        cv.put("descripcion", descripcion != null ? descripcion : "");
        return db.insert("EVENTO_CALENDARIO", null, cv);
    }

    public boolean eliminarEvento(long idEvento) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        return db.delete("EVENTO_CALENDARIO", "idEvento = ?", new String[]{String.valueOf(idEvento)}) > 0;
    }

    public List<EventoCalendario> obtenerEventosPorFecha(String fecha, String correoUsuario) {
        List<EventoCalendario> lista = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        String selection = "fecha = ?";
        List<String> selectionArgs = new ArrayList<>();
        selectionArgs.add(fecha);

        if (correoUsuario != null && !correoUsuario.isEmpty()) {
            selection += " AND (correoUsuario = ? OR correoUsuario = '' OR correoUsuario IS NULL)";
            selectionArgs.add(correoUsuario.toLowerCase());
        }

        Cursor cursor = db.query("EVENTO_CALENDARIO", null, selection,
                selectionArgs.toArray(new String[0]), null, null, "hora ASC, idEvento ASC");

        if (cursor != null) {
            while (cursor.moveToNext()) {
                long id = cursor.getLong(cursor.getColumnIndexOrThrow("idEvento"));
                String correo = cursor.getString(cursor.getColumnIndexOrThrow("correoUsuario"));
                String titulo = cursor.getString(cursor.getColumnIndexOrThrow("titulo"));
                String tipo = cursor.getString(cursor.getColumnIndexOrThrow("tipo"));
                String f = cursor.getString(cursor.getColumnIndexOrThrow("fecha"));
                String h = cursor.getString(cursor.getColumnIndexOrThrow("hora"));
                String desc = cursor.getString(cursor.getColumnIndexOrThrow("descripcion"));
                lista.add(new EventoCalendario(id, correo, titulo, tipo, f, h, desc));
            }
            cursor.close();
        }
        return lista;
    }

    public boolean hayEventos() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM EVENTO_CALENDARIO", null);
        boolean hay = false;
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                hay = cursor.getInt(0) > 0;
            }
            cursor.close();
        }
        return hay;
    }

    private void poblarEventosDeEjemploSiVacio() {
        if (!hayEventos()) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Calendar cal = Calendar.getInstance();

            // Evento 1: Hoy
            String hoy = sdf.format(cal.getTime());
            insertarEvento("", "Examen Parcial - NPI", "Examen", hoy, "10:30", "Aula 1.1 - ETSIIT");

            // Evento 2: Mañana
            cal.add(Calendar.DAY_OF_MONTH, 1);
            String manana = sdf.format(cal.getTime());
            insertarEvento("", "Entrega Práctica 2 - Sistemas Interactivos", "Entrega", manana, "23:59", "Subir archivo ZIP a PRADO");

            // Evento 3: En 4 días
            cal.add(Calendar.DAY_OF_MONTH, 3);
            String en4dias = sdf.format(cal.getTime());
            insertarEvento("", "Jornada Puertas Abiertas ETSIIT", "Actividades", en4dias, "12:00", "Salón de Actos");

            // Evento 4: En 7 días
            cal.add(Calendar.DAY_OF_MONTH, 3);
            String en7dias = sdf.format(cal.getTime());
            insertarEvento("", "Entrega de Memoria de Proyecto", "Entrega", en7dias, "18:00", "Secretaría de la ETSIIT");
        }
    }
}
