package com.example.nuietsiit.data.Comedores;

import java.util.Locale;

/**
 * Modelo que representa un comedor universitario (UGR) con sus datos de ubicación,
 * coordenadas y cálculo de distancia respecto a la posición actual del usuario.
 */
public class Comedor {

    private final int idComedor;
    private final String nombreComedor;
    private final Integer idEdificio;
    private final String direccion;
    private final double latitud;
    private final double longitud;
    private float distanciaMetros;

    public Comedor(int idComedor, String nombreComedor, Integer idEdificio,
                   String direccion, double latitud, double longitud) {
        this.idComedor = idComedor;
        this.nombreComedor = nombreComedor;
        this.idEdificio = idEdificio;
        this.direccion = direccion;
        this.latitud = latitud;
        this.longitud = longitud;
        this.distanciaMetros = -1f; // Por defecto sin calcular
    }

    public int getIdComedor() {
        return idComedor;
    }

    public String getNombreComedor() {
        return nombreComedor;
    }

    public Integer getIdEdificio() {
        return idEdificio;
    }

    public String getDireccion() {
        return direccion;
    }

    public double getLatitud() {
        return latitud;
    }

    public double getLongitud() {
        return longitud;
    }

    public float getDistanciaMetros() {
        return distanciaMetros;
    }

    public void setDistanciaMetros(float distanciaMetros) {
        this.distanciaMetros = distanciaMetros;
    }

    /**
     * Devuelve la distancia formateada en metros (< 1km) o en kilómetros (>= 1km).
     */
    public String getDistanciaFormateada() {
        if (distanciaMetros < 0) {
            return "Distancia desconocida";
        } else if (distanciaMetros < 1000) {
            return Math.round(distanciaMetros) + " m";
        } else {
            return String.format(Locale.getDefault(), "%.1f km", distanciaMetros / 1000.0);
        }
    }
}
