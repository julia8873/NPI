package com.example.nuietsiit.activities.secciones.biblioteca;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.example.nuietsiit.R;
import com.example.nuietsiit.activities.secciones.mapa.Mapa;
import com.example.nuietsiit.data.BibliotecaDB;
import com.example.nuietsiit.data.DBHelper;
import com.example.nuietsiit.data.modelo.Libro;
import com.example.nuietsiit.util.RecienteUtil;
import com.example.nuietsiit.util.tutorial.Tutorial;
import com.google.android.material.appbar.MaterialToolbar;

public class DetalleLibro extends PantallaBiblioteca {

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
        if (!libro.disponible()) {
            // igual que en la balda: portada apagada y la marca PRESTADO encima
            LibroAdapter.apagarPortada(findViewById(R.id.ivPortada));
            findViewById(R.id.tvPrestado).setVisibility(View.VISIBLE);
        }
        ((TextView) findViewById(R.id.tvSignatura)).setText(
                libro.signatura == null || libro.signatura.isEmpty() ? "Sin signatura" : libro.signatura);
        ((TextView) findViewById(R.id.tvTitulo)).setText(libro.titulo);
        ((TextView) findViewById(R.id.tvAutores)).setText(libro.autores);
        LibroAdapter.mostrarDisponibilidad(findViewById(R.id.tvDisponibilidad), libro);
        mostrarDato(R.id.filaUbicacion, R.id.tvUbicacion, libro.ubicacion);
        boolean hayEdicion = mostrarDato(R.id.filaEditorial, R.id.tvEditorial, libro.editorial)
                | mostrarDato(R.id.filaEdicion, R.id.tvEdicion, libro.edicion)
                | mostrarDato(R.id.filaAnio, R.id.tvAnio, libro.anioPublicacion != null ? String.valueOf(libro.anioPublicacion) : null)
                | mostrarDato(R.id.filaIsbn, R.id.tvIsbn, libro.isbn);
        if (!hayEdicion) {
            findViewById(R.id.papelEdicion).setVisibility(View.GONE);
        }

        findViewById(R.id.btnComoLlegar).setOnClickListener(v -> startActivity(new Intent(this, Mapa.class)
                .putExtra("RUTA_A_MARCADOR", "Biblioteca")
                .putExtra("CORREO", getIntent().getStringExtra("CORREO"))));
    }

    @Override
    protected Tutorial crearTutorial() {
        return new Tutorial(this, "detalle_libro", 1)
                .info(R.id.tvDisponibilidad, "Aquí ves si queda algún ejemplar. Si está prestado, la portada sale apagada")
                .info(R.id.tvSignatura, "La signatura es la etiqueta del lomo: con ella lo encuentras en la estantería")
                .info(R.id.filaUbicacion, "Planta y estantería donde está").soloSiSeVe()
                .info(R.id.btnComoLlegar, "¿No sabes dónde está la biblioteca? El mapa te lleva hasta ella")
                .info(R.id.toolbar, "Vuelve atrás y el libro regresa volando a su balda");
    }

    // oculta la fila si el dato no está; devuelve si se ve
    private boolean mostrarDato(int idFila, int idValor, String valor) {
        if (valor == null || valor.isEmpty()) {
            findViewById(idFila).setVisibility(View.GONE);
            return false;
        }
        ((TextView) findViewById(idValor)).setText(valor);
        return true;
    }
}
