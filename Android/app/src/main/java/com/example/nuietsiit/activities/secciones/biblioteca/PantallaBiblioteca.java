package com.example.nuietsiit.activities.secciones.biblioteca;

import android.os.Bundle;

import com.example.nuietsiit.R;
import com.example.nuietsiit.activities.BaseActivity;

/**
 * Base de las pantallas de Biblioteca: cada pantalla es un papel que se pone encima
 * del anterior al abrirla y se retira hacia abajo al volver (design system NUI).
 * Si la pantalla llega con el libro volando desde la balda (transición compartida),
 * esa transición manda y no se anima el papel.
 */
public abstract class PantallaBiblioteca extends BaseActivity {

    /** Extra: la pantalla se abrió con la portada volando desde la balda. */
    public static final String EXTRA_DESDE_BALDA = "desdeBalda";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!desdeBalda()) overridePendingTransition(R.anim.nui_papel_entra, R.anim.nui_quieto);
    }

    @Override
    public void finish() {
        super.finish();
        // al volver con la portada a su balda, Android pone su propia animación después de esta
        overridePendingTransition(R.anim.nui_quieto, R.anim.nui_papel_sale);
    }

    @Override
    public boolean onSupportNavigateUp() {
        // como el botón atrás: si el libro vino volando, vuelve volando a su balda
        supportFinishAfterTransition();
        return true;
    }

    private boolean desdeBalda() {
        return getIntent().getBooleanExtra(EXTRA_DESDE_BALDA, false);
    }
}
