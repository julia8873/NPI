package com.example.nuietsiit.activities.secciones.actividades;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.ComponentActivity;
import androidx.activity.OnBackPressedCallback;

import com.example.nuietsiit.R;
import com.google.zxing.BarcodeFormat;
import com.journeyapps.barcodescanner.BarcodeEncoder;

/**
 * "Pásaselo a un amigo": QR grande de una actividad a pantalla completa, para que otro lo escanee con su app
 * (Actividades → Escanear) y le salga esa actividad para apuntarse. Lleva el mismo texto que los carteles de la
 * escuela (Actividades.PREFIJO_QR + título). Se pone encima de todo (android.R.id.content) con el brillo al
 * máximo, para que la cámara del otro lo lea bien. Al darle la vuelta a la ficha (deslizarla hacia arriba) aparece
 * girando en 3D, como el reverso de la ficha; se quita tocándolo o con "Atrás" y la ficha se vuelve a dar la vuelta.
 */
public final class QrParaEnsenar {

    private final Activity act;
    private View vista;
    private float brilloAntes;
    private boolean volteado;
    private Runnable alCerrar;
    // "Atrás" con el QR fuera lo quita, en vez de salir de la pantalla
    private final OnBackPressedCallback atras = new OnBackPressedCallback(false) {
        @Override public void handleOnBackPressed() { quitar(); }
    };

    public QrParaEnsenar(Activity act) {
        this.act = act;
        if (act instanceof ComponentActivity) {
            ((ComponentActivity) act).getOnBackPressedDispatcher().addCallback((ComponentActivity) act, atras);
        }
    }

    public boolean visible() {
        return vista != null;
    }

    /** volteando: entra girando en 3D (el reverso de la ficha); alCerrar se llama al quitarlo (puede ser null). */
    public boolean mostrar(String contenido, String titulo, boolean volteando, Runnable alCerrar) {
        if (vista != null) quitar();
        ViewGroup contenidoRaiz = act.findViewById(android.R.id.content);
        int lado = Math.round(Math.min(contenidoRaiz.getWidth(), contenidoRaiz.getHeight()) * 0.78f);
        if (lado <= 0) return false;
        Bitmap qr;
        try {
            qr = new BarcodeEncoder().encodeBitmap(contenido, BarcodeFormat.QR_CODE, lado, lado);
        } catch (Exception e) {
            return false;
        }
        float dp = act.getResources().getDisplayMetrics().density;

        LinearLayout caja = new LinearLayout(act);
        caja.setOrientation(LinearLayout.VERTICAL);
        caja.setGravity(Gravity.CENTER);
        caja.setBackgroundColor(Color.WHITE);
        caja.setPadding(Math.round(24 * dp), Math.round(24 * dp), Math.round(24 * dp), Math.round(24 * dp));
        caja.setClickable(true); // que no pase el toque a lo de debajo
        caja.setOnClickListener(v -> quitar());

        TextView tvTitulo = new TextView(act);
        tvTitulo.setText(titulo);
        tvTitulo.setTextAppearance(R.style.TextAppearance_Seccion_Titulo);
        tvTitulo.setGravity(Gravity.CENTER);
        caja.addView(tvTitulo);

        ImageView iv = new ImageView(act);
        iv.setImageBitmap(qr);
        iv.setContentDescription(act.getString(R.string.nui_act_qr_ensenar_desc, titulo));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(lado, lado);
        lp.topMargin = lp.bottomMargin = Math.round(20 * dp);
        caja.addView(iv, lp);

        TextView tvPista = new TextView(act);
        tvPista.setText(R.string.nui_act_qr_ensenar_pista);
        tvPista.setTextAppearance(R.style.TextAppearance_Seccion_Cuerpo);
        tvPista.setGravity(Gravity.CENTER);
        caja.addView(tvPista);

        contenidoRaiz.addView(caja, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        volteado = volteando;
        this.alCerrar = alCerrar;
        if (volteando) {
            caja.setCameraDistance(8000 * dp);
            caja.setRotationX(-90f);
            caja.animate().rotationX(0f).setDuration(180).start();
        }
        vista = caja;
        atras.setEnabled(true);

        WindowManager.LayoutParams w = act.getWindow().getAttributes();
        brilloAntes = w.screenBrightness;
        w.screenBrightness = 1f;
        act.getWindow().setAttributes(w);
        return true;
    }

    public void quitar() {
        if (vista == null) return;
        View v = vista;
        vista = null;
        atras.setEnabled(false);
        WindowManager.LayoutParams w = act.getWindow().getAttributes();
        w.screenBrightness = brilloAntes;
        act.getWindow().setAttributes(w);
        Runnable fin = alCerrar;
        alCerrar = null;
        Runnable quitarDeVerdad = () -> {
            if (v.getParent() != null) ((ViewGroup) v.getParent()).removeView(v);
            if (fin != null) fin.run();
        };
        if (volteado && v.isAttachedToWindow()) {
            v.animate().rotationX(90f).setDuration(180).withEndAction(quitarDeVerdad).start();
        } else {
            quitarDeVerdad.run();
        }
    }
}
