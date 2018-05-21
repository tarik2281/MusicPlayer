package com.example.musicplayer;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by 19tarik97 on 25.12.16.
 */

public class Observable<T> {

    private List<IObserver<T>> mObservers = new ArrayList<>();

    public void addObserver(IObserver<T> observer) {
        mObservers.add(observer);
    }

    public void removeObserver(IObserver<T> observer) {
        mObservers.remove(observer);
    }

    protected void notifyObservers(T data) {
        IObserver<T>[] observers = mObservers.toArray(new IObserver[mObservers.size()]);
        for (IObserver<T> o : observers) {
            o.update(this, data);
        }
    }
}
