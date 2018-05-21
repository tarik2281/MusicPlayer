package com.example.musicplayer.ui;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Shader;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import com.example.musicplayer.R;

public class CoverView extends View {

	private static final float ANIMATION_STEP = 0.1f;

	private static final int MODE_NONE = 0;
	private static final int MODE_SQUARE_WIDTH = 1;
	private static final int MODE_SQUARE_HEIGHT = 2;
	private static final int MODE_FLEXIBLE = 3;

	private Bitmap mCurrentBitmap;
	private Bitmap mNextBitmap;
	private Bitmap mEnqueuedBitmap;

	private float mOpacity;

	private int mSizeMode;
	private boolean mDrawGradient;
	private int mGradientColor;

    private LinearGradient mGradientShader;
	private Rect mDrawingRect;
	private Paint mPaint;

	public CoverView(Context context) {
		this(context, null);
	}

	public CoverView(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public CoverView(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);

		TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.CoverView);

		mSizeMode = a.getInteger(R.styleable.CoverView_sizeMode, MODE_NONE);
		mDrawGradient = a.getBoolean(R.styleable.CoverView_drawGradient, false);
		mGradientColor = a.getColor(R.styleable.CoverView_gradientColor, Color.WHITE);

		a.recycle();

		initialize();
	}

	@Override
	protected void onDraw(Canvas canvas) {
		float w = getWidth();
		float h = getHeight();

		if (w > h) {
			float y = h / 2.0f - w / 2.0f;

			mDrawingRect.set(0, (int)y, (int)w, (int)(y + w));
		}
		else {
			float x = w / 2.0f - h / 2.0f;

			mDrawingRect.set((int)x, 0, (int)(x + h), (int)h);
		}

		mPaint.setColor(Color.WHITE);
		mPaint.setShader(null);

		if (mCurrentBitmap != null)
			canvas.drawBitmap(mCurrentBitmap, null, mDrawingRect, mPaint);

		if (mNextBitmap != null) {
			mOpacity += ANIMATION_STEP;

			mPaint.setAlpha((int)(mOpacity * 255.0f));
			canvas.drawBitmap(mNextBitmap, null, mDrawingRect, mPaint);

			if (mOpacity >= 1.0f) {
				mCurrentBitmap = mNextBitmap;

				mOpacity = 0.0f;
				mNextBitmap = mEnqueuedBitmap;
				mEnqueuedBitmap = null;
			}

			ViewCompat.postInvalidateOnAnimation(this);
		}

		if (mDrawGradient) {
			if (mGradientShader == null)
				mGradientShader = new LinearGradient(0, 0, 0, getHeight(), Color.TRANSPARENT, mGradientColor, Shader.TileMode.CLAMP);

			mPaint.setShader(mGradientShader);
			mPaint.setAlpha(255);
			canvas.drawRect(0, 0, getWidth(), getHeight(), mPaint);
		}
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		return true;
	}

	public void setCurrentBitmap(Bitmap bitmap) {
		mCurrentBitmap = bitmap;

		ViewCompat.postInvalidateOnAnimation(this);
	}

	public void enqueueBitmap(Bitmap bitmap) {
		if (mCurrentBitmap == null)
			setCurrentBitmap(bitmap);
		else if (mNextBitmap == null) {
			mNextBitmap = bitmap;
			mOpacity = 0.0f;
			ViewCompat.postInvalidateOnAnimation(this);
		}
		else
			mEnqueuedBitmap = bitmap;
	}

	private void initialize() {
		mDrawingRect = new Rect();
		mPaint = new Paint();
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		switch (mSizeMode) {
			case MODE_SQUARE_WIDTH: {
				int width = MeasureSpec.getSize(widthMeasureSpec);

				setMeasuredDimension(width, width);
				break;
			}
			case MODE_SQUARE_HEIGHT: {
				int height = MeasureSpec.getSize(heightMeasureSpec);

				setMeasuredDimension(height, height);
				break;
			}
			case MODE_FLEXIBLE: {
				int width = MeasureSpec.getSize(widthMeasureSpec);
				int height = MeasureSpec.getSize(heightMeasureSpec);

				if (height > width)
					height = width;

				setMeasuredDimension(width, height);
				break;
			}
			default:
				super.onMeasure(widthMeasureSpec, heightMeasureSpec);
				break;
		}
	}
}
