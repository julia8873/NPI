package com.example.nuietsiit.util.tutorial;

import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.accessibility.AccessibilityManager;
import android.view.animation.LinearInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.nuietsiit.R;

/**
 * Capa del tutorial sobre {@code android.R.id.content}: oscurece la pantalla salvo un foco
 * (hueco redondeado) sobre la vista del paso. Los toques que empiezan en el foco se reenvían
 * a la pantalla de verdad (con todos sus dedos, para poder pellizcar); el resto se bloquea.
 * Lleva el bocadillo (texto, progreso, saltar), la pista animada y la celebración del acierto.
 */
@SuppressLint("ViewConstructor")
class CapaTutorial extends FrameLayout {

    private static final long ESPERA_PISTA = 4000;
    private static final long ESPERA_RESALTAR_SALTAR = 10000;
    private static final long DURACION_ACIERTO = 1100;
    private static final long DURACION_DESPEDIDA = 3500;
    private static final int COLOR_VELO = 0xB814110F; // tinta al 72 %

    private final Activity actividad;
    private final Tutorial tutorial;
    private final float dp;
    private final boolean conTalkBack;

    // foco
    private final Paint pinturaVelo = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pinturaBorde = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pinturaPulso = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path velo = new Path();
    private final RectF foco = new RectF();
    private final RectF nuevoFoco = new RectF();
    private final RectF anillo = new RectF();
    private final int[] pos = new int[2];
    private final int[] posCapa = new int[2];
    private boolean hayFoco;
    private float fasePulso;
    private ValueAnimator pulso;

    // bocadillo
    private final LinearLayout bocadillo;
    private final LinearLayout puntos;
    private final TextView tvProgreso, tvTexto, btnSaltarTodo, btnPaso;
    private final TextView marcaAcierto;
    private final ManoGuia mano;

    // estado del paso
    private Paso paso;
    private View objetivo;
    private boolean celebrando, despidiendo;

    // gesto que empezó en el foco: se reenvía entero a la pantalla
    private View destino;
    private float xAbajo, yAbajo;
    private long tAbajo;
    private boolean movido;

    private final Runnable pista = this::mostrarPista;
    private final Runnable resaltarSaltar = this::resaltarSaltar;
    private final Runnable trasAcierto = this::trasAcierto;
    private final Runnable quitarSola = this::quitar;
    private final ViewTreeObserver.OnPreDrawListener seguirObjetivo = () -> {
        // la vista aún no estaba (se crea al abrir algo, o en otra hoja) o ha dejado de verse: se vuelve a buscar
        if (paso != null && paso.idVista != 0 && (objetivo == null || !objetivo.isShown())) {
            objetivo = buscarVisible(pantallaDebajo(), paso.idVista);
        }
        actualizarFoco();
        return true;
    };

    CapaTutorial(Activity actividad, Tutorial tutorial) {
        super(actividad);
        this.actividad = actividad;
        this.tutorial = tutorial;
        dp = getResources().getDisplayMetrics().density;
        AccessibilityManager am = (AccessibilityManager) actividad.getSystemService(Context.ACCESSIBILITY_SERVICE);
        conTalkBack = am != null && am.isEnabled() && am.isTouchExplorationEnabled();
        setWillNotDraw(false);
        setClickable(true);
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);

        int acento = color(R.color.nui_acento);
        pinturaVelo.setColor(COLOR_VELO);
        pinturaBorde.setStyle(Paint.Style.STROKE);
        pinturaBorde.setStrokeWidth(3 * dp);
        pinturaBorde.setColor(acento);
        pinturaPulso.setStyle(Paint.Style.STROKE);
        pinturaPulso.setStrokeWidth(2 * dp);
        pinturaPulso.setColor(acento);

        mano = new ManoGuia(this);

        // ---------- bocadillo ----------
        bocadillo = new LinearLayout(actividad);
        bocadillo.setOrientation(LinearLayout.VERTICAL);
        int sombra = (int) (5 * dp);
        bocadillo.setPadding((int) (18 * dp), (int) (12 * dp), (int) (18 * dp) + sombra, (int) (16 * dp) + sombra);
        bocadillo.setBackground(fondoBocadillo(sombra));
        bocadillo.setClickable(true);

        LinearLayout arriba = new LinearLayout(actividad);
        arriba.setGravity(Gravity.CENTER_VERTICAL);
        puntos = new LinearLayout(actividad);
        puntos.setGravity(Gravity.CENTER_VERTICAL);
        arriba.addView(puntos);
        tvProgreso = texto(13, R.color.nui_apagado, false);
        tvProgreso.setPadding((int) (8 * dp), 0, 0, 0);
        arriba.addView(tvProgreso);
        arriba.addView(new View(actividad), new LinearLayout.LayoutParams(0, 1, 1f));
        btnSaltarTodo = texto(14, R.color.nui_apagado, false);
        btnSaltarTodo.setText("Saltar tutorial");
        btnSaltarTodo.setPaintFlags(btnSaltarTodo.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
        btnSaltarTodo.setMinHeight((int) (44 * dp));
        btnSaltarTodo.setGravity(Gravity.CENTER_VERTICAL);
        btnSaltarTodo.setPadding((int) (8 * dp), 0, 0, 0);
        btnSaltarTodo.setOnClickListener(v -> tutorial.saltarTodo());
        arriba.addView(btnSaltarTodo);
        bocadillo.addView(arriba);

        tvTexto = texto(18, R.color.nui_tinta, true);
        tvTexto.setLineSpacing(2 * dp, 1f);
        LinearLayout.LayoutParams lpTexto = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lpTexto.topMargin = (int) (4 * dp);
        bocadillo.addView(tvTexto, lpTexto);

        btnPaso = texto(15, R.color.nui_tinta, true);
        btnPaso.setGravity(Gravity.CENTER);
        btnPaso.setMinHeight((int) (44 * dp));
        btnPaso.setPadding((int) (16 * dp), 0, (int) (16 * dp), 0);
        btnPaso.setOnClickListener(v -> pulsarBotonPaso());
        LinearLayout.LayoutParams lpBoton = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lpBoton.gravity = Gravity.END;
        lpBoton.topMargin = (int) (12 * dp);
        bocadillo.addView(btnPaso, lpBoton);

        int ancho = (int) Math.min(getResources().getDisplayMetrics().widthPixels - 32 * dp, 440 * dp);
        addView(bocadillo, new LayoutParams(ancho, LayoutParams.WRAP_CONTENT, Gravity.TOP | Gravity.CENTER_HORIZONTAL));
        // al cambiar el texto cambia el alto: recolocar
        bocadillo.addOnLayoutChangeListener((v, l, t, r, b, lv, tv, rv, bv) -> {
            if (b - t != bv - tv) colocarBocadillo();
        });

        // ✓ del acierto
        marcaAcierto = new TextView(actividad);
        marcaAcierto.setText("✓");
        marcaAcierto.setTextSize(34);
        marcaAcierto.setTypeface(Typeface.DEFAULT_BOLD);
        marcaAcierto.setTextColor(color(R.color.nui_tinta));
        marcaAcierto.setGravity(Gravity.CENTER);
        GradientDrawable circulo = new GradientDrawable();
        circulo.setShape(GradientDrawable.OVAL);
        circulo.setColor(acento);
        circulo.setStroke((int) (3 * dp), color(R.color.nui_tinta));
        marcaAcierto.setBackground(circulo);
        marcaAcierto.setAlpha(0f);
        marcaAcierto.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
        addView(marcaAcierto, new LayoutParams((int) (72 * dp), (int) (72 * dp)));
    }

    // ---------- pasos ----------

    void mostrarPaso(Paso nuevo, int indice, int total) {
        removeCallbacks(pista);
        removeCallbacks(resaltarSaltar);
        mano.parar();
        celebrando = false;
        paso = nuevo;
        objetivo = nuevo.idVista != 0 ? buscarVisible(pantallaDebajo(), nuevo.idVista) : null;
        actualizarFoco();

        pintarPuntos(indice, total);
        tvProgreso.setText((indice + 1) + "/" + total);
        tvProgreso.setContentDescription("Paso " + (indice + 1) + " de " + total);
        tvTexto.setText(nuevo.texto);
        btnPaso.setText(nuevo.esInfo() ? "Entendido" : "Saltar paso");
        // con TalkBack muchos gestos no se pueden hacer, y escanear necesita un código a mano:
        // saltar se ofrece desde el principio
        estiloBoton(btnPaso, nuevo.esInfo() || conTalkBack || nuevo.gesto == Gesto.ESCANEAR);

        bocadillo.setAlpha(0f);
        bocadillo.setTranslationY(0);
        post(() -> {
            colocarBocadillo();
            float y = bocadillo.getY();
            bocadillo.setY(y + 12 * dp);
            bocadillo.animate().alpha(1f).y(y).setDuration(220).start();
        });
        announceForAccessibility(nuevo.texto);

        if (!nuevo.esInfo()) {
            postDelayed(pista, ESPERA_PISTA);
            postDelayed(resaltarSaltar, ESPERA_RESALTAR_SALTAR);
        }
    }

    /** El usuario hizo el gesto del paso: vibración, ✓ con rebote, "¡Eso es!" y al siguiente. */
    void acierto() {
        if (paso == null || paso.esInfo() || celebrando || despidiendo) return;
        celebrando = true;
        removeCallbacks(pista);
        removeCallbacks(resaltarSaltar);
        mano.parar();
        vibrar();
        tvTexto.setText("¡Eso es!");
        announceForAccessibility("¡Eso es!");

        float cx = hayFoco ? foco.centerX() : getWidth() / 2f;
        float cy = hayFoco ? foco.centerY() : bocadillo.getY() - 60 * dp;
        marcaAcierto.setX(cx - marcaAcierto.getWidth() / 2f);
        marcaAcierto.setY(cy - marcaAcierto.getHeight() / 2f);
        marcaAcierto.setScaleX(0.2f);
        marcaAcierto.setScaleY(0.2f);
        marcaAcierto.setAlpha(1f);
        marcaAcierto.animate().scaleX(1f).scaleY(1f).setInterpolator(new OvershootInterpolator(3f))
                .setDuration(380).start();
        postDelayed(trasAcierto, DURACION_ACIERTO);
    }

    private void resaltarSaltar() {
        estiloBoton(btnPaso, true);
    }

    private void trasAcierto() {
        marcaAcierto.animate().alpha(0f).setDuration(180).start();
        tutorial.siguiente();
    }

    /** Fin: sin foco, "¡Listo!" y se va sola. */
    void despedirse() {
        despidiendo = true;
        paso = null;
        objetivo = null;
        actualizarFoco();
        mano.parar();
        removeCallbacks(pista);
        removeCallbacks(resaltarSaltar);
        for (int i = 0; i < puntos.getChildCount(); i++) puntos.getChildAt(i).setBackground(punto(true));
        tvProgreso.setText("");
        btnSaltarTodo.setVisibility(GONE);
        tvTexto.setText("¡Listo! Toca ? para repetirlo");
        announceForAccessibility("¡Listo! Toca el botón de ayuda para repetirlo");
        btnPaso.setText("Cerrar");
        estiloBoton(btnPaso, true);
        post(this::colocarBocadillo);
        postDelayed(quitarSola, DURACION_DESPEDIDA);
    }

    private void pulsarBotonPaso() {
        if (despidiendo) quitar();
        else if (!celebrando) tutorial.siguiente(); // "Entendido" o "Saltar paso"
    }

    /** Se va con un fundido y avisa al tutorial. */
    void quitar() {
        if (!isEnabled()) return; // ya se está yendo
        cancelarEsperas();
        mano.parar();
        setEnabled(false);
        animate().alpha(0f).setDuration(200).withEndAction(() -> {
            ViewGroup padre = (ViewGroup) getParent();
            if (padre != null) padre.removeView(this);
        }).start();
        tutorial.alQuitarCapa(this);
    }

    private void cancelarEsperas() {
        removeCallbacks(pista);
        removeCallbacks(resaltarSaltar);
        removeCallbacks(trasAcierto);
        removeCallbacks(quitarSola);
    }

    private void mostrarPista() {
        if (paso == null || paso.gesto == null || celebrando) return;
        float xMovil = getWidth() / 2f;
        float yMovil = bocadillo.getY() - 70 * dp;
        if (paso.pista != null && objetivo != null) paso.pista.accept(objetivo);
        else mano.mostrar(paso.gesto, hayFoco ? foco : null, xMovil, yMovil);
    }

    /**
     * La primera vista con ese id que de verdad se ve: en listas (o páginas de un ViewPager) hay muchas con el
     * mismo id y findViewById daría la primera aunque esté oculta o fuera de la pantalla.
     */
    static View buscarVisible(View v, int id) {
        if (v == null || v.getVisibility() != VISIBLE) return null;
        if (v.getId() == id && v.isShown() && v.getGlobalVisibleRect(new android.graphics.Rect())) return v;
        if (!(v instanceof ViewGroup)) return null;
        ViewGroup g = (ViewGroup) v;
        for (int i = 0; i < g.getChildCount(); i++) {
            View encontrada = buscarVisible(g.getChildAt(i), id);
            if (encontrada != null) return encontrada;
        }
        return null;
    }

    // ---------- foco ----------

    private void actualizarFoco() {
        boolean hay = objetivo != null && objetivo.isShown() && objetivo.getWidth() > 0;
        if (hay) {
            objetivo.getLocationInWindow(pos);
            getLocationInWindow(posCapa);
            float m = 8 * dp;
            float x = pos[0] - posCapa[0], y = pos[1] - posCapa[1];
            nuevoFoco.set(x - m, y - m, x + objetivo.getWidth() + m, y + objetivo.getHeight() + m);
        }
        if (hay == hayFoco && (!hay || nuevoFoco.equals(foco))) return;
        hayFoco = hay;
        if (hay) foco.set(nuevoFoco);
        invalidate();
        colocarBocadillo();
    }

    /** Debajo del foco si cabe; si no, encima; si tampoco, abajo del todo. Sin foco, abajo. */
    private void colocarBocadillo() {
        int alto = bocadillo.getHeight();
        if (alto == 0 || getHeight() == 0) return;
        Insets barras = barrasSistema();
        float margen = 16 * dp;
        float abajo = getHeight() - barras.bottom - margen - alto;
        float y = abajo - margen;
        if (hayFoco) {
            if (foco.bottom + 2 * margen + alto <= getHeight() - barras.bottom) y = foco.bottom + margen;
            else if (foco.top - 2 * margen - alto >= barras.top) y = foco.top - margen - alto;
            else y = abajo;
        }
        if (bocadillo.getAlpha() < 1f) bocadillo.setY(y); // aún entrando: sin animar
        else bocadillo.animate().y(y).setDuration(180).start();
    }

    private Insets barrasSistema() {
        WindowInsetsCompat w = ViewCompat.getRootWindowInsets(this);
        return w == null ? Insets.NONE : w.getInsets(WindowInsetsCompat.Type.systemBars());
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float r = 16 * dp;
        velo.reset();
        velo.setFillType(Path.FillType.EVEN_ODD);
        velo.addRect(0, 0, getWidth(), getHeight(), Path.Direction.CW);
        if (hayFoco) velo.addRoundRect(foco, r, r, Path.Direction.CW);
        canvas.drawPath(velo, pinturaVelo);
        if (!hayFoco) return;
        canvas.drawRoundRect(foco, r, r, pinturaBorde);
        float crece = fasePulso * 12 * dp;
        anillo.set(foco.left - crece, foco.top - crece, foco.right + crece, foco.bottom + crece);
        pinturaPulso.setAlpha((int) (255 * (1f - fasePulso)));
        canvas.drawRoundRect(anillo, r + crece, r + crece, pinturaPulso);
    }

    @Override
    protected void onSizeChanged(int w, int h, int ow, int oh) {
        super.onSizeChanged(w, h, ow, oh);
        post(this::colocarBocadillo);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        getViewTreeObserver().addOnPreDrawListener(seguirObjetivo);
        setAlpha(0f);
        animate().alpha(1f).setDuration(220).start();
        pulso = ValueAnimator.ofFloat(0f, 1f);
        pulso.setDuration(1400);
        pulso.setRepeatCount(ValueAnimator.INFINITE);
        pulso.setInterpolator(new LinearInterpolator());
        pulso.addUpdateListener(a -> {
            fasePulso = (float) a.getAnimatedValue();
            if (hayFoco) invalidate();
        });
        pulso.start();
    }

    @Override
    protected void onDetachedFromWindow() {
        getViewTreeObserver().removeOnPreDrawListener(seguirObjetivo);
        if (pulso != null) pulso.cancel();
        cancelarEsperas();
        mano.parar();
        super.onDetachedFromWindow();
    }

    // ---------- toques ----------

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (!isEnabled()) return true; // saliendo: no se deja tocar nada
        int accion = ev.getActionMasked();
        if (accion == MotionEvent.ACTION_DOWN) {
            destino = null;
            boolean enFoco = hayFoco && paso != null && !paso.esInfo() && !celebrando
                    && foco.contains(ev.getX(), ev.getY()) && !dentro(bocadillo, ev);
            if (enFoco) {
                destino = pantallaDebajo();
                xAbajo = ev.getX();
                yAbajo = ev.getY();
                tAbajo = ev.getEventTime();
                movido = false;
                // está intentándolo: se quita la pista y se le da otro rato
                mano.parar();
                removeCallbacks(pista);
                postDelayed(pista, ESPERA_PISTA);
            }
        }
        if (destino != null) {
            reenviar(ev, destino);
            detectarToque(ev);
            if (accion == MotionEvent.ACTION_UP || accion == MotionEvent.ACTION_CANCEL) destino = null;
            return true;
        }
        super.dispatchTouchEvent(ev); // botones del bocadillo
        return true; // fuera del foco no llega nada a la pantalla
    }

    /** La vista visible justo debajo de la capa (puede haber otras capas ocultas, p. ej. el QR a pantalla completa). */
    private View pantallaDebajo() {
        ViewGroup padre = (ViewGroup) getParent();
        int i = padre == null ? -1 : padre.indexOfChild(this);
        for (int j = i - 1; j >= 0; j--) {
            if (padre.getChildAt(j).getVisibility() == VISIBLE) return padre.getChildAt(j);
        }
        return null;
    }

    private void reenviar(MotionEvent ev, View v) {
        MotionEvent copia = MotionEvent.obtain(ev);
        copia.offsetLocation(getLeft() - v.getLeft(), getTop() - v.getTop());
        v.dispatchTouchEvent(copia);
        copia.recycle();
    }

    private void detectarToque(MotionEvent ev) {
        int accion = ev.getActionMasked();
        float slop = ViewConfiguration.get(getContext()).getScaledTouchSlop();
        if (accion == MotionEvent.ACTION_MOVE || accion == MotionEvent.ACTION_POINTER_DOWN) {
            if (ev.getPointerCount() > 1 || Math.hypot(ev.getX() - xAbajo, ev.getY() - yAbajo) > slop) movido = true;
        } else if (accion == MotionEvent.ACTION_UP && paso != null && paso.gesto == Gesto.TOCAR && !movido
                && ev.getEventTime() - tAbajo < ViewConfiguration.getLongPressTimeout()) {
            post(this::acierto); // después del clic de la pantalla
        }
    }

    private static boolean dentro(View v, MotionEvent ev) {
        return ev.getX() >= v.getX() && ev.getX() <= v.getX() + v.getWidth()
                && ev.getY() >= v.getY() && ev.getY() <= v.getY() + v.getHeight();
    }

    // ---------- aspecto ----------

    private void pintarPuntos(int indice, int total) {
        puntos.removeAllViews();
        for (int i = 0; i < total; i++) {
            View p = new View(getContext());
            p.setBackground(punto(i <= indice));
            int lado = (int) ((i == indice ? 10 : 8) * dp);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(lado, lado);
            lp.setMarginEnd((int) (5 * dp));
            puntos.addView(p, lp);
        }
    }

    private Drawable punto(boolean hecho) {
        GradientDrawable d = new GradientDrawable();
        d.setShape(GradientDrawable.OVAL);
        d.setColor(hecho ? color(R.color.nui_acento) : color(R.color.nui_linea));
        d.setStroke((int) (1.5f * dp), color(R.color.nui_tinta));
        return d;
    }

    /** Neobrutalista: borde grueso de tinta y sombra dura desplazada. */
    private Drawable fondoBocadillo(int sombra) {
        float r = 14 * dp;
        GradientDrawable fondoSombra = new GradientDrawable();
        fondoSombra.setCornerRadius(r);
        fondoSombra.setColor(color(R.color.nui_tinta));
        GradientDrawable papel = new GradientDrawable();
        papel.setCornerRadius(r);
        papel.setColor(color(R.color.nui_superficie));
        papel.setStroke((int) (3 * dp), color(R.color.nui_tinta));
        LayerDrawable capas = new LayerDrawable(new Drawable[]{fondoSombra, papel});
        capas.setLayerInset(0, sombra, sombra, 0, 0);
        capas.setLayerInset(1, 0, 0, sombra, sombra);
        return capas;
    }

    private void estiloBoton(TextView b, boolean resaltado) {
        GradientDrawable d = new GradientDrawable();
        d.setCornerRadius(10 * dp);
        d.setColor(resaltado ? color(R.color.nui_acento) : color(R.color.nui_superficie));
        d.setStroke((int) ((resaltado ? 2.5f : 1.5f) * dp), color(resaltado ? R.color.nui_tinta : R.color.nui_apagado));
        b.setBackground(d);
        b.setTextColor(color(resaltado ? R.color.nui_tinta : R.color.nui_apagado));
    }

    private TextView texto(int sp, int colorRes, boolean negrita) {
        TextView t = new TextView(getContext());
        t.setTextSize(sp);
        t.setTextColor(color(colorRes));
        Typeface fuente = ResourcesCompat.getFont(getContext(), R.font.nui_texto);
        t.setTypeface(Typeface.create(fuente, negrita ? Typeface.BOLD : Typeface.NORMAL));
        return t;
    }

    private int color(int res) {
        return ContextCompat.getColor(getContext(), res);
    }

    private void vibrar() {
        Vibrator v = getContext().getSystemService(Vibrator.class);
        if (v != null && v.hasVibrator()) v.vibrate(VibrationEffect.createOneShot(40, VibrationEffect.DEFAULT_AMPLITUDE));
    }
}
