package com.example.weather.util;

import android.text.TextUtils;

import com.example.weather.db.CoolWeatherDB;
import com.example.weather.moudle.Province;

import java.lang.reflect.Array;

/**
 * Created by ZYH on 2016/6/30.
 */
public class Utility {

    /**
     * 解析和处理服务器返回的省级数据
     */
    private synchronized static boolean handleProvinces(CoolWeatherDB coolWeatherDB,
                                                        String response){
        if (!TextUtils.isEmpty(response)){
            String[] provinces = response.split(",");
            if (provinces != null && provinces.length > 0){
                for (String p : provinces){
                    String[] array = p.split("\\|");
                    Province province = new Province();
                    province.setProvinceCode(array[0]);
                    province.setProvinceName(array[1]);
                    //将解析出来的数据存储到Province表
                    coolWeatherDB.saveProvince(province);
                }
                return true;
            }
        }
        return false;
    }
}
