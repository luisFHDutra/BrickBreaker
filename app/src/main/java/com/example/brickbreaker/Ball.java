package com.example.brickbreaker;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.RectF;
import java.util.Random;

public class Ball {
    public static final int SIZE = 48;
    private Bitmap[] sprites;
    private int frame;
    private float x,y,vx,vy;
    private final Random random = new Random();
    private static final int ANIMATION_SPEED = 5;
    private int animationTicker = 0;

    public Ball(Context context) {
        sprites = new Bitmap[4];
        for (int i=0;i<4;i++){
            Bitmap tmp=BitmapFactory.decodeResource(context.getResources(),
                    context.getResources().getIdentifier("ball"+(i+1),"drawable",context.getPackageName()));
            sprites[i]=Bitmap.createScaledBitmap(tmp,SIZE,SIZE,false);
        }
        reset();
    }

    public void update() {
        // Movimento da bola
        x += vx;
        y += vy;

        // Contador de animação
        animationTicker++;
        if (animationTicker >= ANIMATION_SPEED) {
            frame = (frame + 1) % sprites.length;
            animationTicker = 0;
        }
    }
    public void draw(Canvas c){
        c.drawBitmap(sprites[frame],x,y,null);
    }
    public RectF getRect(){
        return new RectF(x,y,x+SIZE,y+SIZE);
    }

    public void bouncePaddle(RectF paddleRect) {
        // calcula o módulo atual da velocidade
        float speed = (float)Math.hypot(vx, vy);

        // inverte vertical (sempre pra cima)
        // calcula norm (-1..+1)
        float paddleCenter = paddleRect.left + paddleRect.width()/2f;
        float ballCenter   = x + SIZE/2f;
        float norm = (ballCenter - paddleCenter) / (paddleRect.width()/2f);
        norm = Math.max(-1f, Math.min(1f, norm));  // garante faixa

        // distribui speed em vx e vy via seno/cosseno
        vx = speed * norm;
        vy = -Math.abs(speed * (float)Math.sqrt(1 - norm*norm));
    }

    public void bounceBrick(){
        vy=-vy;
        vx=-vx;
    }
    public boolean checkWallCollision(int w, int h) {
        boolean bounced = false;
        // parede esquerda/direita
        if (x <= 0 || x + SIZE >= w) {
            vx = -vx; bounced = true;
        }
        // teto
        if (y <= 0) {
            vy = Math.abs(vy); bounced = true;
        }
        return bounced;
    }
    public void launch(){
        if (vy==0) {
            vy=-5;

            int min = 3, max = 5;
            int speed = min + random.nextInt(max - min + 1);

            vx = random.nextBoolean() ? speed : -speed;
        }
    }
    public void reset(){
        vx=0;
        vy=0;
    }
    public void setPosition(float x,float y){
        this.x=x;
        this.y=y;
    }
    public float getY(){
        return y;
    }

    public float getVx() { return vx; }
    public float getVy() { return vy; }
    public void setVelocity(float newVx, float newVy) {
        vx = newVx;
        vy = newVy;
    }

}
