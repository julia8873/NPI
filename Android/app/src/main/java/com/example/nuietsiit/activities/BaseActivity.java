package com.example.nuietsiit.activities;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.nuietsiit.R;
import com.example.nuietsiit.activities.auth.Login;
import com.example.nuietsiit.activities.auth.SignUp;
import com.example.nuietsiit.activities.main.AjustesCuenta;
import com.example.nuietsiit.activities.main.MenuPrincipal;
import com.example.nuietsiit.activities.main.ShakeDetector;
import com.example.nuietsiit.activities.main.WaveView;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.card.MaterialCardView;

import androidx.appcompat.app.AppCompatDelegate;

public class BaseActivity extends AppCompatActivity {

    private SensorManager mSensorManager;
    private Sensor mAccelerometer;
    private ShakeDetector mShakeDetector;
    private WaveView mWaveView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        super.onCreate(savedInstanceState);

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (mSensorManager != null) {
            mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        }

        mShakeDetector = new ShakeDetector();
        mShakeDetector.setOnShakeListener(() -> {
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
    }

    @Override
    protected void onResume() {
        super.onResume();
        // siempre se registra: usaAgitar() se mira al agitar porque puede cambiar sin pasar por aquí
        if (mSensorManager != null && mAccelerometer != null) {
            mSensorManager.registerListener(mShakeDetector, mAccelerometer, SensorManager.SENSOR_DELAY_GAME);
        }
    }

    @Override
    protected void onPause() {
        if (mSensorManager != null) {
            mSensorManager.unregisterListener(mShakeDetector);
        }
        super.onPause();
    }
}