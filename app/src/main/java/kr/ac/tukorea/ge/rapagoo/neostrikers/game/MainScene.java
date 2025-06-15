package kr.ac.tukorea.ge.rapagoo.neostrikers.game;

import android.graphics.Canvas;
import android.util.Log;
import android.view.MotionEvent;

import kr.ac.tukorea.ge.rapagoo.neostrikers.R;
import kr.ac.tukorea.ge.rapagoo.neostrikers.app.DragonFlightActivity; // Activity 참조를 위해 import
import kr.ac.tukorea.ge.spgp2025.a2dg.framework.interfaces.IGameObject;
import kr.ac.tukorea.ge.spgp2025.a2dg.framework.objects.Score;
import kr.ac.tukorea.ge.spgp2025.a2dg.framework.objects.Sprite;
import kr.ac.tukorea.ge.spgp2025.a2dg.framework.objects.VertScrollBackground;
import kr.ac.tukorea.ge.spgp2025.a2dg.framework.res.Sound;
import kr.ac.tukorea.ge.spgp2025.a2dg.framework.scene.Scene;
import kr.ac.tukorea.ge.spgp2025.a2dg.framework.view.GameView; // GameView.view.getContext() 사용
import kr.ac.tukorea.ge.spgp2025.a2dg.framework.view.Metrics;

import java.util.ArrayList; // ArrayList import 추가
import java.util.Random;

public class MainScene extends Scene {
    private static final String TAG = MainScene.class.getSimpleName();

    public enum GameState {
        PLAYING, BOSS_WARNING, BOSS_BATTLE, GAME_CLEAR_SEQUENCE, GAME_OVER
    }
    private GameState gameState = GameState.PLAYING;

    private Fighter fighter; // fighter를 멤버 변수로 선언 (생성자에서 초기화)
    private final Score score;
    private HealthUI healthUI;
    private Boss boss;
    private BombButton bombButton;
    private BombCountUI bombCountUI;
    private final Random random = new Random();
    private WarningUI warningUI;

    private static final float POWERUP_DROP_RATE = 0.15f;
    private static final float BOMB_DROP_RATE = 0.1f;

    private boolean isGameOver = false; // 게임 오버 상태 플래그
    private boolean isGameWon = false;  // 게임 승리 상태 플래그
    private boolean gameOverDialogShown = false; // 게임 오버 다이얼로그가 이미 표시되었는지 확인하는 플래그

    // 게임 레이어 정의
    public enum Layer {
        bg1, enemy, enemy_bullet, bullet, fighter, item, bg2, ui, controller, effect; // effect 레이어 추가 (폭발 등)
        public static final int COUNT = values().length;
    }

    public MainScene() {
        Metrics.setGameSize(900, 1950); // 19.5:9 비율로 설정
        initLayers(Layer.COUNT); // 레이어 초기화

        // 배경 레이어 추가
        add(Layer.bg1, new VertScrollBackground(R.mipmap.bg_space, 20));
        //add(Layer.bg2, new VertScrollBackground(R.mipmap.clouds, 40));

        // 플레이어 생성 및 fighter 레이어에 추가
        this.fighter = new Fighter(); // Fighter 객체 생성
        add(Layer.fighter, this.fighter);

        // 점수 UI 생성 및 ui 레이어에 추가
        this.score = new Score(R.mipmap.number_24x32, 850f, 50f, 60f); // 위치 및 크기 조정 가능
        score.setScore(0);
        add(Layer.ui, score);

        this.healthUI = new HealthUI(this.fighter);
        add(Layer.ui, healthUI);

        this.bombCountUI = new BombCountUI(this.fighter);
        add(Layer.ui, bombCountUI);

        float bombButtonRadius = 80f;
        float bombButtonMargin = 40f;
        this.bombButton = new BombButton(
                Metrics.width - bombButtonMargin - bombButtonRadius,
                Metrics.height - bombButtonMargin - 200f - bombButtonRadius,
                bombButtonRadius
        );
        add(Layer.ui, this.bombButton);

        // 컨트롤러 레이어에 EnemyGenerator와 CollisionChecker 추가
        add(Layer.controller, new EnemyGenerator(this)); // 보스 테스트를 위해 임시 주석 처리
        add(Layer.controller, new CollisionChecker(this));

        // 보스 테스트를 위해 즉시 보스전 시작
        //startBossBattle();
    }

    public void handleEnemyDeath(Enemy enemy) {
        Sound.play(R.raw.se_enemy_destroy);
        // 1. Determine explosion scale
        float scale = 0.5f;
        if (enemy instanceof ShootingEnemy) {
            ShootingEnemy se = (ShootingEnemy) enemy;
            switch (se.getEnemyType()) {
                case TYPE_1: scale = 0.6f; break;
                case TYPE_2: scale = 0.9f; break;
                case TYPE_3: scale = 1.2f; break;
            }
        } else { // It's a basic Enemy
            scale = 0.4f + enemy.getLevel() * 0.1f;
        }

        // 2. Create explosion
        Explosion explosion = Explosion.get(enemy.getX(), enemy.getY(), scale);
        add(Layer.effect, explosion);

        // 3. Add score
        addScore(enemy.getScore());

        // 4. Drop item
        if (random.nextFloat() < POWERUP_DROP_RATE) {
            PowerUpItem item = PowerUpItem.get(enemy.getX(), enemy.getY());
            add(Layer.item, item);
        } else if (random.nextFloat() < BOMB_DROP_RATE) {
            BombItem bomb = BombItem.get(enemy.getX(), enemy.getY());
            add(Layer.item, bomb);
        }

        // 5. Remove enemy
        remove(Layer.enemy, enemy);
    }

    public void handleBossDeath(Boss boss) {
        Explosion explosion = Explosion.get(boss.getX(), boss.getY(), 2.5f);
        add(Layer.effect, explosion);
        addScore(boss.getScore());
        remove(Layer.enemy, boss);
    }

    // 점수 추가 메소드
    public void addScore(int amount) {
        if (isGameOver) return; // 게임 오버 시 점수 추가 안 함
        score.add(amount);
    }

    // 현재 점수 반환 메소드
    public int getScore() {
        return score.getScore();
    }

    // Fighter 객체 반환 메소드
    public Fighter getFighter() {
        return fighter;
    }

    public void startBossBattle() {
        if (gameState != GameState.PLAYING) return;

        this.gameState = GameState.BOSS_WARNING;
        this.warningUI = new WarningUI();
        add(Layer.ui, warningUI);

        // 기존 적 생성기를 멈추고 적들을 제거
        ArrayList<IGameObject> controllers = objectsAt(Layer.controller);
        for (IGameObject controller : controllers) {
            if (controller instanceof EnemyGenerator) {
                ((EnemyGenerator) controller).stop(); // Assuming EnemyGenerator has a stop() method
            }
        }
        clearLayer(Layer.enemy);
        clearLayer(Layer.enemy_bullet);
    }

    private void spawnBoss() {
        clearLayer(Layer.item);
        this.boss = new Boss();
        add(Layer.enemy, this.boss);
        this.gameState = GameState.BOSS_BATTLE;
    }

    @Override
    public void update() {
        if (gameState == GameState.BOSS_WARNING) {
            super.update(); // Update the warning UI
            if (warningUI != null && warningUI.isFinished()) {
                remove(Layer.ui, warningUI);
                warningUI = null;
                spawnBoss();
            }
            return; // Don't update the rest of the game during warning
        }

        if (isGameOver || isGameWon) {
            if (!gameOverDialogShown) {
                if (GameView.view.getContext() instanceof DragonFlightActivity) {
                    if (isGameWon) {
                        ((DragonFlightActivity) GameView.view.getContext()).showGameWinDialog(score.getScore());
                    } else {
                        ((DragonFlightActivity) GameView.view.getContext()).showGameOverDialog(score.getScore());
                    }
                    gameOverDialogShown = true;
                }
            }
            return;
        }

        super.update();

        // 폭탄이 없을 때 버튼 비활성화 (시각적 처리)
        if (bombButton instanceof BombButton) {
            ((BombButton) bombButton).setDisabled(fighter.getBombCount() <= 0);
        }

        if (gameState == GameState.BOSS_BATTLE && boss != null && boss.isDead()) {
            handleBossDeath(boss); // 최종 폭발
            gameState = GameState.GAME_CLEAR_SEQUENCE;
            fighter.setControllable(false); // 플레이어 조작 비활성화
        }

        if (gameState == GameState.GAME_CLEAR_SEQUENCE) {
            fighter.setPosition(fighter.x, fighter.y - 400 * GameView.frameTime, fighter.width, fighter.height); // 위로 이동
            if (fighter.y < -fighter.height) {
                isGameWon = true; // 화면 밖으로 나가면 게임 승리
                if (!gameOverDialogShown && GameView.view.getContext() instanceof DragonFlightActivity) {
                    ((DragonFlightActivity) GameView.view.getContext()).showGameWinDialog(score.getScore());
                    gameOverDialogShown = true;
                }
            }
        }

        if (fighter != null && fighter.isDead() && gameState != GameState.GAME_OVER) {
            isGameOver = true;
            gameState = GameState.GAME_OVER;
            Log.i(TAG, "Game Over! Final Score: " + score.getScore());
        }
    }

    @Override
    public void draw(Canvas canvas) { // Canvas import 필요 android.graphics.Canvas
        super.draw(canvas);
    }


    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (isGameOver || gameState == GameState.GAME_CLEAR_SEQUENCE) {
            return false;
        }

        float[] pts = Metrics.fromScreen(event.getX(), event.getY());
        boolean isHandled = false;

        // 폭탄 버튼 터치 처리
        if (bombButton.getBounds().contains(pts[0], pts[1])) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    bombButton.setPressed(true);
                    isHandled = true;
                    break;
                case MotionEvent.ACTION_MOVE:
                    isHandled = true; // 버튼 위에서 움직이면 이벤트를 소비하여 기체가 움직이지 않도록 함
                    break;
                case MotionEvent.ACTION_UP:
                    if (bombButton.isPressed()) {
                        bombButton.setPressed(false);
                        useBomb();
                    }
                    isHandled = true;
                    break;
            }
        } else {
            // 버튼 영역 밖에서 손가락이 움직이거나 떼지면 버튼 눌림 상태 해제
            if (event.getAction() == MotionEvent.ACTION_MOVE || event.getAction() == MotionEvent.ACTION_UP) {
                bombButton.setPressed(false);
            }
        }

        // 폭탄 버튼이 이벤트를 처리했다면 여기서 반환
        if (isHandled) {
            return true;
        }

        // 폭탄 버튼이 처리하지 않았다면 파이터에게 이벤트 전달
        if (fighter != null) {
            return fighter.onTouch(event);
        }

        return super.onTouchEvent(event);
    }

    private void useBomb() {
        if (fighter.getBombCount() <= 0) {
            return;
        }
        if (fighter.useBomb()) {
            // 1. 화면 중앙에 크고 번쩍이는 폭발 효과 추가
            float centerX = Metrics.width / 2;
            float centerY = Metrics.height / 2;
            Explosion screenExplosion = Explosion.get(centerX, centerY, 3.0f, true);
            add(Layer.effect, screenExplosion);

            // 2. 모든 적에게 데미지를 주거나 즉시 사망 처리
            ArrayList<IGameObject> enemies = objectsAt(Layer.enemy);
            if (enemies != null) {
                for (int i = enemies.size() - 1; i >= 0; i--) {
                    IGameObject enemyObject = enemies.get(i);
                    if (enemyObject instanceof Boss) {
                        Boss boss = (Boss) enemyObject;
                        // 보스에게 데미지만 주고, 죽음 여부는 CollisionChecker에서 처리하므로 여기서 직접 핸들링하지 않음
                        boss.decreaseLife(500);
                    } else if (enemyObject instanceof Enemy) {
                        handleEnemyDeath((Enemy) enemyObject); // 일반 적은 즉시 사망 처리
                    }
                }
            }
            // 3. 적 총알도 모두 제거
            clearLayer(Layer.enemy_bullet);
        }
    }

    public void restartGame() {
        Log.d(TAG, "Restarting game from MainScene...");
        isGameOver = false;
        isGameWon = false;
        gameOverDialogShown = false;
        gameState = GameState.PLAYING;
        score.setScore(0);

        if (fighter != null) {
            fighter.setControllable(true);
            fighter.resetHealth();
            fighter.setPosition(Metrics.width / 2, Metrics.height - 150, 120f, 120f);
            fighter.resetPowerLevel(); // fighter.powerLevel = 1; 대신 resetPowerLevel() 호출
            fighter.resetBombCount();
        }

        clearLayer(Layer.enemy);
        clearLayer(Layer.bullet);
        clearLayer(Layer.enemy_bullet);
        clearLayer(Layer.item);
        clearLayer(Layer.effect);


        ArrayList<IGameObject> controllers = objectsAt(Layer.controller);
        for (int i = controllers.size() - 1; i >= 0; i--) {
            IGameObject controller = controllers.get(i);
            if (controller instanceof EnemyGenerator) {
                ((EnemyGenerator) controller).reset();
            }
        }

        Log.d(TAG, "Game has been reset.");
    }

    protected void clearLayer(Layer layerEnum) {
        ArrayList<IGameObject> layerObjects = objectsAt(layerEnum);
        if (layerObjects != null) {
            for (int i = layerObjects.size() - 1; i >= 0; i--) {
                remove(layerEnum, layerObjects.get(i));
            }
        }
    }

    @Override
    public void onExit() {
        super.onExit();
        Log.d(TAG, "MainScene onExit called.");
    }
}

