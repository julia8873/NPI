package com.example.nuietsiit.activities.secciones;

import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.nuietsiit.R;
import com.example.nuietsiit.activities.BaseActivity;
import com.example.nuietsiit.activities.auth.Login;
import com.example.nuietsiit.activities.main.AjustesCuenta;
import com.example.nuietsiit.activities.secciones.profesorado.BuscadorProfesores;
import com.example.nuietsiit.data.DBHelper;
import com.example.nuietsiit.data.TutoriasDB;
import com.example.nuietsiit.util.tutorial.Gesto;
import com.example.nuietsiit.util.tutorial.Tutorial;

import java.util.Locale;

public class Profesorado extends BaseActivity {

    private ImageView ivIcono;
    private LinearLayout layoutLoggedOut;
    private LinearLayout layoutLoggedIn;
    private Button btnLogin;
    private Button btnBuscarProfesorado;
    private LinearLayout contenedorTutoriasReservadas;

    private String correoUsuario;
    private DBHelper BD;
    private TutoriasDB tutoriasDB;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.profesorado);

        BD = DBHelper.getInstance(this);
        tutoriasDB = new TutoriasDB(BD);

        ivIcono = findViewById(R.id.ivIcono);
        layoutLoggedOut = findViewById(R.id.layoutLoggedOut);
        layoutLoggedIn = findViewById(R.id.layoutLoggedIn);
        btnLogin = findViewById(R.id.btnLogin);
        btnBuscarProfesorado = findViewById(R.id.btnBuscarProfesorado);
        contenedorTutoriasReservadas = findViewById(R.id.contenedorTutoriasReservadas);

        SharedPreferences prefs = getSharedPreferences("SesionUsuario", MODE_PRIVATE);
        correoUsuario = prefs.getString("CORREO", null);

        actualizarVistaSesion();
        configurarBotones();
    }

    @Override
    protected Tutorial crearTutorial() {
        return new Tutorial(this, "profesorado", 1)
                .tocar(R.id.btnLogin, getString(R.string.prof_tut_login))
                .soloSi(() -> correoUsuario == null || correoUsuario.isEmpty())
                .info(R.id.cardTutorias, getString(R.string.prof_tut_tutorias))
                .soloSi(() -> correoUsuario != null && !correoUsuario.isEmpty())
                .tocar(R.id.btnBuscarProfesorado, getString(R.string.prof_tut_buscar));
    }

    @Override
    protected void onResume() {
        super.onResume();
        SharedPreferences prefs = getSharedPreferences("SesionUsuario", MODE_PRIVATE);
        correoUsuario = prefs.getString("CORREO", null);
        actualizarVistaSesion();
    }

    private void configurarBotones() {
        ivIcono.setOnClickListener(v -> {
            if (correoUsuario == null || correoUsuario.isEmpty()) {
                startActivity(new Intent(Profesorado.this, Login.class));
            } else {
                Intent intent = new Intent(Profesorado.this, AjustesCuenta.class);
                intent.putExtra("CORREO", correoUsuario);
                startActivity(intent);
            }
        });

        btnLogin.setOnClickListener(v -> {
            avisarTutorial(Gesto.TOCAR);
            startActivity(new Intent(Profesorado.this, Login.class));
        });

        btnBuscarProfesorado.setOnClickListener(v -> {
            avisarTutorial(Gesto.TOCAR);
            Intent intent = new Intent(Profesorado.this, BuscadorProfesores.class);
            startActivity(intent);
        });
    }

    private void actualizarVistaSesion() {
        if (correoUsuario != null && !correoUsuario.isEmpty()) {
            layoutLoggedOut.setVisibility(View.GONE);
            layoutLoggedIn.setVisibility(View.VISIBLE);
            cargarTutoriasEstudiante();
        } else {
            layoutLoggedOut.setVisibility(View.VISIBLE);
            layoutLoggedIn.setVisibility(View.GONE);
        }
    }

    private void cargarTutoriasEstudiante() {
        contenedorTutoriasReservadas.removeAllViews();
        Cursor cursor = tutoriasDB.listarReservasDe(correoUsuario);

        if (cursor != null && cursor.moveToFirst()) {
            do {
                int idReserva = cursor.getInt(cursor.getColumnIndexOrThrow("idReserva"));
                String fecha = cursor.getString(cursor.getColumnIndexOrThrow("fecha"));
                String horaInicio = cursor.getString(cursor.getColumnIndexOrThrow("horaInicio"));
                String horaFin = cursor.getString(cursor.getColumnIndexOrThrow("horaFin"));
                String modalidad = cursor.getString(cursor.getColumnIndexOrThrow("modalidad"));
                String nombreProfesor = cursor.getString(cursor.getColumnIndexOrThrow("nombreProfesor"));
                String estado = cursor.getString(cursor.getColumnIndexOrThrow("estado"));

                View tarjetaReserva = crearTarjetaReserva(idReserva, nombreProfesor, fecha, horaInicio + " - " + horaFin, modalidad, estado);
                contenedorTutoriasReservadas.addView(tarjetaReserva);
            } while (cursor.moveToNext());
            cursor.close();
        } else {
            if (cursor != null) cursor.close();
            TextView tvVacio = new TextView(this);
            tvVacio.setText(R.string.prof_sin_tutorias_reservadas);
            tvVacio.setTextSize(14f);
            tvVacio.setTextColor(Color.GRAY);
            tvVacio.setPadding(0, 24, 0, 0);
            contenedorTutoriasReservadas.addView(tvVacio);
        }
    }

    private View crearTarjetaReserva(int idReserva, String nombreProfesor, String fecha, String horario, String modalidad, String estado) {
        Locale localeActivo = getResources().getConfiguration().getLocales().get(0);

        LinearLayout tarjeta = new LinearLayout(this);
        tarjeta.setOrientation(LinearLayout.VERTICAL);
        tarjeta.setBackgroundColor(Color.WHITE);
        tarjeta.setPadding(24, 24, 24, 24);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 0, 0, 20);
        tarjeta.setLayoutParams(params);

        TextView tvProfesor = new TextView(this);
        tvProfesor.setText(nombreProfesor);
        tvProfesor.setTextSize(16f);
        tvProfesor.setTypeface(null, Typeface.BOLD);
        tvProfesor.setTextColor(Color.BLACK);

        TextView tvDetalles = new TextView(this);
        tvDetalles.setText(getString(
                R.string.prof_reserva_detalles_fmt,
                fecha,
                horario,
                modalidad != null ? modalidad.toUpperCase(localeActivo) : ""
        ));
        tvDetalles.setTextSize(13f);
        tvDetalles.setTextColor(Color.DKGRAY);
        tvDetalles.setPadding(0, 8, 0, 8);

        TextView tvEstado = new TextView(this);
        String estadoTexto;
        if ("confirmada".equalsIgnoreCase(estado)) {
            estadoTexto = getString(R.string.prof_estado_confirmada);
            tvEstado.setTextColor(Color.parseColor("#388E3C")); // Verde
        } else if ("cancelada".equalsIgnoreCase(estado)) {
            estadoTexto = getString(R.string.prof_estado_cancelada);
            tvEstado.setTextColor(Color.RED);
        } else if ("pendiente".equalsIgnoreCase(estado)) {
            estadoTexto = getString(R.string.prof_estado_pendiente);
            tvEstado.setTextColor(Color.parseColor("#F57C00")); // Naranja
        } else {
            estadoTexto = estado != null ? estado.toUpperCase(localeActivo) : "";
            tvEstado.setTextColor(Color.parseColor("#F57C00"));
        }
        tvEstado.setText(getString(R.string.prof_reserva_estado_fmt, estadoTexto));
        tvEstado.setTextSize(13f);
        tvEstado.setTypeface(null, Typeface.BOLD);

        Button btnCancelar = new Button(this);
        btnCancelar.setText(R.string.prof_btn_cancelar_reserva);
        btnCancelar.setTextSize(12f);
        btnCancelar.setBackgroundColor(Color.parseColor("#E53935"));
        btnCancelar.setTextColor(Color.WHITE);
        LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        btnParams.setMargins(0, 12, 0, 0);
        btnCancelar.setLayoutParams(btnParams);

        if ("cancelada".equalsIgnoreCase(estado)) {
            btnCancelar.setVisibility(View.GONE);
        } else {
            btnCancelar.setOnClickListener(v -> {
                tutoriasDB.actualizarEstadoReserva(idReserva, "cancelada");
                Toast.makeText(this, R.string.prof_toast_reserva_cancelada, Toast.LENGTH_SHORT).show();
                cargarTutoriasEstudiante();
            });
        }

        tarjeta.addView(tvProfesor);
        tarjeta.addView(tvDetalles);
        tarjeta.addView(tvEstado);
        tarjeta.addView(btnCancelar);

        return tarjeta;
    }


}