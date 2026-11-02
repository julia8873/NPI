package com.example.nuietsiit.activities.secciones;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public class CalendarGridView extends View {

    private Paint paintTexto;
    private Paint paintTextoDiaSemana;
    private Paint paintSeleccion;
    private Paint paintDot;
    private Paint paintHeader;

    private Calendar calendarActual;
    private int selectedDay = -1;
    private Set<String> fechasConEventos = new HashSet<>();
    private OnDateSelectedListener listener;

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

    private void init() {
        calendarActual = Calendar.getInstance();
        selectedDay = calendarActual.get(Calendar.DAY_OF_MONTH);

        paintTexto = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintTexto.setColor(Color.parseColor("#0F172A"));
        paintTexto.setTextAlign(Paint.Align.CENTER);
        paintTexto.setFakeBoldText(true);

        paintTextoDiaSemana = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintTextoDiaSemana.setColor(Color.parseColor("#64748B"));
        paintTextoDiaSemana.setTextAlign(Paint.Align.CENTER);
        paintTextoDiaSemana.setFakeBoldText(true);

        paintSeleccion = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintSeleccion.setColor(Color.parseColor("#FF8C42"));
        paintSeleccion.setStyle(Paint.Style.FILL);

        paintDot = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintDot.setColor(Color.parseColor("#FF8C42"));
        paintDot.setStyle(Paint.Style.FILL);

        paintHeader = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintHeader.setColor(Color.parseColor("#0F172A"));
        paintHeader.setTextAlign(Paint.Align.CENTER);
        paintHeader.setFakeBoldText(true);
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
        calendarActual.set(Calendar.DAY_OF_MONTH, 1);
        selectedDay = 1;
        notificarSeleccion();
        invalidate();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = (int) (width * 0.85f);
        setMeasuredDimension(width, height);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float w = getWidth();
        float h = getHeight();

        float cellW = w / 7f;
        float headerH = h * 0.18f;
        float bodyH = h - headerH;
        float rowH = bodyH / 6f;

        paintTexto.setTextSize(cellW * 0.35f);
        paintTextoDiaSemana.setTextSize(cellW * 0.28f);
        paintHeader.setTextSize(cellW * 0.40f);

        // 1. Dibujar Mes y Año actual
        SimpleDateFormat sdf = new SimpleDateFormat("MMMM yyyy", new Locale("es", "ES"));
        String mesAno = sdf.format(calendarActual.getTime());
        mesAno = mesAno.substring(0, 1).toUpperCase() + mesAno.substring(1);
        canvas.drawText(mesAno, w / 2f, headerH * 0.6f, paintHeader);

        // 2. Días de la semana
        String[] diasSemana = {"L", "M", "X", "J", "V", "S", "D"};
        for (int i = 0; i < 7; i++) {
            float cx = (i + 0.5f) * cellW;
            canvas.drawText(diasSemana[i], cx, headerH * 0.95f, paintTextoDiaSemana);
        }

        // 3. Calcular días del mes
        Calendar calTemp = (Calendar) calendarActual.clone();
        calTemp.set(Calendar.DAY_OF_MONTH, 1);
        
        int primerDiaSemana = calTemp.get(Calendar.DAY_OF_WEEK);
        int offset = (primerDiaSemana + 5) % 7;
        int diasEnMes = calendarActual.getActualMaximum(Calendar.DAY_OF_MONTH);

        SimpleDateFormat sdfDb = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        int year = calendarActual.get(Calendar.YEAR);
        int month = calendarActual.get(Calendar.MONTH);

        for (int day = 1; day <= diasEnMes; day++) {
            int index = offset + day - 1;
            int col = index % 7;
            int row = index / 7;

            float cx = (col + 0.5f) * cellW;
            float cy = headerH + (row + 0.5f) * rowH;

            calTemp.set(year, month, day);
            String fechaStr = sdfDb.format(calTemp.getTime());

            if (day == selectedDay) {
                canvas.drawCircle(cx, cy - cellW * 0.05f, cellW * 0.38f, paintSeleccion);
                paintTexto.setColor(Color.WHITE);
            } else {
                paintTexto.setColor(Color.parseColor("#0F172A"));
            }

            canvas.drawText(String.valueOf(day), cx, cy + (paintTexto.getTextSize() / 3f), paintTexto);

            // Resaltar días con eventos mediante punto naranja
            if (fechasConEventos.contains(fechaStr)) {
                if (day != selectedDay) {
                    paintDot.setColor(Color.parseColor("#FF8C42"));
                } else {
                    paintDot.setColor(Color.WHITE);
                }
                canvas.drawCircle(cx, cy + cellW * 0.28f, cellW * 0.09f, paintDot);
            }
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_UP) {
            float x = event.getX();
            float y = event.getY();

            float w = getWidth();
            float h = getHeight();
            float headerH = h * 0.18f;

            if (y < headerH) {
                if (x < w * 0.3f) {
                    cambiarMes(-1);
                } else if (x > w * 0.7f) {
                    cambiarMes(1);
                }
                return true;
            }

            float cellW = w / 7f;
            float bodyH = h - headerH;
            float rowH = bodyH / 6f;

            int col = (int) (x / cellW);
            int row = (int) ((y - headerH) / rowH);

            Calendar calTemp = (Calendar) calendarActual.clone();
            calTemp.set(Calendar.DAY_OF_MONTH, 1);
            int primerDiaSemana = calTemp.get(Calendar.DAY_OF_WEEK);
            int offset = (primerDiaSemana + 5) % 7;

            int index = row * 7 + col;
            int day = index - offset + 1;

            int diasEnMes = calendarActual.getActualMaximum(Calendar.DAY_OF_MONTH);
            if (day >= 1 && day <= diasEnMes) {
                selectedDay = day;
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
        SimpleDateFormat sdfDb = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        listener.onDateSelected(sdfDb.format(calTemp.getTime()));
    }
}