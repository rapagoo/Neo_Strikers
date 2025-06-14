package kr.ac.tukorea.ge.rapagoo.neostrikers.game;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;

import kr.ac.tukorea.ge.spgp2025.a2dg.framework.interfaces.IBoxCollidable;
import kr.ac.tukorea.ge.spgp2025.a2dg.framework.interfaces.IGameObject;
import kr.ac.tukorea.ge.spgp2025.a2dg.framework.interfaces.IRecyclable;
import kr.ac.tukorea.ge.spgp2025.a2dg.framework.scene.Scene;
import kr.ac.tukorea.ge.spgp2025.a2dg.framework.view.GameView;
import kr.ac.tukorea.ge.spgp2025.a2dg.framework.view.Metrics;

public class BossBullet implements IGameObject, IRecyclable, IBoxCollidable {
    protected float x, y, dx, dy, radius;
    protected RectF dstRect = new RectF();
    private int power;

    private Paint corePaint;
    private Paint glowPaint;
    private Paint innerPaint;
    private Paint outerPaint;
    private float rotation;
    private float pulseTime;

    public static BossBullet get(float x, float y, float dx, float dy, int power) {
        BossBullet bullet = (BossBullet) Scene.top().getRecyclable(BossBullet.class);
        if (bullet == null) {
            bullet = new BossBullet();
        }
        bullet.init(x, y, dx, dy, power);
        return bullet;
    }

    private BossBullet() {
        corePaint = new Paint();
        corePaint.setAntiAlias(true);
        corePaint.setStyle(Paint.Style.FILL);

        glowPaint = new Paint();
        glowPaint.setAntiAlias(true);
        glowPaint.setStyle(Paint.Style.FILL);

        innerPaint = new Paint();
        innerPaint.setAntiAlias(true);
        innerPaint.setStyle(Paint.Style.FILL);

        outerPaint = new Paint();
        outerPaint.setAntiAlias(true);
        outerPaint.setStyle(Paint.Style.STROKE);
        outerPaint.setStrokeWidth(3f);
    }

    private void init(float x, float y, float dx, float dy, int power) {
        this.x = x;
        this.y = y;
        this.dx = dx;
        this.dy = dy;
        this.power = power;
        this.radius = 25f; // 크기를 줄임 (기존 40f에서 25f로)
        this.rotation = 0;
        this.pulseTime = 0;
        updateDstRect();
    }

    @Override
    public void update() {
        float frameTime = GameView.frameTime;
        x += dx * frameTime;
        y += dy * frameTime;
        rotation += 300 * frameTime; // 회전 속도
        pulseTime += frameTime * 4; // 맥동 효과
        updateDstRect();

        if (dstRect.bottom < 0 || dstRect.top > Metrics.height || dstRect.right < 0 || dstRect.left > Metrics.width) {
            Scene.top().remove(MainScene.Layer.enemy_bullet,this);
        }
    }

    private void updateDstRect() {
        dstRect.set(x - radius, y - radius, x + radius, y + radius);
    }

    @Override
    public void draw(Canvas canvas) {
        // 맥동 효과
        float pulseMod = 1.0f + 0.3f * (float)Math.sin(pulseTime);
        float currentRadius = radius * pulseMod;
        
        // 외부 글로우 (보스용으로 더 화려하게)
        glowPaint.setColor(Color.rgb(255, 20, 150));
        glowPaint.setAlpha(100);
        canvas.drawCircle(x, y, currentRadius * 2.0f, glowPaint);
        
        glowPaint.setColor(Color.rgb(255, 100, 50));
        glowPaint.setAlpha(80);
        canvas.drawCircle(x, y, currentRadius * 1.5f, glowPaint);

        canvas.save();
        canvas.translate(x, y);
        canvas.rotate(rotation);

        // 외부 육각형 형태 (보스용으로 더 복잡한 모양)
        float hexRadius = currentRadius * 0.9f;
        for (int i = 0; i < 6; i++) {
            float angle = (float)(i * Math.PI / 3);
            float x1 = (float)(Math.cos(angle) * hexRadius);
            float y1 = (float)(Math.sin(angle) * hexRadius);
            float x2 = (float)(Math.cos(angle + Math.PI / 3) * hexRadius);
            float y2 = (float)(Math.sin(angle + Math.PI / 3) * hexRadius);
            
            corePaint.setColor(Color.rgb(255, 50, 200));
            canvas.drawLine(x1, y1, x2, y2, corePaint);
        }

        // 중심 다이아몬드
        corePaint.setColor(Color.rgb(255, 150, 50));
        canvas.drawRect(-currentRadius * 0.5f, -currentRadius * 0.5f, currentRadius * 0.5f, currentRadius * 0.5f, corePaint);

        canvas.restore();

        // 중심 밝은 코어
        innerPaint.setColor(Color.WHITE);
        canvas.drawCircle(x, y, currentRadius * 0.3f, innerPaint);
        
        // 작은 반짝이는 점들
        innerPaint.setColor(Color.rgb(255, 255, 100));
        canvas.drawCircle(x, y, currentRadius * 0.15f, innerPaint);
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
} 