package com.example.nuietsiit.activities.main;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.AttributeSet;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AccelerateInterpolator;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.example.nuietsiit.R;

import java.util.ArrayList;
import java.util.List;

public class OrbitalBurbujasView extends View {

    private final String[] nombresSecciones = {
            "Mapa", "Actividades", "Asignaturas", "Profesorado", "Secretaría", "Comedores", "Biblioteca"
    };

    private final int[] iconosRes = {
            R.drawable.ic_seccion_mapa,
            R.drawable.ic_seccion_actividades,
            R.drawable.ic_seccion_asignaturas,
            R.drawable.ic_seccion_profesorado,
            R.drawable.ic_seccion_secretaria,
            R.drawable.ic_seccion_comedores,
            R.drawable.ic_seccion_biblioteca
    };

    // Paleta de colores Aesthetic Pastel / Soft Vibrant
    private final int[] coloresBase = {
            Color.parseColor("#FF6B6B"), // Mapa (Coral Sunset)
            Color.parseColor("#FF9F43"), // Actividades (Soft Mango)
            Color.parseColor("#FECA57"), // Asignaturas (Warm Gold)
            Color.parseColor("#FF8E8E"), // Profesorado (Rose Pink)
            Color.parseColor("#FFA502"), // Secretaría (Vibrant Tangerine)
            Color.parseColor("#E1B12C"), // Comedores (Warm Earth)
            Color.parseColor("#78E08F")  // Biblioteca (Mint / Sage)
    };

    private final PointF[] posiciones = new PointF[7];
    private float radioBurbuja = 0f;

    private Paint paintBurbujaFondo;
    private Paint paintReflejoSuperficie;
    private Paint paintLuzInferior;
    private Paint paintBorde;
    private Paint paintTexto;
    private Paint paintSombra;
    private Paint paintExplosion;
    private Paint paintParticula;

    private int burbujaArrastrada = -1;
    private float lastTouchX, lastTouchY;
    private float initialTouchX, initialTouchY;
    private static final float UBRAL_ARRASTRE = 12f;

    private boolean explotando = false;
    private int burbujaExplotando = -1;
    private float progresoExplosion = 0f;
    private ValueAnimator animadorExplosion;

    private static class Particula {
        float x, y, vx, vy, radio;
        int color;
    }
    private final List<Particula> particulas = new ArrayList<>();

    private OnSeccionSelectedListener listener;

    public interface OnSeccionSelectedListener {
        void onSeccionSelected(int posicion);
    }

    public OrbitalBurbujasView(Context context) {
        super(context);
        init();
    }

    public OrbitalBurbujasView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public OrbitalBurbujasView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        setLayerType(LAYER_TYPE_SOFTWARE, null);

        paintBurbujaFondo = new Paint(Paint.ANTI_ALIAS_FLAG);

        // Reflejo blanco curvo (Glassmorphism top shine)
        paintReflejoSuperficie = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintReflejoSuperficie.setStyle(Paint.Style.FILL);
        paintReflejoSuperficie.setColor(Color.argb(140, 255, 255, 255));

        // Luz tenue inferior (Efecto volumen 3D)
        paintLuzInferior = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintLuzInferior.setStyle(Paint.Style.FILL);
        paintLuzInferior.setColor(Color.argb(45, 255, 255, 255));

        // Borde cristalino translúcido
        paintBorde = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintBorde.setStyle(Paint.Style.STROKE);
        paintBorde.setColor(Color.argb(200, 255, 255, 255));
        paintBorde.setStrokeWidth(5f);

        // Sombra de gota difusa (Soft Drop Shadow)
        paintSombra = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintSombra.setColor(Color.argb(40, 0, 0, 0));
        paintSombra.setStyle(Paint.Style.FILL);

        // Tipografía limpia y definida
        paintTexto = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintTexto.setColor(Color.WHITE);
        paintTexto.setTextAlign(Paint.Align.CENTER);
        paintTexto.setFakeBoldText(true);
        paintTexto.setShadowLayer(6f, 0f, 2f, Color.argb(160, 0, 0, 0));

        paintExplosion = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintExplosion.setStyle(Paint.Style.STROKE);

        paintParticula = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintParticula.setStyle(Paint.Style.FILL);

        for (int i = 0; i < 7; i++) {
            posiciones[i] = new PointF();
        }
    }

    public void setOnSeccionSelectedListener(OnSeccionSelectedListener listener) {
        this.listener = listener;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        float cx = w / 2f;
        float cy = h / 2f;

        // Tamaño más grande (~15.5% del menor lado de pantalla)
        radioBurbuja = Math.min(w, h) * 0.155f;

        // Distancia orbital ajustada para acomodar el tamaño mayor
        float orbitaRadio = radioBurbuja * 2.3f;

        // 1. Burbuja central (Mapa)
        posiciones[0].set(cx, cy);

        // 2. Distribución circular perfecta en 360° para las 6 burbujas restantes
        int numBurbujasExteriores = 6;
        float pasoAngulo = (float) (2 * Math.PI / numBurbujasExteriores);
        float anguloInicial = (float) (-Math.PI / 2); // Inicia a las 12h

        for (int i = 1; i <= numBurbujasExteriores; i++) {
            float angulo = anguloInicial + (i - 1) * pasoAngulo;
            posiciones[i].set(
                    cx + (float) (Math.cos(angulo) * orbitaRadio),
                    cy + (float) (Math.sin(angulo) * orbitaRadio)
            );
        }

        // Texto proporcional al nuevo tamaño
        paintTexto.setTextSize(radioBurbuja * 0.25f);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        for (int i = 0; i < 7; i++) {
            if (explotando && i == burbujaExplotando) continue;

            PointF p = posiciones[i];
            float r = radioBurbuja;

            // 1. Sombra Suave Volumétrica
            canvas.drawCircle(p.x + 4f, p.y + 8f, r * 0.98f, paintSombra);

            // 2. Gradiente Principal Vibrant Soft
            int baseColor = coloresBase[i];
            int colorHighlight = Color.argb(255,
                    Math.min(255, Color.red(baseColor) + 50),
                    Math.min(255, Color.green(baseColor) + 50),
                    Math.min(255, Color.blue(baseColor) + 50));
            int colorShadow = Color.argb(240,
                    Math.max(0, Color.red(baseColor) - 35),
                    Math.max(0, Color.green(baseColor) - 35),
                    Math.max(0, Color.blue(baseColor) - 35));

            RadialGradient shader = new RadialGradient(
                    p.x - r * 0.35f,
                    p.y - r * 0.35f,
                    r * 1.4f,
                    new int[]{ colorHighlight, baseColor, colorShadow },
                    new float[]{ 0.0f, 0.5f, 1.0f },
                    Shader.TileMode.CLAMP
            );
            paintBurbujaFondo.setShader(shader);
            canvas.drawCircle(p.x, p.y, r, paintBurbujaFondo);

            // 3. Luz de ambiente inferior (Reflejo 3D inferior)
            canvas.drawCircle(p.x, p.y + r * 0.45f, r * 0.45f, paintLuzInferior);

            // 4. Reflejo Especular Superior (Efecto Burbuja de Cristal)
            canvas.drawOval(
                    p.x - r * 0.52f,
                    p.y - r * 0.72f,
                    p.x + r * 0.52f,
                    p.y - r * 0.18f,
                    paintReflejoSuperficie
            );

            // 5. Borde Fino Translúcido
            canvas.drawCircle(p.x, p.y, r, paintBorde);

            // 6. Ícono Ampliado
            Drawable icon = ContextCompat.getDrawable(getContext(), iconosRes[i]);
            if (icon != null) {
                int tam = (int) (r * 0.52f); // Ícono más amplio y claro
                int left = (int) (p.x - tam / 2f);
                int top = (int) (p.y - tam * 1.08f);
                icon.setBounds(left, top, left + tam, top + tam);
                icon.setTint(Color.WHITE);
                icon.draw(canvas);
            }

            // 7. Texto de Sección
            canvas.drawText(nombresSecciones[i], p.x, p.y + r * 0.5f, paintTexto);
        }

        // Animación de Explosión / Pop
        if (explotando && burbujaExplotando >= 0) {
            PointF p = posiciones[burbujaExplotando];
            float rBase = radioBurbuja;

            float radioAnillo = rBase * (1.0f + progresoExplosion * 1.6f);
            int alphaAnillo = (int) ((1f - progresoExplosion) * 255);
            paintExplosion.setColor(coloresBase[burbujaExplotando]);
            paintExplosion.setAlpha(Math.max(0, alphaAnillo));
            paintExplosion.setStrokeWidth(14f * (1f - progresoExplosion));
            canvas.drawCircle(p.x, p.y, radioAnillo, paintExplosion);

            for (Particula pt : particulas) {
                paintParticula.setColor(pt.color);
                paintParticula.setAlpha(Math.max(0, alphaAnillo));
                canvas.drawCircle(pt.x, pt.y, pt.radio * (1f - progresoExplosion * 0.5f), paintParticula);
            }
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (explotando) return true;

        float x = event.getX();
        float y = event.getY();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                burbujaArrastrada = obtenerBurbujaEnTocada(x, y);
                initialTouchX = lastTouchX = x;
                initialTouchY = lastTouchY = y;
                if (burbujaArrastrada >= 0) {
                    vibrarSuave();
                }
                break;

            case MotionEvent.ACTION_MOVE:
                if (burbujaArrastrada >= 0) {
                    float dx = x - lastTouchX;
                    float dy = y - lastTouchY;
                    posiciones[burbujaArrastrada].x += dx;
                    posiciones[burbujaArrastrada].y += dy;
                    lastTouchX = x;
                    lastTouchY = y;
                    invalidate();
                }
                break;

            case MotionEvent.ACTION_UP:
                if (burbujaArrastrada >= 0) {
                    int idBurbuja = burbujaArrastrada;
                    burbujaArrastrada = -1;

                    float distDesplazada = (float) Math.hypot(x - initialTouchX, y - initialTouchY);
                    if (distDesplazada < UBRAL_ARRASTRE) {
                        iniciarExplosionYEntrar(idBurbuja);
                    }
                }
                break;

            case MotionEvent.ACTION_CANCEL:
                burbujaArrastrada = -1;
                break;
        }
        return true;
    }

    private int obtenerBurbujaEnTocada(float x, float y) {
        for (int i = 0; i < 7; i++) {
            PointF p = posiciones[i];
            float dist = (float) Math.hypot(x - p.x, y - p.y);
            if (dist <= radioBurbuja * 1.05f) {
                return i;
            }
        }
        return -1;
    }

    private void iniciarExplosionYEntrar(int indice) {
        if (explotando) return;
        explotando = true;
        burbujaExplotando = indice;
        performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);

        PointF p = posiciones[indice];
        particulas.clear();
        int color = coloresBase[indice];
        for (int i = 0; i < 22; i++) {
            Particula pt = new Particula();
            pt.x = p.x;
            pt.y = p.y;
            double angulo = Math.random() * Math.PI * 2;
            float velocidad = (float) (Math.random() * 240f + 100f);
            pt.vx = (float) (Math.cos(angulo) * velocidad);
            pt.vy = (float) (Math.sin(angulo) * velocidad);
            pt.radio = (float) (Math.random() * 16f + 7f);
            pt.color = color;
            particulas.add(pt);
        }

        if (animadorExplosion != null && animadorExplosion.isRunning()) {
            animadorExplosion.cancel();
        }

        animadorExplosion = ValueAnimator.ofFloat(0f, 1f);
        animadorExplosion.setDuration(320);
        animadorExplosion.setInterpolator(new AccelerateInterpolator());
        animadorExplosion.addUpdateListener(animation -> {
            progresoExplosion = (float) animation.getAnimatedValue();
            float dt = 0.03f;
            for (Particula pt : particulas) {
                pt.x += pt.vx * dt;
                pt.y += pt.vy * dt;
            }
            invalidate();
        });

        animadorExplosion.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                explotando = false;
                if (listener != null) {
                    listener.onSeccionSelected(indice);
                }
            }
        });

        animadorExplosion.start();
    }

    private void vibrarSuave() {
        try {
            Vibrator v = (Vibrator) getContext().getSystemService(Context.VIBRATOR_SERVICE);
            if (v != null && v.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    v.vibrate(VibrationEffect.createOneShot(18, VibrationEffect.DEFAULT_AMPLITUDE));
                } else {
                    v.vibrate(18);
                }
            }
        } catch (Exception ignored) {}
    }
}