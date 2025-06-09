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
    
    private static final int DEFAULT_PARTICLE_COUNT = 50; // 더 많은 파티클
    private static final int SPARK_COUNT = 20; // 스파크 파티클
    private static final float DEFAULT_LIFETIME = 1.5f; // 더 긴 지속시간
    private static final float MAX_SPEED = 500f; // 더 빠른 속도
    private static final float MIN_SPEED = 50f;
    private static final int MAX_PARTICLE_SIZE = 12; // 더 큰 파티클
    private static final int MIN_PARTICLE_SIZE = 1;
    
    private Particle[] particles;
    private Particle[] sparks; // 스파크 파티클 배열
    private ShockWave shockWave; // 충격파 효과
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
        
        // 메인 폭발 파티클들
        particles = new Particle[DEFAULT_PARTICLE_COUNT];
        for (int i = 0; i < particles.length; i++) {
            particles[i] = new Particle(x, y, ParticleType.EXPLOSION);
        }
        
        // 스파크 파티클들 (더 빠르고 작은 파티클)
        sparks = new Particle[SPARK_COUNT];
        for (int i = 0; i < sparks.length; i++) {
            sparks[i] = new Particle(x, y, ParticleType.SPARK);
        }
        
        // 충격파 효과
        shockWave = new ShockWave(x, y);
    }
    
    @Override
    public void update() {
        if (state == STATE_DEAD) return;
        
        boolean anyAlive = false;
        
        // 메인 파티클 업데이트
        for (Particle particle : particles) {
            if (particle.isAlive()) {
                particle.update();
                anyAlive = true;
            }
        }
        
        // 스파크 파티클 업데이트
        for (Particle spark : sparks) {
            if (spark.isAlive()) {
                spark.update();
                anyAlive = true;
            }
        }
        
        // 충격파 업데이트
        if (shockWave.isAlive()) {
            shockWave.update();
            anyAlive = true;
        }
        
        if (!anyAlive) {
            state = STATE_DEAD;
            Scene.top().remove(this);
        }
    }
    
    @Override
    public void draw(Canvas canvas) {
        if (state == STATE_DEAD) return;
        
        // 충격파를 먼저 그리기 (배경)
        if (shockWave.isAlive()) {
            shockWave.draw(canvas);
        }
        
        // 메인 파티클 그리기
        for (Particle particle : particles) {
            if (particle.isAlive()) {
                particle.draw(canvas);
            }
        }
        
        // 스파크를 마지막에 그리기 (전경)
        for (Particle spark : sparks) {
            if (spark.isAlive()) {
                spark.draw(canvas);
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
    
    // 파티클 타입 열거형
    private enum ParticleType {
        EXPLOSION, SPARK
    }
    
    // 충격파 클래스
    private class ShockWave {
        private float x, y;
        private float radius;
        private float maxRadius;
        private float life;
        private float maxLife;
        private Paint paint;
        
        public ShockWave(float x, float y) {
            this.x = x;
            this.y = y;
            this.radius = 0f;
            this.maxRadius = 100f;
            this.maxLife = this.life = 0.3f; // 짧은 지속시간
            this.paint = new Paint();
            this.paint.setStyle(Paint.Style.STROKE);
            this.paint.setStrokeWidth(8f);
            this.paint.setAntiAlias(true);
        }
        
        public void update() {
            if (life <= 0) return;
            
            life -= GameView.frameTime;
            radius += (maxRadius / maxLife) * GameView.frameTime;
            
            // 페이드 아웃
            float alpha = life / maxLife;
            int alphaInt = (int) (255 * alpha * 0.7f); // 반투명
            paint.setColor(Color.argb(alphaInt, 255, 255, 255)); // 흰색 충격파
        }
        
        public void draw(Canvas canvas) {
            if (life <= 0) return;
            canvas.drawCircle(x, y, radius, paint);
        }
        
        public boolean isAlive() {
            return life > 0;
        }
    }
    
    // 파티클 내부 클래스
    private class Particle {
        private float x, y;
        private float vx, vy;
        private float size;
        private float initialSize;
        private float life;
        private float maxLife;
        private int baseColor;
        private Paint paint;
        private ParticleType type;
        private float rotation;
        private float rotationSpeed;
        private boolean trail; // 궤적 효과
        
        public Particle(float startX, float startY, ParticleType type) {
            this.x = startX;
            this.y = startY;
            this.type = type;
            
            // 타입에 따른 다른 설정
            if (type == ParticleType.EXPLOSION) {
                setupExplosionParticle();
            } else {
                setupSparkParticle();
            }
            
            this.paint = new Paint();
            this.paint.setAntiAlias(true);
            
            // 회전 효과
            this.rotation = 0f;
            this.rotationSpeed = (random.nextFloat() - 0.5f) * 720f; // -360도 ~ +360도/초
            this.trail = random.nextBoolean();
        }
        
        private void setupExplosionParticle() {
            // 랜덤한 속도 벡터 생성
            float angle = random.nextFloat() * 2 * (float) Math.PI;
            float speed = MIN_SPEED + random.nextFloat() * (MAX_SPEED - MIN_SPEED);
            this.vx = (float) Math.cos(angle) * speed;
            this.vy = (float) Math.sin(angle) * speed;
            
            // 크기와 수명 설정
            this.initialSize = this.size = MIN_PARTICLE_SIZE + random.nextFloat() * (MAX_PARTICLE_SIZE - MIN_PARTICLE_SIZE);
            this.maxLife = this.life = DEFAULT_LIFETIME * (0.8f + random.nextFloat() * 0.4f); // 수명 변화
            
            // 폭발 색상 (더 다양한 색상)
            int[] colors = {
                Color.rgb(255, 165, 0),  // 주황색
                Color.rgb(255, 69, 0),   // 빨간 주황색
                Color.rgb(255, 255, 0),  // 노란색
                Color.rgb(255, 0, 0),    // 빨간색
                Color.rgb(255, 215, 0),  // 금색
                Color.rgb(255, 140, 0),  // 진한 주황색
                Color.rgb(255, 20, 147), // 분홍색
                Color.rgb(255, 105, 180) // 핫 핑크
            };
            this.baseColor = colors[random.nextInt(colors.length)];
        }
        
        private void setupSparkParticle() {
            // 스파크는 더 빠르고 직선적
            float angle = random.nextFloat() * 2 * (float) Math.PI;
            float speed = MAX_SPEED * 0.8f + random.nextFloat() * MAX_SPEED * 0.6f;
            this.vx = (float) Math.cos(angle) * speed;
            this.vy = (float) Math.sin(angle) * speed;
            
            // 더 작고 짧은 수명
            this.initialSize = this.size = 1f + random.nextFloat() * 3f;
            this.maxLife = this.life = DEFAULT_LIFETIME * 0.5f;
            
            // 밝은 색상
            int[] sparkColors = {
                Color.rgb(255, 255, 255), // 흰색
                Color.rgb(255, 255, 0),   // 노란색
                Color.rgb(255, 215, 0),   // 금색
                Color.rgb(255, 255, 224), // 연한 노란색
                Color.rgb(255, 250, 205)  // 레몬색
            };
            this.baseColor = sparkColors[random.nextInt(sparkColors.length)];
        }
        
        public void update() {
            if (life <= 0) return;
            
            // 위치 업데이트
            x += vx * GameView.frameTime;
            y += vy * GameView.frameTime;
            
            // 타입별 물리 효과
            if (type == ParticleType.EXPLOSION) {
                // 중력 효과 (아래로 약간 끌어당김)
                vy += 200f * GameView.frameTime;
                
                // 공기 저항 (속도 감소)
                vx *= 0.97f;
                vy *= 0.97f;
                
                // 크기 변화 (시간에 따라 커지다가 작아짐)
                float lifeRatio = life / maxLife;
                if (lifeRatio > 0.7f) {
                    // 초반에는 커짐
                    size = initialSize * (1.0f + (1.0f - lifeRatio) * 0.5f);
                } else {
                    // 후반에는 작아짐
                    size = initialSize * lifeRatio * 1.2f;
                }
            } else {
                // 스파크는 중력의 영향을 덜 받음
                vy += 50f * GameView.frameTime;
                vx *= 0.99f;
                vy *= 0.99f;
                
                // 스파크는 크기가 일정
                size = initialSize * (life / maxLife);
            }
            
            // 회전 업데이트
            rotation += rotationSpeed * GameView.frameTime;
            
            // 수명 감소
            life -= GameView.frameTime;
            
            // 알파값과 색상 변화 계산
            float alpha = life / maxLife;
            int alphaInt = (int) (255 * alpha);
            
            // 색상 변화 (폭발 파티클은 시간에 따라 색이 변함)
            int finalColor;
            if (type == ParticleType.EXPLOSION && alpha < 0.5f) {
                // 후반부에는 더 어두운 색으로 변화
                int r = (int) (Color.red(baseColor) * (0.5f + alpha));
                int g = (int) (Color.green(baseColor) * (0.3f + alpha * 0.7f));
                int b = (int) (Color.blue(baseColor) * alpha);
                finalColor = Color.argb(alphaInt, r, g, b);
            } else {
                int r = Color.red(baseColor);
                int g = Color.green(baseColor);
                int b = Color.blue(baseColor);
                finalColor = Color.argb(alphaInt, r, g, b);
            }
            
            paint.setColor(finalColor);
        }
        
        public void draw(Canvas canvas) {
            if (life <= 0) return;
            
            canvas.save();
            
            if (type == ParticleType.EXPLOSION) {
                // 궤적 효과 (일부 파티클만)
                if (trail && life < maxLife * 0.8f) {
                    Paint trailPaint = new Paint(paint);
                    int trailAlpha = (int) (paint.getAlpha() * 0.3f);
                    trailPaint.setAlpha(trailAlpha);
                    
                    // 이전 위치들을 그려서 궤적 효과
                    float prevX = x - vx * GameView.frameTime * 2;
                    float prevY = y - vy * GameView.frameTime * 2;
                    canvas.drawLine(x, y, prevX, prevY, trailPaint);
                }
                
                // 회전된 사각형으로 그리기 (다양한 모양)
                canvas.translate(x, y);
                canvas.rotate(rotation);
                
                if (size > 6f) {
                    // 큰 파티클은 사각형
                    canvas.drawRect(-size/2, -size/2, size/2, size/2, paint);
                } else {
                    // 작은 파티클은 원형
                    canvas.drawCircle(0, 0, size, paint);
                }
            } else {
                // 스파크는 선으로 그리기
                Paint sparkPaint = new Paint(paint);
                sparkPaint.setStrokeWidth(Math.max(1f, size / 2f));
                sparkPaint.setStrokeCap(Paint.Cap.ROUND);
                
                float lineLength = Math.max(size * 2f, 8f);
                float endX = x + (vx / Math.abs(vx + vy)) * lineLength;
                float endY = y + (vy / Math.abs(vx + vy)) * lineLength;
                
                canvas.drawLine(x, y, endX, endY, sparkPaint);
                
                // 스파크 끝에 작은 원점
                canvas.drawCircle(x, y, size * 0.5f, paint);
            }
            
            canvas.restore();
        }
        
        public boolean isAlive() {
            return life > 0;
        }
    }
} 