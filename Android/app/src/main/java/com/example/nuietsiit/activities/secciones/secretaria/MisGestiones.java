package com.example.nuietsiit.activities.secciones.secretaria;

import android.content.Intent;
import android.os.Bundle;
import android.view.HapticFeedbackConstants;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.example.nuietsiit.R;
import com.example.nuietsiit.data.DBHelper;
import com.example.nuietsiit.data.SecretariaDB;
import com.example.nuietsiit.data.modelo.Cita;
import com.example.nuietsiit.data.modelo.Solicitud;
import com.example.nuietsiit.ui.InclinarParaPasar;
import com.example.nuietsiit.util.RecienteUtil;
import com.example.nuietsiit.util.SesionUtil;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import java.util.ArrayList;
import java.util.List;

/**
 * Mis citas y solicitudes con su estado. Pestañas "Citas" y "Solicitudes".
 * Citas: rasgar el resguardo por la línea perforada = cancelar (con "Deshacer" y su tiempo). Tocar cualquiera = justificante.
 */
public class MisGestiones extends PantallaSecretaria {

    private static final int PAGINA_CITAS = 0;
    private static final int PAGINA_SOLICITUDES = 1;
    static final int DURACION_DESHACER = 8000; // ms

    private SecretariaDB db;
    private String correoUsuario;

    private final View[] paginas = new View[2]; // se rellenan al crear cada página (PaginasAdapter)
    private GestionAdapter citasAdapter, solicitudesAdapter;
    private InclinarParaPasar inclinar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.mis_gestiones);

        MaterialToolbar barra = findViewById(R.id.toolbar);
        setSupportActionBar(barra);
        getSupportActionBar().setTitle("Mis gestiones");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        db = new SecretariaDB(DBHelper.getInstance(this));
        correoUsuario = SesionUtil.correo(getIntent());
        RecienteUtil.guardar(this, MisGestiones.class, "Mis Gestiones", "Citas y solicitudes", null, null, 5);

        citasAdapter = new GestionAdapter(this::abrirJustificante, this::cancelarCita);
        solicitudesAdapter = new GestionAdapter(this::abrirJustificante, null);

        ViewPager2 vp = findViewById(R.id.vpGestiones);
        vp.setAdapter(new PaginasAdapter());
        vp.setOffscreenPageLimit(1);
        // el arrastre horizontal se reserva para rasgar resguardos; las pestañas se cambian tocándolas
        vp.setUserInputEnabled(false);
        TabLayout tabs = findViewById(R.id.tabs);
        new TabLayoutMediator(tabs, vp,
                (tab, pos) -> tab.setText(pos == PAGINA_CITAS ? "Citas" : "Solicitudes")).attach();
        // como el arrastre es para rasgar, de pestaña también se cambia inclinando el móvil (como los pasos)
        inclinar = new InclinarParaPasar(vp, sentido -> {
            int nueva = vp.getCurrentItem() + sentido;
            if (nueva < 0 || nueva >= paginas.length) return;
            vp.setCurrentItem(nueva, true);
            vp.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);
        }, () -> true);
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
        paginas[pagina] = vista;
        actualizarVacio(pagina);
        return vista;
    }

    private void cargarCitas() {
        List<GestionAdapter.Gestion> lista = new ArrayList<>();
        for (Cita c : db.getCitas(correoUsuario)) lista.add(GestionAdapter.Gestion.deCita(c));
        citasAdapter.setGestiones(lista);
        actualizarVacio(PAGINA_CITAS);
        // la primera vez que hay algo que rasgar, las tijeras enseñan el gesto recorriendo la línea
        if (citasAdapter.hayCancelables() && ResguardoRasgable.primeraVez(this)) citasAdapter.ensenarTijeras();
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
                ? "No tienes citas.\nPídela con «Pedir cita» en Secretaría."
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
        // la lista pudo quedarse abierta hasta pasada la hora de la cita
        Cita cita = db.getCita(g.id);
        if (cita == null || !cita.esFutura()) {
            Toast.makeText(this, "Esta cita ya ha pasado y no se puede cancelar", Toast.LENGTH_SHORT).show();
            cargarCitas();
            return;
        }
        if (!db.cancelarCita(g.id)) {
            citasAdapter.notifyItemChanged(posicion); // el resguardo vuelve entero
            return;
        }
        citasAdapter.quitar(posicion);
        actualizarVacio(PAGINA_CITAS);
        AvisoDeshacer.mostrar(findViewById(R.id.vpGestiones), "Cita cancelada",
                g.codigo + " · Si la añadiste a tu calendario, bórrala también.",
                DURACION_DESHACER, () -> deshacerCancelacion(g.id));
    }

    private void deshacerCancelacion(int idCita) {
        if (!db.recuperarCita(correoUsuario, idCita)) {
            Toast.makeText(this, "Ya no se puede recuperar la cita", Toast.LENGTH_SHORT).show();
            return;
        }
        cargarCitas();
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
