package com.example.nuietsiit.activities.secciones.calendario;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;

import com.example.nuietsiit.R;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public class CalendarGridView extends View {

    private Paint paintTexto;
    private Paint paintTextoDiaSemana;
    private Paint paintSeleccion;
    private Paint paintSeleccionHalo;
    private Paint paintHoyBorde;
    private Paint paintDot;
    private Paint paintHeader;
    private Paint paintBtnNavBg;
    private Paint paintFlecha;
    private Paint paintDivider;

    private String[] diasSemana;
    private Calendar calendarActual;
    private final Calendar hoyCalendar = Calendar.getInstance();

    private int selectedDay = -1;
    private Set<String> fechasConEventos = new HashSet<>();
    private OnDateSelectedListener listener;

    private final SimpleDateFormat sdfDb = new SimpleDateFormat("yyyy-MM-dd", Locale.ROOT);
    private final RectF rectBtnPrev = new RectF();
    private final RectF rectBtnNext = new RectF();

    public interface OnDateSelectedListener {
        void onDateSelected(String fechaYYYYMMDD);
    }

    public CalendarGridView(Context context) {
        super(context);
        init();
    }

    public CalendarGridView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public CalendarGridView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        diasSemana = getResources().getStringArray(R.array.calendario_dias_semana);

        calendarActual = Calendar.getInstance();
        selectedDay = calendarActual.get(Calendar.DAY_OF_MONTH);

        int colorNaranja = Color.parseColor("#FF8C42");
        int colorTinta = Color.parseColor("#0F172A");

        paintTexto = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintTexto.setColor(colorTinta);
        paintTexto.setTextAlign(Paint.Align.CENTER);
        paintTexto.setFakeBoldText(true);

        paintTextoDiaSemana = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintTextoDiaSemana.setColor(Color.parseColor("#94A3B8"));
        paintTextoDiaSemana.setTextAlign(Paint.Align.CENTER);
        paintTextoDiaSemana.setFakeBoldText(true);
        paintTextoDiaSemana.setLetterSpacing(0.05f);

        paintSeleccion = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintSeleccion.setColor(colorNaranja);
        paintSeleccion.setStyle(Paint.Style.FILL);

        paintSeleccionHalo = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintSeleccionHalo.setColor(Color.parseColor("#26FF8C42"));
        paintSeleccionHalo.setStyle(Paint.Style.FILL);

        paintHoyBorde = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintHoyBorde.setColor(colorNaranja);
        paintHoyBorde.setStyle(Paint.Style.STROKE);
        paintHoyBorde.setStrokeWidth(3.5f);

        paintDot = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintDot.setColor(colorNaranja);
        paintDot.setStyle(Paint.Style.FILL);

        paintHeader = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintHeader.setColor(colorTinta);
        paintHeader.setTextAlign(Paint.Align.CENTER);
        paintHeader.setFakeBoldText(true);

        paintBtnNavBg = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintBtnNavBg.setColor(Color.parseColor("#FFF3EB"));
        paintBtnNavBg.setStyle(Paint.Style.FILL);

        paintFlecha = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintFlecha.setColor(colorNaranja);
        paintFlecha.setTextAlign(Paint.Align.CENTER);
        paintFlecha.setFakeBoldText(true);

        paintDivider = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintDivider.setColor(Color.parseColor("#F1F5F9"));
        paintDivider.setStrokeWidth(2f);
    }

    public void setFechasConEventos(Set<String> fechas) {
        this.fechasConEventos = fechas != null ? fechas : new HashSet<>();
        invalidate();
    }

    public void setOnDateSelectedListener(OnDateSelectedListener listener) {
        this.listener = listener;
    }

    public void cambiarMes(int delta) {
        calendarActual.add(Calendar.MONTH, delta);
        if (esMesActual()) {
            selectedDay = hoyCalendar.get(Calendar.DAY_OF_MONTH);
        } else {
            selectedDay = 1;
        }
        calendarActual.set(Calendar.DAY_OF_MONTH, selectedDay);
        notificarSeleccion();
        invalidate();
    }

    private boolean esMesActual() {
        return calendarActual.get(Calendar.YEAR) == hoyCalendar.get(Calendar.YEAR)
                && calendarActual.get(Calendar.MONTH) == hoyCalendar.get(Calendar.MONTH);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = (int) (width * 1.06f);
        setMeasuredDimension(width, height);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float w = getWidth();
        float h = getHeight();
        float paddingH = w * 0.03f;
        float usableW = w - (paddingH * 2);

        float cellW = usableW / 7f;
        float headerH = h * 0.25f;
        float bodyH = h - headerH - (h * 0.02f);
        float rowH = bodyH / 6f;

        paintTexto.setTextSize(cellW * 0.35f);
        paintTextoDiaSemana.setTextSize(cellW * 0.25f);
        paintHeader.setTextSize(cellW * 0.40f);
        paintFlecha.setTextSize(cellW * 0.48f);

        // 1. Título del Mes y Año
        Locale localeActivo = getResources().getConfiguration().getLocales().get(0);
        SimpleDateFormat sdf = new SimpleDateFormat("MMMM yyyy", localeActivo);
        String mesAno = sdf.format(calendarActual.getTime());
        if (!mesAno.isEmpty()) {
            mesAno = mesAno.substring(0, 1).toUpperCase(localeActivo) + mesAno.substring(1);
        }

        float titleCenterY = headerH * 0.36f;
        Paint.FontMetrics fmHeader = paintHeader.getFontMetrics();
        float titleBaselineY = titleCenterY - (fmHeader.ascent + fmHeader.descent) / 2f;
        canvas.drawText(mesAno, w / 2f, titleBaselineY, paintHeader);

        // 2. Botones circulares ‹ y ›
        float btnRadius = cellW * 0.36f;
        float btnPrevX = paddingH + cellW * 0.55f;
        float btnNextX = w - paddingH - cellW * 0.55f;

        rectBtnPrev.set(btnPrevX - btnRadius * 1.4f, titleCenterY - btnRadius * 1.4f,
                btnPrevX + btnRadius * 1.4f, titleCenterY + btnRadius * 1.4f);
        rectBtnNext.set(btnNextX - btnRadius * 1.4f, titleCenterY - btnRadius * 1.4f,
                btnNextX + btnRadius * 1.4f, titleCenterY + btnRadius * 1.4f);

        canvas.drawCircle(btnPrevX, titleCenterY, btnRadius, paintBtnNavBg);
        canvas.drawCircle(btnNextX, titleCenterY, btnRadius, paintBtnNavBg);

        Paint.FontMetrics fmFlecha = paintFlecha.getFontMetrics();
        float flechaBaselineY = titleCenterY - (fmFlecha.ascent + fmFlecha.descent) / 2f;
        canvas.drawText("‹", btnPrevX, flechaBaselineY, paintFlecha);
        canvas.drawText("›", btnNextX, flechaBaselineY, paintFlecha);

        // 3. Cabecera de días de la semana
        float diasSemanaY = headerH * 0.78f;
        for (int i = 0; i < 7; i++) {
            float cx = paddingH + (i + 0.5f) * cellW;
            paintTextoDiaSemana.setColor(i >= 5 ? Color.parseColor("#FF8C42") : Color.parseColor("#94A3B8"));
            canvas.drawText(diasSemana[i].toUpperCase(localeActivo), cx, diasSemanaY, paintTextoDiaSemana);
        }

        // Línea divisoria sutil
        canvas.drawLine(paddingH + cellW * 0.2f, headerH * 0.93f, w - paddingH - cellW * 0.2f, headerH * 0.93f, paintDivider);

        // 4. Celdas de los días
        Calendar calTemp = (Calendar) calendarActual.clone();
        calTemp.set(Calendar.DAY_OF_MONTH, 1);

        int primerDiaSemana = calTemp.get(Calendar.DAY_OF_WEEK);
        int offset = (primerDiaSemana + 5) % 7;
        int diasEnMes = calendarActual.getActualMaximum(Calendar.DAY_OF_MONTH);

        int year = calendarActual.get(Calendar.YEAR);
        int month = calendarActual.get(Calendar.MONTH);
        int hoyDia = hoyCalendar.get(Calendar.DAY_OF_MONTH);
        boolean mesEsHoy = esMesActual();

        float radioDia = Math.min(cellW, rowH) * 0.37f;
        Paint.FontMetrics fmTexto = paintTexto.getFontMetrics();

        for (int day = 1; day <= diasEnMes; day++) {
            int index = offset + day - 1;
            int col = index % 7;
            int row = index / 7;

            float cx = paddingH + (col + 0.5f) * cellW;
            float cy = headerH + (row + 0.5f) * rowH;

            calTemp.set(year, month, day);
            String fechaStr = sdfDb.format(calTemp.getTime());

            boolean isSelected = (day == selectedDay);
            boolean isToday = (mesEsHoy && day == hoyDia);
            boolean hasEvent = fechasConEventos.contains(fechaStr);

            if (isSelected) {
                canvas.drawCircle(cx, cy, radioDia * 1.18f, paintSeleccionHalo);
                canvas.drawCircle(cx, cy, radioDia, paintSeleccion);
                paintTexto.setColor(Color.WHITE);
            } else {
                if (isToday) {
                    canvas.drawCircle(cx, cy, radioDia, paintHoyBorde);
                    paintTexto.setColor(Color.parseColor("#FF8C42"));
                } else if (col >= 5) {
                    paintTexto.setColor(Color.parseColor("#64748B"));
                } else {
                    paintTexto.setColor(Color.parseColor("#0F172A"));
                }
            }

            float textY = cy - (fmTexto.ascent + fmTexto.descent) / 2f;
            canvas.drawText(String.valueOf(day), cx, textY, paintTexto);

            if (hasEvent) {
                paintDot.setColor(isSelected ? Color.WHITE : Color.parseColor("#FF8C42"));
                float dotY = cy + radioDia * 0.65f;
                canvas.drawCircle(cx, dotY, cellW * 0.055f, paintDot);
            }
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_UP) {
            float x = event.getX();
            float y = event.getY();

            if (rectBtnPrev.contains(x, y)) {
                performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
                cambiarMes(-1);
                return true;
            }
            if (rectBtnNext.contains(x, y)) {
                performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
                cambiarMes(1);
                return true;
            }

            float w = getWidth();
            float h = getHeight();
            float paddingH = w * 0.03f;
            float usableW = w - (paddingH * 2);
            float headerH = h * 0.25f;

            if (y < headerH || x < paddingH || x > w - paddingH) {
                return true;
            }

            float cellW = usableW / 7f;
            float bodyH = h - headerH - (h * 0.02f);
            float rowH = bodyH / 6f;

            int col = (int) ((x - paddingH) / cellW);
            int row = (int) ((y - headerH) / rowH);

            Calendar calTemp = (Calendar) calendarActual.clone();
            calTemp.set(Calendar.DAY_OF_MONTH, 1);
            int primerDiaSemana = calTemp.get(Calendar.DAY_OF_WEEK);
            int offset = (primerDiaSemana + 5) % 7;

            int index = row * 7 + col;
            int day = index - offset + 1;

            int diasEnMes = calendarActual.getActualMaximum(Calendar.DAY_OF_MONTH);
            if (day >= 1 && day <= diasEnMes && day != selectedDay) {
                selectedDay = day;
                performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);
                invalidate();
                notificarSeleccion();
            }
        }
        return true;
    }

    private void notificarSeleccion() {
        if (listener == null) return;
        Calendar calTemp = (Calendar) calendarActual.clone();
        calTemp.set(Calendar.DAY_OF_MONTH, selectedDay);
        listener.onDateSelected(sdfDb.format(calTemp.getTime()));
    }
}