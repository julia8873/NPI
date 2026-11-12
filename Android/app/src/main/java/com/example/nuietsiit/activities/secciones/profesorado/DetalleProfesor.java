package com.example.nuietsiit.activities.secciones.profesorado;

import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.example.nuietsiit.R;
import com.example.nuietsiit.activities.BaseActivity;
import com.example.nuietsiit.activities.auth.Login;
import com.example.nuietsiit.activities.secciones.asignaturas.DetalleAsignatura;
import com.example.nuietsiit.data.DBHelper;
import com.example.nuietsiit.data.TutoriasDB;
import com.example.nuietsiit.data.UsuariosDB;
import com.example.nuietsiit.util.RecienteUtil;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.Locale;

public class DetalleProfesor extends BaseActivity {

    private TextView btnVolver;
    private TextView tvNombreProfesor;
    private TextView tvCorreoProfesor;
    private TextView tvDespacho;
    private Button btnEmail;
    private Button btnWeb;
    private LinearLayout contenedorClases;
    private LinearLayout contenedorTutorias;

    private DBHelper BD;
    private UsuariosDB usuariosDB;
    private TutoriasDB tutoriasDB;

    private String correoProfesor;
    private String enlaceWebDocente = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.detalle_profesor);

        BD = DBHelper.getInstance(this);
        usuariosDB = new UsuariosDB(BD);
        tutoriasDB = new TutoriasDB(BD);

        View btnVolver = findViewById(R.id.btnVolver);
        if (btnVolver != null) {
            btnVolver.setOnClickListener(v -> finish());
        }
        tvNombreProfesor = findViewById(R.id.tvNombreProfesor);
        tvCorreoProfesor = findViewById(R.id.tvCorreoProfesor);
        tvDespacho = findViewById(R.id.tvDespacho);
        btnEmail = findViewById(R.id.btnEmail);
        btnWeb = findViewById(R.id.btnWeb);
        contenedorClases = findViewById(R.id.contenedorClases);
        contenedorTutorias = findViewById(R.id.contenedorTutorias);

        correoProfesor = getIntent().getStringExtra("CORREO_PROFESOR");

        if (correoProfesor != null) {
            cargarDatosProfesor();
            cargarDespacho();
            cargarClases();
            cargarTutorias();
        } else {
            Toast.makeText(this, R.string.prof_no_encontrado, Toast.LENGTH_SHORT).show();
            finish();
        }

        btnEmail.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_SENDTO);
            intent.setData(Uri.parse("mailto:" + correoProfesor));
            startActivity(Intent.createChooser(intent, getString(R.string.prof_enviar_correo_fmt, correoProfesor)));
        });

        btnWeb.setOnClickListener(v -> {
            if (enlaceWebDocente != null && !enlaceWebDocente.isEmpty()) {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(enlaceWebDocente));
                startActivity(browserIntent);
            } else {
                Toast.makeText(this, R.string.prof_web_no_disponible, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void cargarDatosProfesor() {
        Cursor cursor = usuariosDB.obtenerProfesor(correoProfesor);
        if (cursor != null && cursor.moveToFirst()) {
            String nombre = cursor.getString(cursor.getColumnIndexOrThrow("nombreProfesor"));
            enlaceWebDocente = cursor.getString(cursor.getColumnIndexOrThrow("enlacePaginaWeb"));

            tvNombreProfesor.setText(nombre);
            tvCorreoProfesor.setText(correoProfesor);
            cursor.close();

            RecienteUtil.guardar(
                    this,
                    DetalleProfesor.class,
                    getString(R.string.prof_reciente_titulo_fmt, nombre),
                    getString(R.string.prof_reciente_subtitulo),
                    "CORREO_PROFESOR",
                    correoProfesor,
                    3
            );
        }
    }

    private void cargarDespacho() {
        Cursor cursor = usuariosDB.obtenerDespachosDe(correoProfesor);
        if (cursor != null && cursor.moveToFirst()) {
            String nombreEspacio = cursor.getString(cursor.getColumnIndexOrThrow("nombreEspacio"));
            String descripcion = cursor.getString(cursor.getColumnIndexOrThrow("descripcion"));
            tvDespacho.setText(nombreEspacio + (descripcion != null ? " (" + descripcion + ")" : ""));
            cursor.close();
        } else {
            if (cursor != null) cursor.close();
            tvDespacho.setText(R.string.prof_sin_despacho);
        }
    }

    private void cargarClases() {
        contenedorClases.removeAllViews();
        Cursor cursor = usuariosDB.listarSesionesDocentesDeProfesor(correoProfesor);

        if (cursor != null && cursor.moveToFirst()) {
            do {
                String codigoAsignatura = cursor.getString(cursor.getColumnIndexOrThrow("codigoAsignatura"));
                String nombreAsignatura = cursor.getString(cursor.getColumnIndexOrThrow("nombreAsignatura"));
                int diaSemana = cursor.getInt(cursor.getColumnIndexOrThrow("diaSemana"));
                String horaInicio = cursor.getString(cursor.getColumnIndexOrThrow("horaInicio"));
                String horaFin = cursor.getString(cursor.getColumnIndexOrThrow("horaFin"));
                String tipoSesion = cursor.getString(cursor.getColumnIndexOrThrow("tipoSesion"));
                String grupo = cursor.getString(cursor.getColumnIndexOrThrow("grupo"));

                String diaNombre = obtenerNombreDia(diaSemana);
                String textoClase = getString(
                        R.string.prof_clase_item_fmt,
                        nombreAsignatura,
                        tipoSesion,
                        grupo,
                        diaNombre,
                        horaInicio,
                        horaFin
                );

                TextView tvClase = new TextView(this);
                tvClase.setText(textoClase);
                tvClase.setTextSize(14f);
                tvClase.setTextColor(Color.parseColor("#0F172A"));
                tvClase.setBackgroundColor(Color.parseColor("#F8FAFC"));
                tvClase.setPadding(24, 16, 24, 16);

                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
                );
                params.setMargins(0, 0, 0, 12);
                tvClase.setLayoutParams(params);

                if (codigoAsignatura != null) {
                    tvClase.setOnClickListener(v -> {
                        Intent intent = new Intent(DetalleProfesor.this, DetalleAsignatura.class);
                        intent.putExtra("CODIGO_ASIGNATURA", codigoAsignatura);
                        startActivity(intent);
                    });
                }

                contenedorClases.addView(tvClase);
            } while (cursor.moveToNext());
            cursor.close();
        } else {
            if (cursor != null) cursor.close();
            TextView tvVacio = new TextView(this);
            tvVacio.setText(R.string.prof_sin_clases);
            tvVacio.setTextSize(14f);
            tvVacio.setTextColor(Color.GRAY);
            contenedorClases.addView(tvVacio);
        }
    }

    private void cargarTutorias() {
        contenedorTutorias.removeAllViews();
        Cursor cursor = tutoriasDB.listarHorariosDe(correoProfesor);

        if (cursor != null && cursor.moveToFirst()) {
            do {
                int idHorario = cursor.getInt(cursor.getColumnIndexOrThrow("idHorario"));
                int diaSemana = cursor.getInt(cursor.getColumnIndexOrThrow("diaSemana"));
                String horaInicio = cursor.getString(cursor.getColumnIndexOrThrow("horaInicio"));
                String horaFin = cursor.getString(cursor.getColumnIndexOrThrow("horaFin"));
                String modalidad = cursor.getString(cursor.getColumnIndexOrThrow("modalidad"));
                String enlaceOnline = cursor.getString(cursor.getColumnIndexOrThrow("enlaceOnline"));

                View tarjetaTutoria = crearTarjetaTutoria(idHorario, diaSemana, horaInicio, horaFin, modalidad, enlaceOnline);
                contenedorTutorias.addView(tarjetaTutoria);
            } while (cursor.moveToNext());
            cursor.close();
        } else {
            if (cursor != null) cursor.close();
            TextView tvVacio = new TextView(this);
            tvVacio.setText(R.string.prof_sin_tutorias);
            tvVacio.setTextSize(14f);
            tvVacio.setTextColor(Color.GRAY);
            contenedorTutorias.addView(tvVacio);
        }
    }

    private View crearTarjetaTutoria(int idHorario, int diaSemana, String horaInicio, String horaFin, String modalidad, String enlaceOnline) {
        LinearLayout tarjeta = new LinearLayout(this);
        tarjeta.setOrientation(LinearLayout.VERTICAL);
        tarjeta.setBackgroundColor(Color.parseColor("#FFF8E1")); // Fondo crema
        tarjeta.setPadding(24, 24, 24, 24);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 0, 0, 16);
        tarjeta.setLayoutParams(params);

        String diaNombre = obtenerNombreDia(diaSemana);
        Locale localeActivo = getResources().getConfiguration().getLocales().get(0);

        TextView tvTitulo = new TextView(this);
        tvTitulo.setText(getString(R.string.prof_tutoria_horario_fmt, diaNombre, horaInicio, horaFin));
        tvTitulo.setTextSize(16f);
        tvTitulo.setTypeface(null, Typeface.BOLD);
        tvTitulo.setTextColor(Color.BLACK);

        TextView tvModalidad = new TextView(this);
        tvModalidad.setText(getString(R.string.prof_tutoria_modalidad_fmt, modalidad.toUpperCase(localeActivo)));
        tvModalidad.setTextSize(13f);
        tvModalidad.setTextColor(Color.DKGRAY);
        tvModalidad.setPadding(0, 4, 0, 12);

        Button btnReservar = new Button(this);
        btnReservar.setText(R.string.prof_btn_reservar_tutoria);
        btnReservar.setBackgroundColor(Color.parseColor("#FF9800"));
        btnReservar.setTextColor(Color.WHITE);

        btnReservar.setOnClickListener(v -> intentarReservarTutoria(idHorario, diaSemana, horaInicio, horaFin, modalidad, enlaceOnline));

        tarjeta.addView(tvTitulo);
        tarjeta.addView(tvModalidad);
        tarjeta.addView(btnReservar);

        return tarjeta;
    }

    private void intentarReservarTutoria(int idHorario, int diaSemana, String horaInicio, String horaFin, String modalidad, String enlaceOnline) {
        SharedPreferences prefs = getSharedPreferences("SesionUsuario", MODE_PRIVATE);
        String correoEstudiante = prefs.getString("CORREO", null);

        if (correoEstudiante == null || correoEstudiante.isEmpty()) {
            new AlertDialog.Builder(this)
                    .setTitle(R.string.prof_sesion_requerida_titulo)
                    .setMessage(R.string.prof_sesion_requerida_mensaje)
                    .setPositiveButton(R.string.prof_btn_iniciar_sesion, (dialog, which) -> {
                        startActivity(new Intent(DetalleProfesor.this, Login.class));
                    })
                    .setNegativeButton(R.string.prof_btn_cancelar, null)
                    .show();
        } else {
            mostrarDialogoConfirmarReserva(idHorario, correoEstudiante, diaSemana, horaInicio, horaFin, modalidad, enlaceOnline);
        }
    }

    private void mostrarDialogoConfirmarReserva(int idHorario, String correoEstudiante, int diaSemana, String horaInicio, String horaFin, String modalidad, String enlaceOnline) {
        LocalDate proximaFecha = calcularProximaFecha(diaSemana);
        String fechaStr = proximaFecha.toString();

        if (tutoriasDB.slotOcupado(idHorario, fechaStr)) {
            Toast.makeText(this, getString(R.string.prof_tutoria_ocupada_fmt, fechaStr), Toast.LENGTH_LONG).show();
            return;
        }

        Locale localeActivo = getResources().getConfiguration().getLocales().get(0);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.prof_confirmar_reserva_titulo);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(40, 20, 40, 10);

        TextView tvInfo = new TextView(this);
        tvInfo.setText(getString(
                R.string.prof_confirmar_reserva_info_fmt,
                fechaStr,
                horaInicio,
                horaFin,
                modalidad.toUpperCase(localeActivo)
        ));
        tvInfo.setTextSize(14f);
        tvInfo.setTextColor(Color.BLACK);
        layout.addView(tvInfo);

        EditText etMotivo = new EditText(this);
        etMotivo.setHint(R.string.prof_motivo_tutoria_hint);
        etMotivo.setTextSize(14f);
        layout.addView(etMotivo);

        builder.setView(layout);

        builder.setPositiveButton(R.string.prof_btn_confirmar_reserva, (dialog, which) -> {
            String motivo = etMotivo.getText().toString().trim();
            long res = tutoriasDB.insertarReserva(idHorario, correoEstudiante, fechaStr, motivo, enlaceOnline);
            if (res != -1) {
                Toast.makeText(this, R.string.prof_reserva_exito, Toast.LENGTH_LONG).show();
                finish();
            } else {
                Toast.makeText(this, R.string.prof_reserva_error, Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton(R.string.prof_btn_cancelar, null);
        builder.show();
    }

    private LocalDate calcularProximaFecha(int diaSemanaTarget) {
        DayOfWeek targetDay = DayOfWeek.of(diaSemanaTarget);
        LocalDate hoy = LocalDate.now();
        if (hoy.getDayOfWeek() == targetDay) {
            return hoy;
        }
        return hoy.with(TemporalAdjusters.next(targetDay));
    }

    private String obtenerNombreDia(int diaSemana) {
        String[] dias = getResources().getStringArray(R.array.prof_dias_semana);
        if (diaSemana >= 1 && diaSemana <= dias.length) {
            return dias[diaSemana - 1];
        }
        return getString(R.string.prof_dia_fallback_fmt, diaSemana);
    }
}