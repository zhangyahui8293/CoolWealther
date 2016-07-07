package com.example.weather.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.text.TextUtils;

import com.example.weather.db.CoolWeatherDB;
import com.example.weather.moudle.City;
import com.example.weather.moudle.County;
import com.example.weather.moudle.Province;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Array;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Created by ZYH on 2016/6/30.
 */
public class Utility {

    /**
     * 解析和处理服务器返回的省级数据
     */
    public synchronized static boolean handleProvinces(CoolWeatherDB coolWeatherDB,
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

    /**
     * 解析和处理服务器返回的市级数据
     */
    public synchronized static boolean handleCities(CoolWeatherDB coolWeatherDB, String response,
                                                    int provinceId){
        if (!TextUtils.isEmpty(response)){
            String[] cities = response.split(",");
            if (cities != null && cities.length > 0){
                for (String p : cities){
                    City city = new City();
                    String[] array = p.split("\\|");
                    city.setProvinceId(provinceId);
                    city.setCityCode(array[0]);
                    city.setCityName(array[1]);
                    //将解析出来的数据存储到City表
                    coolWeatherDB.saveCity(city);
                }
                return true;
            }
        }
        return false;
    }

    /**
     * 解析和处理服务器返回的County信息
     */
    public synchronized static boolean handleCounties(CoolWeatherDB coolWeatherDB, String response,
                                                      int cityId){
        if (!TextUtils.isEmpty(response)){
            String[] counties = response.split(",");
            if (counties != null && counties.length > 0){
                for (String p : counties){
                    County county = new County();
                    String[] array = p.split("\\|");
                    county.setCityId(cityId);
                    county.setCountyCode(array[0]);
                    county.setCountyName(array[1]);
                    //将解析出来的数据存储到数据库
                    coolWeatherDB.saveCounty(county);
                }
                return true;
            }
        }
        return false;
    }

    /**
     * 解析服务器返回的JSON数据，并将解析出的数据存储到本地
     */
    public static void handleWeatherResponse(Context context, String response){
        try {
            JSONObject jsonObject = new JSONObject(response);
            JSONObject weatherInfo = jsonObject.getJSONObject("weatherinfo");
            String cityName = weatherInfo.getString("city");
            String weatherCode = weatherInfo.getString("cityid");
            String temp1 = weatherInfo.getString("temp1");
            String temp2 = weatherInfo.getString("temp2");
            String weatherDesp = weatherInfo.getString("weather");
            String publishTime = weatherInfo.getString("ptime");
            saveWeatherInfo(context, cityName, weatherCode, temp1, temp2, weatherDesp, publishTime);
        }catch (JSONException e){
            e.printStackTrace();
        }
    }

    /**
     * 将服务器返回的所有天气信息存储到SharedPreference文件中
     */
    public static void saveWeatherInfo(Context context, String cityName, String weatherCode,
                                       String temp1, String temp2, String weatherDesp, String publishTime){
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy年M月d日", Locale.CHINA);
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
        editor.putBoolean("city_selected", true);
        editor.putString("city_name", cityName);
        editor.putString("weather_code", weatherCode);
        editor.putString("temp1", temp1);
        editor.putString("temp2", temp2);
        editor.putString("weather_desp", weatherDesp);
        editor.putString("publish_time", publishTime);
        editor.putString("current_date", sdf.format(new Date()));
        editor.commit();
    }

}
