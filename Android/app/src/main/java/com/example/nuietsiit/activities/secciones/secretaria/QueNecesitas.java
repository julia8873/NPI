package com.example.nuietsiit.activities.secciones.secretaria;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.OvershootInterpolator;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;

import com.example.nuietsiit.ui.CabeceraNui;
import com.example.nuietsiit.R;
import com.example.nuietsiit.ui.HojasPaginadas;
import com.example.nuietsiit.util.tutorial.Gesto;
import com.example.nuietsiit.util.tutorial.Tutorial;
import com.example.nuietsiit.data.DBHelper;
import com.example.nuietsiit.data.SecretariaDB;
import com.example.nuietsiit.data.modelo.Tramite;
import com.example.nuietsiit.util.SesionUtil;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.progressindicator.LinearProgressIndicator;

import java.util.ArrayList;

/**
 * "¿Qué necesitas?": dos preguntas con opciones grandes que acaban en un trámite,
 * para no tener que buscar en el catálogo. Paso 3 = el trámite encontrado.
 */
public class QueNecesitas extends PantallaSecretaria {

    // resultado para Secretaria: abrir el catálogo con el buscador
    public static final String EXTRA_VER_CATALOGO = "verCatalogo";

    private static final String ESTADO_CAMINO = "camino";
    private static final String DESTINO_CITA = "#cita";
    private static final String DESTINO_CATALOGO = "#catalogo";
    private static final int PASOS = 3;

    // ------------------------------------------------------------
    //   Árbol de preguntas (los destinos son nombres de trámites de TablasSecretaria)
    // ------------------------------------------------------------

    private static class Opcion {
        final String texto, detalle;
        final Pregunta siguiente; // null = acaba en destino
        final String destino;

        Opcion(String texto, String detalle, Pregunta siguiente) {
            this(texto, detalle, siguiente, null);
        }

        Opcion(String texto, String detalle, String destino) {
            this(texto, detalle, null, destino);
        }

        private Opcion(String texto, String detalle, Pregunta siguiente, String destino) {
            this.texto = texto;
            this.detalle = detalle;
            this.siguiente = siguiente;
            this.destino = destino;
        }
    }

    private static class Pregunta {
        final String texto;
        final Opcion[] opciones;

        Pregunta(String texto, Opcion... opciones) {
            this.texto = texto;
            this.opciones = opciones;
        }
    }

    private static final Pregunta RAIZ = new Pregunta("¿Sobre qué es?",
            new Opcion("Mi matrícula", "Matricularme, cambiarla o anularla",
                    new Pregunta("¿Qué quieres hacer con tu matrícula?",
                            new Opcion("Matricularme este curso", "Elegir las asignaturas del curso",
                                    "Automatrícula"),
                            new Opcion("Cambiar o añadir asignaturas", "Dentro del plazo oficial",
                                    "Modificación y ampliación de matrícula"),
                            new Opcion("Anularla", "Toda o solo una parte",
                                    "Anulación de matrícula"),
                            new Opcion("Cambiarme de grupo", "De teoría o de prácticas",
                                    "Cambio de grupo"))),
            new Opcion("Mis exámenes", "Cómo y cuándo me examino",
                    new Pregunta("¿Qué te pasa con los exámenes?",
                            new Opcion("Quiero hacer solo el examen final", "Sin evaluación continua",
                                    "Evaluación única final"),
                            new Opcion("Me queda poco para acabar", "Una convocatoria extra",
                                    "Convocatoria especial"))),
            new Opcion("Créditos que ya tengo", "Convalidar lo que hiciste en otro sitio",
                    new Pregunta("¿De dónde vienen esos créditos?",
                            new Opcion("De otros estudios", "Otra carrera o FP superior",
                                    "Reconocimiento y transferencia de créditos"),
                            new Opcion("De actividades", "Culturales, deportivas, solidarias…",
                                    "Créditos por actividades universitarias"))),
            new Opcion("Un papel oficial", "Certificados y el título",
                    new Pregunta("¿Qué papel necesitas?",
                            new Opcion("Mis notas y la nota media", "Certificado académico",
                                    "Certificado académico personal"),
                            new Opcion("El título", "Porque ya he terminado",
                                    "Solicitud del título de grado y SET"))),
            new Opcion("Otra cosa", "No lo veo aquí",
                    new Pregunta("¿Cómo seguimos?",
                            new Opcion("Hablarlo en Secretaría", "Pide una cita de consulta general",
                                    DESTINO_CITA),
                            new Opcion("Buscarlo yo", "Ver todos los trámites",
                                    DESTINO_CATALOGO))));

    // ------------------------------------------------------------

    // icono, color y tinte de cada tema de la primera pregunta (sus opciones heredan los del tema)
    private static final int[] ICONOS = {R.drawable.nui_ic_matricula, R.drawable.nui_ic_examen,
            R.drawable.nui_ic_creditos, R.drawable.nui_ic_titulo, R.drawable.nui_ic_consulta};
    private static final int[] COLORES = {R.color.nui_tema_matricula, R.color.nui_tema_examenes,
            R.color.nui_tema_creditos, R.color.nui_tema_titulos, R.color.nui_tema_certificados};
    private static final int[] TINTES = {R.color.nui_tema_matricula_tinte, R.color.nui_tema_examenes_tinte,
            R.color.nui_tema_creditos_tinte, R.color.nui_tema_titulos_tinte, R.color.nui_tema_certificados_tinte};

    private SecretariaDB secretariaDB;
    private String correoUsuario;
    private ArrayList<Integer> camino = new ArrayList<>(); // índices elegidos desde la raíz

    private TextView tvPaso, tvPregunta;
    private ImageView ivRosa;
    private LinearProgressIndicator progreso;
    private LinearLayout contenedorOpciones;
    private Button btnIrTramite, btnAnterior;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.que_necesitas);

        MaterialToolbar barra = findViewById(R.id.toolbar);
        setSupportActionBar(barra);
        CabeceraNui.preparar(barra);
        getSupportActionBar().setTitle("¿Qué necesitas?");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        correoUsuario = SesionUtil.correo(getIntent());
        secretariaDB = new SecretariaDB(DBHelper.getInstance(this));
        if (savedInstanceState != null) {
            camino = savedInstanceState.getIntegerArrayList(ESTADO_CAMINO);
        }

        tvPaso = findViewById(R.id.tvPaso);
        tvPregunta = findViewById(R.id.tvPregunta);
        ivRosa = findViewById(R.id.ivRosa);
        progreso = findViewById(R.id.progreso);
        contenedorOpciones = findViewById(R.id.contenedorOpciones);
        btnIrTramite = findViewById(R.id.btnIrTramite);
        btnAnterior = findViewById(R.id.btnAnterior);

        btnAnterior.setOnClickListener(v -> volver());
        // atrás del sistema = pregunta anterior; en la primera, salir
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (camino.isEmpty()) finish();
                else volver();
            }
        });

        mostrar(0);
    }

    @Override
    protected Tutorial crearTutorial() {
        Tutorial t = new Tutorial(this, "que_necesitas", 1)
                .alEmpezar(() -> { // desde la primera pregunta
                    camino.clear();
                    mostrar(0);
                })
                .tocar(R.id.contenedorOpciones, "Toca la opción que mejor te describa: con unas pocas preguntas llegas a tu trámite")
                .tocar(R.id.btnAnterior, "¿Te has equivocado? Toca aquí para volver a la pregunta anterior").soloSiSeVe();
        return ((HojasPaginadas) findViewById(R.id.hojas)).pasosTutorial(t);
    }

    @Override
    protected void onSaveInstanceState(Bundle salida) {
        super.onSaveInstanceState(salida);
        salida.putIntegerArrayList(ESTADO_CAMINO, camino);
    }

    private void elegir(int indice) {
        camino.add(indice);
        mostrar(1);
    }

    private void volver() {
        if (camino.isEmpty()) return;
        camino.remove(camino.size() - 1);
        mostrar(-1);
    }

    // direccion: 1 = avanza (entra por la derecha), -1 = vuelve (por la izquierda), 0 = sin animación
    private void mostrar(int direccion) {
        // recorre el árbol por el camino elegido
        Pregunta pregunta = RAIZ;
        Opcion ultima = null;
        for (int i : camino) {
            ultima = pregunta.opciones[i];
            if (ultima.siguiente != null) pregunta = ultima.siguiente;
        }

        contenedorOpciones.removeAllViews();
        btnAnterior.setVisibility(camino.isEmpty() ? View.INVISIBLE : View.VISIBLE);
        btnIrTramite.setVisibility(View.GONE);

        if (ultima != null && ultima.destino != null) {
            if (!irADestino(ultima.destino)) return;
            mostrarPaso(PASOS);
        } else {
            mostrarPaso(camino.size() + 1);
            tvPregunta.setText(pregunta.texto);
            for (int i = 0; i < pregunta.opciones.length; i++) {
                Opcion o = pregunta.opciones[i];
                int indice = i;
                anadirTarjeta(camino.isEmpty() ? i : camino.get(0), null, o.texto, o.detalle, null).setOnClickListener(v -> elegir(indice));
            }
        }
        animarEntrada(direccion);
    }

    private void mostrarPaso(int paso) {
        tvPaso.setText("Paso " + paso + " de " + PASOS);
        progreso.setProgressCompat(paso, true);
        // la rosa de los vientos se orienta un rumbo (45°) con cada respuesta
        ivRosa.animate().rotation(45f * (paso - 1)).setDuration(400)
                .setInterpolator(new OvershootInterpolator()).start();
    }

    // destinos especiales salen de aquí; un trámite se enseña como paso 3. false = ya no hay nada que mostrar
    private boolean irADestino(String destino) {
        if (DESTINO_CITA.equals(destino)) {
            camino.remove(camino.size() - 1); // al volver de la cita, sigue en la pregunta anterior
            startActivity(new Intent(this, CitaPrevia.class).putExtra("CORREO", correoUsuario));
            mostrar(0);
            return false;
        }
        if (DESTINO_CATALOGO.equals(destino)) {
            setResult(RESULT_OK, new Intent().putExtra(EXTRA_VER_CATALOGO, true));
            finish();
            return false;
        }
        Tramite tramite = buscarTramite(destino);
        if (tramite == null) { // los datos han cambiado: mejor el catálogo que nada
            return irADestino(DESTINO_CATALOGO);
        }

        tvPregunta.setText("Esto es lo que buscas");
        Intent detalle = new Intent(this, DetalleTramite.class)
                .putExtra("idProcedimiento", tramite.id)
                .putExtra("CORREO", correoUsuario);
        mostrarResultado(tramite, camino.get(0)).setOnClickListener(v -> startActivity(detalle));
        btnIrTramite.setText(tramite.sePuedeSolicitar() ? "Ver qué necesito y empezar" : "Ver detalles y pedir cita");
        btnIrTramite.setVisibility(View.VISIBLE);
        btnIrTramite.setOnClickListener(v -> startActivity(detalle));
        return true;
    }

    private Tramite buscarTramite(String nombre) {
        for (Tramite t : secretariaDB.getTramites(null)) {
            if (t.nombre.equals(nombre)) return t;
        }
        return null;
    }

    // tema = la opción elegida en la primera pregunta (da icono y color)
    private View mostrarResultado(Tramite t, int tema) {
        View tarjeta = LayoutInflater.from(this).inflate(R.layout.item_resultado_tramite, contenedorOpciones, false);
        ponerIcono(tarjeta.findViewById(R.id.ivIcono), tema);
        TextView categoria = tarjeta.findViewById(R.id.tvCategoria);
        categoria.setText(t.etiquetaCategoria());
        categoria.setTextColor(getColor(t.colorCategoria()));
        ((TextView) tarjeta.findViewById(R.id.tvNombre)).setText(t.nombre);
        ((TextView) tarjeta.findViewById(R.id.tvDescripcion)).setText(t.descripcion);
        Pastilla.pintar(tarjeta.findViewById(R.id.tvEstado), t.textoEstado(), t.colorEstado());
        int documentos = secretariaDB.getDocumentos(t.id).size();
        int pasos = secretariaDB.getPasos(t.id).size();
        ((TextView) tarjeta.findViewById(R.id.tvResumen)).setText(
                (documentos == 0 ? "Sin documentos" : documentos == 1 ? "1 documento" : documentos + " documentos")
                + " · " + pasos + (pasos == 1 ? " paso" : " pasos"));
        contenedorOpciones.addView(tarjeta);
        return tarjeta;
    }

    private View anadirTarjeta(int tema, String etiqueta, String titulo, String detalle, String accion) {
        View tarjeta = LayoutInflater.from(this)
                .inflate(R.layout.item_tarjeta_lo_tuyo, contenedorOpciones, false);
        ImageView ivIcono = tarjeta.findViewById(R.id.ivIcono);
        ivIcono.setBackgroundResource(R.drawable.nui_tesela_estrella);
        ponerIcono(ivIcono, tema);
        ivIcono.setVisibility(View.VISIBLE);
        tarjeta.findViewById(R.id.tvIcono).setVisibility(View.GONE);
        ponerTexto(tarjeta.findViewById(R.id.tvEtiqueta), etiqueta);
        ponerTexto(tarjeta.findViewById(R.id.tvTitulo), titulo);
        ponerTexto(tarjeta.findViewById(R.id.tvDetalle), detalle);
        ponerTexto(tarjeta.findViewById(R.id.tvAccion), accion);
        contenedorOpciones.addView(tarjeta);
        return tarjeta;
    }

    // icono del tema dibujado sobre una estrella nazarí de su color
    private void ponerIcono(ImageView iv, int tema) {
        iv.setImageResource(ICONOS[tema]);
        iv.setImageTintList(ColorStateList.valueOf(getColor(COLORES[tema])));
        iv.setBackgroundTintList(ColorStateList.valueOf(getColor(TINTES[tema])));
    }

    private static void ponerTexto(TextView tv, String texto) {
        tv.setText(texto);
        tv.setVisibility(texto != null ? View.VISIBLE : View.GONE);
    }

    // las opciones entran de lado, una tras otra, en el sentido en que se avanza
    private void animarEntrada(int direccion) {
        if (direccion == 0) return;
        float desde = direccion * 48 * getResources().getDisplayMetrics().density;
        for (int i = 0; i < contenedorOpciones.getChildCount(); i++) {
            View v = contenedorOpciones.getChildAt(i);
            v.setAlpha(0f);
            v.setTranslationX(desde);
            v.animate().alpha(1f).translationX(0f).setStartDelay(50L * i).setDuration(220).start();
        }
        tvPregunta.setAlpha(0f);
        tvPregunta.animate().alpha(1f).setDuration(220).start();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
