package com.example.nuietsiit.activities.secciones;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityOptionsCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.nuietsiit.R;
import com.example.nuietsiit.activities.BaseActivity;
import com.example.nuietsiit.activities.secciones.biblioteca.ApuntarLibros;
import com.example.nuietsiit.activities.secciones.biblioteca.BaldaDecoration;
import com.example.nuietsiit.activities.secciones.biblioteca.DetalleLibro;
import com.example.nuietsiit.activities.secciones.biblioteca.FiltroExplorar;
import com.example.nuietsiit.activities.secciones.biblioteca.LibroAdapter;
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
import com.google.android.material.chip.ChipGroup;

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

public class Biblioteca extends BaseActivity {

    private static final float UMBRAL_PELLIZCO = 1.3f; // cuánto hay que abrir o cerrar los dedos para cambiar de tamaño
    private static final int UMBRAL_SACAR_DP = 80;     // cuánto hay que subir el dedo para abrir el libro que se ojea
    private static final int UMBRAL_DESLIZAR_DP = 80;  // cuánto hay que deslizar en horizontal para cambiar de asignatura
    private static final float ARRASTRE_DESLIZAR = 0.35f; // las baldas siguen al dedo más despacio que él

    // la vista es el tag del chip marcado: una de estas o un código de asignatura
    private static final String VISTA_MIS = "mis";
    private static final String VISTA_TODOS = "todos";

    private EditText etBuscar;
    private TextView tvMostrandoPara, tvVacio, tvContador;
    private Chip chipSugerencia, chipDisponibles, chipExplorar;
    private ChipGroup chipsVista;
    private HorizontalScrollView scrollVistas;
    private LibroAdapter adaptador;
    private GridLayoutManager rejilla;
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

        etBuscar = findViewById(R.id.etBuscar);
        tvMostrandoPara = findViewById(R.id.tvMostrandoPara);
        tvVacio = findViewById(R.id.tvVacio);
        tvContador = findViewById(R.id.tvContador);
        chipSugerencia = findViewById(R.id.chipSugerencia);
        chipDisponibles = findViewById(R.id.chipDisponibles);
        chipExplorar = findViewById(R.id.chipExplorar);
        chipsVista = findViewById(R.id.chipsVista);
        scrollVistas = findViewById(R.id.scrollVistas);
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
            @Override public void afterTextChanged(Editable s) { aplicarFiltro(); }
        });
        chipSugerencia.setOnClickListener(v -> etBuscar.setText(chipSugerencia.getTag().toString()));
        findViewById(R.id.btnApuntar).setOnClickListener(v -> startActivity(new Intent(this, ApuntarLibros.class)
                .putExtra("CORREO", getIntent().getStringExtra("CORREO"))));

        aplicarFiltro();
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
        rejilla = new GridLayoutManager(this, columnas);
        RecyclerView rv = findViewById(R.id.rvLibros);
        rv.setLayoutManager(rejilla);
        rv.setAdapter(adaptador);
        rv.addItemDecoration(new BaldaDecoration(this));

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
            yInicioDedo = e.getRawY();
            xInicioDedo = e.getRawX();
        } else if (accion == MotionEvent.ACTION_UP || accion == MotionEvent.ACTION_CANCEL) {
            dedoEnBaldas = false;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // si otra pantalla se pone delante, no llegará el evento de soltar
        if (libroOjeado != null) terminarOjear();
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
            rv.animate().translationX(0).setDuration(160).start();
            float umbral = UMBRAL_DESLIZAR_DP * getResources().getDisplayMetrics().density;
            if (!abandonado && Math.abs(dx) > umbral) cambiarVista(dx < 0 ? 1 : -1);
        }
    }

    // deslizar hacia la izquierda = siguiente chip, como pasar de página
    private void cambiarVista(int sentido) {
        int actual = -1;
        for (int i = 0; i < chipsVista.getChildCount(); i++) {
            if (((Chip) chipsVista.getChildAt(i)).isChecked()) actual = i;
        }
        int nueva = actual < 0 ? 0 : actual + sentido;
        if (nueva < 0 || nueva >= chipsVista.getChildCount()) return;
        chipsVista.check(chipsVista.getChildAt(nueva).getId());
        chipsVista.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);
    }

    private void cambiarColumnas(RecyclerView rv, int cambio) {
        int nuevas = rejilla.getSpanCount() + cambio;
        if (nuevas < columnasMin || nuevas > columnasMax) return;
        rejilla.setSpanCount(nuevas);
        adaptador.notifyItemRangeChanged(0, adaptador.getItemCount());
        rv.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);
    }

    private void empezarOjear(Libro libro, ImageView portada) {
        libroOjeado = libro;
        portadaOjeada = portada;
        sobreUmbralSacar = false;
        portada.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);

        LibroAdapter.mostrarPortada(findViewById(R.id.ivPortadaOjear), libro);
        ((TextView) findViewById(R.id.tvTituloOjear)).setText(libro.titulo);
        ((TextView) findViewById(R.id.tvAutoresOjear)).setText(libro.autores);
        LibroAdapter.mostrarDisponibilidad(findViewById(R.id.tvDisponibilidadOjear), libro);
        ((TextView) findViewById(R.id.tvUbicacionOjear)).setText(libro.ubicacion);

        tarjetaOjear.setTranslationY(0);
        panelOjear.setAlpha(0f);
        panelOjear.setVisibility(View.VISIBLE);
        panelOjear.animate().alpha(1f).setDuration(120).start();
        tarjetaOjear.setScaleX(0.85f);
        tarjetaOjear.setScaleY(0.85f);
        tarjetaOjear.animate().scaleX(1f).scaleY(1f).setDuration(160).start();
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
                }
                break;
            case MotionEvent.ACTION_UP:
                Libro libro = libroOjeado;
                ImageView portada = portadaOjeada;
                terminarOjear();
                if (subido > umbral) abrirDetalle(libro, portada);
                break;
            case MotionEvent.ACTION_CANCEL:
                terminarOjear();
                break;
            default:
                break;
        }
    }

    private void terminarOjear() {
        libroOjeado = null;
        portadaOjeada = null;
        panelOjear.animate().alpha(0f).setDuration(120)
                .withEndAction(() -> panelOjear.setVisibility(View.GONE)).start();
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

        if (!misAsignaturas.isEmpty()) anadirChipVista("Mías", VISTA_MIS);
        if (proximaClase != null) {
            anadirChipVista(proximaClase.codigo + " · " + proximaClase.cuando, proximaClase.codigo);
        }
        for (String codigo : misAsignaturas) {
            if (proximaClase == null || !codigo.equals(proximaClase.codigo)) anadirChipVista(codigo, codigo);
        }
        anadirChipVista("Todos", VISTA_TODOS);

        chipsVista.setOnCheckedStateChangeListener((grupo, ids) -> {
            if (!ids.isEmpty()) {
                filtro.limpiar();
                pintarChipExplorar();
                View marcado = grupo.findViewById(ids.get(0));
                scrollVistas.smoothScrollTo(marcado.getLeft() - scrollVistas.getPaddingLeft(), 0);
            }
            aplicarFiltro();
        });

        // tocar el chip marcaría/desmarcaría por sí solo: su estado depende de lo elegido en el panel
        chipExplorar.setOnClickListener(v -> {
            chipExplorar.setChecked(filtro.activo());
            abrirPanelExplorar(filtro);
        });
        chipExplorar.setOnCloseIconClickListener(v -> marcarVistaInicial());

        if (guardado == null) {
            marcarVistaInicial();
        } else {
            leerFiltro(guardado, filtro, "");
            pintarChipExplorar();
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle estado) {
        super.onSaveInstanceState(estado);
        guardarFiltro(estado, filtro, "");
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

    private void anadirChipVista(String texto, String vista) {
        Chip chip = PanelExplorar.crearChip(this, texto);
        chip.setId(2000 + chipsVista.getChildCount()); // ids fijos para que sobrevivan a la rotación
        chip.setTag(vista);
        chipsVista.addView(chip);
    }

    // sin matrícula no hay "Mis asignaturas": se empieza por todo el catálogo
    private void marcarVistaInicial() {
        filtro.limpiar();
        pintarChipExplorar();
        chipsVista.check(chipsVista.getChildAt(0).getId());
    }

    private void pintarChipExplorar() {
        boolean activo = filtro.activo();
        chipExplorar.setChecked(activo);
        chipExplorar.setText(activo ? filtro.resumen() : "Explorar");
        chipExplorar.setCloseIconVisible(activo);
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
                pintarChipExplorar();
                chipsVista.clearCheck();
                aplicarFiltro(); // si ya no había chip marcado, clearCheck no avisa
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
        Chip marcado = chipsVista.findViewById(chipsVista.getCheckedChipId());
        return marcado != null ? (String) marcado.getTag() : null;
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
        adaptador.setLibros(visibles, basicos);

        String cuantos = PanelExplorar.textoLibros(visibles.size());
        tvContador.setText(ampliado
                ? "No está en " + nombreDeLaVista(vista) + ". En todo el catálogo: " + cuantos
                : nombreDeLaVista(vista) + " · " + cuantos);
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
        // String, igual que los extras de RecienteUtil
        Intent intent = new Intent(this, DetalleLibro.class)
                .putExtra("idLibro", String.valueOf(libro.id))
                .putExtra("CORREO", getIntent().getStringExtra("CORREO"));
        startActivity(intent, ActivityOptionsCompat.makeSceneTransitionAnimation(this, portada, "portada").toBundle());
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
