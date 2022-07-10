package com.example.sendsms;

import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;


/**
 * API接口调用次数限制,限制API调用次数，只针对单机节点，分布式集群请考虑redis，zookeeper <Change to the actual
 *
 * @author doctor
 */
public class ApiCallLockController {


    private static SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    private static Map<String, ApiCallLockController> maps = new HashMap<>();

    private Long startTime = System.currentTimeMillis();

    private int callTimes = 0;// 已经调用次数

    private int callLimit = 20;// 限制次数

    private int timeLimit = 10 * 60 * 1000;// 10分钟

    private String name;

    public ApiCallLockController(String name) {
        this.name = name;
        if (name == null) {
            this.name = UUID.randomUUID().toString();
        }

    }

    public ApiCallLockController(int callLimit, String name) {
        this.callLimit = callLimit;
        this.name = name;
    }

    public ApiCallLockController(int callLimit, int timeLimit, String name) {
        this.callLimit = callLimit;
        this.timeLimit = timeLimit;
        this.name = name;
    }

    public int getCallLimit() {
        return callLimit;
    }

    public void setCallLimit(int callLimit) {
        this.callLimit = callLimit;
    }

    public int getTimeLimit() {
        return timeLimit;
    }

    public void setTimeLimit(int timeLimit) {
        this.timeLimit = timeLimit;
    }

    public int getCallTimes() {
        return callTimes;
    }

    public void setCallTimes(int callTimes) {
        this.callTimes = callTimes;
    }

    /**
     * 执行过程，重置以及睡眠
     *
     * @param waitting 是否阻塞等待
     * @return
     */
    public synchronized boolean process(boolean waitting) {
        long now = System.currentTimeMillis();
        long time = now - startTime;
        // 判断当前时间是否超过限制时间
        if (time > timeLimit) {
            // 重置接口已调用次数为0
            callTimes = 0;
            startTime = now;
        }
        // 这里要用大于等于>= 防止异常，导致判断错误
        while (callTimes >= callLimit && time <= timeLimit) {
            // 如果不等待，立即放回false
            if (!waitting) {
                return false;
            }
            try {
                // 调用20次API大概用时4-6秒，这里大部分时候其实都在等待，所以设置大一些。
                Thread.sleep(2200);
            } catch (InterruptedException e) {
                Log.i(ApiCallLockController.class.toString(), this.name + " error:" + e.getMessage());
                e.printStackTrace();
                return false;
            }
            time = System.currentTimeMillis() - startTime;
            Log.i(ApiCallLockController.class.toString(), "come in sleep ,controller name：" + this.name + " current time: " + format.format(new Date())
                    + " callTimes: " + callTimes);
        }
        callTimes++;
        return true;
    }

    /**
     * 这里的锁，只有第一次生产对象才会有竞争，对性能没影响
     *
     * @param name
     * @return
     */
    public static ApiCallLockController getInstance(String name) {
        ApiCallLockController ctrl = maps.get(name);
        if (ctrl != null) {
            return ctrl;
        }
        synchronized (ApiCallLockController.class) {
            ctrl = maps.get(name);
            if (ctrl == null) {
                ctrl = new ApiCallLockController(name);
                maps.put(name, ctrl);
            }
            return ctrl;
        }
    }
}
