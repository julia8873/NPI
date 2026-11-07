package com.example.nuietsiit.activities.secciones;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.widget.NestedScrollView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.nuietsiit.R;
import com.example.nuietsiit.activities.BaseActivity;
import com.example.nuietsiit.activities.secciones.secretaria.CitaPrevia;
import com.example.nuietsiit.activities.secciones.secretaria.DetalleTramite;
import com.example.nuietsiit.activities.secciones.secretaria.Justificante;
import com.example.nuietsiit.activities.secciones.secretaria.MisGestiones;
import com.example.nuietsiit.activities.secciones.secretaria.QueNecesitas;
import com.example.nuietsiit.activities.secciones.secretaria.TramiteAdapter;
import com.example.nuietsiit.data.DBHelper;
import com.example.nuietsiit.data.SecretariaDB;
import com.example.nuietsiit.data.UsuariosDB;
import com.example.nuietsiit.data.modelo.Cita;
import com.example.nuietsiit.data.modelo.Solicitud;
import com.example.nuietsiit.data.modelo.Tramite;
import com.example.nuietsiit.util.BuscadorTolerante;
import com.example.nuietsiit.util.FechasUtil;
import com.example.nuietsiit.util.SesionUtil;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


public class Secretaria extends BaseActivity {

    private static final String ESTADO_CATEGORIA = "categoria";
    private static final String ESTADO_CATALOGO = "catalogo";
    private static final int MAX_SOLICITUDES = 3;
    private static final int MAX_PLAZOS = 2;

    private SecretariaDB secretariaDB;
    private String correoUsuario;
    private String categoriaSeleccionada; // null = todas

    private EditText etBuscar;
    private ChipGroup chipsCategorias;
    private TextView tvMostrandoPara, tvVacio;
    private Chip chipSugerencia;
    private Button btnMisGestiones;
    private ViewGroup contenedorLoTuyo, grupoCatalogo;
    private TextView tvVerTodos;
    private boolean animarEntrada;
    private boolean tecladoPendiente; // abrir el teclado cuando la ventana recupere el foco

    // "Buscarlo yo" en las preguntas guiadas → abre aquí el catálogo con el buscador
    private final ActivityResultLauncher<Intent> lanzadorQueNecesitas = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), r -> {
                if (r.getData() != null && r.getData().getBooleanExtra(QueNecesitas.EXTRA_VER_CATALOGO, false)) {
                    abrirBuscador();
                }
            });
    private TramiteAdapter adaptador;
    private BuscadorTolerante.Indice<Tramite> indice;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.secretaria);

        // Botón atrás en la barra superior
        MaterialToolbar barra = findViewById(R.id.toolbar);
        setSupportActionBar(barra);
        getSupportActionBar().setTitle("Secretaría");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        correoUsuario = SesionUtil.correo(getIntent());
        String nombre = new UsuariosDB(DBHelper.getInstance(this)).obtenerNombreUsuario(correoUsuario);
        if (nombre != null && !nombre.trim().isEmpty()) {
            ((TextView) findViewById(R.id.tvSaludo)).setText("Hola, " + nombre.trim().split("\\s+")[0]);
        }
        secretariaDB = new SecretariaDB(DBHelper.getInstance(this));
        boolean catalogoVisible = false;
        if (savedInstanceState != null) {
            categoriaSeleccionada = savedInstanceState.getString(ESTADO_CATEGORIA);
            catalogoVisible = savedInstanceState.getBoolean(ESTADO_CATALOGO);
        }
        animarEntrada = savedInstanceState == null;

        etBuscar = findViewById(R.id.etBuscar);
        chipsCategorias = findViewById(R.id.chipsCategorias);
        tvMostrandoPara = findViewById(R.id.tvMostrandoPara);
        tvVacio = findViewById(R.id.tvVacio);
        chipSugerencia = findViewById(R.id.chipSugerencia);
        btnMisGestiones = findViewById(R.id.btnMisGestiones);
        contenedorLoTuyo = findViewById(R.id.contenedorLoTuyo);
        grupoCatalogo = findViewById(R.id.grupoCatalogo);
        tvVerTodos = findViewById(R.id.tvVerTodos);

        adaptador = new TramiteAdapter(this::abrirDetalle);
        RecyclerView rv = findViewById(R.id.rvTramites);
        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.setAdapter(adaptador);

        crearChipsCategorias();

        etBuscar.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void afterTextChanged(Editable s) { aplicarFiltro(); }
        });
        chipSugerencia.setOnClickListener(v -> etBuscar.setText(chipSugerencia.getTag().toString()));

        findViewById(R.id.btnPedirCita).setOnClickListener(v -> startActivity(
                new Intent(this, CitaPrevia.class).putExtra("CORREO", correoUsuario)));
        btnMisGestiones.setOnClickListener(v -> startActivity(
                new Intent(this, MisGestiones.class).putExtra("CORREO", correoUsuario)));

        tvVerTodos.setOnClickListener(v -> mostrarCatalogo(grupoCatalogo.getVisibility() != View.VISIBLE));
        findViewById(R.id.btnQueNecesitas).setOnClickListener(v -> lanzadorQueNecesitas.launch(
                new Intent(this, QueNecesitas.class).putExtra("CORREO", correoUsuario)));
        mostrarCatalogo(catalogoVisible);

        cargarCategoria();
    }

    @Override
    protected void onResume() {
        super.onResume();
        actualizarContadorGestiones();
        mostrarLoTuyo(); // el estado de citas y solicitudes cambia al volver de otras pantallas
    }

    @Override
    protected void onSaveInstanceState(Bundle salida) {
        super.onSaveInstanceState(salida);
        salida.putString(ESTADO_CATEGORIA, categoriaSeleccionada);
        salida.putBoolean(ESTADO_CATALOGO, grupoCatalogo.getVisibility() == View.VISIBLE);
    }

    private void mostrarCatalogo(boolean visible) {
        grupoCatalogo.setVisibility(visible ? View.VISIBLE : View.GONE);
        tvVerTodos.setText(visible ? "Ocultar el catálogo ▴" : "Ver todos los trámites ▾");
        if (visible) {
            // baja hasta el catálogo para que se vea que se ha abierto
            NestedScrollView scroll = findViewById(R.id.scroll);
            scroll.post(() -> scroll.smoothScrollTo(0, tvVerTodos.getTop()));
        }
    }

    private void abrirBuscador() {
        mostrarCatalogo(true);
        etBuscar.requestFocus();
        tecladoPendiente = true; // al volver de otra Activity la ventana aún no tiene el foco
    }

    @Override
    public void onWindowFocusChanged(boolean tieneFoco) {
        super.onWindowFocusChanged(tieneFoco);
        if (tieneFoco && tecladoPendiente) {
            tecladoPendiente = false;
            getSystemService(InputMethodManager.class).showSoftInput(etBuscar, InputMethodManager.SHOW_IMPLICIT);
        }
    }

    // ------------------------------------------------------------
    //   Lo tuyo: cita próxima, solicitudes en curso y plazos abiertos
    // ------------------------------------------------------------

    private void mostrarLoTuyo() {
        contenedorLoTuyo.removeAllViews();

        Cita cita = proximaCita();
        if (cita != null) {
            LocalDateTime cuando = cita.fechaHora();
            boolean hoy = FechasUtil.diasHasta(cuando.toLocalDate()) == 0;
            MaterialCardView tarjeta = anadirTarjeta("📅", "Tu próxima cita",
                    FechasUtil.cuando(cuando) + " a las " + FechasUtil.hora(cuando),
                    cita.motivo(),
                    "📱 En Secretaría, gira el móvil para enseñar el QR",
                    v -> startActivity(new Intent(this, Justificante.class)
                            .putExtra("idCita", cita.id).putExtra("CORREO", correoUsuario)));
            if (hoy) resaltar(tarjeta);
        }

        Set<Integer> tramitesEnCurso = new HashSet<>();
        int nSolicitudes = 0;
        for (Solicitud s : secretariaDB.getSolicitudes(correoUsuario)) {
            if (nSolicitudes == MAX_SOLICITUDES) break;
            String estado = s.estado();
            LocalDateTime envio = LocalDateTime.parse(s.fechaEnvio);
            boolean resuelta = Solicitud.RESUELTA.equals(estado);
            // las resueltas solo se recuerdan el primer día
            if (resuelta && Duration.between(envio, LocalDateTime.now()).toHours() >= 24) continue;
            if (!resuelta) tramitesEnCurso.add(s.idProcedimiento);

            MaterialCardView tarjeta = anadirTarjeta(resuelta ? "✅" : "📨",
                    "Solicitud · " + estado, s.nombreTramite,
                    "Enviada " + FechasUtil.hace(envio), null,
                    v -> startActivity(new Intent(this, Justificante.class)
                            .putExtra("idSolicitud", s.id).putExtra("CORREO", correoUsuario)));
            ((TextView) tarjeta.findViewById(R.id.tvEtiqueta)).setTextColor(Solicitud.colorEstado(estado));
            nSolicitudes++;
        }

        for (Tramite t : plazosAbiertos(tramitesEnCurso)) {
            long dias = FechasUtil.diasHasta(LocalDate.parse(t.plazoFin));
            String queda = dias == 0 ? "Último día" : dias == 1 ? "Queda 1 día" : "Quedan " + dias + " días";
            MaterialCardView tarjeta = anadirTarjeta("⏳", "Plazo abierto", t.nombre,
                    queda + " · hasta el " + FechasUtil.diaCorto(LocalDate.parse(t.plazoFin)), null,
                    v -> abrirDetalle(t));
            if (dias <= 3) resaltar(tarjeta);
        }

        if (contenedorLoTuyo.getChildCount() == 0) {
            MaterialCardView tarjeta = anadirTarjeta("🌿", "Todo en orden",
                    "No tienes nada pendiente", "Ni citas, ni solicitudes, ni plazos que se acaben",
                    "Si necesitas algo, pulsa «¿Qué necesitas?»", null);
            tarjeta.setClickable(false);
            tarjeta.findViewById(R.id.tvChevron).setVisibility(View.GONE);
        }

        if (animarEntrada) {
            animarEntrada = false;
            animarTarjetas();
        }
    }

    // la primera cita activa que aún no ha pasado (getCitas pone las futuras arriba)
    private Cita proximaCita() {
        List<Cita> citas = secretariaDB.getCitas(correoUsuario);
        return !citas.isEmpty() && citas.get(0).esFutura() ? citas.get(0) : null;
    }

    // trámites online con el plazo abierto, los que antes acaban primero (sin los que ya tienes en curso)
    private List<Tramite> plazosAbiertos(Set<Integer> excluir) {
        List<Tramite> abiertos = new ArrayList<>();
        for (Tramite t : secretariaDB.getTramites(null)) {
            if (t.tienePlazo() && t.sePuedeSolicitar() && !excluir.contains(t.id)) abiertos.add(t);
        }
        abiertos.sort(Comparator.comparing(t -> t.plazoFin));
        return abiertos.size() > MAX_PLAZOS ? abiertos.subList(0, MAX_PLAZOS) : abiertos;
    }

    private MaterialCardView anadirTarjeta(String icono, String etiqueta, String titulo, String detalle,
                                           String accion, View.OnClickListener alTocar) {
        MaterialCardView tarjeta = (MaterialCardView) LayoutInflater.from(this)
                .inflate(R.layout.item_tarjeta_lo_tuyo, contenedorLoTuyo, false);
        ((TextView) tarjeta.findViewById(R.id.tvIcono)).setText(icono);
        ((TextView) tarjeta.findViewById(R.id.tvEtiqueta)).setText(etiqueta);
        ((TextView) tarjeta.findViewById(R.id.tvTitulo)).setText(titulo);
        ((TextView) tarjeta.findViewById(R.id.tvDetalle)).setText(detalle);
        TextView tvAccion = tarjeta.findViewById(R.id.tvAccion);
        tvAccion.setText(accion); // solo para enseñar un gesto; que se puede tocar ya lo dice el ›
        tvAccion.setVisibility(accion != null ? View.VISIBLE : View.GONE);
        tarjeta.setOnClickListener(alTocar);
        contenedorLoTuyo.addView(tarjeta);
        return tarjeta;
    }

    // lo urgente (cita hoy, plazo que se acaba) con fondo y borde de acento
    private void resaltar(MaterialCardView tarjeta) {
        tarjeta.setCardBackgroundColor(getColor(R.color.nui_tinte));
        tarjeta.setStrokeColor(getColor(R.color.nui_acento));
    }

    // entran de abajo una tras otra, para que se vea que son "lo tuyo" de ahora
    private void animarTarjetas() {
        float desplazamiento = 24 * getResources().getDisplayMetrics().density;
        for (int i = 0; i < contenedorLoTuyo.getChildCount(); i++) {
            View v = contenedorLoTuyo.getChildAt(i);
            v.setAlpha(0f);
            v.setTranslationY(desplazamiento);
            v.animate().alpha(1f).translationY(0f).setStartDelay(80L * i).setDuration(260).start();
        }
    }

    private void crearChipsCategorias() {
        anadirChip(1000, null, "Todas");
        for (int i = 0; i < Tramite.CATEGORIAS.length; i++) {
            anadirChip(1001 + i, Tramite.CATEGORIAS[i][0], Tramite.CATEGORIAS[i][1]);
        }
        chipsCategorias.setOnCheckedStateChangeListener((grupo, ids) -> {
            if (ids.isEmpty()) return;
            categoriaSeleccionada = (String) grupo.findViewById(ids.get(0)).getTag();
            cargarCategoria();
        });
    }

    private void anadirChip(int id, String clave, String etiqueta) {
        Chip chip = new Chip(this, null, com.google.android.material.R.attr.chipStyle);
        chip.setId(id); // ids fijos para que sobrevivan a la rotación
        chip.setText(etiqueta);
        chip.setTag(clave);
        chip.setCheckable(true);
        chip.setChipBackgroundColorResource(R.color.chip_fondo);
        chip.setTextColor(getColorStateList(R.color.chip_texto));
        chip.setCheckedIconVisible(false);
        chipsCategorias.addView(chip);
        if (clave == null ? categoriaSeleccionada == null : clave.equals(categoriaSeleccionada)) {
            chip.setChecked(true);
        }
    }

    private void cargarCategoria() {
        indice = BuscadorTolerante.crearIndice(secretariaDB.getTramites(categoriaSeleccionada),
                t -> t.nombre, Tramite::textoBusqueda);
        aplicarFiltro();
    }

    // categoría (BD) AND texto (BuscadorTolerante, en cada pulsación)
    private void aplicarFiltro() {
        BuscadorTolerante.Busqueda<Tramite> r = indice.buscar(etBuscar.getText().toString());
        adaptador.setTramites(r.elementos);

        tvMostrandoPara.setVisibility(r.mostrandoPara != null ? View.VISIBLE : View.GONE);
        if (r.mostrandoPara != null) {
            tvMostrandoPara.setText("Mostrando resultados para «" + r.mostrandoPara + "»");
        }
        chipSugerencia.setVisibility(r.sugerencia != null ? View.VISIBLE : View.GONE);
        if (r.sugerencia != null) {
            chipSugerencia.setText("¿Quisiste decir «" + r.sugerencia + "»?");
            chipSugerencia.setTag(r.sugerencia);
        }
        tvVacio.setVisibility(r.elementos.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void actualizarContadorGestiones() {
        int n = secretariaDB.contarCitasActivas(correoUsuario);
        btnMisGestiones.setText(n > 0 ? "📂 Mis gestiones (" + n + ")" : "📂 Mis gestiones");
    }

    private void abrirDetalle(Tramite tramite) {
        Intent intent = new Intent(this, DetalleTramite.class);
        intent.putExtra("idProcedimiento", tramite.id);
        intent.putExtra("CORREO", correoUsuario);
        startActivity(intent);
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
