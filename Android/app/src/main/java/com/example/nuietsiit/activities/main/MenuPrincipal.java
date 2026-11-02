package com.example.nuietsiit.activities.main;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import android.os.Handler;
import android.os.Looper;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import androidx.appcompat.app.AlertDialog;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

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
import com.example.nuietsiit.data.AnunciosDB;
import com.example.nuietsiit.data.AnunciosLeidos;
import com.example.nuietsiit.data.CalendarioDB;
import com.example.nuietsiit.data.DBHelper;
import com.example.nuietsiit.data.SecretariaDB;
import com.example.nuietsiit.data.modelo.Anuncio;
import com.example.nuietsiit.data.modelo.EventoCalendario;
import com.example.nuietsiit.util.DetectorInclinacionMenu;
import com.example.nuietsiit.util.FechasUtil;
import com.example.nuietsiit.util.Notificador;
import com.google.android.material.card.MaterialCardView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Menú principal (rediseño minimalista).
 *
 * Inclinando el móvil se va seleccionando una sección de la cuadrícula 3x3 (la bola de la cabecera
 * muestra la inclinación: inclinar hacia uno mismo baja de fila, alejarlo sube). Para entrar se agita
 * el móvil. Tocar una ficha también la abre.
 */
public class MenuPrincipal extends BaseActivity {

    /** Mismo orden que HubSeccionesView.SECCIONES. */
    private static final Class<?>[] DESTINOS = {
            Noticias.class,
            Mapa.class,
            Actividades.class,
            Profesorado.class,
            Asignaturas.class,
            Secretaria.class,
            Comedores.class,
            Biblioteca.class,
            Calendario.class
    };
    private static final int INDICE_AVISOS = 0;

    private TextView tvHoraEvento, tvTituloEventoHoy, tvSubtituloEventoHoy;
    private ImageView ivIcono;
    private View cardMascotaAyudante;
    private MaterialCardView cardEventoHoy;
    private HubSeccionesView hub;
    private NivelInclinacionView nivel;
    private DetectorInclinacionMenu detector;

    private CalendarioDB calendarioDB;
    private ActividadesDB actividadesDB;
    private SecretariaDB secretariaDB;
    private AnunciosDB anunciosDB;

    private DBHelper BD;
    private String correoUsuario;
    private boolean abriendo;
    private boolean giroscopioHabilitado = true;
    private ViewPager2 vpNoticias;
    private final Handler sliderHandler = new Handler(Looper.getMainLooper());
    private List<Integer> listaNoticias;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.menu_principal);


        BD = DBHelper.getInstance(this);
        calendarioDB = new CalendarioDB(BD);
        actividadesDB = new ActividadesDB(BD);
        secretariaDB = new SecretariaDB(BD);
        anunciosDB = new AnunciosDB(BD);
        secretariaDB.poblarCitasDeEjemploSiVacio();

        SharedPreferences prefsConfig = getSharedPreferences("ConfiguracionApp", MODE_PRIVATE);
        giroscopioHabilitado = prefsConfig.getBoolean("GIROSCOPIO_HABILITADO", true);

        SharedPreferences prefs = getSharedPreferences("SesionUsuario", MODE_PRIVATE);
        correoUsuario = prefs.getString("CORREO", null);

        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("CORREO")) {
            correoUsuario = intent.getStringExtra("CORREO");
            prefs.edit().putString("CORREO", correoUsuario).apply();
        }

        ivIcono = findViewById(R.id.ivIcono);
        cardMascotaAyudante = findViewById(R.id.cardMascotaAyudante);
        cardEventoHoy = findViewById(R.id.cardEventoHoy);
        vpNoticias = findViewById(R.id.vpNoticias);
        configurarSliderNoticias();
        tvHoraEvento = findViewById(R.id.tvHoraEvento);
        tvTituloEventoHoy = findViewById(R.id.tvTituloEventoHoy);
        tvSubtituloEventoHoy = findViewById(R.id.tvSubtituloEventoHoy);
        hub = findViewById(R.id.hubSecciones);
        nivel = findViewById(R.id.nivelInclinacion);

        aplicarMargenesSistema();
        configurarPerfilClick();
        configurarMascotaClick();
        configurarHub();
        configurarInclinacion();

        Notificador.revisarAvisos(this, correoUsuario);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
    }

        /** Deja el contenido fuera de la barra de estado y de navegación (pantalla de borde a borde). */
    private void aplicarMargenesSistema() {
        View contenido = findViewById(R.id.contenidoMenu);
        ViewCompat.setOnApplyWindowInsetsListener(contenido, (v, insets) -> {
            Insets barras = insets.getInsets(
                    WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
            v.setPadding(barras.left, barras.top, barras.right, barras.bottom);
            return insets;
        });
        ViewCompat.requestApplyInsets(contenido);
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

    // -------------------------------------------------
    //              HUB + INCLINACIÓN
    // -------------------------------------------------

    private void configurarHub() {
        hub.setOnSeccionListener(indice -> {
            // un toque también deja la selección donde se ha tocado
            detector.fijarSeleccion(indice / 3, indice % 3);
            nivel.setZona(indice / 3, indice % 3);
            abrirSeccion(indice);
        });
    }

    private void configurarInclinacion() {
        nivel.setOnClickListener(v -> {
            v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
            mostrarBottomSheetGiroscopio();
        });

        actualizarEstadoVisualGiroscopio();

        detector = new DetectorInclinacionMenu(this, new DetectorInclinacionMenu.Listener() {
            @Override
            public void alMoverBola(float x, float y) {
                nivel.setInclinacion(x, y);
            }

            @Override
            public void alCambiarSeleccion(int fila, int columna) {
                hub.setSeleccion(fila * 3 + columna, true);
                nivel.setZona(fila, columna);
                hub.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);
            }

            @Override
            public void alEstarListo(boolean listo) {
                // Desactivado: no hay estado listo de vertical a horizontal
            }

            @Override
            public void alConfirmar() {
                // Desactivado: solo se entra agitando el móvil
            }
        });
    }

    @Override
    protected void onShakeMenuPrincipal() {
        vibrarConfirmacion();
        abrirSeccion(hub.getSeleccion());
    }

    private void mostrarBottomSheetGiroscopio() {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View view = getLayoutInflater().inflate(R.layout.bottom_sheet_giroscopio, null);
        dialog.setContentView(view);

        View btnActivado = view.findViewById(R.id.btnGiroscopioActivado);
        View btnDesactivado = view.findViewById(R.id.btnGiroscopioDesactivado);
        TextView badgeActivado = view.findViewById(R.id.badgeGiroscopioActivado);
        TextView badgeDesactivado = view.findViewById(R.id.badgeGiroscopioDesactivado);

        actualizarVistasDialogoGiroscopio(btnActivado, btnDesactivado, badgeActivado, badgeDesactivado);

        btnActivado.setOnClickListener(v -> {
            v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
            giroscopioHabilitado = true;
            guardarEstadoGiroscopio(true);
            actualizarVistasDialogoGiroscopio(btnActivado, btnDesactivado, badgeActivado, badgeDesactivado);
            actualizarEstadoVisualGiroscopio();
            actualizarEstadoVisualGiroscopio();
        if (giroscopioHabilitado) {
            detector.iniciar();
        } else {
            detector.parar();
        }
            Toast.makeText(this, "🧭 Sensor de inclinación activado", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });

        btnDesactivado.setOnClickListener(v -> {
            v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
            giroscopioHabilitado = false;
            guardarEstadoGiroscopio(false);
            actualizarVistasDialogoGiroscopio(btnActivado, btnDesactivado, badgeActivado, badgeDesactivado);
            actualizarEstadoVisualGiroscopio();
            detector.parar();
            Toast.makeText(this, "🛑 Sensor de inclinación desactivado", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });

        dialog.show();
    }

    private void actualizarVistasDialogoGiroscopio(View btnActivado, View btnDesactivado, TextView badgeActivado, TextView badgeDesactivado) {
        if (giroscopioHabilitado) {
            btnActivado.setBackgroundResource(R.drawable.fondo_opcion_menu_activa);
            badgeActivado.setVisibility(View.VISIBLE);
            btnDesactivado.setBackgroundResource(R.drawable.fondo_opcion_menu_inactiva);
            badgeDesactivado.setVisibility(View.GONE);
        } else {
            btnDesactivado.setBackgroundResource(R.drawable.fondo_opcion_menu_activa);
            badgeDesactivado.setVisibility(View.VISIBLE);
            btnActivado.setBackgroundResource(R.drawable.fondo_opcion_menu_inactiva);
            badgeActivado.setVisibility(View.GONE);
        }
    }

    private void actualizarEstadoVisualGiroscopio() {
        if (nivel != null) {
            nivel.setAlpha(giroscopioHabilitado ? 1.0f : 0.35f);
        }
    }

    private void guardarEstadoGiroscopio(boolean habilitado) {
        getSharedPreferences("ConfiguracionApp", MODE_PRIVATE)
                .edit()
                .putBoolean("GIROSCOPIO_HABILITADO", habilitado)
                .apply();
    }

    private void vibrarConfirmacion() {
        hub.performHapticFeedback(Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
                ? HapticFeedbackConstants.CONFIRM
                : HapticFeedbackConstants.VIRTUAL_KEY);
    }

    private void abrirSeccion(int indice) {
        if (abriendo || indice < 0 || indice >= DESTINOS.length) return;
        abriendo = true;
        hub.setSeleccion(indice, true);
        hub.animarApertura(indice, () -> {
            Intent intent = new Intent(MenuPrincipal.this, DESTINOS[indice]);
            intent.putExtra("CORREO", correoUsuario);
            startActivity(intent);
        });
    }

    // -------------------------------------------------
    //              DATOS DE LA PANTALLA
    // -------------------------------------------------

    private void actualizarAvisosSinLeer() {
        List<Anuncio> todos = anunciosDB.obtenerAnunciosGenerales(FechasUtil.ahoraBD());
        Set<String> leidos = AnunciosLeidos.leidos(this);
        int sinLeer = 0;
        for (Anuncio a : todos) {
            if (!leidos.contains(String.valueOf(a.id))) sinLeer++;
        }
        hub.setBadge(INDICE_AVISOS, sinLeer);
    }

    private void configurarSliderNoticias() {
        if (vpNoticias == null) return;
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
                sliderHandler.postDelayed(sliderRunnable, 3500);
            }
        });
    }

    private final Runnable sliderRunnable = new Runnable() {
        @Override
        public void run() {
            if (listaNoticias != null && !listaNoticias.isEmpty() && vpNoticias != null) {
                int siguientePosicion = (vpNoticias.getCurrentItem() + 1) % listaNoticias.size();
                vpNoticias.setCurrentItem(siguientePosicion, true);
            }
        }
    };

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

        tvHoraEvento.setText("HOY");
        if (!todosEventosHoy.isEmpty()) {
            EventoCalendario eventoProximo = todosEventosHoy.get(0);
            boolean hayHora = eventoProximo.hora != null && !eventoProximo.hora.isEmpty();
            boolean hayDescripcion = eventoProximo.descripcion != null && !eventoProximo.descripcion.isEmpty();
            tvTituloEventoHoy.setText(eventoProximo.titulo);
            if (hayHora && hayDescripcion) {
                tvSubtituloEventoHoy.setText(eventoProximo.hora + " · " + eventoProximo.descripcion);
            } else if (hayHora) {
                tvSubtituloEventoHoy.setText(eventoProximo.hora + " · Calendario");
            } else {
                tvSubtituloEventoHoy.setText(hayDescripcion ? eventoProximo.descripcion : "Calendario");
            }
        } else {
            tvTituloEventoHoy.setText("Calendario de la ETSIIT");
            tvSubtituloEventoHoy.setText("Actividades, citas, exámenes y entregas");
        }

        cardEventoHoy.setOnClickListener(v -> abrirSeccion(DESTINOS.length - 1));
    }

    @Override
    protected void onPause() {
        super.onPause();
        sliderHandler.removeCallbacks(sliderRunnable);
        detector.parar();
    }

    @Override
    protected void onResume() {
        super.onResume();
        abriendo = false;
        sliderHandler.removeCallbacks(sliderRunnable);
        sliderHandler.postDelayed(sliderRunnable, 3500);
        hub.restaurar();
        nivel.setListo(false);
        cargarEventoHoy();
        actualizarAvisosSinLeer();
        actualizarEstadoVisualGiroscopio();
        if (giroscopioHabilitado) {
            detector.iniciar();
        } else {
            detector.parar();
        }
    }
}
