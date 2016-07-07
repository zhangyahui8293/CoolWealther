package com.example.weather.activity;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.Window;
import android.webkit.WebView;
import android.widget.Adapter;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.weather.R;
import com.example.weather.db.CoolWeatherDB;
import com.example.weather.moudle.City;
import com.example.weather.moudle.County;
import com.example.weather.moudle.Province;
import com.example.weather.util.HttpCallbackListener;
import com.example.weather.util.HttpUtil;
import com.example.weather.util.Utility;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class ChooseAreaActivity extends Activity {

    /**
     * 是否从WeatherActivity中跳转过来
     */
    private boolean isFromWeatherActivity;

    private static final int LEVEL_PROVINCE = 0;
    private static final int LEVEL_CITY = 1;
    private static final int LEVEL_COUNTY = 2;

    private List<Province> provinceList;
    private List<City> cityList;
    private List<County> countyList;

    private TextView titlText;
    private ListView listView;
    private ArrayAdapter<String> adapter;
    private CoolWeatherDB coolWeatherDB;
    private List<String> dataList = new ArrayList<>();
    private ProgressDialog progressDialog;

    private Province selectedProvince;
    private City selectedCity;

    private int CURRENT_LEVEL;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        isFromWeatherActivity = getIntent().getBooleanExtra("from_weather_activity", false);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        //已经选择了城市且不是从weatherActivity跳转过来，才会直接跳转到weatherActivity
        if (prefs.getBoolean("city_selected", false) && !isFromWeatherActivity){
            Intent intent = new Intent(this, WeatherActivity.class);
            startActivity(intent);
            finish();
            return;
        }

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.choose_area);
        titlText = (TextView) findViewById(R.id.title_text);
        listView = (ListView) findViewById(R.id.list_view);
        adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, dataList);
        listView.setAdapter(adapter);
        coolWeatherDB = CoolWeatherDB.getInstance(this);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                if (CURRENT_LEVEL == LEVEL_PROVINCE){
                    selectedProvince = provinceList.get(i);
                    queryCity();
                }else if (CURRENT_LEVEL == LEVEL_CITY){
                    selectedCity = cityList.get(i);
                    querycounty();
                }else if (CURRENT_LEVEL == LEVEL_COUNTY){
                    String countyCode = countyList.get(i).getCountyCode();
                    Intent intent = new Intent(ChooseAreaActivity.this, WeatherActivity.class);
                    intent.putExtra("county_code", countyCode);
                    startActivity(intent);
                    finish();
                }
            }
        });
        queryProvince();
    }

    /**
     * 从数据库读取所有的省信息，如果没有则从服务器读取
     */
    private void queryProvince(){
        provinceList = coolWeatherDB.loadProvinces();
        if (provinceList.size() > 0){
            dataList.clear();
            for (Province province : provinceList){
                dataList.add(province.getProvinceName());
            }
            adapter.notifyDataSetChanged();
            listView.setSelection(0);
            titlText.setText("中国");
            CURRENT_LEVEL = LEVEL_PROVINCE;
        }else {
            queryFromServer(null, "province");
        }
    }

    /**
     * 从数据库读取某省的所有市信息
     */
    private void queryCity(){
        cityList = coolWeatherDB.loadCities(selectedProvince.getId());
        if (cityList.size() > 0){
            dataList.clear();
            for (City city : cityList){
                dataList.add(city.getCityName());
            }
            adapter.notifyDataSetChanged();
            listView.setSelection(0);
            titlText.setText(selectedProvince.getProvinceName());
            CURRENT_LEVEL = LEVEL_CITY;
        }else {
            queryFromServer(selectedProvince.getProvinceCode(), "city");
        }
    }

    /**
     * 从数据库读取某市的所有县信息
     */
    private void querycounty(){
        countyList = coolWeatherDB.loadCounties(selectedCity.getId());
        if (countyList.size() > 0){
            dataList.clear();
            for (County county : countyList){
                dataList.add(county.getCountyName());
            }
            adapter.notifyDataSetChanged();
            listView.setSelection(0);
            titlText.setText(selectedCity.getCityName());
            CURRENT_LEVEL = LEVEL_COUNTY;
        }else {
            queryFromServer(selectedCity.getCityCode(), "county");
        }
    }

    /**
     *根据传入的代号和类型从服务器查询省市县信息
     */
    private void queryFromServer(final String code, final String type){
        String address;
        if (!TextUtils.isEmpty(code)){
            address = "http://www.weather.com.cn/data/list3/city" + code + ".xml";
        }else {
            address = "http://www.weather.com.cn/data/list3/city.xml";
        }
        showProgressDialog();
        HttpUtil.sendRequest(address, new HttpCallbackListener() {
            boolean result =false;
            @Override
            public void onFinish(String response) {
                if ("province".equals(type)){
                    result = Utility.handleProvinces(coolWeatherDB, response);
                }else if ("city".equals(type)){
                    result = Utility.handleCities(coolWeatherDB, response, selectedProvince.getId());
                }else if ("county".equals(type)){
                    result = Utility.handleCounties(coolWeatherDB, response, selectedCity.getId());
                }

                if (result){
                    //返回主线程处理逻辑
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            closeProgressDialog();
                            if ("province".equals(type)){
                                queryProvince();
                            }else if ("city".equals(type)){
                                queryCity();
                            }else if ("county".equals(type)){
                                querycounty();
                            }
                        }
                    });
                }
            }

            @Override
            public void onError(Exception e) {
                //回到主线程处理逻辑
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        closeProgressDialog();
                        Toast.makeText(ChooseAreaActivity.this, "加载失败", Toast.LENGTH_SHORT);
                    }
                });
            }
        });
    }

    /**
     * 显示进度对话框
     */
    private void showProgressDialog(){
        if (progressDialog == null){
            progressDialog = new ProgressDialog(this);
            progressDialog.setMessage("正在加载...");
            progressDialog.setCanceledOnTouchOutside(false);
        }
        progressDialog.show();
    }

    private void closeProgressDialog(){
        if (progressDialog != null){
            progressDialog.dismiss();
        }
    }

    /**
     * 捕获back按键
     */
    @Override
    public void onBackPressed() {
        if (CURRENT_LEVEL == LEVEL_COUNTY){
            queryCity();
        }else if (CURRENT_LEVEL == LEVEL_CITY){
            queryProvince();
        }else {
            if (isFromWeatherActivity){
                Intent intent = new Intent(this, WeatherActivity.class);
                startActivity(intent);
            }
            finish();
        }
    }
}
