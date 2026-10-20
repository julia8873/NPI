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

    private DBHelper BD;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.buscador_asignaturas);

        BD = DBHelper.getInstance(this);

        // Referencias
        scrollGrados = findViewById(R.id.scrollGrados);
        layoutRuletaCursos = findViewById(R.id.layoutRuletaCursos);
        contenedorBotonesGrados = findViewById(R.id.contenedorBotonesGrados);
        tvTituloGradoSeleccionado = findViewById(R.id.tvTituloGradoSeleccionado);
        btnVolver = findViewById(R.id.btnVolver);

        // Configurar el botón "Volver" de arriba
        btnVolver.setOnClickListener(v -> manejarRetroceso());

        // Configurar el botón/gesto de "Atrás" físico del móvil (Forma moderna y segura)
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
            // Si estamos en la ruleta, volvemos a la lista de grados
            layoutRuletaCursos.setVisibility(View.GONE);
            scrollGrados.setVisibility(View.VISIBLE);
        } else {
            // Si ya estamos en los grados, cerramos la ventana y volvemos al horario
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

                // Crear tarjeta dinámica para cada grado
                View tarjeta = crearTarjetaGrado(idGrado, nombreGrado);
                contenedorBotonesGrados.addView(tarjeta);

            } while (cursor.moveToNext());
        }
        cursor.close();
    }

    /**
     - Crea un bloque visual que contiene el botón naranja y una imagen debajo.
     */
    private View crearTarjetaGrado(int idGrado, String nombreGrado) {
        // Contenedor de la tarjeta
        LinearLayout tarjeta = new LinearLayout(this);
        tarjeta.setOrientation(LinearLayout.VERTICAL);

        // Espaciado entre tarjetas
        LinearLayout.LayoutParams paramsTarjeta = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        paramsTarjeta.setMargins(0, 0, 0, 80); // Gran margen inferior para separarlos bien
        tarjeta.setLayoutParams(paramsTarjeta);

        // 1. El botón Naranja
        Button btnGrado = new Button(this);
        btnGrado.setText(nombreGrado);
        btnGrado.setAllCaps(false);
        btnGrado.setTextSize(16f);
        btnGrado.setBackgroundColor(Color.parseColor("#FF9800")); // Naranja principal
        btnGrado.setTextColor(Color.WHITE);

        // 2. La imagen de portada
        ImageView ivPortada = new ImageView(this);
        LinearLayout.LayoutParams paramsImg = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                450 // Altura fija de la imagen para rellenar espacio
        );
        ivPortada.setLayoutParams(paramsImg);
        ivPortada.setScaleType(ImageView.ScaleType.CENTER_CROP);

        // Lógica de imágenes dinámicas: busca "portada_grado_1", "portada_grado_2", etc.
        int idRecursoImg = getResources().getIdentifier("portada_grado_" + idGrado, "drawable", getPackageName());
        if (idRecursoImg != 0) {
            ivPortada.setImageResource(idRecursoImg);
        } else {
            // Si no hay imagen en la carpeta res/drawable, ponemos una por defecto
            ivPortada.setImageResource(android.R.drawable.ic_menu_gallery);
            ivPortada.setBackgroundColor(Color.parseColor("#EEEEEE"));
        }

        // Añadir elementos a la tarjeta
        tarjeta.addView(btnGrado);
        tarjeta.addView(ivPortada);

        // Hacer que toda la tarjeta reaccione al click
        View.OnClickListener clickListener = v -> prepararRuletaParaGrado(idGrado, nombreGrado);
        btnGrado.setOnClickListener(clickListener);
        ivPortada.setOnClickListener(clickListener);

        return tarjeta;
    }

    private void prepararRuletaParaGrado(int idGrado, String nombreGrado) {
        scrollGrados.setVisibility(View.GONE);
        layoutRuletaCursos.setVisibility(View.VISIBLE);
        tvTituloGradoSeleccionado.setText(nombreGrado);
    }
}