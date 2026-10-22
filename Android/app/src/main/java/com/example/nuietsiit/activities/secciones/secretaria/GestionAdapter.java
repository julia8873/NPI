package com.example.nuietsiit.activities.secciones.secretaria;

import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.nuietsiit.R;
import com.example.nuietsiit.data.modelo.Cita;
import com.example.nuietsiit.data.modelo.Solicitud;
import com.example.nuietsiit.util.FechasUtil;
import com.google.android.material.chip.Chip;

import java.util.ArrayList;
import java.util.List;

/**
 * Tarjetas de "Mis gestiones": sirve tanto para citas como para solicitudes.
 */
public class GestionAdapter extends RecyclerView.Adapter<GestionAdapter.Holder> {

    /** Lo que se pinta en una tarjeta, sacado de una Cita o de una Solicitud. */
    public static class Gestion {
        public final boolean esCita;
        public final int id;
        public final boolean cancelable; // solo citas activas y futuras
        final String codigo, titulo, detalle, estado;
        final int colorEstado;

        private Gestion(boolean esCita, int id, boolean cancelable, String codigo, String titulo,
                        String detalle, String estado, int colorEstado) {
            this.esCita = esCita;
            this.id = id;
            this.cancelable = cancelable;
            this.codigo = codigo;
            this.titulo = titulo;
            this.detalle = detalle;
            this.estado = estado;
            this.colorEstado = colorEstado;
        }

        public static Gestion deCita(Cita c) {
            boolean futura = c.esFutura();
            String detalle = FechasUtil.capitalizar(FechasUtil.diaLargo(c.fechaHora())) + " · " + c.hora;
            return new Gestion(true, c.id, futura, c.codigo(), c.motivo(), detalle,
                    futura ? "Activa" : "Pasada", futura ? 0xFFFF8C42 : 0xFF9E9E9E);
        }

        public static Gestion deSolicitud(Solicitud s) {
            String estado = s.estado();
            return new Gestion(false, s.id, false, s.codigo(), s.nombreTramite, s.textoEnvio(),
                    estado, Solicitud.colorEstado(estado));
        }
    }

    public interface OnGestionClick {
        void alPulsar(Gestion gestion);
    }

    private List<Gestion> gestiones = new ArrayList<>();
    private final OnGestionClick listener;

    public GestionAdapter(OnGestionClick listener) {
        this.listener = listener;
    }

    public void setGestiones(List<Gestion> nuevas) {
        gestiones = nuevas;
        notifyDataSetChanged(); // listas cortas
    }

    public Gestion get(int posicion) {
        return gestiones.get(posicion);
    }

    public void quitar(int posicion) {
        gestiones.remove(posicion);
        notifyItemRemoved(posicion);
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_gestion, parent, false);
        return new Holder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder h, int position) {
        Gestion g = gestiones.get(position);
        h.tvCodigo.setText(g.codigo);
        h.tvTitulo.setText(g.titulo);
        h.tvDetalle.setText(g.detalle);
        h.chipEstado.setText(g.estado);
        h.chipEstado.setChipBackgroundColor(ColorStateList.valueOf(g.colorEstado));
        h.itemView.setOnClickListener(v -> listener.alPulsar(g));
    }

    @Override
    public int getItemCount() {
        return gestiones.size();
    }

    static class Holder extends RecyclerView.ViewHolder {
        final TextView tvCodigo, tvTitulo, tvDetalle;
        final Chip chipEstado;

        Holder(View v) {
            super(v);
            tvCodigo = v.findViewById(R.id.tvCodigo);
            tvTitulo = v.findViewById(R.id.tvTitulo);
            tvDetalle = v.findViewById(R.id.tvDetalle);
            chipEstado = v.findViewById(R.id.chipEstado);
        }
    }
}
