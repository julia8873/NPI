package com.example.nuietsiit.activities.secciones.secretaria;

import android.content.Intent;
import android.os.Bundle;
import android.view.HapticFeedbackConstants;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.nuietsiit.R;
import com.example.nuietsiit.data.DBHelper;
import com.example.nuietsiit.data.SecretariaDB;
import com.example.nuietsiit.data.modelo.Cita;
import com.example.nuietsiit.data.modelo.Tramite;
import com.example.nuietsiit.ui.InclinarParaPasar;
import com.example.nuietsiit.ui.PestanasPapel;
import com.example.nuietsiit.util.FechasUtil;
import com.example.nuietsiit.util.SesionUtil;
import com.example.nuietsiit.util.tutorial.Gesto;
import com.example.nuietsiit.util.tutorial.Tutorial;
import com.google.android.material.appbar.MaterialToolbar;
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
 * Cita previa en la secretaría: motivo → día (próximos 10 laborables) → hora → franja de 10 min → confirmar.
 * Sin scroll: cada hora es una pestaña con sus seis franjas; se cambia de hora tocando la pestaña,
 * deslizando las franjas de lado o inclinando el móvil (como los pasos de un trámite).
 * Máximo una cita activa por motivo: si ya hay, elegir otra franja la cambia.
 */
public class CitaPrevia extends PantallaSecretaria {

    private static final String ESTADO_MOTIVO = "motivo";
    private static final String ESTADO_FECHA = "fecha";
    private static final String ESTADO_HORA = "hora";
    private static final Locale ES = Locale.forLanguageTag("es-ES");

    private static final int NUM_DIAS = 10;
    private static final int DIAS_POR_FILA = 5;
    private static final LocalTime PRIMERA_FRANJA = LocalTime.of(9, 0);
    private static final LocalTime ULTIMA_FRANJA = LocalTime.of(13, 50);
    private static final int MINUTOS_FRANJA = 10;
    private static final int UMBRAL_DESLIZAR_DP = 60; // cuánto hay que deslizar de lado para cambiar de hora
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
    private LinearLayout chipsDias; // filas de hojas de taco
    private FranjaAdapter adaptador;
    private PestanasPapel pestanasHoras;
    private RecyclerView rvFranjas;
    private InclinarParaPasar inclinar;
    private final List<FranjaAdapter.Franja> franjasDia = new ArrayList<>();
    private int horaElegida = -1; // -1 = la elige cargarFranjas (la de tu cita o la primera con huecos)
    private float xInicio, yInicio;
    private boolean deslizando;

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
        chipsDias = findViewById(R.id.llDias);

        adaptador = new FranjaAdapter(this::confirmar);
        rvFranjas = findViewById(R.id.rvFranjas);
        rvFranjas.setLayoutManager(new GridLayoutManager(this, 3)); // seis franjas: dos filas de tres
        rvFranjas.setAdapter(adaptador);
        pestanasHoras = findViewById(R.id.pestanasHoras);
        pestanasHoras.setOnElegir(clave -> mostrarHora((Integer) clave));
        configurarDeslizarHoras();
        inclinar = new InclinarParaPasar(rvFranjas, this::pasarHora, () -> true);

        tramites.addAll(db.getTramites(null));
        List<LocalDate> dias = proximosLaborables();
        if (savedInstanceState != null) {
            motivoSeleccionado = savedInstanceState.getInt(ESTADO_MOTIVO);
            fechaSeleccionada = LocalDate.parse(savedInstanceState.getString(ESTADO_FECHA));
            horaElegida = savedInstanceState.getInt(ESTADO_HORA, -1);
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
    protected Tutorial crearTutorial() {
        return new Tutorial(this, "cita_previa", 2)
                .tocar(R.id.actvMotivo, "Toca aquí para elegir para qué es la cita")
                .info(R.id.tvCitaExistente, "Ya tienes una cita para esto: si eliges otra franja, la cambias").soloSiSeVe()
                .tocar(R.id.llDias, "Toca el día que te venga bien")
                .tocar(R.id.pestanasHoras, "Toca una hora para ver sus franjas de 10 minutos")
                .gesto(R.id.rvFranjas, Gesto.DESLIZAR_IZQ, "También puedes deslizar las franjas de lado para cambiar de hora")
                .sensor(Gesto.INCLINAR, "O inclinar el móvil a un lado o al otro para cambiar de hora")
                .soloSi(() -> inclinar.escuchando())
                .tocar(R.id.rvFranjas, "Toca una franja libre para pedir la cita. Antes de guardarla te pediremos confirmación");
    }

    @Override
    protected void onSaveInstanceState(Bundle salida) {
        super.onSaveInstanceState(salida);
        salida.putInt(ESTADO_MOTIVO, motivoSeleccionado);
        salida.putString(ESTADO_FECHA, fechaSeleccionada.toString());
        salida.putInt(ESTADO_HORA, horaElegida);
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
            horaElegida = -1;
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

    // hojas de taco de calendario, cinco por fila (una semana laborable): nada queda cortado
    private void crearChipsDias(List<LocalDate> dias) {
        LinearLayout fila = null;
        int hueco = getResources().getDimensionPixelSize(R.dimen.nui_espacio_xs);
        for (int i = 0; i < dias.size(); i++) {
            if (i % DIAS_POR_FILA == 0) {
                fila = new LinearLayout(this);
                fila.setLayoutParams(new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                fila.setPadding(0, 0, 0, hueco);
                chipsDias.addView(fila);
            }
            LocalDate dia = dias.get(i);
            View hoja = LayoutInflater.from(this).inflate(R.layout.item_dia_taco, fila, false);
            ((LinearLayout.LayoutParams) hoja.getLayoutParams()).setMargins(hueco / 2, 0, hueco / 2, 0);
            long faltan = FechasUtil.diasHasta(dia);
            ((TextView) hoja.findViewById(R.id.tvSemana)).setText(faltan == 0 ? "Hoy"
                    : dia.getDayOfWeek().getDisplayName(TextStyle.SHORT, ES).replace(".", ""));
            ((TextView) hoja.findViewById(R.id.tvNumero)).setText(String.valueOf(dia.getDayOfMonth()));
            ((TextView) hoja.findViewById(R.id.tvMes)).setText(
                    dia.getMonth().getDisplayName(TextStyle.SHORT, ES).replace(".", ""));
            hoja.setTag(dia);
            hoja.setContentDescription(etiquetaDia(dia) + ", " + FechasUtil.diaLargo(dia.atStartOfDay()));
            hoja.setSelected(dia.equals(fechaSeleccionada));
            hoja.setOnClickListener(v -> elegirDia(v));
            fila.addView(hoja);
        }
    }

    private void elegirDia(View hoja) {
        if (hoja.isSelected()) return;
        for (int f = 0; f < chipsDias.getChildCount(); f++) {
            ViewGroup fila = (ViewGroup) chipsDias.getChildAt(f);
            for (int i = 0; i < fila.getChildCount(); i++) fila.getChildAt(i).setSelected(false);
        }
        hoja.setSelected(true);
        hoja.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);
        fechaSeleccionada = (LocalDate) hoja.getTag();
        horaElegida = -1;
        cargarFranjas();
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

        franjasDia.clear();
        for (LocalTime t = PRIMERA_FRANJA; !t.isAfter(ULTIMA_FRANJA); t = t.plusMinutes(MINUTOS_FRANJA)) {
            String hora = t.toString(); // "HH:mm" (sin segundos porque valen 0)
            boolean ocupadaRelleno = relleno.nextFloat() < PROB_OCUPADA; // se saca siempre para no desordenar la serie
            boolean pasada = !fechaSeleccionada.atTime(t).isAfter(ahora);
            int estado;
            if (hora.equals(horaMia)) estado = FranjaAdapter.TUYA;
            else if (pasada || ocupadaRelleno || ocupadasBD.contains(hora)) estado = FranjaAdapter.OCUPADA;
            else estado = FranjaAdapter.LIBRE;
            franjasDia.add(new FranjaAdapter.Franja(hora, estado));
        }

        // al cambiar de día o de motivo se abre la hora de tu cita o, si no, la primera con huecos
        if (horaElegida < PRIMERA_FRANJA.getHour() || horaElegida > ULTIMA_FRANJA.getHour()) {
            horaElegida = -1;
            for (FranjaAdapter.Franja f : franjasDia) {
                if (f.estado == FranjaAdapter.TUYA) horaElegida = horaDe(f);
            }
            for (FranjaAdapter.Franja f : franjasDia) {
                if (horaElegida == -1 && f.estado == FranjaAdapter.LIBRE) horaElegida = horaDe(f);
            }
            if (horaElegida == -1) horaElegida = PRIMERA_FRANJA.getHour();
        }
        crearPestanasHoras();
        mostrarHora(horaElegida);
    }

    private static int horaDe(FranjaAdapter.Franja f) {
        return LocalTime.parse(f.hora).getHour();
    }

    // "9:00 · 4 libres": así se ve de un vistazo qué hora merece la pena abrir
    private void crearPestanasHoras() {
        pestanasHoras.vaciar();
        pestanasHoras.nuevaFila();
        for (int h = PRIMERA_FRANJA.getHour(); h <= ULTIMA_FRANJA.getHour(); h++) {
            int libres = 0;
            boolean tuya = false;
            for (FranjaAdapter.Franja f : franjasDia) {
                if (horaDe(f) != h) continue;
                if (f.estado == FranjaAdapter.LIBRE) libres++;
                if (f.estado == FranjaAdapter.TUYA) tuya = true;
            }
            String detalle = tuya ? "tu cita" : libres == 0 ? "llena" : libres == 1 ? "1 libre" : libres + " libres";
            pestanasHoras.anadir(h, h + ":00", detalle, 0, 0);
        }
        pestanasHoras.marcar(horaElegida);
    }

    private void mostrarHora(int hora) {
        horaElegida = hora;
        pestanasHoras.marcar(hora);
        List<FranjaAdapter.Franja> deLaHora = new ArrayList<>();
        for (FranjaAdapter.Franja f : franjasDia) {
            if (horaDe(f) == hora) deLaHora.add(f);
        }
        adaptador.setFranjas(deLaHora);
    }

    /** Hora siguiente (+1) o anterior (-1); las franjas salen por un lado y entran por el otro. */
    private void pasarHora(int sentido) {
        int nueva = horaElegida + sentido;
        if (nueva < PRIMERA_FRANJA.getHour() || nueva > ULTIMA_FRANJA.getHour()) {
            rvFranjas.animate().translationX(0).setDuration(150).start(); // tope: vuelve a su sitio
            return;
        }
        rvFranjas.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);
        float ancho = rvFranjas.getWidth();
        rvFranjas.animate().translationX(-sentido * ancho).alpha(0f).setDuration(140).withEndAction(() -> {
            mostrarHora(nueva);
            rvFranjas.setTranslationX(sentido * ancho * 0.4f);
            rvFranjas.animate().translationX(0).alpha(1f).setDuration(180).start();
        }).start();
    }

    // deslizar de lado sobre las franjas cambia de hora; un toque sigue eligiendo la franja
    private void configurarDeslizarHoras() {
        int umbralToque = ViewConfiguration.get(this).getScaledTouchSlop();
        float umbral = UMBRAL_DESLIZAR_DP * getResources().getDisplayMetrics().density;
        rvFranjas.addOnItemTouchListener(new RecyclerView.SimpleOnItemTouchListener() {
            @Override
            public boolean onInterceptTouchEvent(@NonNull RecyclerView rv, @NonNull MotionEvent e) {
                if (e.getActionMasked() == MotionEvent.ACTION_DOWN) {
                    xInicio = e.getX();
                    yInicio = e.getY();
                    deslizando = false;
                } else if (e.getActionMasked() == MotionEvent.ACTION_MOVE) {
                    float dx = Math.abs(e.getX() - xInicio), dy = Math.abs(e.getY() - yInicio);
                    deslizando = dx > umbralToque && dx > dy;
                }
                return deslizando;
            }

            @Override
            public void onTouchEvent(@NonNull RecyclerView rv, @NonNull MotionEvent e) {
                float dx = e.getX() - xInicio;
                switch (e.getActionMasked()) {
                    case MotionEvent.ACTION_MOVE:
                        rv.setTranslationX(dx * 0.6f); // las franjas siguen al dedo, con algo de resistencia
                        break;
                    case MotionEvent.ACTION_UP:
                        deslizando = false;
                        if (Math.abs(dx) > umbral) {
                            pasarHora(dx < 0 ? 1 : -1);
                            avisarTutorial(dx < 0 ? Gesto.DESLIZAR_IZQ : Gesto.DESLIZAR_DER);
                        }
                        else rv.animate().translationX(0).setDuration(150).start();
                        break;
                    case MotionEvent.ACTION_CANCEL:
                        deslizando = false;
                        rv.animate().translationX(0).setDuration(150).start();
                        break;
                }
            }
        });
    }

    private void confirmar(String hora) {
        if (dialogoConfirmar != null && dialogoConfirmar.isShowing()) return;
        actualizarCitaExistente();
        String cuando = textoFechaHora(fechaSeleccionada.atTime(LocalTime.parse(hora)));
        String motivo = nombresMotivos.get(motivoSeleccionado);
        MaterialAlertDialogBuilder dialogo = new MaterialAlertDialogBuilder(this, R.style.DialogoSecretaria);
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
