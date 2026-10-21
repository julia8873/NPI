package com.example.nuietsiit.activities.secciones.noticias;

import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.nuietsiit.R;
import com.example.nuietsiit.data.modelo.Anuncio;
import com.example.nuietsiit.util.FechasUtil;
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.List;

/** Pinta cada anuncio con el diseño de item_anuncio.xml */
public class AnuncioAdapter extends RecyclerView.Adapter<AnuncioAdapter.Holder> {

    public interface OnAnuncioClick {
        void alPulsar(Anuncio anuncio);
    }

    private static final int NARANJA = 0xFFFF8C42;
    private static final int ROJO = 0xFFD32F2F;

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
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_anuncio, parent, false);
        return new Holder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder h, int posicion) {
        Anuncio a = anuncios.get(posicion);
        float densidad = h.itemView.getResources().getDisplayMetrics().density;

        h.tvEmoji.setText(Anuncio.emoji(a.categoria));
        h.tvTitulo.setText(a.titulo);
        h.tvMeta.setText(Anuncio.nombreCategoria(a.categoria) + " · " + FechasUtil.hace(a.publicacion));
        h.tvContenido.setText(a.contenido);

        // Urgentes: borde rojo más grueso y etiqueta
        h.tvUrgente.setVisibility(a.esUrgente() ? View.VISIBLE : View.GONE);
        h.card.setStrokeColor(a.esUrgente() ? ROJO : NARANJA);
        h.card.setStrokeWidth(Math.round((a.esUrgente() ? 2 : 1) * densidad));

        // No leídos: título en negrita + puntito naranja. Leídos: algo más apagados
        h.tvTitulo.setTypeface(null, a.leido ? Typeface.NORMAL : Typeface.BOLD);
        h.vNoLeido.setVisibility(a.leido ? View.GONE : View.VISIBLE);
        h.itemView.setAlpha(a.leido ? 0.7f : 1f);

        h.itemView.setOnClickListener(v -> listener.alPulsar(a));
    }

    @Override
    public int getItemCount() {
        return anuncios.size();
    }

    static class Holder extends RecyclerView.ViewHolder {
        final MaterialCardView card;
        final TextView tvEmoji, tvTitulo, tvMeta, tvContenido, tvUrgente;
        final View vNoLeido;

        Holder(View v) {
            super(v);
            card = (MaterialCardView) v;
            tvEmoji = v.findViewById(R.id.tvEmoji);
            tvTitulo = v.findViewById(R.id.tvTitulo);
            tvMeta = v.findViewById(R.id.tvMeta);
            tvContenido = v.findViewById(R.id.tvContenido);
            tvUrgente = v.findViewById(R.id.tvUrgente);
            vNoLeido = v.findViewById(R.id.vNoLeido);
        }
    }
}
