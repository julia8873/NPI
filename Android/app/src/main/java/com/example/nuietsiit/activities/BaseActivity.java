package com.example.nuietsiit.activities;

import android.content.Context;
import android.content.Intent;
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

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.appcompat.app.AppCompatDelegate;

import com.example.nuietsiit.R;
import com.example.nuietsiit.activities.auth.Login;
import com.example.nuietsiit.activities.auth.SignUp;
import com.example.nuietsiit.activities.main.AjustesCuenta;
import com.example.nuietsiit.activities.main.MenuPrincipal;
import com.example.nuietsiit.activities.main.ShakeDetector;
import com.example.nuietsiit.activities.main.WaveView;
import com.example.nuietsiit.activities.secciones.chat.ChatActivity;
import com.example.nuietsiit.util.DetectorBocaAbajo;
import com.google.android.material.appbar.MaterialToolbar;

public class BaseActivity extends AppCompatActivity {

    private SensorManager mSensorManager;
    private Sensor mAccelerometer;
    private ShakeDetector mShakeDetector;
    private WaveView mWaveView;
    private DetectorBocaAbajo mDetectorBocaAbajo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        super.onCreate(savedInstanceState);

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (mSensorManager != null) {
            mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        }

        mDetectorBocaAbajo = new DetectorBocaAbajo(this);

        mShakeDetector = new ShakeDetector();
        mShakeDetector.setOnShakeListener(() -> {
            if (!usaAgitar()) return; // puede cambiar sin pasar por onResume (p. ej. guía del mapa)
            if (!(BaseActivity.this instanceof MenuPrincipal)) {
                if (isFinishing() || isDestroyed()) return;

                if (mSensorManager != null) {
                    mSensorManager.unregisterListener(mShakeDetector);
                }

                ViewGroup rootView = findViewById(android.R.id.content);

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

                if (mWaveView != null) {
                    mWaveView.setVisibility(View.VISIBLE);
                    mWaveView.bringToFront();

                    mWaveView.startWaveUp(() -> {
                        Intent intent = new Intent(BaseActivity.this, MenuPrincipal.class);
                        intent.putExtra("START_WAVE_DOWN", true);
                        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                        startActivity(intent);
                        overridePendingTransition(0, 0);
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
            } else {
                onShakeMenuPrincipal();
            }
        });
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
}
