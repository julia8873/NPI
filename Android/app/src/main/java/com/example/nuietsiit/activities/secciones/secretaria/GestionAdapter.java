package com.example.nuietsiit.activities.secciones.secretaria;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.core.view.ViewCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.nuietsiit.R;
import com.example.nuietsiit.data.modelo.Cita;
import com.example.nuietsiit.data.modelo.Solicitud;
import com.example.nuietsiit.util.FechasUtil;

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
        final String codigo, titulo, detalle;
        @StringRes final int estadoResId; // 0 si se usa estadoTexto
        final String estadoTexto;
        final int colorEstado; // recurso de color

        private Gestion(boolean esCita, int id, boolean cancelable, String codigo, String titulo,
                        String detalle, @StringRes int estadoResId, String estadoTexto, int colorEstado) {
            this.esCita = esCita;
            this.id = id;
            this.cancelable = cancelable;
            this.codigo = codigo;
            this.titulo = titulo;
            this.detalle = detalle;
            this.estadoResId = estadoResId;
            this.estadoTexto = estadoTexto;
            this.colorEstado = colorEstado;
        }

        public static Gestion deCita(Cita c) {
            boolean futura = c.esFutura();
            String detalle = FechasUtil.capitalizar(FechasUtil.diaLargo(c.fechaHora())) + " · " + c.hora;
            return new Gestion(
                    true,
                    c.id,
                    futura,
                    c.codigo(),
                    c.motivo(),
                    detalle,
                    futura ? R.string.gestion_estado_cita_activa : R.string.gestion_estado_cita_pasada,
                    null,
                    futura ? R.color.nui_acento_profundo : R.color.nui_apagado
            );
        }

        public static Gestion deSolicitud(Solicitud s) {
            String estado = s.estado();
            return new Gestion(
                    false,
                    s.id,
                    false,
                    s.codigo(),
                    s.nombreTramite,
                    s.textoEnvio(),
                    0,
                    estado,
                    Solicitud.colorEstado(estado)
            );
        }

        String obtenerEstado(Context ctx) {
            return estadoResId != 0 ? ctx.getString(estadoResId) : estadoTexto;
        }
    }

    public interface OnGestionClick {
        void alPulsar(Gestion gestion);
    }

    /** El resguardo de una cita se ha rasgado del todo: hay que cancelarla. */
    public interface OnRasgar {
        void alRasgar(int posicion, View vista);
    }

    private List<Gestion> gestiones = new ArrayList<>();
    private final OnGestionClick listener;
    @Nullable private final OnRasgar alRasgar;

    public GestionAdapter(OnGestionClick listener, @Nullable OnRasgar alRasgar) {
        this.listener = listener;
        this.alRasgar = alRasgar;
    }

    /** ¿Hay alguna cita con resguardo que rasgar? */
    public boolean hayCancelables() {
        for (Gestion g : gestiones) if (g.cancelable) return true;
        return false;
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
        Context ctx = h.itemView.getContext();
        Gestion g = gestiones.get(position);
        h.tvCodigo.setText(g.codigo);
        h.tvTitulo.setText(g.titulo);
        h.tvDetalle.setText(g.detalle);
        // el estado es un sello estampado en la ficha, del color del estado
        Pastilla.sellar(h.tvEstado, g.obtenerEstado(ctx), g.colorEstado);
        h.ficha.setOnClickListener(v -> listener.alPulsar(g));

        boolean rasgable = g.cancelable && alRasgar != null;
        h.resguardo.reiniciar();
        h.resguardo.setVisibility(rasgable ? View.VISIBLE : View.GONE);
        h.resguardo.alRasgar(rasgable ? () -> {
            int pos = h.getAdapterPosition();
            if (pos != RecyclerView.NO_POSITION) alRasgar.alRasgar(pos, h.itemView);
        } : null);
        if (h.accionCancelar != View.NO_ID) ViewCompat.removeAccessibilityAction(h.ficha, h.accionCancelar);
        h.accionCancelar = rasgable
                ? ViewCompat.addAccessibilityAction(
                h.ficha,
                ctx.getString(R.string.gestion_accion_cancelar_cita),
                (v, a) -> {
                    h.resguardo.completar();
                    return true;
                })
                : View.NO_ID;
    }

    @Override
    public int getItemCount() {
        return gestiones.size();
    }

    static class Holder extends RecyclerView.ViewHolder {
        final TextView tvCodigo, tvTitulo, tvDetalle;
        final TextView tvEstado;
        final View ficha;
        final ResguardoRasgable resguardo;
        int accionCancelar = View.NO_ID;

        Holder(View v) {
            super(v);
            ficha = v.findViewById(R.id.ficha);
            resguardo = v.findViewById(R.id.resguardo);
            tvCodigo = v.findViewById(R.id.tvCodigo);
            tvTitulo = v.findViewById(R.id.tvTitulo);
            tvDetalle = v.findViewById(R.id.tvDetalle);
            tvEstado = v.findViewById(R.id.tvEstado);
        }
    }
}