package com.example.nuietsiit.activities.secciones.secretaria;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.nuietsiit.R;
import com.example.nuietsiit.data.DBHelper;
import com.example.nuietsiit.data.SecretariaDB;
import com.example.nuietsiit.data.modelo.Documento;
import com.example.nuietsiit.data.modelo.Paso;
import com.example.nuietsiit.data.modelo.Tramite;
import com.example.nuietsiit.ui.CabeceraNui;
import com.example.nuietsiit.ui.HojasPaginadas;
import com.example.nuietsiit.util.RecienteUtil;
import com.example.nuietsiit.util.tutorial.Tutorial;
import com.google.android.material.appbar.MaterialToolbar;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class DetalleTramite extends PantallaSecretaria {

    private static final DateTimeFormatter FORMATO = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.detalle_tramite);

        MaterialToolbar barra = findViewById(R.id.toolbar);
        setSupportActionBar(barra);
        CabeceraNui.preparar(barra);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(R.string.det_tramite_titulo);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        SecretariaDB db = new SecretariaDB(DBHelper.getInstance(this));
        // int desde Secretaría; String desde "Página reciente" (RecienteUtil solo guarda Strings)
        int idProc = getIntent().getIntExtra("idProcedimiento", -1);
        String idReciente = getIntent().getStringExtra("idProcedimiento");
        if (idReciente != null) idProc = Integer.parseInt(idReciente);
        Tramite tramite = db.getTramite(idProc);
        if (tramite == null) {
            Toast.makeText(this, R.string.det_tramite_no_encontrado, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        RecienteUtil.guardar(
                this,
                DetalleTramite.class,
                getString(R.string.det_tramite_reciente_fmt, tramite.nombre),
                getString(R.string.det_tramite_reciente_subtitulo),
                "idProcedimiento",
                String.valueOf(tramite.id),
                5
        );
        List<Paso> pasos = db.getPasos(tramite.id);
        List<Documento> documentos = db.getDocumentos(tramite.id);

        ((TextView) findViewById(R.id.tvNombre)).setText(tramite.nombre);
        ((TextView) findViewById(R.id.tvDescripcion)).setText(tramite.descripcion);
        TextView tvCategoria = findViewById(R.id.tvCategoria);
        tvCategoria.setText(tramite.etiquetaCategoria());
        tvCategoria.setTextColor(getColor(tramite.colorCategoria()));
        mostrarSeccion(R.id.tarjetaRequisitos, R.id.tvRequisitos, tramite.requisitos);
        mostrarSeccion(R.id.tarjetaPlazos, R.id.tvPlazos, tramite.plazosInfo);
        Pastilla.pintar(findViewById(R.id.tvEstado), tramite.textoEstado(), tramite.colorEstado());
        mostrarDocumentos(documentos);

        String correoUsuario = getIntent().getStringExtra("CORREO");
        View btnPasos = findViewById(R.id.btnPasos);
        ((TextView) findViewById(R.id.tvPasos)).setText(getString(R.string.det_tramite_pasos_fmt, pasos.size()));
        btnPasos.setVisibility(pasos.isEmpty() ? View.GONE : View.VISIBLE);
        btnPasos.setOnClickListener(v -> startActivity(new Intent(this, PasosTramite.class)
                .putExtra("idProcedimiento", tramite.id)
                .putExtra("CORREO", correoUsuario)));

        // un solo botón grande con el siguiente paso; la otra opción queda como enlace
        Intent solicitar = new Intent(this, SolicitudTramite.class)
                .putExtra("idProcedimiento", tramite.id)
                .putExtra("CORREO", correoUsuario);
        Intent pedirCita = new Intent(this, CitaPrevia.class)
                .putExtra("idProcedimiento", tramite.id)
                .putExtra("CORREO", correoUsuario);
        Button btnSiguiente = findViewById(R.id.btnSiguiente);
        Button btnAlternativa = findViewById(R.id.btnAlternativa);
        if (tramite.sePuedeSolicitar()) {
            btnSiguiente.setText(R.string.det_tramite_btn_empezar);
            btnSiguiente.setOnClickListener(v -> startActivity(solicitar));
            btnAlternativa.setText(R.string.det_tramite_btn_alternativa);
            btnAlternativa.setOnClickListener(v -> startActivity(pedirCita));
        } else {
            btnSiguiente.setText(tramite.solicitudOnline
                    ? R.string.det_tramite_btn_cita_consultar
                    : R.string.det_tramite_btn_cita_ventanilla);
            btnSiguiente.setOnClickListener(v -> startActivity(pedirCita));
            btnAlternativa.setVisibility(View.GONE);
        }
        String motivo = motivoNoSolicitable(tramite);
        TextView tvMotivo = findViewById(R.id.tvMotivoSolicitar);
        tvMotivo.setVisibility(motivo != null ? View.VISIBLE : View.GONE);
        tvMotivo.setText(motivo);
    }

    @Override
    protected Tutorial crearTutorial() {
        if (findViewById(R.id.btnSiguiente) == null) return null;
        Tutorial t = new Tutorial(this, "detalle_tramite", 1)
                .info(R.id.tarjetaDocumentos, getString(R.string.det_tramite_tut_documentos)).soloSiSeVe()
                .info(R.id.btnPasos, getString(R.string.det_tramite_tut_pasos)).soloSiSeVe();
        ((HojasPaginadas) findViewById(R.id.hojas)).pasosTutorial(t);
        return t.info(R.id.btnAlternativa, getString(R.string.det_tramite_tut_alternativa)).soloSiSeVe()
                .tocar(R.id.btnSiguiente, getString(R.string.det_tramite_tut_siguiente));
    }

    // oculta la tarjeta si el campo es opcional y no existe
    private void mostrarSeccion(int idTarjeta, int idTexto, String texto) {
        boolean hay = texto != null && !texto.isEmpty();
        findViewById(idTarjeta).setVisibility(hay ? View.VISIBLE : View.GONE);
        ((TextView) findViewById(idTexto)).setText(texto);
    }

    // una fila por documento: nombre (obligatorio u opcional) y, debajo, cómo tiene que ser
    private void mostrarDocumentos(List<Documento> documentos) {
        LinearLayout lista = findViewById(R.id.llDocumentos);
        if (documentos.isEmpty()) {
            lista.addView(textoDocumento(getString(R.string.det_tramite_sin_documentos), false));
            return;
        }
        for (Documento d : documentos) {
            TextView nombre = textoDocumento(d.nombre
                    + (d.obligatorio ? "" : getString(R.string.det_tramite_doc_opcional_sufijo)), true);
            lista.addView(nombre);
            if (d.descripcion != null && !d.descripcion.isEmpty()) {
                TextView detalle = textoDocumento(d.descripcion, false);
                detalle.setPadding(0, 0, 0, dp(2));
                lista.addView(detalle);
            }
        }
    }

    private TextView textoDocumento(String texto, boolean titulo) {
        TextView tv = new TextView(this);
        tv.setText(texto);
        tv.setTextAppearance(titulo ? R.style.TextAppearance_Nui_Cuerpo_Fuerte : R.style.TextAppearance_Nui_Cuerpo_Secundario);
        if (titulo) tv.setPadding(0, dp(10), 0, dp(2));
        return tv;
    }

    private int dp(int valor) {
        return Math.round(valor * getResources().getDisplayMetrics().density);
    }

    // null si se puede solicitar
    private String motivoNoSolicitable(Tramite t) {
        if (t.sePuedeSolicitar()) return null;
        if (!t.solicitudOnline) return getString(R.string.det_tramite_motivo_solo_ventanilla);
        if (t.plazoAbierto() || !t.tienePlazo()) return null;
        LocalDate inicio = LocalDate.parse(t.plazoInicio);
        if (LocalDate.now().isBefore(inicio)) {
            return getString(R.string.det_tramite_motivo_plazo_abre_fmt, inicio.format(FORMATO));
        }
        return getString(R.string.det_tramite_motivo_plazo_termino_fmt, LocalDate.parse(t.plazoFin).format(FORMATO));
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}