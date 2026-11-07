package com.example.nuietsiit.activities.secciones.secretaria;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.pm.PackageManager;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.text.InputType;
import android.view.DragEvent;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityManager;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.core.view.ViewCompat;

import com.example.nuietsiit.R;
import com.example.nuietsiit.activities.BaseActivity;
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
import java.io.InputStream;
import java.io.OutputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Expediente físico para solicitar un trámite: los documentos son tarjetas sobre la mesa (tocar = foto,
 * mantener = archivo) que se arrastran a una carpeta; con todos dentro la carpeta se cierra, se firma
 * en la tapa (o con la huella), se sella manteniendo el dedo y se lanza hacia arriba para enviarla.
 * Al enviar guarda la SOLICITUD y abre su justificante.
 */
public class SolicitudTramite extends BaseActivity {

    // fases: documentos fuera → carpeta cerrada (firma y sello) → sellada (lanzar)
    private static final int DOCUMENTOS = 0, FIRMA = 1, LANZAR = 2;
    private static final String[] NOMBRES_PASO = {"Documentos", "Firma", "Sello", "Envío"};
    private static final long DURACION_SELLO = 1500;
    private static final long ESPERA_BOTON_ENVIAR = 4000;
    private static final float VELOCIDAD_LANZAR_DP = 1200; // dp/s hacia arriba
    private static final int ALTO_TAPA_ABIERTA_DP = 230, ALTO_TAPA_CERRADA_DP = 352;
    private static final long TAMANO_MAXIMO_ARCHIVO = 10 * 1024 * 1024; // 10 MB
    private static final DateTimeFormatter FORMATO_SELLO = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private SecretariaDB db;
    private Tramite tramite;
    private List<Documento> documentos;
    private String correoUsuario, nombreUsuario;
    private String observaciones;   // nota opcional para Secretaría (null = ninguna)
    private int fase;
    private Bitmap firma;           // exportada al sellar
    private boolean firmaConHuella; // true = firmada con la huella en vez de a mano
    private boolean hayHuella;      // el móvil tiene sensor y una huella registrada
    private boolean enviando;       // evita dos envíos (lanzar + botón); true desde que se lanza la carpeta
    private boolean enviada;        // la SOLICITUD ya está en la BD (sus archivos ya no se borran al salir)
    private boolean arrastrando;    // hay un documento en el aire
    // idDocumento → ruta del archivo copiado en getFilesDir()/secretaria/
    private final Map<Integer, String> archivos = new HashMap<>();
    private final Set<Integer> enCarpeta = new LinkedHashSet<>(); // en el orden en que se metieron
    private final MiniaturaDocumento miniaturas = new MiniaturaDocumento();
    // documento para el que se ha abierto la cámara / el selector (la app puede morir mientras)
    private int idDocumentoPendiente = -1;
    private String rutaFotoPendiente;
    private ActivityResultLauncher<String> lanzadorPermisoCamara;
    private ActivityResultLauncher<Uri> lanzadorFoto;
    private ActivityResultLauncher<String[]> lanzadorArchivo;
    private AlertDialog dialogo;

    private Vibrator vibrador;
    private ValueAnimator animSello, animTapa;
    private ObjectAnimator animFlecha;
    private VelocityTracker velocidad;
    private float yInicioLanzar;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable mostrarBotonEnviar = () -> {
        if (fase == LANZAR && !enviando) aparecer(findViewById(R.id.btnEnviar));
    };

    private LinearProgressIndicator progreso;
    private TextView tvPaso, tvInstruccion, tvAyuda, tvContador, tvNota, tvFirmaAqui, tvFirmadoHuella, tvFlecha;
    private TextView marcaSello;
    private View mesaScroll, zonaSoltar, carpeta, tapa, zonaFirma, botonesFirma, sello, capaLanzar;
    private LinearLayout mesa, papeles;
    private LienzoFirma lienzoFirma;
    private CircularProgressIndicator anilloSello;
    private Button btnCerrarCarpeta, btnAbrirCarpeta, btnFirmarHuella, btnEnviar;

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
        getSupportActionBar().setTitle("Solicitud"); // el trámite va en la etiqueta de la carpeta
        documentos = db.getDocumentos(tramite.id);

        correoUsuario = SesionUtil.correo(getIntent());
        nombreUsuario = new UsuariosDB(DBHelper.getInstance(this)).obtenerNombreUsuario(correoUsuario);
        if (nombreUsuario == null) nombreUsuario = "—";
        vibrador = getSystemService(Vibrator.class);

        progreso = findViewById(R.id.progreso);
        tvPaso = findViewById(R.id.tvPaso);
        tvInstruccion = findViewById(R.id.tvInstruccion);
        tvAyuda = findViewById(R.id.tvAyuda);
        mesaScroll = findViewById(R.id.mesaScroll);
        mesa = findViewById(R.id.mesa);
        carpeta = findViewById(R.id.carpeta);
        zonaSoltar = findViewById(R.id.zonaSoltar);
        papeles = findViewById(R.id.papeles);
        tapa = findViewById(R.id.tapa);
        tvContador = findViewById(R.id.tvContador);
        tvNota = findViewById(R.id.tvNota);
        zonaFirma = findViewById(R.id.zonaFirma);
        botonesFirma = findViewById(R.id.botonesFirma);
        lienzoFirma = findViewById(R.id.lienzoFirma);
        tvFirmaAqui = findViewById(R.id.tvFirmaAqui);
        tvFirmadoHuella = findViewById(R.id.tvFirmadoHuella);
        sello = findViewById(R.id.sello);
        anilloSello = findViewById(R.id.anilloSello);
        marcaSello = findViewById(R.id.marcaSello);
        capaLanzar = findViewById(R.id.capaLanzar);
        tvFlecha = findViewById(R.id.tvFlecha);
        btnCerrarCarpeta = findViewById(R.id.btnCerrarCarpeta);
        btnAbrirCarpeta = findViewById(R.id.btnAbrirCarpeta);
        btnFirmarHuella = findViewById(R.id.btnFirmarHuella);
        btnEnviar = findViewById(R.id.btnEnviar);

        ((TextView) findViewById(R.id.tvTituloCarpeta)).setText(tramite.nombre);
        tvNota.setOnClickListener(v -> editarNota());
        btnCerrarCarpeta.setOnClickListener(v -> cerrarCarpeta());
        btnAbrirCarpeta.setOnClickListener(v -> pedirAbrirCarpeta());

        configurarDocumentos();
        carpeta.setOnDragListener(this::alArrastrarSobreCarpeta);
        mesaScroll.setOnDragListener(this::alArrastrarSobreMesa); // sacar hojas de la carpeta

        lienzoFirma.setAlCambiar(this::actualizarSello);
        findViewById(R.id.btnBorrarFirma).setOnClickListener(v -> {
            firmaConHuella = false;
            lienzoFirma.borrar(); // avisa a actualizarSello
        });
        configurarFirmaConHuella();
        configurarSello();
        configurarLanzar();
        btnEnviar.setOnClickListener(v -> {
            if (enviando) return;
            vibrar(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK));
            enviar();
        });

        // atrás: avisar antes de tirar fotos y firma
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                salir();
            }
        });

        if (savedInstanceState != null) {
            restaurar(savedInstanceState);
        } else {
            borrarArchivosSobrantes();
            fase = documentos.isEmpty() ? FIRMA : DOCUMENTOS;
        }
        construirMesa();
        construirPapeles(-1);
        aplicarFase(false);
    }

    // ------------------------------------------------------------
    //   Estado en pantalla según la fase
    // ------------------------------------------------------------

    // pone cada vista como toca en la fase actual. conTransicion = quien llama anima la tapa y las hojas
    @SuppressLint("ClickableViewAccessibility") // el lienzo bloqueado no tiene clic que hacer
    private void aplicarFase(boolean conTransicion) {
        boolean cerrada = fase != DOCUMENTOS;
        boolean firmada = firmaConHuella || lienzoFirma.tieneFirma();

        mesaScroll.setVisibility(cerrada ? View.GONE : View.VISIBLE);
        // sin mesa, la carpeta cerrada se centra en vez de quedarse abajo con un hueco encima
        FrameLayout.LayoutParams lpCarpeta = (FrameLayout.LayoutParams) carpeta.getLayoutParams();
        lpCarpeta.gravity = cerrada ? Gravity.CENTER : Gravity.BOTTOM;
        carpeta.setLayoutParams(lpCarpeta);
        boolean cerrable = fase == DOCUMENTOS && sePuedeCerrar();
        btnCerrarCarpeta.setVisibility(cerrable ? View.VISIBLE : View.GONE);
        btnCerrarCarpeta.setText(todosDentro() ? "Cerrar la carpeta" : "Cerrar la carpeta sin los opcionales");
        btnAbrirCarpeta.setVisibility(cerrada && !documentos.isEmpty() && !enviando ? View.VISIBLE : View.GONE);
        if (!conTransicion) {
            ViewGroup.LayoutParams lp = tapa.getLayoutParams();
            lp.height = dp(cerrada ? ALTO_TAPA_CERRADA_DP : ALTO_TAPA_ABIERTA_DP);
            tapa.setLayoutParams(lp);
            papeles.setAlpha(cerrada ? 0f : 1f);
            papeles.setTranslationY(0);
            zonaFirma.setAlpha(1f);
        }
        zonaFirma.setVisibility(cerrada ? View.VISIBLE : View.GONE);

        mostrarEstadoFirma(firmada);
        botonesFirma.setVisibility(fase == LANZAR ? View.GONE : View.VISIBLE);
        tvNota.setVisibility(fase == LANZAR ? View.GONE : View.VISIBLE);
        tvNota.setText(observaciones == null ? "✎ Añadir una nota (opcional)" : "✎ Nota: " + observaciones);
        // ya sellada: la firma no se puede tocar
        lienzoFirma.setOnTouchListener(fase == LANZAR ? (v, e) -> true : null);

        // sello y lanzar
        sello.setVisibility(fase == FIRMA && firmada ? View.VISIBLE : View.GONE);
        marcaSello.setVisibility(fase == LANZAR ? View.VISIBLE : View.GONE);
        marcaSello.setText("REGISTRADO\nETSIIT · " + LocalDate.now().format(FORMATO_SELLO));
        capaLanzar.setVisibility(fase == LANZAR ? View.VISIBLE : View.GONE);
        zonaSoltar.setVisibility(fase == DOCUMENTOS && enCarpeta.isEmpty() ? View.VISIBLE : View.GONE);
        // la flecha señala a dónde ir: abajo a la carpeta con documentos fuera, arriba para lanzar
        boolean flechaAbajo = fase == DOCUMENTOS && hayDocumentosFuera();
        tvFlecha.setVisibility(fase == LANZAR || flechaAbajo ? View.VISIBLE : View.GONE);
        if (flechaAbajo) animarFlecha(false);
        // abajo, la flecha va justo debajo de la mesa; arriba, justo encima de la carpeta (que está centrada)
        carpeta.post(() -> {
            FrameLayout.LayoutParams lpFlecha = (FrameLayout.LayoutParams) tvFlecha.getLayoutParams();
            lpFlecha.topMargin = fase == LANZAR ? Math.max(0, carpeta.getTop() - tvFlecha.getHeight() - dp(8)) : dp(4);
            tvFlecha.setLayoutParams(lpFlecha);
        });
        if (fase == LANZAR) {
            animarFlecha(true);
            handler.removeCallbacks(mostrarBotonEnviar);
            if (btnEnviar.getVisibility() != View.VISIBLE) handler.postDelayed(mostrarBotonEnviar, ESPERA_BOTON_ENVIAR);
        } else {
            btnEnviar.setVisibility(View.GONE);
        }

        tvContador.setText(fase == DOCUMENTOS
                ? enCarpeta.size() + " de " + documentos.size() + " documentos dentro"
                : "A nombre de " + nombreUsuario);
        mostrarTextos(firmada);
    }

    private void mostrarEstadoFirma(boolean firmada) {
        lienzoFirma.setVisibility(firmaConHuella ? View.INVISIBLE : View.VISIBLE);
        tvFirmadoHuella.setVisibility(firmaConHuella ? View.VISIBLE : View.GONE);
        tvFirmaAqui.setVisibility(!firmada ? View.VISIBLE : View.GONE);
        btnFirmarHuella.setVisibility(hayHuella && !firmaConHuella ? View.VISIBLE : View.GONE);
        findViewById(R.id.btnBorrarFirma).setVisibility(firmada ? View.VISIBLE : View.GONE);
    }

    private void mostrarTextos(boolean firmada) {
        int paso; // 1..4
        if (fase == DOCUMENTOS) {
            paso = 1;
            if (faltaAlgunaFoto()) {
                ponerTextos("Haz una foto de cada documento",
                        "Toca una tarjeta para abrir la cámara · mantenla pulsada para subir un archivo");
            } else if (hayDocumentosFuera()) {
                ponerTextos("Arrastra los documentos a la carpeta",
                        "Desliza cada tarjeta hacia abajo y suéltala sobre la carpeta");
            } else if (todosDentro()) {
                ponerTextos("Todo está en la carpeta", "Toca una hoja para verla o cambiarla, o cierra la carpeta");
            } else {
                ponerTextos("Lo obligatorio ya está dentro", "Añade los opcionales o cierra la carpeta");
            }
        } else if (fase == FIRMA && !firmada) {
            paso = 2;
            ponerTextos("Firma en la carpeta",
                    "Con el dedo, en el recuadro" + (hayHuella ? " · o usa tu huella" : ""));
        } else if (fase == FIRMA) {
            paso = 3;
            ponerTextos("Mantén el dedo sobre el sello 🔏", "Hasta que se complete el anillo y suene el «clac»");
        } else {
            paso = 4;
            ponerTextos("Lanza la carpeta hacia arriba", "Arrástrala y suéltala con un gesto rápido para enviarla");
        }
        tvPaso.setText("Paso " + paso + " de 4 · " + NOMBRES_PASO[paso - 1]);
        progreso.setProgressCompat(paso, true);
    }

    private void ponerTextos(String instruccion, String ayuda) {
        tvInstruccion.setText(instruccion);
        tvAyuda.setText(ayuda);
    }

    private boolean faltaAlgunaFoto() {
        for (Documento d : documentos) {
            if (d.obligatorio && !archivos.containsKey(d.id)) return true;
        }
        return false;
    }

    // con foto pero aún sobre la mesa
    private boolean hayDocumentosFuera() {
        for (Documento d : documentos) {
            if (archivos.containsKey(d.id) && !enCarpeta.contains(d.id)) return true;
        }
        return false;
    }

    private boolean todosDentro() {
        return enCarpeta.size() == documentos.size();
    }

    // todo lo obligatorio dentro y nada con foto fuera (con todo dentro solo pasa al reabrirla:
    // al meter el último documento se cierra sola)
    private boolean sePuedeCerrar() {
        return obligatoriosDentro() && !hayDocumentosFuera();
    }

    // ------------------------------------------------------------
    //   La mesa: tarjetas de documentos
    // ------------------------------------------------------------

    private void construirMesa() {
        mesa.removeAllViews();
        for (Documento d : documentos) {
            if (!enCarpeta.contains(d.id)) mesa.addView(crearTarjeta(d));
        }
        if (mesa.getChildCount() == 0) { // todo dentro: la mesa sigue ahí para sacar hojas
            TextView vacia = new TextView(this);
            vacia.setLayoutParams(new LinearLayout.LayoutParams(dp(260), dp(168)));
            vacia.setGravity(Gravity.CENTER);
            vacia.setPadding(dp(16), 0, dp(16), 0);
            vacia.setBackgroundResource(R.drawable.fondo_documento_vacio);
            vacia.setTextColor(getColor(R.color.nui_apagado));
            vacia.setText("La mesa está vacía\n\nArrastra aquí una hoja de la carpeta para sacarla");
            mesa.addView(vacia);
        }
    }

    @SuppressLint("ClickableViewAccessibility") // el toque simple llama a performClick
    private View crearTarjeta(Documento d) {
        View tarjeta = LayoutInflater.from(this).inflate(R.layout.item_documento_expediente, mesa, false);
        String ruta = archivos.get(d.id);
        boolean llena = ruta != null;
        ImageView ivMiniatura = tarjeta.findViewById(R.id.ivMiniatura);
        ((TextView) tarjeta.findViewById(R.id.tvNombre)).setText(d.nombre);
        tarjeta.findViewById(R.id.tvOpcional).setVisibility(d.obligatorio ? View.GONE : View.VISIBLE);

        if (llena) {
            tarjeta.setBackgroundResource(R.drawable.fondo_documento_lleno);
            tarjeta.setClipToOutline(true); // la foto respeta las esquinas redondeadas
            Bitmap miniatura = miniaturas.de(ruta);
            ivMiniatura.setImageBitmap(miniatura);
            tarjeta.findViewById(R.id.grupoVacio).setVisibility(View.GONE);
            tarjeta.findViewById(R.id.tvALaCarpeta).setVisibility(View.VISIBLE);
            if (miniatura == null) ivMiniatura.setImageResource(android.R.drawable.ic_menu_agenda); // PDF ilegible
            tarjeta.setContentDescription(d.nombre + ", añadido. Arrástralo a la carpeta o tócalo para cambiarlo");
            ViewCompat.addAccessibilityAction(tarjeta, "Meter en la carpeta", (v, args) -> {
                meterEnCarpeta(d.id);
                return true;
            });
            // la tarjeta llena "apunta" a la carpeta balanceándose hacia abajo
            ObjectAnimator balanceo = ObjectAnimator.ofFloat(tarjeta, View.TRANSLATION_Y, 0, dp(8));
            balanceo.setDuration(700);
            balanceo.setRepeatCount(ValueAnimator.INFINITE);
            balanceo.setRepeatMode(ValueAnimator.REVERSE);
            balanceo.start();
            tarjeta.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
                @Override
                public void onViewAttachedToWindow(@NonNull View v) {}

                @Override
                public void onViewDetachedFromWindow(@NonNull View v) {
                    balanceo.cancel(); // la mesa se reconstruye a menudo
                }
            });
        } else {
            tarjeta.setContentDescription(d.nombre + (d.obligatorio ? "" : ", opcional")
                    + ". Toca para fotografiarlo; mantén pulsado para subir un archivo");
        }

        tarjeta.setOnClickListener(v -> {
            if (llena) opcionesDocumento(d);
            else hacerFoto(d.id);
        });
        // toque = foto/opciones, mantener = opciones, deslizar hacia abajo = coger la tarjeta
        // (el deslizamiento horizontal lo intercepta la mesa para desplazarse)
        GestureDetector gestos = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onDown(@NonNull MotionEvent e) {
                return true;
            }

            @Override
            public boolean onSingleTapUp(@NonNull MotionEvent e) {
                tarjeta.performClick();
                return true;
            }

            @Override
            public void onLongPress(@NonNull MotionEvent e) {
                tarjeta.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
                opcionesDocumento(d);
            }

            @Override
            public boolean onScroll(MotionEvent inicio, @NonNull MotionEvent e, float dx, float dy) {
                if (llena && !arrastrando && inicio != null
                        && Math.abs(e.getY() - inicio.getY()) > Math.abs(e.getX() - inicio.getX())) {
                    cogerTarjeta(tarjeta, d);
                }
                return true;
            }
        });
        tarjeta.setOnTouchListener((v, e) -> gestos.onTouchEvent(e));
        return tarjeta;
    }

    // arrastrar y soltar del sistema: la "sombra" de la tarjeta sigue al dedo por toda la pantalla
    private void cogerTarjeta(View tarjeta, Documento d) {
        ClipData datos = ClipData.newPlainText("documento", d.nombre);
        if (tarjeta.startDragAndDrop(datos, new View.DragShadowBuilder(tarjeta), d.id, 0)) {
            arrastrando = true;
            tarjeta.setAlpha(0.3f);
            tarjeta.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
        }
    }

    private boolean alArrastrarSobreCarpeta(View v, DragEvent e) {
        switch (e.getAction()) {
            case DragEvent.ACTION_DRAG_STARTED:
                // solo documentos de la mesa (las hojas de la carpeta van a la mesa)
                if (fase != DOCUMENTOS || !(e.getLocalState() instanceof Integer)
                        || enCarpeta.contains((Integer) e.getLocalState())) return false;
                escalarTapa(1.03f);
                tvContador.setText("Suéltalo aquí");
                return true;
            case DragEvent.ACTION_DRAG_ENTERED:
                escalarTapa(1.07f);
                carpeta.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);
                return true;
            case DragEvent.ACTION_DRAG_EXITED:
                escalarTapa(1.03f);
                return true;
            case DragEvent.ACTION_DROP:
                meterEnCarpeta((Integer) e.getLocalState());
                return true;
            case DragEvent.ACTION_DRAG_ENDED:
                arrastrando = false;
                escalarTapa(1f);
                if (!e.getResult()) { // soltada fuera: vuelve a la mesa
                    construirMesa();
                    aplicarFase(false);
                }
                return true;
            default:
                return true;
        }
    }

    // hojas sacadas de la carpeta: la mesa se ilumina y, al soltar encima, vuelven a ella
    private boolean alArrastrarSobreMesa(View v, DragEvent e) {
        switch (e.getAction()) {
            case DragEvent.ACTION_DRAG_STARTED:
                if (fase != DOCUMENTOS || !(e.getLocalState() instanceof Integer)
                        || !enCarpeta.contains((Integer) e.getLocalState())) return false;
                mesaScroll.setBackgroundResource(R.drawable.fondo_seccion);
                tvInstruccion.setText("Suéltala en la mesa para sacarla");
                return true;
            case DragEvent.ACTION_DRAG_ENTERED:
                mesaScroll.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);
                return true;
            case DragEvent.ACTION_DROP:
                sacarDeCarpeta((Integer) e.getLocalState());
                return true;
            case DragEvent.ACTION_DRAG_ENDED:
                arrastrando = false;
                mesaScroll.setBackground(null);
                if (!e.getResult()) { // soltada fuera: vuelve a la carpeta
                    construirPapeles(-1);
                    aplicarFase(false);
                }
                return true;
            default:
                return true;
        }
    }

    private void escalarTapa(float escala) {
        tapa.animate().scaleX(escala).scaleY(escala).setDuration(150).start();
    }

    private void meterEnCarpeta(int idDocumento) {
        if (fase != DOCUMENTOS || !archivos.containsKey(idDocumento)) return;
        enCarpeta.add(idDocumento);
        vibrar(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK));
        construirMesa();
        construirPapeles(idDocumento);
        aplicarFase(false);
        if (todosDentro()) handler.postDelayed(() -> {
            if (todosDentro()) cerrarCarpeta();
        }, 400);
    }

    // ------------------------------------------------------------
    //   La carpeta: hojas dentro, cerrar
    // ------------------------------------------------------------

    // animarId = hoja recién metida (cae desde arriba); -1 = ninguna
    @SuppressLint("ClickableViewAccessibility") // el toque simple llama a performClick
    private void construirPapeles(int animarId) {
        papeles.removeAllViews();
        int i = 0;
        for (Documento d : documentos) {
            if (!enCarpeta.contains(d.id)) continue;
            // hoja de papel con la miniatura del documento y su nombre abajo
            FrameLayout hoja = new FrameLayout(this);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(64), dp(90));
            lp.setMarginStart(i == 0 ? 0 : (enCarpeta.size() > 4 ? -dp(12) : dp(8)));
            hoja.setLayoutParams(lp);
            hoja.setBackgroundResource(R.drawable.fondo_papel);
            hoja.setClipToOutline(true);
            ImageView miniatura = new ImageView(this);
            miniatura.setScaleType(ImageView.ScaleType.CENTER_CROP);
            miniatura.setImageBitmap(miniaturas.de(archivos.get(d.id)));
            hoja.addView(miniatura, new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            TextView nombre = new TextView(this);
            nombre.setText(d.nombre);
            nombre.setTextSize(9);
            nombre.setTextColor(getColor(R.color.nui_tinta));
            nombre.setMaxLines(2);
            nombre.setGravity(Gravity.CENTER);
            nombre.setBackgroundColor(0xE6FFFFFF);
            nombre.setPadding(dp(3), dp(2), dp(3), dp(2));
            hoja.addView(nombre, new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.TOP));
            hoja.setRotation(i % 2 == 0 ? -3f : 3f);
            hoja.setElevation(dp(1));
            hoja.setContentDescription(d.nombre + ", dentro de la carpeta. Toca para cambiarlo o sacarlo");
            hoja.setOnClickListener(v -> opcionesHoja(d));
            ViewCompat.addAccessibilityAction(hoja, "Sacar de la carpeta", (v, args) -> {
                sacarDeCarpeta(d.id);
                return true;
            });
            // toque = opciones; deslizar hacia arriba = sacarla y llevarla a la mesa
            GestureDetector gestos = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
                @Override
                public boolean onDown(@NonNull MotionEvent e) {
                    return true;
                }

                @Override
                public boolean onSingleTapUp(@NonNull MotionEvent e) {
                    hoja.performClick();
                    return true;
                }

                @Override
                public boolean onScroll(MotionEvent inicio, @NonNull MotionEvent e, float dx, float dy) {
                    if (fase == DOCUMENTOS && !arrastrando && inicio != null && e.getY() - inicio.getY() < -dp(8)) {
                        cogerTarjeta(hoja, d);
                    }
                    return true;
                }
            });
            hoja.setOnTouchListener((v, e) -> gestos.onTouchEvent(e));
            papeles.addView(hoja);
            if (d.id == animarId) {
                hoja.setTranslationY(-dp(70));
                hoja.setAlpha(0f);
                hoja.animate().translationY(0).alpha(1f).setDuration(260)
                        .setInterpolator(new DecelerateInterpolator()).start();
            }
            i++;
        }
    }

    // las hojas se hunden, la tapa sube hasta arriba y "clac"; luego aparece la zona de firma
    private void cerrarCarpeta() {
        if (fase != DOCUMENTOS) return;
        if (dialogo != null) dialogo.dismiss(); // un menú de documento abierto ya no vale con la carpeta cerrada
        fase = FIRMA;
        animTapa = ValueAnimator.ofInt(tapa.getHeight(), dp(ALTO_TAPA_CERRADA_DP));
        animTapa.setDuration(380);
        animTapa.setInterpolator(new DecelerateInterpolator());
        animTapa.addUpdateListener(a -> {
            ViewGroup.LayoutParams lp = tapa.getLayoutParams();
            lp.height = (int) a.getAnimatedValue();
            tapa.setLayoutParams(lp);
        });
        animTapa.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                vibrar(VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK));
                zonaFirma.animate().alpha(1f).setDuration(250).start();
            }
        });
        papeles.animate().translationY(dp(50)).alpha(0f).setDuration(300).start();
        zonaFirma.setAlpha(0f);
        animTapa.start();
        aplicarFase(true);
    }

    // sellada: abrirla rompe el sello (hay que volver a sellar; la firma se conserva)
    private void pedirAbrirCarpeta() {
        if (fase != LANZAR) {
            abrirCarpeta();
            return;
        }
        mostrarDialogo(new MaterialAlertDialogBuilder(this, R.style.DialogoSecretaria)
                .setTitle("¿Abrir la carpeta?")
                .setMessage("Se romperá el sello. Tu firma se queda; solo tendrás que volver a sellar.")
                .setPositiveButton("Abrirla", (d, w) -> abrirCarpeta())
                .setNegativeButton("Cancelar", null));
    }

    // la tapa baja y las hojas vuelven a asomar
    private void abrirCarpeta() {
        if (fase == DOCUMENTOS || enviando) return;
        fase = DOCUMENTOS;
        firma = null;
        archivoFirmaEnCurso().delete();
        handler.removeCallbacks(mostrarBotonEnviar);
        if (animFlecha != null) animFlecha.cancel();
        animTapa = ValueAnimator.ofInt(tapa.getHeight(), dp(ALTO_TAPA_ABIERTA_DP));
        animTapa.setDuration(320);
        animTapa.setInterpolator(new DecelerateInterpolator());
        animTapa.addUpdateListener(a -> {
            ViewGroup.LayoutParams lp = tapa.getLayoutParams();
            lp.height = (int) a.getAnimatedValue();
            tapa.setLayoutParams(lp);
        });
        papeles.setTranslationY(dp(50));
        papeles.animate().translationY(0).alpha(1f).setDuration(300).setStartDelay(120).start();
        vibrar(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK));
        construirMesa();
        animTapa.start();
        aplicarFase(true);
    }

    // ------------------------------------------------------------
    //   Opciones de un documento y nota
    // ------------------------------------------------------------

    private void opcionesDocumento(Documento d) {
        if (fase != DOCUMENTOS) return;
        boolean lleno = archivos.containsKey(d.id);
        String[] opciones = lleno
                ? new String[]{"👁 Verlo en grande", "📷 Hacer otra foto", "📁 Elegir otro archivo", "🗑 Quitar"}
                : new String[]{"📷 Hacer una foto", "📁 Subir un archivo (imagen o PDF)"};
        mostrarDialogo(new MaterialAlertDialogBuilder(this, R.style.DialogoSecretaria)
                .setTitle(d.nombre)
                .setItems(opciones, (dlg, i) -> {
                    if (lleno) i--; // "Ver" va primero
                    if (i == -1) verDocumento(d);
                    else if (i == 0) hacerFoto(d.id);
                    else if (i == 1) elegirArchivo(d.id);
                    else quitarArchivo(d.id);
                })
                .setNegativeButton("Cancelar", null));
    }

    private void opcionesHoja(Documento d) {
        if (fase != DOCUMENTOS) return;
        mostrarDialogo(new MaterialAlertDialogBuilder(this, R.style.DialogoSecretaria)
                .setTitle(d.nombre)
                .setItems(new String[]{"👁 Verlo en grande", "📷 Hacer otra foto", "📁 Elegir otro archivo",
                        "↩ Sacar de la carpeta"}, (dlg, i) -> {
                    if (i == 0) verDocumento(d);
                    else if (i == 1) hacerFoto(d.id);
                    else if (i == 2) elegirArchivo(d.id);
                    else sacarDeCarpeta(d.id);
                })
                .setNegativeButton("Cancelar", null));
    }

    // el documento a pantalla casi completa, con atajo para rehacer la foto
    private void verDocumento(Documento d) {
        String ruta = archivos.get(d.id);
        if (ruta == null) return;
        Bitmap imagen = MiniaturaDocumento.grande(ruta);
        if (imagen == null) {
            Toast.makeText(this, "No se puede mostrar este archivo", Toast.LENGTH_SHORT).show();
            return;
        }
        ImageView iv = new ImageView(this);
        iv.setImageBitmap(imagen);
        iv.setAdjustViewBounds(true);
        iv.setMaxHeight(Math.round(getResources().getDisplayMetrics().heightPixels * 0.65f));
        iv.setPadding(dp(16), dp(8), dp(16), 0);
        iv.setContentDescription(d.nombre);
        mostrarDialogo(new MaterialAlertDialogBuilder(this, R.style.DialogoSecretaria)
                .setTitle(d.nombre)
                .setView(iv)
                .setPositiveButton("Cerrar", null)
                .setNeutralButton("📷 Repetir foto", (dlg, w) -> hacerFoto(d.id)));
    }

    private void sacarDeCarpeta(int idDocumento) {
        if (fase != DOCUMENTOS) return;
        enCarpeta.remove(idDocumento);
        construirMesa();
        construirPapeles(-1);
        aplicarFase(false);
    }

    private void editarNota() {
        EditText et = new EditText(this);
        et.setText(observaciones);
        et.setHint("Algo que deba saber Secretaría");
        et.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE
                | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        et.setMaxLines(4);
        FrameLayout marco = new FrameLayout(this);
        marco.setPadding(dp(24), dp(8), dp(24), 0);
        marco.addView(et);
        mostrarDialogo(new MaterialAlertDialogBuilder(this, R.style.DialogoSecretaria)
                .setTitle("Nota para Secretaría")
                .setView(marco)
                .setPositiveButton("Guardar", (dlg, w) -> {
                    String texto = et.getText().toString().trim();
                    observaciones = texto.isEmpty() ? null : texto;
                    aplicarFase(false);
                })
                .setNegativeButton("Cancelar", null));
    }

    private void mostrarDialogo(MaterialAlertDialogBuilder constructor) {
        if (dialogo != null) dialogo.dismiss();
        dialogo = constructor.show();
    }

    // ------------------------------------------------------------
    //   Documentos: cámara o almacenamiento
    // ------------------------------------------------------------

    private void configurarDocumentos() {
        lanzadorPermisoCamara = registerForActivityResult(new ActivityResultContracts.RequestPermission(), concedido -> {
            if (concedido) abrirCamara(idDocumentoPendiente);
            else Toast.makeText(this, "Sin permiso de cámara: mantén pulsada la tarjeta para subir un archivo",
                    Toast.LENGTH_LONG).show();
        });
        lanzadorFoto = registerForActivityResult(new ActivityResultContracts.TakePicture(), hecha -> {
            if (rutaFotoPendiente == null) return;
            File foto = new File(rutaFotoPendiente);
            // algunas cámaras devuelven OK sin escribir nada
            if (Boolean.TRUE.equals(hecha) && foto.length() > 0) ponerArchivo(idDocumentoPendiente, foto.getAbsolutePath());
            else foto.delete();
            rutaFotoPendiente = null;
        });
        lanzadorArchivo = registerForActivityResult(new ActivityResultContracts.OpenDocument(), uri -> {
            if (uri != null) copiarArchivo(idDocumentoPendiente, uri);
        });
    }

    // la librería del escáner QR declara CAMERA en el manifest, así que hay que pedirlo para usar la cámara
    private void hacerFoto(int idDocumento) {
        idDocumentoPendiente = idDocumento;
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            abrirCamara(idDocumento);
        } else {
            lanzadorPermisoCamara.launch(Manifest.permission.CAMERA);
        }
    }

    // la app de cámara escribe la foto directamente en nuestra carpeta a través del FileProvider
    private void abrirCamara(int idDocumento) {
        if (rutaFotoPendiente != null) return; // doble toque: la cámara ya se está abriendo
        File carpetaArchivos = carpetaSecretaria();
        if (carpetaArchivos == null) {
            Toast.makeText(this, "No se ha podido preparar la foto", Toast.LENGTH_SHORT).show();
            return;
        }
        File foto = new File(carpetaArchivos, "doc_" + idDocumento + "_" + System.currentTimeMillis() + ".jpg");
        idDocumentoPendiente = idDocumento;
        rutaFotoPendiente = foto.getAbsolutePath();
        try {
            lanzadorFoto.launch(FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", foto));
        } catch (ActivityNotFoundException e) {
            rutaFotoPendiente = null;
            Toast.makeText(this, "No hay ninguna app de cámara: mantén pulsada la tarjeta para subir un archivo",
                    Toast.LENGTH_LONG).show();
        }
    }

    private void elegirArchivo(int idDocumento) {
        idDocumentoPendiente = idDocumento;
        try {
            lanzadorArchivo.launch(new String[]{"image/*", "application/pdf"});
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, "No hay ninguna app para elegir archivos", Toast.LENGTH_SHORT).show();
        }
    }

    // copia el archivo elegido a nuestra carpeta: el permiso sobre la Uri no dura para siempre
    private void copiarArchivo(int idDocumento, Uri uri) {
        File carpetaArchivos = carpetaSecretaria();
        String tipo = getContentResolver().getType(uri);
        String extension = tipo != null ? MimeTypeMap.getSingleton().getExtensionFromMimeType(tipo) : null;
        if (carpetaArchivos == null || tipo == null || (!tipo.startsWith("image/") && !tipo.equals("application/pdf"))) {
            Toast.makeText(this, "Elige una imagen o un PDF", Toast.LENGTH_SHORT).show();
            return;
        }
        File copia = new File(carpetaArchivos, "doc_" + idDocumento + "_" + System.currentTimeMillis()
                + "." + (extension != null ? extension : "jpg"));
        try (InputStream entrada = getContentResolver().openInputStream(uri);
             OutputStream salida = new FileOutputStream(copia)) {
            if (entrada == null) throw new IOException();
            byte[] bufer = new byte[8192];
            long total = 0;
            int leidos;
            while ((leidos = entrada.read(bufer)) != -1) {
                total += leidos;
                if (total > TAMANO_MAXIMO_ARCHIVO) {
                    salida.close();
                    copia.delete();
                    Toast.makeText(this, "El archivo es demasiado grande (máximo 10 MB)", Toast.LENGTH_LONG).show();
                    return;
                }
                salida.write(bufer, 0, leidos);
            }
        } catch (IOException | SecurityException e) {
            copia.delete();
            Toast.makeText(this, "No se ha podido leer el archivo", Toast.LENGTH_SHORT).show();
            return;
        }
        ponerArchivo(idDocumento, copia.getAbsolutePath());
    }

    // sustituye (y borra) el archivo anterior del documento
    private void ponerArchivo(int idDocumento, String ruta) {
        String anterior = archivos.put(idDocumento, ruta);
        if (anterior != null) {
            new File(anterior).delete();
            miniaturas.olvidar(anterior);
        }
        construirMesa();
        construirPapeles(-1); // por si el documento ya estaba dentro de la carpeta
        aplicarFase(false);
    }

    private void quitarArchivo(int idDocumento) {
        if (fase != DOCUMENTOS) return;
        String ruta = archivos.remove(idDocumento);
        if (ruta != null) {
            new File(ruta).delete();
            miniaturas.olvidar(ruta);
        }
        enCarpeta.remove(idDocumento);
        construirMesa();
        construirPapeles(-1);
        aplicarFase(false);
    }

    // getFilesDir()/secretaria/ (privado de la app, sin permisos). null si no se puede crear
    private File carpetaSecretaria() {
        File carpetaArchivos = new File(getFilesDir(), "secretaria");
        return carpetaArchivos.isDirectory() || carpetaArchivos.mkdirs() ? carpetaArchivos : null;
    }

    // archivos de solicitudes que no llegaron a enviarse (p. ej. Android cerró la app en segundo plano)
    private void borrarArchivosSobrantes() {
        archivoFirmaEnCurso().delete();
        File[] enCarpetaArchivos = new File(getFilesDir(), "secretaria").listFiles();
        if (enCarpetaArchivos == null) return;
        Set<String> enUso = db.getRutasArchivosSolicitudes();
        for (File f : enCarpetaArchivos) {
            if (!enUso.contains(f.getAbsolutePath())) f.delete();
        }
    }

    // ------------------------------------------------------------
    //   Firma: a mano o con la huella
    // ------------------------------------------------------------

    // al firmar o borrar: aparece (o se va) el sello
    private void actualizarSello() {
        if (fase != FIRMA) return;
        boolean antes = sello.getVisibility() == View.VISIBLE;
        boolean firmada = firmaConHuella || lienzoFirma.tieneFirma();
        mostrarEstadoFirma(firmada);
        sello.setVisibility(firmada ? View.VISIBLE : View.GONE);
        mostrarTextos(firmada);
        if (!antes && sello.getVisibility() == View.VISIBLE) {
            sello.setScaleX(0f);
            sello.setScaleY(0f);
            sello.animate().scaleX(1f).scaleY(1f).setDuration(320)
                    .setInterpolator(new OvershootInterpolator(2.5f)).start();
            sello.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);
        }
    }

    // el botón solo aparece si hay sensor y una huella registrada
    private void configurarFirmaConHuella() {
        int autenticador = BiometricManager.Authenticators.BIOMETRIC_STRONG;
        hayHuella = BiometricManager.from(this).canAuthenticate(autenticador) == BiometricManager.BIOMETRIC_SUCCESS;
        if (!hayHuella) return;

        BiometricPrompt prompt = new BiometricPrompt(this, ContextCompat.getMainExecutor(this),
                new BiometricPrompt.AuthenticationCallback() {
                    @Override
                    public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult resultado) {
                        firmaConHuella = true;
                        firma = null;
                        archivoFirmaEnCurso().delete();
                        actualizarSello();
                    }

                    @Override
                    public void onAuthenticationError(int codigo, @NonNull CharSequence mensaje) {
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
        btnFirmarHuella.setOnClickListener(v -> prompt.authenticate(info));
    }

    // ------------------------------------------------------------
    //   Sello: mantener el dedo, vibración que crece hasta el "clac"
    // ------------------------------------------------------------

    @SuppressLint("ClickableViewAccessibility") // se llama a performClick al soltar
    private void configurarSello() {
        // con TalkBack no se puede mantener pulsado con precisión: el doble toque sella
        sello.setOnClickListener(v -> {
            if (conTalkBack() && fase == FIRMA) sellar();
        });
        sello.setOnTouchListener((v, e) -> {
            switch (e.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    v.getParent().requestDisallowInterceptTouchEvent(true);
                    empezarSello();
                    return true;
                case MotionEvent.ACTION_UP:
                    v.performClick();
                    soltarSello();
                    return true;
                case MotionEvent.ACTION_CANCEL:
                    soltarSello();
                    return true;
                default:
                    return true;
            }
        });
    }

    private void empezarSello() {
        if (fase != FIRMA) return;
        vibrarCreciente();
        sello.animate().scaleX(0.88f).scaleY(0.88f).setDuration(DURACION_SELLO).start(); // se va hundiendo
        animSello = ValueAnimator.ofInt(0, anilloSello.getMax());
        animSello.setDuration(DURACION_SELLO);
        animSello.addUpdateListener(a -> anilloSello.setProgress((int) a.getAnimatedValue()));
        animSello.addListener(new AnimatorListenerAdapter() {
            private boolean cancelado;

            @Override
            public void onAnimationCancel(Animator animation) {
                cancelado = true;
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                if (!cancelado) sellar();
            }
        });
        animSello.start();
    }

    // soltar antes de completar el anillo = no se sella
    private void soltarSello() {
        if (animSello == null || !animSello.isRunning()) return;
        animSello.cancel();
        if (vibrador != null) vibrador.cancel();
        anilloSello.setProgressCompat(0, true);
        sello.animate().scaleX(1f).scaleY(1f).setDuration(150).start();
        tvAyuda.setText("Mantén el dedo hasta que se complete el anillo");
    }

    // 15 tramos de 100 ms cada vez más fuertes; sin control de intensidad, pulsos cada vez más seguidos
    private void vibrarCreciente() {
        if (vibrador == null || !vibrador.hasVibrator()) return;
        if (vibrador.hasAmplitudeControl()) {
            int tramos = 15;
            long[] tiempos = new long[tramos];
            int[] intensidades = new int[tramos];
            for (int i = 0; i < tramos; i++) {
                tiempos[i] = DURACION_SELLO / tramos;
                intensidades[i] = 10 + i * 245 / (tramos - 1);
            }
            vibrador.vibrate(VibrationEffect.createWaveform(tiempos, intensidades, -1));
        } else {
            vibrador.vibrate(VibrationEffect.createWaveform(
                    new long[]{0, 25, 220, 25, 170, 25, 130, 25, 100, 25, 80, 25, 60, 25, 45, 25, 35, 25}, -1));
        }
    }

    private void sellar() {
        if (fase != FIRMA) return;
        if (vibrador != null) vibrador.cancel();
        vibrar(VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK)); // el "clac"
        if (!firmaConHuella) {
            firma = lienzoFirma.exportar();
            // el Bitmap es demasiado grande para el Bundle: se guarda ya por si se rota antes de lanzar
            guardarPng(firma, archivoFirmaEnCurso());
        }
        fase = LANZAR;
        sello.animate().cancel();
        sello.setScaleX(1f);
        sello.setScaleY(1f);
        anilloSello.setProgress(0);
        aplicarFase(false);
        // la marca cae de golpe sobre la tapa, que se hunde un poco
        marcaSello.setScaleX(2.4f);
        marcaSello.setScaleY(2.4f);
        marcaSello.setAlpha(0f);
        marcaSello.animate().scaleX(1f).scaleY(1f).alpha(1f).setDuration(170)
                .setInterpolator(new AccelerateInterpolator()).start();
        tapa.animate().translationY(dp(4)).setStartDelay(150).setDuration(70)
                .withEndAction(() -> tapa.animate().translationY(0).setStartDelay(0).setDuration(140).start())
                .start();
    }

    // ------------------------------------------------------------
    //   Lanzar la carpeta hacia arriba (VelocityTracker)
    // ------------------------------------------------------------

    @SuppressLint("ClickableViewAccessibility") // se llama a performClick al soltar
    private void configurarLanzar() {
        capaLanzar.setOnClickListener(v -> {
            if (conTalkBack()) confirmarEnvio();
        });
        capaLanzar.setOnTouchListener((v, e) -> {
            if (enviando) return true;
            // coordenadas de pantalla: la capa se mueve con la carpeta
            MotionEvent absoluto = MotionEvent.obtain(e);
            absoluto.setLocation(e.getRawX(), e.getRawY());
            switch (e.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    velocidad = VelocityTracker.obtain();
                    velocidad.addMovement(absoluto);
                    yInicioLanzar = e.getRawY();
                    carpeta.animate().cancel();
                    pararFlecha();
                    break;
                case MotionEvent.ACTION_MOVE:
                    if (velocidad != null) velocidad.addMovement(absoluto);
                    float dy = e.getRawY() - yInicioLanzar;
                    carpeta.setTranslationY(dy < 0 ? dy : dy * 0.2f); // hacia abajo apenas se mueve
                    carpeta.setRotation(Math.max(-6f, dy / dp(40)));
                    break;
                case MotionEvent.ACTION_UP:
                    v.performClick();
                    float vy = 0;
                    if (velocidad != null) {
                        velocidad.addMovement(absoluto);
                        velocidad.computeCurrentVelocity(1000);
                        vy = velocidad.getYVelocity();
                    }
                    soltarVelocidad();
                    if (vy < -dp(VELOCIDAD_LANZAR_DP) || carpeta.getTranslationY() < -carpeta.getHeight() * 0.6f) {
                        lanzar(vy);
                    } else {
                        devolverCarpeta(true);
                    }
                    break;
                case MotionEvent.ACTION_CANCEL:
                    soltarVelocidad();
                    devolverCarpeta(false);
                    break;
            }
            absoluto.recycle();
            return true;
        });
    }

    private void soltarVelocidad() {
        if (velocidad != null) velocidad.recycle();
        velocidad = null;
    }

    private void lanzar(float vy) {
        if (enviando) return;
        enviando = true;
        handler.removeCallbacks(mostrarBotonEnviar);
        vibrar(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK));
        float destino = -(carpeta.getBottom() + dp(60)); // fuera por arriba
        float distancia = Math.abs(destino - carpeta.getTranslationY());
        long duracion = (long) Math.max(160, Math.min(420, distancia / Math.max(1f, -vy) * 1000));
        carpeta.animate().translationY(destino).rotation(-10f).setDuration(duracion)
                .setInterpolator(new AccelerateInterpolator())
                .withEndAction(this::enviar).start();
    }

    // vuelve a su sitio con un rebote; si se soltó flojo, se explica cómo lanzarla
    private void devolverCarpeta(boolean explicar) {
        carpeta.animate().translationY(0).rotation(0).setDuration(320)
                .setInterpolator(new OvershootInterpolator()).start();
        animarFlecha(true);
        if (explicar && carpeta.getTranslationY() < -dp(10)) {
            tvAyuda.setText("Más rápido: desliza hacia arriba y suelta sin frenar");
        }
    }

    private void confirmarEnvio() {
        mostrarDialogo(new MaterialAlertDialogBuilder(this, R.style.DialogoSecretaria)
                .setTitle("¿Enviar la solicitud?")
                .setMessage(tramite.nombre)
                .setPositiveButton("Enviar", (d, w) -> {
                    if (enviando) return;
                    vibrar(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK));
                    enviar();
                })
                .setNegativeButton("Cancelar", null));
    }

    private void animarFlecha(boolean arriba) {
        String texto = arriba ? "⬆" : "⬇";
        if (animFlecha == null || !texto.contentEquals(tvFlecha.getText())) {
            if (animFlecha != null) animFlecha.cancel();
            tvFlecha.setText(texto);
            animFlecha = ObjectAnimator.ofFloat(tvFlecha, View.TRANSLATION_Y, 0, dp(arriba ? -16 : 16));
            animFlecha.setDuration(550);
            animFlecha.setRepeatCount(ValueAnimator.INFINITE);
            animFlecha.setRepeatMode(ValueAnimator.REVERSE);
        }
        tvFlecha.setAlpha(1f);
        if (!animFlecha.isStarted()) animFlecha.start();
    }

    private void pararFlecha() {
        if (animFlecha != null) animFlecha.cancel();
        tvFlecha.setAlpha(0f);
    }

    // ------------------------------------------------------------
    //   Envío
    // ------------------------------------------------------------

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
        long idSolicitud = db.insertarSolicitud(correoUsuario, tramite.id, observaciones,
                rutaFirma != null ? rutaFirma.getAbsolutePath() : null, archivos);
        if (idSolicitud == -1) {
            if (rutaFirma != null) rutaFirma.delete();
            fallo("No se ha podido enviar la solicitud");
            return;
        }
        enviada = true;
        archivoFirmaEnCurso().delete();
        startActivity(new Intent(this, Justificante.class)
                .putExtra("idSolicitud", (int) idSolicitud)
                .putExtra("CORREO", correoUsuario));
        finish(); // al volver desde el justificante no se vuelve a la solicitud
    }

    private void fallo(String mensaje) {
        enviando = false;
        carpeta.animate().cancel();
        carpeta.setTranslationY(0);
        carpeta.setRotation(0);
        aplicarFase(false);
        Toast.makeText(this, mensaje, Toast.LENGTH_SHORT).show();
    }

    // PNG en getFilesDir()/secretaria/. null si falla
    private File guardarFirma() {
        File carpetaArchivos = carpetaSecretaria();
        if (carpetaArchivos == null || firma == null) return null;
        File archivo = new File(carpetaArchivos, "firma_" + System.currentTimeMillis() + ".png");
        return guardarPng(firma, archivo) ? archivo : null;
    }

    private static boolean guardarPng(Bitmap bitmap, File archivo) {
        try (FileOutputStream salida = new FileOutputStream(archivo)) {
            if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, salida)) throw new IOException();
            return true;
        } catch (IOException e) {
            archivo.delete();
            return false;
        }
    }

    // ------------------------------------------------------------
    //   Utilidades
    // ------------------------------------------------------------

    private void salir() {
        boolean hayAlgo = !archivos.isEmpty() || observaciones != null || fase == LANZAR
                || firmaConHuella || lienzoFirma.tieneFirma();
        if (enviando) return; // la carpeta va volando: el envío ya está en marcha
        if (!hayAlgo) {
            finish();
            return;
        }
        mostrarDialogo(new MaterialAlertDialogBuilder(this, R.style.DialogoSecretaria)
                .setTitle("¿Salir sin enviar?")
                .setMessage("Se perderán las fotos y la firma.")
                .setPositiveButton("Salir", (d, w) -> finish())
                .setNegativeButton("Seguir", null));
    }

    private void aparecer(View v) {
        v.setAlpha(0f);
        v.setVisibility(View.VISIBLE);
        v.animate().alpha(1f).setDuration(250).start();
    }

    private void vibrar(VibrationEffect efecto) {
        if (vibrador != null && vibrador.hasVibrator()) vibrador.vibrate(efecto);
    }

    private boolean conTalkBack() {
        AccessibilityManager am = getSystemService(AccessibilityManager.class);
        return am != null && am.isTouchExplorationEnabled();
    }

    private int dp(float valor) {
        return Math.round(valor * getResources().getDisplayMetrics().density);
    }

    // agitar a mitad de la solicitud volvía al menú y se perdía todo lo rellenado
    @Override
    protected boolean usaAgitar() {
        return false;
    }

    // ------------------------------------------------------------
    //   Que Android cierre la app mientras está abierta la cámara
    // ------------------------------------------------------------

    @Override
    protected void onSaveInstanceState(@NonNull Bundle estado) {
        super.onSaveInstanceState(estado); // los trazos del lienzo se guardan solos
        estado.putInt("fase", fase);
        estado.putBoolean("firmaConHuella", firmaConHuella);
        estado.putString("observaciones", observaciones);
        ArrayList<Integer> ids = new ArrayList<>(archivos.keySet());
        ArrayList<String> rutas = new ArrayList<>();
        for (int id : ids) rutas.add(archivos.get(id));
        estado.putIntegerArrayList("idsDocumentos", ids);
        estado.putStringArrayList("rutasDocumentos", rutas);
        estado.putIntegerArrayList("enCarpeta", new ArrayList<>(enCarpeta));
        estado.putInt("idDocumentoPendiente", idDocumentoPendiente);
        estado.putString("rutaFotoPendiente", rutaFotoPendiente);
        // la firma exportada ya está en archivoFirmaEnCurso() (se guarda al sellar)
    }

    // PNG de la firma a mano entre sellar y enviar
    private File archivoFirmaEnCurso() {
        return new File(getCacheDir(), "firma_en_curso.png");
    }

    private void restaurar(Bundle estado) {
        fase = estado.getInt("fase");
        firmaConHuella = estado.getBoolean("firmaConHuella");
        observaciones = estado.getString("observaciones");
        ArrayList<Integer> ids = estado.getIntegerArrayList("idsDocumentos");
        ArrayList<String> rutas = estado.getStringArrayList("rutasDocumentos");
        for (int i = 0; ids != null && rutas != null && i < ids.size(); i++) {
            if (new File(rutas.get(i)).exists()) archivos.put(ids.get(i), rutas.get(i));
        }
        ArrayList<Integer> dentro = estado.getIntegerArrayList("enCarpeta");
        if (dentro != null) {
            for (int id : dentro) if (archivos.containsKey(id)) enCarpeta.add(id);
        }
        idDocumentoPendiente = estado.getInt("idDocumentoPendiente", -1);
        rutaFotoPendiente = estado.getString("rutaFotoPendiente");
        if (fase == LANZAR && !firmaConHuella) {
            File archivoFirma = archivoFirmaEnCurso();
            if (archivoFirma.exists()) firma = BitmapFactory.decodeFile(archivoFirma.getAbsolutePath());
            // Android ha vaciado la caché: volver a sellar (los trazos del lienzo sí se conservan)
            if (firma == null) fase = FIRMA;
        }
        // si falta algún archivo obligatorio (borrado mientras tanto), la carpeta vuelve a abrirse
        if (fase != DOCUMENTOS && !obligatoriosDentro()) fase = DOCUMENTOS;
    }

    private boolean obligatoriosDentro() {
        for (Documento d : documentos) {
            if (d.obligatorio && !enCarpeta.contains(d.id)) return false;
        }
        return true;
    }

    // los trazos del lienzo se restauran después de onCreate: ahora ya se sabe si hay firma
    @Override
    protected void onRestoreInstanceState(@NonNull Bundle estado) {
        super.onRestoreInstanceState(estado);
        aplicarFase(false);
    }

    @Override
    protected void onPause() {
        super.onPause();
        soltarSello(); // salir de la app con el dedo en el sello no debe sellar
        if (velocidad != null && !enviando) {
            soltarVelocidad();
            devolverCarpeta(false);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);
        if (dialogo != null) dialogo.dismiss();
        if (animFlecha != null) animFlecha.cancel();
        // salir sin enviar: los archivos copiados ya no sirven para nada
        if (isFinishing() && !enviada) {
            for (String ruta : archivos.values()) new File(ruta).delete();
            if (rutaFotoPendiente != null) new File(rutaFotoPendiente).delete();
            archivoFirmaEnCurso().delete();
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        salir();
        return true;
    }
}
