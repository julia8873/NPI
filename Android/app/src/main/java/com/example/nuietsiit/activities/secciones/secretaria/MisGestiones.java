package com.example.nuietsiit.activities.secciones.secretaria;

import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.HapticFeedbackConstants;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.example.nuietsiit.R;
import com.example.nuietsiit.data.DBHelper;
import com.example.nuietsiit.data.SecretariaDB;
import com.example.nuietsiit.data.TablasActividades;
import com.example.nuietsiit.data.modelo.Cita;
import com.example.nuietsiit.data.modelo.Solicitud;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import java.util.ArrayList;
import java.util.List;

/**
 * Mis citas y solicitudes con su estado. Pestañas "Citas" y "Solicitudes".
 * Citas: deslizar a la izquierda = cancelar (con "Deshacer"). Tocar cualquiera = justificante.
 */
public class MisGestiones extends AppCompatActivity {

    private static final int PAGINA_CITAS = 0;
    private static final int PAGINA_SOLICITUDES = 1;

    private SecretariaDB db;
    private String correoUsuario;

    private final View[] paginas = new View[2]; // se rellenan al crear cada página (PaginasAdapter)
    private GestionAdapter citasAdapter, solicitudesAdapter;
    private ItemTouchHelper deslizarParaCancelar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.mis_gestiones);

        MaterialToolbar barra = findViewById(R.id.toolbar);
        setSupportActionBar(barra);
        getSupportActionBar().setTitle("Mis gestiones");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        db = new SecretariaDB(DBHelper.getInstance(this));
        correoUsuario = getIntent().getStringExtra("CORREO");
        if (correoUsuario == null || correoUsuario.isEmpty()) correoUsuario = TablasActividades.CORREO_INVITADO;

        citasAdapter = new GestionAdapter(this::abrirJustificante);
        solicitudesAdapter = new GestionAdapter(this::abrirJustificante);
        deslizarParaCancelar = new ItemTouchHelper(new DeslizarParaCancelar());

        ViewPager2 vp = findViewById(R.id.vpGestiones);
        vp.setAdapter(new PaginasAdapter());
        vp.setOffscreenPageLimit(1);
        // el deslizamiento horizontal se reserva para cancelar citas; las pestañas se cambian tocándolas
        vp.setUserInputEnabled(false);
        TabLayout tabs = findViewById(R.id.tabs);
        new TabLayoutMediator(tabs, vp,
                (tab, pos) -> tab.setText(pos == PAGINA_CITAS ? "Citas" : "Solicitudes")).attach();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // al volver (p. ej. del justificante) se recalculan estados y citas pasadas
        cargarCitas();
        cargarSolicitudes();
    }

    private View crearPagina(ViewGroup parent, int pagina) {
        View vista = LayoutInflater.from(this).inflate(R.layout.pagina_gestiones, parent, false);
        RecyclerView rv = vista.findViewById(R.id.rvGestiones);
        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.setAdapter(pagina == PAGINA_CITAS ? citasAdapter : solicitudesAdapter);
        if (pagina == PAGINA_CITAS) deslizarParaCancelar.attachToRecyclerView(rv);
        paginas[pagina] = vista;
        actualizarVacio(pagina);
        return vista;
    }

    private void cargarCitas() {
        List<GestionAdapter.Gestion> lista = new ArrayList<>();
        for (Cita c : db.getCitas(correoUsuario)) lista.add(GestionAdapter.Gestion.deCita(c));
        citasAdapter.setGestiones(lista);
        actualizarVacio(PAGINA_CITAS);
    }

    private void cargarSolicitudes() {
        List<GestionAdapter.Gestion> lista = new ArrayList<>();
        for (Solicitud s : db.getSolicitudes(correoUsuario)) lista.add(GestionAdapter.Gestion.deSolicitud(s));
        solicitudesAdapter.setGestiones(lista);
        actualizarVacio(PAGINA_SOLICITUDES);
    }

    private void actualizarVacio(int pagina) {
        if (paginas[pagina] == null) return; // la página aún no se ha creado
        boolean vacia = (pagina == PAGINA_CITAS ? citasAdapter : solicitudesAdapter).getItemCount() == 0;
        TextView tvVacio = paginas[pagina].findViewById(R.id.tvVacio);
        tvVacio.setText(pagina == PAGINA_CITAS
                ? "No tienes citas.\nPídela con «📅 Pedir cita» en Secretaría."
                : "Todavía no has enviado ninguna solicitud.");
        tvVacio.setVisibility(vacia ? View.VISIBLE : View.GONE);
    }

    private void abrirJustificante(GestionAdapter.Gestion g) {
        startActivity(new Intent(this, Justificante.class)
                .putExtra(g.esCita ? "idCita" : "idSolicitud", g.id)
                .putExtra("CORREO", correoUsuario));
    }

    private void cancelarCita(int posicion, View vista) {
        GestionAdapter.Gestion g = citasAdapter.get(posicion);
        if (!db.cancelarCita(g.id)) {
            citasAdapter.notifyItemChanged(posicion); // vuelve a su sitio
            return;
        }
        vista.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
        citasAdapter.quitar(posicion);
        actualizarVacio(PAGINA_CITAS);
        Snackbar.make(findViewById(R.id.vpGestiones),
                        "Cita " + g.codigo + " cancelada. Si la añadiste a tu calendario, bórrala también.",
                        Snackbar.LENGTH_LONG)
                .setAction("Deshacer", v -> deshacerCancelacion(g.id))
                .show();
    }

    // mismas comprobaciones que al pedir cita: futura, franja libre y sin otra cita para ese motivo
    private void deshacerCancelacion(int idCita) {
        Cita cita = db.getCita(idCita);
        boolean recuperable = cita != null && cita.esFutura()
                && !db.getHorasOcupadas(cita.fecha).contains(cita.hora)
                && db.getCitaActiva(correoUsuario, cita.idProcedimiento) == null;
        if (!recuperable || !db.reactivarCita(idCita)) {
            Toast.makeText(this, "Ya no se puede recuperar la cita", Toast.LENGTH_SHORT).show();
            return;
        }
        cargarCitas();
    }

    /** Deslizar una cita activa y futura a la izquierda la cancela; fondo rojo con 🗑 debajo. */
    private class DeslizarParaCancelar extends ItemTouchHelper.SimpleCallback {
        private final Paint fondo = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint texto = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final float densidad = getResources().getDisplayMetrics().density;

        DeslizarParaCancelar() {
            super(0, ItemTouchHelper.LEFT);
            fondo.setColor(0xFFD32F2F);
            texto.setColor(0xFFFFFFFF);
            texto.setTextSize(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 16, getResources().getDisplayMetrics()));
            texto.setFakeBoldText(true);
            texto.setTextAlign(Paint.Align.RIGHT);
        }

        @Override
        public int getSwipeDirs(@NonNull RecyclerView rv, @NonNull RecyclerView.ViewHolder vh) {
            int pos = vh.getAdapterPosition();
            if (pos == RecyclerView.NO_POSITION || !citasAdapter.get(pos).cancelable) return 0;
            return super.getSwipeDirs(rv, vh);
        }

        @Override
        public boolean onMove(@NonNull RecyclerView rv, @NonNull RecyclerView.ViewHolder vh,
                              @NonNull RecyclerView.ViewHolder destino) {
            return false;
        }

        @Override
        public void onSwiped(@NonNull RecyclerView.ViewHolder vh, int direccion) {
            int pos = vh.getAdapterPosition();
            if (pos != RecyclerView.NO_POSITION) cancelarCita(pos, vh.itemView);
        }

        @Override
        public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView rv, @NonNull RecyclerView.ViewHolder vh,
                                float dX, float dY, int estado, boolean activo) {
            if (estado == ItemTouchHelper.ACTION_STATE_SWIPE && dX < 0) {
                View v = vh.itemView;
                float radio = 20 * densidad; // mismo radio que la tarjeta
                c.drawRoundRect(new RectF(v.getLeft(), v.getTop(), v.getRight(), v.getBottom()), radio, radio, fondo);
                float y = (v.getTop() + v.getBottom()) / 2f - (texto.descent() + texto.ascent()) / 2f;
                c.drawText("🗑 Cancelar", v.getRight() - 24 * densidad, y, texto);
            }
            super.onChildDraw(c, rv, vh, dX, dY, estado, activo);
        }
    }

    /** Página 0 = citas, 1 = solicitudes; cada posición tiene su propio tipo de vista. */
    private class PaginasAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        @Override
        public int getItemViewType(int position) {
            return position;
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new RecyclerView.ViewHolder(crearPagina(parent, viewType)) {};
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        }

        @Override
        public int getItemCount() {
            return paginas.length;
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
