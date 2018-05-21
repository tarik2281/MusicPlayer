package com.example.musicplayer;

import android.content.Context;
import android.content.res.TypedArray;
import android.preference.EditTextPreference;
import android.text.InputFilter;
import android.text.InputType;
import android.util.AttributeSet;

/**
 * Created by 19tarik97 on 05.09.16.
 */
public class IntegerPreference extends EditTextPreference {

    private int mValue;

    public IntegerPreference(Context context, AttributeSet attrs) {
        super(context, attrs);

        getEditText().setInputType(InputType.TYPE_CLASS_NUMBER);
        getEditText().setFilters(new InputFilter[] { new InputFilter.LengthFilter(6) });
    }

    public IntegerPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        getEditText().setInputType(InputType.TYPE_CLASS_NUMBER);
        getEditText().setFilters(new InputFilter[] { new InputFilter.LengthFilter(6) });
    }

    @Override
    public String getText() {
        return String.valueOf(mValue);
    }

    @Override
    public void setText(String text) {
        final boolean wasBlocking = shouldDisableDependents();

        mValue = Integer.parseInt(text);

        persistInt(mValue);

        final boolean isBlocking = shouldDisableDependents();
        if (isBlocking != wasBlocking) {
            notifyDependencyChange(isBlocking);
        }
    }

    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
        return a.getInteger(index, 0);
    }

    @Override
    protected void onSetInitialValue(boolean restoreValue, Object defaultValue) {
        setText(restoreValue ? String.valueOf(getPersistedInt(0)) : String.valueOf((int)defaultValue));
    }

    @Override
    public boolean shouldDisableDependents() {
        return mValue == 0 || super.shouldDisableDependents();
    }
}
