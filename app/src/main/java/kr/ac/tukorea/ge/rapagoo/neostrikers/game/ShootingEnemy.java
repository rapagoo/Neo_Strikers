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

    public enum EnemyType {
        TYPE_1, TYPE_2, TYPE_3
    }
    private EnemyType enemyType;

    private Sprite bodySprite;

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

    // 이미지 리소스 ID - 타입별로 설정되도록 변경
    private int bodySpriteId;

    private float targetWidth;
    private float collisionInsetRatio;

    // 멤버 변수로 본체 및 엔진의 실제 계산된 크기를 저장 (update에서 사용하기 위함)
    private float calculatedBodyActualWidth;
    private float calculatedBodyActualHeight;


    public ShootingEnemy() {
        super(); // Enemy 생성자 호출
    }

    public static ShootingEnemy get(int level, int index, EnemyType type) {
        ShootingEnemy enemy = (ShootingEnemy) Scene.top().getRecyclable(ShootingEnemy.class);
        if (enemy == null) {
            enemy = new ShootingEnemy();
        }
        enemy.initShootingEnemy(level, index, type);
        return enemy;
    }

    private void initShootingEnemy(int level, int index, EnemyType type) {
        super.init(level, index); // 부모 init에서 x, y 등 기본 위치 설정
        this.enemyType = type;

        // 타입에 따른 리소스 및 스탯 설정
        switch (type) {
            case TYPE_1:
                bodySpriteId = R.mipmap.enemy_fighter_body;
                this.life = this.maxLife = (level + 1) * 20;
                this.bulletPattern = BulletPattern.SINGLE; // 가장 단순한 패턴
                this.targetWidth = 180f;
                this.collisionInsetRatio = 0.3f;
                break;
            case TYPE_2:
                bodySpriteId = R.mipmap.enemy_fighter2_body;
                this.life = this.maxLife = (level + 1) * 70; // 체력 증가
                // 더 복잡한 패턴 중에서 랜덤 선택
                this.bulletPattern = Math.random() > 0.5 ? BulletPattern.TRIPLE : BulletPattern.SPREAD;
                this.targetWidth = 240f;
                this.collisionInsetRatio = 0.25f;
                break;
            case TYPE_3:
                bodySpriteId = R.mipmap.enemy_fighter3_body;
                this.life = this.maxLife = (level + 1) * 120; // 체력 대폭 증가
                // 가장 어려운 패턴 중에서 랜덤 선택
                this.bulletPattern = Math.random() > 0.5 ? BulletPattern.PLAYER_AIM : BulletPattern.SPREAD;
                this.targetWidth = 320f;
                this.collisionInsetRatio = 0.2f;
                break;
        }

        // 초기 위치 저장
        this.initialX = this.x;

        // 랜덤하게 이동 패턴 선택
        MovementPattern[] patterns = MovementPattern.values();
        this.movementPattern = patterns[(int)(Math.random() * patterns.length)];

        Log.d(TAG, "ShootingEnemy " + type + " initialized with pattern: " + movementPattern + ", bullet: " + bulletPattern);

        // 1. 본체 스프라이트 로드 및 원본 비율에 따른 크기 계산
        Bitmap bodyBitmap = BitmapPool.get(bodySpriteId);
        if (bodyBitmap == null) {
            Log.e(TAG, "Failed to load bodyBitmap! Check bodySpriteId: " + bodySpriteId);
            return;
        }
        float originalBodyWidth = bodyBitmap.getWidth();
        float originalBodyHeight = bodyBitmap.getHeight();
        if (originalBodyWidth == 0) { // 비정상적인 비트맵 로드 방지
            Log.e(TAG, "BodyBitmap width is zero! bodySpriteId: " + bodySpriteId);
            return;
        }
        float bodyAspectRatio = originalBodyHeight / originalBodyWidth;

        calculatedBodyActualWidth = this.targetWidth;
        calculatedBodyActualHeight = calculatedBodyActualWidth * bodyAspectRatio;

        if (bodySprite == null) bodySprite = new Sprite(bodySpriteId);
        else bodySprite.setImageResourceId(bodySpriteId);


        // 3. ShootingEnemy 객체 자체의 전체적인 크기 업데이트
        this.width = calculatedBodyActualWidth;
        this.height = calculatedBodyActualHeight;
        this.radius = Math.min(this.width, this.height) / 2;
        RectUtil.setRect(this.dstRect, this.x, this.y, this.width, this.height);

        // 4. 본체의 정확한 위치 설정
        bodySprite.setPosition(this.x, this.y, calculatedBodyActualWidth, calculatedBodyActualHeight);


        this.bitmap = null; // 부모의 비트맵은 사용 안 함
        fireCoolTime = FIRE_INTERVAL_BASE + (float) Math.random();
    }

    @Override
    public void update() {
        // 이동 패턴 업데이트
        updateMovementPattern();
        
        // y 위치가 특정 지점에 도달하면 멈춤
        if (y < Metrics.height * 0.4f) {
            super.update(); // this.x, this.y 위치 변경
        } else {
            dy = 0; // y 방향 속도를 0으로 만들어 멈춤
        }
        
        // init에서 계산된 크기를 사용하여 위치 업데이트
        if (bodySprite != null) { // Null 체크 추가
            bodySprite.setPosition(this.x, this.y, calculatedBodyActualWidth, calculatedBodyActualHeight);
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
        // 부모의 draw()를 호출하지 않고 자체적으로 그림
        // super.draw(canvas)를 호출하면 부모가 가진 이미지(지금은 null)를 그리려 하므로 주석 처리
        if (bodySprite != null) {
            bodySprite.draw(canvas);
        }

        // 체력 게이지를 충돌 영역에 맞춰 그리기
        RectF collisionRect = getCollisionRect();
        float gauge_width = collisionRect.width();
        float gauge_x = collisionRect.left;
        float gauge_y = collisionRect.bottom + 5; // 충돌 영역 바로 아래에 약간의 간격을 두고 표시

        if (maxLife > 0) {
            gauge.draw(canvas, gauge_x, gauge_y, gauge_width, (float)life / maxLife);
        }
    }

    private void fireBullet() {
        // bodySprite가 null일 경우를 대비한 방어 코드
        if (bodySprite == null) {
            Log.e(TAG, "fireBullet called but bodySprite is null!");
            return;
        }
        
        int bulletPower = 1; // 총알 위력 증가
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
                Fighter player = mainScene.getFighter();
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
        // MainScene에서 플레이어 참조를 직접 가져오도록 수정
        return scene.getFighter();
    }

    @Override
    public RectF getCollisionRect() {
        if (bodySprite != null) {
            RectF rect = new RectF();
            float bodyX = bodySprite.getX();
            float bodyY = bodySprite.getY();
            
            // 플레이어와 동일한 방식: 전체 크기에서 inset 적용
            RectUtil.setRect(rect, bodyX, bodyY, calculatedBodyActualWidth, calculatedBodyActualHeight);
            rect.inset(calculatedBodyActualWidth * collisionInsetRatio, calculatedBodyActualHeight * collisionInsetRatio);
            
            return rect;
        }
        Log.w(TAG, "bodySprite is null in getCollisionRect, returning overall dstRect.");
        return this.dstRect;
    }
}
