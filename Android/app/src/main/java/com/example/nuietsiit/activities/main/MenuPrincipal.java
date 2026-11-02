package com.example.nuietsiit.activities.main;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.example.nuietsiit.R;
import com.example.nuietsiit.activities.BaseActivity;
import com.example.nuietsiit.activities.auth.Login;
import com.example.nuietsiit.activities.secciones.Actividades;
import com.example.nuietsiit.activities.secciones.Asignaturas;
import com.example.nuietsiit.activities.secciones.Biblioteca;
import com.example.nuietsiit.activities.secciones.Calendario;
import com.example.nuietsiit.activities.secciones.Comedores;
import com.example.nuietsiit.activities.secciones.Noticias;
import com.example.nuietsiit.activities.secciones.Profesorado;
import com.example.nuietsiit.activities.secciones.Secretaria;
import com.example.nuietsiit.activities.secciones.mapa.Mapa;
import com.example.nuietsiit.data.ActividadesDB;
import com.example.nuietsiit.data.CalendarioDB;
import com.example.nuietsiit.data.DBHelper;
import com.example.nuietsiit.data.SecretariaDB;
import com.example.nuietsiit.data.modelo.EventoCalendario;
import com.example.nuietsiit.util.Notificador;
import com.google.android.material.card.MaterialCardView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MenuPrincipal extends BaseActivity {

    private TextView tvHoraEvento, tvTituloEventoHoy, tvSubtituloEventoHoy;
    private ImageView ivIcono;
    private View cardMascotaAyudante;
    private MaterialCardView cardEventoHoy;
    private View btnAbrirCalendario;

    private CalendarioDB calendarioDB;
    private ActividadesDB actividadesDB;
    private SecretariaDB secretariaDB;

    private ViewPager2 vpNoticias;
    private final Handler sliderHandler = new Handler(Looper.getMainLooper());
    private List<Integer> listaNoticias;

    private DBHelper BD;
    private String correoUsuario;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.menu_principal);

        comprobarAnimacionOla(getIntent());

        BD = DBHelper.getInstance(this);
        calendarioDB = new CalendarioDB(BD);
        actividadesDB = new ActividadesDB(BD);
        secretariaDB = new SecretariaDB(BD);
        secretariaDB.poblarCitasDeEjemploSiVacio();

        SharedPreferences prefs = getSharedPreferences("SesionUsuario", MODE_PRIVATE);
        correoUsuario = prefs.getString("CORREO", null);

        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("CORREO")) {
            correoUsuario = intent.getStringExtra("CORREO");
            prefs.edit().putString("CORREO", correoUsuario).apply();
        }

        ivIcono = findViewById(R.id.ivIcono);
        cardMascotaAyudante = findViewById(R.id.cardMascotaAyudante);
        vpNoticias = findViewById(R.id.vpNoticias);
        cardEventoHoy = findViewById(R.id.cardEventoHoy);
        btnAbrirCalendario = findViewById(R.id.btnAbrirCalendario);
        tvHoraEvento = findViewById(R.id.tvHoraEvento);
        tvTituloEventoHoy = findViewById(R.id.tvTituloEventoHoy);
        tvSubtituloEventoHoy = findViewById(R.id.tvSubtituloEventoHoy);

        configurarPerfilClick();
        configurarMascotaClick();
        configurarSliderNoticias();
        configurarBotonesSecciones();
        cargarEventoHoy();

        Notificador.revisarAvisos(this, correoUsuario);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        comprobarAnimacionOla(intent);
    }

    private void comprobarAnimacionOla(Intent intent) {
        if (intent != null && intent.getBooleanExtra("START_WAVE_DOWN", false)) {
            intent.removeExtra("START_WAVE_DOWN");
            WaveView waveView = findViewById(R.id.waveView);
            if (waveView != null) {
                waveView.startWaveDown(null);
            }
        }
    }

    private void configurarPerfilClick() {
        ivIcono.setOnClickListener(v -> {
            if (correoUsuario == null || correoUsuario.isEmpty()) {
                Intent intent = new Intent(MenuPrincipal.this, Login.class);
                startActivity(intent);
            } else {
                Intent intent = new Intent(MenuPrincipal.this, AjustesCuenta.class);
                intent.putExtra("CORREO", correoUsuario);
                startActivity(intent);
            }
        });
    }

    private void configurarMascotaClick() {
        if (cardMascotaAyudante != null) {
            cardMascotaAyudante.setOnClickListener(v -> {
                v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
                new AlertDialog.Builder(MenuPrincipal.this)
                        .setIcon(R.drawable.ic_mascota_ayudante)
                        .setTitle("Asistente Virtual ETSIIT 🤖")
                        .setMessage("¡Hola! En el futuro seré tu asistente inteligente con IA para resolver todas tus dudas sobre la ETSIIT, tus clases, entregas y la escuela.\n\nPor ahora puedes usar los accesos rápidos para navegar por las secciones y el calendario para tus exámenes.")
                        .setPositiveButton("¡Entendido!", null)
                        .show();
            });
        }
    }

    private void configurarBotonesSecciones() {
        findViewById(R.id.btnSeccionMapa).setOnClickListener(v -> abrirSeccion(0));
        findViewById(R.id.btnSeccionActividades).setOnClickListener(v -> abrirSeccion(1));
        findViewById(R.id.btnSeccionAsignaturas).setOnClickListener(v -> abrirSeccion(2));
        findViewById(R.id.btnSeccionProfesorado).setOnClickListener(v -> abrirSeccion(3));
        findViewById(R.id.btnSeccionSecretaria).setOnClickListener(v -> abrirSeccion(4));
        findViewById(R.id.btnSeccionComedores).setOnClickListener(v -> abrirSeccion(5));
        findViewById(R.id.btnSeccionBiblioteca).setOnClickListener(v -> abrirSeccion(6));
    }

    private void abrirSeccion(int posicion) {
        Intent intent;
        switch (posicion) {
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
    }

    private void abrirCalendario() {
        getWindow().getDecorView().performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
        Intent intent = new Intent(MenuPrincipal.this, Calendario.class);
        intent.putExtra("CORREO", correoUsuario);
        startActivity(intent);
    }

    private void cargarEventoHoy() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String fechaHoyStr = sdf.format(new Date());

        List<EventoCalendario> eventosUsuario = calendarioDB.obtenerEventosPorFecha(fechaHoyStr, correoUsuario);
        List<EventoCalendario> actividadesOficiales = actividadesDB.obtenerActividadesPorFecha(fechaHoyStr);
        List<EventoCalendario> citasSecretaria = secretariaDB.obtenerCitasPorFecha(fechaHoyStr, correoUsuario);

        List<EventoCalendario> todosEventosHoy = new ArrayList<>();
        todosEventosHoy.addAll(eventosUsuario);
        todosEventosHoy.addAll(actividadesOficiales);
        todosEventosHoy.addAll(citasSecretaria);

        if (!todosEventosHoy.isEmpty()) {
            EventoCalendario eventoProximo = todosEventosHoy.get(0);
            tvHoraEvento.setText("HOY" + (eventoProximo.hora != null && !eventoProximo.hora.isEmpty() ? " • " + eventoProximo.hora : ""));
            tvTituloEventoHoy.setText(eventoProximo.titulo);
            tvSubtituloEventoHoy.setText(eventoProximo.descripcion != null && !eventoProximo.descripcion.isEmpty()
                    ? eventoProximo.descripcion


                    : "Toca para abrir el calendario completo 📅");
        } else {
            tvHoraEvento.setText("HOY • CALENDARIO");
            tvTituloEventoHoy.setText("Calendario de la ETSIIT");
            tvSubtituloEventoHoy.setText("Actividades, citas y anota tus exámenes y entregas 📅");
        }

        if (btnAbrirCalendario != null) {
            btnAbrirCalendario.setOnClickListener(v -> abrirCalendario());
        }

        cardEventoHoy.setOnClickListener(v -> abrirCalendario());
    }

    private void configurarSliderNoticias() {
        listaNoticias = new ArrayList<>();
        listaNoticias.add(R.drawable.noticia1);
        listaNoticias.add(R.drawable.noticia2);
        listaNoticias.add(R.drawable.noticia3);
        listaNoticias.add(R.drawable.noticia4);
        listaNoticias.add(R.drawable.noticia5);
        listaNoticias.add(R.drawable.noticia6);

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

        vpNoticias.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                sliderHandler.removeCallbacks(sliderRunnable);
                sliderHandler.postDelayed(sliderRunnable, 4000);
            }
        });
    }

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
    protected void onPause() {
        super.onPause();
        sliderHandler.removeCallbacks(sliderRunnable);
    }

    @Override
    protected void onResume() {
        super.onResume();
        sliderHandler.postDelayed(sliderRunnable, 3000);
        cargarEventoHoy();
    }
}