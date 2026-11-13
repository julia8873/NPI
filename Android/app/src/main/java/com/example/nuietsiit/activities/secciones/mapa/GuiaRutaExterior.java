package com.example.nuietsiit.activities.secciones.mapa;

import android.Manifest;
import android.annotation.SuppressLint;
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
import android.transition.TransitionManager;
import android.view.GestureDetector;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

import com.example.nuietsiit.R;
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
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

/**
 * Guía paso a paso de una ruta exterior (botón "Empezar" del panel): sigue el GPS con la app abierta,
 * alimenta {@link SeguimientoRuta} y pinta en el panel la etapa, la foto grande del camino, la cuenta atrás y
 * una flecha (brújula) hacia el siguiente objetivo. Los avisos vibran con un patrón distinto cada uno.
 * Fotos (G2): en los tramos a pie avanzan solas con {@link FotosRutaExterior#siguiente}; deslizar en la foto
 * = otra foto (izquierda/derecha) o plegarla/desplegarla (abajo/arriba); tocarla = {@link VisorFotoExterior}.
 * "Simular ×10" recorre el plan sin moverse (para la demo; también vale {@code adb emu geo fix}).
 */
public final class GuiaRutaExterior implements Brujula.Listener, LocationListener {

    private static final int VELOCIDAD_SIMULACION = 10;
    private static final long TICK_MS = 1000, TICK_SIMULACION_MS = 500;
    /** Si el GPS lleva este tiempo sin dar posición, se da al usuario por parado. */
    private static final long GPS_PARADO_MS = 10_000;
    private static final long AVISO_MS = 6000;
    /** Alto de la foto plegada (tira); desplegada es 4:3 hasta el 45 % de la pantalla. */
    private static final int ALTO_PLEGADA_DP = 104;

    private final AppCompatActivity act;
    private final MapView mapa;
    private final PanelRutaExterior panel;
    private final Supplier<Calendar> reloj;
    private final Brujula brujula;
    private final LocationManager gps;
    private final Vibrator vibrador;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final OnBackPressedCallback atras;

    private final TextView tvAviso, tvEtapa, tvInstruccion, tvMetros, tvCuenta, tvLlegada, tvContador, tvPaso,
            tvPuntualidad;
    private final ImageView ivFoto, ivFlecha;
    private final View cardFoto;
    private final LinearLayout llPuntos;
    private final int altoFoto, altoPlegada;
    private final MaterialButton btnSimular, btnTerminar, btnVoz;
    private final Marker marcaSimulada;
    private final VozGuia voz;
    private final VistaLevantada levantada;
    private PlanificadorRuta planificador;

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
    // Fotos del tramo actual: claves de FotoLugar; puntosTramo solo si es un camino a pie con puntos
    private PlanRuta.Tramo tramoFotos;
    private List<String> fotos = Collections.emptyList();
    private List<FotosRutaExterior.PuntoFoto> puntosTramo = Collections.emptyList();
    private int indiceFoto, indiceAuto = -1;
    private String instruccionRuta = "";
    // Empieza plegada: con el móvil en la mano se ve el mapa; la foto en grande sale al levantarlo
    private boolean plegada = true;
    // Viaje en curso (de "¿A dónde vas?"): con hora límite se dice si llegas a tiempo
    private DestinoViaje viaje;
    private Puntualidad puntualidad;
    private PlanRuta mejorPlan;
    private long ultimaAlternativa;
    private static final long ALTERNATIVA_MS = 60_000;

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
        cardFoto = raiz.findViewById(R.id.cardGuiaFoto);
        tvContador = raiz.findViewById(R.id.tvGuiaContador);
        tvPaso = raiz.findViewById(R.id.tvGuiaPaso);
        tvPuntualidad = raiz.findViewById(R.id.tvGuiaPuntualidad);
        btnVoz = raiz.findViewById(R.id.btnGuiaVoz);
        llPuntos = raiz.findViewById(R.id.llGuiaPuntos);
        btnSimular = raiz.findViewById(R.id.btnGuiaSimular);
        btnTerminar = raiz.findViewById(R.id.btnGuiaTerminar);

        btnSimular.setOnClickListener(v -> simular(!simulando));
        btnTerminar.setOnClickListener(v -> terminar());
        gestosFoto();

        voz = new VozGuia(act);
        pintarVoz();
        btnVoz.setOnClickListener(v -> {
            voz.activar(!voz.activa());
            pintarVoz();
            v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
            if (voz.activa()) voz.decir(tvInstruccion.getText().toString());
        });
        levantada = new VistaLevantada(act, this::alCambiarPostura);
        tvPuntualidad.setOnClickListener(v -> {
            if (mejorPlan != null) empezar(mejorPlan, viaje);
        });

        // Foto 4:3 a lo ancho del panel (16 dp de margen a cada lado), sin pasar del 45 % de la pantalla
        android.util.DisplayMetrics dm = act.getResources().getDisplayMetrics();
        altoFoto = Math.min((int) ((dm.widthPixels - 32 * dm.density) * 3 / 4), (int) (dm.heightPixels * 0.45f));
        altoPlegada = (int) (ALTO_PLEGADA_DP * dm.density);
        ponerAltoFoto();

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
            @Override public void handleOnBackPressed() {
                if (levantada.visible()) levantada.ocultar();
                else confirmarTerminar();
            }
        };
        act.getOnBackPressedDispatcher().addCallback(act, atras);

        act.getLifecycle().addObserver(new DefaultLifecycleObserver() {
            @Override public void onResume(@NonNull LifecycleOwner o) { if (activa() && !llegado) reanudar(); }

            @Override public void onPause(@NonNull LifecycleOwner o) { if (activa()) pausar(); }

            @Override public void onDestroy(@NonNull LifecycleOwner o) { voz.cerrar(); }
        });
    }

    // ---------- API ----------

    /** Empieza a guiar con el plan del panel (callback de "Empezar"). */
    public void empezar(PlanRuta plan) {
        empezar(plan, null);
    }

    /** Igual, con el viaje de "¿A dónde vas?" (null = sin hora límite). */
    public void empezar(PlanRuta plan, DestinoViaje viaje) {
        if (activa()) terminar();
        this.viaje = viaje;
        puntualidad = null;
        mejorPlan = null;
        ultimaAlternativa = 0;
        tvPuntualidad.setVisibility(View.GONE);
        seguimiento = new SeguimientoRuta(plan);
        trayecto = new TrayectoSimulado(plan);
        llegado = false;
        simulando = false;
        posicion = null;
        tramoFotos = null;
        fotos = Collections.emptyList();
        puntosTramo = Collections.emptyList();
        ivFoto.setImageDrawable(null);
        tvContador.setVisibility(View.GONE);
        llPuntos.removeAllViews();
        tvPaso.setVisibility(View.GONE);
        tvAviso.setVisibility(View.GONE);
        btnSimular.setText(act.getString(R.string.mapa_guia_simular));
        btnSimular.setVisibility(View.VISIBLE);
        btnTerminar.setText(act.getString(R.string.mapa_guia_terminar));

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

    /** Para proponer otro bus si llegas tarde a clase (sin él solo se dice "ve más rápido"). */
    public void conPlanificador(PlanificadorRuta planificador) {
        this.planificador = planificador;
    }

    /** Lo dice la voz de la guía si está encendida (p. ej. el mapa al llegar: "sigue hasta el aula"). */
    public void decir(String texto) {
        voz.avisar(texto);
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
        voz.callar();
        atras.setEnabled(false);
        act.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        panel.modoGuia(null);
    }

    // ---------- Sensores ----------

    private void reanudar() {
        enMarcha = true;
        brujula.iniciar();
        levantada.iniciar();
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
        levantada.parar();
        escucharGps(false);
    }

    private void escucharGps(boolean escuchar) {
        if (gps == null) return;
        gps.removeUpdates(this);
        if (!escuchar) return;
        // con ubicación precisa el GPS; si solo dieron la aproximada, la de red (peor, pero no se queda buscando)
        String proveedor;
        if (ContextCompat.checkSelfPermission(act, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            proveedor = LocationManager.GPS_PROVIDER;
        } else if (ContextCompat.checkSelfPermission(act, Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            proveedor = LocationManager.NETWORK_PROVIDER;
        } else {
            return;
        }
        try {
            gps.requestLocationUpdates(proveedor, 1000, 0, this, Looper.getMainLooper());
            Location ultima = gps.getLastKnownLocation(proveedor);
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
            btnSimular.setText(act.getString(R.string.mapa_guia_gps));
        } else {
            mapa.getOverlays().remove(marcaSimulada);
            escucharGps(true);
            btnSimular.setText(act.getString(R.string.mapa_guia_simular));
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
            tvEtapa.setText(act.getString(R.string.mapa_guia_buscando_ubicacion));
            instruccionRuta = act.getString(R.string.mapa_guia_sal_a_la_calle);
            tvInstruccion.setText(instruccionRuta);
            tvCuenta.setText("");
            tvLlegada.setText("");
            tvMetros.setText("");
            return;
        }

        SeguimientoRuta.Estado e = seguimiento.actualizar(p, v, ahora);
        pintar(e, ahora);
        pintarPuntualidad(e, p, ahora);
        pintarFotos(e.tramo, p);
        levantada.copiar(fotos.isEmpty() ? null : fotos.get(indiceFoto), tvInstruccion, tvMetros);
        mapa.getController().animateTo(new GeoPoint(p.lat, p.lon));
        for (SeguimientoRuta.Evento ev : e.eventos) avisar(ev);
        if (e.etapa == SeguimientoRuta.Etapa.LLEGADO && !llegado) {
            llegado = true;
            pausar();
            btnSimular.setVisibility(View.GONE);
            btnTerminar.setText(act.getString(R.string.cerrar));
            if (alLlegar != null) alLlegar.accept(seguimiento.plan().destino.id, ahora);
        }
    }

    private void pintar(SeguimientoRuta.Estado e, LocalDateTime ahora) {
        String etapa = e.etapa.texto(act);
        if (e.etapa == SeguimientoRuta.Etapa.EN_BUS && e.paradasRestantes > 0) {
            etapa += " · " + e.paradasRestantes + (e.paradasRestantes == 1 ? " parada" : " paradas");
        }
        tvEtapa.setText(simulando ? etapa + act.getString(R.string.mapa_guia_simulacion) + PlanRuta.hora(ahora) : etapa);
        instruccionRuta = e.instruccion;
        if (puntosTramo.isEmpty()) tvInstruccion.setText(instruccionRuta);
        tvPaso.setText(instruccionRuta);
        tvPaso.setVisibility(puntosTramo.isEmpty() ? View.GONE : View.VISIBLE);

        if (e.etapa == SeguimientoRuta.Etapa.LLEGADO) {
            tvCuenta.setText(act.getString(R.string.mapa_guia_has_llegado));
            tvLlegada.setText(seguimiento.plan().destino.nombre);
        } else {
            tvCuenta.setText(e.cuentaAtras);
            tvLlegada.setText(act.getString(R.string.mapa_guia_llegas_a_las, PlanRuta.horaArriba(e.llegada), e.minutosRestantes(ahora)));
        }

        tvMetros.setText(Double.isNaN(e.rumbo) ? "" : e.metrosAlObjetivo < 1000
                ? Math.round(e.metrosAlObjetivo) + " m"
                : String.format(Locale.getDefault(), "%.1f km", e.metrosAlObjetivo / 1000));
        rumbo = (float) e.rumbo;
        girarFlecha();
    }

    // ---------- Fotos ----------

    /** Al cambiar de tramo carga sus fotos; en un camino a pie, pasa sola a la del siguiente punto. */
    private void pintarFotos(PlanRuta.Tramo tramo, Punto p) {
        if (tramo != tramoFotos || fotos.isEmpty()) {
            tramoFotos = tramo;
            FotosRutaExterior tabla = CapaLugares.cargarFotos(act);
            puntosTramo = tabla == null || tramo == null ? Collections.emptyList() : tabla.delTramo(tramo);
            fotos = tramo != null ? FotoLugar.delTramo(act, tramo)
                    : FotoLugar.deLugar(act, seguimiento.plan().destino.id);
            indiceAuto = -1;
            if (puntosTramo.isEmpty()) voz.decir(instruccionRuta);
            if (!fotos.isEmpty()) {
                ponerFoto(0, false);
            } else {
                ivFoto.setImageDrawable(null);
                tvContador.setVisibility(View.GONE);
                llPuntos.removeAllViews();
            }
        }
        if (puntosTramo.isEmpty()) return;
        int i = FotosRutaExterior.siguiente(puntosTramo, p);
        if (i != indiceAuto) {
            boolean primera = indiceAuto < 0;
            indiceAuto = i;
            if (i != indiceFoto || primera) ponerFoto(i, !primera);
        }
    }

    /** Enseña la foto i del tramo con su contador, puntitos e instrucción (vibrar = avance automático). */
    private void ponerFoto(int i, boolean vibrar) {
        indiceFoto = i;
        String clave = fotos.get(i);
        ivFoto.setImageDrawable(FotoLugar.deClave(act, clave, FotoLugar.ANCHO));
        tvContador.setText((i + 1) + "/" + fotos.size());
        tvContador.setVisibility(fotos.size() > 1 ? View.VISIBLE : View.GONE);
        VisorFotoExterior.pintarPuntos(llPuntos, fotos.size(), i);
        tvInstruccion.setText(puntosTramo.isEmpty() ? instruccionRuta : FotoLugar.textoDe(act, clave));
        levantada.copiar(fotos.isEmpty() ? null : fotos.get(indiceFoto), tvInstruccion, tvMetros);
        if (vibrar && vibrador != null && vibrador.hasVibrator()) {
            vibrador.vibrate(VibrationEffect.createOneShot(40, VibrationEffect.DEFAULT_AMPLITUDE));
        }
        // La voz lee la foto nueva cuando avanza sola (o la primera del tramo), no al deslizar a mano
        if (!puntosTramo.isEmpty() && i == indiceAuto) {
            voz.decir(FotoLugar.textoDe(act, clave));
        }
    }

    /** Deslizar sobre la foto: otra foto o plegar; tocar: pantalla completa. El panel no se lleva el gesto. */
    @SuppressLint("ClickableViewAccessibility")
    private void gestosFoto() {
        GestureDetector gestos = new GestureDetector(act, new GestureDetector.SimpleOnGestureListener() {
            @Override public boolean onDown(@NonNull MotionEvent e) {
                ivFoto.getParent().requestDisallowInterceptTouchEvent(true);
                return true;
            }

            @Override public boolean onSingleTapUp(@NonNull MotionEvent e) {
                ivFoto.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
                VisorFotoExterior.abrir(act, fotos, indiceFoto);
                return true;
            }

            @Override public boolean onFling(MotionEvent e1, @NonNull MotionEvent e2, float vx, float vy) {
                if (Math.abs(vx) > Math.abs(vy)) {
                    int nueva = indiceFoto + (vx < 0 ? 1 : -1);
                    if (nueva >= 0 && nueva < fotos.size()) {
                        ivFoto.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
                        ponerFoto(nueva, false);
                    }
                } else if ((vy > 0) != plegada) {
                    plegar(vy > 0);
                }
                return true;
            }
        });
        ivFoto.setOnTouchListener((v, e) -> gestos.onTouchEvent(e));
    }

    /** Foto plegada = tira baja para ver más mapa; el panel ajusta su alto solo (PanelRutaExterior). */
    private void plegar(boolean si) {
        plegada = si;
        TransitionManager.beginDelayedTransition((ViewGroup) cardFoto.getParent());
        ponerAltoFoto();
    }

    private void ponerAltoFoto() {
        ViewGroup.LayoutParams lp = cardFoto.getLayoutParams();
        lp.height = plegada ? altoPlegada : altoFoto;
        cardFoto.setLayoutParams(lp);
        tvInstruccion.setMaxLines(plegada ? 1 : 2);
    }

    /** Flecha = rumbo al objetivo − hacia dónde mira el móvil (sin brújula: respecto al norte). */
    private void girarFlecha() {
        if (Float.isNaN(rumbo)) {
            ivFlecha.setVisibility(View.INVISIBLE);
            levantada.girar(ivFlecha);
            return;
        }
        ivFlecha.setVisibility(View.VISIBLE);
        ivFlecha.setRotation(rumbo - (brujula.disponible() ? azimut : 0));
        levantada.girar(ivFlecha);
    }

    /** Levantado: la vista copia la guía al momento. Boca abajo: silencia la voz (como el botón 🔇). */
    private void alCambiarPostura(Postura.Tipo postura) {
        if (postura == Postura.Tipo.LEVANTADO) {
            levantada.copiar(fotos.isEmpty() ? null : fotos.get(indiceFoto), tvInstruccion, tvMetros);
            levantada.girar(ivFlecha);
        } else if (postura == Postura.Tipo.NORMAL && !plegada) {
            plegar(true); // al bajar el móvil se vuelve a ver el mapa (Miguel)
        } else if (postura == Postura.Tipo.BOCA_ABAJO && voz.activa()) {
            voz.activar(false);
            pintarVoz();
            if (vibrador != null && vibrador.hasVibrator()) {
                vibrador.vibrate(VibrationEffect.createWaveform(new long[]{0, 60, 80, 60}, -1));
            }
        }
    }

    private void avisar(SeguimientoRuta.Evento ev) {
        tvAviso.setText(ev.texto);
        tvAviso.setVisibility(View.VISIBLE);
        handler.removeCallbacks(quitarAviso);
        handler.postDelayed(quitarAviso, AVISO_MS);
        tvAviso.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
        voz.avisar(ev.texto);
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

    // ---------- ¿Llego a tiempo? ----------

    /**
     * Pastilla verde/ámbar/roja con el margen hasta la hora límite del viaje. En rojo, antes de subir al bus,
     * busca (como mucho una vez por minuto) un plan que llegue a tiempo: tocar la pastilla lo empieza.
     */
    private void pintarPuntualidad(SeguimientoRuta.Estado e, Punto p, LocalDateTime ahora) {
        Puntualidad nueva = viaje == null || e.etapa == SeguimientoRuta.Etapa.LLEGADO ? null
                : Puntualidad.de(e.llegada, viaje.horaLimite);
        if (nueva == null) {
            tvPuntualidad.setVisibility(View.GONE);
            return;
        }
        if (puntualidad != null && puntualidad.nivel != nueva.nivel) voz.avisar(nueva.texto(act));
        puntualidad = nueva;

        boolean antesDelBus = e.etapa == SeguimientoRuta.Etapa.POR_SALIR
                || e.etapa == SeguimientoRuta.Etapa.A_LA_PARADA || e.etapa == SeguimientoRuta.Etapa.ESPERANDO;
        if (nueva.nivel != Puntualidad.Nivel.ROJO || !antesDelBus) {
            mejorPlan = null;
        } else if (planificador != null && SystemClock.elapsedRealtime() - ultimaAlternativa > ALTERNATIVA_MS) {
            ultimaAlternativa = SystemClock.elapsedRealtime();
            mejorPlan = buscarMejorPlan(p, ahora);
        }

        String texto = nueva.texto(act);
        int color;
        switch (nueva.nivel) {
            case VERDE: color = Color.rgb(46, 125, 50); texto = "✅ " + texto; break;
            case AMBAR: color = Color.rgb(239, 108, 0); texto = "⏱ " + texto; break;
            default:
                color = Color.rgb(198, 40, 40);
                texto = "⚠ " + texto + (mejorPlan == null ? (" · " + act.getString(R.string.mapa_guia_ve_mas_rapido))
                        : mejorPlan.tramoBus() != null
                        ? act.getString(R.string.mapa_guia_coge_bus_de, PlanRuta.hora(mejorPlan.tramoBus().inicio))
                        : act.getString(R.string.mapa_guia_ve_andando));
        }
        tvPuntualidad.setText(texto);
        GradientDrawable fondo = new GradientDrawable();
        fondo.setCornerRadius(16 * act.getResources().getDisplayMetrics().density);
        fondo.setColor(color);
        tvPuntualidad.setBackground(fondo);
        tvPuntualidad.setVisibility(View.VISIBLE);
    }

    /** El plan desde aquí que llega a tiempo y antes que el actual (bus anterior, otro bus o a pie); o null. */
    private PlanRuta buscarMejorPlan(Punto p, LocalDateTime ahora) {
        RedTransporte.Lugar destino = seguimiento.plan().destino;
        List<PlanRuta> opciones = new java.util.ArrayList<>(planificador.alternativas(p, destino, ahora));
        opciones.add(planificador.planificar(p, destino, ahora));
        PlanRuta mejor = null;
        Puntualidad referencia = puntualidad;
        for (PlanRuta o : opciones) {
            if (o == null || o.tipo == PlanRuta.Tipo.LEJOS) continue;
            Puntualidad po = Puntualidad.de(o.llegada, viaje.horaLimite);
            if (Puntualidad.mejora(po, referencia)) {
                mejor = o;
                referencia = po;
            }
        }
        return mejor;
    }

    // ---------- Utilidades ----------

    private void pintarVoz() {
        btnVoz.setText(voz.activa() ? "🔊" : "🔇");
        btnVoz.setContentDescription(voz.activa() ? act.getString(R.string.mapa_guia_silenciar_voz) : act.getString(R.string.mapa_guia_activar_voz));
    }

    private void confirmarTerminar() {
        if (llegado) {
            terminar();
            return;
        }
        new AlertDialog.Builder(act)
                .setTitle(act.getString(R.string.mapa_guia_confirmar_titulo))
                .setMessage(act.getString(R.string.mapa_guia_confirmar_msg))
                .setPositiveButton(act.getString(R.string.mapa_guia_terminar), (d, w) -> terminar())
                .setNegativeButton(act.getString(R.string.mapa_guia_seguir), null)
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
