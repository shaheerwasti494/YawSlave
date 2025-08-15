package com.yaw.yawcommon;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;
public class DialView extends View {
    private final Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint text = new Paint(Paint.ANTI_ALIAS_FLAG);

    private float angleDeg0to360 = 0f;
    private String title = "";

    public DialView(Context c) { super(c); init(); }
    public DialView(Context c, @Nullable AttributeSet a) { super(c, a); init(); }
    public DialView(Context c, @Nullable AttributeSet a, int s) { super(c, a, s); init(); }

    private void init() {
        stroke.setStyle(Paint.Style.STROKE);
        stroke.setStrokeWidth(8f);
        text.setTextSize(40f);
        text.setTextAlign(Paint.Align.CENTER);
    }

    /** Title shown above the dial */
    public void setTitle(String t) {
        this.title = t != null ? t : "";
        invalidate();
    }

    /** Angle in degrees [0..360) where 0° is “up” */
    public void setAngleDeg0to360(float deg) {
        float d = deg;
        if (d < 0f) d += 360f;
        if (d >= 360f) d -= 360f;
        this.angleDeg0to360 = d;
        invalidate();
    }

    @Override protected void onDraw(Canvas c) {
        super.onDraw(c);
        float w = getWidth(), h = getHeight();
        float cx = w / 2f, cy = h / 2f;
        float r = Math.min(w, h) * 0.45f;

        // dial
        RectF oval = new RectF(cx - r, cy - r, cx + r, cy + r);
        c.drawOval(oval, stroke);

        // top tick
        c.drawLine(cx, cy - r, cx, cy - r + 40f, stroke);

        // arrow (0° = up, clockwise positive)
        double rad = Math.toRadians(angleDeg0to360);
        float ex = (float) (cx + Math.sin(rad) * (r - 40f));
        float ey = (float) (cy - Math.cos(rad) * (r - 40f));
        c.drawLine(cx, cy, ex, ey, stroke);

        // title
        if (!title.isEmpty()) {
            c.drawText(title, cx, cy - r - 20f, text);
        }
    }
}