package com.example.musicplayer.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.support.annotation.DrawableRes;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.view.View;

/**
 * Created by Tarik on 19.05.2016.
 */
public class AlbumArtView extends View {

    private static final float ANIMATION_STEP = 0.05f;

    private AlbumArtCache.AlbumArt mAlbumArt;
    private AlbumArtCache.Callback mAlbumArtCallback = new AlbumArtCache.Callback() {
        @Override
        public void onArtLoad(Bitmap bitmap) {
            setAlbumArtBitmap(bitmap, true);
        }

        @Override
        public void onArtReleased() {
            mAlbumArt = null;
            setAlbumArtDrawable(null, false);
        }
    };

    private Bitmap mAlbumArtBitmap;
    private Drawable mAlbumArtDrawable;
    private Drawable mEmptyDrawable;
    private float mOpacity;

    private Drawable mForegroundDrawable;

    private Paint mPaint;
    private Rect mDrawingRect;

    public AlbumArtView(Context context) {
        super(context);

        initialize();
    }

    public AlbumArtView(Context context, AttributeSet attrs) {
        super(context, attrs);

        initialize();
    }

    public AlbumArtView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        initialize();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int w = MeasureSpec.getSize(widthMeasureSpec);
        int h = MeasureSpec.getSize(heightMeasureSpec);

        int size = widthMeasureSpec;

        if (w == 0)
            size = heightMeasureSpec;
        else if (h == 0)
            size = widthMeasureSpec;

        setMeasuredDimension(w, w);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        mDrawingRect.set(0, 0, getWidth(), getHeight());

        if (mAlbumArtBitmap != null) {
            mPaint.setARGB((int)(mOpacity * 255.0f), 255, 255, 255);
            canvas.drawBitmap(mAlbumArtBitmap, null, mDrawingRect, mPaint);
        }
        else if (mAlbumArtDrawable != null) {
            mAlbumArtDrawable.setBounds(mDrawingRect);
            mAlbumArtDrawable.draw(canvas);
        }

        if (mForegroundDrawable != null) {
            mForegroundDrawable.setBounds(mDrawingRect);
            mForegroundDrawable.draw(canvas);
        }

        if (mOpacity < 1.0f) {
            mOpacity += ANIMATION_STEP;

            if (mOpacity > 1.0f)
                mOpacity = 1.0f;

            ViewCompat.postInvalidateOnAnimation(this);
        }
    }

    @Override
    protected boolean verifyDrawable(Drawable who) {
        return who == mAlbumArtDrawable || who == mForegroundDrawable || super.verifyDrawable(who);
    }

    @Override
    public void drawableHotspotChanged(float x, float y) {
        super.drawableHotspotChanged(x, y);

        if (mForegroundDrawable != null)
            DrawableCompat.setHotspot(mForegroundDrawable, x, y);
    }

    @Override
    protected void drawableStateChanged() {
        super.drawableStateChanged();

        if (mForegroundDrawable != null && mForegroundDrawable.isStateful())
            mForegroundDrawable.setState(getDrawableState());
    }

    public void setAlbumArt(AlbumArtCache.AlbumArt albumArt) {
        if (albumArt == null)
            setAlbumArtDrawable(mEmptyDrawable, false);

        if (albumArt == mAlbumArt)
            return;

        mAlbumArtCallback.cancel();

        if (albumArt != null) {
            if (albumArt.isCached())
                setAlbumArtBitmap(albumArt.getBitmap(), false);
            else
                setAlbumArtDrawable(null, false);

            albumArt.addCallback(mAlbumArtCallback);
        }

        mAlbumArt = albumArt;
    }

    public void setAlbumArt(int albumId) {
        setAlbumArt(AlbumArtCache.getInstance().getAlbumArt(albumId));
    }

    public void setForegroundDrawable(Drawable drawable) {
        if (mForegroundDrawable != null) {
            unscheduleDrawable(mForegroundDrawable);
            mForegroundDrawable.setCallback(null);
        }

        mForegroundDrawable = drawable;

        if (mForegroundDrawable != null) {
            mForegroundDrawable.setCallback(this);
            mForegroundDrawable.setState(getDrawableState());
            mForegroundDrawable.setLevel(0);
            mForegroundDrawable.setVisible(true, true);
        }

        invalidate();
    }

    public void setForegroundDrawable(@DrawableRes int res) {
        setForegroundDrawable(ResourcesCompat.getDrawable(getResources(), res, null));
    }

    public void setEmptyDrawable(@DrawableRes int res) {
        mEmptyDrawable = ResourcesCompat.getDrawable(getResources(), res, null);

        if (mAlbumArtBitmap == null && mAlbumArtDrawable == null)
            setAlbumArtDrawable(mEmptyDrawable, false);
    }


    private void initialize() {
        mPaint = new Paint();
        mDrawingRect = new Rect();
    }

    private void setAlbumArtBitmap(Bitmap bitmap, boolean animate) {
        if (mAlbumArtDrawable != null) {
            unscheduleDrawable(mAlbumArtDrawable);
            mAlbumArtDrawable.setCallback(null);
        }

        mAlbumArtDrawable = null;
        mAlbumArtBitmap = bitmap;

        mOpacity = animate ? 0.0f : 1.0f;
        invalidate();
    }

    private void setAlbumArtDrawable(Drawable drawable, boolean animate) {
        if (mAlbumArtDrawable != null) {
            unscheduleDrawable(mAlbumArtDrawable);
            mAlbumArtDrawable.setCallback(null);
        }

        mAlbumArtDrawable = drawable;
        mAlbumArtBitmap = null;

        if (mAlbumArtDrawable != null)
            mAlbumArtDrawable.setCallback(this);

        mOpacity = animate ? 0.0f : 1.0f;
        invalidate();
    }
}
