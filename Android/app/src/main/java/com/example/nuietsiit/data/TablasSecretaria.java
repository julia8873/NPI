package com.example.nuietsiit.data;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;

import com.example.nuietsiit.util.TextoUtil;

import java.time.LocalDate;

/**
 * Tablas de Secretaría: PROCEDIMIENTO, PROCEDIMIENTO_PASO, PROCEDIMIENTO_DOCUMENTO,
 * SOLICITUD, SOLICITUD_DOCUMENTO y CITA.
 * Todo se gestiona dentro de la app (solicitudes y citas simuladas, sin enlaces externos).
 * Las llama DBHelper desde onCreate / onUpgrade.
 */
public final class TablasSecretaria {

    private TablasSecretaria() {}

    // -------------------------------------------------
    //              CREAR / BORRAR
    // -------------------------------------------------
    public static void crear(SQLiteDatabase db) {
        db.execSQL(
            "CREATE TABLE PROCEDIMIENTO (" +
            "  idProcedimiento     INTEGER PRIMARY KEY AUTOINCREMENT," +
            "  nombreProcedimiento TEXT NOT NULL," +
            "  categoria           TEXT NOT NULL CHECK (categoria IN " +
            "    ('matricula','titulos','reconocimiento_creditos','convocatorias','certificados','otro'))," +
            "  descripcion         TEXT NOT NULL," +
            "  requisitos          TEXT," +
            "  plazosInfo          TEXT," +
            "  plazoInicio         TEXT," +                     // ISO yyyy-MM-dd
            "  plazoFin            TEXT," +
            "  solicitudOnline     INTEGER NOT NULL DEFAULT 1," + // 0 = solo presencial (con cita previa)
            "  palabrasClave       TEXT," +                     // sinónimos (búsqueda por texto y por voz)
            "  textoBusqueda       TEXT NOT NULL" +             // nombre+palabrasClave+descripción normalizados
            ")"
        );

        db.execSQL(
            "CREATE TABLE PROCEDIMIENTO_PASO (" +
            "  idProcedimiento INTEGER NOT NULL," +
            "  orden           INTEGER NOT NULL," +
            "  tituloPaso      TEXT NOT NULL," +
            "  descripcionPaso TEXT NOT NULL," +
            "  PRIMARY KEY (idProcedimiento, orden)," +
            "  FOREIGN KEY (idProcedimiento) REFERENCES PROCEDIMIENTO(idProcedimiento)" +
            ")"
        );

        // documentos que el usuario tiene que fotografiar al solicitar
        db.execSQL(
            "CREATE TABLE PROCEDIMIENTO_DOCUMENTO (" +
            "  idDocumento     INTEGER PRIMARY KEY AUTOINCREMENT," +
            "  idProcedimiento INTEGER NOT NULL," +
            "  nombreDocumento TEXT NOT NULL," +
            "  descripcion     TEXT," +
            "  obligatorio     INTEGER NOT NULL DEFAULT 1," +
            "  FOREIGN KEY (idProcedimiento) REFERENCES PROCEDIMIENTO(idProcedimiento)" +
            ")"
        );

        // estado (Enviada / En revisión / Resuelta) se deriva de fechaEnvio; código = "SOL-" + id
        db.execSQL(
            "CREATE TABLE SOLICITUD (" +
            "  idSolicitud     INTEGER PRIMARY KEY AUTOINCREMENT," +
            "  correoUsuario   TEXT NOT NULL," +
            "  idProcedimiento INTEGER NOT NULL," +
            "  fechaEnvio      TEXT NOT NULL," +                // ISO yyyy-MM-ddTHH:mm:ss
            "  observaciones   TEXT," +
            "  rutaFirma       TEXT," +                         // PNG en getFilesDir()/secretaria/
            "  FOREIGN KEY (correoUsuario)   REFERENCES usuarios(correoUsuario)," +
            "  FOREIGN KEY (idProcedimiento) REFERENCES PROCEDIMIENTO(idProcedimiento)" +
            ")"
        );

        db.execSQL(
            "CREATE TABLE SOLICITUD_DOCUMENTO (" +
            "  idSolicitud INTEGER NOT NULL," +
            "  idDocumento INTEGER NOT NULL," +
            "  rutaFoto    TEXT NOT NULL," +
            "  PRIMARY KEY (idSolicitud, idDocumento)," +
            "  FOREIGN KEY (idSolicitud) REFERENCES SOLICITUD(idSolicitud)," +
            "  FOREIGN KEY (idDocumento) REFERENCES PROCEDIMIENTO_DOCUMENTO(idDocumento)" +
            ")"
        );

        // código = "CITA-" + id
        db.execSQL(
            "CREATE TABLE CITA (" +
            "  idCita          INTEGER PRIMARY KEY AUTOINCREMENT," +
            "  correoUsuario   TEXT NOT NULL," +
            "  idProcedimiento INTEGER," +                      // NULL = consulta general
            "  fecha           TEXT NOT NULL," +                // ISO yyyy-MM-dd
            "  hora            TEXT NOT NULL," +                // HH:mm
            "  estado          TEXT NOT NULL DEFAULT 'activa' CHECK (estado IN ('activa','cancelada'))," +
            "  FOREIGN KEY (correoUsuario)   REFERENCES usuarios(correoUsuario)," +
            "  FOREIGN KEY (idProcedimiento) REFERENCES PROCEDIMIENTO(idProcedimiento)" +
            ")"
        );
    }

    public static void borrar(SQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS CITA");
        db.execSQL("DROP TABLE IF EXISTS SOLICITUD_DOCUMENTO");
        db.execSQL("DROP TABLE IF EXISTS SOLICITUD");
        db.execSQL("DROP TABLE IF EXISTS PROCEDIMIENTO_DOCUMENTO");
        db.execSQL("DROP TABLE IF EXISTS PROCEDIMIENTO_PASO");
        db.execSQL("DROP TABLE IF EXISTS PROCEDIMIENTO");
    }

    // -------------------------------------------------
    //              DATOS DE EJEMPLO
    // -------------------------------------------------
    private static final boolean ONLINE = true;
    private static final boolean PRESENCIAL = false;

    public static void poblar(SQLiteDatabase db) {
        // plazos relativos a la fecha de instalación para que siempre haya alguno abierto al probar
        LocalDate hoy = LocalDate.now();
        String abiertoDesde = hoy.minusDays(10).toString();
        String abiertoHasta = hoy.plusDays(20).toString();
        String cerradoDesde = hoy.minusDays(90).toString();
        String cerradoHasta = hoy.minusDays(15).toString();
        String proximoDesde = hoy.plusDays(18).toString();
        String proximoHasta = hoy.plusDays(29).toString();

        long id;

        // ---------- MATRÍCULA ----------
        // plazo cerrado (relativo a hoy para que siempre lo esté): botón Solicitar deshabilitado
        id = tramite(db, "Automatrícula", "matricula",
                "Matrícula de las asignaturas del curso desde la app.",
                "Haber sido admitido en el grado o tener matrícula en el curso anterior.",
                "Julio a septiembre, según la cita de matrícula asignada a cada estudiante.",
                cerradoDesde, cerradoHasta, ONLINE,
                "matricularme, matricula, inscribirme, asignaturas, oficina virtual");
        paso(db, id, 1, "Pulsa Solicitar", "Tus datos ya aparecen rellenos. En observaciones escribe las asignaturas y grupos que quieres.");
        paso(db, id, 2, "Haz una foto de tu DNI", "Fotografía tu DNI o NIE por la cara de la foto, sin reflejos.");
        paso(db, id, 3, "Añade tus descuentos", "Si tienes familia numerosa o beca, haz una foto del carné o de la credencial.");
        paso(db, id, 4, "Firma con el dedo", "Firma en el recuadro de la pantalla. Si no te gusta, pulsa Borrar y repite.");
        paso(db, id, 5, "Envía y guarda el justificante", "Mantén pulsado Enviar. Tendrás un justificante con QR en Mis gestiones.");
        documento(db, id, "DNI o NIE", "Cara de la foto, legible y sin reflejos.", true);
        documento(db, id, "Carné de familia numerosa o credencial de beca", "Solo si tienes derecho a descuento.", false);

        id = tramite(db, "Modificación y ampliación de matrícula", "matricula",
                "Permite cambiar asignaturas matriculadas o añadir otras nuevas dentro del plazo oficial.",
                "Tener matrícula formalizada en el curso actual.",
                "Plazo de modificación del primer cuatrimestre y ampliación para asignaturas del segundo.",
                abiertoDesde, abiertoHasta, ONLINE,
                "cambiar matricula, ampliar, ampliacion, alteracion, añadir asignatura, quitar asignatura, modificar");
        paso(db, id, 1, "Revisa tu matrícula actual", "Comprueba qué asignaturas tienes ahora y cuáles quieres añadir o quitar.");
        paso(db, id, 2, "Pulsa Solicitar", "En observaciones indica las asignaturas que añades y las que quitas.");
        paso(db, id, 3, "Haz una foto de tu DNI", "Fotografía tu DNI o NIE para identificarte.");
        paso(db, id, 4, "Firma y envía", "Firma con el dedo y mantén pulsado Enviar. Sigue el estado en Mis gestiones.");
        documento(db, id, "DNI o NIE", "Cara de la foto, legible y sin reflejos.", true);

        id = tramite(db, "Anulación de matrícula", "matricula",
                "Anula total o parcialmente la matrícula del curso, con o sin devolución de precios públicos.",
                "Solicitarlo dentro de plazo. Para anular fuera de plazo hay que justificar causa sobrevenida.",
                "Anulación con devolución hasta el 31 de diciembre; después solo por causa justificada.",
                null, null, ONLINE,
                "anular, darme de baja, baja, dejar el grado, cancelar matricula, devolucion");
        paso(db, id, 1, "Elige el tipo de anulación", "Decide si anulas toda la matrícula o solo algunas asignaturas.");
        paso(db, id, 2, "Pulsa Solicitar", "En observaciones indica qué anulas y el motivo.");
        paso(db, id, 3, "Haz fotos de los documentos", "Fotografía tu DNI y, si estás fuera de plazo, el documento que justifique la causa.");
        paso(db, id, 4, "Firma y envía", "Firma con el dedo y mantén pulsado Enviar.");
        paso(db, id, 5, "Consulta la resolución", "En Mis gestiones verás cuándo tu solicitud está resuelta.");
        documento(db, id, "DNI o NIE", "Cara de la foto, legible y sin reflejos.", true);
        documento(db, id, "Justificante de la causa", "Informe médico, contrato... Solo si anulas fuera de plazo.", false);

        // ---------- TÍTULOS ----------
        id = tramite(db, "Solicitud del título de grado y SET", "titulos",
                "Solicitud de expedición del título oficial de grado y del Suplemento Europeo al Título (SET).",
                "Haber superado todos los créditos del grado, incluido el TFG, y acreditar el nivel B1 de idioma.",
                "Se puede solicitar en cualquier momento una vez superados todos los créditos.",
                null, null, ONLINE,
                "titulo, graduarme, terminar la carrera, suplemento europeo, SET, expedicion titulo");
        paso(db, id, 1, "Comprueba tus requisitos", "Verifica que tienes todos los créditos superados y el idioma acreditado.");
        paso(db, id, 2, "Pulsa Solicitar", "Tus datos ya aparecen rellenos; añade observaciones si las hay.");
        paso(db, id, 3, "Haz fotos de los documentos", "Fotografía tu DNI y el certificado de idioma B1.");
        paso(db, id, 4, "Firma y envía", "Firma con el dedo y mantén pulsado Enviar. El justificante sustituye al título mientras se expide.");
        paso(db, id, 5, "Recoge el título", "Cuando la solicitud esté resuelta, pide cita desde la app y recoge el título en secretaría.");
        documento(db, id, "DNI o NIE", "Cara de la foto, legible y sin reflejos.", true);
        documento(db, id, "Certificado de idioma B1", "Solo si no lo tienes ya reconocido en tu expediente.", false);

        // ---------- RECONOCIMIENTO DE CRÉDITOS ----------
        id = tramite(db, "Reconocimiento y transferencia de créditos", "reconocimiento_creditos",
                "Reconocimiento de asignaturas cursadas en otros estudios universitarios o de FP superior.",
                "Certificado académico de los estudios de origen y programas de las asignaturas sellados. Se abona el 30 % de los precios públicos.",
                "Plazo fijado por la escuela; la resolución llega en un máximo de tres meses.",
                abiertoDesde, abiertoHasta, ONLINE,
                "convalidar, convalidacion, convalidar asignaturas, reconocer creditos, transferencia, ciclo superior");
        paso(db, id, 1, "Reúne la documentación", "Ten a mano el certificado académico y los programas sellados de las asignaturas de origen.");
        paso(db, id, 2, "Pulsa Solicitar", "En observaciones indica qué asignatura de origen corresponde a cada asignatura del grado.");
        paso(db, id, 3, "Haz fotos de los documentos", "Fotografía el certificado académico y los programas, una foto por documento.");
        paso(db, id, 4, "Firma y envía", "Firma con el dedo y mantén pulsado Enviar.");
        paso(db, id, 5, "Consulta la resolución", "La comisión estudia tu solicitud; en Mis gestiones verás cuándo está resuelta.");
        documento(db, id, "Certificado académico de origen", "Con las asignaturas superadas y sus calificaciones.", true);
        documento(db, id, "Programas de las asignaturas", "Sellados por el centro de origen.", true);
        documento(db, id, "DNI o NIE", "Cara de la foto, legible y sin reflejos.", true);

        id = tramite(db, "Créditos por actividades universitarias", "reconocimiento_creditos",
                "Reconocimiento de hasta 6 ECTS por actividades culturales, deportivas, de representación o solidarias.",
                "Certificado de la actividad realizada con las horas o créditos reconocidos. Estos créditos no tienen nota ni cuentan en la media.",
                "Se puede solicitar durante todo el curso.",
                null, null, ONLINE,
                "creditos optativos, actividades, cursos, voluntariado, deporte, creditos de libre configuracion");
        paso(db, id, 1, "Consigue el certificado", "Pide el certificado de la actividad al organizador.");
        paso(db, id, 2, "Pulsa Solicitar y haz la foto", "Fotografía el certificado de la actividad.");
        paso(db, id, 3, "Firma y envía", "Firma con el dedo y mantén pulsado Enviar.");
        documento(db, id, "Certificado de la actividad", "Debe indicar las horas o créditos reconocidos.", true);

        // ---------- CONVOCATORIAS ----------
        // plazo cerrado
        id = tramite(db, "Evaluación única final", "convocatorias",
                "Solicitud para ser evaluado mediante una única prueba final en lugar de la evaluación continua.",
                "Causa justificada (trabajo, enfermedad, deporte de alto nivel, etc.) que impida seguir la evaluación continua.",
                "Durante las dos primeras semanas de impartición de la asignatura.",
                cerradoDesde, cerradoHasta, ONLINE,
                "examen unico, evaluacion final, no puedo ir a clase, trabajo, evaluacion unica");
        paso(db, id, 1, "Pulsa Solicitar", "En observaciones indica las asignaturas y el motivo.");
        paso(db, id, 2, "Haz una foto del justificante", "Fotografía el documento que acredite el motivo (contrato, informe médico...).");
        paso(db, id, 3, "Firma y envía", "Firma con el dedo y mantén pulsado Enviar.");
        paso(db, id, 4, "Espera la respuesta", "Consulta el estado en Mis gestiones antes de la fecha del examen.");
        documento(db, id, "Justificante del motivo", "Contrato de trabajo, informe médico, acreditación deportiva...", true);

        // plazo aún no abierto
        id = tramite(db, "Convocatoria especial", "convocatorias",
                "Convocatoria adicional para estudiantes a los que les quedan pocos créditos para terminar.",
                "Tener pendientes como máximo el TFG y un número limitado de créditos.",
                "Solicitud en noviembre; exámenes en diciembre.",
                proximoDesde, proximoHasta, ONLINE,
                "convocatoria extraordinaria, examen diciembre, ultimas asignaturas, terminar");
        paso(db, id, 1, "Comprueba si cumples requisitos", "Revisa los créditos que te faltan para terminar.");
        paso(db, id, 2, "Pulsa Solicitar", "En observaciones indica las asignaturas de las que te examinarás.");
        paso(db, id, 3, "Firma y envía", "Haz la foto de tu DNI, firma con el dedo y mantén pulsado Enviar.");
        documento(db, id, "DNI o NIE", "Cara de la foto, legible y sin reflejos.", true);

        // ---------- CERTIFICADOS ----------
        id = tramite(db, "Certificado académico personal", "certificados",
                "Certificado oficial con las asignaturas superadas, calificaciones y nota media.",
                "Pago de las tasas del certificado.",
                "En cualquier momento.",
                null, null, ONLINE,
                "certificado de notas, expediente, nota media, calificaciones, certificado");
        paso(db, id, 1, "Pulsa Solicitar", "Indica en observaciones para qué lo necesitas (beca, trabajo...).");
        paso(db, id, 2, "Haz fotos de los documentos", "Fotografía tu DNI y el justificante de pago de las tasas.");
        paso(db, id, 3, "Firma y envía", "Firma con el dedo y mantén pulsado Enviar. Cuando esté resuelta, pide cita para recogerlo.");
        documento(db, id, "DNI o NIE", "Cara de la foto, legible y sin reflejos.", true);
        documento(db, id, "Justificante de pago", "Recibo del banco o del pago con tarjeta.", true);

        // ---------- OTROS ----------
        // solo presencial: sin botón Solicitar, solo "Pedir cita"
        id = tramite(db, "Cambio de grupo", "otro",
                "Solicitud de cambio de grupo de teoría o prácticas por motivos justificados. Se tramita en ventanilla.",
                "Motivo justificado (trabajo, conciliación, solapamiento de horarios).",
                "Primeras semanas de cada cuatrimestre.",
                null, null, PRESENCIAL,
                "grupo, horario, cambiar de grupo, permuta, cambio de turno");
        paso(db, id, 1, "Consulta los grupos libres", "Comprueba en los horarios qué grupos tienen plazas.");
        paso(db, id, 2, "Pide cita", "Pulsa Pedir cita y elige el día y la hora que te vengan bien.");
        paso(db, id, 3, "Prepara el justificante", "Lleva el documento que justifique el motivo (contrato, horario...).");
        paso(db, id, 4, "Enseña el QR en ventanilla", "Abre el justificante de la cita en Mis gestiones y enséñalo en secretaría.");
        documento(db, id, "Justificante del motivo", "Contrato de trabajo, horario que se solapa...", true);
    }

    // -------------------------------------------------
    //              INSERCIONES (con db de onCreate)
    // -------------------------------------------------
    private static long tramite(SQLiteDatabase db, String nombre, String categoria, String descripcion,
                                String requisitos, String plazosInfo, String plazoInicio, String plazoFin,
                                boolean solicitudOnline, String palabrasClave) {
        ContentValues cv = new ContentValues();
        cv.put("nombreProcedimiento", nombre);
        cv.put("categoria", categoria);
        cv.put("descripcion", descripcion);
        cv.put("requisitos", requisitos);
        cv.put("plazosInfo", plazosInfo);
        cv.put("plazoInicio", plazoInicio);
        cv.put("plazoFin", plazoFin);
        cv.put("solicitudOnline", solicitudOnline ? 1 : 0);
        cv.put("palabrasClave", palabrasClave);
        cv.put("textoBusqueda", TextoUtil.textoBusqueda(nombre, palabrasClave, descripcion));
        return db.insert("PROCEDIMIENTO", null, cv);
    }

    private static void paso(SQLiteDatabase db, long idProcedimiento, int orden, String titulo, String descripcion) {
        ContentValues cv = new ContentValues();
        cv.put("idProcedimiento", idProcedimiento);
        cv.put("orden", orden);
        cv.put("tituloPaso", titulo);
        cv.put("descripcionPaso", descripcion);
        db.insert("PROCEDIMIENTO_PASO", null, cv);
    }

    private static void documento(SQLiteDatabase db, long idProcedimiento, String nombre,
                                  String descripcion, boolean obligatorio) {
        ContentValues cv = new ContentValues();
        cv.put("idProcedimiento", idProcedimiento);
        cv.put("nombreDocumento", nombre);
        cv.put("descripcion", descripcion);
        cv.put("obligatorio", obligatorio ? 1 : 0);
        db.insert("PROCEDIMIENTO_DOCUMENTO", null, cv);
    }
}
