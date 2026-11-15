package com.example.nuietsiit.data;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;

import java.util.Locale;

/**
 * Traducción al inglés de los DATOS de Actividades, Noticias y Comunidades (títulos, descripciones y avisos),
 * no solo de la interfaz: así un visitante que use la app en inglés entiende también las actividades y los avisos.
 * Usa la tabla TRADUCCION (entidad, idEntidad, campo, codigoIdioma, texto) que ya define DBHelper.
 * - guardar(): mete las traducciones de los datos de ejemplo (sin repetir). Lo llama TablasActividades.completar().
 * - traducir(): devuelve el texto en el idioma de la app, o el original si no hay traducción.
 * Las traducciones se buscan por el texto en español para no depender del número que la BD dé a cada fila.
 */
public final class TraduccionesActividades {

    private TraduccionesActividades() {}

    // {título en español, título en inglés, descripción en inglés}
    private static final String[][] ACTIVIDADES = {
            {"IA generativa: del laboratorio a la empresa", "Generative AI: from the lab to the company",
                    "Engineers from a Granada startup explain how they take language models into real products: "
                            + "what works, what doesn't and which profiles they are looking for. Q&A and coffee at the end."},
            {"Taller de impresión 3D: diseña tu llavero", "3D printing workshop: design your keyring",
                    "Learn to model a simple part with Tinkercad and take home your printed keyring. "
                            + "No experience needed. Places are limited to the number of printers."},
            {"Arduino desde cero", "Arduino from scratch",
                    "Build your first circuit: LEDs, sensors and a small final project. "
                            + "The Bibliomaker lends you the material during the session."},
            {"Ciberseguridad: tu primer CTF", "Cybersecurity: your first CTF",
                    "Introduction to Capture The Flag competitions with web and cryptography challenges. "
                            + "Bring your laptop. At the end, the ETSIIT team for the next tournament is formed."},
            {"Torneo de ajedrez ETSIIT", "ETSIIT chess tournament",
                    "Round-based tournament with 10-minute games in the school hall. "
                            + "There are boards for everyone and prizes for the top three."},
            {"Día de la ETSIIT", "ETSIIT Day",
                    "The school's big day: official ceremony, programming contest, robot exhibition, "
                            + "video game tournament and food for everyone in the courtyard."},
            {"Mujeres en la ingeniería: mesa redonda", "Women in engineering: round table",
                    "ETSIIT graduates talk about their careers, role models and how to break the glass ceiling."},
            {"Python para ciencia de datos", "Python for data science",
                    "Hands-on course on pandas, charts and a first machine learning model with real data from Granada."},
            {"Fotografía digital con el móvil", "Digital photography with your phone",
                    "Composition, light and editing to take real photos with your phone. "
                            + "Photo walks around the Albaicín included."},
            {"Lengua de signos española: nivel inicial", "Spanish Sign Language: beginner level",
                    "Learn to communicate in LSE: alphabet, greetings and simple conversations."},
            {"Taller de Git y GitHub", "Git and GitHub workshop",
                    "Learn to use Git without fear: branches, merges, conflicts and pull requests on GitHub. "
                            + "Bring your laptop with Git installed."},
            {"Cómo preparar tu TFG", "How to prepare your final degree project",
                    "Lecturers explain how to choose a topic and a supervisor, the submission deadlines "
                            + "and what is assessed in the defence. Questions at the end."},
            {"Feria de empleo TIC", "ICT job fair",
                    "Tech companies from Granada and all over Spain are looking for students for internships "
                            + "and first jobs. Bring a printed CV."},
            {"Hackathon ETSIIT 24 h", "ETSIIT 24 h Hackathon",
                    "24 hours to build an app in teams of 3 or 4 people. Food, mentors and prizes "
                            + "for the three best projects."},
            {"Introducción a la ciberseguridad", "Introduction to cybersecurity",
                    "Hands-on course on network and web application security: passwords, encryption, "
                            + "common attacks and how to defend yourself."},
            {"LaTeX para el TFG", "LaTeX for your final degree project",
                    "Learn to typeset your final degree project with LaTeX: the school template, bibliography, "
                            + "figures and tables."},
    };

    // {título en español, título en inglés, contenido en inglés}
    private static final String[][] ANUNCIOS = {
            {"Último día para modificar la matrícula", "Last day to change your enrolment",
                    "The deadline to change or add subjects ends tomorrow. After that, changes will only be accepted "
                            + "for justified reasons. Do it from Secretaría or the online office."},
            {"Huelga de autobuses urbanos el próximo lunes", "City bus strike next Monday",
                    "There will be minimum services of 30% on the lines that reach the school. "
                            + "We recommend coming by metro or leaving early."},
            {"Convocatoria de becas de colaboración", "Collaboration grants call",
                    "The Ministry opens collaboration grants in departments for final-year students. "
                            + "€2,000 and credit recognition."},
            {"Ayudas para el comedor universitario", "Grants for the university canteen",
                    "Students with a Ministry grant can request free meals in the UGR canteens."},
            {"Abierto el plazo de propuestas de TFG", "Final degree project proposals now open",
                    "You can now check the departments' proposals and request a supervisor. "
                            + "Assignments are published at the end of the month."},
            {"Horario ampliado de la biblioteca en exámenes", "Extended library hours during exams",
                    "During the exam period the library will open from 8:00 to 2:00, weekends included."},
            {"Nuevos microondas en la cafetería", "New microwaves in the cafeteria",
                    "Four more microwaves have been installed next to the cafeteria entrance. Goodbye queues!"},
            {"Nuevo aparcamiento de bicicletas", "New bike parking",
                    "The covered parking for 40 bikes next to the main entrance is now available."},
            {"Simulacro de evacuación el jueves", "Evacuation drill on Thursday",
                    "On Thursday at 11:30 the fire alarm will sound throughout the building. Leave through the nearest "
                            + "exit and wait at the meeting point in the car park."},
            {"Cambios en la línea 9 por obras", "Changes to bus line 9 due to roadworks",
                    "Due to roadworks on Camino de Ronda, line 9 may be up to 10 minutes late this week. "
                            + "The stops do not change."},
            {"Abierta la convocatoria de prácticas en empresa", "Company internships call now open",
                    "You can now apply for second-semester external internships. Check the offers and request "
                            + "a place before the deadline."},
            {"Reunión informativa de becas Erasmus+", "Erasmus+ grants information session",
                    "The International Relations Office explains destinations, deadlines and grants for next year. "
                            + "In the assembly hall."},
            {"Nueva sala de estudio en el Edificio Auxiliar", "New study room in the Auxiliary Building",
                    "A study room with 60 seats and power sockets opens on the ground floor of the Auxiliary Building."},
    };

    // {descripción en español, descripción en inglés} de las comunidades de chat
    private static final String[][] COMUNIDADES = {
            {"Avisos de los delegados, asambleas y dudas del grado.", "Announcements from class reps, assemblies and questions about the degree."},
            {"Grupo de clase: horarios, prácticas y quedadas para estudiar.", "Class group: timetables, lab work and study meet-ups."},
            {"Entrenamientos semanales y equipos para concursos de programación.", "Weekly training and teams for programming contests."},
            {"Grupo de estudiantes del doble grado.", "Group for double degree students."},
            {"Avisos y dudas de Telecomunicación.", "Announcements and questions about Telecommunications."},
            {"Canal para compartir apuntes y exámenes de años anteriores.", "Channel to share notes and past exams."},
            {"Canal no oficial con los avisos importantes de la escuela.", "Unofficial channel with the school's important announcements."},
            {"Torneos de videojuegos, CTF, hackathons y quedadas.", "Video game tournaments, CTFs, hackathons and meet-ups."},
    };

    /** Guarda en TRADUCCION las traducciones al inglés de los datos de ejemplo (si ya estaban, las reemplaza). */
    public static void guardar(SQLiteDatabase db) {
        db.execSQL("INSERT OR IGNORE INTO IDIOMA (codigoIdioma, nombreIdioma) VALUES ('en', 'English')");
        for (String[] t : ACTIVIDADES) {
            for (Integer id : ids(db, "SELECT idActividad FROM ACTIVIDAD WHERE titulo = ?", t[0])) {
                poner(db, "ACTIVIDAD", id, "titulo", t[1]);
                poner(db, "ACTIVIDAD", id, "descripcion", t[2]);
            }
        }
        for (String[] t : ANUNCIOS) {
            for (Integer id : ids(db, "SELECT idAnuncioGeneral FROM ANUNCIO_GENERAL WHERE titulo = ?", t[0])) {
                poner(db, "ANUNCIO_GENERAL", id, "titulo", t[1]);
                poner(db, "ANUNCIO_GENERAL", id, "contenido", t[2]);
            }
        }
        for (String[] t : COMUNIDADES) {
            for (Integer id : ids(db, "SELECT idComunidad FROM COMUNIDAD_CHAT WHERE descripcion = ?", t[0])) {
                poner(db, "COMUNIDAD_CHAT", id, "descripcion", t[1]);
            }
        }
    }

    /** Idioma de la app (el que se elige en Ajustes): "es" o "en". */
    public static String idiomaApp() {
        LocaleListCompat locales = AppCompatDelegate.getApplicationLocales();
        Locale idioma = locales.isEmpty() ? Locale.getDefault() : locales.get(0);
        return idioma != null ? idioma.getLanguage() : "es";
    }

    /** El texto en el idioma de la app; si está en español o no hay traducción, el original. */
    public static String traducir(SQLiteDatabase db, String entidad, int id, String campo, String original) {
        String idioma = idiomaApp();
        if (original == null || "es".equals(idioma)) return original;
        try (Cursor c = db.rawQuery(
                "SELECT texto FROM TRADUCCION WHERE entidad = ? AND idEntidad = ? AND campo = ? AND codigoIdioma = ?",
                new String[]{entidad, String.valueOf(id), campo, idioma})) {
            return c.moveToFirst() ? c.getString(0) : original;
        } catch (Exception e) {
            return original; // sin tabla o sin traducción: se enseña en español
        }
    }

    private static java.util.List<Integer> ids(SQLiteDatabase db, String consulta, String valor) {
        java.util.List<Integer> ids = new java.util.ArrayList<>();
        try (Cursor c = db.rawQuery(consulta, new String[]{valor})) {
            while (c.moveToNext()) ids.add(c.getInt(0));
        }
        return ids;
    }

    private static void poner(SQLiteDatabase db, String entidad, int id, String campo, String texto) {
        ContentValues cv = new ContentValues();
        cv.put("entidad", entidad);
        cv.put("idEntidad", String.valueOf(id));
        cv.put("campo", campo);
        cv.put("codigoIdioma", "en");
        cv.put("texto", texto);
        db.insertWithOnConflict("TRADUCCION", null, cv, SQLiteDatabase.CONFLICT_REPLACE);
    }
}
