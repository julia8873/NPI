package com.example.nuietsiit.activities.secciones.biblioteca;

import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;

import com.example.nuietsiit.R;
import com.example.nuietsiit.util.tutorial.Gesto;
import com.example.nuietsiit.util.tutorial.Tutorial;

import java.util.function.BooleanSupplier;

/**
 * "¿Vas a estudiar?": aviso con un anillo que se llena mientras el móvil está sobre la mesa y, lleno,
 * abre el modo estudio ya estudiando. Tocar el aviso empieza al momento.
 * <ul>
 *  <li>{@link #enLaBalda}: el aviso sale solo tras 2 s en la mesa; tocar la pantalla fuera de él lo descarta
 *     hasta levantar el móvil (se está usando la balda). La Activity reenvía sus toques a {@link #alTocar}.</li>
 *  <li>{@link #alLlegar}: al acabar la ruta a la biblioteca; el aviso se queda a la vista con "Ahora no".</li>
 * </ul>
 * Sigue el ciclo de vida de la Activity: solo escucha el sensor mientras está delante.
 */
public class SugerenciaEstudio implements DefaultLifecycleObserver {

    private static final long ANTES_DE_AVISAR_MS = 2000;
    private static final long LLENAR_MS = 3000;
    private static final long AL_VOLVER_MS = 800; // ya en la mesa al volver (p. ej. del modo estudio): no se propone

    private final AppCompatActivity act;
    private final boolean enBalda;
    private final BooleanSupplier ocupado;
    private final View aviso;
    private final AnilloEstudio anillo;
    private final DetectorMesa mesa;
    private final Handler reloj = new Handler(Looper.getMainLooper());
    private final Runnable tic = this::tic;

    private long inicioMesa = -1;   // -1 = no está en la mesa
    private boolean descartado;     // en la balda: se tocó la pantalla; no vuelve hasta levantarlo
    private boolean terminado;      // al llegar: ya se abrió el modo estudio o se dijo "Ahora no"
    private long reanudada;
    private long mostradoEn;        // los toques que ya venían de camino (p. ej. a "Siguiente") no cuentan
    private static final long TOQUE_TRAS_MOSTRAR_MS = 700;

    /** En la balda: el aviso aparece al dejar el móvil en la mesa. ocupado = se está usando (p. ej. ojeando). */
    public static SugerenciaEstudio enLaBalda(AppCompatActivity act, ViewGroup contenedor, BooleanSupplier ocupado) {
        return new SugerenciaEstudio(act, contenedor, true, ocupado);
    }

    /** Al llegar a la biblioteca: el aviso aparece ya y espera a que se deje el móvil en la mesa. */
    public static SugerenciaEstudio alLlegar(AppCompatActivity act) {
        SugerenciaEstudio s = new SugerenciaEstudio(act, act.findViewById(android.R.id.content), false, () -> false);
        s.mostrar();
        return s;
    }

    private SugerenciaEstudio(AppCompatActivity act, ViewGroup contenedor, boolean enBalda, BooleanSupplier ocupado) {
        this.act = act;
        this.enBalda = enBalda;
        this.ocupado = ocupado;
        aviso = LayoutInflater.from(act).inflate(R.layout.aviso_estudio, contenedor, false);
        contenedor.addView(aviso);
        anillo = aviso.findViewById(R.id.anilloAviso);
        anillo.setGrosor(5f * act.getResources().getDisplayMetrics().density);
        aviso.setOnClickListener(v -> {
            if (SystemClock.elapsedRealtime() - mostradoEn > TOQUE_TRAS_MOSTRAR_MS) abrir();
        });

        TextView titulo = aviso.findViewById(R.id.tvTituloAviso);
        TextView detalle = aviso.findViewById(R.id.tvDetalleAviso);
        View ahoraNo = aviso.findViewById(R.id.btnAhoraNo);
        if (enBalda) {
            titulo.setText(R.string.sug_estudio_balda_titulo);
            detalle.setText(R.string.sug_estudio_balda_detalle);
            ahoraNo.setVisibility(View.GONE);
        } else {
            // arriba: abajo están los botones de la ruta
            FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) aviso.getLayoutParams();
            lp.gravity = Gravity.TOP;
            lp.topMargin = act.getResources().getDimensionPixelSize(R.dimen.biblio_aviso_arriba);
            aviso.setLayoutParams(lp);
            titulo.setText(R.string.sug_estudio_llegar_titulo);
            detalle.setText(R.string.sug_estudio_llegar_detalle);
            ahoraNo.setOnClickListener(v -> {
                terminado = true;
                ocultar();
            });
        }

        mesa = new DetectorMesa(act, this::alCambiarMesa);
        act.getLifecycle().addObserver(this);
    }

    @Override
    public void onResume(@NonNull LifecycleOwner owner) {
        reanudada = SystemClock.elapsedRealtime();
        mesa.iniciar();
    }

    @Override
    public void onPause(@NonNull LifecycleOwner owner) {
        mesa.parar();
        descartado = false;
        pararAnillo();
        if (enBalda) ocultar();
    }

    /** Llamar desde dispatchTouchEvent de la Activity (solo hace algo en la balda). */
    public void alTocar(MotionEvent e) {
        if (!enBalda || e.getActionMasked() != MotionEvent.ACTION_DOWN || !mesa.enMesa() || dentro(e)) return;
        descartado = true;
        pararAnillo();
        ocultar();
    }

    private void alCambiarMesa(boolean enMesa) {
        if (enMesa) Tutorial.avisar(act, Gesto.MESA);
        if (terminado) return;
        if (enMesa && SystemClock.elapsedRealtime() - reanudada < AL_VOLVER_MS) descartado = true;
        if (enMesa && !descartado) {
            inicioMesa = SystemClock.elapsedRealtime();
            reloj.removeCallbacks(tic);
            reloj.post(tic);
        } else if (!enMesa) {
            descartado = false; // levantado: la próxima vez que se deje, vuelve a proponerse
            pararAnillo();
            if (enBalda) ocultar();
        }
    }

    // en la balda: 2 s en la mesa sin tocar nada y sale el aviso; luego el anillo se llena en 3 s
    private void tic() {
        if (inicioMesa < 0) return;
        if (ocupado.getAsBoolean() || !act.hasWindowFocus()) {
            // ojeando un libro o con otra ventana delante (Explorar, un diálogo): se vuelve a esperar
            if (enBalda) ocultar();
            anillo.setProgreso(0f);
            inicioMesa = SystemClock.elapsedRealtime();
        } else {
            long espera = enBalda ? ANTES_DE_AVISAR_MS : 0;
            long enMesa = SystemClock.elapsedRealtime() - inicioMesa;
            if (enMesa >= espera) {
                if (aviso.getVisibility() != View.VISIBLE) mostrar();
                float p = (enMesa - espera) / (float) LLENAR_MS;
                anillo.setProgreso(p);
                if (p >= 1f) {
                    abrir();
                    return;
                }
            }
        }
        reloj.postDelayed(tic, 30);
    }

    private void abrir() {
        if (!enBalda) terminado = true; // al volver del modo estudio, la ruta ya no lo propone
        pararAnillo();
        ocultar();
        act.startActivity(new Intent(act, ModoEstudio.class)
                .putExtra("CORREO", act.getIntent().getStringExtra("CORREO"))
                .putExtra(ModoEstudio.EXTRA_EMPEZAR, true));
    }

    private void mostrar() {
        mostradoEn = SystemClock.elapsedRealtime();
        aviso.setVisibility(View.VISIBLE);
        aviso.setAlpha(0f);
        float desde = aviso.getHeight() > 0 ? aviso.getHeight() : 200f;
        aviso.setTranslationY(enBalda ? desde : -desde); // entra desde su borde
        aviso.animate().alpha(1f).translationY(0f).setDuration(250)
                .setInterpolator(new DecelerateInterpolator()).start();
        aviso.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);
    }

    private void pararAnillo() {
        inicioMesa = -1;
        reloj.removeCallbacks(tic);
        anillo.setProgreso(0f);
    }

    private void ocultar() {
        if (aviso.getVisibility() != View.VISIBLE) return;
        aviso.animate().alpha(0f).translationY((enBalda ? 1 : -1) * aviso.getHeight() / 2f).setDuration(180)
                .withEndAction(() -> aviso.setVisibility(View.GONE)).start();
    }

    private boolean dentro(MotionEvent e) {
        if (aviso.getVisibility() != View.VISIBLE) return false;
        int[] pos = new int[2];
        aviso.getLocationOnScreen(pos);
        return e.getRawX() >= pos[0] && e.getRawX() <= pos[0] + aviso.getWidth()
                && e.getRawY() >= pos[1] && e.getRawY() <= pos[1] + aviso.getHeight();
    }
}