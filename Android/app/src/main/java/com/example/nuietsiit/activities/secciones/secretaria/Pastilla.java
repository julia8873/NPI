package com.example.nuietsiit.activities.secciones.secretaria;

import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.widget.TextView;

import com.example.nuietsiit.R;

/**
 * Estado de un trámite o solicitud como etiqueta del design system NUI:
 * esquinas casi rectas, borde del color del estado y fondo de ese color muy claro.
 */
public final class Pastilla {

    private Pastilla() {}

    /** @param colorRes color de estado (R.color.nui_ok, nui_info, nui_aviso…) */
    public static void pintar(TextView tv, String texto, int colorRes) {
        float densidad = tv.getResources().getDisplayMetrics().density;
        int color = tv.getContext().getColor(colorRes);
        GradientDrawable fondo = new GradientDrawable();
        fondo.setCornerRadius(3 * densidad);
        fondo.setColor((color & 0x00FFFFFF) | 0x1F000000); // mismo color al 12 %
        fondo.setStroke(Math.round(densidad), color);
        tv.setBackground(fondo);
        tv.setText(texto);
        tv.setTextColor(color);
        int h = Math.round(8 * densidad), v = Math.round(2 * densidad);
        tv.setPadding(h, v, h, v);
    }

    /**
     * Sello (Widget.Nui.Sello) entintado del color del estado: el mismo doble marco que nui_sello,
     * hecho aquí con el trazo ya del color (teñir el drawable rellenaba también el centro).
     */
    public static void sellar(TextView tv, String texto, int colorRes) {
        float d = tv.getResources().getDisplayMetrics().density;
        int color = tv.getContext().getColor(colorRes);
        GradientDrawable fuera = new GradientDrawable();
        fuera.setCornerRadius(4 * d);
        fuera.setStroke(tv.getResources().getDimensionPixelSize(R.dimen.nui_borde_sello), color);
        GradientDrawable dentro = new GradientDrawable();
        dentro.setCornerRadius(2 * d);
        dentro.setStroke(tv.getResources().getDimensionPixelSize(R.dimen.nui_borde), color);
        LayerDrawable marco = new LayerDrawable(new Drawable[]{fuera, dentro});
        int hueco = Math.round(4 * d);
        marco.setLayerInset(1, hueco, hueco, hueco, hueco);
        tv.setBackground(marco);
        tv.setText(texto);
        tv.setTextColor(color);
    }
}
