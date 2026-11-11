package com.example.nuietsiit.util;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

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
            TablasActividades.poblarActividades(db);
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
            try (Cursor c = db.rawQuery("SELECT titulo, tipoActividad, fechaInicio, descripcion FROM ACTIVIDAD", null)) {
                while (c != null && c.moveToNext()) {
                    sb.append("- Actividad: ").append(c.getString(0))
                            .append(" | Tipo: ").append(c.getString(1))
                            .append(" | Fecha: ").append(c.getString(2))
                            .append(" | Desc: ").append(c.getString(3)).append("\n");
                }
            }

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
