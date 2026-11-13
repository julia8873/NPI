package com.example.nuietsiit.data;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.DatabaseUtils;
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
        poblarComunidades(db);
        completar(db);
    }

    // -------------------------------------------------
    //                  ACTIVIDADES
    // -------------------------------------------------
    public static void poblarActividades(SQLiteDatabase db) {
        // Si ya están, no se vuelven a meter: el bot (GeminiManager) llama a esto cada vez que se abre el chat
        // y, como ACTIVIDAD no tiene nada único, salían todas las actividades repetidas
        if (android.database.DatabaseUtils.queryNumEntries(db, "ACTIVIDAD") > 0) {
            completar(db);
            return;
        }
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
    //                  COMUNIDADES DE CHAT
    // -------------------------------------------------
    // Los enlaces no se abren: la app enseña una vista previa simulada del grupo.
    // idGrado: null = para todos, 1 = Informática, 2 = Doble Inf+Mat, 3 = Teleco
    private static void poblarComunidades(SQLiteDatabase db) {
        comunidad(db, "Informática · Delegación", "whatsapp",
                "Avisos de los delegados, asambleas y dudas del grado.", 1);
        comunidad(db, "Informática 2º curso", "whatsapp",
                "Grupo de clase: horarios, prácticas y quedadas para estudiar.", 1);
        comunidad(db, "Club de programación competitiva", "discord",
                "Entrenamientos semanales y equipos para concursos de programación.", 1);
        comunidad(db, "Doble Grado Inf+Mat", "whatsapp",
                "Grupo de estudiantes del doble grado.", 2);
        comunidad(db, "Teleco · Delegación", "whatsapp",
                "Avisos y dudas de Telecomunicación.", 3);
        comunidad(db, "Apuntes y exámenes ETSIIT", "telegram",
                "Canal para compartir apuntes y exámenes de años anteriores.", null);
        comunidad(db, "ETSIIT Avisos", "telegram",
                "Canal no oficial con los avisos importantes de la escuela.", null);
        comunidad(db, "ETSIIT Gaming & Hackathons", "discord",
                "Torneos de videojuegos, CTF, hackathons y quedadas.", null);
    }

    // -------------------------------------------------
    //                  MÁS DATOS DE EJEMPLO
    // -------------------------------------------------

    /**
     * Datos de ejemplo añadidos después: se meten también en las BD que ya existían (sin subir la versión
     * de la BD) y solo si no están ya (se busca por título). Lo llaman poblar(), poblarActividades()
     * y ActividadesDB la primera vez que se usa en cada arranque.
     */
    public static void completar(SQLiteDatabase db) {
        LocalDate hoy = LocalDate.now();
        LocalDateTime ahora = LocalDateTime.now();

        if (!existe(db, "ACTIVIDAD", "Taller de Git y GitHub")) {
            actividad(db, "Taller de Git y GitHub",
                    "Aprende a usar Git sin miedo: ramas, merge, conflictos y pull requests en GitHub. " +
                    "Trae tu portátil con Git instalado.",
                    "bibliomaker", en(hoy, 2, 17, 0), en(hoy, 2, 19, 0), null);
        }
        if (!existe(db, "ACTIVIDAD", "Cómo preparar tu TFG")) {
            actividad(db, "Cómo preparar tu TFG",
                    "Profesores de la escuela explican cómo elegir tema y tutor, los plazos de entrega " +
                    "y qué se valora en la defensa. Con preguntas al final.",
                    "charla", en(hoy, 4, 12, 0), en(hoy, 4, 13, 30), "https://etsiit.ugr.es");
        }
        if (!existe(db, "ACTIVIDAD", "Feria de empleo TIC")) {
            actividad(db, "Feria de empleo TIC",
                    "Empresas tecnológicas de Granada y de toda España buscan estudiantes para prácticas " +
                    "y primer empleo. Lleva tu CV impreso.",
                    "otro", en(hoy, 8, 10, 0), en(hoy, 8, 14, 0), "https://etsiit.ugr.es");
        }
        if (!existe(db, "ACTIVIDAD", "Hackathon ETSIIT 24 h")) {
            actividad(db, "Hackathon ETSIIT 24 h",
                    "24 horas para crear una app en equipos de 3 o 4 personas. Hay comida, mentores y premios " +
                    "para los tres mejores proyectos.",
                    "otro", en(hoy, 16, 10, 0), en(hoy, 17, 10, 0), null);
        }
        if (!existe(db, "ACTIVIDAD", "Introducción a la ciberseguridad")) {
            long id = actividad(db, "Introducción a la ciberseguridad",
                    "Curso práctico de seguridad en redes y aplicaciones web: contraseñas, cifrado, ataques " +
                    "habituales y cómo defenderse.",
                    "abiertaugr", en(hoy, 22, 16, 0), en(hoy, 40, 18, 0), "https://abierta.ugr.es");
            if (id > 0) curso(db, id, 2, hoy.minusDays(2), hoy.plusDays(10), 25);
        }
        // Curso ya terminado: sus créditos cuentan como conseguidos para la cuenta de prueba
        if (!existe(db, "ACTIVIDAD", "LaTeX para el TFG")) {
            long id = actividad(db, "LaTeX para el TFG",
                    "Aprende a maquetar tu TFG con LaTeX: plantilla de la escuela, bibliografía, figuras y tablas.",
                    "abiertaugr", en(hoy, -40, 16, 0), en(hoy, -12, 18, 0), "https://abierta.ugr.es");
            if (id > 0) curso(db, id, 2, hoy.minusDays(60), hoy.minusDays(45), 30);
        }
        inscribirSiFalta(db, CORREO_INVITADO, "LaTeX para el TFG");
        inscribirSiFalta(db, CORREO_INVITADO, "Fotografía digital con el móvil");

        if (!existe(db, "ANUNCIO_GENERAL", "Simulacro de evacuación el jueves")) {
            anuncio(db, "Simulacro de evacuación el jueves",
                    "El jueves a las 11:30 sonará la alarma de incendios en todo el edificio. Sal por la salida " +
                    "más cercana y espera en el punto de reunión del aparcamiento.",
                    "general", "alta", ahora.minusHours(1), hoy.plusDays(3), null);
        }
        if (!existe(db, "ANUNCIO_GENERAL", "Cambios en la línea 9 por obras")) {
            anuncio(db, "Cambios en la línea 9 por obras",
                    "Por obras en Camino de Ronda, la línea 9 puede retrasarse hasta 10 minutos esta semana. " +
                    "Las paradas no cambian.",
                    "transporte", "normal", ahora.minusHours(3), hoy.plusDays(7), null);
        }
        if (!existe(db, "ANUNCIO_GENERAL", "Abierta la convocatoria de prácticas en empresa")) {
            anuncio(db, "Abierta la convocatoria de prácticas en empresa",
                    "Ya puedes apuntarte a las prácticas externas del segundo cuatrimestre. Consulta las " +
                    "ofertas y pide plaza antes de que acabe el plazo.",
                    "plazo", "normal", ahora.minusDays(1).minusHours(2), hoy.plusDays(15), "https://www.ugr.es");
        }
        if (!existe(db, "ANUNCIO_GENERAL", "Reunión informativa de becas Erasmus+")) {
            anuncio(db, "Reunión informativa de becas Erasmus+",
                    "La Oficina de Relaciones Internacionales explica destinos, plazos y ayudas para el " +
                    "próximo curso. En el salón de actos.",
                    "beca", "normal", ahora.minusDays(2), hoy.plusDays(10), "https://www.ugr.es");
        }
        if (!existe(db, "ANUNCIO_GENERAL", "Nueva sala de estudio en el Edificio Auxiliar")) {
            anuncio(db, "Nueva sala de estudio en el Edificio Auxiliar",
                    "Abre una sala de estudio con 60 puestos y enchufes en la planta baja del Edificio Auxiliar.",
                    "general", "baja", ahora.minusDays(5), null, null);
        }
    }

    private static boolean existe(SQLiteDatabase db, String tabla, String titulo) {
        return DatabaseUtils.queryNumEntries(db, tabla, "titulo = ?", new String[]{titulo}) > 0;
    }

    /** Apunta al estudiante a la actividad con ese título, si existe y no estaba ya apuntado. */
    private static void inscribirSiFalta(SQLiteDatabase db, String correo, String titulo) {
        long id;
        try (Cursor c = db.rawQuery("SELECT idActividad FROM ACTIVIDAD WHERE titulo = ? LIMIT 1", new String[]{titulo})) {
            if (!c.moveToFirst()) return;
            id = c.getLong(0);
        }
        if (DatabaseUtils.queryNumEntries(db, "INSCRIPCION_ACTIVIDAD", "idActividad = ? AND correoEstudiante = ?",
                new String[]{String.valueOf(id), correo}) > 0) return;
        ContentValues cv = new ContentValues();
        cv.put("correoEstudiante", correo);
        cv.put("idActividad", id);
        cv.put("fechaInscripcion", FechasUtil.ahoraBD());
        db.insert("INSCRIPCION_ACTIVIDAD", null, cv);
    }

    // -------------------------------------------------
    //                  AUXILIARES
    // -------------------------------------------------

    // fecha dentro de 'dias' días a la hora indicada
    private static LocalDateTime en(LocalDate hoy, int dias, int hora, int minuto) {
        return hoy.plusDays(dias).atTime(hora, minuto);
    }

    private static void comunidad(SQLiteDatabase db, String nombre, String plataforma,
                                  String descripcion, Integer idGrado) {
        ContentValues cv = new ContentValues();
        cv.put("nombreComunidad", nombre);
        cv.put("plataforma", plataforma);
        // enlace de ejemplo (obligatorio en la tabla, pero no se abre)
        cv.put("enlaceAcceso", "https://ejemplo.etsiit/" + plataforma + "/"
                + nombre.toLowerCase().replaceAll("[^a-z0-9]+", "-"));
        cv.put("descripcion", descripcion);
        if (idGrado != null) cv.put("idGrado", idGrado); else cv.putNull("idGrado");
        db.insert("COMUNIDAD_CHAT", null, cv);
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
