package com.example.nuietsiit.activities.secciones.secretaria;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.nuietsiit.R;
import com.example.nuietsiit.data.DBHelper;
import com.example.nuietsiit.data.SecretariaDB;
import com.example.nuietsiit.data.modelo.Cita;
import com.example.nuietsiit.data.modelo.Tramite;
import com.example.nuietsiit.util.FechasUtil;
import com.example.nuietsiit.util.SesionUtil;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Set;

/**
 * Cita previa en la secretaría: motivo → día (próximos 10 laborables) → franja de 10 min → confirmar.
 * Máximo una cita activa por motivo: si ya hay, elegir otra franja la cambia.
 */
public class CitaPrevia extends AppCompatActivity {

    private static final String ESTADO_MOTIVO = "motivo";
    private static final String ESTADO_FECHA = "fecha";
    private static final Locale ES = Locale.forLanguageTag("es-ES");

    private static final int NUM_DIAS = 10;
    private static final LocalTime PRIMERA_FRANJA = LocalTime.of(9, 0);
    private static final LocalTime ULTIMA_FRANJA = LocalTime.of(13, 50);
    private static final int MINUTOS_FRANJA = 10;
    // parte de franjas que salen ocupadas "de relleno" (simula otros alumnos)
    private static final float PROB_OCUPADA = 0.35f;

    private SecretariaDB db;
    private String correoUsuario;
    private final List<Tramite> tramites = new ArrayList<>();
    private final List<String> nombresMotivos = new ArrayList<>();
    private int motivoSeleccionado; // 0 = consulta general, i = tramites.get(i - 1)
    private LocalDate fechaSeleccionada;
    private Cita citaExistente;     // cita activa y futura para el motivo elegido (o null)
    private AlertDialog dialogoConfirmar; // evita abrir dos a la vez con un doble toque

    private MaterialAutoCompleteTextView actvMotivo;
    private TextView tvCitaExistente, tvTituloHoras;
    private ChipGroup chipsDias;
    private FranjaAdapter adaptador;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.cita_previa);

        MaterialToolbar barra = findViewById(R.id.toolbar);
        setSupportActionBar(barra);
        getSupportActionBar().setTitle("Pedir cita");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        correoUsuario = SesionUtil.correo(getIntent());
        db = new SecretariaDB(DBHelper.getInstance(this));

        actvMotivo = findViewById(R.id.actvMotivo);
        tvCitaExistente = findViewById(R.id.tvCitaExistente);
        tvTituloHoras = findViewById(R.id.tvTituloHoras);
        chipsDias = findViewById(R.id.chipsDias);

        adaptador = new FranjaAdapter(this::confirmar);
        RecyclerView rv = findViewById(R.id.rvFranjas);
        rv.setLayoutManager(new GridLayoutManager(this, 4));
        rv.setAdapter(adaptador);

        tramites.addAll(db.getTramites(null));
        List<LocalDate> dias = proximosLaborables();
        if (savedInstanceState != null) {
            motivoSeleccionado = savedInstanceState.getInt(ESTADO_MOTIVO);
            fechaSeleccionada = LocalDate.parse(savedInstanceState.getString(ESTADO_FECHA));
        } else {
            motivoSeleccionado = indiceMotivoInicial(getIntent().getIntExtra("idProcedimiento", -1));
        }
        // al restaurar más tarde (p. ej. al día siguiente) el día guardado puede no estar ya en la lista
        if (!dias.contains(fechaSeleccionada)) fechaSeleccionada = dias.get(0);

        crearMotivos();
        crearChipsDias(dias);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // al volver a la pantalla la cita existente puede haber pasado y las franjas de hoy haber caducado
        actualizarCitaExistente();
        cargarFranjas();
    }

    @Override
    protected void onSaveInstanceState(Bundle salida) {
        super.onSaveInstanceState(salida);
        salida.putInt(ESTADO_MOTIVO, motivoSeleccionado);
        salida.putString(ESTADO_FECHA, fechaSeleccionada.toString());
    }

    // -------------------------------------------------
    //                    MOTIVO
    // -------------------------------------------------
    private void crearMotivos() {
        nombresMotivos.add("Consulta general");
        for (Tramite t : tramites) nombresMotivos.add(t.nombre);

        actvMotivo.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, nombresMotivos));
        // el texto se pone a mano: si se restaura solo, el filtro del desplegable oculta las demás opciones
        actvMotivo.setSaveEnabled(false);
        actvMotivo.setText(nombresMotivos.get(motivoSeleccionado), false);
        actvMotivo.setOnItemClickListener((padre, v, posicion, id) -> {
            motivoSeleccionado = posicion;
            actualizarCitaExistente();
            cargarFranjas();
        });
    }

    private int indiceMotivoInicial(int idProcedimiento) {
        for (int i = 0; i < tramites.size(); i++) {
            if (tramites.get(i).id == idProcedimiento) return i + 1;
        }
        return 0;
    }

    private Integer idProcedimientoSeleccionado() {
        return motivoSeleccionado == 0 ? null : tramites.get(motivoSeleccionado - 1).id;
    }

    private void actualizarCitaExistente() {
        citaExistente = db.getCitaActiva(correoUsuario, idProcedimientoSeleccionado());
        tvCitaExistente.setVisibility(citaExistente != null ? View.VISIBLE : View.GONE);
        if (citaExistente != null) {
            tvCitaExistente.setText("Ya tienes cita para este motivo el " + textoFechaHora(citaExistente.fechaHora()) +
                    ". Si eliges otra hora, se cambiará.");
        }
    }

    // -------------------------------------------------
    //                     DÍAS
    // -------------------------------------------------
    // hoy (si aún quedan franjas) y los siguientes días de lunes a viernes
    private List<LocalDate> proximosLaborables() {
        List<LocalDate> dias = new ArrayList<>();
        LocalDate dia = LocalDate.now();
        if (!LocalTime.now().isBefore(ULTIMA_FRANJA)) dia = dia.plusDays(1);
        while (dias.size() < NUM_DIAS) {
            if (dia.getDayOfWeek() != DayOfWeek.SATURDAY && dia.getDayOfWeek() != DayOfWeek.SUNDAY) dias.add(dia);
            dia = dia.plusDays(1);
        }
        return dias;
    }

    private void crearChipsDias(List<LocalDate> dias) {
        for (int i = 0; i < dias.size(); i++) {
            LocalDate dia = dias.get(i);
            Chip chip = new Chip(this, null, com.google.android.material.R.attr.chipStyle);
            chip.setId(2000 + i);
            chip.setText(etiquetaDia(dia));
            chip.setTag(dia);
            chip.setCheckable(true);
            chip.setChipBackgroundColorResource(R.color.chip_fondo);
            chip.setTextColor(getColorStateList(R.color.chip_texto));
            chip.setCheckedIconVisible(false);
            chip.setContentDescription(FechasUtil.diaLargo(dia.atStartOfDay()));
            chipsDias.addView(chip);
            if (dia.equals(fechaSeleccionada)) chip.setChecked(true);
        }
        chipsDias.setOnCheckedStateChangeListener((grupo, ids) -> {
            if (ids.isEmpty()) return;
            fechaSeleccionada = (LocalDate) grupo.findViewById(ids.get(0)).getTag();
            cargarFranjas();
        });
    }

    // "Hoy 22 oct", "Mañana 23 oct", "Lun 26 oct"
    private String etiquetaDia(LocalDate dia) {
        long dias = FechasUtil.diasHasta(dia);
        String nombre = dias <= 1 ? FechasUtil.cuando(dia.atStartOfDay())
                : FechasUtil.capitalizar(dia.getDayOfWeek().getDisplayName(TextStyle.SHORT, ES).replace(".", ""));
        return nombre + " " + FechasUtil.diaCorto(dia);
    }

    // -------------------------------------------------
    //                    FRANJAS
    // -------------------------------------------------
    private void cargarFranjas() {
        tvTituloHoras.setText("Elige la hora · " + FechasUtil.capitalizar(FechasUtil.diaLargo(fechaSeleccionada.atStartOfDay())));

        String fecha = fechaSeleccionada.toString();
        Set<String> ocupadasBD = db.getHorasOcupadas(fecha);
        // misma semilla para el mismo día → siempre las mismas franjas ocupadas
        Random relleno = new Random(fechaSeleccionada.toEpochDay());
        LocalDateTime ahora = LocalDateTime.now();
        String horaMia = citaExistente != null && citaExistente.fecha.equals(fecha) ? citaExistente.hora : null;

        List<FranjaAdapter.Franja> franjas = new ArrayList<>();
        for (LocalTime t = PRIMERA_FRANJA; !t.isAfter(ULTIMA_FRANJA); t = t.plusMinutes(MINUTOS_FRANJA)) {
            String hora = t.toString(); // "HH:mm" (sin segundos porque valen 0)
            boolean ocupadaRelleno = relleno.nextFloat() < PROB_OCUPADA; // se saca siempre para no desordenar la serie
            boolean pasada = !fechaSeleccionada.atTime(t).isAfter(ahora);
            int estado;
            if (hora.equals(horaMia)) estado = FranjaAdapter.TUYA;
            else if (pasada || ocupadaRelleno || ocupadasBD.contains(hora)) estado = FranjaAdapter.OCUPADA;
            else estado = FranjaAdapter.LIBRE;
            franjas.add(new FranjaAdapter.Franja(hora, estado));
        }
        adaptador.setFranjas(franjas);
    }

    private void confirmar(String hora) {
        if (dialogoConfirmar != null && dialogoConfirmar.isShowing()) return;
        actualizarCitaExistente();
        String cuando = textoFechaHora(fechaSeleccionada.atTime(LocalTime.parse(hora)));
        String motivo = nombresMotivos.get(motivoSeleccionado);
        MaterialAlertDialogBuilder dialogo = new MaterialAlertDialogBuilder(this);
        if (citaExistente != null) {
            dialogo.setTitle("¿Cambiar tu cita?")
                    .setMessage(motivo + "\n\nAntes: " + textoFechaHora(citaExistente.fechaHora()) +
                            "\nAhora: " + cuando)
                    .setPositiveButton("Cambiar", (d, w) -> guardar(hora));
        } else {
            dialogo.setTitle("¿Confirmar cita?")
                    .setMessage(motivo + "\n\n" + FechasUtil.capitalizar(cuando))
                    .setPositiveButton("Confirmar", (d, w) -> guardar(hora));
        }
        dialogoConfirmar = dialogo.setNegativeButton("Cancelar", null).show();
    }

    private void guardar(String hora) {
        String fecha = fechaSeleccionada.toString();
        // se comprueba otra vez por si la franja ha caducado o se ha ocupado con el diálogo abierto
        boolean pasada = !fechaSeleccionada.atTime(LocalTime.parse(hora)).isAfter(LocalDateTime.now());
        if (pasada || db.getHorasOcupadas(fecha).contains(hora)) {
            Toast.makeText(this, "Esa hora ya no está libre", Toast.LENGTH_SHORT).show();
            actualizarCitaExistente();
            cargarFranjas();
            return;
        }
        actualizarCitaExistente();
        long idCita;
        if (citaExistente != null) {
            idCita = db.cambiarCita(citaExistente.id, fecha, hora) ? citaExistente.id : -1;
        } else {
            idCita = db.insertarCita(correoUsuario, idProcedimientoSeleccionado(), fecha, hora);
        }
        if (idCita == -1) {
            Toast.makeText(this, "No se ha podido guardar la cita", Toast.LENGTH_SHORT).show();
            return;
        }
        startActivity(new Intent(this, Justificante.class)
                .putExtra("idCita", (int) idCita)
                .putExtra("CORREO", correoUsuario)
                .putExtra("citaCambiada", citaExistente != null));
        finish(); // al volver desde el justificante no se vuelve a la rejilla
    }

    // "jueves 22 de octubre a las 10:30"
    private static String textoFechaHora(LocalDateTime fechaHora) {
        return FechasUtil.diaLargo(fechaHora) + " a las " + FechasUtil.hora(fechaHora);
    }

    @Override
    protected void onDestroy() {
        // al rotar, cerrarlo evita que la ventana se quede colgada (WindowLeaked)
        if (dialogoConfirmar != null) dialogoConfirmar.dismiss();
        super.onDestroy();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
