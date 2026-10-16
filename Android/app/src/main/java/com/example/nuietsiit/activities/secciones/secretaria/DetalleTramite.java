package com.example.nuietsiit.activities.secciones.secretaria;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.nuietsiit.R;
import com.example.nuietsiit.data.DBHelper;
import com.example.nuietsiit.data.SecretariaDB;
import com.example.nuietsiit.data.modelo.Documento;
import com.example.nuietsiit.data.modelo.Paso;
import com.example.nuietsiit.data.modelo.Tramite;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.chip.Chip;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class DetalleTramite extends AppCompatActivity {

    private static final DateTimeFormatter FORMATO = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.detalle_tramite);

        MaterialToolbar barra = findViewById(R.id.toolbar);
        setSupportActionBar(barra);
        getSupportActionBar().setTitle("Trámite");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        SecretariaDB db = new SecretariaDB(DBHelper.getInstance(this));
        Tramite tramite = db.getTramite(getIntent().getIntExtra("idProcedimiento", -1));
        if (tramite == null) {
            Toast.makeText(this, "Trámite no encontrado", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        List<Paso> pasos = db.getPasos(tramite.id);
        List<Documento> documentos = db.getDocumentos(tramite.id);

        ((TextView) findViewById(R.id.tvNombre)).setText(tramite.nombre);
        ((TextView) findViewById(R.id.tvDescripcion)).setText(tramite.descripcion);
        mostrarSeccion(R.id.tvTituloRequisitos, R.id.tvRequisitos, tramite.requisitos);
        mostrarSeccion(R.id.tvTituloPlazos, R.id.tvPlazos, tramite.plazosInfo);
        mostrarChipPlazo(tramite);
        mostrarDocumentos(documentos);

        // TODO tarea 6: CitaPrevia (con el motivo ya elegido); tarea 7: SolicitudTramite
        Button btnPasos = findViewById(R.id.btnPasos);
        btnPasos.setText("Ver pasos (" + pasos.size() + ")");
        btnPasos.setEnabled(!pasos.isEmpty());
        btnPasos.setOnClickListener(v -> startActivity(new Intent(this, PasosTramite.class)
                .putExtra("idProcedimiento", tramite.id)));
        findViewById(R.id.btnPedirCita).setOnClickListener(v -> proximamente());

        Button btnSolicitar = findViewById(R.id.btnSolicitar);
        btnSolicitar.setEnabled(tramite.sePuedeSolicitar());
        btnSolicitar.setOnClickListener(v -> proximamente());
        String motivo = motivoNoSolicitable(tramite);
        TextView tvMotivo = findViewById(R.id.tvMotivoSolicitar);
        tvMotivo.setVisibility(motivo != null ? View.VISIBLE : View.GONE);
        tvMotivo.setText(motivo);
    }

    private void proximamente() {
        Toast.makeText(this, "Próximamente", Toast.LENGTH_SHORT).show();
    }

    // oculta título y texto si el campo es opcional y no existe
    private void mostrarSeccion(int idTitulo, int idTexto, String texto) {
        boolean hay = texto != null && !texto.isEmpty();
        findViewById(idTitulo).setVisibility(hay ? View.VISIBLE : View.GONE);
        TextView tv = findViewById(idTexto);
        tv.setVisibility(hay ? View.VISIBLE : View.GONE);
        tv.setText(texto);
    }

    private void mostrarChipPlazo(Tramite t) {
        Chip chip = findViewById(R.id.chipPlazo);
        if (!t.tienePlazo()) return;
        LocalDate hoy = LocalDate.now();
        chip.setVisibility(View.VISIBLE);
        if (t.plazoAbierto()) chip.setText("Plazo abierto");
        else if (hoy.isBefore(LocalDate.parse(t.plazoInicio))) chip.setText("Plazo aún no abierto");
        else chip.setText("Plazo cerrado");
    }

    private void mostrarDocumentos(List<Documento> documentos) {
        LinearLayout lista = findViewById(R.id.llDocumentos);
        if (documentos.isEmpty()) {
            TextView tv = new TextView(this);
            tv.setText("No hace falta aportar documentos.");
            lista.addView(tv);
            return;
        }
        for (Documento d : documentos) {
            TextView tv = new TextView(this);
            StringBuilder sb = new StringBuilder("• ").append(d.nombre);
            sb.append(d.obligatorio ? " (obligatorio)" : " (opcional)");
            if (d.descripcion != null && !d.descripcion.isEmpty()) sb.append("\n   ").append(d.descripcion);
            tv.setText(sb);
            tv.setPadding(0, 4, 0, 4);
            lista.addView(tv);
        }
    }

    // null si se puede solicitar
    private String motivoNoSolicitable(Tramite t) {
        if (t.sePuedeSolicitar()) return null;
        if (!t.solicitudOnline) return "Este trámite solo se hace en ventanilla: pide cita.";
        if (t.plazoAbierto() || !t.tienePlazo()) return null;
        LocalDate inicio = LocalDate.parse(t.plazoInicio);
        if (LocalDate.now().isBefore(inicio)) {
            return "El plazo se abre el " + inicio.format(FORMATO) + ".";
        }
        return "El plazo terminó el " + LocalDate.parse(t.plazoFin).format(FORMATO) + ".";
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
