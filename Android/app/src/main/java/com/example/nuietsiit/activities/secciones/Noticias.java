package com.example.nuietsiit.activities.secciones;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.TextView;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.viewpager2.widget.ViewPager2;

import com.example.nuietsiit.R;
import com.example.nuietsiit.activities.BaseActivity;
import com.example.nuietsiit.activities.secciones.noticias.AnuncioAdapter;
import com.example.nuietsiit.data.AnunciosDB;
import com.example.nuietsiit.data.AnunciosLeidos;
import com.example.nuietsiit.data.DBHelper;
import com.example.nuietsiit.data.modelo.Anuncio;
import com.example.nuietsiit.util.FechasUtil;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Pantalla de Noticias SIN SCROLL: carrusel horizontal con una tarjeta grande por aviso.
 * - Los urgentes van primero, con borde rojo.
 * - Un aviso cuenta como leído si te quedas en él un momento o lo abres entero.
 * - Tocar la tarjeta = aviso completo en una hoja (con su enlace si tiene).
 */
public class Noticias extends BaseActivity {

    private static final String FILTRO_NO_LEIDOS = "__noleidos";
    private static final int ROJO = 0xFFD32F2F;
    private static final int NARANJA = 0xFFFF8C42;
    /** Tiempo mirando una tarjeta para que cuente como leída. */
    private static final long MS_PARA_LEIDO = 1500;

    private AnunciosDB anunciosDB;
    private List<Anuncio> todos = new ArrayList<>();
    private List<Anuncio> visibles = new ArrayList<>();
    private String filtroSeleccionado; // null = todos

    private View raiz;
    private ViewPager2 vpAnuncios;
    private AnuncioAdapter adaptador;
    private TextView tvSinLeer, tvMarcarTodo, tvVacio, tvPagina;
    private final Handler reloj = new Handler(Looper.getMainLooper());
    private Runnable marcarPendiente;

    @android.annotation.SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.noticias);

        MaterialToolbar barra = findViewById(R.id.toolbar);
        if (barra != null) {
            setSupportActionBar(barra);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle(R.string.not_titulo);
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            }
        }

        anunciosDB = new AnunciosDB(DBHelper.getInstance(this));

        raiz = findViewById(android.R.id.content);
        tvSinLeer = findViewById(R.id.tvSinLeer);
        tvMarcarTodo = findViewById(R.id.tvMarcarTodo);
        tvVacio = findViewById(R.id.tvVacio);
        tvPagina = findViewById(R.id.tvPagina);

        // Carrusel: se ve un trozo de la tarjeta anterior y la siguiente
        adaptador = new AnuncioAdapter(this::mostrarDetalle);
        vpAnuncios = findViewById(R.id.vpAnuncios);
        vpAnuncios.setAdapter(adaptador);
        vpAnuncios.setOffscreenPageLimit(1);
        View interior = vpAnuncios.getChildAt(0); // el RecyclerView de dentro del ViewPager2
        if (interior instanceof androidx.recyclerview.widget.RecyclerView) {
            ((androidx.recyclerview.widget.RecyclerView) interior).setClipToPadding(false);
        }
        // Las tarjetas de los lados se ven un poco más pequeñas
        vpAnuncios.setPageTransformer((pagina, posicion) -> {
            float escala = 1f - 0.08f * Math.min(1f, Math.abs(posicion));
            pagina.setScaleY(escala);
        });
        vpAnuncios.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int posicion) {
                actualizarPagina();
                programarLeido(posicion);
            }
        });

        crearChips();

        tvMarcarTodo.setOnClickListener(v -> {
            List<Integer> ids = new ArrayList<>();
            for (Anuncio a : todos) ids.add(a.id);
            AnunciosLeidos.marcarVarios(this, ids);
            for (Anuncio a : todos) a.leido = true;
            adaptador.notifyDataSetChanged();
            actualizarContador();
        });

        cargarAnuncios();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (marcarPendiente != null) reloj.removeCallbacks(marcarPendiente);
    }

    // -------------------------------------------------
    //              CARGA Y FILTROS
    // -------------------------------------------------

    private void cargarAnuncios() {
        todos = anunciosDB.obtenerAnunciosGenerales(FechasUtil.ahoraBD());
        Set<String> leidos = AnunciosLeidos.leidos(this);
        for (Anuncio a : todos) a.leido = leidos.contains(String.valueOf(a.id));
        aplicarFiltro();
    }

    private void crearChips() {
        anadirChip(3000, null, getString(R.string.not_chip_todos));
        anadirChip(3001, FILTRO_NO_LEIDOS, getString(R.string.not_chip_sin_leer));
        for (int i = 0; i < Anuncio.CATEGORIAS.length; i++) {
            String cat = Anuncio.CATEGORIAS[i][0];
            anadirChip(3002 + i, cat, getString(R.string.not_chip_categoria_fmt, Anuncio.emoji(cat), Anuncio.CATEGORIAS[i][1]));
        }
        ChipGroup chips = findViewById(R.id.chipsCategorias);
        chips.setOnCheckedStateChangeListener((grupo, ids) -> {
            if (ids.isEmpty()) return;
            filtroSeleccionado = (String) grupo.findViewById(ids.get(0)).getTag();
            aplicarFiltro();
        });
    }

    private void anadirChip(int id, String clave, String etiqueta) {
        ChipGroup chips = findViewById(R.id.chipsCategorias);
        Chip chip = new Chip(this, null, com.google.android.material.R.attr.chipStyle);
        chip.setId(id);
        chip.setText(etiqueta);
        chip.setTag(clave);
        chip.setCheckable(true);
        chip.setChipBackgroundColorResource(R.color.chip_fondo);
        chip.setTextColor(getColorStateList(R.color.chip_texto));
        chip.setCheckedIconVisible(false);
        chips.addView(chip);
        if (clave == null) chip.setChecked(true);
    }

    private void aplicarFiltro() {
        visibles = new ArrayList<>();
        for (Anuncio a : todos) {
            if (filtroSeleccionado == null
                    || (FILTRO_NO_LEIDOS.equals(filtroSeleccionado) && !a.leido)
                    || filtroSeleccionado.equals(a.categoria)) {
                visibles.add(a);
            }
        }
        adaptador.setAnuncios(visibles);
        vpAnuncios.setCurrentItem(0, false);
        tvVacio.setVisibility(visibles.isEmpty() ? View.VISIBLE : View.GONE);
        actualizarContador();
        actualizarPagina();
        if (!visibles.isEmpty()) programarLeido(0);
    }

    private void actualizarContador() {
        int sinLeer = 0;
        for (Anuncio a : todos) if (!a.leido) sinLeer++;
        if (sinLeer == 0) {
            tvSinLeer.setText(R.string.not_al_dia);
        } else {
            tvSinLeer.setText(getResources().getQuantityString(R.plurals.not_avisos_sin_leer, sinLeer, sinLeer));
        }
        tvMarcarTodo.setVisibility(sinLeer > 0 ? View.VISIBLE : View.GONE);
    }

    // "← 2 de 8 →"
    private void actualizarPagina() {
        if (visibles.isEmpty()) {
            tvPagina.setText("");
            return;
        }
        int actual = vpAnuncios.getCurrentItem();
        String flechaIzq = actual > 0 ? "← " : "   ";
        String flechaDer = actual < visibles.size() - 1 ? " →" : "   ";
        tvPagina.setText(getString(R.string.not_paginacion_fmt, flechaIzq, actual + 1, visibles.size(), flechaDer));
    }

    // -------------------------------------------------
    //              LEÍDOS
    // -------------------------------------------------

    /** Si el usuario se queda mirando la tarjeta un momento, cuenta como leída. */
    private void programarLeido(int posicion) {
        if (marcarPendiente != null) reloj.removeCallbacks(marcarPendiente);
        marcarPendiente = () -> {
            if (posicion < visibles.size() && vpAnuncios.getCurrentItem() == posicion) {
                marcarLeido(visibles.get(posicion));
            }
        };
        reloj.postDelayed(marcarPendiente, MS_PARA_LEIDO);
    }

    private void marcarLeido(Anuncio a) {
        if (a.leido) return;
        AnunciosLeidos.marcar(this, a.id);
        a.leido = true;
        int pos = visibles.indexOf(a);
        if (pos >= 0) adaptador.notifyItemChanged(pos);
        actualizarContador();
    }

    // -------------------------------------------------
    //              DETALLE (hoja inferior)
    // -------------------------------------------------

    private void mostrarDetalle(Anuncio a) {
        BottomSheetDialog hoja = new BottomSheetDialog(this);
        View v = getLayoutInflater().inflate(R.layout.detalle_anuncio, null);
        hoja.setContentView(v);

        TextView tvCategoria = v.findViewById(R.id.tvCategoriaDetalle);
        String catTexto = getString(R.string.not_detalle_categoria_fmt, Anuncio.emoji(a.categoria), Anuncio.nombreCategoria(a.categoria));
        if (a.esUrgente()) {
            catTexto += getString(R.string.not_detalle_urgente_sufijo);
        }
        tvCategoria.setText(catTexto);
        tvCategoria.setBackgroundTintList(ColorStateList.valueOf(a.esUrgente() ? ROJO : NARANJA));

        ((TextView) v.findViewById(R.id.tvTituloDetalle)).setText(a.titulo);
        ((TextView) v.findViewById(R.id.tvFechaDetalle)).setText(
                getString(R.string.not_detalle_publicado_fmt, FechasUtil.hace(this, a.publicacion), FechasUtil.diaLargo(a.publicacion)));
        ((TextView) v.findViewById(R.id.tvContenidoDetalle)).setText(a.contenido);

        if (a.caducidad != null) {
            TextView tvCaducidad = v.findViewById(R.id.tvCaducidadDetalle);
            tvCaducidad.setVisibility(View.VISIBLE);
            tvCaducidad.setText(getString(R.string.not_detalle_caducidad_fmt, FechasUtil.diaLargo(a.caducidad)));
        }

        if (a.enlace != null && !a.enlace.isEmpty()) {
            View btnEnlace = v.findViewById(R.id.btnEnlace);
            btnEnlace.setVisibility(View.VISIBLE);
            btnEnlace.setOnClickListener(x -> {
                try {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(a.enlace)));
                } catch (ActivityNotFoundException e) {
                    Snackbar.make(raiz, R.string.not_error_sin_navegador, Snackbar.LENGTH_SHORT).show();
                }
            });
        }

        hoja.show();
        marcarLeido(a); // abrirlo entero también cuenta como leído
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}