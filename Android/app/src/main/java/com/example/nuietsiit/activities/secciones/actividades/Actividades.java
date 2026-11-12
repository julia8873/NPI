package com.example.nuietsiit.activities.secciones.actividades;

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
import com.example.nuietsiit.data.ActividadesDB;
import com.example.nuietsiit.data.DBHelper;
import com.example.nuietsiit.data.modelo.Actividad;
import com.example.nuietsiit.util.CalendarioUtil;
import com.example.nuietsiit.util.FechasUtil;
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
 * ↺ = volver a la anterior. Tocar la tarjeta = detalle completo.
 */
public class Actividades extends BaseActivity {

    private static final String FILTRO_APUNTADO = "__apuntado"; // Chip "Mis actividades"
    private static final int NARANJA = 0xFFFF8C42;
    private static final int ROJO = 0xFFD32F2F;
    private static final int AZUL = 0xFF1565C0;

    private ActividadesDB actividadesDB;
    private String correoUsuario;          // null = no hay sesión iniciada
    private String filtroSeleccionado;     // null = todas
    private List<Actividad> todas = new ArrayList<>();
    private List<Actividad> mazo = new ArrayList<>(); // Las del filtro actual
    private int posicion;                  // Carta que se ve ahora

    private View raiz, botonera, vacio;
    private MaterialCardView carta;
    private ChipGroup chipsTipos;
    private TextView tvVacio, tvContador, selloSi, selloNo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.actividades);

        configurarCabeceraUnificada(getString(R.string.titulo_actividades));

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

        // Botón de comunidades de chat
        findViewById(R.id.btnComunidades).setOnClickListener(
                v -> HojaComunidades.mostrar(this, correoUsuario));

        // Botonera y gesto de arrastrar la carta
        findViewById(R.id.btnPasar).setOnClickListener(v -> decidir(-1));
        findViewById(R.id.btnApuntar).setOnClickListener(v -> decidir(+1));
        findViewById(R.id.btnAnterior).setOnClickListener(v -> anterior());
        findViewById(R.id.btnReiniciar).setOnClickListener(v -> { posicion = 0; mostrarCarta(); });
        carta.setOnTouchListener(new ArrastrarCarta());

        crearChips();
        cargarActividades();
    }

    @Override
    protected void onResume() {
        super.onResume(); // BaseActivity: activa el sensor de agitar
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
        anadirChip(2000, null, getString(R.string.chip_todas));
        anadirChip(2001, FILTRO_APUNTADO, getString(R.string.chip_mis_actividades));
        for (int i = 0; i < Actividad.TIPOS.length; i++) {
            String tipo = Actividad.TIPOS[i][0];
            anadirChip(2002 + i, tipo, Actividad.emoji(tipo) + " " + Actividad.TIPOS[i][1]);
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
            boolean visteTodas = !mazo.isEmpty();
            findViewById(R.id.btnReiniciar).setVisibility(visteTodas ? View.VISIBLE : View.GONE);
            tvContador.setText("");
            if (visteTodas) {
                tvVacio.setText(R.string.vacio_viste_todas);
            } else if (!FILTRO_APUNTADO.equals(filtroSeleccionado)) {
                tvVacio.setText(R.string.vacio_no_hay_tipo);
            } else if (correoUsuario == null) {
                tvVacio.setText(R.string.vacio_inicia_sesion);
            } else {
                tvVacio.setText(R.string.vacio_no_apuntado);
            }
            return;
        }

        Actividad a = mazo.get(posicion);
        findViewById(R.id.cabeceraCarta).setBackgroundColor(Actividad.color(a.tipo));
        ((TextView) findViewById(R.id.tvEmojiCarta)).setText(Actividad.emoji(a.tipo));
        ((TextView) findViewById(R.id.tvTipoCarta)).setText(Actividad.nombreTipo(a.tipo));
        ((TextView) findViewById(R.id.tvCuandoCarta)).setText(
                "📅 " + FechasUtil.cuando(this, a.inicio) + " · " + FechasUtil.hora(a.inicio));
        ((TextView) findViewById(R.id.tvTituloCarta)).setText(a.titulo);
        ((TextView) findViewById(R.id.tvDescCarta)).setText(a.descripcion);

        TextView tvExtra = findViewById(R.id.tvExtraCarta);
        if (a.esAbiertaUGR()) {
            tvExtra.setVisibility(View.VISIBLE);
            tvExtra.setText("🎓 " + a.creditosTexto() + " · " + a.estadoInscripcion());
            tvExtra.setTextColor(a.inscripcionUrgente() ? ROJO : AZUL);
        } else {
            tvExtra.setVisibility(View.GONE);
        }
        findViewById(R.id.tvInscritoCarta).setVisibility(a.inscrito ? View.VISIBLE : View.GONE);
        tvContador.setText(getString(R.string.contador_paginas, posicion + 1, mazo.size()));
    }

    private void decidir(int direccion) {
        if (posicion >= mazo.size()) return;
        Actividad a = mazo.get(posicion);
        if (direccion > 0 && !puedeApuntarse(a)) {
            volverAlCentro();
            return;
        }
        float ancho = raiz.getWidth();
        carta.animate().translationX(direccion * ancho * 1.5f).rotation(direccion * 25f)
                .setDuration(220)
                .withEndAction(() -> {
                    if (direccion > 0) apuntar(a);
                    posicion++;
                    mostrarCarta();
                    carta.setScaleX(0.92f);
                    carta.setScaleY(0.92f);
                    carta.animate().scaleX(1f).scaleY(1f).setDuration(150).start();
                })
                .start();
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

    private class ArrastrarCarta implements View.OnTouchListener {
        private float xInicial;
        private final int umbralToque = ViewConfiguration.get(Actividades.this).getScaledTouchSlop();

        @SuppressLint("ClickableViewAccessibility")
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

    private boolean puedeApuntarse(Actividad a) {
        if (correoUsuario == null) {
            pedirSesion(getString(R.string.btn_apuntarme).replace("🙋 ", "").toLowerCase());
            return false;
        }
        if (a.inscrito) return true;
        if (!a.plazoAbierto()) {
            avisar(getString(R.string.plazo_cerrado_msg, a.estadoInscripcion()));
            return false;
        }
        if (a.completa()) {
            avisar(getString(R.string.sin_plazas));
            return false;
        }
        return true;
    }

    private void apuntar(Actividad a) {
        if (a.inscrito) {
            avisar(getString(R.string.ya_apuntado, a.titulo));
            return;
        }
        if (!actividadesDB.insertarInscripcion(correoUsuario, a.id, FechasUtil.ahoraBD())) {
            avisar(getString(R.string.error_inscripcion));
            return;
        }
        a.inscrito = true;
        a.inscritos++;
        mostrarCarta();
        Snackbar.make(raiz, getString(R.string.snack_apuntado, a.titulo), Snackbar.LENGTH_LONG)
                .setAnchorView(botonera)
                .setAction("📅 " + getString(R.string.calendario_etiqueta), v -> anadirACalendario(a))
                .setActionTextColor(NARANJA)
                .show();
    }

    private void desapuntar(Actividad a) {
        actividadesDB.borrarInscripcion(correoUsuario, a.id);
        a.inscrito = false;
        a.inscritos--;
        mostrarCarta();
        Snackbar.make(raiz, getString(R.string.snack_desapuntado, a.titulo), Snackbar.LENGTH_LONG)
                .setAnchorView(botonera)
                .setAction(R.string.action_deshacer, v -> {
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

    private void pedirSesion(String accion) {
        Snackbar.make(raiz, getString(R.string.pedir_sesion_msg, accion), Snackbar.LENGTH_LONG)
                .setAnchorView(botonera)
                .setAction(R.string.btn_iniciar_sesion, v -> startActivity(new Intent(this, Login.class)))
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
        tvTipo.setText(Actividad.emoji(a.tipo) + " " + Actividad.nombreTipo(a.tipo));
        tvTipo.setBackgroundTintList(ColorStateList.valueOf(Actividad.color(a.tipo)));

        ((TextView) v.findViewById(R.id.tvTituloDetalle)).setText(a.titulo);
        ((TextView) v.findViewById(R.id.tvFechaDetalle)).setText("📅 " + textoFechas(a));
        ((TextView) v.findViewById(R.id.tvLugarDetalle)).setText("📍 " + lugar(a));
        ((TextView) v.findViewById(R.id.tvDescripcionDetalle)).setText(a.descripcion);

        TextView tvAbierta = v.findViewById(R.id.tvAbiertaDetalle);
        if (a.esAbiertaUGR()) {
            tvAbierta.setVisibility(View.VISIBLE);
            tvAbierta.setText("🎓 " + a.creditosTexto() + "\n" + a.estadoInscripcion());
            if (a.inscripcionUrgente()) tvAbierta.setTextColor(ROJO);
        }

        String plazas = a.plazasTotales != null
                ? getString(R.string.plazas_ocupadas_fmt, a.inscritos, a.plazasTotales)
                : (a.inscritos == 1 ? getString(R.string.personas_apuntadas_uno, a.inscritos) : getString(R.string.personas_apuntadas_varios, a.inscritos));
        ((TextView) v.findViewById(R.id.tvPlazasDetalle)).setText(plazas);

        Button btnApuntarme = v.findViewById(R.id.btnApuntarme);
        if (a.inscrito) {
            btnApuntarme.setText(R.string.btn_desapuntarme);
            btnApuntarme.setBackgroundResource(R.drawable.boton_gris);
            btnApuntarme.setOnClickListener(x -> { hoja.dismiss(); desapuntar(a); });
        } else if (!a.plazoAbierto() || a.completa()) {
            btnApuntarme.setText(a.completa() ? getString(R.string.btn_plazas_agotadas) : a.estadoInscripcion());
            btnApuntarme.setBackgroundResource(R.drawable.boton_gris);
            btnApuntarme.setEnabled(false);
        } else if (correoUsuario == null) {
            btnApuntarme.setText(R.string.btn_inicia_sesion_apuntarte);
            btnApuntarme.setOnClickListener(x -> {
                hoja.dismiss();
                startActivity(new Intent(this, Login.class));
            });
        } else {
            btnApuntarme.setText(R.string.btn_apuntarme);
            btnApuntarme.setOnClickListener(x -> { hoja.dismiss(); apuntar(a); });
        }

        v.findViewById(R.id.btnCalendario).setOnClickListener(x -> anadirACalendario(a));

        View btnMasInfo = v.findViewById(R.id.btnMasInfo);
        if (a.enlace != null && !a.enlace.isEmpty()) {
            btnMasInfo.setOnClickListener(x -> abrirEnlace(a.enlace));
        } else {
            btnMasInfo.setVisibility(View.GONE);
        }

        hoja.show();
    }

    private String textoFechas(Actividad a) {
        String texto = FechasUtil.capitalizar(FechasUtil.diaLargo(a.inicio)) + " · " + FechasUtil.hora(a.inicio);
        if (a.fin != null) {
            if (a.fin.toLocalDate().equals(a.inicio.toLocalDate())) {
                texto += " – " + FechasUtil.hora(a.fin);
            } else {
                texto += "\n    " + getString(R.string.hasta_el, FechasUtil.diaLargo(a.fin));
            }
        }
        return texto;
    }

    private String lugar(Actividad a) {
        return a.lugar != null ? getString(R.string.lugar_etsiit_fmt, a.lugar) : getString(R.string.lugar_default);
    }

    // -------------------------------------------------
    //              CALENDARIO Y ENLACES
    // -------------------------------------------------

    private void anadirACalendario(Actividad a) {
        boolean mismoDia = a.fin != null && a.fin.toLocalDate().equals(a.inicio.toLocalDate());
        boolean ok = CalendarioUtil.anadirEvento(this, a.titulo, a.descripcion, lugar(a),
                a.inicio, mismoDia ? a.fin : a.inicio.plusHours(2));
        if (!ok) avisar(getString(R.string.sin_app_calendario));
    }

    private void abrirEnlace(String url) {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        } catch (ActivityNotFoundException e) {
            avisar(getString(R.string.sin_navegador));
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}