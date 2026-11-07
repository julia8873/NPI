package com.example.nuietsiit.activities.secciones.secretaria;

import android.graphics.drawable.GradientDrawable;
import android.widget.TextView;

/**
 * Pastilla de estado: texto del color dado sobre un fondo suave del mismo color.
 */
public final class Pastilla {

    private Pastilla() {}

    public static void pintar(TextView tv, String texto, int color) {
        float densidad = tv.getResources().getDisplayMetrics().density;
        GradientDrawable fondo = new GradientDrawable();
        fondo.setCornerRadius(100 * densidad);
        fondo.setColor((color & 0x00FFFFFF) | 0x1F000000); // mismo color al 12 %
        tv.setBackground(fondo);
        tv.setText("● " + texto);
        tv.setTextColor(color);
        int h = Math.round(12 * densidad), v = Math.round(5 * densidad);
        tv.setPadding(h, v, h, v);
    }
}
