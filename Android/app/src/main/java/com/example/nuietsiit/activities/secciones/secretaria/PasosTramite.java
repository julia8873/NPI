package com.example.nuietsiit.activities.secciones.secretaria;

import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.viewpager2.widget.ViewPager2;

import com.example.nuietsiit.R;
import com.example.nuietsiit.data.DBHelper;
import com.example.nuietsiit.data.SecretariaDB;
import com.example.nuietsiit.data.modelo.Paso;
import com.example.nuietsiit.data.modelo.Tramite;
import com.example.nuietsiit.ui.CabeceraNui;
import com.example.nuietsiit.ui.InclinarParaPasar;
import com.example.nuietsiit.util.DetectorInclinacion;
import com.example.nuietsiit.util.tutorial.Gesto;
import com.example.nuietsiit.util.tutorial.Tutorial;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.materialswitch.MaterialSwitch;

import java.util.List;

public class PasosTramite extends PantallaSecretaria {

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
        CabeceraNui.preparar(barra);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        SecretariaDB db = new SecretariaDB(DBHelper.getInstance(this));
        Tramite tramite = db.getTramite(getIntent().getIntExtra("idProcedimiento", -1));
        List<Paso> pasos = tramite != null ? db.getPasos(tramite.id) : null;
        if (pasos == null || pasos.isEmpty()) {
            Toast.makeText(this, R.string.pasos_sin_pasos, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(R.string.pasos_titulo);
            getSupportActionBar().setSubtitle(tramite.nombre);
        }
        numPasos = pasos.size();

        String textoFinal = getString(tramite.sePuedeSolicitar()
                ? R.string.pasos_btn_solicitar_ahora
                : R.string.pasos_btn_pedir_cita);
        View.OnClickListener accionFinal = tramite.sePuedeSolicitar()
                ? v -> startActivity(new Intent(this, SolicitudTramite.class)
                .putExtra("idProcedimiento", tramite.id)
                .putExtra("CORREO", getIntent().getStringExtra("CORREO")))
                : v -> startActivity(new Intent(this, CitaPrevia.class)
                .putExtra("idProcedimiento", tramite.id)
                .putExtra("CORREO", getIntent().getStringExtra("CORREO")));
        vpPasos = findViewById(R.id.vpPasos);
        vpPasos.setAdapter(new PasoAdapter(pasos, textoFinal, accionFinal));
        // se pasa una ficha del fichero: la que se va se inclina y baja un poco
        float baja = getResources().getDimension(R.dimen.nui_espacio_l);
        vpPasos.setPageTransformer((pagina, posicion) -> {
            float p = Math.max(-1f, Math.min(1f, posicion));
            pagina.setRotation(-4f * p);
            pagina.setTranslationY(Math.abs(p) * baja);
            pagina.setAlpha(1f - 0.3f * Math.abs(p));
        });

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
                    if (position > paginaActual) avisarTutorial(Gesto.DESLIZAR_IZQ);
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
        detector = new DetectorInclinacion(this, direccion -> {
            boolean activado = swInclinar.isChecked();
            if (hayTutorialActivo()) avisarTutorial(Gesto.INCLINAR);
            else if (!activado) { // solo estaba escuchando para el tutorial
                detector.parar();
                return;
            }
            irA(vpPasos.getCurrentItem() + (direccion == DetectorInclinacion.DERECHA ? 1 : -1));
        });
        if (!detector.disponible()) {
            swInclinar.setVisibility(View.GONE);
            return;
        }
        // el estado sale de las preferencias (saveEnabled=false), así no salta el listener al rotar
        swInclinar.setSaveEnabled(false);
        swInclinar.setChecked(InclinarParaPasar.activado(this)); // la misma preferencia que pasar hojas y fichas
        swInclinar.setOnCheckedChangeListener((b, activado) -> {
            InclinarParaPasar.setActivado(this, activado);
            if (activado) {
                detector.iniciar();
                Toast.makeText(this, R.string.pasos_toast_inclinar, Toast.LENGTH_LONG).show();
            } else {
                detector.parar();
            }
        });
    }

    @Override
    protected Tutorial crearTutorial() {
        if (numPasos == 0) return null; // se está cerrando: el trámite no tiene pasos
        Tutorial t = new Tutorial(this, "pasos_tramite", 2)
                .alEmpezar(() -> irA(0));
        if (numPasos > 1) {
            t.gesto(R.id.vpPasos, Gesto.DESLIZAR_IZQ, getString(R.string.pasos_tut_deslizar))
                    .tocar(R.id.btnSiguiente, getString(R.string.pasos_tut_flecha))
                    .soloSi(() -> btnSiguiente.isEnabled());
        }
        if (detector.disponible() && numPasos > 1) {
            t.alEmpezar(() -> { // en el tutorial se escucha aunque el interruptor esté apagado
                        irA(0);
                        detector.iniciar();
                    })
                    .alTerminar(() -> { if (!swInclinar.isChecked()) detector.parar(); })
                    .sensor(Gesto.INCLINAR, getString(R.string.pasos_tut_inclinar))
                    .info(R.id.swInclinar, getString(R.string.pasos_tut_interruptor));
        }
        return t;
    }

    private void irA(int pagina) {
        if (pagina >= 0 && pagina < numPasos) vpPasos.setCurrentItem(pagina, true);
    }

    private void crearPuntos() {
        // puntos de página
        int tam = getResources().getDimensionPixelSize(R.dimen.nui_espacio_s);
        for (int i = 0; i < numPasos; i++) {
            View punto = new View(this);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(tam, tam);
            lp.setMargins(tam / 2, tam / 2, tam / 2, tam / 2);
            punto.setLayoutParams(lp);
            GradientDrawable circulo = new GradientDrawable();
            circulo.setShape(GradientDrawable.OVAL);
            punto.setBackground(circulo);
            llPuntos.addView(punto);
        }
    }

    private void actualizarIndicadores(int pagina) {
        for (int i = 0; i < numPasos; i++) {
            View punto = llPuntos.getChildAt(i);
            ((GradientDrawable) punto.getBackground()).setColor(getColor(i == pagina ? R.color.nui_resaltado
                    : i < pagina ? R.color.nui_oro : R.color.nui_papel_sombra));
            float escala = i == pagina ? 1.4f : 1f;
            punto.animate().scaleX(escala).scaleY(escala).setDuration(150).start();
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