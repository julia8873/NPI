package com.example.nuietsiit.activities.secciones;

import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

import com.example.nuietsiit.R;
import com.example.nuietsiit.activities.auth.Login;
import com.example.nuietsiit.activities.main.AjustesCuenta;
import com.example.nuietsiit.data.DBHelper;

import java.util.HashMap;
import java.util.Map;

public class Asignaturas extends AppCompatActivity {

    private ImageView ivIcono;
    private LinearLayout layoutLoggedOut;
    private LinearLayout layoutLoggedIn;
    private Button btnLogin;
    private Button btnVerMasAsignaturas;
    private TableLayout tablaHorario;

    private String correoUsuario;
    private DBHelper BD;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.asignaturas);

        // Inicializar Base de Datos
        BD = DBHelper.getInstance(this);

        // Referencias a las vistas
        ivIcono = findViewById(R.id.ivIcono);
        layoutLoggedOut = findViewById(R.id.layoutLoggedOut);
        layoutLoggedIn = findViewById(R.id.layoutLoggedIn);
        btnLogin = findViewById(R.id.btnLogin);
        btnVerMasAsignaturas = findViewById(R.id.btnVerMasAsignaturas);
        tablaHorario = findViewById(R.id.tablaHorario);

        // Leer sesión
        SharedPreferences prefs = getSharedPreferences("SesionUsuario", MODE_PRIVATE);
        correoUsuario = prefs.getString("CORREO", null);

        // Actualizar interfaz
        actualizarVistaSesion();

        // Configurar botones
        configurarBotones();
    }

    private void configurarBotones() {
        ivIcono.setOnClickListener(v -> {
            if (correoUsuario == null || correoUsuario.isEmpty()) {
                startActivity(new Intent(Asignaturas.this, Login.class));
            } else {
                Intent intent = new Intent(Asignaturas.this, AjustesCuenta.class);
                intent.putExtra("CORREO", correoUsuario);
                startActivity(intent);
            }
        });

        btnLogin.setOnClickListener(v -> startActivity(new Intent(Asignaturas.this, Login.class)));

        btnVerMasAsignaturas.setOnClickListener(v -> {
            // Aquí navegaremos al buscador de asignaturas (Fase 3)
            // Intent intent = new Intent(Asignaturas.this, BuscadorAsignaturasActivity.class);
            // startActivity(intent);
        });
    }

    private void actualizarVistaSesion() {
        if (correoUsuario != null && !correoUsuario.isEmpty()) {
            layoutLoggedOut.setVisibility(View.GONE);
            layoutLoggedIn.setVisibility(View.VISIBLE);
            cargarHorarioEstudiante();
        } else {
            layoutLoggedOut.setVisibility(View.VISIBLE);
            layoutLoggedIn.setVisibility(View.GONE);
        }
    }

    private void cargarHorarioEstudiante() {
        tablaHorario.removeAllViews(); // Limpiar tabla

        // 1. Obtener las clases
        Map<String, String> clasesHorario = obtenerClasesDesdeBD();

        // 2. Crear cabecera (Días de la semana)
        String[] dias = {"Hora", "Lunes", "Martes", "Miércoles", "Jueves", "Viernes"};
        TableRow rowCabecera = new TableRow(this);
        rowCabecera.setBackgroundColor(Color.parseColor("#E0E0E0"));

        for (String dia : dias) {
            TextView tv = crearCelda(dia, true, dia.equals("Hora"));
            rowCabecera.addView(tv);
        }
        tablaHorario.addView(rowCabecera);

        // 3. Crear las filas de horas (De 8:30 a 21:30, franjas de 1 hora)
        String[] horasInicio = {
                "08:30", "09:30", "10:30", "11:30", "12:30", "13:30",
                "14:30", "15:30", "16:30", "17:30", "18:30", "19:30", "20:30"
        };
        String[] horasFin = {
                "09:30", "10:30", "11:30", "12:30", "13:30", "14:30",
                "15:30", "16:30", "17:30", "18:30", "19:30", "20:30", "21:30"
        };

        for (int i = 0; i < horasInicio.length; i++) {
            TableRow row = new TableRow(this);
            // Añadir un poco más de margen superior/inferior para que la fila "respire" y ocupe más alto
            row.setPadding(0, 12, 0, 12);

            // Primera celda: La franja horaria completa (ej: 08:30\n09:30)
            String franja = horasInicio[i] + "\n" + horasFin[i];
            row.addView(crearCelda(franja, true, true));

            // Celdas de los días
            for (int dia = 1; dia <= 5; dia++) {
                String claveBusqueda = dia + "_" + horasInicio[i];
                String infoClase = clasesHorario.getOrDefault(claveBusqueda, "---");
                row.addView(crearCelda(infoClase, false, false));
            }
            tablaHorario.addView(row);
        }
    }

    /**
     - Método auxiliar modificado para dar más anchura a las celdas
     */
    private TextView crearCelda(String texto, boolean esCabecera, boolean esColumnaHora) {
        TextView tv = new TextView(this);
        tv.setText(texto);
        tv.setGravity(Gravity.CENTER);

        // Aumentar el padding interno de cada celda
        tv.setPadding(24, 24, 24, 24);

        // Definir la anchura: la columna de horas es más estrecha que las de los días
        if (esColumnaHora) {
            tv.setMinWidth(150); // Ancho mínimo en píxeles para las horas
        } else {
            tv.setMinWidth(300); // Ancho mínimo mayor para que quepan los nombres de asignaturas
        }

        if (esCabecera) {
            tv.setTypeface(null, Typeface.BOLD);
            tv.setTextColor(Color.BLACK);
        } else {
            tv.setTextColor(Color.DKGRAY);
            tv.setBackgroundResource(android.R.drawable.editbox_background_normal);
        }
        return tv;
    }

    /**
     - Consulta cruzada a la BD para obtener el horario del estudiante actual.
     */
    private Map<String, String> obtenerClasesDesdeBD() {
        Map<String, String> mapaClases = new HashMap<>();
        SQLiteDatabase db = BD.getReadableDatabase();

        // Consulta: Obtener Asignatura, Día y HoraInicio de las asignaturas en las que está matriculado
        String query = "SELECT A.nombreAsignatura, S.diaSemana, S.horaInicio, S.grupo, S.tipoSesion " +
                "FROM MATRICULA M " +
                "INNER JOIN ASIGNATURA A ON M.codigoAsignatura = A.codigoAsignatura " +
                "INNER JOIN SESION_CLASE S ON M.codigoAsignatura = S.codigoAsignatura " +
                "WHERE M.correoEstudiante = ?";

        Cursor cursor = db.rawQuery(query, new String[]{correoUsuario});

        if (cursor.moveToFirst()) {
            do {
                String nombreAsignatura = cursor.getString(0);
                int diaSemana = cursor.getInt(1);
                String horaInicio = cursor.getString(2); // Asume formato "HH:mm" ej "08:30"
                String grupo = cursor.getString(3);
                String tipo = cursor.getString(4); // "teoria" o "practicas"

                // Abreviar el nombre para que quepa mejor (puedes ajustar esto)
                String nombreAbreviado = nombreAsignatura.length() > 15
                        ? nombreAsignatura.substring(0, 15) + "..."
                        : nombreAsignatura;

                String contenidoCelda = nombreAbreviado + "\n(" + tipo + " " + grupo + ")";

                String clave = diaSemana + "_" + horaInicio;
                mapaClases.put(clave, contenidoCelda);

            } while (cursor.moveToNext());
        }
        cursor.close();
        return mapaClases;
    }

    /**
     - Método auxiliar para crear las celdas de texto de la tabla.
     */
    private TextView crearCelda(String texto, boolean esCabecera) {
        TextView tv = new TextView(this);
        tv.setText(texto);
        tv.setPadding(16, 16, 16, 16);
        tv.setGravity(Gravity.CENTER);

        if (esCabecera) {
            tv.setTypeface(null, Typeface.BOLD);
            tv.setTextColor(Color.BLACK);
        } else {
            tv.setTextColor(Color.DKGRAY);
            tv.setBackgroundResource(android.R.drawable.editbox_background_normal); // Añade un borde ligero
        }
        return tv;
    }
}