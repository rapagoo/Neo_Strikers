package kr.ac.tukorea.ge.rapagoo.neostrikers.game;

import android.graphics.Canvas;
import android.util.Log;
import android.view.MotionEvent;

import kr.ac.tukorea.ge.rapagoo.neostrikers.R;
import kr.ac.tukorea.ge.rapagoo.neostrikers.app.DragonFlightActivity; // Activity 참조를 위해 import
import kr.ac.tukorea.ge.spgp2025.a2dg.framework.interfaces.IGameObject;
import kr.ac.tukorea.ge.spgp2025.a2dg.framework.objects.Score;
import kr.ac.tukorea.ge.spgp2025.a2dg.framework.objects.VertScrollBackground;
import kr.ac.tukorea.ge.spgp2025.a2dg.framework.scene.Scene;
import kr.ac.tukorea.ge.spgp2025.a2dg.framework.view.GameView; // GameView.view.getContext() 사용
import kr.ac.tukorea.ge.spgp2025.a2dg.framework.view.Metrics;

import java.util.ArrayList; // ArrayList import 추가

public class MainScene extends Scene {
    private static final String TAG = MainScene.class.getSimpleName();
    private Fighter fighter; // fighter를 멤버 변수로 선언 (생성자에서 초기화)
    private final Score score;

    private boolean isGameOver = false; // 게임 오버 상태 플래그
    private boolean gameOverDialogShown = false; // 게임 오버 다이얼로그가 이미 표시되었는지 확인하는 플래그

    // 게임 레이어 정의
    public enum Layer {
        bg1, enemy, bullet, fighter, item, bg2, ui, controller, effect; // effect 레이어 추가 (폭발 등)
        public static final int COUNT = values().length;
    }

    public MainScene() {
        // Metrics.setGameSize(900, 1600); // 기본값 사용 시 주석 처리 또는 삭제
        initLayers(Layer.COUNT); // 레이어 초기화

        // 배경 레이어 추가
        add(Layer.bg1, new VertScrollBackground(R.mipmap.bg_city, 20));
        add(Layer.bg2, new VertScrollBackground(R.mipmap.clouds, 40));

        // 플레이어 생성 및 fighter 레이어에 추가
        this.fighter = new Fighter(); // Fighter 객체 생성
        add(Layer.fighter, this.fighter);

        // 점수 UI 생성 및 ui 레이어에 추가
        this.score = new Score(R.mipmap.number_24x32, 850f, 50f, 60f); // 위치 및 크기 조정 가능
        score.setScore(0);
        add(Layer.ui, score);

        // 컨트롤러 레이어에 EnemyGenerator와 CollisionChecker 추가
        add(Layer.controller, new EnemyGenerator(this));
        add(Layer.controller, new CollisionChecker(this));
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


    @Override
    public void update() {
        if (isGameOver) {
            if (!gameOverDialogShown) {
                if (GameView.view.getContext() instanceof DragonFlightActivity) {
                    ((DragonFlightActivity) GameView.view.getContext()).showGameOverDialog(score.getScore());
                    gameOverDialogShown = true;
                }
            }
            return;
        }

        super.update();

        if (fighter != null && fighter.isDead()) {
            isGameOver = true;
            Log.i(TAG, "Game Over! Final Score: " + score.getScore());
        }
    }

    @Override
    public void draw(Canvas canvas) { // Canvas import 필요 android.graphics.Canvas
        super.draw(canvas);
    }


    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (isGameOver) {
            return false;
        }
        if (fighter != null) {
            return fighter.onTouch(event);
        }
        return super.onTouchEvent(event);
    }

    public void restartGame() {
        Log.d(TAG, "Restarting game from MainScene...");
        isGameOver = false;
        gameOverDialogShown = false;
        score.setScore(0);

        if (fighter != null) {
            fighter.resetHealth();
            fighter.setPosition(Metrics.width / 2, Metrics.height - 150, 120f, 120f);
            fighter.resetPowerLevel(); // fighter.powerLevel = 1; 대신 resetPowerLevel() 호출
        }

        clearLayer(Layer.enemy);
        clearLayer(Layer.bullet);
        clearLayer(Layer.item);
        // clearLayer(Layer.effect);


        ArrayList<IGameObject> controllers = objectsAt(Layer.controller);
        for (int i = controllers.size() - 1; i >= 0; i--) {
            if (controllers.get(i) instanceof EnemyGenerator) {
                remove(Layer.controller, controllers.get(i));
                break;
            }
        }
        add(Layer.controller, new EnemyGenerator(this));

        Log.d(TAG, "Game has been reset.");
    }

    private void clearLayer(Layer layerEnum) {
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
