package com.example.nuietsiit.util;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.text.format.DateFormat;

import androidx.core.content.ContextCompat;

import com.example.nuietsiit.BuildConfig;
import com.example.nuietsiit.R;
import com.example.nuietsiit.data.DBHelper;
import com.example.nuietsiit.data.UsuariosDB;
import com.google.ai.client.generativeai.GenerativeModel;
import com.google.ai.client.generativeai.java.ChatFutures;
import com.google.ai.client.generativeai.java.GenerativeModelFutures;
import com.google.ai.client.generativeai.type.Content;
import com.google.ai.client.generativeai.type.FunctionCallPart;
import com.google.ai.client.generativeai.type.GenerateContentResponse;
import com.google.ai.client.generativeai.type.GenerationConfig;
import com.google.ai.client.generativeai.type.RequestOptions;
import com.google.ai.client.generativeai.type.SafetySetting;
import com.google.ai.client.generativeai.type.Tool;
import com.google.ai.client.generativeai.type.ToolConfig;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import java.text.SimpleDateFormat;
import java.util.Date;
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

    private String cargarBaseDeDatosComoTexto(Context context) {
        StringBuilder sb = new StringBuilder();
        try {
            DBHelper dbHelper = DBHelper.getInstance(context);
            SQLiteDatabase db = dbHelper.getWritableDatabase();

            // 1. Datos específicos de matriculación del usuario actual
            if (correoUsuarioRef != null && !correoUsuarioRef.isEmpty()) {
                sb.append("\n[MATRÍCULA Y ASIGNATURAS DEL USUARIO ACTUAL]\n");
                try (Cursor c = db.rawQuery(
                        "SELECT M.codigoAsignatura, A.nombreAsignatura, M.grupo, M.cursoAcademico " +
                                "FROM MATRICULA M " +
                                "JOIN ASIGNATURA A ON M.codigoAsignatura = A.codigoAsignatura " +
                                "WHERE LOWER(M.correoEstudiante) = LOWER(?)", new String[]{correoUsuarioRef})) {

                    boolean tieneMatricula = false;
                    while (c != null && c.moveToNext()) {
                        tieneMatricula = true;
                        sb.append("- Asignatura: ").append(c.getString(1))
                                .append(" (Código: ").append(c.getString(0)).append(")")
                                .append(" | Grupo: ").append(c.getString(2))
                                .append(" | Curso: ").append(c.getString(3)).append("\n");
                    }
                    if (!tieneMatricula) {
                        sb.append("- El usuario no tiene asignaturas matriculadas registradas.\n");
                    }
                }
            }

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

            // 3. Actividades, inscripciones, créditos AbiertaUGR y avisos (sección de Miguel): ver TextoBotActividades
            sb.append(TextoBotActividades.generar(context, correoUsuarioRef));

            // 4. Académico, Profesores, Despachos y Asignaturas
            sb.append("\n[PROFESORES Y ASIGNATURAS - TABLAS ACADÉMICO]\n");
            try (Cursor c = db.rawQuery(
                    "SELECT P.nombreProfesor, P.correoProfesor, " +
                            "GROUP_CONCAT(DISTINCT A.nombreAsignatura) AS asignaturas, " +
                            "E.nombreEspacio " +
                            "FROM PROFESOR P " +
                            "LEFT JOIN IMPARTE I ON P.correoProfesor = I.correoProfesor " +
                            "LEFT JOIN ASIGNATURA A ON I.codigoAsignatura = A.codigoAsignatura " +
                            "LEFT JOIN DESPACHO_PROFESOR DP ON P.correoProfesor = DP.correoProfesor " +
                            "LEFT JOIN ESPACIO E ON DP.idEspacio = E.idEspacio " +
                            "GROUP BY P.correoProfesor", null)) {
                while (c != null && c.moveToNext()) {
                    sb.append("- Prof. ").append(c.getString(0))
                            .append(" (Email: ").append(c.getString(1)).append(")")
                            .append(" | Imparte: ").append(c.getString(2) != null ? c.getString(2) : "Sin asignaturas asignadas")
                            .append(" | Despacho: ").append(c.getString(3) != null ? c.getString(3) : "Sin despacho asignado").append("\n");
                }
            }

            // 5. Biblioteca (Incluye disponibilidad y ubicación exacta)
            sb.append("\n[BIBLIOTECA - TABLAS BIBLIOTECA]\n");
            try (Cursor c = db.rawQuery(
                    "SELECT titulo, autores, isbn, ejemplaresDisponibles, ejemplaresTotales, ubicacion " +
                            "FROM LIBRO", null)) {
                while (c != null && c.moveToNext()) {
                    sb.append("- Libro: ").append(c.getString(0))
                            .append(" de ").append(c.getString(1))
                            .append(" (ISBN: ").append(c.getString(2)).append(")")
                            .append(" | Disponibles: ").append(c.getInt(3)).append("/").append(c.getInt(4))
                            .append(" | Ubicación: ").append(c.getString(5)).append("\n");
                }
            }

            // 6. Trámites Secretaría (Corrección del nombre de la tabla a PROCEDIMIENTO)
            sb.append("\n[TRÁMITES SECRETARÍA - TABLAS SECRETARÍA]\n");
            try (Cursor c = db.rawQuery(
                    "SELECT nombreProcedimiento, descripcion, requisitos, plazosInfo " +
                            "FROM PROCEDIMIENTO", null)) {
                while (c != null && c.moveToNext()) {
                    sb.append("- Trámite: ").append(c.getString(0))
                            .append(" | Desc: ").append(c.getString(1))
                            .append(" | Requisitos: ").append(c.getString(2) != null ? c.getString(2) : "Ninguno")
                            .append(" | Plazos: ").append(c.getString(3) != null ? c.getString(3) : "No especificado").append("\n");
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
            nombre = context.getString(R.string.gemini_usuario_defecto);
        }
        if (grado == null || grado.isEmpty()) {
            grado = context.getString(R.string.gemini_grado_no_especificado);
        }

        Locale loc = Locale.getDefault();
        String patronFecha = DateFormat.getBestDateTimePattern(loc, "EEEEdMMMMyyyy");
        SimpleDateFormat sdfFecha = new SimpleDateFormat(patronFecha, loc);
        String fechaHoy = sdfFecha.format(new Date());

        String datosBd = cargarBaseDeDatosComoTexto(context);
        String correoMostrado = correoUsuario != null ? correoUsuario : context.getString(R.string.gemini_invitado);

        String promptText = context.getString(
                R.string.gemini_system_prompt,
                loc.getDisplayLanguage(loc),
                datosBd,
                nombre,
                correoMostrado,
                grado,
                fechaHoy
        );

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

    public interface GeminiCallback {

        void onRespuesta(GenerateContentResponse respuesta);

        void onError(Throwable error);
    }
}