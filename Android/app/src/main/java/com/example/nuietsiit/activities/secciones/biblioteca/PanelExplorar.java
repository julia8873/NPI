package com.example.nuietsiit.activities.secciones.biblioteca;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.example.nuietsiit.R;
import com.example.nuietsiit.data.modelo.AsignaturaCatalogo;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

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
    private final ChipGroup chipsGrado, chipsCurso, chipsRama, chipsPlanta;
    private final ViewGroup listaAsignaturas;
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

        chipsGrado.removeAllViews();
        for (Map.Entry<Integer, String> g : grados.entrySet()) {
            anadirOpcion(chipsGrado, g.getValue(), Objects.equals(borrador.idGrado, g.getKey()), f -> f.idGrado = g.getKey(), f -> f.idGrado = null);
        }
        chipsCurso.removeAllViews();
        for (Map.Entry<Integer, String> c : cursos.entrySet()) {
            anadirOpcion(chipsCurso, c.getValue(), Objects.equals(borrador.curso, c.getKey()), f -> f.curso = c.getKey(), f -> f.curso = null);
        }
        chipsRama.removeAllViews();
        for (String r : ramas.keySet()) {
            anadirOpcion(chipsRama, r, r.equals(borrador.rama), f -> f.rama = r, f -> f.rama = null);
        }
        chipsPlanta.removeAllViews();
        for (String p : plantas) {
            anadirOpcion(chipsPlanta, p, p.equals(borrador.planta), f -> f.planta = p, f -> f.planta = null);
        }

        ocultarSiVacio(chipsGrado);
        ocultarSiVacio(chipsCurso);
        ocultarSiVacio(chipsRama);
        ocultarSiVacio(chipsPlanta);

        listaAsignaturas.removeAllViews();
        for (Map.Entry<String, String> a : asignaturas.entrySet()) {
            FiltroExplorar conEsta = borrador.copia();
            conEsta.asignatura = a.getKey();
            int libros = contarLibros.applyAsInt(conEsta);
            if (libros == 0) continue;
            TextView fila = (TextView) LayoutInflater.from(listaAsignaturas.getContext())
                    .inflate(R.layout.item_asignatura_explorar, listaAsignaturas, false);
            fila.setText(a.getValue() + " · " + textoLibros(libros));
            fila.setOnClickListener(v -> {
                borrador.asignatura = a.getKey();
                aplicar();
            });
            listaAsignaturas.addView(fila);
        }

        btnVer.setText("Ver " + textoLibros(contarLibros.applyAsInt(borrador)));
    }

    // sin opciones sobra también el título, que es la vista anterior del grupo
    private static void ocultarSiVacio(ChipGroup grupo) {
        int visibilidad = grupo.getChildCount() == 0 ? View.GONE : View.VISIBLE;
        ViewGroup padre = (ViewGroup) grupo.getParent();
        padre.getChildAt(padre.indexOfChild(grupo) - 1).setVisibility(visibilidad);
        grupo.setVisibility(visibilidad);
    }

    private void anadirOpcion(ChipGroup grupo, String texto, boolean marcada, Consumer<FiltroExplorar> elegir, Consumer<FiltroExplorar> quitar) {
        FiltroExplorar conEsta = borrador.copia();
        elegir.accept(conEsta);
        int libros = contarLibros.applyAsInt(conEsta);
        if (libros == 0 && !marcada) return;

        Chip chip = crearChip(grupo.getContext(), texto + " (" + libros + ")");
        chip.setChecked(marcada);
        chip.setOnClickListener(v -> {
            if (marcada) quitar.accept(borrador);
            else elegir.accept(borrador);
            pintar();
        });
        grupo.addView(chip);
    }

    public static Chip crearChip(Context ctx, String texto) {
        Chip chip = new Chip(ctx, null, com.google.android.material.R.attr.chipStyle);
        chip.setText(texto);
        chip.setCheckable(true);
        chip.setChipBackgroundColorResource(R.color.chip_fondo);
        chip.setTextColor(ctx.getColorStateList(R.color.chip_texto));
        chip.setCheckedIconVisible(false);
        return chip;
    }

    public static String textoLibros(int libros) {
        return libros == 1 ? "1 libro" : libros + " libros";
    }
}
