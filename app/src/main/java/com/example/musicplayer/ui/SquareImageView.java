package com.example.musicplayer.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.support.v7.widget.AppCompatImageView;

public class SquareImageView extends AppCompatImageView {

	public SquareImageView(Context context) {
		this(context, null);
	}
	
	public SquareImageView(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}
	
	public SquareImageView(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);

	    /*int width = MeasureSpec.getSize(widthMeasureSpec);
		int height = MeasureSpec.getSize(heightMeasureSpec);

		int size = widthMeasureSpec;

		if (width == 0)
			size = heightMeasureSpec;
		else if (height == 0)
			size = widthMeasureSpec;

	    setMeasuredDimension(size, size);*/
	}
}
