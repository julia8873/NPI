package com.example.nuietsiit.activities.secciones.mapa;

import android.app.Activity;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AlertDialog;

import com.example.nuietsiit.R;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

/**
 * Al llegar con la guía a la ETSIIT o al Edificio Auxiliar: "¿A dónde vas ahora?".
 * Enlaza la ruta exterior con la interior (planos + VisorRuta) o con la ruta ETSIIT ↔ Auxiliar.
 */
public final class LlegadaEdificio {

    /** Lo que hace el mapa con la opción elegida. */
    public interface Acciones {
        /** Ruta interior desde la entrada hasta el marcador cuyo título contiene {@code buscado}. */
        void rutaInterior(String buscado);

        /** Ruta exterior entre dos lugares de la red (con la guía). */
        void rutaExterior(String idDesde, String idHasta);
    }

    /** La clase cuenta como "ahora" si está en curso o empieza en este margen. */
    static final int MARGEN_CLASE_MIN = 30;

    private static final String[][] DESTINOS_INTERIORES = {
            {"📚 Biblioteca", "Biblioteca"},
            {"☕ Comedor", "Comedor"},
            {"🚪 Secretaría", "Secretaría"},
    };

    private LlegadaEdificio() {}

    /** Segundos de la cuenta atrás de "Has llegado · sigue hasta…". */
    static final int SEGUNDOS_SEGUIR = 5;

    /**
     * @param titulos  títulos de los marcadores de los planos (para saber qué aulas hay)
     * @param clase    clase en curso o siguiente de hoy (puede ser null)
     * @param llegando true = acabas de llegar con la guía ("Has llegado a…"); false = ya estabas dentro
     */
    public static void mostrar(Activity act, String idLugar, ProximaClase clase, Calendar ahora,
                               List<String> titulos, boolean llegando, Acciones acciones) {
        if (act.isFinishing()) return;
        ProximaClase cercana = clase != null && (clase.enCurso || minutosHasta(clase.horaInicio, ahora)
                <= MARGEN_CLASE_MIN) ? clase : null;

        if (ProximaClase.ETSIIT.equals(idLugar)) {
            enLaEtsiit(act, cercana, titulos, llegando, acciones);
        } else if (ProximaClase.AUXILIAR.equals(idLugar)) {
            enElAuxiliar(act, cercana, acciones);
        }
    }

    /**
     * Llegada con el destino de dentro ya elegido: no se vuelve a preguntar. "Has llegado · sigue hasta
     * el Aula 2.1" y a los 5 s pasa sola a la ruta interior ({@code seguir}); "Cancelar" se queda en el mapa.
     */
    public static void seguirHasta(Activity act, String edificio, String destino, Runnable seguir) {
        if (act.isFinishing()) return;
        Handler handler = new Handler(Looper.getMainLooper());
        AlertDialog dialogo = new AlertDialog.Builder(act)
                .setTitle(act.getString(R.string.mapa_has_llegado_a, edificio))
                .setMessage(textoSeguir(act, destino, SEGUNDOS_SEGUIR))
                .setPositiveButton(act.getString(R.string.mapa_ir_ya), (d, w) -> seguir.run())
                .setNegativeButton(act.getString(R.string.cancelar), null)
                .create();
        int[] quedan = {SEGUNDOS_SEGUIR};
        Runnable cuenta = new Runnable() {
            @Override public void run() {
                if (!dialogo.isShowing()) return;
                if (--quedan[0] <= 0) {
                    dialogo.dismiss();
                    seguir.run();
                    return;
                }
                dialogo.setMessage(textoSeguir(act, destino, quedan[0]));
                handler.postDelayed(this, 1000);
            }
        };
        dialogo.setOnDismissListener(d -> handler.removeCallbacks(cuenta));
        dialogo.show();
        handler.postDelayed(cuenta, 1000);
    }

    private static String textoSeguir(Activity act, String destino, int segundos) {
        return act.getString(R.string.mapa_texto_seguir, destino, segundos);
    }

    private static void enLaEtsiit(Activity act, ProximaClase clase, List<String> titulos, boolean llegando,
                                   Acciones acciones) {
        List<String> textos = new ArrayList<>();
        List<Runnable> opciones = new ArrayList<>();

        if (clase != null) {
            if (ProximaClase.AUXILIAR.equals(clase.idEdificio)) {
                textos.add(act.getString(R.string.mapa_a_tu_clase_de_auxiliar, clase.texto(act)));
                opciones.add(() -> acciones.rutaExterior(ProximaClase.ETSIIT, ProximaClase.AUXILIAR));
            } else if (clase.aula != null && hayMarcador(titulos, clase.aula)) {
                textos.add(act.getString(R.string.mapa_a_tu_clase_de, clase.texto(act)));
                opciones.add(() -> acciones.rutaInterior(clase.aula));
            }
        }
        for (String[] d : DESTINOS_INTERIORES) {
            textos.add(d[0]);
            opciones.add(() -> acciones.rutaInterior(d[1]));
        }
        textos.add(act.getString(R.string.mapa_buscar_un_aula));
        opciones.add(() -> elegirAula(act, titulos, acciones));

        new AlertDialog.Builder(act)
                .setTitle(llegando ? act.getString(R.string.mapa_llegado_etsiit_preguntar)
                        : act.getString(R.string.mapa_estas_etsiit_preguntar))
                .setItems(textos.toArray(new String[0]), (d, i) -> opciones.get(i).run())
                .setNegativeButton(act.getString(R.string.mapa_a_ningun_sitio), null)
                .show();
    }

    private static void enElAuxiliar(Activity act, ProximaClase clase, Acciones acciones) {
        AlertDialog.Builder dialogo = new AlertDialog.Builder(act)
                .setTitle(act.getString(R.string.mapa_estas_en_auxiliar))
                .setPositiveButton(act.getString(R.string.mapa_vale), null);
        if (clase == null) {
            dialogo.setMessage(act.getString(R.string.mapa_sin_clase_margen, MARGEN_CLASE_MIN));
        } else if (ProximaClase.AUXILIAR.equals(clase.idEdificio)) {
            dialogo.setMessage(act.getString(R.string.mapa_tu_clase_es_aqui, clase.texto(act)));
        } else {
            // La clase es en la ETSIIT: se ofrece volver
            dialogo.setMessage(act.getString(R.string.mapa_tu_clase_es_en_etsiit, clase.texto(act)))
                    .setNeutralButton(act.getString(R.string.mapa_ir_a_etsiit),
                            (d, w) -> acciones.rutaExterior(ProximaClase.AUXILIAR, ProximaClase.ETSIIT));
        }
        dialogo.show();
    }

    private static void elegirAula(Activity act, List<String> titulos, Acciones acciones) {
        List<String> aulas = new ArrayList<>();
        for (String t : titulos) {
            if (t.toLowerCase(Locale.ROOT).contains("aula")) aulas.add(t);
        }
        new AlertDialog.Builder(act)
                .setTitle(act.getString(R.string.mapa_a_que_aula_vas))
                .setItems(aulas.toArray(new String[0]), (d, i) -> acciones.rutaInterior(aulas.get(i)))
                .setNegativeButton(act.getString(R.string.cancelar), null)
                .show();
    }

    private static boolean hayMarcador(List<String> titulos, String buscado) {
        for (String t : titulos) {
            if (t.contains(buscado)) return true;
        }
        return false;
    }

    /** Minutos desde {@code ahora} hasta "HH:mm" (negativo si ya pasó). */
    static int minutosHasta(String hhmm, Calendar ahora) {
        int clase = Integer.parseInt(hhmm.substring(0, 2)) * 60 + Integer.parseInt(hhmm.substring(3, 5));
        return clase - (ahora.get(Calendar.HOUR_OF_DAY) * 60 + ahora.get(Calendar.MINUTE));
    }
}
