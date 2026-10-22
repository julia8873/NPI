package com.example.nuietsiit.activities.secciones.secretaria;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.accessibility.AccessibilityManager;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

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
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Asistente de 4 pasos para solicitar un trámite: datos → documentos (foto con la cámara o archivo
 * del almacenamiento) → firma → resumen y envío (mantener pulsado 1,5 s). Al enviar guarda la
 * SOLICITUD y abre su justificante. Al rotar se conservan el paso, los documentos y la firma.
 */
public class SolicitudTramite extends AppCompatActivity {

    private static final String[] TITULOS = {"Tus datos", "Documentos", "Firma", "Resumen"};
    private static final long DURACION_MANTENER = 1500;
    private static final long TAMANO_MAXIMO_ARCHIVO = 10 * 1024 * 1024; // 10 MB

    private SecretariaDB db;
    private Tramite tramite;
    private List<Documento> documentos;
    private String correoUsuario, nombreUsuario;
    private int paso;
    private Bitmap firma;           // exportada al salir del paso de firma
    private boolean firmaConHuella; // true = firmada con la huella en vez de a mano
    private ValueAnimator animEnviar;
    private boolean enviando;       // evita dos envíos si el anillo se completa dos veces
    // idDocumento → ruta del archivo copiado en getFilesDir()/secretaria/
    private final Map<Integer, String> archivos = new HashMap<>();
    private DocumentoSolicitudAdapter adaptadorDocumentos;
    // documento para el que se ha abierto la cámara / el selector (la app puede morir mientras)
    private int idDocumentoPendiente = -1;
    private String rutaFotoPendiente;
    private ActivityResultLauncher<String> lanzadorPermisoCamara;
    private ActivityResultLauncher<Uri> lanzadorFoto;
    private ActivityResultLauncher<String[]> lanzadorArchivo;
    private AlertDialog dialogoDocumento;

    private View[] vistasPaso;
    private LinearProgressIndicator progreso;
    private TextView tvPaso, tvErrorDocumentos, tvErrorFirma, tvResumen, tvAyudaEnviar;
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

        // 2. Documentos: foto con la cámara o archivo del almacenamiento
        ((TextView) findViewById(R.id.tvDocumentos)).setText(documentos.isEmpty()
                ? "Este trámite no necesita documentos."
                : "Haz una foto de cada documento o súbelo desde el móvil (imagen o PDF).");
        tvErrorDocumentos = findViewById(R.id.tvErrorDocumentos);
        configurarDocumentos();

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

        if (savedInstanceState != null) {
            mostrarPaso(restaurar(savedInstanceState));
        } else {
            borrarArchivosSobrantes();
            mostrarPaso(0);
        }
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
        if (paso == 1) {
            List<String> faltan = documentosQueFaltan();
            if (!faltan.isEmpty()) {
                tvErrorDocumentos.setText("Falta: " + String.join(", ", faltan));
                tvErrorDocumentos.setVisibility(View.VISIBLE);
                tvErrorDocumentos.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
                return;
            }
        }
        if (paso == 2) {
            if (!lienzoFirma.tieneFirma()) {
                tvErrorFirma.setVisibility(View.VISIBLE);
                lienzoFirma.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
                return;
            }
            firma = lienzoFirma.exportar();
            firmaConHuella = false;
            // el Bitmap es demasiado grande para el Bundle: se guarda ya por si se rota en el Resumen
            guardarPng(firma, archivoFirmaEnCurso());
        }
        mostrarPaso(paso + 1);
    }

    private List<String> documentosQueFaltan() {
        List<String> faltan = new ArrayList<>();
        for (Documento d : documentos) {
            if (d.obligatorio && !archivos.containsKey(d.id)) faltan.add(d.nombre);
        }
        return faltan;
    }

    private String textoDocumentosResumen() {
        if (documentos.isEmpty()) return "no hacen falta";
        StringBuilder sb = new StringBuilder();
        for (Documento d : documentos) {
            String ruta = archivos.get(d.id);
            sb.append("\n   ").append(ruta == null ? "— " : "✓ ").append(d.nombre);
            if (ruta == null) sb.append(" (no añadido)");
            else if (DocumentoSolicitudAdapter.esPdf(ruta)) sb.append(" (PDF)");
        }
        return sb.toString();
    }

    private void mostrarResumen() {
        String obs = observaciones();
        tvResumen.setText("Trámite: " + tramite.nombre
                + "\nNombre: " + nombreUsuario
                + "\nCorreo: " + correoUsuario
                + "\nObservaciones: " + (obs != null ? obs : "ninguna")
                + "\nDocumentos: " + textoDocumentosResumen()
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

    // ---------- Documentos: cámara o almacenamiento ----------

    private void configurarDocumentos() {
        lanzadorPermisoCamara = registerForActivityResult(new ActivityResultContracts.RequestPermission(), concedido -> {
            if (concedido) abrirCamara(idDocumentoPendiente);
            else Toast.makeText(this, "Sin permiso de cámara: puedes subir el documento como archivo",
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

        RecyclerView rv = findViewById(R.id.rvDocumentos);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adaptadorDocumentos = new DocumentoSolicitudAdapter(documentos, archivos, new DocumentoSolicitudAdapter.Acciones() {
            @Override
            public void hacerFoto(Documento d) {
                SolicitudTramite.this.hacerFoto(d.id);
            }

            @Override
            public void subirArchivo(Documento d) {
                elegirArchivo(d.id);
            }

            @Override
            public void opciones(Documento d) {
                dialogoDocumento = new MaterialAlertDialogBuilder(SolicitudTramite.this)
                        .setTitle(d.nombre)
                        .setItems(new String[]{"📷 Hacer otra foto", "📁 Elegir otro archivo", "🗑 Quitar"}, (dlg, i) -> {
                            if (i == 0) SolicitudTramite.this.hacerFoto(d.id);
                            else if (i == 1) elegirArchivo(d.id);
                            else quitarArchivo(d.id);
                        })
                        .setNegativeButton("Cancelar", null)
                        .show();
            }
        });
        rv.setAdapter(adaptadorDocumentos);
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
        File carpeta = carpetaSecretaria();
        if (carpeta == null) {
            Toast.makeText(this, "No se ha podido preparar la foto", Toast.LENGTH_SHORT).show();
            return;
        }
        File foto = new File(carpeta, "doc_" + idDocumento + "_" + System.currentTimeMillis() + ".jpg");
        idDocumentoPendiente = idDocumento;
        rutaFotoPendiente = foto.getAbsolutePath();
        try {
            lanzadorFoto.launch(FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", foto));
        } catch (ActivityNotFoundException e) {
            rutaFotoPendiente = null;
            Toast.makeText(this, "No hay ninguna app de cámara: sube el documento como archivo",
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
        File carpeta = carpetaSecretaria();
        String tipo = getContentResolver().getType(uri);
        String extension = tipo != null ? MimeTypeMap.getSingleton().getExtensionFromMimeType(tipo) : null;
        if (carpeta == null || tipo == null || (!tipo.startsWith("image/") && !tipo.equals("application/pdf"))) {
            Toast.makeText(this, "Elige una imagen o un PDF", Toast.LENGTH_SHORT).show();
            return;
        }
        File copia = new File(carpeta, "doc_" + idDocumento + "_" + System.currentTimeMillis()
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
        if (anterior != null) new File(anterior).delete();
        adaptadorDocumentos.archivoCambiado(idDocumento, anterior);
        if (documentosQueFaltan().isEmpty()) tvErrorDocumentos.setVisibility(View.GONE);
    }

    private void quitarArchivo(int idDocumento) {
        String ruta = archivos.remove(idDocumento);
        if (ruta != null) new File(ruta).delete();
        adaptadorDocumentos.archivoCambiado(idDocumento, ruta);
    }

    // getFilesDir()/secretaria/ (privado de la app, sin permisos). null si no se puede crear
    private File carpetaSecretaria() {
        File carpeta = new File(getFilesDir(), "secretaria");
        return carpeta.isDirectory() || carpeta.mkdirs() ? carpeta : null;
    }

    // archivos de asistentes que no llegaron a enviarse (p. ej. Android cerró la app en segundo plano)
    private void borrarArchivosSobrantes() {
        archivoFirmaEnCurso().delete();
        File[] enCarpeta = new File(getFilesDir(), "secretaria").listFiles();
        if (enCarpeta == null) return;
        Set<String> enUso = db.getRutasArchivosSolicitudes();
        for (File f : enCarpeta) {
            if (!enUso.contains(f.getAbsolutePath())) f.delete();
        }
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
                        archivoFirmaEnCurso().delete();
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
                rutaFirma != null ? rutaFirma.getAbsolutePath() : null, archivos);
        if (idSolicitud == -1) {
            if (rutaFirma != null) rutaFirma.delete();
            fallo("No se ha podido enviar la solicitud");
            return;
        }
        archivoFirmaEnCurso().delete();
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

    // PNG en getFilesDir()/secretaria/. null si falla
    private File guardarFirma() {
        File carpeta = carpetaSecretaria();
        if (carpeta == null || firma == null) return null;
        File archivo = new File(carpeta, "firma_" + System.currentTimeMillis() + ".png");
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

    private void ocultarTeclado() {
        View foco = getCurrentFocus();
        if (foco == null) return;
        InputMethodManager imm = getSystemService(InputMethodManager.class);
        if (imm != null) imm.hideSoftInputFromWindow(foco.getWindowToken(), 0);
        foco.clearFocus();
    }

    // ---------- Rotación (y que Android cierre la app mientras está abierta la cámara) ----------

    @Override
    protected void onSaveInstanceState(@NonNull Bundle estado) {
        super.onSaveInstanceState(estado); // las observaciones y los trazos del lienzo se guardan solos
        estado.putInt("paso", paso);
        estado.putBoolean("firmaConHuella", firmaConHuella);
        ArrayList<Integer> ids = new ArrayList<>(archivos.keySet());
        ArrayList<String> rutas = new ArrayList<>();
        for (int id : ids) rutas.add(archivos.get(id));
        estado.putIntegerArrayList("idsDocumentos", ids);
        estado.putStringArrayList("rutasDocumentos", rutas);
        estado.putInt("idDocumentoPendiente", idDocumentoPendiente);
        estado.putString("rutaFotoPendiente", rutaFotoPendiente);
        // la firma exportada ya está en archivoFirmaEnCurso() (se guarda al salir del paso de firma)
    }

    // PNG de la firma a mano mientras se está en el Resumen
    private File archivoFirmaEnCurso() {
        return new File(getCacheDir(), "firma_en_curso.png");
    }

    // devuelve el paso en el que hay que seguir
    private int restaurar(Bundle estado) {
        firmaConHuella = estado.getBoolean("firmaConHuella");
        ArrayList<Integer> ids = estado.getIntegerArrayList("idsDocumentos");
        ArrayList<String> rutas = estado.getStringArrayList("rutasDocumentos");
        for (int i = 0; ids != null && rutas != null && i < ids.size(); i++) {
            if (new File(rutas.get(i)).exists()) archivos.put(ids.get(i), rutas.get(i));
        }
        adaptadorDocumentos.notifyDataSetChanged();
        idDocumentoPendiente = estado.getInt("idDocumentoPendiente", -1);
        rutaFotoPendiente = estado.getString("rutaFotoPendiente");
        int pasoGuardado = estado.getInt("paso");
        if (pasoGuardado == 3 && !firmaConHuella) {
            File archivoFirma = archivoFirmaEnCurso();
            if (archivoFirma.exists()) firma = BitmapFactory.decodeFile(archivoFirma.getAbsolutePath());
            // Android ha vaciado la caché: volver a la firma (los trazos del lienzo sí se conservan)
            if (firma == null) return 2;
        }
        return pasoGuardado;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (dialogoDocumento != null) dialogoDocumento.dismiss();
        // salir sin enviar: los archivos copiados ya no sirven para nada
        if (isFinishing() && !enviando) {
            for (String ruta : archivos.values()) new File(ruta).delete();
            if (rutaFotoPendiente != null) new File(rutaFotoPendiente).delete();
            archivoFirmaEnCurso().delete();
        }
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
