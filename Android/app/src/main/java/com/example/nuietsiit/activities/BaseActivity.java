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

public class BaseActivity extends AppCompatActivity {

    private SensorManager mSensorManager;
    private Sensor mAccelerometer;
    private ShakeDetector mShakeDetector;
    private WaveView mWaveView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (mSensorManager != null) {
            mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        }

        mShakeDetector = new ShakeDetector();
        mShakeDetector.setOnShakeListener(() -> {
            if (!(BaseActivity.this instanceof MenuPrincipal)) {

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
                }
            }
        });
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        if (!(this instanceof MenuPrincipal) && !(this instanceof Login) && !(this instanceof SignUp)) {
            MaterialToolbar toolbar = findViewById(R.id.toolbar);
            if (toolbar == null) toolbar = findViewById(R.id.toolbarCalendario);
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

            ViewGroup rootView = findViewById(android.R.id.content);
            if (rootView != null) {
                FrameLayout mascotLayout = new FrameLayout(this);
                FrameLayout.LayoutParams mascotParams = new FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.WRAP_CONTENT,
                        FrameLayout.LayoutParams.WRAP_CONTENT
                );
                mascotParams.gravity = Gravity.BOTTOM | Gravity.END;
                mascotParams.setMargins(0, 0, 48, 48);
                mascotLayout.setLayoutParams(mascotParams);

                MaterialCardView mascotCard = new MaterialCardView(this);
                FrameLayout.LayoutParams cardParams = new FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.WRAP_CONTENT,
                        FrameLayout.LayoutParams.WRAP_CONTENT
                );
                mascotCard.setLayoutParams(cardParams);
                mascotCard.setRadius(50f);
                mascotCard.setCardBackgroundColor(Color.parseColor("#FFF3EB"));
                mascotCard.setCardElevation(8f);
                mascotCard.setStrokeWidth(2);
                mascotCard.setStrokeColor(Color.parseColor("#FFD8BE"));
                mascotCard.setClickable(true);
                mascotCard.setFocusable(true);

                LinearLayout layout = new LinearLayout(this);
                layout.setOrientation(LinearLayout.HORIZONTAL);
                layout.setGravity(Gravity.CENTER_VERTICAL);
                layout.setPadding(32, 20, 32, 20);

                ImageView iv = new ImageView(this);
                iv.setImageResource(R.drawable.ic_mascota_ayudante);
                LinearLayout.LayoutParams imgParams = new LinearLayout.LayoutParams(72, 72);
                iv.setLayoutParams(imgParams);

                TextView tv = new TextView(this);
                tv.setText(" Asistente AI");
                tv.setTextColor(Color.parseColor("#FF8C42"));
                tv.setTextSize(13f);
                tv.setTypeface(null, Typeface.BOLD);

                layout.addView(iv);
                layout.addView(tv);
                mascotCard.addView(layout);
                mascotLayout.addView(mascotCard);

                mascotCard.setOnClickListener(v -> {
                    v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
                    new AlertDialog.Builder(this)
                            .setIcon(R.drawable.ic_mascota_ayudante)
                            .setTitle("Asistente Virtual ETSIIT 🤖")
                            .setMessage("¡Hola! Soy tu asistente inteligente de la ETSIIT. Estoy aquí para ayudarte en esta sección.")
                            .setPositiveButton("¡Entendido!", null)
                            .show();
                });

                rootView.addView(mascotLayout);
            }
        }
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