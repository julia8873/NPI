package com.example.nuietsiit.data.Comedores;

/**
 * Modelo auxiliar que almacena la información gastronómica del menú de un comedor
 * para un día específico (primer plato, segundo plato y postre).
 */
public class MenuInfo {
    private final String primerPlato;
    private final String segundoPlato;
    private final String postre;

    public MenuInfo(String primerPlato, String segundoPlato, String postre) {
        this.primerPlato = primerPlato != null ? primerPlato : "No disponible";
        this.segundoPlato = segundoPlato != null ? segundoPlato : "No disponible";
        this.postre = postre != null ? postre : "No disponible";
    }

    public String getPrimerPlato() {
        return primerPlato;
    }

    public String getSegundoPlato() {
        return segundoPlato;
    }

    public String getPostre() {
        return postre;
    }
}
