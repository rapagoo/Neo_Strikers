package kr.ac.tukorea.ge.rapagoo.neostrikers.game;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

import java.util.Random;

import kr.ac.tukorea.ge.spgp2025.a2dg.framework.interfaces.IGameObject;
import kr.ac.tukorea.ge.spgp2025.a2dg.framework.interfaces.ILayerProvider;
import kr.ac.tukorea.ge.spgp2025.a2dg.framework.interfaces.IRecyclable;
import kr.ac.tukorea.ge.spgp2025.a2dg.framework.scene.Scene;
import kr.ac.tukorea.ge.spgp2025.a2dg.framework.view.GameView;

public class Explosion implements IGameObject, IRecyclable, ILayerProvider<MainScene.Layer> {
    
    public static final int STATE_ALIVE = 0;
    public static final int STATE_DEAD = 1;
    
    private static final int DEFAULT_PARTICLE_COUNT = 20;
    private static final float DEFAULT_LIFETIME = 1.0f; // 1초
    private static final float MAX_SPEED = 300f;
    private static final float MIN_SPEED = 100f;
    private static final int MAX_PARTICLE_SIZE = 8;
    private static final int MIN_PARTICLE_SIZE = 2;
    
    private Particle[] particles;
    private int state;
    private float x, y;
    private Random random;
    
    public Explosion() {
        random = new Random();
    }
    
    public static Explosion get(float x, float y) {
        Explosion explosion = (Explosion) Scene.top().getRecyclable(Explosion.class);
        if (explosion == null) {
            explosion = new Explosion();
        }
        explosion.init(x, y);
        return explosion;
    }
    
    private void init(float x, float y) {
        this.x = x;
        this.y = y;
        this.state = STATE_ALIVE;
        
        particles = new Particle[DEFAULT_PARTICLE_COUNT];
        for (int i = 0; i < particles.length; i++) {
            particles[i] = new Particle(x, y);
        }
    }
    
    @Override
    public void update() {
        if (state == STATE_DEAD) return;
        
        boolean anyAlive = false;
        for (Particle particle : particles) {
            if (particle.isAlive()) {
                particle.update();
                anyAlive = true;
            }
        }
        
        if (!anyAlive) {
            state = STATE_DEAD;
            Scene.top().remove(this);
        }
    }
    
    @Override
    public void draw(Canvas canvas) {
        if (state == STATE_DEAD) return;
        
        for (Particle particle : particles) {
            if (particle.isAlive()) {
                particle.draw(canvas);
            }
        }
    }
    
    public boolean isAlive() {
        return state == STATE_ALIVE;
    }
    
    @Override
    public void onRecycle() {
        // 재활용 시 초기화
    }
    
    @Override
    public MainScene.Layer getLayer() {
        return MainScene.Layer.effect;
    }
    
    // 파티클 내부 클래스
    private class Particle {
        private float x, y;
        private float vx, vy;
        private float size;
        private float life;
        private float maxLife;
        private int color;
        private Paint paint;
        
        public Particle(float startX, float startY) {
            this.x = startX;
            this.y = startY;
            
            // 랜덤한 속도 벡터 생성
            float angle = random.nextFloat() * 2 * (float) Math.PI;
            float speed = MIN_SPEED + random.nextFloat() * (MAX_SPEED - MIN_SPEED);
            this.vx = (float) Math.cos(angle) * speed;
            this.vy = (float) Math.sin(angle) * speed;
            
            // 크기와 수명 설정
            this.size = MIN_PARTICLE_SIZE + random.nextFloat() * (MAX_PARTICLE_SIZE - MIN_PARTICLE_SIZE);
            this.maxLife = this.life = DEFAULT_LIFETIME;
            
            // 폭발 색상 (주황색, 빨간색, 노란색 계열)
            int[] colors = {
                Color.rgb(255, 165, 0),  // 주황색
                Color.rgb(255, 69, 0),   // 빨간 주황색
                Color.rgb(255, 255, 0),  // 노란색
                Color.rgb(255, 0, 0),    // 빨간색
                Color.rgb(255, 215, 0)   // 금색
            };
            this.color = colors[random.nextInt(colors.length)];
            
            this.paint = new Paint();
            this.paint.setAntiAlias(true);
        }
        
        public void update() {
            if (life <= 0) return;
            
            // 위치 업데이트
            x += vx * GameView.frameTime;
            y += vy * GameView.frameTime;
            
            // 중력 효과 (아래로 약간 끌어당김)
            vy += 150f * GameView.frameTime;
            
            // 공기 저항 (속도 감소)
            vx *= 0.98f;
            vy *= 0.98f;
            
            // 수명 감소
            life -= GameView.frameTime;
            
            // 알파값 계산 (페이드 아웃)
            float alpha = life / maxLife;
            int alphaInt = (int) (255 * alpha);
            
            // 색상에 알파값 적용
            int r = Color.red(color);
            int g = Color.green(color);
            int b = Color.blue(color);
            int colorWithAlpha = Color.argb(alphaInt, r, g, b);
            
            paint.setColor(colorWithAlpha);
        }
        
        public void draw(Canvas canvas) {
            if (life <= 0) return;
            
            canvas.drawCircle(x, y, size, paint);
        }
        
        public boolean isAlive() {
            return life > 0;
        }
    }
} 