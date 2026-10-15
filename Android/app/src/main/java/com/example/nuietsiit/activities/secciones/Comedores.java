package com.example.nuietsiit.activities.secciones;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.nuietsiit.data.DBHelper;

public class Comedores extends AppCompatActivity {

    private TextView tvDia, tvPrimerPlato, tvSegundoPlato, tvPostre, tvHorario, tvPrecio;
    private DBHelper BD;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.comedores);
    }
}
