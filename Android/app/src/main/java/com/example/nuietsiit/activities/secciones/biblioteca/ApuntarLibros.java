package com.example.nuietsiit.activities.secciones.biblioteca;

import android.Manifest;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.os.SystemClock;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.LinearInterpolator;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.nuietsiit.R;
import com.example.nuietsiit.data.BibliotecaDB;
import com.example.nuietsiit.data.DBHelper;
import com.example.nuietsiit.data.modelo.Libro;
import com.example.nuietsiit.ui.CabeceraNui;
import com.example.nuietsiit.util.SesionUtil;
import com.example.nuietsiit.util.tutorial.Gesto;
import com.example.nuietsiit.util.tutorial.Tutorial;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.zxing.ResultPoint;
import com.journeyapps.barcodescanner.BarcodeResult;
import com.journeyapps.barcodescanner.BarcodeView;
import com.journeyapps.barcodescanner.Size;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Pasar la cámara por los libros sin pulsar nada: encima de cada código de barras aparece una etiqueta
 * con su estado, y cada estado vibra distinto (ver SenalLibro).
 */
public class ApuntarLibros extends PantallaBiblioteca {

    private static final long ETIQUETA_FIJA_MS = 500;
    private static final long ETIQUETA_DESVANECER_MS = 300;
    private static final long VIBRACION_REPOSO_MS = 5000;
    private static final long ETIQUETA_MOVER_MS = 80;
    private static final long LECTURA_PERDIDA_MS = 250; // sin leer el código en este tiempo, la etiqueta sigue sola
    private static final long VELOCIDAD_CADUCA_MS = 400; // dos lecturas más separadas no dicen hacia dónde va
    private static final int ANCHO_ETIQUETA_DP = 240;
    private static final int MARGEN_ETIQUETA_DP = 8;
    private static final int PUNTA_BORDE_DP = 24;     // las esquinas redondeadas no dejan llevarla más lejos

    private BarcodeView camara;
    private FrameLayout capaEtiquetas;
    private View barra, panelInferior, panelPermiso;
    private TextView tvPista;
    private BibliotecaDB db;
    private List<String> misAsignaturas;
    private final Map<String, Seguida> etiquetas = new HashMap<>(); // por ISBN
    private final Map<String, Long> ultimaVibracion = new HashMap<>(); // por ISBN

    private final ActivityResultLauncher<String> pedirCamara =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), concedido -> {
                if (!concedido) panelPermiso.setVisibility(View.VISIBLE);
                else mostrarTutorialSiPrimeraVez(crearTutorial());
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.apuntar_libros);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        CabeceraNui.preparar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(R.string.titulo_escanear_estanteria);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        barra = findViewById(R.id.cabecera);
        View zocalo = findViewById(R.id.zocalo);
        int altoZocalo = zocalo.getLayoutParams().height;
        int altoToolbar = toolbar.getLayoutParams().height;
        camara = findViewById(R.id.camara);
        capaEtiquetas = findViewById(R.id.capaEtiquetas);
        panelInferior = findViewById(R.id.panelInferior);
        panelPermiso = findViewById(R.id.panelPermiso);
        tvPista = findViewById(R.id.tvPista);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content), (v, insets) -> {
            Insets sistema = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            toolbar.getLayoutParams().height = altoToolbar + sistema.top;
            toolbar.setPadding(0, sistema.top, 0, 0);
            toolbar.requestLayout();
            zocalo.getLayoutParams().height = altoZocalo + sistema.bottom;
            zocalo.requestLayout();
            return insets;
        });

        db = new BibliotecaDB(DBHelper.getInstance(this));
        misAsignaturas = db.getAsignaturasMatriculadas(SesionUtil.correo(getIntent()));

        camara.setDecoderFactory(DecodificadorVarios.fabrica());
        camara.getCameraSettings().setContinuousFocusEnabled(true);
        camara.setFramingRectSize(new Size(10000, 10000));
        camara.decodeContinuous(this::alLeerCodigo);

        findViewById(R.id.btnPermiso).setOnClickListener(v -> {
            if (shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)) {
                pedirCamara.launch(Manifest.permission.CAMERA);
            } else {
                startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.fromParts("package", getPackageName(), null)));
            }
        });

        if (!tieneCamara()) {
            if (savedInstanceState == null) pedirCamara.launch(Manifest.permission.CAMERA);
            else panelPermiso.setVisibility(View.VISIBLE);
        }
    }

    @Override
    protected Tutorial crearTutorial() {
        return new Tutorial(this, "apuntar_libros", 1)
                .sensor(Gesto.ESCANEAR, getString(R.string.tut_escanear_sensor))
                .soloSi(this::tieneCamara)
                .tocar(R.id.cuerpoEtiqueta, getString(R.string.tut_escanear_tocar)).soloSiSeVe()
                .info(R.id.panelInferior, getString(R.string.tut_escanear_vibracion))
                .soloSi(this::tieneCamara)
                .info(R.id.btnPermiso, getString(R.string.tut_escanear_permiso)).soloSiSeVe();
    }

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
        for (Seguida seguida : etiquetas.values()) seguida.parar();
        etiquetas.clear();
        capaEtiquetas.removeAllViews();
    }

    private static class Seguida {
        final View etiqueta;
        float xCodigo, yCodigo; // en pantalla
        float vx, vy;           // px/ms
        float xEtiqueta, yEtiqueta; // a donde va la etiqueta tras la última lectura
        long ultimaLectura;
        ObjectAnimator movimiento, deriva;

        Seguida(View etiqueta) {
            this.etiqueta = etiqueta;
        }

        void parar() {
            if (movimiento != null) movimiento.cancel();
            if (deriva != null) deriva.cancel();
            etiqueta.animate().cancel();
        }
    }

    private void alLeerCodigo(BarcodeResult resultado) {
        Map<String, List<ResultPoint>> codigos =
                DecodificadorVarios.separar(resultado.getResult(), resultado.getTransformedResultPoints());
        for (Map.Entry<String, List<ResultPoint>> codigo : codigos.entrySet()) {
            alLeerCodigo(codigo.getKey(), codigo.getValue());
        }
    }

    private void alLeerCodigo(String isbn, List<ResultPoint> puntos) {
        if (!isbn.startsWith("978") && !isbn.startsWith("979")) return;

        Seguida seguida = etiquetas.get(isbn);
        boolean nueva = seguida == null;
        if (nueva) {
            seguida = new Seguida(crearEtiqueta(isbn));
            etiquetas.put(isbn, seguida);
            capaEtiquetas.addView(seguida.etiqueta);
            tvPista.setVisibility(View.GONE);
            avisarTutorial(Gesto.ESCANEAR);
        }
        if (!colocar(seguida, puntos, nueva)) return;
        programarDesaparicion(isbn, seguida);
    }

    private View crearEtiqueta(String isbn) {
        View etiqueta = LayoutInflater.from(this).inflate(R.layout.item_etiqueta_libro, capaEtiquetas, false);
        etiqueta.getLayoutParams().width = dp(ANCHO_ETIQUETA_DP);
        TextView tvTitulo = etiqueta.findViewById(R.id.tvTituloEtiqueta);
        TextView tvEstado = etiqueta.findViewById(R.id.tvEstadoEtiqueta);
        int color;

        Libro libro = db.getLibroPorIsbn(isbn);
        if (libro == null) {
            color = getColor(R.color.nui_apagado);
            tvTitulo.setText(R.string.libro_no_catalogo);
            tvEstado.setText(getString(R.string.libro_isbn_fmt, isbn));
        } else {
            List<String> comunes = new ArrayList<>(db.getAsignaturasDeLibro(libro.id));
            comunes.retainAll(misAsignaturas);
            SenalLibro senal = SenalLibro.de(libro, !comunes.isEmpty());
            color = getColor(senal.color);
            Long ultima = ultimaVibracion.get(isbn);
            long ahora = SystemClock.elapsedRealtime();
            if (ultima == null || ahora - ultima > VIBRACION_REPOSO_MS) {
                ultimaVibracion.put(isbn, ahora);
                vibrar(senal.vibracion);
            }

            tvTitulo.setText(libro.titulo);
            String estado = LibroAdapter.textoDisponibilidad(this, libro);
            if (!comunes.isEmpty()) {
                estado += "\n" + getString(R.string.libro_de_tu_asignatura_fmt, String.join(", ", comunes));
            }
            tvEstado.setText(estado);
            etiqueta.setOnClickListener(v -> abrirDetalle(libro));
        }

        etiqueta.findViewById(R.id.bandaEtiqueta).setBackgroundColor(color);
        etiqueta.findViewById(R.id.puntaEtiqueta).setBackgroundColor(color);
        return etiqueta;
    }

    private boolean colocar(Seguida seguida, List<ResultPoint> puntos, boolean nueva) {
        Rect enPantalla = camara.getFramingRect();
        Rect enImagen = camara.getPreviewFramingRect();
        if (enPantalla == null || enImagen == null || puntos.isEmpty()) return false;
        float xImagen = 0, yImagen = 0;
        for (ResultPoint p : puntos) {
            xImagen += p.getX() / puntos.size();
            yImagen += p.getY() / puntos.size();
        }
        float escala = enPantalla.width() / (float) enImagen.width();
        float xCodigo = enPantalla.left + (xImagen - enImagen.left) * escala;
        float yCodigo = enPantalla.top + (yImagen - enImagen.top) * escala;

        long ahora = SystemClock.elapsedRealtime();
        long dt = ahora - seguida.ultimaLectura;
        if (nueva || dt > VELOCIDAD_CADUCA_MS) {
            seguida.vx = 0;
            seguida.vy = 0;
        } else if (dt > 0) {
            seguida.vx = (seguida.vx + (xCodigo - seguida.xCodigo) / dt) / 2;
            seguida.vy = (seguida.vy + (yCodigo - seguida.yCodigo) / dt) / 2;
        }
        seguida.xCodigo = xCodigo;
        seguida.yCodigo = yCodigo;
        seguida.ultimaLectura = ahora;

        xCodigo += seguida.vx * ETIQUETA_MOVER_MS;
        yCodigo += seguida.vy * ETIQUETA_MOVER_MS;

        View etiqueta = seguida.etiqueta;
        etiqueta.measure(
                View.MeasureSpec.makeMeasureSpec(dp(ANCHO_ETIQUETA_DP), View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        int margen = dp(MARGEN_ETIQUETA_DP);
        float x = limitar(xCodigo - etiqueta.getMeasuredWidth() / 2f,
                margen, capaEtiquetas.getWidth() - etiqueta.getMeasuredWidth() - margen);
        float y = limitar(yCodigo - etiqueta.getMeasuredHeight() - margen,
                barra.getBottom() + margen, panelInferior.getTop() - etiqueta.getMeasuredHeight() - margen);

        float mitad = etiqueta.getMeasuredWidth() / 2f;
        float desvio = xCodigo - (x + mitad);
        etiqueta.findViewById(R.id.puntaEtiqueta).setTranslationX(limitar(desvio, -mitad + dp(PUNTA_BORDE_DP), mitad - dp(PUNTA_BORDE_DP)));

        if (seguida.movimiento != null) seguida.movimiento.cancel();
        if (seguida.deriva != null) seguida.deriva.cancel();
        seguida.xEtiqueta = x;
        seguida.yEtiqueta = y;
        if (nueva) {
            etiqueta.setTranslationX(x);
            etiqueta.setTranslationY(y);
        } else {
            seguida.movimiento = ObjectAnimator.ofPropertyValuesHolder(etiqueta,
                    PropertyValuesHolder.ofFloat(View.TRANSLATION_X, x),
                    PropertyValuesHolder.ofFloat(View.TRANSLATION_Y, y));
            seguida.movimiento.setInterpolator(new LinearInterpolator());
            seguida.movimiento.setDuration(ETIQUETA_MOVER_MS).start();
        }
        return true;
    }

    private static float limitar(float valor, float minimo, float maximo) {
        return Math.max(minimo, Math.min(valor, maximo));
    }

    private void programarDesaparicion(String isbn, Seguida seguida) {
        View etiqueta = seguida.etiqueta;
        etiqueta.animate().cancel();
        etiqueta.setAlpha(1f);
        etiqueta.animate().alpha(0f).setStartDelay(ETIQUETA_FIJA_MS).setDuration(ETIQUETA_DESVANECER_MS)
                .withEndAction(() -> {
                    seguida.parar();
                    etiquetas.remove(isbn);
                    ((ViewGroup) etiqueta.getParent()).removeView(etiqueta);
                }).start();

        if (seguida.vx == 0 && seguida.vy == 0) return;

        long duracion = ETIQUETA_FIJA_MS + ETIQUETA_DESVANECER_MS - LECTURA_PERDIDA_MS;
        float dx = seguida.vx * duracion / 2f;
        float dy = seguida.vy * duracion / 2f;

        seguida.deriva = ObjectAnimator.ofPropertyValuesHolder(etiqueta,
                PropertyValuesHolder.ofFloat(View.TRANSLATION_X, seguida.xEtiqueta, seguida.xEtiqueta + dx),
                PropertyValuesHolder.ofFloat(View.TRANSLATION_Y, seguida.yEtiqueta, seguida.yEtiqueta + dy));
        seguida.deriva.setInterpolator(new DecelerateInterpolator());
        seguida.deriva.setStartDelay(LECTURA_PERDIDA_MS);
        seguida.deriva.setDuration(duracion);
        seguida.deriva.start();
    }

    private void vibrar(long[] patron) {
        Vibrator vibrador = getSystemService(Vibrator.class);
        if (vibrador != null && vibrador.hasVibrator()) {
            vibrador.vibrate(VibrationEffect.createWaveform(patron, -1));
        }
    }

    private void abrirDetalle(Libro libro) {
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