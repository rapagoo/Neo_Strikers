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

public class EnemyBullet implements IGameObject, IRecyclable, IBoxCollidable, ILayerProvider<MainScene.Layer> {
    protected float x, y, dx, dy, width, height, radius;
    protected RectF dstRect = new RectF();
    private int power;

    private Paint corePaint;
    private Paint glowPaint;
    private float rotation;

    public static EnemyBullet get(float x, float y, float dx, float dy, int power) {
        EnemyBullet bullet = (EnemyBullet) Scene.top().getRecyclable(EnemyBullet.class);
        if (bullet == null) {
            bullet = new EnemyBullet();
        }
        bullet.init(x, y, dx, dy, power);
        return bullet;
    }

    private EnemyBullet() {
        corePaint = new Paint();
        corePaint.setAntiAlias(true);
        glowPaint = new Paint();
        glowPaint.setAntiAlias(true);
    }

    private void init(float x, float y, float dx, float dy, int power) {
        this.x = x;
        this.y = y;
        this.dx = dx;
        this.dy = dy;
        this.power = power;
        this.width = 30;
        this.height = 30;
        this.radius = width / 2;
        this.rotation = 0;
        updateDstRect();
    }

    @Override
    public void update() {
        float frameTime = GameView.frameTime;
        x += dx * frameTime;
        y += dy * frameTime;
        rotation += 450 * frameTime; // 초당 450도 회전
        updateDstRect();

        if (dstRect.bottom < 0 || dstRect.top > Metrics.height || dstRect.right < 0 || dstRect.left > Metrics.width) {
            Scene.top().remove(this);
        }
    }

    private void updateDstRect() {
        dstRect.set(x - radius, y - radius, x + radius, y + radius);
    }

    @Override
    public void draw(Canvas canvas) {
        // 글로우
        glowPaint.setColor(Color.rgb(255, 0, 100));
        glowPaint.setAlpha(128);
        canvas.drawCircle(x, y, radius * 1.5f, glowPaint);

        canvas.save();
        canvas.translate(x, y);
        canvas.rotate(rotation);

        // 중심 코어 (마름모 형태)
        corePaint.setColor(Color.rgb(255, 100, 200));
        canvas.drawRect(-radius * 0.7f, -radius * 0.7f, radius * 0.7f, radius * 0.7f, corePaint);

        canvas.restore();

        // 내부 밝은 점
        corePaint.setColor(Color.WHITE);
        canvas.drawCircle(x, y, radius * 0.3f, corePaint);
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
        return MainScene.Layer.enemy_bullet;
    }
}
