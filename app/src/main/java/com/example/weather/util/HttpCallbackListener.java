package com.example.weather.util;

/**
 * Created by ZYH on 2016/6/30.
 */
public interface HttpCallbackListener {

    void onFinish(String response);
    void onError(Exception e);
}
