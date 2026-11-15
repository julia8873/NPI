package com.example.nuietsiit.activities.secciones.mapa;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.SystemClock;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.nuietsiit.R;

import java.util.function.Consumer;

/**
 * Gesto de la guía exterior "levanta el móvil para ver el camino": con el móvil en vertical la foto
 * del sitio ocupa toda la pantalla con la flecha grande (realidad aumentada ligera); al bajarlo,
 * vuelve el mapa. Boca abajo avisa aparte (la guía lo usa para silenciar la voz).
 * La vista se pone encima de todo (android.R.id.content) y copia lo que enseña la guía.
 */
public final class VistaLevantada implements SensorEventListener {

    private final AppCompatActivity act;
    private final SensorManager sensores;
    private final Sensor acelerometro;
    private final Consumer<Postura.Tipo> alCambiar;
    private Postura postura = new Postura();
    private View vista;
    private ImageView ivFoto, ivFlecha;
    private TextView tvInstruccion, tvMetros;
    private String claveFoto;

    /** @param alCambiar se llama con cada postura nueva (después de mostrar u ocultar la vista) */
    public VistaLevantada(AppCompatActivity act, Consumer<Postura.Tipo> alCambiar) {
        this.act = act;
        this.alCambiar = alCambiar;
        sensores = (SensorManager) act.getSystemService(Context.SENSOR_SERVICE);
        acelerometro = sensores == null ? null : sensores.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
    }

    public void iniciar() {
        if (acelerometro == null) return;
        postura = new Postura();
        sensores.registerListener(this, acelerometro, SensorManager.SENSOR_DELAY_UI);
    }

    public void parar() {
        if (sensores != null) sensores.unregisterListener(this);
        ocultar();
    }

    public boolean visible() {
        return vista != null && vista.getVisibility() == View.VISIBLE;
    }

    @Override
    public void onSensorChanged(SensorEvent e) {
        Postura.Tipo nueva = postura.medir(e.values[0], e.values[1], e.values[2], SystemClock.elapsedRealtime());
        if (nueva == null) return;
        if (nueva == Postura.Tipo.LEVANTADO) mostrar();
        else ocultar();
        alCambiar.accept(nueva);
    }

    @Override public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    /** Copia foto (clave de FotoLugar, a toda la pantalla), instrucción y distancia de la guía, si se está viendo. */
    public void copiar(String clave, TextView instruccion, TextView metros) {
        if (!visible()) return;
        if (clave == null) {
            ivFoto.setImageDrawable(null);
        } else if (!clave.equals(claveFoto)) {
            ivFoto.setImageDrawable(FotoLugar.deClave(act, clave, act.getResources().getDisplayMetrics().widthPixels));
        }
        claveFoto = clave;
        tvInstruccion.setText(instruccion.getText());
        tvMetros.setText(metros.getText());
    }

    /** Copia el giro de la flecha pequeña de la guía. */
    public void girar(ImageView flecha) {
        if (!visible()) return;
        ivFlecha.setVisibility(flecha.getVisibility());
        ivFlecha.setRotation(flecha.getRotation());
    }

    private void mostrar() {
        if (vista == null) {
            ViewGroup raiz = act.findViewById(android.R.id.content);
            vista = LayoutInflater.from(act).inflate(R.layout.vista_levantada, raiz, false);
            ivFoto = vista.findViewById(R.id.ivLevantadaFoto);
            ivFlecha = vista.findViewById(R.id.ivLevantadaFlecha);
            tvInstruccion = vista.findViewById(R.id.tvLevantadaInstruccion);
            tvMetros = vista.findViewById(R.id.tvLevantadaMetros);
            vista.setOnClickListener(v -> ocultar());
            raiz.addView(vista);
        }
        vista.setAlpha(0f);
        vista.setVisibility(View.VISIBLE);
        vista.animate().alpha(1f).setDuration(250).start();
    }

    public void ocultar() {
        if (visible()) vista.setVisibility(View.GONE);
    }
}
