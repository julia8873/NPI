package com.example.nuietsiit.activities.secciones.noticias;

import android.content.Context;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.nuietsiit.R;
import com.example.nuietsiit.data.modelo.Anuncio;
import com.example.nuietsiit.util.FechasUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * Páginas del carrusel de Noticias (pagina_anuncio.xml), una ficha NUI por anuncio:
 * tesela con el icono de la categoría, etiqueta "Nuevo" si no está leído y sello "Urgente".
 */
public class AnuncioAdapter extends RecyclerView.Adapter<AnuncioAdapter.Holder> {

    public interface OnAnuncioClick {
        void alPulsar(Anuncio anuncio);
    }

    private List<Anuncio> anuncios = new ArrayList<>();
    private final OnAnuncioClick listener;

    public AnuncioAdapter(OnAnuncioClick listener) {
        this.listener = listener;
    }

    public void setAnuncios(List<Anuncio> nuevos) {
        anuncios = nuevos;
        notifyDataSetChanged();
    }

    public Anuncio get(int posicion) {
        return anuncios.get(posicion);
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.pagina_anuncio, parent, false);
        return new Holder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder h, int posicion) {
        Anuncio a = anuncios.get(posicion);
        Context ctx = h.itemView.getContext();

        // Un solo color fuerte por ficha: el de la categoría, en la tesela
        h.ivIcono.setImageResource(Anuncio.iconoRes(a.categoria));
        h.ivIcono.setImageTintList(ColorStateList.valueOf(ctx.getColor(Anuncio.colorRes(a.categoria))));
        h.ivIcono.setBackgroundTintList(ColorStateList.valueOf(ctx.getColor(Anuncio.tinteRes(a.categoria))));

        h.tvTitulo.setText(a.titulo);
        h.tvMeta.setText(Anuncio.nombreCategoria(a.categoria) + " · " + FechasUtil.hace(a.publicacion));
        h.tvContenido.setText(a.contenido);

        // Urgentes: sello granada. No leídos: etiqueta "Nuevo"
        h.tvUrgente.setVisibility(a.esUrgente() ? View.VISIBLE : View.GONE);
        h.vNoLeido.setVisibility(a.leido ? View.INVISIBLE : View.VISIBLE);

        h.ficha.setContentDescription((a.esUrgente() ? "Urgente. " : "") + (a.leido ? "" : "Nuevo. ")
                + Anuncio.nombreCategoria(a.categoria) + ". " + a.titulo);
        h.ficha.setOnClickListener(v -> listener.alPulsar(a));
    }

    @Override
    public int getItemCount() {
        return anuncios.size();
    }

    static class Holder extends RecyclerView.ViewHolder {
        final View ficha, vNoLeido;
        final ImageView ivIcono;
        final TextView tvTitulo, tvMeta, tvContenido, tvUrgente;

        Holder(View v) {
            super(v);
            ficha = v.findViewById(R.id.fichaAnuncio);
            ivIcono = v.findViewById(R.id.ivIcono);
            tvTitulo = v.findViewById(R.id.tvTitulo);
            tvMeta = v.findViewById(R.id.tvMeta);
            tvContenido = v.findViewById(R.id.tvContenido);
            tvUrgente = v.findViewById(R.id.tvUrgente);
            vNoLeido = v.findViewById(R.id.vNoLeido);
        }
    }
}
