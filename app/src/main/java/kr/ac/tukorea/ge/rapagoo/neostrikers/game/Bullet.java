package kr.ac.tukorea.ge.rapagoo.neostrikers.game;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;

import kr.ac.tukorea.ge.spgp2025.a2dg.framework.interfaces.IBoxCollidable;
import kr.ac.tukorea.ge.spgp2025.a2dg.framework.interfaces.IGameObject;
import kr.ac.tukorea.ge.spgp2025.a2dg.framework.interfaces.ILayerProvider;
import kr.ac.tukorea.ge.spgp2025.a2dg.framework.interfaces.IRecyclable;
import kr.ac.tukorea.ge.spgp2025.a2dg.framework.scene.Scene;
import kr.ac.tukorea.ge.spgp2025.a2dg.framework.view.GameView;
import kr.ac.tukorea.ge.spgp2025.a2dg.framework.view.Metrics;

public class Bullet implements IGameObject, IRecyclable, IBoxCollidable, ILayerProvider<MainScene.Layer> {
    protected float x, y, dx, dy, width, height, radius;
    protected RectF dstRect = new RectF();
    private int power;
    private int powerLevel;

    private Paint corePaint;
    private Paint glowPaint;
    private Paint trailPaint;
    
    private float life;

    public static Bullet get(float x, float y, float dx, float dy, int power, int powerLevel) {
        Bullet bullet = (Bullet) Scene.top().getRecyclable(Bullet.class);
        if (bullet == null) {
            bullet = new Bullet();
        }
        bullet.init(x, y, dx, dy, power, powerLevel);
        return bullet;
    }

    private Bullet() {
        corePaint = new Paint();
        corePaint.setAntiAlias(true);
        glowPaint = new Paint();
        glowPaint.setAntiAlias(true);
        trailPaint = new Paint();
        trailPaint.setAntiAlias(true);
    }

    private void init(float x, float y, float dx, float dy, int power, int powerLevel) {
        this.x = x;
        this.y = y;
        this.dx = dx;
        this.dy = dy;
        this.power = power;
        this.powerLevel = powerLevel;
        this.life = 0; // for trail effect

        // 파워 레벨에 따라 총알의 크기와 스타일 결정
        switch (powerLevel) {
            case 1:
                this.width = 15;
                this.height = 40;
                break;
            case 2:
                this.width = 25;
                this.height = 50;
                break;
            case 3:
            default:
                this.width = 35;
                this.height = 60;
                break;
        }
        this.radius = width / 2;
        updateDstRect();
    }

    @Override
    public void update() {
        float frameTime = GameView.frameTime;
        x += dx * frameTime;
        y += dy * frameTime;
        life += frameTime;

        updateDstRect();

        if (dstRect.bottom < 0) {
            Scene.top().remove(this);
        }
    }
    
    private void updateDstRect() {
        dstRect.set(x - radius, y - height / 2, x + radius, y + height / 2);
    }

    @Override
    public void draw(Canvas canvas) {
        // 레벨에 따른 색상 및 스타일 설정
        int coreColor, glowColor;
        float trailLength;

        switch (powerLevel) {
            case 1:
                coreColor = Color.rgb(150, 200, 255);
                glowColor = Color.rgb(50, 100, 255);
                trailLength = 40f;
                break;
            case 2:
                coreColor = Color.rgb(200, 220, 255);
                glowColor = Color.rgb(100, 150, 255);
                trailLength = 70f;
                break;
            case 3:
            default:
                coreColor = Color.WHITE;
                glowColor = Color.rgb(150, 200, 255);
                trailLength = 100f;
                break;
        }

        // 궤적 효과 (Trail)
        trailPaint.setColor(glowColor);
        trailPaint.setStrokeWidth(width);
        trailPaint.setAlpha(80);
        trailPaint.setStrokeCap(Paint.Cap.ROUND);
        canvas.drawLine(x, y, x, y - trailLength, trailPaint);

        // 글로우 효과 (Glow)
        glowPaint.setColor(glowColor);
        glowPaint.setAlpha(150);
        canvas.drawOval(
                x - width * 0.7f, y - height * 0.4f,
                x + width * 0.7f, y + height * 0.6f,
                glowPaint
        );

        // 중심 코어 (Core)
        corePaint.setColor(coreColor);
        canvas.drawOval(dstRect, corePaint);

        // 맥동 효과 (Pulse for level 3)
        if (powerLevel >= 3) {
            float pulseRatio = (float) (Math.sin(life * 20) + 1) / 2; // 0~1 사이로 진동
            int pulseAlpha = (int) (100 * pulseRatio);
            glowPaint.setAlpha(pulseAlpha);
            canvas.drawOval(
                    x - width * (0.8f + pulseRatio * 0.4f), y - height * (0.4f + pulseRatio * 0.2f),
                    x + width * (0.8f + pulseRatio * 0.4f), y + height * (0.6f + pulseRatio * 0.2f),
                    glowPaint
            );
        }
    }

    public int getPower() {
        return power;
    }

    @Override
    public RectF getCollisionRect() {
        return dstRect;
    }

    @Override
    public void onRecycle() {}

    @Override
    public MainScene.Layer getLayer() {
        return MainScene.Layer.bullet;
    }
}