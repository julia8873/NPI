package com.example.nuietsiit.activities.main;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.example.nuietsiit.R;

/**
 * Nivel de burbuja: un aro con una bola que se mueve según la inclinación del móvil.
 * Los 9 puntitos del aro marcan las zonas de la cuadrícula y el naranja indica la zona seleccionada.
 * Cuando el móvil está totalmente vertical ("listo") la bola se centra, crece y el aro se resalta.
 */
public class NivelInclinacionView extends View {

    private final Paint aro = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint halo = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint punto = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint bola = new Paint(Paint.ANTI_ALIAS_FLAG);

    private final int colorAcento;
    private final int colorApagado;
    private final float dp;

    private float objX, objY;      // posición objetivo (-1..1)
    private float curX, curY;      // posición dibujada (suavizada)
    private boolean listo;
    private float escalaListo;     // 0..1 animada
    private int filaSel = 1, colSel = 1;

    public NivelInclinacionView(Context context) {
        this(context, null);
    }

    public NivelInclinacionView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public NivelInclinacionView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        dp = getResources().getDisplayMetrics().density;
        colorAcento = ContextCompat.getColor(context, R.color.nui_acento);
        colorApagado = ContextCompat.getColor(context, R.color.nui_apagado);

        aro.setStyle(Paint.Style.STROKE);
        halo.setStyle(Paint.Style.FILL);
        punto.setStyle(Paint.Style.FILL);
        bola.setStyle(Paint.Style.FILL);
        bola.setColor(colorAcento);
        halo.setColor(colorAcento);
        halo.setAlpha(36);
    }

    /** Posición objetivo de la bola, de -1 a 1 en cada eje. */
    public void setInclinacion(float x, float y) {
        objX = x;
        objY = y;
        invalidate();
    }

    public void setListo(boolean listo) {
        this.listo = listo;
        invalidate();
    }

    public void setZona(int fila, int columna) {
        filaSel = fila;
        colSel = columna;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float cx = getWidth() / 2f;
        float cy = getHeight() / 2f;
        float trazo = (listo ? 2.5f : 1.5f) * dp;
        float r = Math.min(getWidth(), getHeight()) / 2f - trazo;
        float rBolaBase = r * 0.34f;
        float maxDesp = r - rBolaBase - 3f * dp;

        // suavizado hacia el objetivo
        float tx = listo ? 0f : objX;
        float ty = listo ? 0f : objY;
        curX += (tx - curX) * 0.3f;
        curY += (ty - curY) * 0.3f;
        escalaListo += ((listo ? 1f : 0f) - escalaListo) * 0.2f;

        // halo suave cuando está listo
        if (escalaListo > 0.02f) {
            halo.setAlpha((int) (46 * escalaListo));
            canvas.drawCircle(cx, cy, r, halo);
        }

        // aro
        aro.setStrokeWidth(trazo);
        aro.setColor(listo ? colorAcento : colorApagado);
        aro.setAlpha(listo ? 255 : 110);
        canvas.drawCircle(cx, cy, r, aro);

        // 9 zonas
        float paso = 0.75f * maxDesp;
        for (int f = 0; f < 3; f++) {
            for (int c = 0; c < 3; c++) {
                boolean sel = f == filaSel && c == colSel;
                punto.setColor(sel ? colorAcento : colorApagado);
                punto.setAlpha(sel ? 255 : 90);
                canvas.drawCircle(cx + (c - 1) * paso, cy + (f - 1) * paso, (sel ? 2f : 1.3f) * dp, punto);
            }
        }

        // bola
        float rBola = rBolaBase * (1f + 0.45f * escalaListo);
        canvas.drawCircle(cx + curX * maxDesp, cy + curY * maxDesp, rBola, bola);

        boolean moviendo = Math.abs(tx - curX) > 0.003f || Math.abs(ty - curY) > 0.003f
                || Math.abs((listo ? 1f : 0f) - escalaListo) > 0.01f;
        if (moviendo) postInvalidateOnAnimation();
    }
}
