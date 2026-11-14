package com.example.nuietsiit.activities.secciones.noticias;

import android.content.Context;
import android.content.res.ColorStateList;
import android.text.Layout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.view.OneShotPreDrawListener;
import androidx.recyclerview.widget.RecyclerView;

import com.example.nuietsiit.R;
import com.example.nuietsiit.data.modelo.Anuncio;
import com.example.nuietsiit.util.FechasUtil;
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.List;

/**
 * Páginas del carrusel de Noticias (pagina_anuncio.xml), una tarjeta por anuncio:
 * icono de la categoría, puntito si no está leído y etiqueta y borde rojos si es urgente.
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

        // Icono de la categoría en un círculo de su color
        h.ivIcono.setImageResource(Anuncio.iconoRes(a.categoria));
        h.ivIcono.setImageTintList(ColorStateList.valueOf(ctx.getColor(Anuncio.colorRes(a.categoria))));
        h.ivIcono.setBackgroundTintList(ColorStateList.valueOf(ctx.getColor(Anuncio.tinteRes(a.categoria))));

        h.tvTitulo.setText(a.titulo);
        h.tvMeta.setText(Anuncio.nombreCategoria(ctx, a.categoria) + " · " + FechasUtil.hace(ctx, a.publicacion));
        h.tvContenido.setText(a.contenido);
        ajustarPista(h, a);

        // Urgentes: etiqueta roja. No leídos: puntito naranja
        h.tvUrgente.setVisibility(a.esUrgente() ? View.VISIBLE : View.GONE);
        h.vNoLeido.setVisibility(a.leido ? View.INVISIBLE : View.VISIBLE);
        // Urgentes: además, borde rojo más grueso
        MaterialCardView tarjeta = (MaterialCardView) h.ficha;
        tarjeta.setStrokeColor(ctx.getColor(a.esUrgente() ? R.color.sec_error : R.color.sec_borde));
        tarjeta.setStrokeWidth(Math.round((a.esUrgente() ? 2 : 1) * ctx.getResources().getDisplayMetrics().density));

        h.ficha.setContentDescription((a.esUrgente() ? ctx.getString(R.string.nui_not_urgente) + ". " : "") + (a.leido ? "" : ctx.getString(R.string.nui_not_nuevo) + ". ")
                + Anuncio.nombreCategoria(ctx, a.categoria) + ". " + a.titulo);
        h.ficha.setOnClickListener(v -> listener.alPulsar(a));
    }

    /**
     * La pista de abajo solo invita a tocar si hay algo más que ver: si el texto no cabe entero en la
     * tarjeta (se corta con "…") pone "Toca para leerlo entero"; si cabe pero tiene enlace, "Toca para
     * abrir el enlace"; y si ya se ve todo, no pone nada (en una tablet casi siempre cabe).
     */
    private void ajustarPista(Holder h, Anuncio a) {
        TextView tv = h.tvContenido;
        tv.setMaxLines(Integer.MAX_VALUE);
        tv.setTag(a);
        h.tvLeerMas.setVisibility(View.INVISIBLE);
        // se mira justo antes de dibujar, cuando ya se sabe cuánto mide la tarjeta
        OneShotPreDrawListener.add(tv, () -> {
            if (tv.getTag() != a) return; // la página ya muestra otro aviso
            Layout layout = tv.getLayout();
            int alto = tv.getHeight() - tv.getPaddingTop() - tv.getPaddingBottom();
            boolean cortado = false;
            if (layout != null && alto > 0 && layout.getHeight() > alto) {
                // caben las líneas que no se salen: con maxLines el final lleva "…" en vez de cortarse a medias
                int caben = 0;
                while (caben < layout.getLineCount() && layout.getLineBottom(caben) <= alto) caben++;
                tv.setMaxLines(Math.max(1, caben));
                cortado = true;
            }
            boolean hayEnlace = a.enlace != null && !a.enlace.isEmpty();
            if (cortado) h.tvLeerMas.setText(R.string.nui_not_toca_leer);
            else if (hayEnlace) h.tvLeerMas.setText(R.string.nui_not_toca_enlace);
            h.tvLeerMas.setVisibility(cortado || hayEnlace ? View.VISIBLE : View.INVISIBLE);
        });
    }

    @Override
    public int getItemCount() {
        return anuncios.size();
    }

    static class Holder extends RecyclerView.ViewHolder {
        final View ficha, vNoLeido;
        final ImageView ivIcono;
        final TextView tvTitulo, tvMeta, tvContenido, tvUrgente, tvLeerMas;

        Holder(View v) {
            super(v);
            ficha = v.findViewById(R.id.fichaAnuncio);
            ivIcono = v.findViewById(R.id.ivIcono);
            tvTitulo = v.findViewById(R.id.tvTitulo);
            tvMeta = v.findViewById(R.id.tvMeta);
            tvContenido = v.findViewById(R.id.tvContenido);
            tvUrgente = v.findViewById(R.id.tvUrgente);
            tvLeerMas = v.findViewById(R.id.tvLeerMas);
            vNoLeido = v.findViewById(R.id.vNoLeido);
        }
    }
}
