package com.example.nuietsiit.activities.secciones.secretaria;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.example.nuietsiit.R;
import com.example.nuietsiit.activities.BaseActivity;
import com.example.nuietsiit.data.DBHelper;
import com.example.nuietsiit.data.SecretariaDB;
import com.example.nuietsiit.data.modelo.Paso;
import com.example.nuietsiit.data.modelo.Tramite;
import com.example.nuietsiit.util.DetectorInclinacion;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.materialswitch.MaterialSwitch;

import java.util.List;

public class PasosTramite extends BaseActivity {

    private static final String PREFS = "secretaria";
    private static final String PREF_INCLINAR = "inclinarPasos";
    private static final int NARANJA = 0xFFFF8C42;
    private static final int GRIS = 0xFFCCCCCC;

    private ViewPager2 vpPasos;
    private LinearLayout llPuntos;
    private Button btnAnterior, btnSiguiente;
    private MaterialSwitch swInclinar;
    private DetectorInclinacion detector;
    private int numPasos;
    private int paginaActual = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.pasos_tramite);

        MaterialToolbar barra = findViewById(R.id.toolbar);
        setSupportActionBar(barra);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        SecretariaDB db = new SecretariaDB(DBHelper.getInstance(this));
        Tramite tramite = db.getTramite(getIntent().getIntExtra("idProcedimiento", -1));
        List<Paso> pasos = tramite != null ? db.getPasos(tramite.id) : null;
        if (pasos == null || pasos.isEmpty()) {
            Toast.makeText(this, "Este trámite no tiene pasos", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        getSupportActionBar().setTitle(tramite.nombre);
        numPasos = pasos.size();

        String textoFinal = tramite.sePuedeSolicitar() ? "Solicitar ahora" : "Pedir cita";
        View.OnClickListener accionFinal = tramite.sePuedeSolicitar()
                ? v -> startActivity(new Intent(this, SolicitudTramite.class)
                        .putExtra("idProcedimiento", tramite.id)
                        .putExtra("CORREO", getIntent().getStringExtra("CORREO")))
                : v -> startActivity(new Intent(this, CitaPrevia.class)
                        .putExtra("idProcedimiento", tramite.id)
                        .putExtra("CORREO", getIntent().getStringExtra("CORREO")));
        vpPasos = findViewById(R.id.vpPasos);
        vpPasos.setAdapter(new PasoAdapter(pasos, textoFinal, accionFinal));

        llPuntos = findViewById(R.id.llPuntos);
        crearPuntos();

        btnAnterior = findViewById(R.id.btnAnterior);
        btnSiguiente = findViewById(R.id.btnSiguiente);
        btnAnterior.setOnClickListener(v -> irA(vpPasos.getCurrentItem() - 1));
        btnSiguiente.setOnClickListener(v -> irA(vpPasos.getCurrentItem() + 1));

        vpPasos.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                // la primera llamada es la página inicial: no vibra
                if (paginaActual != -1 && paginaActual != position) {
                    vpPasos.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
                }
                paginaActual = position;
                actualizarIndicadores(position);
            }
        });
        // tras restaurar la página ya es la correcta; se fija aquí por si no llega onPageSelected
        vpPasos.post(() -> {
            if (paginaActual == -1) paginaActual = vpPasos.getCurrentItem();
            actualizarIndicadores(vpPasos.getCurrentItem());
        });

        configurarInclinacion();
    }

    private void configurarInclinacion() {
        swInclinar = findViewById(R.id.swInclinar);
        detector = new DetectorInclinacion(this, direccion ->
                irA(vpPasos.getCurrentItem() + (direccion == DetectorInclinacion.DERECHA ? 1 : -1)));
        if (!detector.disponible()) {
            swInclinar.setVisibility(View.GONE);
            return;
        }
        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        // el estado sale de las preferencias (saveEnabled=false), así no salta el listener al rotar
        swInclinar.setSaveEnabled(false);
        swInclinar.setChecked(prefs.getBoolean(PREF_INCLINAR, false));
        swInclinar.setOnCheckedChangeListener((b, activado) -> {
            prefs.edit().putBoolean(PREF_INCLINAR, activado).apply();
            if (activado) {
                detector.iniciar();
                Toast.makeText(this, "Inclina a la derecha para avanzar y a la izquierda para volver",
                        Toast.LENGTH_LONG).show();
            } else {
                detector.parar();
            }
        });
    }

    private void irA(int pagina) {
        if (pagina >= 0 && pagina < numPasos) vpPasos.setCurrentItem(pagina, true);
    }

    private void crearPuntos() {
        int tam = (int) (10 * getResources().getDisplayMetrics().density);
        for (int i = 0; i < numPasos; i++) {
            View punto = new View(this);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(tam, tam);
            lp.setMargins(tam / 2, 0, tam / 2, 0);
            punto.setLayoutParams(lp);
            GradientDrawable circulo = new GradientDrawable();
            circulo.setShape(GradientDrawable.OVAL);
            punto.setBackground(circulo);
            llPuntos.addView(punto);
        }
    }

    private void actualizarIndicadores(int pagina) {
        for (int i = 0; i < numPasos; i++) {
            ((GradientDrawable) llPuntos.getChildAt(i).getBackground()).setColor(i == pagina ? NARANJA : GRIS);
        }
        btnAnterior.setEnabled(pagina > 0);
        btnSiguiente.setEnabled(pagina < numPasos - 1);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (swInclinar != null && swInclinar.isChecked()) detector.iniciar();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (detector != null) detector.parar();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
