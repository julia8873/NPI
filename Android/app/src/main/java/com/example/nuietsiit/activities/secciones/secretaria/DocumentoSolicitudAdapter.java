package com.example.nuietsiit.activities.secciones.secretaria;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.pdf.PdfRenderer;
import android.media.ExifInterface;
import android.os.ParcelFileDescriptor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.nuietsiit.R;
import com.example.nuietsiit.data.modelo.Documento;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Documentos del paso 2 de la solicitud: cada uno con su miniatura y los botones "Hacer foto" y
 * "Subir archivo". Si ya tiene archivo, tocar la tarjeta abre las opciones (repetir, cambiar, quitar).
 */
public class DocumentoSolicitudAdapter extends RecyclerView.Adapter<DocumentoSolicitudAdapter.Holder> {

    public interface Acciones {
        void hacerFoto(Documento d);

        void subirArchivo(Documento d);

        void opciones(Documento d); // ya tiene archivo
    }

    private static final int LADO_MINIATURA_PX = 256;

    private final List<Documento> documentos;
    private final Map<Integer, String> archivos; // idDocumento → ruta (compartido con la Activity)
    private final Acciones acciones;
    private final Map<String, Bitmap> miniaturas = new HashMap<>(); // ruta → miniatura ya decodificada

    public DocumentoSolicitudAdapter(List<Documento> documentos, Map<Integer, String> archivos, Acciones acciones) {
        this.documentos = documentos;
        this.archivos = archivos;
        this.acciones = acciones;
    }

    // llamar tras cambiar el archivo de un documento; rutaAnterior = la que se ha sustituido o quitado
    public void archivoCambiado(int idDocumento, String rutaAnterior) {
        if (rutaAnterior != null) miniaturas.remove(rutaAnterior);
        for (int i = 0; i < documentos.size(); i++) {
            if (documentos.get(i).id == idDocumento) notifyItemChanged(i);
        }
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_documento_solicitud, parent, false);
        return new Holder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder h, int position) {
        Documento d = documentos.get(position);
        String ruta = archivos.get(d.id);
        boolean tiene = ruta != null;

        h.tvNombre.setText(d.nombre);
        String detalle = d.obligatorio ? "Obligatorio" : "Opcional";
        if (d.descripcion != null && !d.descripcion.isEmpty()) detalle += " · " + d.descripcion;
        h.tvDetalle.setText(detalle);

        if (tiene) {
            h.tvEstado.setText(esPdf(ruta) ? "✓ PDF añadido · toca para cambiarlo" : "✓ Añadido · toca para cambiarlo");
            h.tvEstado.setTextColor(Color.parseColor("#2E7D32"));
        } else {
            h.tvEstado.setText(d.obligatorio ? "Falta este documento" : "Puedes añadirlo si quieres");
            h.tvEstado.setTextColor(Color.parseColor(d.obligatorio ? "#D32F2F" : "#666666"));
        }

        Bitmap miniatura = tiene ? miniatura(ruta) : null;
        h.ivMiniatura.setImageBitmap(miniatura);
        h.tvSinArchivo.setVisibility(miniatura == null ? View.VISIBLE : View.GONE);

        h.botones.setVisibility(tiene ? View.GONE : View.VISIBLE);
        h.btnFoto.setOnClickListener(v -> acciones.hacerFoto(d));
        h.btnArchivo.setOnClickListener(v -> acciones.subirArchivo(d));
        h.itemView.setClickable(tiene);
        h.itemView.setOnClickListener(tiene ? v -> acciones.opciones(d) : null);
        h.itemView.setContentDescription(d.nombre + ", " + detalle + ". " + h.tvEstado.getText());
    }

    @Override
    public int getItemCount() {
        return documentos.size();
    }

    private Bitmap miniatura(String ruta) {
        if (!miniaturas.containsKey(ruta)) {
            miniaturas.put(ruta, esPdf(ruta) ? miniaturaPdf(ruta) : miniaturaImagen(ruta));
        }
        return miniaturas.get(ruta);
    }

    static boolean esPdf(String ruta) {
        return ruta.toLowerCase(Locale.ROOT).endsWith(".pdf");
    }

    // decodifica a ~256 px con inSampleSize (una foto de la cámara ocupa decenas de MB en memoria)
    // y la gira según el EXIF, porque muchas cámaras guardan la foto "tumbada". null si falla
    private static Bitmap miniaturaImagen(String ruta) {
        BitmapFactory.Options opciones = new BitmapFactory.Options();
        opciones.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(ruta, opciones);
        if (opciones.outWidth <= 0 || opciones.outHeight <= 0) return null;

        int muestra = 1;
        while (Math.min(opciones.outWidth, opciones.outHeight) / (muestra * 2) >= LADO_MINIATURA_PX) muestra *= 2;
        opciones.inJustDecodeBounds = false;
        opciones.inSampleSize = muestra;
        Bitmap bitmap = BitmapFactory.decodeFile(ruta, opciones);
        if (bitmap == null) return null;

        int grados = 0;
        try {
            int orientacion = new ExifInterface(ruta).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
            if (orientacion == ExifInterface.ORIENTATION_ROTATE_90) grados = 90;
            else if (orientacion == ExifInterface.ORIENTATION_ROTATE_180) grados = 180;
            else if (orientacion == ExifInterface.ORIENTATION_ROTATE_270) grados = 270;
        } catch (IOException ignored) {
            // sin EXIF: se deja como está
        }
        if (grados == 0) return bitmap;
        Matrix giro = new Matrix();
        giro.postRotate(grados);
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), giro, true);
    }

    // primera página del PDF sobre fondo blanco. null si falla
    private static Bitmap miniaturaPdf(String ruta) {
        try (ParcelFileDescriptor fd = ParcelFileDescriptor.open(new File(ruta), ParcelFileDescriptor.MODE_READ_ONLY);
             PdfRenderer pdf = new PdfRenderer(fd)) {
            if (pdf.getPageCount() == 0) return null;
            try (PdfRenderer.Page pagina = pdf.openPage(0)) {
                float escala = (float) LADO_MINIATURA_PX / Math.max(pagina.getWidth(), pagina.getHeight());
                Bitmap bitmap = Bitmap.createBitmap(Math.max(1, (int) (pagina.getWidth() * escala)),
                        Math.max(1, (int) (pagina.getHeight() * escala)), Bitmap.Config.ARGB_8888);
                bitmap.eraseColor(Color.WHITE);
                pagina.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
                return bitmap;
            }
        } catch (IOException | RuntimeException e) { // PDF dañado o protegido con contraseña
            return null;
        }
    }

    static class Holder extends RecyclerView.ViewHolder {
        final TextView tvNombre, tvDetalle, tvEstado, tvSinArchivo;
        final ImageView ivMiniatura;
        final View botones, btnFoto, btnArchivo;

        Holder(View v) {
            super(v);
            tvNombre = v.findViewById(R.id.tvNombre);
            tvDetalle = v.findViewById(R.id.tvDetalle);
            tvEstado = v.findViewById(R.id.tvEstado);
            tvSinArchivo = v.findViewById(R.id.tvSinArchivo);
            ivMiniatura = v.findViewById(R.id.ivMiniatura);
            botones = v.findViewById(R.id.botones);
            btnFoto = v.findViewById(R.id.btnFoto);
            btnArchivo = v.findViewById(R.id.btnArchivo);
        }
    }
}
