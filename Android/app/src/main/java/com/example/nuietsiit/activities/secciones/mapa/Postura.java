package com.example.nuietsiit.activities.secciones.mapa;

/**
 * Postura del móvil a partir de la gravedad (acelerómetro filtrado). Java puro para poder probarlo.
 * LEVANTADO = en vertical delante de la cara (gravedad sobre el eje Y), BOCA_ABAJO = pantalla contra
 * el suelo/la mesa, NORMAL = lo demás (en la mano mirando el mapa). Una postura nueva solo cuenta
 * cuando se mantiene {@link #MANTENER_MS}; con histéresis para no parpadear al andar.
 */
public final class Postura {

    public enum Tipo { NORMAL, LEVANTADO, BOCA_ABAJO }

    public static final long MANTENER_MS = 1000;
    static final float ENTRAR_LEVANTADO = 8f, SALIR_LEVANTADO = 6.5f;
    static final float ENTRAR_BOCA_ABAJO = -8f, SALIR_BOCA_ABAJO = -6f;
    /** Filtro paso bajo: cuánto pesa la medida nueva (quita los golpes de los pasos). */
    private static final float ALFA = 0.2f;

    private Tipo actual = Tipo.NORMAL, candidata = Tipo.NORMAL;
    private long desde;
    private float gx, gy, gz = 9.8f;
    private boolean primera = true;

    public Tipo actual() {
        return actual;
    }

    /**
     * Añade una medida del acelerómetro (m/s², ejes del móvil).
     * @return la postura nueva si acaba de cambiar; null si sigue igual
     */
    public Tipo medir(float x, float y, float z, long ms) {
        if (primera) {
            gx = x; gy = y; gz = z;
            primera = false;
        } else {
            gx += ALFA * (x - gx);
            gy += ALFA * (y - gy);
            gz += ALFA * (z - gz);
        }
        Tipo vista = clasificar();
        if (vista != candidata) {
            candidata = vista;
            desde = ms;
        }
        if (candidata != actual && ms - desde >= MANTENER_MS) {
            actual = candidata;
            return actual;
        }
        return null;
    }

    /** Con histéresis: para salir de una postura hace falta pasar un umbral más bajo que para entrar. */
    private Tipo clasificar() {
        if (actual == Tipo.LEVANTADO ? gy > SALIR_LEVANTADO : gy > ENTRAR_LEVANTADO) return Tipo.LEVANTADO;
        if (actual == Tipo.BOCA_ABAJO ? gz < SALIR_BOCA_ABAJO : gz < ENTRAR_BOCA_ABAJO) return Tipo.BOCA_ABAJO;
        return Tipo.NORMAL;
    }
}
