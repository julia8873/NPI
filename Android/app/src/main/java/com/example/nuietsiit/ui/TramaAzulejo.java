package com.example.nuietsiit.ui;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.example.nuietsiit.R;

/**
 * Repite una tesela para rellenar la vista (design system NUI, ver NPI/DisenoNUI/DESIGN.md).
 * Por defecto la tesela es @drawable/nui_azulejo; con app:tesela="@drawable/nui_arqueria" hace la arquería.
 * Alto pequeño (cenefa, zócalo, arquería) → una fila; la tesela toma el alto de la vista.
 * Alto grande → trama de fondo con teselas de 48dp de alto. Bajar android:alpha (0.08–0.15) si va detrás de texto.
 * Uso: <com.example.nuietsiit.ui.TramaAzulejo android:layout_width="match_parent"
 *           android:layout_height="@dimen/nui_zocalo" />
 */
public class TramaAzulejo extends View {

    private final Drawable tesela;
    private final int altoMaximo;

    public TramaAzulejo(Context context) {
        this(context, null);
    }

    public TramaAzulejo(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        int idTesela = R.drawable.nui_azulejo;
        if (attrs != null) {
            TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.TramaAzulejo);
            idTesela = a.getResourceId(R.styleable.TramaAzulejo_tesela, idTesela);
            a.recycle();
        }
        tesela = ContextCompat.getDrawable(context, idTesela);
        altoMaximo = Math.round(48 * getResources().getDisplayMetrics().density);
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO); // decorativa
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int alto = getHeight();
        int ancho = getWidth();
        if (tesela == null || alto == 0 || ancho == 0) return;

        // la tesela conserva su proporción (el azulejo es cuadrado, la arquería no)
        int altoTesela = Math.min(alto, altoMaximo);
        int anchoTesela = Math.max(1, Math.round(altoTesela
                * (float) tesela.getIntrinsicWidth() / tesela.getIntrinsicHeight()));
        for (int y = 0; y < alto; y += altoTesela) {
            for (int x = 0; x < ancho; x += anchoTesela) {
                tesela.setBounds(x, y, x + anchoTesela, y + altoTesela);
                tesela.draw(canvas);
            }
        }
    }
}