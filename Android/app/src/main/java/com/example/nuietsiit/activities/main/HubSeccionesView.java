package com.example.nuietsiit.activities.main;

import android.content.Context;
import android.graphics.Color;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.ColorUtils;

import com.example.nuietsiit.R;
import com.google.android.material.card.MaterialCardView;

/**
 * Cuadrícula 3x3 de fichas cuadradas del menú principal (toma todo el ancho, sin estirarse a lo alto);
 * la ficha seleccionada se agranda y toda la cuadrícula se inclina ligeramente hacia ella (efecto 3D).
 */
public class HubSeccionesView extends ViewGroup {

    public static class Seccion {
        @StringRes public final int tituloRes;
        @DrawableRes public final int icono;
        public final int color;
        public String titulo; // Se asigna dinámicamente según el contexto de la vista

        Seccion(@StringRes int tituloRes, @DrawableRes int icono, String color) {
            this.tituloRes = tituloRes;
            this.icono = icono;
            this.color = Color.parseColor(color);
        }
    }

    /** Orden de la cuadrícula (fila a fila) utilizando referencias a R.string. */
    public static final Seccion[] SECCIONES = {
            new Seccion(R.string.sec_avisos, R.drawable.ic_nui_avisos, "#FF7A2F"),
            new Seccion(R.string.sec_mapa, R.drawable.ic_nui_mapa, "#3B82F6"),
            new Seccion(R.string.sec_actividades, R.drawable.ic_nui_actividades, "#A855F7"),
            new Seccion(R.string.sec_profesorado, R.drawable.ic_nui_profesorado, "#EC4899"),
            new Seccion(R.string.sec_asignaturas, R.drawable.ic_nui_asignaturas, "#10B981"),
            new Seccion(R.string.sec_secretaria, R.drawable.ic_nui_secretaria, "#F59E0B"),
            new Seccion(R.string.sec_comedores, R.drawable.ic_nui_comedores, "#EF4444"),
            new Seccion(R.string.sec_biblioteca, R.drawable.ic_nui_biblioteca, "#14B8A6"),
            new Seccion(R.string.sec_calendario, R.drawable.ic_nui_calendario, "#6366F1"),
    };

    public interface OnSeccionListener {
        void onSeccion(int indice);
    }

    private static final int N = 9;
    private static final float ESCALA_SELECCION = 1.07f;
    private static final float ESCALA_LISTO = 1.12f;

    private final float dp;
    private final int hueco;
    private final MaterialCardView[] fichas = new MaterialCardView[N];
    private final TextView[] badges = new TextView[N];
    private final int colorSuperficie;

    private int seleccion = 4;
    private boolean listo;
    private OnSeccionListener listener;

    public HubSeccionesView(Context context) {
        this(context, null);
    }

    public HubSeccionesView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public HubSeccionesView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        dp = getResources().getDisplayMetrics().density;
        hueco = Math.round(7 * dp);
        colorSuperficie = ContextCompat.getColor(context, R.color.nui_superficie);

        setClipChildren(false);
        setClipToPadding(false);
        // Perspectiva suave para la inclinación 3D
        setCameraDistance(840f * dp * 2f);

        LayoutInflater inflater = LayoutInflater.from(context);
        for (int i = 0; i < N; i++) {
            Seccion s = SECCIONES[i];
            s.titulo = context.getString(s.tituloRes); // Actualiza la propiedad pública según el idioma actual

            MaterialCardView ficha = (MaterialCardView) inflater.inflate(R.layout.item_nui_ficha, this, false);
            ficha.setCardBackgroundColor(ColorUtils.blendARGB(colorSuperficie, s.color, 0.13f));
            ficha.setOutlineSpotShadowColor(s.color);
            ficha.setOutlineAmbientShadowColor(s.color);
            ficha.setContentDescription(s.titulo);

            ficha.findViewById(R.id.fichaIconoCaja).getBackground().mutate().setTint(s.color);
            ((ImageView) ficha.findViewById(R.id.fichaIcono)).setImageResource(s.icono);
            ((TextView) ficha.findViewById(R.id.fichaTitulo)).setText(s.titulo);

            TextView badge = ficha.findViewById(R.id.fichaBadge);
            badge.getBackground().mutate().setTint(s.color);
            badges[i] = badge;

            final int indice = i;
            ficha.setOnClickListener(v -> {
                if (listener != null) listener.onSeccion(indice);
            });

            fichas[i] = ficha;
            addView(ficha);
        }
        aplicarSeleccion(false);
    }

    public void setOnSeccionListener(OnSeccionListener listener) {
        this.listener = listener;
    }

    public int getSeleccion() {
        return seleccion;
    }

    public void setSeleccion(int indice, boolean animar) {
        if (indice < 0 || indice >= N) return;
        seleccion = indice;
        aplicarSeleccion(animar);
    }

    /** Resalta aún más la ficha seleccionada. */
    public void setListo(boolean listo) {
        this.listo = listo;
        aplicarSeleccion(true);
    }

    public void setBadge(int indice, int cantidad) {
        if (indice < 0 || indice >= N) return;
        TextView b = badges[indice];
        if (cantidad > 0) {
            b.setText(cantidad > 99 ? "99+" : String.valueOf(cantidad));
            b.setVisibility(VISIBLE);
        } else {
            b.setVisibility(GONE);
        }
    }

    /** Pequeño "pop" de la ficha antes de abrir la sección. */
    public void animarApertura(int indice, Runnable alTerminar) {
        View ficha = fichas[indice];
        ficha.animate().scaleX(1.18f).scaleY(1.18f).alpha(0.85f).setDuration(140)
                .withEndAction(alTerminar).start();
    }

    /** Devuelve las fichas a su estado normal (al volver de una sección). */
    public void restaurar() {
        for (View f : fichas) f.animate().cancel();
        listo = false;
        for (View f : fichas) f.setAlpha(1f);
        aplicarSeleccion(false);
    }

    private void aplicarSeleccion(boolean animar) {
        for (int i = 0; i < N; i++) {
            MaterialCardView f = fichas[i];
            boolean foco = i == seleccion;
            float escala = foco ? (listo ? ESCALA_LISTO : ESCALA_SELECCION) : 1f;
            float z = foco ? (listo ? 16 : 9) * dp : 0f;
            f.setStrokeColor(foco ? SECCIONES[i].color : Color.TRANSPARENT);
            if (animar) {
                f.animate().scaleX(escala).scaleY(escala).translationZ(z).setDuration(150).start();
            } else {
                f.setScaleX(escala);
                f.setScaleY(escala);
                f.setTranslationZ(z);
            }
        }

        // La cuadrícula se inclina hacia la ficha seleccionada
        int c = seleccion % 3, r = seleccion / 3;
        float ry = (c - 1) * 7f;
        float rx = (1 - r) * 6f;
        if (animar) {
            animate().rotationY(ry).rotationX(rx).setDuration(250).start();
        } else {
            setRotationY(ry);
            setRotationX(rx);
        }
    }

    /** Lado de las fichas (cuadradas) y margen izquierdo para centrar la cuadrícula. */
    private int lado;
    private int margenX;

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int w = MeasureSpec.getSize(widthMeasureSpec);
        int ladoPorAncho = (w - 2 * hueco) / 3;
        lado = ladoPorAncho;
        if (MeasureSpec.getMode(heightMeasureSpec) != MeasureSpec.UNSPECIFIED) {
            int ladoPorAlto = (MeasureSpec.getSize(heightMeasureSpec) - 2 * hueco) / 3;
            lado = Math.min(lado, ladoPorAlto);
        }
        lado = Math.max(lado, 0);
        margenX = (w - (3 * lado + 2 * hueco)) / 2;
        setMeasuredDimension(w, 3 * lado + 2 * hueco);

        int spec = MeasureSpec.makeMeasureSpec(lado, MeasureSpec.EXACTLY);
        for (int i = 0; i < getChildCount(); i++) getChildAt(i).measure(spec, spec);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        for (int i = 0; i < getChildCount(); i++) {
            int fila = i / 3, col = i % 3;
            int x = margenX + col * (lado + hueco);
            int y = fila * (lado + hueco);
            getChildAt(i).layout(x, y, x + lado, y + lado);
        }
        // El centro de giro es el centro de la cuadrícula
        setPivotX((r - l) / 2f);
        setPivotY((b - t) / 2f);
    }
}