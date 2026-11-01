package com.example.nuietsiit.activities.secciones;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CalendarView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.nuietsiit.R;
import com.example.nuietsiit.activities.BaseActivity;
import com.example.nuietsiit.data.ActividadesDB;
import com.example.nuietsiit.data.CalendarioDB;
import com.example.nuietsiit.data.DBHelper;
import com.example.nuietsiit.data.SecretariaDB;
import com.example.nuietsiit.data.modelo.EventoCalendario;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class Calendario extends BaseActivity {

    private CalendarView calendarView;
    private TextView tvFechaSeleccionadaHeader, tvSinEventos;
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

        MaterialToolbar toolbar = findViewById(R.id.toolbarCalendario);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        calendarView = findViewById(R.id.calendarView);
        tvFechaSeleccionadaHeader = findViewById(R.id.tvFechaSeleccionadaHeader);
        tvSinEventos = findViewById(R.id.tvSinEventos);
        rvEventosCalendario = findViewById(R.id.rvEventosCalendario);
        btnAnadirEventoRapido = findViewById(R.id.btnAnadirEventoRapido);

        rvEventosCalendario.setLayoutManager(new LinearLayoutManager(this));
        adapter = new EventosAdapter(listaEventos, correoUsuario, this::eliminarEventoPersonal);
        rvEventosCalendario.setAdapter(adapter);

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        fechaSeleccionadaStr = sdf.format(new Date());

        actualizarHeaderFecha(new Date());
        cargarEventosFecha(fechaSeleccionadaStr);

        calendarView.setOnDateChangeListener((view, year, month, dayOfMonth) -> {
            Calendar cal = Calendar.getInstance();
            cal.set(year, month, dayOfMonth);
            fechaSeleccionadaStr = sdf.format(cal.getTime());
            actualizarHeaderFecha(cal.getTime());
            cargarEventosFecha(fechaSeleccionadaStr);
        });

        btnAnadirEventoRapido.setOnClickListener(v -> mostrarDialogoAnadirEvento());
    }

    private void actualizarHeaderFecha(Date date) {
        SimpleDateFormat sdfFormat = new SimpleDateFormat("EEEE, d 'de' MMMM", new Locale("es", "ES"));
        String fechaLegible = sdfFormat.format(date);
        if (!fechaLegible.isEmpty()) {
            fechaLegible = fechaLegible.substring(0, 1).toUpperCase() + fechaLegible.substring(1);
        }
        tvFechaSeleccionadaHeader.setText("Eventos: " + fechaLegible);
    }

    private void cargarEventosFecha(String fechaStr) {
        listaEventos.clear();

        List<EventoCalendario> personales = calendarioDB.obtenerEventosPorFecha(fechaStr, correoUsuario);
        List<EventoCalendario> actividades = actividadesDB.obtenerActividadesPorFecha(fechaStr);
        List<EventoCalendario> citas = secretariaDB.obtenerCitasPorFecha(fechaStr, correoUsuario);

        listaEventos.addAll(personales);
        listaEventos.addAll(actividades);
        listaEventos.addAll(citas);

        if (listaEventos.isEmpty()) {
            tvSinEventos.setVisibility(View.VISIBLE);
            rvEventosCalendario.setVisibility(View.GONE);
        } else {
            tvSinEventos.setVisibility(View.GONE);
            rvEventosCalendario.setVisibility(View.VISIBLE);
        }
        adapter.notifyDataSetChanged();
    }

    private void eliminarEventoPersonal(EventoCalendario evento) {
        if (evento.id > 0) {
            boolean ok = calendarioDB.eliminarEvento(evento.id);
            if (ok) {
                Toast.makeText(this, "Evento eliminado", Toast.LENGTH_SHORT).show();
                cargarEventosFecha(fechaSeleccionadaStr);
            }
        }
    }

    private void mostrarDialogoAnadirEvento() {
        if (correoUsuario == null || correoUsuario.isEmpty()) {
            new AlertDialog.Builder(this)
                    .setTitle("Usuario no registrado")
                    .setMessage("Debes iniciar sesión para guardar tus exámenes y entregas personales en tu cuenta.")
                    .setPositiveButton("Aceptar", null)
                    .show();
            return;
        }

        View view = LayoutInflater.from(this).inflate(R.layout.dialog_anadir_evento, null);
        EditText etTitulo = view.findViewById(R.id.etTituloEvento);
        Spinner spTipo = view.findViewById(R.id.spTipoEvento);
        EditText etHora = view.findViewById(R.id.etHoraEvento);
        EditText etDescripcion = view.findViewById(R.id.etDescripcionEvento);

        new AlertDialog.Builder(this)
                .setTitle("Anotar Examen / Entrega")
                .setView(view)
                .setPositiveButton("Guardar", (dialog, which) -> {
                    String titulo = etTitulo.getText().toString().trim();
                    String tipo = spTipo.getSelectedItem() != null ? spTipo.getSelectedItem().toString() : "Examen";
                    String hora = etHora.getText().toString().trim();
                    String desc = etDescripcion.getText().toString().trim();

                    if (titulo.isEmpty()) {
                        Toast.makeText(this, "Introduce un título para el evento", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    calendarioDB.insertarEvento(correoUsuario, titulo, tipo, fechaSeleccionadaStr, hora, desc);
                    Toast.makeText(this, "Evento anotado para el calendario", Toast.LENGTH_SHORT).show();
                    cargarEventosFecha(fechaSeleccionadaStr);
                })
                .setNegativeButton("Cancelar", null)
                .show();
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
            EventoCalendario ev = eventos.get(position);
            holder.tvTitulo.setText(ev.titulo);
            holder.tvHora.setText(ev.hora != null && !ev.hora.isEmpty() ? ev.hora : "Todo el día");
            holder.tvDescripcion.setText(ev.descripcion != null && !ev.descripcion.isEmpty() ? ev.descripcion : "Sin descripción");
            holder.tvTipo.setText(ev.tipo != null ? ev.tipo : "Evento");

            String tipoLower = ev.tipo != null ? ev.tipo.toLowerCase() : "";
            if (tipoLower.contains("examen")) {
                holder.cardTipoBadge.setCardBackgroundColor(Color.parseColor("#FEE2E2"));
                holder.tvTipo.setTextColor(Color.parseColor("#DC2626"));
            } else if (tipoLower.contains("entrega")) {
                holder.cardTipoBadge.setCardBackgroundColor(Color.parseColor("#FFEDD5"));
                holder.tvTipo.setTextColor(Color.parseColor("#EA580C"));
            } else if (tipoLower.contains("cita")) {
                holder.cardTipoBadge.setCardBackgroundColor(Color.parseColor("#F3E8FF"));
                holder.tvTipo.setTextColor(Color.parseColor("#9333EA"));
            } else {
                holder.cardTipoBadge.setCardBackgroundColor(Color.parseColor("#E0F2FE"));
                holder.tvTipo.setTextColor(Color.parseColor("#0284C7"));
            }

            if (ev.id > 0 && correoActual != null && correoActual.equalsIgnoreCase(ev.correoUsuario)) {
                holder.btnEliminar.setVisibility(View.VISIBLE);
                holder.btnEliminar.setOnClickListener(v -> eliminarListener.onEliminar(ev));
            } else {
                holder.btnEliminar.setVisibility(View.GONE);
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

            public EventoViewHolder(@NonNull View itemView) {
                super(itemView);
                tvTitulo = itemView.findViewById(R.id.tvTituloEvento);
                tvHora = itemView.findViewById(R.id.tvHoraEvento);
                tvDescripcion = itemView.findViewById(R.id.tvDescripcionEvento);
                tvTipo = itemView.findViewById(R.id.tvTipoEvento);
                cardTipoBadge = itemView.findViewById(R.id.cardTipoBadge);
                btnEliminar = itemView.findViewById(R.id.btnEliminarEvento);
            }
        }
    }
}