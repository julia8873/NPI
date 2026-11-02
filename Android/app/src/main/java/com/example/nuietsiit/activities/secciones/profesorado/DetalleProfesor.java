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
import com.example.nuietsiit.data.DBHelper;
import com.example.nuietsiit.data.TutoriasDB;
import com.example.nuietsiit.data.UsuariosDB;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;

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

        btnVolver = findViewById(R.id.btnVolverDetalle);
        tvNombreProfesor = findViewById(R.id.tvNombreProfesor);
        tvCorreoProfesor = findViewById(R.id.tvCorreoProfesor);
        tvDespacho = findViewById(R.id.tvDespacho);
        btnEmail = findViewById(R.id.btnEmail);
        btnWeb = findViewById(R.id.btnWeb);
        contenedorClases = findViewById(R.id.contenedorClases);
        contenedorTutorias = findViewById(R.id.contenedorTutorias);

        btnVolver.setOnClickListener(v -> finish());

        correoProfesor = getIntent().getStringExtra("CORREO_PROFESOR");

        if (correoProfesor != null) {
            cargarDatosProfesor();
            cargarDespacho();
            cargarClases();
            cargarTutorias();
        } else {
            Toast.makeText(this, "Profesor no encontrado", Toast.LENGTH_SHORT).show();
            finish();
        }

        btnEmail.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_SENDTO);
            intent.setData(Uri.parse("mailto:" + correoProfesor));
            startActivity(Intent.createChooser(intent, "Enviar correo a " + correoProfesor));
        });

        btnWeb.setOnClickListener(v -> {
            if (enlaceWebDocente != null && !enlaceWebDocente.isEmpty()) {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(enlaceWebDocente));
                startActivity(browserIntent);
            } else {
                Toast.makeText(this, "Página web docente no disponible", Toast.LENGTH_SHORT).show();
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
            tvDespacho.setText("Sin despacho asignado en la base de datos.");
        }
    }

    private void cargarClases() {
        contenedorClases.removeAllViews();
        Cursor cursor = usuariosDB.listarSesionesDocentesDeProfesor(correoProfesor);

        if (cursor != null && cursor.moveToFirst()) {
            do {
                String nombreAsignatura = cursor.getString(cursor.getColumnIndexOrThrow("nombreAsignatura"));
                int diaSemana = cursor.getInt(cursor.getColumnIndexOrThrow("diaSemana"));
                String horaInicio = cursor.getString(cursor.getColumnIndexOrThrow("horaInicio"));
                String horaFin = cursor.getString(cursor.getColumnIndexOrThrow("horaFin"));
                String tipoSesion = cursor.getString(cursor.getColumnIndexOrThrow("tipoSesion"));
                String grupo = cursor.getString(cursor.getColumnIndexOrThrow("grupo"));

                String diaNombre = obtenerNombreDia(diaSemana);
                String textoClase = nombreAsignatura + " (" + tipoSesion + " " + grupo + ")\n" + diaNombre + " de " + horaInicio + " a " + horaFin;

                TextView tvClase = new TextView(this);
                tvClase.setText(textoClase);
                tvClase.setTextSize(14f);
                tvClase.setTextColor(Color.DKGRAY);
                tvClase.setBackgroundColor(Color.parseColor("#F9F9F9"));
                tvClase.setPadding(20, 16, 20, 16);

                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
                );
                params.setMargins(0, 0, 0, 12);
                tvClase.setLayoutParams(params);

                contenedorClases.addView(tvClase);
            } while (cursor.moveToNext());
            cursor.close();
        } else {
            if (cursor != null) cursor.close();
            TextView tvVacio = new TextView(this);
            tvVacio.setText("No hay clases registradas para este profesor.");
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
            tvVacio.setText("No hay horarios de tutoría disponibles actualmente.");
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

        TextView tvTitulo = new TextView(this);
        tvTitulo.setText(diaNombre + ": " + horaInicio + " - " + horaFin);
        tvTitulo.setTextSize(16f);
        tvTitulo.setTypeface(null, Typeface.BOLD);
        tvTitulo.setTextColor(Color.BLACK);

        TextView tvModalidad = new TextView(this);
        tvModalidad.setText("Modalidad: " + modalidad.toUpperCase());
        tvModalidad.setTextSize(13f);
        tvModalidad.setTextColor(Color.DKGRAY);
        tvModalidad.setPadding(0, 4, 0, 12);

        Button btnReservar = new Button(this);
        btnReservar.setText("Reservar Tutoría");
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
                    .setTitle("Sesión requerida")
                    .setMessage("Para reservar una tutoría debes haber iniciado sesión previamente con tus credenciales. ¿Deseas iniciar sesión ahora?")
                    .setPositiveButton("Iniciar sesión", (dialog, which) -> {
                        startActivity(new Intent(DetalleProfesor.this, Login.class));
                    })
                    .setNegativeButton("Cancelar", null)
                    .show();
        } else {
            mostrarDialogoConfirmarReserva(idHorario, correoEstudiante, diaSemana, horaInicio, horaFin, modalidad, enlaceOnline);
        }
    }

    private void mostrarDialogoConfirmarReserva(int idHorario, String correoEstudiante, int diaSemana, String horaInicio, String horaFin, String modalidad, String enlaceOnline) {
        LocalDate proximaFecha = calcularProximaFecha(diaSemana);
        String fechaStr = proximaFecha.toString();

        if (tutoriasDB.slotOcupado(idHorario, fechaStr)) {
            Toast.makeText(this, "Esta franja ya está reservada para el día " + fechaStr, Toast.LENGTH_LONG).show();
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Confirmar Reserva de Tutoría");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(40, 20, 40, 10);

        TextView tvInfo = new TextView(this);
        tvInfo.setText("Fecha sugerida: " + fechaStr + "\nHorario: " + horaInicio + " - " + horaFin + "\nModalidad: " + modalidad.toUpperCase());
        tvInfo.setTextSize(14f);
        tvInfo.setTextColor(Color.BLACK);
        layout.addView(tvInfo);

        EditText etMotivo = new EditText(this);
        etMotivo.setHint("Escribe el motivo o asunto de la tutoría (opcional)");
        etMotivo.setTextSize(14f);
        layout.addView(etMotivo);

        builder.setView(layout);

        builder.setPositiveButton("Confirmar Reserva", (dialog, which) -> {
            String motivo = etMotivo.getText().toString().trim();
            long res = tutoriasDB.insertarReserva(idHorario, correoEstudiante, fechaStr, motivo, enlaceOnline);
            if (res != -1) {
                Toast.makeText(this, "¡Reserva realizada con éxito!", Toast.LENGTH_LONG).show();
                finish();
            } else {
                Toast.makeText(this, "Error al guardar la reserva.", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancelar", null);
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
        switch (diaSemana) {
            case 1: return "Lunes";
            case 2: return "Martes";
            case 3: return "Miércoles";
            case 4: return "Jueves";
            case 5: return "Viernes";
            case 6: return "Sábado";
            case 7: return "Domingo";
            default: return "Día " + diaSemana;
        }
    }
}
