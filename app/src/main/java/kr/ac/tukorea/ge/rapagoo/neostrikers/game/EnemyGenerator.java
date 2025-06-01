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
    public static final float GEN_INTERVAL = 5.0f; // 다음 웨이브까지의 시간 (초)
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

        // 웨이브 당 생성할 적의 수 (예시)
        int enemiesPerWave = 5;

        for (int i = 0; i < enemiesPerWave; i++) {
            // 적 레벨 결정 로직 (기존과 유사하게)
            // 이 레벨은 ShootingEnemy.get() 내부에서 다양성을 주는데 사용될 수 있습니다.
            // (예: 레벨에 따라 체력, 발사 간격, 사용하는 총알 종류 등을 다르게 설정)
            int level = (wave + 8) / 10 - random.nextInt(3);
            if (level < 0) level = 0;
            // Enemy.MAX_LEVEL 대신 ShootingEnemy 또는 공통 상위 클래스에 정의된 최대 레벨을 사용하는 것이 좋습니다.
            // 여기서는 임시로 Enemy.MAX_LEVEL을 사용하거나, ShootingEnemy에 맞게 조정합니다.
            // 예를 들어, ShootingEnemy가 항상 동일한 타입이라면 level 변수가 크게 의미 없을 수도 있습니다.
            // 또는 ShootingEnemy.initShootingEnemy(level, index) 내부에서 level을 활용하도록 구현해야 합니다.
            if (Enemy.MAX_LEVEL > 0 && level > Enemy.MAX_LEVEL) level = Enemy.MAX_LEVEL;


            // 항상 ShootingEnemy만 생성하도록 수정
            Log.d(TAG, "Spawning ShootingEnemy at index " + i + " for wave " + wave + " with level " + level);
            ShootingEnemy shootingEnemy = ShootingEnemy.get(level, i);
            scene.add(shootingEnemy); // Enemy를 상속했으므로 ILayerProvider를 통해 자동으로 enemy 레이어에 추가됨
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
