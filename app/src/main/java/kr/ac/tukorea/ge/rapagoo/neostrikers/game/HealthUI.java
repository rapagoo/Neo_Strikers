package kr.ac.tukorea.ge.rapagoo.neostrikers.game;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.RectF;

import kr.ac.tukorea.ge.rapagoo.neostrikers.R;
import kr.ac.tukorea.ge.spgp2025.a2dg.framework.interfaces.IGameObject;
import kr.ac.tukorea.ge.spgp2025.a2dg.framework.res.BitmapPool;
import kr.ac.tukorea.ge.spgp2025.a2dg.framework.view.Metrics;

public class HealthUI implements IGameObject {
    private final Fighter fighter;
    private final Bitmap healthIcon;
    private final float iconSize;
    private final float margin;
    private final RectF rect = new RectF();

    public HealthUI(Fighter fighter) {
        this.fighter = fighter;
        this.healthIcon = BitmapPool.get(R.mipmap.fighter);
        this.iconSize = 80f; // 아이콘 크기
        this.margin = 20f;   // 아이콘 간격
    }

    @Override
    public void update() {
    }

    @Override
    public void draw(Canvas canvas) {
        int health = fighter.getHealth();
        float x = Metrics.width - margin - iconSize; // 오른쪽 하단 기준
        float y = Metrics.height - margin - iconSize; // 오른쪽 하단 기준

        for (int i = 0; i < health; i++) {
            rect.set(x, y, x + iconSize, y + iconSize);
            canvas.drawBitmap(healthIcon, null, rect, null);
            x -= (iconSize + margin); // 왼쪽으로 아이콘 이동
        }
    }
} 