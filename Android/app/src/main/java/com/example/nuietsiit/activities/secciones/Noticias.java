package com.example.nuietsiit.activities.secciones;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.net.Uri;
import android.os.Bundle;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.nuietsiit.R;
import com.example.nuietsiit.activities.secciones.noticias.AnuncioAdapter;
import com.example.nuietsiit.data.AnunciosDB;
import com.example.nuietsiit.data.AnunciosLeidos;
import com.example.nuietsiit.data.DBHelper;
import com.example.nuietsiit.data.modelo.Anuncio;
import com.example.nuietsiit.util.DetectorProximidad;
import com.example.nuietsiit.util.FechasUtil;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Pantalla de Noticias: anuncios generales ordenados por prioridad y fecha.
 * - Los urgentes salen arriba con borde rojo.
 * - Se recuerda qué avisos has leído.
 * - SENSOR: con el "modo manos libres", pasar la mano por encima del móvil
 *   (sensor de proximidad) abre el siguiente aviso sin leer.
 */
public class Noticias extends AppCompatActivity implements DetectorProximidad.OnPasarMano {

    private static final String FILTRO_NO_LEIDOS = "__noleidos";
    private static final int ROJO = 0xFFD32F2F;
    private static final int NARANJA = 0xFFFF8C42;

    private AnunciosDB anunciosDB;
    private List<Anuncio> todos = new ArrayList<>();
    private String filtroSeleccionado; // null = todos

    private View raiz;
    private RecyclerView rv;
    private AnuncioAdapter adaptador;
    private TextView tvSinLeer, tvMarcarTodo, tvVacio, tvMano;
    private MaterialSwitch swManosLibres;
    private DetectorProximidad detector;
    private BottomSheetDialog hojaAbierta;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.noticias);

        MaterialToolbar barra = findViewById(R.id.toolbar);
        setSupportActionBar(barra);
        getSupportActionBar().setTitle("Noticias y avisos");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        anunciosDB = new AnunciosDB(DBHelper.getInstance(this));

        raiz = findViewById(android.R.id.content);
        tvSinLeer = findViewById(R.id.tvSinLeer);
        tvMarcarTodo = findViewById(R.id.tvMarcarTodo);
        tvVacio = findViewById(R.id.tvVacio);
        tvMano = findViewById(R.id.tvMano);
        swManosLibres = findViewById(R.id.swManosLibres);

        adaptador = new AnuncioAdapter(this::mostrarDetalle);
        rv = findViewById(R.id.rvAnuncios);
        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.setAdapter(adaptador);

        crearChips();

        tvMarcarTodo.setOnClickListener(v -> {
            List<Integer> ids = new ArrayList<>();
            for (Anuncio a : todos) ids.add(a.id);
            AnunciosLeidos.marcarVarios(this, ids);
            cargarAnuncios();
        });

        // Sensor de proximidad
        detector = new DetectorProximidad(this, this);
        if (detector.disponible()) {
            swManosLibres.setChecked(true);
            swManosLibres.setOnCheckedChangeListener((boton, activado) -> {
                if (activado) detector.iniciar(); else detector.parar();
            });
        } else {
            swManosLibres.setChecked(false);
            swManosLibres.setEnabled(false);
            ((TextView) findViewById(R.id.tvManosLibres)).setText("Este móvil no tiene sensor de proximidad");
        }

        cargarAnuncios();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (swManosLibres.isChecked()) detector.iniciar();
    }

    @Override
    protected void onPause() {
        super.onPause();
        detector.parar(); // ahorra batería al salir de la pantalla
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
        anadirChip(3000, null, "Todos");
        anadirChip(3001, FILTRO_NO_LEIDOS, "Sin leer");
        for (int i = 0; i < Anuncio.CATEGORIAS.length; i++) {
            String cat = Anuncio.CATEGORIAS[i][0];
            anadirChip(3002 + i, cat, Anuncio.emoji(cat) + " " + Anuncio.CATEGORIAS[i][1]);
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
        List<Anuncio> visibles = new ArrayList<>();
        for (Anuncio a : todos) {
            if (filtroSeleccionado == null
                    || (FILTRO_NO_LEIDOS.equals(filtroSeleccionado) && !a.leido)
                    || filtroSeleccionado.equals(a.categoria)) {
                visibles.add(a);
            }
        }
        adaptador.setAnuncios(visibles);
        tvVacio.setVisibility(visibles.isEmpty() ? View.VISIBLE : View.GONE);
        actualizarContador();
    }

    private void actualizarContador() {
        int sinLeer = 0;
        for (Anuncio a : todos) if (!a.leido) sinLeer++;
        if (sinLeer == 0) {
            tvSinLeer.setText("✅ Estás al día");
        } else {
            tvSinLeer.setText("📬 " + sinLeer + (sinLeer == 1 ? " aviso sin leer" : " avisos sin leer"));
        }
        tvMarcarTodo.setVisibility(sinLeer > 0 ? View.VISIBLE : View.GONE);
    }

    // -------------------------------------------------
    //              DETALLE (hoja inferior)
    // -------------------------------------------------

    private void mostrarDetalle(Anuncio a) {
        if (hojaAbierta != null && hojaAbierta.isShowing()) hojaAbierta.dismiss();

        BottomSheetDialog hoja = new BottomSheetDialog(this);
        View v = getLayoutInflater().inflate(R.layout.detalle_anuncio, null);
        hoja.setContentView(v);

        TextView tvCategoria = v.findViewById(R.id.tvCategoriaDetalle);
        tvCategoria.setText(Anuncio.emoji(a.categoria) + " " + Anuncio.nombreCategoria(a.categoria)
                + (a.esUrgente() ? " · URGENTE" : ""));
        tvCategoria.setBackgroundTintList(ColorStateList.valueOf(a.esUrgente() ? ROJO : NARANJA));

        ((TextView) v.findViewById(R.id.tvTituloDetalle)).setText(a.titulo);
        ((TextView) v.findViewById(R.id.tvFechaDetalle)).setText("Publicado " + FechasUtil.hace(a.publicacion)
                + " · " + FechasUtil.diaLargo(a.publicacion));
        ((TextView) v.findViewById(R.id.tvContenidoDetalle)).setText(a.contenido);

        if (a.caducidad != null) {
            TextView tvCaducidad = v.findViewById(R.id.tvCaducidadDetalle);
            tvCaducidad.setVisibility(View.VISIBLE);
            tvCaducidad.setText("Válido hasta el " + FechasUtil.diaLargo(a.caducidad));
        }

        if (a.enlace != null && !a.enlace.isEmpty()) {
            View btnEnlace = v.findViewById(R.id.btnEnlace);
            btnEnlace.setVisibility(View.VISIBLE);
            btnEnlace.setOnClickListener(x -> {
                try {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(a.enlace)));
                } catch (ActivityNotFoundException e) {
                    Snackbar.make(raiz, "No hay navegador para abrir el enlace", Snackbar.LENGTH_SHORT).show();
                }
            });
        }

        hojaAbierta = hoja;
        hoja.show();

        // Al abrirlo cuenta como leído
        if (!a.leido) {
            AnunciosLeidos.marcar(this, a.id);
            a.leido = true;
            adaptador.notifyDataSetChanged();
            actualizarContador();
        }
    }

    // -------------------------------------------------
    //              SENSOR DE PROXIMIDAD
    // -------------------------------------------------

    /** Se llama cuando se pasa la mano por encima del móvil. */
    @Override
    public void alPasarMano() {
        // feedback: vibración corta y la manita "salta"
        rv.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
        tvMano.animate().rotationBy(30f).setDuration(120)
                .withEndAction(() -> tvMano.animate().rotationBy(-30f).setDuration(120).start())
                .start();

        int pos = adaptador.primeraNoLeida();
        if (pos < 0) {
            if (hojaAbierta != null && hojaAbierta.isShowing()) hojaAbierta.dismiss();
            Snackbar.make(raiz, "🎉 ¡No te queda nada por leer!", Snackbar.LENGTH_SHORT).show();
            return;
        }
        rv.smoothScrollToPosition(pos);
        adaptador.destacar(pos);
        mostrarDetalle(adaptador.get(pos));
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
