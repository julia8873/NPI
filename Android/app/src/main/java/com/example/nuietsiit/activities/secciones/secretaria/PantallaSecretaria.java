package com.example.nuietsiit.activities.secciones.secretaria;

import android.os.Bundle;

import com.example.nuietsiit.R;
import com.example.nuietsiit.activities.BaseActivity;
import com.example.nuietsiit.ui.InclinarParaPasar;

/**
 * Base de todas las pantallas de Secretaría: cada pantalla es un papel que se pone
 * encima del anterior al abrirla y se retira hacia abajo al volver (design system NUI).
 */
public abstract class PantallaSecretaria extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        overridePendingTransition(R.anim.nui_papel_entra, R.anim.nui_quieto);
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.nui_quieto, R.anim.nui_papel_sale);
    }

    // mientras se pasan hojas inclinando el móvil, agitar no saca al menú (inclinar con fuerza contaba como agitar)
    @Override
    protected boolean usaAgitar() {
        return !InclinarParaPasar.hayActivo();
    }
}
