package com.example.nuietsiit.activities.secciones;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.viewpager2.widget.ViewPager2;

import com.example.nuietsiit.R;
import com.example.nuietsiit.activities.BaseActivity;
import com.example.nuietsiit.activities.secciones.actividades.TirarHaciaTi;
import com.example.nuietsiit.activities.secciones.noticias.AnuncioAdapter;
import com.example.nuietsiit.activities.secciones.noticias.GestoMano;
import com.example.nuietsiit.data.AnunciosDB;
import com.example.nuietsiit.data.AnunciosGuardados;
import com.example.nuietsiit.data.AnunciosLeidos;
import com.example.nuietsiit.data.DBHelper;
import com.example.nuietsiit.data.modelo.Anuncio;
import com.example.nuietsiit.ui.InclinarParaPasar;
import com.example.nuietsiit.util.FechasUtil;
import com.example.nuietsiit.util.tutorial.Gesto;
import com.example.nuietsiit.util.tutorial.Tutorial;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Pantalla de Noticias SIN SCROLL: carrusel horizontal con una ficha grande por aviso.
 * Mismo aspecto que Asignaturas, Profesorado y Calendario: cabecera unificada, fondo gris claro, tarjetas
 * blancas y el naranja de la app (colores_secciones.xml y estilos_secciones.xml).
 * - Los urgentes van primero, con borde y etiqueta rojos; los no leídos llevan un punto naranja.
 * - Un aviso cuenta como leído si te quedas en él un momento o lo abres entero.
 * - Tocar la tarjeta = aviso completo en una hoja (con su enlace si tiene).
 * - SENSORES, sin tocar la pantalla:
 *   · inclinar el móvil (acelerómetro, InclinarParaPasar, como en Actividades): a la izquierda, el siguiente aviso;
 *     a la derecha, el anterior;
 *   · pasar la mano por delante (sensor de proximidad, GestoMano): el siguiente aviso (del último, al primero).
 * - GUARDAR (como en Actividades): inclinar el móvil hacia ti (acelerómetro, TirarHaciaTi) o tocar el marcapáginas
 *   de la ficha = guardar el aviso para tenerlo a mano en «Guardados» (AnunciosGuardados); si ya estaba, se quita.
 * - MULTITÁCTIL: barrer hacia abajo con dos dedos = marcar todos como leídos (con "Deshacer"), como el botón.
 */
public class Noticias extends BaseActivity {

    private static final String FILTRO_TODOS = "__todos";
    private static final String FILTRO_NO_LEIDOS = "__noleidos";
    private static final String FILTRO_GUARDADOS = "__guardados";
    /** Tiempo mirando una tarjeta para que cuente como leída. */
    private static final long MS_PARA_LEIDO = 1500;

    private AnunciosDB anunciosDB;
    private List<Anuncio> todos = new ArrayList<>();
    private List<Anuncio> visibles = new ArrayList<>();
    private String filtroSeleccionado; // null = todos

    private View raiz;
    private ViewPager2 vpAnuncios;
    private AnuncioAdapter adaptador;
    private TextView tvSinLeer, tvMarcarTodo, tvVacio, tvPagina;
    private final Handler reloj = new Handler(Looper.getMainLooper());
    private Runnable marcarPendiente;
    private int ultimaPagina;

    private GestoMano gestoMano;
    private InclinarParaPasar inclinar;
    private TirarHaciaTi haciaTi;          // inclinar hacia ti = guardar el aviso (como en Actividades)
    private String correo;                 // los guardados van por cuenta
    // barrido con dos dedos
    private static final float BARRIDO_FRACCION = 0.18f; // de la altura de la pantalla
    private boolean barriendo, barrido;
    private float barridoX, barridoY;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.noticias);

        // Cabecera unificada (como Asignaturas y Profesorado): volver, título, chat y perfil
        configurarCabeceraUnificada(getString(R.string.seccion_noticias));
        Actividades.margenBarraEstado(findViewById(R.id.rootNoticias));

        anunciosDB = new AnunciosDB(DBHelper.getInstance(this));

        raiz = findViewById(android.R.id.content);
        tvSinLeer = findViewById(R.id.tvSinLeer);
        tvMarcarTodo = findViewById(R.id.tvMarcarTodo);
        tvVacio = findViewById(R.id.tvVacio);
        tvPagina = findViewById(R.id.tvPagina);

        // Carrusel: se ve un trozo de la tarjeta anterior y la siguiente
        correo = getSharedPreferences("SesionUsuario", MODE_PRIVATE).getString("CORREO", null);
        adaptador = new AnuncioAdapter(this::mostrarDetalle, this::alternarGuardado);
        vpAnuncios = findViewById(R.id.vpAnuncios);
        vpAnuncios.setAdapter(adaptador);
        vpAnuncios.setOffscreenPageLimit(1);
        View interior = vpAnuncios.getChildAt(0); // el RecyclerView de dentro del ViewPager2
        if (interior instanceof androidx.recyclerview.widget.RecyclerView) {
            ((androidx.recyclerview.widget.RecyclerView) interior).setClipToPadding(false);
        }
        // Las fichas de los lados se ven un poco más pequeñas
        vpAnuncios.setPageTransformer((pagina, posicion) -> {
            float escala = 1f - 0.08f * Math.min(1f, Math.abs(posicion));
            pagina.setScaleY(escala);
        });
        vpAnuncios.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int posicion) {
                if (posicion > ultimaPagina) avisarTutorial(Gesto.DESLIZAR_IZQ); // ha pasado a la siguiente
                ultimaPagina = posicion;
                actualizarPagina();
                programarLeido(posicion);
            }
        });

        crearPestanas();

        // Sensores para pasar avisos sin tocar la pantalla
        inclinar = new InclinarParaPasar(vpAnuncios, this::alInclinar, () -> visibles.size() > 1);
        gestoMano = new GestoMano(this, () -> {
            if (visibles.size() < 2) return;
            avisarTutorial(Gesto.MANO);
            int siguiente = vpAnuncios.getCurrentItem() + 1;
            vpAnuncios.setCurrentItem(siguiente < visibles.size() ? siguiente : 0, true);
        });

        haciaTi = new TirarHaciaTi(this, this::alTirarHaciaTi);

        tvMarcarTodo.setOnClickListener(v -> marcarTodosLeidos());

        cargarAnuncios();
    }

    @Override
    protected void onResume() {
        super.onResume();
        gestoMano.iniciar();
        haciaTi.iniciar();
    }

    @Override
    protected void onPause() {
        super.onPause();
        gestoMano.parar();
        haciaTi.parar();
        if (marcarPendiente != null) reloj.removeCallbacks(marcarPendiente);
    }

    // -------------------------------------------------
    //              CARGA Y FILTROS
    // -------------------------------------------------

    private void cargarAnuncios() {
        todos = anunciosDB.obtenerAnunciosGenerales(FechasUtil.ahoraBD());
        Set<String> leidos = AnunciosLeidos.leidos(this);
        Set<String> guardados = AnunciosGuardados.guardados(this, correo);
        for (Anuncio a : todos) {
            a.leido = leidos.contains(String.valueOf(a.id));
            a.guardado = guardados.contains(String.valueOf(a.id));
        }
        aplicarFiltro();
    }

    /** Chips de filtro (bajan de línea si no caben): Todos · Sin leer · Plazos · Becas · Transporte · General */
    private void crearPestanas() {
        ChipGroup chips = findViewById(R.id.pestanasCategorias);
        anadirChip(chips, FILTRO_TODOS, getString(R.string.nui_not_todos));
        anadirChip(chips, FILTRO_NO_LEIDOS, getString(R.string.nui_not_sin_leer_pestana));
        anadirChip(chips, FILTRO_GUARDADOS, getString(R.string.nui_not_f_guardados));
        for (String[] c : Anuncio.CATEGORIAS) anadirChip(chips, c[0], Anuncio.nombreFiltro(this, c[0]));
        chips.check(chips.getChildAt(0).getId());
        chips.setOnCheckedStateChangeListener((grupo, ids) -> {
            if (ids.isEmpty()) return;
            String clave = (String) grupo.findViewById(ids.get(0)).getTag();
            filtroSeleccionado = FILTRO_TODOS.equals(clave) ? null : clave;
            aplicarFiltro();
        });
    }

    private void anadirChip(ChipGroup chips, String clave, String texto) {
        Chip chip = (Chip) getLayoutInflater().inflate(R.layout.item_chip_filtro, chips, false);
        chip.setId(View.generateViewId());
        chip.setText(texto);
        chip.setTag(clave);
        chips.addView(chip);
    }

    private void aplicarFiltro() {
        visibles = new ArrayList<>();
        for (Anuncio a : todos) {
            if (filtroSeleccionado == null
                    || (FILTRO_NO_LEIDOS.equals(filtroSeleccionado) && !a.leido)
                    || (FILTRO_GUARDADOS.equals(filtroSeleccionado) && a.guardado)
                    || filtroSeleccionado.equals(a.categoria)) {
                visibles.add(a);
            }
        }
        adaptador.setAnuncios(visibles);
        vpAnuncios.setCurrentItem(0, false);
        ultimaPagina = 0;
        if (inclinar != null) inclinar.actualizar(); // con un solo aviso no hay a dónde pasar
        tvVacio.setText(FILTRO_GUARDADOS.equals(filtroSeleccionado) ? R.string.nui_not_vacio_guardados : R.string.nui_not_vacio);
        tvVacio.setVisibility(visibles.isEmpty() ? View.VISIBLE : View.GONE);
        actualizarContador();
        actualizarPagina();
        if (!visibles.isEmpty()) programarLeido(0);
    }

    private void actualizarContador() {
        int sinLeer = 0;
        for (Anuncio a : todos) if (!a.leido) sinLeer++;
        if (sinLeer == 0) {
            tvSinLeer.setText(R.string.nui_not_al_dia);
        } else {
            tvSinLeer.setText(getResources().getQuantityString(R.plurals.nui_not_sin_leer, sinLeer, sinLeer));
        }
        tvMarcarTodo.setVisibility(sinLeer > 0 ? View.VISIBLE : View.GONE);
    }

    // "‹ 2 / 8 ›"
    private void actualizarPagina() {
        if (visibles.isEmpty()) {
            tvPagina.setText("");
            return;
        }
        int actual = vpAnuncios.getCurrentItem();
        tvPagina.setText((actual > 0 ? "‹  " : "   ") + (actual + 1) + " / " + visibles.size()
                + (actual < visibles.size() - 1 ? "  ›" : "   "));
        tvPagina.setContentDescription(getString(R.string.nui_not_pagina_desc_fmt, actual + 1, visibles.size()));
    }

    // -------------------------------------------------
    //              LEÍDOS
    // -------------------------------------------------

    /** Si el usuario se queda mirando la tarjeta un momento, cuenta como leída. */
    private void programarLeido(int posicion) {
        if (marcarPendiente != null) reloj.removeCallbacks(marcarPendiente);
        marcarPendiente = () -> {
            if (posicion < visibles.size() && vpAnuncios.getCurrentItem() == posicion) {
                marcarLeido(visibles.get(posicion));
            }
        };
        reloj.postDelayed(marcarPendiente, MS_PARA_LEIDO);
    }

    /** Botón "Marcar leídos" o barrer con dos dedos: todos leídos de una vez, con "Deshacer". */
    private void marcarTodosLeidos() {
        List<Integer> ids = new ArrayList<>();
        for (Anuncio a : todos) if (!a.leido) ids.add(a.id);
        if (ids.isEmpty()) { // no había nada que marcar: se dice, para que se vea que el gesto ha contado
            Snackbar.make(raiz, R.string.nui_not_ya_todos_leidos, Snackbar.LENGTH_SHORT).show();
            return;
        }
        AnunciosLeidos.marcarVarios(this, ids);
        for (Anuncio a : todos) a.leido = true;
        adaptador.notifyDataSetChanged();
        actualizarContador();
        Snackbar.make(raiz, getResources().getQuantityString(R.plurals.nui_not_marcados, ids.size(), ids.size()), 5000)
                .setActionTextColor(getColor(R.color.nui_acento))
                .setAction(R.string.deshacer, x -> {
                    AnunciosLeidos.desmarcarVarios(this, ids);
                    for (Anuncio a : todos) if (ids.contains(a.id)) a.leido = false;
                    adaptador.notifyDataSetChanged();
                    actualizarContador();
                })
                .show();
    }

    // -------------------------------------------------
    //              BARRER CON DOS DEDOS
    // -------------------------------------------------

    /**
     * Dos dedos que bajan juntos más de BARRIDO_FRACCION de la pantalla (y casi en vertical) = marcar todos como
     * leídos, como quien barre el tablón. Se mira aquí, antes que el carrusel, para que valga en toda la pantalla;
     * en cuanto cuenta, el carrusel recibe un "cancelar" y deja de moverse.
     */
    @Override
    public boolean dispatchTouchEvent(MotionEvent e) {
        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                barriendo = false;
                barrido = false;
                break;
            case MotionEvent.ACTION_POINTER_DOWN:
                if (e.getPointerCount() == 2 && !barrido) {
                    barriendo = true;
                    barridoX = media(e, true);
                    barridoY = media(e, false);
                }
                break;
            case MotionEvent.ACTION_MOVE:
                if (barriendo && !barrido && e.getPointerCount() >= 2) {
                    float dy = media(e, false) - barridoY, dx = Math.abs(media(e, true) - barridoX);
                    if (dy > getResources().getDisplayMetrics().heightPixels * BARRIDO_FRACCION && dx < dy * 0.6f) {
                        barrido = true;
                        MotionEvent cancelar = MotionEvent.obtain(e);
                        cancelar.setAction(MotionEvent.ACTION_CANCEL);
                        super.dispatchTouchEvent(cancelar);
                        cancelar.recycle();
                        marcarTodosLeidos();
                        return true;
                    }
                }
                break;
            case MotionEvent.ACTION_POINTER_UP:
                if (e.getPointerCount() <= 2) barriendo = false;
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                barriendo = false;
                if (barrido) {
                    barrido = false;
                    return true;
                }
                break;
        }
        if (barrido) return true; // el resto del gesto ya no llega al carrusel
        return super.dispatchTouchEvent(e);
    }

    /** Media de la X (o la Y) de los dedos que hay en pantalla. */
    private static float media(MotionEvent e, boolean x) {
        float suma = 0;
        for (int i = 0; i < e.getPointerCount(); i++) suma += x ? e.getX(i) : e.getY(i);
        return suma / e.getPointerCount();
    }

    // -------------------------------------------------
    //              GUARDAR (como en Actividades)
    // -------------------------------------------------

    /** Inclinar el móvil hacia ti: guarda (o quita) el aviso que se está viendo. En el tutorial, solo se practica. */
    private void alTirarHaciaTi() {
        if (visibles.isEmpty() || !hasWindowFocus()) return;
        if (hayTutorialActivo()) {
            avisarTutorial(Gesto.HACIA_TI);
            levantarFicha();
            Snackbar.make(raiz, R.string.nui_not_practica_guardar, Snackbar.LENGTH_SHORT).show();
            return;
        }
        alternarGuardado(visibles.get(vpAnuncios.getCurrentItem()));
    }

    /** El aviso sube un poco hacia ti y vuelve (se ve que el gesto ha contado). */
    private void levantarFicha() {
        float dp = getResources().getDisplayMetrics().density;
        vpAnuncios.animate().translationY(-24 * dp).setDuration(140)
                .withEndAction(() -> vpAnuncios.animate().translationY(0).setDuration(160).start()).start();
    }

    /** Marcapáginas o inclinar hacia ti: guarda el aviso o, si ya estaba, lo quita (con "Deshacer"). */
    private void alternarGuardado(Anuncio a) {
        boolean guardar = !a.guardado;
        ponerGuardado(a, guardar);
        if (guardar) levantarFicha();
        Snackbar.make(raiz, getString(guardar ? R.string.nui_not_aviso_guardado_fmt : R.string.nui_not_aviso_quitado_fmt, a.titulo), 5000)
                .setActionTextColor(getColor(R.color.nui_acento))
                .setAction(R.string.deshacer, x -> ponerGuardado(a, !guardar))
                .show();
    }

    private void ponerGuardado(Anuncio a, boolean si) {
        AnunciosGuardados.poner(this, correo, a.id, si);
        a.guardado = si;
        if (FILTRO_GUARDADOS.equals(filtroSeleccionado)) {
            // en «Guardados», el que se quita desaparece (y el que se recupera con "Deshacer" vuelve)
            int antes = vpAnuncios.getCurrentItem();
            aplicarFiltro();
            if (!visibles.isEmpty()) vpAnuncios.setCurrentItem(Math.min(antes, visibles.size() - 1), false);
        } else {
            int pos = visibles.indexOf(a);
            if (pos >= 0) adaptador.notifyItemChanged(pos);
        }
    }

    private void marcarLeido(Anuncio a) {
        if (a.leido) return;
        AnunciosLeidos.marcar(this, a.id);
        a.leido = true;
        int pos = visibles.indexOf(a);
        if (pos >= 0) adaptador.notifyItemChanged(pos);
        actualizarContador();
    }

    // -------------------------------------------------
    //              INCLINAR
    // -------------------------------------------------

    /** Inclinar a la izquierda = siguiente aviso (como "pasar" en Actividades); a la derecha = el anterior. */
    private void alInclinar(int direccion) {
        int destino = vpAnuncios.getCurrentItem() + (direccion < 0 ? 1 : -1);
        if (destino >= 0 && destino < visibles.size()) vpAnuncios.setCurrentItem(destino, true);
    }

    // -------------------------------------------------
    //              DETALLE (hoja inferior)
    // -------------------------------------------------

    private void mostrarDetalle(Anuncio a) {
        BottomSheetDialog hoja = new BottomSheetDialog(this);
        View v = getLayoutInflater().inflate(R.layout.detalle_anuncio, null);
        hoja.setContentView(v);

        ImageView ivIcono = v.findViewById(R.id.ivIconoDetalle);
        ivIcono.setImageResource(Anuncio.iconoRes(a.categoria));
        ivIcono.setImageTintList(ColorStateList.valueOf(getColor(Anuncio.colorRes(a.categoria))));
        ivIcono.setBackgroundTintList(ColorStateList.valueOf(getColor(Anuncio.tinteRes(a.categoria))));
        ((TextView) v.findViewById(R.id.tvCategoriaDetalle)).setText(Anuncio.nombreCategoria(this, a.categoria));
        v.findViewById(R.id.tvUrgenteDetalle).setVisibility(a.esUrgente() ? View.VISIBLE : View.GONE);

        ((TextView) v.findViewById(R.id.tvTituloDetalle)).setText(a.titulo);
        ((TextView) v.findViewById(R.id.tvFechaDetalle)).setText(getString(R.string.nui_not_publicado_fmt, FechasUtil.hace(this, a.publicacion), FechasUtil.diaLargo(a.publicacion)));
        ((TextView) v.findViewById(R.id.tvContenidoDetalle)).setText(a.contenido);

        if (a.caducidad != null) {
            TextView tvCaducidad = v.findViewById(R.id.tvCaducidadDetalle);
            tvCaducidad.setVisibility(View.VISIBLE);
            tvCaducidad.setText(getString(R.string.nui_not_valido_hasta_fmt, FechasUtil.diaLargo(a.caducidad)));
        }

        if (a.enlace != null && !a.enlace.isEmpty()) {
            View btnEnlace = v.findViewById(R.id.btnEnlace);
            btnEnlace.setVisibility(View.VISIBLE);
            btnEnlace.setOnClickListener(x -> {
                try {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(a.enlace)));
                } catch (ActivityNotFoundException e) {
                    Snackbar.make(raiz, R.string.nui_sin_navegador, Snackbar.LENGTH_SHORT).show();
                }
            });
        }

        hoja.show();
        marcarLeido(a); // abrirlo entero también cuenta como leído
    }

    // -------------------------------------------------
    //              TUTORIAL (util/tutorial, de Izaro)
    // -------------------------------------------------

    /** Lo enseña BaseActivity la primera vez; el botón "?" de la barra lo repite. */
    @Override
    protected Tutorial crearTutorial() {
        return new Tutorial(this, "noticias", 8)
                .info(R.id.pestanasCategorias, getString(R.string.nui_tut_not_pestanas))
                .gesto(R.id.vpAnuncios, Gesto.DESLIZAR_IZQ, getString(R.string.nui_tut_not_deslizar))
                .soloSi(() -> visibles.size() > 1 && vpAnuncios.getCurrentItem() < visibles.size() - 1)
                // Sensores: se prueban de verdad, como los pasos de sensor de Izaro
                .sensor(Gesto.INCLINAR, getString(R.string.nui_tut_not_inclinar))
                .soloSi(() -> visibles.size() > 1 && inclinar.escuchando())
                .sensor(Gesto.HACIA_TI, getString(R.string.nui_tut_not_hacia_ti))
                .soloSi(() -> !visibles.isEmpty() && haciaTi.disponible())
                .sensor(Gesto.MANO, getString(R.string.nui_tut_not_mano))
                .soloSi(() -> visibles.size() > 1 && gestoMano.disponible())
                .info(R.id.tvMarcarTodo, getString(R.string.nui_tut_not_marcar))
                .soloSiSeVe()
                // el último: al tocar se abre el aviso entero encima
                .tocar(R.id.vpAnuncios, getString(R.string.nui_tut_not_tocar))
                .soloSi(() -> !visibles.isEmpty());
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
