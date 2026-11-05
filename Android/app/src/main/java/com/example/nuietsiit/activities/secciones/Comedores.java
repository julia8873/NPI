package com.example.nuietsiit.activities.secciones;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
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
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.nuietsiit.R;
import com.example.nuietsiit.activities.BaseActivity;
import com.example.nuietsiit.activities.auth.Login;
import com.example.nuietsiit.activities.main.AjustesCuenta;
import com.example.nuietsiit.data.Comedores.ComedoresDB;
import com.example.nuietsiit.data.Comedores.Comedor;
import com.example.nuietsiit.data.Comedores.MenuInfo;
import com.example.nuietsiit.data.UsuariosDB;
import com.example.nuietsiit.util.DetectorInclinacion;
import com.example.nuietsiit.util.DetectorInclinacionFrontal;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Actividad encargada de gestionar la sección de Comedores universitarios.
 * Permite calcular la distancia desde la ubicación actual del usuario, ordenar los comedores por cercanía,
 * seleccionar entre menú normal y vegetariano (mediante desplegable inferior o inclinando el móvil hacia atrás),
 * consultar horarios y precios mediante BottomSheetDialog, y navegar entre los días de la semana (Lunes a Viernes).
 */
public class Comedores extends BaseActivity {

    // Componentes de la interfaz de usuario
    private TextView tvMensajeComedorCercano;
    private TextView tvTagComedorCercano;
    private TextView tvDia, tvPrimerPlato, tvSegundoPlato, tvPostre;
    private ImageView ivIcono;
    private View cardMenu;
    private ImageButton btnDiaAnterior, btnDiaSiguiente;
    private Button btnHorario, btnPrecios, btnTipoMenu;
    private TextView btnDiaL, btnDiaM, btnDiaX, btnDiaJ, btnDiaV;

    // Estado del modo de menú (normal vs vegetariano)
    private boolean esModoVegetariano = false;

    // Servicios de datos, geolocalización y sensores
    private ComedoresDB comedoresDB;
    private LocationManager locationManager;
    private DetectorInclinacionFrontal detectorInclinacionAtras; // Sensor para mover hacia atrás (alternar menú vegetariano/normal)
    private DetectorInclinacion detectorInclinacionDias;         // Sensor para inclinar a derecha/izquierda (cambiar de día)
    private Calendar calendarioSeleccionado;                     // Fecha actualmente seleccionada para el menú
    private List<Comedor> listaComedores;                        // Lista de comedores ordenados por distancia
    private Comedor comedorSeleccionado;                         // Comedor actualmente seleccionado en la tarjeta principal

    // Diálogo desplegable de tipo de menú y sus vistas de opciones activas
    private BottomSheetDialog dialogTipoMenu;
    private View btnOpcionMenuNormalDialog;
    private View btnOpcionMenuVegetarianoDialog;
    private TextView badgeMenuNormalDialog;
    private TextView badgeMenuVegetarianoDialog;

    // Lanzador para solicitar permisos de ubicación en tiempo de ejecución
    private final ActivityResultLauncher<String[]> locationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                Boolean fineGranted = result.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false);
                Boolean coarseGranted = result.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false);

                if ((fineGranted != null && fineGranted) || (coarseGranted != null && coarseGranted)) {
                    obtenerUbicacionYProcesar();
                } else {
                    aplicarUbicacionPorDefecto(37.197050, -3.624260);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.comedores);

        // Inicializar base de datos SQLite y servicios de ubicación
        comedoresDB = new ComedoresDB(this);
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        // Asegurar que los menús guardados de la simulación (normales y vegetarianos) estén presentes
        comedoresDB.asegurarMenusSimulacion();

        // Simulación: Iniciar en el martes 3 de noviembre de 2026
        calendarioSeleccionado = Calendar.getInstance();
        calendarioSeleccionado.set(Calendar.YEAR, 2026);
        calendarioSeleccionado.set(Calendar.MONTH, Calendar.NOVEMBER);
        calendarioSeleccionado.set(Calendar.DAY_OF_MONTH, 3);
        limpiarHora(calendarioSeleccionado);

        // Enlazar vistas del layout XML
        tvMensajeComedorCercano = findViewById(R.id.tvMensajeComedoreCercano);
        tvTagComedorCercano = findViewById(R.id.tvTagComedorCercano);
        tvDia = findViewById(R.id.tvDia);
        tvPrimerPlato = findViewById(R.id.tvPrimerPlato);
        tvSegundoPlato = findViewById(R.id.tvSegundoPlato);
        tvPostre = findViewById(R.id.tvPostre);
        ivIcono = findViewById(R.id.ivIcono);
        cardMenu = findViewById(R.id.cardMenu);

        View btnVolver = findViewById(R.id.btnVolver);
        if (btnVolver != null) {
            btnVolver.setOnClickListener(v -> {
                getWindow().getDecorView().performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
                finish();
            });
        }

        aplicarMargenesSistema();

        // Botones superiores: Horario, Precios y Alternar Vegetariano/Normal
        btnHorario = findViewById(R.id.btnHorario);
        btnPrecios = findViewById(R.id.btnPrecios);
        btnTipoMenu = findViewById(R.id.btnTipoMenu);

        // Determinar modo de menú vegetariano/normal por defecto según el perfil del usuario
        determinarModoVegetarianoInicial();

        // Botones inferiores de días de la semana (Lunes a Viernes)
        btnDiaL = findViewById(R.id.btnDiaL);
        btnDiaM = findViewById(R.id.btnDiaM);
        btnDiaX = findViewById(R.id.btnDiaX);
        btnDiaJ = findViewById(R.id.btnDiaJ);
        btnDiaV = findViewById(R.id.btnDiaV);

        btnHorario.setOnClickListener(v -> {
            getWindow().getDecorView().performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
            mostrarBottomSheetHorario();
        });

        btnPrecios.setOnClickListener(v -> {
            getWindow().getDecorView().performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
            mostrarBottomSheetPrecios();
        });

        btnTipoMenu.setOnClickListener(v -> {
            getWindow().getDecorView().performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
            mostrarBottomSheetTipoMenu();
        });

        View cardMascotaAyudante = findViewById(R.id.cardMascotaAyudante);
        if (cardMascotaAyudante != null) {
            cardMascotaAyudante.setOnClickListener(v -> {
                getWindow().getDecorView().performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
                new AlertDialog.Builder(this)
                        .setIcon(R.drawable.ic_mascota_ayudante)
                        .setTitle("Asistente Virtual ETSIIT 🤖")
                        .setMessage("¡Hola! Soy tu asistente de Comedores. Aquí puedes consultar el menú del día de la UGR, ver los horarios y cambiar entre menú normal o vegetariano.")
                        .setPositiveButton("¡Entendido!", null)
                        .show();
            });
        }

        View cardComedorCercano = findViewById(R.id.cardComedorCercano);
        if (cardComedorCercano != null) {
            cardComedorCercano.setOnClickListener(v -> {
                getWindow().getDecorView().performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
                mostrarBottomSheetComedores();
            });
        }
        if (tvMensajeComedorCercano != null) {
            tvMensajeComedorCercano.setOnClickListener(v -> {
                getWindow().getDecorView().performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
                mostrarBottomSheetComedores();
            });
        }



        // Configurar selección directa mediante los botones de los días
        btnDiaL.setOnClickListener(v ->
                Toast.makeText(this, "Lunes no disponible en el período de simulación", Toast.LENGTH_SHORT).show());
        btnDiaM.setOnClickListener(v -> seleccionarDiaDirecto(3));
        btnDiaX.setOnClickListener(v -> seleccionarDiaDirecto(4));
        btnDiaJ.setOnClickListener(v -> seleccionarDiaDirecto(5));
        btnDiaV.setOnClickListener(v -> seleccionarDiaDirecto(6));

        // Flechas laterales para navegación entre días
        btnDiaAnterior = findViewById(R.id.btnDiaAnterior);
        btnDiaSiguiente = findViewById(R.id.btnDiaSiguiente);

        btnDiaAnterior.setOnClickListener(v -> {
            getWindow().getDecorView().performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
            retrocederDia();
        });

        btnDiaSiguiente.setOnClickListener(v -> {
            getWindow().getDecorView().performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
            avanzarDia();
        });

        // Configurar navegación a la pantalla de ajustes de cuenta o login desde el icono de perfil
        ivIcono.setOnClickListener(v -> {
            String correo = obtenerCorreoUsuarioActual();
            if (correo == null || correo.isEmpty()) {
                Intent intent = new Intent(Comedores.this, Login.class);
                startActivity(intent);
            } else {
                Intent intent = new Intent(Comedores.this, AjustesCuenta.class);
                intent.putExtra("CORREO", correo);
                startActivity(intent);
            }
        });

        // Detector de inclinación hacia atrás: alterna entre menú normal y vegetariano
        detectorInclinacionAtras = new DetectorInclinacionFrontal(this, () -> alternarTipoMenu());

        // Detector de inclinación lateral: derecha = día siguiente, izquierda = día anterior
        detectorInclinacionDias = new DetectorInclinacion(this, direccion -> {
            getWindow().getDecorView().performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
            if (direccion == DetectorInclinacion.DERECHA) {
                if (hayMenuDiaSiguiente()) {
                    avanzarDia();
                }
            } else if (direccion == DetectorInclinacion.IZQUIERDA) {
                if (hayMenuDiaAnterior()) {
                    retrocederDia();
                }
            }
        });

        // Detector de gestos táctiles (swipe / deslizar) sobre la tarjeta del menú
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
                            if (hayMenuDiaAnterior()) {
                                retrocederDia(); // Deslizar hacia la derecha -> Día anterior
                            }
                        } else {
                            if (hayMenuDiaSiguiente()) {
                                avanzarDia(); // Deslizar hacia la izquierda -> Día siguiente
                            }
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

    /**
     * Obtiene el correo del usuario actualmente autenticado (por Intent o SharedPreferences).
     */
    private String obtenerCorreoUsuarioActual() {
        if (getIntent() != null && getIntent().hasExtra("CORREO")) {
            String c = getIntent().getStringExtra("CORREO");
            if (c != null && !c.isEmpty()) return c;
        }
        SharedPreferences prefs = getSharedPreferences("SesionUsuario", MODE_PRIVATE);
        return prefs.getString("CORREO", null);
    }

    /**
     * Determina el modo de menú por defecto según las preferencias del perfil:
     * Si no hay cuenta iniciada o no es vegetariano, menú normal por defecto.
     * Si el perfil tiene activada la preferencia vegetariana, menú vegetariano por defecto.
     */
    private void determinarModoVegetarianoInicial() {
        SharedPreferences prefs = getSharedPreferences("SesionUsuario", MODE_PRIVATE);
        boolean prefVegetariano = prefs.getBoolean("ES_VEGETARIANO", false);
        String correo = obtenerCorreoUsuarioActual();
        if (correo != null && !correo.isEmpty()) {
            UsuariosDB usuariosDB = new UsuariosDB(this);
            esModoVegetariano = usuariosDB.esUsuarioVegetariano(correo) || prefVegetariano;
        } else {
            esModoVegetariano = prefVegetariano;
        }
        actualizarBotonTipoMenu();
    }

    private void guardarPreferenciaVegetariano(boolean esVegetariano) {
        getSharedPreferences("SesionUsuario", MODE_PRIVATE)
                .edit()
                .putBoolean("ES_VEGETARIANO", esVegetariano)
                .apply();
        String correo = obtenerCorreoUsuarioActual();
        if (correo != null && !correo.isEmpty()) {
            try {
                UsuariosDB usuariosDB = new UsuariosDB(this);
                usuariosDB.actualizarPreferenciaVegetariano(correo, esVegetariano);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Alterna entre el menú normal y el menú vegetariano (usado por el sensor de inclinación).
     */
    private void alternarTipoMenu() {
        getWindow().getDecorView().performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
        esModoVegetariano = !esModoVegetariano;
        guardarPreferenciaVegetariano(esModoVegetariano);
        actualizarBotonTipoMenu();
        actualizarVistasDialogoTipoMenu();

        if (comedorSeleccionado != null) {
            mostrarComedorPrincipal(comedorSeleccionado);
        }
    }

    /**
     * Muestra una hoja desplegable desde abajo para seleccionar qué tipo de menú visualizar
     * (Menú o Menú Vegetariano), mostrando claramente cuál está activo.
     */
    private void mostrarBottomSheetTipoMenu() {
        dialogTipoMenu = new BottomSheetDialog(this);
        View view = getLayoutInflater().inflate(R.layout.bottom_sheet_tipo_menu, null);
        dialogTipoMenu.setContentView(view);

        btnOpcionMenuNormalDialog = view.findViewById(R.id.btnOpcionMenuNormal);
        btnOpcionMenuVegetarianoDialog = view.findViewById(R.id.btnOpcionMenuVegetariano);
        badgeMenuNormalDialog = view.findViewById(R.id.badgeMenuNormal);
        badgeMenuVegetarianoDialog = view.findViewById(R.id.badgeMenuVegetariano);

        // Reflejar la opción actualmente seleccionada en el diálogo
        actualizarVistasDialogoTipoMenu();

        btnOpcionMenuNormalDialog.setOnClickListener(v -> {
            getWindow().getDecorView().performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
            esModoVegetariano = false;
            guardarPreferenciaVegetariano(false);
            actualizarBotonTipoMenu();
            if (comedorSeleccionado != null) {
                mostrarComedorPrincipal(comedorSeleccionado);
            }
            if (dialogTipoMenu != null) {
                dialogTipoMenu.dismiss();
            }
        });

        btnOpcionMenuVegetarianoDialog.setOnClickListener(v -> {
            getWindow().getDecorView().performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
            esModoVegetariano = true;
            guardarPreferenciaVegetariano(true);
            actualizarBotonTipoMenu();
            if (comedorSeleccionado != null) {
                mostrarComedorPrincipal(comedorSeleccionado);
            }
            if (dialogTipoMenu != null) {
                dialogTipoMenu.dismiss();
            }
        });

        dialogTipoMenu.setOnDismissListener(d -> {
            dialogTipoMenu = null;
            btnOpcionMenuNormalDialog = null;
            btnOpcionMenuVegetarianoDialog = null;
            badgeMenuNormalDialog = null;
            badgeMenuVegetarianoDialog = null;
        });

        dialogTipoMenu.show();
    }

    /**
     * Sincroniza en tiempo real la opción activa marcada en el desplegable de tipo de menú,
     * permitiendo que si el usuario tumba o mueve el móvil hacia abajo mientras el menú está abierto,
     * la opción activa cambie inmediatamente en pantalla.
     */
    private void actualizarVistasDialogoTipoMenu() {
        if (btnOpcionMenuNormalDialog != null && btnOpcionMenuVegetarianoDialog != null
                && badgeMenuNormalDialog != null && badgeMenuVegetarianoDialog != null) {

            View vActiva = esModoVegetariano ? btnOpcionMenuVegetarianoDialog : btnOpcionMenuNormalDialog;
            View vInactiva = esModoVegetariano ? btnOpcionMenuNormalDialog : btnOpcionMenuVegetarianoDialog;
            View bActiva = esModoVegetariano ? badgeMenuVegetarianoDialog : badgeMenuNormalDialog;
            View bInactiva = esModoVegetariano ? badgeMenuNormalDialog : badgeMenuVegetarianoDialog;

            vActiva.setBackgroundResource(R.drawable.fondo_opcion_menu_activa);
            bActiva.setVisibility(View.VISIBLE);
            vInactiva.setBackgroundResource(R.drawable.fondo_opcion_menu_inactiva);
            bInactiva.setVisibility(View.GONE);

            final View targetView = vActiva;
            targetView.animate().scaleX(1.03f).scaleY(1.03f).setDuration(120).withEndAction(() -> {
                if (targetView.isAttachedToWindow()) {
                    targetView.animate().scaleX(1.0f).scaleY(1.0f).setDuration(120).start();
                }
            }).start();
        }
    }

    /**
     * Actualiza el texto y color del botón superior de tipo de menú para reflejar el estado actual.
     */
    private void actualizarBotonTipoMenu() {
        if (btnTipoMenu == null) return;
        if (esModoVegetariano) {
            btnTipoMenu.setText("🌱 Menú Vegetariano ▾");
            btnTipoMenu.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#2E7D32")));
        } else {
            btnTipoMenu.setText("🥩 Menú ▾");
            btnTipoMenu.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#FF8C42")));
        }
    }

    /**
     * Selecciona directamente un día de la semana a través de su botón (Lunes a Viernes).
     */
    private void seleccionarDiaDirecto(int diaMes) {
        getWindow().getDecorView().performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
        if (calendarioSeleccionado != null) {
            calendarioSeleccionado.set(Calendar.DAY_OF_MONTH, diaMes);
            if (comedorSeleccionado != null) {
                mostrarComedorPrincipal(comedorSeleccionado);
            }
            actualizarVisibilidadBotonesNavegacion();
            actualizarIndicadoresDias();
        }
    }

    /**
     * Actualiza el estilo visual de los botones de días de la semana según el día seleccionado.
     */
    private void actualizarIndicadoresDias() {
        if (calendarioSeleccionado == null) return;
        int diaActual = calendarioSeleccionado.get(Calendar.DAY_OF_MONTH);

        configurarEstiloBotonDia(btnDiaM, diaActual == 3);
        configurarEstiloBotonDia(btnDiaX, diaActual == 4);
        configurarEstiloBotonDia(btnDiaJ, diaActual == 5);
        configurarEstiloBotonDia(btnDiaV, diaActual == 6);
    }

    private void configurarEstiloBotonDia(TextView btn, boolean seleccionado) {
        if (btn == null) return;
        if (seleccionado) {
            btn.setBackgroundResource(R.drawable.circulo_dia_seleccionado);
            btn.setTextColor(Color.WHITE);
        } else {
            btn.setBackgroundResource(R.drawable.circulo_dia_disponible);
            btn.setTextColor(Color.parseColor("#FF8C42"));
        }
    }

    /**
     * Devuelve la fecha mínima permitida de la semana simulada (Martes 3 de noviembre de 2026).
     */
    private Calendar obtenerFechaMinima() {
        Calendar min = Calendar.getInstance();
        min.set(Calendar.YEAR, 2026);
        min.set(Calendar.MONTH, Calendar.NOVEMBER);
        min.set(Calendar.DAY_OF_MONTH, 3);
        limpiarHora(min);
        return min;
    }

    /**
     * Devuelve la fecha máxima permitida de la semana simulada (Viernes 6 de noviembre de 2026).
     */
    private Calendar obtenerFechaMaxima() {
        Calendar max = Calendar.getInstance();
        max.set(Calendar.YEAR, 2026);
        max.set(Calendar.MONTH, Calendar.NOVEMBER);
        max.set(Calendar.DAY_OF_MONTH, 6);
        limpiarHora(max);
        return max;
    }

    /**
     * Comprueba si existe un menú disponible para el día anterior dentro del rango permitido.
     */
    private boolean hayMenuDiaAnterior() {
        if (calendarioSeleccionado == null) return false;
        Calendar min = obtenerFechaMinima();
        Calendar temp = (Calendar) calendarioSeleccionado.clone();
        limpiarHora(temp);
        temp.add(Calendar.DAY_OF_YEAR, -1);
        return !temp.before(min);
    }

    /**
     * Comprueba si existe un menú disponible para el día siguiente hasta final de semana (viernes).
     */
    private boolean hayMenuDiaSiguiente() {
        if (calendarioSeleccionado == null) return false;
        Calendar max = obtenerFechaMaxima();
        Calendar temp = (Calendar) calendarioSeleccionado.clone();
        limpiarHora(temp);
        temp.add(Calendar.DAY_OF_YEAR, 1);
        return !temp.after(max);
    }

    /**
     * Actualiza la visibilidad de los botones de navegación lateral:
     * Si no hay menú del día anterior, oculta directamente el botón izquierdo.
     * Si se llega al viernes (final de semana), oculta el botón derecho.
     */
    private void actualizarVisibilidadBotonesNavegacion() {
        if (btnDiaAnterior != null) {
            btnDiaAnterior.setVisibility(hayMenuDiaAnterior() ? View.VISIBLE : View.GONE);
        }
        if (btnDiaSiguiente != null) {
            btnDiaSiguiente.setVisibility(hayMenuDiaSiguiente() ? View.VISIBLE : View.GONE);
        }
    }

    /**
     * Muestra una hoja desplegable desde abajo con el horario de apertura del comedor.
     */
    private void mostrarBottomSheetHorario() {
        if (comedorSeleccionado == null) return;
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View view = getLayoutInflater().inflate(R.layout.bottom_sheet_horario, null);
        dialog.setContentView(view);

        TextView tvComedor = view.findViewById(R.id.tvBsHorarioComedor);
        TextView tvDia = view.findViewById(R.id.tvBsHorarioDia);
        TextView tvDetalle = view.findViewById(R.id.tvBsHorarioDetalle);

        tvComedor.setText(comedorSeleccionado.getNombreComedor());

        SimpleDateFormat sdf = new SimpleDateFormat("d 'de' MMMM", Locale.forLanguageTag("es"));
        String fechaFormateada = sdf.format(calendarioSeleccionado.getTime());
        tvDia.setText("📅 " + fechaFormateada);

        String horario = comedoresDB.obtenerHorarioParaFecha(comedorSeleccionado.getIdComedor(), calendarioSeleccionado);
        tvDetalle.setText(horario);

        dialog.show();
    }

    /**
     * Muestra una hoja desplegable desde abajo con los precios oficiales del menú.
     */
    private void mostrarBottomSheetPrecios() {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View view = getLayoutInflater().inflate(R.layout.bottom_sheet_precios, null);
        dialog.setContentView(view);
        dialog.show();
    }

    /**
     * Muestra una hoja desplegable desde abajo con la lista completa de comedores ordenados por distancia,
     * permitiendo hacer scroll y seleccionar cualquiera de ellos para ver su menú en el cartel.
     */
    private void mostrarBottomSheetComedores() {
        if (listaComedores == null || listaComedores.isEmpty()) {
            listaComedores = comedoresDB.obtenerListaComedores();
        }

        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View view = getLayoutInflater().inflate(R.layout.bottom_sheet_comedores, null);
        dialog.setContentView(view);

        LinearLayout llBsListaComedores = view.findViewById(R.id.llBsListaComedores);
        llBsListaComedores.removeAllViews();

        for (int i = 0; i < listaComedores.size(); i++) {
            Comedor comedor = listaComedores.get(i);

            View itemView = getLayoutInflater().inflate(R.layout.item_comedor_cercania, llBsListaComedores, false);
            TextView tvNombre = itemView.findViewById(R.id.tvNombreComedorItem);
            TextView tvDireccion = itemView.findViewById(R.id.tvDireccionComedorItem);
            TextView tvDistancia = itemView.findViewById(R.id.tvDistanciaComedorItem);

            StringBuilder textoNombre = new StringBuilder();
            textoNombre.append(i + 1).append(". ").append(comedor.getNombreComedor());

            if (i == 0) {
                textoNombre.append(" (Más cercano)");
            }

            if (comedorSeleccionado != null && comedor.getIdComedor() == comedorSeleccionado.getIdComedor()) {
                textoNombre.append(" ⭐");
            }

            tvNombre.setText(textoNombre.toString());
            tvDireccion.setText(comedor.getDireccion());
            tvDistancia.setText(comedor.getDistanciaFormateada());

            itemView.setOnClickListener(v -> {
                comedorSeleccionado = comedor;
                mostrarComedorPrincipal(comedor);
                dialog.dismiss();
            });

            llBsListaComedores.addView(itemView);
        }

        dialog.show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (detectorInclinacionAtras != null) {
            detectorInclinacionAtras.iniciar();
        }
        if (detectorInclinacionDias != null) {
            detectorInclinacionDias.iniciar();
        }

        // Si el usuario regresa de AjustesCuenta tras cambiar su preferencia, actualizar el menú
        SharedPreferences prefs = getSharedPreferences("SesionUsuario", MODE_PRIVATE);
        boolean preferenciaVeg = prefs.getBoolean("ES_VEGETARIANO", false);
        String correo = obtenerCorreoUsuarioActual();
        if (correo != null && !correo.isEmpty()) {
            UsuariosDB usuariosDB = new UsuariosDB(this);
            if (usuariosDB.esUsuarioVegetariano(correo)) {
                preferenciaVeg = true;
            }
        }
        if (preferenciaVeg != esModoVegetariano) {
            esModoVegetariano = preferenciaVeg;
            actualizarBotonTipoMenu();
            if (comedorSeleccionado != null) {
                mostrarComedorPrincipal(comedorSeleccionado);
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (detectorInclinacionAtras != null) {
            detectorInclinacionAtras.parar();
        }
        if (detectorInclinacionDias != null) {
            detectorInclinacionDias.parar();
        }
    }

    /**
     * Comprueba los permisos de ubicación (Fina/Aproximada) y procede a localizar.
     */
    private void verificarPermisosYLocalizar() {
        boolean fine = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
        boolean coarse = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;

        if (fine || coarse) {
            obtenerUbicacionYProcesar();
        } else {
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

            solicitarPosicionActual();
        } catch (SecurityException e) {
            e.printStackTrace();
        }

        if (ultimaUbicacion != null) {
            procesarUbicacionUsuario(ultimaUbicacion);
        } else {
            aplicarUbicacionPorDefecto(37.197050, -3.624260);
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



        calcularDistanciasYOrdenar(userLat, userLon);
    }

    /**
     * Aplica coordenadas iniciales por defecto para ordenar comedores si aún no se tiene GPS.
     */
    private void aplicarUbicacionPorDefecto(double defLat, double defLon) {
        calcularDistanciasYOrdenar(defLat, defLon);
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

        Collections.sort(listaComedores, Comparator.comparingDouble(Comedor::getDistanciaMetros));

        if (!listaComedores.isEmpty()) {
            comedorSeleccionado = listaComedores.get(0);
            mostrarComedorPrincipal(comedorSeleccionado);
        }
    }

    /**
     * Actualiza el cartel principal con los datos del comedor seleccionado y la fecha seleccionada.
     * Muestra solo la fecha (ej: "3 de noviembre") para que todo quepa perfectamente.
     */
    private void mostrarComedorPrincipal(Comedor comedor) {
        boolean esElMasCercano = !listaComedores.isEmpty() && listaComedores.get(0).getIdComedor() == comedor.getIdComedor();
        if (esElMasCercano) {
            if (tvTagComedorCercano != null) tvTagComedorCercano.setText("📍 COMEDOR MÁS CERCANO");
            tvMensajeComedorCercano.setText(comedor.getNombreComedor());
        } else {
            if (tvTagComedorCercano != null) tvTagComedorCercano.setText("📍 COMEDOR SELECCIONADO");
            tvMensajeComedorCercano.setText(comedor.getNombreComedor());
        }

        if (calendarioSeleccionado == null) {
            calendarioSeleccionado = Calendar.getInstance();
        }

        // Consultar base de datos para verificar si el comedor está abierto en esa fecha
        boolean esCerrado = comedoresDB.esDiaCerrado(comedor.getIdComedor(), calendarioSeleccionado);

        if (esCerrado) {
            // Directamente no mostrar el menú/cartel en días sin menú
            if (cardMenu != null) {
                cardMenu.setVisibility(View.GONE);
            }
        } else {
            // Mostrar el cartel y cargar los platos del menú (normal o vegetariano)
            if (cardMenu != null) {
                cardMenu.setVisibility(View.VISIBLE);
            }

            // Mostrar el día y mes (ej: "3 de noviembre") e indicar si es menú vegetariano
            SimpleDateFormat sdf = new SimpleDateFormat("d 'de' MMMM", Locale.forLanguageTag("es"));
            String fechaFormateada = sdf.format(calendarioSeleccionado.getTime());
            tvDia.setText(fechaFormateada);

            SimpleDateFormat sdfDb = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            String fechaStr = sdfDb.format(calendarioSeleccionado.getTime());

            MenuInfo menuInfo = comedoresDB.obtenerMenuParaComedorYFecha(comedor.getIdComedor(), fechaStr, esModoVegetariano);

            tvPrimerPlato.setVisibility(View.VISIBLE);
            tvSegundoPlato.setVisibility(View.VISIBLE);
            tvPostre.setVisibility(View.VISIBLE);

            tvPrimerPlato.setText(menuInfo.getPrimerPlato());
            tvSegundoPlato.setText(menuInfo.getSegundoPlato());
            tvPostre.setText(menuInfo.getPostre());

            // Los iconos y títulos están ahora integrados en la cabecera de cada sección en el XML
            tvPrimerPlato.setCompoundDrawablesRelative(null, null, null, null);
            tvSegundoPlato.setCompoundDrawablesRelative(null, null, null, null);
            tvPostre.setCompoundDrawablesRelative(null, null, null, null);
        }

        // Actualizar visibilidad de flechas y el indicador del día de la semana
        actualizarVisibilidadBotonesNavegacion();
        actualizarIndicadoresDias();
        actualizarBotonTipoMenu();
    }

    /**
     * Avanza un día en el calendario si existe menú disponible (hasta el viernes de esa semana).
     */
    private void avanzarDia() {
        if (calendarioSeleccionado != null && hayMenuDiaSiguiente()) {
            calendarioSeleccionado.add(Calendar.DAY_OF_YEAR, 1);
            if (comedorSeleccionado != null) {
                mostrarComedorPrincipal(comedorSeleccionado);
            }
            actualizarVisibilidadBotonesNavegacion();
            actualizarIndicadoresDias();
        }
    }

    /**
     * Retrocede un día en el calendario si existe menú disponible (hasta el inicio de la simulación).
     */
    private void retrocederDia() {
        if (calendarioSeleccionado != null && hayMenuDiaAnterior()) {
            calendarioSeleccionado.add(Calendar.DAY_OF_YEAR, -1);
            if (comedorSeleccionado != null) {
                mostrarComedorPrincipal(comedorSeleccionado);
            }
            actualizarVisibilidadBotonesNavegacion();
            actualizarIndicadoresDias();
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (detectorInclinacionAtras != null) {
            detectorInclinacionAtras.parar();
        }
        if (detectorInclinacionDias != null) {
            detectorInclinacionDias.parar();
        }
    }

    /** Deja el contenido fuera de la barra de estado y de navegación (pantalla de borde a borde). */
    private void aplicarMargenesSistema() {
        View scroll = findViewById(R.id.scrollComedores);
        if (scroll != null) {
            ViewCompat.setOnApplyWindowInsetsListener(scroll, (v, insets) -> {
                Insets barras = insets.getInsets(
                        WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
                v.setPadding(barras.left, barras.top, barras.right, barras.bottom);
                return insets;
            });
            ViewCompat.requestApplyInsets(scroll);
        }
    }
}
