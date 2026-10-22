package com.example.nuietsiit.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

/**
 * Lienzo para firmar con el dedo. Los trazos se suavizan con curvas (Path.quadTo) y se puede
 * exportar la firma a un Bitmap con fondo blanco.
 */
public class LienzoFirma extends View {

    // longitud mínima de trazo para considerar que hay firma (evita un simple toque)
    private static final float LONGITUD_MINIMA_DP = 120;

    private final Path trazo = new Path();
    private final Paint pincel = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final float longitudMinima;
    private float ultimoX, ultimoY;
    private float longitud;
    private Runnable alCambiar;

    public LienzoFirma(Context context, AttributeSet attrs) {
        super(context, attrs);
        float densidad = getResources().getDisplayMetrics().density;
        pincel.setColor(Color.BLACK);
        pincel.setStyle(Paint.Style.STROKE);
        pincel.setStrokeWidth(3 * densidad);
        pincel.setStrokeCap(Paint.Cap.ROUND);
        pincel.setStrokeJoin(Paint.Join.ROUND);
        longitudMinima = LONGITUD_MINIMA_DP * densidad;
    }

    // se llama al terminar un trazo y al borrar
    public void setAlCambiar(Runnable alCambiar) {
        this.alCambiar = alCambiar;
    }

    public boolean tieneFirma() {
        return longitud >= longitudMinima;
    }

    public void borrar() {
        trazo.reset();
        longitud = 0;
        invalidate();
        if (alCambiar != null) alCambiar.run();
    }

    public Bitmap exportar() {
        Bitmap bitmap = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        canvas.drawColor(Color.WHITE);
        canvas.drawPath(trazo, pincel);
        return bitmap;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawPath(trazo, pincel);
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        float x = e.getX(), y = e.getY();
        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                // que un ScrollView padre no robe el gesto al firmar en vertical
                getParent().requestDisallowInterceptTouchEvent(true);
                trazo.moveTo(x, y);
                ultimoX = x;
                ultimoY = y;
                return true;
            case MotionEvent.ACTION_MOVE:
                // en trazos rápidos Android agrupa varios puntos en un mismo evento: usarlos todos
                for (int i = 0; i < e.getHistorySize(); i++) anadirPunto(e.getHistoricalX(i), e.getHistoricalY(i));
                anadirPunto(x, y);
                break;
            case MotionEvent.ACTION_UP:
                trazo.lineTo(x, y);
                if (alCambiar != null) alCambiar.run();
                performClick();
                break;
            default:
                return false;
        }
        invalidate();
        return true;
    }

    // curva hasta el punto medio usando el anterior como control → trazo suave
    private void anadirPunto(float x, float y) {
        trazo.quadTo(ultimoX, ultimoY, (x + ultimoX) / 2, (y + ultimoY) / 2);
        longitud += (float) Math.hypot(x - ultimoX, y - ultimoY);
        ultimoX = x;
        ultimoY = y;
    }

    @Override
    public boolean performClick() {
        return super.performClick();
    }
}
