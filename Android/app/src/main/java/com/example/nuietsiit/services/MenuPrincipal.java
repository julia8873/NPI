package com.example.nuietsiit.services;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;


import com.example.nuietsiit.R;

public class MenuPrincipal extends AppCompatActivity {

    private TextView tvBienvenida;

    // Declaración de los componentes de la interfaz de usuario
    Button btnNoticias, btnMapa, btnActividades, btnAsignaturas;
    Button btnProfesorado, btnSecretaria, btnComedores;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.menu_principal);

        tvBienvenida = findViewById(R.id.tvBienvenida);

        // Obtener el Intent con los datos
        Intent intent = getIntent();
        if (intent != null) {
            String usuario = intent.getStringExtra("USUARIO");

            // Asignar los valores al TextView
            tvBienvenida.setText("¡Bienvenido " + usuario + "!");
        }

        // Configuración del evento de clic para los distintos botones
        btnNoticias = findViewById(R.id.btnNoticias);
        btnNoticias.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Pasar a la siguiente pantalla
                Intent intent = new Intent(MenuPrincipal.this, Noticias.class);
                startActivity(intent);
            }
        });

        btnMapa = findViewById(R.id.btnMapa);
        btnMapa.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Pasar a la siguiente pantalla
                Intent intent = new Intent(MenuPrincipal.this, Mapa.class);
                startActivity(intent);
            }
        });

        btnActividades = findViewById(R.id.btnActividades);
        btnActividades.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Pasar a la siguiente pantalla
                Intent intent = new Intent(MenuPrincipal.this, Actividades.class);
                startActivity(intent);
            }
        });

        btnAsignaturas = findViewById(R.id.btnAsignaturas);
        btnAsignaturas.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Pasar a la siguiente pantalla
                Intent intent = new Intent(MenuPrincipal.this, Asignaturas.class);
                startActivity(intent);
            }
        });

        btnProfesorado = findViewById(R.id.btnProfesorado);
        btnProfesorado.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Pasar a la siguiente pantalla
                Intent intent = new Intent(MenuPrincipal.this, Profesorado.class);
                startActivity(intent);
            }
        });

        btnSecretaria = findViewById(R.id.btnSecretaria);
        btnSecretaria.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Pasar a la siguiente pantalla
                Intent intent = new Intent(MenuPrincipal.this, Secretaria.class);
                startActivity(intent);
            }
        });

        btnComedores = findViewById(R.id.btnComedores);
        btnComedores.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Pasar a la siguiente pantalla
                Intent intent = new Intent(MenuPrincipal.this, Comedores.class);
                startActivity(intent);
            }
        });

    }


}