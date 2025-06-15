package kr.ac.tukorea.ge.rapagoo.neostrikers.game;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;

import kr.ac.tukorea.ge.spgp2025.a2dg.framework.interfaces.IGameObject;

public class BombButton implements IGameObject {

    private final RectF bounds = new RectF();
    private final Paint backgroundPaint = new Paint();
    private final Paint borderPaint = new Paint();
    private final Paint textPaint = new Paint();
    private final Paint cooldownPaint = new Paint();
    private boolean isPressed = false;
    private boolean isDisabled = false;
    private final float cx, cy, radius;

    public BombButton(float cx, float cy, float radius) {
        this.cx = cx;
        this.cy = cy;
        this.radius = radius;
        this.bounds.set(cx - radius, cy - radius, cx + radius, cy + radius);

        // Border Paint
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(radius * 0.1f);
        borderPaint.setColor(Color.WHITE);
        borderPaint.setAntiAlias(true);

        // Background Paint (Gradient)
        backgroundPaint.setAntiAlias(true);
        Shader gradient = new RadialGradient(cx, cy, radius,
                Color.parseColor("#FF6B6B"), Color.parseColor("#C92A2A"), Shader.TileMode.CLAMP);
        backgroundPaint.setShader(gradient);


        // Text Paint
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(radius);
        textPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setShadowLayer(5, 2, 2, Color.BLACK);

        // Cooldown/Disabled Paint
        cooldownPaint.setColor(0xAA000000); // Semi-transparent black
        cooldownPaint.setStyle(Paint.Style.FILL);
    }

    @Override
    public void update() {
        // Can be used for animations in the future
    }

    @Override
    public void draw(Canvas canvas) {
        // Draw border first (as a background)
        canvas.drawCircle(cx, cy, radius, borderPaint);

        // Draw main button circle
        float bgRadius = isPressed ? radius * 0.85f : radius * 0.9f;
        canvas.drawCircle(cx, cy, bgRadius, backgroundPaint);

        // Draw Text "B"
        float textY = cy - (textPaint.descent() + textPaint.ascent()) / 2;
        canvas.drawText("B", cx, textY, textPaint);

        if (isDisabled) {
            canvas.drawCircle(cx, cy, radius, cooldownPaint);
        }
    }

    public RectF getBounds() {
        return bounds;
    }

    public void setPressed(boolean pressed) {
        isPressed = pressed;
    }

    public void setDisabled(boolean disabled) {
        isDisabled = disabled;
    }
} 