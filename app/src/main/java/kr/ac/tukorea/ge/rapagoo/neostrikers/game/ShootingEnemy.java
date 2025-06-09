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
import kr.ac.tukorea.ge.spgp2025.a2dg.framework.view.Metrics;

public class ShootingEnemy extends Enemy {
    private static final String TAG = ShootingEnemy.class.getSimpleName();

    private Sprite bodySprite;
    private AnimSprite engineSprite;

    private float fireCoolTime;
    private static final float FIRE_INTERVAL_BASE = 1.5f; // 기본 발사 간격 (더 빠르게)
    private static final float BULLET_SPEED_Y = 600f;

    // 이동 패턴 관련 변수
    private enum MovementPattern {
        STRAIGHT_DOWN,      // 직진
        ZIGZAG,            // 지그재그
        SINE_WAVE,         // 사인파
        SPIRAL,            // 나선형
        RANDOM_DRIFT       // 랜덤 표류
    }
    
    private MovementPattern movementPattern;
    private float moveTime = 0f;           // 이동 패턴용 시간
    private float initialX;                // 초기 X 위치
    private float horizontalSpeed = 150f;  // 좌우 이동 속도
    private float patternAmplitude = 80f;  // 패턴 진폭

    // 총알 패턴 관련 변수
    private enum BulletPattern {
        SINGLE,      // 단발
        TRIPLE,      // 3발
        SPREAD,      // 부채꼴
        PLAYER_AIM   // 플레이어 추적
    }
    
    private BulletPattern bulletPattern;
    private int shotsRemaining = 0;        // 연속 사격 시 남은 총알 수
    private float burstInterval = 0.1f;    // 연속 사격 간격

    // 이미지 리소스 ID - 실제 파일과 일치하는지 반드시 확인하세요!
    private static final int BODY_SPRITE_ID = R.mipmap.enemy_fighter_body;
    private static final int ENGINE_SPRITE_ID = R.mipmap.enemy_fighter_engine;

    private static final float ENGINE_FPS = 10.0f;
    private static final int ENGINE_FRAME_COUNT = 10; // 엔진 스프라이트 시트의 실제 프레임 수

    private static final float ENGINE_OFFSET_Y = 70f; // 본체 중심 Y에서 엔진 중심 Y까지의 윗방향 거리

    private static final float SHOOTER_TARGET_WIDTH = 180f;

    private static final float ENGINE_WIDTH_RATIO_OF_BODY = 0.6f;
    private static final float ENGINE_HEIGHT_ADJUST = 1.0f;

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
        
        // 체력을 레벨에 따라 증가 (3~8배)
        this.life = this.maxLife = (level + 1) * 30; // 기존 10에서 30으로 증가
        
        // 초기 위치 저장
        this.initialX = this.x;
        
        // 랜덤하게 이동 패턴 선택
        MovementPattern[] patterns = MovementPattern.values();
        this.movementPattern = patterns[(int)(Math.random() * patterns.length)];
        
        // 랜덤하게 총알 패턴 선택
        BulletPattern[] bulletPatterns = BulletPattern.values();
        this.bulletPattern = bulletPatterns[(int)(Math.random() * bulletPatterns.length)];
        
        Log.d(TAG, "ShootingEnemy initialized with pattern: " + movementPattern + ", bullet: " + bulletPattern);

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
        fireCoolTime = FIRE_INTERVAL_BASE + (float) Math.random();
    }

    @Override
    public void update() {
        // 이동 패턴 업데이트
        updateMovementPattern();
        
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


        // 총알 발사 처리 (연속 사격 고려)
        fireCoolTime -= GameView.frameTime;
        if (fireCoolTime <= 0) {
            if (shotsRemaining > 0) {
                fireBullet();
                shotsRemaining--;
                fireCoolTime = burstInterval; // 연속 사격 간격
            } else {
                startBurstFire();
                fireCoolTime = FIRE_INTERVAL_BASE + (float)(Math.random() * 1.0f); // 랜덤 발사 간격
            }
        }
    }
    
    private void updateMovementPattern() {
        moveTime += GameView.frameTime;
        
        float newX = this.x;
        
        switch (movementPattern) {
            case STRAIGHT_DOWN:
                // 기본 직진 (변화 없음)
                break;
                
            case ZIGZAG:
                newX = initialX + (float)Math.sin(moveTime * 3.0) * patternAmplitude;
                break;
                
            case SINE_WAVE:
                newX = initialX + (float)Math.sin(moveTime * 2.0) * patternAmplitude * 1.5f;
                break;
                
            case SPIRAL:
                float spiralRadius = patternAmplitude * (0.5f + moveTime * 0.1f);
                newX = initialX + (float)Math.cos(moveTime * 4.0) * spiralRadius;
                break;
                
            case RANDOM_DRIFT:
                if ((int)(moveTime * 2) % 2 == 0) { // 0.5초마다 방향 변경
                    newX += horizontalSpeed * GameView.frameTime * (Math.random() > 0.5 ? 1 : -1);
                }
                break;
        }
        
        // 화면 경계 체크
        float halfWidth = calculatedBodyActualWidth / 2;
        newX = Math.max(halfWidth, Math.min(newX, Metrics.width - halfWidth));
        this.x = newX;
    }
    
    private void startBurstFire() {
        switch (bulletPattern) {
            case SINGLE:
                shotsRemaining = 1;
                break;
            case TRIPLE:
                shotsRemaining = 3;
                burstInterval = 0.15f;
                break;
            case SPREAD:
                shotsRemaining = 1; // 한 번에 여러 발 발사
                break;
            case PLAYER_AIM:
                shotsRemaining = 1;
                break;
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
        
        int bulletPower = 8; // 총알 위력 증가
        float firePosX = bodySprite.getX();
        float firePosY = bodySprite.getY() + bodySprite.getHeight() / 2;

        Scene currentScene = Scene.top();
        if (!(currentScene instanceof MainScene)) return;
        MainScene mainScene = (MainScene) currentScene;
        
        switch (bulletPattern) {
            case SINGLE:
                EnemyBullet bullet = EnemyBullet.get(firePosX, firePosY, 0, BULLET_SPEED_Y, bulletPower);
                mainScene.add(MainScene.Layer.enemy_bullet, bullet);
                break;
                
            case TRIPLE:
                // 3발 직진
                EnemyBullet bullet1 = EnemyBullet.get(firePosX, firePosY, 0, BULLET_SPEED_Y, bulletPower);
                mainScene.add(MainScene.Layer.enemy_bullet, bullet1);
                break;
                
            case SPREAD:
                // 부채꼴 5발
                for (int i = -2; i <= 2; i++) {
                    float spreadAngle = i * 0.3f; // 각도 (라디안)
                    float speedX = BULLET_SPEED_Y * (float)Math.sin(spreadAngle) * 0.5f;
                    float speedY = BULLET_SPEED_Y * (float)Math.cos(spreadAngle);
                    EnemyBullet spreadBullet = EnemyBullet.get(firePosX, firePosY, speedX, speedY, bulletPower);
                    mainScene.add(MainScene.Layer.enemy_bullet, spreadBullet);
                }
                break;
                
            case PLAYER_AIM:
                // 플레이어 방향으로 발사
                Fighter player = findPlayer(mainScene);
                if (player != null) {
                    float dx = player.getX() - firePosX;
                    float dy = player.getY() - firePosY;
                    float distance = (float)Math.sqrt(dx * dx + dy * dy);
                    if (distance > 0) {
                        float aimSpeedX = (dx / distance) * BULLET_SPEED_Y * 0.8f;
                        float aimSpeedY = (dy / distance) * BULLET_SPEED_Y * 0.8f;
                        EnemyBullet aimBullet = EnemyBullet.get(firePosX, firePosY, aimSpeedX, aimSpeedY, bulletPower);
                        mainScene.add(MainScene.Layer.enemy_bullet, aimBullet);
                    }
                }
                break;
        }
    }
    
    private Fighter findPlayer(MainScene scene) {
        // 간단한 플레이어 찾기 - 실제 구현은 MainScene에서 플레이어 참조를 제공하는 것이 좋음
        // 여기서는 임시로 null 반환 (플레이어 추적 기능은 나중에 완성)
        return null; // TODO: MainScene에서 플레이어 참조 제공 필요
    }

    @Override
    public RectF getCollisionRect() {
        if (bodySprite != null) {
            RectF rect = new RectF();
            float bodyX = bodySprite.getX();
            float bodyY = bodySprite.getY();
            
            // 플레이어와 동일한 방식: 전체 크기에서 inset 적용
            RectUtil.setRect(rect, bodyX, bodyY, calculatedBodyActualWidth, calculatedBodyActualHeight);
            rect.inset(calculatedBodyActualWidth * 0.3f, calculatedBodyActualHeight * 0.3f);
            
            return rect;
        }
        Log.w(TAG, "bodySprite is null in getCollisionRect, returning overall dstRect.");
        return this.dstRect;
    }
}
