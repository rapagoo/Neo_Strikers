package kr.ac.tukorea.ge.rapagoo.neostrikers.game;

import android.graphics.RectF;

import kr.ac.tukorea.ge.rapagoo.neostrikers.R; // R 파일 임포트 확인
import kr.ac.tukorea.ge.spgp2025.a2dg.framework.interfaces.IBoxCollidable;
import kr.ac.tukorea.ge.spgp2025.a2dg.framework.interfaces.ILayerProvider;
import kr.ac.tukorea.ge.spgp2025.a2dg.framework.interfaces.IRecyclable;
import kr.ac.tukorea.ge.spgp2025.a2dg.framework.objects.Sprite;
import kr.ac.tukorea.ge.spgp2025.a2dg.framework.scene.Scene;
import kr.ac.tukorea.ge.spgp2025.a2dg.framework.view.Metrics;

public class PowerUpItem extends Sprite implements IRecyclable, IBoxCollidable, ILayerProvider<MainScene.Layer> {

    private static final float ITEM_WIDTH = 60f; // 아이템 너비 (조절 가능)
    private static final float ITEM_HEIGHT = 60f; // 아이템 높이 (조절 가능)
    private static final float SPEED = 200f; // 아이템 하강 속도 (조절 가능)
    protected RectF collisionRect = new RectF();

    // 재활용 위한 생성자 (protected)
    protected PowerUpItem() {
        // 실제 이미지는 init에서 설정하므로 여기서는 0 또는 기본값 사용 가능
        super(R.mipmap.powerup_bolt); // <<-- 실제 파워업 아이템 이미지 리소스 ID로 변경 필요!
        dy = SPEED; // 아래로 이동
    }

    // 객체 풀에서 가져오는 static factory method
    public static PowerUpItem get(float x, float y) {
        // Scene에서 재활용 가능한 PowerUpItem 객체를 가져옴
        PowerUpItem item = (PowerUpItem) Scene.top().getRecyclable(PowerUpItem.class);
        if (item == null) {
            // 재활용할 객체가 없으면 새로 생성 (실제로는 getRecyclable 내부에서 처리될 수 있음)
            item = new PowerUpItem();
        }
        // 아이템 초기화
        item.init(x, y);
        return item;
    }

    // 아이템 초기화 (위치, 크기 설정)
    private void init(float x, float y) {
        // 이미지 리소스 설정 (생성자에서 설정했다면 생략 가능)
        // setImageResourceId(R.mipmap.powerup_item_placeholder); // <<-- 실제 이미지 ID
        setPosition(x, y, ITEM_WIDTH, ITEM_HEIGHT);
        updateCollisionRect(); // 충돌 영역 초기화
    }

    @Override
    public void update() {
        // Sprite의 기본 이동 로직 사용
        super.update();

        // 화면 아래로 벗어나면 제거
        if (dstRect.top > Metrics.height) {
            Scene.top().remove(this); // 씬에서 제거 (자동으로 재활용 풀로 이동)
        } else {
            updateCollisionRect(); // 이동 후 충돌 영역 업데이트
        }
    }

    // 충돌 영역 계산 및 업데이트
    private void updateCollisionRect() {
        collisionRect.set(dstRect);
        // 필요시 충돌 영역 미세 조정 (예: collisionRect.inset(5f, 5f);)
    }

    // IBoxCollidable 인터페이스 구현
    @Override
    public RectF getCollisionRect() {
        return collisionRect;
    }

    // IRecyclable 인터페이스 구현 (재활용될 때 호출됨)
    @Override
    public void onRecycle() {
        // 재활용 시 필요한 초기화 작업 (예: 특정 상태 초기화)
        // 여기서는 특별히 초기화할 상태가 없을 수 있음
    }

    // ILayerProvider 인터페이스 구현 (아이템이 속할 레이어 지정)
    @Override
    public MainScene.Layer getLayer() {
        // 아이템을 위한 별도 레이어를 MainScene.Layer에 추가하거나,
        // 임시로 다른 레이어(예: enemy)를 사용할 수 있음
        // return MainScene.Layer.enemy; // 임시
        return MainScene.Layer.item; // <<-- MainScene.Layer에 item 레이어 추가 필요!
    }
}