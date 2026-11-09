package com.example.nuietsiit.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.text.Layout;
import android.util.AttributeSet;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.example.nuietsiit.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Sustituto de ScrollView sin scroll: si el contenido no cabe, se reparte en hojas que se pasan enteras,
 * deslizando de lado o inclinando el móvil (como los pasos de un trámite). Las hojas se cortan entre
 * secciones (hijos del contenido, o sus hijos si una sección sola no cabe) y, si no hay otra, entre líneas
 * de texto. Abajo, rombos de azulejo dicen en qué hoja estás; tocar a izquierda o derecha también pasa.
 * Como un ScrollView: un solo hijo, y las Activities lo usan igual (findViewById de lo de dentro).
 */
public class HojasPaginadas extends FrameLayout {

    public interface OnHoja {
        void alCambiar(int hoja, int hojas);
    }

    private final List<Integer> cortes = new ArrayList<>(); // dónde empieza cada hoja (px del contenido)
    private int hoja = 0;
    private int altoContenido;
    private OnHoja alCambiarHoja;

    private final int altoIndicador, tamRombo, umbralToque;
    private final Paint pintura = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pinturaFlecha = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path rombo = new Path();
    private final InclinarParaPasar inclinar;

    private float xInicio, yInicio;
    private boolean arrastrando, toqueEnPie;
    private View focoAnterior;
    private boolean permitirInclinar = true;

    public HojasPaginadas(Context context, AttributeSet attrs) {
        super(context, attrs);
        float d = getResources().getDisplayMetrics().density;
        altoIndicador = Math.round(32 * d);
        tamRombo = getResources().getDimensionPixelSize(R.dimen.nui_espacio_s);
        umbralToque = ViewConfiguration.get(context).getScaledTouchSlop();
        pinturaFlecha.setTextSize(22 * getResources().getDisplayMetrics().scaledDensity);
        pinturaFlecha.setTextAlign(Paint.Align.CENTER);
        pinturaFlecha.setFakeBoldText(true);
        setWillNotDraw(false);
        inclinar = new InclinarParaPasar(this, this::pasar, () -> permitirInclinar && cortes.size() > 1);
    }

    public void setOnHoja(OnHoja listener) {
        alCambiarHoja = listener;
    }

    /** false en pantallas donde girar el móvil ya significa otra cosa (enseñar el justificante). */
    public void setPermitirInclinar(boolean si) {
        permitirInclinar = si;
        inclinar.actualizar();
    }

    public int getHoja() {
        return hoja;
    }

    public int getHojas() {
        return Math.max(1, cortes.size());
    }

    /** Pasa a la hoja siguiente (+1) o a la anterior (-1). */
    public void pasar(int sentido) {
        View contenido = getContenido();
        int nueva = hoja + sentido;
        if (contenido == null || nueva < 0 || nueva >= cortes.size()) {
            if (contenido != null) contenido.animate().translationX(0).setDuration(150).start(); // tope
            return;
        }
        performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);
        float ancho = getWidth();
        contenido.animate().translationX(-sentido * ancho).alpha(0f).setDuration(140).withEndAction(() -> {
            irA(nueva, true);
            contenido.setTranslationX(sentido * ancho * 0.4f);
            contenido.animate().translationX(0).alpha(1f).setDuration(180).start();
        }).start();
    }

    public void irA(int nueva) {
        irA(nueva, true);
    }

    private void irA(int nueva, boolean anunciar) {
        if (cortes.isEmpty()) return;
        hoja = Math.max(0, Math.min(nueva, cortes.size() - 1));
        scrollTo(0, cortes.get(hoja));
        invalidate();
        if (anunciar && cortes.size() > 1) announceForAccessibility("Hoja " + (hoja + 1) + " de " + cortes.size());
        if (alCambiarHoja != null) alCambiarHoja.alCambiar(hoja, getHojas());
    }

    private View getContenido() {
        return getChildCount() > 0 ? getChildAt(0) : null;
    }

    // ------------------------------ medir y cortar ------------------------------

    // como ScrollView: el contenido se mide con toda la altura que necesite
    @Override
    protected void measureChildWithMargins(View hijo, int anchoSpec, int anchoUsado, int altoSpec, int altoUsado) {
        MarginLayoutParams lp = (MarginLayoutParams) hijo.getLayoutParams();
        int anchoHijo = getChildMeasureSpec(anchoSpec, getPaddingLeft() + getPaddingRight()
                + lp.leftMargin + lp.rightMargin + anchoUsado, lp.width);
        hijo.measure(anchoHijo, MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
    }

    @Override
    protected void onLayout(boolean cambiado, int l, int t, int r, int b) {
        super.onLayout(cambiado, l, t, r, b);
        View contenido = getContenido();
        if (contenido == null) return;
        altoContenido = contenido.getHeight();
        int visible = getHeight() - getPaddingTop() - getPaddingBottom();

        List<Integer> nuevos = new ArrayList<>();
        nuevos.add(0);
        if (altoContenido > visible && visible > altoIndicador * 3) {
            int alto = visible - altoIndicador;
            List<Integer> candidatos = new ArrayList<>();
            recogerCortes(contenido, 0, alto, candidatos);
            Collections.sort(candidatos);
            int inicio = 0;
            while (altoContenido - inicio > alto) {
                int corte = -1;
                for (int c : candidatos) if (c > inicio + alto / 4 && c <= inicio + alto) corte = c;
                if (corte < 0) corte = corteEntreLineas(contenido, inicio, inicio + alto);
                nuevos.add(corte);
                inicio = corte;
            }
        }
        boolean distintos = !nuevos.equals(cortes);
        cortes.clear();
        cortes.addAll(nuevos);
        inclinar.actualizar();

        // si se enfoca un campo (teclado), se va a su hoja
        View foco = findFocus();
        if (foco != null && foco != focoAnterior) {
            focoAnterior = foco;
            irA(hojaDe(foco), false);
        } else if (distintos || getScrollY() != cortes.get(Math.min(hoja, cortes.size() - 1))) {
            irA(hoja, false);
        }
    }

    // puntos de corte: el principio de cada sección; si una sección sola no cabe, también entre sus hijos
    private void recogerCortes(View v, int y, int alto, List<Integer> candidatos) {
        if (!(v instanceof ViewGroup)) return;
        ViewGroup grupo = (ViewGroup) v;
        View anterior = null;
        for (int i = 0; i < grupo.getChildCount(); i++) {
            View h = grupo.getChildAt(i);
            if (h.getVisibility() == GONE) continue;
            int arriba = y + h.getTop();
            // nunca justo después de un título: el título se va a la hoja de lo que encabeza
            if (anterior != null && !esTitulo(anterior)) {
                ViewGroup.LayoutParams lp = h.getLayoutParams();
                int margen = lp instanceof MarginLayoutParams ? ((MarginLayoutParams) lp).topMargin : 0;
                candidatos.add(Math.max(y, arriba - margen));
            }
            // las listas largas (más de un tercio de hoja) también se cortan entre sus filas; una tarjeta
            // (con fondo) solo si no cabe entera en una hoja
            if (h.getHeight() > alto || (h.getHeight() > alto / 3 && h.getBackground() == null)) {
                recogerCortes(h, arriba, alto, candidatos);
            }
            anterior = h;
        }
    }

    private static boolean esTitulo(View v) {
        return v instanceof TextView && v.isAccessibilityHeading();
    }

    // sin sección que cortar: se corta entre dos líneas del texto que queda en el borde
    private int corteEntreLineas(View contenido, int inicio, int limite) {
        int[] encontrado = {limite};
        buscarTexto(contenido, 0, inicio, limite, encontrado);
        return encontrado[0];
    }

    // arriba: dónde empieza v, en px del contenido
    private void buscarTexto(View v, int arriba, int inicio, int limite, int[] encontrado) {
        if (arriba > limite || arriba + v.getHeight() < limite) return;
        if (v instanceof TextView && ((TextView) v).getLayout() != null) {
            TextView tv = (TextView) v;
            Layout layout = tv.getLayout();
            int yTexto = limite - arriba - tv.getTotalPaddingTop();
            int linea = layout.getLineForVertical(yTexto);
            int corte = arriba + tv.getTotalPaddingTop() + layout.getLineTop(linea);
            if (corte > inicio) encontrado[0] = corte;
            return;
        }
        if (v instanceof ViewGroup) {
            ViewGroup g = (ViewGroup) v;
            for (int i = 0; i < g.getChildCount(); i++) {
                View h = g.getChildAt(i);
                if (h.getVisibility() != GONE) buscarTexto(h, arriba + h.getTop(), inicio, limite, encontrado);
            }
        }
    }

    private int hojaDe(View descendiente) {
        int y = 0;
        View v = descendiente;
        while (v != null && v != this) {
            y += v.getTop();
            v = v.getParent() instanceof View ? (View) v.getParent() : null;
        }
        y -= getPaddingTop();
        int h = 0;
        for (int i = 0; i < cortes.size(); i++) if (cortes.get(i) <= y) h = i;
        return h;
    }

    private int finHoja() {
        return hoja + 1 < cortes.size() ? cortes.get(hoja + 1) : altoContenido;
    }

    private int altoVisible() {
        return finHoja() - (cortes.isEmpty() ? 0 : cortes.get(hoja));
    }

    // ------------------------------ dibujar ------------------------------

    @Override
    protected void dispatchDraw(@NonNull Canvas canvas) {
        if (cortes.size() <= 1) {
            super.dispatchDraw(canvas);
            return;
        }
        // lo que sigue a la hoja no se ve: empieza en la siguiente
        canvas.save();
        int arriba = getScrollY() + getPaddingTop();
        canvas.clipRect(0, arriba, getWidth(), arriba + altoVisible());
        super.dispatchDraw(canvas);
        canvas.restore();
        pintarIndicador(canvas);
    }

    // rombos de azulejo como en los pasos: hecho = oro, actual = almagra y más grande, por venir = sombra
    private void pintarIndicador(Canvas canvas) {
        int n = cortes.size();
        float cy = getScrollY() + getHeight() - getPaddingBottom() - altoIndicador / 2f;
        float paso = tamRombo * 2.2f;
        float x0 = getWidth() / 2f - paso * (n - 1) / 2f;
        for (int i = 0; i < n; i++) {
            pintura.setColor(getContext().getColor(i == hoja ? R.color.nui_resaltado
                    : i < hoja ? R.color.nui_oro : R.color.nui_papel_sombra));
            float r = tamRombo * (i == hoja ? 0.85f : 0.6f);
            float cx = x0 + paso * i;
            rombo.reset();
            rombo.moveTo(cx, cy - r);
            rombo.lineTo(cx + r, cy);
            rombo.lineTo(cx, cy + r);
            rombo.lineTo(cx - r, cy);
            rombo.close();
            canvas.drawPath(rombo, pintura);
        }
        // flechas a los lados: se puede tocar para pasar
        float base = cy - (pinturaFlecha.descent() + pinturaFlecha.ascent()) / 2f;
        float margen = getPaddingLeft() + altoIndicador;
        pinturaFlecha.setColor(getContext().getColor(R.color.nui_acento_profundo));
        pinturaFlecha.setAlpha(hoja > 0 ? 255 : 60);
        canvas.drawText("‹", margen, base, pinturaFlecha);
        pinturaFlecha.setAlpha(hoja < n - 1 ? 255 : 60);
        canvas.drawText("›", getWidth() - getPaddingRight() - altoIndicador, base, pinturaFlecha);
    }

    // ------------------------------ tocar ------------------------------

    // lo tapado (el resto del contenido y el pie con los rombos) no recibe toques
    private boolean enZonaTapada(float y) {
        return cortes.size() > 1 && y > getPaddingTop() + altoVisible();
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent e) {
        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                xInicio = e.getX();
                yInicio = e.getY();
                arrastrando = false;
                toqueEnPie = enZonaTapada(e.getY());
                return toqueEnPie;
            case MotionEvent.ACTION_MOVE:
                float dx = e.getX() - xInicio, dy = e.getY() - yInicio;
                if (cortes.size() > 1 && Math.abs(dx) > umbralToque && Math.abs(dx) > Math.abs(dy) * 1.5f) {
                    arrastrando = true;
                    if (getParent() != null) getParent().requestDisallowInterceptTouchEvent(true);
                }
                break;
        }
        return arrastrando;
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        View contenido = getContenido();
        float dx = e.getX() - xInicio;
        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                return cortes.size() > 1;
            case MotionEvent.ACTION_MOVE:
                if (!arrastrando && Math.abs(dx) > umbralToque && Math.abs(dx) > Math.abs(e.getY() - yInicio)) {
                    arrastrando = true;
                }
                if (arrastrando && contenido != null) contenido.setTranslationX(dx * 0.6f);
                return true;
            case MotionEvent.ACTION_UP:
                if (arrastrando && Math.abs(dx) > getWidth() / 6f) {
                    pasar(dx < 0 ? 1 : -1);
                } else if (!arrastrando && toqueEnPie) {
                    pasar(e.getX() < getWidth() / 2f ? -1 : 1); // tocar a un lado del pie
                } else if (contenido != null) {
                    contenido.animate().translationX(0).setDuration(150).start();
                }
                arrastrando = false;
                return true;
            case MotionEvent.ACTION_CANCEL:
                if (contenido != null) contenido.animate().translationX(0).setDuration(150).start();
                arrastrando = false;
                return true;
        }
        return super.onTouchEvent(e);
    }

    // sin scroll de verdad: que nada lo desplace por su cuenta (el foco ya va a su hoja en onLayout)
    @Override
    public boolean requestChildRectangleOnScreen(@NonNull View hijo, android.graphics.Rect rect, boolean inmediato) {
        return false;
    }

    @Override
    public void requestChildFocus(View hijo, View enfocado) {
        super.requestChildFocus(hijo, enfocado);
        if (enfocado != focoAnterior && !cortes.isEmpty()) {
            focoAnterior = enfocado;
            irA(hojaDe(enfocado), false);
        }
    }
}
