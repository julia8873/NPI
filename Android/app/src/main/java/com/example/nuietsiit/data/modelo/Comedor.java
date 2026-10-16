package com.example.nuietsiit.data.modelo;

import java.util.Locale;

public class Comedor {

    private int idComedor;
    private String nombreComedor;
    private Integer idEdificio;
    private String direccion;
    private double latitud;
    private double longitud;
    private float distanciaMetros;

    public Comedor(int idComedor, String nombreComedor, Integer idEdificio,
                   String direccion, double latitud, double longitud) {
        this.idComedor = idComedor;
        this.nombreComedor = nombreComedor;
        this.idEdificio = idEdificio;
        this.direccion = direccion;
        this.latitud = latitud;
        this.longitud = longitud;
        this.distanciaMetros = -1f;
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
