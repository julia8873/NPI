package com.example.nuietsiit.data.modelo;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Modelo de mensaje para el Chat Conversacional con Gemini AI.
 */
public class MensajeChat {

    private String texto;
    private boolean esUsuario; // true = mensaje enviado por el usuario, false = respuesta del bot
    private long timestamp;
    private boolean cargando;  // true = indicador de "Pensando..." / procesando respuesta

    public MensajeChat(String texto, boolean esUsuario) {
        this.texto = texto;
        this.esUsuario = esUsuario;
        this.timestamp = System.currentTimeMillis();
        this.cargando = false;
    }

    public MensajeChat(boolean cargando) {
        this.texto = "Pensando...";
        this.esUsuario = false;
        this.timestamp = System.currentTimeMillis();
        this.cargando = cargando;
    }

    public String getTexto() {
        return texto;
    }

    public void setTexto(String texto) {
        this.texto = texto;
    }

    public boolean isEsUsuario() {
        return esUsuario;
    }

    public void setEsUsuario(boolean esUsuario) {
        this.esUsuario = esUsuario;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public boolean isCargando() {
        return cargando;
    }

    public void setCargando(boolean cargando) {
        this.cargando = cargando;
    }

    /**
     * Devuelve el timestamp formateado como cadena de hora local (HH:mm).
     */
    public String formatearHora() {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
        return sdf.format(new Date(timestamp));
    }
}
