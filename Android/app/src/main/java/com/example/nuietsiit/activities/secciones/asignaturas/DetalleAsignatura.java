package com.example.nuietsiit.activities.secciones.asignaturas;

import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.nuietsiit.R;
import com.example.nuietsiit.activities.BaseActivity;
import com.example.nuietsiit.activities.secciones.profesorado.DetalleProfesor;
import com.example.nuietsiit.data.AcademicoDB;
import com.example.nuietsiit.data.DBHelper;
import com.example.nuietsiit.data.UsuariosDB;
import com.example.nuietsiit.util.RecienteUtil;

public class DetalleAsignatura extends BaseActivity {

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
        LinearLayout contenedorProfesorado = findViewById(R.id.contenedorProfesorado);

        btnVolver.setOnClickListener(v -> finish());

        // Recuperar el código de la asignatura desde el Intent
        String codigoAsignatura = getIntent().getStringExtra("CODIGO_ASIGNATURA");

        if (codigoAsignatura != null) {
            DBHelper bd = DBHelper.getInstance(this);
            AcademicoDB academicoDB = new AcademicoDB(bd);
            Cursor cursor = academicoDB.obtenerAsignatura(codigoAsignatura);

            if (cursor.moveToFirst()) {
                String codigo = cursor.getString(0);
                String nombre = cursor.getString(1);
                tvCodigoAsignatura.setText("Código: " + codigo);
                tvNombreAsignatura.setText(nombre);
                tvCuatrimestre.setText(cursor.getInt(2) + "º");
                tvCreditos.setText(String.valueOf(cursor.getDouble(3)));
                enlaceGuiaDocente = cursor.getString(4);

                RecienteUtil.guardar(this, DetalleAsignatura.class, nombre, "Detalle de asignatura", "CODIGO_ASIGNATURA", codigo, 4);
            }
            cursor.close();

            // Cargar profesorado impartidor
            UsuariosDB usuariosDB = new UsuariosDB(bd);
            Cursor cursorProf = usuariosDB.obtenerProfesoresDeAsignatura(codigoAsignatura);
            if (contenedorProfesorado != null) {
                contenedorProfesorado.removeAllViews();
                if (cursorProf != null && cursorProf.moveToFirst()) {
                    do {
                        String correoProf = cursorProf.getString(cursorProf.getColumnIndexOrThrow("correoProfesor"));
                        String nombreProf = cursorProf.getString(cursorProf.getColumnIndexOrThrow("nombreProfesor"));

                        TextView tvProf = new TextView(this);
                        tvProf.setText("👨‍🏫 " + nombreProf + "\n✉️ " + correoProf);
                        tvProf.setTextSize(14f);
                        tvProf.setTextColor(Color.parseColor("#0F172A"));
                        tvProf.setBackgroundColor(Color.parseColor("#F8FAFC"));
                        tvProf.setPadding(24, 16, 24, 16);

                        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
                        );
                        params.setMargins(0, 0, 0, 12);
                        tvProf.setLayoutParams(params);

                        tvProf.setOnClickListener(v -> {
                            Intent intent = new Intent(DetalleAsignatura.this, DetalleProfesor.class);
                            intent.putExtra("CORREO_PROFESOR", correoProf);
                            startActivity(intent);
                        });

                        contenedorProfesorado.addView(tvProf);
                    } while (cursorProf.moveToNext());
                    cursorProf.close();
                } else {
                    if (cursorProf != null) cursorProf.close();
                    TextView tvVacio = new TextView(this);
                    tvVacio.setText("No hay profesores asignados en esta asignatura.");
                    tvVacio.setTextSize(14f);
                    tvVacio.setTextColor(Color.GRAY);
                    contenedorProfesorado.addView(tvVacio);
                }
            }
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
