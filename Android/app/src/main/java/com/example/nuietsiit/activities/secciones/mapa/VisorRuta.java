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

import androidx.appcompat.app.AlertDialog;

import com.example.nuietsiit.activities.BaseActivity;
import com.example.nuietsiit.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Visor de ruta interactivo que guía al usuario mediante pasos y giros.
 * Soporta navegación guiada por ascensor con diálogo y navegación precisa.
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
    private boolean esRutaGuiada = false;
    private boolean esRutaComedor = false;
    private boolean esRutaAula = false;
    private boolean usaAscensor = false;
    private boolean preguntaAscensorMostrada = false;
    private boolean esperandoLlegadaPlanta = false;

    private int indiceRutaActual = 1;
    private int pasosDados = 0;
    private int pasosRequeridosNodo = 3;
    private float rumboRequeridoNodo = 0f;
    private boolean esperandoGiro = false;
    private final List<Integer> recursosRuta = new ArrayList<>();

    // --- Configuración Ruta 1: Comedor (Vía Normal) ---
    private final int[] pasosPorFotoComedor = {
            3, 0, 6, 7, 0, 5, 0, 4, 0, 5,
            0, 7, 0, 6, 6, 8, 4, 0, 6, 8,
            0, 3, 7, 8, 0, 4, 6, 0, 5, 5,
            0, 6
    };
    private final float[] rumbosGiroComedor = {
            0f,   90f, 0f, 0f,   180f,  0f,   135f, 0f,   180f,  0f,
            135f, 0f,   180f, 0f,  0f,   0f,   0f,   270f,  0f,   0f,
            180f, 0f,   0f,  0f,  270f,  0f,   0f,   0f,  0f,   0f,
            90f,  0f
    };

    private final int[] pasosPorFotoComedorAscensor = pasosPorFotoComedor;
    private final float[] rumbosGiroComedorAscensor = rumbosGiroComedor;

    // --- Configuración Ruta 2: Aula 0.5 -> Aula 1.4 (Vía Normal) ---
    private final int[] pasosPorFotoAula = {
            3, 0, 7, 9, 0, 6, 6, 6, 3, 0,
            3, 0, 6, 6, 6, 6, 6, 6, 2, 0
    };
    private final float[] rumbosGiroAula = {
            0f, 90f, 0f, 0f, 45f, 0f, 0f, 0f, 0f, 225f,
            0f, 270f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f,
            0f, 0f, 0f, 0f, 0f
    };

    // --- Configuración Ruta 2: Aula 0.5 -> Aula 1.4 (Vía Ascensor) ---
    private final int[] pasosPorFotoAulaAscensor = {
            0, 3, 3, 0, 0, 3, 0, 6, 6, 6, 6, 6, 2, 0
    };
    private final float[] rumbosGiroAulaAscensor = {
            180f, 0f, 0f, 0f, 0f, 0f, 270f, 0f, 0f,
            0f, 0f, 0f, 0f, 0f
    };

    private static final int FOTO_OPCION_ASCENSOR_AULA = 5;

    private SensorManager sensorManager;
    private Sensor acelerometro;
    private long ultimoPasoTimestamp = 0;

    private String destinoNombre = "Destino ETSIIT";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.visor_ruta);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        inicializarVistas();

        String origenId = getIntent().getStringExtra("ORIGEN_ID");
        String extraDestino = getIntent().getStringExtra("DESTINO_NOMBRE");
        if (extraDestino != null && !extraDestino.isEmpty()) {
            destinoNombre = extraDestino;
        }

        esRutaComedor = (origenId != null && origenId.equalsIgnoreCase("comedor"));
        esRutaAula = (origenId != null && origenId.equalsIgnoreCase("aula_0_5"));
        esRutaGuiada = esRutaComedor || esRutaAula;

        if (esRutaGuiada) {
            configurarRutaGuiada(origenId);
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

    private void configurarRutaGuiada(String tipo) {
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

        String prefijo = esRutaComedor ? "ruta_comedor_" : "ruta_aula_";
        int maxFotos = esRutaComedor ? 33 : 30;

        for (int i = 1; i <= maxFotos; i++) {
            int id = getResources().getIdentifier(prefijo + i, "drawable", getPackageName());
            if (id != 0) recursosRuta.add(id);
        }

        if (recursosRuta.isEmpty()) {
            int fallbackId = getResources().getIdentifier("ruta_demo_0", "drawable", getPackageName());
            if (fallbackId != 0) recursosRuta.add(fallbackId);
        }

        if (!recursosRuta.isEmpty()) {
            ivFotoRuta.setImageResource(recursosRuta.get(0));
            ivFotoRuta.animate().alpha(1f).setDuration(150).start();
        }

        configurarNodoRutaActual();

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
            tvEstadoFoto.setText(getString(R.string.mapa_sin_sensor_orientacion));
            activarModoManual(true);
        } else if (!esRutaGuiada) {
            tvEstadoFoto.setText(getString(R.string.mapa_esperando_sensor_orientacion));
        }
    }

    private void avanzarPasoRuta() {
        if (!esRutaGuiada) return;
        if (esperandoLlegadaPlanta) {
            Toast.makeText(this, getString(R.string.mapa_confirma_llegada_p1), Toast.LENGTH_SHORT).show();
            return;
        }

        pasosDados++;
        if (pasosDados >= pasosRequeridosNodo) {
            avanzarSiguienteFotoRuta();
        } else {
            actualizarTextoRuta();
        }
    }

    private void avanzarSiguienteFotoRuta() {
        pasosDados = 0;
        esperandoGiro = false;
        indiceRutaActual++;

        if (indiceRutaActual > recursosRuta.size()) {
            tvEstadoFoto.setText(getString(R.string.mapa_has_llegado_al_destino, destinoNombre));
        } else {
            ivFotoRuta.setImageResource(recursosRuta.get(indiceRutaActual - 1));
            configurarNodoRutaActual();
            comprobarEventosAscensor();
        }
    }

    private void comprobarEventosAscensor() {
        if (esRutaAula && usaAscensor) {
            if (indiceRutaActual == 4) {
                tvEstadoFoto.setText(getString(R.string.mapa_subiendo_ascensor));
                mostrarDialogoBotonAscensor();
                return;
            }
            if (indiceRutaActual == 5) {
                esperandoLlegadaPlanta = true;
                tvEstadoFoto.setText(getString(R.string.mapa_esperando_confirmacion_llegada));
                mostrarDialogoLlegadaPlanta();
                return;
            }
        }

        if (preguntaAscensorMostrada) return;
        int fotoClave = esRutaComedor ? -1 : FOTO_OPCION_ASCENSOR_AULA;

        if (indiceRutaActual == fotoClave) {
            preguntaAscensorMostrada = true;
            tvEstadoFoto.setText(getString(R.string.mapa_deseas_ascensor_titulo));
            mostrarDialogoAscensor();
        }
    }

    private void mostrarDialogoBotonAscensor() {
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.mapa_uso_ascensor_titulo))
                .setMessage(getString(R.string.mapa_uso_ascensor_msg))
                .setCancelable(false)
                .setPositiveButton(getString(R.string.aceptar), (dialog, which) -> {
                    avanzarSiguienteFotoRuta();
                })
                .show();
    }

    private void mostrarDialogoLlegadaPlanta() {
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.mapa_en_el_ascensor_titulo))
                .setMessage(getString(R.string.mapa_en_el_ascensor_msg))
                .setCancelable(false)
                .setPositiveButton(getString(R.string.mapa_si_he_llegado), (dialog, which) -> {
                    esperandoLlegadaPlanta = false;
                    avanzarSiguienteFotoRuta();
                })
                .show();
    }

    private void mostrarDialogoAscensor() {
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.mapa_deseas_ascensor_titulo))
                .setMessage(getString(R.string.mapa_deseas_ascensor_msg))
                .setCancelable(false)
                .setPositiveButton(getString(R.string.mapa_si), (dialog, which) -> {
                    usaAscensor = true;
                    Toast.makeText(this, getString(R.string.mapa_ruta_ascensor_activada), Toast.LENGTH_SHORT).show();
                    cargarRutaAscensorImagenes();
                })
                .setNegativeButton(getString(R.string.mapa_no), (dialog, which) -> {
                    usaAscensor = false;
                    Toast.makeText(this, getString(R.string.mapa_continuando_escaleras), Toast.LENGTH_SHORT).show();
                    configurarNodoRutaActual();
                })
                .show();
    }

    private void cargarRutaAscensorImagenes() {
        recursosRuta.clear();
        String prefijo = "ruta_aula_ascensor_";
        for (int i = 1; i <= 30; i++) {
            int id = getResources().getIdentifier(prefijo + i, "drawable", getPackageName());
            if (id != 0) recursosRuta.add(id);
        }
        if (recursosRuta.isEmpty()) {
            int fallbackId = getResources().getIdentifier("ruta_demo_0", "drawable", getPackageName());
            if (fallbackId != 0) recursosRuta.add(fallbackId);
        }
        indiceRutaActual = 1;
        pasosDados = 0;
        esperandoGiro = false;
        esperandoLlegadaPlanta = false;

        if (!recursosRuta.isEmpty()) {
            ivFotoRuta.setImageResource(recursosRuta.get(0));
            ivFotoRuta.animate().alpha(1f).setDuration(150).start();
        }
        configurarNodoRutaActual();
    }

    /**
     * FORZAR FOTO ANTERIOR:
     * Resetea completamente el estado del nodo y revalúa las condiciones
     * para permitir retroceder limpiamente incluso si estabas en un giro.
     */
    private void forzarFotoAnterior() {
        if (!esRutaGuiada) return;

        if (indiceRutaActual > 1) {
            indiceRutaActual--; // Retrocede una foto

            // Resetear todos los bloqueos e indicadores
            pasosDados = 0;
            esperandoGiro = false;
            esperandoLlegadaPlanta = false;

            // Actualizar la imagen mostrada en pantalla
            if ((indiceRutaActual - 1) < recursosRuta.size()) {
                ivFotoRuta.setImageResource(recursosRuta.get(indiceRutaActual - 1));
            }

            // Reconfigurar los requisitos (pasos o giro) del nodo anterior al que acabamos de volver
            configurarNodoRutaActual();
        }
    }

    private void configurarNodoRutaActual() {
        int index = indiceRutaActual - 1;

        int[] pasosArray;
        float[] rumbosArray;

        if (esRutaComedor) {
            pasosArray = usaAscensor ? pasosPorFotoComedorAscensor : pasosPorFotoComedor;
            rumbosArray = usaAscensor ? rumbosGiroComedorAscensor : rumbosGiroComedor;
        } else {
            pasosArray = usaAscensor ? pasosPorFotoAulaAscensor : pasosPorFotoAula;
            rumbosArray = usaAscensor ? rumbosGiroAulaAscensor : rumbosGiroAula;
        }

        if (index >= 0 && index < pasosArray.length) {
            pasosRequeridosNodo = pasosArray[index];
            // Si el número de pasos requeridos en este nodo es 0, requiere un giro
            esperandoGiro = (pasosRequeridosNodo == 0);
            if (esperandoGiro) {
                rumboRequeridoNodo = rumbosArray[index];
            }
        } else {
            pasosRequeridosNodo = 3;
            esperandoGiro = false;
        }

        actualizarTextoRuta();
    }

    private void forzarFotoSiguiente() {
        if (!esRutaGuiada) return;
        if (indiceRutaActual < recursosRuta.size()) {
            avanzarSiguienteFotoRuta();
        } else {
            tvEstadoFoto.setText(getString(R.string.mapa_has_llegado_al_destino, destinoNombre));
        }
    }

    private void actualizarTextoRuta() {
        if (!esRutaGuiada) return;
        if (indiceRutaActual > recursosRuta.size()) {
            tvEstadoFoto.setText(getString(R.string.mapa_destino_alcanzado, destinoNombre));
            return;
        }
        if (esperandoLlegadaPlanta) {
            tvEstadoFoto.setText(getString(R.string.mapa_subiendo_ascensor));
        } else if (esperandoGiro) {
            tvEstadoFoto.setText(getString(R.string.mapa_gira_direccion_indicada));
        } else if (pasosRequeridosNodo == 0) {
            tvEstadoFoto.setText(getString(R.string.mapa_gira_para_continuar));
        } else {
            int restantes = pasosRequeridosNodo - pasosDados;
            tvEstadoFoto.setText(getString(R.string.mapa_avanza_pasos_al_frente, restantes));
        }
    }

    private void activarModoManual(boolean activo) {
        modoManual = activo;
        sbRumboManual.setVisibility(activo ? View.VISIBLE : View.GONE);
        if (activo && !esRutaGuiada) mostrarRumbo(sbRumboManual.getProgress());
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
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER && esRutaGuiada && !esperandoGiro && !esperandoLlegadaPlanta) {
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
                    avanzarPasoRuta();
                }
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    @Override
    public void onRumbo(float grados) {
        if (esRutaGuiada) {
            if (indiceRutaActual == 1) {
                float difSur = FotosRuta.diferencia(180f, grados);
                if (difSur > 35f) {
                    tvEstadoFoto.setText(getString(R.string.mapa_gira_sur_para_iniciar));
                } else {
                    tvEstadoFoto.setText(getString(R.string.mapa_orientado_al_sur));
                    avanzarSiguienteFotoRuta();
                }
            } else if (esperandoGiro && !esperandoLlegadaPlanta) {
                float difGiro = FotosRuta.diferencia(rumboRequeridoNodo, grados);
                if (difGiro <= 30f) {
                    avanzarSiguienteFotoRuta();
                } else {
                    float diffRelativa = (rumboRequeridoNodo - grados + 540f) % 360f - 180f;
                    String sentido = diffRelativa > 0 ? getString(R.string.mapa_hacia_derecha) : getString(R.string.mapa_hacia_izquierda);
                    tvEstadoFoto.setText(getString(R.string.mapa_gira_sentido_para_continuar, sentido));
                }
            }
            return;
        }

        if (!modoManual) mostrarRumbo(grados);
    }

    private void mostrarRumbo(float rumbo) {
        int redondeado = Math.round(rumbo) % 360;
        tvRumbo.setText(getString(R.string.mapa_rumbo_formato, redondeado, puntoCardinal(rumbo)));

        FotosRuta.Foto foto = FotosRuta.elegir(fotos, rumbo);
        int novo = (foto == null) ? 0 : foto.idRecurso;
        if (novo == recursoMostrado) return;
        recursoMostrado = novo;

        if (novo == 0) {
            tvEstadoFoto.setText(getString(R.string.mapa_sin_foto_direccion));
            ivFotoRuta.animate().alpha(0f).setDuration(150).start();
        } else {
            tvEstadoFoto.setText(getString(R.string.mapa_mostrando_foto, foto.grados));
            ivFotoRuta.setImageResource(novo);
            ivFotoRuta.animate().alpha(1f).setDuration(150).start();
        }
    }

    private static String puntoCardinal(float grados) {
        String[] nombres = {"N", "NE", "E", "SE", "S", "SO", "O", "NO"};
        return nombres[Math.round(grados / 45f) % 8];
    }
}