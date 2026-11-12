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
import android.widget.Button;
import android.widget.TextView;

import com.example.nuietsiit.R;
import com.example.nuietsiit.activities.BaseActivity;
import com.example.nuietsiit.activities.auth.Login;
import com.example.nuietsiit.activities.secciones.actividades.HojaComunidades;
import com.example.nuietsiit.activities.secciones.mapa.Mapa;
import com.example.nuietsiit.data.ActividadesDB;
import com.example.nuietsiit.data.DBHelper;
import com.example.nuietsiit.data.modelo.Actividad;
import com.example.nuietsiit.ui.InclinarParaPasar;
import com.example.nuietsiit.util.CalendarioUtil;
import com.example.nuietsiit.util.FechasUtil;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.List;

/**
 * Pantalla de Actividades SIN SCROLL: un "mazo" con una tarjeta grande cada vez.
 * Deslizar a la derecha (o botón ✓) = me apunto y pasa a la siguiente.
 * Deslizar a la izquierda (o botón ✕) = paso, siguiente.
 * SENSOR (acelerómetro): inclinar el móvil a la derecha = me apunto, a la izquierda = paso.
 * Usa InclinarParaPasar, el mismo gesto que pasar páginas en Secretaría y Biblioteca.
 * ↺ = volver a la anterior. Tocar la tarjeta = detalle completo (apuntarse, calendario, llévame, más info).
 * Sin sesión iniciada se pueden ver, pero no apuntarse.
 */
public class Actividades extends BaseActivity {

    private static final String FILTRO_APUNTADO = "__apuntado"; // chip "Mis actividades"
    private static final int NARANJA = 0xFFFF8C42;
    private static final int ROJO = 0xFFD32F2F;
    private static final int AZUL = 0xFF1565C0;

    private ActividadesDB actividadesDB;
    private String correoUsuario;          // null = no hay sesión iniciada
    private String filtroSeleccionado;     // null = todas
    private List<Actividad> todas = new ArrayList<>();
    private List<Actividad> mazo = new ArrayList<>(); // las del filtro actual
    private int posicion;                  // carta que se ve ahora

    private View raiz, botonera, vacio;
    private MaterialCardView carta;
    private ChipGroup chipsTipos;
    private TextView tvVacio, tvContador, selloSi, selloNo;
    private InclinarParaPasar inclinar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.actividades);

        // Barra superior con botón atrás
        MaterialToolbar barra = findViewById(R.id.toolbar);
        if (barra != null) {
            setSupportActionBar(barra);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle(R.string.act_titulo);
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            }
        }

        // Sesión guardada al iniciar sesión (SharedPreferences "SesionUsuario")
        correoUsuario = correoSesion();
        actividadesDB = new ActividadesDB(DBHelper.getInstance(this));

        raiz = findViewById(android.R.id.content);
        botonera = findViewById(R.id.botonera);
        vacio = findViewById(R.id.vacio);
        carta = findViewById(R.id.carta);
        chipsTipos = findViewById(R.id.chipsTipos);
        tvVacio = findViewById(R.id.tvVacio);
        tvContador = findViewById(R.id.tvContador);
        selloSi = findViewById(R.id.selloSi);
        selloNo = findViewById(R.id.selloNo);

        // Botón de comunidades de chat (grupos de WhatsApp/Telegram/Discord simulados)
        findViewById(R.id.btnComunidades).setOnClickListener(
                v -> HojaComunidades.mostrar(this, correoUsuario));

        // Botonera y gesto de arrastrar la carta
        findViewById(R.id.btnPasar).setOnClickListener(v -> decidir(-1));
        findViewById(R.id.btnApuntar).setOnClickListener(v -> decidir(+1));
        findViewById(R.id.btnAnterior).setOnClickListener(v -> anterior());
        findViewById(R.id.btnReiniciar).setOnClickListener(v -> { posicion = 0; mostrarCarta(); });
        carta.setOnTouchListener(new ArrastrarCarta());

        // Sensor: inclinar el móvil hace lo mismo que los botones (+1 derecha = me apunto, -1 izquierda = paso)
        inclinar = new InclinarParaPasar(carta, this::alInclinar, () -> posicion < mazo.size());

        crearChips();
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

    private void crearChips() {
        anadirChip(2000, null, getString(R.string.act_chip_todas));
        anadirChip(2001, FILTRO_APUNTADO, getString(R.string.act_chip_mis));
        for (int i = 0; i < Actividad.TIPOS.length; i++) {
            String tipo = Actividad.TIPOS[i][0];
            anadirChip(2002 + i, tipo, getString(R.string.act_chip_tipo_fmt, Actividad.emoji(tipo), Actividad.nombreTipo(tipo)));
        }
        chipsTipos.setOnCheckedStateChangeListener((grupo, ids) -> {
            if (ids.isEmpty()) return;
            filtroSeleccionado = (String) grupo.findViewById(ids.get(0)).getTag();
            aplicarFiltro();
        });
    }

    private void anadirChip(int id, String clave, String etiqueta) {
        Chip chip = new Chip(this, null, com.google.android.material.R.attr.chipStyle);
        chip.setId(id);
        chip.setText(etiqueta);
        chip.setTag(clave);
        chip.setCheckable(true);
        chip.setChipBackgroundColorResource(R.color.chip_fondo);
        chip.setTextColor(getColorStateList(R.color.chip_texto));
        chip.setCheckedIconVisible(false);
        chipsTipos.addView(chip);
        if (clave == null) chip.setChecked(true);
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

        if (!hayCarta) {
            if (inclinar != null) inclinar.actualizar();
            boolean visteTodas = !mazo.isEmpty();
            findViewById(R.id.btnReiniciar).setVisibility(visteTodas ? View.VISIBLE : View.GONE);
            tvContador.setText("");
            if (visteTodas) {
                tvVacio.setText(R.string.act_vacio_todas_vistas);
            } else if (!FILTRO_APUNTADO.equals(filtroSeleccionado)) {
                tvVacio.setText(R.string.act_vacio_sin_tipo);
            } else if (correoUsuario == null) {
                tvVacio.setText(R.string.act_vacio_sin_sesion);
            } else {
                tvVacio.setText(R.string.act_vacio_sin_inscripciones);
            }
            return;
        }

        Actividad a = mazo.get(posicion);
        findViewById(R.id.cabeceraCarta).setBackgroundColor(Actividad.color(a.tipo));
        ((TextView) findViewById(R.id.tvEmojiCarta)).setText(Actividad.emoji(a.tipo));
        ((TextView) findViewById(R.id.tvTipoCarta)).setText(Actividad.nombreTipo(a.tipo));
        ((TextView) findViewById(R.id.tvCuandoCarta)).setText(
                getString(R.string.act_carta_cuando_fmt, FechasUtil.cuando(this, a.inicio), FechasUtil.hora(a.inicio)));
        ((TextView) findViewById(R.id.tvTituloCarta)).setText(a.titulo);
        ((TextView) findViewById(R.id.tvDescCarta)).setText(a.descripcion);

        TextView tvExtra = findViewById(R.id.tvExtraCarta);
        if (a.esAbiertaUGR()) {
            tvExtra.setVisibility(View.VISIBLE);
            tvExtra.setText(getString(R.string.act_carta_extra_fmt, a.creditosTexto(), a.estadoInscripcion()));
            tvExtra.setTextColor(a.inscripcionUrgente() ? ROJO : AZUL);
        } else {
            tvExtra.setVisibility(View.GONE);
        }
        findViewById(R.id.tvInscritoCarta).setVisibility(a.inscrito ? View.VISIBLE : View.GONE);
        tvContador.setText(getString(R.string.act_contador_fmt, posicion + 1, mazo.size()));
        if (inclinar != null) inclinar.actualizar();
    }

    /**
     * Botones ✕ / ✓ y final del gesto. direccion: +1 = me apunto, -1 = paso.
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
    //              APUNTARSE / DESAPUNTARSE
    // -------------------------------------------------

    /** Comprueba si se puede apuntar; si no, enseña el motivo. */
    private boolean puedeApuntarse(Actividad a) {
        if (correoUsuario == null) {
            pedirSesion(getString(R.string.act_accion_apuntarte));
            return false;
        }
        if (a.inscrito) return true; // ya lo estaba: simplemente pasa a la siguiente
        if (!a.plazoAbierto()) {
            avisar(getString(R.string.act_plazo_cerrado_fmt, a.estadoInscripcion()));
            return false;
        }
        if (a.completa()) {
            avisar(getString(R.string.act_sin_plazas));
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
        mostrarCarta();
        Snackbar.make(raiz, getString(R.string.act_apuntado_ok_fmt, a.titulo), Snackbar.LENGTH_LONG)
                .setAnchorView(botonera)
                .setAction(R.string.act_accion_calendario, v -> anadirACalendario(a))
                .setActionTextColor(NARANJA)
                .show();
    }

    private void desapuntar(Actividad a) {
        actividadesDB.borrarInscripcion(correoUsuario, a.id);
        a.inscrito = false;
        a.inscritos--;
        mostrarCarta();
        Snackbar.make(raiz, getString(R.string.act_desapuntado_ok_fmt, a.titulo), Snackbar.LENGTH_LONG)
                .setAnchorView(botonera)
                .setAction(R.string.act_accion_deshacer, v -> {
                    actividadesDB.insertarInscripcion(correoUsuario, a.id, FechasUtil.ahoraBD());
                    a.inscrito = true;
                    a.inscritos++;
                    mostrarCarta();
                })
                .setActionTextColor(NARANJA)
                .show();
    }

    private void avisar(String texto) {
        Snackbar.make(raiz, texto, Snackbar.LENGTH_SHORT).setAnchorView(botonera).show();
    }

    /** Aviso de que hace falta iniciar sesión, con botón para ir al login. */
    private void pedirSesion(String accion) {
        Snackbar.make(raiz, getString(R.string.act_pedir_sesion_fmt, accion), Snackbar.LENGTH_LONG)
                .setAnchorView(botonera)
                .setAction(R.string.act_accion_login, v -> startActivity(new Intent(this, Login.class)))
                .setActionTextColor(NARANJA)
                .show();
    }

    // -------------------------------------------------
    //              DETALLE (hoja inferior)
    // -------------------------------------------------

    private void mostrarDetalle(Actividad a) {
        BottomSheetDialog hoja = new BottomSheetDialog(this);
        View v = getLayoutInflater().inflate(R.layout.detalle_actividad, null);
        hoja.setContentView(v);

        TextView tvTipo = v.findViewById(R.id.tvTipoDetalle);
        tvTipo.setText(getString(R.string.act_detalle_tipo_fmt, Actividad.emoji(a.tipo), Actividad.nombreTipo(a.tipo)));
        tvTipo.setBackgroundTintList(ColorStateList.valueOf(Actividad.color(a.tipo)));

        ((TextView) v.findViewById(R.id.tvTituloDetalle)).setText(a.titulo);
        ((TextView) v.findViewById(R.id.tvFechaDetalle)).setText(getString(R.string.act_detalle_fecha_fmt, textoFechas(a)));
        ((TextView) v.findViewById(R.id.tvLugarDetalle)).setText(getString(R.string.act_detalle_lugar_fmt, lugar(a)));
        ((TextView) v.findViewById(R.id.tvDescripcionDetalle)).setText(a.descripcion);

        TextView tvAbierta = v.findViewById(R.id.tvAbiertaDetalle);
        if (a.esAbiertaUGR()) {
            tvAbierta.setVisibility(View.VISIBLE);
            tvAbierta.setText(getString(R.string.act_detalle_abierta_fmt, a.creditosTexto(), a.estadoInscripcion()));
            if (a.inscripcionUrgente()) tvAbierta.setTextColor(ROJO);
        }

        String plazas = a.plazasTotales != null
                ? getString(R.string.act_plazas_ocupadas_fmt, a.inscritos, a.plazasTotales)
                : getResources().getQuantityString(R.plurals.act_personas_apuntadas, a.inscritos, a.inscritos);
        ((TextView) v.findViewById(R.id.tvPlazasDetalle)).setText(getString(R.string.act_detalle_plazas_fmt, plazas));

        // Botón principal según el estado
        Button btnApuntarme = v.findViewById(R.id.btnApuntarme);
        if (a.inscrito) {
            btnApuntarme.setText(R.string.act_btn_desapuntarme);
            btnApuntarme.setBackgroundResource(R.drawable.boton_gris);
            btnApuntarme.setOnClickListener(x -> { hoja.dismiss(); desapuntar(a); });
        } else if (!a.plazoAbierto() || a.completa()) {
            btnApuntarme.setText(a.completa() ? getString(R.string.act_btn_agotadas) : a.estadoInscripcion());
            btnApuntarme.setBackgroundResource(R.drawable.boton_gris);
            btnApuntarme.setEnabled(false);
        } else if (correoUsuario == null) {
            btnApuntarme.setText(R.string.act_btn_login_apuntar);
            btnApuntarme.setOnClickListener(x -> {
                hoja.dismiss();
                startActivity(new Intent(this, Login.class));
            });
        } else {
            btnApuntarme.setText(R.string.act_btn_apuntarme);
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
        String texto = getString(R.string.act_fecha_inicio_fmt,
                FechasUtil.capitalizar(FechasUtil.diaLargo(a.inicio)),
                FechasUtil.hora(a.inicio));
        if (a.fin != null) {
            if (a.fin.toLocalDate().equals(a.inicio.toLocalDate())) {
                texto = getString(R.string.act_fecha_rango_mismo_dia_fmt, texto, FechasUtil.hora(a.fin));
            } else {
                texto = getString(R.string.act_fecha_hasta_fmt, texto, FechasUtil.diaLargo(a.fin));
            }
        }
        return texto;
    }

    private String lugar(Actividad a) {
        String sitio = a.sitioEnMapa();
        return sitio != null
                ? getString(R.string.act_lugar_sitio_fmt, sitio)
                : getString(R.string.act_lugar_general);
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
        if (!ok) avisar(getString(R.string.act_error_sin_calendario));
    }

    private void abrirEnlace(String url) {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        } catch (ActivityNotFoundException e) {
            avisar(getString(R.string.act_error_sin_navegador));
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}