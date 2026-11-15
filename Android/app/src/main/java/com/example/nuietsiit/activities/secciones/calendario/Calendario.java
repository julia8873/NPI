package com.example.nuietsiit.activities.secciones.calendario;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.HapticFeedbackConstants;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.nuietsiit.R;
import com.example.nuietsiit.activities.BaseActivity;
import com.example.nuietsiit.activities.auth.Login;
import com.example.nuietsiit.activities.main.AjustesCuenta;
import com.example.nuietsiit.activities.secciones.Actividades;
import com.example.nuietsiit.activities.secciones.chat.ChatActivity;
import com.example.nuietsiit.data.ActividadesDB;
import com.example.nuietsiit.data.CalendarioDB;
import com.example.nuietsiit.data.DBHelper;
import com.example.nuietsiit.data.SecretariaDB;
import com.example.nuietsiit.data.modelo.EventoCalendario;
import com.example.nuietsiit.util.tutorial.Tutorial;
import com.google.android.material.card.MaterialCardView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class Calendario extends BaseActivity {

    private CalendarGridView calendarView;
    private TextView tvFechaSeleccionadaHeader;
    private View tvSinEventos;
    private RecyclerView rvEventosCalendario;
    private View btnAnadirEventoRapido;

    private DBHelper BD;
    private CalendarioDB calendarioDB;
    private ActividadesDB actividadesDB;
    private SecretariaDB secretariaDB;

    private String correoUsuario;
    private String fechaSeleccionadaStr; // YYYY-MM-DD

    private EventosAdapter adapter;
    private final List<EventoCalendario> listaEventos = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.calendario);

        BD = DBHelper.getInstance(this);
        calendarioDB = new CalendarioDB(BD);
        actividadesDB = new ActividadesDB(BD);
        secretariaDB = new SecretariaDB(BD);

        SharedPreferences prefs = getSharedPreferences("SesionUsuario", MODE_PRIVATE);
        correoUsuario = prefs.getString("CORREO", null);
        if (getIntent() != null && getIntent().hasExtra("CORREO")) {
            correoUsuario = getIntent().getStringExtra("CORREO");
        }

        aplicarMargenesSistema();

        View btnVolver = findViewById(R.id.btnVolver);
        if (btnVolver != null) {
            btnVolver.setOnClickListener(v -> {
                v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
                finish();
            });
        }

        View cardMascotaAyudante = findViewById(R.id.cardMascotaAyudante);
        if (cardMascotaAyudante != null) {
            cardMascotaAyudante.setOnClickListener(v -> {
                v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
                Intent chatIntent = new Intent(this, ChatActivity.class);
                if (correoUsuario != null) chatIntent.putExtra("CORREO", correoUsuario);
                startActivity(chatIntent);
            });
        }

        ImageView ivIcono = findViewById(R.id.ivIcono);
        if (ivIcono != null) {
            ivIcono.setOnClickListener(v -> {
                v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
                if (correoUsuario == null || correoUsuario.isEmpty()) {
                    Intent intent = new Intent(Calendario.this, Login.class);
                    startActivity(intent);
                } else {
                    Intent intent = new Intent(Calendario.this, AjustesCuenta.class);
                    intent.putExtra("CORREO", correoUsuario);
                    startActivity(intent);
                }
            });
        }

        calendarView = findViewById(R.id.calendarView);
        tvFechaSeleccionadaHeader = findViewById(R.id.tvFechaSeleccionadaHeader);
        tvSinEventos = findViewById(R.id.tvSinEventos);
        rvEventosCalendario = findViewById(R.id.rvEventosCalendario);
        btnAnadirEventoRapido = findViewById(R.id.btnAnadirEventoRapido);

        rvEventosCalendario.setLayoutManager(new LinearLayoutManager(this));
        adapter = new EventosAdapter(listaEventos, correoUsuario, this::eliminarEventoPersonal);
        rvEventosCalendario.setAdapter(adapter);

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.ROOT);
        fechaSeleccionadaStr = sdf.format(new Date());

        actualizarHeaderFecha(new Date());
        cargarEventosFecha(fechaSeleccionadaStr);

        if (calendarView != null) {
            calendarView.setOnDateSelectedListener(fechaYYYYMMDD -> {
                fechaSeleccionadaStr = fechaYYYYMMDD;
                try {
                    Date date = sdf.parse(fechaYYYYMMDD);
                    if (date != null) {
                        actualizarHeaderFecha(date);
                    }
                } catch (Exception ignored) {}
                cargarEventosFecha(fechaSeleccionadaStr);
            });
        }

        actualizarFechasResaltadas();

        btnAnadirEventoRapido.setOnClickListener(v -> {
            v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
            mostrarDialogoAnadirEvento();
        });
    }

    private void actualizarFechasResaltadas() {
        if (calendarView != null && calendarioDB != null) {
            Set<String> fechas = calendarioDB.obtenerTodasLasFechasConEventos(correoUsuario);
            calendarView.setFechasConEventos(fechas);
        }
    }

    private void actualizarHeaderFecha(Date date) {
        Locale localeActivo = getResources().getConfiguration().getLocales().get(0);
        SimpleDateFormat sdfFormat = new SimpleDateFormat(getString(R.string.cal_patron_fecha_header), localeActivo);
        String fechaLegible = sdfFormat.format(date);
        if (!fechaLegible.isEmpty()) {
            fechaLegible = fechaLegible.substring(0, 1).toUpperCase(localeActivo) + fechaLegible.substring(1);
        }
        tvFechaSeleccionadaHeader.setText(getString(R.string.cal_header_eventos_fmt, fechaLegible));
    }

    private void cargarEventosFecha(String fechaStr) {
        listaEventos.clear();

        List<EventoCalendario> personales = calendarioDB.obtenerEventosPorFecha(fechaStr, correoUsuario);
        List<EventoCalendario> actividades = actividadesDB.obtenerActividadesPorFecha(fechaStr, correoUsuario);
        List<EventoCalendario> citas = secretariaDB.obtenerCitasPorFecha(fechaStr, correoUsuario);

        listaEventos.addAll(personales);
        listaEventos.addAll(actividades);
        listaEventos.addAll(citas);

        if (listaEventos.isEmpty()) {
            tvSinEventos.setVisibility(View.VISIBLE);
            tvSinEventos.setAlpha(0f);
            tvSinEventos.animate().alpha(1f).setDuration(180).start();
            rvEventosCalendario.setVisibility(View.GONE);
        } else {
            tvSinEventos.setVisibility(View.GONE);
            rvEventosCalendario.setVisibility(View.VISIBLE);
            rvEventosCalendario.setAlpha(0f);
            rvEventosCalendario.animate().alpha(1f).setDuration(180).start();
        }
        adapter.notifyDataSetChanged();
    }

    private void eliminarEventoPersonal(EventoCalendario evento) {
        if (evento.id > 0) {
            boolean ok = calendarioDB.eliminarEvento(evento.id);
            if (ok) {
                Toast.makeText(this, R.string.cal_toast_eliminado, Toast.LENGTH_SHORT).show();
                cargarEventosFecha(fechaSeleccionadaStr);
                actualizarFechasResaltadas();
            }
        }
    }

    private void mostrarDialogoAnadirEvento() {
        if (correoUsuario == null || correoUsuario.isEmpty()) {
            new AlertDialog.Builder(this)
                    .setTitle(R.string.cal_no_registrado_titulo)
                    .setMessage(R.string.cal_no_registrado_mensaje)
                    .setPositiveButton(R.string.cal_btn_aceptar, null)
                    .show();
            return;
        }

        View view = LayoutInflater.from(this).inflate(R.layout.dialog_anadir_evento, null);
        EditText etTitulo = view.findViewById(R.id.etTituloEvento);
        Spinner spTipo = view.findViewById(R.id.spTipoEvento);
        EditText etHora = view.findViewById(R.id.etHoraEvento);
        EditText etDescripcion = view.findViewById(R.id.etDescripcionEvento);

        new AlertDialog.Builder(this)
                .setTitle(R.string.cal_dialog_anadir_titulo)
                .setView(view)
                .setPositiveButton(R.string.cal_btn_guardar, (dialog, which) -> {
                    String titulo = etTitulo.getText().toString().trim();
                    String tipoSeleccionado = spTipo.getSelectedItem() != null ? spTipo.getSelectedItem().toString() : "";
                    String tipoBd = normalizarTipoParaBd(tipoSeleccionado);
                    String hora = etHora.getText().toString().trim();
                    String desc = etDescripcion.getText().toString().trim();

                    if (titulo.isEmpty()) {
                        Toast.makeText(this, R.string.cal_toast_titulo_vacio, Toast.LENGTH_SHORT).show();
                        return;
                    }

                    calendarioDB.insertarEvento(correoUsuario, titulo, tipoBd, fechaSeleccionadaStr, hora, desc);
                    Toast.makeText(this, R.string.cal_toast_anotado, Toast.LENGTH_LONG).show();
                    cargarEventosFecha(fechaSeleccionadaStr);
                    actualizarFechasResaltadas();
                })
                .setNegativeButton(R.string.cal_btn_cancelar, null)
                .show();
    }

    private String normalizarTipoParaBd(String textoSpinner) {
        String lower = textoSpinner.toLowerCase(Locale.ROOT);
        if (lower.contains("exam") || lower.contains("examen")) return "Examen";
        if (lower.contains("assign") || lower.contains("entrega")) return "Entrega";
        if (lower.contains("appoint") || lower.contains("cita")) return "Cita";
        return textoSpinner.isEmpty() ? "Examen" : textoSpinner;
    }

    private static class EventosAdapter extends RecyclerView.Adapter<EventosAdapter.EventoViewHolder> {

        private final List<EventoCalendario> eventos;
        private final String correoActual;
        private final OnEliminarListener eliminarListener;

        public interface OnEliminarListener {
            void onEliminar(EventoCalendario evento);
        }

        public EventosAdapter(List<EventoCalendario> eventos, String correoActual, OnEliminarListener eliminarListener) {
            this.eventos = eventos;
            this.correoActual = correoActual;
            this.eliminarListener = eliminarListener;
        }

        @NonNull
        @Override
        public EventoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_evento_calendario, parent, false);
            return new EventoViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull EventoViewHolder holder, int position) {
            Context ctx = holder.itemView.getContext();
            EventoCalendario ev = eventos.get(position);
            holder.tvTitulo.setText(ev.titulo);
            holder.tvHora.setText(ev.hora != null && !ev.hora.isEmpty() ? ev.hora : ctx.getString(R.string.cal_todo_el_dia));
            holder.tvDescripcion.setText(ev.descripcion != null && !ev.descripcion.isEmpty() ? ev.descripcion : ctx.getString(R.string.cal_sin_descripcion));

            String tipoLower = ev.tipo != null ? ev.tipo.toLowerCase(Locale.ROOT) : "";
            int colorAcento;

            if (tipoLower.contains("examen") || tipoLower.contains("exam")) {
                holder.tvTipo.setText(R.string.cal_tipo_examen);
                holder.cardTipoBadge.setCardBackgroundColor(Color.parseColor("#FEE2E2"));
                colorAcento = Color.parseColor("#DC2626");
            } else if (tipoLower.contains("entrega") || tipoLower.contains("assign")) {
                holder.tvTipo.setText(R.string.cal_tipo_entrega);
                holder.cardTipoBadge.setCardBackgroundColor(Color.parseColor("#FFEDD5"));
                colorAcento = Color.parseColor("#EA580C");
            } else if (tipoLower.contains("cita") || tipoLower.contains("appoint")) {
                holder.tvTipo.setText(R.string.cal_tipo_cita);
                holder.cardTipoBadge.setCardBackgroundColor(Color.parseColor("#F3E8FF"));
                colorAcento = Color.parseColor("#9333EA");
            } else {
                holder.tvTipo.setText(ev.tipo != null && !ev.tipo.isEmpty() ? ev.tipo : ctx.getString(R.string.cal_tipo_evento));
                holder.cardTipoBadge.setCardBackgroundColor(Color.parseColor("#E0F2FE"));
                colorAcento = Color.parseColor("#0284C7");
            }

            holder.tvTipo.setTextColor(colorAcento);
            if (holder.viewIndicadorLateral != null) {
                holder.viewIndicadorLateral.setBackgroundColor(colorAcento);
            }

            if (ev.id > 0 && correoActual != null && correoActual.equalsIgnoreCase(ev.correoUsuario)) {
                holder.btnEliminar.setVisibility(View.VISIBLE);
                holder.btnEliminar.setOnClickListener(v -> {
                    v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
                    eliminarListener.onEliminar(ev);
                });
            } else {
                holder.btnEliminar.setVisibility(View.GONE);
            }

            if (ev.id < 0 || tipoLower.contains("actividad")) {
                holder.itemView.setOnClickListener(v -> {
                    Intent intent = new Intent(v.getContext(), Actividades.class);
                    v.getContext().startActivity(intent);
                });
            } else {
                holder.itemView.setOnClickListener(null);
            }
        }

        @Override
        public int getItemCount() {
            return eventos.size();
        }

        static class EventoViewHolder extends RecyclerView.ViewHolder {
            TextView tvTitulo, tvHora, tvDescripcion, tvTipo;
            MaterialCardView cardTipoBadge;
            ImageView btnEliminar;
            View viewIndicadorLateral;

            public EventoViewHolder(@NonNull View itemView) {
                super(itemView);
                tvTitulo = itemView.findViewById(R.id.tvTituloEvento);
                tvHora = itemView.findViewById(R.id.tvHoraEvento);
                tvDescripcion = itemView.findViewById(R.id.tvDescripcionEvento);
                tvTipo = itemView.findViewById(R.id.tvTipoEvento);
                cardTipoBadge = itemView.findViewById(R.id.cardTipoBadge);
                btnEliminar = itemView.findViewById(R.id.btnEliminarEvento);
                viewIndicadorLateral = itemView.findViewById(R.id.viewIndicadorLateral);
            }
        }
    }

    private void aplicarMargenesSistema() {
        View root = findViewById(R.id.rootCalendario);
        if (root != null) {
            ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
                Insets statusBarInsets = insets.getInsets(WindowInsetsCompat.Type.statusBars());
                v.setPadding(v.getPaddingLeft(), statusBarInsets.top, v.getPaddingRight(), v.getPaddingBottom());
                return insets;
            });
        }
    }

    // -------------------------------------------------
    //              TUTORIAL
    // -------------------------------------------------

    /** Lo enseña BaseActivity la primera vez; el botón "?" de la barra lo repite. */
    @Override
    protected Tutorial crearTutorial() {
        return new Tutorial(this, "calendario", 1)
                .info(R.id.calendarView, getString(R.string.tut_cal_grid))
                .info(R.id.tvFechaSeleccionadaHeader, getString(R.string.tut_cal_eventos))
                .tocar(R.id.btnAnadirEventoRapido, getString(R.string.tut_cal_anadir));
    }
}