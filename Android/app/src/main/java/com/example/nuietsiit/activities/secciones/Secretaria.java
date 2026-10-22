package com.example.nuietsiit.activities.secciones;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.nuietsiit.R;
import com.example.nuietsiit.activities.secciones.secretaria.CitaPrevia;
import com.example.nuietsiit.activities.secciones.secretaria.DetalleTramite;
import com.example.nuietsiit.activities.secciones.secretaria.MisGestiones;
import com.example.nuietsiit.activities.secciones.secretaria.TramiteAdapter;
import com.example.nuietsiit.data.DBHelper;
import com.example.nuietsiit.data.SecretariaDB;
import com.example.nuietsiit.data.TablasActividades;
import com.example.nuietsiit.data.modelo.Tramite;
import com.example.nuietsiit.util.BuscadorTolerante;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;


public class Secretaria extends AppCompatActivity {

    private static final String ESTADO_CATEGORIA = "categoria";

    private SecretariaDB secretariaDB;
    private String correoUsuario;
    private String categoriaSeleccionada; // null = todas

    private EditText etBuscar;
    private ChipGroup chipsCategorias;
    private TextView tvMostrandoPara, tvVacio;
    private Chip chipSugerencia;
    private Button btnMisGestiones;
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

        correoUsuario = getIntent().getStringExtra("CORREO");
        if (correoUsuario == null || correoUsuario.isEmpty()) correoUsuario = TablasActividades.CORREO_INVITADO;
        secretariaDB = new SecretariaDB(DBHelper.getInstance(this));
        if (savedInstanceState != null) {
            categoriaSeleccionada = savedInstanceState.getString(ESTADO_CATEGORIA);
        }

        etBuscar = findViewById(R.id.etBuscar);
        chipsCategorias = findViewById(R.id.chipsCategorias);
        tvMostrandoPara = findViewById(R.id.tvMostrandoPara);
        tvVacio = findViewById(R.id.tvVacio);
        chipSugerencia = findViewById(R.id.chipSugerencia);
        btnMisGestiones = findViewById(R.id.btnMisGestiones);

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

        cargarCategoria();
    }

    @Override
    protected void onResume() {
        super.onResume();
        actualizarContadorGestiones();
    }

    @Override
    protected void onSaveInstanceState(Bundle salida) {
        super.onSaveInstanceState(salida);
        salida.putString(ESTADO_CATEGORIA, categoriaSeleccionada);
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
