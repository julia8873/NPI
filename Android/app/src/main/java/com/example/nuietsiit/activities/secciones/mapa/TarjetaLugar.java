package com.example.nuietsiit.activities.secciones.mapa;

import android.view.HapticFeedbackConstants;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;

import com.example.nuietsiit.R;
import com.google.android.material.button.MaterialButton;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

/**
 * Tarjeta inferior al tocar un lugar del mapa (contorno o etiqueta): foto, nombre, cuánto se tarda desde
 * donde estás y "Llévame aquí". Si ya estás dentro, pregunta a qué sitio de dentro vas.
 */
public final class TarjetaLugar {

    /** Lo que hace el mapa con el botón pulsado. */
    public interface Acciones {
        /** Abrir "Cómo llegar" hacia el lugar. */
        void llevame(String idLugar);

        /** Cambiar a los planos del edificio (solo la ETSIIT). */
        void verPorDentro();

        /** Ya estás en el edificio: ¿a qué sitio de dentro vas? */
        void dentro(String idLugar);
    }

    /** Sin contorno (residencia), "ya estás aquí" = a menos de esto de su punto. */
    private static final double CERCA_M = 60;

    private final View vista;
    private final ViewGroup contenedor;
    private final OnBackPressedCallback atras;

    private TarjetaLugar(AppCompatActivity act, ViewGroup contenedor, View vista) {
        this.contenedor = contenedor;
        this.vista = vista;
        atras = new OnBackPressedCallback(true) {
            @Override public void handleOnBackPressed() { cerrar(); }
        };
        act.getOnBackPressedDispatcher().addCallback(act, atras);
    }

    /**
     * Enseña la tarjeta del lugar abajo del contenedor del mapa (sustituye a la que hubiera).
     *
     * @param posicion dónde estás (null = sin GPS: no se dice el tiempo)
     */
    public static TarjetaLugar mostrar(AppCompatActivity act, ViewGroup contenedor, RedTransporte red,
                                       String idLugar, Punto posicion, LocalDateTime ahora, Acciones acciones) {
        RedTransporte.Lugar lugar = red.lugar(idLugar);
        if (lugar == null) return null;
        cerrarEn(contenedor);

        View vista = LayoutInflater.from(act).inflate(R.layout.tarjeta_lugar, contenedor, false);
        TarjetaLugar tarjeta = new TarjetaLugar(act, contenedor, vista);
        vista.setTag(tarjeta);

        ImageView ivFoto = vista.findViewById(R.id.ivTarjetaLugarFoto);
        TextView tvPregunta = vista.findViewById(R.id.tvTarjetaLugarPregunta);
        TextView tvNombre = vista.findViewById(R.id.tvTarjetaLugarNombre);
        TextView tvTiempo = vista.findViewById(R.id.tvTarjetaLugarTiempo);
        MaterialButton btnLlevame = vista.findViewById(R.id.btnTarjetaLugarLlevame);
        MaterialButton btnDentro = vista.findViewById(R.id.btnTarjetaLugarDentro);

        int ancho = act.getResources().getDisplayMetrics().widthPixels;
        List<String> fotos = FotoLugar.deLugar(act, lugar.id);
        ivFoto.setImageDrawable(FotoLugar.deClave(act, lugar.id, ancho));
        ivFoto.setOnClickListener(v -> VisorFotoExterior.abrir(act, fotos, 0));
        tvNombre.setText(lugar.icono + " " + lugar.nombre);
        vista.findViewById(R.id.btnTarjetaLugarCerrar).setOnClickListener(v -> tarjeta.cerrar());

        boolean conPlanos = ProximaClase.ETSIIT.equals(lugar.id) || ProximaClase.AUXILIAR.equals(lugar.id);
        if (posicion != null && estasEn(lugar, posicion)) {
            tvPregunta.setText("Ya estás aquí");
            if (conPlanos) {
                tvTiempo.setText("¿A qué sitio de dentro vas?");
                btnLlevame.setText("Elegir sitio de dentro");
                btnLlevame.setOnClickListener(v -> { tarjeta.cerrar(); acciones.dentro(lugar.id); });
            } else {
                tvTiempo.setText("Estás en " + lugar.corto);
                btnLlevame.setVisibility(View.GONE);
            }
        } else {
            tvPregunta.setText("¿Te llevo?");
            tvTiempo.setText(posicion != null
                    ? tiempo(new PlanificadorRuta(red).planificar(posicion, lugar, ahora))
                    : "📍 Sin ubicación: te lo calculo al abrir la ruta");
            btnLlevame.setOnClickListener(v -> { tarjeta.cerrar(); acciones.llevame(lugar.id); });
        }
        if (ProximaClase.ETSIIT.equals(lugar.id)) {
            btnDentro.setVisibility(View.VISIBLE);
            btnDentro.setOnClickListener(v -> { tarjeta.cerrar(); acciones.verPorDentro(); });
        }

        contenedor.addView(vista);
        vista.setTranslationY(400 * act.getResources().getDisplayMetrics().density);
        vista.animate().translationY(0).setDuration(220).start();
        vista.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK);
        return tarjeta;
    }

    /** Cierra la tarjeta abierta en ese contenedor, si la hay. */
    public static void cerrarEn(ViewGroup contenedor) {
        View vieja = contenedor.findViewById(R.id.tarjetaLugar);
        if (vieja != null && vieja.getTag() instanceof TarjetaLugar) ((TarjetaLugar) vieja.getTag()).cerrar();
    }

    public void cerrar() {
        atras.remove();
        contenedor.removeView(vista);
    }

    static boolean estasEn(RedTransporte.Lugar lugar, Punto posicion) {
        return lugar.contiene(posicion) || posicion.distanciaA(lugar.punto) <= CERCA_M;
    }

    /** "🚶 A 12 min a pie" / "🚌 15 min en bus · sal a las 8:33" / "Está a 4,2 km". */
    static String tiempo(PlanRuta plan) {
        switch (plan.tipo) {
            case A_PIE:
                return "🚶 A " + plan.minutosTotales() + " min a pie";
            case BUS:
                return "🚌 " + plan.minutosTotales() + " min en bus · sal a las " + PlanRuta.hora(plan.salida);
            default:
                return String.format(Locale.getDefault(), "Está a %.1f km de ti", plan.metrosRecta / 1000);
        }
    }
}
