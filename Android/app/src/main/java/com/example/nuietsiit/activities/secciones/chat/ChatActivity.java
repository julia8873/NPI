package com.example.nuietsiit.activities.secciones.chat;

import android.os.Bundle;
import android.util.Log;
import android.widget.EditText;
import android.widget.ImageButton;


import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.nuietsiit.R;
import com.example.nuietsiit.activities.BaseActivity;
import com.example.nuietsiit.data.modelo.MensajeChat;
import com.example.nuietsiit.util.GeminiManager;
import com.example.nuietsiit.util.SesionUtil;
import com.google.ai.client.generativeai.type.GenerateContentResponse;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

/**
 * Pantalla principal del Asistente Conversacional NUI ETSIIT impulsado por Gemini AI.
 */
public class ChatActivity extends BaseActivity {

    private RecyclerView rvChatMensajes;
    private ChatAdapter chatAdapter;
    private List<MensajeChat> listaMensajes;
    private EditText etMensajeInput;
    private FloatingActionButton btnEnviarMensaje;
    private ImageButton btnVolverChat;
    private String correoUsuario;
    private GeminiManager geminiManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        correoUsuario = SesionUtil.correo(getIntent());

        inicializarVistas();
        configurarRecyclerView();
        configurarEventos();

        // Inicializar GeminiManager (sin herramientas aún en la Fase C)
        geminiManager = new GeminiManager(this, correoUsuario, null);

        // Saludo inicial de bienvenida del asistente
        agregarMensajeBienvenida();
    }

    private void inicializarVistas() {
        rvChatMensajes = findViewById(R.id.rvChatMensajes);
        etMensajeInput = findViewById(R.id.etMensajeInput);
        btnEnviarMensaje = findViewById(R.id.btnEnviarMensaje);
        btnVolverChat = findViewById(R.id.btnVolverChat);
    }

    private void configurarRecyclerView() {
        listaMensajes = new ArrayList<>();
        chatAdapter = new ChatAdapter(listaMensajes);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        rvChatMensajes.setLayoutManager(layoutManager);
        rvChatMensajes.setAdapter(chatAdapter);
    }

    private void configurarEventos() {
        btnVolverChat.setOnClickListener(v -> finish());

        btnEnviarMensaje.setOnClickListener(v -> enviarMensajeUsuario());
    }

    private void agregarMensajeBienvenida() {
        String saludo = "¡Hola! Soy tu Asistente Virtual de la ETSIIT impulsado por Gemini.\n\n" +
                "¿En qué te puedo ayudar hoy? Puedes hacerme preguntas sobre asignaturas, profesores, " +
                "menús del comedor, o pedirme información de la facultad.";
        listaMensajes.add(new MensajeChat(saludo, false));
        chatAdapter.notifyItemInserted(listaMensajes.size() - 1);
        rvChatMensajes.scrollToPosition(listaMensajes.size() - 1);
    }

    private void enviarMensajeUsuario() {
        String texto = etMensajeInput.getText().toString().trim();
        if (texto.isEmpty()) {
            return;
        }

        // 1. Añadir mensaje del usuario a la lista
        MensajeChat mensajeUsuario = new MensajeChat(texto, true);
        listaMensajes.add(mensajeUsuario);
        int posUsuario = listaMensajes.size() - 1;
        chatAdapter.notifyItemInserted(posUsuario);
        rvChatMensajes.scrollToPosition(posUsuario);
        etMensajeInput.setText("");

        // 2. Añadir burbuja de estado "Pensando..." del asistente
        MensajeChat mensajeCargando = new MensajeChat(true);
        listaMensajes.add(mensajeCargando);
        int posCargando = listaMensajes.size() - 1;
        chatAdapter.notifyItemInserted(posCargando);
        rvChatMensajes.scrollToPosition(posCargando);

        // 3. Enviar consulta a GeminiManager de forma asíncrona
        geminiManager.enviarMensaje(texto, new GeminiManager.GeminiCallback() {
            @Override
            public void onRespuesta(GenerateContentResponse respuesta) {
                String respuestaTexto = respuesta.getText();
                if (respuestaTexto == null || respuestaTexto.isEmpty()) {
                    respuestaTexto = "No he recibido una respuesta de la API.";
                }

                // Reemplazar la burbuja "Pensando..." con la respuesta real del modelo
                mensajeCargando.setCargando(false);
                mensajeCargando.setTexto(respuestaTexto);
                mensajeCargando.setTimestamp(System.currentTimeMillis());

                chatAdapter.notifyItemChanged(posCargando);
                rvChatMensajes.scrollToPosition(posCargando);
            }

            @Override
            public void onError(Throwable error) {
                Log.e("ChatGemini", "Error de conexión con la API", error);
                String detalleError = error != null && error.getMessage() != null ? error.getMessage() : "Error desconocido";
                String errorTexto = "Error de conexión con Gemini:\n\n" + detalleError +
                        "\n\nPor favor, comprueba tu conexión o tu API Key e inténtalo de nuevo.";

                mensajeCargando.setCargando(false);
                mensajeCargando.setTexto(errorTexto);
                mensajeCargando.setTimestamp(System.currentTimeMillis());

                chatAdapter.notifyItemChanged(posCargando);
                rvChatMensajes.scrollToPosition(posCargando);
            }


        });
    }

    public String getCorreoUsuario() {
        return correoUsuario;
    }
}
