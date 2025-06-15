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
            if (e >= enemies.size()) continue;
            IGameObject enemyObject = enemies.get(e);

            for (int pb = playerBullets.size() - 1; pb >= 0; pb--) {
                if (pb >= playerBullets.size()) continue;
                Bullet playerBullet = (Bullet) playerBullets.get(pb);

                if (enemyObject instanceof Boss) {
                    Boss boss = (Boss) enemyObject;
                    if (CollisionHelper.collides(boss, playerBullet)) {
                        scene.remove(playerBullet);
                        boss.decreaseLife(playerBullet.getPower());
                        break;
                    }
                } else if (enemyObject instanceof ShootingEnemy) {
                    ShootingEnemy enemy = (ShootingEnemy) enemyObject;
                    if (CollisionHelper.collides(enemy, playerBullet)) {
                        scene.remove(playerBullet);
                        boolean dead = enemy.decreaseLife(playerBullet.getPower());
                        if (dead) {
                            float scale = 0.6f;
                            switch (enemy.getEnemyType()) {
                                case TYPE_2: scale = 0.9f; break;
                                case TYPE_3: scale = 1.2f; break;
                            }
                            scene.handleEnemyDeath(enemy);
                        }
                        break;
                    }
                } else if (enemyObject instanceof Enemy) {
                    Enemy enemy = (Enemy) enemyObject;
                    if (CollisionHelper.collides(enemy, playerBullet)) {
                        scene.remove(playerBullet);
                        boolean dead = enemy.decreaseLife(playerBullet.getPower());
                        if (dead) {
                            scene.handleEnemyDeath(enemy);
                        }
                        break;
                    }
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
            IGameObject itemObject = items.get(i);
            if (itemObject instanceof PowerUpItem) {
                PowerUpItem item = (PowerUpItem) itemObject;
                if (CollisionHelper.collides(fighter, item)) {
                    scene.remove(item);
                    fighter.increasePowerLevel();
                }
            } else if (itemObject instanceof BombItem) {
                BombItem item = (BombItem) itemObject;
                if (CollisionHelper.collides(fighter, item)) {
                    scene.remove(item);
                    fighter.increaseBombCount();
                }
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
            if (e >= enemies.size()) continue;
            IGameObject enemyObject = enemies.get(e);

            if (enemyObject instanceof Boss) {
                Boss boss = (Boss) enemyObject;
                if (CollisionHelper.collides(fighter, boss)) {
                    Log.d(TAG, "Player collided with the BOSS!");
                    fighter.decreaseHealth(3);
                    if (fighter.isDead()) break;
                }
            } else if (enemyObject instanceof Enemy) {
                Enemy enemy = (Enemy) enemyObject;
                if (CollisionHelper.collides(fighter, enemy)) {
                    Log.d(TAG, "Player collided with an enemy!");
                    fighter.decreaseHealth(1);
                    scene.handleEnemyDeath(enemy);
                    if (fighter.isDead()) break;
                }
            }
        }
    }

    // 플레이어 - 적 총알 충돌 검사 메소드
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
            IGameObject bulletObject = enemyBullets.get(eb);

            if (bulletObject instanceof EnemyBullet) {
                EnemyBullet enemyBullet = (EnemyBullet) bulletObject;
                if (CollisionHelper.collides(fighter, enemyBullet)) {
                    Log.d(TAG, "Player hit by an enemy bullet!");
                    fighter.decreaseHealth(enemyBullet.getPower());
                    scene.remove(enemyBullet);
                    if (fighter.isDead()) break;
                }
            } else if (bulletObject instanceof BossBullet) {
                BossBullet bossBullet = (BossBullet) bulletObject;
                if (CollisionHelper.collides(fighter, bossBullet)) {
                    Log.d(TAG, "Player hit by a BOSS bullet!");
                    fighter.decreaseHealth(bossBullet.getPower());
                    scene.remove(MainScene.Layer.enemy_bullet, bossBullet);
                    if (fighter.isDead()) break;
                }
            }
        }
    }

    @Override
    public void draw(Canvas canvas) {
        // 시각적 요소 없음
    }
}
