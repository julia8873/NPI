package com.example.nuietsiit.activities.secciones.mapa;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.nuietsiit.R;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import org.osmdroid.events.MapEventsReceiver;
import org.osmdroid.util.BoundingBox;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.FolderOverlay;
import org.osmdroid.views.overlay.MapEventsOverlay;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Overlay;
import org.osmdroid.views.overlay.Polyline;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Panel inferior de "Cómo llegar" en el modo mapa: elige origen (GPS, punto del mapa o un lugar) y
 * destino (automático según el horario o un lugar), planifica con PlanificadorRuta, enseña la línea
 * de tiempo con el bus anterior/siguiente y dibuja la ruta en el mapa.
 */
public final class PanelRutaExterior {

    private static final String DESDE_GPS = "gps";
    private static final String DESDE_MAPA = "mapa";
    private static final String HASTA_AUTO = "auto";
    private static final int INTENTOS_GPS = 20;
    private static final long REFRESCO_MS = 20_000;
    private static final long SEGUIR_GPS_MS = 3_000;
    /** Si el GPS se mueve más que esto desde la salida del plan, se vuelve a calcular. */
    private static final double METROS_RECALCULAR = 60;
    private static final String PREFS = "mapa_ruta_exterior";
    private static final String AYUDA_VISTA = "ayuda_vista";
    private static final long PASEO_CORTO_MIN = 15;
    private static final int COLOR_A_PIE = Color.rgb(69, 90, 100);

    private final AppCompatActivity act;
    private final MapView mapa;
    private final RedTransporte red;
    private final PlanificadorRuta planificador;
    private final Supplier<Calendar> reloj;
    private final Supplier<GeoPoint> ubicacion;
    private final Runnable pedirUbicacion;
    private final String correo;

    private final View contenedor;
    private final BottomSheetBehavior<View> hoja;
    private final TextView tvTitulo, tvLlegada, tvSalida, tvPista, tvOtrosBuses;
    private final View scrollAlternativas;
    private final ChipGroup chipsDesde, chipsHasta, chipsAlternativas;
    private final LinearLayout llPasos;
    private final View llResumen, llElegir, llGuia;
    private final MaterialButton btnEmpezar, btnMaps;
    private final Chip chipPuntoMapa;

    private final FolderOverlay capaRuta = new FolderOverlay();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final OnBackPressedCallback atras;

    private final View.OnLayoutChangeListener ajustarPeek = this::alCambiarGuia;

    private View[] ocultas = new View[0];
    private Punto puntoMapa;
    private RedTransporte.Lugar destinoActual;
    private PlanRuta planActual;
    private Consumer<PlanRuta> alEmpezar;
    private int intentosGps;
    private boolean pintandoChips;
    private Runnable finGuia;
    private int peekNormal;
    /** Miniaturas ya generadas por clave (el panel se repinta cada poco). */
    private final Map<String, Bitmap> miniaturas = new HashMap<>();
    /** El origen lo puso la app (no había GPS): en cuanto haya posición se cambia a "Mi ubicación". */
    private boolean desdeProvisional;

    private final Runnable esperarGps = new Runnable() {
        @Override public void run() {
            if (!estaAbierto() || !DESDE_GPS.equals(tagMarcado(chipsDesde))) return;
            if (ubicacion.get() != null) {
                recalcular();
            } else if (++intentosGps < INTENTOS_GPS) {
                handler.postDelayed(this, 1000);
            } else {
                mostrarAviso("No encuentro tu ubicación",
                        "Mantén pulsado el mapa para elegir desde dónde sales, o elige un lugar.");
            }
        }
    };

    /** Con "Mi ubicación": sigue al GPS y rehace la ruta si te has movido (o si por fin hay posición). */
    private final Runnable seguirGps = new Runnable() {
        @Override public void run() {
            if (!estaAbierto() || finGuia != null) return;
            GeoPoint gps = ubicacion.get();
            if (gps != null) {
                Punto aqui = new Punto(gps.getLatitude(), gps.getLongitude());
                if (desdeProvisional) {
                    desdeProvisional = false;
                    pintandoChips = true;
                    marcar(chipsDesde, DESDE_GPS);
                    pintandoChips = false;
                    recalcular();
                } else if (DESDE_GPS.equals(tagMarcado(chipsDesde)) && planActual != null
                        && aqui.distanciaA(planActual.origen) > METROS_RECALCULAR) {
                    recalcular();
                }
            }
            handler.postDelayed(this, SEGUIR_GPS_MS);
        }
    };

    private final Runnable refrescar = new Runnable() {
        @Override public void run() {
            if (!estaAbierto()) return;
            actualizarSalida();
            handler.postDelayed(this, REFRESCO_MS);
        }
    };

    /**
     * @param raiz           vista con el include de panel_ruta_exterior.xml
     * @param reloj          hora actual (la real o la simulada del mapa)
     * @param ubicacion      última posición GPS o null
     * @param pedirUbicacion pide permiso / activa el GPS si no hay posición
     */
    public PanelRutaExterior(AppCompatActivity act, View raiz, MapView mapa, RedTransporte red, String correo,
                             Supplier<Calendar> reloj, Supplier<GeoPoint> ubicacion, Runnable pedirUbicacion) {
        this.act = act;
        this.mapa = mapa;
        this.red = red;
        this.planificador = new PlanificadorRuta(red);
        this.correo = correo;
        this.reloj = reloj;
        this.ubicacion = ubicacion;
        this.pedirUbicacion = pedirUbicacion;

        contenedor = raiz.findViewById(R.id.panelRutaExterior);
        View sheet = raiz.findViewById(R.id.sheetRutaExterior);
        tvTitulo = raiz.findViewById(R.id.tvRutaExtTitulo);
        tvLlegada = raiz.findViewById(R.id.tvRutaExtLlegada);
        tvSalida = raiz.findViewById(R.id.tvRutaExtSalida);
        tvPista = raiz.findViewById(R.id.tvRutaExtPista);
        tvOtrosBuses = raiz.findViewById(R.id.tvRutaExtOtrosBuses);
        scrollAlternativas = raiz.findViewById(R.id.scrollRutaExtAlternativas);
        chipsDesde = raiz.findViewById(R.id.chipsRutaExtDesde);
        chipsHasta = raiz.findViewById(R.id.chipsRutaExtHasta);
        chipsAlternativas = raiz.findViewById(R.id.chipsRutaExtAlternativas);
        llPasos = raiz.findViewById(R.id.llRutaExtPasos);
        llResumen = raiz.findViewById(R.id.llRutaExtResumen);
        llElegir = raiz.findViewById(R.id.llRutaExtElegir);
        llGuia = raiz.findViewById(R.id.llRutaExtGuia);
        btnEmpezar = raiz.findViewById(R.id.btnRutaExtEmpezar);
        btnMaps = raiz.findViewById(R.id.btnRutaExtMaps);

        hoja = BottomSheetBehavior.from(sheet);
        hoja.setHideable(true);
        hoja.setState(BottomSheetBehavior.STATE_HIDDEN);
        peekNormal = hoja.getPeekHeight();
        hoja.addBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override public void onStateChanged(@NonNull View v, int estado) {
                if (estado == BottomSheetBehavior.STATE_HIDDEN) alCerrar();
            }

            @Override public void onSlide(@NonNull View v, float desplazamiento) {}
        });
        raiz.findViewById(R.id.btnRutaExtCerrar).setOnClickListener(v -> cerrar());
        raiz.findViewById(R.id.btnRutaExtAyuda).setOnClickListener(v -> mostrarAyuda());
        // Tocar "origen → destino" despliega el panel para cambiarlos
        tvTitulo.setOnClickListener(v -> {
            if (finGuia == null) hoja.setState(BottomSheetBehavior.STATE_EXPANDED);
        });

        // Desde: GPS, punto del mapa (tras mantener pulsado) y los lugares de la red
        chipsDesde.addView(chip("📍 Mi ubicación", DESDE_GPS));
        chipPuntoMapa = chip("📌 Punto del mapa", DESDE_MAPA);
        chipPuntoMapa.setVisibility(View.GONE);
        chipsDesde.addView(chipPuntoMapa);
        for (RedTransporte.Lugar l : red.lugares) {
            chipsDesde.addView(chip(l.icono + " " + l.corto + (l.ejemplo ? " (ejemplo)" : ""), l.id));
        }
        // Hasta: automático según el horario y los lugares de la red
        chipsHasta.addView(chip("🎓 Automático", HASTA_AUTO));
        for (RedTransporte.Lugar l : red.lugares) {
            chipsHasta.addView(chip(l.icono + " " + l.corto, l.id));
        }
        chipsDesde.setOnCheckedStateChangeListener((g, ids) -> {
            if (pintandoChips) return;
            desdeProvisional = false;
            recalcular();
        });
        chipsHasta.setOnCheckedStateChangeListener((g, ids) -> { if (!pintandoChips) recalcular(); });
        chipsAlternativas.setOnCheckedStateChangeListener((g, ids) -> {
            if (pintandoChips || ids.isEmpty()) return;
            Object plan = g.findViewById(ids.get(0)).getTag();
            if (plan instanceof PlanRuta) mostrarPlan((PlanRuta) plan);
        });

        btnEmpezar.setOnClickListener(v -> empezarGuia());
        btnMaps.setOnClickListener(v -> abrirGoogleMaps());

        // Capa de la ruta: encima de los contornos y debajo de las paradas (ver CapaLugares.instalar)
        mapa.getOverlays().add(Math.min(1, mapa.getOverlays().size()), capaRuta);
        // Mantener pulsado el mapa = salir desde ahí (va al fondo: los marcadores reciben antes sus toques)
        mapa.getOverlays().add(0, new MapEventsOverlay(new MapEventsReceiver() {
            @Override public boolean singleTapConfirmedHelper(GeoPoint p) { return false; }

            @Override public boolean longPressHelper(GeoPoint p) {
                if (finGuia != null) return false;
                mapa.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
                abrirDesde(new Punto(p.getLatitude(), p.getLongitude()));
                return true;
            }
        }));

        atras = new OnBackPressedCallback(false) {
            @Override public void handleOnBackPressed() { cerrar(); }
        };
        act.getOnBackPressedDispatcher().addCallback(act, atras);
    }

    // ---------- API ----------

    /**
     * Abre el panel saliendo desde el GPS si ya hay posición; si no, desde el lugar de ejemplo
     * (y en cuanto llega la posición se cambia solo a "Mi ubicación").
     */
    public void abrir() {
        if (finGuia != null) return;
        String desde = DESDE_GPS;
        if (ubicacion.get() == null) {
            for (RedTransporte.Lugar l : red.lugares) {
                if (l.ejemplo) { desde = l.id; break; }
            }
        }
        desdeProvisional = !DESDE_GPS.equals(desde);
        if (desdeProvisional) pedirUbicacion.run();
        abrirCon(desde);
    }

    /** Abre el panel saliendo desde un punto elegido en el mapa. */
    public void abrirDesde(Punto punto) {
        if (finGuia != null) return; // no cambiar la ruta en mitad de la guía
        desdeProvisional = false;
        puntoMapa = punto;
        chipPuntoMapa.setVisibility(View.VISIBLE);
        abrirCon(DESDE_MAPA);
    }

    /** Abre el panel entre dos lugares de la red (p. ej. ETSIIT → Auxiliar al llegar a la ETSIIT). */
    public void abrirEntre(String idDesde, String idHasta) {
        if (finGuia != null) return;
        desdeProvisional = false;
        pintandoChips = true;
        marcar(chipsHasta, idHasta);
        pintandoChips = false;
        abrirCon(idDesde);
    }

    /** Abre el panel hacia un lugar (tocar un edificio → "Llévame aquí") con "Empezar guía" destacado. */
    public void abrirHacia(String idHasta) {
        if (finGuia != null) return;
        pintandoChips = true;
        marcar(chipsHasta, idHasta);
        pintandoChips = false;
        abrir();
        btnEmpezar.postDelayed(() -> {
            if (!btnEmpezar.isEnabled()) return;
            btnEmpezar.animate().scaleX(1.08f).scaleY(1.08f).setDuration(180)
                    .withEndAction(() -> btnEmpezar.animate().scaleX(1f).scaleY(1f).setDuration(180).start())
                    .start();
        }, 700);
    }

    /** Vistas (botones flotantes del mapa) que se esconden mientras el panel está abierto. */
    public void ocultarAlAbrir(View... vistas) {
        ocultas = vistas;
    }

    public boolean estaAbierto() {
        return contenedor.getVisibility() == View.VISIBLE;
    }

    /** Cierra el panel; si la guía está activa, la termina antes. */
    public void cerrar() {
        if (finGuia != null) finGuia.run();
        if (!estaAbierto()) return;
        hoja.setState(BottomSheetBehavior.STATE_HIDDEN);
    }

    /** Lo engancha la guía (F4): sin callback el botón "Empezar" queda desactivado. */
    public void alEmpezar(Consumer<PlanRuta> callback) {
        alEmpezar = callback;
        activarEmpezar(callback != null && planActual != null && planActual.tipo != PlanRuta.Tipo.LEJOS);
    }

    /**
     * Guía activa (F4): esconde el resumen y la elección de ruta, deja arriba el bloque de la guía
     * (guia_ruta_exterior.xml, lo pinta GuiaRutaExterior) y la línea de tiempo debajo.
     *
     * @param alTerminar termina la guía (lo llama cerrar()); null = salir del modo guía
     */
    public void modoGuia(Runnable alTerminar) {
        finGuia = alTerminar;
        boolean guiando = alTerminar != null;
        llResumen.setVisibility(guiando ? View.GONE : View.VISIBLE);
        llElegir.setVisibility(guiando ? View.GONE : View.VISIBLE);
        llGuia.setVisibility(guiando ? View.VISIBLE : View.GONE);
        hoja.setHideable(!guiando);
        handler.removeCallbacks(refrescar);
        if (guiando) {
            handler.removeCallbacks(seguirGps);
            tvTitulo.setText("Te llevo a " + planActual.destino.corto);
            llGuia.addOnLayoutChangeListener(ajustarPeek);
            llGuia.post(() -> hoja.setState(BottomSheetBehavior.STATE_COLLAPSED));
        } else {
            llGuia.removeOnLayoutChangeListener(ajustarPeek);
            hoja.setPeekHeight(peekNormal);
            mapa.setMapCenterOffset(0, 0);
            if (planActual != null) mostrarPlan(planActual);
            if (estaAbierto()) {
                handler.postDelayed(refrescar, REFRESCO_MS);
                handler.postDelayed(seguirGps, SEGUIR_GPS_MS);
            }
        }
    }

    /** Con la guía, el trozo visible del panel = justo el bloque de la guía (crece con el aviso). */
    private void alCambiarGuia(View v, int l, int t, int r, int b, int ol, int ot, int or, int ob) {
        int alto = b + (int) (12 * v.getResources().getDisplayMetrics().density);
        if (alto != hoja.getPeekHeight()) v.post(() -> hoja.setPeekHeight(alto, true));
        // tu posición (centro del mapa) en medio del trozo de mapa que deja ver el panel
        mapa.setMapCenterOffset(0, -alto / 2);
    }

    public PlanRuta planActual() {
        return planActual;
    }

    /** Pinta un plan: resumen, línea de tiempo y ruta en el mapa. */
    public void mostrarPlan(PlanRuta plan) {
        planActual = plan;
        destinoActual = plan.destino;
        pintarTitulo(plan.destino);
        boolean lejos = plan.tipo == PlanRuta.Tipo.LEJOS;
        activarEmpezar(alEmpezar != null && !lejos);
        btnMaps.setEnabled(true);

        if (lejos) {
            tvLlegada.setText(String.format(Locale.getDefault(), "Estás a %.1f km", plan.metrosRecta / 1000));
            tvSalida.setText("Desde aquí no hay ruta a pie ni en bus: usa Google Maps");
        } else {
            tvLlegada.setText("Llegas a las " + PlanRuta.horaArriba(plan.llegada) + " · "
                    + plan.minutosTotales() + " min");
            actualizarSalida();
        }
        pintarPasos(plan);
        marcarAlternativa(plan);
        dibujar(plan);
    }

    // ---------- Planificación ----------

    private void abrirCon(String tagDesde) {
        intentosGps = 0;
        pintandoChips = true;
        marcar(chipsDesde, tagDesde);
        if (chipsHasta.getCheckedChipId() == View.NO_ID) marcar(chipsHasta, HASTA_AUTO);
        pintandoChips = false;

        if (!estaAbierto()) {
            contenedor.setVisibility(View.VISIBLE);
            for (View v : ocultas) v.setVisibility(View.GONE);
            contenedor.post(() -> hoja.setState(BottomSheetBehavior.STATE_COLLAPSED));
            atras.setEnabled(true);
            handler.postDelayed(refrescar, REFRESCO_MS);
            handler.postDelayed(seguirGps, SEGUIR_GPS_MS);
            if (!prefs().getBoolean(AYUDA_VISTA, false)) contenedor.postDelayed(this::mostrarAyuda, 600);
        }
        recalcular();
    }

    private void recalcular() {
        handler.removeCallbacks(esperarGps);
        RedTransporte.Lugar destino = destinoElegido();
        Punto origen = origenElegido();
        if (origen == null) return; // esperando al GPS

        destinoActual = destino;
        pintarTitulo(destino);
        if (destino.contiene(origen) || origen.distanciaA(destino.punto) < 25) {
            limpiarPlan();
            mostrarAviso("Ya estás en " + destino.corto, "Elige otro destino o sal desde otro sitio.");
            return;
        }

        LocalDateTime ahora = ahora();
        PlanRuta plan = planificador.planificar(origen, destino, ahora);
        List<PlanRuta> opciones = new ArrayList<>();
        if (plan.tipo == PlanRuta.Tipo.A_PIE) opciones.add(plan);
        // Si andando es un paseo corto, no tiene sentido ofrecer buses
        if (plan.tipo == PlanRuta.Tipo.BUS || (plan.tipo == PlanRuta.Tipo.A_PIE && plan.minutosTotales() > PASEO_CORTO_MIN)) {
            opciones.addAll(planificador.alternativas(origen, destino, ahora));
        }
        pintarAlternativas(opciones);
        mostrarPlan(plan);
    }

    /** Punto de salida según el chip marcado; null si hay que esperar al GPS. */
    private Punto origenElegido() {
        String tag = tagMarcado(chipsDesde);
        if (DESDE_MAPA.equals(tag) && puntoMapa != null) return puntoMapa;
        if (DESDE_GPS.equals(tag) || tag == null) {
            GeoPoint gps = ubicacion.get();
            if (gps != null) return new Punto(gps.getLatitude(), gps.getLongitude());
            limpiarPlan();
            mostrarAviso("Buscando tu ubicación…", "Mientras, puedes mantener pulsado el mapa o elegir un lugar.");
            pedirUbicacion.run();
            handler.postDelayed(esperarGps, 1000);
            return null;
        }
        return red.lugar(tag).punto;
    }

    /** Destino según el chip marcado; "Automático" = edificio de la clase en curso o la siguiente de hoy. */
    private RedTransporte.Lugar destinoElegido() {
        String tag = tagMarcado(chipsHasta);
        if (tag != null && !HASTA_AUTO.equals(tag)) {
            tvPista.setVisibility(View.GONE);
            return red.lugar(tag);
        }
        ProximaClase clase = ProximaClase.de(act, correo, reloj.get());
        RedTransporte.Lugar lugar = red.lugar(clase != null ? clase.idEdificio : ProximaClase.ETSIIT);
        tvPista.setVisibility(View.VISIBLE);
        tvPista.setText(clase != null
                ? "Según tu horario: " + clase.texto() + " → " + lugar.corto
                : "Hoy no tienes más clases: te llevo a la " + lugar.corto);
        return lugar;
    }

    private LocalDateTime ahora() {
        return LocalDateTime.ofInstant(reloj.get().toInstant(), ZoneId.systemDefault());
    }

    /** "Sal ya" / "Sal a las 8:39 (en 3 min)" según la hora actual. */
    private void actualizarSalida() {
        if (planActual == null || planActual.tipo == PlanRuta.Tipo.LEJOS) return;
        if (planActual.tipo == PlanRuta.Tipo.A_PIE) {
            tvSalida.setText(String.format(Locale.getDefault(), "🚶 Todo a pie · %d m · sal cuando quieras",
                    Math.round(planActual.metrosAPie)));
            return;
        }
        long min = PlanRuta.minutosArriba(Duration.between(ahora(), planActual.salida));
        tvSalida.setText(min <= 0 ? "¡Sal ya! (" + PlanRuta.hora(planActual.salida) + ")"
                : "Sal a las " + PlanRuta.hora(planActual.salida) + " (en " + min + " min)");
    }

    /**
     * La guía sigue el GPS: si la ruta sale de otro sitio (punto del mapa o un lugar) y no estás allí,
     * se pregunta si simular el trayecto o rehacer la ruta desde tu ubicación.
     */
    private void empezarGuia() {
        if (alEmpezar == null || planActual == null) return;
        GeoPoint gps = ubicacion.get();
        boolean estasEnLaSalida = gps != null
                && new Punto(gps.getLatitude(), gps.getLongitude()).distanciaA(planActual.origen) <= 150;
        if (DESDE_GPS.equals(tagMarcado(chipsDesde)) || estasEnLaSalida) {
            alEmpezar.accept(planActual);
            return;
        }
        String origen = nombreOrigen();
        // Sin posición no sabemos dónde estás: no decir "no estás en…" (puede que acabes de elegirlo tú)
        AlertDialog.Builder dialogo = new AlertDialog.Builder(act)
                .setTitle(gps == null ? "No te encuentro con el GPS" : "No estás en " + origen)
                .setMessage(gps == null
                        ? "La guía te sigue con el GPS. Si empiezas, te irá guiando desde " + origen
                        + " en cuanto el GPS te encuentre.\n\nTambién puedes simular el trayecto para ver cómo sería."
                        : "La guía te sigue con el GPS, y esta ruta sale desde " + origen + ".\n\n"
                        + "Puedes simular el trayecto para ver cómo sería, o calcular la ruta desde donde estás.")
                .setPositiveButton("▶ Simular trayecto", (d, w) -> {
                    alEmpezar.accept(planActual);
                    // El botón "Simular" es de la guía (guia_ruta_exterior.xml va dentro de este panel)
                    llGuia.findViewById(R.id.btnGuiaSimular).performClick();
                })
                .setNeutralButton("Cancelar", null);
        if (gps != null) {
            dialogo.setNegativeButton("📍 Desde mi ubicación", (d, w) -> marcar(chipsDesde, DESDE_GPS));
        } else {
            dialogo.setNegativeButton("Empezar", (d, w) -> alEmpezar.accept(planActual));
        }
        dialogo.show();
    }

    // ---------- Vistas ----------

    /** Cabecera "📍 Tu ubicación → 🎓 ETSIIT · cambiar": dice siempre desde dónde y hasta dónde. */
    private void pintarTitulo(RedTransporte.Lugar destino) {
        String tag = tagMarcado(chipsDesde);
        String icono = DESDE_MAPA.equals(tag) ? "📌" : tag == null || DESDE_GPS.equals(tag) ? "📍" : red.lugar(tag).icono;
        String origen = nombreOrigen();
        SpannableStringBuilder sb = new SpannableStringBuilder(icono + " "
                + origen.substring(0, 1).toUpperCase(Locale.getDefault()) + origen.substring(1)
                + (desdeProvisional ? " (sin GPS)" : "") + "  →  " + destino.icono + " " + destino.corto);
        int ini = sb.length();
        sb.append("   cambiar ▾");
        sb.setSpan(new ForegroundColorSpan(ContextCompat.getColor(act, R.color.nui_acento)),
                ini, sb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        tvTitulo.setText(sb);
    }

    private String nombreOrigen() {
        String tag = tagMarcado(chipsDesde);
        return DESDE_MAPA.equals(tag) ? "el punto elegido"
                : tag == null || DESDE_GPS.equals(tag) ? "tu ubicación" : red.lugar(tag).corto;
    }

    /** Instrucciones del panel; salen solas la primera vez y luego con el botón "?". */
    private void mostrarAyuda() {
        if (act.isFinishing()) return;
        prefs().edit().putBoolean(AYUDA_VISTA, true).apply();
        new AlertDialog.Builder(act)
                .setTitle("Cómo llegar")
                .setMessage("📍 Desde: por defecto sales de tu ubicación (GPS). Si te mueves, la ruta se recalcula sola.\n\n"
                        + "👆 Mantén pulsado cualquier punto del mapa para salir desde ahí, o elige un lugar en «Desde».\n\n"
                        + "🎓 Hasta: «Automático» te lleva al edificio de tu próxima clase según tu horario.\n\n"
                        + "🚌 Si compensa, te propone el bus 9 con la hora a la que tienes que salir. "
                        + "En «Otros buses» puedes elegir uno antes o después.\n\n"
                        + "📷 Toca un paso de la lista para ver una foto del sitio.\n\n"
                        + "▶ «Empezar guía» te acompaña paso a paso: flecha, cuenta atrás y vibración "
                        + "al salir, al llegar el bus y al bajarte.\n\n"
                        + "📱 Durante la guía, levanta el móvil en vertical para ver la foto del camino en grande "
                        + "con la flecha; bájalo para volver al mapa. Ponlo boca abajo para silenciar la voz.\n\n"
                        + "Arrastra el panel hacia arriba para ver todo y hacia abajo para ver el mapa.")
                .setPositiveButton("Entendido", null)
                .show();
    }

    private SharedPreferences prefs() {
        return act.getSharedPreferences(PREFS, AppCompatActivity.MODE_PRIVATE);
    }

    private void pintarAlternativas(List<PlanRuta> opciones) {
        pintandoChips = true;
        chipsAlternativas.removeAllViews();
        for (PlanRuta p : opciones) {
            PlanRuta.Tramo bus = p.tramoBus();
            String texto = bus != null
                    ? "🚌 " + bus.linea.id + " · " + PlanRuta.hora(bus.inicio) + " → " + PlanRuta.horaArriba(p.llegada)
                    : "🚶 A pie → " + PlanRuta.horaArriba(p.llegada);
            Chip c = chip(texto, null);
            c.setTag(p);
            chipsAlternativas.addView(c);
        }
        pintandoChips = false;
        int visible = opciones.size() > 1 ? View.VISIBLE : View.GONE;
        tvOtrosBuses.setVisibility(visible);
        scrollAlternativas.setVisibility(visible);
    }

    private void marcarAlternativa(PlanRuta plan) {
        pintandoChips = true;
        for (int i = 0; i < chipsAlternativas.getChildCount(); i++) {
            Chip c = (Chip) chipsAlternativas.getChildAt(i);
            PlanRuta p = (PlanRuta) c.getTag();
            if (p == plan || mismoPlan(p, plan)) c.setChecked(true);
        }
        pintandoChips = false;
    }

    private static boolean mismoPlan(PlanRuta a, PlanRuta b) {
        return a.tipo == b.tipo && a.salida.equals(b.salida) && a.llegada.equals(b.llegada);
    }

    /** Línea de tiempo: salida, una fila por tramo (miniatura; toque = fotos del camino) y llegada. */
    private void pintarPasos(PlanRuta plan) {
        llPasos.removeAllViews();
        if (plan.tipo == PlanRuta.Tipo.LEJOS) return;
        String tag = tagMarcado(chipsDesde);
        String icono = tag == null || DESDE_GPS.equals(tag) || DESDE_MAPA.equals(tag) ? "📍" : "🏠";
        llPasos.addView(fila(PlanRuta.hora(plan.salida), icono, null, "Sal desde " + nombreOrigen(), null, null));
        for (PlanRuta.Tramo t : plan.tramos) {
            switch (t.tipo) {
                case A_PIE:
                    llPasos.addView(fila(PlanRuta.hora(t.inicio), "🚶", null, t.instruccion,
                            t.minutos() + " min a pie", t));
                    break;
                case ESPERA:
                    llPasos.addView(fila(PlanRuta.hora(t.inicio), "🕒", null, t.instruccion,
                            t.minutos() + " min de margen en la parada", t));
                    break;
                case BUS:
                    llPasos.addView(fila(PlanRuta.hora(t.inicio), t.linea.id, colorDe(t.linea.color),
                            t.instruccion, t.minutos() + " min en bus · bájate a las " + PlanRuta.hora(t.fin), t));
                    break;
            }
        }
        llPasos.addView(fila(PlanRuta.horaArriba(plan.llegada), "🏁", null,
                "Llegas a " + plan.destino.nombre, null, null));
    }

    /** Fila: hora · icono (o pastilla de la línea si color != null) · texto y detalle en gris. */
    private View fila(String hora, String icono, Integer color, String texto, String detalle, PlanRuta.Tramo tramo) {
        float dp = act.getResources().getDisplayMetrics().density;
        LinearLayout fila = new LinearLayout(act);
        fila.setOrientation(LinearLayout.HORIZONTAL);
        fila.setGravity(Gravity.CENTER_VERTICAL);
        fila.setMinimumHeight((int) (48 * dp));
        fila.setPadding(0, (int) (4 * dp), 0, (int) (4 * dp));

        TextView tvHora = new TextView(act);
        tvHora.setText(hora);
        tvHora.setTypeface(Typeface.DEFAULT_BOLD);
        tvHora.setTextColor(ContextCompat.getColor(act, R.color.nui_tinta));
        tvHora.setTextSize(13);
        fila.addView(tvHora, new LinearLayout.LayoutParams((int) (44 * dp), -2));

        TextView tvIcono = new TextView(act);
        tvIcono.setText(icono);
        tvIcono.setGravity(Gravity.CENTER);
        tvIcono.setTextSize(color != null ? 13 : 18);
        if (color != null) {
            GradientDrawable pastilla = new GradientDrawable();
            pastilla.setCornerRadius(6 * dp);
            pastilla.setColor(color);
            tvIcono.setBackground(pastilla);
            tvIcono.setTextColor(Color.WHITE);
            tvIcono.setTypeface(Typeface.DEFAULT_BOLD);
        }
        LinearLayout.LayoutParams lpIcono = new LinearLayout.LayoutParams((int) (28 * dp), (int) (28 * dp));
        lpIcono.setMarginEnd((int) (10 * dp));
        fila.addView(tvIcono, lpIcono);

        SpannableStringBuilder sb = new SpannableStringBuilder(texto);
        if (detalle != null) {
            int ini = sb.length();
            sb.append("\n").append(detalle);
            sb.setSpan(new RelativeSizeSpan(0.85f), ini, sb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            sb.setSpan(new ForegroundColorSpan(ContextCompat.getColor(act, R.color.nui_apagado)),
                    ini, sb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        TextView tvTexto = new TextView(act);
        tvTexto.setText(sb);
        tvTexto.setTextColor(ContextCompat.getColor(act, R.color.nui_tinta));
        tvTexto.setTextSize(14);
        fila.addView(tvTexto, new LinearLayout.LayoutParams(0, -2, 1f));

        List<String> fotos = FotoLugar.delTramo(act, tramo);
        if (fotos.isEmpty()) return fila;
        fila.addView(miniatura(fotos.get(0), 64, 48));
        fila.setBackgroundResource(fondoSeleccionable());
        fila.setOnClickListener(v -> verTramo(tramo, fotos, 0));
        if (fotos.size() < 2) return fila;

        // "📷 Ver el camino (8 fotos)": una línea tocable que abre el visor desde la primera.
        // (Antes había una tira de miniaturas por tramo; se quitó para que el panel quepa sin scroll.)
        LinearLayout bloque = new LinearLayout(act);
        bloque.setOrientation(LinearLayout.VERTICAL);
        bloque.addView(fila);
        int sangria = (int) (82 * dp);
        TextView titulo = new TextView(act);
        titulo.setText("📷 Ver el camino (" + fotos.size() + " fotos)");
        titulo.setTextColor(ContextCompat.getColor(act, R.color.nui_acento));
        titulo.setTypeface(Typeface.DEFAULT_BOLD);
        titulo.setTextSize(12);
        titulo.setPadding(sangria, 0, 0, (int) (6 * dp));
        titulo.setBackgroundResource(fondoSeleccionable());
        titulo.setOnClickListener(v -> verTramo(tramo, fotos, 0));
        bloque.addView(titulo);
        return bloque;
    }

    /** Foto pequeña con esquinas redondeadas (anchoDp × altoDp, recortada). */
    private ImageView miniatura(String clave, int anchoDp, int altoDp) {
        float dp = act.getResources().getDisplayMetrics().density;
        Bitmap bmp = miniaturas.get(clave);
        if (bmp == null) {
            bmp = ((BitmapDrawable) FotoLugar.deClave(act, clave, (int) (anchoDp * dp))).getBitmap();
            miniaturas.put(clave, bmp);
        }
        ImageView iv = new ImageView(act);
        iv.setImageDrawable(new BitmapDrawable(act.getResources(), bmp));
        iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
        GradientDrawable forma = new GradientDrawable();
        forma.setCornerRadius(10 * dp);
        iv.setBackground(forma);
        iv.setClipToOutline(true);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams((int) (anchoDp * dp), (int) (altoDp * dp));
        lp.setMarginStart((int) (6 * dp));
        iv.setLayoutParams(lp);
        return iv;
    }

    private int fondoSeleccionable() {
        android.util.TypedValue tv = new android.util.TypedValue();
        act.getTheme().resolveAttribute(android.R.attr.selectableItemBackground, tv, true);
        return tv.resourceId;
    }

    /** Centra el mapa en el tramo y abre sus fotos a pantalla completa desde la `inicial`. */
    private void verTramo(PlanRuta.Tramo t, List<String> fotos, int inicial) {
        if (!t.geometria.isEmpty()) mapa.getController().animateTo(geo(t.geometria.get(0)));
        VisorFotoExterior.abrir(act, fotos, inicial);
    }

    private void activarEmpezar(boolean activo) {
        btnEmpezar.setEnabled(activo);
        btnEmpezar.setAlpha(activo ? 1f : 0.45f);
    }

    private void mostrarAviso(String titulo, String texto) {
        tvLlegada.setText(titulo);
        tvSalida.setText(texto);
    }

    private void limpiarPlan() {
        planActual = null;
        activarEmpezar(false);
        llPasos.removeAllViews();
        pintarAlternativas(new ArrayList<>());
        capaRuta.getItems().clear();
        mapa.invalidate();
    }

    private void alCerrar() {
        contenedor.setVisibility(View.GONE);
        if (mapa.getVisibility() == View.VISIBLE) {
            for (View v : ocultas) v.setVisibility(View.VISIBLE);
        }
        atras.setEnabled(false);
        handler.removeCallbacks(esperarGps);
        handler.removeCallbacks(refrescar);
        handler.removeCallbacks(seguirGps);
        capaRuta.getItems().clear();
        mapa.invalidate();
    }

    // ---------- Mapa ----------

    /** A pie: discontinua gris; bus: del color de la línea; marcas en salida, subida y bajada. */
    private void dibujar(PlanRuta plan) {
        capaRuta.getItems().clear();
        float dp = act.getResources().getDisplayMetrics().density;
        List<GeoPoint> todos = new ArrayList<>();
        todos.add(geo(plan.origen));
        todos.add(geo(plan.destino.punto));

        for (PlanRuta.Tramo t : plan.tramos) {
            if (t.tipo == PlanRuta.TipoTramo.ESPERA || t.geometria.size() < 2) continue;
            List<GeoPoint> puntos = new ArrayList<>();
            for (Punto p : t.geometria) puntos.add(geo(p));
            todos.addAll(puntos);
            Polyline linea = new Polyline(mapa);
            linea.setPoints(puntos);
            linea.setInfoWindow(null);
            Paint pintura = linea.getOutlinePaint();
            pintura.setStrokeCap(Paint.Cap.ROUND);
            if (t.tipo == PlanRuta.TipoTramo.BUS) {
                pintura.setColor(colorDe(t.linea.color));
                pintura.setStrokeWidth(9 * dp);
            } else {
                pintura.setColor(COLOR_A_PIE);
                pintura.setStrokeWidth(5 * dp);
                pintura.setPathEffect(new DashPathEffect(new float[]{3 * dp, 9 * dp}, 0));
            }
            capaRuta.add(linea);
        }

        capaRuta.add(marca(plan.origen, ContextCompat.getColor(act, R.color.nui_acento), Color.WHITE, 24, "Sales de aquí"));
        capaRuta.add(marca(plan.destino.punto, ContextCompat.getColor(act, R.color.nui_tinta), Color.WHITE, 20,
                "Llegas aquí · " + plan.destino.nombre));
        PlanRuta.Tramo bus = plan.tramoBus();
        if (bus != null) {
            int color = colorDe(bus.linea.color);
            capaRuta.add(marca(bus.subida.punto, color, Color.WHITE, 18, "Sube al " + bus.linea.id + " · " + bus.subida.nombre));
            capaRuta.add(marca(bus.bajada.punto, color, Color.WHITE, 18, "Bájate · " + bus.bajada.nombre));
        }
        mapa.invalidate();
        encuadrar(todos);
    }

    /** Ajusta el zoom a la ruta dejando hueco abajo para el panel. */
    private void encuadrar(List<GeoPoint> puntos) {
        // Si el mapa sigue al GPS, volvería a centrarse en el usuario
        for (Overlay o : mapa.getOverlays()) {
            if (o instanceof MyLocationNewOverlay) ((MyLocationNewOverlay) o).disableFollowLocation();
        }
        BoundingBox caja = BoundingBox.fromGeoPoints(puntos);
        double alto = Math.max(caja.getLatitudeSpan(), 0.002);
        double ancho = Math.max(caja.getLongitudeSpan(), 0.002);
        // Parte del mapa que tapa el panel recogido: se deja ese hueco abajo
        int altoMapa = mapa.getHeight();
        double tapado = altoMapa > 0 ? Math.min(0.6, (double) hoja.getPeekHeight() / altoMapa) : 0.35;
        double abajo = alto * (0.12 + tapado / (1 - tapado));
        // A los lados, más margen: las etiquetas de los edificios van centradas en su punto
        BoundingBox conHueco = new BoundingBox(caja.getLatNorth() + alto * 0.15, caja.getLonEast() + ancho * 0.25,
                caja.getLatSouth() - abajo, caja.getLonWest() - ancho * 0.25);
        mapa.post(() -> mapa.zoomToBoundingBox(conHueco, false,
                (int) (24 * act.getResources().getDisplayMetrics().density), 18.0, null));
    }

    private Marker marca(Punto p, int relleno, int borde, int tamDp, String titulo) {
        float dp = act.getResources().getDisplayMetrics().density;
        GradientDrawable circulo = new GradientDrawable();
        circulo.setShape(GradientDrawable.OVAL);
        circulo.setColor(relleno);
        circulo.setStroke((int) (3 * dp), borde);
        circulo.setSize((int) (tamDp * dp), (int) (tamDp * dp));
        Marker m = new Marker(mapa);
        m.setPosition(geo(p));
        m.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER);
        m.setIcon(circulo);
        m.setTitle(titulo);
        return m;
    }

    private void abrirGoogleMaps() {
        RedTransporte.Lugar destino = destinoActual != null ? destinoActual : destinoElegido();
        StringBuilder url = new StringBuilder("https://www.google.com/maps/dir/?api=1");
        if (planActual != null) {
            url.append("&origin=").append(planActual.origen.lat).append(',').append(planActual.origen.lon);
        }
        url.append("&destination=").append(destino.punto.lat).append(',').append(destino.punto.lon)
                .append("&travelmode=transit");
        act.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url.toString())));
    }

    // ---------- Utilidades ----------

    private Chip chip(String texto, String tag) {
        Chip c = new Chip(act);
        c.setText(texto);
        c.setTag(tag);
        c.setCheckable(true);
        c.setCheckedIconVisible(false);
        int[][] estados = {{android.R.attr.state_checked}, {}};
        c.setChipBackgroundColor(new ColorStateList(estados, new int[]{
                ContextCompat.getColor(act, R.color.nui_tinte), ContextCompat.getColor(act, R.color.nui_suave)}));
        c.setChipStrokeColor(new ColorStateList(estados, new int[]{
                ContextCompat.getColor(act, R.color.nui_acento), ContextCompat.getColor(act, R.color.nui_linea)}));
        c.setChipStrokeWidth(act.getResources().getDisplayMetrics().density * 1.5f);
        c.setTextColor(ContextCompat.getColor(act, R.color.nui_tinta));
        c.setId(View.generateViewId());
        return c;
    }

    private static void marcar(ChipGroup grupo, String tag) {
        for (int i = 0; i < grupo.getChildCount(); i++) {
            View v = grupo.getChildAt(i);
            if (tag.equals(v.getTag())) grupo.check(v.getId());
        }
    }

    private static String tagMarcado(ChipGroup grupo) {
        View v = grupo.findViewById(grupo.getCheckedChipId());
        return v != null ? (String) v.getTag() : null;
    }

    private static GeoPoint geo(Punto p) {
        return new GeoPoint(p.lat, p.lon);
    }

    private static int colorDe(String hex) {
        try {
            return Color.parseColor(hex);
        } catch (Exception e) {
            return Color.rgb(194, 24, 91);
        }
    }
}
