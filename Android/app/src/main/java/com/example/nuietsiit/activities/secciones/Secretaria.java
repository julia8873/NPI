package com.example.nuietsiit.activities.secciones;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.transition.AutoTransition;
import android.transition.TransitionManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.OneShotPreDrawListener;

import com.example.nuietsiit.ui.CabeceraNui;
import com.example.nuietsiit.R;
import com.example.nuietsiit.activities.secciones.secretaria.PantallaSecretaria;
import com.example.nuietsiit.activities.secciones.secretaria.CitaPrevia;
import com.example.nuietsiit.activities.secciones.secretaria.DetalleTramite;
import com.example.nuietsiit.activities.secciones.secretaria.FicheroPaginado;
import com.example.nuietsiit.ui.InclinarParaPasar;
import com.example.nuietsiit.ui.PestanasPapel;
import com.example.nuietsiit.activities.secciones.secretaria.Justificante;
import com.example.nuietsiit.activities.secciones.secretaria.MisGestiones;
import com.example.nuietsiit.activities.secciones.secretaria.QueNecesitas;
import com.example.nuietsiit.activities.secciones.secretaria.TramiteAdapter;
import com.example.nuietsiit.data.DBHelper;
import com.example.nuietsiit.data.SecretariaDB;
import com.example.nuietsiit.data.modelo.Cita;
import com.example.nuietsiit.data.modelo.Solicitud;
import com.example.nuietsiit.data.modelo.Tramite;
import com.example.nuietsiit.util.BuscadorTolerante;
import com.example.nuietsiit.util.FechasUtil;
import com.example.nuietsiit.util.SesionUtil;
import com.example.nuietsiit.util.tutorial.Gesto;
import com.example.nuietsiit.util.tutorial.Tutorial;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.chip.Chip;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


public class Secretaria extends PantallaSecretaria {

    private static final String ESTADO_CATEGORIA = "categoria";
    private static final String ESTADO_CATALOGO = "catalogo";
    private static final int MAX_SOLICITUDES = 3;
    private static final int MAX_PLAZOS = 2;

    private SecretariaDB secretariaDB;
    private String correoUsuario;
    private String categoriaSeleccionada; // null = todas

    private EditText etBuscar;
    private PestanasPapel pestanasCategorias;
    private FicheroPaginado fichero;
    private InclinarParaPasar inclinar;
    private TextView tvMostrandoPara, tvVacio;
    private Chip chipSugerencia;
    private TextView tvMisGestiones;
    private ViewGroup contenedorLoTuyo, grupoCatalogo, grupoMostrador;
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
        if (barra != null) {
            setSupportActionBar(barra);
            CabeceraNui.preparar(barra);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle("Secretaría");
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            }
        }

        correoUsuario = SesionUtil.correo(getIntent());
        secretariaDB = new SecretariaDB(DBHelper.getInstance(this));
        boolean catalogoVisible = false;
        if (savedInstanceState != null) {
            categoriaSeleccionada = savedInstanceState.getString(ESTADO_CATEGORIA);
            catalogoVisible = savedInstanceState.getBoolean(ESTADO_CATALOGO);
        }
        animarEntrada = savedInstanceState == null;

        etBuscar = findViewById(R.id.etBuscar);
        pestanasCategorias = findViewById(R.id.pestanasCategorias);
        tvMostrandoPara = findViewById(R.id.tvMostrandoPara);
        tvVacio = findViewById(R.id.tvVacio);
        chipSugerencia = findViewById(R.id.chipSugerencia);
        tvMisGestiones = findViewById(R.id.tvMisGestiones);
        contenedorLoTuyo = findViewById(R.id.contenedorLoTuyo);
        grupoCatalogo = findViewById(R.id.grupoCatalogo);
        grupoMostrador = findViewById(R.id.grupoMostrador);
        tvVerTodos = findViewById(R.id.tvVerTodos);

        adaptador = new TramiteAdapter(this::abrirDetalle);
        fichero = findViewById(R.id.fichero);
        View grupoPaginas = findViewById(R.id.grupoPaginas);
        TextView tvPagina = findViewById(R.id.tvPagina);
        fichero.setAdaptador(adaptador, (pagina, paginas) -> {
            int n = adaptador.getItemCount();
            String cuantos = n == 1 ? "1 trámite" : n + " trámites";
            boolean varias = paginas > 1;
            tvPagina.setText(varias ? cuantos + " · " + (pagina + 1) + " de " + paginas + " · desliza o inclina" : cuantos);
            View atras = findViewById(R.id.btnFichasAtras), adelante = findViewById(R.id.btnFichasAdelante);
            atras.setVisibility(varias ? View.VISIBLE : View.INVISIBLE);
            adelante.setVisibility(varias ? View.VISIBLE : View.INVISIBLE);
            atras.setAlpha(pagina > 0 ? 1f : 0.3f);
            adelante.setAlpha(pagina < paginas - 1 ? 1f : 0.3f);
            grupoPaginas.setVisibility(View.VISIBLE);
            if (inclinar != null) inclinar.actualizar();
        });
        // inclinar el móvil pasa las fichas, como en los pasos de un trámite
        inclinar = new InclinarParaPasar(fichero, fichero::pasar, () -> fichero.getPaginas() > 1);
        findViewById(R.id.btnFichasAtras).setOnClickListener(v -> fichero.pasar(-1));
        findViewById(R.id.btnFichasAdelante).setOnClickListener(v -> fichero.pasar(1));

        crearPestanas();

        etBuscar.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void afterTextChanged(Editable s) {
                aplicarFiltro();
                if (s.length() >= 3) avisarTutorial(Gesto.ESCRIBIR);
            }
        });
        chipSugerencia.setOnClickListener(v -> etBuscar.setText(chipSugerencia.getTag().toString()));

        findViewById(R.id.btnPedirCita).setOnClickListener(v -> startActivity(
                new Intent(this, CitaPrevia.class).putExtra("CORREO", correoUsuario)));
        findViewById(R.id.btnMisGestiones).setOnClickListener(v -> startActivity(
                new Intent(this, MisGestiones.class).putExtra("CORREO", correoUsuario)));

        tvVerTodos.setOnClickListener(v -> mostrarCatalogo(grupoCatalogo.getVisibility() != View.VISIBLE));
        findViewById(R.id.btnQueNecesitas).setOnClickListener(v -> lanzadorQueNecesitas.launch(
                new Intent(this, QueNecesitas.class).putExtra("CORREO", correoUsuario)));
        mostrarCatalogo(catalogoVisible);

        cargarCategoria();
    }

    @Override
    protected Tutorial crearTutorial() {
        return new Tutorial(this, "secretaria", 2)
                .alEmpezar(() -> { // empieza con el catálogo cerrado y sin búsqueda
                    etBuscar.setText("");
                    mostrarCatalogo(false);
                })
                .info(R.id.contenedorLoTuyo, "Aquí tienes lo tuyo: tu próxima cita, lo que has solicitado y los plazos abiertos. Tócalo para abrirlo")
                .soloSi(() -> contenedorLoTuyo.getChildCount() > 0)
                .info(R.id.btnQueNecesitas, "¿No sabes qué trámite es el tuyo? Aquí te lo preguntamos paso a paso")
                .info(R.id.btnPedirCita, "Pide cita para ir a la ventanilla de Secretaría")
                .info(R.id.btnMisGestiones, "Tus citas y solicitudes, con su justificante y su QR")
                .tocar(R.id.tvVerTodos, "Toca aquí para abrir el catálogo con todos los trámites")
                .tocar(R.id.pestanasCategorias, "Toca una categoría para ver solo sus trámites")
                .gesto(R.id.fichero, Gesto.DESLIZAR_IZQ, "Hay más fichas: desliza a la izquierda para pasarlas")
                .soloSi(() -> fichero.getPaginas() > 1)
                .sensor(Gesto.INCLINAR, "También puedes inclinar el móvil a un lado o al otro para pasar las fichas")
                .soloSi(() -> inclinar.escuchando())
                .gesto(R.id.etBuscar, Gesto.ESCRIBIR,
                        "Escribe lo que buscas, aunque sea con faltas: te entendemos igual")
                .tocar(R.id.chipSugerencia, "¿Era esto lo que querías decir? Tócalo para buscarlo").soloSiSeVe()
                .tocar(R.id.itemTramite, "Toca un trámite para ver qué necesitas y sus pasos");
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
        // el cajón se abre deslizando, no aparece de golpe
        if (visible != (grupoCatalogo.getVisibility() == View.VISIBLE) && grupoCatalogo.isLaidOut()) {
            TransitionManager.beginDelayedTransition((ViewGroup) grupoCatalogo.getParent(),
                    new AutoTransition().setDuration(220));
        }
        boolean cerrando = !visible && grupoCatalogo.getVisibility() == View.VISIBLE;
        // el catálogo ocupa el sitio de «lo tuyo» y del mostrador: así nunca hace falta scroll
        grupoCatalogo.setVisibility(visible ? View.VISIBLE : View.GONE);
        contenedorLoTuyo.setVisibility(visible ? View.GONE : View.VISIBLE);
        grupoMostrador.setVisibility(visible ? View.GONE : View.VISIBLE);
        tvVerTodos.setText(visible ? "Cerrar el catálogo ▾" : "Ver todos los trámites ▴");
        if (inclinar != null) inclinar.actualizar(); // solo se escucha con el catálogo abierto
        if (cerrando) mostrarLoTuyo(); // con el catálogo abierto no se pudo medir qué tarjetas caben
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
            View tarjeta = anadirTarjeta(R.drawable.nui_ic_cita, R.color.nui_cobalto_tinte, R.color.nui_cobalto,
                    "Tu próxima cita",
                    FechasUtil.cuando(cuando) + " a las " + FechasUtil.hora(cuando),
                    cita.motivo(),
                    "En Secretaría, gira el móvil para enseñar el QR",
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

            View tarjeta = anadirTarjeta(R.drawable.nui_ic_sobre,
                    resuelta ? R.color.nui_arrayan_tinte : R.color.nui_granada_tinte,
                    resuelta ? R.color.nui_arrayan : R.color.nui_granada,
                    "Solicitud", s.nombreTramite,
                    "Enviada " + FechasUtil.hace(envio), null,
                    v -> startActivity(new Intent(this, Justificante.class)
                            .putExtra("idSolicitud", s.id).putExtra("CORREO", correoUsuario)));
            TextView sello = tarjeta.findViewById(R.id.tvSello);
            sello.setText(estado);
            sello.setVisibility(View.VISIBLE);
            nSolicitudes++;
        }

        for (Tramite t : plazosAbiertos(tramitesEnCurso)) {
            long dias = FechasUtil.diasHasta(LocalDate.parse(t.plazoFin));
            String queda = dias == 0 ? "Último día" : dias == 1 ? "Queda 1 día" : "Quedan " + dias + " días";
            View tarjeta = anadirTarjeta(R.drawable.nui_ic_reloj_arena, R.color.nui_resaltado_tinte, R.color.nui_acento_profundo,
                    "Plazo abierto", t.nombre,
                    queda + " · hasta el " + FechasUtil.diaCorto(LocalDate.parse(t.plazoFin)), null,
                    v -> abrirDetalle(t));
            if (dias <= 3) resaltar(tarjeta);
        }

        if (contenedorLoTuyo.getChildCount() == 0) {
            View tarjeta = anadirTarjeta(R.drawable.nui_ic_hoja, R.color.nui_arrayan_tinte, R.color.nui_arrayan,
                    "Todo en orden",
                    "No tienes nada pendiente", "Ni citas, ni solicitudes, ni plazos que se acaben",
                    "Si necesitas algo, pulsa «¿Qué necesitas?»", null);
            tarjeta.setClickable(false);
            tarjeta.findViewById(R.id.tvChevron).setVisibility(View.GONE);
        }

        // antes de pintarlas se quitan las que no caben; después entran animadas
        OneShotPreDrawListener.add(contenedorLoTuyo, () -> {
            recortarLoTuyo();
            if (animarEntrada) {
                animarEntrada = false;
                animarTarjetas();
            }
        });
    }

    // sin scroll: si en el hueco no caben todas las tarjetas, se quitan las últimas (lo menos urgente).
    // Las solicitudes siguen en «Mis gestiones» y los plazos en el catálogo
    private void recortarLoTuyo() {
        int alto = contenedorLoTuyo.getHeight() - contenedorLoTuyo.getPaddingBottom();
        // con el catálogo abierto no se ve: ya se recortará al cerrarlo
        if (contenedorLoTuyo.getVisibility() != View.VISIBLE || alto <= 0) return;
        for (int i = contenedorLoTuyo.getChildCount() - 1; i > 0; i--) {
            if (contenedorLoTuyo.getChildAt(i).getBottom() <= alto) break;
            contenedorLoTuyo.removeViewAt(i);
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

    // icono dibujado sobre una tesela del color del tipo de papel
    private View anadirTarjeta(int icono, int tinte, int color, String etiqueta, String titulo, String detalle,
                               String accion, View.OnClickListener alTocar) {
        View tarjeta = LayoutInflater.from(this)
                .inflate(R.layout.item_tarjeta_lo_tuyo, contenedorLoTuyo, false);
        ImageView ivIcono = tarjeta.findViewById(R.id.ivIcono);
        ivIcono.setImageResource(icono);
        ivIcono.setImageTintList(ColorStateList.valueOf(getColor(color)));
        ivIcono.setBackgroundTintList(ColorStateList.valueOf(getColor(tinte)));
        ivIcono.setVisibility(View.VISIBLE);
        tarjeta.findViewById(R.id.tvIcono).setVisibility(View.GONE);
        // papeles sobre la mesa: cada uno un poco girado, alternando
        tarjeta.setRotation(contenedorLoTuyo.getChildCount() % 2 == 0 ? -0.6f : 0.5f);
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

    // lo urgente (cita hoy, plazo que se acaba): papel naranja (nui_papel_estado seleccionado)
    private void resaltar(View tarjeta) {
        tarjeta.setSelected(true);
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

    // dos filas del mismo ancho: los nombres cortos arriba y los largos abajo, para que todos se lean enteros
    private void crearPestanas() {
        pestanasCategorias.nuevaFila();
        pestanasCategorias.anadir(null, "Todas", null, R.drawable.nui_ic_granada, R.color.nui_granada);
        anadirPestana("matricula", R.drawable.nui_ic_matricula);
        anadirPestana("titulos", R.drawable.nui_ic_titulo);
        anadirPestana("otro", R.drawable.nui_ic_consulta);
        pestanasCategorias.nuevaFila();
        anadirPestana("reconocimiento_creditos", R.drawable.nui_ic_creditos);
        anadirPestana("convocatorias", R.drawable.nui_ic_examen);
        anadirPestana("certificados", R.drawable.nui_ic_sobre);
        pestanasCategorias.marcar(categoriaSeleccionada);
        pestanasCategorias.setOnElegir(clave -> {
            categoriaSeleccionada = (String) clave;
            cargarCategoria();
        });
    }

    private void anadirPestana(String clave, int icono) {
        String nombre = clave;
        for (String[] c : Tramite.CATEGORIAS) if (c[0].equals(clave)) nombre = c[1];
        if (clave.equals("reconocimiento_creditos")) nombre = "Créditos"; // «Reconocimiento de créditos» no cabe
        pestanasCategorias.anadir(clave, nombre, null, icono, Tramite.colorCategoria(clave));
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
        tvMisGestiones.setText(n > 0 ? "Mis gestiones (" + n + ")" : "Mis gestiones");
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
