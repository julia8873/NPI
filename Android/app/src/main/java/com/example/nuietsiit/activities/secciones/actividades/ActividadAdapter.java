package com.example.nuietsiit.activities.secciones.actividades;

import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.nuietsiit.R;
import com.example.nuietsiit.data.modelo.Actividad;
import com.example.nuietsiit.util.FechasUtil;

import java.util.ArrayList;
import java.util.List;

/** Pinta cada actividad de la lista con el diseño de item_actividad.xml */
public class ActividadAdapter extends RecyclerView.Adapter<ActividadAdapter.Holder> {

    public interface OnActividadClick {
        void alPulsar(Actividad actividad);
    }

    private static final int ROJO = 0xFFD32F2F;
    private static final int AZUL = 0xFF1565C0;

    private List<Actividad> actividades = new ArrayList<>();
    private final OnActividadClick listener;

    public ActividadAdapter(OnActividadClick listener) {
        this.listener = listener;
    }

    public void setActividades(List<Actividad> nuevas) {
        actividades = nuevas;
        notifyDataSetChanged(); // lista corta (~10 elementos)
    }

    public Actividad get(int posicion) {
        return actividades.get(posicion);
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_actividad, parent, false);
        return new Holder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder h, int posicion) {
        Actividad a = actividades.get(posicion);

        // Bloque de fecha con el color del tipo
        h.bloqueFecha.setBackgroundTintList(ColorStateList.valueOf(Actividad.color(a.tipo)));
        h.tvDia.setText(String.valueOf(a.inicio.getDayOfMonth()));
        h.tvMes.setText(FechasUtil.mesCorto(a.inicio));

        h.tvTitulo.setText(a.titulo);
        h.tvTipo.setText(Actividad.emoji(a.tipo) + " " + Actividad.nombreTipo(a.tipo)
                + " · " + FechasUtil.cuando(a.inicio) + ", " + FechasUtil.hora(a.inicio));

        // Cursos AbiertaUGR: créditos + cuenta atrás del plazo (en rojo si quedan ≤3 días)
        if (a.esAbiertaUGR()) {
            h.tvExtra.setVisibility(View.VISIBLE);
            h.tvExtra.setText("🎓 " + a.creditosTexto() + " · " + a.estadoInscripcion());
            h.tvExtra.setTextColor(a.inscripcionUrgente() ? ROJO : AZUL);
        } else {
            h.tvExtra.setVisibility(View.GONE);
        }

        h.tvInscrito.setVisibility(a.inscrito ? View.VISIBLE : View.GONE);
        h.itemView.setOnClickListener(v -> listener.alPulsar(a));
    }

    @Override
    public int getItemCount() {
        return actividades.size();
    }

    static class Holder extends RecyclerView.ViewHolder {
        final View bloqueFecha;
        final TextView tvDia, tvMes, tvTitulo, tvTipo, tvExtra, tvInscrito;

        Holder(View v) {
            super(v);
            bloqueFecha = v.findViewById(R.id.bloqueFecha);
            tvDia = v.findViewById(R.id.tvDia);
            tvMes = v.findViewById(R.id.tvMes);
            tvTitulo = v.findViewById(R.id.tvTitulo);
            tvTipo = v.findViewById(R.id.tvTipo);
            tvExtra = v.findViewById(R.id.tvExtra);
            tvInscrito = v.findViewById(R.id.tvInscrito);
        }
    }
}
