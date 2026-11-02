package com.example.nuietsiit.activities.secciones;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.nuietsiit.R;
import com.example.nuietsiit.activities.BaseActivity;
import com.example.nuietsiit.activities.auth.Login;
import com.example.nuietsiit.activities.secciones.actividades.ActividadAdapter;
import com.example.nuietsiit.activities.secciones.actividades.HojaComunidades;
import com.example.nuietsiit.data.ActividadesDB;
import com.example.nuietsiit.data.DBHelper;
import com.example.nuietsiit.data.modelo.Actividad;
import com.example.nuietsiit.util.CalendarioUtil;
import com.example.nuietsiit.util.FechasUtil;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Pantalla de Actividades: lista de actividades y cursos AbiertaUGR con filtros.
 * - Sin sesión iniciada se pueden ver, pero no apuntarse.
 * - Deslizar una tarjeta a la derecha = apuntarse. A la izquierda = ocultarla (con "Deshacer").
 * - Pulsar una tarjeta = detalle con botones de apuntarse, añadir al calendario y más info.
 */
public class Actividades extends BaseActivity {

    private static final String FILTRO_APUNTADO = "__apuntado"; // chip "Mis actividades"
    private static final int NARANJA = 0xFFFF8C42;
    private static final int ROJO = 0xFFD32F2F;

    private ActividadesDB actividadesDB;
    private String correoUsuario;                     // null = no hay sesión iniciada
    private String filtroSeleccionado;                // null = todas
    private final Set<Integer> ocultas = new HashSet<>(); // las descartadas deslizando a la izquierda
    private List<Actividad> todas = new ArrayList<>();

    private View raiz;
    private ChipGroup chipsTipos;
    private TextView tvVacio;
    private ActividadAdapter adaptador;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.actividades);

        // Barra superior con botón atrás
        MaterialToolbar barra = findViewById(R.id.toolbar);
        setSupportActionBar(barra);
        getSupportActionBar().setTitle("Actividades");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // Sesión guardada al iniciar sesión (SharedPreferences "SesionUsuario")
        correoUsuario = correoSesion();
        actividadesDB = new ActividadesDB(DBHelper.getInstance(this));

        raiz = findViewById(android.R.id.content);

        // Botón de comunidades de chat (grupos de WhatsApp/Telegram/Discord simulados)
        findViewById(R.id.btnComunidades).setOnClickListener(
                v -> HojaComunidades.mostrar(this, correoUsuario));
        chipsTipos = findViewById(R.id.chipsTipos);
        tvVacio = findViewById(R.id.tvVacio);

        adaptador = new ActividadAdapter(this::mostrarDetalle);
        RecyclerView rv = findViewById(R.id.rvActividades);
        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.setAdapter(adaptador);

        // Gesto de deslizar tipo "Tinder"
        new ItemTouchHelper(new GestoDeslizar()).attachToRecyclerView(rv);

        crearChips();

        cargarActividades();
    }

    @Override
    protected void onResume() {
        super.onResume(); // BaseActivity: activa el sensor de agitar
        // Por si ha iniciado o cerrado sesión mientras tanto
        String correo = correoSesion();
        if (correo == null ? correoUsuario != null : !correo.equals(correoUsuario)) {
            correoUsuario = correo;
            cargarActividades();
        }
    }

    /** Correo del usuario con sesión iniciada, o null si no hay sesión. */
    private String correoSesion() {
        String correo = getSharedPreferences("SesionUsuario", MODE_PRIVATE).getString("CORREO", null);
        return correo == null || correo.isEmpty() ? null : correo;
    }

    // -------------------------------------------------
    //              CARGA Y FILTROS
    // -------------------------------------------------

    private void cargarActividades() {
        todas = actividadesDB.listarActividadesConDetalles(correoUsuario, FechasUtil.hoyBD());
        aplicarFiltro();
    }

    private void crearChips() {
        anadirChip(2000, null, "Todas");
        anadirChip(2001, FILTRO_APUNTADO, "✓ Mis actividades");
        for (int i = 0; i < Actividad.TIPOS.length; i++) {
            String tipo = Actividad.TIPOS[i][0];
            anadirChip(2002 + i, tipo, Actividad.emoji(tipo) + " " + Actividad.TIPOS[i][1]);
        }
        chipsTipos.setOnCheckedStateChangeListener((grupo, ids) -> {
            if (ids.isEmpty()) return;
            filtroSeleccionado = (String) grupo.findViewById(ids.get(0)).getTag();
            aplicarFiltro();
        });
    }

    private void anadirChip(int id, String clave, String etiqueta) {
        Chip chip = new Chip(this, null, com.google.android.material.R.attr.chipStyle);
        chip.setId(id);
        chip.setText(etiqueta);
        chip.setTag(clave);
        chip.setCheckable(true);
        chip.setChipBackgroundColorResource(R.color.chip_fondo);
        chip.setTextColor(getColorStateList(R.color.chip_texto));
        chip.setCheckedIconVisible(false);
        chipsTipos.addView(chip);
        if (clave == null) chip.setChecked(true);
    }

    // filtro del chip seleccionado
    private void aplicarFiltro() {
        List<Actividad> visibles = new ArrayList<>();
        for (Actividad a : todas) {
            if (ocultas.contains(a.id)) continue;
            if (filtroSeleccionado == null
                    || (FILTRO_APUNTADO.equals(filtroSeleccionado) && a.inscrito)
                    || filtroSeleccionado.equals(a.tipo)) {
                visibles.add(a);
            }
        }
        adaptador.setActividades(visibles);

        tvVacio.setVisibility(visibles.isEmpty() ? View.VISIBLE : View.GONE);
        if (!FILTRO_APUNTADO.equals(filtroSeleccionado)) {
            tvVacio.setText("No hay actividades de este tipo.");
        } else if (correoUsuario == null) {
            tvVacio.setText("Inicia sesión para ver las actividades a las que te apuntes.");
        } else {
            tvVacio.setText("Todavía no te has apuntado a nada.");
        }
    }

    // -------------------------------------------------
    //              APUNTARSE / DESAPUNTARSE
    // -------------------------------------------------

    private void apuntar(Actividad a) {
        if (correoUsuario == null) {
            pedirSesion("apuntarte");
            return;
        }
        if (a.inscrito) {
            avisar("Ya estabas apuntado a «" + a.titulo + "»");
            return;
        }
        if (!a.plazoAbierto()) {
            avisar("El plazo de inscripción no está abierto: " + a.estadoInscripcion());
            return;
        }
        if (a.completa()) {
            avisar("No quedan plazas 😢");
            return;
        }
        if (!actividadesDB.insertarInscripcion(correoUsuario, a.id, FechasUtil.ahoraBD())) {
            avisar("No se ha podido completar la inscripción");
            return;
        }
        cargarActividades();
        Snackbar.make(raiz, "🎉 Te has apuntado a «" + a.titulo + "»", Snackbar.LENGTH_LONG)
                .setAction("📅 Calendario", v -> anadirACalendario(a))
                .setActionTextColor(NARANJA)
                .show();
    }

    private void desapuntar(Actividad a) {
        actividadesDB.borrarInscripcion(correoUsuario, a.id);
        cargarActividades();
        Snackbar.make(raiz, "Ya no estás apuntado a «" + a.titulo + "»", Snackbar.LENGTH_LONG)
                .setAction("Deshacer", v -> {
                    actividadesDB.insertarInscripcion(correoUsuario, a.id, FechasUtil.ahoraBD());
                    cargarActividades();
                })
                .setActionTextColor(NARANJA)
                .show();
    }

    private void avisar(String texto) {
        Snackbar.make(raiz, texto, Snackbar.LENGTH_SHORT).show();
    }

    /** Aviso de que hace falta iniciar sesión, con botón para ir al login. */
    private void pedirSesion(String accion) {
        Snackbar.make(raiz, "🔒 Inicia sesión para " + accion, Snackbar.LENGTH_LONG)
                .setAction("Iniciar sesión", v -> startActivity(new Intent(this, Login.class)))
                .setActionTextColor(NARANJA)
                .show();
    }

    // -------------------------------------------------
    //              DETALLE (hoja inferior)
    // -------------------------------------------------

    private void mostrarDetalle(Actividad a) {
        BottomSheetDialog hoja = new BottomSheetDialog(this);
        View v = getLayoutInflater().inflate(R.layout.detalle_actividad, null);
        hoja.setContentView(v);

        TextView tvTipo = v.findViewById(R.id.tvTipoDetalle);
        tvTipo.setText(Actividad.emoji(a.tipo) + " " + Actividad.nombreTipo(a.tipo));
        tvTipo.setBackgroundTintList(ColorStateList.valueOf(Actividad.color(a.tipo)));

        ((TextView) v.findViewById(R.id.tvTituloDetalle)).setText(a.titulo);
        ((TextView) v.findViewById(R.id.tvFechaDetalle)).setText("📅 " + textoFechas(a));
        ((TextView) v.findViewById(R.id.tvLugarDetalle)).setText("📍 " + lugar(a));
        ((TextView) v.findViewById(R.id.tvDescripcionDetalle)).setText(a.descripcion);

        TextView tvAbierta = v.findViewById(R.id.tvAbiertaDetalle);
        if (a.esAbiertaUGR()) {
            tvAbierta.setVisibility(View.VISIBLE);
            tvAbierta.setText("🎓 " + a.creditosTexto() + "\n" + a.estadoInscripcion());
            if (a.inscripcionUrgente()) tvAbierta.setTextColor(ROJO);
        }

        String plazas = a.plazasTotales != null
                ? a.inscritos + " de " + a.plazasTotales + " plazas ocupadas"
                : a.inscritos + (a.inscritos == 1 ? " persona apuntada" : " personas apuntadas");
        ((TextView) v.findViewById(R.id.tvPlazasDetalle)).setText("👥 " + plazas);

        // Botón principal según el estado
        Button btnApuntarme = v.findViewById(R.id.btnApuntarme);
        if (a.inscrito) {
            btnApuntarme.setText("Ya no puedo ir (desapuntarme)");
            btnApuntarme.setBackgroundResource(R.drawable.boton_gris);
            btnApuntarme.setOnClickListener(x -> { hoja.dismiss(); desapuntar(a); });
        } else if (!a.plazoAbierto() || a.completa()) {
            btnApuntarme.setText(a.completa() ? "Plazas agotadas" : a.estadoInscripcion());
            btnApuntarme.setBackgroundResource(R.drawable.boton_gris);
            btnApuntarme.setEnabled(false);
        } else if (correoUsuario == null) {
            btnApuntarme.setText("🔒 Inicia sesión para apuntarte");
            btnApuntarme.setOnClickListener(x -> {
                hoja.dismiss();
                startActivity(new Intent(this, Login.class));
            });
        } else {
            btnApuntarme.setText("🙋 Apuntarme");
            btnApuntarme.setOnClickListener(x -> { hoja.dismiss(); apuntar(a); });
        }

        v.findViewById(R.id.btnCalendario).setOnClickListener(x -> anadirACalendario(a));

        View btnMasInfo = v.findViewById(R.id.btnMasInfo);
        if (a.enlace != null && !a.enlace.isEmpty()) {
            btnMasInfo.setOnClickListener(x -> abrirEnlace(a.enlace));
        } else {
            btnMasInfo.setVisibility(View.GONE);
        }

        hoja.show();
    }

    // "Jueves 22 de octubre · 12:00 – 13:30"
    private String textoFechas(Actividad a) {
        String texto = FechasUtil.capitalizar(FechasUtil.diaLargo(a.inicio)) + " · " + FechasUtil.hora(a.inicio);
        if (a.fin != null) {
            if (a.fin.toLocalDate().equals(a.inicio.toLocalDate())) {
                texto += " – " + FechasUtil.hora(a.fin);
            } else {
                texto += "\n    hasta el " + FechasUtil.diaLargo(a.fin);
            }
        }
        return texto;
    }

    private String lugar(Actividad a) {
        return a.lugar != null ? a.lugar + " · ETSIIT" : "ETSIIT · Universidad de Granada";
    }

    // -------------------------------------------------
    //              CALENDARIO Y ENLACES
    // -------------------------------------------------

    /** Abre la app de calendario con el evento ya relleno (el usuario solo pulsa Guardar). */
    private void anadirACalendario(Actividad a) {
        // Si dura varios días (cursos) se añade solo la primera sesión
        boolean mismoDia = a.fin != null && a.fin.toLocalDate().equals(a.inicio.toLocalDate());
        boolean ok = CalendarioUtil.anadirEvento(this, a.titulo, a.descripcion, lugar(a),
                a.inicio, mismoDia ? a.fin : a.inicio.plusHours(2));
        if (!ok) avisar("No hay ninguna app de calendario en este móvil");
    }

    private void abrirEnlace(String url) {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        } catch (ActivityNotFoundException e) {
            avisar("No hay navegador para abrir el enlace");
        }
    }

    // -------------------------------------------------
    //              GESTO DE DESLIZAR
    // -------------------------------------------------

    /**
     * Derecha = apuntarse (fondo verde), izquierda = ocultar (fondo gris).
     * Mientras se arrastra, la tarjeta se inclina un poco, como en Tinder.
     */
    private class GestoDeslizar extends ItemTouchHelper.SimpleCallback {

        private final Paint fondo = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint texto = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final float radio = 20 * getResources().getDisplayMetrics().density;
        private final float margen = 24 * getResources().getDisplayMetrics().density;

        GestoDeslizar() {
            super(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT);
            texto.setColor(0xFFFFFFFF);
            texto.setFakeBoldText(true);
            texto.setTextSize(16 * getResources().getDisplayMetrics().scaledDensity);
        }

        @Override
        public boolean onMove(@NonNull RecyclerView rv, @NonNull RecyclerView.ViewHolder vh,
                              @NonNull RecyclerView.ViewHolder destino) {
            return false; // no se reordena
        }

        @Override
        public float getSwipeThreshold(@NonNull RecyclerView.ViewHolder vh) {
            return 0.35f; // basta con arrastrar un tercio
        }

        @Override
        public void onSwiped(@NonNull RecyclerView.ViewHolder vh, int direccion) {
            int pos = vh.getAdapterPosition();
            if (pos == RecyclerView.NO_POSITION) return;
            Actividad a = adaptador.get(pos);

            if (direccion == ItemTouchHelper.RIGHT) {
                adaptador.notifyItemChanged(pos); // la tarjeta vuelve a su sitio
                apuntar(a);
            } else {
                ocultas.add(a.id);
                aplicarFiltro();
                Snackbar.make(raiz, "Has ocultado «" + a.titulo + "»", Snackbar.LENGTH_LONG)
                        .setAction("Deshacer", v -> { ocultas.remove(a.id); aplicarFiltro(); })
                        .setActionTextColor(NARANJA)
                        .show();
            }
        }

        @Override
        public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView rv, @NonNull RecyclerView.ViewHolder vh,
                                float dX, float dY, int estado, boolean activo) {
            View item = vh.itemView;
            if (estado == ItemTouchHelper.ACTION_STATE_SWIPE && dX != 0) {
                boolean derecha = dX > 0;
                fondo.setColor(derecha ? 0xFF2E7D32 : 0xFF9E9E9E);
                RectF zona = derecha
                        ? new RectF(item.getLeft(), item.getTop(), item.getLeft() + dX, item.getBottom())
                        : new RectF(item.getRight() + dX, item.getTop(), item.getRight(), item.getBottom());
                c.drawRoundRect(zona, radio, radio, fondo);

                float y = item.getTop() + item.getHeight() / 2f + texto.getTextSize() / 3f;
                if (derecha) {
                    texto.setTextAlign(Paint.Align.LEFT);
                    c.drawText("✓ Apuntarme", item.getLeft() + margen, y, texto);
                } else {
                    texto.setTextAlign(Paint.Align.RIGHT);
                    c.drawText("Ocultar ✕", item.getRight() - margen, y, texto);
                }
                // inclinación proporcional a lo que se ha arrastrado
                item.setRotation(8f * dX / item.getWidth());
            }
            super.onChildDraw(c, rv, vh, dX, dY, estado, activo);
        }

        @Override
        public void clearView(@NonNull RecyclerView rv, @NonNull RecyclerView.ViewHolder vh) {
            super.clearView(rv, vh);
            vh.itemView.setRotation(0f);
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
