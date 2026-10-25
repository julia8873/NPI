package com.example.nuietsiit.activities.secciones.mapa;

import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.nuietsiit.R;

import java.util.List;
import java.util.Locale;

/**
 * Visor de ruta: pantalla negra que enseña una foto según hacia dónde mire el usuario.
 * Primera versión: solo hay una foto de prueba, que se ve al mirar al NORTE (±30°).
 *
 * Extras: ORIGEN_ID, ORIGEN_NOMBRE, ORIGEN_METODO, DESTINO_NOMBRE
 * Prueba sin sensores: mantener pulsado el texto del rumbo para activar el deslizador manual.
 */
public class VisorRuta extends AppCompatActivity implements Brujula.Listener {

    private ImageView ivFotoRuta;
    private TextView tvRumbo;
    private TextView tvEstadoFoto;
    private TextView tvTrayecto;
    private SeekBar sbRumboManual;

    private Brujula brujula;
    private List<FotosRuta.Foto> fotos;
    private int recursoMostrado = 0;
    private boolean modoManual = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.visor_ruta);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        ivFotoRuta = findViewById(R.id.ivFotoRuta);
        tvRumbo = findViewById(R.id.tvRumbo);
        tvEstadoFoto = findViewById(R.id.tvEstadoFoto);
        tvTrayecto = findViewById(R.id.tvTrayecto);
        sbRumboManual = findViewById(R.id.sbRumboManual);
        Button btnCerrar = findViewById(R.id.btnCerrarVisor);

        String origenId = getIntent().getStringExtra("ORIGEN_ID");
        String origenNombre = getIntent().getStringExtra("ORIGEN_NOMBRE");
        String metodo = getIntent().getStringExtra("ORIGEN_METODO");
        String destino = getIntent().getStringExtra("DESTINO_NOMBRE");

        tvTrayecto.setText("📍 " + valor(origenNombre) + (metodo == null ? "" : " (" + metodo + ")")
                + "\n🎯 " + valor(destino));

        fotos = FotosRuta.cargarConFallback(this, origenId == null ? FotosRuta.CLAVE_DEMO : origenId);

        btnCerrar.setOnClickListener(v -> finish());

        brujula = new Brujula(this, this);

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
            tvEstadoFoto.setText("Este móvil no tiene sensor de orientación: usa el deslizador");
            activarModoManual(true);
        } else {
            tvEstadoFoto.setText("Esperando al sensor de orientación...");
        }
    }

    private static String valor(String s) {
        return s == null ? "?" : s;
    }

    private void activarModoManual(boolean activo) {
        modoManual = activo;
        sbRumboManual.setVisibility(activo ? View.VISIBLE : View.GONE);
        if (activo) mostrarRumbo(sbRumboManual.getProgress());
    }

    @Override
    protected void onResume() {
        super.onResume();
        brujula.iniciar();
    }

    @Override
    protected void onPause() {
        super.onPause();
        brujula.parar();
    }

    @Override
    public void onRumbo(float grados) {
        if (!modoManual) mostrarRumbo(grados);
    }

    private void mostrarRumbo(float rumbo) {
        int redondeado = Math.round(rumbo) % 360;
        tvRumbo.setText(String.format(Locale.getDefault(), "Rumbo: %d° %s", redondeado, puntoCardinal(rumbo)));

        FotosRuta.Foto foto = FotosRuta.elegir(fotos, rumbo);
        int nuevo = (foto == null) ? 0 : foto.idRecurso;
        if (nuevo == recursoMostrado) return;
        recursoMostrado = nuevo;

        if (nuevo == 0) {
            tvEstadoFoto.setText("Sin foto en esta dirección (negro)");
            ivFotoRuta.animate().alpha(0f).setDuration(150).start();
        } else {
            tvEstadoFoto.setText(String.format(Locale.getDefault(), "Mostrando foto de %d°", foto.grados));
            ivFotoRuta.setImageResource(nuevo);
            ivFotoRuta.animate().alpha(1f).setDuration(150).start();
        }
    }

    private static String puntoCardinal(float grados) {
        String[] nombres = {"N", "NE", "E", "SE", "S", "SO", "O", "NO"};
        return nombres[Math.round(grados / 45f) % 8];
    }
}
