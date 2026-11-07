package com.example.nuietsiit.util;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.LruCache;
import android.widget.ImageView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Portadas reales por ISBN (Open Library), con caché en memoria y en disco.
 * Sin red o sin portada, el ImageView se queda con lo que ya tuviera puesto.
 */
public final class CargadorPortadas {

    private static final String URL_PORTADA = "https://covers.openlibrary.org/b/isbn/%s-L.jpg?default=false";
    private static final int TIEMPO_MAXIMO_MS = 8000;
    private static final long ESPERA_TRAS_FALLO_MS = 60_000; // sin portada o sin red: no reintentar en cada scroll
    private static final int ANCHO_MAXIMO_PX = 600;          // de sobra para la balda, ojear y el detalle

    private static final ExecutorService HILOS = Executors.newFixedThreadPool(3);
    private static final Handler PRINCIPAL = new Handler(Looper.getMainLooper());
    private static final LruCache<String, Bitmap> MEMORIA = new LruCache<String, Bitmap>(32 * 1024 * 1024) {
        @Override
        protected int sizeOf(String isbn, Bitmap portada) {
            return portada.getAllocationByteCount();
        }
    };

    // solo se usan en el hilo principal
    // una descarga por ISBN; WeakReference para no retener las vistas de una pantalla ya cerrada
    private static final Map<String, List<WeakReference<ImageView>>> EN_CURSO = new HashMap<>();
    private static final Map<String, Long> ULTIMO_FALLO = new HashMap<>();

    private CargadorPortadas() {}

    public static void cargar(ImageView destino, String isbn) {
        destino.setTag(isbn); // al reciclar la vista, una descarga antigua no debe pisar la portada nueva
        Bitmap enMemoria = MEMORIA.get(isbn);
        if (enMemoria != null) {
            destino.setImageBitmap(enMemoria);
            return;
        }
        List<WeakReference<ImageView>> esperando = EN_CURSO.get(isbn);
        if (esperando != null) {
            esperando.add(new WeakReference<>(destino));
            return;
        }
        Long fallo = ULTIMO_FALLO.get(isbn);
        if (fallo != null && SystemClock.elapsedRealtime() - fallo < ESPERA_TRAS_FALLO_MS) return;

        esperando = new ArrayList<>();
        esperando.add(new WeakReference<>(destino));
        EN_CURSO.put(isbn, esperando);
        File carpeta = new File(destino.getContext().getCacheDir(), "portadas");
        HILOS.execute(() -> {
            Bitmap portada = leerSinFallar(carpeta, isbn);
            if (portada != null) MEMORIA.put(isbn, portada);
            PRINCIPAL.post(() -> terminar(isbn, portada));
        });
    }

    private static void terminar(String isbn, Bitmap portada) {
        List<WeakReference<ImageView>> esperando = EN_CURSO.remove(isbn);
        if (portada == null) {
            ULTIMO_FALLO.put(isbn, SystemClock.elapsedRealtime());
            return;
        }
        ULTIMO_FALLO.remove(isbn);
        for (WeakReference<ImageView> referencia : esperando) {
            ImageView vista = referencia.get();
            if (vista != null && isbn.equals(vista.getTag())) vista.setImageBitmap(portada);
        }
    }

    // cualquier fallo cuenta como "sin portada": si no, ese ISBN se quedaría en EN_CURSO para siempre
    private static Bitmap leerSinFallar(File carpeta, String isbn) {
        try {
            return leerODescargar(carpeta, isbn);
        } catch (RuntimeException | OutOfMemoryError e) {
            return null;
        }
    }

    private static Bitmap leerODescargar(File carpeta, String isbn) {
        File fichero = new File(carpeta, isbn + ".jpg");
        if (!fichero.exists() && !descargar(isbn, carpeta, fichero)) return null;
        Bitmap portada = decodificarReducida(fichero);
        if (portada == null) fichero.delete();
        return portada;
    }

    private static Bitmap decodificarReducida(File fichero) {
        BitmapFactory.Options opciones = new BitmapFactory.Options();
        opciones.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(fichero.getPath(), opciones);
        int reduccion = 1;
        while (opciones.outWidth / reduccion > ANCHO_MAXIMO_PX) reduccion *= 2;

        opciones.inJustDecodeBounds = false;
        opciones.inSampleSize = reduccion;
        opciones.inPreferredConfig = Bitmap.Config.RGB_565; // las portadas no tienen transparencia: ocupan la mitad
        return BitmapFactory.decodeFile(fichero.getPath(), opciones);
    }

    private static boolean descargar(String isbn, File carpeta, File fichero) {
        carpeta.mkdirs();
        File temporal = new File(carpeta, isbn + ".tmp"); // sin ficheros a medias si se corta la conexión
        HttpURLConnection conexion = null;
        try {
            conexion = (HttpURLConnection) new URL(String.format(URL_PORTADA, isbn)).openConnection();
            conexion.setConnectTimeout(TIEMPO_MAXIMO_MS);
            conexion.setReadTimeout(TIEMPO_MAXIMO_MS);
            if (conexion.getResponseCode() != HttpURLConnection.HTTP_OK) return false;
            try (InputStream entrada = conexion.getInputStream();
                 FileOutputStream salida = new FileOutputStream(temporal)) {
                byte[] bloque = new byte[8192];
                int leidos;
                while ((leidos = entrada.read(bloque)) != -1) salida.write(bloque, 0, leidos);
            }
            return temporal.renameTo(fichero);
        } catch (IOException e) {
            temporal.delete();
            return false;
        } finally {
            if (conexion != null) conexion.disconnect();
        }
    }
}
