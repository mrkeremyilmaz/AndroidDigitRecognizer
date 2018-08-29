package com.krmylmz.characterrecognizer.drawing;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;

import static com.krmylmz.characterrecognizer.drawing.DrawPath.getStrokeWidth;

public class DrawView extends View {
    public static final int BRUSH_SIZE = 20;
    public static final int DEFAULT_BG_COLOR = Color.WHITE;
    public static final int DEFAULT_COLOR = Color.BLACK;
    private static final float TOUCH_LIMIT = 4;
    private float mX, mY;
    private Path mPath;
    private Paint mPaint;
    private ArrayList<DrawPath> paths = new ArrayList<>();
    private int currentColor;
    private int backgroundColor = DEFAULT_BG_COLOR;
    private int strokeWidth;
    private Bitmap mBitmap;
    private Canvas mCanvas;
    private Paint mBitmapPaint = new Paint(Paint.DITHER_FLAG);

    public DrawView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setDither(true);
        mPaint.setColor(DEFAULT_COLOR);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeJoin(Paint.Join.ROUND);
        mPaint.setStrokeCap(Paint.Cap.ROUND);
        mPaint.setXfermode(null);
        mPaint.setAlpha(0xff);
    }

    public DrawView(Context context) {
        this(context, null);
    }

    public void init(DisplayMetrics metrics) {
//        int height = metrics.heightPixels;
        int width = metrics.widthPixels;

        mBitmap = Bitmap.createBitmap(width, width, Bitmap.Config.ARGB_8888);

        mCanvas = new Canvas(mBitmap);

        currentColor = DEFAULT_COLOR;
        strokeWidth = BRUSH_SIZE;
    }

    public void setBitmap(Bitmap bitmap){
        mCanvas = new Canvas(bitmap);
        invalidate();
    }

    public Bitmap getBitmap() {
        return mBitmap.copy(mBitmap.getConfig(), true);
    }

    public void clear() {
        backgroundColor = DEFAULT_BG_COLOR;
        paths.clear();
        invalidate();
    }



    @Override
    protected void onDraw(Canvas canvas) {
        canvas.save();
        mCanvas.drawColor(backgroundColor);

        for (DrawPath dPath : paths) {
            mPaint.setColor(dPath.getColor());
            mPaint.setStrokeWidth(getStrokeWidth());
            mPaint.setMaskFilter(null);

            mCanvas.drawPath(dPath.getPath(), mPaint);
        }


        canvas.drawBitmap(mBitmap, 0, 0, mBitmapPaint);
        canvas.restore();
    }


    private void touchStart(float x, float y) {
        mPath = new Path();
        DrawPath dPath = new DrawPath(currentColor, mPath);
        paths.add(dPath);

        mPath.reset();
        mPath.moveTo(x, y);
        mX = x;
        mY = y;
    }

    private void touchMove(float x, float y) {
        float dx = Math.abs(x - mX);
        float dy = Math.abs(y - mY);

        if (dx >= TOUCH_LIMIT || dy >= TOUCH_LIMIT) {
            mPath.quadTo(mX, mY, (x + mX) / 2, (y + mY) / 2);
            mX = x;
            mY = y;
        }
    }

    private void touchUp() {
        mPath.lineTo(mX, mY);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                touchStart(x, y);
                invalidate();
                break;

            case MotionEvent.ACTION_MOVE:
                touchMove(x, y);
                invalidate();
                break;

            case MotionEvent.ACTION_UP:
                touchUp();
                invalidate();
                break;
        }

        return true;
    }
}
