package com.example.nuietsiit.activities.secciones.biblioteca;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

import com.example.nuietsiit.R;

/** Anillo del modo estudio: se llena al dejar el móvil quieto y luego marca el tiempo que queda. */
public class AnilloEstudio extends View {

    private final Paint pista = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint arco = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF caja = new RectF();
    private float progreso; // 0..1

    public AnilloEstudio(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        float grosor = 12 * getResources().getDisplayMetrics().density;
        pista.setStyle(Paint.Style.STROKE);
        pista.setStrokeWidth(grosor);
        pista.setColor(context.getColor(R.color.nui_linea));
        arco.setStyle(Paint.Style.STROKE);
        arco.setStrokeWidth(grosor);
        arco.setStrokeCap(Paint.Cap.ROUND);
        arco.setColor(context.getColor(R.color.nui_cobalto));
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
    }

    public void setProgreso(float p) {
        p = Math.max(0f, Math.min(1f, p));
        if (p == progreso) return;
        progreso = p;
        invalidate();
    }

    public float getProgreso() {
        return progreso;
    }

    public void setColorArco(int color) {
        arco.setColor(color);
        invalidate();
    }

    public void setGrosor(float px) {
        pista.setStrokeWidth(px);
        arco.setStrokeWidth(px);
        invalidate();
    }

    public void setColorPista(int color) {
        pista.setColor(color);
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float m = arco.getStrokeWidth() / 2f;
        caja.set(m, m, getWidth() - m, getHeight() - m);
        canvas.drawOval(caja, pista);
        if (progreso > 0f) canvas.drawArc(caja, -90f, 360f * progreso, false, arco);
    }
}
