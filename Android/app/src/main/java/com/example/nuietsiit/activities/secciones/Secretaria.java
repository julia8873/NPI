package com.example.nuietsiit.activities.secciones;

import android.os.Bundle;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import com.example.nuietsiit.R;

public class Secretaria extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.secretaria);

        // Botón atrás en la barra superior
        ActionBar barra = getSupportActionBar();
        if (barra != null) {
            barra.setTitle("Secretaría");
            barra.setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
