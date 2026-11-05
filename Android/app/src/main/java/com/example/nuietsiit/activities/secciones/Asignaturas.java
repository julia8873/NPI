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

import com.example.nuietsiit.R;
import com.example.nuietsiit.activities.BaseActivity;
import com.example.nuietsiit.activities.auth.Login;
import com.example.nuietsiit.activities.main.AjustesCuenta;
import com.example.nuietsiit.activities.secciones.asignaturas.BuscadorAsignaturas;
import com.example.nuietsiit.activities.secciones.asignaturas.DetalleAsignatura;
import com.example.nuietsiit.data.DBHelper;

import java.util.ArrayList;
import java.util.List;

public class Asignaturas extends BaseActivity {

    private ImageView ivIcono;
    private LinearLayout layoutLoggedOut;
    private LinearLayout layoutLoggedIn;
    private Button btnLogin;
    private Button btnVerMasAsignaturas;
    private TableLayout tablaHorario;

    private String correoUsuario;
    private DBHelper BD;

    private static class SesionHorarioInfo {
        String codigoAsignatura;
        String nombreAsignatura;
        int diaSemana;
        String horaInicio;
        String horaFin;
        String grupo;
        String tipoSesion;

        SesionHorarioInfo(String codigo, String nombre, int dia, String ini, String fin, String grupo, String tipo) {
            this.codigoAsignatura = codigo;
            this.nombreAsignatura = nombre;
            this.diaSemana = dia;
            this.horaInicio = ini;
            this.horaFin = fin;
            this.grupo = grupo;
            this.tipoSesion = tipo;
        }

        boolean ocupaSlot(int dia, String slotInicio) {
            if (this.diaSemana != dia) return false;
            return horaInicio.compareTo(slotInicio) <= 0 && horaFin.compareTo(slotInicio) > 0;
        }

        String textoCelda() {
            String nombreAbreviado = nombreAsignatura.length() > 15
                    ? nombreAsignatura.substring(0, 15) + "..."
                    : nombreAsignatura;
            return nombreAbreviado + "\n" + horaInicio + " - " + horaFin + "\n(" + tipoSesion + " " + grupo + ")";
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

        List<SesionHorarioInfo> sesiones = obtenerSesionesDesdeBD();

        // 2. Crear cabecera
        String[] dias = {"Hora", "Lunes", "Martes", "Miércoles", "Jueves", "Viernes"};
        TableRow rowCabecera = new TableRow(this);
        rowCabecera.setBackgroundColor(Color.parseColor("#E0E0E0"));

        for (String dia : dias) {
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

            String franja = horasInicio[i] + "\n" + horasFin[i];
            row.addView(crearCelda(franja, true, true, null));

            for (int dia = 1; dia <= 5; dia++) {
                SesionHorarioInfo encontrada = null;
                for (SesionHorarioInfo s : sesiones) {
                    if (s.ocupaSlot(dia, horasInicio[i])) {
                        encontrada = s;
                        break;
                    }
                }

                if (encontrada != null) {
                    row.addView(crearCelda(encontrada.textoCelda(), false, false, encontrada.codigoAsignatura));
                } else {
                    row.addView(crearCelda("---", false, false, null));
                }
            }
            tablaHorario.addView(row);
        }
    }

    private List<SesionHorarioInfo> obtenerSesionesDesdeBD() {
        List<SesionHorarioInfo> lista = new ArrayList<>();
        SQLiteDatabase db = BD.getReadableDatabase();

        String query = "SELECT A.codigoAsignatura, A.nombreAsignatura, S.diaSemana, S.horaInicio, S.horaFin, S.grupo, S.tipoSesion " +
                "FROM MATRICULA M " +
                "INNER JOIN ASIGNATURA A ON M.codigoAsignatura = A.codigoAsignatura " +
                "INNER JOIN SESION_CLASE S ON M.codigoAsignatura = S.codigoAsignatura " +
                "AND M.grupo = S.grupo " +
                "WHERE M.correoEstudiante = ?";

        Cursor cursor = db.rawQuery(query, new String[]{correoUsuario});

        if (cursor.moveToFirst()) {
            do {
                String codigoAsignatura = cursor.getString(0);
                String nombreAsignatura = cursor.getString(1);
                int diaSemana = cursor.getInt(2);
                String horaInicio = cursor.getString(3);
                String horaFin = cursor.getString(4);
                String grupo = cursor.getString(5);
                String tipo = cursor.getString(6);

                lista.add(new SesionHorarioInfo(codigoAsignatura, nombreAsignatura, diaSemana, horaInicio, horaFin, grupo, tipo));
            } while (cursor.moveToNext());
        }
        cursor.close();
        return lista;
    }

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
            if (codigoAsignatura != null) {
                tv.setBackgroundColor(Color.parseColor("#FFF3E0"));

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
