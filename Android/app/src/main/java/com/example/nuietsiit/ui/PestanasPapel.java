package com.example.nuietsiit.ui;

import android.content.Context;
import android.content.res.ColorStateList;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.DrawableRes;

import com.example.nuietsiit.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Filtros del design system NUI: pestañas de papel, todas a la vista y en filas del mismo ancho
 * (nada de chips cortados ni scroll lateral). Cada una con su nombre escrito (y un icono si ayuda);
 * la elegida se pone en papel naranja (state_selected de nui_papel_estado).
 * Uso: nuevaFila(), anadir(...) tantas veces como pestañas, y otra nuevaFila() para la siguiente fila.
 */
public class PestanasPapel extends LinearLayout {

    public interface OnElegir {
        void alElegir(Object clave);
    }

    private final List<View> pestanas = new ArrayList<>();
    private LinearLayout fila;
    private OnElegir alElegir;
    private Object marcada;
    private boolean permiteNinguna;
    private final int separacion;

    public PestanasPapel(Context context, AttributeSet attrs) {
        super(context, attrs);
        setOrientation(VERTICAL);
        separacion = getResources().getDimensionPixelSize(R.dimen.nui_espacio_xs);
    }

    public void setOnElegir(OnElegir listener) {
        alElegir = listener;
    }

    /** true: tocar la elegida la desmarca (filtros opcionales). */
    public void setPermiteNinguna(boolean si) {
        permiteNinguna = si;
    }

    public void vaciar() {
        removeAllViews();
        pestanas.clear();
        fila = null;
    }

    public void nuevaFila() {
        fila = new LinearLayout(getContext());
        fila.setOrientation(HORIZONTAL);
        fila.setBaselineAligned(false);
        LayoutParams lp = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        if (getChildCount() > 0) lp.topMargin = separacion;
        addView(fila, lp);
    }

    /**
     * @param texto      nombre de la pestaña (hasta dos líneas)
     * @param detalle    línea pequeña debajo (p. ej. "23 libros"), o null
     * @param icono      0 = sin icono
     * @param colorIcono color del icono (R.color.*), o 0 = tinta
     */
    public View anadir(Object clave, String texto, String detalle, @DrawableRes int icono, int colorIcono) {
        if (fila == null) nuevaFila();
        Context ctx = getContext();
        float d = getResources().getDisplayMetrics().density;

        LinearLayout p = new LinearLayout(ctx);
        p.setOrientation(VERTICAL);
        p.setGravity(Gravity.CENTER);
        p.setBackgroundResource(R.drawable.nui_papel_estado);
        // +3dp a la derecha y abajo: la sombra dura del papel
        p.setPadding(Math.round(6 * d), Math.round(6 * d), Math.round(9 * d), Math.round(9 * d));
        p.setMinimumHeight(getResources().getDimensionPixelSize(R.dimen.nui_toque_min) + Math.round(6 * d));
        p.setClickable(true);
        p.setFocusable(true);
        p.setTag(clave);

        if (icono != 0) {
            ImageView iv = new ImageView(ctx);
            iv.setImageResource(icono);
            iv.setImageTintList(ColorStateList.valueOf(ctx.getColor(colorIcono != 0 ? colorIcono : R.color.nui_tinta)));
            iv.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
            int tam = Math.round(22 * d);
            p.addView(iv, new LayoutParams(tam, tam));
        }
        TextView tv = new TextView(ctx);
        tv.setTextAppearance(R.style.TextAppearance_Nui_Cuerpo_Fuerte);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_PX, getResources().getDimension(R.dimen.nui_texto_s));
        tv.setGravity(Gravity.CENTER);
        tv.setMaxLines(2);
        tv.setEllipsize(TextUtils.TruncateAt.END);
        tv.setText(texto);
        LayoutParams lpTexto = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        if (icono != 0) lpTexto.topMargin = Math.round(2 * d);
        p.addView(tv, lpTexto);
        if (detalle != null) {
            TextView tvDetalle = new TextView(ctx);
            tvDetalle.setTextAppearance(R.style.TextAppearance_Nui_Cuerpo_Secundario);
            tvDetalle.setTextSize(TypedValue.COMPLEX_UNIT_PX, getResources().getDimension(R.dimen.nui_texto_s));
            tvDetalle.setGravity(Gravity.CENTER);
            tvDetalle.setSingleLine(true);
            tvDetalle.setText(detalle);
            p.addView(tvDetalle, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        }
        p.setContentDescription(detalle != null ? texto + ", " + detalle : texto);

        p.setOnClickListener(v -> {
            v.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);
            Object nueva = permiteNinguna && Objects.equals(clave, marcada) ? null : clave;
            marcar(nueva);
            if (alElegir != null) alElegir.alElegir(nueva);
        });

        LayoutParams lp = new LayoutParams(0, LayoutParams.MATCH_PARENT, 1f);
        if (fila.getChildCount() > 0) lp.setMarginStart(separacion);
        fila.addView(p, lp);
        pestanas.add(p);
        p.setSelected(Objects.equals(clave, marcada));
        return p;
    }

    /** Rellena la última fila con huecos para que sus pestañas midan lo mismo que las de arriba. */
    public void completarFila(int porFila) {
        if (fila == null) return;
        while (fila.getChildCount() < porFila) {
            View hueco = new View(getContext());
            LayoutParams lp = new LayoutParams(0, 1, 1f);
            lp.setMarginStart(separacion);
            fila.addView(hueco, lp);
        }
    }

    public void marcar(Object clave) {
        marcada = clave;
        for (View p : pestanas) p.setSelected(Objects.equals(p.getTag(), clave));
    }

    public Object getMarcada() {
        return marcada;
    }

    /** Cambia el texto de una pestaña ya creada (p. ej. Explorar con el resumen del filtro). */
    public void setTexto(Object clave, String texto) {
        for (View p : pestanas) {
            if (!Objects.equals(p.getTag(), clave)) continue;
            LinearLayout l = (LinearLayout) p;
            for (int i = 0; i < l.getChildCount(); i++) {
                if (l.getChildAt(i) instanceof TextView) {
                    ((TextView) l.getChildAt(i)).setText(texto);
                    p.setContentDescription(texto);
                    return;
                }
            }
        }
    }

    public List<View> getPestanas() {
        return pestanas;
    }
}
