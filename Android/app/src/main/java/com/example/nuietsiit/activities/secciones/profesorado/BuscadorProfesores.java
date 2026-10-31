package com.example.nuietsiit.activities.secciones.profesorado;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;

import com.example.nuietsiit.R;
import com.example.nuietsiit.activities.BaseActivity;
import com.example.nuietsiit.activities.secciones.asignaturas.RuletaCursosView;
import com.example.nuietsiit.data.DBHelper;
import com.example.nuietsiit.data.UsuariosDB;

public class BuscadorProfesores extends BaseActivity {

    private ScrollView scrollGrados;
    private LinearLayout layoutRuletaCursos;
    private ScrollView scrollResultados;
    private LinearLayout contenedorBotonesGrados;
    private LinearLayout contenedorResultados;
    private TextView tvTituloGradoSeleccionado;
    private TextView tvTituloResultados;
    private TextView btnVolver;
    private EditText etBuscadorRapido;
    private RuletaCursosView ruletaCursosView;

    private DBHelper BD;
    private UsuariosDB usuariosDB;

    private int idGradoActual = -1;
    private String nombreGradoActual = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.buscador_profesores);

        BD = DBHelper.getInstance(this);
        usuariosDB = new UsuariosDB(BD);

        scrollGrados = findViewById(R.id.scrollGrados);
        layoutRuletaCursos = findViewById(R.id.layoutRuletaCursos);
        scrollResultados = findViewById(R.id.scrollResultados);
        contenedorBotonesGrados = findViewById(R.id.contenedorBotonesGrados);
        contenedorResultados = findViewById(R.id.contenedorResultados);
        tvTituloGradoSeleccionado = findViewById(R.id.tvTituloGradoSeleccionado);
        tvTituloResultados = findViewById(R.id.tvTituloResultados);
        btnVolver = findViewById(R.id.btnVolver);
        etBuscadorRapido = findViewById(R.id.etBuscadorRapido);
        ruletaCursosView = findViewById(R.id.ruletaCursosView);

        ruletaCursosView.setOnCursoSelectedListener(curso -> {
            mostrarResultadosPorRuleta(curso);
        });

        etBuscadorRapido.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.toString().trim().length() > 0) {
                    mostrarResultadosPorTexto(s.toString().trim());
                } else {
                    restaurarVistaInicial();
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        btnVolver.setOnClickListener(v -> manejarRetroceso());

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                manejarRetroceso();
            }
        });

        cargarGrados();
    }

    private void manejarRetroceso() {
        if (scrollResultados.getVisibility() == View.VISIBLE && etBuscadorRapido.getText().toString().isEmpty()) {
            scrollResultados.setVisibility(View.GONE);
            layoutRuletaCursos.setVisibility(View.VISIBLE);
        } else if (layoutRuletaCursos.getVisibility() == View.VISIBLE) {
            layoutRuletaCursos.setVisibility(View.GONE);
            scrollGrados.setVisibility(View.VISIBLE);
        } else {
            finish();
        }
    }

    private void restaurarVistaInicial() {
        scrollResultados.setVisibility(View.GONE);
        layoutRuletaCursos.setVisibility(View.GONE);
        scrollGrados.setVisibility(View.VISIBLE);
    }

    private void cargarGrados() {
        SQLiteDatabase db = BD.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT idGrado, nombreGrado FROM GRADO WHERE idGrado > 0", null);

        if (cursor.moveToFirst()) {
            do {
                int idGrado = cursor.getInt(0);
                String nombreGrado = cursor.getString(1);

                View tarjeta = crearTarjetaGrado(idGrado, nombreGrado);
                contenedorBotonesGrados.addView(tarjeta);

            } while (cursor.moveToNext());
        }
        cursor.close();
    }

    private View crearTarjetaGrado(int idGrado, String nombreGrado) {
        LinearLayout tarjeta = new LinearLayout(this);
        tarjeta.setOrientation(LinearLayout.VERTICAL);

        LinearLayout.LayoutParams paramsTarjeta = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        paramsTarjeta.setMargins(0, 0, 0, 80);
        tarjeta.setLayoutParams(paramsTarjeta);

        Button btnGrado = new Button(this);
        btnGrado.setText(nombreGrado);
        btnGrado.setAllCaps(false);
        btnGrado.setTextSize(16f);
        btnGrado.setBackgroundColor(Color.parseColor("#FF9800"));
        btnGrado.setTextColor(Color.WHITE);

        ImageView ivPortada = new ImageView(this);
        LinearLayout.LayoutParams paramsImg = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 450
        );
        ivPortada.setLayoutParams(paramsImg);
        ivPortada.setScaleType(ImageView.ScaleType.CENTER_CROP);

        int idRecursoImg = getResources().getIdentifier("portada_grado_" + idGrado, "drawable", getPackageName());
        if (idRecursoImg != 0) {
            ivPortada.setImageResource(idRecursoImg);
        } else {
            ivPortada.setImageResource(android.R.drawable.ic_menu_gallery);
            ivPortada.setBackgroundColor(Color.parseColor("#EEEEEE"));
        }

        tarjeta.addView(btnGrado);
        tarjeta.addView(ivPortada);

        View.OnClickListener clickListener = v -> prepararRuletaParaGrado(idGrado, nombreGrado);
        btnGrado.setOnClickListener(clickListener);
        ivPortada.setOnClickListener(clickListener);

        return tarjeta;
    }

    private void prepararRuletaParaGrado(int idGrado, String nombreGrado) {
        idGradoActual = idGrado;
        nombreGradoActual = nombreGrado;

        scrollGrados.setVisibility(View.GONE);
        scrollResultados.setVisibility(View.GONE);
        layoutRuletaCursos.setVisibility(View.VISIBLE);

        tvTituloGradoSeleccionado.setText(nombreGrado);

        int numCursos = obtenerNumCursosGrado(idGrado, nombreGrado);
        ruletaCursosView.setNumCursos(numCursos);
    }

    private int obtenerNumCursosGrado(int idGrado, String nombreGrado) {
        SQLiteDatabase db = BD.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT MAX(curso) FROM ASIGNATURA_GRADO WHERE idGrado = ?",
                new String[]{String.valueOf(idGrado)}
        );

        int numCursosFallback = 4;
        if (nombreGrado != null && (nombreGrado.toLowerCase().contains("doble") || nombreGrado.toLowerCase().contains("dgiim"))) {
            numCursosFallback = 5;
        }

        int numCursos = numCursosFallback;
        if (cursor.moveToFirst()) {
            int maxCurso = cursor.getInt(0);
            if (maxCurso > 0) {
                numCursos = maxCurso;
            }
        }
        cursor.close();
        return numCursos;
    }

    private void mostrarResultadosPorRuleta(int curso) {
        layoutRuletaCursos.setVisibility(View.GONE);
        scrollResultados.setVisibility(View.VISIBLE);
        tvTituloResultados.setText("Profesorado de " + curso + "º Curso");

        contenedorResultados.removeAllViews();
        Cursor cursor = usuariosDB.obtenerProfesoresPorGradoYCurso(idGradoActual, curso);

        if (cursor != null && cursor.moveToFirst()) {
            do {
                String correo = cursor.getString(0);
                String nombre = cursor.getString(1);
                String asignatura = cursor.getString(3);
                View tarjeta = crearTarjetaProfesor(correo, nombre, "Imparte: " + asignatura);
                contenedorResultados.addView(tarjeta);
            } while (cursor.moveToNext());
            cursor.close();
        } else {
            if (cursor != null) cursor.close();
            mostrarMensajeSinResultados();
        }
    }

    private void mostrarResultadosPorTexto(String texto) {
        scrollGrados.setVisibility(View.GONE);
        layoutRuletaCursos.setVisibility(View.GONE);
        scrollResultados.setVisibility(View.VISIBLE);
        tvTituloResultados.setText("Resultados para: '" + texto + "'");

        contenedorResultados.removeAllViews();
        Cursor cursor = usuariosDB.buscarProfesoresPorTexto(texto);

        if (cursor != null && cursor.moveToFirst()) {
            do {
                String correo = cursor.getString(0);
                String nombre = cursor.getString(1);
                View tarjeta = crearTarjetaProfesor(correo, nombre, correo);
                contenedorResultados.addView(tarjeta);
            } while (cursor.moveToNext());
            cursor.close();
        } else {
            if (cursor != null) cursor.close();
            mostrarMensajeSinResultados();
        }
    }

    private View crearTarjetaProfesor(String correoProfesor, String nombreProfesor, String subtitulo) {
        LinearLayout tarjeta = new LinearLayout(this);
        tarjeta.setOrientation(LinearLayout.VERTICAL);
        tarjeta.setBackgroundColor(Color.parseColor("#F5F5F5"));
        tarjeta.setPadding(30, 30, 30, 30);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 0, 0, 24);
        tarjeta.setLayoutParams(params);

        TextView tvNombre = new TextView(this);
        tvNombre.setText(nombreProfesor);
        tvNombre.setTextSize(18f);
        tvNombre.setTextColor(Color.BLACK);
        tvNombre.setTypeface(null, Typeface.BOLD);

        TextView tvInfo = new TextView(this);
        tvInfo.setText(subtitulo);
        tvInfo.setTextSize(14f);
        tvInfo.setTextColor(Color.DKGRAY);
        tvInfo.setPadding(0, 8, 0, 0);

        tarjeta.addView(tvNombre);
        tarjeta.addView(tvInfo);

        tarjeta.setOnClickListener(v -> {
            Intent intent = new Intent(this, DetalleProfesor.class);
            intent.putExtra("CORREO_PROFESOR", correoProfesor);
            startActivity(intent);
        });

        return tarjeta;
    }

    private void mostrarMensajeSinResultados() {
        TextView tvVacio = new TextView(this);
        tvVacio.setText("No se ha encontrado profesorado en la base de datos.");
        tvVacio.setTextSize(16f);
        tvVacio.setTextColor(Color.GRAY);
        tvVacio.setPadding(20, 40, 20, 20);
        contenedorResultados.addView(tvVacio);
    }
}
