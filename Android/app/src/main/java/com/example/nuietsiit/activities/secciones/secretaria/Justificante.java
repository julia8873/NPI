package com.example.nuietsiit.activities.secciones.secretaria;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.nuietsiit.R;
import com.example.nuietsiit.data.DBHelper;
import com.example.nuietsiit.data.SecretariaDB;
import com.example.nuietsiit.data.modelo.Cita;
import com.example.nuietsiit.data.modelo.Solicitud;
import com.example.nuietsiit.util.CalendarioUtil;
import com.example.nuietsiit.util.FechasUtil;
import com.example.nuietsiit.util.GeneradorQR;
import com.example.nuietsiit.util.SesionUtil;
import com.google.android.material.appbar.MaterialToolbar;


/**
 * Justificante para enseñar en ventanilla: código, resumen y QR grande.
 * Sirve para citas (extra idCita) y solicitudes (extra idSolicitud).
 * Mientras está abierto: brillo al máximo y pantalla siempre encendida.
 * QR: pellizcar amplía de 1× a 3× (sin pasar del ancho de pantalla), doble toque vuelve a 1×.
 */
public class Justificante extends AppCompatActivity {

    private static final float ESCALA_MIN = 1f;
    private static final float ESCALA_MAX = 3f;
    private static final int DURACION_CITA_MIN = 10;

    private ImageView ivQR;
    private float escalaQR = ESCALA_MIN;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.justificante);

        MaterialToolbar barra = findViewById(R.id.toolbar);
        setSupportActionBar(barra);
        getSupportActionBar().setTitle("Justificante");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        String correoUsuario = SesionUtil.correo(getIntent());

        // solo se enseñan justificantes del usuario que los pide
        SecretariaDB db = new SecretariaDB(DBHelper.getInstance(this));
        Cita cita = db.getCita(getIntent().getIntExtra("idCita", -1));
        Solicitud solicitud = db.getSolicitud(getIntent().getIntExtra("idSolicitud", -1));
        if (cita != null && correoUsuario.equals(cita.correoUsuario)) {
            mostrarCita(cita);
        } else if (solicitud != null && correoUsuario.equals(solicitud.correoUsuario)) {
            mostrarSolicitud(solicitud);
        } else {
            Toast.makeText(this, "Justificante no encontrado", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        prepararZoomQR();
    }

    private void mostrarCita(Cita cita) {
        ((TextView) findViewById(R.id.tvCodigo)).setText(cita.codigo());
        ((TextView) findViewById(R.id.tvMotivo)).setText(cita.motivo());
        ((TextView) findViewById(R.id.tvFecha)).setText(FechasUtil.capitalizar(FechasUtil.diaLargo(cita.fechaHora())));
        ((TextView) findViewById(R.id.tvHora)).setText(cita.hora);

        TextView tvEstado = findViewById(R.id.tvEstado);
        if (Cita.CANCELADA.equals(cita.estado)) {
            mostrarEstado(tvEstado, "Cita cancelada", 0xFFD32F2F);
        } else if (!cita.esFutura()) {
            mostrarEstado(tvEstado, "Cita pasada", 0xFF666666);
        } else {
            mostrarBotonCalendario(cita);
        }

        mostrarQR(cita.codigo());
    }

    private void mostrarSolicitud(Solicitud solicitud) {
        ((TextView) findViewById(R.id.tvCodigo)).setText(solicitud.codigo());
        ((TextView) findViewById(R.id.tvMotivo)).setText(solicitud.nombreTramite);
        ((TextView) findViewById(R.id.tvFecha)).setText(solicitud.textoEnvio());
        findViewById(R.id.tvHora).setVisibility(View.GONE);

        String estado = solicitud.estado();
        mostrarEstado(findViewById(R.id.tvEstado), "Estado: " + estado, Solicitud.colorEstado(estado));

        mostrarQR(solicitud.codigo());
    }

    private void mostrarEstado(TextView tv, String texto, int color) {
        tv.setVisibility(View.VISIBLE);
        tv.setText(texto);
        tv.setTextColor(color);
    }

    // solo citas activas y futuras. Se abre la app de calendario para que el usuario confirme
    // (sin permisos de calendario ni eventos que haya que mantener sincronizados)
    private void mostrarBotonCalendario(Cita cita) {
        Button btn = findViewById(R.id.btnCalendario);
        btn.setVisibility(View.VISIBLE);
        btn.setOnClickListener(v -> {
            boolean abierto = CalendarioUtil.anadirEvento(this,
                    "Cita Secretaría ETSIIT: " + cita.motivo(),
                    "Justificante " + cita.codigo() + ". Enseña el QR de la app en la ventanilla.",
                    "Secretaría ETSIIT",
                    cita.fechaHora(), cita.fechaHora().plusMinutes(DURACION_CITA_MIN));
            if (!abierto) Toast.makeText(this, "No hay ninguna app de calendario", Toast.LENGTH_SHORT).show();
        });
        // viene de CitaPrevia tras cambiar una cita que ya existía
        if (getIntent().getBooleanExtra("citaCambiada", false)) {
            findViewById(R.id.tvAvisoCalendario).setVisibility(View.VISIBLE);
        }
    }

    private void mostrarQR(String codigo) {
        ivQR = findViewById(R.id.ivQR);
        // generado al tamaño real en píxeles para que no se vea borroso
        int tamPx = findViewById(R.id.flQR).getLayoutParams().width; // tamaño fijo en el layout, ya en px
        Bitmap qr = GeneradorQR.generar(codigo, tamPx);
        if (qr == null) {
            findViewById(R.id.flQR).setVisibility(View.GONE);
            return;
        }
        ivQR.setImageBitmap(qr);
        ivQR.setContentDescription("Código QR del justificante " + codigo);
    }

    private void prepararZoomQR() {
        ScaleGestureDetector pellizco = new ScaleGestureDetector(this,
                new ScaleGestureDetector.SimpleOnScaleGestureListener() {
                    @Override
                    public boolean onScale(ScaleGestureDetector d) {
                        escalaQR = Math.max(ESCALA_MIN, Math.min(escalaMaxima(), escalaQR * d.getScaleFactor()));
                        ivQR.setScaleX(escalaQR);
                        ivQR.setScaleY(escalaQR);
                        return true;
                    }
                });
        GestureDetector dobleToque = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onDoubleTap(MotionEvent e) {
                escalaQR = ESCALA_MIN;
                ivQR.animate().scaleX(ESCALA_MIN).scaleY(ESCALA_MIN).setDuration(200).start();
                return true;
            }
        });
        // los toques se leen del contenedor (no escalado); si se leyeran del QR ampliado sus coordenadas
        // encogerían con la escala y el pellizco se frenaría solo
        View contenedor = findViewById(R.id.flQR);
        contenedor.setOnTouchListener((v, ev) -> {
            // desde el primer dedo: si el ScrollView se quedara el gesto, el segundo dedo ya no llegaría
            // (la página se puede seguir desplazando fuera del QR)
            if (ev.getActionMasked() == MotionEvent.ACTION_DOWN) v.getParent().requestDisallowInterceptTouchEvent(true);
            pellizco.onTouchEvent(ev);
            dobleToque.onTouchEvent(ev);
            if (ev.getActionMasked() == MotionEvent.ACTION_UP) v.performClick();
            return true;
        });
    }

    // hasta 3× pero sin que el QR se salga de la pantalla (perdería las esquinas y no se leería)
    private float escalaMaxima() {
        int anchoQR = ivQR.getWidth();
        if (anchoQR == 0) return ESCALA_MIN;
        float cabe = (float) findViewById(R.id.scroll).getWidth() / anchoQR;
        return Math.max(ESCALA_MIN, Math.min(ESCALA_MAX, cabe));
    }

    @Override
    protected void onResume() {
        super.onResume();
        // solo afecta a esta ventana: al salir el sistema recupera el brillo del usuario
        WindowManager.LayoutParams lp = getWindow().getAttributes();
        lp.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_FULL;
        getWindow().setAttributes(lp);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    @Override
    protected void onPause() {
        super.onPause();
        WindowManager.LayoutParams lp = getWindow().getAttributes();
        lp.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE;
        getWindow().setAttributes(lp);
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
