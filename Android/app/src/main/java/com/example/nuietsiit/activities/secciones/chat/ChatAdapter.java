package com.example.nuietsiit.activities.secciones.chat;

import android.os.Build;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.nuietsiit.R;
import com.example.nuietsiit.data.modelo.MensajeChat;

import java.util.List;

public class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_USUARIO = 1;
    private static final int TYPE_ASISTENTE = 2;

    private final List<MensajeChat> listaMensajes;

    public ChatAdapter(List<MensajeChat> listaMensajes) {
        this.listaMensajes = listaMensajes;
    }

    @Override
    public int getItemViewType(int position) {
        MensajeChat mensaje = listaMensajes.get(position);
        return mensaje.isEsUsuario() ? TYPE_USUARIO : TYPE_ASISTENTE;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == TYPE_USUARIO) {
            View view = inflater.inflate(R.layout.item_mensaje_chat_usuario, parent, false);
            return new ViewHolderUsuario(view);
        } else {
            View view = inflater.inflate(R.layout.item_mensaje_chat_asistente, parent, false);
            return new ViewHolderAsistente(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        MensajeChat mensaje = listaMensajes.get(position);
        if (holder instanceof ViewHolderUsuario) {
            ((ViewHolderUsuario) holder).bind(mensaje);
        } else if (holder instanceof ViewHolderAsistente) {
            ((ViewHolderAsistente) holder).bind(mensaje);
        }
    }

    @Override
    public int getItemCount() {
        return listaMensajes.size();
    }

    public static CharSequence parsearMarkdown(String markdown) {
        if (markdown == null) return "";
        String html = markdown
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replaceAll("\\*\\*(.*?)\\*\\*", "<b>$1</b>")
                .replaceAll("\\*(.*?)\\*", "<i>$1</i>")
                .replace("\n", "<br/>");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return Html.fromHtml(html, Html.FROM_HTML_MODE_COMPACT);
        } else {
            return Html.fromHtml(html);
        }
    }

    // ViewHolder para mensajes del usuario
    static class ViewHolderUsuario extends RecyclerView.ViewHolder {
        private final TextView tvMensaje;
        private final TextView tvHora;

        public ViewHolderUsuario(@NonNull View itemView) {
            super(itemView);
            tvMensaje = itemView.findViewById(R.id.tvMensajeUsuario);
            tvHora = itemView.findViewById(R.id.tvHoraUsuario);
        }

        public void bind(MensajeChat mensaje) {
            tvMensaje.setText(mensaje.getTexto());
            tvHora.setText(mensaje.formatearHora());
        }
    }

    // ViewHolder para mensajes del asistente LLM
    static class ViewHolderAsistente extends RecyclerView.ViewHolder {
        private final TextView tvMensaje;
        private final TextView tvHora;
        private final LinearLayout llCargando;

        public ViewHolderAsistente(@NonNull View itemView) {
            super(itemView);
            tvMensaje = itemView.findViewById(R.id.tvMensajeAsistente);
            tvHora = itemView.findViewById(R.id.tvHoraAsistente);
            llCargando = itemView.findViewById(R.id.llCargando);
        }

        public void bind(MensajeChat mensaje) {
            if (mensaje.isCargando()) {
                llCargando.setVisibility(View.VISIBLE);
                tvMensaje.setVisibility(View.GONE);
                tvHora.setVisibility(View.GONE);
            } else {
                llCargando.setVisibility(View.GONE);
                tvMensaje.setVisibility(View.VISIBLE);
                tvHora.setVisibility(View.VISIBLE);
                tvMensaje.setText(parsearMarkdown(mensaje.getTexto()));
                tvHora.setText(mensaje.formatearHora());
            }
        }
    }
}
