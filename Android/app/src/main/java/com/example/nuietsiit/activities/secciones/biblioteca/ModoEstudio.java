package com.example.nuietsiit.activities.secciones.biblioteca;

import android.animation.ValueAnimator;
import android.app.NotificationManager;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.provider.Settings;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;

import com.example.nuietsiit.ui.CabeceraNui;
import com.example.nuietsiit.R;
import com.example.nuietsiit.util.tutorial.Gesto;
import com.example.nuietsiit.util.tutorial.Tutorial;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;

import java.util.Locale;
import java.util.function.BooleanSupplier;

/**
 * Modo estudio: dejar el móvil boca arriba y quieto sobre la mesa llena un anillo (3 s) y empieza un
 * Pomodoro de 25 minutos con «No molestar» (sin permiso: modo vibración) y la pantalla negra y atenuada.
 * Levantarlo = pausa ("Llevas 18 min, ¿lo dejas?"); dejarlo otra vez = sigue. Al acabar, vibración suave
 * y 5 minutos de descanso. El modo de sonido de antes se restaura siempre al salir del estudio.
 */
public class ModoEstudio extends PantallaBiblioteca {

    /** Extra: se viene de dejar el móvil en la mesa con la balda abierta; el estudio empieza directamente. */
    public static final String EXTRA_EMPEZAR = "empezarYa";

    private static final long ESTUDIO_MS = 25 * 60_000L;
    private static final long DESCANSO_MS = 5 * 60_000L;
    private static final long LLENAR_MS = 3000;          // quieto en la mesa para empezar
    private static final long LLENAR_SEGUIR_MS = 1500;   // para seguir tras una pausa
    private static final long SIN_MESA_MS = 6000;        // sin dejarlo, aparece "Empezar sin dejarlo"
    private static final float BRILLO_ESTUDIO = 0.02f;

    private enum Estado { ESPERA, ESTUDIO, PAUSA, DESCANSO }

    private AnilloEstudio anillo;
    private ImageView ivMovil;
    private TextView tvTiempo, tvTitulo, tvDetalle, tvRondas, tvTiempoEstudio, tvNoMolestarEstudio, btnNoMolestar;
    private MaterialButton btnPrincipal, btnSecundario;
    private View capaEstudio;
    private ValueAnimator animMovil;

    private DetectorMesa mesa;
    private final Handler reloj = new Handler(Looper.getMainLooper());
    private Estado estado = Estado.ESPERA;
    private long estudiadoMs;          // de la ronda actual, sin contar el tramo en marcha
    private long inicioTramo;          // cuándo empezó el tramo de estudio en marcha
    private long inicioDescanso;
    private long inicioLlenado = -1;   // -1 = el anillo no se está llenando
    private long inicioEspera;
    private boolean necesitaLevantar;  // sigue en la mesa tras el descanso o una pausa tocando: no empieza solo
    private boolean manual;            // estudiando sin el móvil en la mesa: levantarlo no pausa
    private int rondas;
    private boolean empezarAlAbrir;

    // lo que había antes de silenciar, para dejarlo igual
    private boolean cambieFiltro, cambieTimbre;
    private int filtroPrevio, timbrePrevio;

    private final Runnable tic = this::tic;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.modo_estudio);

        MaterialToolbar barra = findViewById(R.id.toolbar);
        setSupportActionBar(barra);
        CabeceraNui.preparar(barra);
        getSupportActionBar().setTitle("Modo estudio");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        anillo = findViewById(R.id.anillo);
        ivMovil = findViewById(R.id.ivMovil);
        tvTiempo = findViewById(R.id.tvTiempo);
        tvTitulo = findViewById(R.id.tvTitulo);
        tvDetalle = findViewById(R.id.tvDetalle);
        tvRondas = findViewById(R.id.tvRondas);
        tvTiempoEstudio = findViewById(R.id.tvTiempoEstudio);
        tvNoMolestarEstudio = findViewById(R.id.tvNoMolestarEstudio);
        btnNoMolestar = findViewById(R.id.btnNoMolestar);
        btnPrincipal = findViewById(R.id.btnPrincipal);
        btnSecundario = findViewById(R.id.btnSecundario);
        capaEstudio = findViewById(R.id.capaEstudio);

        mesa = new DetectorMesa(this, this::alCambiarMesa);
        crearAnimacionMovil();

        btnNoMolestar.setOnClickListener(v -> {
            try {
                startActivity(new Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS));
            } catch (android.content.ActivityNotFoundException e) {
                btnNoMolestar.setVisibility(View.GONE);
            }
        });
        btnPrincipal.setOnClickListener(v -> {
            switch (estado) {
                case ESPERA:
                case PAUSA:
                    empezarEstudio(!mesa.enMesa()); // "Empezar sin dejarlo" / "Seguir"
                    break;
                case DESCANSO:
                    finDescanso(false);
                    break;
                default:
                    break;
            }
        });
        btnSecundario.setOnClickListener(v -> finish());
        capaEstudio.setOnClickListener(v -> pausar());

        // atrás mientras se estudia = pausa (como levantarlo); si no, se sale
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (estado == Estado.ESTUDIO) {
                    pausar();
                } else {
                    setEnabled(false);
                    getOnBackPressedDispatcher().onBackPressed();
                }
            }
        });

        if (savedInstanceState != null) {
            estado = Estado.valueOf(savedInstanceState.getString("estado", Estado.ESPERA.name()));
            estudiadoMs = savedInstanceState.getLong("estudiado");
            rondas = savedInstanceState.getInt("rondas");
            inicioDescanso = SystemClock.elapsedRealtime() - savedInstanceState.getLong("descansado");
            if (estado == Estado.ESTUDIO) estado = Estado.PAUSA; // al volver, hay que dejarlo otra vez
            necesitaLevantar = false;
        }
        inicioEspera = SystemClock.elapsedRealtime();
        empezarAlAbrir = savedInstanceState == null && getIntent().getBooleanExtra(EXTRA_EMPEZAR, false);
    }

    @Override
    protected Tutorial crearTutorial() {
        // si se entra ya estudiando (desde la balda) no sale ninguno: saldrá la próxima vez
        BooleanSupplier esperando = () -> estado == Estado.ESPERA || estado == Estado.PAUSA;
        return new Tutorial(this, "modo_estudio", 1)
                .sensor(Gesto.MESA, "Deja el móvil sobre la mesa, boca arriba y quieto: el anillo se llena"
                        + " y empiezan 25 minutos de estudio (ahora es de prueba, no empieza)")
                .soloSi(() -> esperando.getAsBoolean() && mesa.disponible())
                .sensor(Gesto.LEVANTAR, "Ahora levántalo: el anillo se vacía. Estudiando, levantarlo hace una pausa"
                        + " y dejarlo otra vez sigue")
                .soloSi(() -> esperando.getAsBoolean() && mesa.disponible())
                .info(R.id.anillo, "Mientras estudias la pantalla se apaga y el móvil se silencia. Tócala para pausar."
                        + " Al acabar vibra suave y descansas 5 minutos")
                .soloSi(esperando)
                .info(R.id.btnNoMolestar, "Dale permiso de «No molestar» para que no te llegue ni una notificación")
                .soloSiSeVe();
    }

    // estudiar con el móvil en la mano o por accidente no tiene sentido: agitar no saca al menú
    @Override
    protected boolean usaAgitar() {
        return false;
    }

    @Override
    protected void onResume() {
        super.onResume();
        mesa.iniciar();
        inicioEspera = SystemClock.elapsedRealtime();
        if (empezarAlAbrir) {
            // el anillo ya se llenó en la balda; el detector se da cuenta enseguida de que sigue en la mesa
            empezarAlAbrir = false;
            empezarEstudio(false);
        }
        mostrar();
        reloj.post(tic);
    }

    @Override
    protected void onPause() {
        super.onPause();
        // otra app delante o pantalla apagada: el estudio se pausa y el sonido vuelve como estaba
        pausar();
        mesa.parar();
        inicioLlenado = -1;
        reloj.removeCallbacks(tic);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        restaurarSonido();
        if (animMovil != null) animMovil.cancel();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle estado) {
        super.onSaveInstanceState(estado);
        estado.putString("estado", this.estado.name());
        estado.putLong("estudiado", estudiadoMs);
        estado.putInt("rondas", rondas);
        estado.putLong("descansado", SystemClock.elapsedRealtime() - inicioDescanso);
    }

    // ---------- sensor ----------

    private void alCambiarMesa(boolean enMesa) {
        avisarTutorial(enMesa ? Gesto.MESA : Gesto.LEVANTAR);
        switch (estado) {
            case ESPERA:
            case PAUSA:
                if (enMesa && !necesitaLevantar) {
                    inicioLlenado = SystemClock.elapsedRealtime();
                    anillo.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK); // el anillo empieza a llenarse
                } else if (!enMesa) {
                    necesitaLevantar = false;
                    inicioLlenado = -1;
                }
                mostrar();
                break;
            case ESTUDIO:
                if (enMesa) manual = false; // ya está en la mesa: a partir de ahora levantarlo pausa
                else if (!manual) pausar();
                break;
            default:
                break;
        }
    }

    // ---------- reloj ----------

    private void tic() {
        long ahora = SystemClock.elapsedRealtime();
        long siguiente = 250;
        switch (estado) {
            case ESPERA:
            case PAUSA:
                if (inicioLlenado >= 0) {
                    long llenar = estado == Estado.PAUSA ? LLENAR_SEGUIR_MS : LLENAR_MS;
                    float p = (ahora - inicioLlenado) / (float) llenar;
                    anillo.setProgreso(p);
                    // en el tutorial el anillo se llena, pero el estudio (silencio, pantalla negra) no empieza
                    if (p >= 1f && !hayTutorialActivo()) {
                        empezarEstudio(false);
                        break;
                    }
                } else {
                    anillo.setProgreso(anillo.getProgreso() - 0.06f); // se vacía al levantarlo
                }
                if (estado == Estado.ESPERA && btnPrincipal.getVisibility() != View.VISIBLE && inicioLlenado < 0
                        && ahora - inicioEspera > SIN_MESA_MS) {
                    mostrar(); // aparece "Empezar sin dejarlo"
                }
                siguiente = 30;
                break;
            case ESTUDIO:
                long queda = ESTUDIO_MS - estudiado(ahora);
                tvTiempoEstudio.setText(formato(queda));
                if (queda <= 0) terminarRonda();
                break;
            case DESCANSO:
                long resta = DESCANSO_MS - (ahora - inicioDescanso);
                tvTiempo.setText(formato(resta));
                anillo.setProgreso(1f - resta / (float) DESCANSO_MS);
                if (resta <= 0) finDescanso(true);
                break;
        }
        reloj.removeCallbacks(tic);
        if (!isFinishing()) reloj.postDelayed(tic, siguiente);
    }

    private long estudiado(long ahora) {
        return estudiadoMs + (estado == Estado.ESTUDIO ? ahora - inicioTramo : 0);
    }

    private static String formato(long ms) {
        long s = Math.max(0, (ms + 999) / 1000);
        return String.format(Locale.ROOT, "%d:%02d", s / 60, s % 60);
    }

    // ---------- cambios de estado ----------

    private void empezarEstudio(boolean sinMesa) {
        manual = sinMesa;
        estado = Estado.ESTUDIO;
        inicioTramo = SystemClock.elapsedRealtime();
        inicioLlenado = -1;
        necesitaLevantar = false;
        anillo.setProgreso(1f);
        tvNoMolestarEstudio.setText(silenciar());
        tvTiempoEstudio.setText(formato(ESTUDIO_MS - estudiadoMs));
        vibrar(new long[]{0, 30, 60, 50}, new int[]{0, 120, 0, 255}); // "clac": empieza
        capaEstudio.setAlpha(0f);
        capaEstudio.setVisibility(View.VISIBLE);
        capaEstudio.animate().alpha(1f).setDuration(600).withEndAction(() -> brillo(BRILLO_ESTUDIO)).start();
        capaEstudio.announceForAccessibility("Estudiando 25 minutos. Toca la pantalla para hacer una pausa.");
        mostrar();
    }

    private void pausar() {
        if (estado != Estado.ESTUDIO) return;
        estudiadoMs = estudiado(SystemClock.elapsedRealtime());
        estado = Estado.PAUSA;
        // si se pausa tocando, sigue en la mesa: hay que levantarlo y dejarlo (o pulsar Seguir)
        necesitaLevantar = mesa.enMesa();
        inicioLlenado = -1;
        anillo.setProgreso(0f);
        salirDeLaCapa();
        mostrar();
    }

    private void terminarRonda() {
        estudiadoMs = 0;
        rondas++;
        estado = Estado.DESCANSO;
        inicioDescanso = SystemClock.elapsedRealtime();
        anillo.setProgreso(0f);
        salirDeLaCapa();
        // vibración suave: dos toques flojos y uno algo más largo
        vibrar(new long[]{0, 120, 200, 120, 200, 260}, new int[]{0, 60, 0, 60, 0, 100});
        mostrar();
        tvTitulo.announceForAccessibility("Ronda completada. Descansa 5 minutos.");
    }

    private void finDescanso(boolean avisar) {
        estado = Estado.ESPERA;
        necesitaLevantar = mesa.enMesa();
        inicioEspera = SystemClock.elapsedRealtime();
        anillo.setProgreso(0f);
        if (avisar) vibrar(new long[]{0, 120, 200, 260}, new int[]{0, 60, 0, 100});
        mostrar();
    }

    private void salirDeLaCapa() {
        restaurarSonido();
        brillo(WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE);
        capaEstudio.animate().cancel();
        capaEstudio.animate().alpha(0f).setDuration(250)
                .withEndAction(() -> capaEstudio.setVisibility(View.GONE)).start();
    }

    // ---------- pantalla ----------

    private void mostrar() {
        boolean llenando = inicioLlenado >= 0;
        boolean conMovil = estado == Estado.ESPERA || estado == Estado.PAUSA;
        ivMovil.setVisibility(conMovil ? View.VISIBLE : View.GONE);
        tvTiempo.setVisibility(estado == Estado.DESCANSO ? View.VISIBLE : View.GONE);
        anillo.setColorArco(getColor(estado == Estado.DESCANSO ? R.color.nui_arrayan : R.color.nui_cobalto));
        animarMovil(conMovil && !llenando);

        String titulo, detalle;
        String principal = null, secundario = null;
        switch (estado) {
            case PAUSA:
                long min = estudiadoMs / 60_000;
                titulo = (min == 0 ? "Llevas menos de un minuto" : "Llevas " + min + (min == 1 ? " minuto" : " minutos"))
                        + ". ¿Lo dejas?";
                detalle = llenando ? "Quieto… sigues en un momento"
                        : necesitaLevantar ? "Levántalo y vuelve a dejarlo para seguir"
                        : "Vuelve a dejarlo sobre la mesa para seguir";
                principal = "Seguir";
                secundario = "Terminar";
                break;
            case DESCANSO:
                titulo = "¡Ronda completada! Descansa";
                detalle = "Levántate y estira las piernas. Vibrará cuando acabe el descanso.";
                principal = "Saltar el descanso";
                secundario = "Terminar";
                break;
            case ESTUDIO:
                titulo = "Estudiando";
                detalle = "";
                break;
            default:
                if (llenando) {
                    titulo = "Quieto… no lo muevas";
                    detalle = "Cuando se llene el anillo empiezan 25 minutos de estudio";
                } else if (necesitaLevantar) {
                    titulo = "Levántalo y vuelve a dejarlo";
                    detalle = "Así empieza otra ronda de 25 minutos";
                } else {
                    titulo = "Deja el móvil sobre la mesa";
                    detalle = "Boca arriba y quieto: empiezan 25 minutos de estudio y el móvil se silencia";
                }
                boolean sinMesa = !mesa.disponible() || necesitaLevantar
                        || SystemClock.elapsedRealtime() - inicioEspera > SIN_MESA_MS;
                if (!llenando && sinMesa) principal = "Empezar sin dejarlo";
                break;
        }
        tvTitulo.setText(titulo);
        tvDetalle.setText(detalle);
        boton(btnPrincipal, principal, View.INVISIBLE); // guarda su hueco: lo de arriba no salta al aparecer
        boton(btnSecundario, secundario, View.GONE);
        tvRondas.setVisibility(rondas > 0 ? View.VISIBLE : View.GONE);
        tvRondas.setText("Rondas completadas: " + rondas);

        NotificationManager nm = getSystemService(NotificationManager.class);
        boolean sinPermiso = nm != null && !nm.isNotificationPolicyAccessGranted();
        btnNoMolestar.setVisibility(sinPermiso && estado != Estado.ESTUDIO ? View.VISIBLE : View.GONE);
    }

    private static void boton(MaterialButton b, String texto, int oculto) {
        b.setVisibility(texto != null ? View.VISIBLE : oculto);
        if (texto != null) b.setText(texto);
    }

    // el móvil del centro se tumba una y otra vez: "déjalo así, sobre la mesa"
    private void crearAnimacionMovil() {
        float d = getResources().getDisplayMetrics().density;
        ivMovil.setCameraDistance(8000 * d);
        animMovil = ValueAnimator.ofFloat(0f, 1f);
        animMovil.setDuration(2400);
        animMovil.setRepeatCount(ValueAnimator.INFINITE);
        animMovil.addUpdateListener(a -> {
            float t = (float) a.getAnimatedValue();
            // 0–0,4 se tumba, 0,4–0,8 quieto tumbado, 0,8–1 vuelve a ponerse de pie
            float tumbado = t < 0.4f ? suave(t / 0.4f) : t < 0.8f ? 1f : 1f - suave((t - 0.8f) / 0.2f);
            ponerMovil(tumbado, d);
        });
    }

    private void ponerMovil(float tumbado, float d) {
        ivMovil.setRotationX(65f * tumbado);
        ivMovil.setTranslationY(14f * d * tumbado);
    }

    private static float suave(float x) {
        return x * x * (3 - 2 * x);
    }

    private void animarMovil(boolean si) {
        if (animMovil == null) return;
        if (si && !animMovil.isStarted() && ValueAnimator.areAnimatorsEnabled()) {
            animMovil.start();
        } else if (!si && animMovil.isStarted()) {
            animMovil.cancel();
        }
        // llenándose: tumbado y quieto, como está de verdad
        if (!si) ponerMovil(inicioLlenado >= 0 ? 1f : 0f, getResources().getDisplayMetrics().density);
    }

    private void brillo(float valor) {
        WindowManager.LayoutParams lp = getWindow().getAttributes();
        lp.screenBrightness = valor;
        getWindow().setAttributes(lp);
    }

    private void vibrar(long[] tiempos, int[] amplitudes) {
        Vibrator v = getSystemService(Vibrator.class);
        if (v == null || !v.hasVibrator()) return;
        v.vibrate(v.hasAmplitudeControl()
                ? VibrationEffect.createWaveform(tiempos, amplitudes, -1)
                : VibrationEffect.createWaveform(tiempos, -1));
    }

    // ---------- sonido ----------

    /** Pone «No molestar» (o, sin permiso, modo vibración) y dice en una frase lo que ha hecho. */
    private String silenciar() {
        NotificationManager nm = getSystemService(NotificationManager.class);
        if (nm != null && nm.isNotificationPolicyAccessGranted()) {
            int actual = nm.getCurrentInterruptionFilter();
            if (actual == NotificationManager.INTERRUPTION_FILTER_ALL) {
                filtroPrevio = actual;
                nm.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_PRIORITY);
                cambieFiltro = true;
            }
            return "🔕 No molestar activado";
        }
        AudioManager audio = getSystemService(AudioManager.class);
        if (audio == null) return "";
        if (audio.getRingerMode() == AudioManager.RINGER_MODE_NORMAL) {
            try {
                timbrePrevio = AudioManager.RINGER_MODE_NORMAL;
                audio.setRingerMode(AudioManager.RINGER_MODE_VIBRATE);
                cambieTimbre = true;
            } catch (SecurityException e) {
                return "";
            }
        }
        return audio.getRingerMode() == AudioManager.RINGER_MODE_SILENT ? "🔕 Móvil en silencio" : "📳 Móvil en vibración";
    }

    /** Vuelve a dejar el sonido como estaba, salvo que el usuario lo haya cambiado mientras tanto. */
    private void restaurarSonido() {
        if (cambieFiltro) {
            cambieFiltro = false;
            NotificationManager nm = getSystemService(NotificationManager.class);
            try {
                if (nm != null && nm.isNotificationPolicyAccessGranted()
                        && nm.getCurrentInterruptionFilter() == NotificationManager.INTERRUPTION_FILTER_PRIORITY) {
                    nm.setInterruptionFilter(filtroPrevio);
                }
            } catch (SecurityException ignored) {
            }
        }
        if (cambieTimbre) {
            cambieTimbre = false;
            AudioManager audio = getSystemService(AudioManager.class);
            try {
                if (audio != null && audio.getRingerMode() == AudioManager.RINGER_MODE_VIBRATE) {
                    audio.setRingerMode(timbrePrevio);
                }
            } catch (SecurityException ignored) {
            }
        }
    }
}
