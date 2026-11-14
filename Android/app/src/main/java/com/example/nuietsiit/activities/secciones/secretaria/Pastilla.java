package com.example.nuietsiit.activities.secciones.secretaria;

import android.graphics.drawable.GradientDrawable;
import android.widget.TextView;

/**
 * Estado de un trámite o solicitud como pastilla: esquinas redondeadas,
 * texto del color del estado y fondo de ese color muy claro.
 */
public final class Pastilla {

    private Pastilla() {}

    /** @param colorRes color de estado (R.color.nui_ok, nui_info, nui_aviso…) */
    public static void pintar(TextView tv, String texto, int colorRes) {
        float densidad = tv.getResources().getDisplayMetrics().density;
        sellar(tv, texto, colorRes);
        int h = Math.round(10 * densidad), v = Math.round(3 * densidad);
        tv.setPadding(h, v, h, v);
    }

    /** Igual que {@link #pintar} pero respeta el padding del estilo (Widget.Nui.Sello). */
    public static void sellar(TextView tv, String texto, int colorRes) {
        float densidad = tv.getResources().getDisplayMetrics().density;
        int color = tv.getContext().getColor(colorRes);
        GradientDrawable fondo = new GradientDrawable();
        fondo.setCornerRadius(12 * densidad);
        fondo.setColor((color & 0x00FFFFFF) | 0x1F000000); // mismo color al 12 %
        tv.setBackground(fondo);
        tv.setText(texto);
        tv.setTextColor(color);
    }
}
