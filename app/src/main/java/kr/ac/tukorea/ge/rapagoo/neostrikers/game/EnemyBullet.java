package kr.ac.tukorea.ge.rapagoo.neostrikers.game;

import android.graphics.RectF;

import kr.ac.tukorea.ge.rapagoo.neostrikers.R; // R 파일 임포트 확인
import kr.ac.tukorea.ge.spgp2025.a2dg.framework.interfaces.IBoxCollidable;
import kr.ac.tukorea.ge.spgp2025.a2dg.framework.interfaces.ILayerProvider;
import kr.ac.tukorea.ge.spgp2025.a2dg.framework.interfaces.IRecyclable;
import kr.ac.tukorea.ge.spgp2025.a2dg.framework.objects.AnimSprite; // Sprite 대신 AnimSprite 사용
import kr.ac.tukorea.ge.spgp2025.a2dg.framework.scene.Scene;
import kr.ac.tukorea.ge.spgp2025.a2dg.framework.view.GameView;
import kr.ac.tukorea.ge.spgp2025.a2dg.framework.view.Metrics;

public class EnemyBullet extends AnimSprite implements IRecyclable, IBoxCollidable, ILayerProvider<MainScene.Layer> {
    private static final String TAG = EnemyBullet.class.getSimpleName();

    // 총알 크기 (애니메이션 프레임 하나의 크기에 맞춰야 함)
    private static final float BULLET_WIDTH = 50f; // 예시 크기, 실제 프레임 너비에 맞게 조절
    private static final float BULLET_HEIGHT = 50f; // 예시 크기, 실제 프레임 높이에 맞게 조절

    private int power; // 총알 공격력
    // private int imageResId; // AnimSprite는 생성자에서 이미지 ID를 받으므로 별도 저장 불필요

    // dx, dy는 AnimSprite의 부모인 Sprite에 이미 정의되어 있음
    // protected float dx, dy;

    // 기본 생성자
    public EnemyBullet() {
        // AnimSprite(int mipmapId, float fps, int frameCount)
        // mipmapId: 애니메이션 스프라이트 시트의 리소스 ID
        // fps: 초당 프레임 수 (애니메이션 속도)
        // frameCount: 스프라이트 시트 내의 총 프레임 수 (0이면 이미지 높이를 기준으로 자동 계산)
        super(R.mipmap.enemybullet_1, 8.0f, 8); // 예시: enemy_bullet_anim 이미지, 10fps, 4프레임
        // Log.d(TAG, "EnemyBullet constructor called");
    }

    // 재활용 객체를 가져오거나 새로 생성하여 초기화하는 static factory method
    public static EnemyBullet get(float x, float y, float dx, float dy, int power) {
        EnemyBullet bullet = (EnemyBullet) Scene.top().getRecyclable(EnemyBullet.class);
        if (bullet == null) {
            bullet = new EnemyBullet(); // 기본 생성자에서 애니메이션 설정
        }
        bullet.init(x, y, dx, dy, power);
        return bullet;
    }

    // 총알 초기화 메소드
    private void init(float x, float y, float dx, float dy, int power) {
        this.x = x;
        this.y = y;
        this.dx = dx; // 수평 이동 속도 설정
        this.dy = dy; // 수직 이동 속도 설정
        this.power = power;

        // 위치 및 크기 설정 (Sprite의 setPosition 메소드 활용)
        // 크기는 애니메이션 프레임 하나의 크기로 설정해야 함
        setPosition(x, y, BULLET_WIDTH, BULLET_HEIGHT);

        // AnimSprite의 경우, 생성자에서 이미지 리소스 ID, fps, frameCount를 설정하므로
        // init에서는 별도로 setImageResourceId를 호출할 필요가 없음.
        // 만약 다른 애니메이션을 사용하고 싶다면, setFrameInfo 또는 setImageResourceId(mipmapId, fps, frameCount) 호출 가능
        // 예: setImageResourceId(R.mipmap.another_bullet_anim, 12.0f, 5);
        // Log.d(TAG, "EnemyBullet initialized at: " + x + "," + y + " with speed: " + dx + "," + dy);
    }

    @Override
    public void update() {
        // AnimSprite의 부모인 Sprite의 update를 호출하여 dx, dy에 따른 위치 변경 처리
        super.update(); // x, y, dstRect가 dx, dy에 의해 업데이트됨

        // 화면 경계를 벗어나는지 확인하여 제거
        if (dstRect.bottom < 0 || dstRect.top > Metrics.height || dstRect.right < 0 || dstRect.left > Metrics.width) {
            Scene.top().remove(this);
        }
    }

    // AnimSprite는 draw 메소드를 이미 가지고 있으므로, 특별한 추가 로직이 없다면 오버라이드 불필요.
    // @Override
    // public void draw(Canvas canvas) {
    //     super.draw(canvas); // AnimSprite의 draw가 애니메이션 프레임을 자동으로 그려줌
    // }

    public int getPower() {
        return power;
    }

    @Override
    public RectF getCollisionRect() {
        return dstRect;
    }

    @Override
    public void onRecycle() {
        // 재활용 시 필요한 초기화 작업
    }

    @Override
    public MainScene.Layer getLayer() {
        // MainScene.Layer에 enemy_bullet 레이어가 정의되어 있어야 함
        return MainScene.Layer.enemy_bullet;
    }
}
