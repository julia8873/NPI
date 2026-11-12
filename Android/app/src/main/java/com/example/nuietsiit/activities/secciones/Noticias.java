package com.example.nuietsiit.activities.secciones;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.viewpager2.widget.ViewPager2;

import com.example.nuietsiit.R;
import com.example.nuietsiit.activities.BaseActivity;
import com.example.nuietsiit.activities.secciones.noticias.AnuncioAdapter;
import com.example.nuietsiit.activities.secciones.noticias.LectorAvisos;
import com.example.nuietsiit.data.AnunciosDB;
import com.example.nuietsiit.data.AnunciosLeidos;
import com.example.nuietsiit.data.DBHelper;
import com.example.nuietsiit.data.modelo.Anuncio;
import com.example.nuietsiit.ui.CabeceraNui;
import com.example.nuietsiit.ui.PestanasPapel;
import com.example.nuietsiit.util.FechasUtil;
import com.example.nuietsiit.util.tutorial.Gesto;
import com.example.nuietsiit.util.tutorial.Tutorial;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Pantalla de Noticias SIN SCROLL: carrusel horizontal con una ficha grande por aviso.
 * Diseño NUI (DisenoNUI/DESIGN.md), igual que Secretaría y Biblioteca: yesería + arquería, pestañas de papel,
 * fichas con tesela, sello "Urgente" y zócalo. Nada de colores ni tamaños a mano.
 * - Los urgentes van primero, con sello granada.
 * - Un aviso cuenta como leído si te quedas en él un momento o lo abres entero.
 * - Tocar la tarjeta = aviso completo en una hoja (con su enlace si tiene).
 * - VOZ Y SENSORES (LectorAvisos): el altavoz lee en voz alta los avisos sin leer y el carrusel los va pasando.
 *   Acercarse el móvil a la oreja (sensor de proximidad) los lee por el auricular, como un audio;
 *   ponerlo boca abajo lo calla.
 */
public class Noticias extends BaseActivity {

    private static final String FILTRO_TODOS = "__todos";
    private static final String FILTRO_NO_LEIDOS = "__noleidos";
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

    private LectorAvisos lector;
    private MaterialButton btnEscuchar;
    private List<Anuncio> cola = new ArrayList<>(); // los que se están leyendo en voz alta

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.noticias);
        overridePendingTransition(R.anim.nui_papel_entra, R.anim.nui_quieto);

        // Barra de yesería (como Secretaría y Biblioteca): atrás, título, chat y perfil
        MaterialToolbar barra = findViewById(R.id.toolbar);
        setSupportActionBar(barra);
        CabeceraNui.preparar(barra);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Noticias y avisos");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        anunciosDB = new AnunciosDB(DBHelper.getInstance(this));

        raiz = findViewById(android.R.id.content);
        tvSinLeer = findViewById(R.id.tvSinLeer);
        tvMarcarTodo = findViewById(R.id.tvMarcarTodo);
        tvVacio = findViewById(R.id.tvVacio);
        tvPagina = findViewById(R.id.tvPagina);

        // Carrusel: se ve un trozo de la tarjeta anterior y la siguiente
        adaptador = new AnuncioAdapter(this::mostrarDetalle);
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

        // Leer en voz alta (botón o acercándose el móvil a la oreja)
        btnEscuchar = findViewById(R.id.btnEscuchar);
        lector = new LectorAvisos(this, new LectorAvisos.Oyente() {
            @Override public void alLeer(int indice) {
                if (indice >= cola.size()) return;
                Anuncio a = cola.get(indice);
                int pos = visibles.indexOf(a);
                if (pos >= 0) vpAnuncios.setCurrentItem(pos, true);
                marcarLeido(a);
            }

            @Override public void alTerminar() {
                pintarBotonEscuchar();
            }

            @Override public void alAcercarOreja() {
                empezarLectura();
            }

            @Override public void alCambiarOreja(boolean cerca) {
                if (cerca) avisarTutorial(Gesto.OREJA);
                pintarBotonEscuchar();
            }

            @Override public void alBocaAbajo() {
                pintarBotonEscuchar();
                avisarTutorial(Gesto.BOCA_ABAJO);
                Snackbar.make(raiz, "Boca abajo: dejo de leer", Snackbar.LENGTH_SHORT).show();
            }
        });
        btnEscuchar.setOnClickListener(v -> {
            if (lector.leyendo()) lector.parar();
            else empezarLectura();
        });

        tvMarcarTodo.setOnClickListener(v -> {
            List<Integer> ids = new ArrayList<>();
            for (Anuncio a : todos) ids.add(a.id);
            AnunciosLeidos.marcarVarios(this, ids);
            for (Anuncio a : todos) a.leido = true;
            adaptador.notifyDataSetChanged();
            actualizarContador();
        });

        cargarAnuncios();
    }

    @Override
    protected void onResume() {
        super.onResume();
        lector.iniciarSensores();
    }

    @Override
    protected void onDestroy() {
        lector.cerrar();
        super.onDestroy();
    }

    @Override
    protected void onPause() {
        super.onPause();
        lector.pararSensores(); // también para la lectura
        if (marcarPendiente != null) reloj.removeCallbacks(marcarPendiente);
    }

    // -------------------------------------------------
    //              CARGA Y FILTROS
    // -------------------------------------------------

    private void cargarAnuncios() {
        todos = anunciosDB.obtenerAnunciosGenerales(FechasUtil.ahoraBD());
        Set<String> leidos = AnunciosLeidos.leidos(this);
        for (Anuncio a : todos) a.leido = leidos.contains(String.valueOf(a.id));
        aplicarFiltro();
    }

    /**
     * Pestañas de papel (ui/PestanasPapel, de Izaro) en dos filas, todas a la vista:
     * Todos · Sin leer · Plazos / Becas · Transporte · General
     */
    private void crearPestanas() {
        PestanasPapel pestanas = findViewById(R.id.pestanasCategorias);
        pestanas.nuevaFila();
        pestanas.anadir(FILTRO_TODOS, "Todos", null, 0, 0);
        pestanas.anadir(FILTRO_NO_LEIDOS, "Sin leer", null, 0, 0);
        for (int i = 0; i < Anuncio.CATEGORIAS.length; i++) {
            if (i == 1) pestanas.nuevaFila(); // 3 arriba y 3 abajo
            String cat = Anuncio.CATEGORIAS[i][0];
            pestanas.anadir(cat, Anuncio.CATEGORIAS[i][1], null, 0, 0);
        }
        pestanas.completarFila(3);
        pestanas.marcar(FILTRO_TODOS);
        pestanas.setOnElegir(clave -> {
            filtroSeleccionado = FILTRO_TODOS.equals(clave) ? null : (String) clave;
            aplicarFiltro();
        });
    }

    private void aplicarFiltro() {
        visibles = new ArrayList<>();
        for (Anuncio a : todos) {
            if (filtroSeleccionado == null
                    || (FILTRO_NO_LEIDOS.equals(filtroSeleccionado) && !a.leido)
                    || filtroSeleccionado.equals(a.categoria)) {
                visibles.add(a);
            }
        }
        adaptador.setAnuncios(visibles);
        vpAnuncios.setCurrentItem(0, false);
        ultimaPagina = 0;
        tvVacio.setVisibility(visibles.isEmpty() ? View.VISIBLE : View.GONE);
        actualizarContador();
        actualizarPagina();
        if (!visibles.isEmpty()) programarLeido(0);
    }

    private void actualizarContador() {
        int sinLeer = 0;
        for (Anuncio a : todos) if (!a.leido) sinLeer++;
        if (sinLeer == 0) {
            tvSinLeer.setText("Estás al día");
        } else {
            tvSinLeer.setText(sinLeer + (sinLeer == 1 ? " aviso sin leer" : " avisos sin leer"));
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
        tvPagina.setContentDescription("Aviso " + (actual + 1) + " de " + visibles.size());
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

    private void marcarLeido(Anuncio a) {
        if (a.leido) return;
        AnunciosLeidos.marcar(this, a.id);
        a.leido = true;
        int pos = visibles.indexOf(a);
        if (pos >= 0) adaptador.notifyItemChanged(pos);
        actualizarContador();
    }

    // -------------------------------------------------
    //              LEER EN VOZ ALTA
    // -------------------------------------------------

    /** Lee los avisos sin leer de lo que se está viendo; si están todos leídos, todos desde el actual. */
    private void empezarLectura() {
        if (!lector.disponible()) {
            Snackbar.make(raiz, "Este móvil no tiene voz en español para leer los avisos", Snackbar.LENGTH_LONG).show();
            return;
        }
        cola = new ArrayList<>();
        for (Anuncio a : visibles) if (!a.leido) cola.add(a);
        if (cola.isEmpty()) cola.addAll(visibles.subList(vpAnuncios.getCurrentItem(), visibles.size()));
        if (cola.isEmpty()) return;

        List<String> textos = new ArrayList<>();
        for (Anuncio a : cola) {
            textos.add((a.esUrgente() ? "Urgente. " : "") + Anuncio.nombreCategoria(a.categoria) + ". "
                    + a.titulo + ". " + a.contenido);
        }
        lector.leer(textos);
        pintarBotonEscuchar();
    }

    private void pintarBotonEscuchar() {
        boolean leyendo = lector.leyendo();
        btnEscuchar.setIconResource(leyendo ? R.drawable.nui_ic_parar : R.drawable.nui_ic_altavoz);
        btnEscuchar.setContentDescription(leyendo ? "Dejar de leer" : "Escuchar los avisos");
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
        ((TextView) v.findViewById(R.id.tvCategoriaDetalle)).setText(Anuncio.nombreCategoria(a.categoria));
        v.findViewById(R.id.tvUrgenteDetalle).setVisibility(a.esUrgente() ? View.VISIBLE : View.GONE);

        ((TextView) v.findViewById(R.id.tvTituloDetalle)).setText(a.titulo);
        ((TextView) v.findViewById(R.id.tvFechaDetalle)).setText("Publicado " + FechasUtil.hace(a.publicacion)
                + " · " + FechasUtil.diaLargo(a.publicacion));
        ((TextView) v.findViewById(R.id.tvContenidoDetalle)).setText(a.contenido);

        if (a.caducidad != null) {
            TextView tvCaducidad = v.findViewById(R.id.tvCaducidadDetalle);
            tvCaducidad.setVisibility(View.VISIBLE);
            tvCaducidad.setText("Válido hasta el " + FechasUtil.diaLargo(a.caducidad));
        }

        if (a.enlace != null && !a.enlace.isEmpty()) {
            View btnEnlace = v.findViewById(R.id.btnEnlace);
            btnEnlace.setVisibility(View.VISIBLE);
            btnEnlace.setOnClickListener(x -> {
                try {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(a.enlace)));
                } catch (ActivityNotFoundException e) {
                    Snackbar.make(raiz, "No hay navegador para abrir el enlace", Snackbar.LENGTH_SHORT).show();
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
        return new Tutorial(this, "noticias", 3)
                .info(R.id.pestanasCategorias, "Filtra los avisos: los que no has leído, plazos, becas, transporte...")
                .gesto(R.id.vpAnuncios, Gesto.DESLIZAR_IZQ, "Desliza a la izquierda para pasar al siguiente aviso. "
                        + "Si te quedas un momento en uno, cuenta como leído")
                .soloSi(() -> visibles.size() > 1 && vpAnuncios.getCurrentItem() < visibles.size() - 1)
                // Voz y sensores: se prueban de verdad, como los pasos de sensor de Izaro
                .tocar(R.id.btnEscuchar, "Toca el altavoz y te leo en voz alta los avisos sin leer")
                .soloSi(() -> lector.disponible() && !visibles.isEmpty())
                .sensor(Gesto.BOCA_ABAJO, "Ahora pon el móvil boca abajo: me callo")
                .soloSi(() -> lector.leyendo())
                .sensor(Gesto.OREJA, "Acércate el móvil a la oreja, como para escuchar un audio: "
                        + "te leo los avisos por el auricular. Al separarlo, paro")
                .soloSi(() -> lector.disponible() && lector.hayProximidad() && !visibles.isEmpty())
                .info(R.id.tvMarcarTodo, "¿Ya te has enterado de todo? Márcalos todos como leídos de una vez")
                .soloSiSeVe()
                // el último: al tocar se abre el aviso entero encima
                .tocar(R.id.vpAnuncios, "Toca la ficha para leer el aviso entero y abrir su enlace")
                .soloSi(() -> !visibles.isEmpty());
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
