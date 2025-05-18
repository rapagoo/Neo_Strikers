package kr.ac.tukorea.ge.rapagoo.neostrikers.game;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.Log;
import android.view.MotionEvent;

import kr.ac.tukorea.ge.rapagoo.neostrikers.R;
import kr.ac.tukorea.ge.spgp2025.a2dg.framework.interfaces.IBoxCollidable;
import kr.ac.tukorea.ge.spgp2025.a2dg.framework.objects.Sprite;
import kr.ac.tukorea.ge.spgp2025.a2dg.framework.res.BitmapPool;
import kr.ac.tukorea.ge.spgp2025.a2dg.framework.scene.Scene;
import kr.ac.tukorea.ge.spgp2025.a2dg.framework.util.RectUtil;
import kr.ac.tukorea.ge.spgp2025.a2dg.framework.view.GameView;
import kr.ac.tukorea.ge.spgp2025.a2dg.framework.view.Metrics;

public class Fighter extends Sprite implements IBoxCollidable {
    private static final String TAG = Fighter.class.getSimpleName();
    // 비행기 크기
    private static final float PLANE_WIDTH = 120f;
    private static final float PLANE_HEIGHT = PLANE_WIDTH;
    // 비행기 스프라이트 시트의 프레임 너비
    private static final int PLANE_SRC_WIDTH = 80;

    // 목표 위치 변수
    private float targetX;
    private float targetY;

    // 발사 관련 변수
    private static final float FIRE_INTERVAL = 0.25f;
    private float fireCoolTime = FIRE_INTERVAL;
    private static final float BULLET_OFFSET = 60f;

    // 파워업 관련 변수
    private int powerLevel = 1;
    private static final int MAX_POWER_LEVEL = 3;

    // 총알 발사 관련 상수
    private static final float BULLET_SPEED_Y = -2000f;
    private static final float BULLET_SPREAD_SPEED_X = 700f;

    // 발사 섬광 효과 관련 변수
    private static final float SPARK_OFFSET = 50f;
    private static final float SPARK_DURATION = 0.1f;
    private static final float SPARK_WIDTH = 80f;
    private static final float SPARK_HEIGHT = SPARK_WIDTH * 3 / 5;
    private final RectF sparkRect = new RectF();
    private final Bitmap sparkBitmap;

    // 좌우 기울기(Roll) 애니메이션 관련 변수
    private static final float MAX_ROLL_TIME = 0.4f;
    private float rollTime;
    private float prevX;

    // 충돌 영역
    private final RectF collisionRect = new RectF();

    // 체력 관련 변수
    private static final int INITIAL_HEALTH = 3; // 초기 체력
    private int health; // 현재 체력
    private boolean isInvincible = false; // 무적 상태 여부
    private float invincibleTime = 0f; // 무적 지속 시간
    private static final float INVINCIBLE_DURATION = 1.5f; // 피격 후 무적 시간 (초)

    public Fighter() {
        super(R.mipmap.fighters);
        setPosition(Metrics.width / 2, Metrics.height - 150, PLANE_WIDTH, PLANE_HEIGHT);
        targetX = x;
        targetY = y;
        prevX = x;
        health = INITIAL_HEALTH; // 체력 초기화

        sparkBitmap = BitmapPool.get(R.mipmap.laser_spark);
        srcRect = new Rect();
        updateCollisionRect();
    }

    @Override
    public void update() {
        // 무적 시간 처리
        if (isInvincible) {
            invincibleTime -= GameView.frameTime;
            if (invincibleTime <= 0) {
                isInvincible = false;
            }
        }

        // 목표 위치(targetX, targetY)로 비행기 위치 업데이트
        float halfWidth = width / 2;
        float halfHeight = height / 2;
        targetX = Math.max(halfWidth, Math.min(targetX, Metrics.width - halfWidth));
        targetY = Math.max(halfHeight, Math.min(targetY, Metrics.height - halfHeight));
        setPosition(targetX, targetY, width, height);

        updateCollisionRect();
        fireBullet();
        updateRoll();
        prevX = x;
    }

    @Override
    public void setPosition(float x, float y, float width, float height) {
        super.setPosition(x, y, width, height);
        updateCollisionRect();
    }


    @Override
    public void draw(Canvas canvas) {
        if (isInvincible) {
            if ((int)(invincibleTime * 10) % 2 == 0) {
                return;
            }
        }
        canvas.drawBitmap(bitmap, srcRect, dstRect, null);

        if (FIRE_INTERVAL - fireCoolTime < SPARK_DURATION) {
            RectUtil.setRect(sparkRect, x, y - SPARK_OFFSET, SPARK_WIDTH, SPARK_HEIGHT);
            canvas.drawBitmap(sparkBitmap, null, sparkRect, null);
        }
    }

    private void fireBullet() {
        fireCoolTime -= GameView.frameTime;
        if (fireCoolTime > 0) {
            return;
        }
        fireCoolTime = FIRE_INTERVAL;
        MainScene scene = (MainScene) Scene.top();
        if (scene == null) return;

        int basePower = 10;
        int bulletSpriteResId = R.mipmap.laser_1;

        switch (powerLevel) {
            case 1:
                Bullet bullet1 = Bullet.get(x, y - BULLET_OFFSET, 0, BULLET_SPEED_Y, basePower, bulletSpriteResId);
                scene.add(MainScene.Layer.bullet, bullet1);
                break;
            case 2:
                float gapLevel2 = 30f;
                Bullet bullet2_left = Bullet.get(x - gapLevel2 / 2, y - BULLET_OFFSET, 0, BULLET_SPEED_Y, basePower + 5, bulletSpriteResId);
                scene.add(MainScene.Layer.bullet, bullet2_left);
                Bullet bullet2_right = Bullet.get(x + gapLevel2 / 2, y - BULLET_OFFSET, 0, BULLET_SPEED_Y, basePower + 5, bulletSpriteResId);
                scene.add(MainScene.Layer.bullet, bullet2_right);
                break;
            case 3:
                float gapLevel3Center = 20f;
                float diagOffset = 40f;
                Bullet bullet3_center_left = Bullet.get(x - gapLevel3Center / 2, y - BULLET_OFFSET, 0, BULLET_SPEED_Y, basePower + 10, bulletSpriteResId);
                scene.add(MainScene.Layer.bullet, bullet3_center_left);
                Bullet bullet3_center_right = Bullet.get(x + gapLevel3Center / 2, y - BULLET_OFFSET, 0, BULLET_SPEED_Y, basePower + 10, bulletSpriteResId);
                scene.add(MainScene.Layer.bullet, bullet3_center_right);
                Bullet bullet3_diag_left = Bullet.get(x - diagOffset, y - BULLET_OFFSET, -BULLET_SPREAD_SPEED_X, BULLET_SPEED_Y, basePower + 10, bulletSpriteResId);
                scene.add(MainScene.Layer.bullet, bullet3_diag_left);
                Bullet bullet3_diag_right = Bullet.get(x + diagOffset, y - BULLET_OFFSET, BULLET_SPREAD_SPEED_X, BULLET_SPEED_Y, basePower + 10, bulletSpriteResId);
                scene.add(MainScene.Layer.bullet, bullet3_diag_right);
                break;
            default:
                Bullet defaultBullet = Bullet.get(x, y - BULLET_OFFSET, 0, BULLET_SPEED_Y, basePower, bulletSpriteResId);
                scene.add(MainScene.Layer.bullet, defaultBullet);
                Log.w(TAG, "Invalid powerLevel: " + powerLevel + ", defaulting to level 1 firing pattern.");
                break;
        }
    }

    private void updateRoll() {
        float dx = x - prevX;
        int sign = 0;

        if (dx < -0.1f) {
            sign = -1;
        } else if (dx > 0.1f) {
            sign = 1;
        } else {
            if (rollTime > 0.1f) sign = -1;
            else if (rollTime < -0.1f) sign = 1;
        }

        rollTime += sign * GameView.frameTime * 3;

        if (sign < 0 && rollTime < 0) rollTime = 0;
        if (sign > 0 && rollTime > 0) rollTime = 0;

        rollTime = Math.max(-MAX_ROLL_TIME, Math.min(rollTime, MAX_ROLL_TIME));

        int rollIndex = 5 + (int) Math.round(rollTime * 5 / MAX_ROLL_TIME);
        rollIndex = Math.max(0, Math.min(rollIndex, 10));

        srcRect.set(rollIndex * PLANE_SRC_WIDTH, 0, (rollIndex + 1) * PLANE_SRC_WIDTH, PLANE_SRC_WIDTH);
    }

    public boolean onTouch(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_MOVE:
                float[] pts = Metrics.fromScreen(event.getX(), event.getY());
                targetX = pts[0];
                targetY = pts[1];
                return true;
        }
        return false;
    }

    private void updateCollisionRect() {
        collisionRect.set(dstRect);
        collisionRect.inset(width * 0.15f, height * 0.15f);
    }

    @Override
    public RectF getCollisionRect() {
        return collisionRect;
    }

    public void increasePowerLevel() {
        if (powerLevel < MAX_POWER_LEVEL) {
            powerLevel++;
            Log.d(TAG, "Power level increased to: " + powerLevel);
        } else {
            Log.d(TAG, "Power level is already at MAX: " + powerLevel);
            MainScene scene = (MainScene) Scene.top();
            if (scene != null) {
                scene.addScore(500);
            }
        }
    }

    public int getPowerLevel() {
        return powerLevel;
    }

    public void decreaseHealth(int amount) {
        if (isInvincible()) { // 접근자 메소드 사용
            return;
        }
        if (health <= 0) return;

        health -= amount;
        Log.d(TAG, "Player health decreased by " + amount + ". Current health: " + health);

        if (health <= 0) {
            Log.i(TAG, "Player is dead!");
        } else {
            isInvincible = true;
            invincibleTime = INVINCIBLE_DURATION;
            Log.d(TAG, "Player is now invincible for " + INVINCIBLE_DURATION + " seconds.");
        }
    }

    public boolean isDead() {
        return health <= 0;
    }

    public int getHealth() {
        return health;
    }

    // public 접근자로 isInvincible 상태를 반환하는 메소드
    public boolean isInvincible() {
        return isInvincible;
    }

    public void resetHealth() {
        health = INITIAL_HEALTH;
        isInvincible = false;
        invincibleTime = 0f;
    }
}
