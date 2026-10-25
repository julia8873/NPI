package com.example.nuietsiit.activities.secciones;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.view.GestureDetector;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.nuietsiit.R;
import com.example.nuietsiit.activities.BaseActivity;
import com.example.nuietsiit.activities.main.AjustesCuenta;
import com.example.nuietsiit.data.Comedores.ComedoresDB;
import com.example.nuietsiit.data.Comedores.Comedor;
import com.example.nuietsiit.data.Comedores.MenuInfo;
import com.example.nuietsiit.util.DetectorInclinacion;
import com.example.nuietsiit.util.DetectorInclinacionFrontal;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Actividad encargada de gestionar la sección de Comedores universitarios.
 * Permite calcular la distancia desde la ubicación actual del usuario, ordenar los comedores por cercanía,
 * consultar menús, horarios y precios desde SQLite para el día seleccionado, y navegar entre días
 * mediante botones o sensores de inclinación lateral (con restricción de no permitir fechas pasadas).
 */
public class Comedores extends BaseActivity {

    // Componentes de la interfaz de usuario
    private TextView tvMensajeComedorCercano;
    private TextView tvUbicacionActual;
    private TextView tvDia, tvPrimerPlato, tvSegundoPlato, tvPostre, tvHorario, tvPrecio;
    private ImageView ivIcono;
    private LinearLayout llListaComedores;

    // Servicios de datos, geolocalización y sensores
    private ComedoresDB comedoresDB;
    private LocationManager locationManager;
    private DetectorInclinacionFrontal detectorInclinacion; // Sensor para tumbar el móvil hacia adelante (actualizar GPS)
    private DetectorInclinacion detectorInclinacionDias;     // Sensor para inclinar a derecha/izquierda (cambiar de día)
    private Calendar calendarioSeleccionado;                 // Fecha actualmente seleccionada para el menú
    private List<Comedor> listaComedores;                    // Lista de comedores ordenados por distancia
    private Comedor comedorSeleccionado;                     // Comedor actualmente seleccionado en la tarjeta principal

    // Lanzador para solicitar permisos de ubicación en tiempo de ejecución
    private final ActivityResultLauncher<String[]> locationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                Boolean fineGranted = result.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false);
                Boolean coarseGranted = result.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false);

                if ((fineGranted != null && fineGranted) || (coarseGranted != null && coarseGranted)) {
                    obtenerUbicacionYProcesar();
                } else {
                    tvUbicacionActual.setText("📍 Permiso no concedido. (ETSIIT como referencia)");
                    // Ubicación por defecto de referencia (Campus ETSIIT)
                    aplicarUbicacionReferencia(37.197050, -3.624260, "Referencia (ETSIIT)");
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.comedores);

        // Inicializar base de datos SQLite, servicios de ubicación y fecha actual
        comedoresDB = new ComedoresDB(this);
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        calendarioSeleccionado = Calendar.getInstance();

        // Enlazar vistas del layout XML
        tvMensajeComedorCercano = findViewById(R.id.tvMensajeComedoreCercano);
        tvUbicacionActual = findViewById(R.id.tvUbicacionActual);
        tvDia = findViewById(R.id.tvDia);
        tvPrimerPlato = findViewById(R.id.tvPrimerPlato);
        tvSegundoPlato = findViewById(R.id.tvSegundoPlato);
        tvPostre = findViewById(R.id.tvPostre);
        tvHorario = findViewById(R.id.tvHorario);
        tvPrecio = findViewById(R.id.tvPrecio);
        ivIcono = findViewById(R.id.ivIcono);
        llListaComedores = findViewById(R.id.llListaComedores);

        // Botones de navegación entre días (dentro del contenedor de la tarjeta)
        Button btnAnterior = findViewById(R.id.btnDiaAnterior);
        Button btnSiguiente = findViewById(R.id.btnDiaSiguiente);

        btnAnterior.setOnClickListener(v -> {
            getWindow().getDecorView().performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
            retrocederDia();
        });

        btnSiguiente.setOnClickListener(v -> {
            getWindow().getDecorView().performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
            avanzarDia();
        });

        // Configurar navegación a la pantalla de ajustes de cuenta desde el icono de perfil
        ivIcono.setOnClickListener(v -> {
            Intent intent = new Intent(Comedores.this, AjustesCuenta.class);
            startActivity(intent);
        });

        // Detector de inclinación hacia adelante: recalcula la ubicación GPS al tumbar el móvil
        detectorInclinacion = new DetectorInclinacionFrontal(this, () -> {
            getWindow().getDecorView().performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
            Toast.makeText(Comedores.this, "📱 Movimiento detectado: actualizando ubicación...", Toast.LENGTH_SHORT).show();
            obtenerUbicacionYProcesar();
        });

        // Detector de inclinación lateral: derecha = día siguiente, izquierda = día anterior
        detectorInclinacionDias = new DetectorInclinacion(this, direccion -> {
            getWindow().getDecorView().performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
            if (direccion == DetectorInclinacion.DERECHA) {
                avanzarDia();
            } else if (direccion == DetectorInclinacion.IZQUIERDA) {
                retrocederDia();
            }
        });

        // Detector de gestos táctiles (swipe / deslizar) sobre la tarjeta del menú
        View cardMenu = findViewById(R.id.cardMenu);
        GestureDetector gestureDetectorMenu = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            private static final int SWIPE_THRESHOLD = 40;
            private static final int SWIPE_VELOCITY_THRESHOLD = 40;

            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                if (e1 == null || e2 == null) return false;
                float diffX = e2.getX() - e1.getX();
                float diffY = e2.getY() - e1.getY();
                if (Math.abs(diffX) > Math.abs(diffY)) {
                    if (Math.abs(diffX) > SWIPE_THRESHOLD && Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                        getWindow().getDecorView().performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
                        if (diffX > 0) {
                            retrocederDia(); // Deslizar hacia la derecha -> Día anterior
                        } else {
                            avanzarDia(); // Deslizar hacia la izquierda -> Día siguiente
                        }
                        return true;
                    }
                }
                return false;
            }
        });

        if (cardMenu != null) {
            cardMenu.setOnTouchListener((v, event) -> {
                boolean handled = gestureDetectorMenu.onTouchEvent(event);
                if (event.getAction() == MotionEvent.ACTION_UP && !handled) {
                    v.performClick();
                }
                return true;
            });
        }

        // Cargar lista de comedores desde la base de datos
        listaComedores = comedoresDB.obtenerListaComedores();

        // Verificar permisos de ubicación e iniciar geolocalización
        verificarPermisosYLocalizar();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (detectorInclinacion != null) {
            detectorInclinacion.iniciar();
        }
        if (detectorInclinacionDias != null) {
            detectorInclinacionDias.iniciar();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (detectorInclinacion != null) {
            detectorInclinacion.parar();
        }
        if (detectorInclinacionDias != null) {
            detectorInclinacionDias.parar();
        }
    }

    /**
     * Comprueba los permisos de ubicación (Fina/Aproximada) y procede a localizar.
     */
    private void verificarPermisosYLocalizar() {
        // fine es ubicación muy precisa y coarse ubicación aproximada
        // A partir de Android 12, se pregunta si quieres dar tu ubicación precisa o aproximada
        boolean fine = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
        boolean coarse = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;

        if (fine || coarse) {
            obtenerUbicacionYProcesar();
        } else {
            // Solicitar permisos al usuario
            locationPermissionLauncher.launch(new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            });
        }
    }

    /**
     * Obtiene la última ubicación conocida del dispositivo por GPS o red.
     */
    private void obtenerUbicacionYProcesar() {
        if (locationManager == null) return;

        Location ultimaUbicacion = null;
        try {
            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                ultimaUbicacion = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            }
            if (ultimaUbicacion == null && locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                ultimaUbicacion = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            }
            if (ultimaUbicacion == null && locationManager.isProviderEnabled(LocationManager.PASSIVE_PROVIDER)) {
                ultimaUbicacion = locationManager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER);
            }

            // Solicitar posición
            solicitarPosicionActual();
        } catch (SecurityException e) {
            e.printStackTrace();
        }

        if (ultimaUbicacion != null) {
            procesarUbicacionUsuario(ultimaUbicacion);
        } else {
            tvUbicacionActual.setText("📍 Esperando GPS... (Tumba el móvil hacia adelante para recalcular)");
            aplicarUbicacionReferencia(37.197050, -3.624260, "Referencia provisional (ETSIIT)");
        }
    }

    /**
     * Solicita una actualización de posición actual al proveedor disponible.
     */
    private void solicitarPosicionActual() {
        if (locationManager == null) return;

        try {
            String proveedor = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                    ? LocationManager.GPS_PROVIDER
                    : (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) ? LocationManager.NETWORK_PROVIDER : null);

            if (proveedor == null) return;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                locationManager.getCurrentLocation(proveedor, null, ContextCompat.getMainExecutor(this), location -> {
                    if (location != null) {
                        procesarUbicacionUsuario(location);
                    }
                });
            } else {
                locationManager.requestSingleUpdate(proveedor, new LocationListener() {
                    @Override
                    public void onLocationChanged(@NonNull Location location) {
                        procesarUbicacionUsuario(location);
                    }

                    @Override
                    public void onProviderEnabled(@NonNull String provider) {}

                    @Override
                    public void onProviderDisabled(@NonNull String provider) {}
                }, getMainLooper());
            }
        } catch (SecurityException e) {
            e.printStackTrace();
        }
    }

    /**
     * Calcula la distancia a cada comedor con Location.distanceBetween y ordena la lista de más cercano a más lejano.
     */
    private void procesarUbicacionUsuario(@NonNull Location location) {
        double userLat = location.getLatitude();
        double userLon = location.getLongitude();

        tvUbicacionActual.setText(String.format(Locale.getDefault(),
                "📍 Lat: %.4f, Lon: %.4f (Inclina hacia adelante para recalcular)", userLat, userLon));

        calcularDistanciasYOrdenar(userLat, userLon);
    }

    /**
     * En caso de no tener señal GPS inmediata o permisos denegados.
     */
    private void aplicarUbicacionReferencia(double refLat, double refLon, String descripcion) {
        tvUbicacionActual.setText("📍 " + descripcion);
        calcularDistanciasYOrdenar(refLat, refLon);
    }

    /**
     * Aplica Location.distanceBetween y ordena la lista de menor a mayor distancia.
     */
    private void calcularDistanciasYOrdenar(double origenLat, double origenLon) {
        if (listaComedores == null || listaComedores.isEmpty()) {
            listaComedores = comedoresDB.obtenerListaComedores();
        }

        float[] resultadoMetros = new float[1];
        for (Comedor c : listaComedores) {
            Location.distanceBetween(origenLat, origenLon, c.getLatitud(), c.getLongitud(), resultadoMetros);
            c.setDistanciaMetros(resultadoMetros[0]);
        }

        // Ordenar en orden ascendente (el más cercano primero)
        Collections.sort(listaComedores, Comparator.comparingDouble(Comedor::getDistanciaMetros));

        if (!listaComedores.isEmpty()) {
            // Seleccionar automáticamente el más cercano
            comedorSeleccionado = listaComedores.get(0);
            mostrarComedorPrincipal(comedorSeleccionado);
        }

        // Rellenar la lista inferior con todos los comedores ordenados
        mostrarListaComedoresOrdenados();
    }

    /**
     * Actualiza la tarjeta principal superior con los datos del comedor seleccionado y la fecha actual/seleccionada.
     * Consulta la base de datos para obtener el menú, horario y precio correspondientes.
     */
    private void mostrarComedorPrincipal(Comedor comedor) {
        boolean esElMasCercano = !listaComedores.isEmpty() && listaComedores.get(0).getIdComedor() == comedor.getIdComedor();
        if (esElMasCercano) {
            tvMensajeComedorCercano.setText("¡Tu comedor más cercano!\n" + comedor.getNombreComedor());
        } else {
            tvMensajeComedorCercano.setText("Comedor seleccionado:\n" + comedor.getNombreComedor());
        }

        if (calendarioSeleccionado == null) {
            calendarioSeleccionado = Calendar.getInstance();
        }

        // Mostrar la fecha formateada en la etiqueta del día (ej: "Lunes, 26 de Mayo")
        SimpleDateFormat sdf = new SimpleDateFormat("EEEE, d 'de' MMMM", Locale.forLanguageTag("es"));
        String diaActual = sdf.format(calendarioSeleccionado.getTime());
        if (!diaActual.isEmpty()) {
            diaActual = diaActual.substring(0, 1).toUpperCase() + diaActual.substring(1);
        }
        tvDia.setText(diaActual);

        SimpleDateFormat sdfDb = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String fechaStr = sdfDb.format(calendarioSeleccionado.getTime());

        // Consultar base de datos para obtener horario, precio y estado de apertura
        boolean esCerrado = comedoresDB.esDiaCerrado(comedor.getIdComedor(), calendarioSeleccionado);
        String horario = comedoresDB.obtenerHorarioParaFecha(comedor.getIdComedor(), calendarioSeleccionado);
        String precio = comedoresDB.obtenerPrecioEstudiante(comedor.getIdComedor());

        if (esCerrado) {
            // Si el comedor está cerrado o es festivo/fin de semana, NO mostrar platos
            tvPrimerPlato.setVisibility(View.VISIBLE);
            tvPrimerPlato.setText("🚫 Comedor cerrado este día\n(Sin servicio de menú)");
            tvPrimerPlato.setCompoundDrawablesRelative(null, null, null, null);

            tvSegundoPlato.setVisibility(View.GONE);
            tvPostre.setVisibility(View.GONE);

            tvHorario.setText(horario);
            tvPrecio.setText("Precio: --");
        } else {
            // Si está abierto, obtener menú y mostrar platos con sus respectivos iconos
            MenuInfo menuInfo = comedoresDB.obtenerMenuParaComedorYFecha(comedor.getIdComedor(), fechaStr);

            tvPrimerPlato.setVisibility(View.VISIBLE);
            tvSegundoPlato.setVisibility(View.VISIBLE);
            tvPostre.setVisibility(View.VISIBLE);

            tvPrimerPlato.setText("Primer plato: " + menuInfo.getPrimerPlato());
            tvSegundoPlato.setText("Segundo plato: " + menuInfo.getSegundoPlato());
            tvPostre.setText("Postre: " + menuInfo.getPostre());
            tvHorario.setText(horario);
            tvPrecio.setText(precio);

            int iconSize = (int) (getResources().getDisplayMetrics().density * 24);

            Drawable dPrimero = ContextCompat.getDrawable(this, R.drawable.primer_plato);
            if (dPrimero != null) {
                dPrimero.setBounds(0, 0, iconSize, iconSize);
                tvPrimerPlato.setCompoundDrawablesRelative(dPrimero, null, null, null);
                tvPrimerPlato.setCompoundDrawablePadding(12);
            }

            Drawable dSegundo = ContextCompat.getDrawable(this, R.drawable.segundo_plato);
            if (dSegundo != null) {
                dSegundo.setBounds(0, 0, iconSize, iconSize);
                tvSegundoPlato.setCompoundDrawablesRelative(dSegundo, null, null, null);
                tvSegundoPlato.setCompoundDrawablePadding(12);
            }

            Drawable dPostre = ContextCompat.getDrawable(this, R.drawable.postre);
            if (dPostre != null) {
                dPostre.setBounds(0, 0, iconSize, iconSize);
                tvPostre.setCompoundDrawablesRelative(dPostre, null, null, null);
                tvPostre.setCompoundDrawablePadding(12);
            }
        }
    }

    /**
     * Avanza un día en el calendario seleccionado y actualiza la tarjeta.
     */
    private void avanzarDia() {
        if (calendarioSeleccionado != null) {
            calendarioSeleccionado.add(Calendar.DAY_OF_YEAR, 1);
            if (comedorSeleccionado != null) {
                mostrarComedorPrincipal(comedorSeleccionado);
            }
        }
    }

    /**
     * Retrocede un día en el calendario seleccionado, validando que no sea anterior a hoy.
     */
    private void retrocederDia() {
        if (calendarioSeleccionado != null) {
            Calendar hoy = Calendar.getInstance();
            limpiarHora(hoy);
            Calendar temp = (Calendar) calendarioSeleccionado.clone();
            limpiarHora(temp);

            temp.add(Calendar.DAY_OF_YEAR, -1);
            if (temp.before(hoy)) {
                Toast.makeText(this, "⚠️ No se permite ir a un día anterior al actual.", Toast.LENGTH_SHORT).show();
            } else {
                calendarioSeleccionado.add(Calendar.DAY_OF_YEAR, -1);
                if (comedorSeleccionado != null) {
                    mostrarComedorPrincipal(comedorSeleccionado);
                }
            }
        }
    }

    /**
     * Normaliza un objeto Calendar eliminando horas, minutos y segundos para comparar solo fechas.
     */
    private void limpiarHora(Calendar c) {
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
    }

    /**
     * Muestra la lista inferior con todos los comedores ordenados por distancia.
     */
    private void mostrarListaComedoresOrdenados() {
        llListaComedores.removeAllViews();

        for (int i = 0; i < listaComedores.size(); i++) {
            Comedor comedor = listaComedores.get(i);

            View itemView = getLayoutInflater().inflate(R.layout.item_comedor_cercania, llListaComedores, false);
            TextView tvNombre = itemView.findViewById(R.id.tvNombreComedorItem);
            TextView tvDireccion = itemView.findViewById(R.id.tvDireccionComedorItem);
            TextView tvDistancia = itemView.findViewById(R.id.tvDistanciaComedorItem);

            StringBuilder textoNombre = new StringBuilder();
            textoNombre.append(i + 1).append(". ").append(comedor.getNombreComedor());

            if (i == 0) {
                textoNombre.append(" (Más cercano)");
            }

            // Seleccionar comedor
            if (comedorSeleccionado != null && comedor.getIdComedor() == comedorSeleccionado.getIdComedor()) {
                textoNombre.append(" ⭐");
            }

            tvNombre.setText(textoNombre.toString());
            tvDireccion.setText(comedor.getDireccion());
            tvDistancia.setText(comedor.getDistanciaFormateada());

            // Al pulsar sobre cualquier comedor de la lista, se selecciona y actualiza la tarjeta superior
            itemView.setOnClickListener(v -> {
                comedorSeleccionado = comedor;
                mostrarComedorPrincipal(comedor);
                mostrarListaComedoresOrdenados();
                Toast.makeText(Comedores.this, "Mostrando: " + comedor.getNombreComedor(), Toast.LENGTH_SHORT).show();
            });

            llListaComedores.addView(itemView);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (detectorInclinacion != null) {
            detectorInclinacion.parar();
        }
    }
}
