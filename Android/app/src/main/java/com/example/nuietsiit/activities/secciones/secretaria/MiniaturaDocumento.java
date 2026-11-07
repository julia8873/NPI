package com.example.nuietsiit.activities.secciones.secretaria;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.pdf.PdfRenderer;
import android.media.ExifInterface;
import android.os.ParcelFileDescriptor;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Miniaturas de los documentos de una solicitud (foto o primera página del PDF), con caché por ruta.
 */
public class MiniaturaDocumento {

    private static final int LADO_MINIATURA_PX = 256;
    private static final int LADO_GRANDE_PX = 1600; // para verlo a pantalla completa

    private final Map<String, Bitmap> miniaturas = new HashMap<>(); // ruta → miniatura ya decodificada

    // null si no se puede leer
    public Bitmap de(String ruta) {
        if (!miniaturas.containsKey(ruta)) {
            miniaturas.put(ruta, decodificar(ruta, LADO_MINIATURA_PX));
        }
        return miniaturas.get(ruta);
    }

    // a tamaño de pantalla, sin caché (solo al abrir el visor). null si no se puede leer
    public static Bitmap grande(String ruta) {
        return decodificar(ruta, LADO_GRANDE_PX);
    }

    private static Bitmap decodificar(String ruta, int lado) {
        return esPdf(ruta) ? pdf(ruta, lado) : imagen(ruta, lado);
    }

    public void olvidar(String ruta) {
        miniaturas.remove(ruta);
    }

    public static boolean esPdf(String ruta) {
        return ruta.toLowerCase(Locale.ROOT).endsWith(".pdf");
    }

    // decodifica a ~lado px con inSampleSize (una foto de la cámara ocupa decenas de MB en memoria)
    // y la gira según el EXIF, porque muchas cámaras guardan la foto "tumbada". null si falla
    private static Bitmap imagen(String ruta, int lado) {
        BitmapFactory.Options opciones = new BitmapFactory.Options();
        opciones.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(ruta, opciones);
        if (opciones.outWidth <= 0 || opciones.outHeight <= 0) return null;

        int muestra = 1;
        while (Math.min(opciones.outWidth, opciones.outHeight) / (muestra * 2) >= lado) muestra *= 2;
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
    private static Bitmap pdf(String ruta, int lado) {
        try (ParcelFileDescriptor fd = ParcelFileDescriptor.open(new File(ruta), ParcelFileDescriptor.MODE_READ_ONLY);
             PdfRenderer pdf = new PdfRenderer(fd)) {
            if (pdf.getPageCount() == 0) return null;
            try (PdfRenderer.Page pagina = pdf.openPage(0)) {
                float escala = (float) lado / Math.max(pagina.getWidth(), pagina.getHeight());
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
}
