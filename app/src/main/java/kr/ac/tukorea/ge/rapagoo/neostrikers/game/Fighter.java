package kr.ac.tukorea.ge.rapagoo.neostrikers.game;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.Log; // 디버깅용 로그 추가
import android.view.MotionEvent;

import kr.ac.tukorea.ge.rapagoo.neostrikers.R;
import kr.ac.tukorea.ge.spgp2025.a2dg.framework.interfaces.IBoxCollidable; // 충돌 인터페이스 추가
import kr.ac.tukorea.ge.spgp2025.a2dg.framework.objects.Sprite;
import kr.ac.tukorea.ge.spgp2025.a2dg.framework.res.BitmapPool;
import kr.ac.tukorea.ge.spgp2025.a2dg.framework.scene.Scene;
import kr.ac.tukorea.ge.spgp2025.a2dg.framework.util.RectUtil;
import kr.ac.tukorea.ge.spgp2025.a2dg.framework.view.GameView;
import kr.ac.tukorea.ge.spgp2025.a2dg.framework.view.Metrics;

// IBoxCollidable 인터페이스 구현 추가 (플레이어-적 충돌 등을 위해)
public class Fighter extends Sprite implements IBoxCollidable {
    private static final String TAG = Fighter.class.getSimpleName();
    // 비행기 크기 조절 (이 값을 수정하여 크기 변경)
    private static final float PLANE_WIDTH = 120f; // 예시로 크기 줄임
    private static final float PLANE_HEIGHT = PLANE_WIDTH; // 너비와 높이를 같게 설정 (정사각형)

    private static final int PLANE_SRC_WIDTH = 80; // 원본 스프라이트 시트의 프레임 너비

    // 목표 위치 변수 (이제 직접 위치 설정에 사용)
    private float targetX;
    private float targetY;

    // 발사 관련 변수
    private static final float FIRE_INTERVAL = 0.25f;
    private float fireCoolTime = FIRE_INTERVAL;
    private static final float BULLET_OFFSET = 60f; // 비행기 크기에 맞춰 조정

    // 발사 섬광 효과 관련 변수
    private static final float SPARK_OFFSET = 50f; // 비행기 크기에 맞춰 조정
    private static final float SPARK_DURATION = 0.1f;
    private static final float SPARK_WIDTH = 80f; // 비행기 크기에 맞춰 조정
    private static final float SPARK_HEIGHT = SPARK_WIDTH * 3 / 5;
    private final RectF sparkRect = new RectF();
    private final Bitmap sparkBitmap;

    // 좌우 기울기(Roll) 애니메이션 관련 변수
    private static final float MAX_ROLL_TIME = 0.4f;
    private float rollTime;
    private float prevX; // 이전 프레임의 x 좌표 저장용

    // 충돌 영역
    private final RectF collisionRect = new RectF();

    public Fighter() {
        super(R.mipmap.fighters); // 비행기 스프라이트 시트 로드
        // 초기 위치 설정 (화면 하단 중앙) 및 크기 설정
        setPosition(Metrics.width / 2, Metrics.height - 150, PLANE_WIDTH, PLANE_HEIGHT);
        targetX = x; // 초기 목표 위치는 현재 위치
        targetY = y;
        prevX = x; // 초기 이전 x좌표 설정

        sparkBitmap = BitmapPool.get(R.mipmap.laser_spark); // 발사 섬광 이미지 로드
        srcRect = new Rect(); // 스프라이트 시트에서 잘라낼 영역
        updateCollisionRect(); // 초기 충돌 영역 설정
    }

    @Override
    public void update() {
        // 목표 위치(targetX, targetY)로 비행기 위치 업데이트
        // 화면 경계를 벗어나지 않도록 위치 조정
        float halfWidth = width / 2;
        float halfHeight = height / 2;

        // 목표 위치를 화면 경계 내로 제한
        targetX = Math.max(halfWidth, Math.min(targetX, Metrics.width - halfWidth));
        targetY = Math.max(halfHeight, Math.min(targetY, Metrics.height - halfHeight));

        // 비행기 위치를 목표 위치로 직접 설정
        setPosition(targetX, targetY, width, height); // setPosition 내부에서 x, y, dstRect 업데이트됨

        // 충돌 영역 업데이트
        updateCollisionRect();

        // 총알 발사 로직
        fireBullet();

        // 좌우 기울기(Roll) 애니메이션 업데이트
        updateRoll();

        // 다음 프레임을 위해 현재 x 좌표 저장
        prevX = x;
    }

    // setPosition 오버라이드하여 collisionRect도 함께 업데이트
    @Override
    public void setPosition(float x, float y, float width, float height) {
        super.setPosition(x, y, width, height);
        updateCollisionRect(); // 위치 변경 시 충돌 영역도 업데이트
    }


    @Override
    public void draw(Canvas canvas) {
        // 좌우 기울기에 맞는 스프라이트 프레임 그리기
        canvas.drawBitmap(bitmap, srcRect, dstRect, null);

        // 발사 섬광 효과 그리기 (발사 직후 짧은 시간 동안)
        if (FIRE_INTERVAL - fireCoolTime < SPARK_DURATION) {
            RectUtil.setRect(sparkRect, x, y - SPARK_OFFSET, SPARK_WIDTH, SPARK_HEIGHT);
            canvas.drawBitmap(sparkBitmap, null, sparkRect, null);
        }

        // 디버그용: 충돌 영역 그리기 (GameView.drawsDebugStuffs 활성화 시)
        // if (GameView.drawsDebugStuffs) {
        //     Paint paint = new Paint();
        //     paint.setStyle(Paint.Style.STROKE);
        //     paint.setColor(Color.YELLOW);
        //     canvas.drawRect(collisionRect, paint);
        // }
    }

    private void fireBullet() {
        fireCoolTime -= GameView.frameTime;
        if (fireCoolTime > 0) {
            return;
        }
        fireCoolTime = FIRE_INTERVAL; // 발사 쿨타임 초기화
        MainScene scene = (MainScene) Scene.top();
        if (scene == null) return;

        // 현재 점수에 따른 파워 계산 (향후 파워업 레벨로 변경 필요)
        int score = scene.getScore();
        int power = 10 + score / 1000;

        // 총알 생성 및 씬에 추가
        Bullet bullet = Bullet.get(x, y - BULLET_OFFSET, power);
        scene.add(MainScene.Layer.bullet, bullet); // 레이어 명시적 지정
    }

    private void updateRoll() {
        // 이전 프레임과 현재 프레임의 x좌표 변화량 계산
        float dx = x - prevX;
        int sign = 0;

        if (dx < -0.1f) { // 왼쪽으로 이동 중
            sign = -1;
        } else if (dx > 0.1f) { // 오른쪽으로 이동 중
            sign = 1;
        } else { // 거의 정지 상태
            // 정지 시 중앙으로 복귀하는 로직
            if (rollTime > 0.1f) sign = -1;
            else if (rollTime < -0.1f) sign = 1;
        }

        rollTime += sign * GameView.frameTime * 3; // 복귀 속도 조절 가능 ( * 3 부분)

        // 정지 시 중앙 프레임(0)을 지나치지 않도록 처리
        if (sign < 0 && rollTime < 0) rollTime = 0;
        if (sign > 0 && rollTime > 0) rollTime = 0;

        // 최대 기울기 제한
        rollTime = Math.max(-MAX_ROLL_TIME, Math.min(rollTime, MAX_ROLL_TIME));

        // rollTime 값에 따라 스프라이트 인덱스 계산 (0 ~ 10 사이, 중앙은 5)
        int rollIndex = 5 + (int) Math.round(rollTime * 5 / MAX_ROLL_TIME);
        rollIndex = Math.max(0, Math.min(rollIndex, 10)); // 인덱스 범위 보정

        // 스프라이트 시트에서 해당 프레임 영역 설정
        srcRect.set(rollIndex * PLANE_SRC_WIDTH, 0, (rollIndex + 1) * PLANE_SRC_WIDTH, PLANE_SRC_WIDTH);
    }

    // 터치 이벤트 처리
    public boolean onTouch(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN: // 터치를 시작했을 때
            case MotionEvent.ACTION_MOVE: // 터치한 채로 드래그할 때
                // 화면 터치 좌표를 게임 좌표계로 변환
                float[] pts = Metrics.fromScreen(event.getX(), event.getY());
                // 변환된 좌표로 목표 위치 업데이트
                targetX = pts[0];
                targetY = pts[1];
                // Log.d(TAG, "Touch at: " + targetX + ", " + targetY); // 디버깅 로그
                return true; // 이벤트 처리 완료
            // case MotionEvent.ACTION_UP: // 터치를 뗐을 때 (필요 시 로직 추가)
            //     return true;
        }
        return false; // 처리하지 않은 이벤트는 false 반환
    }

    // 충돌 영역 계산 및 업데이트
    private void updateCollisionRect() {
        // dstRect를 기반으로 충돌 영역 설정 (약간 안쪽으로 축소하여 정확도 높임)
        collisionRect.set(dstRect);
        collisionRect.inset(width * 0.1f, height * 0.1f); // 예: 10% 축소
    }

    // IBoxCollidable 인터페이스 구현
    @Override
    public RectF getCollisionRect() {
        return collisionRect; // 계산된 충돌 영역 반환
    }
}