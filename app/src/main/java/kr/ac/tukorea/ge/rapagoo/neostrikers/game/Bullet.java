package kr.ac.tukorea.ge.rapagoo.neostrikers.game;

import android.graphics.RectF;

import kr.ac.tukorea.ge.rapagoo.neostrikers.R;
import kr.ac.tukorea.ge.spgp2025.a2dg.framework.interfaces.IBoxCollidable;
import kr.ac.tukorea.ge.spgp2025.a2dg.framework.interfaces.ILayerProvider;
import kr.ac.tukorea.ge.spgp2025.a2dg.framework.interfaces.IRecyclable;
import kr.ac.tukorea.ge.spgp2025.a2dg.framework.objects.Sprite;
import kr.ac.tukorea.ge.spgp2025.a2dg.framework.scene.Scene;
import kr.ac.tukorea.ge.spgp2025.a2dg.framework.view.GameView;
import kr.ac.tukorea.ge.spgp2025.a2dg.framework.view.Metrics;

public class Bullet extends Sprite implements IRecyclable, IBoxCollidable, ILayerProvider<MainScene.Layer> {
    private static final String TAG = Bullet.class.getSimpleName();
    private static final float BULLET_WIDTH = 68f; // 총알 너비
    private static final float BULLET_HEIGHT = BULLET_WIDTH * 40 / 28; // 총알 높이 (원본 비율 유지)
    private int power; // 총알 공격력
    private int imageResId; // 총알 이미지 리소스 ID

    // 기본 생성자: Sprite의 생성자를 호출 (mipmapId 0으로 하여 비트맵 즉시 로딩 방지)
    public Bullet() {
        super(0); // Sprite 생성자 호출 (이미지는 init에서 설정)
    }

    // 재활용 객체를 가져오거나 새로 생성하여 초기화하는 static factory method
    public static Bullet get(float x, float y, float dx, float dy, int power, int imageResId) {
        Bullet bullet = (Bullet) Scene.top().getRecyclable(Bullet.class);
        if (bullet == null) {
            // 재활용할 객체가 없으면 새로 생성
            bullet = new Bullet();
        }
        // 객체 초기화
        bullet.init(x, y, dx, dy, power, imageResId);
        return bullet;
    }

    // 총알 초기화 메소드
    private void init(float x, float y, float dx, float dy, int power, int imageResId) {
        this.x = x; // Sprite의 x 필드 직접 접근
        this.y = y; // Sprite의 y 필드 직접 접근
        this.dx = dx; // Sprite의 dx 필드 직접 접근 (수평 이동 속도)
        this.dy = dy; // Sprite의 dy 필드 직접 접근 (수직 이동 속도)
        this.power = power;
        this.imageResId = imageResId;

        // 이미지 리소스 설정
        if (this.imageResId != 0) {
            setImageResourceId(this.imageResId);
        } else {
            setImageResourceId(R.mipmap.laser_1); // 기본 이미지 (예: laser_1)
        }

        // 위치 및 크기 설정 (Sprite의 setPosition 메소드 활용)
        setPosition(x, y, BULLET_WIDTH, BULLET_HEIGHT);
    }

    @Override
    public void update() {
        // Sprite의 update를 호출하여 dx, dy에 따른 위치 변경 처리
        super.update(); // x, y, dstRect가 dx, dy에 의해 업데이트됨

        // 화면 경계를 벗어나는지 확인하여 제거
        // 화면의 상하좌우 모든 경계를 확인
        if (dstRect.bottom < 0 || dstRect.top > Metrics.height || dstRect.right < 0 || dstRect.left > Metrics.width) {
            Scene.top().remove(this); // 씬에서 제거 (자동으로 재활용 풀로 이동)
        }
    }

    // 총알의 공격력 반환
    public int getPower() {
        return power;
    }

    // IBoxCollidable 인터페이스 구현: 충돌 영역 반환
    @Override
    public RectF getCollisionRect() {
        return dstRect; // Sprite의 dstRect를 그대로 사용
    }

    // IRecyclable 인터페이스 구현: 재활용될 때 호출됨
    @Override
    public void onRecycle() {
        // 재활용 시 필요한 초기화 작업 (예: 특정 상태 초기화)
        // 여기서는 특별히 초기화할 상태가 없을 수 있음 (init에서 대부분 처리)
    }

    // ILayerProvider 인터페이스 구현: 총알이 속할 레이어 지정
    @Override
    public MainScene.Layer getLayer() {
        return MainScene.Layer.bullet;
    }
}