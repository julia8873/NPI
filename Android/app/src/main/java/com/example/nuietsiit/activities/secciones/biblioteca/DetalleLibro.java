package com.example.nuietsiit.activities.secciones.biblioteca;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.example.nuietsiit.R;
import com.example.nuietsiit.activities.BaseActivity;
import com.example.nuietsiit.activities.secciones.mapa.Mapa;
import com.example.nuietsiit.data.BibliotecaDB;
import com.example.nuietsiit.data.DBHelper;
import com.example.nuietsiit.data.modelo.Libro;
import com.example.nuietsiit.util.RecienteUtil;
import com.google.android.material.appbar.MaterialToolbar;

public class DetalleLibro extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.detalle_libro);

        MaterialToolbar barra = findViewById(R.id.toolbar);
        setSupportActionBar(barra);
        getSupportActionBar().setTitle("Libro");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // el extra es String porque RecienteUtil solo guarda Strings
        String idLibro = getIntent().getStringExtra("idLibro");
        Libro libro = idLibro == null ? null
                : new BibliotecaDB(DBHelper.getInstance(this)).getLibro(Integer.parseInt(idLibro));
        if (libro == null) {
            Toast.makeText(this, "Libro no encontrado", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        RecienteUtil.guardar(this, DetalleLibro.class, "Libro: " + libro.titulo, "Biblioteca ETSIIT", "idLibro", idLibro, 7);

        LibroAdapter.mostrarPortada(findViewById(R.id.ivPortada), libro);
        ((TextView) findViewById(R.id.tvTitulo)).setText(libro.titulo);
        ((TextView) findViewById(R.id.tvAutores)).setText(libro.autores);
        LibroAdapter.mostrarDisponibilidad(findViewById(R.id.tvDisponibilidad), libro);
        mostrarDato(R.id.tvUbicacion, "Ubicación: ", libro.ubicacion);
        mostrarDato(R.id.tvSignatura, "Signatura: ", libro.signatura);
        mostrarDato(R.id.tvEditorial, "Editorial: ", libro.editorial);
        mostrarDato(R.id.tvEdicion, "Edición: ", libro.edicion);
        mostrarDato(R.id.tvAnio, "Año: ", libro.anioPublicacion != null ? String.valueOf(libro.anioPublicacion) : null);
        mostrarDato(R.id.tvIsbn, "ISBN: ", libro.isbn);

        findViewById(R.id.btnComoLlegar).setOnClickListener(v -> startActivity(new Intent(this, Mapa.class)
                .putExtra("RUTA_A_MARCADOR", "Biblioteca")
                .putExtra("CORREO", getIntent().getStringExtra("CORREO"))));
    }

    // oculta la línea si el dato no está
    private void mostrarDato(int idVista, String etiqueta, String valor) {
        TextView tv = findViewById(idVista);
        if (valor == null || valor.isEmpty()) {
            tv.setVisibility(View.GONE);
        } else {
            tv.setText(etiqueta + valor);
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
