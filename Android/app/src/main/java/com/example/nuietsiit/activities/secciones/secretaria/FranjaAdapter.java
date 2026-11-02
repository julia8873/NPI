package com.example.nuietsiit.activities.secciones.secretaria;

import android.content.res.ColorStateList;
import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.nuietsiit.R;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

/**
 * Rejilla de franjas horarias de CitaPrevia. Las ocupadas salen tachadas y no se pueden pulsar;
 * la del usuario (si ya tiene cita para el motivo ese día) sale rellena.
 */
public class FranjaAdapter extends RecyclerView.Adapter<FranjaAdapter.Holder> {

    public static final int LIBRE = 0;
    public static final int OCUPADA = 1;
    public static final int TUYA = 2;

    private static final int NARANJA = 0xFFFF8C42;
    private static final int GRIS = 0xFFBBBBBB;

    public interface OnFranjaPulsada {
        void alPulsar(String hora);
    }

    public static class Franja {
        final String hora;  // "HH:mm"
        final int estado;   // LIBRE | OCUPADA | TUYA

        public Franja(String hora, int estado) {
            this.hora = hora;
            this.estado = estado;
        }
    }

    private final OnFranjaPulsada listener;
    private List<Franja> franjas = new ArrayList<>();

    public FranjaAdapter(OnFranjaPulsada listener) {
        this.listener = listener;
    }

    public void setFranjas(List<Franja> franjas) {
        this.franjas = franjas;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_franja, parent, false);
        return new Holder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder h, int position) {
        String hora = franjas.get(position).hora;
        int estado = franjas.get(position).estado;
        MaterialButton b = h.btnFranja;
        b.setText(hora);

        boolean tachada = estado == OCUPADA;
        b.setPaintFlags(tachada ? b.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG
                : b.getPaintFlags() & ~Paint.STRIKE_THRU_TEXT_FLAG);
        b.setEnabled(estado == LIBRE);
        b.setTextColor(estado == TUYA ? 0xFFFFFFFF : tachada ? GRIS : NARANJA);
        b.setBackgroundTintList(ColorStateList.valueOf(estado == TUYA ? NARANJA : 0x00000000));
        b.setStrokeColor(ColorStateList.valueOf(tachada ? GRIS : NARANJA));
        b.setContentDescription(hora + (estado == OCUPADA ? ", ocupada" : estado == TUYA ? ", tu cita" : ", libre"));
        b.setOnClickListener(v -> listener.alPulsar(hora));
    }

    @Override
    public int getItemCount() {
        return franjas.size();
    }

    static class Holder extends RecyclerView.ViewHolder {
        final MaterialButton btnFranja;

        Holder(View v) {
            super(v);
            btnFranja = v.findViewById(R.id.btnFranja);
        }
    }
}
