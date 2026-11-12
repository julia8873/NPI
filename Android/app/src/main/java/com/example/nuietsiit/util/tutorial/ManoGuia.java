package com.example.nuietsiit.util.tutorial;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.graphics.Path;
import android.graphics.RectF;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.example.nuietsiit.R;

/**
 * Pista animada del tutorial: una mano fantasma que hace el gesto sobre el foco
 * (tocar, mantener, deslizar, pellizcar, dibujar) o un móvil dibujado que se mueve
 * como hay que mover el de verdad (inclinar, agitar, girar, boca abajo, escanear).
 * Se repite en bucle hasta que se para.
 */
class ManoGuia {

    private static final long PAUSA_ENTRE_CICLOS = 700;

    private final FrameLayout capa;
    private final ImageView mano, mano2, movil, onda;
    private final float dp;
    /** Yema del índice dentro del dibujo de la mano (tutorial_mano.xml: 22, 2 de 48x56). */
    private final float yemaX, yemaY;

    private AnimatorSet ciclo;
    private int generacion; // cambia al parar: un ciclo viejo que acaba no relanza nada

    ManoGuia(FrameLayout capa) {
        this.capa = capa;
        dp = capa.getResources().getDisplayMetrics().density;
        yemaX = 22 * dp;
        yemaY = 2 * dp;
        onda = anadir(R.drawable.tutorial_onda);
        movil = anadir(R.drawable.tutorial_movil);
        movil.setCameraDistance(8000 * dp);
        mano2 = anadir(R.drawable.tutorial_mano);
        mano = anadir(R.drawable.tutorial_mano);
        for (ImageView m : new ImageView[]{mano, mano2}) {
            m.setPivotX(yemaX); // al pulsar o girar, la yema se queda en su sitio
            m.setPivotY(yemaY);
        }
    }

    private ImageView anadir(int dibujo) {
        ImageView v = new ImageView(capa.getContext());
        v.setImageResource(dibujo);
        v.setAlpha(0f);
        v.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        capa.addView(v, new FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT));
        return v;
    }

    /**
     * Empieza la pista del gesto. {@code foco} es dónde hacerlo (null: centro de la pantalla);
     * {@code xMovil, yMovil} es el centro del móvil dibujado para los gestos de sensor.
     */
    void mostrar(Gesto gesto, RectF foco, float xMovil, float yMovil) {
        parar();
        int esta = generacion;
        RectF zona = foco != null ? new RectF(foco)
                : new RectF(capa.getWidth() * 0.3f, yMovil - 60 * dp, capa.getWidth() * 0.7f, yMovil + 60 * dp);
        Runnable[] lanzar = new Runnable[1];
        lanzar[0] = () -> {
            if (esta != generacion) return;
            reiniciar();
            ciclo = gesto.esSensor() ? cicloMovil(gesto, xMovil, yMovil) : cicloMano(gesto, zona);
            ciclo.addListener(new AnimatorListenerAdapter() {
                @Override public void onAnimationEnd(Animator a) {
                    if (esta == generacion) capa.postDelayed(lanzar[0], PAUSA_ENTRE_CICLOS);
                }
            });
            ciclo.start();
        };
        lanzar[0].run();
    }

    void parar() {
        generacion++;
        if (ciclo != null) ciclo.cancel();
        ciclo = null;
        reiniciar();
    }

    private void reiniciar() {
        for (ImageView v : new ImageView[]{mano, mano2, movil, onda}) {
            v.animate().cancel();
            v.setAlpha(0f);
            v.setScaleX(1f);
            v.setScaleY(1f);
            v.setRotation(0f);
            v.setRotationX(0f);
            v.setRotationY(0f);
        }
        mano2.setRotation(180f); // la segunda mano viene del lado contrario (pellizcar)
    }

    // ---------- mano ----------

    private AnimatorSet cicloMano(Gesto gesto, RectF zona) {
        float cx = zona.centerX(), cy = zona.centerY();
        float lado = Math.min(zona.width() * 0.3f, 110 * dp);
        AnimatorSet s = new AnimatorSet();
        switch (gesto) {
            case DESLIZAR_IZQ:
            case DESLIZAR_DER: {
                float sentido = gesto == Gesto.DESLIZAR_IZQ ? -1 : 1;
                ponerYema(mano, cx - sentido * lado, cy);
                s.playSequentially(aparecer(mano), pulsar(mano, true),
                        moverYema(mano, cx - sentido * lado, cy, cx + sentido * lado, cy, 650),
                        pulsar(mano, false), desaparecer(mano));
                break;
            }
            case ARRASTRAR: {
                float abajo = cy + Math.max(zona.height() / 2f + 40 * dp, 140 * dp);
                ponerYema(mano, cx, cy);
                s.playSequentially(aparecer(mano), pulsar(mano, true),
                        moverYema(mano, cx, cy, cx, abajo, 800), pulsar(mano, false), desaparecer(mano));
                break;
            }
            case LANZAR: {
                float arriba = cy - Math.max(zona.height() / 2f, 160 * dp);
                ponerYema(mano, cx, cy);
                s.playSequentially(aparecer(mano), pulsar(mano, true),
                        moverYema(mano, cx, cy, cx, arriba, 260), pulsar(mano, false), desaparecer(mano));
                break;
            }
            case PELLIZCAR: {
                float lejos = Math.min(lado, 60 * dp), cerca = 14 * dp;
                ponerYema(mano, cx + lejos, cy + lejos);
                ponerYema(mano2, cx - lejos, cy - lejos);
                AnimatorSet juntar = new AnimatorSet();
                juntar.playTogether(
                        moverYema(mano, cx + lejos, cy + lejos, cx + cerca, cy + cerca, 700),
                        moverYema(mano2, cx - lejos, cy - lejos, cx - cerca, cy - cerca, 700));
                AnimatorSet entrar = new AnimatorSet();
                entrar.playTogether(aparecer(mano), aparecer(mano2));
                AnimatorSet salir = new AnimatorSet();
                salir.playTogether(desaparecer(mano), desaparecer(mano2));
                s.playSequentially(entrar, juntar, salir);
                break;
            }
            case DIBUJAR: {
                Path p = new Path();
                float x0 = cx - lado, ancho = 2 * lado, alto = Math.min(zona.height() * 0.25f, 30 * dp);
                p.moveTo(x0 - yemaX, cy - yemaY);
                p.cubicTo(x0 + ancho * 0.25f - yemaX, cy - alto - yemaY, x0 + ancho * 0.35f - yemaX, cy + alto - yemaY,
                        x0 + ancho * 0.55f - yemaX, cy - yemaY);
                p.cubicTo(x0 + ancho * 0.7f - yemaX, cy - alto - yemaY, x0 + ancho * 0.85f - yemaX, cy + alto - yemaY,
                        x0 + ancho - yemaX, cy - alto * 0.5f - yemaY);
                ponerYema(mano, x0, cy);
                ObjectAnimator trazo = ObjectAnimator.ofFloat(mano, View.X, View.Y, p);
                trazo.setDuration(1300);
                s.playSequentially(aparecer(mano), pulsar(mano, true), trazo, pulsar(mano, false), desaparecer(mano));
                break;
            }
            default: { // TOCAR, MANTENER, ESCRIBIR: se acerca, pulsa y suelta
                boolean mantener = gesto == Gesto.MANTENER;
                float desdeX = cx + 36 * dp, desdeY = cy + 48 * dp;
                ponerYema(mano, desdeX, desdeY);
                AnimatorSet entrar = new AnimatorSet();
                entrar.playTogether(aparecer(mano), moverYema(mano, desdeX, desdeY, cx, cy, 380));
                AnimatorSet pulso = new AnimatorSet();
                pulso.playTogether(pulsar(mano, true), ondaEn(cx, cy, mantener ? 1100 : 450));
                ObjectAnimator quieto = ObjectAnimator.ofFloat(mano, View.ALPHA, 1f, 1f);
                quieto.setDuration(mantener ? 900 : 60);
                s.playSequentially(entrar, pulso, quieto, pulsar(mano, false), desaparecer(mano));
            }
        }
        return s;
    }

    private void ponerYema(View m, float x, float y) {
        m.setX(x - yemaX);
        m.setY(y - yemaY);
    }

    private Animator moverYema(View m, float x0, float y0, float x1, float y1, long ms) {
        AnimatorSet s = new AnimatorSet();
        s.playTogether(ObjectAnimator.ofFloat(m, View.X, x0 - yemaX, x1 - yemaX),
                ObjectAnimator.ofFloat(m, View.Y, y0 - yemaY, y1 - yemaY));
        s.setDuration(ms);
        s.setInterpolator(new AccelerateDecelerateInterpolator());
        return s;
    }

    private Animator pulsar(View m, boolean abajo) {
        float a = abajo ? 1f : 0.85f, b = abajo ? 0.85f : 1f;
        AnimatorSet s = new AnimatorSet();
        s.playTogether(ObjectAnimator.ofFloat(m, View.SCALE_X, a, b), ObjectAnimator.ofFloat(m, View.SCALE_Y, a, b));
        s.setDuration(130);
        return s;
    }

    private Animator ondaEn(float x, float y, long ms) {
        onda.setX(x - 24 * dp);
        onda.setY(y - 24 * dp);
        AnimatorSet s = new AnimatorSet();
        s.playTogether(ObjectAnimator.ofFloat(onda, View.SCALE_X, 0.3f, 1.6f),
                ObjectAnimator.ofFloat(onda, View.SCALE_Y, 0.3f, 1.6f),
                ObjectAnimator.ofFloat(onda, View.ALPHA, 1f, 0f));
        s.setDuration(ms);
        return s;
    }

    private Animator aparecer(View v) {
        return ObjectAnimator.ofFloat(v, View.ALPHA, 0f, 1f).setDuration(250);
    }

    private Animator desaparecer(View v) {
        return ObjectAnimator.ofFloat(v, View.ALPHA, 1f, 0f).setDuration(250);
    }

    // ---------- móvil ----------

    private AnimatorSet cicloMovil(Gesto gesto, float cx, float cy) {
        int ancho = movil.getDrawable().getIntrinsicWidth(), alto = movil.getDrawable().getIntrinsicHeight();
        float x = cx - ancho / 2f, y = cy - alto / 2f;
        movil.setX(x);
        movil.setY(y);
        Animator mover;
        switch (gesto) {
            case INCLINAR:
                mover = ObjectAnimator.ofFloat(movil, View.ROTATION_Y, 0, -35, -35, 35, 35, 0).setDuration(1800);
                break;
            case AGITAR:
                float d = 16 * dp;
                mover = ObjectAnimator.ofFloat(movil, View.X, x, x - d, x + d, x - d, x + d, x - d, x + d, x).setDuration(800);
                break;
            case GIRAR:
                mover = ObjectAnimator.ofFloat(movil, View.ROTATION, 0, 90, 90, 90, 0).setDuration(1800);
                break;
            case BOCA_ABAJO:
                mover = ObjectAnimator.ofFloat(movil, View.ROTATION_X, 0, 180, 180, 180, 0).setDuration(2000);
                break;
            case MESA: // se tumba hasta quedar plano
                mover = ObjectAnimator.ofFloat(movil, View.ROTATION_X, 0, 70, 70, 70, 0).setDuration(2000);
                break;
            case LEVANTAR: // está plano y se levanta
                mover = ObjectAnimator.ofFloat(movil, View.ROTATION_X, 70, 70, 0, 0, 70).setDuration(2000);
                break;
            default: // ESCANEAR: acercar y alejar el móvil del código
                AnimatorSet s = new AnimatorSet();
                s.playTogether(ObjectAnimator.ofFloat(movil, View.SCALE_X, 1f, 1.25f, 1f, 1.25f, 1f),
                        ObjectAnimator.ofFloat(movil, View.SCALE_Y, 1f, 1.25f, 1f, 1.25f, 1f));
                s.setDuration(1600);
                mover = s;
        }
        mover.setInterpolator(new AccelerateDecelerateInterpolator());
        AnimatorSet s = new AnimatorSet();
        s.playSequentially(aparecer(movil), mover, desaparecer(movil));
        return s;
    }
}