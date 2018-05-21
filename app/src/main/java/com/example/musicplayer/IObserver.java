package com.example.musicplayer;

/**
 * Created by 19tarik97 on 25.12.16.
 */

public interface IObserver<T> {
    void update(Observable sender, T data);
}
