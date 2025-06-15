package kr.ac.tukorea.ge.rapagoo.neostrikers.game;

import android.graphics.RectF;

import kr.ac.tukorea.ge.rapagoo.neostrikers.R;
import kr.ac.tukorea.ge.spgp2025.a2dg.framework.interfaces.IBoxCollidable;
import kr.ac.tukorea.ge.spgp2025.a2dg.framework.interfaces.ILayerProvider;
import kr.ac.tukorea.ge.spgp2025.a2dg.framework.interfaces.IRecyclable;
import kr.ac.tukorea.ge.spgp2025.a2dg.framework.objects.AnimSprite;
import kr.ac.tukorea.ge.spgp2025.a2dg.framework.scene.Scene;
import kr.ac.tukorea.ge.spgp2025.a2dg.framework.view.Metrics;

public class BombItem extends AnimSprite implements IRecyclable, IBoxCollidable, ILayerProvider<MainScene.Layer> {

    private static final float ITEM_WIDTH = 60f;
    private static final float ITEM_HEIGHT = 60f;
    private static final float SPEED = 200f;
    protected RectF collisionRect = new RectF();

    protected BombItem() {
        super(0, 0, 0); // AnimSprite 생성자 호출
    }

    // 객체 풀에서 가져오는 static factory method
    public static BombItem get(float x, float y) {
        BombItem item = (BombItem) Scene.top().getRecyclable(BombItem.class);
        if (item == null) {
            item = new BombItem();
        }
        item.init(x, y);
        return item;
    }

    private void init(float x, float y) {
        setImageResourceId(R.mipmap.bomb_item_sprite, 15); // 스프라이트 시트와 프레임 수
        setPosition(x, y, ITEM_WIDTH, ITEM_HEIGHT);
        dy = SPEED;
        updateCollisionRect();
    }

    @Override
    public void update() {
        super.update();

        if (dstRect.top > Metrics.height) {
            Scene.top().remove(this);
        } else {
            updateCollisionRect();
        }
    }

    private void updateCollisionRect() {
        collisionRect.set(dstRect);
    }

    @Override
    public RectF getCollisionRect() {
        return collisionRect;
    }

    @Override
    public void onRecycle() {
    }

    @Override
    public MainScene.Layer getLayer() {
        return MainScene.Layer.item;
    }
} 