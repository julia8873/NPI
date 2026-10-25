package com.example.nuietsiit.activities.main;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.GestureDetector;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;


import com.example.nuietsiit.R;
import com.example.nuietsiit.activities.BaseActivity;
import com.example.nuietsiit.activities.auth.Login;
import com.example.nuietsiit.activities.secciones.Actividades;
import com.example.nuietsiit.activities.secciones.Asignaturas;
import com.example.nuietsiit.activities.secciones.Biblioteca;
import com.example.nuietsiit.activities.secciones.Comedores;
import com.example.nuietsiit.activities.secciones.mapa.Mapa;
import com.example.nuietsiit.activities.secciones.Noticias;
import com.example.nuietsiit.activities.secciones.Profesorado;
import com.example.nuietsiit.activities.secciones.Secretaria;
import com.example.nuietsiit.data.ActividadesDB;
import com.example.nuietsiit.data.CalendarioDB;
import com.example.nuietsiit.data.DBHelper;
import com.example.nuietsiit.data.SecretariaDB;
import com.example.nuietsiit.data.UsuariosDB;
import com.example.nuietsiit.data.modelo.EventoCalendario;
import com.example.nuietsiit.util.DetectorInclinacion;
import com.example.nuietsiit.util.Notificador;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.card.MaterialCardView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class MenuPrincipal extends BaseActivity {

    // Componentes de la interfaz de usuario
    private TextView tvBienvenida, tvAnteriorNombre, tvSiguienteNombre, tvNombreSeccion;
    private ImageView ivIcono, ivIconoSeccion;
    private Button btnSeccionGrande;
    private ImageButton btnAnteriorSeccion, btnSiguienteSeccion;
    private DetectorInclinacion detectorInclinacion;
    private int indiceSeccionActual = 0;
    private boolean fueDeslizamiento = false;
    private final String[] nombresSecciones = {
            "Mapa", "Actividades", "Asignaturas", "Profesorado", "Secretaría", "Comedores", "Biblioteca"
    };
    private final int[] iconosSecciones = {
            R.drawable.ic_seccion_mapa,
            R.drawable.ic_seccion_actividades,
            R.drawable.ic_seccion_asignaturas,
            R.drawable.ic_seccion_profesorado,
            R.drawable.ic_seccion_secretaria,
            R.drawable.ic_seccion_comedores,
            R.drawable.ic_seccion_biblioteca
    };

    // Componentes del Calendario de Actividades
    private GridView gvCalendario;
    private TextView tvMesAno, tvFechaSeleccionada, tvNumEventosFecha, tvSinEventos;
    private ImageButton btnMesAnterior, btnMesSiguiente;
    private LinearLayout llEventosContainer;
    private Button btnAnadirEvento;
    private CalendarioDB calendarioDB;
    private ActividadesDB actividadesDB;
    private SecretariaDB secretariaDB;
    private final Calendar mesMostradoCal = Calendar.getInstance();
    private String fechaSeleccionadaStr;

    // Componentes para el slider de noticias
    private ViewPager2 vpNoticias;
    private final Handler sliderHandler = new Handler(Looper.getMainLooper());
    private List<Integer> listaNoticias;

    // Helper para la base de datos
    private DBHelper BD;

    // Variable para almacenar el correo del usuario logueado
    private String correoUsuario;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.menu_principal);

        // !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
        // QUITAR???????????????????????????????????????????????????????????????
        comprobarAnimacionOla(getIntent());

        // Inicializar base de datos
        BD = DBHelper.getInstance(this);

        // Inicializar vistas
        tvBienvenida = findViewById(R.id.tvBienvenida);
        ivIcono = findViewById(R.id.ivIcono);
        vpNoticias = findViewById(R.id.vpNoticias);
        tvAnteriorNombre = findViewById(R.id.tvAnteriorNombre);
        tvSiguienteNombre = findViewById(R.id.tvSiguienteNombre);

        // Configurar sensor de inclinación lateral para cambiar de sección (Derecha = Siguiente, Izquierda = Anterior)
        detectorInclinacion = new DetectorInclinacion(this, direccion -> {
            getWindow().getDecorView().performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
            if (direccion == DetectorInclinacion.DERECHA) {
                siguienteSeccion();
            } else if (direccion == DetectorInclinacion.IZQUIERDA) {
                anteriorSeccion();
            }
        });

        // Leer sesión de SharedPreferences
        SharedPreferences prefs = getSharedPreferences("SesionUsuario", MODE_PRIVATE);
        correoUsuario = prefs.getString("CORREO", null); // Si no hay nadie logueado, será null (invitado)

        // Obtención de datos pasados mediante Intent
        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("CORREO")) {
            correoUsuario = intent.getStringExtra("CORREO");
            prefs.edit().putString("CORREO", correoUsuario).apply();
        }

        // Cargar y mostrar la bienvenida con el nombre del usuario
        mostrarBienvenida();

        // Configuración del slider de noticias
        configurarSliderNoticias();

        // Vinculación y configuración de eventos de navegación
        inicializarBotonesNavegacion();

        // Inicializar Calendario y eventos
        calendarioDB = new CalendarioDB(BD);
        actividadesDB = new ActividadesDB(BD);
        secretariaDB = new SecretariaDB(BD);
        secretariaDB.poblarCitasDeEjemploSiVacio();
        inicializarCalendario();

        // Notificaciones del sistema: avisos urgentes y actividades de hoy/mañana (Miguel)
        Notificador.revisarAvisos(this, correoUsuario);
    }

    // !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
    // QUITAR???????????????????????????????????????????????????????????????
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        comprobarAnimacionOla(intent);
    }

    // !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
    // QUITAR???????????????????????????????????????????????????????????????
    private void comprobarAnimacionOla(Intent intent) {
        if (intent != null && intent.getBooleanExtra("START_WAVE_DOWN", false)) {
            // Limpiamos el extra para no repetirlo innecesariamente
            intent.removeExtra("START_WAVE_DOWN");

            WaveView waveView = findViewById(R.id.waveView);

            if (waveView != null) {
                waveView.startWaveDown(null);
            }
        }
    }
    /**
     * Obtiene el nombre del usuario según su correo y actualiza la etiqueta de bienvenida.
     */
    private void mostrarBienvenida() {
        if (correoUsuario != null && !correoUsuario.isEmpty()) {
            UsuariosDB usuariosDB = new UsuariosDB(BD);
            String nombreUsuario = usuariosDB.obtenerNombreUsuario(correoUsuario);
            if (nombreUsuario != null && !nombreUsuario.isEmpty()) {
                tvBienvenida.setText("¡Bienvenido " + nombreUsuario + "!");
            } else {
                tvBienvenida.setText("¡Bienvenido " + correoUsuario + "!");
            }
        } else {
            tvBienvenida.setText("¡Bienvenido!");
        }
    }

    private void siguienteSeccion() {
        indiceSeccionActual = (indiceSeccionActual + 1) % nombresSecciones.length;
        actualizarBotonSeccion();
    }

    private void anteriorSeccion() {
        indiceSeccionActual = (indiceSeccionActual - 1 + nombresSecciones.length) % nombresSecciones.length;
        actualizarBotonSeccion();
    }

    private void actualizarBotonSeccion() {
        if (tvNombreSeccion != null) {
            tvNombreSeccion.setText(nombresSecciones[indiceSeccionActual]);
        } else if (btnSeccionGrande != null) {
            btnSeccionGrande.setText(nombresSecciones[indiceSeccionActual]);
        }

        if (ivIconoSeccion != null) {
            ivIconoSeccion.setImageResource(iconosSecciones[indiceSeccionActual]);
        }

        if (tvAnteriorNombre != null) {
            int indexAnterior = (indiceSeccionActual - 1 + nombresSecciones.length) % nombresSecciones.length;
            tvAnteriorNombre.setText(nombresSecciones[indexAnterior]);
        }
        if (tvSiguienteNombre != null) {
            int indexSiguiente = (indiceSeccionActual + 1) % nombresSecciones.length;
            tvSiguienteNombre.setText(nombresSecciones[indexSiguiente]);
        }
    }

    /**
     * Asigna las vistas y listeners a todos los botones del menú.
     */
    @SuppressLint("ClickableViewAccessibility")
    private void inicializarBotonesNavegacion() {
        // Clic en la foto/icono de perfil
        ivIcono.setOnClickListener(v -> {
            if (correoUsuario == null || correoUsuario.isEmpty()) {
                // Si no ha iniciado sesión / no registrado -> llevar a Login
                Intent intent = new Intent(MenuPrincipal.this, Login.class);
                startActivity(intent);
            } else {
                // Si ya ha iniciado sesión -> llevar a Ajustes de cuenta
                Intent intent = new Intent(MenuPrincipal.this, AjustesCuenta.class);
                intent.putExtra("CORREO", correoUsuario);
                startActivity(intent);
            }
        });

        btnSeccionGrande = findViewById(R.id.btnSeccionGrande);
        ivIconoSeccion = findViewById(R.id.ivIconoSeccion);
        tvNombreSeccion = findViewById(R.id.tvNombreSeccion);
        btnAnteriorSeccion = findViewById(R.id.btnAnteriorSeccion);
        btnSiguienteSeccion = findViewById(R.id.btnSiguienteSeccion);
        View layoutAnterior = findViewById(R.id.layoutAnterior);
        View layoutSiguiente = findViewById(R.id.layoutSiguiente);

        actualizarBotonSeccion();

        btnAnteriorSeccion.setOnClickListener(v -> {
            v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
            anteriorSeccion();
        });

        btnSiguienteSeccion.setOnClickListener(v -> {
            v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
            siguienteSeccion();
        });

        if (layoutAnterior != null) {
            layoutAnterior.setOnClickListener(v -> {
                v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
                anteriorSeccion();
            });
        }

        if (layoutSiguiente != null) {
            layoutSiguiente.setOnClickListener(v -> {
                v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
                siguienteSeccion();
            });
        }

        // Detector de gestos táctiles (swipe / deslizar) sobre el botón grande
        GestureDetector gestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            private static final int SWIPE_THRESHOLD = 40;
            private static final int SWIPE_VELOCITY_THRESHOLD = 40;

            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                if (e1 == null || e2 == null) return false;
                float diffX = e2.getX() - e1.getX();
                float diffY = e2.getY() - e1.getY();
                if (Math.abs(diffX) > Math.abs(diffY)) {
                    if (Math.abs(diffX) > SWIPE_THRESHOLD && Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                        fueDeslizamiento = true;
                        btnSeccionGrande.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
                        if (diffX > 0) {
                            anteriorSeccion(); // Deslizar hacia la derecha -> Anterior
                        } else {
                            siguienteSeccion(); // Deslizar hacia la izquierda -> Siguiente
                        }
                        return true;
                    }
                }
                return false;
            }
        });

        btnSeccionGrande.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                fueDeslizamiento = false;
            }
            gestureDetector.onTouchEvent(event);
            if (event.getAction() == MotionEvent.ACTION_UP && fueDeslizamiento) {
                return true; // Consume el evento para evitar que salte el click después de deslizar
            }
            return false;
        });

        btnSeccionGrande.setOnClickListener(v -> {
            if (fueDeslizamiento) return; // Si fue un gesto de deslizar, no abrir la sección
            v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
            Intent intent;
            switch (indiceSeccionActual) {
                case 0:
                    intent = new Intent(MenuPrincipal.this, Mapa.class);
                    break;
                case 1:
                    intent = new Intent(MenuPrincipal.this, Actividades.class);
                    break;
                case 2:
                    intent = new Intent(MenuPrincipal.this, Asignaturas.class);
                    break;
                case 3:
                    intent = new Intent(MenuPrincipal.this, Profesorado.class);
                    break;
                case 4:
                    intent = new Intent(MenuPrincipal.this, Secretaria.class);
                    break;
                case 5:
                    intent = new Intent(MenuPrincipal.this, Comedores.class);
                    break;
                case 6:
                    intent = new Intent(MenuPrincipal.this, Biblioteca.class);
                    break;
                default:
                    return;
            }
            intent.putExtra("CORREO", correoUsuario);
            startActivity(intent);
        });
    }

    /**
     * Configura el ViewPager2 con el adaptador y el cambio automático de páginas.
     */
    private void configurarSliderNoticias() {
        listaNoticias = new ArrayList<>();
        listaNoticias.add(R.drawable.noticia1);
        listaNoticias.add(R.drawable.noticia2);
        listaNoticias.add(R.drawable.noticia3);
        listaNoticias.add(R.drawable.noticia4);
        listaNoticias.add(R.drawable.noticia5);
        listaNoticias.add(R.drawable.noticia6);

        // Adaptador
        vpNoticias.setAdapter(new RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            @NonNull
            @Override
            public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                ImageView imageView = new ImageView(parent.getContext());
                imageView.setLayoutParams(new ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                ));
                imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
                // Al pulsar una noticia del carrusel se abre la pantalla de Noticias
                imageView.setOnClickListener(v -> {
                    Intent intent = new Intent(MenuPrincipal.this, Noticias.class);
                    intent.putExtra("CORREO", correoUsuario);
                    startActivity(intent);
                });
                return new RecyclerView.ViewHolder(imageView) {};
            }

            @Override
            public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
                ((ImageView) holder.itemView).setImageResource(listaNoticias.get(position));
            }

            @Override
            public int getItemCount() {
                return listaNoticias.size();
            }
        });

        // Reinicia el temporizador cuando el usuario cambia manualmente la página
        vpNoticias.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                sliderHandler.removeCallbacks(sliderRunnable);
                sliderHandler.postDelayed(sliderRunnable, 5000); // 3 segundos
            }
        });
    }

    /**
     * Runnable encargado de la transición automática de las noticias en el ViewPager2.
     */
    private final Runnable sliderRunnable = new Runnable() {
        @Override
        public void run() {
            if (listaNoticias != null && !listaNoticias.isEmpty()) {
                int siguientePosicion = (vpNoticias.getCurrentItem() + 1) % listaNoticias.size();
                vpNoticias.setCurrentItem(siguientePosicion, true);
            }
        }
    };

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        // Si acepta el permiso de notificaciones, se revisan los avisos (Miguel)
        Notificador.alResponderPermiso(this, requestCode, grantResults, correoUsuario);
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Pausa el slider automático para ahorrar batería y recursos al salir de la pantalla
        sliderHandler.removeCallbacks(sliderRunnable);
        if (detectorInclinacion != null) {
            detectorInclinacion.parar();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Reanuda la animación del slider cuando la actividad regresa al primer plano
        sliderHandler.postDelayed(sliderRunnable, 3000);
        if (detectorInclinacion != null) {
            detectorInclinacion.iniciar();
        }
    }

    /**
     * Comprueba si el usuario actual ha iniciado sesión (no es invitado ni nulo).
     */
    private boolean usuarioEstaRegistrado() {
        return correoUsuario != null
                && !correoUsuario.trim().isEmpty()
                && !"invitado@correo.ugr.es".equalsIgnoreCase(correoUsuario.trim());
    }

    /**
     * Muestra aviso de inicio de sesión si es un usuario no registrado/invitado.
     */
    private void verificarYMostrarDialogoAnadirEvento() {
        if (!usuarioEstaRegistrado()) {
            new AlertDialog.Builder(this)
                    .setTitle("🔒 Inicio de sesión requerido")
                    .setMessage("Para añadir o editar tus exámenes, entregas y actividades en el calendario debes iniciar sesión con tu cuenta.")
                    .setPositiveButton("Iniciar sesión", (dialog, which) -> {
                        Intent intent = new Intent(MenuPrincipal.this, Login.class);
                        startActivity(intent);
                    })
                    .setNegativeButton("Cancelar", null)
                    .show();
            return;
        }
        mostrarDialogoAnadirEvento();
    }

    /**
     * Configura y gestiona el Calendario de Actividades, Exámenes y Entregas.
     */
    private void inicializarCalendario() {
        btnMesAnterior = findViewById(R.id.btnMesAnterior);
        btnMesSiguiente = findViewById(R.id.btnMesSiguiente);
        tvMesAno = findViewById(R.id.tvMesAno);
        gvCalendario = findViewById(R.id.gvCalendario);
        tvFechaSeleccionada = findViewById(R.id.tvFechaSeleccionada);
        tvNumEventosFecha = findViewById(R.id.tvNumEventosFecha);
        tvSinEventos = findViewById(R.id.tvSinEventos);
        llEventosContainer = findViewById(R.id.llEventosContainer);
        btnAnadirEvento = findViewById(R.id.btnAnadirEvento);

        // Fecha seleccionada inicial: HOY (YYYY-MM-DD)
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        Calendar calHoy = Calendar.getInstance();
        fechaSeleccionadaStr = sdf.format(calHoy.getTime());

        if (btnMesAnterior != null) {
            btnMesAnterior.setOnClickListener(v -> {
                mesMostradoCal.add(Calendar.MONTH, -1);
                actualizarGridCalendario();
            });
        }

        if (btnMesSiguiente != null) {
            btnMesSiguiente.setOnClickListener(v -> {
                mesMostradoCal.add(Calendar.MONTH, 1);
                actualizarGridCalendario();
            });
        }

        if (btnAnadirEvento != null) {
            btnAnadirEvento.setOnClickListener(v -> verificarYMostrarDialogoAnadirEvento());
        }

        actualizarGridCalendario();
        cargarEventosFechaSeleccionada();
    }

    /**
     * Dibuja la matriz del mes en el GridView resaltando en color los días que tienen eventos.
     */
    private void actualizarGridCalendario() {
        if (tvMesAno != null) {
            SimpleDateFormat sdfMesAno = new SimpleDateFormat("MMMM yyyy", new Locale("es", "ES"));
            String mesAno = sdfMesAno.format(mesMostradoCal.getTime());
            if (mesAno.length() > 0) {
                mesAno = mesAno.substring(0, 1).toUpperCase() + mesAno.substring(1);
            }
            tvMesAno.setText(mesAno);
        }

        Calendar calGrid = (Calendar) mesMostradoCal.clone();
        calGrid.set(Calendar.DAY_OF_MONTH, 1);
        int primerDiaSemana = calGrid.get(Calendar.DAY_OF_WEEK); // 1 = Dom, 2 = Lun...
        int offsetLunes = (primerDiaSemana + 5) % 7;
        calGrid.add(Calendar.DAY_OF_MONTH, -offsetLunes);

        List<Date> listaDias = new ArrayList<>();
        for (int i = 0; i < 35; i++) {
            listaDias.add(calGrid.getTime());
            calGrid.add(Calendar.DAY_OF_MONTH, 1);
        }

        if (gvCalendario != null) {
            gvCalendario.setAdapter(new BaseAdapter() {
                @Override
                public int getCount() {
                    return listaDias.size();
                }

                @Override
                public Object getItem(int position) {
                    return listaDias.get(position);
                }

                @Override
                public long getItemId(int position) {
                    return position;
                }

                @Override
                public View getView(int position, View convertView, ViewGroup parent) {
                    View cell = convertView;
                    if (cell == null) {
                        cell = getLayoutInflater().inflate(R.layout.item_dia_calendario, parent, false);
                    }

                    Date date = listaDias.get(position);
                    Calendar cDia = Calendar.getInstance();
                    cDia.setTime(date);

                    SimpleDateFormat sdfIso = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                    String dateStr = sdfIso.format(date);

                    TextView tvNumeroDia = cell.findViewById(R.id.tvNumeroDia);
                    FrameLayout flCirculoDia = cell.findViewById(R.id.flCirculoDia);
                    LinearLayout llPuntosIndicadores = cell.findViewById(R.id.llPuntosIndicadores);

                    tvNumeroDia.setText(String.valueOf(cDia.get(Calendar.DAY_OF_MONTH)));

                    boolean esMismoMes = cDia.get(Calendar.MONTH) == mesMostradoCal.get(Calendar.MONTH);
                    boolean esSeleccionado = dateStr.equals(fechaSeleccionadaStr);

                    // Consultar eventos para esta fecha (Usuario, Actividades Oficiales y Citas de Secretaría)
                    List<EventoCalendario> evsUser = calendarioDB != null ? calendarioDB.obtenerEventosPorFecha(dateStr, correoUsuario) : new ArrayList<>();
                    List<EventoCalendario> evsOficiales = actividadesDB != null ? actividadesDB.obtenerActividadesPorFecha(dateStr) : new ArrayList<>();
                    List<EventoCalendario> evsCitas = secretariaDB != null ? secretariaDB.obtenerCitasPorFecha(dateStr, correoUsuario) : new ArrayList<>();
                    boolean tieneEventos = !evsUser.isEmpty() || !evsOficiales.isEmpty() || !evsCitas.isEmpty();

                    if (!esMismoMes) {
                        tvNumeroDia.setTextColor(Color.parseColor("#CCCCCC"));
                    } else if (esSeleccionado) {
                        tvNumeroDia.setTextColor(Color.WHITE);
                    } else {
                        tvNumeroDia.setTextColor(Color.parseColor("#1A1A1A"));
                        if (tieneEventos) {
                            tvNumeroDia.setTypeface(null, Typeface.BOLD);
                        } else {
                            tvNumeroDia.setTypeface(null, Typeface.NORMAL);
                        }
                    }

                    // Fondo destacado del día
                    GradientDrawable gdCirculo = new GradientDrawable();
                    gdCirculo.setShape(GradientDrawable.OVAL);

                    if (esSeleccionado) {
                        gdCirculo.setColor(Color.parseColor("#FF8C42")); // Naranja primario
                        flCirculoDia.setBackground(gdCirculo);
                    } else if (tieneEventos && esMismoMes) {
                        // ¡RESALTAR DÍAS CON EVENTOS O CITAS! Fondo cálido suave
                        gdCirculo.setColor(Color.parseColor("#FFE0B2"));
                        flCirculoDia.setBackground(gdCirculo);
                    } else {
                        flCirculoDia.setBackground(null);
                    }

                    // Puntos indicadores de tipo de evento (Examen, Entrega, Actividad, Cita, Otro)
                    llPuntosIndicadores.removeAllViews();
                    if (tieneEventos && esMismoMes) {
                        Set<String> tipos = new HashSet<>();
                        for (EventoCalendario e : evsUser) tipos.add(e.tipo);
                        if (!evsOficiales.isEmpty()) tipos.add("Actividad");
                        if (!evsCitas.isEmpty()) tipos.add("Cita");

                        for (String t : tipos) {
                            View dot = new View(MenuPrincipal.this);
                            LinearLayout.LayoutParams dotParams = new LinearLayout.LayoutParams(10, 10);
                            dotParams.setMargins(2, 0, 2, 0);
                            dot.setLayoutParams(dotParams);

                            GradientDrawable gdDot = new GradientDrawable();
                            gdDot.setShape(GradientDrawable.OVAL);

                            int colorDot = Color.parseColor("#FF8C42");
                            if ("Examen".equalsIgnoreCase(t)) colorDot = Color.parseColor("#E53935");
                            else if ("Entrega".equalsIgnoreCase(t)) colorDot = Color.parseColor("#FB8C00");
                            else if ("Actividad".equalsIgnoreCase(t)) colorDot = Color.parseColor("#1E88E5");
                            else if ("Cita".equalsIgnoreCase(t) || "Cita Secretaría".equalsIgnoreCase(t)) colorDot = Color.parseColor("#8E24AA");
                            else if ("Otro".equalsIgnoreCase(t)) colorDot = Color.parseColor("#43A047");

                            gdDot.setColor(colorDot);
                            dot.setBackground(gdDot);
                            llPuntosIndicadores.addView(dot);
                        }
                    }

                    cell.setOnClickListener(v -> {
                        fechaSeleccionadaStr = dateStr;
                        actualizarGridCalendario();
                        cargarEventosFechaSeleccionada();
                    });

                    return cell;
                }
            });
        }
    }

    private void cargarEventosFechaSeleccionada() {
        if (llEventosContainer == null) return;
        llEventosContainer.removeAllViews();

        String fechaFormateada = fechaSeleccionadaStr;
        try {
            SimpleDateFormat sdfIso = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            SimpleDateFormat sdfPretty = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            fechaFormateada = sdfPretty.format(sdfIso.parse(fechaSeleccionadaStr));
        } catch (Exception ignored) {}

        if (tvFechaSeleccionada != null) {
            tvFechaSeleccionada.setText("Eventos del " + fechaFormateada);
        }

        // 1. Obtener eventos guardados por el usuario
        List<EventoCalendario> eventosUsuario = calendarioDB != null ? calendarioDB.obtenerEventosPorFecha(fechaSeleccionadaStr, correoUsuario) : new ArrayList<>();

        // 2. Obtener actividades oficiales de la sección "Actividades"
        List<EventoCalendario> actividadesSeccion = actividadesDB != null ? actividadesDB.obtenerActividadesPorFecha(fechaSeleccionadaStr) : new ArrayList<>();

        // 3. Obtener citas activas de la sección "Secretaría"
        List<EventoCalendario> citasSecretaria = secretariaDB != null ? secretariaDB.obtenerCitasPorFecha(fechaSeleccionadaStr, correoUsuario) : new ArrayList<>();

        List<EventoCalendario> eventos = new ArrayList<>();
        eventos.addAll(eventosUsuario);
        eventos.addAll(actividadesSeccion);
        eventos.addAll(citasSecretaria);

        if (tvNumEventosFecha != null) {
            tvNumEventosFecha.setText(eventos.size() + (eventos.size() == 1 ? " evento" : " eventos"));
        }

        if (eventos.isEmpty()) {
            if (tvSinEventos != null) tvSinEventos.setVisibility(View.VISIBLE);
        } else {
            if (tvSinEventos != null) tvSinEventos.setVisibility(View.GONE);

            for (EventoCalendario ev : eventos) {
                MaterialCardView card = new MaterialCardView(this);
                LinearLayout.LayoutParams paramsCard = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                paramsCard.setMargins(0, 0, 0, 16);
                card.setLayoutParams(paramsCard);
                card.setRadius(24f);
                card.setCardElevation(2f);
                card.setStrokeWidth(1);
                card.setStrokeColor(Color.parseColor("#EAEAEA"));
                card.setCardBackgroundColor(Color.WHITE);

                LinearLayout rowLayout = new LinearLayout(this);
                rowLayout.setOrientation(LinearLayout.HORIZONTAL);
                rowLayout.setPadding(24, 20, 24, 20);
                rowLayout.setGravity(Gravity.CENTER_VERTICAL);

                TextView tvBadge = new TextView(this);
                boolean esActividadOficial = ev.id < 0 && ev.id > -20000;
                boolean esCitaSecretaria = ev.id <= -20000;

                if (esActividadOficial) {
                    tvBadge.setText("ACTIVIDAD ETSIIT");
                } else if (esCitaSecretaria) {
                    tvBadge.setText("CITA SECRETARÍA");
                } else {
                    tvBadge.setText(ev.tipo.toUpperCase());
                }

                tvBadge.setTextSize(10);
                tvBadge.setTypeface(null, Typeface.BOLD);
                tvBadge.setTextColor(Color.WHITE);
                tvBadge.setPadding(16, 8, 16, 8);

                int colorBadge = Color.parseColor("#FF8C42");
                if (esCitaSecretaria || "Cita".equalsIgnoreCase(ev.tipo)) {
                    colorBadge = Color.parseColor("#8E24AA"); // morado
                } else if (esActividadOficial || "Actividad".equalsIgnoreCase(ev.tipo)) {
                    colorBadge = Color.parseColor("#1E88E5"); // azul
                } else if ("Examen".equalsIgnoreCase(ev.tipo)) {
                    colorBadge = Color.parseColor("#E53935"); // rojo
                } else if ("Entrega".equalsIgnoreCase(ev.tipo)) {
                    colorBadge = Color.parseColor("#FB8C00"); // naranja
                } else if ("Otro".equalsIgnoreCase(ev.tipo)) {
                    colorBadge = Color.parseColor("#43A047"); // verde
                }

                GradientDrawable gd = new GradientDrawable();
                gd.setColor(colorBadge);
                gd.setCornerRadius(16);
                tvBadge.setBackground(gd);

                LinearLayout infoCol = new LinearLayout(this);
                infoCol.setOrientation(LinearLayout.VERTICAL);
                LinearLayout.LayoutParams infoParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
                infoParams.setMargins(20, 0, 16, 0);
                infoCol.setLayoutParams(infoParams);

                TextView tvTituloEv = new TextView(this);
                tvTituloEv.setText(ev.titulo);
                tvTituloEv.setTextSize(14);
                tvTituloEv.setTypeface(null, Typeface.BOLD);
                tvTituloEv.setTextColor(Color.parseColor("#1A1A1A"));

                String detalles = "";
                if (ev.hora != null && !ev.hora.isEmpty()) {
                    detalles += "🕒 " + ev.hora;
                }
                if (ev.descripcion != null && !ev.descripcion.isEmpty()) {
                    if (!detalles.isEmpty()) detalles += " · ";
                    detalles += ev.descripcion;
                }

                infoCol.addView(tvTituloEv);
                if (!detalles.isEmpty()) {
                    TextView tvDetallesEv = new TextView(this);
                    tvDetallesEv.setText(detalles);
                    tvDetallesEv.setTextSize(12);
                    tvDetallesEv.setTextColor(Color.parseColor("#666666"));
                    tvDetallesEv.setPadding(0, 4, 0, 0);
                    infoCol.addView(tvDetallesEv);
                }

                ImageButton btnAccion = new ImageButton(this);
                btnAccion.setBackgroundColor(Color.TRANSPARENT);

                if (esActividadOficial) {
                    btnAccion.setImageResource(android.R.drawable.ic_menu_info_details);
                    btnAccion.setColorFilter(Color.parseColor("#1E88E5"));
                    btnAccion.setContentDescription("Ver actividad");
                    btnAccion.setOnClickListener(v -> {
                        Intent intent = new Intent(MenuPrincipal.this, Actividades.class);
                        intent.putExtra("CORREO", correoUsuario);
                        startActivity(intent);
                    });
                } else if (esCitaSecretaria) {
                    btnAccion.setImageResource(android.R.drawable.ic_menu_info_details);
                    btnAccion.setColorFilter(Color.parseColor("#8E24AA"));
                    btnAccion.setContentDescription("Ver Secretaría");
                    btnAccion.setOnClickListener(v -> {
                        Intent intent = new Intent(MenuPrincipal.this, Secretaria.class);
                        intent.putExtra("CORREO", correoUsuario);
                        startActivity(intent);
                    });
                } else {
                    btnAccion.setImageResource(android.R.drawable.ic_menu_delete);
                    btnAccion.setColorFilter(Color.parseColor("#B0BEC5"));
                    btnAccion.setContentDescription("Eliminar evento");
                    btnAccion.setOnClickListener(v -> {
                        if (!usuarioEstaRegistrado()) {
                            verificarYMostrarDialogoAnadirEvento();
                            return;
                        }
                        new AlertDialog.Builder(MenuPrincipal.this)
                                .setTitle("Eliminar evento")
                                .setMessage("¿Deseas borrar '" + ev.titulo + "' de tu calendario?")
                                .setPositiveButton("Eliminar", (dialog, which) -> {
                                    calendarioDB.eliminarEvento(ev.id);
                                    Toast.makeText(MenuPrincipal.this, "Evento eliminado", Toast.LENGTH_SHORT).show();
                                    actualizarGridCalendario();
                                    cargarEventosFechaSeleccionada();
                                })
                                .setNegativeButton("Cancelar", null)
                                .show();
                    });
                }

                rowLayout.addView(tvBadge);
                rowLayout.addView(infoCol);
                rowLayout.addView(btnAccion);
                card.addView(rowLayout);

                llEventosContainer.addView(card);
            }
        }
    }

    private void mostrarDialogoAnadirEvento() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("➕ Añadir evento al Calendario");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(40, 20, 40, 10);

        TextView tvLabelTitulo = new TextView(this);
        tvLabelTitulo.setText("Título del evento / examen / entrega:");
        tvLabelTitulo.setTextSize(12);
        tvLabelTitulo.setTextColor(Color.parseColor("#666666"));
        tvLabelTitulo.setPadding(0, 8, 0, 4);

        final EditText etTitulo = new EditText(this);
        etTitulo.setHint("Ej: Examen de NPI, Entrega P1...");
        etTitulo.setTextSize(14);

        TextView tvLabelTipo = new TextView(this);
        tvLabelTipo.setText("Tipo:");
        tvLabelTipo.setTextSize(12);
        tvLabelTipo.setTextColor(Color.parseColor("#666666"));
        tvLabelTipo.setPadding(0, 16, 0, 4);

        final Spinner spTipo = new Spinner(this);
        String[] tipos = {"Examen", "Entrega", "Actividad", "Otro"};
        ArrayAdapter<String> adapterTipo = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, tipos);
        spTipo.setAdapter(adapterTipo);

        TextView tvLabelHora = new TextView(this);
        tvLabelHora.setText("Hora (opcional):");
        tvLabelHora.setTextSize(12);
        tvLabelHora.setTextColor(Color.parseColor("#666666"));
        tvLabelHora.setPadding(0, 16, 0, 4);

        final EditText etHora = new EditText(this);
        etHora.setHint("Ej: 10:30, 23:59");
        etHora.setTextSize(14);

        TextView tvLabelDesc = new TextView(this);
        tvLabelDesc.setText("Lugar o notas (opcional):");
        tvLabelDesc.setTextSize(12);
        tvLabelDesc.setTextColor(Color.parseColor("#666666"));
        tvLabelDesc.setPadding(0, 16, 0, 4);

        final EditText etDesc = new EditText(this);
        etDesc.setHint("Ej: Aula 1.1, Subir a PRADO...");
        etDesc.setTextSize(14);

        layout.addView(tvLabelTitulo);
        layout.addView(etTitulo);
        layout.addView(tvLabelTipo);
        layout.addView(spTipo);
        layout.addView(tvLabelHora);
        layout.addView(etHora);
        layout.addView(tvLabelDesc);
        layout.addView(etDesc);

        builder.setView(layout);

        builder.setPositiveButton("Guardar", (dialog, which) -> {
            String titulo = etTitulo.getText().toString().trim();
            if (titulo.isEmpty()) {
                Toast.makeText(MenuPrincipal.this, "Debes escribir un título para el evento", Toast.LENGTH_SHORT).show();
                return;
            }
            String tipo = spTipo.getSelectedItem().toString();
            String hora = etHora.getText().toString().trim();
            String desc = etDesc.getText().toString().trim();

            calendarioDB.insertarEvento(correoUsuario, titulo, tipo, fechaSeleccionadaStr, hora, desc);
            Toast.makeText(MenuPrincipal.this, "Evento guardado en el calendario", Toast.LENGTH_SHORT).show();
            actualizarGridCalendario();
            cargarEventosFechaSeleccionada();
        });

        builder.setNegativeButton("Cancelar", null);
        builder.show();
    }
}