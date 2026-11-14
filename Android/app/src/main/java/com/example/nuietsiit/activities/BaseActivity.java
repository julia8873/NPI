package com.example.nuietsiit.activities;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.os.LocaleListCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.nuietsiit.R;
import com.example.nuietsiit.activities.auth.Login;
import com.example.nuietsiit.activities.auth.SignUp;
import com.example.nuietsiit.activities.main.AjustesCuenta;
import com.example.nuietsiit.activities.main.MenuPrincipal;
import com.example.nuietsiit.activities.main.ShakeDetector;
import com.example.nuietsiit.activities.main.WaveView;
import com.example.nuietsiit.activities.secciones.chat.ChatActivity;
import com.example.nuietsiit.data.DBHelper;
import com.example.nuietsiit.data.UsuariosDB;
import com.example.nuietsiit.util.DetectorBocaAbajo;
import com.example.nuietsiit.util.tutorial.Gesto;
import com.example.nuietsiit.util.tutorial.Tutorial;
import com.google.android.material.appbar.MaterialToolbar;

public class BaseActivity extends AppCompatActivity implements Tutorial.ConTutorial {

    private SensorManager mSensorManager;
    private Sensor mAccelerometer;
    private ShakeDetector mShakeDetector;
    private WaveView mWaveView;
    private DetectorBocaAbajo mDetectorBocaAbajo;
    private Tutorial mTutorial;        // el de la pantalla (crearTutorial)
    private Tutorial mTutorialParte;   // el de una parte (mostrarTutorialSiPrimeraVez)

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);

        // 1. Determinar cuál es el idioma que debe aplicarse (prioridad: BD > SharedPreferences > "es")
        String idiomaAplica = "es";

        SharedPreferences prefsSesion = getSharedPreferences("SesionUsuario", MODE_PRIVATE);
        String correo = prefsSesion.getString("CORREO", null);

        if (correo != null && !correo.isEmpty()) {
            DBHelper db = DBHelper.getInstance(this);
            UsuariosDB usuariosDB = new UsuariosDB(db);
            String idiomaDb = usuariosDB.obtenerCodigoIdiomaUsuario(correo);
            if (idiomaDb != null && !idiomaDb.isEmpty()) {
                idiomaAplica = idiomaDb;
            }
        } else {
            // Si no hay sesión, leemos el idioma guardado globalmente en las preferencias
            SharedPreferences prefsConfig = getSharedPreferences("ConfiguracionApp", MODE_PRIVATE);
            idiomaAplica = prefsConfig.getString("CODIGO_IDIOMA", "es");
        }

        // 2. Comprobar si el idioma activo en la app YA ES el que corresponde
        LocaleListCompat localesActuales = AppCompatDelegate.getApplicationLocales();

        boolean necesitaCambiar = localesActuales.isEmpty()
                || localesActuales.get(0) == null
                || !idiomaAplica.equals(localesActuales.get(0).getLanguage());

        // 3. Aplicar solo si realmente difiere (evita bucles infinitos de recreación de pantalla)
        if (necesitaCambiar) {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(idiomaAplica));
        }

        super.onCreate(savedInstanceState);

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (mSensorManager != null) {
            mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        }

        mDetectorBocaAbajo = new DetectorBocaAbajo(this);

        mShakeDetector = new ShakeDetector();
        mShakeDetector.setOnShakeListener(() -> {
            if (hayTutorialActivo()) { // en el tutorial agitar es un paso, no saca de la pantalla
                avisarTutorial(Gesto.AGITAR);
                return;
            }
            if (!usaAgitar()) return; // puede cambiar sin pasar por onResume (p. ej. guía del mapa)
            if (!(BaseActivity.this instanceof MenuPrincipal)) {
                if (isFinishing() || isDestroyed()) return;
                alAgitar();
            } else {
                onShakeMenuPrincipal();
            }
        });
    }

    /**
     * Agitar fuera del menú. Por defecto vuelve al menú; una pantalla con un procedimiento a medias
     * puede pedir antes confirmación y llamar luego a volverAlMenu().
     */
    protected void alAgitar() {
        volverAlMenu();
    }

    /** Vuelve al menú principal con la animación de la ola. */
    protected void volverAlMenu() {
        if (isFinishing() || isDestroyed()) return;
        if (mSensorManager != null) {
            mSensorManager.unregisterListener(mShakeDetector);
        }

        // 1. Obtener el contenedor raíz de la actividad
        ViewGroup rootView = findViewById(android.R.id.content);

        // 2. Si no se ha creado aún la vista de la ola, la instanciamos
        if (mWaveView == null && rootView != null) {
            mWaveView = new WaveView(BaseActivity.this, null);
            mWaveView.setId(View.generateViewId());

            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
            );
            mWaveView.setLayoutParams(params);
            mWaveView.setElevation(100f);

            rootView.addView(mWaveView);
        }

        // 3. Ejecutar la animación
        if (mWaveView != null) {
            mWaveView.setVisibility(View.VISIBLE);
            mWaveView.bringToFront();

            // Llama a la animación SOLO de subida
            mWaveView.startWaveUp(() -> {
                Intent intent = new Intent(BaseActivity.this, MenuPrincipal.class);
                intent.putExtra("START_WAVE_DOWN", true); // Aviso para el MenuPrincipal
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                overridePendingTransition(0, 0); // Evita parpadeos nativos
                finish();
            });
        } else {
            Intent intent = new Intent(BaseActivity.this, MenuPrincipal.class);
            intent.putExtra("START_WAVE_DOWN", true);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            overridePendingTransition(0, 0);
            finish();
        }
    }

    protected void configurarCabeceraUnificada(String titulo) {
        View btnVolver = findViewById(R.id.btnVolver);
        if (btnVolver != null) {
            btnVolver.setOnClickListener(v -> {
                v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
                finish();
            });
        }

        TextView tvTitulo = findViewById(R.id.tvTituloSeccion);
        if (tvTitulo != null && titulo != null) {
            tvTitulo.setText(titulo);
        }

        View cardMascotaAyudante = findViewById(R.id.cardMascotaAyudante);
        if (cardMascotaAyudante != null) {
            cardMascotaAyudante.setOnClickListener(v -> {
                v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
                Intent intent = new Intent(this, ChatActivity.class);
                startActivity(intent);
            });
        }

        ImageView ivIcono = findViewById(R.id.ivIcono);
        if (ivIcono != null) {
            ivIcono.setOnClickListener(v -> {
                v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
                String correo = getSharedPreferences("SesionUsuario", MODE_PRIVATE).getString("CORREO", null);
                if (correo == null || correo.isEmpty()) {
                    startActivity(new Intent(this, Login.class));
                } else {
                    Intent intent = new Intent(this, AjustesCuenta.class);
                    intent.putExtra("CORREO", correo);
                    startActivity(intent);
                }
            });
        }
    }

    /**
     * Gancho para que MenuPrincipal entre a la sección seleccionada al agitar el móvil.
     */
    protected void onShakeMenuPrincipal() {
    }

    /**
     * Las pantallas donde mover el móvil es parte del uso (p. ej. barrer una balda con la cámara)
     * pueden desactivar aquí el agitar.
     */
    protected boolean usaAgitar() {
        return true;
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        aplicarMargenesSistema();
        configurarCabeceraUnificada(null);

        if (!(this instanceof MenuPrincipal) && !(this instanceof Login) && !(this instanceof SignUp)) {
            MaterialToolbar toolbar = findViewById(R.id.toolbar);
            if (toolbar != null && toolbar.findViewById(R.id.ivIcono) == null) {
                ImageView ivIcono = new ImageView(this);
                ivIcono.setId(R.id.ivIcono);
                Toolbar.LayoutParams params = new Toolbar.LayoutParams(
                        100, 100, Gravity.END | Gravity.CENTER_VERTICAL
                );
                params.setMarginEnd(32);
                ivIcono.setLayoutParams(params);
                ivIcono.setImageResource(R.drawable.ic_perfil_etsiit);
                ivIcono.setScaleType(ImageView.ScaleType.CENTER_CROP);
                ivIcono.setClickable(true);
                ivIcono.setFocusable(true);
                ivIcono.setOnClickListener(v -> {
                    v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
                    String correo = getSharedPreferences("SesionUsuario", MODE_PRIVATE).getString("CORREO", null);
                    if (correo == null || correo.isEmpty()) {
                        startActivity(new Intent(this, Login.class));
                    } else {
                        Intent intent = new Intent(this, AjustesCuenta.class);
                        intent.putExtra("CORREO", correo);
                        startActivity(intent);
                    }
                });
                toolbar.addView(ivIcono);
            }
        }

        mTutorial = crearTutorial();
        if (mTutorial != null) {
            ponerBotonTutorial();
            // tras el primer layout, para que el foco caiga sobre vistas ya colocadas
            getWindow().getDecorView().post(() -> {
                if (!isFinishing()) mTutorial.mostrarSiPrimeraVez();
            });
        }
    }

    // ---------- Tutorial interactivo (util/tutorial, ver PLAN_TUTORIAL) ----------

    /** Pasos del tutorial de esta pantalla; null = sin tutorial ni botón "?". */
    protected Tutorial crearTutorial() {
        return null;
    }

    /** Llamar donde la pantalla ya reconoce un gesto. No hace nada si no hay tutorial activo. */
    @Override
    public void avisarTutorial(Gesto gesto) {
        if (mTutorial != null) mTutorial.avisar(gesto);
        if (mTutorialParte != null) mTutorialParte.avisar(gesto);
    }

    /** Tutorial de una parte de la pantalla (p. ej. al llegar a la firma). Devuelve si se mostró. */
    protected boolean mostrarTutorialSiPrimeraVez(Tutorial tutorial) {
        if (tutorial == null || hayTutorialActivo()) return false;
        mTutorialParte = tutorial;
        return tutorial.mostrarSiPrimeraVez();
    }

    /** Como el anterior, pero aunque ya se haya visto (p. ej. para acabar una práctica que se empezó en otra parte). */
    protected boolean mostrarTutorialParte(Tutorial tutorial) {
        if (tutorial == null || hayTutorialActivo()) return false;
        mTutorialParte = tutorial;
        tutorial.mostrar();
        return true;
    }

    /** El que repite el botón "?": por defecto el de la pantalla; si tiene partes, el de la parte en la que se está. */
    protected Tutorial tutorialParaAyuda() {
        return mTutorial;
    }

    protected boolean hayTutorialActivo() {
        return (mTutorial != null && mTutorial.activo()) || (mTutorialParte != null && mTutorialParte.activo());
    }

    /** Botón "?" para repetir el tutorial: junto al chat si la cabecera lo tiene; si no, en la toolbar. */
    private void ponerBotonTutorial() {
        int lado = Math.round(40 * getResources().getDisplayMetrics().density);
        ImageView boton = new ImageView(this);
        boton.setImageResource(R.drawable.ic_help);
        boton.setColorFilter(getColor(R.color.nui_tinta));
        boton.setBackgroundResource(R.drawable.bg_circulo_blanco_volver);
        boton.setPadding(lado / 5, lado / 5, lado / 5, lado / 5);
        boton.setContentDescription(getString(R.string.base_desc_repetir_tutorial));
        boton.setOnClickListener(v -> {
            v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
            Tutorial t = tutorialParaAyuda();
            if (t == null || hayTutorialActivo()) return;
            if (t != mTutorial) mTutorialParte = t; // para que le lleguen los avisos
            t.mostrar();
        });

        View chat = findViewById(R.id.cardMascotaAyudante);
        if (chat != null && chat.getParent() instanceof android.widget.LinearLayout) {
            ViewGroup cabecera = (ViewGroup) chat.getParent();
            android.widget.LinearLayout.LayoutParams lp = new android.widget.LinearLayout.LayoutParams(lado, lado);
            lp.setMarginEnd(lado / 4);
            lp.gravity = Gravity.CENTER_VERTICAL;
            cabecera.addView(boton, cabecera.indexOfChild(chat), lp);
            return;
        }
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            Toolbar.LayoutParams lp = new Toolbar.LayoutParams(lado, lado, Gravity.END | Gravity.CENTER_VERTICAL);
            lp.setMarginEnd(lado / 4);
            toolbar.addView(boton, lp); // tras el icono de perfil: queda a su izquierda
        }
    }

    @Override
    protected void onDestroy() {
        if (mTutorial != null) mTutorial.cerrar();
        if (mTutorialParte != null) mTutorialParte.cerrar();
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // siempre se registra: usaAgitar() se mira al agitar porque puede cambiar sin pasar por aquí
        if (mSensorManager != null && mAccelerometer != null) {
            mSensorManager.registerListener(mShakeDetector, mAccelerometer, SensorManager.SENSOR_DELAY_GAME);
        }
        if (mDetectorBocaAbajo != null) {
            mDetectorBocaAbajo.iniciar();
        }
    }

    @Override
    protected void onPause() {
        if (mSensorManager != null) {
            mSensorManager.unregisterListener(mShakeDetector);
        }
        if (mDetectorBocaAbajo != null) {
            mDetectorBocaAbajo.parar();
        }
        super.onPause();
    }

    private void aplicarMargenesSistema() {
        if (this instanceof MenuPrincipal) return;
        View root = findViewById(R.id.rootAsignaturas);
        if (root != null) {
            ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
                Insets statusBarInsets = insets.getInsets(WindowInsetsCompat.Type.statusBars());
                Insets navBarInsets = insets.getInsets(WindowInsetsCompat.Type.navigationBars());
                v.setPadding(
                        v.getPaddingLeft(),
                        statusBarInsets.top,
                        v.getPaddingRight(),
                        Math.max(v.getPaddingBottom(), navBarInsets.bottom)
                );
                return insets;
            });
        }
    }
}