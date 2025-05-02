package kr.ac.tukorea.ge.rapagoo.neostrikers.app;

import android.os.Bundle;

import kr.ac.tukorea.ge.rapagoo.neostrikers.BuildConfig;
import kr.ac.tukorea.ge.rapagoo.neostrikers.game.MainScene;
import kr.ac.tukorea.ge.spgp2025.a2dg.framework.activity.GameActivity;
import kr.ac.tukorea.ge.spgp2025.a2dg.framework.view.GameView;

public class DragonFlightActivity extends GameActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        GameView.drawsDebugStuffs = BuildConfig.DEBUG;
        super.onCreate(savedInstanceState);
        new MainScene().push();
    }
}