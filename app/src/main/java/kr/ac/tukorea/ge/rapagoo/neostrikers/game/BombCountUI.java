package kr.ac.tukorea.ge.rapagoo.neostrikers.game;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.RectF;

import kr.ac.tukorea.ge.rapagoo.neostrikers.R;
import kr.ac.tukorea.ge.spgp2025.a2dg.framework.interfaces.IGameObject;
import kr.ac.tukorea.ge.spgp2025.a2dg.framework.res.BitmapPool;
import kr.ac.tukorea.ge.spgp2025.a2dg.framework.view.Metrics;

public class BombCountUI implements IGameObject {
    private final Fighter fighter;
    private final Bitmap bombIcon;
    private final float iconSize;
    private final float margin;
    private final RectF rect = new RectF();

    public BombCountUI(Fighter fighter) {
        this.fighter = fighter;
        this.bombIcon = BitmapPool.get(R.mipmap.bomb_item);
        this.iconSize = 80f;
        this.margin = 20f;
    }

    @Override
    public void update() {
    }

    @Override
    public void draw(Canvas canvas) {
        int bombCount = fighter.getBombCount();
        float x = margin; 
        float y = Metrics.height - margin - iconSize; 

        for (int i = 0; i < bombCount; i++) {
            rect.set(x, y, x + iconSize, y + iconSize);
            canvas.drawBitmap(bombIcon, null, rect, null);
            x += (iconSize + margin); 
        }
    }
} 