// ShootingEnemy.java
package kr.ac.tukorea.ge.rapagoo.neostrikers.game;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.RectF;
import android.util.Log;

import kr.ac.tukorea.ge.rapagoo.neostrikers.R;
import kr.ac.tukorea.ge.spgp2025.a2dg.framework.objects.AnimSprite;
import kr.ac.tukorea.ge.spgp2025.a2dg.framework.objects.Sprite;
import kr.ac.tukorea.ge.spgp2025.a2dg.framework.res.BitmapPool;
import kr.ac.tukorea.ge.spgp2025.a2dg.framework.scene.Scene;
import kr.ac.tukorea.ge.spgp2025.a2dg.framework.util.RectUtil;
import kr.ac.tukorea.ge.spgp2025.a2dg.framework.view.GameView;

public class ShootingEnemy extends Enemy {
    private static final String TAG = ShootingEnemy.class.getSimpleName();

    private Sprite bodySprite;
    private AnimSprite engineSprite;

    private float fireCoolTime;
    private static final float FIRE_INTERVAL = 2.0f;
    private static final float BULLET_SPEED_Y = 600f;

    // 이미지 리소스 ID - 실제 파일과 일치하는지 반드시 확인하세요!
    private static final int BODY_SPRITE_ID = R.mipmap.enemy_fighter_body;
    private static final int ENGINE_SPRITE_ID = R.mipmap.enemy_fighter_engine;

    private static final float ENGINE_FPS = 15.0f;
    private static final int ENGINE_FRAME_COUNT = 4; // 엔진 스프라이트 시트의 실제 프레임 수

    private static final float ENGINE_OFFSET_Y = 70f; // 본체 중심 Y에서 엔진 중심 Y까지의 윗방향 거리

    private static final float SHOOTER_TARGET_WIDTH = 180f;

    private static final float ENGINE_WIDTH_RATIO_OF_BODY = 0.6f;
    private static final float ENGINE_HEIGHT_ADJUST = 1.0f;

    private static final float COLLISION_BOX_WIDTH_RATIO = 0.7f;
    private static final float COLLISION_BOX_HEIGHT_RATIO = 0.6f;

    // 멤버 변수로 본체 및 엔진의 실제 계산된 크기를 저장 (update에서 사용하기 위함)
    private float calculatedBodyActualWidth;
    private float calculatedBodyActualHeight;
    private float calculatedEngineActualWidth;
    private float calculatedEngineActualHeight;


    public ShootingEnemy() {
        super(); // Enemy 생성자 호출
    }

    public static ShootingEnemy get(int level, int index) {
        ShootingEnemy enemy = (ShootingEnemy) Scene.top().getRecyclable(ShootingEnemy.class);
        if (enemy == null) {
            enemy = new ShootingEnemy();
        }
        enemy.initShootingEnemy(level, index);
        return enemy;
    }

    private void initShootingEnemy(int level, int index) {
        super.init(level, index); // 부모 init에서 x, y 등 기본 위치 설정

        // 1. 본체 스프라이트 로드 및 원본 비율에 따른 크기 계산
        Bitmap bodyBitmap = BitmapPool.get(BODY_SPRITE_ID);
        if (bodyBitmap == null) {
            Log.e(TAG, "Failed to load bodyBitmap! Check BODY_SPRITE_ID: " + BODY_SPRITE_ID);
            // 게임이 비정상 종료될 수 있으므로, 여기서 리턴하거나 기본 이미지로 대체하는 등의 처리가 필요할 수 있습니다.
            // 우선 로그만 남기고 진행 (크래시 발생 지점 추적용)
            return; // 또는 throw new RuntimeException("Failed to load body sprite");
        }
        float originalBodyWidth = bodyBitmap.getWidth();
        float originalBodyHeight = bodyBitmap.getHeight();
        if (originalBodyWidth == 0) { // 비정상적인 비트맵 로드 방지
            Log.e(TAG, "BodyBitmap width is zero! BODY_SPRITE_ID: " + BODY_SPRITE_ID);
            return;
        }
        float bodyAspectRatio = originalBodyHeight / originalBodyWidth;

        calculatedBodyActualWidth = SHOOTER_TARGET_WIDTH;
        calculatedBodyActualHeight = calculatedBodyActualWidth * bodyAspectRatio;

        if (bodySprite == null) bodySprite = new Sprite(BODY_SPRITE_ID);
        else bodySprite.setImageResourceId(BODY_SPRITE_ID);


        // 2. 엔진 스프라이트 계산 및 설정
        Bitmap engineBitmap = BitmapPool.get(ENGINE_SPRITE_ID);
        if (engineBitmap == null) {
            Log.e(TAG, "Failed to load engineBitmap! Check ENGINE_SPRITE_ID: " + ENGINE_SPRITE_ID);
            return; // 또는 throw new RuntimeException("Failed to load engine sprite");
        }
        float originalEngineFrameWidth = (float)engineBitmap.getWidth() / ENGINE_FRAME_COUNT; // 정수 나눗셈 방지
        float originalEngineHeight = engineBitmap.getHeight();
        if (originalEngineFrameWidth == 0) { // 비정상적인 비트맵 로드 방지
            Log.e(TAG, "EngineBitmap frame width is zero! ENGINE_SPRITE_ID: " + ENGINE_SPRITE_ID);
            return;
        }
        float engineAspectRatio = originalEngineHeight / originalEngineFrameWidth;

        calculatedEngineActualWidth = calculatedBodyActualWidth * ENGINE_WIDTH_RATIO_OF_BODY;
        calculatedEngineActualHeight = calculatedEngineActualWidth * engineAspectRatio * ENGINE_HEIGHT_ADJUST;

        if (engineSprite == null) engineSprite = new AnimSprite(ENGINE_SPRITE_ID, ENGINE_FPS, ENGINE_FRAME_COUNT);
        else engineSprite.setImageResourceId(ENGINE_SPRITE_ID, ENGINE_FPS, ENGINE_FRAME_COUNT);


        // 3. ShootingEnemy 객체 자체의 전체적인 크기 업데이트
        this.width = calculatedBodyActualWidth;
        this.height = calculatedBodyActualHeight + calculatedEngineActualHeight - ENGINE_OFFSET_Y * 0.5f; // 엔진이 위로 가면서 겹치는 부분 고려 (대략적)
        this.radius = Math.min(this.width, this.height) / 2;
        RectUtil.setRect(this.dstRect, this.x, this.y, this.width, this.height);

        // 4. 본체와 엔진의 정확한 위치 설정 (this.x, this.y는 적 객체의 중심점)
        // 엔진이 본체 위에 위치하도록 Y 좌표 조정
        // (본체 Y 중심) = (적 전체 Y 중심) + (엔진 높이의 일부) - (엔진 오프셋의 일부) -> 본체가 살짝 아래로
        // (엔진 Y 중심) = (적 전체 Y 중심) - (엔진 오프셋) + (본체 높이와 엔진 높이 차이 보정) -> 엔진이 위로
        float bodyCenterY = this.y + (calculatedEngineActualHeight / 2) - (ENGINE_OFFSET_Y / 2) ; // << Y 좌표 계산 로직 단순화 및 조정 필요
        bodySprite.setPosition(this.x, bodyCenterY, calculatedBodyActualWidth, calculatedBodyActualHeight);

        float engineCenterY = this.y - ENGINE_OFFSET_Y + (calculatedBodyActualHeight / 2) - (calculatedEngineActualHeight / 2); // << Y 좌표 계산 로직 단순화 및 조정 필요
        engineSprite.setPosition(this.x, engineCenterY, calculatedEngineActualWidth, calculatedEngineActualHeight);


        this.bitmap = null; // 부모의 비트맵은 사용 안 함
        fireCoolTime = FIRE_INTERVAL + (float) Math.random();
    }

    @Override
    public void update() {
        super.update(); // this.x, this.y 위치 변경

        // init에서 계산된 크기를 사용하여 위치 업데이트
        // Y 좌표 계산 로직을 initShootingEnemy와 일관되게 수정
        float bodyCenterY = this.y + (calculatedEngineActualHeight / 2) - (ENGINE_OFFSET_Y / 2) ;
        if (bodySprite != null) { // Null 체크 추가
            bodySprite.setPosition(this.x, bodyCenterY, calculatedBodyActualWidth, calculatedBodyActualHeight);
        }

        float engineCenterY = this.y - ENGINE_OFFSET_Y + (calculatedBodyActualHeight / 2) - (calculatedEngineActualHeight / 2);
        if (engineSprite != null) { // Null 체크 추가
            engineSprite.setPosition(this.x, engineCenterY, calculatedEngineActualWidth, calculatedEngineActualHeight);
            engineSprite.update(); // 엔진 애니메이션 업데이트
        }


        fireCoolTime -= GameView.frameTime;
        if (fireCoolTime <= 0) {
            fireBullet();
            fireCoolTime = FIRE_INTERVAL;
        }
    }

    @Override
    public void draw(Canvas canvas) {
        if (bodySprite != null) {
            bodySprite.draw(canvas);
        }
        if (engineSprite != null) {
            engineSprite.draw(canvas);
        }
    }

    private void fireBullet() {
        // bodySprite가 null일 경우를 대비한 방어 코드
        if (bodySprite == null) {
            Log.e(TAG, "fireBullet called but bodySprite is null!");
            return;
        }
        int bulletPower = 5;
        float firePosX = bodySprite.getX();
        float firePosY = bodySprite.getY() + bodySprite.getHeight() / 2;

        EnemyBullet bullet = EnemyBullet.get(firePosX, firePosY, 0, BULLET_SPEED_Y, bulletPower);

        Scene currentScene = Scene.top();
        if (currentScene instanceof MainScene) {
            ((MainScene)currentScene).add(MainScene.Layer.enemy_bullet, bullet);
        }
    }

    @Override
    public RectF getCollisionRect() {
        if (bodySprite != null) {
            RectF rect = new RectF();
            float bodyX = bodySprite.getX();
            float bodyY = bodySprite.getY();
            // init에서 계산된 본체 크기를 사용
            float collisionWidth = calculatedBodyActualWidth * COLLISION_BOX_WIDTH_RATIO;
            float collisionHeight = calculatedBodyActualHeight * COLLISION_BOX_HEIGHT_RATIO;

            RectUtil.setRect(rect, bodyX, bodyY, collisionWidth, collisionHeight);
            return rect;
        }
        Log.w(TAG, "bodySprite is null in getCollisionRect, returning overall dstRect.");
        return this.dstRect;
    }
}
