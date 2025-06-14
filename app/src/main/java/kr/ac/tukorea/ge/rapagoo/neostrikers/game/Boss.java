package kr.ac.tukorea.ge.rapagoo.neostrikers.game;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import kr.ac.tukorea.ge.rapagoo.neostrikers.R;
import kr.ac.tukorea.ge.spgp2025.a2dg.framework.interfaces.IBoxCollidable;
import kr.ac.tukorea.ge.spgp2025.a2dg.framework.objects.Sprite;
import kr.ac.tukorea.ge.spgp2025.a2dg.framework.scene.Scene;
import kr.ac.tukorea.ge.spgp2025.a2dg.framework.view.GameView;
import kr.ac.tukorea.ge.spgp2025.a2dg.framework.view.Metrics;

public class Boss extends Sprite implements IBoxCollidable {
    private int life, maxLife;
    protected RectF collisionRect = new RectF();

    // 커스텀 체력바용 Paint 객체들
    private Paint healthBarBgPaint;
    private Paint healthBarFgPaint;
    private Paint healthBarBorderPaint;
    private RectF healthBarRect = new RectF();

    private enum State { APPEARING, BATTLE, DYING }
    private State state = State.APPEARING;

    private float moveSpeed = 100f;
    private float elapsedTime = 0;

    // 공격 패턴 관련
    private enum AttackPattern { SPREAD, SPIRAL, AIMED }
    private AttackPattern currentPattern = AttackPattern.SPREAD;
    private float patternChangeInterval = 8.0f; // 패턴 변경 시간을 늘림
    private float patternTimer = 0;
    private float fireInterval = 0.3f; // 기본 발사 간격을 늘림
    private float fireTimer = 0;
    private int spiralAngle = 0;

    public Boss() {
        super(R.mipmap.enemy_boss_body);
        // 보스 크기를 줄임 (기존 640f에서 320f로)
        setPosition(Metrics.width / 2, -300, 320f);
        this.maxLife = this.life = 5000;
        updateCollisionRect();
        
        // 커스텀 체력바 Paint 초기화
        initHealthBarPaints();
    }

    private void initHealthBarPaints() {
        // 배경 (어두운 회색)
        healthBarBgPaint = new Paint();
        healthBarBgPaint.setColor(Color.rgb(40, 40, 40));
        healthBarBgPaint.setAntiAlias(true);

        // 전경 (빨간색에서 주황색으로)
        healthBarFgPaint = new Paint();
        healthBarFgPaint.setAntiAlias(true);

        // 테두리 (흰색)
        healthBarBorderPaint = new Paint();
        healthBarBorderPaint.setColor(Color.WHITE);
        healthBarBorderPaint.setStyle(Paint.Style.STROKE);
        healthBarBorderPaint.setStrokeWidth(4f);
        healthBarBorderPaint.setAntiAlias(true);
    }

    @Override
    public void update() {
        elapsedTime += GameView.frameTime;

        switch (state) {
            case APPEARING:
                y += 100f * GameView.frameTime;
                if (y >= Metrics.height * 0.2f) {
                    y = Metrics.height * 0.2f;
                    state = State.BATTLE;
                }
                break;
            case BATTLE:
                // 좌우 이동을 부드럽게
                x = Metrics.width / 2 + (float)Math.sin(elapsedTime * 0.3f) * (Metrics.width * 0.25f);

                // 공격 패턴 변경
                patternTimer += GameView.frameTime;
                if (patternTimer > patternChangeInterval) {
                    changePattern();
                    patternTimer = 0;
                }

                // 공격
                fireTimer -= GameView.frameTime;
                if (fireTimer <= 0) {
                    fire();
                }
                break;
            case DYING:
                // 사망 연출 (예: 폭발)
                // 현재는 바로 사라짐
                MainScene.top().remove(MainScene.Layer.enemy, this);
                break;
        }
        setPosition(x, y, 320f); // 크기 줄임
        updateCollisionRect();
    }

    private void changePattern() {
        int next = (currentPattern.ordinal() + 1) % AttackPattern.values().length;
        currentPattern = AttackPattern.values()[next];
    }

    private void fire() {
        MainScene scene = (MainScene) Scene.top();
        Fighter player = scene.getFighter();
        if (player == null) return;

        int bulletPower = 2; // 보스 총알 데미지
        float firePosY = y + height / 2;

        switch (currentPattern) {
            case SPREAD:
                fireTimer = 1.0f; // 발사 간격을 늘림
                // 총알 개수를 줄이고 각도를 넓힘 (피하기 쉽게)
                for (int i = -2; i <= 2; i++) {
                    float angle = i * 0.4f; // 각도를 더 넓힘
                    float speedX = (float)Math.sin(angle) * 500f; // 속도 줄임
                    float speedY = (float)Math.cos(angle) * 500f;
                    scene.add(MainScene.Layer.enemy_bullet, BossBullet.get(x, firePosY, speedX, speedY, bulletPower));
                }
                break;
            case SPIRAL:
                fireTimer = 0.15f; // 발사 간격을 늘림
                float angle = (float)Math.toRadians(spiralAngle);
                float speedX = (float)Math.cos(angle) * 400f; // 속도 줄임
                float speedY = (float)Math.sin(angle) * 400f;
                scene.add(MainScene.Layer.enemy_bullet, BossBullet.get(x, y, speedX, speedY, bulletPower));
                spiralAngle = (spiralAngle + 30) % 360; // 각도 변화를 크게
                break;
            case AIMED:
                fireTimer = 2.0f; // 발사 간격을 더 늘림
                float dx = player.getX() - x;
                float dy = player.getY() - y;
                float dist = (float)Math.sqrt(dx*dx + dy*dy);
                float aimSpeedX = (dx/dist) * 600f; // 속도 줄임
                float aimSpeedY = (dy/dist) * 600f;
                scene.add(MainScene.Layer.enemy_bullet, BossBullet.get(x, firePosY, aimSpeedX, aimSpeedY, bulletPower));
                break;
        }
    }

    @Override
    public void draw(Canvas canvas) {
        super.draw(canvas);
        
        // 커스텀 보스 체력바 그리기
        drawBossHealthBar(canvas);
    }

    private void drawBossHealthBar(Canvas canvas) {
        float healthRatio = (float)life / maxLife;
        
        // 체력바 위치와 크기 설정
        float barWidth = Metrics.width * 0.7f;
        float barHeight = 25f;
        float barX = (Metrics.width - barWidth) / 2;
        float barY = 40f;
        
        // 배경 그리기
        healthBarRect.set(barX, barY, barX + barWidth, barY + barHeight);
        canvas.drawRoundRect(healthBarRect, 12f, 12f, healthBarBgPaint);
        
        // 체력에 따른 색상 변경 (빨간색 -> 주황색 -> 노란색)
        if (healthRatio > 0.6f) {
            healthBarFgPaint.setColor(Color.rgb(255, 100, 100)); // 연한 빨간색
        } else if (healthRatio > 0.3f) {
            healthBarFgPaint.setColor(Color.rgb(255, 150, 50)); // 주황색
        } else {
            healthBarFgPaint.setColor(Color.rgb(255, 50, 50)); // 진한 빨간색
        }
        
        // 현재 체력 그리기
        if (healthRatio > 0) {
            float currentBarWidth = barWidth * healthRatio;
            healthBarRect.set(barX, barY, barX + currentBarWidth, barY + barHeight);
            canvas.drawRoundRect(healthBarRect, 12f, 12f, healthBarFgPaint);
        }
        
        // 테두리 그리기
        healthBarRect.set(barX, barY, barX + barWidth, barY + barHeight);
        canvas.drawRoundRect(healthBarRect, 12f, 12f, healthBarBorderPaint);
    }

    private void updateCollisionRect() {
        collisionRect.set(dstRect);
        collisionRect.inset(width * 0.1f, height * 0.1f);
    }

    @Override
    public RectF getCollisionRect() {
        return collisionRect;
    }

    public boolean decreaseLife(int power) {
        life -= power;
        if (life <= 0) {
            state = State.DYING;
            return true;
        }
        return false;
    }

    public boolean isDead() {
        return life <= 0;
    }

    public int getScore() {
        return 50000; // 보스 처치 점수
    }
} 