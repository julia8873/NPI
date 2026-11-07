package com.example.nuietsiit.activities.secciones.biblioteca;

import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.example.nuietsiit.R;
import com.example.nuietsiit.data.modelo.Libro;
import com.example.nuietsiit.util.CargadorPortadas;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class LibroAdapter extends RecyclerView.Adapter<LibroAdapter.Holder> {

    public interface OnLibroGesto {
        void alPulsar(Libro libro, ImageView portada);
        void alMantener(Libro libro, ImageView portada);
    }

    private static final Object CAMBIO_BASICA = new Object();

    private List<Libro> libros = new ArrayList<>();
    private Set<Integer> basicos = Collections.emptySet();
    private final OnLibroGesto listener;

    public LibroAdapter(OnLibroGesto listener) {
        this.listener = listener;
    }

    // con DiffUtil los libros se recolocan con animación al filtrar, en vez de cambiar de golpe
    // basicos = ids de los libros con la marca "Básica" en la balda actual
    public void setLibros(List<Libro> nuevos, Set<Integer> nuevosBasicos) {
        List<Libro> antiguos = libros;
        Set<Integer> basicosAntes = basicos;
        DiffUtil.DiffResult cambios = DiffUtil.calculateDiff(new DiffUtil.Callback() {
            @Override public int getOldListSize() { return antiguos.size(); }
            @Override public int getNewListSize() { return nuevos.size(); }
            @Override public boolean areItemsTheSame(int a, int b) { return antiguos.get(a).id == nuevos.get(b).id; }
            @Override public boolean areContentsTheSame(int a, int b) { return basicosAntes.contains(antiguos.get(a).id) == nuevosBasicos.contains(nuevos.get(b).id); }
            // solo cambia la marca: sin esto la portada volvería a cargarse
            @Override public Object getChangePayload(int a, int b) { return CAMBIO_BASICA; }
        });
        basicos = nuevosBasicos;
        libros = nuevos;
        cambios.dispatchUpdatesTo(this);
    }

    // también en DetalleLibro y al ojear un libro
    public static void mostrarDisponibilidad(TextView tv, Libro libro) {
        tv.setText(textoDisponibilidad(libro));
        tv.setTextColor(Color.parseColor(libro.disponible() ? "#2E7D32" : "#C62828"));
    }

    static String textoDisponibilidad(Libro libro) {
        return libro.disponible()
                ? "Disponible · " + libro.ejemplaresDisponibles + " de " + libro.ejemplaresTotales + " ejemplares"
                : "Sin ejemplares disponibles";
    }

    // la portada generada queda debajo hasta que llega la real
    public static void mostrarPortada(ImageView portada, Libro libro) {
        portada.setImageDrawable(new PortadaGenerada(libro));
        CargadorPortadas.cargar(portada, libro.isbn);
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_libro, parent, false);
        return new Holder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder h, int position) {
        Libro l = libros.get(position);
        mostrarPortada(h.ivPortada, l);
        if (l.disponible()) {
            h.ivPortada.clearColorFilter();
            h.tvPrestado.setVisibility(View.GONE);
        } else {
            ColorMatrix gris = new ColorMatrix();
            gris.setSaturation(0);
            h.ivPortada.setColorFilter(new ColorMatrixColorFilter(gris));
            h.tvPrestado.setVisibility(View.VISIBLE);
        }
        mostrarBasica(h, l);
        h.itemView.setOnClickListener(v -> listener.alPulsar(l, h.ivPortada));
        h.itemView.setOnLongClickListener(v -> {
            listener.alMantener(l, h.ivPortada);
            return true;
        });
    }

    @Override
    public void onBindViewHolder(@NonNull Holder h, int position, @NonNull List<Object> payloads) {
        if (payloads.isEmpty()) onBindViewHolder(h, position);
        else mostrarBasica(h, libros.get(position));
    }

    private void mostrarBasica(Holder h, Libro l) {
        boolean basica = basicos.contains(l.id);
        h.tvBasica.setVisibility(basica ? View.VISIBLE : View.GONE);
        h.itemView.setContentDescription(l.titulo + (basica ? ". Bibliografía básica. " : ". ") + textoDisponibilidad(l));
    }

    @Override
    public int getItemCount() {
        return libros.size();
    }

    static class Holder extends RecyclerView.ViewHolder {
        final ImageView ivPortada;
        final TextView tvPrestado;
        final TextView tvBasica;

        Holder(View v) {
            super(v);
            ivPortada = v.findViewById(R.id.ivPortada);
            tvPrestado = v.findViewById(R.id.tvPrestado);
            tvBasica = v.findViewById(R.id.tvBasica);
        }
    }
}
