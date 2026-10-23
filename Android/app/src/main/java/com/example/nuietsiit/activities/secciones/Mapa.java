package com.example.nuietsiit.activities.secciones;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.nuietsiit.R;
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
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;

public class Mapa extends AppCompatActivity {

    // Fuente de mapa pública basada en OpenStreetMap (100% gratuita y sin requerir API Key)
    public static final OnlineTileSourceBase OPENMAP_TILES = new XYTileSource(
            "OSM_DE",
            0, 19, 256, ".png",
            new String[]{
                    "https://tile.openstreetmap.de/"
            },
            "© OpenStreetMap contributors"
    );

    private MapView mapView;
    private ImageView ivIcono;
    private FloatingActionButton fabMiUbicacion;
    private ExtendedFloatingActionButton fabComoLlegar;
    private EditText etBuscadorMapa;
    private MyLocationNewOverlay myLocationOverlay;
    private Polyline rutaPolyline;

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

        // Configurar User-Agent válido de navegador antes de cargar la configuración
        configurarUserAgent();

        setContentView(R.layout.mapa);

        ivIcono = findViewById(R.id.ivIcono);
        mapView = findViewById(R.id.mapView);
        fabMiUbicacion = findViewById(R.id.fabMiUbicacion);
        fabComoLlegar = findViewById(R.id.fabComoLlegar);
        etBuscadorMapa = findViewById(R.id.etBuscadorMapa);

        if (ivIcono != null) {
            ivIcono.setOnClickListener(v -> finish());
        }

        configurarMapa();
        preguntaPermisoUbicacion();
    }

    private void configurarUserAgent() {
        String userAgent = "Mozilla/5.0 (Android; Mobile; rv:120.0) Gecko/120.0 Firefox/120.0 NuiETSIITApp/1.0";
        Configuration.getInstance().setUserAgentValue(userAgent);
        Configuration.getInstance().load(getApplicationContext(), getSharedPreferences("osmdroid", MODE_PRIVATE));
        Configuration.getInstance().setUserAgentValue(userAgent);
    }

    private void configurarMapa() {
        if (mapView == null) return;

        // Usar OpenStreetMap DE (público, gratuito y sin requerir API Key)
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

        if (etBuscadorMapa != null) {
            etBuscadorMapa.setOnEditorActionListener((v, actionId, event) -> {
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    String query = etBuscadorMapa.getText().toString().trim();
                    if (!query.isEmpty()) {
                        Toast.makeText(this, "Buscando: " + query + " en ETSIIT...", Toast.LENGTH_SHORT).show();
                        centrarEnETSIIT();
                    }
                    return true;
                }
                return false;
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

            // Si falla OSRM o no hay internet, trazamos línea recta como respaldo
            if (!exitoOSRM || puntosRuta.isEmpty()) {
                puntosRuta.add(origen);
                puntosRuta.add(destino);
                distanciaMetros = origen.distanceToAsDouble(destino);
                duracionSegundosCoche = (distanciaMetros / 1000.0) / 30.0 * 3600.0; // aprox. 30 km/h en ciudad
            }

            final List<GeoPoint> rutaFinal = puntosRuta;
            final double distFinal = distanciaMetros;
            final double durFinal = duracionSegundosCoche;

            runOnUiThread(() -> {
                if (mapView == null) return;

                // 1. Eliminar linea de ruta anterior si existía
                if (rutaPolyline != null) {
                    mapView.getOverlays().remove(rutaPolyline);
                }

                // 2. Crear y añadir la nueva línea de ruta al fondo (índice 0)
                rutaPolyline = new Polyline(mapView);
                rutaPolyline.setPoints(rutaFinal);
                rutaPolyline.getOutlinePaint().setColor(Color.parseColor("#FF8C42"));
                rutaPolyline.getOutlinePaint().setStrokeWidth(12f);
                mapView.getOverlays().add(0, rutaPolyline);

                // 3. Asegurar que el icono de ubicación ("monigote") siga activo y visible sobre la ruta
                if (myLocationOverlay != null) {
                    if (!mapView.getOverlays().contains(myLocationOverlay)) {
                        mapView.getOverlays().add(myLocationOverlay);
                    }
                    myLocationOverlay.disableFollowLocation(); // Evitar que fuerce la cámara al usuario
                    myLocationOverlay.enableMyLocation();     // Mantener visible el icono/monigote
                }

                mapView.invalidate();

                // 4. Centrar y encuadrar mapa para ver la ruta entera
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
        // Velocidad media a pie: 75 m/min (~4.5 km/h)
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
            // Si no tiene la app de Google Maps instalada, abrir navegador/intención genérica
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