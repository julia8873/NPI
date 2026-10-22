package com.example.nuietsiit.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Matrix;
import android.graphics.Path;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import java.util.Arrays;

/**
 * Lienzo para firmar con el dedo. Los trazos se suavizan con curvas (Path.quadTo) y se puede
 * exportar la firma a un Bitmap con fondo blanco. Los trazos se conservan al rotar la pantalla.
 */
public class LienzoFirma extends View {

    // longitud mínima de trazo para considerar que hay firma (evita un simple toque)
    private static final float LONGITUD_MINIMA_DP = 120;
    private static final int EMPEZAR = 0, PUNTO = 1, TERMINAR = 2;

    private final Path trazo = new Path();
    private final Paint pincel = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final float longitudMinima;
    private float ultimoX, ultimoY;
    private float longitud;
    private Runnable alCambiar;
    // trazos como (tipo, x, y) para poder rehacer el Path al rotar
    private float[] registro = new float[300];
    private int nRegistro;
    // tamaño con el que se dibujaron los trazos restaurados (0 = nada pendiente de reescalar)
    private int anchoRestaurado, altoRestaurado;

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
        nRegistro = 0;
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
                aplicar(EMPEZAR, x, y);
                return true;
            case MotionEvent.ACTION_MOVE:
                // en trazos rápidos Android agrupa varios puntos en un mismo evento: usarlos todos
                for (int i = 0; i < e.getHistorySize(); i++) aplicar(PUNTO, e.getHistoricalX(i), e.getHistoricalY(i));
                aplicar(PUNTO, x, y);
                break;
            case MotionEvent.ACTION_UP:
                aplicar(TERMINAR, x, y);
                if (alCambiar != null) alCambiar.run();
                performClick();
                break;
            default:
                return false;
        }
        invalidate();
        return true;
    }

    // añade el punto al Path y lo anota en el registro
    private void aplicar(int tipo, float x, float y) {
        if (tipo == EMPEZAR) {
            trazo.moveTo(x, y);
        } else if (tipo == PUNTO) {
            // curva hasta el punto medio usando el anterior como control → trazo suave
            trazo.quadTo(ultimoX, ultimoY, (x + ultimoX) / 2, (y + ultimoY) / 2);
            longitud += (float) Math.hypot(x - ultimoX, y - ultimoY);
        } else {
            trazo.lineTo(x, y);
        }
        ultimoX = x;
        ultimoY = y;
        if (nRegistro + 3 > registro.length) registro = Arrays.copyOf(registro, registro.length * 2);
        registro[nRegistro++] = tipo;
        registro[nRegistro++] = x;
        registro[nRegistro++] = y;
    }

    // ---------- Conservar la firma al rotar ----------

    @Override
    protected Parcelable onSaveInstanceState() {
        Bundle estado = new Bundle();
        estado.putParcelable("super", super.onSaveInstanceState());
        estado.putFloatArray("registro", Arrays.copyOf(registro, nRegistro));
        // si aún no se ha reescalado (lienzo oculto desde la rotación anterior), los trazos siguen en ese tamaño
        boolean pendiente = anchoRestaurado > 0 && altoRestaurado > 0;
        estado.putInt("ancho", pendiente ? anchoRestaurado : getWidth());
        estado.putInt("alto", pendiente ? altoRestaurado : getHeight());
        return estado;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        Bundle estado = (Bundle) state;
        super.onRestoreInstanceState(estado.getParcelable("super"));
        float[] guardado = estado.getFloatArray("registro");
        trazo.reset();
        longitud = 0;
        nRegistro = 0;
        for (int i = 0; guardado != null && i + 2 < guardado.length; i += 3) {
            aplicar((int) guardado[i], guardado[i + 1], guardado[i + 2]);
        }
        anchoRestaurado = estado.getInt("ancho");
        altoRestaurado = estado.getInt("alto");
        if (anchoRestaurado > 0 && altoRestaurado > 0 && getWidth() > 0) reescalar(getWidth(), getHeight());
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (anchoRestaurado > 0 && altoRestaurado > 0 && w > 0 && h > 0) reescalar(w, h);
    }

    // en horizontal el lienzo cambia de tamaño: escalar la firma para que siga cabiendo
    private void reescalar(int w, int h) {
        float escala = Math.min((float) w / anchoRestaurado, (float) h / altoRestaurado);
        anchoRestaurado = altoRestaurado = 0;
        if (escala == 1) return;
        Matrix matriz = new Matrix();
        matriz.setScale(escala, escala);
        trazo.transform(matriz);
        for (int i = 0; i + 2 < nRegistro; i += 3) {
            registro[i + 1] *= escala;
            registro[i + 2] *= escala;
        }
        invalidate(); // longitud no se escala: si ya era firma válida, lo sigue siendo
    }

    @Override
    public boolean performClick() {
        return super.performClick();
    }
}
