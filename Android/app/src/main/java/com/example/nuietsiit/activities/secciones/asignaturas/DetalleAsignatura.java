package com.example.nuietsiit.activities.secciones.asignaturas;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.nuietsiit.R;
import com.example.nuietsiit.data.AcademicoDB;
import com.example.nuietsiit.data.DBHelper;

public class DetalleAsignatura extends AppCompatActivity {

    private String enlaceGuiaDocente = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.detalle_asignatura);

        TextView btnVolver = findViewById(R.id.btnVolverDetalle);
        TextView tvNombreAsignatura = findViewById(R.id.tvNombreAsignatura);
        TextView tvCodigoAsignatura = findViewById(R.id.tvCodigoAsignatura);
        TextView tvCreditos = findViewById(R.id.tvCreditos);
        TextView tvCuatrimestre = findViewById(R.id.tvCuatrimestre);
        Button btnGuiaDocente = findViewById(R.id.btnGuiaDocente);

        btnVolver.setOnClickListener(v -> finish());

        // Recuperar el código de la asignatura desde el Intent
        String codigoAsignatura = getIntent().getStringExtra("CODIGO_ASIGNATURA");

        if (codigoAsignatura != null) {
            DBHelper bd = DBHelper.getInstance(this);
            AcademicoDB academicoDB = new AcademicoDB(bd);
            Cursor cursor = academicoDB.obtenerAsignatura(codigoAsignatura);

            if (cursor.moveToFirst()) {
                tvCodigoAsignatura.setText("Código: " + cursor.getString(0));
                tvNombreAsignatura.setText(cursor.getString(1));
                tvCuatrimestre.setText(cursor.getInt(2) + "º");
                tvCreditos.setText(String.valueOf(cursor.getDouble(3)));
                enlaceGuiaDocente = cursor.getString(4);
            }
            cursor.close();
        }

        // Abrir la guía docente en el navegador web
        btnGuiaDocente.setOnClickListener(v -> {
            if (enlaceGuiaDocente != null && !enlaceGuiaDocente.isEmpty()) {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(enlaceGuiaDocente));
                startActivity(browserIntent);
            } else {
                Toast.makeText(this, "Enlace no disponible", Toast.LENGTH_SHORT).show();
            }
        });
    }
}