package com.example.nuietsiit.activities.secciones.asignaturas;

import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
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
import com.example.nuietsiit.data.DBHelper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
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

    // Paleta de colores suaves (fondo, borde, texto principal) para diferenciar asignaturas
    private static final int[][] PALETA_ASIGNATURAS = {
            {Color.parseColor("#FFF3EB"), Color.parseColor("#FF8C42"), Color.parseColor("#9A3412")}, // Naranja NUI
            {Color.parseColor("#EEF2FF"), Color.parseColor("#6366F1"), Color.parseColor("#3730A3")}, // Índigo
            {Color.parseColor("#ECFDF5"), Color.parseColor("#10B981"), Color.parseColor("#065F46")}, // Esmeralda
            {Color.parseColor("#F5F3FF"), Color.parseColor("#8B5CF6"), Color.parseColor("#5B21B6")}, // Violeta
            {Color.parseColor("#F0F9FF"), Color.parseColor("#0EA5E9"), Color.parseColor("#075985")}, // Azul cielo
            {Color.parseColor("#FDF2F8"), Color.parseColor("#EC4899"), Color.parseColor("#9D174D")}  // Rosa
    };

    private final Map<String, Integer> mapaColorAsignatura = new HashMap<>();

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

        View btnVolver = findViewById(R.id.btnVolver);
        if (btnVolver != null) {
            btnVolver.setOnClickListener(v -> {
                v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
                finish();
            });
        }

        SharedPreferences prefs = getSharedPreferences("SesionUsuario", MODE_PRIVATE);
        correoUsuario = prefs.getString("CORREO", null);

        actualizarVistaSesion();
        configurarBotones();
    }

    private void configurarBotones() {
        if (ivIcono != null) {
            ivIcono.setOnClickListener(v -> {
                if (correoUsuario == null || correoUsuario.isEmpty()) {
                    startActivity(new Intent(Asignaturas.this, Login.class));
                } else {
                    Intent intent = new Intent(Asignaturas.this, AjustesCuenta.class);
                    intent.putExtra("CORREO", correoUsuario);
                    startActivity(intent);
                }
            });
        }

        if (btnLogin != null) {
            btnLogin.setOnClickListener(v -> startActivity(new Intent(Asignaturas.this, Login.class)));
        }

        if (btnVerMasAsignaturas != null) {
            btnVerMasAsignaturas.setOnClickListener(v -> {
                v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
                Intent intent = new Intent(Asignaturas.this, BuscadorAsignaturas.class);
                startActivity(intent);
            });
        }
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

    private int obtenerIndiceColor(String codigoAsignatura) {
        if (!mapaColorAsignatura.containsKey(codigoAsignatura)) {
            int idx = mapaColorAsignatura.size() % PALETA_ASIGNATURAS.length;
            mapaColorAsignatura.put(codigoAsignatura, idx);
        }
        return mapaColorAsignatura.get(codigoAsignatura);
    }

    private void cargarHorarioEstudiante() {
        tablaHorario.removeAllViews();
        mapaColorAsignatura.clear();

        List<SesionHorarioInfo> sesiones = obtenerSesionesDesdeBD();
        float d = getResources().getDisplayMetrics().density;

        String[] dias = {
                getString(R.string.cabecera_hora),
                getString(R.string.dia_lunes),
                getString(R.string.dia_martes),
                getString(R.string.dia_miercoles),
                getString(R.string.dia_jueves),
                getString(R.string.dia_viernes)
        };

        // 1. FILA DE CABECERA DE DÍAS
        TableRow rowCabecera = new TableRow(this);
        TableLayout.LayoutParams paramsHeaderRow = new TableLayout.LayoutParams(
                TableLayout.LayoutParams.WRAP_CONTENT,
                TableLayout.LayoutParams.WRAP_CONTENT
        );
        paramsHeaderRow.setMargins(0, 0, 0, (int) (8 * d));
        rowCabecera.setLayoutParams(paramsHeaderRow);

        for (int c = 0; c < dias.length; c++) {
            boolean esHora = (c == 0);
            TextView tvCab = crearCeldaCabecera(dias[c], esHora, d);
            rowCabecera.addView(tvCab);
        }
        tablaHorario.addView(rowCabecera);

        // 2. FRANJAS HORARIAS
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
            TableLayout.LayoutParams rowParams = new TableLayout.LayoutParams(
                    TableLayout.LayoutParams.WRAP_CONTENT,
                    TableLayout.LayoutParams.WRAP_CONTENT
            );
            rowParams.setMargins(0, 0, 0, (int) (6 * d));
            row.setLayoutParams(rowParams);

            // Columna de la hora estilizada
            row.addView(crearCeldaHora(horasInicio[i], horasFin[i], d));

            // Celdas Lunes a Viernes
            for (int dia = 1; dia <= 5; dia++) {
                SesionHorarioInfo encontrada = null;
                for (SesionHorarioInfo s : sesiones) {
                    if (s.ocupaSlot(dia, horasInicio[i])) {
                        encontrada = s;
                        break;
                    }
                }

                if (encontrada != null) {
                    row.addView(crearTarjetaSesion(encontrada, d));
                } else {
                    row.addView(crearCeldaVacia(d));
                }
            }
            tablaHorario.addView(row);
        }
    }

    private TextView crearCeldaCabecera(String texto, boolean esColumnaHora, float d) {
        TextView tv = new TextView(this);
        tv.setText(texto.toUpperCase(Locale.ROOT));
        tv.setGravity(Gravity.CENTER);
        tv.setTypeface(null, Typeface.BOLD);
        tv.setTextSize(11.5f);
        tv.setLetterSpacing(0.05f);
        tv.setPadding((int) (8 * d), (int) (10 * d), (int) (8 * d), (int) (10 * d));

        TableRow.LayoutParams params = new TableRow.LayoutParams(
                esColumnaHora ? (int) (68 * d) : (int) (136 * d),
                TableRow.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(esColumnaHora ? 0 : (int) (6 * d), 0, 0, 0);
        tv.setLayoutParams(params);

        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(10 * d);
        if (esColumnaHora) {
            bg.setColor(Color.parseColor("#F1F5F9"));
            tv.setTextColor(Color.parseColor("#64748B"));
        } else {
            bg.setColor(Color.parseColor("#0F172A"));
            tv.setTextColor(Color.WHITE);
        }
        tv.setBackground(bg);

        return tv;
    }

    private TextView crearCeldaHora(String ini, String fin, float d) {
        TextView tv = new TextView(this);
        tv.setGravity(Gravity.CENTER);
        tv.setPadding((int) (4 * d), (int) (10 * d), (int) (4 * d), (int) (10 * d));

        SpannableString span = new SpannableString(ini + "\n" + fin);
        span.setSpan(new StyleSpan(Typeface.BOLD), 0, ini.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        span.setSpan(new ForegroundColorSpan(Color.parseColor("#0F172A")), 0, ini.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        span.setSpan(new RelativeSizeSpan(1.05f), 0, ini.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        span.setSpan(new ForegroundColorSpan(Color.parseColor("#94A3B8")), ini.length() + 1, span.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        span.setSpan(new RelativeSizeSpan(0.85f), ini.length() + 1, span.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        tv.setText(span);
        tv.setTextSize(12f);

        TableRow.LayoutParams params = new TableRow.LayoutParams(
                (int) (68 * d),
                TableRow.LayoutParams.MATCH_PARENT
        );
        tv.setLayoutParams(params);

        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(10 * d);
        bg.setColor(Color.parseColor("#F8FAFC"));
        tv.setBackground(bg);

        return tv;
    }

    private View crearTarjetaSesion(SesionHorarioInfo sesion, float d) {
        int colorIdx = obtenerIndiceColor(sesion.codigoAsignatura);
        int colorFondo = PALETA_ASIGNATURAS[colorIdx][0];
        int colorBorde = PALETA_ASIGNATURAS[colorIdx][1];
        int colorTexto = PALETA_ASIGNATURAS[colorIdx][2];

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding((int) (10 * d), (int) (8 * d), (int) (10 * d), (int) (8 * d));

        TableRow.LayoutParams params = new TableRow.LayoutParams(
                (int) (136 * d),
                TableRow.LayoutParams.MATCH_PARENT
        );
        params.setMargins((int) (6 * d), 0, 0, 0);
        card.setLayoutParams(params);

        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(12 * d);
        bg.setColor(colorFondo);
        bg.setStroke((int) (1.2f * d), colorBorde);
        card.setBackground(bg);

        // Nombre de la asignatura (máx 2 líneas)
        TextView tvNombre = new TextView(this);
        tvNombre.setText(sesion.nombreAsignatura);
        tvNombre.setTextColor(colorTexto);
        tvNombre.setTextSize(12f);
        tvNombre.setTypeface(null, Typeface.BOLD);
        tvNombre.setMaxLines(2);
        tvNombre.setEllipsize(TextUtils.TruncateAt.END);

        // Etiqueta inferior: Tipo · Grupo (ej. TEORÍA · A)
        TextView tvDetalle = new TextView(this);
        String tipoFormateado = sesion.tipoSesion != null ? sesion.tipoSesion.toUpperCase(Locale.ROOT) : "";
        tvDetalle.setText(tipoFormateado + " · " + sesion.grupo);
        tvDetalle.setTextColor(colorBorde);
        tvDetalle.setTextSize(10f);
        tvDetalle.setTypeface(null, Typeface.BOLD);
        tvDetalle.setPadding(0, (int) (3 * d), 0, 0);
        tvDetalle.setMaxLines(1);
        tvDetalle.setEllipsize(TextUtils.TruncateAt.END);

        card.addView(tvNombre);
        card.addView(tvDetalle);

        card.setOnClickListener(v -> {
            v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
            Intent intent = new Intent(Asignaturas.this, DetalleAsignatura.class);
            intent.putExtra("CODIGO_ASIGNATURA", sesion.codigoAsignatura);
            startActivity(intent);
        });

        return card;
    }

    private View crearCeldaVacia(float d) {
        View v = new View(this);
        TableRow.LayoutParams params = new TableRow.LayoutParams(
                (int) (136 * d),
                (int) (56 * d)
        );
        params.setMargins((int) (6 * d), 0, 0, 0);
        v.setLayoutParams(params);

        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(10 * d);
        bg.setColor(Color.parseColor("#F8FAFC"));
        bg.setStroke((int) (0.8f * d), Color.parseColor("#E2E8F0"));
        v.setBackground(bg);

        return v;
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
}