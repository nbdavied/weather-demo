package com.example.nbdv.weatherdemo;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.os.Handler;
import android.util.Log;

import com.example.nbdv.weatherdemo.json.JsonCities;
import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

/**
 * Created by nbdav on 2016/1/26.
 */
public class DownLoadCityThread extends Thread {
    private static final String APIURL="https://api.heweather.com/x3/citylist";
    private static final String KEY="259b556b9f504e1db746e19fe813ff22";
    private  Context context;
    private Handler handler;
    public DownLoadCityThread(Context context,Handler handler) {
        this.context=context;
        this.handler=handler;
    }

    private  String result;
    @Override
    public void run() {
        super.run();
        String requestURL=APIURL+"?search=allchina&key="+KEY;

        result="";
        URL url= null;
        HttpsURLConnection httpsURLConnection;
        handler.sendEmptyMessage(SettingActivity.DATA_LOADING);
        try {
            url = new URL(requestURL);
            httpsURLConnection= (HttpsURLConnection) url.openConnection();
            httpsURLConnection.setConnectTimeout(30000);
            httpsURLConnection.setRequestMethod("GET");

            //获得读取的内容
            InputStreamReader in;
            InputStream inputStream;
            inputStream = httpsURLConnection.getInputStream();
            in = new InputStreamReader(inputStream);
            BufferedReader buffer = new BufferedReader(in);
            String inputLine = null;
            while ((inputLine = buffer.readLine()) != null) {
                result += inputLine + "\n";
            }
            httpsURLConnection.disconnect();

            //解析json
            JsonCities cities;
            Gson gson=new Gson();
            cities=gson.fromJson(result,JsonCities.class);
            if(cities.status.equals("ok")){
                //当数据正常下载，则导入数据库
                Log.i("status", "download success");
                SQLiteDatabase db=context.openOrCreateDatabase("weather.db",Context.MODE_PRIVATE,null);

                //创建新表
                db.execSQL("DROP TABLE IF EXISTS city");
                db.execSQL("CREATE TABLE city(city VARCHAR,cnty VARCHAR,id VARCHAR PRIMARY KEY,lat float,lon float,prov VARCHAR)");
                for (JsonCities.CityInfo cityInfo:cities.city_info
                     ) {
                    //执行插入语句,将城市数据插入数据库
                    db.execSQL("insert into city values(?,?,?,?,?,?)",new Object[]{cityInfo.city,cityInfo.cnty,cityInfo.id,cityInfo.lat,cityInfo.lon,cityInfo.prov});
                }
                db.close();

                handler.sendEmptyMessage(SettingActivity.DATA_LOADING_FINISHED);
            }else
            {
                //未正常下载数据
                Log.i("status",cities.status);
                handler.sendEmptyMessage(SettingActivity.DATA_LOADING_FAULT);
            }

        } catch (MalformedURLException e) {
            e.printStackTrace();
            handler.sendEmptyMessage(SettingActivity.DATA_LOADING_FAULT);
        } catch (ProtocolException e) {
            e.printStackTrace();
            handler.sendEmptyMessage(SettingActivity.DATA_LOADING_FAULT);
        } catch (IOException e) {
            e.printStackTrace();
            handler.sendEmptyMessage(SettingActivity.DATA_LOADING_FAULT);
            Log.e("error","internet wrong");
        }

    }
}
