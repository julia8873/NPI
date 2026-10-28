package com.example.nuietsiit.activities;

import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.example.nuietsiit.activities.main.MenuPrincipal;
import com.example.nuietsiit.activities.main.ShakeDetector;
import com.example.nuietsiit.activities.main.WaveView;

public class BaseActivity extends AppCompatActivity {

    private SensorManager mSensorManager;
    private Sensor mAccelerometer;
    private ShakeDetector mShakeDetector;
    private WaveView mWaveView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Inicializar el sensor de acelerómetro
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (mSensorManager != null) {
            mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        }

        mShakeDetector = new ShakeDetector();
        mShakeDetector.setOnShakeListener(() -> {
            if (!(BaseActivity.this instanceof MenuPrincipal)) {

                // 1. Obtener el contenedor raíz de la actividad
                android.view.ViewGroup rootView = findViewById(android.R.id.content);

                // 2. Si no se ha creado aún la vista de la ola, la instanciamos
                if (mWaveView == null && rootView != null) {
                    mWaveView = new WaveView(BaseActivity.this, null);
                    mWaveView.setId(View.generateViewId()); // Asigna un ID único nativo

                    android.widget.FrameLayout.LayoutParams params = new android.widget.FrameLayout.LayoutParams(
                            android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                            android.widget.FrameLayout.LayoutParams.MATCH_PARENT
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
                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mSensorManager != null && mAccelerometer != null) {
            mSensorManager.registerListener(mShakeDetector, mAccelerometer, SensorManager.SENSOR_DELAY_UI);
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