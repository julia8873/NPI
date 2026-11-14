package com.example.nuietsiit.activities.secciones.biblioteca;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;

import androidx.annotation.NonNull;

import com.example.nuietsiit.R;
import com.example.nuietsiit.data.modelo.Libro;

/**
 * Portada dibujada con el título y el autor: se ve mientras llega la real o si no hay red.
 * Tela de los colores del design system NUI y título en la fuente de titulares (Zilla Slab).
 */
public class PortadaGenerada extends Drawable {

    // colores del estilo general de la app (todos con texto blanco legible encima)
    private static final int[] COLORES = {
            0xFFFF8C42, 0xFF1976D2, 0xFF2E7D32, 0xFF7B1FA2,
            0xFFD32F2F, 0xFF00897B, 0xFF455A64, 0xFFE06A1F};

    private final String titulo, autor;
    private final int color;
    private final Paint pintura = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final TextPaint pinturaTitulo = new TextPaint(Paint.ANTI_ALIAS_FLAG);
    private final TextPaint pinturaAutor = new TextPaint(Paint.ANTI_ALIAS_FLAG);
    private StaticLayout bloqueTitulo, bloqueAutor; // null si no caben; se recalculan solo al cambiar de tamaño
    private int xTexto, yTitulo, yAutor;

    public PortadaGenerada(Context ctx, Libro libro) {
        titulo = libro.titulo;
        autor = libro.autores.split(";")[0]; // solo el primero
        color = (COLORES[Math.floorMod(libro.isbn.hashCode(), COLORES.length)]);
        int texto = ctx.getColor(R.color.nui_sobre_resaltado);
        pinturaTitulo.setColor(texto);
        pinturaTitulo.setTypeface(Typeface.DEFAULT_BOLD);
        pinturaAutor.setColor(texto);
    }

    @Override
    protected void onBoundsChange(@NonNull Rect r) {
        int lomo = r.width() / 12;
        int margen = r.width() / 10;
        int ancho = r.width() - lomo - 2 * margen;

        if (ancho <= 0) {
            bloqueTitulo = null;
            bloqueAutor = null;
            return;
        }
        pinturaTitulo.setTextSize(r.width() / 9f);
        pinturaAutor.setTextSize(r.width() / 13f);
        bloqueTitulo = crearBloque(titulo, pinturaTitulo, ancho, 6);
        bloqueAutor = crearBloque(autor, pinturaAutor, ancho, 2);
        xTexto = r.left + lomo + margen;
        yTitulo = r.top + r.height() / 8;
        yAutor = yTitulo + bloqueTitulo.getHeight() + margen;
    }

    @Override
    public void draw(@NonNull Canvas lienzo) {
        Rect r = getBounds();
        pintura.setColor(color);
        lienzo.drawRect(r, pintura);
        // lomo: banda oscura a la izquierda
        int lomo = r.width() / 12;
        pintura.setColor(0x33000000);
        lienzo.drawRect(r.left, r.top, r.left + lomo, r.bottom, pintura);

        if (bloqueTitulo == null) return;
        dibujarBloque(lienzo, bloqueTitulo, yTitulo);
        dibujarBloque(lienzo, bloqueAutor, yAutor);
    }

    private void dibujarBloque(Canvas lienzo, StaticLayout bloque, int y) {
        lienzo.save();
        lienzo.translate(xTexto, y);
        bloque.draw(lienzo);
        lienzo.restore();
    }

    private static StaticLayout crearBloque(String texto, TextPaint pintura, int ancho, int maxLineas) {
        return StaticLayout.Builder.obtain(texto, 0, texto.length(), pintura, ancho)
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setEllipsize(android.text.TextUtils.TruncateAt.END)
                .setMaxLines(maxLineas)
                .build();
    }

    @Override public void setAlpha(int alpha) { pintura.setAlpha(alpha); }
    @Override public void setColorFilter(ColorFilter filtro) { pintura.setColorFilter(filtro); }
    @Override public int getOpacity() { return PixelFormat.OPAQUE; }
}