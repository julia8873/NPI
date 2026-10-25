package com.example.nuietsiit.activities.secciones.mapa;

import android.Manifest;
import android.annotation.SuppressLint;

import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.nuietsiit.R;
import com.example.nuietsiit.activities.BaseActivity;
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
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polyline;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;

public class Mapa extends BaseActivity {

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
    private ExtendedFloatingActionButton fabComoLlegar;
    private EditText etBuscadorMapa;
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
            android.location.Location fix = (myLocationOverlay != null) ? myLocationOverlay.getLastFix() : null;
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
    };        // solo pruebas (builds debug): fecha/hora falsa para el horario

    // --- Aviso "activa la ubicación del móvil" ---
    private final Handler handler = new Handler(Looper.getMainLooper());
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
                    activarUbicacionUsuario();
                    if (rutaPendienteGps) {
                        rutaPendienteGps = false;
                        ubicarConGps();
                    }
                } else {
                    centrarEnETSIIT();
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

        bindViews();

        if (ivIcono != null) {
            ivIcono.setOnClickListener(v -> finish());
        }

        configurarMapaOSM();
        configurarSelectorModo();
        configurarSelectorPlantas();
        configurarTouchListenerPlano();
        configurarBuscador();
        configurarPanelRuta();

        preguntaPermisoUbicacion();

        containerPlanoConMarcadores = findViewById(R.id.containerPlanoConMarcadores);
        // Inicializar los marcadores de la aplicación
        registrarMarcadores();
    }

    private void registrarMarcadores() {
        listaMarcadores.clear();

        listaMarcadores.add(new Marcador("☕ Comedor", "sotano", 2300f, 2050f));
        listaMarcadores.add(new Marcador("🚪 Secretaría", "planta0", 3000f, 2050f));
        listaMarcadores.add(new Marcador("📚 Biblioteca ETSIIT", "planta1", 2200f, 1400f));
        listaMarcadores.add(new Marcador("💻 Aula 2.1", "planta2", 2850f, 950f));
        listaMarcadores.add(new Marcador("💻 Aula 0.5", "planta0", 4850f, 2450f));

        // Crear dinámicamente las vistas ImageView para cada marcador
        crearVistasMarcadores();
    }

    private void crearVistasMarcadores() {
        if (containerPlanoConMarcadores == null) return;

        int sizePx = (int) (16 * getResources().getDisplayMetrics().density);

        for (Marcador m : listaMarcadores) {
            View puntoInteres = new View(this);

            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(sizePx, sizePx);
            puntoInteres.setLayoutParams(params);

            // Estilo del marcador circular
            android.graphics.drawable.GradientDrawable indicador = new android.graphics.drawable.GradientDrawable();
            indicador.setShape(android.graphics.drawable.GradientDrawable.OVAL);
            indicador.setColor(Color.parseColor("#FF8C42"));
            indicador.setStroke((int) (2 * getResources().getDisplayMetrics().density), Color.WHITE);
            puntoInteres.setBackground(indicador);

            puntoInteres.setVisibility(View.GONE);

            // EVENTO DE CLIC: Ahora muestra la burbuja flotante pequeña
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

        // Tarjeta pequeña: título + botón "Iniciar ruta"
        LinearLayout tarjeta = new LinearLayout(this);
        tarjeta.setOrientation(LinearLayout.VERTICAL);
        tarjeta.setPadding((int) (12 * d), (int) (8 * d), (int) (12 * d), (int) (8 * d));

        android.graphics.drawable.GradientDrawable fondo = new android.graphics.drawable.GradientDrawable();
        fondo.setColor(Color.WHITE);
        fondo.setCornerRadius(16f * d / 2f);
        fondo.setStroke((int) Math.max(1, d), Color.parseColor("#FF8C42"));
        tarjeta.setBackground(fondo);
        tarjeta.setElevation(8f);

        TextView tvMensaje = new TextView(this);
        tvMensaje.setText(marcador.getTitulo());
        tvMensaje.setTextColor(Color.BLACK);
        tvMensaje.setTextSize(13f);
        tarjeta.addView(tvMensaje);

        MaterialButton btnIniciar = new MaterialButton(this);
        btnIniciar.setText("Iniciar ruta");
        btnIniciar.setTextSize(12f);
        btnIniciar.setAllCaps(false);
        btnIniciar.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#FF8C42")));
        btnIniciar.setTextColor(Color.WHITE);
        LinearLayout.LayoutParams lpBtn = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lpBtn.topMargin = (int) (6 * d);
        tarjeta.addView(btnIniciar, lpBtn);

        PopupWindow popupWindow = new PopupWindow(
                tarjeta,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                true // Permite cerrarlo tocando fuera
        );
        popupWindow.setOutsideTouchable(true);
        popupWindow.setFocusable(true);

        btnIniciar.setOnClickListener(v -> {
            popupWindow.dismiss();
            iniciarRuta(marcador);
        });

        // Mostrar justo encima/al lado del icono del marcador
        int offsetX = 20;
        int offsetY = -vistaIcono.getHeight() - 10;
        popupWindow.showAsDropDown(vistaIcono, offsetX, offsetY);
    }

    private void bindViews() {
        ivIcono = findViewById(R.id.ivIcono);
        mapView = findViewById(R.id.mapView);
        ivPlanoEdificio = findViewById(R.id.ivPlanoEdificio);
        fabMiUbicacion = findViewById(R.id.fabMiUbicacion);
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

            if (checkedId == R.id.btnModoMapa) {
                // Modo Mapa GPS
                if (mapView != null) mapView.setVisibility(View.VISIBLE);
                if (fabMiUbicacion != null) fabMiUbicacion.setVisibility(View.VISIBLE);
                if (fabComoLlegar != null) fabComoLlegar.setVisibility(View.VISIBLE);

                // Ocultar el contenedor del plano (y sus marcadores)
                if (containerPlanoConMarcadores != null) containerPlanoConMarcadores.setVisibility(View.GONE);
                if (cardBuscadorMapa != null) cardBuscadorMapa.setVisibility(View.GONE);
                if (scrollPlantas != null) scrollPlantas.setVisibility(View.GONE);

            } else if (checkedId == R.id.btnModoPlanos) {
                // Modo Planos ETSIIT
                if (mapView != null) mapView.setVisibility(View.GONE);
                if (fabMiUbicacion != null) fabMiUbicacion.setVisibility(View.GONE);
                if (fabComoLlegar != null) fabComoLlegar.setVisibility(View.GONE);

                // MOSTRAR el contenedor del plano
                if (containerPlanoConMarcadores != null) containerPlanoConMarcadores.setVisibility(View.VISIBLE);
                if (cardBuscadorMapa != null) cardBuscadorMapa.setVisibility(View.VISIBLE);
                if (scrollPlantas != null) scrollPlantas.setVisibility(View.VISIBLE);

                // Cargar planta baja por defecto
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

    private void cargarPlanoDrawable(String nombreSinExtension) {
        this.plantaActual = nombreSinExtension; // Registrar qué planta se está viendo

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

            // Aplicar restricción para no hacer la imagen más pequeña que su visión completa
            constrainMatrix(view);

            view.setImageMatrix(matrix);

            // Actualizar la posición de los marcadores con la matriz recién modificada
            actualizarPosicionMarcadores();

            return true;
        });
    }

    /**
     * Asegura que la imagen no se reduzca más allá de su tamaño completo inicial.
     */
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

        // Escala mínima para mostrar la imagen completa dentro del cuadro (Fit Center)
        float minScale = Math.min((float) viewWidth / drawableWidth, (float) viewHeight / drawableHeight);

        // Si el usuario intenta reducir la imagen más allá del tamaño completo, la forzamos a mantenerse en minScale
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

        // Reposicionar los marcadores tras recalcular la matriz inicial
        actualizarPosicionMarcadores();
    }

    private void configurarBuscador() {
        if (etBuscadorMapa != null) {
            etBuscadorMapa.setOnEditorActionListener((v, actionId, event) -> {
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    String query = etBuscadorMapa.getText().toString().trim().toLowerCase(Locale.ROOT);
                    if (!query.isEmpty()) {
                        procesarBusquedaPlano(query);
                    }
                    return true;
                }
                return false;
            });
        }
    }

    private void procesarBusquedaPlano(String query) {
        if (chipGroupPlantas == null) return;

        if (query.contains("sotano") || query.contains("sótano") || query.contains("-1")) {
            chipGroupPlantas.check(R.id.chipSotano);
            Toast.makeText(this, "Mostrando Sótano para: " + query, Toast.LENGTH_SHORT).show();
        } else if (query.contains("1") || query.contains("primera")) {
            chipGroupPlantas.check(R.id.chipPrimera);
            Toast.makeText(this, "Mostrando Planta 1 para: " + query, Toast.LENGTH_SHORT).show();
        } else if (query.contains("2") || query.contains("segunda")) {
            chipGroupPlantas.check(R.id.chipSegunda);
            Toast.makeText(this, "Mostrando Planta 2 para: " + query, Toast.LENGTH_SHORT).show();
        } else if (query.contains("3") || query.contains("tercera")) {
            chipGroupPlantas.check(R.id.chipTercera);
            Toast.makeText(this, "Mostrando Planta 3 para: " + query, Toast.LENGTH_SHORT).show();
        } else if (query.contains("4") || query.contains("cuarta")) {
            chipGroupPlantas.check(R.id.chipCuarta);
            Toast.makeText(this, "Mostrando Planta 4 para: " + query, Toast.LENGTH_SHORT).show();
        } else if (query.contains("5") || query.contains("quinta")) {
            chipGroupPlantas.check(R.id.chipQuinta);
            Toast.makeText(this, "Mostrando Planta 5 para: " + query, Toast.LENGTH_SHORT).show();
        } else {
            chipGroupPlantas.check(R.id.chipBaja);
            Toast.makeText(this, "Buscando '" + query + "' en Planta Baja", Toast.LENGTH_SHORT).show();
        }
    }

    private void configurarMapaOSM() {
        if (mapView == null) return;

        mapView.setTileSource(OPENMAP_TILES);
        mapView.setMultiTouchControls(true);

        mapView.getController().setZoom(18.0);
        mapView.getController().setCenter(etsiitPoint);

        Marker etsiitMarker = new Marker(mapView);
        etsiitMarker.setPosition(etsiitPoint);
        etsiitMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        etsiitMarker.setTitle("ETSIIT - UGR");
        etsiitMarker.setSnippet("Escuela Técnica Superior de Ingenierías Informática y de Telecomunicación");
        mapView.getOverlays().add(etsiitMarker);

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
                    .setNegativeButton("No", (dialog, which) -> {
                        centrarEnETSIIT();
                    })
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
        if (mapView == null) return;

        if (myLocationOverlay == null) {
            myLocationOverlay = new MyLocationNewOverlay(new GpsMyLocationProvider(this), mapView);
            mapView.getOverlays().add(myLocationOverlay);
        }

        myLocationOverlay.enableMyLocation();
        myLocationOverlay.enableFollowLocation();

        myLocationOverlay.runOnFirstFix(() -> runOnUiThread(() -> {
            if (myLocationOverlay != null && myLocationOverlay.getMyLocation() != null) {
                mapView.getController().animateTo(myLocationOverlay.getMyLocation());
                mapView.getController().setZoom(17.0);
            }
        }));

        comprobarUbicacionDelMovil();
    }

    /**
     * Si el móvil tiene la ubicación apagada, o tras unos segundos seguimos sin posición,
     * preguntamos si quiere activarla (lleva a los ajustes de ubicación).
     */
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

        // Solo builds debug: mantener pulsado el título para simular día/hora del horario
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

    /**
     * Se llama al pulsar "Iniciar ruta". Orden de decisión:
     *  1) Horario: si ahora tiene clase con aula conocida -> está en esa puerta.
     *  2) GPS: fuera de la ETSIIT -> entrada; dentro -> escanear QR.
     *  3) Manual: el usuario elige.
     */
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

    // ---------------------------- 2) GPS ----------------------------

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

        // Permiso y ubicación activados: esperamos una posición (hasta 8 s)
        activarUbicacionUsuario();
        android.location.Location fix = (myLocationOverlay != null) ? myLocationOverlay.getLastFix() : null;
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

    /** Con una posición GPS decidimos si está dentro o fuera de la ETSIIT. */
    private void decidirPorGps(android.location.Location fix) {
        if (fix.hasAccuracy() && fix.getAccuracy() > PRECISION_MAXIMA_METROS) {
            Toast.makeText(this, "El GPS no es lo bastante preciso ahora mismo", Toast.LENGTH_SHORT).show();
            mostrarMenuManual();
            return;
        }
        double metros = new GeoPoint(fix.getLatitude(), fix.getLongitude()).distanceToAsDouble(etsiitPoint);
        if (metros <= RADIO_ETSIIT_METROS) {
            pedirEscaneoQR(true);   // dentro: el GPS no sirve en interior, usamos QR
        } else {
            pedirIrALaEntrada(metros);   // fuera: entrada
        }
    }

    // ---------------------------- 3) Manual ----------------------------

    /** El usuario elige cómo indicar dónde está. También sirve para corregir la ubicación detectada. */
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
        final java.util.List<Marcador> aulas = new ArrayList<>();
        for (Marcador m : listaMarcadores) {
            if (m.getTitulo().contains("Aula")) aulas.add(m);
        }
        if (aulas.isEmpty()) {
            Toast.makeText(this, "No hay aulas registradas en el mapa", Toast.LENGTH_SHORT).show();
            mostrarMenuManual();
            return;
        }
        String[] nombres = new String[aulas.size()];
        for (int i = 0; i < nombres.length; i++) nombres[i] = aulas.get(i).getTitulo();
        new AlertDialog.Builder(this)
                .setTitle("¿En qué aula estás?")
                .setItems(nombres, (d, which) -> {
                    Marcador m = aulas.get(which);
                    fijarOrigen(UbicacionUsuario.enPixeles(
                            UbicacionUsuario.idDesdeNombre(m.getTitulo().replaceAll("^[^A-Za-z]+", "")),
                            "Puerta de " + m.getTitulo().replaceAll("^[^A-Za-z]+", ""),
                            m.getPlanta(), m.getXOriginal(), m.getYOriginal(),
                            UbicacionUsuario.Metodo.MANUAL));
                })
                .setNegativeButton("Atrás", (d, w) -> mostrarMenuManual())
                .show();
    }

    /** Busca el punto naranja del aula de la clase y lo usa como "puerta de la clase". */
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

    /** Dentro del edificio -> escanear el QR más cercano. */
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

    /** Solo debug: elegir un QR de la lista sin tener que imprimirlo/escanearlo. */
    private void mostrarListaSimularQR() {
        final java.util.List<UbicacionUsuario> puntos = RegistroPuntosQR.todosLosQR();
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

    /** Solo debug: simular qué día/hora es para probar el método del horario. */
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

    /** El usuario ya está ubicado: lo dibujamos en el plano y mostramos el panel con el visor. */
    private void fijarOrigen(UbicacionUsuario u) {
        origenActual = u;

        // Ir a la planta donde está el usuario
        int chip = chipIdDePlanta(u.planta);
        if (chip != 0 && chipGroupPlantas != null && !u.planta.equalsIgnoreCase(plantaActual)) {
            chipGroupPlantas.check(chip); // el listener carga el plano
        }

        int ancho = 1;
        int alto = 1;
        if (ivPlanoEdificio != null && ivPlanoEdificio.getDrawable() != null) {
            ancho = ivPlanoEdificio.getDrawable().getIntrinsicWidth();
            alto = ivPlanoEdificio.getDrawable().getIntrinsicHeight();
        }

        // Punto azul de "tú estás aquí"
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
        Toast.makeText(this, "Te hemos ubicado: " + u.nombre + " (" + u.descripcionMetodo() + ")",
                Toast.LENGTH_LONG).show();
    }

    private View crearPuntoCircular(int color, int dp) {
        float d = getResources().getDisplayMetrics().density;
        int px = (int) (dp * d);
        View v = new View(this);
        v.setLayoutParams(new FrameLayout.LayoutParams(px, px));
        android.graphics.drawable.GradientDrawable g = new android.graphics.drawable.GradientDrawable();
        g.setShape(android.graphics.drawable.GradientDrawable.OVAL);
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
        Intent i = new Intent(this, VisorRuta.class);
        i.putExtra("ORIGEN_ID", origenActual.idPunto);
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
        if (esperandoAjustesUbicacion) { // volvemos de los ajustes de ubicación del móvil
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
}