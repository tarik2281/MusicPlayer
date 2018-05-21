package com.example.musicplayer.ui.fragments;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.musicplayer.R;
import com.example.musicplayer.ui.activities.IntroActivity;

/**
 * Created by Tarik on 16.10.2017.
 */

public class GettingStartedFragment extends Fragment implements View.OnClickListener {

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_getting_started, null);

        if (getResources().getBoolean(R.bool.isTablet)) {
            ((TextView)v.findViewById(R.id.text_instruction)).setText(R.string.intro_instruction_getting_started_tablet);
            ((ImageView)v.findViewById(R.id.image)).setImageResource(R.drawable.screen_getting_started_tablet);
        }

        v.findViewById(R.id.button_done).setOnClickListener(this);

        return v;
    }

    @Override
    public void onClick(View v) {
        if (getActivity() instanceof IntroActivity) {
            ((IntroActivity)getActivity()).finishSuccess();
        }
    }
}
