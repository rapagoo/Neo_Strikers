package kr.ac.tukorea.ge.rapagoo.neostrikers.game;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;

import kr.ac.tukorea.ge.rapagoo.neostrikers.R;
import kr.ac.tukorea.ge.spgp2025.a2dg.framework.interfaces.IGameObject;
import kr.ac.tukorea.ge.spgp2025.a2dg.framework.res.Sound;
import kr.ac.tukorea.ge.spgp2025.a2dg.framework.view.GameView;
import kr.ac.tukorea.ge.spgp2025.a2dg.framework.view.Metrics;

public class WarningUI implements IGameObject {
    private final Paint textPaint = new Paint();
    private float life = 2.5f; // 2.5초 동안 표시
    private boolean isVisible = true;
    private float blinkTimer = 0.0f;
    private static final float BLINK_INTERVAL = 0.2f; // 0.2초 간격으로 깜빡임

    public WarningUI() {
        textPaint.setColor(Color.RED);
        textPaint.setTextSize(150f);
        textPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setShadowLayer(10, 5, 5, Color.BLACK);
        Sound.play(R.raw.se_warning);
    }

    @Override
    public void update() {
        life -= GameView.frameTime;

        blinkTimer -= GameView.frameTime;
        if (blinkTimer < 0) {
            isVisible = !isVisible;
            blinkTimer = BLINK_INTERVAL;
        }
    }

    @Override
    public void draw(Canvas canvas) {
        if (!isVisible || isFinished()) return;

        float x = Metrics.width / 2;
        float y = Metrics.height / 2;
        canvas.drawText("WARNING", x, y, textPaint);
    }

    public boolean isFinished() {
        return life <= 0;
    }
} 