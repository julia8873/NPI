package com.example.nuietsiit.activities.secciones.mapa;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;

import com.example.nuietsiit.R;
import com.google.android.material.button.MaterialButton;

import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Calendar;
import java.util.Locale;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

/**
 * Guía paso a paso de una ruta exterior (botón "Empezar" del panel): sigue el GPS con la app abierta,
 * alimenta {@link SeguimientoRuta} y pinta en el panel la etapa, la foto del sitio, la cuenta atrás y una
 * flecha (brújula) hacia el siguiente objetivo. Los avisos vibran con un patrón distinto cada uno.
 * "Simular ×10" recorre el plan sin moverse (para la demo; también vale {@code adb emu geo fix}).
 */
public final class GuiaRutaExterior implements Brujula.Listener, LocationListener {

    private static final int VELOCIDAD_SIMULACION = 10;
    private static final long TICK_MS = 1000, TICK_SIMULACION_MS = 500;
    /** Si el GPS lleva este tiempo sin dar posición, se da al usuario por parado. */
    private static final long GPS_PARADO_MS = 10_000;
    private static final long AVISO_MS = 6000;

    private final AppCompatActivity act;
    private final MapView mapa;
    private final PanelRutaExterior panel;
    private final Supplier<Calendar> reloj;
    private final Brujula brujula;
    private final LocationManager gps;
    private final Vibrator vibrador;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final OnBackPressedCallback atras;

    private final TextView tvAviso, tvEtapa, tvInstruccion, tvMetros, tvCuenta, tvLlegada;
    private final ImageView ivFoto, ivFlecha;
    private final MaterialButton btnSimular, btnTerminar;
    private final Marker marcaSimulada;

    private SeguimientoRuta seguimiento;
    private TrayectoSimulado trayecto;
    private BiConsumer<String, LocalDateTime> alLlegar;
    private boolean llegado, simulando, enMarcha;
    private LocalDateTime horaSimulada;
    private long ultimoTick;
    private Punto posicion;
    private double velocidad;
    private long horaPosicion;
    private float rumbo = Float.NaN, azimut;
    private String fotoActual;

    private final Runnable tick = new Runnable() {
        @Override public void run() {
            actualizar();
            if (enMarcha && !llegado) handler.postDelayed(this, simulando ? TICK_SIMULACION_MS : TICK_MS);
        }
    };

    private final Runnable quitarAviso = new Runnable() {
        @Override public void run() { tvAviso.setVisibility(View.GONE); }
    };

    /**
     * @param raiz  vista con el include de panel_ruta_exterior.xml
     * @param reloj hora actual (la real o la simulada del mapa)
     */
    public GuiaRutaExterior(AppCompatActivity act, View raiz, MapView mapa, PanelRutaExterior panel,
                            Supplier<Calendar> reloj) {
        this.act = act;
        this.mapa = mapa;
        this.panel = panel;
        this.reloj = reloj;
        brujula = new Brujula(act, this);
        gps = (LocationManager) act.getSystemService(Context.LOCATION_SERVICE);
        vibrador = (Vibrator) act.getSystemService(Context.VIBRATOR_SERVICE);

        tvAviso = raiz.findViewById(R.id.tvGuiaAviso);
        tvEtapa = raiz.findViewById(R.id.tvGuiaEtapa);
        tvInstruccion = raiz.findViewById(R.id.tvGuiaInstruccion);
        tvMetros = raiz.findViewById(R.id.tvGuiaMetros);
        tvCuenta = raiz.findViewById(R.id.tvGuiaCuenta);
        tvLlegada = raiz.findViewById(R.id.tvGuiaLlegada);
        ivFoto = raiz.findViewById(R.id.ivGuiaFoto);
        ivFlecha = raiz.findViewById(R.id.ivGuiaFlecha);
        btnSimular = raiz.findViewById(R.id.btnGuiaSimular);
        btnTerminar = raiz.findViewById(R.id.btnGuiaTerminar);

        btnSimular.setOnClickListener(v -> simular(!simulando));
        btnTerminar.setOnClickListener(v -> terminar());
        ivFoto.setOnClickListener(v -> verFoto());

        GradientDrawable fondoAviso = new GradientDrawable();
        fondoAviso.setCornerRadius(12 * act.getResources().getDisplayMetrics().density);
        fondoAviso.setColor(ContextCompat.getColor(act, R.color.nui_acento));
        tvAviso.setBackground(fondoAviso);

        marcaSimulada = new Marker(mapa);
        marcaSimulada.setIcon(punto(Color.rgb(25, 118, 210)));
        marcaSimulada.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER);
        marcaSimulada.setTitle("Tú (simulación)");

        // Atrás durante la guía: preguntar antes de terminar (va antes que el del panel)
        atras = new OnBackPressedCallback(false) {
            @Override public void handleOnBackPressed() { confirmarTerminar(); }
        };
        act.getOnBackPressedDispatcher().addCallback(act, atras);

        act.getLifecycle().addObserver(new DefaultLifecycleObserver() {
            @Override public void onResume(@NonNull LifecycleOwner o) { if (activa() && !llegado) reanudar(); }

            @Override public void onPause(@NonNull LifecycleOwner o) { if (activa()) pausar(); }
        });
    }

    // ---------- API ----------

    /** Empieza a guiar con el plan del panel (callback de "Empezar"). */
    public void empezar(PlanRuta plan) {
        if (activa()) terminar();
        seguimiento = new SeguimientoRuta(plan);
        trayecto = new TrayectoSimulado(plan);
        llegado = false;
        simulando = false;
        posicion = null;
        fotoActual = null;
        tvAviso.setVisibility(View.GONE);
        btnSimular.setText("▶ Simular ×10");
        btnSimular.setVisibility(View.VISIBLE);
        btnTerminar.setText("Terminar");

        panel.modoGuia(this::terminar);
        atras.setEnabled(true);
        act.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        mapa.getController().setZoom(17.5);
        mapa.getController().animateTo(new GeoPoint(plan.origen.lat, plan.origen.lon));
        reanudar();
    }

    public boolean activa() {
        return seguimiento != null;
    }

    /** Se llama una vez al llegar, con el id del lugar de destino y la hora (simulada o real). */
    public void alLlegar(BiConsumer<String, LocalDateTime> callback) {
        alLlegar = callback;
    }

    public void terminar() {
        if (!activa()) return;
        pausar();
        seguimiento = null;
        simulando = false;
        mapa.getOverlays().remove(marcaSimulada);
        mapa.invalidate();
        handler.removeCallbacks(quitarAviso);
        atras.setEnabled(false);
        act.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        panel.modoGuia(null);
    }

    // ---------- Sensores ----------

    private void reanudar() {
        enMarcha = true;
        brujula.iniciar();
        ivFlecha.setAlpha(brujula.disponible() ? 1f : 0.6f);
        if (!simulando) escucharGps(true);
        ultimoTick = SystemClock.elapsedRealtime();
        handler.removeCallbacks(tick);
        handler.post(tick);
    }

    private void pausar() {
        enMarcha = false;
        handler.removeCallbacks(tick);
        brujula.parar();
        escucharGps(false);
    }

    private void escucharGps(boolean escuchar) {
        if (gps == null) return;
        gps.removeUpdates(this);
        if (!escuchar || ContextCompat.checkSelfPermission(act, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) return;
        try {
            gps.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 0, this, Looper.getMainLooper());
            Location ultima = gps.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if (ultima != null && posicion == null) onLocationChanged(ultima);
        } catch (IllegalArgumentException | SecurityException e) {
            // sin proveedor GPS: queda la simulación
        }
    }

    @Override
    public void onLocationChanged(@NonNull Location l) {
        Punto p = new Punto(l.getLatitude(), l.getLongitude());
        long ahora = SystemClock.elapsedRealtime();
        if (l.hasSpeed()) {
            velocidad = l.getSpeed();
        } else if (posicion != null && ahora > horaPosicion) {
            velocidad = posicion.distanciaA(p) / ((ahora - horaPosicion) / 1000.0);
        } else {
            velocidad = 0;
        }
        posicion = p;
        horaPosicion = ahora;
    }

    // En Android 10 estos métodos aún no tienen implementación por defecto
    @Override public void onProviderEnabled(@NonNull String proveedor) {}

    @Override public void onProviderDisabled(@NonNull String proveedor) {}

    @Override public void onStatusChanged(String proveedor, int estado, android.os.Bundle extras) {}

    @Override
    public void onRumbo(float grados) {
        azimut = grados;
        girarFlecha();
    }

    // ---------- Simulación ----------

    /** Recorre el plan ×10 desde un poco antes de salir (empieza de cero la guía). */
    private void simular(boolean si) {
        simulando = si;
        seguimiento = new SeguimientoRuta(seguimiento.plan());
        llegado = false;
        posicion = null;
        if (si) {
            horaSimulada = trayecto.inicio();
            escucharGps(false);
            if (!mapa.getOverlays().contains(marcaSimulada)) mapa.getOverlays().add(marcaSimulada);
            btnSimular.setText("■ Usar el GPS");
        } else {
            mapa.getOverlays().remove(marcaSimulada);
            escucharGps(true);
            btnSimular.setText("▶ Simular ×10");
        }
        ultimoTick = SystemClock.elapsedRealtime();
        handler.removeCallbacks(tick);
        handler.post(tick);
    }

    // ---------- Seguimiento ----------

    private void actualizar() {
        if (!activa()) return;
        long ahoraMs = SystemClock.elapsedRealtime();
        LocalDateTime ahora;
        Punto p;
        double v;
        if (simulando) {
            horaSimulada = horaSimulada.plus(Duration.ofMillis((ahoraMs - ultimoTick) * VELOCIDAD_SIMULACION));
            TrayectoSimulado.Muestra m = trayecto.en(horaSimulada);
            ahora = horaSimulada;
            p = m.posicion;
            v = m.velocidad;
            marcaSimulada.setPosition(new GeoPoint(p.lat, p.lon));
        } else {
            ahora = LocalDateTime.ofInstant(reloj.get().toInstant(), ZoneId.systemDefault());
            p = posicion;
            v = ahoraMs - horaPosicion > GPS_PARADO_MS ? 0 : velocidad;
        }
        ultimoTick = ahoraMs;

        if (p == null) {
            tvEtapa.setText("Buscando tu ubicación…");
            tvInstruccion.setText("Sal a la calle o pulsa «Simular» para ver la ruta");
            tvCuenta.setText("");
            tvLlegada.setText("");
            tvMetros.setText("");
            return;
        }

        SeguimientoRuta.Estado e = seguimiento.actualizar(p, v, ahora);
        pintar(e, ahora);
        mapa.getController().animateTo(new GeoPoint(p.lat, p.lon));
        for (SeguimientoRuta.Evento ev : e.eventos) avisar(ev);
        if (e.etapa == SeguimientoRuta.Etapa.LLEGADO && !llegado) {
            llegado = true;
            pausar();
            btnSimular.setVisibility(View.GONE);
            btnTerminar.setText("Cerrar");
            if (alLlegar != null) alLlegar.accept(seguimiento.plan().destino.id, ahora);
        }
    }

    private void pintar(SeguimientoRuta.Estado e, LocalDateTime ahora) {
        String etapa = e.etapa.texto;
        if (e.etapa == SeguimientoRuta.Etapa.EN_BUS && e.paradasRestantes > 0) {
            etapa += " · " + e.paradasRestantes + (e.paradasRestantes == 1 ? " parada" : " paradas");
        }
        tvEtapa.setText(simulando ? etapa + " · simulación " + PlanRuta.hora(ahora) : etapa);
        tvInstruccion.setText(e.instruccion);

        if (e.etapa == SeguimientoRuta.Etapa.LLEGADO) {
            tvCuenta.setText("🏁 Has llegado");
            tvLlegada.setText(seguimiento.plan().destino.nombre);
        } else {
            tvCuenta.setText(e.cuentaAtras);
            tvLlegada.setText("Llegas a las " + PlanRuta.horaArriba(e.llegada) + " · quedan "
                    + e.minutosRestantes(ahora) + " min");
        }

        tvMetros.setText(Double.isNaN(e.rumbo) ? "" : e.metrosAlObjetivo < 1000
                ? Math.round(e.metrosAlObjetivo) + " m"
                : String.format(Locale.getDefault(), "%.1f km", e.metrosAlObjetivo / 1000));
        rumbo = (float) e.rumbo;
        girarFlecha();

        String foto = e.tramo != null ? e.tramo.idFoto : seguimiento.plan().destino.id;
        if (foto != null && !foto.equals(fotoActual)) {
            fotoActual = foto;
            ivFoto.setImageDrawable(FotoLugar.para(act, foto, 1));
        }
    }

    /** Flecha = rumbo al objetivo − hacia dónde mira el móvil (sin brújula: respecto al norte). */
    private void girarFlecha() {
        if (Float.isNaN(rumbo)) {
            ivFlecha.setVisibility(View.INVISIBLE);
            return;
        }
        ivFlecha.setVisibility(View.VISIBLE);
        ivFlecha.setRotation(rumbo - (brujula.disponible() ? azimut : 0));
    }

    private void avisar(SeguimientoRuta.Evento ev) {
        tvAviso.setText(ev.texto);
        tvAviso.setVisibility(View.VISIBLE);
        handler.removeCallbacks(quitarAviso);
        handler.postDelayed(quitarAviso, AVISO_MS);
        tvAviso.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
        if (vibrador != null && vibrador.hasVibrator()) {
            vibrador.vibrate(VibrationEffect.createWaveform(patron(ev.tipo), -1));
        }
    }

    /** Un patrón por aviso para reconocerlo con el móvil en el bolsillo. */
    private static long[] patron(SeguimientoRuta.Evento.Tipo tipo) {
        switch (tipo) {
            case SAL_YA: return new long[]{0, 400, 150, 400};
            case BUS_EN_1_MIN: return new long[]{0, 120, 100, 120, 100, 120};
            case BUS_PERDIDO: return new long[]{0, 900};
            case SIGUIENTE_ES_TU_PARADA: return new long[]{0, 250, 150, 250};
            case BAJATE_AHORA: return new long[]{0, 600, 200, 600, 200, 600};
            default: return new long[]{0, 100, 80, 100, 80, 500}; // LLEGADO
        }
    }

    // ---------- Utilidades ----------

    private void confirmarTerminar() {
        if (llegado) {
            terminar();
            return;
        }
        new AlertDialog.Builder(act)
                .setTitle("¿Terminar la guía?")
                .setMessage("Dejaré de avisarte del bus y de la llegada.")
                .setPositiveButton("Terminar", (d, w) -> terminar())
                .setNegativeButton("Seguir", null)
                .show();
    }

    private void verFoto() {
        if (fotoActual == null) return;
        ImageView foto = new ImageView(act);
        foto.setAdjustViewBounds(true);
        foto.setImageDrawable(FotoLugar.para(act, fotoActual, 1));
        new AlertDialog.Builder(act)
                .setTitle(tvInstruccion.getText())
                .setView(foto)
                .setPositiveButton("Vale", null)
                .show();
    }

    private GradientDrawable punto(int color) {
        float dp = act.getResources().getDisplayMetrics().density;
        GradientDrawable circulo = new GradientDrawable();
        circulo.setShape(GradientDrawable.OVAL);
        circulo.setColor(color);
        circulo.setStroke((int) (3 * dp), Color.WHITE);
        circulo.setSize((int) (20 * dp), (int) (20 * dp));
        return circulo;
    }
}
