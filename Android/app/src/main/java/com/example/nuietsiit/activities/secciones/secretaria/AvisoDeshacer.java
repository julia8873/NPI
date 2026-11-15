package com.example.nuietsiit.activities.secciones.secretaria;

import android.content.Context;
import android.graphics.Color;
import android.os.SystemClock;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.example.nuietsiit.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;

/**
 * Aviso de Deshacer del design system (Widget.Nui.Aviso): nota de papel flotante con etiqueta, texto, botón
 * "Deshacer · N" que cuenta los segundos y una mecha de almagra que se consume. Va dentro de un Snackbar para
 * conservar su comportamiento (deslizar para quitarlo, encima del contenido, más tiempo con TalkBack).
 */
public final class AvisoDeshacer {

    private AvisoDeshacer() {}

    public static Snackbar mostrar(@NonNull View ancla, String etiqueta, String texto, int duracionMs,
                                   @NonNull Runnable deshacer) {
        Context ctx = ancla.getContext();
        Snackbar aviso = Snackbar.make(ancla, "", duracionMs);
        ViewGroup caja = (ViewGroup) aviso.getView();
        // fuera el aspecto de Material (caja negra con sombra suave): solo queda nuestro papel
        aviso.setBackgroundTint(Color.TRANSPARENT);
        caja.setElevation(0);
        caja.setPadding(0, 0, 0, 0);
        caja.getChildAt(0).setVisibility(View.GONE);

        View papel = LayoutInflater.from(ctx).inflate(R.layout.aviso_deshacer, caja, false);
        ((TextView) papel.findViewById(R.id.tvAvisoEtiqueta)).setText(etiqueta);
        ((TextView) papel.findViewById(R.id.tvAvisoTexto)).setText(texto);
        MaterialButton boton = papel.findViewById(R.id.btnAvisoDeshacer);
        boton.setOnClickListener(v -> {
            aviso.dismiss();
            deshacer.run();
        });
        caja.addView(papel);
        papel.setContentDescription(etiqueta + ". " + texto);

        // mecha y segundos: cuánto queda para poder deshacer
        View mecha = papel.findViewById(R.id.vAvisoMecha);
        // se consume desde la izquierda hacia el botón
        mecha.addOnLayoutChangeListener((v, l, t, r, b, ol, ot, or, ob) -> v.setPivotX(v.getWidth()));
        mecha.animate().scaleX(0f).setDuration(duracionMs).setInterpolator(new LinearInterpolator()).start();
        long fin = SystemClock.uptimeMillis() + duracionMs;
        Runnable segundos = new Runnable() {
            @Override
            public void run() {
                long quedan = (fin - SystemClock.uptimeMillis() + 999) / 1000;
                if (quedan <= 0 || !aviso.isShownOrQueued()) return;
                boton.setText(ctx.getString(R.string.sec_aviso_deshacer_fmt, quedan));
                boton.postDelayed(this, 250);
            }
        };
        boton.post(segundos);

        aviso.show();
        return aviso;
    }
}