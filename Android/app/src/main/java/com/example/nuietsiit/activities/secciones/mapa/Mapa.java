package com.example.nuietsiit.activities.secciones.mapa;

import android.Manifest;
import android.annotation.SuppressLint;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.graphics.drawable.GradientDrawable;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Filter;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;

import com.example.nuietsiit.R;
import com.example.nuietsiit.activities.BaseActivity;
import android.content.SharedPreferences;
import android.view.HapticFeedbackConstants;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.nuietsiit.activities.auth.Login;
import com.example.nuietsiit.activities.main.AjustesCuenta;
import com.example.nuietsiit.util.RecienteUtil;
import com.example.nuietsiit.util.SesionUtil;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;

import org.json.JSONArray;
import org.json.JSONObject;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase;
import org.osmdroid.tileprovider.tilesource.XYTileSource;
import org.osmdroid.util.BoundingBox;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.CustomZoomButtonsController;
import org.osmdroid.views.CustomZoomButtonsDisplay;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polyline;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;

public class Mapa extends BaseActivity {

    private static final String ESTADO_RUTA_DESTINO = "rutaDestino";
    private static final String ESTADO_ORIGEN_ID = "origenId";
    private static final String ESTADO_ORIGEN_NOMBRE = "origenNombre";
    private static final String ESTADO_ORIGEN_PLANTA = "origenPlanta";
    private static final String ESTADO_ORIGEN_METODO = "origenMetodo";
    private static final String ESTADO_ORIGEN_X = "origenX";
    private static final String ESTADO_ORIGEN_Y = "origenY";

    public static final OnlineTileSourceBase OPENMAP_TILES = new XYTileSource(
            "OSM_DE",
            0, 19, 256, ".png",
            new String[]{
                    "https://tile.openstreetmap.de/"
            },
            "© OpenStreetMap contributors"
    );

    // Vistas principales
    private MapView mapView;
    private ImageView ivPlanoEdificio;
    private ImageView ivIcono;
    private FloatingActionButton fabMiUbicacion;
    private ExtendedFloatingActionButton fabADondeVas;
    private ExtendedFloatingActionButton fabComoLlegar;
    private AutoCompleteTextView etBuscadorMapa;
    private MaterialCardView cardBuscadorMapa;
    private HorizontalScrollView scrollPlantas;
    private ChipGroup chipGroupPlantas;
    private MaterialButtonToggleGroup toggleGroupModo;

    private MyLocationNewOverlay myLocationOverlay;
    private Polyline rutaPolyline;

    // Matrix para zoom/pan en la imagen del plano
    private final Matrix matrix = new Matrix();
    private final Matrix savedMatrix = new Matrix();
    private static final int NONE = 0;
    private static final int DRAG = 1;
    private static final int ZOOM = 2;
    private int mode = NONE;
    private final PointF start = new PointF();
    private final PointF mid = new PointF();
    private float oldDist = 1f;

    // Coordenadas de la ETSIIT UGR (Granada)
    private final GeoPoint etsiitPoint = new GeoPoint(37.197087, -3.624508);
    private RedTransporte redTransporte;
    private PanelRutaExterior panelRutaExterior; // "Cómo llegar" a pie y en bus
    private GuiaRutaExterior guiaRutaExterior; // "Empezar": guía con GPS, brújula y avisos

    private FrameLayout containerPlanoConMarcadores;
    private final List<Marcador> listaMarcadores = new ArrayList<>();
    private String plantaActual = "planta0";

    // --- Ruta / ubicación del usuario dentro de la ETSIIT ---
    private TextView tvTituloMapa;
    private MaterialCardView cardRutaActiva;
    private TextView tvRutaInfo;
    private MaterialButton btnAbrirVisor;
    private MaterialButton btnCancelarRuta;
    private Marcador destinoRuta;          // punto naranja al que quiere ir
    private UbicacionUsuario origenActual; // dónde hemos ubicado al usuario
    private Marcador marcadorOrigen;       // punto azul que dibuja el origen en el plano
    private Calendar ahoraSimulado;
    private MaterialButton btnCambiarUbicacion;
    private boolean rutaPendienteGps = false;      // estamos esperando permiso/ajustes de GPS para seguir con la ruta
    private AlertDialog dialogoBuscandoGps;
    private int intentosGps = 0;
    private static final double RADIO_ETSIIT_METROS = 80;   // a menos de esto del punto de la ETSIIT = "dentro"
    private static final float PRECISION_MAXIMA_METROS = 100; // peor precisión que esto = no nos fiamos del GPS
    private static final int MAX_INTENTOS_GPS = 16;          // x 500 ms = 8 s
    private final Runnable esperarFixGps = new Runnable() {
        @Override
        public void run() {
            if (isFinishing()) return;
            Location fix = (myLocationOverlay != null) ? myLocationOverlay.getLastFix() : null;
            if (fix != null) {
                cerrarDialogoBuscando();
                decidirPorGps(fix);
            } else if (++intentosGps >= MAX_INTENTOS_GPS) {
                cerrarDialogoBuscando();
                Toast.makeText(Mapa.this, "No hemos podido obtener tu posición", Toast.LENGTH_SHORT).show();
                mostrarMenuManual();
            } else {
                handler.postDelayed(this, 500);
            }
        }
    };

    // --- Aviso "activa la ubicación del móvil" ---
    private final Handler handler = new Handler(Looper.getMainLooper());

    // --- "¿A dónde vas?" (InicioMapa): destino del viaje y espera del GPS ---
    private DestinoViaje viaje;
    private boolean sinPedirUbicacion = false; // "En otro sitio": ya eligió a mano, el panel no pide la ubicación
    private Runnable llegadaPendiente;         // "Has llegado" → seguir dentro (se cancela si cierra la guía)
    // hora de la app: la simulada si se ha cambiado, si no la real
    private final java.util.function.Supplier<Calendar> reloj =
            () -> ahoraSimulado != null ? (Calendar) ahoraSimulado.clone() : Calendar.getInstance();
    private boolean viajePendientePermiso = false;
    private static final int ESPERA_GPS_VIAJE_MS = 5000;
    private static final int PASO_GPS_VIAJE_MS = 500;
    private boolean dialogoUbicacionVisible = false;
    private boolean esperandoAjustesUbicacion = false;
    private static final long ESPERA_PRIMERA_POSICION_MS = 12000;
    private final Runnable comprobarPrimeraPosicion = () -> {
        if (isFinishing() || !tienePermisoUbicacion()) return;
        if (myLocationOverlay == null || myLocationOverlay.getMyLocation() == null) {
            mostrarDialogoActivarUbicacion("Todavía no detectamos tu ubicación.\n\n"
                    + "¿Quieres revisar los ajustes de ubicación del móvil?");
        }
    };

    private final ActivityResultLauncher<ScanOptions> escanerQR =
            registerForActivityResult(new ScanContract(), result -> {
                if (result.getContents() == null) {
                    Toast.makeText(this, "Escaneo cancelado", Toast.LENGTH_SHORT).show();
                } else {
                    procesarQR(result.getContents());
                }
            });

    private final ActivityResultLauncher<String[]> locationPermissionRequest =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                Boolean fineGranted = result.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false);
                Boolean coarseGranted = result.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false);

                if (Boolean.TRUE.equals(fineGranted) || Boolean.TRUE.equals(coarseGranted)) {
                    boolean conViaje = viajePendientePermiso;
                    viajePendientePermiso = false;
                    activarUbicacionUsuario(!conViaje);
                    if (conViaje) esperarPosicion(viaje, 0);
                    if (rutaPendienteGps) {
                        rutaPendienteGps = false;
                        ubicarConGps();
                    }
                } else {
                    centrarEnETSIIT();
                    if (viajePendientePermiso) {
                        viajePendientePermiso = false;
                        preguntarDondeEstas(viaje);
                    }
                    if (rutaPendienteGps) {
                        rutaPendienteGps = false;
                        mostrarMenuManual();
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        configurarUserAgent();

        setContentView(R.layout.mapa);

        aplicarMargenesSistema();

        bindViews();

        View btnVolver = findViewById(R.id.btnVolver);
        if (btnVolver != null) {
            btnVolver.setOnClickListener(v -> {
                v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
                finish();
            });
        }

        if (ivIcono != null) {
            ivIcono.setOnClickListener(v -> {
                v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
                String correo = obtenerCorreoUsuarioActual();
                if (correo == null || correo.isEmpty()) {
                    Intent intent = new Intent(Mapa.this, Login.class);
                    startActivity(intent);
                } else {
                    Intent intent = new Intent(Mapa.this, AjustesCuenta.class);
                    intent.putExtra("CORREO", correo);
                    startActivity(intent);
                }
            });
        }

        View cardMascotaAyudante = findViewById(R.id.cardMascotaAyudante);
        if (cardMascotaAyudante != null) {
            cardMascotaAyudante.setOnClickListener(v -> {
                v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
                new AlertDialog.Builder(this)
                        .setIcon(R.drawable.ic_mascota_ayudante)
                        .setTitle("Asistente Virtual ETSIIT 🤖")
                        .setMessage("¡Hola! En esta sección puedes consultar el mapa GPS del campus y los planos interactivos de cada planta de la ETSIIT, buscar aulas y calcular rutas.")
                        .setPositiveButton("¡Entendido!", null)
                        .show();
            });
        }

        configurarMapaOSM();
        configurarSelectorModo();
        configurarSelectorPlantas();
        configurarTouchListenerPlano();
        configurarBuscador();
        configurarPanelRuta();

        // con ruta pedida desde otra sección o recuperada tras girar ya pregunta iniciarRuta
        boolean rutaRecuperada = savedInstanceState != null && savedInstanceState.containsKey(ESTADO_RUTA_DESTINO);
        // Al entrar sin encargo: "¿A dónde vas?" (el permiso de ubicación se pide desde ahí, al elegir)
        boolean preguntarDestino = savedInstanceState == null && getIntent().getStringExtra("RUTA_A_MARCADOR") == null
                && !getIntent().getBooleanExtra("AUTO_RUTA_COMEDOR", false) && InicioMapa.tocaPreguntar(this);
        if (preguntarDestino) {
            if (tienePermisoUbicacion()) activarUbicacionUsuario(false);
        } else if (getIntent().getStringExtra("RUTA_A_MARCADOR") == null && !rutaRecuperada) {
            preguntaPermisoUbicacion();
        }

        containerPlanoConMarcadores = findViewById(R.id.containerPlanoConMarcadores);
        // Inicializar los marcadores de la aplicación
        registrarMarcadores();
        if (savedInstanceState == null) {
            comprobarAutoRutaComedor();
            comprobarRutaAMarcador();
        } else if (rutaRecuperada) {
            recuperarRuta(savedInstanceState);
        }
        if (fabADondeVas != null) fabADondeVas.setOnClickListener(v -> {
            v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
            mostrarInicio();
        });
        if (preguntarDestino) mostrarInicio();
    }

    // al girar el móvil la Activity se recrea: se guarda la ruta en curso
    @Override
    protected void onSaveInstanceState(@NonNull Bundle salida) {
        super.onSaveInstanceState(salida);
        if (destinoRuta == null) return;
        salida.putString(ESTADO_RUTA_DESTINO, destinoRuta.getTitulo());
        if (origenActual != null && marcadorOrigen != null) {
            salida.putString(ESTADO_ORIGEN_ID, origenActual.idPunto);
            salida.putString(ESTADO_ORIGEN_NOMBRE, origenActual.nombre);
            salida.putString(ESTADO_ORIGEN_PLANTA, origenActual.planta);
            salida.putString(ESTADO_ORIGEN_METODO, origenActual.metodo.name());
            salida.putFloat(ESTADO_ORIGEN_X, marcadorOrigen.getXOriginal());
            salida.putFloat(ESTADO_ORIGEN_Y, marcadorOrigen.getYOriginal());
        }
    }

    private void recuperarRuta(Bundle estado) {
        Marcador destino = null;
        for (Marcador m : listaMarcadores) {
            if (m.getTitulo().equals(estado.getString(ESTADO_RUTA_DESTINO))) destino = m;
        }
        if (destino == null) return;

        if (toggleGroupModo != null) {
            toggleGroupModo.check(R.id.btnModoPlanos);
        }
        if (!estado.containsKey(ESTADO_ORIGEN_METODO)) {
            iniciarRuta(destino); // aún no se sabía dónde estaba: se vuelve a preguntar
            return;
        }
        destinoRuta = destino;
        fijarOrigen(UbicacionUsuario.enPixeles(
                estado.getString(ESTADO_ORIGEN_ID), estado.getString(ESTADO_ORIGEN_NOMBRE),
                estado.getString(ESTADO_ORIGEN_PLANTA), estado.getFloat(ESTADO_ORIGEN_X), estado.getFloat(ESTADO_ORIGEN_Y),
                UbicacionUsuario.Metodo.valueOf(estado.getString(ESTADO_ORIGEN_METODO))), false);
    }

    // Abrir con ruta desde otra sección: extra RUTA_A_MARCADOR = parte del título del marcador ("Biblioteca")
    private void comprobarRutaAMarcador() {
        String buscado = getIntent().getStringExtra("RUTA_A_MARCADOR");
        if (buscado == null) return;

        if (toggleGroupModo != null) {
            toggleGroupModo.check(R.id.btnModoPlanos);
        }
        for (Marcador m : listaMarcadores) {
            if (m.getTitulo().contains(buscado)) {
                iniciarRuta(m);
                return;
            }
        }
    }

    // Llegada con la guía exterior: si el viaje ya decía a qué sitio de dentro vas, se sigue solo hasta allí
    private void alLlegarConGuia(String idLugar, java.time.LocalDateTime hora) {
        DestinoViaje destino = viajeHacia(idLugar);
        viaje = null;
        Marcador m = destino != null && destino.interior != null ? marcadorQueContiene(destino.interior) : null;
        if (m == null) {
            alLlegarAEdificio(idLugar, hora, true);
            return;
        }
        guiaRutaExterior.decir("Has llegado. Sigue hasta " + destino.interior);
        LlegadaEdificio.seguirHasta(this, "la ETSIIT", m.getTitulo(),
                () -> rutaInteriorDesdeEntrada(destino.interior));
    }

    /** El viaje en curso si va a ese edificio (para la hora límite y el destino de dentro); si no, null. */
    private DestinoViaje viajeHacia(String idLugar) {
        return viaje != null && viaje.idEdificio.equals(idLugar) ? viaje : null;
    }

    // En la ETSIIT o el Auxiliar sin destino de dentro: "¿a dónde vas?" y enlace con la ruta interior
    private void alLlegarAEdificio(String idLugar, java.time.LocalDateTime hora, boolean llegando) {
        Calendar ahora = java.util.GregorianCalendar.from(hora.atZone(java.time.ZoneId.systemDefault()));
        LlegadaEdificio.mostrar(this, idLugar, ProximaClase.de(this, SesionUtil.correo(getIntent()), ahora),
                ahora, titulosMarcadores(), llegando, new LlegadaEdificio.Acciones() {
                    @Override public void rutaInterior(String buscado) { rutaInteriorDesdeEntrada(buscado); }

                    @Override public void rutaExterior(String idDesde, String idHasta) {
                        guiaRutaExterior.terminar();
                        panelRutaExterior.abrirEntre(idDesde, idHasta);
                        PlanRuta plan = panelRutaExterior.planActual();
                        if (plan != null && plan.tipo != PlanRuta.Tipo.LEJOS) {
                            guiaRutaExterior.empezar(plan, viajeHacia(idHasta));
                        }
                    }
                });
    }

    // Tocar un edificio del mapa exterior: "¿Te llevo?" (TarjetaLugar)
    private void alTocarLugar(String idLugar) {
        if (guiaRutaExterior != null && guiaRutaExterior.activa()) return; // no cambiar de destino en plena guía
        java.time.LocalDateTime hora = horaActual();
        GeoPoint gps = myLocationOverlay != null ? myLocationOverlay.getMyLocation() : null;
        TarjetaLugar.mostrar(this, findViewById(R.id.flContenedorMapa), redTransporte, idLugar,
                gps != null ? new Punto(gps.getLatitude(), gps.getLongitude()) : null, hora,
                new TarjetaLugar.Acciones() {
                    @Override public void llevame(String id) {
                        viaje = null; // destino nuevo: sin sitio de dentro ni hora
                        panelRutaExterior.abrirHacia(id);
                    }

                    @Override public void verPorDentro() {
                        if (toggleGroupModo != null) toggleGroupModo.check(R.id.btnModoPlanos);
                    }

                    @Override public void dentro(String id) { alLlegarAEdificio(id, hora, false); }
                });
    }

    // =====================================================================
    //  "¿A dónde vas?" al entrar: elegir destino y llevarte (exterior o directo a la ruta interior)
    // =====================================================================

    private void mostrarInicio() {
        if (guiaRutaExterior != null && guiaRutaExterior.activa()) return;
        Calendar ahora = reloj.get();
        InicioMapa.mostrar(this, ProximaClase.de(this, SesionUtil.correo(getIntent()), ahora), ahora,
                titulosMarcadores(), tienePermisoUbicacion(), new InicioMapa.Acciones() {
                    @Override public void ir(DestinoViaje destino) { viajar(destino); }

                    // como antes de "¿A dónde vas?": se ofrece la ubicación al quedarse en el mapa
                    @Override public void soloMapa() {
                        if (!tienePermisoUbicacion()) preguntaPermisoUbicacion();
                    }
                });
    }

    private void viajar(DestinoViaje destino) {
        viaje = destino;
        TarjetaLugar.cerrarEn(findViewById(R.id.flContenedorMapa));
        if (!tienePermisoUbicacion()) {
            new AlertDialog.Builder(this)
                    .setTitle("📍 ¿Te busco en el mapa?")
                    .setMessage("Para llevarte a " + destino.etiqueta + " necesito saber dónde estás. "
                            + "Solo se usa mientras tienes el mapa abierto.")
                    .setPositiveButton("Usar mi ubicación", (d, w) -> {
                        viajePendientePermiso = true;
                        solicitarPermisosUbicacion();
                    })
                    .setNegativeButton("Elegir a mano", (d, w) -> preguntarDondeEstas(destino))
                    .setCancelable(false)
                    .show();
            return;
        }
        activarUbicacionUsuario(false);
        esperarPosicion(destino, 0);
    }

    /** Espera la posición hasta ESPERA_GPS_VIAJE_MS; sin GPS (o desactivado) se pregunta dónde estás. */
    private void esperarPosicion(DestinoViaje destino, int esperado) {
        if (isFinishing() || destino != viaje) return;
        GeoPoint gps = myLocationOverlay != null ? myLocationOverlay.getMyLocation() : null;
        if (gps != null) {
            seguirViaje(destino, new Punto(gps.getLatitude(), gps.getLongitude()));
            return;
        }
        LocationManager lm = (LocationManager) getSystemService(LOCATION_SERVICE);
        if (esperado >= ESPERA_GPS_VIAJE_MS || lm == null || !lm.isLocationEnabled()) {
            handler.removeCallbacks(comprobarPrimeraPosicion);
            preguntarDondeEstas(destino);
            return;
        }
        if (esperado == 0) Toast.makeText(this, "📍 Buscando dónde estás…", Toast.LENGTH_SHORT).show();
        handler.postDelayed(() -> esperarPosicion(destino, esperado + PASO_GPS_VIAJE_MS), PASO_GPS_VIAJE_MS);
    }

    /** Dentro del edificio de destino → ruta interior directa; fuera → plan exterior hacia él. */
    private void seguirViaje(DestinoViaje destino, Punto posicion) {
        String dentroDe = null;
        if (redTransporte != null) {
            for (String id : new String[]{ProximaClase.ETSIIT, ProximaClase.AUXILIAR}) {
                RedTransporte.Lugar lugar = redTransporte.lugar(id);
                if (lugar != null && TarjetaLugar.estasEn(lugar, posicion)) dentroDe = id;
            }
        }
        if (destino.idEdificio.equals(dentroDe) || panelRutaExterior == null) {
            Marcador m = destino.interior != null ? marcadorQueContiene(destino.interior) : null;
            if (m != null) {
                if (toggleGroupModo != null) toggleGroupModo.check(R.id.btnModoPlanos);
                iniciarRuta(m); // horario → GPS (dentro: QR)
            } else {
                alLlegarAEdificio(destino.idEdificio, horaActual(), false);
            }
        } else if (dentroDe != null) {
            panelRutaExterior.abrirEntre(dentroDe, destino.idEdificio);
        } else {
            panelRutaExterior.abrirHacia(destino.idEdificio);
        }
    }

    private void preguntarDondeEstas(DestinoViaje destino) {
        String residencia = null;
        if (redTransporte != null) {
            for (RedTransporte.Lugar l : redTransporte.lugares) {
                if (l.ejemplo) residencia = l.id;
            }
        }
        List<String> textos = new ArrayList<>();
        List<Runnable> opciones = new ArrayList<>();
        if (residencia != null && panelRutaExterior != null) {
            String desde = residencia;
            textos.add("🏠 En la residencia");
            opciones.add(() -> panelRutaExterior.abrirEntre(desde, destino.idEdificio));
        }
        textos.add("🎓 Ya estoy en la ETSIIT (escanear QR)");
        opciones.add(() -> {
            Marcador m = destino.interior != null ? marcadorQueContiene(destino.interior) : null;
            if (m != null) {
                if (toggleGroupModo != null) toggleGroupModo.check(R.id.btnModoPlanos);
                destinoRuta = m;
                pedirEscaneoQR(false);
            } else if (ProximaClase.AUXILIAR.equals(destino.idEdificio) && panelRutaExterior != null) {
                panelRutaExterior.abrirEntre(ProximaClase.ETSIIT, ProximaClase.AUXILIAR);
            } else {
                alLlegarAEdificio(ProximaClase.ETSIIT, horaActual(), false);
            }
        });
        if (panelRutaExterior != null) {
            textos.add("📍 En otro sitio (mantén pulsado en el mapa)");
            opciones.add(() -> {
                sinPedirUbicacion = true;
                panelRutaExterior.abrirHacia(destino.idEdificio);
                sinPedirUbicacion = false;
                Toast.makeText(this, "Mantén pulsado en el mapa el sitio donde estás", Toast.LENGTH_LONG).show();
            });
        }
        new AlertDialog.Builder(this)
                .setTitle("No te encuentro · ¿dónde estás?")
                .setItems(textos.toArray(new String[0]), (d, i) -> opciones.get(i).run())
                .setNegativeButton("Cancelar", (d, w) -> viaje = null)
                .setOnCancelListener(d -> viaje = null) // tocar fuera o atrás también lo descarta
                .show();
    }

    /** Marcador cuyo título lleva ese sitio entero ("Aula 1.1" no casa con "Aula 1.10"). */
    private Marcador marcadorQueContiene(String buscado) {
        for (Marcador m : listaMarcadores) {
            String titulo = m.getTitulo();
            int i = titulo.indexOf(buscado);
            int fin = i + buscado.length();
            if (i >= 0 && (fin == titulo.length()
                    || !(Character.isDigit(titulo.charAt(fin)) || titulo.charAt(fin) == '.'))) {
                return m;
            }
        }
        return null;
    }

    private List<String> titulosMarcadores() {
        List<String> titulos = new ArrayList<>();
        for (Marcador m : listaMarcadores) titulos.add(m.getTitulo());
        return titulos;
    }

    private java.time.LocalDateTime horaActual() {
        return java.time.LocalDateTime.ofInstant(reloj.get().toInstant(), java.time.ZoneId.systemDefault());
    }

    private void rutaInteriorDesdeEntrada(String buscado) {
        Marcador m = marcadorQueContiene(buscado);
        if (m == null) return;
        if (panelRutaExterior != null) panelRutaExterior.cerrar(); // también termina la guía
        if (toggleGroupModo != null) toggleGroupModo.check(R.id.btnModoPlanos);
        destinoRuta = m;
        fijarOrigen(RegistroPuntosQR.entrada());
    }

    private void comprobarAutoRutaComedor() {
        if (!getIntent().getBooleanExtra("AUTO_RUTA_COMEDOR", false)) return;

        if (toggleGroupModo != null) {
            toggleGroupModo.check(R.id.btnModoPlanos);
        }

        Marcador destino = null;
        for (Marcador m : listaMarcadores) {
            if (m.getTitulo().toLowerCase(Locale.ROOT).contains("comedor")) {
                destino = m;
                break;
            }
        }
        if (destino != null) {
            destinoRuta = destino;
            UbicacionUsuario u = UbicacionUsuario.normalizada(
                    "aula_0_7", "Aula 0.7", "planta0", 0.60f, 0.45f, UbicacionUsuario.Metodo.HORARIO);
            fijarOrigen(u);
            cargarPlanoDrawable("planta0");
            Toast.makeText(this, "🍽️ Ruta contextual activa: Aula 0.7 ➔ Comedor", Toast.LENGTH_LONG).show();
        }
    }

    private void registrarMarcadores() {
        listaMarcadores.clear();

        listaMarcadores.add(new Marcador("☕ Comedor", "sotano", 2300f, 2050f));
        listaMarcadores.add(new Marcador("🚪 Secretaría", "planta0", 3000f, 2050f));
        listaMarcadores.add(new Marcador("📚 Biblioteca", "planta1", 2200f, 1400f));
        listaMarcadores.add(new Marcador("💻 Aula 2.1", "planta2", 2850f, 950f));
        listaMarcadores.add(new Marcador("💻 Aula 0.4", "planta0", 4450f, 2250f));
        listaMarcadores.add(new Marcador("💻 Aula 0.5", "planta0", 4650f, 2450f));
        listaMarcadores.add(new Marcador("💻 Aula 1.4", "planta1", 3300f, 1450f));

        // Crear dinámicamente las vistas ImageView para cada marcador
        crearVistasMarcadores();

        // Actualizar el adaptador del buscador tras registrar los marcadores
        if (etBuscadorMapa != null) {
            etBuscadorMapa.setAdapter(new MarcadorAdapter(this, listaMarcadores));
        }
    }

    private void crearVistasMarcadores() {
        if (containerPlanoConMarcadores == null) return;

        int sizePx = (int) (16 * getResources().getDisplayMetrics().density);

        for (Marcador m : listaMarcadores) {
            View puntoInteres = new View(this);

            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(sizePx, sizePx);
            puntoInteres.setLayoutParams(params);

            // Estilo del marcador circular
            GradientDrawable indicador = new GradientDrawable();
            indicador.setShape(GradientDrawable.OVAL);
            indicador.setColor(Color.parseColor("#FF8C42"));
            indicador.setStroke((int) (2 * getResources().getDisplayMetrics().density), Color.WHITE);
            puntoInteres.setBackground(indicador);

            puntoInteres.setVisibility(View.GONE);

            // EVENTO DE CLIC: Muestra la burbuja flotante pequeña
            puntoInteres.setOnClickListener(v -> mostrarBurbujaInfo(v, m));

            m.setVistaIcono(puntoInteres);
            containerPlanoConMarcadores.addView(puntoInteres);
        }
    }

    private void actualizarPosicionMarcadores() {
        if (ivPlanoEdificio == null) return;

        for (Marcador m : listaMarcadores) {
            posicionarMarcador(m);
        }
        if (marcadorOrigen != null) {
            posicionarMarcador(marcadorOrigen);
        }
    }

    private void posicionarMarcador(Marcador m) {
        View icono = m.getVistaIcono();
        if (icono == null) return;

        if (m.getPlanta().equalsIgnoreCase(plantaActual)) {
            icono.setVisibility(View.VISIBLE);

            float[] pts = {m.getXOriginal(), m.getYOriginal()};
            matrix.mapPoints(pts);

            // Centrar el círculo justo sobre la coordenada X e Y de la imagen
            float ancho = icono.getWidth() > 0 ? icono.getWidth() : icono.getLayoutParams().width;
            float alto = icono.getHeight() > 0 ? icono.getHeight() : icono.getLayoutParams().height;
            icono.setX(pts[0] - (ancho / 2f));
            icono.setY(pts[1] - (alto / 2f));
        } else {
            icono.setVisibility(View.GONE);
        }
    }

    private void mostrarBurbujaInfo(View vistaIcono, Marcador marcador) {
        float d = getResources().getDisplayMetrics().density;
        vistaIcono.performHapticFeedback(android.view.HapticFeedbackConstants.CONTEXT_CLICK);

        LinearLayout tarjeta = new LinearLayout(this);
        tarjeta.setOrientation(LinearLayout.VERTICAL);
        tarjeta.setPadding((int) (12 * d), (int) (8 * d), (int) (12 * d), (int) (8 * d));

        GradientDrawable fondo = new GradientDrawable();
        fondo.setColor(Color.WHITE);
        fondo.setCornerRadius(16f * d / 2f);
        fondo.setStroke((int) Math.max(1, d), Color.parseColor("#FF8C42"));
        tarjeta.setBackground(fondo);
        tarjeta.setElevation(8f);

        TextView tvMensaje = new TextView(this);
        tvMensaje.setText("¿Te llevo a\n" + marcador.getTitulo() + "?");
        tvMensaje.setTextColor(Color.BLACK);
        tvMensaje.setTextSize(13f);
        tarjeta.addView(tvMensaje);

        MaterialButton btnIniciar = new MaterialButton(this);
        btnIniciar.setText("Llévame");
        btnIniciar.setTextSize(12f);
        btnIniciar.setAllCaps(false);
        btnIniciar.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#FF8C42")));
        btnIniciar.setTextColor(Color.WHITE);
        LinearLayout.LayoutParams lpBtn = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lpBtn.topMargin = (int) (6 * d);
        tarjeta.addView(btnIniciar, lpBtn);

        PopupWindow popupWindow = new PopupWindow(
                tarjeta,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                true
        );
        popupWindow.setOutsideTouchable(true);
        popupWindow.setFocusable(true);

        btnIniciar.setOnClickListener(v -> {
            popupWindow.dismiss();
            iniciarRuta(marcador);
        });

        int offsetX = 20;
        int offsetY = -vistaIcono.getHeight() - 10;
        popupWindow.showAsDropDown(vistaIcono, offsetX, offsetY);
    }

    private void bindViews() {
        ivIcono = findViewById(R.id.ivIcono);
        mapView = findViewById(R.id.mapView);
        ivPlanoEdificio = findViewById(R.id.ivPlanoEdificio);
        fabMiUbicacion = findViewById(R.id.fabMiUbicacion);
        fabADondeVas = findViewById(R.id.fabADondeVas);
        fabComoLlegar = findViewById(R.id.fabComoLlegar);
        etBuscadorMapa = findViewById(R.id.etBuscadorMapa);
        cardBuscadorMapa = findViewById(R.id.cardBuscadorMapa);
        scrollPlantas = findViewById(R.id.scrollPlantas);
        chipGroupPlantas = findViewById(R.id.chipGroupPlantas);
        toggleGroupModo = findViewById(R.id.toggleGroupModo);
        tvTituloMapa = findViewById(R.id.tvTituloMapa);
        cardRutaActiva = findViewById(R.id.cardRutaActiva);
        tvRutaInfo = findViewById(R.id.tvRutaInfo);
        btnAbrirVisor = findViewById(R.id.btnAbrirVisor);
        btnCancelarRuta = findViewById(R.id.btnCancelarRuta);
        btnCambiarUbicacion = findViewById(R.id.btnCambiarUbicacion);
    }

    private void configurarUserAgent() {
        String userAgent = "Mozilla/5.0 (Android; Mobile; rv:120.0) Gecko/120.0 Firefox/120.0 NuiETSIITApp/1.0";
        Configuration.getInstance().setUserAgentValue(userAgent);
        Configuration.getInstance().load(getApplicationContext(), getSharedPreferences("osmdroid", MODE_PRIVATE));
        Configuration.getInstance().setUserAgentValue(userAgent);
    }

    private void configurarSelectorModo() {
        if (toggleGroupModo == null) return;

        toggleGroupModo.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) return;
            TarjetaLugar.cerrarEn(findViewById(R.id.flContenedorMapa));

            if (checkedId == R.id.btnModoMapa) {
                // Modo Mapa GPS
                if (mapView != null) {
                    mapView.setVisibility(View.VISIBLE);
                    mapView.getZoomController().setVisibility(CustomZoomButtonsController.Visibility.ALWAYS);
                }
                if (fabMiUbicacion != null) fabMiUbicacion.setVisibility(View.VISIBLE);
                if (fabComoLlegar != null) fabComoLlegar.setVisibility(View.VISIBLE);
                if (fabADondeVas != null) fabADondeVas.setVisibility(View.VISIBLE);

                if (containerPlanoConMarcadores != null) containerPlanoConMarcadores.setVisibility(View.GONE);
                if (cardBuscadorMapa != null) cardBuscadorMapa.setVisibility(View.GONE);
                if (scrollPlantas != null) scrollPlantas.setVisibility(View.GONE);

            } else if (checkedId == R.id.btnModoPlanos) {
                // Modo Planos ETSIIT
                if (mapView != null) {
                    mapView.setVisibility(View.GONE);
                    mapView.getZoomController().setVisibility(CustomZoomButtonsController.Visibility.NEVER);
                }
                if (fabMiUbicacion != null) fabMiUbicacion.setVisibility(View.GONE);
                if (fabComoLlegar != null) fabComoLlegar.setVisibility(View.GONE);
                if (fabADondeVas != null) fabADondeVas.setVisibility(View.GONE);
                if (panelRutaExterior != null) panelRutaExterior.cerrar();

                if (containerPlanoConMarcadores != null) containerPlanoConMarcadores.setVisibility(View.VISIBLE);
                if (cardBuscadorMapa != null) cardBuscadorMapa.setVisibility(View.VISIBLE);
                if (scrollPlantas != null) scrollPlantas.setVisibility(View.VISIBLE);

                cargarPlanoDrawable("planta0");
            }
            actualizarPanelRuta();
        });
    }

    private void configurarSelectorPlantas() {
        if (chipGroupPlantas == null) return;

        chipGroupPlantas.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) return;
            int checkedId = checkedIds.get(0);

            if (checkedId == R.id.chipSotano) {
                cargarPlanoDrawable("sotano");
            } else if (checkedId == R.id.chipBaja) {
                cargarPlanoDrawable("planta0");
            } else if (checkedId == R.id.chipPrimera) {
                cargarPlanoDrawable("planta1");
            } else if (checkedId == R.id.chipSegunda) {
                cargarPlanoDrawable("planta2");
            } else if (checkedId == R.id.chipTercera) {
                cargarPlanoDrawable("planta3");
            } else if (checkedId == R.id.chipCuarta) {
                cargarPlanoDrawable("planta4");
            } else if (checkedId == R.id.chipQuinta) {
                cargarPlanoDrawable("planta5");
            }
        });
    }

    private String nombrePlantaLegible(String id) {
        switch (id) {
            case "sotano": return "Sótano";
            case "planta0": return "Planta Baja";
            case "planta1": return "Planta 1";
            case "planta2": return "Planta 2";
            case "planta3": return "Planta 3";
            case "planta4": return "Planta 4";
            case "planta5": return "Planta 5";
            default: return id;
        }
    }

    private void cargarPlanoDrawable(String nombreSinExtension) {
        this.plantaActual = nombreSinExtension;
        RecienteUtil.guardar(this, Mapa.class, "Plano: " + nombrePlantaLegible(nombreSinExtension), "Mapa ETSIIT", "PLANTA", nombreSinExtension, 1);

        int resId = getResources().getIdentifier(nombreSinExtension, "drawable", getPackageName());

        if (resId != 0 && ivPlanoEdificio != null) {
            ivPlanoEdificio.setImageResource(resId);

            Bitmap bitmap = BitmapFactory.decodeResource(getResources(), resId);
            if (bitmap != null) {
                ivPlanoEdificio.post(() -> resetearMatrizImagen(bitmap));
            }
        } else {
            Toast.makeText(this, "No se encontró el plano: " + nombreSinExtension, Toast.LENGTH_SHORT).show();
        }

        actualizarPosicionMarcadores();
    }

    @SuppressLint("ClickableViewAccessibility")
    private void configurarTouchListenerPlano() {
        if (ivPlanoEdificio == null) return;

        ivPlanoEdificio.setOnTouchListener((v, event) -> {
            ImageView view = (ImageView) v;
            switch (event.getAction() & MotionEvent.ACTION_MASK) {
                case MotionEvent.ACTION_DOWN:
                    savedMatrix.set(matrix);
                    start.set(event.getX(), event.getY());
                    mode = DRAG;
                    break;

                case MotionEvent.ACTION_POINTER_DOWN:
                    oldDist = calcularDistancia(event);
                    if (oldDist > 10f) {
                        savedMatrix.set(matrix);
                        calcularPuntoMedio(mid, event);
                        mode = ZOOM;
                    }
                    break;

                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_POINTER_UP:
                    mode = NONE;
                    break;

                case MotionEvent.ACTION_MOVE:
                    if (mode == DRAG) {
                        matrix.set(savedMatrix);
                        matrix.postTranslate(event.getX() - start.x, event.getY() - start.y);
                    } else if (mode == ZOOM) {
                        float newDist = calcularDistancia(event);
                        if (newDist > 10f) {
                            matrix.set(savedMatrix);
                            float scale = newDist / oldDist;
                            matrix.postScale(scale, scale, mid.x, mid.y);
                        }
                    }
                    break;
            }

            constrainMatrix(view);
            view.setImageMatrix(matrix);
            actualizarPosicionMarcadores();

            return true;
        });
    }

    private void constrainMatrix(ImageView view) {
        if (view.getDrawable() == null) return;

        float[] values = new float[9];
        matrix.getValues(values);

        float currentScaleX = values[Matrix.MSCALE_X];

        int viewWidth = view.getWidth();
        int viewHeight = view.getHeight();
        int drawableWidth = view.getDrawable().getIntrinsicWidth();
        int drawableHeight = view.getDrawable().getIntrinsicHeight();

        if (viewWidth == 0 || viewHeight == 0 || drawableWidth == 0 || drawableHeight == 0) return;

        float minScale = Math.min((float) viewWidth / drawableWidth, (float) viewHeight / drawableHeight);

        if (currentScaleX < minScale) {
            float scaleFactor = minScale / currentScaleX;
            matrix.postScale(scaleFactor, scaleFactor, viewWidth / 2f, viewHeight / 2f);
        }
    }

    private float calcularDistancia(MotionEvent event) {
        float x = event.getX(0) - event.getX(1);
        float y = event.getY(0) - event.getY(1);
        return (float) Math.sqrt(x * x + y * y);
    }

    private void calcularPuntoMedio(PointF point, MotionEvent event) {
        float x = event.getX(0) + event.getX(1);
        float y = event.getY(0) + event.getY(1);
        point.set(x / 2, y / 2);
    }

    private void resetearMatrizImagen(Bitmap bitmap) {
        if (ivPlanoEdificio == null || bitmap == null) return;
        int viewWidth = ivPlanoEdificio.getWidth();
        int viewHeight = ivPlanoEdificio.getHeight();
        if (viewWidth == 0 || viewHeight == 0) return;

        int imgWidth = bitmap.getWidth();
        int imgHeight = bitmap.getHeight();

        float scale = Math.min((float) viewWidth / imgWidth, (float) viewHeight / imgHeight);
        float dx = (viewWidth - imgWidth * scale) / 2f;
        float dy = (viewHeight - imgHeight * scale) / 2f;

        matrix.reset();
        matrix.postScale(scale, scale);
        matrix.postTranslate(dx, dy);
        ivPlanoEdificio.setImageMatrix(matrix);

        actualizarPosicionMarcadores();
    }

    private void configurarBuscador() {
        if (etBuscadorMapa != null) {
            MarcadorAdapter adapter = new MarcadorAdapter(this, listaMarcadores);
            etBuscadorMapa.setAdapter(adapter);
            etBuscadorMapa.setThreshold(1);

            etBuscadorMapa.setOnItemClickListener((parent, view, position, id) -> {
                Marcador m = (Marcador) parent.getItemAtPosition(position);
                if (m != null) {
                    etBuscadorMapa.setText(m.getTitulo());
                    etBuscadorMapa.dismissDropDown();
                    procesarBusquedaPlano(m.getTitulo().toLowerCase(Locale.ROOT));
                }
            });

            etBuscadorMapa.setOnEditorActionListener((v, actionId, event) -> {
                if (actionId == EditorInfo.IME_ACTION_SEARCH || actionId == EditorInfo.IME_ACTION_DONE) {
                    String query = etBuscadorMapa.getText().toString().trim().toLowerCase(Locale.ROOT);
                    if (!query.isEmpty()) {
                        procesarBusquedaPlano(query);
                    }
                    etBuscadorMapa.dismissDropDown();
                    return true;
                }
                return false;
            });
        }
    }

    private void procesarBusquedaPlano(String query) {
        if (chipGroupPlantas == null) return;

        Marcador encontrado = null;
        String qNorm = normalizarTexto(query);
        for (Marcador m : listaMarcadores) {
            if (normalizarTexto(m.getTitulo()).contains(qNorm)) {
                encontrado = m;
                break;
            }
        }

        if (encontrado != null) {
            String planta = encontrado.getPlanta();
            if (planta.equals("sotano")) {
                chipGroupPlantas.check(R.id.chipSotano);
            } else if (planta.equals("planta1")) {
                chipGroupPlantas.check(R.id.chipPrimera);
            } else if (planta.equals("planta2")) {
                chipGroupPlantas.check(R.id.chipSegunda);
            } else if (planta.equals("planta3")) {
                chipGroupPlantas.check(R.id.chipTercera);
            } else if (planta.equals("planta4")) {
                chipGroupPlantas.check(R.id.chipCuarta);
            } else if (planta.equals("planta5")) {
                chipGroupPlantas.check(R.id.chipQuinta);
            } else {
                chipGroupPlantas.check(R.id.chipBaja);
            }
            resaltarMarcador(encontrado);
            Toast.makeText(this, "🎯 Punto encontrado: " + encontrado.getTitulo(), Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "⚠️ Solo se permite buscar entre los puntos registrados del mapa", Toast.LENGTH_LONG).show();
        }
    }

    private void resaltarMarcador(Marcador m) {
        View vista = m.getVistaIcono();
        if (vista != null) {
            vista.setVisibility(View.VISIBLE);
            vista.animate().scaleX(2.0f).scaleY(2.0f).setDuration(300).withEndAction(() -> {
                vista.animate().scaleX(1f).scaleY(1f).setDuration(300).start();
            }).start();
            mostrarBurbujaInfo(vista, m);
        }
    }

    private void configurarMapaOSM() {
        if (mapView == null) return;

        mapView.setTileSource(OPENMAP_TILES);
        mapView.setMultiTouchControls(true);

        // Configuración de los botones de zoom (+ y -):
        // Posicionados en horizontal hacia la derecha, de modo que el botón '+' queda pegado
        // a la izquierda del botón de ubicación y el botón '-' cabe holgadamente entre 'Cómo llegar' y '+'.
        mapView.getZoomController().setVisibility(CustomZoomButtonsController.Visibility.ALWAYS);
        CustomZoomButtonsDisplay zoomDisplay = mapView.getZoomController().getDisplay();
        zoomDisplay.setPositions(
                true,
                CustomZoomButtonsDisplay.HorizontalPosition.RIGHT,
                CustomZoomButtonsDisplay.VerticalPosition.BOTTOM
        );
        float density = getResources().getDisplayMetrics().density;
        zoomDisplay.setMarginPadding(0f, 0.25f);
        // Margen derecho: ancho fabMiUbicacion (48dp) + margen (14dp) + separación (8dp) = 70dp
        zoomDisplay.setAdditionalPixelMargins(0f, 0f, 70f * density, 20f * density);

        mapView.getController().setZoom(18.0);
        mapView.getController().setCenter(etsiitPoint);

        // Contornos de la ETSIIT y del Auxiliar, residencia y paradas del bus (rutas exteriores)
        redTransporte = CapaLugares.cargarRed(this);
        if (redTransporte != null) {
            CapaLugares.instalar(mapView, redTransporte, this::alTocarLugar);
            panelRutaExterior = new PanelRutaExterior(this, findViewById(R.id.flContenedorMapa), mapView,
                    redTransporte, SesionUtil.correo(getIntent()), reloj,
                    () -> myLocationOverlay != null ? myLocationOverlay.getMyLocation() : null,
                    () -> {
                        if (sinPedirUbicacion) return;
                        if (tienePermisoUbicacion()) activarUbicacionUsuario();
                        else preguntaPermisoUbicacion();
                    });
            panelRutaExterior.ocultarAlAbrir(fabComoLlegar, fabMiUbicacion, fabADondeVas);
            guiaRutaExterior = new GuiaRutaExterior(this, findViewById(R.id.flContenedorMapa), mapView,
                    panelRutaExterior, reloj);
            guiaRutaExterior.conPlanificador(new PlanificadorRuta(redTransporte));
            panelRutaExterior.alEmpezar(plan -> guiaRutaExterior.empezar(plan, viajeHacia(plan.destino.id)));
            // Al llegar se deja ver "Has llegado" un momento y se sigue al sitio de dentro (o se pregunta)
            // (si mientras tanto cierra la guía, ya no se sigue)
            guiaRutaExterior.alLlegar((idLugar, hora) -> {
                handler.removeCallbacks(llegadaPendiente);
                llegadaPendiente = () -> {
                    if (!isFinishing() && guiaRutaExterior.activa()) alLlegarConGuia(idLugar, hora);
                };
                handler.postDelayed(llegadaPendiente, 1500);
            });
        } else {
            Marker etsiitMarker = new Marker(mapView);
            etsiitMarker.setPosition(etsiitPoint);
            etsiitMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
            etsiitMarker.setTitle("ETSIIT - UGR");
            etsiitMarker.setSnippet("Escuela Técnica Superior de Ingenierías Informática y de Telecomunicación");
            mapView.getOverlays().add(etsiitMarker);
        }

        if (fabMiUbicacion != null) {
            fabMiUbicacion.setOnClickListener(v -> {
                if (tienePermisoUbicacion()) {
                    if (myLocationOverlay != null && myLocationOverlay.getMyLocation() != null) {
                        myLocationOverlay.enableFollowLocation();
                        mapView.getController().animateTo(myLocationOverlay.getMyLocation());
                        mapView.getController().setZoom(17.0);
                    } else {
                        activarUbicacionUsuario();
                    }
                } else {
                    preguntaPermisoUbicacion();
                }
            });
        }

        if (fabComoLlegar != null) {
            fabComoLlegar.setOnClickListener(v -> {
                if (panelRutaExterior != null) {
                    panelRutaExterior.abrir();
                    return;
                }
                if (!tienePermisoUbicacion()) {
                    preguntaPermisoUbicacion();
                    return;
                }
                GeoPoint userLocation = (myLocationOverlay != null) ? myLocationOverlay.getMyLocation() : null;
                if (userLocation == null) {
                    activarUbicacionUsuario();
                    Toast.makeText(this, "Obteniendo ubicación actual para la ruta...", Toast.LENGTH_SHORT).show();
                    return;
                }
                calcularYMostrarRuta(userLocation, etsiitPoint);
            });
        }
    }

    private void calcularYMostrarRuta(GeoPoint origen, GeoPoint destino) {
        Toast.makeText(this, "Calculando ruta hacia la ETSIIT...", Toast.LENGTH_SHORT).show();

        Executors.newSingleThreadExecutor().execute(() -> {
            List<GeoPoint> puntosRuta = new ArrayList<>();
            double distanciaMetros = 0;
            double duracionSegundosCoche = 0;
            boolean exitoOSRM = false;

            try {
                String urlStr = String.format(Locale.US,
                        "https://router.project-osrm.org/route/v1/driving/%.6f,%.6f;%.6f,%.6f?overview=full&geometries=geojson",
                        origen.getLongitude(), origen.getLatitude(),
                        destino.getLongitude(), destino.getLatitude());

                URL url = new URL(urlStr);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);

                if (conn.getResponseCode() == 200) {
                    BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) {
                        sb.append(line);
                    }
                    br.close();

                    JSONObject json = new JSONObject(sb.toString());
                    JSONArray routes = json.getJSONArray("routes");
                    if (routes.length() > 0) {
                        JSONObject route = routes.getJSONObject(0);
                        distanciaMetros = route.getDouble("distance");
                        duracionSegundosCoche = route.getDouble("duration");

                        JSONObject geometry = route.getJSONObject("geometry");
                        JSONArray coordinates = geometry.getJSONArray("coordinates");
                        for (int i = 0; i < coordinates.length(); i++) {
                            JSONArray point = coordinates.getJSONArray(i);
                            puntosRuta.add(new GeoPoint(point.getDouble(1), point.getDouble(0)));
                        }
                        exitoOSRM = true;
                    }
                }
                conn.disconnect();
            } catch (Exception e) {
                e.printStackTrace();
            }

            if (!exitoOSRM || puntosRuta.isEmpty()) {
                puntosRuta.add(origen);
                puntosRuta.add(destino);
                distanciaMetros = origen.distanceToAsDouble(destino);
                duracionSegundosCoche = (distanciaMetros / 1000.0) / 30.0 * 3600.0;
            }

            final List<GeoPoint> rutaFinal = puntosRuta;
            final double distFinal = distanciaMetros;
            final double durFinal = duracionSegundosCoche;

            runOnUiThread(() -> {
                if (mapView == null) return;

                if (rutaPolyline != null) {
                    mapView.getOverlays().remove(rutaPolyline);
                }

                rutaPolyline = new Polyline(mapView);
                rutaPolyline.setPoints(rutaFinal);
                rutaPolyline.getOutlinePaint().setColor(Color.parseColor("#FF8C42"));
                rutaPolyline.getOutlinePaint().setStrokeWidth(12f);
                mapView.getOverlays().add(0, rutaPolyline);

                if (myLocationOverlay != null) {
                    if (!mapView.getOverlays().contains(myLocationOverlay)) {
                        mapView.getOverlays().add(myLocationOverlay);
                    }
                    myLocationOverlay.disableFollowLocation();
                    myLocationOverlay.enableMyLocation();
                }

                mapView.invalidate();

                if (rutaFinal.size() >= 2) {
                    BoundingBox boundingBox = BoundingBox.fromGeoPoints(rutaFinal);
                    mapView.zoomToBoundingBox(boundingBox, true, 120);
                }

                mostrarDialogoInfoRuta(distFinal, durFinal);
            });
        });
    }

    private void mostrarDialogoInfoRuta(double distanciaMetros, double duracionCocheSeg) {
        String distanciaTexto = (distanciaMetros >= 1000)
                ? String.format(Locale.getDefault(), "%.2f km", distanciaMetros / 1000.0)
                : String.format(Locale.getDefault(), "%d m", (int) distanciaMetros);

        int minsCoche = (int) Math.max(1, Math.ceil(duracionCocheSeg / 60.0));
        int minsAPie = (int) Math.max(1, Math.ceil(distanciaMetros / 75.0));

        String mensaje = "📍 Distancia: " + distanciaTexto + "\n\n"
                + "🚗 En coche / transporte: ~" + minsCoche + " min\n"
                + "🚶 A pie: ~" + minsAPie + " min";

        new AlertDialog.Builder(this)
                .setTitle("Ruta a la ETSIIT - UGR")
                .setMessage(mensaje)
                .setPositiveButton("Navegar en GPS / Maps", (dialog, which) -> abrirNavegacionExterna())
                .setNegativeButton("Cerrar", null)
                .show();
    }

    private void abrirNavegacionExterna() {
        Uri gmmIntentUri = Uri.parse("google.navigation:q=" + etsiitPoint.getLatitude() + "," + etsiitPoint.getLongitude());
        Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
        mapIntent.setPackage("com.google.android.apps.maps");

        if (mapIntent.resolveActivity(getPackageManager()) != null) {
            startActivity(mapIntent);
        } else {
            Intent genericIntent = new Intent(Intent.ACTION_VIEW,
                    Uri.parse("https://www.google.com/maps/dir/?api=1&destination=" + etsiitPoint.getLatitude() + "," + etsiitPoint.getLongitude()));
            startActivity(genericIntent);
        }
    }

    private void preguntaPermisoUbicacion() {
        if (tienePermisoUbicacion()) {
            activarUbicacionUsuario();
        } else {
            new AlertDialog.Builder(this)
                    .setTitle("Uso de Ubicación")
                    .setMessage("¿Deseas activar tu ubicación actual para mostrarla en el mapa?")
                    .setPositiveButton("Sí", (dialog, which) -> solicitarPermisosUbicacion())
                    .setNegativeButton("No", (dialog, which) -> centrarEnETSIIT())
                    .setCancelable(false)
                    .show();
        }
    }

    private boolean tienePermisoUbicacion() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private void solicitarPermisosUbicacion() {
        locationPermissionRequest.launch(new String[]{
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
        });
    }

    private void activarUbicacionUsuario() {
        activarUbicacionUsuario(true);
    }

    /** @param avisar si la ubicación del móvil está apagada o no llega posición, decirlo con un diálogo */
    private void activarUbicacionUsuario(boolean avisar) {
        if (mapView == null) return;

        if (myLocationOverlay == null) {
            myLocationOverlay = new MyLocationNewOverlay(new GpsMyLocationProvider(this), mapView);
            mapView.getOverlays().add(myLocationOverlay);
        }

        myLocationOverlay.enableMyLocation();
        myLocationOverlay.enableFollowLocation();

        myLocationOverlay.runOnFirstFix(() -> runOnUiThread(() -> {
            // Con "Cómo llegar" abierto el mapa ya está encuadrado en la ruta: no moverlo
            if (panelRutaExterior != null && panelRutaExterior.estaAbierto()) {
                myLocationOverlay.disableFollowLocation();
                return;
            }
            if (myLocationOverlay != null && myLocationOverlay.getMyLocation() != null) {
                mapView.getController().animateTo(myLocationOverlay.getMyLocation());
                mapView.getController().setZoom(17.0);
            }
        }));

        if (avisar) comprobarUbicacionDelMovil();
    }

    private void comprobarUbicacionDelMovil() {
        handler.removeCallbacks(comprobarPrimeraPosicion);

        LocationManager lm = (LocationManager) getSystemService(LOCATION_SERVICE);
        boolean activada = lm != null && lm.isLocationEnabled();
        if (!activada) {
            mostrarDialogoActivarUbicacion("No detectamos tu ubicación porque la ubicación del móvil está desactivada.\n\n"
                    + "¿Quieres activarla?");
            return;
        }
        handler.postDelayed(comprobarPrimeraPosicion, ESPERA_PRIMERA_POSICION_MS);
    }

    private void mostrarDialogoActivarUbicacion(String mensaje) {
        if (dialogoUbicacionVisible || isFinishing()) return;
        dialogoUbicacionVisible = true;

        new AlertDialog.Builder(this)
                .setTitle("Ubicación no detectada")
                .setMessage(mensaje)
                .setPositiveButton("Activar ubicación", (dialog, which) -> {
                    esperandoAjustesUbicacion = true;
                    startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                })
                .setNegativeButton("Ahora no", (dialog, which) -> centrarEnETSIIT())
                .setOnDismissListener(d -> dialogoUbicacionVisible = false)
                .show();
    }

    // =====================================================================
    //  RUTA: ubicar al usuario (horario -> entrada -> QR) y abrir el visor
    // =====================================================================

    private void configurarPanelRuta() {
        if (btnCancelarRuta != null) btnCancelarRuta.setOnClickListener(v -> cancelarRuta());
        if (btnAbrirVisor != null) btnAbrirVisor.setOnClickListener(v -> abrirVisorRuta());
        if (btnCambiarUbicacion != null) btnCambiarUbicacion.setOnClickListener(v -> mostrarMenuManual());

        if (tvTituloMapa != null && esDebug()) {
            tvTituloMapa.setOnLongClickListener(v -> {
                mostrarDialogoSimulacionHorario();
                return true;
            });
        }
    }

    private boolean esDebug() {
        return (getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0;
    }

    private void iniciarRuta(Marcador destino) {
        destinoRuta = destino;
        rutaPendienteGps = false;

        Calendar ahora = (ahoraSimulado != null) ? ahoraSimulado : Calendar.getInstance();
        LocalizadorUsuario.SesionActual sesion = LocalizadorUsuario.sesionEnCurso(
                this, SesionUtil.correo(getIntent()), ahora);
        UbicacionUsuario puertaClase = (sesion != null) ? puertaDeClase(sesion) : null;

        if (puertaClase != null) {
            Toast.makeText(this, "Según tu horario ahora tienes " + sesion.nombreAsignatura
                    + " en el " + sesion.aula, Toast.LENGTH_LONG).show();
            fijarOrigen(puertaClase);
        } else {
            ubicarConGps();
        }
    }

    private void ubicarConGps() {
        if (!tienePermisoUbicacion()) {
            new AlertDialog.Builder(this)
                    .setTitle("Necesitamos tu ubicación")
                    .setMessage("No tienes clase ahora mismo, así que intentaremos ubicarte con el GPS. "
                            + "¿Quieres permitir el acceso a tu ubicación?")
                    .setPositiveButton("Sí", (d, w) -> {
                        rutaPendienteGps = true;
                        solicitarPermisosUbicacion();
                    })
                    .setNegativeButton("No, elegir a mano", (d, w) -> mostrarMenuManual())
                    .setNeutralButton("Cancelar", (d, w) -> destinoRuta = null)
                    .show();
            return;
        }

        LocationManager lm = (LocationManager) getSystemService(LOCATION_SERVICE);
        if (lm == null || !lm.isLocationEnabled()) {
            new AlertDialog.Builder(this)
                    .setTitle("GPS desactivado")
                    .setMessage("Para ubicarte automáticamente necesitamos que actives la ubicación del móvil. "
                            + "¿Quieres activarla?")
                    .setPositiveButton("Activar ubicación", (d, w) -> {
                        rutaPendienteGps = true;
                        esperandoAjustesUbicacion = true;
                        startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                    })
                    .setNegativeButton("No, elegir a mano", (d, w) -> mostrarMenuManual())
                    .setNeutralButton("Cancelar", (d, w) -> destinoRuta = null)
                    .show();
            return;
        }

        activarUbicacionUsuario();
        Location fix = (myLocationOverlay != null) ? myLocationOverlay.getLastFix() : null;
        if (fix != null) {
            decidirPorGps(fix);
            return;
        }
        intentosGps = 0;
        dialogoBuscandoGps = new AlertDialog.Builder(this)
                .setTitle("Buscando tu posición")
                .setMessage("Un momento...")
                .setNegativeButton("Elegir a mano", (d, w) -> {
                    handler.removeCallbacks(esperarFixGps);
                    mostrarMenuManual();
                })
                .setCancelable(false)
                .show();
        handler.postDelayed(esperarFixGps, 500);
    }

    private void cerrarDialogoBuscando() {
        handler.removeCallbacks(esperarFixGps);
        if (dialogoBuscandoGps != null && dialogoBuscandoGps.isShowing()) dialogoBuscandoGps.dismiss();
        dialogoBuscandoGps = null;
    }

    private void decidirPorGps(Location fix) {
        if (fix.hasAccuracy() && fix.getAccuracy() > PRECISION_MAXIMA_METROS) {
            Toast.makeText(this, "El GPS no es lo bastante preciso ahora mismo", Toast.LENGTH_SHORT).show();
            mostrarMenuManual();
            return;
        }
        double metros = new GeoPoint(fix.getLatitude(), fix.getLongitude()).distanceToAsDouble(etsiitPoint);
        if (metros <= RADIO_ETSIIT_METROS) {
            pedirEscaneoQR(true);
        } else {
            pedirIrALaEntrada(metros);
        }
    }

    private void mostrarMenuManual() {
        final String[] opciones = {
                "📚 Estoy en la puerta de un aula",
                "🚪 Estoy en la entrada de la ETSIIT",
                "📷 Escanear un código QR"
        };
        new AlertDialog.Builder(this)
                .setTitle("¿Dónde estás?")
                .setItems(opciones, (d, which) -> {
                    if (which == 0) elegirAula();
                    else if (which == 1) fijarOrigen(RegistroPuntosQR.entrada());
                    else pedirEscaneoQR(false);
                })
                .setNegativeButton("Cancelar", (d, w) -> {
                    if (origenActual == null) destinoRuta = null;
                })
                .show();
    }

    private void elegirAula() {
        final List<Marcador> aulas = new ArrayList<>();
        for (Marcador m : listaMarcadores) {
            if (m.getTitulo().toLowerCase(Locale.ROOT).contains("aula")) {
                aulas.add(m);
            }
        }
        if (aulas.isEmpty()) {
            Toast.makeText(this, "No hay aulas registradas en el mapa", Toast.LENGTH_SHORT).show();
            mostrarMenuManual();
            return;
        }

        float density = getResources().getDisplayMetrics().density;
        LinearLayout layoutDialog = new LinearLayout(this);
        layoutDialog.setOrientation(LinearLayout.VERTICAL);
        layoutDialog.setPadding((int) (20 * density), (int) (16 * density), (int) (20 * density), (int) (8 * density));

        TextView tvInstruction = new TextView(this);
        tvInstruction.setText("Escribe el nombre o número del aula donde estás:");
        tvInstruction.setTextColor(Color.BLACK);
        tvInstruction.setTextSize(14f);
        layoutDialog.addView(tvInstruction);

        AutoCompleteTextView autoText = new AutoCompleteTextView(this);
        autoText.setHint("Ej: Aula 0.5, 1.4, 2.1...");
        autoText.setTextSize(14f);
        autoText.setThreshold(1);
        autoText.setPadding((int) (12 * density), (int) (12 * density), (int) (12 * density), (int) (12 * density));

        LinearLayout.LayoutParams lpAuto = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lpAuto.topMargin = (int) (10 * density);
        layoutDialog.addView(autoText, lpAuto);

        MarcadorAdapter adapter = new MarcadorAdapter(this, aulas);
        autoText.setAdapter(adapter);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("🔍 Buscar tu aula actual")
                .setView(layoutDialog)
                .setNegativeButton("Atrás", (d, w) -> mostrarMenuManual())
                .create();

        autoText.setOnItemClickListener((parent, view, position, id) -> {
            Marcador m = (Marcador) parent.getItemAtPosition(position);
            dialog.dismiss();
            fijarOrigen(UbicacionUsuario.enPixeles(
                    UbicacionUsuario.idDesdeNombre(m.getTitulo().replaceAll("^[^A-Za-z0-9.]+", "")),
                    "Puerta de " + m.getTitulo().replaceAll("^[^A-Za-z0-9.]+", ""),
                    m.getPlanta(), m.getXOriginal(), m.getYOriginal(),
                    UbicacionUsuario.Metodo.MANUAL));
        });

        dialog.show();
        autoText.post(autoText::showDropDown);
    }

    private UbicacionUsuario puertaDeClase(LocalizadorUsuario.SesionActual sesion) {
        if (sesion.aula == null) return null;
        for (Marcador m : listaMarcadores) {
            if (m.getTitulo().contains(sesion.aula)) {
                return UbicacionUsuario.enPixeles(
                        UbicacionUsuario.idDesdeNombre(sesion.aula),
                        "Puerta del " + sesion.aula,
                        m.getPlanta(), m.getXOriginal(), m.getYOriginal(),
                        UbicacionUsuario.Metodo.HORARIO);
            }
        }
        return null;
    }

    private void pedirIrALaEntrada(double metrosALaEtsiit) {
        String distancia = (metrosALaEtsiit >= 1000)
                ? String.format(Locale.getDefault(), "a %.1f km", metrosALaEtsiit / 1000.0)
                : String.format(Locale.getDefault(), "a unos %d m", (int) metrosALaEtsiit);
        new AlertDialog.Builder(this)
                .setTitle("Estás fuera de la ETSIIT")
                .setMessage("Según tu GPS estás " + distancia + " de la ETSIIT.\n\n"
                        + "Colócate en la entrada principal (la de la escalinata frontal) "
                        + "y pulsa «Ya estoy aquí» para empezar la ruta.")
                .setPositiveButton("Ya estoy aquí", (d, w) -> fijarOrigen(RegistroPuntosQR.entrada()))
                .setNegativeButton("Elegir a mano", (d, w) -> mostrarMenuManual())
                .show();
    }

    private void pedirEscaneoQR(boolean detectadoPorGps) {
        AlertDialog.Builder b = new AlertDialog.Builder(this)
                .setTitle("Escanea un código QR")
                .setMessage((detectadoPorGps ? "Parece que estás dentro de la ETSIIT, y el GPS no es fiable en interiores.\n\n" : "")
                        + "Busca el código QR de ubicación más cercano (están en los halls y pasillos) "
                        + "y escanéalo para saber dónde estás.")
                .setPositiveButton("Escanear QR", (d, w) -> lanzarEscaner())
                .setNegativeButton("Elegir a mano", (d, w) -> mostrarMenuManual());
        if (esDebug()) {
            b.setNeutralButton("Simular QR (pruebas)", (d, w) -> mostrarListaSimularQR());
        }
        b.show();
    }

    private void lanzarEscaner() {
        ScanOptions opciones = new ScanOptions();
        opciones.setDesiredBarcodeFormats(ScanOptions.QR_CODE);
        opciones.setPrompt("Apunta al código QR de ubicación más cercano");
        opciones.setBeepEnabled(false);
        opciones.setOrientationLocked(false);
        escanerQR.launch(opciones);
    }

    private void procesarQR(String contenido) {
        UbicacionUsuario u = RegistroPuntosQR.desdeContenido(contenido);
        if (u == null) {
            new AlertDialog.Builder(this)
                    .setTitle("QR no reconocido")
                    .setMessage("Ese código no es uno de los QR de ubicación de la ETSIIT.")
                    .setPositiveButton("Reintentar", (d, w) -> lanzarEscaner())
                    .setNegativeButton("Cancelar", (d, w) -> destinoRuta = null)
                    .show();
            return;
        }
        fijarOrigen(u);
    }

    private void mostrarListaSimularQR() {
        final List<UbicacionUsuario> puntos = RegistroPuntosQR.todosLosQR();
        String[] nombres = new String[puntos.size()];
        for (int i = 0; i < nombres.length; i++) {
            nombres[i] = puntos.get(i).nombre + "  [" + puntos.get(i).idPunto + "]";
        }
        new AlertDialog.Builder(this)
                .setTitle("Simular escaneo de QR")
                .setItems(nombres, (d, which) -> fijarOrigen(puntos.get(which)))
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void mostrarDialogoSimulacionHorario() {
        final String[] opciones = {
                "Usar la hora real",
                "Lunes 09:00  (NPI teoría)",
                "Lunes 11:00  (PL teoría)",
                "Miércoles 11:00  (NPI prácticas)",
                "Jueves 09:00  (PL prácticas)",
                "Sábado 12:00  (sin clase)"
        };
        final int[] dias = {0, Calendar.MONDAY, Calendar.MONDAY, Calendar.WEDNESDAY, Calendar.THURSDAY, Calendar.SATURDAY};
        final int[] horas = {0, 9, 11, 11, 9, 12};

        new AlertDialog.Builder(this)
                .setTitle("Simular día y hora (pruebas)")
                .setItems(opciones, (d, which) -> {
                    if (which == 0) {
                        ahoraSimulado = null;
                    } else {
                        Calendar c = Calendar.getInstance();
                        c.set(Calendar.DAY_OF_WEEK, dias[which]);
                        c.set(Calendar.HOUR_OF_DAY, horas[which]);
                        c.set(Calendar.MINUTE, 0);
                        ahoraSimulado = c;
                    }
                    Toast.makeText(this, "Horario: " + opciones[which], Toast.LENGTH_SHORT).show();
                })
                .show();
    }

    private String nombreDestino() {
        return destinoRuta != null ? destinoRuta.getTitulo() : "tu destino";
    }

    private void fijarOrigen(UbicacionUsuario u) {
        fijarOrigen(u, true);
    }

    // avisar = false al recuperar la ruta tras girar: no se ha ubicado a nadie de nuevo
    private void fijarOrigen(UbicacionUsuario u, boolean avisar) {
        origenActual = u;

        int chip = chipIdDePlanta(u.planta);
        if (chip != 0 && chipGroupPlantas != null && !u.planta.equalsIgnoreCase(plantaActual)) {
            chipGroupPlantas.check(chip);
        }

        int ancho = 1;
        int alto = 1;
        if (ivPlanoEdificio != null && ivPlanoEdificio.getDrawable() != null) {
            ancho = ivPlanoEdificio.getDrawable().getIntrinsicWidth();
            alto = ivPlanoEdificio.getDrawable().getIntrinsicHeight();
        }

        if (marcadorOrigen != null && marcadorOrigen.getVistaIcono() != null) {
            containerPlanoConMarcadores.removeView(marcadorOrigen.getVistaIcono());
        }
        marcadorOrigen = new Marcador("Tu posición", u.planta, u.xPx(ancho), u.yPx(alto));
        View punto = crearPuntoCircular(Color.parseColor("#1E88E5"), 18);
        punto.setVisibility(View.GONE);
        marcadorOrigen.setVistaIcono(punto);
        containerPlanoConMarcadores.addView(punto);

        actualizarPosicionMarcadores();
        actualizarPanelRuta();
        if (avisar) Toast.makeText(this, "Te hemos ubicado: " + u.nombre + " (" + u.descripcionMetodo() + ")",
                Toast.LENGTH_LONG).show();
    }

    private View crearPuntoCircular(int color, int dp) {
        float d = getResources().getDisplayMetrics().density;
        int px = (int) (dp * d);
        View v = new View(this);
        v.setLayoutParams(new FrameLayout.LayoutParams(px, px));
        GradientDrawable g = new GradientDrawable();
        g.setShape(GradientDrawable.OVAL);
        g.setColor(color);
        g.setStroke((int) (2 * d), Color.WHITE);
        v.setBackground(g);
        return v;
    }

    private int chipIdDePlanta(String planta) {
        switch (planta) {
            case "sotano":  return R.id.chipSotano;
            case "planta0": return R.id.chipBaja;
            case "planta1": return R.id.chipPrimera;
            case "planta2": return R.id.chipSegunda;
            case "planta3": return R.id.chipTercera;
            case "planta4": return R.id.chipCuarta;
            case "planta5": return R.id.chipQuinta;
            default:        return 0;
        }
    }

    private void actualizarPanelRuta() {
        if (cardRutaActiva == null) return;
        boolean enPlanos = containerPlanoConMarcadores != null
                && containerPlanoConMarcadores.getVisibility() == View.VISIBLE;
        if (origenActual == null || destinoRuta == null || !enPlanos) {
            cardRutaActiva.setVisibility(View.GONE);
            return;
        }
        tvRutaInfo.setText("📍 Estás en: " + origenActual.nombre + " (" + origenActual.descripcionMetodo() + ")\n"
                + "🎯 Destino: " + destinoRuta.getTitulo());
        cardRutaActiva.setVisibility(View.VISIBLE);
    }

    private void cancelarRuta() {
        if (marcadorOrigen != null && marcadorOrigen.getVistaIcono() != null && containerPlanoConMarcadores != null) {
            containerPlanoConMarcadores.removeView(marcadorOrigen.getVistaIcono());
        }
        marcadorOrigen = null;
        origenActual = null;
        destinoRuta = null;
        actualizarPanelRuta();
    }

    private void abrirVisorRuta() {
        if (origenActual == null || destinoRuta == null) return;
        String claveRuta = origenActual.idPunto;
        boolean esAula05Origen = (origenActual.idPunto != null && origenActual.idPunto.contains("aula_0_5"))
                || (origenActual.nombre != null && origenActual.nombre.contains("0.5"));
        boolean esAula14Destino = (destinoRuta.getTitulo() != null && destinoRuta.getTitulo().contains("1.4"));

        if (destinoRuta.getTitulo().contains("Biblioteca")) {
            claveRuta = "biblioteca";
        } else if (esAula05Origen && esAula14Destino) {
            claveRuta = "aula_0_5";
        } else if (origenActual.idPunto != null
                && (origenActual.idPunto.contains("aula_0_4") || origenActual.idPunto.contains("0_7") || origenActual.idPunto.contains("aula_0_7"))
                && destinoRuta != null && destinoRuta.getTitulo().toLowerCase(Locale.ROOT).contains("comedor")) {
            claveRuta = "comedor";
        } else if ("aula_0_5".equalsIgnoreCase(claveRuta)) {
            // esa clave es la ruta guiada a la 1.4: a otros destinos no debe mostrarla
            claveRuta = FotosRuta.CLAVE_DEMO;
        }
        Intent i = new Intent(this, VisorRuta.class);
        i.putExtra("ORIGEN_ID", claveRuta);
        i.putExtra("ORIGEN_NOMBRE", origenActual.nombre);
        i.putExtra("ORIGEN_METODO", origenActual.descripcionMetodo());
        i.putExtra("DESTINO_NOMBRE", destinoRuta.getTitulo());
        startActivity(i);
    }

    private void centrarEnETSIIT() {
        if (mapView != null) {
            mapView.getController().animateTo(etsiitPoint);
            mapView.getController().setZoom(18.0);
        }
    }

    /** Durante la guía exterior no se vuelve al menú al agitar (se anda o se va en bus con el móvil). */
    @Override
    protected boolean usaAgitar() {
        return guiaRutaExterior == null || !guiaRutaExterior.activa();
    }

    @Override
    protected void onResume() {
        super.onResume();
        configurarUserAgent();
        if (mapView != null) {
            mapView.onResume();
        }
        if (myLocationOverlay != null && tienePermisoUbicacion()) {
            myLocationOverlay.enableMyLocation();
        }
        if (esperandoAjustesUbicacion) {
            esperandoAjustesUbicacion = false;
            if (tienePermisoUbicacion()) activarUbicacionUsuario();
            if (rutaPendienteGps && destinoRuta != null) {
                rutaPendienteGps = false;
                ubicarConGps();
            } else {
                rutaPendienteGps = false;
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        handler.removeCallbacks(comprobarPrimeraPosicion);
        cerrarDialogoBuscando();
        Configuration.getInstance().save(getApplicationContext(), getSharedPreferences("osmdroid", MODE_PRIVATE));
        if (mapView != null) {
            mapView.onPause();
        }
        if (myLocationOverlay != null) {
            myLocationOverlay.disableMyLocation();
        }
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacksAndMessages(null); // esperas del GPS, llegada de la guía…
        super.onDestroy();
    }

    private String obtenerCorreoUsuarioActual() {
        if (getIntent() != null && getIntent().hasExtra("CORREO")) {
            String c = getIntent().getStringExtra("CORREO");
            if (c != null && !c.isEmpty()) return c;
        }
        SharedPreferences prefs = getSharedPreferences("SesionUsuario", MODE_PRIVATE);
        return prefs.getString("CORREO", null);
    }

    private void aplicarMargenesSistema() {
        View root = findViewById(R.id.rootMapa);
        if (root != null) {
            ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
                Insets barras = insets.getInsets(
                        WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
                v.setPadding(barras.left, barras.top, barras.right, barras.bottom);
                return insets;
            });
            ViewCompat.requestApplyInsets(root);
        }
    }

    // Adaptador personalizado para autocompletado con filtro normalizado (sin distinción de emojis/mayúsculas/tildes)
    public static class MarcadorAdapter extends ArrayAdapter<Marcador> {
        private final List<Marcador> originalList;
        private List<Marcador> filteredList;

        public MarcadorAdapter(Context context, List<Marcador> objects) {
            super(context, android.R.layout.simple_dropdown_item_1line, objects);
            this.originalList = new ArrayList<>(objects);
            this.filteredList = new ArrayList<>(objects);
        }

        @Override
        public int getCount() {
            return filteredList.size();
        }

        @Override
        public Marcador getItem(int position) {
            return filteredList.get(position);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = super.getView(position, convertView, parent);
            if (view instanceof TextView) {
                Marcador m = getItem(position);
                if (m != null) {
                    ((TextView) view).setText(m.getTituloLimpio());
                }
            }
            return view;
        }

        @Override
        public Filter getFilter() {
            return new Filter() {
                @Override
                protected FilterResults performFiltering(CharSequence constraint) {
                    FilterResults results = new FilterResults();
                    if (constraint == null || constraint.length() == 0) {
                        results.values = originalList;
                        results.count = originalList.size();
                    } else {
                        String query = normalizarTexto(constraint.toString());
                        List<Marcador> match = new ArrayList<>();
                        for (Marcador m : originalList) {
                            String tituloNorm = normalizarTexto(m.getTitulo());
                            if (tituloNorm.contains(query)) {
                                match.add(m);
                            }
                        }
                        results.values = match;
                        results.count = match.size();
                    }
                    return results;
                }

                @Override
                protected void publishResults(CharSequence constraint, FilterResults results) {
                    if (results.values != null) {
                        filteredList = (List<Marcador>) results.values;
                    } else {
                        filteredList = new ArrayList<>();
                    }
                    notifyDataSetChanged();
                }

                @Override
                public CharSequence convertResultToString(Object resultValue) {
                    if (resultValue instanceof Marcador) {
                        return ((Marcador) resultValue).getTituloLimpio();
                    }
                    return super.convertResultToString(resultValue);
                }
            };
        }
    }

    public static String normalizarTexto(String texto) {
        if (texto == null) return "";
        return Normalizer.normalize(texto, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .trim();
    }
}
