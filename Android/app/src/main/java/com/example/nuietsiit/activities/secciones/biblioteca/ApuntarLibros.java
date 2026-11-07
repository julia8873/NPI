package com.example.nuietsiit.activities.secciones.biblioteca;

import android.Manifest;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.nuietsiit.R;
import com.example.nuietsiit.activities.BaseActivity;
import com.example.nuietsiit.data.BibliotecaDB;
import com.example.nuietsiit.data.DBHelper;
import com.example.nuietsiit.data.modelo.Libro;
import com.example.nuietsiit.util.SesionUtil;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.DecodeHintType;
import com.google.zxing.ResultPoint;
import com.journeyapps.barcodescanner.BarcodeResult;
import com.journeyapps.barcodescanner.BarcodeView;
import com.journeyapps.barcodescanner.DefaultDecoderFactory;
import com.journeyapps.barcodescanner.Size;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Pasar la cámara por los libros sin pulsar nada: encima de cada código de barras aparece una etiqueta
 * con su estado, y cada estado vibra distinto (ver SenalLibro).
 */
public class ApuntarLibros extends BaseActivity {

    // mientras una etiqueta sigue en pantalla, su libro no vuelve a vibrar
    private static final long ETIQUETA_FIJA_MS = 1500;
    private static final long ETIQUETA_DESVANECER_MS = 1500;
    private static final long ETIQUETA_MOVER_MS = 120;
    private static final int ANCHO_ETIQUETA_DP = 240;
    private static final int MARGEN_ETIQUETA_DP = 8;
    private static final int PUNTA_BORDE_DP = 24;     // las esquinas redondeadas no dejan llevarla más lejos

    private BarcodeView camara;
    private FrameLayout capaEtiquetas;
    private View barra, panelInferior, panelPermiso;
    private TextView tvPista;
    private BibliotecaDB db;
    private List<String> misAsignaturas;
    private final Map<String, View> etiquetas = new HashMap<>(); // por ISBN

    private final ActivityResultLauncher<String> pedirCamara =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), concedido -> {
                // si se concede, la cámara arranca en onResume
                if (!concedido) panelPermiso.setVisibility(View.VISIBLE);
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.apuntar_libros);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("Apuntar a los libros");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        barra = toolbar;
        camara = findViewById(R.id.camara);
        capaEtiquetas = findViewById(R.id.capaEtiquetas);
        panelInferior = findViewById(R.id.panelInferior);
        panelPermiso = findViewById(R.id.panelPermiso);
        tvPista = findViewById(R.id.tvPista);

        // la cámara llega hasta los bordes; la barra y la leyenda se apartan de las barras del sistema
        int rellenoInferior = panelInferior.getPaddingBottom();
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content), (v, insets) -> {
            Insets sistema = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            barra.setPadding(0, sistema.top, 0, 0);
            panelInferior.setPadding(panelInferior.getPaddingLeft(), panelInferior.getPaddingTop(),
                    panelInferior.getPaddingRight(), rellenoInferior + sistema.bottom);
            return insets;
        });

        db = new BibliotecaDB(DBHelper.getInstance(this));
        misAsignaturas = db.getAsignaturasMatriculadas(SesionUtil.correo(getIntent()));

        // TRY_HARDER: lee también el código girado, como cuando el libro está tumbado
        camara.setDecoderFactory(new DefaultDecoderFactory(
                Collections.singletonList(BarcodeFormat.EAN_13),
                Collections.singletonMap(DecodeHintType.TRY_HARDER, true), null, 0));
        camara.getCameraSettings().setContinuousFocusEnabled(true);
        // la zona de lectura ocupa toda la pantalla, no solo el cuadrado central
        camara.setFramingRectSize(new Size(10000, 10000));
        camara.decodeContinuous(this::alLeerCodigo);

        findViewById(R.id.btnPermiso).setOnClickListener(v -> {
            if (shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)) {
                pedirCamara.launch(Manifest.permission.CAMERA);
            } else {
                // denegado para siempre: Android ya no muestra el diálogo
                startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.fromParts("package", getPackageName(), null)));
            }
        });
        // al girar no se vuelve a preguntar: cada denegación acerca a que Android deje de mostrar el diálogo
        if (!tieneCamara()) {
            if (savedInstanceState == null) pedirCamara.launch(Manifest.permission.CAMERA);
            else panelPermiso.setVisibility(View.VISIBLE);
        }
    }

    // barrer la balda de lado a lado parecería agitar y sacaría del escáner
    @Override
    protected boolean usaAgitar() {
        return false;
    }

    private boolean tieneCamara() {
        return checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (tieneCamara()) {
            panelPermiso.setVisibility(View.GONE);
            camara.resume();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        camara.pause();
        for (View etiqueta : etiquetas.values()) etiqueta.animate().cancel();
        etiquetas.clear();
        capaEtiquetas.removeAllViews();
    }

    private void alLeerCodigo(BarcodeResult resultado) {
        String isbn = resultado.getText();
        // un EAN-13 de libro siempre empieza por 978 o 979; lo demás (una caja, un bote) se ignora
        if (!isbn.startsWith("978") && !isbn.startsWith("979")) return;

        View etiqueta = etiquetas.get(isbn);
        boolean nueva = etiqueta == null;
        if (nueva) {
            etiqueta = crearEtiqueta(isbn);
            etiquetas.put(isbn, etiqueta);
            capaEtiquetas.addView(etiqueta);
            tvPista.setVisibility(View.GONE);
        }
        colocar(etiqueta, resultado, nueva);
        programarDesaparicion(isbn, etiqueta);
    }

    private View crearEtiqueta(String isbn) {
        View etiqueta = LayoutInflater.from(this).inflate(R.layout.item_etiqueta_libro, capaEtiquetas, false);
        etiqueta.getLayoutParams().width = dp(ANCHO_ETIQUETA_DP);
        TextView tvTitulo = etiqueta.findViewById(R.id.tvTituloEtiqueta);
        TextView tvEstado = etiqueta.findViewById(R.id.tvEstadoEtiqueta);
        int color;

        Libro libro = db.getLibroPorIsbn(isbn);
        if (libro == null) {
            // no vibra, pero se ve que lo ha leído
            color = Color.parseColor("#616161");
            tvTitulo.setText("No está en el catálogo");
            tvEstado.setText("ISBN " + isbn);
        } else {
            List<String> comunes = new ArrayList<>(db.getAsignaturasDeLibro(libro.id));
            comunes.retainAll(misAsignaturas);
            SenalLibro senal = SenalLibro.de(libro, !comunes.isEmpty());
            color = Color.parseColor(senal.color);
            vibrar(senal.vibracion);

            tvTitulo.setText(libro.titulo);
            String estado = LibroAdapter.textoDisponibilidad(libro);
            if (!comunes.isEmpty()) estado += "\nDe tu asignatura: " + String.join(", ", comunes);
            tvEstado.setText(estado);
            etiqueta.setOnClickListener(v -> abrirDetalle(libro));
        }

        etiqueta.findViewById(R.id.cuerpoEtiqueta).setBackgroundTintList(ColorStateList.valueOf(color));
        ((TextView) etiqueta.findViewById(R.id.tvPuntaEtiqueta)).setTextColor(color);
        return etiqueta;
    }

    // la etiqueta queda justo encima del código, sin salirse de la zona libre de la pantalla
    private void colocar(View etiqueta, BarcodeResult resultado, boolean nueva) {
        // los puntos vienen en píxeles de la imagen de la cámara: se pasan a píxeles de la pantalla
        Rect enPantalla = camara.getFramingRect();
        Rect enImagen = camara.getPreviewFramingRect();
        if (enPantalla == null || enImagen == null) return;
        List<ResultPoint> puntos = resultado.getTransformedResultPoints();
        if (puntos.isEmpty()) return;
        float xImagen = 0, yImagen = 0;
        for (ResultPoint p : puntos) {
            xImagen += p.getX() / puntos.size();
            yImagen += p.getY() / puntos.size();
        }
        float escala = enPantalla.width() / (float) enImagen.width();
        float xCodigo = enPantalla.left + (xImagen - enImagen.left) * escala;
        float yCodigo = enPantalla.top + (yImagen - enImagen.top) * escala;

        etiqueta.measure(
                View.MeasureSpec.makeMeasureSpec(dp(ANCHO_ETIQUETA_DP), View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        int margen = dp(MARGEN_ETIQUETA_DP);
        float x = limitar(xCodigo - etiqueta.getMeasuredWidth() / 2f,
                margen, capaEtiquetas.getWidth() - etiqueta.getMeasuredWidth() - margen);
        float y = limitar(yCodigo - etiqueta.getMeasuredHeight() - margen,
                barra.getBottom() + margen, panelInferior.getTop() - etiqueta.getMeasuredHeight() - margen);

        // si la etiqueta choca con el borde, la punta se desplaza para seguir señalando el código
        float mitad = etiqueta.getMeasuredWidth() / 2f;
        float desvio = xCodigo - (x + mitad);
        etiqueta.findViewById(R.id.tvPuntaEtiqueta).setTranslationX(limitar(desvio, -mitad + dp(PUNTA_BORDE_DP), mitad - dp(PUNTA_BORDE_DP)));

        if (nueva) {
            etiqueta.setTranslationX(x);
            etiqueta.setTranslationY(y);
        } else {
            ObjectAnimator mover = ObjectAnimator.ofPropertyValuesHolder(etiqueta,
                    PropertyValuesHolder.ofFloat(View.TRANSLATION_X, x),
                    PropertyValuesHolder.ofFloat(View.TRANSLATION_Y, y));
            mover.setAutoCancel(true); // la cámara lee más rápido de lo que dura: sustituye al movimiento anterior
            mover.setDuration(ETIQUETA_MOVER_MS).start();
        }
    }

    private static float limitar(float valor, float minimo, float maximo) {
        return Math.max(minimo, Math.min(valor, maximo));
    }

    // cada lectura del mismo código reinicia la cuenta: se va al dejar de apuntarle
    private void programarDesaparicion(String isbn, View etiqueta) {
        etiqueta.animate().cancel();
        etiqueta.setAlpha(1f);
        etiqueta.animate().alpha(0f).setStartDelay(ETIQUETA_FIJA_MS).setDuration(ETIQUETA_DESVANECER_MS)
                .withEndAction(() -> {
                    etiquetas.remove(isbn);
                    ((ViewGroup) etiqueta.getParent()).removeView(etiqueta);
                }).start();
    }

    private void vibrar(long[] patron) {
        Vibrator vibrador = getSystemService(Vibrator.class);
        if (vibrador != null && vibrador.hasVibrator()) {
            vibrador.vibrate(VibrationEffect.createWaveform(patron, -1));
        }
    }

    private void abrirDetalle(Libro libro) {
        // String, igual que los extras de RecienteUtil
        startActivity(new Intent(this, DetalleLibro.class)
                .putExtra("idLibro", String.valueOf(libro.id))
                .putExtra("CORREO", getIntent().getStringExtra("CORREO")));
    }

    private int dp(int valor) {
        return Math.round(valor * getResources().getDisplayMetrics().density);
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
