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
import androidx.annotation.StringRes;

import com.example.nuietsiit.R;
import com.example.nuietsiit.data.DBHelper;
import com.example.nuietsiit.data.SecretariaDB;
import com.example.nuietsiit.data.modelo.Tramite;
import com.example.nuietsiit.ui.CabeceraNui;
import com.example.nuietsiit.ui.HojasPaginadas;
import com.example.nuietsiit.util.SesionUtil;
import com.example.nuietsiit.util.tutorial.Tutorial;
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
        @StringRes final int textoRes, detalleRes;
        final Pregunta siguiente; // null = acaba en destino
        final String destino;

        Opcion(@StringRes int textoRes, @StringRes int detalleRes, Pregunta siguiente) {
            this(textoRes, detalleRes, siguiente, null);
        }

        Opcion(@StringRes int textoRes, @StringRes int detalleRes, String destino) {
            this(textoRes, detalleRes, null, destino);
        }

        private Opcion(@StringRes int textoRes, @StringRes int detalleRes, Pregunta siguiente, String destino) {
            this.textoRes = textoRes;
            this.detalleRes = detalleRes;
            this.siguiente = siguiente;
            this.destino = destino;
        }
    }

    private static class Pregunta {
        @StringRes final int textoRes;
        final Opcion[] opciones;

        Pregunta(@StringRes int textoRes, Opcion... opciones) {
            this.textoRes = textoRes;
            this.opciones = opciones;
        }
    }

    private static final Pregunta RAIZ = new Pregunta(R.string.qn_p_raiz,
            new Opcion(R.string.qn_op_matricula, R.string.qn_op_matricula_det,
                    new Pregunta(R.string.qn_p_matricula,
                            new Opcion(R.string.qn_op_mat_curso, R.string.qn_op_mat_curso_det,
                                    "Automatrícula"),
                            new Opcion(R.string.qn_op_mat_cambiar, R.string.qn_op_mat_cambiar_det,
                                    "Modificación y ampliación de matrícula"),
                            new Opcion(R.string.qn_op_mat_anular, R.string.qn_op_mat_anular_det,
                                    "Anulación de matrícula"),
                            new Opcion(R.string.qn_op_mat_grupo, R.string.qn_op_mat_grupo_det,
                                    "Cambio de grupo"))),
            new Opcion(R.string.qn_op_examenes, R.string.qn_op_examenes_det,
                    new Pregunta(R.string.qn_p_examenes,
                            new Opcion(R.string.qn_op_ex_unica, R.string.qn_op_ex_unica_det,
                                    "Evaluación única final"),
                            new Opcion(R.string.qn_op_ex_especial, R.string.qn_op_ex_especial_det,
                                    "Convocatoria especial"))),
            new Opcion(R.string.qn_op_creditos, R.string.qn_op_creditos_det,
                    new Pregunta(R.string.qn_p_creditos,
                            new Opcion(R.string.qn_op_cred_estudios, R.string.qn_op_cred_estudios_det,
                                    "Reconocimiento y transferencia de créditos"),
                            new Opcion(R.string.qn_op_cred_actividades, R.string.qn_op_cred_actividades_det,
                                    "Créditos por actividades universitarias"))),
            new Opcion(R.string.qn_op_papel, R.string.qn_op_papel_det,
                    new Pregunta(R.string.qn_p_papel,
                            new Opcion(R.string.qn_op_papel_notas, R.string.qn_op_papel_notas_det,
                                    "Certificado académico personal"),
                            new Opcion(R.string.qn_op_papel_titulo, R.string.qn_op_papel_titulo_det,
                                    "Solicitud del título de grado y SET"))),
            new Opcion(R.string.qn_op_otra, R.string.qn_op_otra_det,
                    new Pregunta(R.string.qn_p_otra,
                            new Opcion(R.string.qn_op_otra_hablar, R.string.qn_op_otra_hablar_det,
                                    DESTINO_CITA),
                            new Opcion(R.string.qn_op_otra_buscar, R.string.qn_op_otra_buscar_det,
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
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(R.string.qn_titulo);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        correoUsuario = SesionUtil.correo(getIntent());
        secretariaDB = new SecretariaDB(DBHelper.getInstance(this));
        if (savedInstanceState != null) {
            ArrayList<Integer> guardado = savedInstanceState.getIntegerArrayList(ESTADO_CAMINO);
            if (guardado != null) camino = guardado;
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
                .tocar(R.id.contenedorOpciones, getString(R.string.qn_tut_opciones))
                .tocar(R.id.btnAnterior, getString(R.string.qn_tut_anterior)).soloSiSeVe();
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
            tvPregunta.setText(pregunta.textoRes);
            for (int i = 0; i < pregunta.opciones.length; i++) {
                Opcion o = pregunta.opciones[i];
                int indice = i;
                anadirTarjeta(
                        camino.isEmpty() ? i : camino.get(0),
                        null,
                        getString(o.textoRes),
                        getString(o.detalleRes),
                        null
                ).setOnClickListener(v -> elegir(indice));
            }
        }
        animarEntrada(direccion);
    }

    private void mostrarPaso(int paso) {
        tvPaso.setText(getString(R.string.paso_numero_fmt, paso, PASOS));
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

        tvPregunta.setText(R.string.qn_resultado_titulo);
        Intent detalle = new Intent(this, DetalleTramite.class)
                .putExtra("idProcedimiento", tramite.id)
                .putExtra("CORREO", correoUsuario);
        mostrarResultado(tramite, camino.get(0)).setOnClickListener(v -> startActivity(detalle));
        btnIrTramite.setText(tramite.sePuedeSolicitar()
                ? R.string.qn_btn_ver_y_empezar
                : R.string.qn_btn_ver_y_cita);
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
        String txtDocs = documentos == 0
                ? getString(R.string.qn_resumen_sin_docs)
                : getResources().getQuantityString(R.plurals.qn_resumen_docs, documentos, documentos);
        String txtPasos = getResources().getQuantityString(R.plurals.qn_resumen_pasos, pasos, pasos);
        ((TextView) tarjeta.findViewById(R.id.tvResumen)).setText(
                getString(R.string.qn_resumen_fmt, txtDocs, txtPasos));
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