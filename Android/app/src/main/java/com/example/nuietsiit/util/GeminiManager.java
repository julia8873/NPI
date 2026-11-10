package com.example.nuietsiit.util;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import androidx.core.content.ContextCompat;

import com.example.nuietsiit.BuildConfig;
import com.example.nuietsiit.data.AcademicoDB;
import com.example.nuietsiit.data.ActividadesDB;
import com.example.nuietsiit.data.BibliotecaDB;
import com.example.nuietsiit.data.Comedores.Comedor;
import com.example.nuietsiit.data.Comedores.ComedoresDB;
import com.example.nuietsiit.data.Comedores.MenuInfo;
import com.example.nuietsiit.data.Comedores.TablasComedores;
import com.example.nuietsiit.data.DBHelper;
import com.example.nuietsiit.data.SecretariaDB;
import com.example.nuietsiit.data.TablasAcademico;
import com.example.nuietsiit.data.TablasActividades;
import com.example.nuietsiit.data.TablasBiblioteca;
import com.example.nuietsiit.data.TablasSecretaria;
import com.example.nuietsiit.data.TutoriasDB;
import com.example.nuietsiit.data.UsuariosDB;
import com.example.nuietsiit.data.modelo.Actividad;
import com.example.nuietsiit.data.modelo.Libro;
import com.example.nuietsiit.data.modelo.Tramite;
import com.google.ai.client.generativeai.GenerativeModel;
import com.google.ai.client.generativeai.java.ChatFutures;
import com.google.ai.client.generativeai.java.GenerativeModelFutures;
import com.google.ai.client.generativeai.type.Content;
import com.google.ai.client.generativeai.type.FunctionCallPart;
import com.google.ai.client.generativeai.type.FunctionResponsePart;
import com.google.ai.client.generativeai.type.GenerateContentResponse;
import com.google.ai.client.generativeai.type.GenerationConfig;
import com.google.ai.client.generativeai.type.Part;
import com.google.ai.client.generativeai.type.RequestOptions;
import com.google.ai.client.generativeai.type.SafetySetting;
import com.google.ai.client.generativeai.type.Tool;
import com.google.ai.client.generativeai.type.ToolConfig;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Executor;

/**
 * Gestor del modelo de Inteligencia Artificial Gemini para la NUI de la ETSIIT.
 * Inyecta todo el contenido de TablasAcademico, TablasActividades, TablasBiblioteca, TablasSecretaria y TablasComedores
 * directamente en el contexto del modelo para garantizar respuestas instantáneas, precisas y sin alucinaciones.
 */
public class GeminiManager {

    private final GenerativeModelFutures modelFutures;
    private ChatFutures chatSession;
    private final Executor mainExecutor;
    private final Context contextRef;
    private final String correoUsuarioRef;

    public GeminiManager(Context context, String correoUsuario, List<Tool> herramientas) {
        this.contextRef = context.getApplicationContext();
        this.correoUsuarioRef = correoUsuario;
        this.mainExecutor = ContextCompat.getMainExecutor(context);

        Content systemInstruction = construirSystemInstruction(context, correoUsuario);

        GenerationConfig genConfig = new GenerationConfig.Builder().build();
        List<SafetySetting> safetySettings = null;
        RequestOptions reqOptions = new RequestOptions();
        ToolConfig toolConfig = null;

        GenerativeModel gm = new GenerativeModel(
                "gemini-3.5-flash-lite",
                BuildConfig.GEMINI_API_KEY,
                genConfig,
                safetySettings,
                reqOptions,
                herramientas,
                toolConfig,
                systemInstruction
        );

        this.modelFutures = GenerativeModelFutures.from(gm);
        this.chatSession = modelFutures.startChat();
    }

    private String cargarBaseDeDatosComoTexto(Context context) {
        StringBuilder sb = new StringBuilder();
        try {
            DBHelper dbHelper = DBHelper.getInstance(context);
            SQLiteDatabase db = dbHelper.getWritableDatabase();

            // 1. Asegurar que TODAS las tablas de los ficheros Tablas* estén pobladas en SQLite
            TablasComedores.poblar(db);
            TablasActividades.poblar(db);
            TablasSecretaria.poblar(db);
            TablasBiblioteca.poblar(db);
            TablasAcademico.poblar(db);

            // 2. Menús de Comedores
            sb.append("\n[REGISTROS DE COMEDORES UGR]\n");
            try (Cursor c = db.rawQuery("SELECT C.nombreComedor, M.fecha, M.modalidad, P.nombrePlato, MP.categoria FROM MENU M JOIN COMEDOR C ON M.idComedor = C.idComedor JOIN MENU_PLATO MP ON M.idMenu = MP.idMenu JOIN PLATO P ON MP.idPlato = P.idPlato ORDER BY M.fecha ASC", null)) {
                while (c != null && c.moveToNext()) {
                    sb.append("- Comedor: ").append(c.getString(0))
                      .append(" | Fecha: ").append(c.getString(1))
                      .append(" | Menú: ").append(c.getString(2))
                      .append(" | ").append(c.getString(4)).append(": ").append(c.getString(3)).append("\n");
                }
            }

            // 3. Actividades (TablasActividades)
            sb.append("\n[ACTIVIDADES Y CURSOS - TABLAS ACTIVIDADES]\n");
            try (Cursor c = db.rawQuery("SELECT titulo, tipoActividad, fechaInicio, lugar, descripcion FROM ACTIVIDAD", null)) {
                while (c != null && c.moveToNext()) {
                    sb.append("- Actividad: ").append(c.getString(0))
                      .append(" | Tipo: ").append(c.getString(1))
                      .append(" | Fecha: ").append(c.getString(2))
                      .append(" | Lugar: ").append(c.getString(3))
                      .append(" | Desc: ").append(c.getString(4)).append("\n");
                }
            }

            // 4. Académico, Profesores y Asignaturas (TablasAcademico)
            sb.append("\n[PROFESORES Y ASIGNATURAS - TABLAS ACADÉMICO]\n");
            try (Cursor c = db.rawQuery("SELECT P.nombreProfesor, P.correoProfesor, A.nombreAsignatura, S.horaInicio, S.horaFin, S.diaSemana FROM PROFESOR P LEFT JOIN SESION_CLASE S ON P.correoProfesor = S.correoProfesor LEFT JOIN ASIGNATURA A ON S.codigoAsignatura = A.codigoAsignatura", null)) {
                while (c != null && c.moveToNext()) {
                    sb.append("- Prof. ").append(c.getString(0))
                      .append(" (Email: ").append(c.getString(1)).append(")")
                      .append(" | Imparte: ").append(c.getString(2) != null ? c.getString(2) : "Sin asignatura")
                      .append(" | Hora: ").append(c.getString(3) != null ? (c.getString(3) + " - " + c.getString(4)) : "N/A").append("\n");
                }
            }

            // 5. Biblioteca (TablasBiblioteca)
            sb.append("\n[BIBLIOTECA - TABLAS BIBLIOTECA]\n");
            try (Cursor c = db.rawQuery("SELECT titulo, autores, isbn FROM LIBRO", null)) {
                while (c != null && c.moveToNext()) {
                    sb.append("- Libro: ").append(c.getString(0)).append(" de ").append(c.getString(1)).append(" (ISBN: ").append(c.getString(2)).append(")\n");
                }
            }

            // 6. Trámites Secretaría (TablasSecretaria)
            sb.append("\n[TRÁMITES SECRETARÍA - TABLAS SECRETARÍA]\n");
            try (Cursor c = db.rawQuery("SELECT nombre, descripcion, requisitos FROM TRAMITE", null)) {
                while (c != null && c.moveToNext()) {
                    sb.append("- Trámite: ").append(c.getString(0)).append(" | Desc: ").append(c.getString(1)).append(" | Requisitos: ").append(c.getString(2)).append("\n");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return sb.toString();
    }

    private Content construirSystemInstruction(Context context, String correoUsuario) {
        UsuariosDB usuariosDB = new UsuariosDB(context);
        String nombre = null;
        String grado = null;

        if (correoUsuario != null && !correoUsuario.isEmpty()) {
            nombre = usuariosDB.obtenerNombreUsuario(correoUsuario);
            grado = usuariosDB.obtenerGradoUsuario(correoUsuario);
        }

        if (nombre == null || nombre.isEmpty()) {
            nombre = "Estudiante";
        }
        if (grado == null || grado.isEmpty()) {
            grado = "No especificado";
        }

        SimpleDateFormat sdfFecha = new SimpleDateFormat("EEEE, d 'de' MMMM 'de' yyyy", new Locale("es", "ES"));
        String fechaHoy = sdfFecha.format(new Date());

        String datosBd = cargarBaseDeDatosComoTexto(context);

        String promptText = "Eres el Asistente Virtual Oficial de la ETSIIT (Escuela Técnica Superior de " +
                "Ingenierías Informática y de Telecomunicación) de la Universidad de Granada (UGR).\n\n" +
                "REGLAS INVIOLABLES DE RESPUESTA - CERO ALUCINACIONES:\n" +
                "1. Queda ESTRICTAMENTE PROHIBIDO inventar datos, menús de comida, platos, profesores, despachos, tutorías, asignaturas, horarios de clase, libros o trámites.\n" +
                "2. Toda la información institucional, menús, actividades, profesores, asignaturas, biblioteca y secretaría facilitada a continuación proviene directamente de los scripts de población (TablasComedores, TablasActividades, TablasAcademico, TablasBiblioteca, TablasSecretaria). Debes responder basándote EXCLUSIVAMENTE en estos datos.\n" +
                "3. NUNCA muestres nombres de funciones en crudo ni escribas textos como 'consultarMenuComedor(...)'. Responde siempre directamente en lenguaje natural y con viñetas amables.\n\n" +
                "=== REGISTROS OFICIALES DE LA FACULTAD (DATOS DE LAS TABLAS) ===\n" +
                datosBd + "\n" +
                "===============================================================\n\n" +
                "=== CONTEXTO DEL USUARIO EN SESIÓN ===\n" +
                "- Usuario actual: " + nombre + " (" + (correoUsuario != null ? correoUsuario : "Invitado") + ")\n" +
                "- Grado académico: " + grado + "\n" +
                "- Fecha actual en el sistema: " + fechaHoy + "\n" +
                "=======================================\n";

        Content.Builder builder = new Content.Builder();
        builder.setRole("system");
        builder.addText(promptText);
        return builder.build();
    }

    public void enviarMensaje(String textoMensaje, GeminiCallback callback) {
        Content contenidoMensaje = new Content.Builder().addText(textoMensaje).build();
        enviarContenido(chatSession.sendMessage(contenidoMensaje), callback);
    }

    private void enviarContenido(ListenableFuture<GenerateContentResponse> responseFuture, GeminiCallback callback) {
        Futures.addCallback(responseFuture, new FutureCallback<GenerateContentResponse>() {
            @Override
            public void onSuccess(GenerateContentResponse result) {
                List<FunctionCallPart> functionCalls = result.getFunctionCalls();
                if (functionCalls != null && !functionCalls.isEmpty()) {
                    FunctionCallPart call = functionCalls.get(0);
                    String funcName = call.getName();
                    Map<String, String> args = call.getArgs();

                    Map<String, Object> toolResult = ejecutarFuncionLocal(contextRef, correoUsuarioRef, funcName, args);

                    FunctionResponsePart responsePart = new FunctionResponsePart(funcName, new JSONObject(toolResult));
                    Content contentResponse = new Content("function", Collections.singletonList(responsePart));

                    ListenableFuture<GenerateContentResponse> nextFuture = chatSession.sendMessage(contentResponse);
                    enviarContenido(nextFuture, callback);
                } else {
                    callback.onRespuesta(result);
                }
            }

            @Override
            public void onFailure(Throwable t) {
                callback.onError(t);
            }
        }, mainExecutor);
    }

    private Map<String, Object> ejecutarFuncionLocal(Context context, String correo, String funcName, Map<String, String> args) {
        Map<String, Object> resultado = new HashMap<>();
        DBHelper dbHelper = DBHelper.getInstance(context);
        if (args == null) args = new HashMap<>();

        try {
            switch (funcName) {
                case "apuntarAActividad": {
                    int idAct = parsearInt(args.get("idActividad"), 1);
                    ActividadesDB dao = new ActividadesDB(dbHelper);
                    String fechaHoraISO = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
                    boolean ok = dao.insertarInscripcion(correo, idAct, fechaHoraISO);
                    resultado.put("estado", ok ? "éxito" : "fallo");
                    resultado.put("mensaje", ok ? "Inscripción realizada correctamente." : "No se pudo realizar la inscripción.");
                    break;
                }
                case "desapuntarDeActividad": {
                    int idAct = parsearInt(args.get("idActividad"), 1);
                    ActividadesDB dao = new ActividadesDB(dbHelper);
                    boolean ok = dao.borrarInscripcion(correo, idAct);
                    resultado.put("estado", ok ? "éxito" : "fallo");
                    resultado.put("mensaje", ok ? "Inscripción cancelada." : "No se encontró la inscripción.");
                    break;
                }
                case "listarActividades": {
                    ActividadesDB dao = new ActividadesDB(dbHelper);
                    String hoyISO = LocalDate.now().toString();
                    List<Actividad> lista = dao.listarActividadesConDetalles(correo, hoyISO);
                    resultado.put("actividades", lista);
                    break;
                }
                case "buscarProfesor": {
                    String nombreProf = args.get("nombreProfesor") != null ? args.get("nombreProfesor") : "";
                    UsuariosDB dao = new UsuariosDB(dbHelper);
                    TutoriasDB tutoriasDB = new TutoriasDB(dbHelper);
                    List<Map<String, Object>> profs = new ArrayList<>();

                    try (Cursor c = dao.buscarProfesoresPorTexto(nombreProf)) {
                        while (c != null && c.moveToNext()) {
                            Map<String, Object> p = new HashMap<>();
                            String correoProf = c.getString(c.getColumnIndexOrThrow("correoProfesor"));
                            String nombreProfesor = c.getString(c.getColumnIndexOrThrow("nombreProfesor"));
                            String web = c.getString(c.getColumnIndexOrThrow("enlacePaginaWeb"));

                            p.put("correo", correoProf);
                            p.put("nombre", nombreProfesor);
                            p.put("webDocente", web != null ? web : "No especificada");

                            try (Cursor cDesp = dao.obtenerDespachosDe(correoProf)) {
                                if (cDesp != null && cDesp.moveToFirst()) {
                                    p.put("despacho", cDesp.getString(cDesp.getColumnIndexOrThrow("nombreEspacio")));
                                } else {
                                    p.put("despacho", "Sin despacho asignado");
                                }
                            }

                            try (Cursor cTut = tutoriasDB.listarHorariosDe(correoProf)) {
                                List<String> horTutorias = new ArrayList<>();
                                while (cTut != null && cTut.moveToNext()) {
                                    int dia = cTut.getInt(cTut.getColumnIndexOrThrow("diaSemana"));
                                    String ini = cTut.getString(cTut.getColumnIndexOrThrow("horaInicio"));
                                    String fin = cTut.getString(cTut.getColumnIndexOrThrow("horaFin"));
                                    String mod = cTut.getString(cTut.getColumnIndexOrThrow("modalidad"));
                                    horTutorias.add("Día " + dia + " (" + ini + " - " + fin + ", " + mod + ")");
                                }
                                p.put("tutorias", horTutorias);
                            }

                            profs.add(p);
                        }
                    }
                    resultado.put("profesores", profs);
                    break;
                }
                case "reservarTutoria": {
                    String fecha = args.get("fecha") != null ? args.get("fecha") : LocalDate.now().toString();
                    int idHorario = parsearInt(args.get("idHorario"), 1);
                    String motivo = args.get("motivo") != null ? args.get("motivo") : "Consulta de asignatura";
                    TutoriasDB dao = new TutoriasDB(dbHelper);
                    if (dao.slotOcupado(idHorario, fecha)) {
                        resultado.put("estado", "ocupado");
                        resultado.put("mensaje", "Esta franja ya está reservada.");
                    } else {
                        long idReserva = dao.insertarReserva(idHorario, correo, fecha, motivo, null);
                        resultado.put("estado", idReserva != -1 ? "éxito" : "fallo");
                        resultado.put("mensaje", idReserva != -1 ? "Tutoría reservada para el " + fecha : "Error al reservar.");
                    }
                    break;
                }
                case "consultarMenuComedor": {
                    String nombreComedor = args.get("nombreComedor") != null ? args.get("nombreComedor") : "ETSIIT";
                    String fecha = args.get("fecha");
                    if (fecha == null || fecha.isEmpty()) {
                        fecha = LocalDate.now().toString();
                    }

                    TablasComedores.poblar(dbHelper.getWritableDatabase());
                    ComedoresDB dao = new ComedoresDB(dbHelper);
                    dao.asegurarMenusSimulacion();

                    List<Comedor> comedores = dao.obtenerListaComedores();
                    int idComedorEncontrado = 1;
                    String nombreOficialComedor = "Comedor ETSIIT (Aynadamar)";

                    String busquedaLower = nombreComedor.toLowerCase();
                    for (Comedor com : comedores) {
                        String comNombreLower = com.getNombreComedor().toLowerCase();
                        if (comNombreLower.contains(busquedaLower) || busquedaLower.contains(comNombreLower) ||
                            (busquedaLower.contains("aynadamar") && com.getIdComedor() == 1) ||
                            (busquedaLower.contains("etsiit") && com.getIdComedor() == 1) ||
                            (busquedaLower.contains("cartuja") && com.getIdComedor() == 4) ||
                            (busquedaLower.contains("fuentenueva") && com.getIdComedor() == 2) ||
                            (busquedaLower.contains("ciencias") && com.getIdComedor() == 3)) {
                            idComedorEncontrado = com.getIdComedor();
                            nombreOficialComedor = com.getNombreComedor();
                            break;
                        }
                    }

                    MenuInfo menuNormal = dao.obtenerMenuParaComedorYFecha(idComedorEncontrado, fecha, false);
                    MenuInfo menuVeg = dao.obtenerMenuParaComedorYFecha(idComedorEncontrado, fecha, true);

                    if ((menuNormal == null || "No disponible".equals(menuNormal.getPrimerPlato())) && !fecha.equals("2026-11-03")) {
                        MenuInfo fallbackNormal = dao.obtenerMenuParaComedorYFecha(idComedorEncontrado, "2026-11-03", false);
                        MenuInfo fallbackVeg = dao.obtenerMenuParaComedorYFecha(idComedorEncontrado, "2026-11-03", true);
                        if (fallbackNormal != null && !"No disponible".equals(fallbackNormal.getPrimerPlato())) {
                            menuNormal = fallbackNormal;
                            menuVeg = fallbackVeg;
                            fecha = fecha + " (Menú registrado en BD)";
                        }
                    }

                    resultado.put("comedor", nombreOficialComedor);
                    resultado.put("fecha", fecha);
                    resultado.put("primerPlato", menuNormal != null ? menuNormal.getPrimerPlato() : "No disponible");
                    resultado.put("segundoPlato", menuNormal != null ? menuNormal.getSegundoPlato() : "No disponible");
                    resultado.put("postre", menuNormal != null ? menuNormal.getPostre() : "No disponible");
                    resultado.put("primerPlatoVegetariano", menuVeg != null ? menuVeg.getPrimerPlato() : "No disponible");
                    resultado.put("segundoPlatoVegetariano", menuVeg != null ? menuVeg.getSegundoPlato() : "No disponible");
                    resultado.put("postreVegetariano", menuVeg != null ? menuVeg.getPostre() : "No disponible");
                    break;
                }
                case "buscarLibroBiblioteca": {
                    String query = args.get("query") != null ? args.get("query") : "";
                    BibliotecaDB dao = new BibliotecaDB(dbHelper);
                    List<Libro> libros = dao.buscarLibros(query);
                    resultado.put("libros", libros);
                    break;
                }
                case "consultarTramitesSecretaria": {
                    String categoria = args.get("categoria");
                    SecretariaDB dao = new SecretariaDB(dbHelper);
                    List<Tramite> tramites = dao.getTramites(categoria);
                    resultado.put("tramites", tramites);
                    break;
                }
                case "consultarHorarioEstudiante": {
                    AcademicoDB academicoDB = new AcademicoDB(dbHelper);
                    List<Map<String, String>> sesiones = new ArrayList<>();
                    if (correo != null && !correo.isEmpty()) {
                        try (Cursor c = academicoDB.listarSesionesDe(correo, "2026/2027")) {
                            while (c != null && c.moveToNext()) {
                                Map<String, String> s = new HashMap<>();
                                s.put("codigo", c.getString(c.getColumnIndexOrThrow("codigoAsignatura")));
                                s.put("nombre", c.getString(c.getColumnIndexOrThrow("nombreAsignatura")));
                                s.put("diaSemana", String.valueOf(c.getInt(c.getColumnIndexOrThrow("diaSemana"))));
                                s.put("horaInicio", c.getString(c.getColumnIndexOrThrow("horaInicio")));
                                s.put("horaFin", c.getString(c.getColumnIndexOrThrow("horaFin")));
                                s.put("grupo", c.getString(c.getColumnIndexOrThrow("grupo")));
                                s.put("tipo", c.getString(c.getColumnIndexOrThrow("tipoSesion")));
                                sesiones.add(s);
                            }
                        }
                    }
                    resultado.put("sesionesHorario", sesiones);
                    break;
                }
                case "consultarAsignatura": {
                    String queryAsig = args.get("query") != null ? args.get("query") : "";
                    AcademicoDB academicoDB = new AcademicoDB(dbHelper);
                    UsuariosDB usuariosDB = new UsuariosDB(dbHelper);

                    Map<String, Object> infoAsig = new HashMap<>();
                    try (Cursor cAsig = academicoDB.obtenerAsignatura(queryAsig)) {
                        if (cAsig != null && cAsig.moveToFirst()) {
                            String cod = cAsig.getString(0);
                            infoAsig.put("codigo", cod);
                            infoAsig.put("nombre", cAsig.getString(1));
                            infoAsig.put("cuatrimestre", cAsig.getInt(2));
                            infoAsig.put("creditos", cAsig.getDouble(3));
                            infoAsig.put("guiaDocente", cAsig.getString(4));

                            try (Cursor cProf = usuariosDB.obtenerProfesoresDeAsignatura(cod)) {
                                List<String> profsList = new ArrayList<>();
                                while (cProf != null && cProf.moveToFirst()) {
                                    profsList.add(cProf.getString(cProf.getColumnIndexOrThrow("nombreProfesor")) + " (" + cProf.getString(cProf.getColumnIndexOrThrow("correoProfesor")) + ")");
                                }
                                infoAsig.put("profesores", profsList);
                            }
                        } else {
                            infoAsig.put("mensaje", "Asignatura no encontrada.");
                        }
                    }
                    resultado.put("asignatura", infoAsig);
                    break;
                }
                case "consultarBaseGeneral": {
                    String query = args.get("query") != null ? args.get("query") : "";
                    Map<String, Object> resultadosGen = new HashMap<>();
                    AcademicoDB acDb = new AcademicoDB(dbHelper);
                    UsuariosDB usDb = new UsuariosDB(dbHelper);
                    BibliotecaDB bibDb = new BibliotecaDB(dbHelper);
                    SecretariaDB secDb = new SecretariaDB(dbHelper);

                    resultadosGen.put("asignaturas", acDb.buscarAsignaturasPorNombre(query));
                    resultadosGen.put("profesores", usDb.buscarProfesoresPorTexto(query));
                    resultadosGen.put("libros", bibDb.buscarLibros(query));
                    resultadosGen.put("tramites", secDb.getTramites(query));
                    resultado.put("resultadoGeneral", resultadosGen);
                    break;
                }
                default:
                    resultado.put("error", "Función desconocida: " + funcName);
                    break;
            }
        } catch (Exception e) {
            e.printStackTrace();
            resultado.put("error", e.getMessage() != null ? e.getMessage() : "Error");
        }

        return resultado;
    }

    private int parsearInt(String valor, int valorPorDefecto) {
        if (valor == null) return valorPorDefecto;
        try {
            return Integer.parseInt(valor.trim());
        } catch (Exception e) {
            return valorPorDefecto;
        }
    }

    public void reiniciarChat() {
        this.chatSession = modelFutures.startChat();
    }

    public interface GeminiCallback {
        void onRespuesta(GenerateContentResponse respuesta);
        void onError(Throwable error);
    }
}
