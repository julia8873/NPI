package com.example.nuietsiit.activities.secciones.actividades;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
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
 */
public final class HojaComunidades {

    private HojaComunidades() {}

    private static final int VERDE = 0xFF2E7D32;
    private static final int GRIS = 0xFF9E9E9E;

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
            tvSubtitulo.setText("Todas las comunidades de la ETSIIT");
        } else {
            tvSubtitulo.setText("De tu grado (" + usuarios.obtenerGradoUsuario(correo) + ") y generales de la escuela");
        }

        LinearLayout lista = v.findViewById(R.id.listaComunidades);
        pintarLista(act, lista, comunidades, correo);
        hoja.show();
    }

    // Crea una tarjeta (item_comunidad.xml) por cada comunidad
    private static void pintarLista(AppCompatActivity act, LinearLayout lista, List<Comunidad> comunidades,
                                    String correo) {
        lista.removeAllViews();
        Set<String> unidas = ComunidadesUnidas.unidas(act);
        LayoutInflater inflater = act.getLayoutInflater();

        for (Comunidad c : comunidades) {
            View item = inflater.inflate(R.layout.item_comunidad, lista, false);

            TextView tvIcono = item.findViewById(R.id.tvIconoComunidad);
            tvIcono.setText(Comunidad.emoji(c.plataforma));
            tvIcono.setBackgroundTintList(ColorStateList.valueOf(Comunidad.color(c.plataforma)));

            ((TextView) item.findViewById(R.id.tvNombreComunidad)).setText(c.nombre);
            ((TextView) item.findViewById(R.id.tvDescComunidad)).setText(c.descripcion != null ? c.descripcion : "");

            boolean unido = unidas.contains(String.valueOf(c.id));
            TextView tvPlataforma = item.findViewById(R.id.tvPlataforma);
            tvPlataforma.setText(unido ? "✓ Unido" : Comunidad.nombrePlataforma(c.plataforma));
            tvPlataforma.setTextColor(unido ? VERDE : Comunidad.color(c.plataforma));

            // al volver de la vista previa se repinta la lista (por si se ha unido)
            item.setOnClickListener(x -> mostrarGrupo(act, c, correo != null,
                    () -> pintarLista(act, lista, comunidades, correo)));
            lista.addView(item);
        }
    }

    // Vista previa simulada del grupo (vista_grupo.xml)
    private static void mostrarGrupo(AppCompatActivity act, Comunidad c, boolean haySesion, Runnable alCambiar) {
        BottomSheetDialog hoja = new BottomSheetDialog(act);
        View v = act.getLayoutInflater().inflate(R.layout.vista_grupo, null);
        hoja.setContentView(v);

        int color = Comunidad.color(c.plataforma);
        v.findViewById(R.id.cabeceraGrupo).setBackgroundColor(color);
        ((TextView) v.findViewById(R.id.tvPlataformaGrupo))
                .setText(Comunidad.emoji(c.plataforma) + " " + Comunidad.nombrePlataforma(c.plataforma));
        ((TextView) v.findViewById(R.id.tvNombreGrupo)).setText(c.nombre);
        ((TextView) v.findViewById(R.id.tvMiembros)).setText("👥 " + c.miembrosSimulados() + " miembros");

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
            btnUnirme.setText("🔒 Inicia sesión para unirte");
            btnUnirme.setBackgroundTintList(ColorStateList.valueOf(GRIS));
            btnUnirme.setOnClickListener(x -> {
                hoja.dismiss();
                act.startActivity(new Intent(act, Login.class));
            });
            hoja.show();
            return;
        }
        actualizarBoton(act, btnUnirme, c);
        btnUnirme.setOnClickListener(x -> {
            ComunidadesUnidas.alternar(act, c.id);
            actualizarBoton(act, btnUnirme, c);
            alCambiar.run();
            boolean unido = ComunidadesUnidas.estaUnido(act, c.id);
            Toast.makeText(act, unido ? "🎉 Te has unido a «" + c.nombre + "»"
                    : "Has salido de «" + c.nombre + "»", Toast.LENGTH_SHORT).show();
        });

        hoja.show();
    }

    private static void actualizarBoton(AppCompatActivity act, Button btn, Comunidad c) {
        boolean unido = ComunidadesUnidas.estaUnido(act, c.id);
        btn.setText(unido ? "✓ Ya eres miembro · Salir" : "Unirme al grupo");
        btn.setBackgroundTintList(ColorStateList.valueOf(unido ? GRIS : Comunidad.color(c.plataforma)));
    }
}
