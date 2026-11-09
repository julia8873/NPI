package com.example.nuietsiit.activities.secciones.mapa;

import android.content.Context;
import android.content.SharedPreferences;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.GridLayout;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.nuietsiit.R;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

/**
 * "¿A dónde vas?" al entrar al mapa: hoja casi a pantalla completa con la clase de ahora (un toque y te lleva),
 * una rejilla de sitios, los recientes y "Solo ver el mapa". El mapa decide luego cómo llevarte (DestinoViaje).
 */
public final class InicioMapa {

    /** Lo que hace el mapa con lo elegido. */
    public interface Acciones {
        void ir(DestinoViaje destino);

        /** Cerrada sin elegir nada. */
        void soloMapa();
    }

    /** La clase se propone si está en curso o empieza en este margen. */
    static final int MARGEN_CLASE_MIN = 120;
    private static final int MAX_RECIENTES = 3;

    private static final String PREFS = "inicio_mapa";
    private static final String CLAVE_RECIENTES = "recientes";
    private static final String CLAVE_NO_PREGUNTAR = "no_preguntar_dia";
    private static final String CLAVE_EXPLICADO = "explicado";

    /** {icono, nombre, edificio, sitio de dentro (parte del título del marcador) o null}. */
    private static final String[][] SITIOS = {
            {"🎓", "ETSIIT", ProximaClase.ETSIIT, null},
            {"🏢", "Edificio Auxiliar", ProximaClase.AUXILIAR, null},
            {"📚", "Biblioteca", ProximaClase.ETSIIT, "Biblioteca"},
            {"☕", "Comedor", ProximaClase.ETSIIT, "Comedor"},
            {"🚪", "Secretaría", ProximaClase.ETSIIT, "Secretaría"},
    };

    private InicioMapa() {}

    /** false si hoy se marcó "No preguntar más hoy". */
    public static boolean tocaPreguntar(Context ctx) {
        return !LocalDate.now().toString().equals(prefs(ctx).getString(CLAVE_NO_PREGUNTAR, ""));
    }

    /**
     * @param clase         clase en curso o siguiente de hoy (puede ser null)
     * @param titulos       títulos de los marcadores de los planos (aulas y sitios de dentro)
     * @param conUbicacion  si ya hay permiso de ubicación (solo cambia el texto de aviso)
     */
    public static void mostrar(AppCompatActivity act, ProximaClase clase, Calendar ahora, List<String> titulos,
                               boolean conUbicacion, Acciones acciones) {
        if (act.isFinishing()) return;
        BottomSheetDialog hoja = new BottomSheetDialog(act);
        View vista = LayoutInflater.from(act).inflate(R.layout.inicio_mapa, null);
        hoja.setContentView(vista);
        BottomSheetBehavior<?> comportamiento = hoja.getBehavior();
        comportamiento.setState(BottomSheetBehavior.STATE_EXPANDED);
        comportamiento.setSkipCollapsed(true);

        boolean[] elegido = {false};
        java.util.function.Consumer<DestinoViaje> ir = d -> {
            elegido[0] = true;
            guardarReciente(act, d);
            vista.performHapticFeedback(HapticFeedbackConstants.CONFIRM);
            hoja.dismiss();
            acciones.ir(d);
        };
        hoja.setOnDismissListener(d -> {
            if (((CheckBox) vista.findViewById(R.id.cbInicioNoPreguntar)).isChecked()) {
                prefs(act).edit().putString(CLAVE_NO_PREGUNTAR, LocalDate.now().toString()).apply();
            }
            if (!elegido[0]) acciones.soloMapa();
        });

        SharedPreferences p = prefs(act);
        if (!p.getBoolean(CLAVE_EXPLICADO, false)) {
            vista.findViewById(R.id.llInicioMascota).setVisibility(View.VISIBLE);
            p.edit().putBoolean(CLAVE_EXPLICADO, true).apply();
        }
        ((TextView) vista.findViewById(R.id.tvInicioUbicacion)).setText(conUbicacion
                ? "📍 Te busco con el GPS al elegir"
                : "📍 Al elegir te pediré la ubicación para guiarte");

        pintarClase(vista, clase, ahora, titulos, ir);
        pintarRecientes(act, vista, ir);
        pintarSitios(act, vista, titulos, ir);
        vista.findViewById(R.id.btnInicioSoloMapa).setOnClickListener(v -> hoja.dismiss());
        hoja.show();
    }

    private static void pintarClase(View vista, ProximaClase clase, Calendar ahora, List<String> titulos,
                                    java.util.function.Consumer<DestinoViaje> ir) {
        if (clase == null) return;
        int faltan = LlegadaEdificio.minutosHasta(clase.horaInicio, ahora);
        if (!clase.enCurso && faltan > MARGEN_CLASE_MIN) return;

        vista.findViewById(R.id.cardInicioClase).setVisibility(View.VISIBLE);
        ((TextView) vista.findViewById(R.id.tvInicioClaseTitulo)).setText("🎓 Tu clase de " + clase.nombreAsignatura);
        String cuando = clase.enCurso ? "ya ha empezado (" + clase.horaInicio + ")"
                : "empieza a las " + clase.horaInicio + " (en " + faltan + " min)";
        ((TextView) vista.findViewById(R.id.tvInicioClaseDetalle))
                .setText((clase.aula != null ? clase.aula + " · " : "") + cuando);

        // Aula de la ETSIIT con marcador → hasta la puerta; si no, hasta el edificio
        String interior = clase.aula != null && ProximaClase.ETSIIT.equals(clase.idEdificio)
                && hayMarcador(titulos, clase.aula) ? clase.aula : null;
        DestinoViaje destino = new DestinoViaje(clase.idEdificio, interior,
                "🎓 Clase de " + clase.codigoAsignatura, clase.horaInicio);
        vista.findViewById(R.id.btnInicioClaseLlevame).setOnClickListener(v -> ir.accept(destino));
    }

    private static void pintarRecientes(Context ctx, View vista, java.util.function.Consumer<DestinoViaje> ir) {
        ChipGroup grupo = vista.findViewById(R.id.cgInicioRecientes);
        for (String linea : recientes(ctx)) {
            DestinoViaje d = DestinoViaje.leer(linea);
            if (d == null) continue;
            Chip chip = new Chip(ctx);
            chip.setText("🕘 " + d.etiqueta);
            chip.setTextSize(15);
            chip.setChipMinHeight(48 * ctx.getResources().getDisplayMetrics().density);
            chip.setOnClickListener(v -> ir.accept(d));
            grupo.addView(chip);
        }
        if (grupo.getChildCount() > 0) {
            grupo.setVisibility(View.VISIBLE);
            vista.findViewById(R.id.tvInicioRecientes).setVisibility(View.VISIBLE);
        }
    }

    private static void pintarSitios(AppCompatActivity act, View vista, List<String> titulos,
                                     java.util.function.Consumer<DestinoViaje> ir) {
        GridLayout rejilla = vista.findViewById(R.id.glInicioDestinos);
        for (String[] s : SITIOS) {
            if (s[3] != null && !hayMarcador(titulos, s[3])) continue;
            DestinoViaje d = new DestinoViaje(s[2], s[3], s[0] + " " + s[1], null);
            rejilla.addView(casilla(act, s[0], s[1], v -> ir.accept(d)));
        }
        rejilla.addView(casilla(act, "🔍", "Buscar aula", v -> elegirAula(act, titulos, ir)));
    }

    private static MaterialButton casilla(Context ctx, String icono, String nombre, View.OnClickListener alTocar) {
        float dp = ctx.getResources().getDisplayMetrics().density;
        MaterialButton b = new MaterialButton(ctx);
        b.setText(icono + "\n" + nombre);
        b.setAllCaps(false);
        b.setTextSize(15);
        b.setGravity(Gravity.CENTER);
        b.setTextColor(ContextCompat.getColor(ctx, R.color.nui_tinta));
        b.setBackgroundTintList(ContextCompat.getColorStateList(ctx, R.color.nui_suave));
        b.setCornerRadius((int) (16 * dp));
        b.setOnClickListener(alTocar);
        GridLayout.LayoutParams lp = new GridLayout.LayoutParams(
                GridLayout.spec(GridLayout.UNDEFINED), GridLayout.spec(GridLayout.UNDEFINED, 1f));
        lp.width = 0;
        lp.height = (int) (84 * dp);
        lp.setMargins((int) (4 * dp), (int) (4 * dp), (int) (4 * dp), (int) (4 * dp));
        b.setLayoutParams(lp);
        return b;
    }

    private static void elegirAula(Context ctx, List<String> titulos, java.util.function.Consumer<DestinoViaje> ir) {
        List<String> aulas = new ArrayList<>();
        for (String t : titulos) {
            if (t.toLowerCase(Locale.ROOT).contains("aula")) aulas.add(t);
        }
        new AlertDialog.Builder(ctx)
                .setTitle("¿A qué aula vas?")
                .setItems(aulas.toArray(new String[0]), (d, i) ->
                        ir.accept(new DestinoViaje(ProximaClase.ETSIIT, aulas.get(i), "🚪 " + aulas.get(i), null)))
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private static boolean hayMarcador(List<String> titulos, String buscado) {
        for (String t : titulos) {
            if (t.contains(buscado)) return true;
        }
        return false;
    }

    private static List<String> recientes(Context ctx) {
        String texto = prefs(ctx).getString(CLAVE_RECIENTES, "");
        return texto.isEmpty() ? new ArrayList<>() : Arrays.asList(texto.split("\n"));
    }

    private static void guardarReciente(Context ctx, DestinoViaje d) {
        List<String> lista = DestinoViaje.anadirReciente(recientes(ctx), d, MAX_RECIENTES);
        prefs(ctx).edit().putString(CLAVE_RECIENTES, String.join("\n", lista)).apply();
    }

    private static SharedPreferences prefs(Context ctx) {
        return ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }
}
