package com.example.musicplayer.request;

import android.os.Handler;

import com.example.musicplayer.Util;
import com.example.musicplayer.ui.fragments.PaneFragment;

import java.util.ArrayList;
import java.util.Iterator;

/**
 * Created by Tarik on 08.06.2016.
 */
public class RequestManager {

    public static class OpenPlayerRequest extends Request {

        public static final int TYPE = Util.HashFNV1a32("OpenPlayer");

        public OpenPlayerRequest() {

        }

        @Override
        public int getType() {
            return TYPE;
        }
    }

    public static class BackRequest extends Request {

        public static final int TYPE = Util.HashFNV1a32("Back");

        public BackRequest() {

        }

        @Override
        public int getType() {
            return TYPE;
        }
    }

    public static abstract class Request {

        private PaneFragment mPaneFragment;

        public PaneFragment getSenderFragment() {
            return mPaneFragment;
        }

        public void setSenderFragment(PaneFragment fragment) {
            mPaneFragment = fragment;
        }

        public abstract int getType();
    }

    public interface Receiver {
        boolean onReceiveRequest(Request request);
    }

    private static RequestManager sSingleton;

    public static RequestManager getInstance() {
        if (sSingleton == null)
            sSingleton = new RequestManager();

        return sSingleton;
    }

    private final Runnable mPollRequests = new Runnable() {
        @Override
        public void run() {
            mCallbackEnqueued = false;
            pollRequests();
        }
    };

    private static final int QUEUES_COUNT = 2;

    private ArrayList<Receiver> mReceivers;
    private ArrayList<Request>[] mRequestQueues;
    private int mProcessQueue;
    private int mActiveQueue;

    private Handler mHandler;
    private boolean mCallbackEnqueued;

    @SuppressWarnings("unchecked")
    private RequestManager() {
        mReceivers = new ArrayList<>();

        mRequestQueues = new ArrayList[QUEUES_COUNT];

        for (int i = 0; i < QUEUES_COUNT; i++)
            mRequestQueues[i] = new ArrayList<>();

        mProcessQueue = 0;
        mActiveQueue = 0;

        mHandler = new Handler();
        mCallbackEnqueued = false;
    }

    public void registerReceiver(Receiver receiver) {
        mReceivers.add(0, receiver);
    }

    public void unregisterReceiver(Receiver receiver) {
        mReceivers.remove(receiver);
    }

    public void pushRequest(Request request) {
        mRequestQueues[mActiveQueue].add(request);

        if (!mCallbackEnqueued)
            mHandler.post(mPollRequests);

        mCallbackEnqueued = true;
    }

    public void triggerRequest(Request request) {
        for (Receiver receiver : mReceivers)
            if (receiver.onReceiveRequest(request))
                break;
    }

    private void pollRequests() {
        mProcessQueue = mActiveQueue;
        mActiveQueue = (mActiveQueue + 1) % QUEUES_COUNT;

        for (Iterator<Request> it = mRequestQueues[mProcessQueue].iterator(); it.hasNext(); ) {
            Request request = it.next();

            triggerRequest(request);

            it.remove();
        }
    }
}
