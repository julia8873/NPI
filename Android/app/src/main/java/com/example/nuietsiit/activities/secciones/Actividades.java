package com.example.nuietsiit.activities.secciones;

import android.annotation.SuppressLint;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.net.Uri;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.nuietsiit.R;
import com.example.nuietsiit.activities.BaseActivity;
import com.example.nuietsiit.activities.auth.Login;
import com.example.nuietsiit.activities.secciones.actividades.CapturaVertical;
import com.example.nuietsiit.activities.secciones.actividades.HojaComunidades;
import com.example.nuietsiit.activities.secciones.actividades.QrParaEnsenar;
import com.example.nuietsiit.activities.secciones.actividades.TirarHaciaTi;
import com.example.nuietsiit.activities.secciones.mapa.Mapa;
import com.example.nuietsiit.data.ActividadesDB;
import com.example.nuietsiit.data.ActividadesGuardadas;
import com.example.nuietsiit.data.DBHelper;
import com.example.nuietsiit.data.UsuariosDB;
import com.example.nuietsiit.data.modelo.Actividad;
import com.example.nuietsiit.ui.InclinarParaPasar;
import com.example.nuietsiit.util.CalendarioUtil;
import com.example.nuietsiit.util.FechasUtil;
import com.example.nuietsiit.util.RecordatorioActividad;
import com.example.nuietsiit.util.tutorial.Gesto;
import com.example.nuietsiit.util.tutorial.Tutorial;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.snackbar.Snackbar;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Pantalla de Actividades SIN SCROLL: un "mazo" con una ficha grande cada vez.
 * Mismo aspecto que Asignaturas, Profesorado y Calendario: cabecera unificada, fondo gris claro, tarjetas
 * blancas y el naranja de la app (colores_secciones.xml y estilos_secciones.xml).
 * - Deslizar a la derecha (o "Me apunto") = me apunto y pasa a la siguiente.
 * - Deslizar a la izquierda (o "Paso") = paso, siguiente.
 * - SENSOR (acelerómetro): inclinar el móvil a la derecha = me apunto, a la izquierda = paso.
 *   Usa InclinarParaPasar, el mismo gesto que pasar páginas en Secretaría y Biblioteca.
 * - SENSOR (acelerómetro, TirarHaciaTi): inclinarlo hacia ti = guardar para luego, sin apuntarse (pestaña
 *   «Guardadas», ActividadesGuardadas); si ya estaba guardada, la quita.
 * - Deslizar la ficha hacia arriba = darle la vuelta: por detrás, un QR grande (QrParaEnsenar) para que un amigo la
 *   escanee con su app y se apunte. También con el botón «Enseñar QR» del detalle.
 * - Flecha = volver a la anterior. Tocar la ficha = detalle completo (apuntarse, guardar, QR, calendario, llévame).
 * - Durante el tutorial los gestos se practican sin apuntarse ni guardar nada de verdad.
 * - Sin sesión iniciada se pueden ver, pero no apuntarse.
 * - CÁMARA: "Escanear" lee el QR de un cartel de actividad (zxing, como el escáner de Biblioteca)
 *   y abre esa actividad para apuntarse. El QR lleva "NUIETSIIT:ACTIVIDAD:" + el título sin tildes en
 *   minúsculas y con guiones (p. ej. "...:arduino-desde-cero"), o el id.
 * - Tutorial interactivo la primera vez (crearTutorial), como los de Izaro.
 */
public class Actividades extends BaseActivity {

    private static final String FILTRO_TODAS = "__todas";       // pestaña "Todas"
    private static final String FILTRO_APUNTADO = "__apuntado"; // pestaña "Mis actividades"
    private static final String FILTRO_GUARDADAS = "__guardadas"; // pestaña "Guardadas" (inclinar hacia ti)
    private static final int MS_DESHACER = 5000;
    /** ECTS que se pueden reconocer por actividades universitarias en un grado (tope del contador). */
    private static final double CREDITOS_RECONOCIBLES = 6;
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
    private ChipGroup pestanasTipos;
    private TextView tvVacio, tvContador, selloSi, selloNo;
    private InclinarParaPasar inclinar;
    private TirarHaciaTi haciaTi;          // inclinar hacia ti = guardar para luego
    private QrParaEnsenar qrEnsenar;
    private Set<String> guardadas = new HashSet<>();

    /** La carta está saliendo volando: hasta que acabe no se acepta otra decisión (doble toque, botón + inclinar…). */
    private boolean decidiendo;

    // Escáner de QR (pantalla de zxing; pide ella el permiso de cámara)
    private final ActivityResultLauncher<ScanOptions> escaner = registerForActivityResult(new ScanContract(),
            resultado -> { if (resultado.getContents() != null) alEscanear(resultado.getContents()); });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.actividades);
        // Cabecera unificada (como Asignaturas y Profesorado): volver, título, chat y perfil
        configurarCabeceraUnificada(getString(R.string.titulo_actividades));
        margenBarraEstado(findViewById(R.id.rootActividades));

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
        findViewById(R.id.btnEscanear).setOnClickListener(v -> escaner.launch(new ScanOptions()
                .setDesiredBarcodeFormats(ScanOptions.QR_CODE)
                .setPrompt(getString(R.string.nui_act_escaner_prompt))
                .setBeepEnabled(false)
                .setOrientationLocked(true)
                .setCaptureActivity(CapturaVertical.class)));

        // Botonera y gesto de arrastrar la carta
        findViewById(R.id.btnPasar).setOnClickListener(v -> decidir(-1));
        findViewById(R.id.btnApuntar).setOnClickListener(v -> decidir(+1));
        findViewById(R.id.btnGuardarCarta).setOnClickListener(v -> {
            if (!decidiendo && posicion < mazo.size()) alternarGuardada();
        });
        findViewById(R.id.btnAnterior).setOnClickListener(v -> anterior());
        findViewById(R.id.btnReiniciar).setOnClickListener(v -> { posicion = 0; mostrarCarta(); });
        carta.setOnTouchListener(new ArrastrarCarta());
        // Con TalkBack no hay arrastre: el doble toque abre el detalle (los toques normales los gestiona ArrastrarCarta)
        carta.setOnClickListener(v -> { if (posicion < mazo.size()) mostrarDetalle(mazo.get(posicion)); });

        // Sensor: inclinar el móvil hace lo mismo que los botones (+1 derecha = me apunto, -1 izquierda = paso)
        inclinar = new InclinarParaPasar(carta, this::alInclinar, () -> posicion < mazo.size());
        // Sensor: inclinarlo hacia ti = guardar para luego (acelerómetro)
        haciaTi = new TirarHaciaTi(this, this::alTirarHaciaTi);
        // Deslizar la ficha hacia arriba = darle la vuelta: por detrás, el QR para pasársela a un amigo
        qrEnsenar = new QrParaEnsenar(this);

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
        haciaTi.iniciar();
    }

    @Override
    protected void onPause() {
        super.onPause();
        haciaTi.parar();
        qrEnsenar.quitar();
    }

    /** Correo del usuario con sesión iniciada, o null si no hay sesión. */
    private String correoSesion() {
        String correo = getSharedPreferences("SesionUsuario", MODE_PRIVATE).getString("CORREO", null);
        if (correo == null || correo.isEmpty()) return null;
        // Si la cuenta ya no está en la BD (al subir de versión la BD se vuelve a crear y se borran las cuentas
        // registradas), se trata como sin sesión: así pide iniciar sesión en vez de fallar al apuntarse
        return new UsuariosDB(DBHelper.getInstance(this)).existeEstudiante(correo) ? correo : null;
    }

    // -------------------------------------------------
    //              CARGA Y FILTROS
    // -------------------------------------------------

    private void cargarActividades() {
        todas = actividadesDB.listarActividadesConDetalles(correoUsuario, FechasUtil.hoyBD());
        guardadas = ActividadesGuardadas.guardadas(this, correoUsuario);
        aplicarFiltro();
    }

    /** Chips de filtro (bajan de línea si no caben): Todas · Mis actividades · Charlas · Bibliomaker · AbiertaUGR · Día ETSIIT · Otras */
    private void crearPestanas() {
        anadirChip(FILTRO_TODAS, getString(R.string.chip_todas));
        anadirChip(FILTRO_APUNTADO, getString(R.string.nui_act_f_mis));
        anadirChip(FILTRO_GUARDADAS, getString(R.string.nui_act_f_guardadas));
        for (String[] t : Actividad.TIPOS) anadirChip(t[0], Actividad.nombreFiltro(this, t[0]));
        marcarFiltro(FILTRO_TODAS);
        pestanasTipos.setOnCheckedStateChangeListener((grupo, ids) -> {
            if (ids.isEmpty()) return;
            String clave = (String) grupo.findViewById(ids.get(0)).getTag();
            filtroSeleccionado = FILTRO_TODAS.equals(clave) ? null : clave;
            aplicarFiltro();
        });
    }

    private void anadirChip(String clave, String texto) {
        Chip chip = (Chip) getLayoutInflater().inflate(R.layout.item_chip_filtro, pestanasTipos, false);
        chip.setId(View.generateViewId());
        chip.setText(texto);
        chip.setTag(clave);
        pestanasTipos.addView(chip);
    }

    private void marcarFiltro(String clave) {
        for (int i = 0; i < pestanasTipos.getChildCount(); i++) {
            View chip = pestanasTipos.getChildAt(i);
            if (clave.equals(chip.getTag())) pestanasTipos.check(chip.getId());
        }
    }

    // Prepara el mazo con las actividades del chip seleccionado y vuelve a la primera
    private void aplicarFiltro() {
        mazo = new ArrayList<>();
        for (Actividad a : todas) {
            if (filtroSeleccionado == null
                    || (FILTRO_APUNTADO.equals(filtroSeleccionado) && a.inscrito)
                    || (FILTRO_GUARDADAS.equals(filtroSeleccionado) && guardadas.contains(String.valueOf(a.id)))
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
        actualizarCreditos();
        carta.setTranslationX(0);
        carta.setTranslationY(0);
        carta.setRotationX(0);
        carta.setAlpha(1f);
        carta.setRotation(0);
        selloSi.setAlpha(0);
        selloNo.setAlpha(0);

        boolean hayCarta = posicion < mazo.size();
        carta.setVisibility(hayCarta ? View.VISIBLE : View.INVISIBLE);
        vacio.setVisibility(hayCarta ? View.GONE : View.VISIBLE);
        findViewById(R.id.btnPasar).setEnabled(hayCarta);
        findViewById(R.id.btnApuntar).setEnabled(hayCarta);
        findViewById(R.id.btnGuardarCarta).setEnabled(hayCarta);
        findViewById(R.id.btnAnterior).setEnabled(posicion > 0);
        findViewById(R.id.btnAnterior).setAlpha(posicion > 0 ? 1f : 0.4f);

        if (!hayCarta) {
            if (inclinar != null) inclinar.actualizar();
            boolean visteTodas = !mazo.isEmpty();
            findViewById(R.id.btnReiniciar).setVisibility(visteTodas ? View.VISIBLE : View.GONE);
            tvContador.setText("");
            if (visteTodas) {
                tvVacio.setText(R.string.nui_act_vacio_todas);
            } else if (FILTRO_GUARDADAS.equals(filtroSeleccionado)) {
                tvVacio.setText(R.string.nui_act_vacio_guardadas);
            } else if (!FILTRO_APUNTADO.equals(filtroSeleccionado)) {
                tvVacio.setText(R.string.nui_act_vacio_tipo);
            } else if (correoUsuario == null) {
                tvVacio.setText(R.string.nui_act_vacio_sin_sesion);
            } else {
                tvVacio.setText(R.string.nui_act_vacio_sin_inscripciones);
            }
            return;
        }

        Actividad a = mazo.get(posicion);
        // Icono del tipo en un círculo de su color
        pintarTesela(findViewById(R.id.ivIconoCarta), a.tipo);
        TextView tvTipoCarta = findViewById(R.id.tvTipoCarta);
        tvTipoCarta.setText(Actividad.nombreTipo(this, a.tipo));
        pintarEtiqueta(tvTipoCarta, a.tipo);
        ((TextView) findViewById(R.id.tvCuandoCarta)).setText(
                FechasUtil.cuando(this, a.inicio) + " · " + FechasUtil.hora(a.inicio));
        ((TextView) findViewById(R.id.tvTituloCarta)).setText(a.titulo);
        ((TextView) findViewById(R.id.tvDescCarta)).setText(a.descripcion);

        TextView tvExtra = findViewById(R.id.tvExtraCarta);
        if (a.esAbiertaUGR()) {
            tvExtra.setVisibility(View.VISIBLE);
            tvExtra.setText(a.creditosTexto() + " · " + a.estadoInscripcion(this));
            tvExtra.setTextColor(getColor(a.inscripcionUrgente() ? R.color.sec_error : R.color.sec_info));
        } else {
            tvExtra.setVisibility(View.GONE);
        }
        // Estado bien visible: verde si estás apuntado, naranja si la tienes guardada, gris si no
        // (sin sesión solo se enseña si está guardada)
        boolean guardada = guardadas.contains(String.valueOf(a.id));
        TextView tvInscrito = findViewById(R.id.tvInscritoCarta);
        tvInscrito.setVisibility(correoUsuario != null || guardada ? View.VISIBLE : View.GONE);
        tvInscrito.setText(a.inscrito ? R.string.nui_act_sello_apuntado
                : guardada ? R.string.nui_act_estado_guardada : R.string.nui_act_estado_no);
        tvInscrito.setTextColor(getColor(a.inscrito ? R.color.sec_ok : guardada ? R.color.nui_acento : R.color.sec_texto_suave));
        tvInscrito.setBackgroundTintList(ColorStateList.valueOf(getColor(
                a.inscrito ? R.color.sec_ok_tinte : guardada ? R.color.nui_tinte : R.color.sec_borde)));
        ((TextView) findViewById(R.id.btnApuntar)).setText(a.inscrito ? R.string.nui_act_btn_ya_apuntado : R.string.nui_act_btn_apunto);
        // Botón de guardar: marcapáginas relleno si ya está guardada
        MaterialButton btnGuardarCarta = findViewById(R.id.btnGuardarCarta);
        btnGuardarCarta.setIconResource(guardada ? R.drawable.nui_ic_guardado : R.drawable.nui_ic_guardar);
        btnGuardarCarta.setContentDescription(getString(guardada ? R.string.nui_act_btn_quitar_guardada : R.string.nui_act_btn_guardar));
        tvContador.setText((posicion + 1) + " / " + mazo.size());
        carta.setContentDescription(Actividad.nombreTipo(this, a.tipo) + ". " + a.titulo + ". "
                + FechasUtil.cuando(this, a.inicio) + (a.inscrito ? getString(R.string.nui_act_desc_ya_apuntado) : ""));
        if (inclinar != null) inclinar.actualizar();
    }

    /** Etiqueta del tipo: texto de su color sobre su tinte. */
    private void pintarEtiqueta(TextView tv, String tipo) {
        tv.setTextColor(getColor(Actividad.colorRes(tipo)));
        tv.setBackgroundTintList(ColorStateList.valueOf(getColor(Actividad.tinteRes(tipo))));
    }

    /** Icono del tipo en un círculo: su color sobre su tinte. */
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
        if (decidiendo || posicion >= mazo.size()) return;
        Actividad a = mazo.get(posicion);
        // En el tutorial se practica el gesto, pero no te apunta de verdad: la carta vuelve a su sitio
        if (direccion > 0 && hayTutorialActivo()) {
            volverAlCentro();
            avisar(getString(R.string.nui_act_practica_apuntar));
            return;
        }
        if (direccion > 0 && !puedeApuntarse(a)) {
            volverAlCentro();
            return;
        }
        // La carta sale volando hacia ese lado y aparece la siguiente
        decidiendo = true;
        int esta = posicion;
        float ancho = raiz.getWidth();
        carta.animate().translationX(direccion * ancho * 1.5f).rotation(direccion * 25f)
                .setDuration(220)
                .withEndAction(() -> {
                    decidiendo = false;
                    if (direccion > 0) apuntar(a);
                    if (posicion == esta) posicion++; // si mientras tanto se cambió de filtro, no se salta ninguna
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
        if (decidiendo || posicion >= mazo.size() || qrEnsenar.visible()) return; // con el QR fuera, no se decide
        TextView sello = direccion > 0 ? selloSi : selloNo;
        sello.setAlpha(1f);
        carta.animate().rotation(direccion * 8f).setDuration(120)
                .withEndAction(() -> {
                    if (qrEnsenar.visible()) volverAlCentro();
                    else decidir(direccion);
                }).start();
    }

    /**
     * Inclinar el móvil hacia ti: guarda la actividad para luego (sin apuntarte) y pasa a la siguiente, como
     * quien se guarda un cartel; si ya estaba guardada, la quita. Las guardadas están en la pestaña «Guardadas».
     */
    private void alTirarHaciaTi() {
        if (decidiendo || posicion >= mazo.size() || qrEnsenar.visible() || !hasWindowFocus()) return;
        // En el tutorial se practica el gesto, pero no guarda nada: la carta se mueve y vuelve
        if (hayTutorialActivo()) {
            avisarTutorial(Gesto.HACIA_TI);
            carta.animate().translationY(-carta.getHeight() * 0.15f).setDuration(150)
                    .withEndAction(() -> carta.animate().translationY(0).setDuration(150).start()).start();
            avisar(getString(R.string.nui_act_practica_guardar));
            return;
        }
        avisarTutorial(Gesto.HACIA_TI);
        alternarGuardada();
    }

    /** Guarda la actividad de la carta para luego (y pasa a la siguiente) o, si ya estaba, la quita. */
    private void alternarGuardada() {
        Actividad a = mazo.get(posicion);
        boolean yaEstaba = guardadas.contains(String.valueOf(a.id));
        ponerGuardada(a, !yaEstaba);
        if (yaEstaba) {
            if (FILTRO_GUARDADAS.equals(filtroSeleccionado)) quitarDelMazo(a);
            else mostrarCarta();
            Snackbar.make(raiz, getString(R.string.nui_act_aviso_quitada_fmt, a.titulo), MS_DESHACER)
                    .setAnchorView(botonera)
                    .setActionTextColor(getColor(R.color.nui_acento))
                    .setAction(R.string.deshacer, x -> { ponerGuardada(a, true); aplicarFiltroEn(a); })
                    .show();
            return;
        }
        // La carta sube hacia ti y aparece la siguiente
        decidiendo = true;
        int esta = posicion;
        carta.animate().translationY(-raiz.getHeight() * 0.6f).scaleX(0.6f).scaleY(0.6f).alpha(0f)
                .setDuration(260)
                .withEndAction(() -> {
                    decidiendo = false;
                    carta.setScaleX(1f);
                    carta.setScaleY(1f);
                    if (posicion == esta) posicion++;
                    mostrarCarta();
                })
                .start();
        Snackbar.make(raiz, getString(R.string.nui_act_aviso_guardada_fmt, a.titulo), MS_DESHACER)
                .setAnchorView(botonera)
                .setActionTextColor(getColor(R.color.nui_acento))
                .setAction(R.string.deshacer, x -> { ponerGuardada(a, false); aplicarFiltroEn(a); })
                .show();
    }

    private void ponerGuardada(Actividad a, boolean si) {
        ActividadesGuardadas.poner(this, correoUsuario, a.id, si);
        guardadas = ActividadesGuardadas.guardadas(this, correoUsuario);
    }

    /** Vuelve a hacer el mazo del filtro actual y se pone en esa actividad (para "Deshacer"). */
    private void aplicarFiltroEn(Actividad a) {
        aplicarFiltro();
        int i = mazo.indexOf(a);
        if (i >= 0) {
            posicion = i;
            mostrarCarta();
        }
    }

    /** En «Guardadas», la que se quita desaparece del mazo y se queda en la misma posición. */
    private void quitarDelMazo(Actividad a) {
        int antes = posicion;
        aplicarFiltro();
        posicion = Math.min(antes, mazo.size());
        mostrarCarta();
    }

    /**
     * Deslizar la ficha hacia arriba = DARLE LA VUELTA A LA FICHA (giro en 3D): por detrás está el QR grande de la
     * actividad, con el mismo texto que los carteles, para pasársela a un amigo: lo escanea con su app
     * (Actividades → Escanear) y le sale para apuntarse. Tocando el QR, la ficha se vuelve a dar la vuelta.
     */
    private void voltearFicha() {
        if (posicion >= mazo.size() || decidiendo || qrEnsenar.visible()) {
            volverAlCentro();
            return;
        }
        Actividad a = mazo.get(posicion);
        avisarTutorial(Gesto.LANZAR);
        decidiendo = true;
        selloSi.setAlpha(0);
        selloNo.setAlpha(0);
        carta.setCameraDistance(8000 * getResources().getDisplayMetrics().density);
        carta.animate().translationX(0).translationY(0).rotation(0).rotationX(90f).setDuration(180)
                .withEndAction(() -> {
                    decidiendo = false;
                    mostrarQr(a, true);
                }).start();
    }

    /** volteando: viene de darle la vuelta a la ficha (al cerrar el QR, la ficha vuelve a darse la vuelta). */
    private void mostrarQr(Actividad a, boolean volteando) {
        String original = a.tituloEs != null ? a.tituloEs : a.titulo; // como los carteles, por el título en español
        boolean ok = qrEnsenar.mostrar(PREFIJO_QR + claveQR(original), a.titulo, volteando, volteando ? () -> {
            carta.setRotationX(-90f);
            carta.animate().rotationX(0f).setDuration(180).start();
        } : null);
        if (!ok) carta.animate().rotationX(0f).setDuration(180).start(); // no se pudo hacer el QR: la ficha vuelve
    }

    private void anterior() {
        if (decidiendo || posicion == 0) return;
        posicion--;
        mostrarCarta();
        carta.setTranslationX(-raiz.getWidth());
        carta.animate().translationX(0).setDuration(220).start();
    }

    private void volverAlCentro() {
        carta.animate().translationX(0).translationY(0).rotation(0).setDuration(200).start();
        selloSi.animate().alpha(0).setDuration(200).start();
        selloNo.animate().alpha(0).setDuration(200).start();
    }

    /**
     * Arrastrar la carta con el dedo: se mueve e inclina, y se van viendo los sellos ME APUNTO / PASO.
     * Si se suelta pasado un tercio de la pantalla, cuenta como decisión; si apenas se ha movido, es un toque.
     */
    private class ArrastrarCarta implements View.OnTouchListener {
        private float xInicial, yInicial;
        private final int umbralToque = ViewConfiguration.get(Actividades.this).getScaledTouchSlop();

        @SuppressLint("ClickableViewAccessibility") // el toque se gestiona aquí (abre el detalle)
        @Override
        public boolean onTouch(View v, MotionEvent e) {
            switch (e.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    if (decidiendo) return false; // aún sale volando la anterior
                    xInicial = e.getRawX();
                    yInicial = e.getRawY();
                    return true;
                case MotionEvent.ACTION_MOVE: {
                    float dX = e.getRawX() - xInicial, dY = e.getRawY() - yInicial;
                    if (dY < 0 && -dY > Math.abs(dX)) { // hacia arriba: la ficha sube un poco (se va a dar la vuelta)
                        carta.setTranslationX(0);
                        carta.setRotation(0);
                        carta.setTranslationY(dY * 0.3f);
                        selloSi.setAlpha(0);
                        selloNo.setAlpha(0);
                        return true;
                    }
                    carta.setTranslationY(0);
                    float ancho = raiz.getWidth();
                    carta.setTranslationX(dX);
                    carta.setRotation(15f * dX / ancho);
                    selloSi.setAlpha(Math.max(0f, Math.min(1f, dX / (ancho * 0.3f))));
                    selloNo.setAlpha(Math.max(0f, Math.min(1f, -dX / (ancho * 0.3f))));
                    return true;
                }
                case MotionEvent.ACTION_UP: {
                    float dX = e.getRawX() - xInicial, dY = e.getRawY() - yInicial;
                    if (dY < -raiz.getHeight() * 0.12f && -dY > Math.abs(dX)) {
                        voltearFicha(); // deslizar hacia arriba = darle la vuelta (QR)
                        return true;
                    }
                    if (Math.abs(dX) < umbralToque && Math.abs(dY) < umbralToque) {
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

    // -------------------------------------------------
    //              CRÉDITOS ABIERTAUGR («Mis actividades»)
    // -------------------------------------------------

    /** Créditos conseguidos (cursos ya acabados) y en curso, sobre los que se pueden reconocer. */
    private void actualizarCreditos() {
        View panel = findViewById(R.id.panelCreditos);
        boolean ver = correoUsuario != null && FILTRO_APUNTADO.equals(filtroSeleccionado);
        panel.setVisibility(ver ? View.VISIBLE : View.GONE);
        if (!ver) return;
        double[] creditos = actividadesDB.creditosAbiertaUGR(correoUsuario, FechasUtil.ahoraBD());
        double total = creditos[0] + creditos[1];
        String textoTotal = getString(R.string.nui_cred_total_fmt, ects(total), ects(CREDITOS_RECONOCIBLES));
        ((TextView) findViewById(R.id.tvCreditosTotal)).setText(textoTotal);
        LinearProgressIndicator barra = findViewById(R.id.barraCreditos);
        barra.setMax((int) Math.round(CREDITOS_RECONOCIBLES * 10));
        barra.setProgressCompat((int) Math.round(Math.min(total, CREDITOS_RECONOCIBLES) * 10), true);
        String detalle = total >= CREDITOS_RECONOCIBLES
                ? getString(R.string.nui_cred_completo_fmt, ects(CREDITOS_RECONOCIBLES))
                : getString(R.string.nui_cred_detalle_fmt, ects(creditos[0]), ects(creditos[1]));
        ((TextView) findViewById(R.id.tvCreditosDetalle)).setText(detalle);
        panel.setContentDescription(getString(R.string.nui_cred_titulo) + ": " + textoTotal + ". " + detalle);
    }

    /** 3.0 → "3", 1.5 → "1.5" */
    private static String ects(double c) {
        return c == Math.floor(c) ? String.valueOf((int) c) : String.valueOf(c);
    }

    // -------------------------------------------------
    //              QR DE LOS CARTELES
    // -------------------------------------------------

    /** Busca la actividad del QR, la pone encima del mazo y abre su detalle para apuntarse. */
    private void alEscanear(String contenido) {
        String clave = contenido.trim();
        if (!clave.toUpperCase(java.util.Locale.ROOT).startsWith(PREFIJO_QR)) {
            avisar(getString(R.string.nui_act_qr_no_es));
            return;
        }
        clave = clave.substring(PREFIJO_QR.length()).trim();
        Actividad encontrada = null;
        for (Actividad a : todas) {
            String original = a.tituloEs != null ? a.tituloEs : a.titulo; // los carteles van en español
            if (claveQR(original).equals(claveQR(clave)) || String.valueOf(a.id).equals(clave)) encontrada = a;
        }
        if (encontrada == null) {
            avisar(getString(R.string.nui_act_qr_no_esta));
            return;
        }
        avisarTutorial(Gesto.ESCANEAR);
        filtroSeleccionado = null;
        marcarFiltro(FILTRO_TODAS);
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
            pedirSesion(getString(R.string.nui_act_accion_apuntarte));
            return false;
        }
        if (a.inscrito) return true; // ya lo estaba: simplemente pasa a la siguiente
        if (!a.plazoAbierto()) {
            avisar(getString(R.string.act_plazo_cerrado_fmt, a.estadoInscripcion(this)));
            return false;
        }
        if (a.completa()) {
            avisar(getString(R.string.nui_act_sin_plazas));
            return false;
        }
        return true;
    }

    private void apuntar(Actividad a) {
        if (a.inscrito) {
            avisar(getString(R.string.act_ya_apuntado_fmt, a.titulo));
            return;
        }
        if (!actividadesDB.insertarInscripcion(correoUsuario, a.id, FechasUtil.ahoraBD())) {
            avisar(getString(R.string.act_error_inscripcion));
            return;
        }
        a.inscrito = true;
        a.inscritos++;
        RecordatorioActividad.programar(this, a); // aviso con "Llévame" media hora antes
        mostrarCarta();
        // Aviso con "Deshacer". Lo de añadir al calendario está en el detalle.
        // Si las notificaciones están desactivadas, el recordatorio no llegará: se dice en el mismo aviso
        String textoApuntado = getString(R.string.nui_act_aviso_apuntado_fmt, a.titulo);
        if (!NotificationManagerCompat.from(this).areNotificationsEnabled())
            textoApuntado += ". " + getString(R.string.nui_act_sin_notificaciones);
        Snackbar.make(raiz, textoApuntado, MS_DESHACER)
                .setAnchorView(botonera)
                .setActionTextColor(getColor(R.color.nui_acento))
                .setAction(R.string.deshacer, x -> {
                    actividadesDB.borrarInscripcion(correoUsuario, a.id);
                    RecordatorioActividad.cancelar(this, a.id);
                    a.inscrito = false;
                    a.inscritos--;
                    mostrarCarta();
                }).show();
    }

    private void desapuntar(Actividad a) {
        actividadesDB.borrarInscripcion(correoUsuario, a.id);
        RecordatorioActividad.cancelar(this, a.id);
        a.inscrito = false;
        a.inscritos--;
        mostrarCarta();
        Snackbar.make(raiz, getString(R.string.nui_act_aviso_desapuntado_fmt, a.titulo), MS_DESHACER)
                .setAnchorView(botonera)
                .setActionTextColor(getColor(R.color.nui_acento))
                .setAction(R.string.deshacer, x -> {
                    actividadesDB.insertarInscripcion(correoUsuario, a.id, FechasUtil.ahoraBD());
                    a.inscrito = true;
                    a.inscritos++;
                    RecordatorioActividad.programar(this, a);
                    mostrarCarta();
                }).show();
    }

    private void avisar(String texto) {
        Snackbar.make(raiz, texto, Snackbar.LENGTH_SHORT).setAnchorView(botonera).show();
    }

    /** Aviso de que hace falta iniciar sesión, con botón para ir al login. */
    private void pedirSesion(String accion) {
        Snackbar.make(raiz, getString(R.string.nui_act_inicia_sesion_para_fmt, accion), Snackbar.LENGTH_LONG)
                .setAnchorView(botonera)
                .setAction(R.string.nui_act_iniciar_sesion, v -> startActivity(new Intent(this, Login.class)))
                .setActionTextColor(getColor(R.color.nui_acento))
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
        TextView tvTipoDetalle = v.findViewById(R.id.tvTipoDetalle);
        tvTipoDetalle.setText(Actividad.nombreTipo(this, a.tipo));
        pintarEtiqueta(tvTipoDetalle, a.tipo);

        ((TextView) v.findViewById(R.id.tvTituloDetalle)).setText(a.titulo);
        ((TextView) v.findViewById(R.id.tvFechaDetalle)).setText(textoFechas(a));
        ((TextView) v.findViewById(R.id.tvLugarDetalle)).setText(lugar(a));
        ((TextView) v.findViewById(R.id.tvDescripcionDetalle)).setText(a.descripcion);

        TextView tvAbierta = v.findViewById(R.id.tvAbiertaDetalle);
        if (a.esAbiertaUGR()) {
            tvAbierta.setVisibility(View.VISIBLE);
            tvAbierta.setText(a.creditosTexto() + "\n" + a.estadoInscripcion(this));
            if (a.inscripcionUrgente()) {
                tvAbierta.setTextColor(getColor(R.color.sec_error));
                tvAbierta.setCompoundDrawableTintList(ColorStateList.valueOf(getColor(R.color.sec_error)));
            }
        }

        String plazas = a.plazasTotales != null
                ? getString(R.string.nui_act_plazas_ocupadas_fmt, a.inscritos, a.plazasTotales)
                : getResources().getQuantityString(R.plurals.nui_act_personas, a.inscritos, a.inscritos);
        ((TextView) v.findViewById(R.id.tvPlazasDetalle)).setText(plazas);

        // Botón principal según el estado (si ya estás apuntado, sale el secundario para desapuntarte)
        MaterialButton btnApuntarme = v.findViewById(R.id.btnApuntarme);
        if (a.inscrito) {
            btnApuntarme.setVisibility(View.GONE);
            View btnDesapuntarme = v.findViewById(R.id.btnDesapuntarme);
            btnDesapuntarme.setVisibility(View.VISIBLE);
            btnDesapuntarme.setOnClickListener(x -> { hoja.dismiss(); desapuntar(a); });
        } else if (!a.plazoAbierto() || a.completa()) {
            btnApuntarme.setText(a.completa() ? getString(R.string.nui_act_btn_agotadas) : a.estadoInscripcion(this));
            btnApuntarme.setEnabled(false);
        } else if (correoUsuario == null) {
            btnApuntarme.setText(R.string.nui_act_btn_login);
            btnApuntarme.setOnClickListener(x -> {
                hoja.dismiss();
                startActivity(new Intent(this, Login.class));
            });
        } else {
            btnApuntarme.setText(R.string.nui_act_btn_apuntarme);
            btnApuntarme.setOnClickListener(x -> { hoja.dismiss(); apuntar(a); });
        }

        v.findViewById(R.id.btnCalendario).setOnClickListener(x -> anadirACalendario(a));
        // Lo mismo que los gestos, con botones (para quien no pueda o no quiera mover el móvil)
        MaterialButton btnGuardar = v.findViewById(R.id.btnGuardar);
        boolean guardada = guardadas.contains(String.valueOf(a.id));
        btnGuardar.setText(guardada ? R.string.nui_act_btn_quitar_guardada : R.string.nui_act_btn_guardar);
        btnGuardar.setOnClickListener(x -> {
            hoja.dismiss();
            ponerGuardada(a, !guardada);
            aplicarFiltroEn(a);
            if (!mazo.contains(a)) mostrarCarta();
            avisar(getString(guardada ? R.string.nui_act_aviso_quitada_fmt : R.string.nui_act_aviso_guardada_fmt, a.titulo));
        });
        v.findViewById(R.id.btnEnsenarQr).setOnClickListener(x -> { hoja.dismiss(); mostrarQr(a, false); });
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
                texto = getString(R.string.nui_act_hasta_fmt, texto, FechasUtil.diaLargo(a.fin));
            }
        }
        return texto;
    }

    private String lugar(Actividad a) {
        String sitio = a.sitioEnMapa();
        return sitio != null ? getString(R.string.nui_act_lugar_fmt, sitio) : getString(R.string.nui_act_lugar_escuela);
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
        if (!ok) avisar(getString(R.string.nui_act_sin_calendario));
    }

    private void abrirEnlace(String url) {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        } catch (ActivityNotFoundException e) {
            avisar(getString(R.string.nui_sin_navegador));
        }
    }

    // -------------------------------------------------
    //              TUTORIAL (util/tutorial, de Izaro)
    // -------------------------------------------------

    /** Lo enseña BaseActivity la primera vez; el botón "?" de la barra lo repite. */
    @Override
    protected Tutorial crearTutorial() {
        return new Tutorial(this, "actividades", 9)
                .info(R.id.pestanasTipos, getString(R.string.nui_tut_act_pestanas))
                .gesto(R.id.carta, Gesto.DESLIZAR_IZQ, getString(R.string.nui_tut_act_deslizar))
                .soloSi(() -> posicion < mazo.size() - 1)
                .sensor(Gesto.INCLINAR, getString(R.string.nui_tut_act_inclinar))
                .soloSi(() -> posicion < mazo.size() - 1 && inclinar != null && inclinar.escuchando())
                .sensor(Gesto.HACIA_TI, getString(R.string.nui_tut_act_hacia_ti))
                .soloSi(() -> posicion < mazo.size() - 1 && haciaTi.disponible())
                .gesto(R.id.carta, Gesto.LANZAR, getString(R.string.nui_tut_act_ensenar))
                .soloSi(() -> posicion < mazo.size())
                .info(R.id.pestanasTipos, getString(R.string.nui_tut_act_creditos))
                .soloSi(() -> correoUsuario != null)
                .info(R.id.btnAnterior, getString(R.string.nui_tut_act_anterior))
                .info(R.id.btnEscanear, getString(R.string.nui_tut_act_escanear))
                .info(R.id.btnComunidades, getString(R.string.nui_tut_act_grupos))
                // el último: al tocar se abre el detalle encima
                .tocar(R.id.carta, getString(R.string.nui_tut_act_tocar))
                .soloSi(() -> posicion < mazo.size());
    }

    /**
     * La app va de borde a borde: la cabecera baja lo que ocupa la barra de estado (como en Asignaturas)
     * y la botonera sube lo que ocupa la barra de navegación (en tablets, la barra de apps), para que
     * Escanear y Grupos no queden tapados.
     */
    static void margenBarraEstado(View raiz) {
        if (raiz == null) return;
        final int abajoOriginal = raiz.getPaddingBottom();
        ViewCompat.setOnApplyWindowInsetsListener(raiz, (v, insets) -> {
            Insets barras = insets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
            v.setPadding(v.getPaddingLeft(), barras.top, v.getPaddingRight(), abajoOriginal + barras.bottom);
            return insets;
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
