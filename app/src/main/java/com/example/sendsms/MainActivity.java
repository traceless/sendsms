package com.example.sendsms;

import android.Manifest;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.telephony.PhoneStateListener;
import android.telephony.SmsManager;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity {

    private EditText fromEmailEdit;

    private EditText fromEmailPasswdEdit;

    private EditText mobileEdit;

    private EditText contentEdit;

    private EditText emailEdit;

    private Toast toast;

    private Switch enableSetting;

    private static String switchCheck = "enable";

    private static String SELECT_MOBILE = "SELECT_MOBILE";

    private static String mobileNumber = "mobile";

    private static String email = "email";

    public static String fromEmail = "fromEmail";

    public static String fromEmailPasswd = "fromEmailPasswd";

    private long lastDate = new Date().getTime(); // 最新的短信时间，按这个时间进行赛选


    private SMSContentObserver smsContentObserver;
    protected static final int MSG_INBOX = 1;
    private Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_INBOX:
                    checkNewSms();
                    break;
            }
        }
    };

    private PendingIntent send_intent;

    private String myselfTel;

    private PhoneStateListener mPhoneStateListener;

    private TelephonyManager tm;

    //    @RequiresApi(api = Build.VERSION_CODES.M)
    @TargetApi(Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && checkSelfPermission(Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.SEND_SMS}, 102);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && checkSelfPermission(Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.READ_SMS}, 103);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && checkSelfPermission(Manifest.permission.RECEIVE_SMS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.RECEIVE_SMS}, 104);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && checkSelfPermission(Manifest.permission.INTERNET) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.INTERNET}, 105);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && checkSelfPermission(Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.READ_PHONE_STATE}, 105);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && checkSelfPermission(Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.CALL_PHONE}, 105);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && checkSelfPermission(Manifest.permission.READ_CALL_LOG) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.READ_CALL_LOG}, 105);
        }
        // 设置短信处理
        smsContentObserver = new SMSContentObserver(MainActivity.this, mHandler);
        Context mContext = getApplicationContext();
        // 初始化控件
        mobileEdit = findViewById(R.id.mobile);
        contentEdit = findViewById(R.id.content);
        emailEdit = findViewById(R.id.email);
        fromEmailEdit = findViewById(R.id.fromEmail);
        fromEmailPasswdEdit = findViewById(R.id.fromEmailPasswd);
        // 初始化本地缓存
        AppData.initAppData(mContext);
        // 获取之前保存的配置更新进去
        this.initConfig();
        // 设置1卡发短信
        Switch selectMobile = findViewById(R.id.select_mobile);
        selectMobile.setChecked(AppData.getInstance().getProperties(SELECT_MOBILE, boolean.class));
        selectMobile.setOnCheckedChangeListener((bv, check) -> {
            saveConfig();
            AppData.getInstance().setProperties(SELECT_MOBILE, check);
        });
        // 测试按钮发送
        Button testBtn = findViewById(R.id.testBtn);
        testBtn.setOnClickListener(view -> {
            String content = contentEdit.getText().toString();
            AppData.getInstance().setProperties(mobileNumber, mobileEdit.getText().toString());
            saveConfig();
            sendSms(content);
        });

        // 是否启动监听短信
        enableSetting = findViewById(R.id.enable_setting);
        enableSetting.setChecked(AppData.getInstance().getProperties(switchCheck, boolean.class));
        enableSetting.setOnCheckedChangeListener((bv, check) -> {
            AppData.getInstance().setProperties(switchCheck, check);
            saveConfig();
        });
        // 保存设置
        Button settingBtn = findViewById(R.id.setting);
        settingBtn.setOnClickListener(view -> {
            saveConfig();
        });

        // 监听来电
        mPhoneStateListener = new PhoneStateListener() {
            public void onCallStateChanged(int state, String incomingNumber) {
                switch (state) { // 如果电话铃响
                    case TelephonyManager.CALL_STATE_RINGING:
                        toastShow("来电号码是：" + incomingNumber);
                        sendSms("来电号码是：" + incomingNumber);
                }
            }
        };
        //获得电话管理器
        tm = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
        //设置监听
        tm.listen(mPhoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);

        //state:电话的状态
        //incomingNumber:打进来的号码
        myselfTel = tm.getLine1Number();//获取本机号码
        // 监听短信发送情况
        String SEND = "sms_send";
        send_intent = PendingIntent.getBroadcast(this, 0, new Intent(SEND), 0);
        registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                int res = getResultCode();
                switch (res) {
                    case Activity.RESULT_OK:
                        toastShow("发送成功了呀！");
                        break;
                    default:
                        toastShow("好像发送失败呀！");
                        break;
                }
            }
        }, new IntentFilter(SEND));
    }

    /**
     * 查询最新的短信，进行转发
     */
    private void checkNewSms() {
        Cursor cursor = null;
        // 添加异常捕捉
        try {
            cursor = getContentResolver().query(
                    Uri.parse("content://sms/inbox"),
                    new String[]{"_id", "address", "read", "body", "date"},
                    null, null, "date desc"); // datephone想要的短信号码
            if (cursor != null) { // 当接受到的新短信与想要的短信做相应判断
                String body = "";
                while (cursor.moveToNext()) {
                    String address = cursor.getString(cursor.getColumnIndex("address"));// 在这里获取短信信息
                    body = cursor.getString(cursor.getColumnIndex("body"));// 在这里获取短信信息
                    long smsdate = Long.parseLong(cursor.getString(cursor
                            .getColumnIndex("date")));
                    // 如果当前时间和短信时间间隔超过60秒，可以认为这条短信无效
                    SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
                    String date = format.format(new Date(smsdate));
                    if (lastDate - smsdate > 1) {
                        Log.i("--smsbody--", body + " 时间：" + date);
                        break;
                    }
                    lastDate = new Date().getTime();
                    boolean isSend = AppData.getInstance().getProperties(switchCheck, boolean.class);
                    if (isSend) {
                        sendSms("来自：" + address + " \n" + body);
                    }
                    Log.i(MainActivity.class.toString(), "have new sms in this time " + isSend);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    /**
     * 执行发送短信
     * @param content
     */
    @TargetApi(Build.VERSION_CODES.N)
    private void sendSms(String content) {
        // 如果不是验证码，那么就使用邮箱发送
        Pattern pattern = Pattern.compile("(\\d{4,6})");
        Matcher matcher = pattern.matcher(content);
        if (matcher.find()) {
            String code = matcher.group(0);
            toastShow("验证码：" + code);
        }
        String emailAddr = AppData.getInstance().getProperties(email, String.class);
        if (emailAddr == null || emailAddr.equals("")) {
            toastShow("邮件地址不能为空");
            return;
        }
        // 默认邮件发送
        new Thread(() -> {
            try {
                Email.send(content, emailAddr);
            } catch (Exception e) {
                toastShow("发送邮件异常" + e.getMessage());
            }
        }).start();
        // 如果b不是验证码或者来电，那么就不发送短信
        if (!content.contains("验证码") && !content.contains("来电号码")) {
            toastShow("非验证码和来电，改用邮件发送！");
            return;
        }
        // 判断短信是否发送频繁，是的话就返回。
        if (!ApiCallLockController.getInstance("sendSms").process(false)) {
            toastShow("发送验证码短信频繁, 已用邮件发送");
            return;
        }
        try {
            String mobileNum = AppData.getInstance().getProperties("mobile");
            if (mobileNum == null || mobileNum.length() != 11) {
                toastShow("手机号格式不对");
                return;
            }
            if (myselfTel != null && myselfTel.contains(mobileNum)) {
                toastShow("不能是自己的号码！");
                return;
            }

            // 这种方式也是获取subId
            SubscriptionManager sManager = (SubscriptionManager) this.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE);
            Method getSubId = SubscriptionManager.class.getMethod("getSubId", int.class);
            Object subIdObj = getSubId.invoke(sManager, 1);
            Log.i("-----MainActivity mSubId-----：", subIdObj.toString());
            // 这种方式也是可以获取到subId
            @SuppressLint("MissingPermission")
            List<SubscriptionInfo> list = sManager.getActiveSubscriptionInfoList();
            //0:默认卡1发送；1：默认卡2发送
            int index = AppData.getInstance().getProperties(SELECT_MOBILE, boolean.class) ? 1 : 0;
            int subId = list.get(index).getSubscriptionId();
            // 可以用反射的方式
//            SmsManager sms = SmsManager.getDefault();
//            Class smClass = SmsManager.class; //通过反射查到了SmsManager有个叫做mSubId的属性
//            Field field = smClass.getDeclaredField("mSubId");
//            field.setAccessible(true);
//            field.set(sms, subId);
//            Object value = field.get(sms);
            SmsManager sms = SmsManager.getSmsManagerForSubscriptionId(subId);
            toastShow(mobileNum + " - " + subId + " 内容： " + content);
            if (content.length() > 70) {
                ArrayList<String> msgs = sms.divideMessage(content);
                ArrayList<PendingIntent> sentIntents = new ArrayList<>();
                for (int i = 0; i < msgs.size(); i++) {
                    sentIntents.add(send_intent);
                }
                //这种方式还是发送多条短信，但用户收到的短信会是连在一起的一整条。
                sms.sendMultipartTextMessage(mobileNum, null, msgs, sentIntents, null);
            } else {
                sms.sendTextMessage(mobileNum, null, content, send_intent, null);
            }
        } catch (Exception e) {
            toastShow("发送出错");
            e.printStackTrace();
        }
    }


    public void saveConfig() {
        String str = emailEdit.getText().toString();
        if (str == null || str.equals("")) {
            toastShow("接收邮箱不能为空");
            return;
        }
        str = mobileEdit.getText().toString();
        if (str == null || str.equals("")) {
            toastShow("手机号不能为空");
            return;
        }
        str = fromEmailEdit.getText().toString();
        if (str == null || str.equals("")) {
            toastShow("发送邮箱不能为空");
            return;
        }
        str = fromEmailPasswdEdit.getText().toString();
        if (str == null || str.equals("")) {
            toastShow("密码不能为空");
            return;
        }
        AppData.getInstance().setProperties(mobileNumber, mobileEdit.getText().toString());
        AppData.getInstance().setProperties(email, emailEdit.getText().toString());
        AppData.getInstance().setProperties(fromEmail, fromEmailEdit.getText().toString());
        AppData.getInstance().setProperties(fromEmailPasswd, fromEmailPasswdEdit.getText().toString());
        toastShow("保存成功");
    }

    /**
     * 初始化配置
     */
    public void initConfig() {
        String mobile = AppData.getInstance().getProperties(mobileNumber);
        mobileEdit.setText(mobile);
        String emailAddr = AppData.getInstance().getProperties(email);
        emailEdit.setText(emailAddr);
        String fEmail = AppData.getInstance().getProperties(fromEmail);
        fromEmailEdit.setText(fEmail);
        String emailPw = AppData.getInstance().getProperties(fromEmailPasswd);
        fromEmailPasswdEdit.setText(emailPw);

    }


    public void toastShow(String msg) {
        toast = Toast.makeText(this, "", Toast.LENGTH_SHORT);
        //toast.setDuration(Toast.LENGTH_LONG);//这一个不能重复设置，不然无法实时显示
        Log.i(MainActivity.class.toString(), msg);
        toast.setText(msg);
        toast.show();
    }

    @Override
    protected void onResume() {
        // TODO Auto-generated method stub
        super.onResume();
        if (smsContentObserver != null) {
            getContentResolver().registerContentObserver(
                    Uri.parse("content://sms/"), true, smsContentObserver);// 注册监听短信数据库的变化
        }
    }

}
