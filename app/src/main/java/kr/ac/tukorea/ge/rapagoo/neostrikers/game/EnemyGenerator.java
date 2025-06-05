// EnemyGenerator.java
package kr.ac.tukorea.ge.rapagoo.neostrikers.game;

import android.graphics.Canvas;
import android.util.Log; // Log 사용을 위해 추가

import java.util.Random;

import kr.ac.tukorea.ge.spgp2025.a2dg.framework.interfaces.IGameObject;
import kr.ac.tukorea.ge.spgp2025.a2dg.framework.view.GameView;

public class EnemyGenerator implements IGameObject {
    private static final String TAG = EnemyGenerator.class.getSimpleName();
    private final Random random = new Random();
    public static final float GEN_INTERVAL = 4.0f; // 다음 웨이브까지의 시간 (초) - 더 빠르게
    private final MainScene scene;
    private float enemyTime = 0; // 다음 웨이브까지 남은 시간
    private int wave; // 현재 웨이브 번호

    public EnemyGenerator(MainScene mainScene) {
        this.scene = mainScene;
        this.enemyTime = GEN_INTERVAL / 2; // 첫 웨이브는 조금 빨리 나오도록 설정 (선택 사항)
    }

    @Override
    public void update() {
        enemyTime -= GameView.frameTime;
        if (enemyTime < 0) {
            generate();
            enemyTime = GEN_INTERVAL; // 다음 웨이브까지 시간 재설정
        }
    }

    private void generate() {
        wave++;
        Log.d(TAG, "Generating wave: " + wave + " (ShootingEnemies only)");

        // 웨이브 당 생성할 적의 수 - 랜덤하게 변경
        int enemiesPerWave = 3 + random.nextInt(4); // 3~6개

        // 다양한 대형 패턴
        int formationPattern = random.nextInt(4);
        
        switch (formationPattern) {
            case 0: // V 대형
                generateVFormation(enemiesPerWave);
                break;
            case 1: // 랜덤 위치
                generateRandomFormation(enemiesPerWave);
                break;
            case 2: // 좌우 양옆에서
                generateSideFormation(enemiesPerWave);
                break;
            case 3: // 기존 일렬 대형 (가끔)
                generateLineFormation(enemiesPerWave);
                break;
        }
    }
    
    private void generateVFormation(int count) {
        for (int i = 0; i < count; i++) {
            int level = (wave + 8) / 10 - random.nextInt(3);
            if (level < 0) level = 0;
            if (Enemy.MAX_LEVEL > 0 && level > Enemy.MAX_LEVEL) level = Enemy.MAX_LEVEL;
            
            // V자 대형으로 배치
            float centerX = kr.ac.tukorea.ge.spgp2025.a2dg.framework.view.Metrics.width / 2;
            float offsetX = (i - count / 2.0f) * 100f + (Math.abs(i - count / 2.0f) * 50f);
            float startX = centerX + offsetX;
            float startY = -100f - (i * 80f); // 시간차 등장
            
            ShootingEnemy enemy = ShootingEnemy.get(level, i);
            enemy.setPosition(startX, startY, enemy.width, enemy.height);
            scene.add(enemy);
            
            Log.d(TAG, "V-Formation enemy " + i + " at (" + startX + ", " + startY + ")");
        }
    }
    
    private void generateRandomFormation(int count) {
        for (int i = 0; i < count; i++) {
            int level = (wave + 8) / 10 - random.nextInt(3);
            if (level < 0) level = 0;
            if (Enemy.MAX_LEVEL > 0 && level > Enemy.MAX_LEVEL) level = Enemy.MAX_LEVEL;
            
            // 랜덤 위치에서 등장
            float startX = 100f + random.nextFloat() * (kr.ac.tukorea.ge.spgp2025.a2dg.framework.view.Metrics.width - 200f);
            float startY = -150f - (random.nextFloat() * 200f); // 랜덤 시간차
            
            ShootingEnemy enemy = ShootingEnemy.get(level, i);
            enemy.setPosition(startX, startY, enemy.width, enemy.height);
            scene.add(enemy);
            
            Log.d(TAG, "Random enemy " + i + " at (" + startX + ", " + startY + ")");
        }
    }
    
    private void generateSideFormation(int count) {
        for (int i = 0; i < count; i++) {
            int level = (wave + 8) / 10 - random.nextInt(3);
            if (level < 0) level = 0;
            if (Enemy.MAX_LEVEL > 0 && level > Enemy.MAX_LEVEL) level = Enemy.MAX_LEVEL;
            
            // 좌우에서 번갈아 등장
            boolean fromLeft = (i % 2 == 0);
            float startX = fromLeft ? 50f : kr.ac.tukorea.ge.spgp2025.a2dg.framework.view.Metrics.width - 50f;
            float startY = -100f - (i * 120f);
            
            ShootingEnemy enemy = ShootingEnemy.get(level, i);
            enemy.setPosition(startX, startY, enemy.width, enemy.height);
            scene.add(enemy);
            
            Log.d(TAG, "Side enemy " + i + " from " + (fromLeft ? "left" : "right"));
        }
    }
    
    private void generateLineFormation(int count) {
        // 기존 방식
        for (int i = 0; i < count; i++) {
            int level = (wave + 8) / 10 - random.nextInt(3);
            if (level < 0) level = 0;
            if (Enemy.MAX_LEVEL > 0 && level > Enemy.MAX_LEVEL) level = Enemy.MAX_LEVEL;

            ShootingEnemy shootingEnemy = ShootingEnemy.get(level, i);
            scene.add(shootingEnemy);
        }
    }

    // EnemyGenerator는 시각적 요소가 없으므로 draw는 비워둡니다.
    @Override
    public void draw(Canvas canvas) {
        // 비워둠
    }

    // (선택) 게임 재시작 시 EnemyGenerator 상태 초기화를 위한 메소드
    public void reset() {
        this.wave = 0;
        this.enemyTime = GEN_INTERVAL / 2; // 첫 웨이브까지 시간 초기화
        Log.d(TAG, "EnemyGenerator has been reset.");
    }
}
