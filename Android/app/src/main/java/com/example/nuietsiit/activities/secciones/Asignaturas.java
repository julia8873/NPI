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
import com.example.nuietsiit.activities.BaseActivity;
import com.example.nuietsiit.activities.auth.Login;
import com.example.nuietsiit.activities.main.AjustesCuenta;
import com.example.nuietsiit.activities.secciones.asignaturas.BuscadorAsignaturas;
import com.example.nuietsiit.activities.secciones.asignaturas.DetalleAsignatura;
import com.example.nuietsiit.data.DBHelper;

import java.util.HashMap;
import java.util.Map;

public class Asignaturas extends BaseActivity {

    private ImageView ivIcono;
    private LinearLayout layoutLoggedOut;
    private LinearLayout layoutLoggedIn;
    private Button btnLogin;
    private Button btnVerMasAsignaturas;
    private TableLayout tablaHorario;

    private String correoUsuario;
    private DBHelper BD;

    // --- NUEVA CLASE AUXILIAR ---
    // Nos permite guardar el texto formateado y el código de la asignatura juntos
    private static class CeldaHorarioInfo {
        String textoMostrar;
        String codigoAsignatura;

        CeldaHorarioInfo(String textoMostrar, String codigoAsignatura) {
            this.textoMostrar = textoMostrar;
            this.codigoAsignatura = codigoAsignatura;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.asignaturas);

        BD = DBHelper.getInstance(this);

        ivIcono = findViewById(R.id.ivIcono);
        layoutLoggedOut = findViewById(R.id.layoutLoggedOut);
        layoutLoggedIn = findViewById(R.id.layoutLoggedIn);
        btnLogin = findViewById(R.id.btnLogin);
        btnVerMasAsignaturas = findViewById(R.id.btnVerMasAsignaturas);
        tablaHorario = findViewById(R.id.tablaHorario);

        SharedPreferences prefs = getSharedPreferences("SesionUsuario", MODE_PRIVATE);
        correoUsuario = prefs.getString("CORREO", null);

        actualizarVistaSesion();
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
            Intent intent = new Intent(Asignaturas.this, BuscadorAsignaturas.class);
            startActivity(intent);
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
        tablaHorario.removeAllViews();

        // 1. Obtener las clases con su info completa (texto + código)
        Map<String, CeldaHorarioInfo> clasesHorario = obtenerClasesDesdeBD();

        // 2. Crear cabecera
        String[] dias = {"Hora", "Lunes", "Martes", "Miércoles", "Jueves", "Viernes"};
        TableRow rowCabecera = new TableRow(this);
        rowCabecera.setBackgroundColor(Color.parseColor("#E0E0E0"));

        for (String dia : dias) {
            // Cabeceras: no son clicables, por lo que pasamos null en codigoAsignatura
            TextView tv = crearCelda(dia, true, dia.equals("Hora"), null);
            rowCabecera.addView(tv);
        }
        tablaHorario.addView(rowCabecera);

        // 3. Crear las filas de horas
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
            row.setPadding(0, 12, 0, 12);

            // Franja horaria (Tampoco es clicable)
            String franja = horasInicio[i] + "\n" + horasFin[i];
            row.addView(crearCelda(franja, true, true, null));

            // Celdas de los días
            for (int dia = 1; dia <= 5; dia++) {
                String claveBusqueda = dia + "_" + horasInicio[i];
                CeldaHorarioInfo info = clasesHorario.get(claveBusqueda);

                if (info != null) {
                    // Hay clase: le pasamos el texto y el código para que sea clicable
                    row.addView(crearCelda(info.textoMostrar, false, false, info.codigoAsignatura));
                } else {
                    // Hueco libre
                    row.addView(crearCelda("---", false, false, null));
                }
            }
            tablaHorario.addView(row);
        }
    }

    /**
     - Consulta cruzada a la BD para obtener el horario del estudiante actual.
     - MODIFICADA: Añadido el grupo a la condición JOIN y se extrae el código.
     */
    private Map<String, CeldaHorarioInfo> obtenerClasesDesdeBD() {
        Map<String, CeldaHorarioInfo> mapaClases = new HashMap<>();
        SQLiteDatabase db = BD.getReadableDatabase();

        String query = "SELECT A.codigoAsignatura, A.nombreAsignatura, S.diaSemana, S.horaInicio, S.grupo, S.tipoSesion " +
                "FROM MATRICULA M " +
                "INNER JOIN ASIGNATURA A ON M.codigoAsignatura = A.codigoAsignatura " +
                "INNER JOIN SESION_CLASE S ON M.codigoAsignatura = S.codigoAsignatura " +
                "AND M.grupo = S.grupo " + // ¡CRÍTICO! Para que no salgan las horas de otros grupos
                "WHERE M.correoEstudiante = ?";

        Cursor cursor = db.rawQuery(query, new String[]{correoUsuario});

        if (cursor.moveToFirst()) {
            do {
                String codigoAsignatura = cursor.getString(0);
                String nombreAsignatura = cursor.getString(1);
                int diaSemana = cursor.getInt(2);
                String horaInicio = cursor.getString(3);
                String grupo = cursor.getString(4);
                String tipo = cursor.getString(5);

                String nombreAbreviado = nombreAsignatura.length() > 15
                        ? nombreAsignatura.substring(0, 15) + "..."
                        : nombreAsignatura;

                String contenidoCelda = nombreAbreviado + "\n(" + tipo + " " + grupo + ")";
                String clave = diaSemana + "_" + horaInicio;

                mapaClases.put(clave, new CeldaHorarioInfo(contenidoCelda, codigoAsignatura));

            } while (cursor.moveToNext());
        }
        cursor.close();
        return mapaClases;
    }

    /**
     - Crea la celda. Si codigoAsignatura NO es nulo, la hace clicable y lanza el Intent.
     */
    private TextView crearCelda(String texto, boolean esCabecera, boolean esColumnaHora, String codigoAsignatura) {
        TextView tv = new TextView(this);
        tv.setText(texto);
        tv.setGravity(Gravity.CENTER);
        tv.setPadding(24, 24, 24, 24);

        if (esColumnaHora) {
            tv.setMinWidth(150);
        } else {
            tv.setMinWidth(300);
        }

        if (esCabecera) {
            tv.setTypeface(null, Typeface.BOLD);
            tv.setTextColor(Color.BLACK);
        } else {
            tv.setTextColor(Color.DKGRAY);
            // Si la celda tiene una asignatura (no es "---"), la resaltamos visualmente un poco
            if (codigoAsignatura != null) {
                tv.setBackgroundColor(Color.parseColor("#FFF3E0")); // Naranja muy clarito

                // Hacerla clicable
                tv.setOnClickListener(v -> {
                    Intent intent = new Intent(Asignaturas.this, DetalleAsignatura.class);
                    intent.putExtra("CODIGO_ASIGNATURA", codigoAsignatura);
                    startActivity(intent);
                });
            } else {
                tv.setBackgroundResource(android.R.drawable.editbox_background_normal);
            }
        }
        return tv;
    }
}