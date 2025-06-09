package kr.ac.tukorea.ge.rapagoo.neostrikers.game;

import android.graphics.Canvas;
import android.graphics.Rect;

import kr.ac.tukorea.ge.rapagoo.neostrikers.R;
import kr.ac.tukorea.ge.spgp2025.a2dg.framework.interfaces.ILayerProvider;
import kr.ac.tukorea.ge.spgp2025.a2dg.framework.interfaces.IRecyclable;
import kr.ac.tukorea.ge.spgp2025.a2dg.framework.objects.Sprite;
import kr.ac.tukorea.ge.spgp2025.a2dg.framework.scene.Scene;
import kr.ac.tukorea.ge.spgp2025.a2dg.framework.view.GameView;

public class Explosion extends Sprite implements IRecyclable, ILayerProvider<MainScene.Layer> {
    // 사용자가 직접 프레임 경계를 설정하는 배열입니다.
    // 8프레임 애니메이션의 경우, 각 프레임의 시작과 끝 x좌표를 나타내는 9개의 값을 넣어야 합니다.
    // 예: {x0, x1, x2, x3, x4, x5, x6, x7, x8}
    // 0번 프레임은 x0 ~ x1, 1번 프레임은 x1 ~ x2 ... 와 같이 사용됩니다.
    // 가지고 계신 `enemy_fighter_explosion.png` 파일을 열어 각 프레임의 x좌표를 확인하고 이 배열을 수정하세요.
    private static final int[] FRAME_X_BOUNDARIES = {
            0, 72, 144, 216, 288, 360, 432, 504, 576
    };
    private static final int FRAME_COUNT = FRAME_X_BOUNDARIES.length - 1;
    private static final float FPS = 10.0f;
    private static final int EXPLOSION_ANIMATION_ID = R.mipmap.enemy_fighter_explosion;

    private float lifeTime;
    private float totalTime;

    public Explosion() {
        super(EXPLOSION_ANIMATION_ID);
        this.totalTime = (float) FRAME_COUNT / FPS;
        this.srcRect = new Rect();
    }

    public static Explosion get(float x, float y, float width) {
        Explosion explosion = (Explosion) Scene.top().getRecyclable(Explosion.class);
        if (explosion == null) {
            explosion = new Explosion();
        }
        explosion.init(x, y, width);
        return explosion;
    }

    private void init(float x, float y, float width) {
        // 가장 넓은 프레임의 비율에 맞춰 높이를 계산하여, 애니메이션이 찌그러지지 않도록 합니다.
        int maxFrameWidth = 0;
        for (int i = 0; i < FRAME_COUNT; i++) {
            int frameW = FRAME_X_BOUNDARIES[i+1] - FRAME_X_BOUNDARIES[i];
            if (frameW > maxFrameWidth) {
                maxFrameWidth = frameW;
            }
        }

        float height = width;
        if (bitmap != null && maxFrameWidth > 0) {
            float aspectRatio = (float) bitmap.getHeight() / maxFrameWidth;
            height = width * aspectRatio;
        }

        setPosition(x, y, width, height);
        this.lifeTime = 0;
    }

    @Override
    public void update() {
        lifeTime += GameView.frameTime;
        if (lifeTime > totalTime) {
            Scene.top().remove(this);
        }
    }

    @Override
    public void draw(Canvas canvas) {
        if (bitmap == null) return;
        int frameIndex = Math.round(lifeTime * FPS);
        if (frameIndex >= FRAME_COUNT) {
            frameIndex = FRAME_COUNT - 1;
        }

        int left = FRAME_X_BOUNDARIES[frameIndex];
        int right = FRAME_X_BOUNDARIES[frameIndex + 1];
        int top = 0;
        int bottom = bitmap.getHeight();
        srcRect.set(left, top, right, bottom);

        canvas.drawBitmap(bitmap, srcRect, dstRect, null);
    }


    @Override
    public void onRecycle() {
    }

    @Override
    public MainScene.Layer getLayer() {
        return MainScene.Layer.effect;
    }
} 