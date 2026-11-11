package com.example.nuietsiit.ui;

import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.nuietsiit.R;

/** Barra de yesería de Secretaría y Biblioteca: chat y perfil a la derecha, y por debajo de la barra de estado. */
public final class CabeceraNui {

    private CabeceraNui() {
    }

    /** Llamar en onCreate, tras setContentView (BaseActivity engancha luego el chat y el perfil). */
    public static void preparar(Toolbar barra) {
        if (barra.findViewById(R.id.ivIcono) == null) {
            View acciones = LayoutInflater.from(barra.getContext())
                    .inflate(R.layout.cabecera_acciones, barra, false);
            Toolbar.LayoutParams lp = new Toolbar.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT,
                    Gravity.END | Gravity.CENTER_VERTICAL);
            lp.setMarginEnd(((ViewGroup.MarginLayoutParams) acciones.getLayoutParams()).getMarginEnd());
            barra.addView(acciones, lp);
        }

        // la app va de borde a borde: la barra crece por arriba lo que ocupa la barra de estado
        TypedValue tv = new TypedValue();
        barra.getContext().getTheme().resolveAttribute(androidx.appcompat.R.attr.actionBarSize, tv, true);
        int alto = TypedValue.complexToDimensionPixelSize(tv.data, barra.getResources().getDisplayMetrics());
        ViewCompat.setOnApplyWindowInsetsListener(barra, (v, insets) -> {
            Insets arriba = insets.getInsets(
                    WindowInsetsCompat.Type.statusBars() | WindowInsetsCompat.Type.displayCutout());
            v.setPadding(v.getPaddingLeft(), arriba.top, v.getPaddingRight(), v.getPaddingBottom());
            ViewGroup.LayoutParams lp = v.getLayoutParams();
            if (lp.height != alto + arriba.top) {
                lp.height = alto + arriba.top;
                v.setLayoutParams(lp);
            }
            return insets;
        });
        ViewCompat.requestApplyInsets(barra);
    }
}
