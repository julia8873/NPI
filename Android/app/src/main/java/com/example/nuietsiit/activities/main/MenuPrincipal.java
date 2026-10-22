package com.example.nuietsiit.activities.main;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
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
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;


import com.example.nuietsiit.R;
import com.example.nuietsiit.activities.auth.Login;
import com.example.nuietsiit.activities.secciones.Actividades;
import com.example.nuietsiit.activities.secciones.Asignaturas;
import com.example.nuietsiit.activities.secciones.Biblioteca;
import com.example.nuietsiit.activities.secciones.Comedores;
import com.example.nuietsiit.activities.secciones.Mapa;
import com.example.nuietsiit.activities.secciones.Noticias;
import com.example.nuietsiit.activities.secciones.Profesorado;
import com.example.nuietsiit.activities.secciones.Secretaria;
import com.example.nuietsiit.data.DBHelper;
import com.example.nuietsiit.data.UsuariosDB;
import com.example.nuietsiit.util.DetectorInclinacion;
import com.example.nuietsiit.util.Notificador;
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.List;

public class MenuPrincipal extends AppCompatActivity {

    // Componentes de la interfaz de usuario
    private TextView tvBienvenida, tvAnteriorNombre, tvSiguienteNombre;
    private ImageView ivIcono;
    private Button btnSeccionGrande;
    private ImageButton btnAnteriorSeccion, btnSiguienteSeccion;
    private DetectorInclinacion detectorInclinacion;
    private int indiceSeccionActual = 0;
    private boolean fueDeslizamiento = false;
    private final String[] nombresSecciones = {
            "Mapa", "Actividades", "Asignaturas", "Profesorado", "Secretaría", "Comedores", "Biblioteca"
    };

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

        // Notificaciones del sistema: avisos urgentes y actividades de hoy/mañana (Miguel)
        Notificador.revisarAvisos(this, correoUsuario);
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
        if (btnSeccionGrande != null) {
            btnSeccionGrande.setText(nombresSecciones[indiceSeccionActual]);
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
        listaNoticias.add(R.drawable.noticia5);
        listaNoticias.add(R.drawable.noticia3);
        listaNoticias.add(R.drawable.noticia6);
        listaNoticias.add(R.drawable.noticia2);
        listaNoticias.add(R.drawable.noticia7);
        listaNoticias.add(R.drawable.noticia4);
        listaNoticias.add(R.drawable.noticia8);

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
                // Al pulsar una noticia del carrusel se abre la pantalla de Noticias (Miguel)
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
                sliderHandler.postDelayed(sliderRunnable, 3000); // 3 segundos
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
}