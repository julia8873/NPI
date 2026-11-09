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
import com.example.nuietsiit.util.SesionUtil;
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

    // la etiqueta se va enseguida al dejar de ver el código (si no, parece trabada en el borde),
    // pero el libro no vuelve a vibrar hasta pasado un rato aunque la cámara lo pierda y lo recupere
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
                // si se concede, la cámara arranca en onResume
                if (!concedido) panelPermiso.setVisibility(View.VISIBLE);
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.apuntar_libros);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("Escanear estantería");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        barra = findViewById(R.id.cabecera); // yesería + arquería: las etiquetas no pasan por debajo
        View zocalo = findViewById(R.id.zocalo);
        int altoZocalo = zocalo.getLayoutParams().height;
        int altoToolbar = toolbar.getLayoutParams().height;
        camara = findViewById(R.id.camara);
        capaEtiquetas = findViewById(R.id.capaEtiquetas);
        panelInferior = findViewById(R.id.panelInferior);
        panelPermiso = findViewById(R.id.panelPermiso);
        tvPista = findViewById(R.id.tvPista);

        // la cámara llega hasta los bordes; la cabecera y el zócalo crecen bajo las barras del sistema
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content), (v, insets) -> {
            Insets sistema = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            // la yesería sube hasta la barra de estado; la arquería sigue dejando ver la cámara
            toolbar.getLayoutParams().height = altoToolbar + sistema.top;
            toolbar.setPadding(0, sistema.top, 0, 0);
            toolbar.requestLayout();
            zocalo.getLayoutParams().height = altoZocalo + sistema.bottom;
            zocalo.requestLayout();
            return insets;
        });

        db = new BibliotecaDB(DBHelper.getInstance(this));
        misAsignaturas = db.getAsignaturasMatriculadas(SesionUtil.correo(getIntent()));

        // lee todos los códigos que haya en pantalla, también girados (libro tumbado)
        camara.setDecoderFactory(DecodificadorVarios.fabrica());
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
        for (Seguida seguida : etiquetas.values()) seguida.parar();
        etiquetas.clear();
        capaEtiquetas.removeAllViews();
    }

    // la etiqueta de un libro mientras se ve su código: recuerda hacia dónde se movía para seguirlo al perderlo
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
        // un EAN-13 de libro siempre empieza por 978 o 979; lo demás (una caja, un bote) se ignora
        if (!isbn.startsWith("978") && !isbn.startsWith("979")) return;

        Seguida seguida = etiquetas.get(isbn);
        boolean nueva = seguida == null;
        if (nueva) {
            seguida = new Seguida(crearEtiqueta(isbn));
            etiquetas.put(isbn, seguida);
            capaEtiquetas.addView(seguida.etiqueta);
            tvPista.setVisibility(View.GONE);
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
            // no vibra, pero se ve que lo ha leído
            color = getColor(R.color.nui_apagado);
            tvTitulo.setText("No está en el catálogo");
            tvEstado.setText("ISBN " + isbn);
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
            String estado = LibroAdapter.textoDisponibilidad(libro);
            if (!comunes.isEmpty()) estado += "\nDe tu asignatura: " + String.join(", ", comunes);
            tvEstado.setText(estado);
            etiqueta.setOnClickListener(v -> abrirDetalle(libro));
        }

        etiqueta.findViewById(R.id.bandaEtiqueta).setBackgroundColor(color);
        etiqueta.findViewById(R.id.puntaEtiqueta).setBackgroundColor(color);
        return etiqueta;
    }

    // la etiqueta queda justo encima del código, sin salirse de la zona libre de la pantalla
    private boolean colocar(Seguida seguida, List<ResultPoint> puntos, boolean nueva) {
        // los puntos vienen en píxeles de la imagen de la cámara: se pasan a píxeles de la pantalla
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

        // velocidad suavizada del código, para adelantar la etiqueta y para seguirlo cuando se deje de ver
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
        // la etiqueta llega a donde estará el código al acabar de moverse, no a donde estaba
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

        // si la etiqueta choca con el borde, la punta se desplaza para seguir señalando el código
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

    // cada lectura del mismo código reinicia la cuenta: se va al dejar de apuntarle. Si no se vuelve a leer,
    // la etiqueta sigue frenando en la dirección en la que iba el código (también fuera de la pantalla)
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
        // con DecelerateInterpolator la velocidad inicial es 2·distancia/duración: así arranca a la del código
        long duracion = ETIQUETA_FIJA_MS + ETIQUETA_DESVANECER_MS - LECTURA_PERDIDA_MS;
        float dx = seguida.vx * duracion / 2, dy = seguida.vy * duracion / 2;
        // empieza cuando ya ha acabado el movimiento hacia la última lectura (LECTURA_PERDIDA_MS > ETIQUETA_MOVER_MS)
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
