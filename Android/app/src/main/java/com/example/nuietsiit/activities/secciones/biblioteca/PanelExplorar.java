package com.example.nuietsiit.activities.secciones.biblioteca;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.nuietsiit.R;
import com.example.nuietsiit.data.modelo.AsignaturaCatalogo;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.example.nuietsiit.ui.PestanasPapel;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.function.Consumer;
import java.util.function.ToIntFunction;

/**
 * Panel para buscar fuera de mis asignaturas: grado, curso, rama, planta y asignatura. Cada opción dice cuántos
 * libros tiene y las que se quedarían sin ninguno no aparecen, así que nunca se llega a una balda vacía.
 */
public class PanelExplorar {

    private final BottomSheetDialog dialogo;
    private final FiltroExplorar borrador;
    private final List<AsignaturaCatalogo> catalogo;
    private final List<String> plantas;
    private final ToIntFunction<FiltroExplorar> contarLibros;
    private final Consumer<FiltroExplorar> alAplicar;
    private final PestanasPapel chipsGrado, chipsCurso, chipsRama, chipsPlanta, listaAsignaturas;
    private final MaterialButton btnVer;

    public PanelExplorar(Context ctx, FiltroExplorar actual, List<AsignaturaCatalogo> catalogo, List<String> plantas,
                         ToIntFunction<FiltroExplorar> contarLibros, Consumer<FiltroExplorar> alAplicar) {
        this.borrador = actual.copia();
        this.catalogo = catalogo;
        this.plantas = plantas;
        this.contarLibros = contarLibros;
        this.alAplicar = alAplicar;

        View vista = LayoutInflater.from(ctx).inflate(R.layout.panel_explorar, null);
        chipsGrado = vista.findViewById(R.id.chipsGrado);
        chipsCurso = vista.findViewById(R.id.chipsCurso);
        chipsRama = vista.findViewById(R.id.chipsRama);
        chipsPlanta = vista.findViewById(R.id.chipsPlanta);
        listaAsignaturas = vista.findViewById(R.id.listaAsignaturas);
        btnVer = vista.findViewById(R.id.btnVerLibros);

        vista.findViewById(R.id.btnLimpiar).setOnClickListener(v -> {
            borrador.limpiar();
            pintar();
        });
        btnVer.setOnClickListener(v -> aplicar());

        dialogo = new BottomSheetDialog(ctx);
        dialogo.setContentView(vista);
        dialogo.getBehavior().setState(BottomSheetBehavior.STATE_EXPANDED);
        dialogo.getBehavior().setSkipCollapsed(true);
        // a toda la altura: las hojas necesitan saber cuánto sitio tienen
        dialogo.setOnShowListener(d -> {
            View hoja = dialogo.findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (hoja != null) {
                hoja.getLayoutParams().height = ViewGroup.LayoutParams.MATCH_PARENT;
                hoja.requestLayout();
            }
        });
        pintar();
    }

    public void mostrar() {
        dialogo.show();
    }

    public boolean estaAbierto() {
        return dialogo.isShowing();
    }

    public void cerrar() {
        dialogo.dismiss();
    }

    // lo elegido sin aplicar todavía
    public FiltroExplorar borrador() {
        return borrador;
    }

    private void aplicar() {
        alAplicar.accept(borrador);
        dialogo.dismiss();
    }

    // al elegir algo cambian los números y las opciones posibles: se repinta todo
    private void pintar() {
        Map<Integer, String> grados = new TreeMap<>();
        Map<Integer, String> cursos = new TreeMap<>();
        Map<String, String> ramas = new TreeMap<>();
        Map<String, String> asignaturas = new LinkedHashMap<>();
        for (AsignaturaCatalogo a : catalogo) {
            grados.put(a.idGrado, a.gradoCorto());
            cursos.put(a.curso, a.curso + "º");
            if (a.rama != null) ramas.put(a.rama, a.rama);
            asignaturas.put(a.codigo, a.nombre);
        }

        vaciar(chipsGrado, 0);
        for (Map.Entry<Integer, String> g : grados.entrySet()) {
            anadirOpcion(chipsGrado, g.getValue(), Objects.equals(borrador.idGrado, g.getKey()), f -> f.idGrado = g.getKey(), f -> f.idGrado = null);
        }
        vaciar(chipsCurso, 0);
        for (Map.Entry<Integer, String> c : cursos.entrySet()) {
            anadirOpcion(chipsCurso, c.getValue(), Objects.equals(borrador.curso, c.getKey()), f -> f.curso = c.getKey(), f -> f.curso = null);
        }
        vaciar(chipsRama, 2);
        for (String r : ramas.keySet()) {
            anadirOpcion(chipsRama, r, r.equals(borrador.rama), f -> f.rama = r, f -> f.rama = null);
        }
        vaciar(chipsPlanta, 0);
        for (String p : plantas) {
            anadirOpcion(chipsPlanta, p, p.equals(borrador.planta), f -> f.planta = p, f -> f.planta = null);
        }

        ocultarSiVacio(chipsGrado);
        ocultarSiVacio(chipsCurso);
        ocultarSiVacio(chipsRama);
        ocultarSiVacio(chipsPlanta);

        // asignaturas: rejilla de 2, como tejuelos; elegir una ya enseña sus libros
        vaciar(listaAsignaturas, 2);
        for (Map.Entry<String, String> a : asignaturas.entrySet()) {
            FiltroExplorar conEsta = borrador.copia();
            conEsta.asignatura = a.getKey();
            int libros = contarLibros.applyAsInt(conEsta);
            if (libros == 0) continue;
            nuevaFilaSiLlena(listaAsignaturas);
            listaAsignaturas.anadir(a.getKey(), a.getValue(), textoLibros(libros), 0, 0).setOnClickListener(v -> {
                v.performHapticFeedback(android.view.HapticFeedbackConstants.CLOCK_TICK);
                borrador.asignatura = a.getKey();
                aplicar();
            });
        }
        listaAsignaturas.completarFila(2);
        ocultarSiVacio(listaAsignaturas);

        btnVer.setText("Ver " + textoLibros(contarLibros.applyAsInt(borrador)));
    }

    // porFila 0 = todas en una fila; si no, filas de porFila pestañas
    private void vaciar(PestanasPapel grupo, int porFila) {
        grupo.vaciar();
        grupo.setTag(porFila);
    }

    private static void nuevaFilaSiLlena(PestanasPapel grupo) {
        int porFila = (int) grupo.getTag();
        int n = grupo.getPestanas().size();
        if (n == 0 || (porFila > 0 && n % porFila == 0)) grupo.nuevaFila();
    }

    // sin opciones sobra también el título, que es la vista anterior del grupo
    private static void ocultarSiVacio(PestanasPapel grupo) {
        int visibilidad = grupo.getPestanas().isEmpty() ? View.GONE : View.VISIBLE;
        ViewGroup padre = (ViewGroup) grupo.getParent();
        padre.getChildAt(padre.indexOfChild(grupo) - 1).setVisibility(visibilidad);
        grupo.setVisibility(visibilidad);
    }

    private void anadirOpcion(PestanasPapel grupo, String texto, boolean marcada, Consumer<FiltroExplorar> elegir, Consumer<FiltroExplorar> quitar) {
        FiltroExplorar conEsta = borrador.copia();
        elegir.accept(conEsta);
        int libros = contarLibros.applyAsInt(conEsta);
        if (libros == 0 && !marcada) return;

        nuevaFilaSiLlena(grupo);
        View pestana = grupo.anadir(texto, texto, textoLibros(libros), 0, 0);
        pestana.setSelected(marcada);
        // tocar la elegida la quita (los filtros de Explorar son opcionales)
        pestana.setOnClickListener(v -> {
            v.performHapticFeedback(android.view.HapticFeedbackConstants.CLOCK_TICK);
            if (marcada) quitar.accept(borrador);
            else elegir.accept(borrador);
            pintar();
        });
    }

    public static String textoLibros(int libros) {
        return libros == 1 ? "1 libro" : libros + " libros";
    }
}
