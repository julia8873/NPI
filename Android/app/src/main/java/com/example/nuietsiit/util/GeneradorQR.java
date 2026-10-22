package com.example.nuietsiit.util;

import android.graphics.Bitmap;
import android.graphics.Color;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import java.util.EnumMap;
import java.util.Map;

/**
 * Genera el QR de un texto (código del justificante) como Bitmap en blanco y negro.
 */
public final class GeneradorQR {

    private GeneradorQR() {}

    // tamPx = lado en píxeles (generarlo al tamaño en que se muestra para que salga nítido).
    // null si no se puede codificar.
    public static Bitmap generar(String texto, int tamPx) {
        Map<EncodeHintType, Object> opciones = new EnumMap<>(EncodeHintType.class);
        opciones.put(EncodeHintType.MARGIN, 1);
        opciones.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
        opciones.put(EncodeHintType.CHARACTER_SET, "UTF-8");
        BitMatrix matriz;
        try {
            matriz = new QRCodeWriter().encode(texto, BarcodeFormat.QR_CODE, tamPx, tamPx, opciones);
        } catch (WriterException | IllegalArgumentException e) {
            return null;
        }

        int ancho = matriz.getWidth(), alto = matriz.getHeight();
        int[] pixeles = new int[ancho * alto];
        for (int y = 0; y < alto; y++) {
            for (int x = 0; x < ancho; x++) {
                pixeles[y * ancho + x] = matriz.get(x, y) ? Color.BLACK : Color.WHITE;
            }
        }
        return Bitmap.createBitmap(pixeles, ancho, alto, Bitmap.Config.ARGB_8888);
    }
}
