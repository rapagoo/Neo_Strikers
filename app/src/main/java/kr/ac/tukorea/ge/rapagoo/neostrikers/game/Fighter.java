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
    private static final float PLANE_HEIGHT = PLANE_WIDTH; // 너비와 높이를 같게 설정 (정사각형)
    // 비행기 스프라이트 시트의 프레임 너비
    private static final int PLANE_SRC_WIDTH = 80;

    // 목표 위치 변수
    private float targetX;
    private float targetY;

    // 발사 관련 변수
    private static final float FIRE_INTERVAL = 0.25f; // 기본 발사 간격 (초)
    private float fireCoolTime = FIRE_INTERVAL; // 남은 발사 쿨타임
    private static final float BULLET_OFFSET = 60f; // 비행기 중앙으로부터 총알 발사 시작점 Y 오프셋

    // 파워업 관련 변수
    private int powerLevel = 1; // 현재 파워 레벨
    private static final int MAX_POWER_LEVEL = 3; // 최대 파워 레벨

    // 총알 발사 관련 상수
    private static final float BULLET_SPEED_Y = -2000f; // 총알의 기본 Y축 속도 (위로 향함)
    private static final float BULLET_SPREAD_SPEED_X = 700f; // 대각선 총알의 X축 확산 속도 (클수록 넓게 퍼짐)

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
    private float prevX; // 이전 프레임의 x 좌표 저장용

    // 충돌 영역
    private final RectF collisionRect = new RectF();

    public Fighter() {
        super(R.mipmap.fighters); // 비행기 스프라이트 시트 로드
        // 초기 위치 설정 (화면 하단 중앙) 및 크기 설정
        setPosition(Metrics.width / 2, Metrics.height - 150, PLANE_WIDTH, PLANE_HEIGHT);
        targetX = x; // 초기 목표 위치는 현재 위치
        targetY = y;
        prevX = x; // 초기 이전 x좌표 설정

        sparkBitmap = BitmapPool.get(R.mipmap.laser_spark); // 발사 섬광 이미지 로드
        srcRect = new Rect(); // 스프라이트 시트에서 잘라낼 영역
        updateCollisionRect(); // 초기 충돌 영역 설정
    }

    @Override
    public void update() {
        // 목표 위치(targetX, targetY)로 비행기 위치 업데이트
        // 화면 경계를 벗어나지 않도록 위치 조정
        float halfWidth = width / 2;
        float halfHeight = height / 2;

        // 목표 위치를 화면 경계 내로 제한
        targetX = Math.max(halfWidth, Math.min(targetX, Metrics.width - halfWidth));
        targetY = Math.max(halfHeight, Math.min(targetY, Metrics.height - halfHeight));

        // 비행기 위치를 목표 위치로 직접 설정
        setPosition(targetX, targetY, width, height); // setPosition 내부에서 x, y, dstRect 업데이트됨

        // 충돌 영역 업데이트
        updateCollisionRect();

        // 총알 발사 로직
        fireBullet();

        // 좌우 기울기(Roll) 애니메이션 업데이트
        updateRoll();

        // 다음 프레임을 위해 현재 x 좌표 저장
        prevX = x;
    }

    // setPosition 오버라이드하여 collisionRect도 함께 업데이트
    @Override
    public void setPosition(float x, float y, float width, float height) {
        super.setPosition(x, y, width, height);
        updateCollisionRect(); // 위치 변경 시 충돌 영역도 업데이트
    }


    @Override
    public void draw(Canvas canvas) {
        // 좌우 기울기에 맞는 스프라이트 프레임 그리기
        canvas.drawBitmap(bitmap, srcRect, dstRect, null);

        // 발사 섬광 효과 그리기 (발사 직후 짧은 시간 동안)
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
        fireCoolTime = FIRE_INTERVAL; // 발사 쿨타임 초기화
        MainScene scene = (MainScene) Scene.top();
        if (scene == null) return;

        int basePower = 10; // 기본 총알 공격력
        int bulletSpriteResId = R.mipmap.laser_1; // 기본 총알 스프라이트 (필요시 레벨별로 변경 가능)

        // 파워 레벨에 따라 발사 패턴 변경
        switch (powerLevel) {
            case 1:
                // 레벨 1: 중앙 1발 (직선)
                // Bullet.get(시작 x, 시작 y, x축 속도, y축 속도, 공격력, 이미지 리소스 ID)
                Bullet bullet1 = Bullet.get(x, y - BULLET_OFFSET, 0, BULLET_SPEED_Y, basePower, bulletSpriteResId);
                scene.add(MainScene.Layer.bullet, bullet1);
                break;
            case 2:
                // 레벨 2: 양옆으로 2줄기 (직선)
                float gapLevel2 = 30f; // 두 총알 사이의 간격 (조절 가능)
                Bullet bullet2_left = Bullet.get(x - gapLevel2 / 2, y - BULLET_OFFSET, 0, BULLET_SPEED_Y, basePower + 5, bulletSpriteResId); // 공격력 약간 증가
                scene.add(MainScene.Layer.bullet, bullet2_left);
                Bullet bullet2_right = Bullet.get(x + gapLevel2 / 2, y - BULLET_OFFSET, 0, BULLET_SPEED_Y, basePower + 5, bulletSpriteResId);
                scene.add(MainScene.Layer.bullet, bullet2_right);
                break;
            case 3:
                // 레벨 3: 중앙 2줄기 (직선) + 양옆 대각선 2줄기
                float gapLevel3Center = 20f; // 중앙 두 총알 사이 간격
                float diagOffset = 40f; // 대각선 총알의 시작 X 오프셋

                // 중앙 직선 총알 (레벨 2와 유사하게, 간격 조절 가능)
                Bullet bullet3_center_left = Bullet.get(x - gapLevel3Center / 2, y - BULLET_OFFSET, 0, BULLET_SPEED_Y, basePower + 10, bulletSpriteResId); // 공격력 증가
                scene.add(MainScene.Layer.bullet, bullet3_center_left);
                Bullet bullet3_center_right = Bullet.get(x + gapLevel3Center / 2, y - BULLET_OFFSET, 0, BULLET_SPEED_Y, basePower + 10, bulletSpriteResId);
                scene.add(MainScene.Layer.bullet, bullet3_center_right);

                // 양옆 대각선 총알
                // 왼쪽 대각선: x축 속도는 음수, y축 속도는 음수(위로)
                Bullet bullet3_diag_left = Bullet.get(x - diagOffset, y - BULLET_OFFSET, -BULLET_SPREAD_SPEED_X, BULLET_SPEED_Y, basePower + 10, bulletSpriteResId);
                scene.add(MainScene.Layer.bullet, bullet3_diag_left);
                // 오른쪽 대각선: x축 속도는 양수, y축 속도는 음수(위로)
                Bullet bullet3_diag_right = Bullet.get(x + diagOffset, y - BULLET_OFFSET, BULLET_SPREAD_SPEED_X, BULLET_SPEED_Y, basePower + 10, bulletSpriteResId);
                scene.add(MainScene.Layer.bullet, bullet3_diag_right);
                break;
            default:
                // 혹시 모를 예외 처리 (powerLevel이 예상 범위를 벗어난 경우, 레벨 1과 동일하게 처리)
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

    // 터치 이벤트 처리
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

    // 충돌 영역 계산 및 업데이트
    private void updateCollisionRect() {
        collisionRect.set(dstRect);
        // 충돌 영역을 비행기 실제 모습에 가깝게 약간 줄임 (필요시 조정)
        collisionRect.inset(width * 0.15f, height * 0.15f);
    }

    // IBoxCollidable 인터페이스 구현
    @Override
    public RectF getCollisionRect() {
        return collisionRect;
    }

    // 파워 레벨 증가 (CollisionChecker에서 호출)
    public void increasePowerLevel() {
        if (powerLevel < MAX_POWER_LEVEL) {
            powerLevel++;
            Log.d(TAG, "Power level increased to: " + powerLevel);
        } else {
            Log.d(TAG, "Power level is already at MAX: " + powerLevel);
            // (선택) 최대 레벨에서 파워업 아이템 획득 시 점수 추가 등의 보너스 제공 가능
            MainScene scene = (MainScene) Scene.top();
            if (scene != null) {
                scene.addScore(500); // 예시: 최대 레벨에서 아이템 획득 시 500점 추가
            }
        }
    }

    // 현재 파워 레벨 반환 (UI 표시 등에 사용 가능)
    public int getPowerLevel() {
        return powerLevel;
    }
}
