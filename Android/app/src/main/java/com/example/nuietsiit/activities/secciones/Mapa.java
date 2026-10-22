package com.example.nuietsiit.activities.secciones;

import android.annotation.SuppressLint;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.nuietsiit.R;

import java.io.IOException;
import java.io.InputStream;

public class Mapa extends AppCompatActivity {

    private ImageView ivMapa;
    private EditText etBuscadorMapa;
    private TextView tvPlantaActual;
    private ImageView ivIcono;

    private Button btnPlantaMinus1, btnPlanta0, btnPlanta1, btnPlanta2, btnPlanta3, btnPlanta4, btnPlanta5;
    private Button[] botonesPlantas;

    private final String[] nombresPlantas = {
            "Planta -1 (Sótano)",
            "Planta Baja (Planta 0)",
            "Planta 1",
            "Planta 2",
            "Planta 3",
            "Planta 4",
            "Planta 5"
    };

    // Ficheros de plano en assets/mapa_etsiit/ConSuelo/
    private final String[] archivosPlantas = {
            "sotano.jpg",
            "baja.jpg",
            "primera.jpg",
            "segunda.jpg",
            "tercera.jpg",
            "cuarta.jpg",
            "quinta.jpg"
    };

    private MatrixTouchListener touchListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.mapa);

        ivMapa = findViewById(R.id.ivMapa);
        etBuscadorMapa = findViewById(R.id.etBuscadorMapa);
        tvPlantaActual = findViewById(R.id.tvPlantaActual);
        ivIcono = findViewById(R.id.ivIcono);

        btnPlantaMinus1 = findViewById(R.id.btnPlantaMinus1);
        btnPlanta0 = findViewById(R.id.btnPlanta0);
        btnPlanta1 = findViewById(R.id.btnPlanta1);
        btnPlanta2 = findViewById(R.id.btnPlanta2);
        btnPlanta3 = findViewById(R.id.btnPlanta3);
        btnPlanta4 = findViewById(R.id.btnPlanta4);
        btnPlanta5 = findViewById(R.id.btnPlanta5);

        botonesPlantas = new Button[]{
                btnPlantaMinus1, btnPlanta0, btnPlanta1, btnPlanta2, btnPlanta3, btnPlanta4, btnPlanta5
        };

        for (int i = 0; i < botonesPlantas.length; i++) {
            final int index = i;
            if (botonesPlantas[i] != null) {
                botonesPlantas[i].setOnClickListener(v -> seleccionarPlanta(index));
            }
        }

        // Listener táctil para zoom con pellizco y desplazamiento (pan) en el mapa
        if (ivMapa != null) {
            touchListener = new MatrixTouchListener();
            ivMapa.setOnTouchListener(touchListener);
        }

        if (ivIcono != null) {
            ivIcono.setOnClickListener(v -> finish());
        }

        // Buscador rápido de aula / planta
        if (etBuscadorMapa != null) {
            etBuscadorMapa.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    filtrarPlantaPorTexto(s.toString().trim());
                }

                @Override
                public void afterTextChanged(Editable s) {}
            });
        }

        // Seleccionar Planta Baja (índice 1) por defecto al iniciar
        seleccionarPlanta(1);
    }

    /**
     * Selecciona una planta, carga su imagen de mapa desde assets y resalta el botón activo.
     */
    private void seleccionarPlanta(int indice) {
        if (indice < 0 || indice >= nombresPlantas.length) return;

        if (tvPlantaActual != null) {
            tvPlantaActual.setText(nombresPlantas[indice]);
        }

        // Cargar imagen de la planta
        cargarImagenPlanta(archivosPlantas[indice]);

        int colorNaranja = 0xFFFF8C42;
        int colorGris = 0xFF888888;

        for (int i = 0; i < botonesPlantas.length; i++) {
            if (botonesPlantas[i] != null) {
                if (i == indice) {
                    botonesPlantas[i].setBackgroundTintList(ColorStateList.valueOf(colorNaranja));
                } else {
                    botonesPlantas[i].setBackgroundTintList(ColorStateList.valueOf(colorGris));
                }
            }
        }
    }

    /**
     * Carga el archivo de imagen JPG de la planta especificada desde assets.
     */
    private void cargarImagenPlanta(String nombreArchivo) {
        if (ivMapa == null) return;

        try (InputStream is = getAssets().open("mapa_etsiit/ConSuelo/" + nombreArchivo)) {
            Bitmap bitmap = BitmapFactory.decodeStream(is);
            if (bitmap != null) {
                ivMapa.setImageBitmap(bitmap);
                ivMapa.setScaleType(ImageView.ScaleType.FIT_CENTER);
                if (touchListener != null) {
                    touchListener.reset(ivMapa);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error al cargar la planta", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Detecta qué planta busca el usuario según el texto introducido en el buscador.
     */
    private void filtrarPlantaPorTexto(String texto) {
        if (texto.isEmpty()) return;
        String query = texto.toLowerCase();

        if (query.contains("-1") || query.contains("sotano") || query.contains("sótano")) {
            seleccionarPlanta(0);
        } else if (query.startsWith("0.") || query.contains("baja") || query.contains("conserj") || query.contains("cafeter")) {
            seleccionarPlanta(1);
        } else if (query.startsWith("1.") || query.contains("primera")) {
            seleccionarPlanta(2);
        } else if (query.startsWith("2.") || query.contains("segunda")) {
            seleccionarPlanta(3);
        } else if (query.startsWith("3.") || query.contains("tercera")) {
            seleccionarPlanta(4);
        } else if (query.startsWith("4.") || query.contains("cuarta")) {
            seleccionarPlanta(5);
        } else if (query.startsWith("5.") || query.contains("quinta")) {
            seleccionarPlanta(6);
        }
    }

    /**
     * Listener para gestionar los gestos de Zoom (pellizco) y Desplazamiento (pan) en el mapa.
     */
    private static class MatrixTouchListener implements View.OnTouchListener {
        private final Matrix matrix = new Matrix();
        private final Matrix savedMatrix = new Matrix();

        private static final int NONE = 0;
        private static final int DRAG = 1;
        private static final int ZOOM = 2;
        private int mode = NONE;

        private final PointF start = new PointF();
        private final PointF mid = new PointF();
        private float oldDist = 1f;

        @SuppressLint("ClickableViewAccessibility")
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            ImageView view = (ImageView) v;
            view.setScaleType(ImageView.ScaleType.MATRIX);

            switch (event.getAction() & MotionEvent.ACTION_MASK) {
                case MotionEvent.ACTION_DOWN:
                    savedMatrix.set(matrix);
                    start.set(event.getX(), event.getY());
                    mode = DRAG;
                    break;

                case MotionEvent.ACTION_POINTER_DOWN:
                    oldDist = spacing(event);
                    if (oldDist > 10f) {
                        savedMatrix.set(matrix);
                        midPoint(mid, event);
                        mode = ZOOM;
                    }
                    break;

                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_POINTER_UP:
                    mode = NONE;
                    break;

                case MotionEvent.ACTION_MOVE:
                    if (mode == DRAG) {
                        matrix.set(savedMatrix);
                        float dx = event.getX() - start.x;
                        float dy = event.getY() - start.y;
                        matrix.postTranslate(dx, dy);
                    } else if (mode == ZOOM) {
                        float newDist = spacing(event);
                        if (newDist > 10f) {
                            matrix.set(savedMatrix);
                            float scale = newDist / oldDist;
                            matrix.postScale(scale, scale, mid.x, mid.y);
                        }
                    }
                    break;
            }

            view.setImageMatrix(matrix);
            return true;
        }

        private float spacing(MotionEvent event) {
            if (event.getPointerCount() < 2) return 0f;
            float x = event.getX(0) - event.getX(1);
            float y = event.getY(0) - event.getY(1);
            return (float) Math.sqrt(x * x + y * y);
        }

        private void midPoint(PointF point, MotionEvent event) {
            if (event.getPointerCount() < 2) return;
            float x = event.getX(0) + event.getX(1);
            float y = event.getY(0) + event.getY(1);
            point.set(x / 2, y / 2);
        }

        public void reset(ImageView view) {
            matrix.reset();
            mode = NONE;
            if (view != null) {
                view.setImageMatrix(matrix);
            }
        }
    }
}
