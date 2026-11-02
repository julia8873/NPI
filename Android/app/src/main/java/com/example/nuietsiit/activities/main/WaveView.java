package com.example.nuietsiit.activities.main;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.LinearInterpolator;

public class WaveView extends View {

    private Paint wavePaint;
    private Path wavePath;
    private float progress = 0f; // 0 = abajo (vacío), 1 = arriba (pantalla cubierta)
    private float waveOffset = 0f;

    public WaveView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        wavePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        wavePaint.setColor(Color.parseColor("#FF8C42")); // Color Azul de la ola
        wavePaint.setStyle(Paint.Style.FILL);

        wavePath = new Path();

        // Animación constante para que la ola "se mueva" horizontalmente
        ValueAnimator waveAnimator = ValueAnimator.ofFloat(0, 1f);
        waveAnimator.setDuration(1000);
        waveAnimator.setRepeatCount(ValueAnimator.INFINITE);
        waveAnimator.setInterpolator(new LinearInterpolator());
        waveAnimator.addUpdateListener(animation -> {
            waveOffset = (float) animation.getAnimatedValue();
            invalidate();
        });
        waveAnimator.start();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        wavePath.reset();

        int width = getWidth();
        int height = getHeight();

        // Calculamos para que en el punto máximo (progress = 1.0) sobrepase ligeramente la pantalla
        // y asegure cubrir el 100% hasta arriba del todo
        float currentWaveHeight = height * (1 - (progress * 1.15f));

        float waveFrequency = 1.5f; // Cantidad de crestas
        float waveAmplitude = 60f;  // Altura de la cresta en px

        wavePath.moveTo(0, height);
        wavePath.lineTo(0, currentWaveHeight);

        for (int x = 0; x <= width; x += 10) {
            float y = (float) (Math.sin((x / (float) width * waveFrequency * 2 * Math.PI) + (waveOffset * 2 * Math.PI)) * waveAmplitude) + currentWaveHeight;
            wavePath.lineTo(x, y);
        }

        wavePath.lineTo(width, height);
        wavePath.close();

        canvas.drawPath(wavePath, wavePaint);
    }
    // Animación 1: SOLO SUBE (se ejecuta en la pantalla que sacudes)
    public void startWaveUp(Runnable onCovered) {
        ValueAnimator fillAnimator = ValueAnimator.ofFloat(0f, 1f);
        fillAnimator.setDuration(1000); // Tiempos ajustados a 1s
        fillAnimator.addUpdateListener(anim -> {
            progress = (float) anim.getAnimatedValue();
            invalidate();
        });

        fillAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                if (onCovered != null) onCovered.run();
            }
        });

        fillAnimator.start();
    }

    // Animación 2: SOLO BAJA (se ejecuta al entrar en MenuPrincipal)
    public void startWaveDown(Runnable onComplete) {
        // Forzamos a que la vista se mida en pantalla antes de animar
        post(() -> {
            progress = 1f; // Empieza llena arriba
            setVisibility(VISIBLE);
            bringToFront();

            ValueAnimator drainAnimator = ValueAnimator.ofFloat(1f, 0f);
            drainAnimator.setDuration(1200); // Duración de bajada
            drainAnimator.addUpdateListener(anim -> {
                progress = (float) anim.getAnimatedValue();
                invalidate();
            });

            drainAnimator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    setVisibility(GONE);
                    if (onComplete != null) onComplete.run();
                }
            });

            drainAnimator.start();
        });
    }
}