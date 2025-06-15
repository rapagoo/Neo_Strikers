package kr.ac.tukorea.ge.rapagoo.neostrikers.app;

import android.content.Intent;
import android.os.Bundle;
import android.view.MotionEvent;

import androidx.appcompat.app.AppCompatActivity;

import kr.ac.tukorea.ge.rapagoo.neostrikers.R;
import kr.ac.tukorea.ge.spgp2025.a2dg.framework.res.Sound;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Sound.init(this);
        // Sound.load(this, R.raw.ui_touch);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            // Sound.play(R.raw.ui_touch);
            startActivity(new Intent(this, DragonFlightActivity.class));
        }
        return super.onTouchEvent(event);
    }
}