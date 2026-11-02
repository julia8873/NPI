package com.example.nuietsiit.activities.secciones.mapa;

import android.view.View;

public class Marcador {
    private String titulo;
    private String planta; // Ej: "planta1", "planta0", "sotano"
    private float xOriginal; // Coordenada X en los píxeles de la imagen original
    private float yOriginal; // Coordenada Y en los píxeles de la imagen original
    private View vistaIcono; // Referencia al ImageView generado dinámicamente

    public Marcador(String titulo, String planta, float xOriginal, float yOriginal) {
        this.titulo = titulo;
        this.planta = planta;
        this.xOriginal = xOriginal;
        this.yOriginal = yOriginal;
    }

    // Getters y Setters
    public String getTitulo() { return titulo; }

    /** Devuelve el título limpio sin emojis ni caracteres raros al principio (ej: "Aula 0.5") */
    public String getTituloLimpio() {
        if (titulo == null) return "";
        return titulo.replaceAll("^[^A-Za-z0-9.]+", "").trim();
    }

    public String getPlanta() { return planta; }
    public float getXOriginal() { return xOriginal; }
    public float getYOriginal() { return yOriginal; }
    public View getVistaIcono() { return vistaIcono; }
    public void setVistaIcono(View vistaIcono) { this.vistaIcono = vistaIcono; }

    @Override
    public String toString() {
        return getTituloLimpio();
    }
}
