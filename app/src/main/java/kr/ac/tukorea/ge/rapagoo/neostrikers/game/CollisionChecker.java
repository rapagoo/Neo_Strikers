package kr.ac.tukorea.ge.rapagoo.neostrikers.game;

import android.graphics.Canvas;
import android.util.Log; // 로그 추가 (디버깅용)

import java.util.ArrayList;
import java.util.Random; // Random 클래스 임포트

import kr.ac.tukorea.ge.spgp2025.a2dg.framework.interfaces.IGameObject;
import kr.ac.tukorea.ge.spgp2025.a2dg.framework.util.CollisionHelper;
// import kr.ac.tukorea.ge.spgp2025.a2dg.framework.view.GameView; // GameView 임포트는 현재 사용되지 않으므로 주석 처리 가능

public class CollisionChecker implements IGameObject {
    private static final String TAG = CollisionChecker.class.getSimpleName();
    private final MainScene scene;
    private final Random random = new Random(); // 랜덤 객체 생성 (아이템 드랍용)

    // 아이템 드랍 확률 (예: 0.2f = 20%)
    private static final float POWERUP_DROP_RATE = 0.2f; // 필요시 확률 조정

    public CollisionChecker(MainScene mainScene) {
        this.scene = mainScene;
    }

    @Override
    public void update() {
        // 적과 플레이어 총알 충돌 검사
        checkEnemyPlayerBulletCollisions();

        // 플레이어와 아이템 충돌 검사
        checkPlayerItemCollisions();

        // 플레이어와 적 충돌 검사 << 새로 추가된 부분
        checkPlayerEnemyCollisions();

        // (향후 구현) 적 총알과 플레이어 충돌 검사
        // checkPlayerEnemyBulletCollisions();
    }

    // 적 - 플레이어 총알 충돌 검사 메소드 (이름 명확화: checkEnemyPlayerBulletCollisions)
    private void checkEnemyPlayerBulletCollisions() {
        ArrayList<IGameObject> enemies = scene.objectsAt(MainScene.Layer.enemy);
        if (enemies == null || enemies.isEmpty()) return;

        ArrayList<IGameObject> bullets = scene.objectsAt(MainScene.Layer.bullet);
        if (bullets == null || bullets.isEmpty()) return;

        for (int e = enemies.size() - 1; e >= 0; e--) {
            Enemy enemy = (Enemy) enemies.get(e);
            for (int b = bullets.size() - 1; b >= 0; b--) {
                if (b >= bullets.size()) continue; // 배열 크기 변경에 따른 방어 코드
                Bullet bullet = (Bullet) bullets.get(b);

                if (CollisionHelper.collides(enemy, bullet)) {
                    scene.remove(bullet); // 총알 제거
                    boolean dead = enemy.decreaseLife(bullet.getPower());
                    if (dead) {
                        scene.remove(MainScene.Layer.enemy, enemy); // 적 제거
                        scene.addScore(enemy.getScore()); // 점수 추가

                        // 아이템 드랍 로직
                        if (random.nextFloat() < POWERUP_DROP_RATE) {
                            PowerUpItem item = PowerUpItem.get(enemy.getX(), enemy.getY());
                            scene.add(MainScene.Layer.item, item);
                            // Log.d(TAG, "PowerUpItem dropped at: " + enemy.getX() + ", " + enemy.getY());
                        }
                        // (선택) 적 폭발 효과 생성
                        // Explosion explosion = Explosion.get(enemy.getX(), enemy.getY(), Explosion.Type.ENEMY);
                        // scene.add(MainScene.Layer.effect, explosion); // effect 레이어 필요
                    }
                    break; // 이 총알은 하나의 적과만 충돌
                }
            }
        }
    }

    // 플레이어 - 아이템 충돌 검사 메소드
    private void checkPlayerItemCollisions() {
        ArrayList<IGameObject> items = scene.objectsAt(MainScene.Layer.item);
        if (items == null || items.isEmpty()) return;

        ArrayList<IGameObject> fighters = scene.objectsAt(MainScene.Layer.fighter);
        if (fighters == null || fighters.isEmpty()) return; // 플레이어가 없으면 검사 X

        Fighter fighter = (Fighter) fighters.get(0); // 씬에 플레이어는 하나만 있다고 가정

        for (int i = items.size() - 1; i >= 0; i--) {
            if (i >= items.size()) continue; // 배열 크기 변경에 따른 방어 코드
            PowerUpItem item = (PowerUpItem) items.get(i);

            if (CollisionHelper.collides(fighter, item)) {
                scene.remove(item); // 아이템 제거
                fighter.increasePowerLevel(); // 플레이어 파워업
                // Log.d(TAG, "PowerUpItem collected! Current Power Level: " + fighter.getPowerLevel());
            }
        }
    }

    // 플레이어 - 적 충돌 검사 메소드 << 새로 추가된 메소드
    private void checkPlayerEnemyCollisions() {
        ArrayList<IGameObject> enemies = scene.objectsAt(MainScene.Layer.enemy);
        if (enemies == null || enemies.isEmpty()) return;

        ArrayList<IGameObject> fighters = scene.objectsAt(MainScene.Layer.fighter);
        if (fighters == null || fighters.isEmpty()) return;

        Fighter fighter = (Fighter) fighters.get(0);
        if (fighter.isDead() || fighter.isInvincible()) { // 플레이어가 죽었거나 무적 상태면 충돌 검사 안 함
            return;
        }

        for (int e = enemies.size() - 1; e >= 0; e--) {
            if (e >= enemies.size()) continue; // 배열 크기 변경에 따른 방어 코드
            Enemy enemy = (Enemy) enemies.get(e);

            if (CollisionHelper.collides(fighter, enemy)) {
                Log.d(TAG, "Player collided with an enemy!");
                fighter.decreaseHealth(1); // 플레이어 체력 1 감소 (적의 충돌 데미지)
                scene.remove(MainScene.Layer.enemy, enemy); // 적 제거

                // (선택) 플레이어 피격 위치 또는 적 제거 위치에 폭발 효과 생성
                // Explosion explosion = Explosion.get(enemy.getX(), enemy.getY(), Explosion.Type.ENEMY_SMALL);
                // scene.add(MainScene.Layer.effect, explosion);

                // 플레이어가 죽지 않았고 무적상태가 되었다면, 더 이상 다른 적과 이번 프레임에 충돌하지 않도록 루프를 빠져나갈 수 있음
                // (또는 모든 적과의 충돌을 한 프레임에 다 처리할 수도 있음 - 현재는 한 번 충돌 시 무적이 되므로 큰 의미는 없을 수 있음)
                if (fighter.isInvincible()) {
                    // Log.d(TAG, "Player became invincible after collision, breaking enemy collision check for this frame.");
                    // break; // 이번 프레임의 추가적인 적-플레이어 충돌 검사를 중단할 수 있음 (선택적)
                }
                // 만약 플레이어가 이 충돌로 죽었다면, 더 이상 검사할 필요 없음
                if (fighter.isDead()) {
                    // Log.d(TAG, "Player died from enemy collision.");
                    break;
                }
            }
        }
    }

    @Override
    public void draw(Canvas canvas) {
        // CollisionChecker는 시각적 요소가 없으므로 draw 내용은 비워둡니다.
    }
}
