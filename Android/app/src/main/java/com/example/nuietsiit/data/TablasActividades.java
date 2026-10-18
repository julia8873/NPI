package com.example.nuietsiit.data;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;

import com.example.nuietsiit.util.FechasUtil;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Datos de ejemplo de Actividades y Noticias:
 * ACTIVIDAD, CURSO_ABIERTAUGR, INSCRIPCION_ACTIVIDAD y ANUNCIO_GENERAL.
 * Las fechas son relativas al día de instalación para que siempre haya actividades
 * próximas y algún plazo a punto de acabar al probar la app.
 * La llama DBHelper desde onCreate.
 */
public final class TablasActividades {

    private TablasActividades() {}

    /** Cuenta de prueba (se usa si se entra a la app sin iniciar sesión). Contraseña: invitado */
    public static final String CORREO_INVITADO = "invitado@correo.ugr.es";
    private static final String CORREO_ADMIN = "comunicacion@ugr.es";

    public static void poblar(SQLiteDatabase db) {
        // Usuarios necesarios por las claves foráneas (si ya existen no se duplican)
        db.execSQL("INSERT OR IGNORE INTO ESTUDIANTE (correoEstudiante, nombreEstudiante, contraseniaHash, idGrado, codigoIdioma) " +
                "VALUES (?, 'Invitado', 'invitado', 1, 'es')", new Object[]{CORREO_INVITADO});
        db.execSQL("INSERT OR IGNORE INTO ADMINISTRADOR (correoAdmin, nombreAdmin, contraseniaHash, rol) " +
                "VALUES (?, 'Comunicación ETSIIT', 'admin', 'comunicacion')", new Object[]{CORREO_ADMIN});

        poblarActividades(db);
        poblarAnuncios(db);
    }

    // -------------------------------------------------
    //                  ACTIVIDADES
    // -------------------------------------------------
    private static void poblarActividades(SQLiteDatabase db) {
        LocalDate hoy = LocalDate.now();

        long charlaIA = actividad(db, "IA generativa: del laboratorio a la empresa",
                "Ingenieras de una startup granadina cuentan cómo llevan modelos de lenguaje a productos reales: " +
                "qué funciona, qué no y qué perfiles buscan. Habrá turno de preguntas y café al final.",
                "charla", en(hoy, 1, 12, 0), en(hoy, 1, 13, 30), "https://etsiit.ugr.es");

        actividad(db, "Taller de impresión 3D: diseña tu llavero",
                "Aprende a modelar una pieza sencilla con Tinkercad y llévate tu llavero impreso. " +
                "No hace falta experiencia. Plazas limitadas al número de impresoras.",
                "bibliomaker", en(hoy, 3, 17, 0), en(hoy, 3, 19, 0), null);

        actividad(db, "Arduino desde cero",
                "Monta tu primer circuito: LEDs, sensores y un pequeño proyecto final. " +
                "El Bibliomaker presta el material durante la sesión.",
                "bibliomaker", en(hoy, 5, 16, 30), en(hoy, 5, 19, 0), null);

        actividad(db, "Ciberseguridad: tu primer CTF",
                "Introducción a las competiciones Capture The Flag con retos de web y criptografía. " +
                "Trae tu portátil. Al final se forma el equipo de la ETSIIT para el próximo torneo.",
                "charla", en(hoy, 6, 18, 0), en(hoy, 6, 20, 0), null);

        actividad(db, "Torneo de ajedrez ETSIIT",
                "Torneo por rondas a 10 minutos en el hall de la escuela. Hay tableros para todos " +
                "y premio para los tres primeros.",
                "otro", en(hoy, 9, 10, 0), en(hoy, 9, 14, 0), null);

        actividad(db, "Día de la ETSIIT",
                "La fiesta de la escuela: acto institucional, concurso de programación, exhibición de robots, " +
                "torneo de videojuegos y comida para todos en el patio.",
                "dia_etsiit", en(hoy, 12, 9, 0), en(hoy, 12, 21, 0), "https://etsiit.ugr.es");

        actividad(db, "Mujeres en la ingeniería: mesa redonda",
                "Egresadas de la ETSIIT hablan de su carrera profesional, referentes y cómo romper el techo de cristal.",
                "charla", en(hoy, 14, 12, 0), en(hoy, 14, 13, 30), null);

        // Cursos AbiertaUGR: uno con el plazo a punto de cerrar, uno abierto y uno que aún no ha abierto
        long python = actividad(db, "Python para ciencia de datos",
                "Curso práctico de pandas, gráficas y un primer modelo de machine learning con datos reales de Granada.",
                "abiertaugr", en(hoy, 20, 16, 0), en(hoy, 48, 18, 0), "https://abierta.ugr.es");
        curso(db, python, 3, hoy.minusDays(5), hoy.plusDays(2), 30);

        long foto = actividad(db, "Fotografía digital con el móvil",
                "Composición, luz y edición para sacar fotos de verdad con el móvil. Salidas por el Albaicín incluidas.",
                "abiertaugr", en(hoy, 25, 17, 0), en(hoy, 46, 19, 0), "https://abierta.ugr.es");
        curso(db, foto, 2, hoy.minusDays(1), hoy.plusDays(15), 20);

        long signos = actividad(db, "Lengua de signos española: nivel inicial",
                "Aprende a comunicarte en LSE: abecedario, saludos y conversaciones sencillas.",
                "abiertaugr", en(hoy, 30, 16, 0), en(hoy, 60, 18, 0), "https://abierta.ugr.es");
        curso(db, signos, 4, hoy.plusDays(3), hoy.plusDays(18), 25);

        // La cuenta de prueba ya está apuntada a la charla de mañana (para ver la notificación)
        ContentValues cv = new ContentValues();
        cv.put("correoEstudiante", CORREO_INVITADO);
        cv.put("idActividad", charlaIA);
        cv.put("fechaInscripcion", FechasUtil.ahoraBD());
        db.insert("INSCRIPCION_ACTIVIDAD", null, cv);
    }

    // -------------------------------------------------
    //                  ANUNCIOS
    // -------------------------------------------------
    private static void poblarAnuncios(SQLiteDatabase db) {
        LocalDateTime ahora = LocalDateTime.now();
        LocalDate hoy = LocalDate.now();

        anuncio(db, "Último día para modificar la matrícula",
                "Mañana termina el plazo para cambiar o ampliar asignaturas. Después solo se admitirán cambios " +
                "por causa justificada. Hazlo desde Secretaría o en la sede electrónica.",
                "plazo", "alta", ahora.minusHours(2), hoy.plusDays(1), "https://www.ugr.es");

        anuncio(db, "Huelga de autobuses urbanos el próximo lunes",
                "Habrá servicios mínimos del 30 % en las líneas que llegan a la escuela. " +
                "Se recomienda venir en metro o con tiempo.",
                "transporte", "alta", ahora.minusHours(5), hoy.plusDays(5), null);

        anuncio(db, "Convocatoria de becas de colaboración",
                "El Ministerio abre las becas de colaboración en departamentos para estudiantes de último curso. " +
                "Dotación de 2.000 € y reconocimiento de créditos.",
                "beca", "normal", ahora.minusDays(1), hoy.plusDays(20), "https://www.ugr.es");

        anuncio(db, "Ayudas para el comedor universitario",
                "Los estudiantes con beca del Ministerio pueden pedir menús gratuitos en los comedores de la UGR.",
                "beca", "normal", ahora.minusDays(1).minusHours(3), hoy.plusDays(30), null);

        anuncio(db, "Abierto el plazo de propuestas de TFG",
                "Ya puedes consultar las propuestas de los departamentos y pedir tutor. " +
                "Las asignaciones se publican a final de mes.",
                "plazo", "normal", ahora.minusDays(2), hoy.plusDays(14), null);

        anuncio(db, "Horario ampliado de la biblioteca en exámenes",
                "Durante el periodo de exámenes la biblioteca abrirá de 8:00 a 2:00, también fines de semana.",
                "general", "normal", ahora.minusDays(3), null, null);

        anuncio(db, "Nuevos microondas en la cafetería",
                "Se han instalado cuatro microondas más junto a la entrada de la cafetería. ¡Adiós a las colas!",
                "general", "baja", ahora.minusDays(4), null, null);

        anuncio(db, "Nuevo aparcamiento de bicicletas",
                "Ya está disponible el aparcamiento cubierto para 40 bicis junto a la entrada principal.",
                "transporte", "baja", ahora.minusDays(6), null, null);
    }

    // -------------------------------------------------
    //                  AUXILIARES
    // -------------------------------------------------

    // fecha dentro de 'dias' días a la hora indicada
    private static LocalDateTime en(LocalDate hoy, int dias, int hora, int minuto) {
        return hoy.plusDays(dias).atTime(hora, minuto);
    }

    private static long actividad(SQLiteDatabase db, String titulo, String descripcion, String tipo,
                                  LocalDateTime inicio, LocalDateTime fin, String enlace) {
        ContentValues cv = new ContentValues();
        cv.put("titulo", titulo);
        cv.put("descripcion", descripcion);
        cv.put("tipoActividad", tipo);
        cv.put("fechaInicio", FechasUtil.aBD(inicio));
        cv.put("fechaFin", fin != null ? FechasUtil.aBD(fin) : null);
        cv.put("enlaceInfo", enlace);
        cv.put("correoAdmin", CORREO_ADMIN);
        return db.insert("ACTIVIDAD", null, cv);
    }

    private static void curso(SQLiteDatabase db, long idActividad, double creditos,
                              LocalDate plazoInicio, LocalDate plazoFin, Integer plazas) {
        ContentValues cv = new ContentValues();
        cv.put("idActividad", idActividad);
        cv.put("creditos", creditos);
        cv.put("plazoInicioInscripcion", plazoInicio.toString());
        cv.put("plazoFinInscripcion", plazoFin.toString());
        cv.put("plazasTotales", plazas);
        db.insert("CURSO_ABIERTAUGR", null, cv);
    }

    private static void anuncio(SQLiteDatabase db, String titulo, String contenido, String categoria,
                                String prioridad, LocalDateTime publicacion, LocalDate caducidad, String enlace) {
        ContentValues cv = new ContentValues();
        cv.put("titulo", titulo);
        cv.put("contenido", contenido);
        cv.put("categoria", categoria);
        cv.put("prioridad", prioridad);
        cv.put("fechaPublicacion", FechasUtil.aBD(publicacion));
        // caduca al final del día indicado
        cv.put("fechaCaducidad", caducidad != null ? FechasUtil.aBD(caducidad.atTime(LocalTime.of(23, 59))) : null);
        cv.put("enlace", enlace);
        cv.put("correoAdmin", CORREO_ADMIN);
        db.insert("ANUNCIO_GENERAL", null, cv);
    }
}
