package com.example.nuietsiit.activities.secciones.asignaturas;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;

import com.example.nuietsiit.R;
import com.example.nuietsiit.data.DBHelper;

public class BuscadorAsignaturas extends AppCompatActivity {

    private ScrollView scrollGrados;
    private LinearLayout layoutRuletaCursos;
    private LinearLayout contenedorBotonesGrados;
    private TextView tvTituloGradoSeleccionado;
    private TextView btnVolver;
    private RuletaCursosView ruletaCursosView;

    private DBHelper BD;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.buscador_asignaturas);

        BD = DBHelper.getInstance(this);

        // Referencias de UI
        scrollGrados = findViewById(R.id.scrollGrados);
        layoutRuletaCursos = findViewById(R.id.layoutRuletaCursos);
        contenedorBotonesGrados = findViewById(R.id.contenedorBotonesGrados);
        tvTituloGradoSeleccionado = findViewById(R.id.tvTituloGradoSeleccionado);
        btnVolver = findViewById(R.id.btnVolver);
        ruletaCursosView = findViewById(R.id.ruletaCursosView);

        // Escuchador para cuando el usuario pulsa el botón "IR" del centro de la ruleta
        ruletaCursosView.setOnCursoSelectedListener(curso -> {
            // Por ahora mostramos una notificación temporal de confirmación
            Toast.makeText(this, "Curso " + curso + "º seleccionado", Toast.LENGTH_SHORT).show();
        });

        // Configurar botón "Volver" de arriba
        btnVolver.setOnClickListener(v -> manejarRetroceso());

        // Configurar gesto/botón físico de atrác
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                manejarRetroceso();
            }
        });

        cargarGrados();
    }

    private void manejarRetroceso() {
        if (layoutRuletaCursos.getVisibility() == View.VISIBLE) {
            layoutRuletaCursos.setVisibility(View.GONE);
            scrollGrados.setVisibility(View.VISIBLE);
        } else {
            finish();
        }
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
                LinearLayout.LayoutParams.MATCH_PARENT,
                450
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
        scrollGrados.setVisibility(View.GONE);
        layoutRuletaCursos.setVisibility(View.VISIBLE);
        tvTituloGradoSeleccionado.setText(nombreGrado);

        // Le pasamos también el nombre del grado para el respaldo inteligente
        int numCursos = obtenerNumCursosGrado(idGrado, nombreGrado);
        ruletaCursosView.setNumCursos(numCursos);
    }

    /**
     * Consulta la BD para obtener el curso máximo. Si no hay datos aún,
     * aplica un respaldo inteligente según el nombre del grado.
     */
    private int obtenerNumCursosGrado(int idGrado, String nombreGrado) {
        SQLiteDatabase db = BD.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT MAX(curso) FROM ASIGNATURA_GRADO WHERE idGrado = ?",
                new String[]{String.valueOf(idGrado)}
        );

        // Fallback inteligente: si es un doble grado, por defecto son 5 cursos
        int numCursosFallback = 4;
        if (nombreGrado != null && (nombreGrado.toLowerCase().contains("doble") || nombreGrado.toLowerCase().contains("dgiim"))) {
            numCursosFallback = 5;
        }

        int numCursos = numCursosFallback;
        if (cursor.moveToFirst()) {
            int maxCurso = cursor.getInt(0);
            if (maxCurso > 0) { // Si ya hay asignaturas reales en la BD, usamos ese dato
                numCursos = maxCurso;
            }
        }
        cursor.close();
        return numCursos;
    }
}