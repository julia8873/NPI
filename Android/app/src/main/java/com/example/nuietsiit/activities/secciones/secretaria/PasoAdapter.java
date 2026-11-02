package com.example.nuietsiit.activities.secciones.secretaria;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.nuietsiit.R;
import com.example.nuietsiit.data.modelo.Paso;

import java.util.List;

/**
 * Tarjetas "Paso i de N" del ViewPager2. La última lleva un botón de acción (solicitar / pedir cita).
 */
public class PasoAdapter extends RecyclerView.Adapter<PasoAdapter.Holder> {

    private final List<Paso> pasos;
    private final String textoAccionFinal;
    private final View.OnClickListener accionFinal;

    public PasoAdapter(List<Paso> pasos, String textoAccionFinal, View.OnClickListener accionFinal) {
        this.pasos = pasos;
        this.textoAccionFinal = textoAccionFinal;
        this.accionFinal = accionFinal;
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_paso, parent, false);
        return new Holder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder h, int position) {
        Paso p = pasos.get(position);
        h.tvNumero.setText("Paso " + (position + 1) + " de " + pasos.size());
        h.tvTitulo.setText(p.titulo);
        h.tvDescripcion.setText(p.descripcion);

        boolean ultimo = position == pasos.size() - 1;
        h.btnAccionFinal.setVisibility(ultimo ? View.VISIBLE : View.GONE);
        h.btnAccionFinal.setText(textoAccionFinal);
        h.btnAccionFinal.setOnClickListener(accionFinal);
    }

    @Override
    public int getItemCount() {
        return pasos.size();
    }

    static class Holder extends RecyclerView.ViewHolder {
        final TextView tvNumero, tvTitulo, tvDescripcion;
        final Button btnAccionFinal;

        Holder(View v) {
            super(v);
            tvNumero = v.findViewById(R.id.tvNumero);
            tvTitulo = v.findViewById(R.id.tvTitulo);
            tvDescripcion = v.findViewById(R.id.tvDescripcion);
            btnAccionFinal = v.findViewById(R.id.btnAccionFinal);
        }
    }
}
