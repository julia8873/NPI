package com.example.nuietsiit.activities.secciones.biblioteca;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.ReaderException;
import com.google.zxing.Result;
import com.google.zxing.ResultMetadataType;
import com.google.zxing.ResultPoint;
import com.google.zxing.multi.GenericMultipleBarcodeReader;
import com.journeyapps.barcodescanner.Decoder;
import com.journeyapps.barcodescanner.DecoderFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * zxing devuelve un solo código por fotograma; este decodificador busca todos los EAN-13 que haya.
 * BarcodeView solo entrega un resultado, así que van juntos en uno: los puntos de todos seguidos
 * (para que BarcodeView los pase a coordenadas de la vista) y los resultados sueltos en
 * {@link ResultMetadataType#OTHER}. Ver {@link #separar}.
 *
 * Un fotograma de cada dos se busca un solo código con TRY_HARDER (lee el código girado, como
 * cuando el libro está tumbado): es lento si no hay nada, y en la búsqueda de varios se repetiría
 * en cada trozo de la imagen.
 */
public class DecodificadorVarios extends Decoder {

    private final Map<DecodeHintType, Object> pistasRapidas = new EnumMap<>(DecodeHintType.class);
    private final MultiFormatReader lectorGirados = new MultiFormatReader();
    private final GenericMultipleBarcodeReader lectorVarios =
            new GenericMultipleBarcodeReader(new MultiFormatReader());
    private boolean tocaGirados;

    public static DecoderFactory fabrica() {
        return pistasBase -> new DecodificadorVarios();
    }

    private DecodificadorVarios() {
        super(new MultiFormatReader());
        pistasRapidas.put(DecodeHintType.POSSIBLE_FORMATS, Collections.singletonList(BarcodeFormat.EAN_13));
        Map<DecodeHintType, Object> pistasGirados = new EnumMap<>(pistasRapidas);
        pistasGirados.put(DecodeHintType.TRY_HARDER, true);
        lectorGirados.setHints(pistasGirados);
    }

    @Override
    protected Result decode(BinaryBitmap imagen) {
        tocaGirados = !tocaGirados;
        try {
            Result[] leidos = tocaGirados
                    ? new Result[]{lectorGirados.decodeWithState(imagen)}
                    : lectorVarios.decodeMultiple(imagen, pistasRapidas);
            return juntar(leidos);
        } catch (ReaderException e) {
            return null; // no hay ningún código en este fotograma
        } finally {
            lectorGirados.reset();
        }
    }

    private static Result juntar(Result[] leidos) {
        List<ResultPoint> puntos = new ArrayList<>();
        for (Result leido : leidos) {
            if (leido.getResultPoints() != null) Collections.addAll(puntos, leido.getResultPoints());
        }
        Result junto = new Result(leidos[0].getText(), leidos[0].getRawBytes(),
                puntos.toArray(new ResultPoint[0]), leidos[0].getBarcodeFormat());
        junto.putMetadata(ResultMetadataType.OTHER, leidos);
        return junto;
    }

    /** Cada código leído (su texto) con sus puntos de {@code BarcodeResult.getTransformedResultPoints()}. */
    public static Map<String, List<ResultPoint>> separar(Result junto, List<ResultPoint> puntosVista) {
        Map<String, List<ResultPoint>> codigos = new LinkedHashMap<>();
        Object otros = junto.getResultMetadata() == null ? null
                : junto.getResultMetadata().get(ResultMetadataType.OTHER);
        Result[] leidos = otros instanceof Result[] ? (Result[]) otros : new Result[]{junto};
        int desde = 0;
        for (Result leido : leidos) {
            int n = leido.getResultPoints() == null ? 0 : leido.getResultPoints().length;
            int hasta = Math.min(desde + n, puntosVista.size());
            codigos.put(leido.getText(), puntosVista.subList(desde, hasta));
            desde = hasta;
        }
        return codigos;
    }
}
