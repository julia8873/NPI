package com.example.nuietsiit.activities.secciones.mapa;

import android.content.Context;
import android.database.Cursor;

import com.example.nuietsiit.data.AcademicoDB;
import com.example.nuietsiit.data.DBHelper;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Método 1 para ubicar al usuario: su horario.
 * Si ahora mismo tiene una clase en curso, asumimos que está en la puerta de esa aula.
 */
public final class LocalizadorUsuario {

    /**
     * Aula de cada clase. De momento SESION_CLASE.idEspacio viene a null en los datos de ejemplo
     * y ESPACIO está vacía, así que lo resolvemos aquí. El texto debe estar contenido en el título
     * de un marcador naranja de Mapa.registrarMarcadores() (p. ej. "Aula 0.5").
     * Clave: codigoAsignatura|tipoSesion.   DATOS DE EJEMPLO: cambiadlos por las aulas reales.
     */
    private static final Map<String, String> AULA_POR_CLASE = new HashMap<>();

    static {
        AULA_POR_CLASE.put("NPI|teoria", "Aula 2.1");
        AULA_POR_CLASE.put("NPI|practicas", "Aula 0.5");
        AULA_POR_CLASE.put("PL|teoria", "Aula 2.1");
        AULA_POR_CLASE.put("PL|practicas", "Aula 0.5");
    }

    private LocalizadorUsuario() {}

    public static final class SesionActual {
        public final String codigoAsignatura;
        public final String nombreAsignatura;
        public final String tipoSesion;
        public final String horaInicio;
        public final String horaFin;
        public final String aula; // texto del aula (ej. "Aula 0.5") o null si no la conocemos

        SesionActual(String codigo, String nombre, String tipo, String ini, String fin, String aula) {
            this.codigoAsignatura = codigo;
            this.nombreAsignatura = nombre;
            this.tipoSesion = tipo;
            this.horaInicio = ini;
            this.horaFin = fin;
            this.aula = aula;
        }
    }

    /** Curso académico en formato "2026-27" (septiembre en adelante cuenta como curso nuevo). */
    public static String cursoAcademico(Calendar ahora) {
        int anio = ahora.get(Calendar.YEAR);
        int mes = ahora.get(Calendar.MONTH); // 0 = enero
        int inicio = (mes >= Calendar.SEPTEMBER) ? anio : anio - 1;
        return String.format(Locale.US, "%d-%02d", inicio, (inicio + 1) % 100);
    }

    /** Clase en curso ahora mismo para ese usuario, o null si no tiene ninguna. */
    public static SesionActual sesionEnCurso(Context ctx, String correo, Calendar ahora) {
        int dia = ((ahora.get(Calendar.DAY_OF_WEEK) + 5) % 7) + 1; // 1 = lunes ... 7 = domingo
        String hora = String.format(Locale.US, "%02d:%02d",
                ahora.get(Calendar.HOUR_OF_DAY), ahora.get(Calendar.MINUTE));

        AcademicoDB academico = new AcademicoDB(DBHelper.getInstance(ctx));
        try (Cursor c = academico.listarSesionesDe(correo, cursoAcademico(ahora))) {
            int iDia = c.getColumnIndexOrThrow("diaSemana");
            int iIni = c.getColumnIndexOrThrow("horaInicio");
            int iFin = c.getColumnIndexOrThrow("horaFin");
            int iTipo = c.getColumnIndexOrThrow("tipoSesion");
            int iCod = c.getColumnIndexOrThrow("codigoAsignatura");
            int iNom = c.getColumnIndexOrThrow("nombreAsignatura");

            while (c.moveToNext()) {
                if (c.getInt(iDia) != dia) continue;
                String ini = c.getString(iIni);
                String fin = c.getString(iFin);
                // "HH:MM" se puede comparar como texto
                if (ini.compareTo(hora) <= 0 && fin.compareTo(hora) > 0) {
                    String codigo = c.getString(iCod);
                    String tipo = c.getString(iTipo);
                    return new SesionActual(codigo, c.getString(iNom), tipo, ini, fin,
                            AULA_POR_CLASE.get(codigo + "|" + tipo));
                }
            }
        }
        return null;
    }
}
