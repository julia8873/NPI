package com.example.nuietsiit.activities.secciones.secretaria;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;

import androidx.annotation.Nullable;
import androidx.core.content.res.ResourcesCompat;

import com.example.nuietsiit.R;

import java.util.Collections;

/**
 * Resguardo de una cita con su línea perforada. Arrastrar el dedo por la línea, de izquierda a derecha, lo va rasgando:
 * las tijeras siguen al dedo, el trozo rasgado se abre y la vibración crece. Soltar a medias = el resguardo se
 * recompone y no pasa nada; llegar al final = el resguardo cae y se avisa con {@link #alRasgar(Runnable)}.
 */
public class ResguardoRasgable extends View {

    private static final float RASGADO_COMPLETO = 0.92f;
    private static final float GIRO_MAXIMO = 9f;      // grados que se abre el trozo rasgado
    private static final float PASO_VIBRACION = 0.05f; // un "tic" cada 5 % rasgado

    private final float dp;
    private final Paint papel = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint sombra = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint borde = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint perforado = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint desgarro = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint texto = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint tijeras = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path linea = new Path();
    private final RectF rect = new RectF();
    private final Vibrator vibrador;
    private final String textoInicial;
    private final String textoArrastrando;

    private float progreso;       // 0..1, cuánto se ha rasgado
    private float tijerasPista = -1; // 0..1 mientras las tijeras enseñan el gesto; -1 = sin pista
    private boolean arrastrando, rasgado;
    private float xInicio, yInicio;
    private float ultimoTic;
    private ValueAnimator animador;
    @Nullable private Runnable alRasgar;

    public ResguardoRasgable(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        dp = getResources().getDisplayMetrics().density;
        vibrador = context.getSystemService(Vibrator.class);
        textoInicial = context.getString(R.string.resguardo_texto_inicial);
        textoArrastrando = context.getString(R.string.resguardo_texto_arrastrando);

        papel.setColor(context.getColor(R.color.nui_superficie));
        sombra.setColor(context.getColor(R.color.nui_papel_sombra));
        borde.setColor(context.getColor(R.color.nui_papel_sombra));
        borde.setStyle(Paint.Style.STROKE);
        borde.setStrokeWidth(getResources().getDimension(R.dimen.nui_borde));
        perforado.setColor(context.getColor(R.color.nui_apagado));
        perforado.setStyle(Paint.Style.STROKE);
        perforado.setStrokeWidth(2 * dp);
        perforado.setPathEffect(new DashPathEffect(new float[]{5 * dp, 4 * dp}, 0));
        desgarro.setColor(context.getColor(R.color.nui_apagado));
        desgarro.setStyle(Paint.Style.STROKE);
        desgarro.setStrokeWidth(1.5f * dp);
        texto.setColor(context.getColor(R.color.nui_apagado));
        texto.setTextSize(getResources().getDimension(R.dimen.nui_texto_etiqueta));
        Typeface fuente = ResourcesCompat.getFont(context, R.font.nui_texto);
        texto.setTypeface(Typeface.create(fuente, Typeface.BOLD));
        texto.setTextAlign(Paint.Align.CENTER);
        tijeras.setColor(context.getColor(R.color.nui_granada));
        tijeras.setStyle(Paint.Style.STROKE);
        tijeras.setStrokeWidth(2.2f * dp);
        tijeras.setStrokeCap(Paint.Cap.ROUND);

        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO); // TalkBack: acción "Cancelar cita" en la ficha
    }

    public void alRasgar(@Nullable Runnable accion) {
        alRasgar = accion;
    }

    /** Vuelve al resguardo entero (al reutilizar la vista en la lista). */
    public void reiniciar() {
        if (animador != null) animador.cancel();
        animate().cancel();
        progreso = 0;
        tijerasPista = -1;
        rasgado = arrastrando = false;
        setTranslationY(0);
        setRotation(0);
        setAlpha(1);
        invalidate();
    }

    /** Las tijeras recorren la línea dos veces para enseñar el gesto (es la pista del tutorial). */
    public void ensenarTijeras() {
        if (rasgado) return;
        animar(0f, 1f, 1400, 2, new DecelerateInterpolator(), v -> tijerasPista = v, () -> tijerasPista = -1);
    }

    /** Rasga del todo sin gesto (TalkBack, o al acabar el arrastre). */
    public void completar() {
        if (rasgado) return;
        rasgado = true;
        arrastrando = false;
        tijerasPista = -1;
        vibrar(VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK));
        animar(progreso, 1f, 120, 0, new DecelerateInterpolator(), v -> progreso = v, () -> {
            // el trozo rasgado se cae
            setPivotX(getWidth());
            setPivotY(0);
            animate().translationY(getHeight() * 1.5f).rotation(-14f).alpha(0f).setDuration(320)
                    .setInterpolator(new AccelerateInterpolator())
                    .withEndAction(() -> { if (alRasgar != null) alRasgar.run(); })
                    .start();
        });
    }

    @Override
    protected void onMeasure(int anchoSpec, int altoSpec) {
        setMeasuredDimension(getDefaultSize(getSuggestedMinimumWidth(), anchoSpec), resolveSize((int) (52 * dp), altoSpec));
    }

    @Override
    protected void onLayout(boolean cambiado, int izq, int arriba, int der, int abajo) {
        super.onLayout(cambiado, izq, arriba, der, abajo);
        // el rasgado empieza junto al borde izquierdo: que el gesto "atrás" del sistema no se lo lleve
        setSystemGestureExclusionRects(Collections.singletonList(new Rect(0, 0, getWidth(), getHeight())));
    }

    // --- Dibujo ---

    @Override
    protected void onDraw(Canvas c) {
        float w = getWidth(), h = getHeight();
        float yLinea = 3 * dp;
        float corte = progreso * w;

        // parte aún unida
        c.save();
        c.clipRect(corte, 0, w, h);
        dibujarPapel(c, w, h, yLinea);
        c.restore();
        linea.reset();
        linea.moveTo(corte, yLinea);
        linea.lineTo(w, yLinea);
        c.drawPath(linea, perforado);

        // trozo rasgado: se abre girando sobre el punto donde va el corte
        if (corte > 0) {
            c.save();
            c.rotate(-GIRO_MAXIMO * progreso, corte, yLinea);
            c.clipRect(0, 0, corte, h);
            dibujarPapel(c, w, h, yLinea);
            dibujarDientes(c, corte, yLinea);
            c.restore();
        }

        // tijeras: siguen al corte, o recorren la línea para enseñar el gesto
        float xTijeras = tijerasPista >= 0 ? tijerasPista * (w - 24 * dp) + 12 * dp : Math.max(corte, 12 * dp);
        if (!rasgado || progreso < 1f) dibujarTijeras(c, xTijeras, yLinea);
    }

    /** Tijeras abiertas apuntando a la derecha, con las hojas sobre la línea (dibujadas: no dependen de la fuente). */
    private void dibujarTijeras(Canvas c, float x, float y) {
        float a = 2.5f * dp + 1.5f * dp * (float) Math.sin(progreso * 40 + (tijerasPista >= 0 ? tijerasPista * 25 : 0));
        c.drawCircle(x - 9 * dp, y - 6 * dp, 3.5f * dp, tijeras); // ojos
        c.drawCircle(x - 9 * dp, y + 6 * dp, 3.5f * dp, tijeras);
        c.drawLine(x - 6 * dp, y - 4 * dp, x + 10 * dp, y + a, tijeras); // hojas cruzadas
        c.drawLine(x - 6 * dp, y + 4 * dp, x + 10 * dp, y - a, tijeras);
    }

    private void dibujarPapel(Canvas c, float w, float h, float yLinea) {
        float s = getResources().getDimension(R.dimen.nui_sombra_papel);
        float r = getResources().getDimension(R.dimen.nui_radio_ficha);
        rect.set(s, yLinea + s, w, h);
        c.drawRoundRect(rect, r, r, sombra);
        rect.set(0, yLinea, w - s, h - s);
        c.drawRoundRect(rect, r, r, papel);
        c.drawRoundRect(rect, r, r, borde);
        String t = arrastrando || progreso > 0 ? textoArrastrando : textoInicial;
        float y = (yLinea + h - s) / 2f - (texto.descent() + texto.ascent()) / 2f;
        c.drawText(t, (w - s) / 2f + 10 * dp, y, texto);
    }

    /** Borde de papel roto (zigzag) en la parte ya rasgada. */
    private void dibujarDientes(Canvas c, float hasta, float yLinea) {
        linea.reset();
        linea.moveTo(0, yLinea);
        float paso = 4 * dp;
        boolean arriba = false;
        for (float x = paso; x < hasta; x += paso) {
            linea.lineTo(x, yLinea + (arriba ? 0 : 2.5f * dp));
            arriba = !arriba;
        }
        linea.lineTo(hasta, yLinea);
        c.drawPath(linea, desgarro);
    }

    // --- Gesto ---

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        if (rasgado) return false;
        float w = getWidth();
        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                // hay que empezar donde va el corte (al principio, donde están las tijeras)
                if (e.getX() > progreso * w + Math.max(56 * dp, w * 0.25f)) return false;
                if (animador != null) animador.cancel();
                tijerasPista = -1;
                arrastrando = true;
                xInicio = e.getX();
                yInicio = e.getY();
                ultimoTic = progreso;
                getParent().requestDisallowInterceptTouchEvent(true);
                invalidate();
                return true;
            case MotionEvent.ACTION_MOVE:
                if (!arrastrando) return false;
                float dx = e.getX() - xInicio, dy = e.getY() - yInicio;
                // si al principio el dedo va claramente en vertical, es desplazar la lista: se le devuelve
                if (progreso < 0.04f && Math.abs(dy) > 24 * dp && Math.abs(dy) > 2 * Math.abs(dx)) {
                    getParent().requestDisallowInterceptTouchEvent(false);
                    soltar();
                    return false;
                }
                float nuevo = Math.min(1f, Math.max(0f, e.getX() / w));
                // el corte sigue al dedo: volver hacia atrás lo cierra (arrepentirse sin soltar)
                if (nuevo != progreso) {
                    progreso = nuevo;
                    if (progreso - ultimoTic >= PASO_VIBRACION) {
                        ultimoTic = progreso;
                        vibrarRasgado(progreso);
                    } else if (progreso < ultimoTic) {
                        ultimoTic = progreso; // al volver no vibra; al avanzar otra vez, sí
                    }
                    invalidate();
                }
                if (progreso >= RASGADO_COMPLETO) completar();
                return true;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (arrastrando) soltar();
                return true;
        }
        return super.onTouchEvent(e);
    }

    /** Soltar a medias: el resguardo se recompone (con un pequeño rebote) y la cita sigue. */
    private void soltar() {
        arrastrando = false;
        if (rasgado) return;
        if (progreso <= 0) { invalidate(); return; }
        animar(progreso, 0f, 350, 0, new OvershootInterpolator(1.2f), v -> progreso = Math.max(0, v), this::invalidate);
    }

    // --- Utilidades ---

    private interface AlValor { void con(float v); }

    private void animar(float desde, float hasta, long ms, int repeticiones, android.animation.TimeInterpolator interp,
                        AlValor alValor, @Nullable Runnable alAcabar) {
        if (animador != null) animador.cancel();
        animador = ValueAnimator.ofFloat(desde, hasta);
        animador.setDuration(ms);
        animador.setRepeatCount(repeticiones);
        animador.setInterpolator(interp);
        animador.addUpdateListener(a -> { alValor.con((float) a.getAnimatedValue()); invalidate(); });
        animador.addListener(new AnimatorListenerAdapter() {
            private boolean cancelado;
            @Override public void onAnimationCancel(Animator a) { cancelado = true; }
            @Override public void onAnimationEnd(Animator a) {
                if (!cancelado && alAcabar != null) alAcabar.run();
                invalidate();
            }
        });
        animador.start();
    }

    /** Un "tic" de papel que se rompe, más fuerte cuanto más rasgado. */
    private void vibrarRasgado(float p) {
        if (vibrador == null || !vibrador.hasVibrator()) return;
        if (vibrador.hasAmplitudeControl()) {
            vibrador.vibrate(VibrationEffect.createOneShot(14, (int) (30 + 225 * p)));
        } else {
            vibrador.vibrate(VibrationEffect.createPredefined(p > 0.6f ? VibrationEffect.EFFECT_CLICK : VibrationEffect.EFFECT_TICK));
        }
    }

    private void vibrar(VibrationEffect efecto) {
        if (vibrador != null && vibrador.hasVibrator()) vibrador.vibrate(efecto);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (animador != null) animador.cancel();
    }
}