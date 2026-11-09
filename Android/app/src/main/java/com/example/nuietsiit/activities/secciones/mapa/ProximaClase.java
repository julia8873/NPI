package com.example.nuietsiit.activities.secciones.mapa;

import android.content.Context;
import android.database.Cursor;

import com.example.nuietsiit.data.AcademicoDB;
import com.example.nuietsiit.data.DBHelper;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

/**
 * Clase en curso o siguiente clase de hoy, y en qué edificio es (ETSIIT o Edificio Auxiliar).
 * Usa la API pública de LocalizadorUsuario para el aula; el horario sale de AcademicoDB.
 */
public final class ProximaClase {

    public static final String ETSIIT = "etsiit";
    public static final String AUXILIAR = "edificio_auxiliar";

    /**
     * Clases que se dan en el Edificio Auxiliar. Clave: codigoAsignatura|tipoSesion.
     * DATOS DE EJEMPLO (para probar la ruta ETSIIT → Auxiliar): cambiadlos por los reales.
     */
    private static final Map<String, String> AULA_EN_AUXILIAR = new HashMap<>();

    static {
        AULA_EN_AUXILIAR.put("PL|practicas", "Aula A1 (Auxiliar)");
    }

    public final String codigoAsignatura;
    public final String nombreAsignatura;
    public final String horaInicio;
    public final String aula;      // puede ser null
    public final String idEdificio; // id del lugar en RedTransporte
    public final boolean enCurso;

    private ProximaClase(String codigo, String nombre, String ini, String aula, String edificio, boolean enCurso) {
        this.codigoAsignatura = codigo;
        this.nombreAsignatura = nombre;
        this.horaInicio = ini;
        this.aula = aula;
        this.idEdificio = edificio;
        this.enCurso = enCurso;
    }

    /** "NPI · Aula 2.1 · empieza 10:30" o "NPI · Aula 2.1 · en curso". */
    public String texto() {
        StringBuilder sb = new StringBuilder(codigoAsignatura);
        if (aula != null) sb.append(" · ").append(aula);
        return sb.append(enCurso ? " · en curso" : " · empieza " + horaInicio).toString();
    }

    /** Clase en curso o la siguiente de hoy; null si hoy ya no hay más. */
    public static ProximaClase de(Context ctx, String correo, Calendar ahora) {
        if (correo == null || correo.isEmpty()) return null;
        int dia = ((ahora.get(Calendar.DAY_OF_WEEK) + 5) % 7) + 1; // 1 = lunes ... 7 = domingo
        String hora = String.format("%02d:%02d", ahora.get(Calendar.HOUR_OF_DAY), ahora.get(Calendar.MINUTE));

        AcademicoDB academico = new AcademicoDB(DBHelper.getInstance(ctx));
        try (Cursor c = academico.listarSesionesDe(correo, LocalizadorUsuario.cursoAcademico(ahora))) {
            int iDia = c.getColumnIndexOrThrow("diaSemana");
            int iIni = c.getColumnIndexOrThrow("horaInicio");
            int iFin = c.getColumnIndexOrThrow("horaFin");
            int iTipo = c.getColumnIndexOrThrow("tipoSesion");
            int iCod = c.getColumnIndexOrThrow("codigoAsignatura");
            int iNom = c.getColumnIndexOrThrow("nombreAsignatura");

            // Vienen ordenadas por día y hora: la primera de hoy que no haya acabado
            while (c.moveToNext()) {
                if (c.getInt(iDia) != dia || c.getString(iFin).compareTo(hora) <= 0) continue;
                String ini = c.getString(iIni);
                String codigo = c.getString(iCod);
                String clave = codigo + "|" + c.getString(iTipo);
                String aula = AULA_EN_AUXILIAR.get(clave);
                if (aula == null) aula = aulaSegunLocalizador(ctx, correo, ahora, ini);
                return new ProximaClase(codigo, c.getString(iNom), ini, aula,
                        AULA_EN_AUXILIAR.containsKey(clave) ? AUXILIAR : ETSIIT, ini.compareTo(hora) <= 0);
            }
        }
        return null;
    }

    /** Pregunta a LocalizadorUsuario qué aula tiene a la hora de inicio de la clase. */
    private static String aulaSegunLocalizador(Context ctx, String correo, Calendar ahora, String ini) {
        Calendar enClase = (Calendar) ahora.clone();
        enClase.set(Calendar.HOUR_OF_DAY, Integer.parseInt(ini.substring(0, 2)));
        enClase.set(Calendar.MINUTE, Integer.parseInt(ini.substring(3, 5)));
        LocalizadorUsuario.SesionActual s = LocalizadorUsuario.sesionEnCurso(ctx, correo, enClase);
        return s != null ? s.aula : null;
    }
}
