package com.example.brickbreaker;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.RectF;
import java.util.Random;

public class Brick {
    public static final int WIDTH = 120;
    public static final int HEIGHT = 60;

    // Bitmaps carregados uma única vez: [0]=intact, [1]=cracked
    private static Bitmap[][] tileBitmaps;
    private static final Random rand = new Random();
    private static final String[] TYPES = { "redtile", "greentile", "purpletile" };

    private Bitmap bitmapIntact;
    private Bitmap bitmapCracked;
    private Bitmap currentBitmap;
    private RectF rect;
    private int hitsLeft;
    private boolean visible;

    /** Chamar antes de criar qualquer Brick, ex: em Application.onCreate() ou GameView constructor */
    public static void initTileBitmaps(Resources res) {
        if (tileBitmaps != null) return;
        tileBitmaps = new Bitmap[TYPES.length][2];
        for (int i = 0; i < TYPES.length; i++) {
            int idIntact  = res.getIdentifier(TYPES[i],        "drawable", "com.example.brickbreaker");
            int idCrack   = res.getIdentifier(TYPES[i] + "2",   "drawable", "com.example.brickbreaker");
            Bitmap intact = BitmapFactory.decodeResource(res, idIntact);
            Bitmap crack  = BitmapFactory.decodeResource(res, idCrack);
            // escala ambos
            intact = Bitmap.createScaledBitmap(intact, WIDTH, HEIGHT, false);
            crack  = Bitmap.createScaledBitmap(crack,  WIDTH, HEIGHT, false);
            tileBitmaps[i][0] = intact;
            tileBitmaps[i][1] = crack;
        }
    }

    public Brick(Context context, float x, float y) {
        // garante que os bitmaps estão prontos
        initTileBitmaps(context.getResources());

        // sorteia um tipo
        int idx = rand.nextInt(TYPES.length);
        bitmapIntact  = tileBitmaps[idx][0];
        bitmapCracked = tileBitmaps[idx][1];
        currentBitmap = bitmapIntact;

        rect     = new RectF(x, y, x + WIDTH, y + HEIGHT);
        hitsLeft = 2;
        visible  = true;
    }

    /** Desenha o tijolo atual */
    public void draw(Canvas canvas) {
        if (!visible) return;
        canvas.drawBitmap(currentBitmap, rect.left, rect.top, null);
    }

    /** Retorna true se colisão deve ser processada (ou seja, jogo deve refletir bola) */
    public boolean handleHit() {
        if (!visible) return false;
        hitsLeft--;
        if (hitsLeft == 1) {
            // troca para versão rachada
            currentBitmap = bitmapCracked;
            return true;   // a bola vai sempre quicar
        } else {
            // segunda batida: some
            visible = false;
            return true;
        }
    }

    public RectF getRect() { return rect; }
    public boolean isVisible() { return visible; }
    public void setInvisible() { visible = false; }
}