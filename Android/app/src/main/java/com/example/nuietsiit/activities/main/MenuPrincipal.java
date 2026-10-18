package com.example.nuietsiit.activities.main;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;


import com.example.nuietsiit.R;
import com.example.nuietsiit.activities.auth.Login;
import com.example.nuietsiit.activities.main.AjustesCuenta;
import com.example.nuietsiit.activities.secciones.Actividades;
import com.example.nuietsiit.activities.secciones.Asignaturas;
import com.example.nuietsiit.activities.secciones.Biblioteca;
import com.example.nuietsiit.activities.secciones.Comedores;
import com.example.nuietsiit.activities.secciones.Mapa;
import com.example.nuietsiit.activities.secciones.Noticias;
import com.example.nuietsiit.activities.secciones.Profesorado;
import com.example.nuietsiit.activities.secciones.Secretaria;
import com.example.nuietsiit.data.DBHelper;
import com.example.nuietsiit.util.Notificador;

import java.util.ArrayList;
import java.util.List;

public class MenuPrincipal extends AppCompatActivity {

    // Componentes de la interfaz de usuario
    private TextView tvBienvenida;
    private ImageView ivIcono;
    private Button btnMapa, btnActividades, btnAsignaturas, btnProfesorado, btnSecretaria, btnComedores, btnBiblioteca;

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

        // Leer sesión de SharedPreferences
        SharedPreferences prefs = getSharedPreferences("SesionUsuario", MODE_PRIVATE);
        correoUsuario = prefs.getString("CORREO", null); // Si no hay nadie logueado, será null (invitado)

        // Obtención de datos pasados mediante Intent
        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("CORREO")) {
            correoUsuario = intent.getStringExtra("CORREO");
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

    /**
     * Asigna las vistas y listeners a todos los botones del menú.
     */
    private void inicializarBotonesNavegacion() {
        // Clic en la foto/icono de perfil
        ivIcono.setOnClickListener(v -> {
            if (correoUsuario != null && !correoUsuario.isEmpty()) { //Nota: Creo que estaba la lógica al revés, he intercambiado bloques if y else
                // Usuario LOGUEADO: Vamos a la pantalla de Ajustes de Cuenta
                Intent intentAjustes = new Intent(MenuPrincipal.this, AjustesCuenta.class);
                intentAjustes.putExtra("CORREO", correoUsuario);
                startActivity(intentAjustes);
            } else {
                // Modo INVITADO: Vamos a la pantalla de Login para que inicie sesión
                Intent intentLogin = new Intent(MenuPrincipal.this, Login.class);
                startActivity(intentLogin);
            }
        });

        // Botones de secciones principales
        btnMapa = findViewById(R.id.btnMapa);
        btnActividades = findViewById(R.id.btnActividades);
        btnAsignaturas = findViewById(R.id.btnAsignaturas);
        btnProfesorado = findViewById(R.id.btnProfesorado);
        btnSecretaria = findViewById(R.id.btnSecretaria);
        btnComedores = findViewById(R.id.btnComedores);
        btnBiblioteca = findViewById(R.id.btnBiblioteca);

        btnMapa.setOnClickListener(view -> {
            Intent intent = new Intent(MenuPrincipal.this, Mapa.class);
            intent.putExtra("CORREO", correoUsuario);
            startActivity(intent);
        });
        btnActividades.setOnClickListener(view -> {
            Intent intent = new Intent(MenuPrincipal.this, Actividades.class);
            intent.putExtra("CORREO", correoUsuario);
            startActivity(intent);
        });
        btnAsignaturas.setOnClickListener(view -> {
            Intent intent = new Intent(MenuPrincipal.this, Asignaturas.class);
            intent.putExtra("CORREO", correoUsuario);
            startActivity(intent);
        });
        btnProfesorado.setOnClickListener(view -> {
            Intent intent = new Intent(MenuPrincipal.this, Profesorado.class);
            intent.putExtra("CORREO", correoUsuario);
            startActivity(intent);
        });
        btnSecretaria.setOnClickListener(view -> {
            Intent intent = new Intent(MenuPrincipal.this, Secretaria.class);
            intent.putExtra("CORREO", correoUsuario);
            startActivity(intent);
        });
        btnComedores.setOnClickListener(view -> {
            Intent intent = new Intent(MenuPrincipal.this, Comedores.class);
            intent.putExtra("CORREO", correoUsuario);
            startActivity(intent);
        });
        btnBiblioteca.setOnClickListener(view -> {
            Intent intent = new Intent(MenuPrincipal.this, Biblioteca.class);
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
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Reanuda la animación del slider cuando la actividad regresa al primer plano
        sliderHandler.postDelayed(sliderRunnable, 3000);
    }
}