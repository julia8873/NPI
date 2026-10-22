package com.example.nuietsiit.activities.secciones.secretaria;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.accessibility.AccessibilityManager;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;

import com.example.nuietsiit.R;
import com.example.nuietsiit.data.DBHelper;
import com.example.nuietsiit.data.SecretariaDB;
import com.example.nuietsiit.data.UsuariosDB;
import com.example.nuietsiit.data.modelo.Documento;
import com.example.nuietsiit.data.modelo.Tramite;
import com.example.nuietsiit.util.LienzoFirma;
import com.example.nuietsiit.util.SesionUtil;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.progressindicator.LinearProgressIndicator;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

/**
 * Asistente de 4 pasos para solicitar un trámite: datos → documentos → firma → resumen y envío
 * (mantener pulsado 1,5 s). Al enviar guarda la SOLICITUD y abre su justificante.
 */
public class SolicitudTramite extends AppCompatActivity {

    private static final String[] TITULOS = {"Tus datos", "Documentos", "Firma", "Resumen"};
    private static final long DURACION_MANTENER = 1500;

    private SecretariaDB db;
    private Tramite tramite;
    private List<Documento> documentos;
    private String correoUsuario, nombreUsuario;
    private int paso;
    private Bitmap firma;           // exportada al salir del paso de firma
    private boolean firmaConHuella; // true = firmada con la huella en vez de a mano
    private ValueAnimator animEnviar;
    private boolean enviando;       // evita dos envíos si el anillo se completa dos veces

    private View[] vistasPaso;
    private LinearProgressIndicator progreso;
    private TextView tvPaso, tvErrorFirma, tvResumen, tvAyudaEnviar;
    private EditText etObservaciones;
    private LienzoFirma lienzoFirma;
    private ImageView ivFirma;
    private CircularProgressIndicator anilloEnviar;
    private Button btnAnterior, btnSiguiente;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.solicitud_tramite);

        MaterialToolbar barra = findViewById(R.id.toolbar);
        setSupportActionBar(barra);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        db = new SecretariaDB(DBHelper.getInstance(this));
        tramite = db.getTramite(getIntent().getIntExtra("idProcedimiento", -1));
        if (tramite == null || !tramite.sePuedeSolicitar()) {
            Toast.makeText(this, "Este trámite no se puede solicitar ahora", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        getSupportActionBar().setTitle("Solicitar: " + tramite.nombre);
        documentos = db.getDocumentos(tramite.id);

        correoUsuario = SesionUtil.correo(getIntent());
        nombreUsuario = new UsuariosDB(DBHelper.getInstance(this)).obtenerNombreUsuario(correoUsuario);
        if (nombreUsuario == null) nombreUsuario = "—";

        vistasPaso = new View[]{findViewById(R.id.pasoDatos), findViewById(R.id.pasoDocumentos),
                findViewById(R.id.pasoFirma), findViewById(R.id.pasoResumen)};
        progreso = findViewById(R.id.progreso);
        tvPaso = findViewById(R.id.tvPaso);
        btnAnterior = findViewById(R.id.btnAnterior);
        btnSiguiente = findViewById(R.id.btnSiguiente);
        btnAnterior.setOnClickListener(v -> mostrarPaso(paso - 1));
        btnSiguiente.setOnClickListener(v -> siguiente());

        // 1. Datos (solo lectura) + observaciones
        ((TextView) findViewById(R.id.etNombre)).setText(nombreUsuario);
        ((TextView) findViewById(R.id.etCorreo)).setText(correoUsuario);
        etObservaciones = findViewById(R.id.etObservaciones);

        // 2. Documentos (de momento solo la lista; la cámara llega en la tarea 7b)
        ((TextView) findViewById(R.id.tvDocumentos)).setText(textoDocumentos());

        // 3. Firma
        lienzoFirma = findViewById(R.id.lienzoFirma);
        tvErrorFirma = findViewById(R.id.tvErrorFirma);
        lienzoFirma.setAlCambiar(() -> {
            if (lienzoFirma.tieneFirma()) tvErrorFirma.setVisibility(View.GONE);
        });
        findViewById(R.id.btnBorrarFirma).setOnClickListener(v -> lienzoFirma.borrar());
        configurarFirmaConHuella(findViewById(R.id.btnFirmarHuella));

        // 4. Resumen + mantener pulsado para enviar
        tvResumen = findViewById(R.id.tvResumen);
        ivFirma = findViewById(R.id.ivFirma);
        anilloEnviar = findViewById(R.id.anilloEnviar);
        tvAyudaEnviar = findViewById(R.id.tvAyudaEnviar);
        configurarMantenerParaEnviar(findViewById(R.id.btnEnviar));

        // atrás del sistema = paso anterior
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (paso > 0) mostrarPaso(paso - 1);
                else finish();
            }
        });

        mostrarPaso(0);
    }

    private void mostrarPaso(int nuevo) {
        if (nuevo < 0 || nuevo >= vistasPaso.length) return;
        paso = nuevo;
        for (int i = 0; i < vistasPaso.length; i++) {
            vistasPaso[i].setVisibility(i == paso ? View.VISIBLE : View.GONE);
        }
        progreso.setProgressCompat(paso + 1, true);
        tvPaso.setText("Paso " + (paso + 1) + " de " + vistasPaso.length + " · " + TITULOS[paso]);
        btnAnterior.setVisibility(paso > 0 ? View.VISIBLE : View.INVISIBLE);
        btnSiguiente.setVisibility(paso < vistasPaso.length - 1 ? View.VISIBLE : View.INVISIBLE);
        if (paso == vistasPaso.length - 1) mostrarResumen();
        ocultarTeclado();
    }

    // valida el paso actual antes de avanzar
    private void siguiente() {
        if (paso == 2) {
            if (!lienzoFirma.tieneFirma()) {
                tvErrorFirma.setVisibility(View.VISIBLE);
                lienzoFirma.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
                return;
            }
            firma = lienzoFirma.exportar();
            firmaConHuella = false;
        }
        mostrarPaso(paso + 1);
    }

    private String textoDocumentos() {
        if (documentos.isEmpty()) return "Este trámite no necesita documentos.";
        StringBuilder sb = new StringBuilder("Documentos que tendrás que aportar:\n");
        for (Documento d : documentos) sb.append("\n").append(d.textoLista());
        return sb.toString();
    }

    private void mostrarResumen() {
        String obs = observaciones();
        tvResumen.setText("Trámite: " + tramite.nombre
                + "\nNombre: " + nombreUsuario
                + "\nCorreo: " + correoUsuario
                + "\nObservaciones: " + (obs != null ? obs : "ninguna")
                + "\nDocumentos: " + (documentos.isEmpty() ? "no hacen falta" : "próximamente")
                + "\nFirma:" + (firmaConHuella ? " ✓ con huella digital" : ""));
        ivFirma.setVisibility(firmaConHuella ? View.GONE : View.VISIBLE);
        ivFirma.setImageBitmap(firma);
        reiniciarAnillo();
    }

    // null si no ha escrito nada
    private String observaciones() {
        String obs = etObservaciones.getText().toString().trim();
        return obs.isEmpty() ? null : obs;
    }

    // ---------- Firma con huella (alternativa a firmar a mano) ----------

    // el botón solo aparece si hay sensor y una huella registrada
    private void configurarFirmaConHuella(Button boton) {
        int autenticador = BiometricManager.Authenticators.BIOMETRIC_STRONG;
        if (BiometricManager.from(this).canAuthenticate(autenticador) != BiometricManager.BIOMETRIC_SUCCESS) return;
        boton.setVisibility(View.VISIBLE);

        BiometricPrompt prompt = new BiometricPrompt(this, ContextCompat.getMainExecutor(this),
                new BiometricPrompt.AuthenticationCallback() {
                    @Override
                    public void onAuthenticationSucceeded(BiometricPrompt.AuthenticationResult resultado) {
                        firmaConHuella = true;
                        firma = null;
                        tvErrorFirma.setVisibility(View.GONE);
                        mostrarPaso(3);
                    }

                    @Override
                    public void onAuthenticationError(int codigo, CharSequence mensaje) {
                        // cancelar o "Firmar a mano" no es un error que haya que enseñar
                        if (codigo != BiometricPrompt.ERROR_NEGATIVE_BUTTON
                                && codigo != BiometricPrompt.ERROR_USER_CANCELED
                                && codigo != BiometricPrompt.ERROR_CANCELED) {
                            Toast.makeText(SolicitudTramite.this, mensaje, Toast.LENGTH_SHORT).show();
                        }
                    }
                });
        BiometricPrompt.PromptInfo info = new BiometricPrompt.PromptInfo.Builder()
                .setTitle("Firmar la solicitud")
                .setSubtitle(tramite.nombre)
                .setNegativeButtonText("Firmar a mano")
                .setAllowedAuthenticators(autenticador)
                .build();
        boton.setOnClickListener(v -> prompt.authenticate(info));
    }

    // ---------- Mantener pulsado para enviar ----------

    @SuppressLint("ClickableViewAccessibility") // se llama a performClick al soltar
    private void configurarMantenerParaEnviar(View boton) {
        // con TalkBack no se puede mantener pulsado con precisión: el doble toque pide confirmación
        boton.setOnClickListener(v -> {
            AccessibilityManager am = getSystemService(AccessibilityManager.class);
            if (enviando || am == null || !am.isTouchExplorationEnabled()) return;
            new MaterialAlertDialogBuilder(this)
                    .setTitle("¿Enviar la solicitud?")
                    .setMessage(tramite.nombre)
                    .setPositiveButton("Enviar", (d, w) -> {
                        if (enviando) return;
                        vibrar();
                        enviar();
                    })
                    .setNegativeButton("Cancelar", null)
                    .show();
        });
        boton.setOnTouchListener((v, e) -> {
            switch (e.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    // que el ScrollView no cancele el gesto si el dedo se mueve un poco
                    v.getParent().requestDisallowInterceptTouchEvent(true);
                    empezarMantener();
                    return true;
                case MotionEvent.ACTION_UP:
                    v.performClick();
                    soltar();
                    return true;
                case MotionEvent.ACTION_CANCEL:
                    soltar();
                    return true;
                default:
                    return true;
            }
        });
    }

    private void empezarMantener() {
        if (enviando) return;
        anilloEnviar.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
        tvAyudaEnviar.setText("Sigue pulsando…");
        animEnviar = ValueAnimator.ofInt(0, anilloEnviar.getMax());
        animEnviar.setDuration(DURACION_MANTENER);
        animEnviar.addUpdateListener(a -> anilloEnviar.setProgress((int) a.getAnimatedValue()));
        animEnviar.addListener(new AnimatorListenerAdapter() {
            private boolean cancelado;

            @Override
            public void onAnimationCancel(Animator animation) {
                cancelado = true;
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                if (cancelado) return;
                vibrar();
                enviar();
            }
        });
        animEnviar.start();
    }

    // soltar antes de completar el anillo = no se envía
    private void soltar() {
        if (animEnviar == null || !animEnviar.isRunning()) return;
        animEnviar.cancel();
        reiniciarAnillo();
        tvAyudaEnviar.setText("Mantén pulsado hasta que se complete el anillo");
    }

    private void reiniciarAnillo() {
        if (animEnviar != null) animEnviar.cancel();
        anilloEnviar.setProgressCompat(0, true);
        tvAyudaEnviar.setText("Mantén pulsado para enviar");
    }

    private void vibrar() {
        Vibrator vibrador = getSystemService(Vibrator.class);
        if (vibrador != null && vibrador.hasVibrator()) {
            vibrador.vibrate(VibrationEffect.createOneShot(150, VibrationEffect.DEFAULT_AMPLITUDE));
        }
    }

    private void enviar() {
        enviando = true;
        File rutaFirma = null; // firmada con huella → sin PNG
        if (!firmaConHuella) {
            rutaFirma = guardarFirma();
            if (rutaFirma == null) {
                fallo("No se ha podido guardar la firma");
                return;
            }
        }
        long idSolicitud = db.insertarSolicitud(correoUsuario, tramite.id, observaciones(),
                rutaFirma != null ? rutaFirma.getAbsolutePath() : null, null);
        if (idSolicitud == -1) {
            if (rutaFirma != null) rutaFirma.delete();
            fallo("No se ha podido enviar la solicitud");
            return;
        }
        startActivity(new Intent(this, Justificante.class)
                .putExtra("idSolicitud", (int) idSolicitud)
                .putExtra("CORREO", correoUsuario));
        finish(); // al volver desde el justificante no se vuelve al asistente
    }

    private void fallo(String mensaje) {
        enviando = false;
        reiniciarAnillo();
        Toast.makeText(this, mensaje, Toast.LENGTH_SHORT).show();
    }

    // PNG en getFilesDir()/secretaria/ (privado de la app, sin permisos). null si falla
    private File guardarFirma() {
        File carpeta = new File(getFilesDir(), "secretaria");
        if (!carpeta.isDirectory() && !carpeta.mkdirs()) return null;
        File archivo = new File(carpeta, "firma_" + System.currentTimeMillis() + ".png");
        try (FileOutputStream salida = new FileOutputStream(archivo)) {
            if (!firma.compress(Bitmap.CompressFormat.PNG, 100, salida)) throw new IOException();
            return archivo;
        } catch (IOException e) {
            archivo.delete();
            return null;
        }
    }

    private void ocultarTeclado() {
        View foco = getCurrentFocus();
        if (foco == null) return;
        InputMethodManager imm = getSystemService(InputMethodManager.class);
        if (imm != null) imm.hideSoftInputFromWindow(foco.getWindowToken(), 0);
        foco.clearFocus();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (!enviando) soltar(); // salir de la app con el dedo puesto no debe enviar
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
