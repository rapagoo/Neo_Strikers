package kr.ac.tukorea.ge.rapagoo.neostrikers.app;

import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AlertDialog;

import kr.ac.tukorea.ge.rapagoo.neostrikers.BuildConfig;
import kr.ac.tukorea.ge.rapagoo.neostrikers.R;
import kr.ac.tukorea.ge.rapagoo.neostrikers.game.MainScene;
import kr.ac.tukorea.ge.spgp2025.a2dg.framework.activity.GameActivity;
import kr.ac.tukorea.ge.spgp2025.a2dg.framework.res.Sound;
import kr.ac.tukorea.ge.spgp2025.a2dg.framework.scene.Scene;
import kr.ac.tukorea.ge.spgp2025.a2dg.framework.view.GameView;

public class DragonFlightActivity extends GameActivity {
    private static final String TAG = DragonFlightActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        GameView.drawsDebugStuffs = BuildConfig.DEBUG;
        super.onCreate(savedInstanceState);
        initSounds();
        // GameActivity의 생성자에서 GameView가 생성되고,
        // GameView는 Scene 스택을 관리합니다.
        // 새로운 MainScene을 생성하여 스택에 push합니다.
        if (Scene.top() == null) { // 현재 씬이 없는 경우에만 (최초 실행 또는 이전 씬이 모두 pop 된 경우)
            new MainScene().push();
        }
    }

    private void initSounds() {
        Sound.playMusic(this, R.raw.bgm_stage1);
        Sound.load(this, R.raw.se_player_fire);
        Sound.load(this, R.raw.se_enemy_destroy);
        Sound.load(this, R.raw.se_warning);
    }

    @Override
    protected void onPause() {
        Sound.pauseMusic();
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Sound.resumeMusic();
    }

    @Override
    protected void onDestroy() {
        Sound.stopMusic();
        super.onDestroy();
    }

    // MainScene에서 호출될 게임 오버 다이얼로그 표시 메소드
    public void showGameOverDialog(final int score) {
        // AlertDialog는 UI 스레드에서 실행되어야 합니다.
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                AlertDialog.Builder builder = new AlertDialog.Builder(DragonFlightActivity.this);
                builder.setTitle("게임 오버!");
                builder.setMessage("최종 점수: " + score);
                builder.setCancelable(false); // 뒤로가기 버튼으로 다이얼로그 닫기 방지

                builder.setPositiveButton("다시 시작", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Log.d(TAG, "Restarting game...");
                        // 현재 MainScene을 가져와서 restartGame() 호출 또는 새로운 Scene으로 교체
                        Scene currentScene = Scene.top();
                        if (currentScene instanceof MainScene) {
                            ((MainScene) currentScene).restartGame();
                        } else {
                            // 혹시 다른 Scene일 경우, 모든 Scene을 pop하고 새로 시작
                            GameView.view.popAllScenes(); // 모든 씬 제거
                            new MainScene().push(); // 새 MainScene 시작
                        }
                    }
                });

                builder.setNegativeButton("메인으로", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Log.d(TAG, "Returning to Main Menu / Exiting Activity...");
                        finish(); // 현재 DragonFlightActivity 종료 (MainActivity로 돌아감)
                    }
                });
                AlertDialog dialog = builder.create();
                dialog.show();
            }
        });
    }

    // 게임 승리 다이얼로그 표시 메소드
    public void showGameWinDialog(final int score) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                AlertDialog.Builder builder = new AlertDialog.Builder(DragonFlightActivity.this);
                builder.setTitle("YOU WIN!");
                builder.setMessage("최종 점수: " + score);
                builder.setCancelable(false);

                builder.setPositiveButton("다시 시작", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Scene currentScene = Scene.top();
                        if (currentScene instanceof MainScene) {
                            ((MainScene) currentScene).restartGame();
                        } else {
                            GameView.view.popAllScenes();
                            new MainScene().push();
                        }
                    }
                });

                builder.setNegativeButton("메인으로", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                    }
                });
                AlertDialog dialog = builder.create();
                dialog.show();
            }
        });
    }

    // GameActivity의 onBackPressed 콜백이 GameView의 onBackPressed를 호출하고,
    // GameView는 Scene 스택의 최상단 Scene의 onBackPressed를 호출합니다.
    // MainScene에서 onBackPressed를 특별히 처리하지 않으면 기본 동작(씬 pop 또는 액티비티 종료)이 수행됩니다.
}
