package com.example.nuietsiit.util;

import android.content.Context;
import android.database.Cursor;
import androidx.core.content.ContextCompat;

import com.example.nuietsiit.BuildConfig;
import com.example.nuietsiit.data.ActividadesDB;
import com.example.nuietsiit.data.BibliotecaDB;
import com.example.nuietsiit.data.Comedores.Comedor;
import com.example.nuietsiit.data.Comedores.ComedoresDB;
import com.example.nuietsiit.data.Comedores.MenuInfo;
import com.example.nuietsiit.data.DBHelper;
import com.example.nuietsiit.data.SecretariaDB;
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

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Executor;

/**
 * Gestor del modelo de Inteligencia Artificial Gemini para la NUI de la ETSIIT.
 * Encapsula GenerativeModel, GenerativeModelFutures, sesión de Chat y Function Calling.
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
                "gemini-3.5-flash",
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

        String promptText = "Eres el Asistente Virtual Oficial de la ETSIIT (Escuela Técnica Superior de " +
                "Ingenierías Informática y de Telecomunicación) de la Universidad de Granada (UGR).\n\n" +
                "TU OBJETIVO PRINCIPAL:\n" +
                "Ayudar a los estudiantes, profesores y personal de la ETSIIT respondiendo dudas sobre la facultad " +
                "(asignaturas, profesores, horarios, comedores, biblioteca, trámites y planos/ubicaciones) y ejecutando " +
                "acciones dentro de la aplicación móvil cuando el usuario lo solicite mediante Function Calling.\n\n" +
                "TONO Y ESTILO DE RESPUESTA:\n" +
                "1. Sé siempre amable, cercano, conciso y profesional.\n" +
                "2. Mantén tus respuestas breves y estructuradas (usa viñetas).\n" +
                "3. Cuando realices una acción (como apuntarse a una actividad o reservar tutoría), confirma claramente el resultado al usuario.\n\n" +
                "=== CONTEXTO DEL USUARIO EN SESIÓN ===\n" +
                "- Usuario actual: " + nombre + " (" + (correoUsuario != null ? correoUsuario : "Invitado") + ")\n" +
                "- Grado académico: " + grado + "\n" +
                "- Fecha actual en el sistema: " + fechaHoy + "\n" +
                "=======================================\n\n" +
                "Dirígete al usuario por su nombre de forma natural cuando sea oportuno (" + nombre + ").";

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

                    // Ejecutar la acción local en SQLite
                    Map<String, Object> toolResult = ejecutarFuncionLocal(contextRef, correoUsuarioRef, funcName, args);

                    // Devolver el resultado de la función a Gemini en un Content con FunctionResponsePart
                    FunctionResponsePart responsePart = new FunctionResponsePart(funcName, new org.json.JSONObject(toolResult));
                    Content contentResponse = new Content(null, Collections.singletonList((Part) responsePart));
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
                    String fechaHoraISO = java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
                    boolean ok = dao.insertarInscripcion(correo, idAct, fechaHoraISO);
                    resultado.put("estado", ok ? "éxito" : "fallo");
                    resultado.put("mensaje", ok ? "Inscripción realizada correctamente." : "No se pudo realizar la inscripción (quizá ya estés inscrito).");
                    break;
                }
                case "desapuntarDeActividad": {
                    int idAct = parsearInt(args.get("idActividad"), 1);
                    ActividadesDB dao = new ActividadesDB(dbHelper);
                    boolean ok = dao.borrarInscripcion(correo, idAct);
                    resultado.put("estado", ok ? "éxito" : "fallo");
                    resultado.put("mensaje", ok ? "Inscripción cancelada correctamente." : "No se encontró la inscripción a esta actividad.");
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
                    try (Cursor c = dao.buscarProfesoresPorTexto(nombreProf)) {
                        List<Map<String, String>> profs = new java.util.ArrayList<>();
                        while (c != null && c.moveToNext()) {
                            Map<String, String> p = new HashMap<>();
                            p.put("correo", c.getString(c.getColumnIndexOrThrow("correoProfesor")));
                            p.put("nombre", c.getString(c.getColumnIndexOrThrow("nombreProfesor")));
                            p.put("web", c.getString(c.getColumnIndexOrThrow("enlacePaginaWeb")));
                            profs.add(p);
                        }
                        resultado.put("profesores", profs);
                    }
                    break;
                }
                case "reservarTutoria": {
                    String fecha = args.get("fecha") != null ? args.get("fecha") : LocalDate.now().toString();
                    int idHorario = parsearInt(args.get("idHorario"), 1);
                    String motivo = args.get("motivo") != null ? args.get("motivo") : "Consulta de asignatura";
                    TutoriasDB dao = new TutoriasDB(dbHelper);
                    if (dao.slotOcupado(idHorario, fecha)) {
                        resultado.put("estado", "ocupado");
                        resultado.put("mensaje", "El slot de tutoría seleccionado ya está reservado.");
                    } else {
                        long idReserva = dao.insertarReserva(idHorario, correo, fecha, motivo, null);
                        resultado.put("estado", idReserva != -1 ? "éxito" : "fallo");
                        resultado.put("mensaje", idReserva != -1 ? "Tutoría reservada con éxito para el " + fecha : "No se pudo registrar la reserva.");
                    }
                    break;
                }
                case "consultarMenuComedor": {
                    String nombreComedor = args.get("nombreComedor") != null ? args.get("nombreComedor") : "Aynadamar";
                    String fecha = args.get("fecha");
                    if (fecha == null || fecha.isEmpty()) {
                        fecha = LocalDate.now().toString();
                    }
                    ComedoresDB dao = new ComedoresDB(dbHelper);
                    List<Comedor> comedores = dao.obtenerListaComedores();
                    int idComedorEncontrado = 1;
                    for (Comedor com : comedores) {
                        if (com.getNombreComedor().toLowerCase().contains(nombreComedor.toLowerCase())) {
                            idComedorEncontrado = com.getIdComedor();
                            break;
                        }
                    }
                    MenuInfo menu = dao.obtenerMenuParaComedorYFecha(idComedorEncontrado, fecha);
                    resultado.put("comedor", nombreComedor);
                    resultado.put("fecha", fecha);
                    resultado.put("primerPlato", menu.getPrimerPlato());
                    resultado.put("segundoPlato", menu.getSegundoPlato());
                    resultado.put("postre", menu.getPostre());
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
                default:
                    resultado.put("error", "Función desconocida: " + funcName);
                    break;
            }
        } catch (Exception e) {
            e.printStackTrace();
            resultado.put("error", e.getMessage() != null ? e.getMessage() : "Error interno al ejecutar la función");
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
