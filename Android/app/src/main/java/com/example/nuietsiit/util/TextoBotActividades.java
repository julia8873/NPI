package com.example.nuietsiit.util;

import android.content.Context;

import com.example.nuietsiit.data.ActividadesDB;
import com.example.nuietsiit.data.ActividadesGuardadas;
import com.example.nuietsiit.data.AnunciosDB;
import com.example.nuietsiit.data.AnunciosGuardados;
import com.example.nuietsiit.data.DBHelper;
import com.example.nuietsiit.data.modelo.Actividad;
import com.example.nuietsiit.data.modelo.Anuncio;

import java.util.List;
import java.util.Set;

/**
 * Datos de Actividades y Noticias para el bot (GeminiManager los mete como texto en sus instrucciones).
 * Se leen de la base de datos cada vez que se abre el chat, así que incluyen a qué se ha apuntado el usuario
 * aunque lo haya hecho hace un momento: actividades próximas con lugar, plazas y plazos de AbiertaUGR,
 * sus inscripciones y créditos, los avisos vigentes y cómo se usa esta parte de la app.
 */
public final class TextoBotActividades {

    private TextoBotActividades() {}

    public static String generar(Context ctx, String correo) {
        StringBuilder sb = new StringBuilder();
        DBHelper bd = DBHelper.getInstance(ctx);
        ActividadesDB actividadesDB = new ActividadesDB(bd);
        boolean sesion = correo != null && !correo.isEmpty();
        List<Actividad> actividades = actividadesDB.listarActividadesConDetalles(sesion ? correo : null, FechasUtil.hoyBD());
        Set<String> guardadas = ActividadesGuardadas.guardadas(ctx, sesion ? correo : null);

        sb.append("\n[ACTIVIDADES PRÓXIMAS DE LA ETSIIT (sección Actividades de la app)]\n");
        for (Actividad a : actividades) {
            sb.append("- ").append(a.titulo)
                    .append(" | Tipo: ").append(Actividad.nombreTipo(a.tipo))
                    .append(" | Cuándo: ").append(FechasUtil.diaLargo(a.inicio)).append(" a las ").append(FechasUtil.hora(a.inicio));
            if (a.fin != null) {
                sb.append(" hasta ").append(a.fin.toLocalDate().equals(a.inicio.toLocalDate())
                        ? "las " + FechasUtil.hora(a.fin) : "el " + FechasUtil.diaLargo(a.fin));
            }
            String sitio = a.sitioEnMapa();
            sb.append(" | Dónde: ").append(sitio != null ? sitio + " (ETSIIT)" : "ETSIIT");
            if (a.esAbiertaUGR()) {
                sb.append(" | Curso AbiertaUGR de ").append(a.creditosTexto()).append(": ").append(a.estadoInscripcion());
            }
            sb.append(" | Apuntados: ").append(a.inscritos);
            if (a.plazasTotales != null) sb.append(" de ").append(a.plazasTotales).append(" plazas");
            if (sesion) sb.append(" | El usuario está apuntado: ").append(a.inscrito ? "sí" : "no");
            if (guardadas.contains(String.valueOf(a.id))) sb.append(" | El usuario la tiene guardada para luego");
            if (a.enlace != null && !a.enlace.isEmpty()) sb.append(" | Más información: ").append(a.enlace);
            sb.append("\n  ").append(a.descripcion).append("\n");
        }

        sb.append("\n[ACTIVIDADES A LAS QUE ESTÁ APUNTADO EL USUARIO]\n");
        if (!sesion) {
            sb.append("- El usuario no ha iniciado sesión: puede ver las actividades, pero para apuntarse tiene que iniciar sesión.\n");
        } else {
            boolean alguna = false;
            for (Actividad a : actividades) {
                if (!a.inscrito) continue;
                alguna = true;
                sb.append("- ").append(a.titulo).append(" (").append(FechasUtil.diaLargo(a.inicio))
                        .append(" a las ").append(FechasUtil.hora(a.inicio)).append(")\n");
            }
            if (!alguna) sb.append("- Ninguna próxima.\n");
            double[] creditos = actividadesDB.creditosAbiertaUGR(correo, FechasUtil.ahoraBD());
            sb.append("- Créditos AbiertaUGR: ").append(ects(creditos[0])).append(" ECTS conseguidos y ")
                    .append(ects(creditos[1])).append(" ECTS en cursos a los que está apuntado ")
                    .append("(se pueden reconocer hasta 6 ECTS por actividades).\n");
        }

        sb.append("\n[AVISOS Y NOTICIAS VIGENTES (sección Noticias de la app)]\n");
        Set<String> avisosGuardados = AnunciosGuardados.guardados(ctx, sesion ? correo : null);
        for (Anuncio n : new AnunciosDB(bd).obtenerAnunciosGenerales(FechasUtil.ahoraBD())) {
            sb.append("- ").append(n.esUrgente() ? "[URGENTE] " : "").append(Anuncio.nombreCategoria(n.categoria))
                    .append(": ").append(n.titulo);
            if (avisosGuardados.contains(String.valueOf(n.id))) sb.append(" | El usuario lo tiene guardado");
            if (n.publicacion != null) sb.append(" | Publicado: ").append(FechasUtil.diaLargo(n.publicacion));
            if (n.caducidad != null) sb.append(" | Válido hasta: ").append(FechasUtil.diaLargo(n.caducidad));
            if (n.enlace != null && !n.enlace.isEmpty()) sb.append(" | Enlace: ").append(n.enlace);
            sb.append("\n  ").append(n.contenido).append("\n");
        }

        sb.append("\n[CÓMO SE USAN ACTIVIDADES Y NOTICIAS EN LA APP]\n")
                .append("- Apuntarse a una actividad: en Actividades, deslizando la tarjeta a la derecha, con el botón «Me apunto», ")
                .append("o inclinando el móvil a la derecha. Para pasar, a la izquierda.\n")
                .append("- Tocando la tarjeta se ve el detalle: añadir al calendario, «Llévame» (el mapa te lleva al sitio) y más información.\n")
                .append("- El botón «Escanear» lee el QR de los carteles de actividades de la escuela.\n")
                .append("- Inclinando el móvil hacia uno mismo, la actividad se guarda para luego sin apuntarse (pestaña «Guardadas»).\n")
                .append("- Deslizando la ficha hacia arriba se le da la vuelta y por detrás sale un QR grande de la actividad: otra persona lo escanea ")
                .append("con su app (Actividades → Escanear) y le sale para apuntarse. También con el botón «Enseñar QR» del detalle.\n")
                .append("- El botón «Grupos» enseña las comunidades de chat del grado y de la escuela.\n")
                .append("- Media hora antes de cada actividad a la que estás apuntado llega una notificación con el botón «Llévame».\n")
                .append("- En Noticias se pasa de aviso deslizando, inclinando el móvil (izquierda: siguiente; derecha: anterior) ")
                .append("o pasando la mano por delante de la parte de arriba del móvil, sin tocar la pantalla.\n")
                .append("- En Noticias, barriendo hacia abajo con dos dedos se marcan todos los avisos como leídos.\n")
                .append("- En Noticias, inclinando el móvil hacia uno mismo o tocando el marcapáginas, el aviso se guarda (pestaña «Guardados»).\n");
        return sb.toString();
    }

    private static String ects(double c) {
        return c == Math.floor(c) ? String.valueOf((int) c) : String.valueOf(c);
    }
}
