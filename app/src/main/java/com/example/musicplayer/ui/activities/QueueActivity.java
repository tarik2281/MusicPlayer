package com.example.musicplayer.ui.activities;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.example.musicplayer.R;
import com.example.musicplayer.request.RequestManager;
import com.example.musicplayer.request.ShowAlbumRequest;
import com.example.musicplayer.request.ShowArtistRequest;
import com.example.musicplayer.request.ShowFolderRequest;
import com.example.musicplayer.ui.fragments.library.QueueFragment;

public class QueueActivity extends BaseActivity implements RequestManager.Receiver {

	private static final String FRAGMENT_TAG = "fragment";

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		showBackButton(true);

		setTitle(R.string.title_queue);
		showBackButton(true);

		RequestManager.getInstance().registerReceiver(this);

		if (savedInstanceState == null) {
			QueueFragment fragment = QueueFragment.newInstance(false);
			getSupportFragmentManager().beginTransaction().add(android.R.id.content, fragment, FRAGMENT_TAG).commit();
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();

		RequestManager.getInstance().unregisterReceiver(this);
	}

	@Override
	public boolean onReceiveRequest(RequestManager.Request request) {
		int type = request.getType();

		if (type == ShowAlbumRequest.TYPE || type == ShowArtistRequest.TYPE || type == ShowFolderRequest.TYPE)
			finish();

		return false;
	}

	public static void start(Context context) {
		Intent intent = new Intent(context, QueueActivity.class);
		context.startActivity(intent);
	}
}
