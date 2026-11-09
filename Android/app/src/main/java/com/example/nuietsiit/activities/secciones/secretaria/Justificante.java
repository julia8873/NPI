package com.example.nuietsiit.activities.secciones.secretaria;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.lifecycle.Lifecycle;

import com.example.nuietsiit.R;
import com.example.nuietsiit.data.DBHelper;
import com.example.nuietsiit.data.SecretariaDB;
import com.example.nuietsiit.data.modelo.Cita;
import com.example.nuietsiit.data.modelo.Solicitud;
import com.example.nuietsiit.util.CalendarioUtil;
import com.example.nuietsiit.ui.HojasPaginadas;
import com.example.nuietsiit.util.DetectorEnsenar;
import com.example.nuietsiit.util.FechasUtil;
import com.example.nuietsiit.util.GeneradorQR;
import com.example.nuietsiit.util.SesionUtil;
import com.google.android.material.appbar.MaterialToolbar;


/**
 * Justificante para enseñar en ventanilla: código, resumen y QR grande.
 * Sirve para citas (extra idCita) y solicitudes (extra idSolicitud).
 * Mientras está abierto: brillo al máximo y pantalla siempre encendida.
 * QR: pellizcar amplía de 1× a 3× (sin pasar del ancho de pantalla), doble toque vuelve a 1×.
 * Enseñar: girar el móvil hacia la ventanilla (o tocar el QR) lo muestra a pantalla completa. Se quita al
 * tocarlo, con atrás o, tras haberlo girado, al inclinarlo hacia uno mismo. No en citas canceladas o pasadas.
 */
public class Justificante extends PantallaSecretaria {

    private static final float ESCALA_MIN = 1f;
    private static final float ESCALA_MAX = 3f;
    private static final int DURACION_CITA_MIN = 10;
    private static final long VIBRACION_MS = 60;
    private static final long FUNDIDO_MS = 150;

    private ImageView ivQR;
    private float escalaQR = ESCALA_MIN;
    private String codigoQR;
    private String estadoCapa;
    private int colorEstadoCapa; // recurso de color
    private View capaQR;
    private ImageView ivCapaQR;
    private DetectorEnsenar detectorEnsenar;
    private OnBackPressedCallback atrasCierraCapa;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.justificante);
        // aquí girar el móvil es enseñar el QR: las hojas solo se pasan deslizando o tocando el pie
        ((HojasPaginadas) findViewById(R.id.scroll)).setPermitirInclinar(false);

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
        prepararEnsenar();
    }

    private void mostrarCita(Cita cita) {
        ((TextView) findViewById(R.id.tvCodigo)).setText(cita.codigo());
        ((TextView) findViewById(R.id.tvMotivo)).setText(cita.motivo());
        ((TextView) findViewById(R.id.tvFecha)).setText(FechasUtil.capitalizar(FechasUtil.diaLargo(cita.fechaHora())));
        ((TextView) findViewById(R.id.tvHora)).setText(cita.hora);

        TextView tvEstado = findViewById(R.id.tvEstado);
        boolean vigente = false;
        if (Cita.CANCELADA.equals(cita.estado)) {
            mostrarEstado(tvEstado, "Cancelada", R.color.nui_error);
        } else if (!cita.esFutura()) {
            mostrarEstado(tvEstado, "Pasada", R.color.nui_apagado);
        } else {
            mostrarBotonCalendario(cita);
            vigente = true;
        }

        mostrarQR(cita.codigo());
        // a pantalla completa no se vería el aviso de cancelada o pasada y parecería un QR válido
        if (!vigente) {
            codigoQR = null;
            ((TextView) findViewById(R.id.tvAyudaQR)).setText("Pellizca para ampliarlo; doble toque para volver.");
        }
    }

    private void mostrarSolicitud(Solicitud solicitud) {
        ((TextView) findViewById(R.id.tvCodigo)).setText(solicitud.codigo());
        ((TextView) findViewById(R.id.tvMotivo)).setText(solicitud.nombreTramite);
        ((TextView) findViewById(R.id.tvFecha)).setText(solicitud.textoEnvio());
        findViewById(R.id.tvHora).setVisibility(View.GONE);

        String estado = solicitud.estado();
        estadoCapa = estado;
        colorEstadoCapa = Solicitud.colorEstado(estado);
        mostrarEstado(findViewById(R.id.tvEstado), estadoCapa, colorEstadoCapa);

        mostrarQR(solicitud.codigo());
    }

    // el estado es un sello estampado sobre el resguardo, del color del estado
    private void mostrarEstado(TextView tv, String texto, int colorRes) {
        tv.setVisibility(View.VISIBLE);
        Pastilla.sellar(tv, texto, colorRes);
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
        codigoQR = codigo;
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

            @Override
            public boolean onSingleTapConfirmed(MotionEvent e) {
                findViewById(R.id.flQR).performClick();
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

    private void prepararEnsenar() {
        if (codigoQR == null) return; // no se pudo generar el QR: no hay nada que enseñar

        capaQR = getLayoutInflater().inflate(R.layout.capa_qr, null);
        ivCapaQR = capaQR.findViewById(R.id.ivCapaQR);
        ((TextView) capaQR.findViewById(R.id.tvCapaCodigo)).setText(codigoQR);
        if (estadoCapa != null) mostrarEstado(capaQR.findViewById(R.id.tvCapaEstado), estadoCapa, colorEstadoCapa);
        capaQR.setContentDescription("Código QR a pantalla completa, " + codigoQR
                + (estadoCapa != null ? ", estado: " + estadoCapa : "") + ". Toca para volver.");
        capaQR.setOnClickListener(v -> ocultarCapaQR());
        addContentView(capaQR, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        // alternativa sin gesto (también es la acción de TalkBack)
        findViewById(R.id.flQR).setOnClickListener(v -> mostrarCapaQR());

        atrasCierraCapa = new OnBackPressedCallback(false) {
            @Override
            public void handleOnBackPressed() {
                ocultarCapaQR();
            }
        };
        getOnBackPressedDispatcher().addCallback(this, atrasCierraCapa);

        detectorEnsenar = new DetectorEnsenar(this, new DetectorEnsenar.OnEnsenar() {
            @Override
            public void alEnsenar() {
                mostrarCapaQR();
            }

            @Override
            public void alVolver() {
                ocultarCapaQR();
            }
        });
    }

    private void mostrarCapaQR() {
        if (capaQR.getVisibility() == View.VISIBLE) return;
        // el toque simple se confirma con retraso: puede llegar cuando ya se ha salido de la pantalla
        if (!getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.RESUMED)) return;
        if (ivCapaQR.getDrawable() == null) {
            // generado aquí y no al abrir: al tamaño real de la pantalla, para que se vea nítido
            int tamPx = Math.min(getResources().getDisplayMetrics().widthPixels,
                    getResources().getDisplayMetrics().heightPixels);
            Bitmap qr = GeneradorQR.generar(codigoQR, tamPx);
            if (qr == null) return;
            ivCapaQR.setImageBitmap(qr);
        }
        barrasSistema().hide(WindowInsetsCompat.Type.systemBars());
        capaQR.setAlpha(0f);
        capaQR.setVisibility(View.VISIBLE);
        capaQR.animate().alpha(1f).setDuration(FUNDIDO_MS).start();
        atrasCierraCapa.setEnabled(true);
        cambiarFocoTalkBack(capaQR, View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);
        vibrar();
    }

    private void ocultarCapaQR() {
        if (capaQR == null || capaQR.getVisibility() != View.VISIBLE) return;
        capaQR.animate().cancel();
        capaQR.setVisibility(View.GONE);
        barrasSistema().show(WindowInsetsCompat.Type.systemBars());
        atrasCierraCapa.setEnabled(false);
        cambiarFocoTalkBack(findViewById(R.id.flQR), View.IMPORTANT_FOR_ACCESSIBILITY_AUTO);
    }

    // con la capa puesta, TalkBack no debe recorrer ni activar lo que queda tapado
    private void cambiarFocoTalkBack(View destino, int contenidoAccesible) {
        findViewById(R.id.contenido).setImportantForAccessibility(contenidoAccesible);
        destino.post(() -> destino.performAccessibilityAction(AccessibilityNodeInfo.ACTION_ACCESSIBILITY_FOCUS, null));
    }

    private WindowInsetsControllerCompat barrasSistema() {
        WindowInsetsControllerCompat barras = WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        barras.setSystemBarsBehavior(WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
        return barras;
    }

    private void vibrar() {
        Vibrator vibrador = getSystemService(Vibrator.class);
        if (vibrador != null && vibrador.hasVibrator()) {
            vibrador.vibrate(VibrationEffect.createOneShot(VIBRACION_MS, VibrationEffect.DEFAULT_AMPLITUDE));
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (detectorEnsenar != null) detectorEnsenar.iniciar();
        // solo afecta a esta ventana: al salir el sistema recupera el brillo del usuario
        WindowManager.LayoutParams lp = getWindow().getAttributes();
        lp.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_FULL;
        getWindow().setAttributes(lp);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (detectorEnsenar != null) detectorEnsenar.parar();
        // el detector empieza de cero en onResume: si la capa siguiera puesta, el gesto ya no la quitaría
        ocultarCapaQR();
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
