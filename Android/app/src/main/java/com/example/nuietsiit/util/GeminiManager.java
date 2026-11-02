package com.example.nuietsiit.util;

import android.content.Context;
import androidx.core.content.ContextCompat;

import com.example.nuietsiit.BuildConfig;
import com.example.nuietsiit.data.UsuariosDB;
import com.google.ai.client.generativeai.GenerativeModel;
import com.google.ai.client.generativeai.java.ChatFutures;
import com.google.ai.client.generativeai.java.GenerativeModelFutures;
import com.google.ai.client.generativeai.type.Content;
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
import java.util.concurrent.Executor;

/**
 * Gestor del modelo de Inteligencia Artificial Gemini para la NUI de la ETSIIT.
 * Encapsula GenerativeModel, GenerativeModelFutures y la gestión de la sesión de Chat.
 */
public class GeminiManager {

    private final GenerativeModelFutures modelFutures;
    private ChatFutures chatSession;
    private final Executor mainExecutor;

    public GeminiManager(Context context, String correoUsuario, List<Tool> herramientas) {
        this.mainExecutor = ContextCompat.getMainExecutor(context);

        // 1. Construir el System Instruction con el contexto del usuario y de la ETSIIT
        Content systemInstruction = construirSystemInstruction(context, correoUsuario);

        GenerationConfig genConfig = new GenerationConfig.Builder().build();
        List<SafetySetting> safetySettings = null;
        RequestOptions reqOptions = new RequestOptions();
        ToolConfig toolConfig = null;

        android.util.Log.d("ChatGemini", "Inicializando GeminiManager con modelo gemini-3.5-flash. Longitud API Key: " +
                (BuildConfig.GEMINI_API_KEY != null ? BuildConfig.GEMINI_API_KEY.length() : 0));

        // 2. Instanciar el modelo Gemini (gemini-3.5-flash para evitar sobrecarga del servidor)
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









        // 3. Iniciar sesión conversacional continua (ChatFutures)
        this.chatSession = modelFutures.startChat();

    }


    /**
     * Construye las instrucciones de sistema inyectando el rol del bot y los datos del estudiante en sesión.
     */
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
                "acciones dentro de la aplicación móvil cuando el usuario lo solicite.\n\n" +
                "TONO Y ESTILO DE RESPUESTA:\n" +
                "1. Sé siempre amable, cercano, conciso y profesional.\n" +
                "2. Como respondes en una pantalla móvil, mantén tus respuestas breves y estructuradas. Evita párrafos largos innecesarios.\n" +
                "3. Utiliza viñetas o listas simples cuando presentes información múltiple (como opciones de menús de comedores o listados de tutorías).\n" +
                "4. Si no dispones de un dato o una función falla, indícalo con amabilidad sin inventar datos no existentes en la facultad.\n\n" +
                "ESTRUCTURA DE CONOCIMIENTO DE LA ETSIIT:\n" +
                "- Comedores Universitarios: Aynadamar (más cercano a la ETSIIT), Fuentenueva, Severo Ochoa, PTS.\n" +
                "- Estudios: Grados en Ingeniería Informática, Ingeniería de Tecnologías de Telecomunicación, Doble Grado Informática-Matemáticas.\n" +
                "- Secretaría y Servicios: Trámites de matriculación, reconocimientos, solicitudes de título, citas previas y justificaciones.\n" +
                "- Instalaciones: Aulas (ej. 1.6, 2.3), Despachos de profesores, Biblioteca, Bibliomaker, Decanato y Cafetería.\n\n" +
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



    /**
     * Envia un mensaje de texto de forma asíncrona al chat de Gemini.
     */
    public void enviarMensaje(String textoMensaje, GeminiCallback callback) {
        Content contenidoMensaje = new Content.Builder().addText(textoMensaje).build();
        ListenableFuture<GenerateContentResponse> responseFuture = chatSession.sendMessage(contenidoMensaje);

        Futures.addCallback(responseFuture, new FutureCallback<GenerateContentResponse>() {
            @Override
            public void onSuccess(GenerateContentResponse result) {
                callback.onRespuesta(result);
            }

            @Override
            public void onFailure(Throwable t) {
                callback.onError(t);
            }
        }, mainExecutor);
    }

    /**
     * Reemplaza la sesión de chat por una nueva si se reinicia.
     */
    public void reiniciarChat() {
        this.chatSession = modelFutures.startChat();
    }

    /**
     * Interfaz Callback para recibir respuestas o errores de Gemini en la UI.
     */
    public interface GeminiCallback {
        void onRespuesta(GenerateContentResponse respuesta);
        void onError(Throwable error);
    }
}
