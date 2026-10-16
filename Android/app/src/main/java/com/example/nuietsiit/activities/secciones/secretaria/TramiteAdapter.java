package com.example.nuietsiit.activities.secciones.secretaria;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.nuietsiit.R;
import com.example.nuietsiit.data.modelo.Tramite;
import com.google.android.material.chip.Chip;

import java.util.ArrayList;
import java.util.List;

public class TramiteAdapter extends RecyclerView.Adapter<TramiteAdapter.Holder> {

    public interface OnTramiteClick {
        void alPulsar(Tramite tramite);
    }

    private List<Tramite> tramites = new ArrayList<>();
    private final OnTramiteClick listener;

    public TramiteAdapter(OnTramiteClick listener) {
        this.listener = listener;
    }

    public void setTramites(List<Tramite> nuevos) {
        tramites = nuevos;
        notifyDataSetChanged(); // lista de ~10 elementos
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_tramite, parent, false);
        return new Holder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder h, int position) {
        Tramite t = tramites.get(position);
        h.tvNombre.setText(t.nombre);
        h.tvCategoria.setText(t.etiquetaCategoria());
        h.chipPlazo.setVisibility(t.plazoAbierto() ? View.VISIBLE : View.GONE);
        h.itemView.setOnClickListener(v -> listener.alPulsar(t));
    }

    @Override
    public int getItemCount() {
        return tramites.size();
    }

    static class Holder extends RecyclerView.ViewHolder {
        final TextView tvNombre, tvCategoria;
        final Chip chipPlazo;

        Holder(View v) {
            super(v);
            tvNombre = v.findViewById(R.id.tvNombre);
            tvCategoria = v.findViewById(R.id.tvCategoria);
            chipPlazo = v.findViewById(R.id.chipPlazo);
        }
    }
}
