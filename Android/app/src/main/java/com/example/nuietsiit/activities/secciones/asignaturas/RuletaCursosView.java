package com.example.nuietsiit.activities.secciones.asignaturas;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.Nullable;

public class RuletaCursosView extends View {

    private Paint paintSector;
    private Paint paintTexto;
    private Paint paintCentro;
    private Paint paintIndicador;
    private RectF rectF;

    private int numCursos = 4; // Por defecto 4 años
    private float anguloActual = 0f; // Ángulo de rotación de la rueda
    private float ultimoX, ultimoY;
    private float centroX, centroY, radio;
    private float radioBotonCentro;

    // Colores alternados para las secciones (tonos acordes con la estética naranja)
    private final int[] coloresSectores = {
            Color.parseColor("#FF9800"), // Naranja principal
            Color.parseColor("#F57C00"), // Naranja más oscuro
            Color.parseColor("#FFB74D"), // Naranja claro
            Color.parseColor("#E65100"), // Naranja intenso
            Color.parseColor("#FF8A65")  // Naranja suave
    };

    private OnCursoSelectedListener listener;

    public interface OnCursoSelectedListener {
        void onCursoSelected(int curso);
    }

    public RuletaCursosView(Context context) {
        super(context);
        init();
    }

    public RuletaCursosView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        paintSector = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintSector.setStyle(Paint.Style.FILL);

        paintTexto = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintTexto.setColor(Color.WHITE);
        paintTexto.setTextAlign(Paint.Align.CENTER);
        paintTexto.setFakeBoldText(true);

        paintCentro = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintCentro.setColor(Color.parseColor("#333333")); // Botón central oscuro
        paintCentro.setStyle(Paint.Style.FILL);

        paintIndicador = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintIndicador.setColor(Color.RED);
        paintIndicador.setStyle(Paint.Style.FILL);

        rectF = new RectF();
    }

    public void setOnCursoSelectedListener(OnCursoSelectedListener listener) {
        this.listener = listener;
    }

    /**
     * Establece el número de cursos según el grado (ej: 4 para Info, 5 para DGIIM)
     */
    public void setNumCursos(int numCursos) {
        if (numCursos > 0) {
            this.numCursos = numCursos;
            this.anguloActual = 0f; // Resetear ángulo
            invalidate(); // Redibujar
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        centroX = w / 2f;
        centroY = h / 2f;
        radio = Math.min(w, h) / 2f - 30f; // Dejar un margen
        radioBotonCentro = radio * 0.35f;  // Tamaño del botón "IR" central

        rectF.set(centroX - radio, centroY - radio, centroX + radio, centroY + radio);
        paintTexto.setTextSize(radio * 0.18f);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (numCursos <= 0) return;

        float barridoAngulo = 360f / numCursos;

        // 1. Dibujar los sectores/porciones de la ruleta
        for (int i = 0; i < numCursos; i++) {
            paintSector.setColor(coloresSectores[i % coloresSectores.length]);
            float inicioAngulo = anguloActual + (i * barridoAngulo);

            canvas.drawArc(rectF, inicioAngulo, barridoAngulo, true, paintSector);

            // Dibujar el texto del curso (1º, 2º, etc.) en cada porción
            float anguloTextoRad = (float) Math.toRadians(inicioAngulo + barridoAngulo / 2f);
            float distanciaTexto = radio * 0.65f;
            float textoX = centroX + (float) (distanciaTexto * Math.cos(anguloTextoRad));
            float textoY = centroY + (float) (distanciaTexto * Math.sin(anguloTextoRad)) + (paintTexto.getTextSize() / 3f);

            canvas.drawText((i + 1) + "º", textoX, textoY, paintTexto);
        }

        // 2. Dibujar el botón central "IR"
        canvas.drawCircle(centroX, centroY, radioBotonCentro, paintCentro);

        Paint paintTextoIr = new Paint(paintTexto);
        paintTextoIr.setTextSize(radioBotonCentro * 0.5f);
        canvas.drawText("IR", centroX, centroY + (paintTextoIr.getTextSize() / 3f), paintTextoIr);

        // 3. Dibujar marcador superior (flecha o indicador fija arriba)
        float tamañoIndicador = 25f;
        canvas.drawCircle(centroX, centroY - radio, tamañoIndicador, paintIndicador);
    }

    /**
     * Gestión del toque táctil: Giro con el dedo y clic en el botón "IR" central
     */
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                // Comprobar si ha pulsado sobre el botón central "IR"
                float distDesdeCentro = (float) Math.hypot(x - centroX, y - centroY);
                if (distDesdeCentro <= radioBotonCentro) {
                    confirmarSeleccionCurso();
                    return true;
                }
                ultimoX = x;
                ultimoY = y;
                break;

            case MotionEvent.ACTION_MOVE:
                // Calcular ángulo girado con el dedo usando Math.atan2
                double anguloPrevio = Math.atan2(ultimoY - centroY, ultimoX - centroX);
                double anguloNuevo = Math.atan2(y - centroY, x - centroX);
                float deltaAngulo = (float) Math.toDegrees(anguloNuevo - anguloPrevio);

                anguloActual = (anguloActual + deltaAngulo) % 360f;
                ultimoX = x;
                ultimoY = y;
                invalidate(); // Redibujar ruleta con la nueva rotación
                break;
        }
        return true;
    }

    /**
     * Calcula qué curso ha quedado seleccionado debajo del indicador superior al pulsar "IR"
     */
    private void confirmarSeleccionCurso() {
        if (listener == null) return;

        // El indicador está arriba (-90 grados en el sistema de coordenadas)
        float anguloEfectivo = (270f - anguloActual) % 360f;
        if (anguloEfectivo < 0) anguloEfectivo += 360f;

        float barridoAngulo = 360f / numCursos;
        int cursoSeleccionado = (int) (anguloEfectivo / barridoAngulo) + 1;

        if (cursoSeleccionado > numCursos) cursoSeleccionado = numCursos;

        listener.onCursoSelected(cursoSeleccionado);
    }
}