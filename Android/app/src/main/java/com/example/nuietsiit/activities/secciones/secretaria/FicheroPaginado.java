package com.example.nuietsiit.activities.secciones.secretaria;

import android.content.Context;
import android.util.AttributeSet;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.recyclerview.widget.RecyclerView;

import com.example.nuietsiit.util.tutorial.Gesto;
import com.example.nuietsiit.util.tutorial.Tutorial;

import java.util.ArrayList;
import java.util.List;

/**
 * Cajón del fichero sin scroll: enseña solo las fichas que caben y las demás se pasan como en un fichero
 * de verdad, deslizando de lado (o con {@link #pasar(int)} desde las flechas).
 * Las fichas las crea y rellena el mismo adaptador de la lista (TramiteAdapter).
 */
public class FicheroPaginado extends LinearLayout {

    public interface OnPagina {
        void alCambiar(int pagina, int paginas);
    }

    private RecyclerView.Adapter<RecyclerView.ViewHolder> adaptador;
    private OnPagina alCambiarPagina;
    private final List<Integer> inicios = new ArrayList<>(); // primera ficha de cada página
    private int pagina = 0;

    private final int umbralToque;
    private float xInicio, yInicio;
    private boolean arrastrando;

    public FicheroPaginado(Context context, AttributeSet attrs) {
        super(context, attrs);
        setOrientation(VERTICAL);
        umbralToque = ViewConfiguration.get(context).getScaledTouchSlop();
    }

    @SuppressWarnings("unchecked")
    public void setAdaptador(RecyclerView.Adapter<? extends RecyclerView.ViewHolder> nuevo, OnPagina alCambiar) {
        adaptador = (RecyclerView.Adapter<RecyclerView.ViewHolder>) nuevo;
        alCambiarPagina = alCambiar;
        adaptador.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override public void onChanged() {
                pagina = 0; // otro filtro: se vuelve a la primera ficha
                post(FicheroPaginado.this::paginar);
            }
        });
    }

    public int getPaginas() {
        return inicios.size();
    }

    /** Pasa las fichas hacia delante (+1) o hacia atrás (-1). */
    public void pasar(int sentido) {
        int nueva = pagina + sentido;
        if (nueva < 0 || nueva >= inicios.size()) {
            animate().translationX(0).setDuration(150).start(); // tope: vuelve a su sitio
            return;
        }
        performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);
        float ancho = getWidth();
        animate().translationX(-sentido * ancho).alpha(0f).setDuration(140).withEndAction(() -> {
            pagina = nueva;
            pintarPagina();
            setTranslationX(sentido * ancho * 0.4f);
            animate().translationX(0).alpha(1f).setDuration(180).start();
        }).start();
    }

    @Override
    protected void onSizeChanged(int w, int h, int wViejo, int hViejo) {
        super.onSizeChanged(w, h, wViejo, hViejo);
        if (h != hViejo || w != wViejo) post(this::paginar); // al abrir el teclado o girar caben otras
    }

    // mide cada ficha con el ancho real y corta la página cuando ya no cabe la siguiente
    private void paginar() {
        if (adaptador == null || getWidth() == 0) return;
        int alto = getHeight() - getPaddingTop() - getPaddingBottom();
        int anchoSpec = MeasureSpec.makeMeasureSpec(getWidth() - getPaddingLeft() - getPaddingRight(), MeasureSpec.EXACTLY);
        int altoSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);

        inicios.clear();
        int usado = 0;
        for (int i = 0; i < adaptador.getItemCount(); i++) {
            View ficha = crearFicha(i);
            ficha.measure(anchoSpec, altoSpec);
            MarginLayoutParams lp = (MarginLayoutParams) ficha.getLayoutParams();
            int altoFicha = ficha.getMeasuredHeight() + lp.topMargin + lp.bottomMargin;
            if (inicios.isEmpty() || usado + altoFicha > alto) { // al menos una ficha por página
                inicios.add(i);
                usado = 0;
            }
            usado += altoFicha;
        }
        pagina = Math.max(0, Math.min(pagina, inicios.size() - 1));
        pintarPagina();
    }

    private void pintarPagina() {
        removeAllViews();
        if (!inicios.isEmpty()) {
            int fin = pagina + 1 < inicios.size() ? inicios.get(pagina + 1) : adaptador.getItemCount();
            for (int i = inicios.get(pagina); i < fin; i++) addView(crearFicha(i));
        }
        if (alCambiarPagina != null) alCambiarPagina.alCambiar(pagina, inicios.size());
    }

    private View crearFicha(int posicion) {
        RecyclerView.ViewHolder holder = adaptador.createViewHolder(this, adaptador.getItemViewType(posicion));
        adaptador.bindViewHolder(holder, posicion);
        View v = holder.itemView;
        if (v.getLayoutParams() == null) v.setLayoutParams(generateDefaultLayoutParams());
        return v;
    }

    // ---------- deslizar de lado para pasar fichas (las fichas siguen recibiendo los toques) ----------

    @Override
    public boolean onInterceptTouchEvent(MotionEvent e) {
        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                xInicio = e.getX();
                yInicio = e.getY();
                arrastrando = false;
                break;
            case MotionEvent.ACTION_MOVE:
                float dx = e.getX() - xInicio, dy = e.getY() - yInicio;
                if (inicios.size() > 1 && Math.abs(dx) > umbralToque && Math.abs(dx) > Math.abs(dy)) {
                    arrastrando = true;
                    getParent().requestDisallowInterceptTouchEvent(true);
                }
                break;
        }
        return arrastrando;
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        float dx = e.getX() - xInicio;
        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                return inicios.size() > 1;
            case MotionEvent.ACTION_MOVE:
                if (arrastrando) setTranslationX(dx * 0.6f); // la página sigue al dedo, con algo de resistencia
                return true;
            case MotionEvent.ACTION_UP:
                if (arrastrando && Math.abs(dx) > getWidth() / 6f) {
                    pasar(dx < 0 ? 1 : -1);
                    Tutorial.avisar(getContext(), dx < 0 ? Gesto.DESLIZAR_IZQ : Gesto.DESLIZAR_DER);
                } else animate().translationX(0).setDuration(150).start();
                arrastrando = false;
                return true;
            case MotionEvent.ACTION_CANCEL:
                animate().translationX(0).setDuration(150).start();
                arrastrando = false;
                return true;
        }
        return super.onTouchEvent(e);
    }

    @Override
    protected LayoutParams generateDefaultLayoutParams() {
        return new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    }
}
