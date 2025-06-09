// CollisionChecker.java
package kr.ac.tukorea.ge.rapagoo.neostrikers.game;

import android.graphics.Canvas;
import android.util.Log;

import java.util.ArrayList;
import java.util.Random;

import kr.ac.tukorea.ge.spgp2025.a2dg.framework.interfaces.IGameObject;
import kr.ac.tukorea.ge.spgp2025.a2dg.framework.util.CollisionHelper;

public class CollisionChecker implements IGameObject {
    private static final String TAG = CollisionChecker.class.getSimpleName();
    private final MainScene scene;
    private final Random random = new Random();

    private static final float POWERUP_DROP_RATE = 0.2f;

    public CollisionChecker(MainScene mainScene) {
        this.scene = mainScene;
    }

    @Override
    public void update() {
        // 적과 플레이어 총알 충돌 검사
        checkEnemyPlayerBulletCollisions();

        // 플레이어와 아이템 충돌 검사
        checkPlayerItemCollisions();

        // 플레이어와 적 충돌 검사
        checkPlayerEnemyCollisions();

        // 플레이어와 적 총알 충돌 검사 << 새로 추가된 부분
        checkPlayerEnemyBulletCollisions();
    }

    private void checkEnemyPlayerBulletCollisions() {
        ArrayList<IGameObject> enemies = scene.objectsAt(MainScene.Layer.enemy);
        if (enemies == null || enemies.isEmpty()) return;

        ArrayList<IGameObject> playerBullets = scene.objectsAt(MainScene.Layer.bullet); // 플레이어 총알
        if (playerBullets == null || playerBullets.isEmpty()) return;

        for (int e = enemies.size() - 1; e >= 0; e--) {
            if (e >= enemies.size()) continue; // 방어 코드
            Enemy enemy = (Enemy) enemies.get(e);
            for (int pb = playerBullets.size() - 1; pb >= 0; pb--) {
                if (pb >= playerBullets.size()) continue; // 방어 코드
                Bullet playerBullet = (Bullet) playerBullets.get(pb);

                if (CollisionHelper.collides(enemy, playerBullet)) {
                    scene.remove(playerBullet);
                    boolean dead = enemy.decreaseLife(playerBullet.getPower());
                    if (dead) {
                        // 폭발 효과 생성
                        Explosion explosion = Explosion.get(enemy.getX(), enemy.getY());
                        scene.add(MainScene.Layer.effect, explosion);
                        
                        scene.remove(MainScene.Layer.enemy, enemy);
                        scene.addScore(enemy.getScore());
                        if (random.nextFloat() < POWERUP_DROP_RATE) {
                            PowerUpItem item = PowerUpItem.get(enemy.getX(), enemy.getY());
                            scene.add(MainScene.Layer.item, item);
                        }
                    }
                    break;
                }
            }
        }
    }

    private void checkPlayerItemCollisions() {
        ArrayList<IGameObject> items = scene.objectsAt(MainScene.Layer.item);
        if (items == null || items.isEmpty()) return;

        Fighter fighter = scene.getFighter(); // MainScene에서 fighter 직접 가져오기
        if (fighter == null || fighter.isDead()) return;

        for (int i = items.size() - 1; i >= 0; i--) {
            if (i >= items.size()) continue; // 방어 코드
            PowerUpItem item = (PowerUpItem) items.get(i);
            if (CollisionHelper.collides(fighter, item)) {
                scene.remove(item);
                fighter.increasePowerLevel();
            }
        }
    }

    private void checkPlayerEnemyCollisions() {
        ArrayList<IGameObject> enemies = scene.objectsAt(MainScene.Layer.enemy);
        if (enemies == null || enemies.isEmpty()) return;

        Fighter fighter = scene.getFighter();
        if (fighter == null || fighter.isDead() || fighter.isInvincible()) {
            return;
        }

        for (int e = enemies.size() - 1; e >= 0; e--) {
            if (e >= enemies.size()) continue; // 방어 코드
            Enemy enemy = (Enemy) enemies.get(e);
            if (CollisionHelper.collides(fighter, enemy)) {
                Log.d(TAG, "Player collided with an enemy!");
                
                // 폭발 효과 생성
                Explosion explosion = Explosion.get(enemy.getX(), enemy.getY());
                scene.add(MainScene.Layer.effect, explosion);
                
                fighter.decreaseHealth(1); // 적과 직접 충돌 시 데미지 1
                scene.remove(MainScene.Layer.enemy, enemy); // 충돌한 적은 제거
                if (fighter.isDead()) break; // 플레이어가 죽으면 더 이상 검사 안 함
            }
        }
    }

    // 플레이어 - 적 총알 충돌 검사 메소드 << 새로 추가된 메소드
    private void checkPlayerEnemyBulletCollisions() {
        ArrayList<IGameObject> enemyBullets = scene.objectsAt(MainScene.Layer.enemy_bullet);
        if (enemyBullets == null || enemyBullets.isEmpty()) return;

        Fighter fighter = scene.getFighter();
        // 플레이어가 없거나, 죽었거나, 무적 상태이면 충돌 검사 안 함
        if (fighter == null || fighter.isDead() || fighter.isInvincible()) {
            return;
        }

        for (int eb = enemyBullets.size() - 1; eb >= 0; eb--) {
            if (eb >= enemyBullets.size()) continue; // 방어 코드
            EnemyBullet enemyBullet = (EnemyBullet) enemyBullets.get(eb);

            if (CollisionHelper.collides(fighter, enemyBullet)) {
                Log.d(TAG, "Player hit by an enemy bullet!");
                fighter.decreaseHealth(enemyBullet.getPower()); // 적 총알의 공격력만큼 체력 감소
                scene.remove(enemyBullet); // 충돌한 적 총알 제거

                // 플레이어가 이 충돌로 죽었다면, 더 이상 검사할 필요 없음
                if (fighter.isDead()) {
                    Log.d(TAG, "Player died from enemy bullet.");
                    break;
                }
            }
        }
    }

    @Override
    public void draw(Canvas canvas) {
        // 시각적 요소 없음
    }
}
