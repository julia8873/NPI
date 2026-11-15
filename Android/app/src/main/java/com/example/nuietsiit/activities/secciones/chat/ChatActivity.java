package com.example.nuietsiit.activities.secciones.chat;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.nuietsiit.R;
import com.example.nuietsiit.activities.BaseActivity;
import com.example.nuietsiit.data.modelo.MensajeChat;
import com.example.nuietsiit.util.GeminiManager;
import com.example.nuietsiit.util.GeminiTools;
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
        configurarAjusteTeclado();

        try {
            geminiManager = new GeminiManager(this, correoUsuario, GeminiTools.obtenerHerramientas());
        } catch (Exception e) {
            Log.e("ChatGemini", "Error al inicializar herramientas de Function Calling", e);
            geminiManager = new GeminiManager(this, correoUsuario, null);
        }

        agregarMensajeBienvenida();
    }

    private void configurarAjusteTeclado() {
        View rootChat = findViewById(R.id.rootChat);
        if (rootChat != null) {
            ViewCompat.setOnApplyWindowInsetsListener(rootChat, (v, insets) -> {
                Insets imeInsets = insets.getInsets(WindowInsetsCompat.Type.ime());
                Insets sysInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                int bottomPadding = Math.max(imeInsets.bottom, sysInsets.bottom);
                v.setPadding(v.getPaddingLeft(), sysInsets.top, v.getPaddingRight(), bottomPadding);

                if (imeInsets.bottom > 0 && listaMensajes != null && !listaMensajes.isEmpty()) {
                    rvChatMensajes.postDelayed(() ->
                            rvChatMensajes.scrollToPosition(listaMensajes.size() - 1), 100);
                }
                return insets;
            });
        }
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

        etMensajeInput.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus && !listaMensajes.isEmpty()) {
                rvChatMensajes.postDelayed(() ->
                        rvChatMensajes.smoothScrollToPosition(listaMensajes.size() - 1), 200);
            }
        });

        etMensajeInput.setOnClickListener(v -> {
            if (!listaMensajes.isEmpty()) {
                rvChatMensajes.postDelayed(() ->
                        rvChatMensajes.smoothScrollToPosition(listaMensajes.size() - 1), 200);
            }
        });
    }

    private void agregarMensajeBienvenida() {
        listaMensajes.add(new MensajeChat(getString(R.string.chat_bienvenida), false));
        chatAdapter.notifyItemInserted(listaMensajes.size() - 1);
        rvChatMensajes.scrollToPosition(listaMensajes.size() - 1);
    }

    private void enviarMensajeUsuario() {
        String texto = etMensajeInput.getText().toString().trim();
        if (texto.isEmpty()) {
            return;
        }

        MensajeChat mensajeUsuario = new MensajeChat(texto, true);
        listaMensajes.add(mensajeUsuario);
        int posUsuario = listaMensajes.size() - 1;
        chatAdapter.notifyItemInserted(posUsuario);
        rvChatMensajes.scrollToPosition(posUsuario);
        etMensajeInput.setText("");

        MensajeChat mensajeCargando = new MensajeChat(true);
        listaMensajes.add(mensajeCargando);
        int posCargando = listaMensajes.size() - 1;
        chatAdapter.notifyItemInserted(posCargando);
        rvChatMensajes.scrollToPosition(posCargando);

        geminiManager.enviarMensaje(texto, new GeminiManager.GeminiCallback() {
            @Override
            public void onRespuesta(GenerateContentResponse respuesta) {
                String respuestaTexto = respuesta.getText();
                if (respuestaTexto == null || respuestaTexto.isEmpty()) {
                    respuestaTexto = getString(R.string.chat_sin_respuesta);
                }

                mensajeCargando.setCargando(false);
                mensajeCargando.setTexto(respuestaTexto);
                mensajeCargando.setTimestamp(System.currentTimeMillis());

                chatAdapter.notifyItemChanged(posCargando);
                rvChatMensajes.scrollToPosition(posCargando);
            }

            @Override
            public void onError(Throwable error) {
                Log.e("ChatGemini", "Error de conexión con la API", error);
                String detalleError = error != null && error.getMessage() != null
                        ? error.getMessage()
                        : getString(R.string.chat_error_desconocido);
                String errorTexto = getString(R.string.chat_error_conexion_fmt, detalleError);

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