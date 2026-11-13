package com.example.nuietsiit.activities.secciones.actividades;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.nuietsiit.R;
import com.example.nuietsiit.activities.auth.Login;
import com.example.nuietsiit.data.ActividadesDB;
import com.example.nuietsiit.data.ComunidadesUnidas;
import com.example.nuietsiit.data.DBHelper;
import com.example.nuietsiit.data.UsuariosDB;
import com.example.nuietsiit.data.modelo.Comunidad;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.util.List;
import java.util.Set;

/**
 * Comunidades de chat (tabla COMUNIDAD_CHAT).
 * 1) mostrar(): hoja con la lista de grupos del grado del usuario + los generales.
 * 2) Al pulsar uno: vista previa SIMULADA del grupo (no abre WhatsApp/Telegram/Discord de verdad).
 * 3) "Unirme" se guarda en el móvil (ComunidadesUnidas). Sin sesión iniciada no se puede unir.
 * Tarjetas blancas con el icono en el color de cada plataforma (colores_secciones.xml).
 */
public final class HojaComunidades {

    private HojaComunidades() {}

    /** correo = null si no hay sesión iniciada (se enseñan todas y no se puede unir). */
    public static void mostrar(AppCompatActivity act, String correo) {
        DBHelper bd = DBHelper.getInstance(act);
        UsuariosDB usuarios = new UsuariosDB(bd);
        Integer idGrado = correo != null ? usuarios.obtenerIdGradoUsuario(correo) : null;
        List<Comunidad> comunidades = new ActividadesDB(bd).obtenerComunidades(idGrado);

        BottomSheetDialog hoja = new BottomSheetDialog(act);
        View v = act.getLayoutInflater().inflate(R.layout.hoja_comunidades, null);
        hoja.setContentView(v);

        // Sin grado elegido (0 = "Ninguno") se enseñan todas
        TextView tvSubtitulo = v.findViewById(R.id.tvSubtituloComunidades);
        if (idGrado == null || idGrado == 0) {
            tvSubtitulo.setText(R.string.nui_com_todas);
        } else {
            tvSubtitulo.setText(act.getString(R.string.nui_com_grado_fmt, usuarios.obtenerGradoUsuario(correo)));
        }

        LinearLayout lista = v.findViewById(R.id.listaComunidades);
        pintarLista(act, lista, comunidades, correo);
        hoja.show();
    }

    // Crea una tarjeta (item_comunidad.xml) por cada comunidad
    private static void pintarLista(AppCompatActivity act, LinearLayout lista, List<Comunidad> comunidades,
                                    String correo) {
        lista.removeAllViews();
        Set<String> unidas = ComunidadesUnidas.unidas(act, correo);
        LayoutInflater inflater = act.getLayoutInflater();

        for (Comunidad c : comunidades) {
            View item = inflater.inflate(R.layout.item_comunidad, lista, false);

            ImageView ivIcono = item.findViewById(R.id.ivIconoComunidad);
            ivIcono.setImageTintList(ColorStateList.valueOf(act.getColor(Comunidad.colorRes(c.plataforma))));
            ivIcono.setBackgroundTintList(ColorStateList.valueOf(act.getColor(Comunidad.tinteRes(c.plataforma))));

            ((TextView) item.findViewById(R.id.tvNombreComunidad)).setText(c.nombre);
            ((TextView) item.findViewById(R.id.tvDescComunidad)).setText(c.descripcion != null ? c.descripcion : "");

            boolean unido = unidas.contains(String.valueOf(c.id));
            TextView tvPlataforma = item.findViewById(R.id.tvPlataforma);
            tvPlataforma.setText(unido ? act.getString(R.string.nui_com_unido) : Comunidad.nombrePlataforma(c.plataforma));
            tvPlataforma.setTextColor(act.getColor(unido ? R.color.sec_ok : Comunidad.colorRes(c.plataforma)));
            item.setContentDescription(c.nombre + ", " + tvPlataforma.getText());

            // al volver de la vista previa se repinta la lista (por si se ha unido)
            item.setOnClickListener(x -> mostrarGrupo(act, c, correo,
                    () -> pintarLista(act, lista, comunidades, correo)));
            lista.addView(item);
        }
    }

    // Vista previa simulada del grupo (vista_grupo.xml)
    private static void mostrarGrupo(AppCompatActivity act, Comunidad c, String correo, Runnable alCambiar) {
        boolean haySesion = correo != null;
        BottomSheetDialog hoja = new BottomSheetDialog(act);
        View v = act.getLayoutInflater().inflate(R.layout.vista_grupo, null);
        hoja.setContentView(v);

        int color = act.getColor(Comunidad.colorRes(c.plataforma));
        v.findViewById(R.id.cabeceraGrupo).setBackgroundColor(act.getColor(Comunidad.tinteRes(c.plataforma)));
        ((ImageView) v.findViewById(R.id.ivIconoGrupo)).setImageTintList(ColorStateList.valueOf(color));
        TextView tvPlataforma = v.findViewById(R.id.tvPlataformaGrupo);
        tvPlataforma.setText(Comunidad.nombrePlataforma(c.plataforma));
        tvPlataforma.setTextColor(color);
        ((TextView) v.findViewById(R.id.tvNombreGrupo)).setText(c.nombre);
        ((TextView) v.findViewById(R.id.tvMiembros)).setText(act.getString(R.string.nui_com_miembros_fmt, c.miembrosSimulados()));

        // Burbujas de mensajes de ejemplo
        LinearLayout mensajes = v.findViewById(R.id.listaMensajes);
        for (String[] m : c.mensajesSimulados()) {
            View burbuja = act.getLayoutInflater().inflate(R.layout.item_mensaje, mensajes, false);
            TextView tvAutor = burbuja.findViewById(R.id.tvAutor);
            tvAutor.setText(m[0]);
            tvAutor.setTextColor(color);
            ((TextView) burbuja.findViewById(R.id.tvMensaje)).setText(m[1]);
            mensajes.addView(burbuja);
        }

        Button btnUnirme = v.findViewById(R.id.btnUnirme);
        if (!haySesion) {
            btnUnirme.setText(R.string.nui_com_login);
            btnUnirme.setOnClickListener(x -> {
                hoja.dismiss();
                act.startActivity(new Intent(act, Login.class));
            });
            hoja.show();
            return;
        }
        actualizarBoton(act, btnUnirme, c, correo);
        btnUnirme.setOnClickListener(x -> {
            ComunidadesUnidas.alternar(act, correo, c.id);
            actualizarBoton(act, btnUnirme, c, correo);
            alCambiar.run();
            boolean unido = ComunidadesUnidas.estaUnido(act, correo, c.id);
            Toast.makeText(act, act.getString(unido ? R.string.nui_com_unido_fmt : R.string.nui_com_salido_fmt, c.nombre),
                    Toast.LENGTH_SHORT).show();
        });

        hoja.show();
    }

    private static void actualizarBoton(AppCompatActivity act, Button btn, Comunidad c, String correo) {
        boolean unido = ComunidadesUnidas.estaUnido(act, correo, c.id);
        btn.setText(unido ? R.string.nui_com_miembro_salir : R.string.nui_com_unirme);
    }
}
