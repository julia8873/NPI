package com.example.nuietsiit.activities.secciones;

import android.annotation.SuppressLint;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.net.Uri;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import com.example.nuietsiit.R;
import com.example.nuietsiit.activities.BaseActivity;
import com.example.nuietsiit.activities.auth.Login;
import com.example.nuietsiit.activities.secciones.actividades.HojaComunidades;
import com.example.nuietsiit.activities.secciones.mapa.Mapa;
import com.example.nuietsiit.activities.secciones.secretaria.AvisoDeshacer;
import com.example.nuietsiit.data.ActividadesDB;
import com.example.nuietsiit.data.DBHelper;
import com.example.nuietsiit.data.modelo.Actividad;
import com.example.nuietsiit.ui.CabeceraNui;
import com.example.nuietsiit.ui.InclinarParaPasar;
import com.example.nuietsiit.ui.PestanasPapel;
import com.example.nuietsiit.util.CalendarioUtil;
import com.example.nuietsiit.util.FechasUtil;
import com.example.nuietsiit.util.RecordatorioActividad;
import com.example.nuietsiit.util.tutorial.Gesto;
import com.example.nuietsiit.util.tutorial.Tutorial;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Pantalla de Actividades SIN SCROLL: un "mazo" con una ficha grande cada vez.
 * Diseño NUI (DisenoNUI/DESIGN.md), igual que Secretaría y Biblioteca: yesería + arquería, pestañas de papel,
 * fichas, sellos y zócalo. Nada de colores ni tamaños a mano: todo sale de los tokens nui_*.
 * - Deslizar a la derecha (o "Me apunto") = me apunto y pasa a la siguiente.
 * - Deslizar a la izquierda (o "Paso") = paso, siguiente.
 * - SENSOR (acelerómetro): inclinar el móvil a la derecha = me apunto, a la izquierda = paso.
 *   Usa InclinarParaPasar, el mismo gesto que pasar páginas en Secretaría y Biblioteca.
 * - Flecha = volver a la anterior. Tocar la ficha = detalle completo (apuntarse, calendario, llévame, más info).
 * - Sin sesión iniciada se pueden ver, pero no apuntarse.
 * - CÁMARA: "Escanear" lee el QR de un cartel de actividad (zxing, como el escáner de Biblioteca)
 *   y abre esa actividad para apuntarse. El QR lleva "NUIETSIIT:ACTIVIDAD:" + el título sin tildes en
 *   minúsculas y con guiones (p. ej. "...:arduino-desde-cero"), o el id.
 * - VOZ: el micrófono escucha una orden ("paso", "me apunto", "anterior", "llévame", "¿cuándo es?",
 *   "léemela", "charlas"...) con el reconocedor de voz de Google (no pide permisos) y contesta hablando.
 * - Tutorial interactivo la primera vez (crearTutorial), como los de Izaro.
 */
public class Actividades extends BaseActivity {

    private static final String FILTRO_TODAS = "__todas";       // pestaña "Todas"
    private static final String FILTRO_APUNTADO = "__apuntado"; // pestaña "Mis actividades"
    private static final int MS_DESHACER = 5000;
    /** Lo que llevan los QR de los carteles: este prefijo y el título en forma de clave (o el id). */
    public static final String PREFIJO_QR = "NUIETSIIT:ACTIVIDAD:";

    private ActividadesDB actividadesDB;
    private String correoUsuario;          // null = no hay sesión iniciada
    private String filtroSeleccionado;     // null = todas
    private List<Actividad> todas = new ArrayList<>();
    private List<Actividad> mazo = new ArrayList<>(); // las del filtro actual
    private int posicion;                  // carta que se ve ahora

    private View raiz, botonera, vacio;
    private View carta;
    private PestanasPapel pestanasTipos;
    private TextView tvVacio, tvContador, selloSi, selloNo;
    private InclinarParaPasar inclinar;

    // Órdenes por voz: reconocedor de Google (pantalla del sistema) y respuesta hablada
    private final ActivityResultLauncher<Intent> reconocedor = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), resultado -> {
                if (resultado.getResultCode() != RESULT_OK || resultado.getData() == null) return;
                List<String> frases = resultado.getData().getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                if (frases != null && !frases.isEmpty()) alOir(frases);
            });
    private TextToSpeech voz;
    private boolean vozLista;

    // Escáner de QR (pantalla de zxing; pide ella el permiso de cámara)
    private final ActivityResultLauncher<ScanOptions> escaner = registerForActivityResult(new ScanContract(),
            resultado -> { if (resultado.getContents() != null) alEscanear(resultado.getContents()); });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.actividades);
        // Entra como una hoja de papel, igual que las pantallas de Secretaría
        overridePendingTransition(R.anim.nui_papel_entra, R.anim.nui_quieto);

        // Barra de yesería (como Secretaría y Biblioteca): atrás, título, chat y perfil
        MaterialToolbar barra = findViewById(R.id.toolbar);
        setSupportActionBar(barra);
        CabeceraNui.preparar(barra);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Actividades");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // Sesión guardada al iniciar sesión (SharedPreferences "SesionUsuario")
        correoUsuario = correoSesion();
        actividadesDB = new ActividadesDB(DBHelper.getInstance(this));

        raiz = findViewById(android.R.id.content);
        botonera = findViewById(R.id.botonera);
        vacio = findViewById(R.id.vacio);
        carta = findViewById(R.id.carta);
        pestanasTipos = findViewById(R.id.pestanasTipos);
        tvVacio = findViewById(R.id.tvVacio);
        tvContador = findViewById(R.id.tvContador);
        selloSi = findViewById(R.id.selloSi);
        selloNo = findViewById(R.id.selloNo);

        // Botón de comunidades de chat (grupos de WhatsApp/Telegram/Discord simulados)
        findViewById(R.id.btnComunidades).setOnClickListener(
                v -> HojaComunidades.mostrar(this, correoUsuario));
        findViewById(R.id.btnVoz).setOnClickListener(v -> escucharOrden());
        voz = new TextToSpeech(this, estado -> {
            if (estado != TextToSpeech.SUCCESS || voz == null) return;
            int r = voz.setLanguage(new Locale("es", "ES"));
            vozLista = r != TextToSpeech.LANG_MISSING_DATA && r != TextToSpeech.LANG_NOT_SUPPORTED;
        });
        findViewById(R.id.btnEscanear).setOnClickListener(v -> escaner.launch(new ScanOptions()
                .setDesiredBarcodeFormats(ScanOptions.QR_CODE)
                .setPrompt("Apunta al QR del cartel de la actividad")
                .setBeepEnabled(false)
                .setOrientationLocked(false)));

        // Botonera y gesto de arrastrar la carta
        findViewById(R.id.btnPasar).setOnClickListener(v -> decidir(-1));
        findViewById(R.id.btnApuntar).setOnClickListener(v -> decidir(+1));
        findViewById(R.id.btnAnterior).setOnClickListener(v -> anterior());
        findViewById(R.id.btnReiniciar).setOnClickListener(v -> { posicion = 0; mostrarCarta(); });
        carta.setOnTouchListener(new ArrastrarCarta());

        // Sensor: inclinar el móvil hace lo mismo que los botones (+1 derecha = me apunto, -1 izquierda = paso)
        inclinar = new InclinarParaPasar(carta, this::alInclinar, () -> posicion < mazo.size());

        crearPestanas();
        cargarActividades();
    }

    @Override
    protected void onResume() {
        super.onResume(); // BaseActivity: activa el sensor de agitar
        // Por si ha iniciado o cerrado sesión mientras tanto
        String correo = correoSesion();
        if (correo == null ? correoUsuario != null : !correo.equals(correoUsuario)) {
            correoUsuario = correo;
            cargarActividades();
        }
    }

    /** Correo del usuario con sesión iniciada, o null si no hay sesión. */
    private String correoSesion() {
        String correo = getSharedPreferences("SesionUsuario", MODE_PRIVATE).getString("CORREO", null);
        return correo == null || correo.isEmpty() ? null : correo;
    }

    // -------------------------------------------------
    //              CARGA Y FILTROS
    // -------------------------------------------------

    private void cargarActividades() {
        todas = actividadesDB.listarActividadesConDetalles(correoUsuario, FechasUtil.hoyBD());
        aplicarFiltro();
    }

    /**
     * Pestañas de papel (ui/PestanasPapel, de Izaro) en dos filas, todas a la vista:
     * Todas · Mis actividades · Charlas · Bibliomaker / AbiertaUGR · Día ETSIIT · Otras
     */
    private void crearPestanas() {
        pestanasTipos.nuevaFila();
        pestanasTipos.anadir(FILTRO_TODAS, "Todas", null, R.drawable.ic_nui_actividades, 0);
        pestanasTipos.anadir(FILTRO_APUNTADO, "Mis actividades", null, R.drawable.nui_ic_cita, R.color.nui_arrayan);
        for (int i = 0; i < Actividad.TIPOS.length; i++) {
            if (i == 2) pestanasTipos.nuevaFila(); // 4 arriba y 3 abajo
            String tipo = Actividad.TIPOS[i][0];
            pestanasTipos.anadir(tipo, Actividad.TIPOS[i][1], null,
                    Actividad.iconoRes(tipo), Actividad.colorRes(tipo));
        }
        pestanasTipos.completarFila(4);
        pestanasTipos.marcar(FILTRO_TODAS);
        pestanasTipos.setOnElegir(clave -> {
            filtroSeleccionado = FILTRO_TODAS.equals(clave) ? null : (String) clave;
            aplicarFiltro();
        });
    }

    // Prepara el mazo con las actividades del chip seleccionado y vuelve a la primera
    private void aplicarFiltro() {
        mazo = new ArrayList<>();
        for (Actividad a : todas) {
            if (filtroSeleccionado == null
                    || (FILTRO_APUNTADO.equals(filtroSeleccionado) && a.inscrito)
                    || filtroSeleccionado.equals(a.tipo)) {
                mazo.add(a);
            }
        }
        posicion = 0;
        mostrarCarta();
    }

    // -------------------------------------------------
    //              LA CARTA
    // -------------------------------------------------

    /** Pinta la carta de la posición actual, o el mensaje de "no hay" / "has visto todas". */
    private void mostrarCarta() {
        carta.setTranslationX(0);
        carta.setRotation(0);
        selloSi.setAlpha(0);
        selloNo.setAlpha(0);

        boolean hayCarta = posicion < mazo.size();
        carta.setVisibility(hayCarta ? View.VISIBLE : View.INVISIBLE);
        vacio.setVisibility(hayCarta ? View.GONE : View.VISIBLE);
        findViewById(R.id.btnPasar).setEnabled(hayCarta);
        findViewById(R.id.btnApuntar).setEnabled(hayCarta);
        findViewById(R.id.btnAnterior).setEnabled(posicion > 0);
        findViewById(R.id.btnAnterior).setAlpha(posicion > 0 ? 1f : 0.4f);

        if (!hayCarta) {
            if (inclinar != null) inclinar.actualizar();
            boolean visteTodas = !mazo.isEmpty();
            findViewById(R.id.btnReiniciar).setVisibility(visteTodas ? View.VISIBLE : View.GONE);
            tvContador.setText("");
            if (visteTodas) {
                tvVacio.setText("¡Has visto todas!");
            } else if (!FILTRO_APUNTADO.equals(filtroSeleccionado)) {
                tvVacio.setText("No hay actividades de este tipo.");
            } else if (correoUsuario == null) {
                tvVacio.setText("Inicia sesión para ver las actividades a las que te apuntes.");
            } else {
                tvVacio.setText("Todavía no te has apuntado a nada.");
            }
            return;
        }

        Actividad a = mazo.get(posicion);
        // Un solo color fuerte por ficha: el del tipo, en la tesela del icono
        pintarTesela(findViewById(R.id.ivIconoCarta), a.tipo);
        ((TextView) findViewById(R.id.tvTipoCarta)).setText(Actividad.nombreTipo(a.tipo));
        ((TextView) findViewById(R.id.tvCuandoCarta)).setText(
                FechasUtil.cuando(a.inicio) + " · " + FechasUtil.hora(a.inicio));
        ((TextView) findViewById(R.id.tvTituloCarta)).setText(a.titulo);
        ((TextView) findViewById(R.id.tvDescCarta)).setText(a.descripcion);

        TextView tvExtra = findViewById(R.id.tvExtraCarta);
        if (a.esAbiertaUGR()) {
            tvExtra.setVisibility(View.VISIBLE);
            tvExtra.setText(a.creditosTexto() + " · " + a.estadoInscripcion());
            tvExtra.setTextColor(getColor(a.inscripcionUrgente() ? R.color.nui_granada : R.color.nui_cobalto));
        } else {
            tvExtra.setVisibility(View.GONE);
        }
        findViewById(R.id.tvInscritoCarta).setVisibility(a.inscrito ? View.VISIBLE : View.GONE);
        tvContador.setText((posicion + 1) + " / " + mazo.size());
        carta.setContentDescription(Actividad.nombreTipo(a.tipo) + ". " + a.titulo + ". "
                + FechasUtil.cuando(a.inicio) + (a.inscrito ? ". Ya estás apuntado" : ""));
        if (inclinar != null) inclinar.actualizar();
    }

    /** Tesela del icono del tipo: icono en su color fuerte sobre su tinte. */
    private void pintarTesela(ImageView iv, String tipo) {
        iv.setImageResource(Actividad.iconoRes(tipo));
        iv.setImageTintList(ColorStateList.valueOf(getColor(Actividad.colorRes(tipo))));
        iv.setBackgroundTintList(ColorStateList.valueOf(getColor(Actividad.tinteRes(tipo))));
    }

    /**
     * Botones Paso / Me apunto y final del gesto. direccion: +1 = me apunto, -1 = paso.
     * Si no se puede apuntar (sin sesión, plazo cerrado…) la carta vuelve a su sitio con un aviso.
     */
    private void decidir(int direccion) {
        if (posicion >= mazo.size()) return;
        Actividad a = mazo.get(posicion);
        if (direccion > 0 && !puedeApuntarse(a)) {
            volverAlCentro();
            return;
        }
        // La carta sale volando hacia ese lado y aparece la siguiente
        float ancho = raiz.getWidth();
        carta.animate().translationX(direccion * ancho * 1.5f).rotation(direccion * 25f)
                .setDuration(220)
                .withEndAction(() -> {
                    if (direccion > 0) apuntar(a);
                    posicion++;
                    mostrarCarta();
                    // pequeña entrada de la nueva carta
                    carta.setScaleX(0.92f);
                    carta.setScaleY(0.92f);
                    carta.animate().scaleX(1f).scaleY(1f).setDuration(150).start();
                })
                .start();
    }

    /** Inclinar el móvil: enseña el sello un momento y decide, como si se hubiera arrastrado la carta. */
    private void alInclinar(int direccion) {
        if (posicion >= mazo.size()) return;
        TextView sello = direccion > 0 ? selloSi : selloNo;
        sello.setAlpha(1f);
        carta.animate().rotation(direccion * 8f).setDuration(120)
                .withEndAction(() -> decidir(direccion)).start();
    }

    private void anterior() {
        if (posicion == 0) return;
        posicion--;
        mostrarCarta();
        carta.setTranslationX(-raiz.getWidth());
        carta.animate().translationX(0).setDuration(220).start();
    }

    private void volverAlCentro() {
        carta.animate().translationX(0).rotation(0).setDuration(200).start();
        selloSi.animate().alpha(0).setDuration(200).start();
        selloNo.animate().alpha(0).setDuration(200).start();
    }

    /**
     * Arrastrar la carta con el dedo: se mueve e inclina, y se van viendo los sellos ME APUNTO / PASO.
     * Si se suelta pasado un tercio de la pantalla, cuenta como decisión; si apenas se ha movido, es un toque.
     */
    private class ArrastrarCarta implements View.OnTouchListener {
        private float xInicial;
        private final int umbralToque = ViewConfiguration.get(Actividades.this).getScaledTouchSlop();

        @SuppressLint("ClickableViewAccessibility") // el toque se gestiona aquí (abre el detalle)
        @Override
        public boolean onTouch(View v, MotionEvent e) {
            switch (e.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    xInicial = e.getRawX();
                    return true;
                case MotionEvent.ACTION_MOVE: {
                    float dX = e.getRawX() - xInicial;
                    float ancho = raiz.getWidth();
                    carta.setTranslationX(dX);
                    carta.setRotation(15f * dX / ancho);
                    selloSi.setAlpha(Math.max(0f, Math.min(1f, dX / (ancho * 0.3f))));
                    selloNo.setAlpha(Math.max(0f, Math.min(1f, -dX / (ancho * 0.3f))));
                    return true;
                }
                case MotionEvent.ACTION_UP: {
                    float dX = e.getRawX() - xInicial;
                    if (Math.abs(dX) < umbralToque) {
                        volverAlCentro();
                        if (posicion < mazo.size()) mostrarDetalle(mazo.get(posicion));
                    } else if (Math.abs(dX) > raiz.getWidth() * 0.3f) {
                        avisarTutorial(dX > 0 ? Gesto.DESLIZAR_DER : Gesto.DESLIZAR_IZQ);
                        decidir(dX > 0 ? +1 : -1);
                    } else {
                        volverAlCentro();
                    }
                    return true;
                }
                case MotionEvent.ACTION_CANCEL:
                    volverAlCentro();
                    return true;
                default:
                    return false;
            }
        }
    }

    @Override
    protected void onDestroy() {
        if (voz != null) voz.shutdown();
        voz = null;
        super.onDestroy();
    }

    // -------------------------------------------------
    //              ÓRDENES POR VOZ
    // -------------------------------------------------

    private void escucharOrden() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
                .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                .putExtra(RecognizerIntent.EXTRA_LANGUAGE, "es-ES")
                .putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                .putExtra(RecognizerIntent.EXTRA_PROMPT, "Di «paso», «me apunto», «llévame», «¿cuándo es?»...");
        try {
            reconocedor.launch(intent);
        } catch (ActivityNotFoundException e) {
            avisar("Este móvil no tiene reconocimiento de voz (hace falta la app de Google)");
        }
    }

    /** Prueba las frases que ha entendido el reconocedor, de la más probable a la menos. */
    private void alOir(List<String> frases) {
        for (String frase : frases) {
            if (interpretar(normalizar(frase))) return;
        }
        String oido = frases.get(0);
        avisar("No te he entendido («" + oido + "»). Prueba con «paso», «me apunto» o «llévame»");
        decir("No te he entendido");
    }

    /** Ejecuta la orden si la reconoce. La frase llega sin tildes y en minúsculas. */
    private boolean interpretar(String f) {
        Actividad a = posicion < mazo.size() ? mazo.get(posicion) : null;

        // Antes los filtros y lo que no depende de la ficha
        if (contiene(" " + f, "escanea", "cartel", "codigo qr", " qr")) {
            findViewById(R.id.btnEscanear).performClick();
            return true;
        }
        if (contiene(f, "grupo", "comunidad", "chat")) {
            HojaComunidades.mostrar(this, correoUsuario);
            return true;
        }
        if (contiene(f, "mis actividades", "apuntado", "las mias")) return elegirFiltro(FILTRO_APUNTADO, "Tus actividades");
        if (contiene(f, "charla")) return elegirFiltro("charla", "Charlas");
        if (contiene(f, "bibliomaker", "biblio maker", "taller")) return elegirFiltro("bibliomaker", "Bibliomaker");
        if (contiene(f, "abierta", "curso", "credito")) return elegirFiltro("abiertaugr", "Cursos de AbiertaUGR");
        if (contiene(f, "dia de la", "fiesta")) return elegirFiltro("dia_etsiit", "Día de la ETSIIT");
        if (contiene(f, "otras")) return elegirFiltro("otro", "Otras actividades");
        if (contiene(f, "todas")) return elegirFiltro(FILTRO_TODAS, "Todas las actividades");
        if (contiene(f, "anterior", "atras", "vuelve", "volver")) {
            if (posicion == 0) decir("Ya estás en la primera");
            anterior();
            return true;
        }

        if (a == null) {
            if (contiene(f, "paso", "siguiente", "apunta", "llevame", "cuando", "lee")) {
                decir("No hay ninguna actividad delante");
                return true;
            }
            return false;
        }
        // "no me apunto" va antes que "me apunto"
        if (contiene(f, "no me apunto", "paso", "pasa", "siguiente", "no me interesa", "descarta")) {
            decidir(-1);
            return true;
        }
        if (contiene(f, "desapunta", "borrame", "ya no puedo")) {
            if (a.inscrito) desapuntar(a);
            else decir("No estabas apuntado");
            return true;
        }
        if (contiene(f, "apunta", "apunto", "me interesa", "inscribe") || f.equals("si")) {
            decidir(+1);
            return true;
        }
        if (contiene(f, "llevame", "como llego", "donde es", "ruta", "mapa")) {
            decir(a.sitioEnMapa() != null ? "Te llevo a " + a.sitioEnMapa() : "Te abro el mapa");
            llevarme(a);
            return true;
        }
        if (contiene(f, "cuando", "hora", "que dia", "donde")) {
            String texto = a.titulo + ": " + textoFechas(a).replace("\n", ", ") + ", en " + lugar(a);
            decir(texto);
            avisar(texto);
            return true;
        }
        if (contiene(f, "lee", "de que va", "cuentame", "que es")) {
            decir(a.titulo + ". " + a.descripcion);
            return true;
        }
        if (contiene(f, "calendario", "agenda")) {
            anadirACalendario(a);
            return true;
        }
        if (contiene(f, "detalle", "informacion", "mas info", "abre")) {
            mostrarDetalle(a);
            return true;
        }
        return false;
    }

    private boolean elegirFiltro(String clave, String nombre) {
        pestanasTipos.marcar(clave);
        filtroSeleccionado = FILTRO_TODAS.equals(clave) ? null : clave;
        aplicarFiltro();
        decir(nombre + (mazo.isEmpty() ? ": no hay ninguna" : ": " + mazo.size()));
        return true;
    }

    private void decir(String texto) {
        if (vozLista && voz != null) voz.speak(texto, TextToSpeech.QUEUE_FLUSH, null, "orden");
    }

    private static boolean contiene(String frase, String... palabras) {
        for (String p : palabras) if (frase.contains(p)) return true;
        return false;
    }

    private static String normalizar(String texto) {
        return java.text.Normalizer.normalize(texto, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT).trim();
    }

    // -------------------------------------------------
    //              QR DE LOS CARTELES
    // -------------------------------------------------

    /** Busca la actividad del QR, la pone encima del mazo y abre su detalle para apuntarse. */
    private void alEscanear(String contenido) {
        String clave = contenido.trim();
        if (!clave.toUpperCase(java.util.Locale.ROOT).startsWith(PREFIJO_QR)) {
            avisar("Ese QR no es de ninguna actividad de la ETSIIT");
            return;
        }
        clave = clave.substring(PREFIJO_QR.length()).trim();
        Actividad encontrada = null;
        for (Actividad a : todas) {
            if (claveQR(a.titulo).equals(claveQR(clave)) || String.valueOf(a.id).equals(clave)) encontrada = a;
        }
        if (encontrada == null) {
            avisar("Esa actividad ya ha pasado o no está en la app");
            return;
        }
        avisarTutorial(Gesto.ESCANEAR);
        filtroSeleccionado = null;
        pestanasTipos.marcar(FILTRO_TODAS);
        aplicarFiltro();
        posicion = Math.max(0, mazo.indexOf(encontrada));
        mostrarCarta();
        mostrarDetalle(encontrada);
    }

    /** "Día de la ETSIIT" → "dia-de-la-etsiit": sin tildes ni símbolos, para que el QR solo lleve ASCII. */
    static String claveQR(String texto) {
        String sinTildes = java.text.Normalizer.normalize(texto, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return sinTildes.toLowerCase(java.util.Locale.ROOT).replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
    }

    // -------------------------------------------------
    //              APUNTARSE / DESAPUNTARSE
    // -------------------------------------------------

    /** Comprueba si se puede apuntar; si no, enseña el motivo. */
    private boolean puedeApuntarse(Actividad a) {
        if (correoUsuario == null) {
            pedirSesion("apuntarte");
            return false;
        }
        if (a.inscrito) return true; // ya lo estaba: simplemente pasa a la siguiente
        if (!a.plazoAbierto()) {
            avisar("El plazo de inscripción no está abierto: " + a.estadoInscripcion());
            return false;
        }
        if (a.completa()) {
            avisar("No quedan plazas");
            return false;
        }
        return true;
    }

    private void apuntar(Actividad a) {
        if (a.inscrito) {
            avisar("Ya estabas apuntado a «" + a.titulo + "»");
            return;
        }
        if (!actividadesDB.insertarInscripcion(correoUsuario, a.id, FechasUtil.ahoraBD())) {
            avisar("No se ha podido completar la inscripción");
            return;
        }
        a.inscrito = true;
        a.inscritos++;
        RecordatorioActividad.programar(this, a); // aviso con "Llévame" media hora antes
        mostrarCarta();
        // Nota de papel con mecha (AvisoDeshacer, de Izaro). Lo de añadir al calendario está en el detalle.
        AvisoDeshacer.mostrar(botonera, "Apuntado", "Te has apuntado a «" + a.titulo + "»", MS_DESHACER, () -> {
            actividadesDB.borrarInscripcion(correoUsuario, a.id);
            RecordatorioActividad.cancelar(this, a.id);
            a.inscrito = false;
            a.inscritos--;
            mostrarCarta();
        });
    }

    private void desapuntar(Actividad a) {
        actividadesDB.borrarInscripcion(correoUsuario, a.id);
        RecordatorioActividad.cancelar(this, a.id);
        a.inscrito = false;
        a.inscritos--;
        mostrarCarta();
        AvisoDeshacer.mostrar(botonera, "Desapuntado", "Ya no estás apuntado a «" + a.titulo + "»", MS_DESHACER, () -> {
            actividadesDB.insertarInscripcion(correoUsuario, a.id, FechasUtil.ahoraBD());
            a.inscrito = true;
            a.inscritos++;
            RecordatorioActividad.programar(this, a);
            mostrarCarta();
        });
    }

    private void avisar(String texto) {
        Snackbar.make(raiz, texto, Snackbar.LENGTH_SHORT).setAnchorView(botonera).show();
    }

    /** Aviso de que hace falta iniciar sesión, con botón para ir al login. */
    private void pedirSesion(String accion) {
        Snackbar.make(raiz, "Inicia sesión para " + accion, Snackbar.LENGTH_LONG)
                .setAnchorView(botonera)
                .setAction("Iniciar sesión", v -> startActivity(new Intent(this, Login.class)))
                .show();
    }

    // -------------------------------------------------
    //              DETALLE (hoja inferior)
    // -------------------------------------------------

    private void mostrarDetalle(Actividad a) {
        BottomSheetDialog hoja = new BottomSheetDialog(this);
        View v = getLayoutInflater().inflate(R.layout.detalle_actividad, null);
        hoja.setContentView(v);

        pintarTesela(v.findViewById(R.id.ivIconoDetalle), a.tipo);
        ((TextView) v.findViewById(R.id.tvTipoDetalle)).setText(Actividad.nombreTipo(a.tipo));

        ((TextView) v.findViewById(R.id.tvTituloDetalle)).setText(a.titulo);
        ((TextView) v.findViewById(R.id.tvFechaDetalle)).setText(textoFechas(a));
        ((TextView) v.findViewById(R.id.tvLugarDetalle)).setText(lugar(a));
        ((TextView) v.findViewById(R.id.tvDescripcionDetalle)).setText(a.descripcion);

        TextView tvAbierta = v.findViewById(R.id.tvAbiertaDetalle);
        if (a.esAbiertaUGR()) {
            tvAbierta.setVisibility(View.VISIBLE);
            tvAbierta.setText(a.creditosTexto() + "\n" + a.estadoInscripcion());
            if (a.inscripcionUrgente()) {
                tvAbierta.setTextColor(getColor(R.color.nui_granada));
                tvAbierta.setCompoundDrawableTintList(ColorStateList.valueOf(getColor(R.color.nui_granada)));
            }
        }

        String plazas = a.plazasTotales != null
                ? a.inscritos + " de " + a.plazasTotales + " plazas ocupadas"
                : a.inscritos + (a.inscritos == 1 ? " persona apuntada" : " personas apuntadas");
        ((TextView) v.findViewById(R.id.tvPlazasDetalle)).setText(plazas);

        // Botón principal según el estado (si ya estás apuntado, sale el secundario para desapuntarte)
        MaterialButton btnApuntarme = v.findViewById(R.id.btnApuntarme);
        if (a.inscrito) {
            btnApuntarme.setVisibility(View.GONE);
            View btnDesapuntarme = v.findViewById(R.id.btnDesapuntarme);
            btnDesapuntarme.setVisibility(View.VISIBLE);
            btnDesapuntarme.setOnClickListener(x -> { hoja.dismiss(); desapuntar(a); });
        } else if (!a.plazoAbierto() || a.completa()) {
            btnApuntarme.setText(a.completa() ? "Plazas agotadas" : a.estadoInscripcion());
            btnApuntarme.setEnabled(false);
        } else if (correoUsuario == null) {
            btnApuntarme.setText("Inicia sesión para apuntarte");
            btnApuntarme.setOnClickListener(x -> {
                hoja.dismiss();
                startActivity(new Intent(this, Login.class));
            });
        } else {
            btnApuntarme.setText("Apuntarme");
            btnApuntarme.setOnClickListener(x -> { hoja.dismiss(); apuntar(a); });
        }

        v.findViewById(R.id.btnCalendario).setOnClickListener(x -> anadirACalendario(a));
        v.findViewById(R.id.btnLlevame).setOnClickListener(x -> { hoja.dismiss(); llevarme(a); });

        View btnMasInfo = v.findViewById(R.id.btnMasInfo);
        if (a.enlace != null && !a.enlace.isEmpty()) {
            btnMasInfo.setOnClickListener(x -> abrirEnlace(a.enlace));
        } else {
            btnMasInfo.setVisibility(View.GONE);
        }

        hoja.show();
    }

    // "Jueves 22 de octubre · 12:00 – 13:30"
    private String textoFechas(Actividad a) {
        String texto = FechasUtil.capitalizar(FechasUtil.diaLargo(a.inicio)) + " · " + FechasUtil.hora(a.inicio);
        if (a.fin != null) {
            if (a.fin.toLocalDate().equals(a.inicio.toLocalDate())) {
                texto += " – " + FechasUtil.hora(a.fin);
            } else {
                texto += "\nhasta el " + FechasUtil.diaLargo(a.fin);
            }
        }
        return texto;
    }

    private String lugar(Actividad a) {
        String sitio = a.sitioEnMapa();
        return sitio != null ? sitio + " · ETSIIT" : "ETSIIT · Universidad de Granada";
    }

    /**
     * "Llévame": abre el mapa con la ruta hasta el sitio de la actividad (igual que "Cómo llegar" de Biblioteca).
     * Si es en toda la escuela, el mapa pregunta "¿A dónde vas?".
     */
    private void llevarme(Actividad a) {
        Intent intent = new Intent(this, Mapa.class).putExtra("CORREO", correoUsuario);
        String sitio = a.sitioEnMapa();
        if (sitio != null) intent.putExtra("RUTA_A_MARCADOR", sitio);
        startActivity(intent);
    }

    // -------------------------------------------------
    //              CALENDARIO Y ENLACES
    // -------------------------------------------------

    /** Abre la app de calendario con el evento ya relleno (CalendarioUtil, de Izaro). */
    private void anadirACalendario(Actividad a) {
        // Si dura varios días (cursos) se añade solo la primera sesión
        boolean mismoDia = a.fin != null && a.fin.toLocalDate().equals(a.inicio.toLocalDate());
        boolean ok = CalendarioUtil.anadirEvento(this, a.titulo, a.descripcion, lugar(a),
                a.inicio, mismoDia ? a.fin : a.inicio.plusHours(2));
        if (!ok) avisar("No hay ninguna app de calendario en este móvil");
    }

    private void abrirEnlace(String url) {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        } catch (ActivityNotFoundException e) {
            avisar("No hay navegador para abrir el enlace");
        }
    }

    // -------------------------------------------------
    //              TUTORIAL (util/tutorial, de Izaro)
    // -------------------------------------------------

    /** Lo enseña BaseActivity la primera vez; el botón "?" de la barra lo repite. */
    @Override
    protected Tutorial crearTutorial() {
        return new Tutorial(this, "actividades", 3)
                .info(R.id.pestanasTipos, "Elige qué actividades ver: charlas, Bibliomaker, AbiertaUGR... "
                        + "En «Mis actividades» están a las que te has apuntado")
                .gesto(R.id.carta, Gesto.DESLIZAR_IZQ, "Desliza la ficha a la izquierda para pasar a la siguiente. "
                        + "A la derecha te apuntas")
                .soloSi(() -> posicion < mazo.size() - 1)
                .sensor(Gesto.INCLINAR, "También puedes inclinar el móvil: a la izquierda pasas, "
                        + "a la derecha te apuntas")
                .soloSi(() -> posicion < mazo.size() - 1 && inclinar != null && inclinar.escuchando())
                .info(R.id.btnAnterior, "Si te has pasado una, vuelve atrás con la flecha")
                .info(R.id.btnVoz, "También me puedes hablar: toca el micrófono y di «paso», «me apunto», "
                        + "«¿cuándo es?», «llévame» o «charlas»")
                .info(R.id.btnEscanear, "¿Ves el cartel de una actividad por la escuela? Escanea su QR "
                        + "con la cámara y te la abro aquí para apuntarte")
                .info(R.id.btnComunidades, "Aquí están los grupos de chat de tu grado y de la escuela")
                // el último: al tocar se abre el detalle encima
                .tocar(R.id.carta, "Toca la ficha para ver los detalles, añadirla al calendario "
                        + "o que el mapa te lleve")
                .soloSi(() -> posicion < mazo.size());
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.nui_quieto, R.anim.nui_papel_sale);
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
