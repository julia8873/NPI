package com.example.nuietsiit.util.tutorial;

/**
 * Gestos que un paso del tutorial puede pedir. TOCAR lo detecta la propia capa;
 * el resto lo avisa la pantalla con {@code avisarTutorial(gesto)} desde el código que ya lo reconoce.
 */
public enum Gesto {
    TOCAR, MANTENER, DESLIZAR_IZQ, DESLIZAR_DER, PELLIZCAR, DIBUJAR, ESCRIBIR,
    ARRASTRAR, // coger algo del foco y llevarlo hacia abajo (p. ej. un documento a la carpeta)
    LANZAR,    // deslizar rápido hacia arriba (lanzar la carpeta)
    INCLINAR, AGITAR, GIRAR, BOCA_ABAJO, ESCANEAR,
    MESA,     // dejar el móvil boca arriba y quieto sobre la mesa
    LEVANTAR, // cogerlo de la mesa
    OREJA;    // acercarse el móvil a la oreja (sensor de proximidad; Noticias lo usa para leer por el auricular)

    /** Se hace moviendo el móvil, no tocando la pantalla: la pista es un móvil dibujado. */
    public boolean esSensor() {
        return this == INCLINAR || this == AGITAR || this == GIRAR || this == BOCA_ABAJO || this == ESCANEAR
                || this == MESA || this == LEVANTAR || this == OREJA;
    }
}
