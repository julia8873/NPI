package com.example.nuietsiit.activities.secciones.mapa;

import android.content.Context;

import com.example.nuietsiit.R;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Planificador de rutas exteriores: compara ir a pie con "a pie + bus + a pie" (paradas a
 * ≤ 700 m del origen y del destino, mismo sentido) y elige la llegada más temprana.
 * Si dos opciones llegan casi a la vez (≤ 1 min), gana la que menos hace andar; ir a pie
 * gana el empate con el bus. "Sal a las" = paso del bus − tramo a pie − 2 min de margen.
 */
public final class PlanificadorRuta {

    public static final double RADIO_PARADA_M = 700;
    public static final double LEJOS_M = 3000;
    public static final Duration MARGEN_PARADA = Duration.ofMinutes(2);
    private static final Duration EMPATE = Duration.ofMinutes(1);
    private static final int SIGUIENTES_ALTERNATIVAS = 2;

    private final Context ctx;
    private final RedTransporte red;
    private final RutaAPie aPie;

    public PlanificadorRuta(Context ctx, RedTransporte red) {
        this(ctx, red, RutaAPie.local(red));
    }

    public PlanificadorRuta(Context ctx, RedTransporte red, RutaAPie aPie) {
        this.ctx = ctx;
        this.red = red;
        this.aPie = aPie;
    }

    public PlanificadorRuta(RedTransporte red) {
        this(null, red, RutaAPie.local(red));
    }

    public PlanificadorRuta(RedTransporte red, RutaAPie aPie) {
        this(null, red, aPie);
    }

    /** Combinación fija subida/bajada; el bus concreto depende de la hora. */
    private static final class Opcion {
        final RedTransporte.Linea linea;
        final RedTransporte.Sentido sentido;
        final RedTransporte.Parada subida, bajada;
        final RutaAPie.Camino ida, vuelta;
        final HorarioBus horario;

        Opcion(RedTransporte.Linea linea, RedTransporte.Sentido sentido, RedTransporte.Parada subida,
               RedTransporte.Parada bajada, RutaAPie.Camino ida, RutaAPie.Camino vuelta) {
            this.linea = linea;
            this.sentido = sentido;
            this.subida = subida;
            this.bajada = bajada;
            this.ida = ida;
            this.vuelta = vuelta;
            this.horario = new HorarioBus(linea);
        }

        double metrosAPie() {
            return ida.metros + vuelta.metros;
        }
    }

    public PlanRuta planificar(RedTransporte.Lugar origen, RedTransporte.Lugar destino, LocalDateTime ahora) {
        return planificar(origen.punto, destino, ahora);
    }

    /** Mejor plan para salir ahora (o lo antes posible) desde un punto cualquiera. */
    public PlanRuta planificar(Punto origen, RedTransporte.Lugar destino, LocalDateTime ahora) {
        RutaAPie.Camino andando = aPie.calcular(origen, destino.punto);
        Candidato bus = mejorBus(origen, destino, opciones(origen, destino), ahora);
        boolean andarEsViable = andando.metros <= LEJOS_M;

        if (bus == null) {
            return andarEsViable ? planAPie(origen, destino, andando, ahora)
                    : new PlanRuta(PlanRuta.Tipo.LEJOS, origen, destino, Collections.emptyList(), ahora, ahora);
        }
        if (!andarEsViable) return bus.plan;
        PlanRuta aPieAhora = planAPie(origen, destino, andando, ahora);
        return bus.plan.llegada.isBefore(aPieAhora.llegada.minus(EMPATE)) ? bus.plan : aPieAhora;
    }

    public List<PlanRuta> alternativas(RedTransporte.Lugar origen, RedTransporte.Lugar destino, LocalDateTime ahora) {
        return alternativas(origen.punto, destino, ahora);
    }

    /**
     * Planes en bus con la mejor combinación de paradas, ordenados por hora: el bus anterior
     * (solo si aún se alcanza saliendo ya, sin margen: su salida puede ser algo anterior a
     * "ahora"), el elegido y los dos siguientes. Vacío si no hay bus.
     */
    public List<PlanRuta> alternativas(Punto origen, RedTransporte.Lugar destino, LocalDateTime ahora) {
        List<PlanRuta> planes = new ArrayList<>();
        Candidato elegido = mejorBus(origen, destino, opciones(origen, destino), ahora);
        if (elegido == null) return planes;
        Opcion op = elegido.opcion;

        HorarioBus.Paso anterior = op.horario.pasoAnterior(op.sentido, op.subida, elegido.paso.hora);
        if (anterior != null && !anterior.hora.isBefore(ahora.plus(minutos(op.ida.minutos())))) {
            planes.add(planBus(origen, destino, op, anterior));
        }
        for (HorarioBus.Paso p : op.horario.proximosPasos(op.sentido, op.subida, elegido.paso.hora,
                SIGUIENTES_ALTERNATIVAS + 1)) {
            planes.add(planBus(origen, destino, op, p));
        }
        return planes;
    }

    // ---------- Internos ----------

    private List<Opcion> opciones(Punto origen, RedTransporte.Lugar destino) {
        List<Opcion> opciones = new ArrayList<>();
        for (RedTransporte.Linea linea : red.lineas) {
            for (RedTransporte.Sentido sentido : linea.sentidos) {
                List<RedTransporte.Parada> paradas = sentido.paradas;
                for (int i = 0; i < paradas.size(); i++) {
                    RedTransporte.Parada subida = paradas.get(i);
                    if (origen.distanciaA(subida.punto) > RADIO_PARADA_M) continue;
                    RutaAPie.Camino ida = null;
                    for (int j = i + 1; j < paradas.size(); j++) {
                        RedTransporte.Parada bajada = paradas.get(j);
                        if (bajada.punto.distanciaA(destino.punto) > RADIO_PARADA_M) continue;
                        if (ida == null) ida = aPie.calcular(origen, subida.punto);
                        opciones.add(new Opcion(linea, sentido, subida, bajada, ida,
                                aPie.calcular(bajada.punto, destino.punto)));
                    }
                }
            }
        }
        return opciones;
    }

    /** Una opción con un bus concreto. */
    private static final class Candidato {
        final Opcion opcion;
        final HorarioBus.Paso paso;
        final PlanRuta plan;

        Candidato(Opcion opcion, HorarioBus.Paso paso, PlanRuta plan) {
            this.opcion = opcion;
            this.paso = paso;
            this.plan = plan;
        }
    }

    /** Llegada más temprana; en empate (≤ 1 min), la que menos hace andar. */
    private Candidato mejorBus(Punto origen, RedTransporte.Lugar destino, List<Opcion> opciones,
                               LocalDateTime ahora) {
        List<Candidato> candidatos = new ArrayList<>();
        Candidato primero = null;
        for (Opcion op : opciones) {
            LocalDateTime enParada = ahora.plus(minutos(op.ida.minutos())).plus(MARGEN_PARADA);
            HorarioBus.Paso paso = op.horario.proximoPaso(op.sentido, op.subida, enParada);
            if (paso == null) continue;
            Candidato c = new Candidato(op, paso, planBus(origen, destino, op, paso));
            candidatos.add(c);
            if (primero == null || c.plan.llegada.isBefore(primero.plan.llegada)) primero = c;
        }
        if (primero == null) return null;
        Candidato mejor = null;
        for (Candidato c : candidatos) {
            if (c.plan.llegada.isAfter(primero.plan.llegada.plus(EMPATE))) continue;
            if (mejor == null || c.plan.metrosAPie < mejor.plan.metrosAPie) mejor = c;
        }
        return mejor;
    }

    private PlanRuta planBus(Punto origen, RedTransporte.Lugar destino, Opcion op, HorarioBus.Paso paso) {
        Duration ida = minutos(op.ida.minutos());
        LocalDateTime salida = paso.hora.minus(ida).minus(MARGEN_PARADA);
        LocalDateTime enParada = salida.plus(ida);
        LocalDateTime bajada = paso.horaEn(op.bajada);
        LocalDateTime llegada = bajada.plus(minutos(op.vuelta.minutos()));
        String linea = op.linea.id;

        String inst1 = ctx != null ? ctx.getString(R.string.mapa_instruccion_camina_parada, textoAPie(op.ida), op.subida.nombre)
                : "Camina " + textoAPie(op.ida) + " hasta la parada " + op.subida.nombre;
        String inst2 = ctx != null ? ctx.getString(R.string.mapa_instruccion_espera_bus, linea, op.sentido.hacia, PlanRuta.hora(paso.hora))
                : "Espera el " + linea + " hacia " + op.sentido.hacia + " (pasa a las " + PlanRuta.hora(paso.hora) + ")";

        List<Punto> forma = op.sentido.formaEntre(op.subida, op.bajada);
        int nParadas = op.sentido.paradas.indexOf(op.bajada) - op.sentido.paradas.indexOf(op.subida);

        String inst3 = ctx != null ? (nParadas == 1
                ? ctx.getString(R.string.mapa_instruccion_sube_bus_sing, linea, op.sentido.hacia, op.bajada.nombre, nParadas)
                : ctx.getString(R.string.mapa_instruccion_sube_bus_plur, linea, op.sentido.hacia, op.bajada.nombre, nParadas))
                : "Sube al " + linea + " hacia " + op.sentido.hacia + " y bájate en " + op.bajada.nombre
                + " (" + nParadas + (nParadas == 1 ? " parada)" : " paradas)");

        String inst4 = ctx != null ? ctx.getString(R.string.mapa_instruccion_camina_hasta, textoAPie(op.vuelta), destino.corto)
                : "Camina " + textoAPie(op.vuelta) + " hasta " + destino.corto;

        List<PlanRuta.Tramo> tramos = new ArrayList<>();
        tramos.add(new PlanRuta.Tramo(PlanRuta.TipoTramo.A_PIE, op.ida.puntos, salida, enParada,
                inst1, op.subida.id, op.ida.metros, null, null, null, null));
        tramos.add(new PlanRuta.Tramo(PlanRuta.TipoTramo.ESPERA, Collections.singletonList(op.subida.punto),
                enParada, paso.hora, inst2, op.subida.id, 0, op.linea, op.sentido, op.subida, op.bajada));
        tramos.add(new PlanRuta.Tramo(PlanRuta.TipoTramo.BUS, forma, paso.hora, bajada,
                inst3, "bus_" + linea, longitud(forma), op.linea, op.sentido, op.subida, op.bajada));
        tramos.add(new PlanRuta.Tramo(PlanRuta.TipoTramo.A_PIE, op.vuelta.puntos, bajada, llegada,
                inst4, destino.id, op.vuelta.metros, null, null, null, null));
        return new PlanRuta(PlanRuta.Tipo.BUS, origen, destino, tramos, salida, llegada);
    }

    private PlanRuta planAPie(Punto origen, RedTransporte.Lugar destino, RutaAPie.Camino camino,
                              LocalDateTime ahora) {
        LocalDateTime llegada = ahora.plus(minutos(camino.minutos()));
        String inst = ctx != null ? ctx.getString(R.string.mapa_instruccion_camina_hasta, textoAPie(camino), destino.corto)
                : "Camina " + textoAPie(camino) + " hasta " + destino.corto;
        PlanRuta.Tramo tramo = new PlanRuta.Tramo(PlanRuta.TipoTramo.A_PIE, camino.puntos, ahora, llegada,
                inst, destino.id, camino.metros, null, null, null, null);
        return new PlanRuta(PlanRuta.Tipo.A_PIE, origen, destino, Collections.singletonList(tramo), ahora, llegada);
    }

    private static String textoAPie(RutaAPie.Camino c) {
        long min = PlanRuta.minutosArriba(minutos(c.minutos()));
        return String.format(Locale.getDefault(), "%d min (%d m)", Math.max(min, 1), Math.round(c.metros));
    }

    private static Duration minutos(double min) {
        return Duration.ofSeconds(Math.round(min * 60));
    }

    private static double longitud(List<Punto> puntos) {
        double total = 0;
        for (int i = 1; i < puntos.size(); i++) total += puntos.get(i - 1).distanciaA(puntos.get(i));
        return total;
    }
}
