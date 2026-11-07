package com.example.nuietsiit.activities.secciones.biblioteca;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

/**
 * Dibuja las baldas de madera bajo cada fila de libros y sigue con baldas vacías hasta llenar la pantalla.
 */
public class BaldaDecoration extends RecyclerView.ItemDecoration {

    private final int grosor, sombra, hueco;
    private int altoFila, primeraBalda = -1; // se conservan para pintar baldas aunque un filtro lo vacíe todo
    private final Paint madera = new Paint();
    private final Paint cantoOscuro = new Paint();
    private final Paint pinturaSombra = new Paint();

    public BaldaDecoration(Context contexto) {
        float d = contexto.getResources().getDisplayMetrics().density;
        grosor = (int) (12 * d);
        sombra = (int) (6 * d);
        hueco = (int) (16 * d);
        altoFila = (int) (220 * d);
        madera.setColor(0xFFA9713D);
        cantoOscuro.setColor(0xFF7C4F27);
        pinturaSombra.setColor(0x33000000);
    }

    @Override
    public void getItemOffsets(@NonNull Rect fuera, @NonNull View vista, @NonNull RecyclerView padre,
                               @NonNull RecyclerView.State estado) {
        fuera.set(0, hueco, 0, grosor + sombra);
    }

    @Override
    public void onDraw(@NonNull Canvas lienzo, @NonNull RecyclerView padre, @NonNull RecyclerView.State estado) {
        int y = primeraBalda;
        View hijo = hijoEnSuFila(padre);
        if (hijo != null) {
            altoFila = padre.getLayoutManager().getDecoratedMeasuredHeight(hijo);
            y = hijo.getBottom();
        }
        if (y < 0 && hijo == null) y = padre.getPaddingTop() + altoFila - grosor - sombra;
        if (altoFila <= 0) return;
        primeraBalda = y;

        // subir hasta la primera balda visible y bajar hasta el final
        while (y - altoFila + grosor + sombra > 0) y -= altoFila;
        for (; y < padre.getHeight(); y += altoFila) {
            lienzo.drawRect(0, y, padre.getWidth(), y + grosor, madera);
            lienzo.drawRect(0, y + grosor - grosor / 4f, padre.getWidth(), y + grosor, cantoOscuro);
            lienzo.drawRect(0, y + grosor, padre.getWidth(), y + grosor + sombra, pinturaSombra);
        }
    }

    // cualquier hijo sirve: todas las filas miden lo mismo (portadas con la misma proporción);
    // menos los que un filtro está quitando, que siguen en su posición antigua mientras se animan
    private static View hijoEnSuFila(RecyclerView padre) {
        for (int i = 0; i < padre.getChildCount(); i++) {
            View hijo = padre.getChildAt(i);
            if (!((RecyclerView.LayoutParams) hijo.getLayoutParams()).isItemRemoved()) return hijo;
        }
        return null;
    }
}
