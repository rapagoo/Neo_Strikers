package kr.ac.tukorea.ge.rapagoo.neostrikers.game;

import android.graphics.Canvas;
import android.util.Log; // 로그 추가 (디버깅용)

import java.util.ArrayList;
import java.util.Random; // Random 클래스 임포트

import kr.ac.tukorea.ge.spgp2025.a2dg.framework.interfaces.IGameObject;
import kr.ac.tukorea.ge.spgp2025.a2dg.framework.util.CollisionHelper;
import kr.ac.tukorea.ge.spgp2025.a2dg.framework.view.GameView; // GameView 임포트 (디버깅용)

public class CollisionChecker implements IGameObject {
    private static final String TAG = CollisionChecker.class.getSimpleName();
    private final MainScene scene;
    private Random random = new Random(); // 랜덤 객체 생성

    // 아이템 드랍 확률 (예: 0.2f = 20%)
    private static final float POWERUP_DROP_RATE = 0.2f; // 필요시 확률 조정

    public CollisionChecker(MainScene mainScene) {
        this.scene = mainScene;
    }

    @Override
    public void update() {
        // 적과 총알 충돌 검사
        checkEnemyBulletCollisions();

        // 플레이어와 아이템 충돌 검사
        checkPlayerItemCollisions();

        // 플레이어와 적 충돌 검사 (향후 구현)
        // checkPlayerEnemyCollisions();

        // 적 총알과 플레이어 충돌 검사 (향후 구현)
        // checkPlayerEnemyBulletCollisions();
    }

    // 적-총알 충돌 검사 메소드
    private void checkEnemyBulletCollisions() {
        ArrayList<IGameObject> enemies = scene.objectsAt(MainScene.Layer.enemy);
        if (enemies == null || enemies.isEmpty()) return; // 적 없으면 검사 X

        ArrayList<IGameObject> bullets = scene.objectsAt(MainScene.Layer.bullet);
        if (bullets == null || bullets.isEmpty()) return; // 총알 없으면 검사 X

        for (int e = enemies.size() - 1; e >= 0; e--) {
            Enemy enemy = (Enemy) enemies.get(e);
            for (int b = bullets.size() - 1; b >= 0; b--) {
                // 총알이 유효한지 확인 (다른 충돌로 이미 제거되었을 수 있음)
                if (b >= bullets.size()) continue;
                Bullet bullet = (Bullet) bullets.get(b);

                if (CollisionHelper.collides(enemy, bullet)) {
                    // 총알 제거
                    scene.remove(bullet);

                    // 적 체력 감소 및 사망 확인
                    boolean dead = enemy.decreaseLife(bullet.getPower());
                    if (dead) {
                        // 적 제거
                        scene.remove(MainScene.Layer.enemy, enemy);
                        // 점수 추가
                        scene.addScore(enemy.getScore());

                        // 아이템 드랍 로직
                        if (random.nextFloat() < POWERUP_DROP_RATE) {
                            PowerUpItem item = PowerUpItem.get(enemy.getX(), enemy.getY());
                            scene.add(MainScene.Layer.item, item);
                            // Log.d(TAG, "PowerUpItem dropped at: " + enemy.getX() + ", " + enemy.getY());
                        }
                    }
                    // 해당 총알은 하나의 적과만 충돌하므로 내부 루프 탈출
                    break;
                }
            }
        }
    }

    // 플레이어-아이템 충돌 검사 메소드
    private void checkPlayerItemCollisions() {
        ArrayList<IGameObject> items = scene.objectsAt(MainScene.Layer.item);
        if (items == null || items.isEmpty()) return; // 아이템 없으면 검사 X

        ArrayList<IGameObject> fighters = scene.objectsAt(MainScene.Layer.fighter);
        if (fighters == null || fighters.isEmpty()) return; // 플레이어 없으면 검사 X (이론상으론 없지만 방어 코드)

        Fighter fighter = (Fighter) fighters.get(0); // 플레이어 가져오기 (씬에 하나만 있다고 가정)

        for (int i = items.size() - 1; i >= 0; i--) {
            // 아이템이 유효한지 확인
            if (i >= items.size()) continue;
            PowerUpItem item = (PowerUpItem) items.get(i);

            if (CollisionHelper.collides(fighter, item)) {
                // 아이템 제거
                scene.remove(item);
                // 플레이어 파워업 로직 호출
                fighter.increasePowerLevel();
                Log.d(TAG, "PowerUpItem collected! Current Power Level: " + fighter.getPowerLevel()); // 파워 레벨 로그 (getter 필요)
            }
        }
    }


    @Override
    public void draw(Canvas canvas) {}
}