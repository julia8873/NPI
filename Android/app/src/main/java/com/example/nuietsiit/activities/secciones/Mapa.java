package com.example.nuietsiit.activities.secciones;

import android.Manifest;
import android.annotation.SuppressLint;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.net.Uri;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.nuietsiit.R;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

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
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;

public class Mapa extends AppCompatActivity {

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

    private final ActivityResultLauncher<String[]> locationPermissionRequest =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                Boolean fineGranted = result.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false);
                Boolean coarseGranted = result.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false);

                if (Boolean.TRUE.equals(fineGranted) || Boolean.TRUE.equals(coarseGranted)) {
                    activarUbicacionUsuario();
                } else {
                    Toast.makeText(this, "Permiso de ubicación denegado. Se muestra el mapa de la ETSIIT.", Toast.LENGTH_LONG).show();
                    centrarEnETSIIT();
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

        preguntaPermisoUbicacion();
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

                if (ivPlanoEdificio != null) ivPlanoEdificio.setVisibility(View.GONE);
                if (cardBuscadorMapa != null) cardBuscadorMapa.setVisibility(View.GONE);
                if (scrollPlantas != null) scrollPlantas.setVisibility(View.GONE);

            } else if (checkedId == R.id.btnModoPlanos) {
                // Modo Planos ETSIIT
                if (mapView != null) mapView.setVisibility(View.GONE);
                if (fabMiUbicacion != null) fabMiUbicacion.setVisibility(View.GONE);
                if (fabComoLlegar != null) fabComoLlegar.setVisibility(View.GONE);

                if (ivPlanoEdificio != null) ivPlanoEdificio.setVisibility(View.VISIBLE);
                if (cardBuscadorMapa != null) cardBuscadorMapa.setVisibility(View.VISIBLE);
                if (scrollPlantas != null) scrollPlantas.setVisibility(View.VISIBLE);

                // Cargar planta baja por defecto si no hay ninguna
                cargarPlanoAsset("baja.jpg");
            }
        });
    }

    private void configurarSelectorPlantas() {
        if (chipGroupPlantas == null) return;

        chipGroupPlantas.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) return;
            int checkedId = checkedIds.get(0);

            if (checkedId == R.id.chipSotano) {
                cargarPlanoAsset("sotano.jpg");
            } else if (checkedId == R.id.chipBaja) {
                cargarPlanoAsset("baja.jpg");
            } else if (checkedId == R.id.chipPrimera) {
                cargarPlanoAsset("primera.jpg");
            } else if (checkedId == R.id.chipSegunda) {
                cargarPlanoAsset("segunda.jpg");
            } else if (checkedId == R.id.chipTercera) {
                cargarPlanoAsset("tercera.jpg");
            } else if (checkedId == R.id.chipCuarta) {
                cargarPlanoAsset("cuarta.jpg");
            } else if (checkedId == R.id.chipQuinta) {
                cargarPlanoAsset("quinta.jpg");
            }
        });
    }

    private void cargarPlanoAsset(String nombreArchivo) {
        try (InputStream is = getAssets().open("mapa_etsiit/ConSuelo/" + nombreArchivo)) {
            Bitmap bitmap = BitmapFactory.decodeStream(is);
            if (bitmap != null && ivPlanoEdificio != null) {
                ivPlanoEdificio.setImageBitmap(bitmap);
                ivPlanoEdificio.post(() -> resetearMatrizImagen(bitmap));
            }
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "No se pudo cargar el plano de la planta", Toast.LENGTH_SHORT).show();
        }
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
            view.setImageMatrix(matrix);
            return true;
        });
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
                        Toast.makeText(this, "Mostrando mapa de la ETSIIT.", Toast.LENGTH_SHORT).show();
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

        Toast.makeText(this, "Ubicación activada", Toast.LENGTH_SHORT).show();
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
    }

    @Override
    protected void onPause() {
        super.onPause();
        Configuration.getInstance().save(getApplicationContext(), getSharedPreferences("osmdroid", MODE_PRIVATE));
        if (mapView != null) {
            mapView.onPause();
        }
        if (myLocationOverlay != null) {
            myLocationOverlay.disableMyLocation();
        }
    }
}