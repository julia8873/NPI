package com.example.nuietsiit.activities.secciones.mapa;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.nuietsiit.activities.BaseActivity;
import com.example.nuietsiit.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Visor de ruta: pantalla interactiva que guía al usuario mediante giros y pasos.
 */
public class VisorRuta extends BaseActivity implements Brujula.Listener, SensorEventListener {

    private ImageView ivFotoRuta;
    private TextView tvRumbo;
    private TextView tvEstadoFoto;
    private SeekBar sbRumboManual;
    private View btnAnterior, btnSiguiente;

    private Brujula brujula;
    private List<FotosRuta.Foto> fotos;
    private int recursoMostrado = 0;
    private boolean modoManual = false;

    // --- Control automático de ruta guiada ---
    private boolean esRutaComedor = false;
    private int indiceComedorActual = 1; // de 1 a 33
    private int pasosDados = 0;
    private int pasosRequeridosNodo = 3;
    private float rumboRequeridoNodo = 0f;
    private boolean esperandoGiro = false;
    private final List<Integer> recursosComedor = new ArrayList<>();

    // Indica cuántos pasos dar en cada foto/nodo (0 si es un nodo exclusivo de giro)
    private final int[] pasosPorFoto = {
            3, 0, 6, 7, 0, 5, 0, 4, 0, 5,
            0, 7, 0, 6, 6, 8, 4, 0, 6, 8,
            0, 3, 7, 8, 0, 4, 6, 0, 5, 5,
            0, 6
    };

    // Ángulos/Rumbos requeridos para los giros (0..359 grados)
    private final float[] rumbosGiroPorFoto = {
            0f,   90f, 0f, 0f,   180f,  0f,   135f, 0f,   180f,  0f,
            135f, 0f,   180f, 0f,  0f,   0f,   0f,   270f,  0f,   0f,
            180f, 0f,   0f,  0f,  270f,  0f,   0f,   0f,  0f,   0f,
            90f,  0f
    };

    private SensorManager sensorManager;
    private Sensor acelerometro;
    private long ultimoPasoTimestamp = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.visor_ruta);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        inicializarVistas();

        String origenId = getIntent().getStringExtra("ORIGEN_ID");
        esRutaComedor = (origenId != null && origenId.equalsIgnoreCase("comedor"));

        if (esRutaComedor) {
            configurarRutaComedor();
        } else {
            fotos = FotosRuta.cargarConFallback(this, origenId == null ? FotosRuta.CLAVE_DEMO : origenId);
        }

        brujula = new Brujula(this, this);
        configurarControlesManuales();
    }

    private void inicializarVistas() {
        ivFotoRuta = findViewById(R.id.ivFotoRuta);
        tvRumbo = findViewById(R.id.tvRumbo);
        tvEstadoFoto = findViewById(R.id.tvEstadoFoto);
        sbRumboManual = findViewById(R.id.sbRumboManual);
        btnAnterior = findViewById(R.id.btnAnterior);
        btnSiguiente = findViewById(R.id.btnSiguiente);

        View btnCerrar = findViewById(R.id.cardCerrarCruz);
        if (btnCerrar != null) {
            btnCerrar.setOnClickListener(v -> {
                v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
                finish();
            });
        }
    }

    private void configurarRutaComedor() {
        btnAnterior.setVisibility(View.VISIBLE);
        btnSiguiente.setVisibility(View.VISIBLE);
        tvRumbo.setVisibility(View.GONE);

        btnAnterior.setOnClickListener(v -> {
            v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
            forzarFotoAnterior();
        });

        btnSiguiente.setOnClickListener(v -> {
            v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
            forzarFotoSiguiente();
        });

        for (int i = 1; i <= 33; i++) {
            int id = getResources().getIdentifier("ruta_comedor_" + i, "drawable", getPackageName());
            if (id != 0) recursosComedor.add(id);
        }

        if (!recursosComedor.isEmpty()) {
            ivFotoRuta.setImageResource(recursosComedor.get(0));
            ivFotoRuta.animate().alpha(1f).setDuration(150).start();
        }

        configurarNodoComedorActual();

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager != null) {
            acelerometro = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        }
    }

    private void configurarControlesManuales() {
        tvRumbo.setOnLongClickListener(v -> {
            activarModoManual(sbRumboManual.getVisibility() != View.VISIBLE);
            return true;
        });

        sbRumboManual.setMax(359);
        sbRumboManual.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (modoManual) mostrarRumbo(progress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) { }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) { }
        });

        if (!brujula.disponible()) {
            tvEstadoFoto.setText("Sin sensor de orientación: usa el control manual");
            activarModoManual(true);
        } else if (!esRutaComedor) {
            tvEstadoFoto.setText("Esperando al sensor de orientación...");
        }
    }

    private void avanzarPasoComedor() {
        if (!esRutaComedor) return;
        if (esperandoGiro) {
            Toast.makeText(this, "⚠️ Primero debes girar hacia la dirección indicada", Toast.LENGTH_SHORT).show();
            return;
        }

        pasosDados++;
        if (pasosDados >= pasosRequeridosNodo) {
            avanzarSiguienteFotoComedor();
        } else {
            actualizarTextoComedor();
        }
    }

    private void avanzarSiguienteFotoComedor() {
        pasosDados = 0;
        esperandoGiro = false;
        indiceComedorActual++;

        if (indiceComedorActual > recursosComedor.size()) {
            tvEstadoFoto.setText("🎉 ¡Has llegado al Comedor de la ETSIIT!");
            Toast.makeText(this, "¡Llegaste al Comedor!", Toast.LENGTH_LONG).show();
        } else {
            ivFotoRuta.setImageResource(recursosComedor.get(indiceComedorActual - 1));
            configurarNodoComedorActual();
        }
    }

    private void forzarFotoAnterior() {
        if (!esRutaComedor) return;
        if (indiceComedorActual > 1) {
            indiceComedorActual--;
            pasosDados = 0;
            esperandoGiro = false;
            ivFotoRuta.setImageResource(recursosComedor.get(indiceComedorActual - 1));
            configurarNodoComedorActual();
        }
    }

    private void forzarFotoSiguiente() {
        if (!esRutaComedor) return;
        if (indiceComedorActual < recursosComedor.size()) {
            avanzarSiguienteFotoComedor();
        } else {
            tvEstadoFoto.setText("🎉 ¡Has llegado al Comedor de la ETSIIT!");
            Toast.makeText(this, "¡Llegaste al Comedor!", Toast.LENGTH_LONG).show();
        }
    }

    private void configurarNodoComedorActual() {
        int index = indiceComedorActual - 1;
        if (index >= 0 && index < pasosPorFoto.length) {
            pasosRequeridosNodo = pasosPorFoto[index];
            esperandoGiro = (pasosRequeridosNodo == 0);
            if (esperandoGiro) {
                rumboRequeridoNodo = rumbosGiroPorFoto[index];
            }
        } else {
            pasosRequeridosNodo = 3;
            esperandoGiro = false;
        }
        actualizarTextoComedor();
    }

    private void actualizarTextoComedor() {
        if (!esRutaComedor) return;
        if (indiceComedorActual > recursosComedor.size()) {
            tvEstadoFoto.setText("🎉 ¡Destino alcanzado: Comedor ETSIIT!");
            return;
        }
        if (esperandoGiro) {
            tvEstadoFoto.setText("🔄 ¡Gira el móvil hacia la dirección indicada!");
        } else if (pasosRequeridosNodo == 0) {
            tvEstadoFoto.setText("🔄 ¡Gira para continuar!");
        } else {
            int restantes = pasosRequeridosNodo - pasosDados;
            tvEstadoFoto.setText(String.format(Locale.getDefault(), "🚶 Avanza %d pasos al frente", restantes));
        }
    }

    private void activarModoManual(boolean activo) {
        modoManual = activo;
        sbRumboManual.setVisibility(activo ? View.VISIBLE : View.GONE);
        if (activo && !esRutaComedor) mostrarRumbo(sbRumboManual.getProgress());
    }

    @Override
    protected void onResume() {
        super.onResume();
        brujula.iniciar();
        if (sensorManager != null && acelerometro != null) {
            sensorManager.registerListener(this, acelerometro, SensorManager.SENSOR_DELAY_UI);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        brujula.parar();
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER && esRutaComedor && !esperandoGiro) {
            float x = event.values[0];
            float y = event.values[1];
            float z = event.values[2];

            float gX = x / SensorManager.GRAVITY_EARTH;
            float gY = y / SensorManager.GRAVITY_EARTH;
            float gZ = z / SensorManager.GRAVITY_EARTH;

            float gForce = (float) Math.sqrt(gX * gX + gY * gY + gZ * gZ);

            if (gForce > 1.12f) {
                long ahora = System.currentTimeMillis();
                if (ahora - ultimoPasoTimestamp > 380) {
                    ultimoPasoTimestamp = ahora;
                    avanzarPasoComedor();
                }
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    @Override
    public void onRumbo(float grados) {
        if (esRutaComedor) {
            if (indiceComedorActual == 1) {
                float difSur = FotosRuta.diferencia(180f, grados);
                if (difSur > 35f) {
                    tvEstadoFoto.setText("⚠️ ¡Gira hacia el Sur para iniciar la ruta!");
                } else {
                    tvEstadoFoto.setText("🧭 ¡Orientado al Sur! Avanza al frente.");
                }
            } else if (esperandoGiro) {
                float difGiro = FotosRuta.diferencia(rumboRequeridoNodo, grados);
                if (difGiro <= 30f) {
                    Toast.makeText(this, "¡Giro completado! Avanzando...", Toast.LENGTH_SHORT).show();
                    avanzarSiguienteFotoComedor();
                } else {
                    float diffRelativa = (rumboRequeridoNodo - grados + 540f) % 360f - 180f;
                    String sentido = diffRelativa > 0 ? "hacia la DERECHA" : "hacia la IZQUIERDA";
                    tvEstadoFoto.setText("🔄 Gira " + sentido + " para continuar.");
                }
            }
            return;
        }

        if (!modoManual) mostrarRumbo(grados);
    }

    private void mostrarRumbo(float rumbo) {
        int redondeado = Math.round(rumbo) % 360;
        tvRumbo.setText(String.format(Locale.getDefault(), "Rumbo: %d° %s", redondeado, puntoCardinal(rumbo)));

        FotosRuta.Foto foto = FotosRuta.elegir(fotos, rumbo);
        int novo = (foto == null) ? 0 : foto.idRecurso;
        if (novo == recursoMostrado) return;
        recursoMostrado = novo;

        if (novo == 0) {
            tvEstadoFoto.setText("Sin foto en esta dirección");
            ivFotoRuta.animate().alpha(0f).setDuration(150).start();
        } else {
            tvEstadoFoto.setText(String.format(Locale.getDefault(), "Mostrando foto de %d°", foto.grados));
            ivFotoRuta.setImageResource(novo);
            ivFotoRuta.animate().alpha(1f).setDuration(150).start();
        }
    }

    private static String puntoCardinal(float grados) {
        String[] nombres = {"N", "NE", "E", "SE", "S", "SO", "O", "NO"};
        return nombres[Math.round(grados / 45f) % 8];
    }
}