package com.example.brickbreaker;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.RectF;

public class Paddle {
    public static final int WIDTH = 200;
    public static final int HEIGHT = 40;

    private Bitmap bitmap;
    private RectF rect;
    private float x;
    private final float y;
    private int screenWidth;

    public Paddle(Context context) {
        bitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.paddle);
        bitmap = Bitmap.createScaledBitmap(bitmap, WIDTH, HEIGHT, false);
        int screenHeight = context.getResources().getDisplayMetrics().heightPixels;
        y = screenHeight - HEIGHT - 50;
        rect = new RectF();
    }

    public void update(float dx) {
        x += dx * 10;
        if (x < 0) x = 0;
        if (x + WIDTH > screenWidth) x = screenWidth - WIDTH;
        rect.set(x, y, x + WIDTH, y + HEIGHT);
    }

    public void draw(Canvas canvas) {
        canvas.drawBitmap(bitmap, x, y, null);
    }
    public RectF getRect() { return rect; }
    public void reset(int screenWidth) {
        this.screenWidth = screenWidth;
        x = (screenWidth - WIDTH) / 2f;
        rect.set(x, y, x + WIDTH, y + HEIGHT);
    }
}