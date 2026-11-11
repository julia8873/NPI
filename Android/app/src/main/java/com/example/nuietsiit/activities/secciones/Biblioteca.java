package com.example.nuietsiit.activities.secciones;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.HapticFeedbackConstants;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityOptionsCompat;
import androidx.core.app.SharedElementCallback;
import androidx.core.view.OneShotPreDrawListener;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.nuietsiit.R;
import com.example.nuietsiit.activities.secciones.biblioteca.PantallaBiblioteca;
import com.example.nuietsiit.activities.secciones.biblioteca.ApuntarLibros;
import com.example.nuietsiit.activities.secciones.biblioteca.BaldaDecoration;
import com.example.nuietsiit.activities.secciones.biblioteca.DetalleLibro;
import com.example.nuietsiit.activities.secciones.biblioteca.FiltroExplorar;
import com.example.nuietsiit.activities.secciones.biblioteca.GravedadBalda;
import com.example.nuietsiit.activities.secciones.biblioteca.SugerenciaEstudio;
import com.example.nuietsiit.activities.secciones.biblioteca.ModoEstudio;
import com.example.nuietsiit.activities.secciones.biblioteca.LibroAdapter;
import com.example.nuietsiit.util.tutorial.Gesto;
import com.example.nuietsiit.util.tutorial.Tutorial;
import com.example.nuietsiit.activities.secciones.biblioteca.PanelExplorar;
import com.example.nuietsiit.data.BibliotecaDB;
import com.example.nuietsiit.data.DBHelper;
import com.example.nuietsiit.data.modelo.AsignaturaCatalogo;
import com.example.nuietsiit.data.modelo.Libro;
import com.example.nuietsiit.util.BuscadorTolerante;
import com.example.nuietsiit.util.ProximaClase;
import com.example.nuietsiit.util.SesionUtil;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.chip.Chip;
import com.example.nuietsiit.ui.InclinarParaPasar;
import com.example.nuietsiit.ui.PestanasPapel;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiPredicate;

public class Biblioteca extends PantallaBiblioteca {

    private static final float UMBRAL_PELLIZCO = 1.3f; // cuánto hay que abrir o cerrar los dedos para cambiar de tamaño
    private static final int UMBRAL_SACAR_DP = 80;     // cuánto hay que subir el dedo para abrir el libro que se ojea
    private static final int UMBRAL_DESLIZAR_DP = 80;  // cuánto hay que deslizar en horizontal para pasar de estante
    private static final float ARRASTRE_DESLIZAR = 0.35f; // las baldas siguen al dedo más despacio que él

    // la vista es el tag del chip marcado: una de estas o un código de asignatura
    private static final String VISTA_MIS = "mis";
    private static final String VISTA_TODOS = "todos";

    private EditText etBuscar;
    private TextView tvMostrandoPara, tvVacio, tvContador;
    private Chip chipSugerencia, chipDisponibles;
    private PestanasPapel pestanasVista;
    private final List<String> vistas = new ArrayList<>(); // en el orden de las pestañas (sin Explorar)
    private String vistaActual;                            // null = manda el filtro de Explorar
    private static final String EXPLORAR = "explorar";
    private LibroAdapter adaptador;
    private GridLayoutManager rejilla;
    private RecyclerView rvLibros;
    private BaldaDecoration baldas;

    // sin scroll: los libros de la vista se reparten en estantes (lo que cabe en pantalla) que se pasan
    // deslizando de lado o inclinando el móvil; tras el último estante viene la siguiente pestaña
    private List<Libro> librosVista = new ArrayList<>();
    private Set<Integer> basicosVista = Collections.emptySet();
    private int estante, librosPorEstante;
    private String textoContador = "";
    private boolean irAlUltimoEstante; // al volver a la pestaña anterior se entra por su último estante
    private boolean pasandoEstante;    // el cambio de vista viene de pasar: sin animación de recolocar
    private InclinarParaPasar inclinar;
    private GravedadBalda gravedad;

    private SugerenciaEstudio sugerencia; // móvil dejado sobre la mesa con la balda abierta: ¿vas a estudiar?
    private BuscadorTolerante.Indice<Libro> indice;
    private List<Libro> libros;
    private Map<Integer, Map<String, String>> bibliografia; // idLibro -> asignatura -> tipo
    private List<AsignaturaCatalogo> catalogo;
    private final Map<String, String> nombresAsignatura = new HashMap<>();
    private List<String> misAsignaturas;
    private ProximaClase proximaClase;
    private FiltroExplorar filtro;
    private PanelExplorar panelExplorar;

    private int columnasMin, columnasMax;

    // ojear: libro sacado de la balda mientras se mantiene el dedo
    private View panelOjear, tarjetaOjear;
    private Libro libroOjeado;
    private ImageView portadaOjeada;
    private float yInicioDedo;
    private boolean dedoEnBaldas;
    private boolean sobreUmbralSacar;
    private static final float GIRO_OJEAR = -3f;       // el libro sacado flota un poco torcido
    private ImageView portadaRetorno;                   // a dónde vuelve la portada al cerrar el detalle
    private boolean abriendoDetalle;

    // deslizar en horizontal sobre las baldas: cambiar de asignatura
    private float xInicioDedo;
    private boolean deslizando;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.biblioteca);

        // Botón atrás en la barra superior
        MaterialToolbar barra = findViewById(R.id.toolbar);
        setSupportActionBar(barra);
        getSupportActionBar().setTitle("Biblioteca");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // al volver del detalle, la portada regresa a su libro de la balda (el panel de ojear ya no está)
        setExitSharedElementCallback(new SharedElementCallback() {
            @Override
            public void onMapSharedElements(List<String> nombres, Map<String, View> vistas) {
                if (portadaRetorno != null && panelOjear.getVisibility() != View.VISIBLE) {
                    for (String nombre : nombres) vistas.put(nombre, portadaRetorno);
                }
            }
        });

        etBuscar = findViewById(R.id.etBuscar);
        tvMostrandoPara = findViewById(R.id.tvMostrandoPara);
        tvVacio = findViewById(R.id.tvVacio);
        tvContador = findViewById(R.id.tvContador);
        chipSugerencia = findViewById(R.id.chipSugerencia);
        chipDisponibles = findViewById(R.id.chipDisponibles);
        pestanasVista = findViewById(R.id.pestanasVista);
        panelOjear = findViewById(R.id.panelOjear);
        tarjetaOjear = findViewById(R.id.tarjetaOjear);

        // se cargan todos y se filtra en Java (sin LIKE): tolera faltas y no hay que escapar % ni _
        BibliotecaDB db = new BibliotecaDB(DBHelper.getInstance(this));
        libros = db.buscarLibros(null);
        indice = BuscadorTolerante.crearIndice(libros, l -> l.titulo, Libro::textoBusqueda);
        bibliografia = db.getBibliografiaPorLibro();
        catalogo = db.getCatalogoAsignaturas();
        for (AsignaturaCatalogo a : catalogo) nombresAsignatura.put(a.codigo, a.nombre);
        filtro = new FiltroExplorar(catalogo);

        configurarBaldas();
        crearChipsVista(db, savedInstanceState);

        etBuscar.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void afterTextChanged(Editable s) {
                aplicarFiltro();
                if (s.length() >= 3) avisarTutorial(Gesto.ESCRIBIR);
            }
        });
        chipSugerencia.setOnClickListener(v -> etBuscar.setText(chipSugerencia.getTag().toString()));
        findViewById(R.id.btnApuntar).setOnClickListener(v -> startActivity(new Intent(this, ApuntarLibros.class)
                .putExtra("CORREO", getIntent().getStringExtra("CORREO"))));
        findViewById(R.id.btnModoEstudio).setOnClickListener(v -> abrirModoEstudio());
        // en el tutorial dejarlo en la mesa es un paso: no se propone estudiar
        sugerencia = SugerenciaEstudio.enLaBalda(this, findViewById(R.id.marcoBalda),
                () -> libroOjeado != null || hayTutorialActivo());

        aplicarFiltro();
        // al entrar, los libros se balancean un poco: están sueltos y se mueven al inclinar el móvil
        if (savedInstanceState == null) {
            rvLibros.getViewTreeObserver().addOnGlobalLayoutListener(new android.view.ViewTreeObserver.OnGlobalLayoutListener() {
                @Override public void onGlobalLayout() {
                    if (rvLibros.getChildCount() == 0) return; // aún no hay libros: esperar
                    rvLibros.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                    rvLibros.postDelayed(() -> gravedad.balancear(), 200); // con la transición de entrada ya casi terminada
                }
            });
        }

    }

    @Override
    protected Tutorial crearTutorial() {
        return new Tutorial(this, "biblioteca", 2)
                .info(R.id.pestanasVista, "Cada pestaña es una estantería: tus asignaturas, la de tu próxima clase y todo"
                        + " el catálogo. En Explorar eliges grado, curso o planta")
                .gesto(R.id.etBuscar, Gesto.ESCRIBIR, "Escribe un título o un autor, aunque sea con faltas: te entendemos igual")
                .tocar(R.id.chipSugerencia, "¿Era esto lo que querías decir? Tócalo para buscarlo").soloSiSeVe()
                .tocar(R.id.chipDisponibles, "Toca Disponibles para ver solo los libros que te puedes llevar")
                .gesto(R.id.rvLibros, Gesto.DESLIZAR_IZQ, "Desliza las baldas a la izquierda para pasar de estante")
                .alLlegar(this::quitarFiltrosTutorial)
                .gesto(R.id.rvLibros, Gesto.PELLIZCAR, "Pellizca la balda para ver más libros a la vez, o menos y más grandes")
                .sensor(Gesto.INCLINAR, "Inclina el móvil a un lado para pasar de estante. Los libros están sueltos: se mueven")
                .soloSi(() -> numEstantes() > 1 || vistas.size() > 1)
                .gesto(R.id.ivPortada, Gesto.MANTENER, "Mantén el dedo en una portada para sacar el libro y ojearlo."
                        + " Suelta y vuelve a su sitio")
                .info(R.id.btnApuntar, "Escanea la estantería con la cámara: cada libro te dice si está libre o es de tu asignatura")
                .info(R.id.btnModoEstudio, "Modo estudio: 25 minutos sin distracciones con el móvil sobre la mesa")
                .sensor(Gesto.MESA, "Prueba: deja el móvil sobre la mesa, boca arriba. Al acabar el tutorial te propondrá estudiar")
                .tocar(R.id.ivPortada, "Toca una portada (o súbela mientras la ojeas) para abrir el libro");
    }

    // tras practicar la búsqueda y Disponibles, la balda vuelve a enseñarlo todo para los demás pasos
    private void quitarFiltrosTutorial() {
        android.view.inputmethod.InputMethodManager teclado = getSystemService(android.view.inputmethod.InputMethodManager.class);
        if (teclado != null) teclado.hideSoftInputFromWindow(etBuscar.getWindowToken(), 0);
        etBuscar.clearFocus();
        chipDisponibles.setChecked(false);
        etBuscar.setText("");
    }

    private void configurarBaldas() {
        // en horizontal caben más libros por balda
        int anchoDp = getResources().getConfiguration().screenWidthDp;
        int columnas = Math.max(3, anchoDp / 130);
        columnasMin = columnas - 1;
        columnasMax = columnas + 1;

        adaptador = new LibroAdapter(new LibroAdapter.OnLibroGesto() {
            @Override public void alPulsar(Libro libro, ImageView portada) { abrirDetalle(libro, portada); }
            @Override
            public void alMantener(Libro libro, ImageView portada) {
                // sin dedo en pantalla (TalkBack, teclado) nada podría cerrar el panel: se abre el libro
                if (dedoEnBaldas) empezarOjear(libro, portada);
                else abrirDetalle(libro, portada);
            }
        });
        rejilla = new GridLayoutManager(this, columnas) {
            @Override
            public boolean canScrollVertically() {
                return false; // sin scroll: se pasa de estante
            }
        };
        RecyclerView rv = findViewById(R.id.rvLibros);
        rvLibros = rv;
        rv.setLayoutManager(rejilla);
        rv.setAdapter(adaptador);
        baldas = new BaldaDecoration(this);
        rv.addItemDecoration(baldas);
        // al abrir el teclado o girar la pantalla caben otros libros por estante
        rv.addOnLayoutChangeListener((v, l, t, r, b, lViejo, tViejo, rViejo, bViejo) -> {
            if (r - l != rViejo - lViejo || b - t != bViejo - tViejo) v.post(() -> repartirEstantes(false));
        });
        inclinar = new InclinarParaPasar(rv, sentido -> {
            avisarTutorial(Gesto.INCLINAR);
            pasar(sentido);
        }, () -> numEstantes() > 1 || vistas.size() > 1);
        gravedad = new GravedadBalda(rv);

        ScaleGestureDetector pellizco = new ScaleGestureDetector(this, new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            private float acumulado = 1f;

            @Override
            public boolean onScaleBegin(@NonNull ScaleGestureDetector d) {
                acumulado = 1f;
                return true;
            }

            @Override
            public boolean onScale(@NonNull ScaleGestureDetector d) {
                acumulado *= d.getScaleFactor();
                if (acumulado > UMBRAL_PELLIZCO) {
                    cambiarColumnas(rv, -1);
                    acumulado = 1f;
                } else if (acumulado < 1f / UMBRAL_PELLIZCO) {
                    cambiarColumnas(rv, 1);
                    acumulado = 1f;
                }
                return true;
            }
        });

        rv.addOnItemTouchListener(new RecyclerView.SimpleOnItemTouchListener() {
            @Override
            public boolean onInterceptTouchEvent(@NonNull RecyclerView r, @NonNull MotionEvent e) {
                // mientras se ojea no se pellizca: cambiar columnas reasignaría la portada sujeta
                if (libroOjeado == null) pellizco.onTouchEvent(e);
                actualizarDedo(e);
                if (libroOjeado != null) {
                    seguirOjear(e);
                    return true;
                }
                if (e.getPointerCount() > 1) return true;
                return empezarDeslizar(r, e);
            }

            @Override
            public void onTouchEvent(@NonNull RecyclerView r, @NonNull MotionEvent e) {
                if (libroOjeado == null) pellizco.onTouchEvent(e);
                actualizarDedo(e);
                if (libroOjeado != null) seguirOjear(e);
                if (deslizando) seguirDeslizar(r, e);
            }
        });
    }

    private void actualizarDedo(MotionEvent e) {
        int accion = e.getActionMasked();
        if (accion == MotionEvent.ACTION_DOWN) {
            dedoEnBaldas = true;
            gravedad.setPausada(true); // con el dedo encima, los libros se quedan derechos
            yInicioDedo = e.getRawY();
            xInicioDedo = e.getRawX();
        } else if (accion == MotionEvent.ACTION_UP || accion == MotionEvent.ACTION_CANCEL) {
            dedoEnBaldas = false;
            gravedad.setPausada(false);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        gravedad.iniciar();
    }

    private void abrirModoEstudio() {
        startActivity(new Intent(this, ModoEstudio.class)
                .putExtra("CORREO", getIntent().getStringExtra("CORREO")));
    }

    // tocar la pantalla = se está usando la balda: descarta el "¿Vas a estudiar?" (salvo si se toca el aviso)
    @Override
    public boolean dispatchTouchEvent(MotionEvent e) {
        sugerencia.alTocar(e);
        return super.dispatchTouchEvent(e);
    }

    @Override
    protected void onPause() {
        super.onPause();
        // si otra pantalla se pone delante, no llegará el evento de soltar
        if (libroOjeado != null) terminarOjear();
        gravedad.parar();
    }

    @Override
    protected void onStop() {
        super.onStop();
        // el detalle ya tapa la pantalla: se recoge el panel sin animación y el hueco vuelve a tener su libro
        if (abriendoDetalle) {
            abriendoDetalle = false;
            panelOjear.setVisibility(View.GONE);
            restaurarTarjeta();
            if (portadaRetorno != null) portadaRetorno.setAlpha(1f);
        }
    }

    // si el dedo va claramente de lado, las baldas se lo quedan: así no se abre el libro que había debajo
    private boolean empezarDeslizar(RecyclerView rv, MotionEvent e) {
        if (e.getActionMasked() != MotionEvent.ACTION_MOVE || rv.getScrollState() != RecyclerView.SCROLL_STATE_IDLE) return false;
        float dx = Math.abs(e.getRawX() - xInicioDedo);
        float dy = Math.abs(e.getRawY() - yInicioDedo);
        deslizando = dx > ViewConfiguration.get(this).getScaledTouchSlop() && dx > 2 * dy;
        return deslizando;
    }

    private void seguirDeslizar(RecyclerView rv, MotionEvent e) {
        float dx = e.getRawX() - xInicioDedo;
        // un segundo dedo convierte el gesto en otra cosa: se abandona
        boolean abandonado = e.getPointerCount() > 1 || e.getActionMasked() == MotionEvent.ACTION_CANCEL;
        if (e.getActionMasked() == MotionEvent.ACTION_MOVE && !abandonado) {
            rv.setTranslationX(dx * ARRASTRE_DESLIZAR);
        } else if (e.getActionMasked() == MotionEvent.ACTION_UP || abandonado) {
            deslizando = false;
            float umbral = UMBRAL_DESLIZAR_DP * getResources().getDisplayMetrics().density;
            if (!abandonado && Math.abs(dx) > umbral) {
                avisarTutorial(dx < 0 ? Gesto.DESLIZAR_IZQ : Gesto.DESLIZAR_DER);
                pasar(dx < 0 ? 1 : -1);
            }
            else rv.animate().translationX(0).setDuration(160).start();
        }
    }

    // ---------- estantes: lo que cabe en pantalla, sin scroll ----------

    // mide un libro con el ancho de su columna: cada fila es el libro más su balda;
    // los libros tienen que quedar por encima del botón flotante (su balda puede quedar detrás)
    private int calcularLibrosPorEstante() {
        int ancho = rvLibros.getWidth() - rvLibros.getPaddingLeft() - rvLibros.getPaddingRight();
        int alto = findViewById(R.id.btnApuntar).getTop() - rvLibros.getPaddingTop() + baldas.altoBalda();
        if (ancho <= 0 || alto <= 0) return 0;
        int columnas = rejilla.getSpanCount();
        View muestra = LayoutInflater.from(this).inflate(R.layout.item_libro, rvLibros, false);
        muestra.measure(View.MeasureSpec.makeMeasureSpec(ancho / columnas, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        int altoFila = muestra.getMeasuredHeight() + baldas.altoExtra();
        return columnas * Math.max(1, alto / altoFila);
    }

    private int numEstantes() {
        if (librosPorEstante <= 0) return 1;
        return Math.max(1, (librosVista.size() + librosPorEstante - 1) / librosPorEstante);
    }

    // pone en las baldas los libros del estante actual
    private void repartirEstantes(boolean animar) {
        librosPorEstante = calcularLibrosPorEstante();
        if (irAlUltimoEstante) {
            estante = Integer.MAX_VALUE;
            irAlUltimoEstante = false;
        }
        estante = Math.max(0, Math.min(estante, numEstantes() - 1));
        List<Libro> enBaldas = librosVista;
        if (librosPorEstante > 0) {
            int desde = estante * librosPorEstante;
            enBaldas = new ArrayList<>(librosVista.subList(desde, Math.min(librosVista.size(), desde + librosPorEstante)));
        }
        if (animar) adaptador.setLibros(enBaldas, basicosVista);
        else adaptador.mostrar(enBaldas, basicosVista);

        int estantes = numEstantes();
        tvContador.setText(estantes > 1
                ? textoContador + " · estante " + (estante + 1) + " de " + estantes
                : textoContador);
        inclinar.actualizar();
    }

    // deslizar o inclinar hacia la izquierda = siguiente estante; tras el último, la pestaña siguiente
    private void pasar(int sentido) {
        if (libroOjeado != null) return; // con un libro en la mano no se pasa
        int nuevo = estante + sentido;
        boolean hayEstante = nuevo >= 0 && nuevo < numEstantes();
        int vecina = vistaVecina(sentido);
        if (!hayEstante && vecina < 0) {
            rvLibros.animate().translationX(0).setDuration(160).start(); // tope: vuelve a su sitio
            return;
        }
        rvLibros.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);
        float ancho = rvLibros.getWidth();
        rvLibros.animate().translationX(-sentido * ancho).alpha(0f).setDuration(140).withEndAction(() -> {
            if (hayEstante) {
                estante = nuevo;
                repartirEstantes(false);
            } else {
                irAlUltimoEstante = sentido < 0;
                pasandoEstante = true;
                elegirVista(vistas.get(vecina));
                pasandoEstante = false;
            }
            rvLibros.setTranslationX(sentido * ancho * 0.4f);
            rvLibros.animate().translationX(0).alpha(1f).setDuration(180).start();
        }).start();
    }

    // índice de la pestaña de al lado, o -1 si no hay (desde Explorar se va a la primera)
    private int vistaVecina(int sentido) {
        int actual = vistas.indexOf(vistaActual);
        int nueva = actual < 0 ? 0 : actual + sentido;
        return nueva >= 0 && nueva < vistas.size() ? nueva : -1;
    }

    // al pellizcar se sigue viendo el mismo primer libro, aunque cambie cuántos caben por estante
    private void cambiarColumnas(RecyclerView rv, int cambio) {
        int nuevas = rejilla.getSpanCount() + cambio;
        if (nuevas < columnasMin || nuevas > columnasMax) return;
        int primero = estante * librosPorEstante;
        rejilla.setSpanCount(nuevas);
        int caben = calcularLibrosPorEstante();
        estante = caben > 0 ? primero / caben : 0;
        repartirEstantes(false);
        rv.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);
        avisarTutorial(Gesto.PELLIZCAR);
    }

    private void empezarOjear(Libro libro, ImageView portada) {
        libroOjeado = libro;
        portadaOjeada = portada;
        sobreUmbralSacar = false;
        gravedad.enderezar(); // la tarjeta nace encima de la portada: tiene que estar en su sitio
        portada.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);

        LibroAdapter.mostrarPortada(findViewById(R.id.ivPortadaOjear), libro);
        ((TextView) findViewById(R.id.tvTituloOjear)).setText(libro.titulo);
        ((TextView) findViewById(R.id.tvAutoresOjear)).setText(libro.autores);
        LibroAdapter.mostrarDisponibilidad(findViewById(R.id.tvDisponibilidadOjear), libro);
        ((TextView) findViewById(R.id.tvUbicacionOjear)).setText(libro.ubicacion);

        // el libro sale de la balda: la tarjeta nace encima de la portada y vuela al centro
        // girándose; en la balda queda el hueco (portada casi transparente)
        restaurarTarjeta();
        panelOjear.setAlpha(0f);
        panelOjear.setVisibility(View.VISIBLE);
        panelOjear.animate().alpha(1f).setStartDelay(0).setDuration(200).start();
        portada.animate().alpha(0.15f).setStartDelay(0).setDuration(200).start();
        tarjetaOjear.setAlpha(0f);
        // tras el primer layout del panel, para poder medir dónde está todo
        OneShotPreDrawListener.add(tarjetaOjear, () -> {
            if (libroOjeado == null) return;
            colocarSobrePortada(portada);
            tarjetaOjear.setAlpha(1f);
            tarjetaOjear.animate().translationX(0f).translationY(0f).scaleX(1f).scaleY(1f)
                    .rotation(GIRO_OJEAR).setDuration(260)
                    .setInterpolator(new DecelerateInterpolator(1.6f)).start();
        });
    }

    // pone la tarjeta (sin animar) de forma que su portada tape justo la portada de la balda
    private void colocarSobrePortada(View portada) {
        View portadaTarjeta = findViewById(R.id.ivPortadaOjear);
        tarjetaOjear.animate().cancel();
        tarjetaOjear.setRotation(0f);
        tarjetaOjear.setTranslationX(0f);
        tarjetaOjear.setTranslationY(0f);
        tarjetaOjear.setScaleX(1f);
        tarjetaOjear.setScaleY(1f);
        int[] enBalda = new int[2], enTarjeta = new int[2], tarjeta = new int[2];
        portada.getLocationOnScreen(enBalda);
        portadaTarjeta.getLocationOnScreen(enTarjeta);
        tarjetaOjear.getLocationOnScreen(tarjeta);
        // pivote = centro de la portada de la tarjeta: así escalar no la mueve
        float pivoteX = enTarjeta[0] - tarjeta[0] + portadaTarjeta.getWidth() / 2f;
        float pivoteY = enTarjeta[1] - tarjeta[1] + portadaTarjeta.getHeight() / 2f;
        tarjetaOjear.setPivotX(pivoteX);
        tarjetaOjear.setPivotY(pivoteY);
        float escala = portada.getWidth() / (float) Math.max(1, portadaTarjeta.getWidth());
        tarjetaOjear.setScaleX(escala);
        tarjetaOjear.setScaleY(escala);
        tarjetaOjear.setTranslationX(enBalda[0] + portada.getWidth() / 2f - (tarjeta[0] + pivoteX));
        tarjetaOjear.setTranslationY(enBalda[1] + portada.getHeight() / 2f - (tarjeta[1] + pivoteY));
    }

    private void restaurarTarjeta() {
        tarjetaOjear.animate().cancel();
        tarjetaOjear.setTranslationX(0f);
        tarjetaOjear.setTranslationY(0f);
        tarjetaOjear.setScaleX(1f);
        tarjetaOjear.setScaleY(1f);
        tarjetaOjear.setRotation(GIRO_OJEAR);
        tarjetaOjear.setAlpha(1f);
    }

    // la tarjeta sigue al dedo hacia arriba; al soltar se abre si ha pasado el umbral
    private void seguirOjear(MotionEvent e) {
        float subido = yInicioDedo - e.getRawY();
        float umbral = UMBRAL_SACAR_DP * getResources().getDisplayMetrics().density;
        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_MOVE:
                tarjetaOjear.setTranslationY(-Math.max(0, subido));
                if ((subido > umbral) != sobreUmbralSacar) {
                    sobreUmbralSacar = subido > umbral;
                    tarjetaOjear.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);
                    // pasado el umbral el libro se endereza y crece un poco: "suelta y se abre"
                    tarjetaOjear.animate().rotation(sobreUmbralSacar ? 0f : GIRO_OJEAR)
                            .scaleX(sobreUmbralSacar ? 1.04f : 1f).scaleY(sobreUmbralSacar ? 1.04f : 1f)
                            .setDuration(150).start();
                }
                break;
            case MotionEvent.ACTION_UP:
                if (subido > umbral) abrirDesdeOjear();
                else terminarOjear();
                break;
            case MotionEvent.ACTION_CANCEL:
                terminarOjear();
                break;
            default:
                break;
        }
    }

    // soltar: el libro vuelve a su hueco de la balda y el velo se va
    private void terminarOjear() {
        avisarTutorial(Gesto.MANTENER);
        View portada = portadaOjeada;
        libroOjeado = null;
        portadaOjeada = null;
        if (portada != null && portada.isAttachedToWindow()) {
            View portadaTarjeta = findViewById(R.id.ivPortadaOjear);
            int[] enBalda = new int[2], enTarjeta = new int[2];
            portada.getLocationOnScreen(enBalda);
            portadaTarjeta.getLocationOnScreen(enTarjeta);
            float escala = portada.getWidth() / (float) Math.max(1, portadaTarjeta.getWidth() * tarjetaOjear.getScaleX());
            // mover el centro de la portada de la tarjeta al centro de la de la balda
            float dx = enBalda[0] + portada.getWidth() / 2f - (enTarjeta[0] + portadaTarjeta.getWidth() * tarjetaOjear.getScaleX() / 2f);
            float dy = enBalda[1] + portada.getHeight() / 2f - (enTarjeta[1] + portadaTarjeta.getHeight() * tarjetaOjear.getScaleY() / 2f);
            tarjetaOjear.animate().translationXBy(dx).translationYBy(dy)
                    .scaleX(tarjetaOjear.getScaleX() * escala).scaleY(tarjetaOjear.getScaleY() * escala)
                    .rotation(0f).setDuration(220).setInterpolator(new AccelerateDecelerateInterpolator()).start();
            portada.animate().alpha(1f).setStartDelay(180).setDuration(60).start();
        }
        panelOjear.animate().alpha(0f).setStartDelay(80).setDuration(160)
                .withEndAction(() -> {
                    panelOjear.setVisibility(View.GONE);
                    restaurarTarjeta();
                }).start();
    }

    // deslizar arriba: la portada de la tarjeta viaja hasta el detalle; el panel se recoge en onStop
    private void abrirDesdeOjear() {
        avisarTutorial(Gesto.MANTENER);
        Libro libro = libroOjeado;
        portadaRetorno = portadaOjeada;
        libroOjeado = null;
        portadaOjeada = null;
        abriendoDetalle = true;
        abrirDetalle(libro, findViewById(R.id.ivPortadaOjear));
    }

    private void crearChipsVista(BibliotecaDB db, Bundle guardado) {
        chipDisponibles.setOnCheckedChangeListener((b, marcado) -> aplicarFiltro());
        String correo = SesionUtil.correo(getIntent());

        // solo interesan las asignaturas con libros, también para decidir cuál es la próxima clase
        misAsignaturas = new ArrayList<>(db.getAsignaturasMatriculadas(correo));
        misAsignaturas.retainAll(nombresAsignatura.keySet());
        Collections.sort(misAsignaturas);
        List<ProximaClase.Sesion> clases = new ArrayList<>();
        for (ProximaClase.Sesion s : db.getClasesMatriculadas(correo)) {
            if (misAsignaturas.contains(s.codigo)) clases.add(s);
        }
        proximaClase = ProximaClase.de(clases, LocalDateTime.now());

        // una sola fila: mis asignaturas, la próxima clase, todo y Explorar (cada asignatura suelta, en Explorar)
        pestanasVista.nuevaFila();
        if (!misAsignaturas.isEmpty()) anadirVista(VISTA_MIS, "Mías", null, R.drawable.ic_nui_asignaturas);
        if (proximaClase != null) {
            anadirVista(proximaClase.codigo, proximaClase.codigo, proximaClase.cuando, R.drawable.ic_nui_calendario);
        }
        anadirVista(VISTA_TODOS, "Todos", null, R.drawable.ic_nui_biblioteca);
        pestanasVista.anadir(EXPLORAR, "Explorar", null, android.R.drawable.ic_menu_sort_by_size, R.color.nui_cobalto);
        pestanasVista.setOnElegir(clave -> {
            if (EXPLORAR.equals(clave)) {
                // su marca depende de lo elegido en el panel, no del toque
                pestanasVista.marcar(filtro.activo() ? EXPLORAR : vistaActual);
                abrirPanelExplorar(filtro);
            } else {
                elegirVista((String) clave);
            }
        });

        if (guardado == null) {
            marcarVistaInicial();
        } else {
            leerFiltro(guardado, filtro, "");
            vistaActual = filtro.activo() ? null : guardado.getString("vista", vistas.get(0));
            pintarExplorar();
            aplicarFiltro();
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle estado) {
        super.onSaveInstanceState(estado);
        guardarFiltro(estado, filtro, "");
        estado.putString("vista", vistaActual);
        if (panelExplorar != null && panelExplorar.estaAbierto()) {
            estado.putBoolean("panelAbierto", true);
            guardarFiltro(estado, panelExplorar.borrador(), "panel_");
        }
    }

    // aquí y no en onCreate: los recuentos del panel dependen de Disponibles, que se restaura justo antes
    @Override
    protected void onRestoreInstanceState(@NonNull Bundle guardado) {
        super.onRestoreInstanceState(guardado);
        if (guardado.getBoolean("panelAbierto")) {
            FiltroExplorar borrador = filtro.copia();
            leerFiltro(guardado, borrador, "panel_");
            abrirPanelExplorar(borrador);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (panelExplorar != null) panelExplorar.cerrar();
    }

    private static void guardarFiltro(Bundle estado, FiltroExplorar f, String prefijo) {
        if (f.idGrado != null) estado.putInt(prefijo + "grado", f.idGrado);
        if (f.curso != null) estado.putInt(prefijo + "curso", f.curso);
        estado.putString(prefijo + "rama", f.rama);
        estado.putString(prefijo + "asignatura", f.asignatura);
        estado.putString(prefijo + "planta", f.planta);
    }

    private static void leerFiltro(Bundle estado, FiltroExplorar f, String prefijo) {
        f.idGrado = estado.containsKey(prefijo + "grado") ? estado.getInt(prefijo + "grado") : null;
        f.curso = estado.containsKey(prefijo + "curso") ? estado.getInt(prefijo + "curso") : null;
        f.rama = estado.getString(prefijo + "rama");
        f.asignatura = estado.getString(prefijo + "asignatura");
        f.planta = estado.getString(prefijo + "planta");
    }

    private void anadirVista(String vista, String texto, String detalle, int icono) {
        vistas.add(vista);
        pestanasVista.anadir(vista, texto, detalle, icono, R.color.nui_tinta);
    }

    private void elegirVista(String vista) {
        filtro.limpiar();
        vistaActual = vista;
        pintarExplorar();
        aplicarFiltro();
    }

    // sin matrícula no hay "Mis asignaturas": se empieza por todo el catálogo
    private void marcarVistaInicial() {
        elegirVista(vistas.get(0));
    }

    // con un filtro de Explorar, su pestaña dice qué se está viendo
    private void pintarExplorar() {
        boolean activo = filtro.activo();
        pestanasVista.setTexto(EXPLORAR, activo ? filtro.resumen() : "Explorar");
        pestanasVista.marcar(activo ? EXPLORAR : vistaActual);
    }

    private void abrirPanelExplorar(FiltroExplorar inicial) {
        Set<String> plantas = new LinkedHashSet<>();
        for (Libro l : libros) {
            String planta = plantaDe(l);
            if (planta != null) plantas.add(planta);
        }
        panelExplorar = new PanelExplorar(this, inicial, catalogo, new ArrayList<>(plantas), this::contarLibros, elegido -> {
            boolean habiaFiltro = filtro.activo();
            filtro.copiarDe(elegido);
            if (filtro.activo()) {
                vistaActual = null;
                pintarExplorar();
                aplicarFiltro();
            } else if (habiaFiltro) {
                marcarVistaInicial();
            }
        });
        panelExplorar.mostrar();
    }

    private int contarLibros(FiltroExplorar f) {
        BiPredicate<Collection<String>, String> admite = f.comprobador();
        int total = 0;
        for (Libro l : libros) {
            if (chipDisponibles.isChecked() && !l.disponible()) continue;
            if (admite.test(asignaturasDe(l), plantaDe(l))) total++;
        }
        return total;
    }

    private Set<String> asignaturasDe(Libro libro) {
        return bibliografia.getOrDefault(libro.id, Collections.emptyMap()).keySet();
    }

    // ubicacion = "Biblioteca ETSIIT · Planta 1 · Estantería 12"
    private static String plantaDe(Libro libro) {
        if (libro.ubicacion == null) return null;
        for (String parte : libro.ubicacion.split(" · ")) {
            if (parte.startsWith("Planta")) return parte;
        }
        return null;
    }

    private String vistaMarcada() {
        return vistaActual;
    }

    private boolean estaEnLaVista(Libro libro, String vista, BiPredicate<Collection<String>, String> admite) {
        if (filtro.activo()) return admite.test(asignaturasDe(libro), plantaDe(libro));
        if (vista == null || VISTA_TODOS.equals(vista)) return true;
        if (VISTA_MIS.equals(vista)) return !Collections.disjoint(asignaturasDe(libro), misAsignaturas);
        return asignaturasDe(libro).contains(vista);
    }

    // asignaturas que cuentan para la marca "Básica"; en todo el catálogo la marca no diría nada
    private Set<String> asignaturasDeLaVista(String vista) {
        if (filtro.activo()) return filtro.filtraAsignaturas() ? filtro.codigos() : Collections.emptySet();
        if (VISTA_MIS.equals(vista)) return new HashSet<>(misAsignaturas);
        if (vista == null || VISTA_TODOS.equals(vista)) return Collections.emptySet();
        return Collections.singleton(vista);
    }

    private boolean esBasica(Libro libro, Set<String> asignaturas) {
        Map<String, String> tipos = bibliografia.getOrDefault(libro.id, Collections.emptyMap());
        for (String codigo : asignaturas) {
            if ("basica".equals(tipos.get(codigo))) return true;
        }
        return false;
    }

    private String nombreDeLaVista(String vista) {
        if (filtro.activo()) return filtro.resumen();
        if (VISTA_MIS.equals(vista)) return "Mis asignaturas";
        if (vista == null || VISTA_TODOS.equals(vista)) return "Todo el catálogo";
        return nombresAsignatura.getOrDefault(vista, vista);
    }

    // buscador (BuscadorTolerante) AND disponibles AND vista
    private void aplicarFiltro() {
        String consulta = etBuscar.getText().toString();
        BuscadorTolerante.Busqueda<Libro> r = indice.buscar(Libro.consultaSinSeparadoresIsbn(consulta));
        boolean buscando = !consulta.trim().isEmpty();
        String vista = vistaMarcada();
        BiPredicate<Collection<String>, String> admite = filtro.comprobador();

        List<Libro> disponibles = new ArrayList<>();
        List<Libro> visibles = new ArrayList<>();
        for (Libro l : r.elementos) {
            if (chipDisponibles.isChecked() && !l.disponible()) continue;
            disponibles.add(l);
            if (estaEnLaVista(l, vista, admite)) visibles.add(l);
        }
        // lo que se busca por su nombre puede no ser de la vista actual: mejor enseñarlo que dejar la balda vacía
        boolean ampliado = buscando && visibles.isEmpty() && !disponibles.isEmpty();
        if (ampliado) visibles = disponibles;

        Set<String> asignaturasVista = ampliado ? Collections.emptySet() : asignaturasDeLaVista(vista);
        Set<Integer> basicos = new HashSet<>();
        for (Libro l : visibles) {
            if (esBasica(l, asignaturasVista)) basicos.add(l.id);
        }
        // al buscar manda la relevancia del buscador; si no, primero lo que se va a necesitar antes
        if (!buscando) {
            String proxima = VISTA_MIS.equals(vista) && !filtro.activo() && proximaClase != null ? proximaClase.codigo : null;
            visibles.sort(Comparator
                    .<Libro, Boolean>comparing(l -> proxima == null || !asignaturasDe(l).contains(proxima))
                    .thenComparing(l -> !basicos.contains(l.id))
                    .thenComparing(l -> !l.disponible()));
        }
        String cuantos = PanelExplorar.textoLibros(visibles.size());
        textoContador = ampliado
                ? "No está en " + nombreDeLaVista(vista) + ". En todo el catálogo: " + cuantos
                : nombreDeLaVista(vista) + " · " + cuantos;
        // otro filtro u otra pestaña: se empieza por el primer estante
        librosVista = visibles;
        basicosVista = basicos;
        estante = 0;
        repartirEstantes(!pasandoEstante);
        tvMostrandoPara.setVisibility(r.mostrandoPara != null ? View.VISIBLE : View.GONE);
        if (r.mostrandoPara != null) {
            tvMostrandoPara.setText("Mostrando resultados para «" + r.mostrandoPara + "»");
        }
        chipSugerencia.setVisibility(r.sugerencia != null ? View.VISIBLE : View.GONE);
        if (r.sugerencia != null) {
            chipSugerencia.setText("¿Quisiste decir «" + r.sugerencia + "»?");
            chipSugerencia.setTag(r.sugerencia);
        }
        tvVacio.setVisibility(visibles.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void abrirDetalle(Libro libro, ImageView portada) {
        if (portada.getId() == R.id.ivPortada) portadaRetorno = portada;
        gravedad.enderezar();
        // String, igual que los extras de RecienteUtil
        Intent intent = new Intent(this, DetalleLibro.class)
                .putExtra("idLibro", String.valueOf(libro.id))
                .putExtra("CORREO", getIntent().getStringExtra("CORREO"))
                .putExtra(PantallaBiblioteca.EXTRA_DESDE_BALDA, true);
        startActivity(intent, ActivityOptionsCompat.makeSceneTransitionAnimation(this, portada, "portada").toBundle());
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
