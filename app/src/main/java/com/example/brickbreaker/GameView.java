package com.example.brickbreaker;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.media.SoundPool;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class GameView extends SurfaceView implements SurfaceHolder.Callback, Runnable, SensorEventListener {
    private static final int MAX_LIVES = 3;
    private static final int HEART_SIZE = 50;
    private static final int HEART_MARGIN_X = 35;
    private static final int HEART_MARGIN_Y = 20;
    private static final float GAMEOVER_SCALE_W = 0.75f;
    private static final float GAMEOVER_SCALE_H = 0.25f;
    private static final int PAUSE_BUTTON_SIZE = 60;   // menor
    private static final int PLAY_BUTTON_SIZE  = 200;  // maior
    // velocidade extra por frame
    private static final float BALL_ACCEL      = 0.05f;
    // velocidade máxima permitida
    private static final float BALL_MAX_SPEED  = 8.0f;


    private Thread gameThread;
    private boolean running;
    private boolean gameStarted;
    private boolean isGameOver;
    private int lives;
    private int currentLevel;

    private Paddle paddle;
    private Ball ball;
    private Brick[][] bricks;
    private final int[][][] levels = {
            {{1,1,1,1,1,1,1,1}, {1,1,1,1,1,1,1,1}, {0,0,0,0,0,0,0,0}},
            {{1,1,1,1,1,1,1,1}, {1,1,1,1,1,1,1,1}, {1,1,1,1,1,1,1,1}, {0,0,0,0,0,0,0,0}},
            {{1,1,1,1,1,1,1,1}, {1,1,1,1,1,1,1,1}, {1,1,1,1,1,1,1,1}, {1,1,1,1,1,1,1,1}}
    };

    private Bitmap rawBackground, background;
    private Bitmap rawGameOver, gameOverBitmap;
    private Bitmap rawHeart, heartBitmap;
    private SoundPool soundPool;
    private int soundBounce;
    private MediaPlayer musicPlayer;
    private SensorManager sensorManager;
    private float accelX;
    // deslocamento atual (em px)
    private int offsetBg = 0;
    // velocidade de scroll
    private static final int SCROLL_SPEED = 2;
    // largura da tela, inicializada em surfaceChanged()
    private Bitmap pauseBitmap, playBitmap;
    private RectF pauseRect, playRect;
    private boolean paused = false;
    private static final int BUTTON_SIZE = 80;   // em pixels
    private int screenW;
    private int screenH;

    public GameView(Context context) {
        super(context);
        getHolder().addCallback(this);

        paddle = new Paddle(context);
        ball = new Ball(context);

        // Load static background and UI images
        rawBackground = BitmapFactory.decodeResource(getResources(), R.drawable.background);
        rawGameOver   = BitmapFactory.decodeResource(getResources(), R.drawable.gameover);
        rawHeart      = BitmapFactory.decodeResource(getResources(), R.drawable.heart);
        heartBitmap   = Bitmap.createScaledBitmap(rawHeart, HEART_SIZE, HEART_SIZE, false);

        // Setup sound effects
        AudioAttributes attrs = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();
        soundPool = new SoundPool.Builder()
                .setAudioAttributes(attrs)
                .setMaxStreams(3)
                .build();
        soundBounce = soundPool.load(context, R.raw.bounce, 1);

        // Setup background music
        musicPlayer = MediaPlayer.create(context, R.raw.background_music);
        musicPlayer.setLooping(true);

        sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        gameStarted  = false;
        isGameOver   = false;
        lives        = MAX_LIVES;
        currentLevel = 0;

        // Carrega botões
        Bitmap rawPause = BitmapFactory.decodeResource(getResources(), R.drawable.pausebutton);
        pauseBitmap     = Bitmap.createScaledBitmap(rawPause, PAUSE_BUTTON_SIZE, PAUSE_BUTTON_SIZE, false);
        Bitmap rawPlay  = BitmapFactory.decodeResource(getResources(), R.drawable.playbutton);
        playBitmap      = Bitmap.createScaledBitmap(rawPlay,  PLAY_BUTTON_SIZE, PLAY_BUTTON_SIZE, false);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        running = true;
        musicPlayer.start();
        sensorManager.registerListener(this,
                sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                SensorManager.SENSOR_DELAY_GAME);
        gameThread = new Thread(this);
        gameThread.start();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        screenW = width;
        screenH = height;

        // posiciona o botão de pausa no canto superior direito
        float px = screenW - PAUSE_BUTTON_SIZE - HEART_MARGIN_X;
        float py = HEART_MARGIN_Y;
        pauseRect = new RectF(px, py, px + PAUSE_BUTTON_SIZE, py + PAUSE_BUTTON_SIZE);

        // posiciona o botão de play no centro
        float cx = (screenW - PLAY_BUTTON_SIZE) / 2f;
        float cy = (screenH - PLAY_BUTTON_SIZE) / 2f;
        playRect  = new RectF(cx, cy, cx + PLAY_BUTTON_SIZE, cy + PLAY_BUTTON_SIZE);

        background     = Bitmap.createScaledBitmap(rawBackground, width * 4, height, false);

        int goW        = (int)(width * GAMEOVER_SCALE_W);
        int goH        = (int)(height * GAMEOVER_SCALE_H);
        gameOverBitmap = Bitmap.createScaledBitmap(rawGameOver, goW, goH, false);

        loadLevel(currentLevel);
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        running = false;
        sensorManager.unregisterListener(this);
        soundPool.release();
        musicPlayer.stop();
        musicPlayer.release();
        try { gameThread.join(); } catch (InterruptedException e) { e.printStackTrace(); }
    }

    private void loadLevel(int level) {
        int rows = levels[level].length;

        int cols = levels[level][0].length;

        bricks = new Brick[rows][cols];

        int startX = (getWidth() - Brick.WIDTH * cols) / 2;

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                if (levels[level][r][c] == 1) {
                    bricks[r][c] = new Brick(getContext(),
                            startX + c * Brick.WIDTH,
                            r * Brick.HEIGHT + 100);
                }
            }
        }
        paddle.reset(getWidth());
        ball.reset();
        gameStarted = false;
        isGameOver  = false;
        lives       = MAX_LIVES;
    }

    @Override
    public void run() {
        while (running) {
            if (!getHolder().getSurface().isValid()) continue;
            Canvas canvas = getHolder().lockCanvas();
            if (!paused) {
                update();
            }
            drawGame(canvas);
            getHolder().unlockCanvasAndPost(canvas);
        }
    }

    private void update() {
        // 1) Atualiza deslocamento do background
        offsetBg = (offsetBg + SCROLL_SPEED) % background.getWidth();

        // 2) Se acabou, não faz nada
        if (isGameOver) return;

        // 3) Antes do início, mantém bola sobre o paddle
        if (!gameStarted) {
            float cx = paddle.getRect().left + Paddle.WIDTH / 2f;
            ball.setPosition(
                    cx - Ball.SIZE / 2f,
                    paddle.getRect().top - Ball.SIZE
            );
            return;
        }

        // 4) Move paddle e bola
        paddle.update(-accelX);
        ball.update();

        // 5) Acelera a bola até um limite máximo
        float vx = ball.getVx();
        float vy = ball.getVy();
        float signX = Math.signum(vx);
        float signY = Math.signum(vy);
        float newVx = Math.min(Math.abs(vx) + BALL_ACCEL, BALL_MAX_SPEED) * signX;
        float newVy = Math.min(Math.abs(vy) + BALL_ACCEL, BALL_MAX_SPEED) * signY;
        ball.setVelocity(newVx, newVy);

        // 6) Colisão com o paddle
        if (RectF.intersects(ball.getRect(), paddle.getRect())) {
            ball.bouncePaddle(paddle.getRect());
//            soundPool.play(soundBounce, 1, 1, 0, 0, 1);
        }

        // 7) Colisão com as paredes (apenas uma vez, via checkWallCollision)
        if (ball.checkWallCollision(getWidth(), getHeight())) {
            soundPool.play(soundBounce, 1, 1, 0, 0, 1);
        }

        // 8) Colisão com tijolos
        boolean cleared = true;
        for (Brick[] row : bricks) {
            for (Brick b : row) {
                if (b != null && b.isVisible()) {
                    cleared = false;
                    if (RectF.intersects(b.getRect(), ball.getRect())) {
                        if (b.handleHit()) {
                            ball.bounceBrick();
                            soundPool.play(soundBounce, 1, 1, 0, 0, 1);
                        }
                        break;
                    }
                }
            }
        }
        if (cleared) {
            currentLevel++;
            if (currentLevel < levels.length) loadLevel(currentLevel);
            else isGameOver = true;
            return;
        }

        // 9) Perda de vida
        if (ball.getY() > getHeight()) {
            soundPool.play(soundBounce, 1, 1, 0, 0, 1);  // som de life lost
            lives--;
            if (lives <= 0) {
                isGameOver = true;
            } else {
                paddle.reset(getWidth());
                ball.reset();
                gameStarted = false;
            }
        }
    }

    private void drawGame(Canvas canvas) {
        // 1) Desenha o fundo rolante
        int x2 = -offsetBg;
        canvas.drawBitmap(background, x2, 0, null);
        canvas.drawBitmap(background, x2 + background.getWidth(), 0, null);

        // 2) Desenha tijolos
        if (bricks != null) {
            for (Brick[] row : bricks) {
                for (Brick b : row) {
                    if (b != null && b.isVisible()) {
                        b.draw(canvas);
                    }
                }
            }
        }

        // 3) Desenha paddle e bola
        paddle.draw(canvas);
        ball.draw(canvas);

        // 4) Desenha vidas (corações)
        for (int i = 0; i < lives; i++) {
            float x = HEART_MARGIN_X + i * (HEART_SIZE + HEART_MARGIN_X);
            canvas.drawBitmap(heartBitmap, x, HEART_MARGIN_Y, null);
        }

        // 5) Se Game Over, desenha a mensagem
        if (isGameOver) {
            float gx = (screenW - gameOverBitmap.getWidth()) / 2f;
            float gy = (screenH - gameOverBitmap.getHeight()) / 2f;
            canvas.drawBitmap(gameOverBitmap, gx, gy, null);
        }

        // 6) Se pausado: overlay + play
        if (paused) {
            Paint fade = new Paint();
            fade.setColor(Color.argb(150, 0, 0, 0));
            canvas.drawRect(0, 0, screenW, screenH, fade);
            canvas.drawBitmap(playBitmap, playRect.left, playRect.top, null);
        }
        // 7) Senão (jogo rodando): botão de pause
        else {
            canvas.drawBitmap(pauseBitmap, pauseRect.left, pauseRect.top, null);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        if (e.getAction() != MotionEvent.ACTION_DOWN)
            return true;

        float x = e.getX(), y = e.getY();

        // 1) Game Over: sempre tem prioridade
        if (isGameOver) {
            currentLevel = 0;
            loadLevel(0);
            return true;
        }

        // 2) Se estiver pausado, só pode tocar no botão de Play
        if (paused) {
            if (playRect.contains(x, y)) {
                paused = false;
            }
            return true;
        }

        // 3) Não está pausado: checa botão de Pause
        if (pauseRect.contains(x, y)) {
            paused = true;
            return true;
        }

        // 4) Se ainda não começou, lança a bola
        if (!gameStarted) {
            gameStarted = true;
            ball.launch();
//            soundPool.play(soundBounce, 1, 1, 0, 0, 1);
            return true;
        }

        return true;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        accelX = event.values[0];
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}
}
