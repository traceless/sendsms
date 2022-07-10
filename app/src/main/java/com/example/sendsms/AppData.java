package com.example.sendsms;

import android.content.Context;
import android.content.SharedPreferences;

import com.alibaba.fastjson.JSONObject;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 时间 ：2018/3/5
 */
public class AppData {

    private static Lock lockHelper = new ReentrantLock();

    private static AppData appData;

    private SharedPreferences sp;

    public static final String tokenData = "token_data_";

    public static final String mobile = "mobile_";

    public static final String channel = "channel_";

    public static final String userData = "user_data_";

    /**
     * Instantiates a new App data.
     *
     * @param content the content
     */
    private AppData(Context content) {
        sp = content.getSharedPreferences("config", Context.MODE_PRIVATE);
    }

    public static AppData getInstance() {
        return appData;
    }
    /**
     * Get instance app data.
     *
     * @param context the context
     * @return the app data
     */
    public static AppData initAppData(Context context) {
        if (appData == null){
            lockHelper.lock();
            if(appData==null){
                appData = new AppData(context);
            }
            lockHelper.lock();
        }
        return appData;
    }


    /**
     * 设置数据
     *
     * @param key 键名称
     * @param value 键值
     */
    public boolean setProperties(String key, Object value) {
        try {
            return sp.edit().putString(key, JSONObject.toJSONString(value)).commit();
        }catch (Exception e){
            e.printStackTrace();
            Object ee = e;
            return false;
        }
    }

    /**
     * 设置数据
     *
     * @param key 键名称
     * @param value 键值
     */
    public void setProperties(String key, String value) {
        sp.edit().putString(key, value).commit();
    }


    /**
     * 设置数据
     *
     * @param key 键名称
     * @param value 键值
     */
    public void setProperties(String key, int value) {
        sp.edit().putInt(key, value).commit();
    }

    /**
     * 设置数据
     *
     * @param key 键名称
     * @param value 键值
     */
    public void setProperties(String key, float value) {
        sp.edit().putFloat(key, value).commit();
    }

    /**
     * 设置数据
     *
     * @param key 键名称
     * @param value 键值
     */
    public void setProperties(String key, boolean value) {
        sp.edit().putBoolean(key, value).commit();
    }

    /**
     * 设置数据
     *
     * @param key 键名称
     * @param value 键值
     */
    public void setProperties(String key, long value) {
        sp.edit().putLong(key, value).commit();
    }


    /**
     * 获取key
     * @param key 键名称
     * @return 键值类型
     */
    public String getProperties(String key){
        return sp.getString(key, null);
    }

    /**
     * 返回值
     * @param key 键名称
     * @param clazz 键值类型
     * @param <T> 返回对象
     * @return
     */
    public <T> T getProperties(String key, Class<T> clazz) {

        try{
            switch (clazz.getSimpleName()) {
                case "String":
                    return (T) sp.getString(key, null);
                case "boolean":
                    return (T) Boolean.valueOf(sp.getBoolean(key, false));
                case "Boolean":
                    return (T) Boolean.valueOf(sp.getBoolean(key, false));
                case "Float":
                    return (T) Float.valueOf(sp.getFloat(key, 0));
                case "float":
                    return (T) Float.valueOf(sp.getFloat(key, 0));
                case "long":
                    return (T) Long.valueOf(sp.getLong(key, 0));
                case "Long":
                    return (T) Long.valueOf(sp.getLong(key, 0));
                case "int":
                    return (T) Integer.valueOf(sp.getInt(key, 0));
                case "Integer":
                    return (T) Integer.valueOf(sp.getInt(key, 0));
                default: {
                    String json = sp.getString(key, "{}");
                    return JSONObject.parseObject(json, clazz);
                }
            }

        }catch (Exception e){
            return null;
        }
    }


}
