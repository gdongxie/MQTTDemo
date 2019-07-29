package com.dongxie.mqttdemo;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;
import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.*;

/**
 * @ClassName: MyMQTTService
 * @Description:
 * @Author: dongxie
 * @CreateDate: 2019/7/29 10:55
 */
public class MyMQTTService extends Service {
    public static final String TAG = MyMQTTService.class.getSimpleName();
    private static MqttAndroidClient mqttAndroidClient;
    private MqttConnectOptions mMqttConnectOptions;
    /**
     * 服务器地址（协议+地址+端口号）
     */
    public String HOST = "tcp://192.168.8.113:61613";
    /**
     * 用户名
     */
    public String USERNAME = "admin";
    /**
     * 密码
     */
    public String PASSWORD = "password";
    /**
     * 发布主题
     */
    public static String PUBLISH_TOPIC = "publish_test";
    /**
     * 响应主题
     */
    public static String RESPONSE_TOPIC = "test_arrived";

    @SuppressLint("MissingPermission")
    public String CLIENTID = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ? Build.getSerial() : Build.SERIAL;


    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        init();
        return super.onStartCommand(intent, flags, startId);
    }

    /**
     * 初始化操作
     */
    private void init() {
        //服务器地址（协议+地址+端口号）
        String serverURI = HOST;
        mqttAndroidClient = new MqttAndroidClient(this, serverURI, CLIENTID);
        //设置监听订阅消息的回调
        mqttAndroidClient.setCallback(mqttCallback);
        mMqttConnectOptions = new MqttConnectOptions();
        //设置是否清除缓存
        mMqttConnectOptions.setCleanSession(true);
        //设置超时时间，单位：秒
        mMqttConnectOptions.setConnectionTimeout(10);
        //设置心跳包发送间隔，单位：秒
        mMqttConnectOptions.setKeepAliveInterval(20);
        //设置用户名
        mMqttConnectOptions.setUserName(USERNAME);
        //设置密码
        mMqttConnectOptions.setPassword(PASSWORD.toCharArray());

        // last will message
        boolean doConnect = true;
        String message = "{\"terminal_uid\":\"" + CLIENTID + "\"}";
        String topic = PUBLISH_TOPIC;
        Integer qos = 2;
        Boolean retained = false;
        if ((!message.equals("")) || (!topic.equals(""))) {
            // 最后的遗嘱
            try {
                mMqttConnectOptions.setWill(topic, message.getBytes(), qos.intValue(), retained.booleanValue());
            } catch (Exception e) {
                Log.e(TAG, "Exception Occured", e);
                doConnect = false;
                iMqttActionListener.onFailure(null, e);
            }
        }
        if (doConnect) {
            doClientConnection();
        }
    }

    private void doClientConnection() {
        if (!mqttAndroidClient.isConnected() && isConnectIsNomarl()) {
            try {
                mqttAndroidClient.connect(mMqttConnectOptions, null, iMqttActionListener);
            } catch (MqttException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 判断网络是否连接
     */
    private boolean isConnectIsNomarl() {
        ConnectivityManager connectivityManager = (ConnectivityManager) this.getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo info = connectivityManager.getActiveNetworkInfo();
        if (info != null && info.isAvailable()) {
            String name = info.getTypeName();
            Log.e(TAG, "当前网络名称：" + name);
            return true;
        } else {
            Log.e(TAG, "没有可用网络");
            /*没有可用网络的时候，延迟3秒再尝试重连*/
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    doClientConnection();
                }
            }, 3000);
            return false;
        }
    }

    /**
     * 连接监听
     */
    private IMqttActionListener iMqttActionListener = new IMqttActionListener() {
        @Override
        public void onSuccess(IMqttToken asyncActionToken) {
            Log.e(TAG, "连接成功");
            Toast.makeText(MyApplication.Companion.getContext(), "连接成功", Toast.LENGTH_LONG).show();

            try {
                //订阅主题，参数：主题、服务质量
                mqttAndroidClient.subscribe(PUBLISH_TOPIC, 2);
            } catch (MqttException e) {
                e.printStackTrace();
            }

        }

        @Override
        public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
            exception.printStackTrace();
            Log.e(TAG, "连接失败");
            Toast.makeText(MyApplication.Companion.getContext(), "连接失败", Toast.LENGTH_LONG).show();
            //连接失败进行重连
            doClientConnection();
        }
    };
    /**
     * 订阅主题回调
     */
    private MqttCallback mqttCallback = new MqttCallback() {
        @Override
        public void connectionLost(Throwable cause) {
            Log.e(TAG, "连接断开 ");
            Toast.makeText(MyApplication.Companion.getContext(), "断开连接", Toast.LENGTH_LONG).show();
            doClientConnection();//连接断开，重连
        }

        @Override
        public void messageArrived(String topic, MqttMessage message) throws Exception {
            Log.e(TAG, "收到消息： " + new String(message.getPayload()));
            //收到消息，这里弹出Toast表示。如果需要更新UI，可以使用广播或者EventBus进行发送
            Toast.makeText(MyApplication.Companion.getContext(), "messageArrived: " + new String(message.getPayload()), Toast.LENGTH_LONG).show();
            //收到其他客户端的消息后，响应给对方告知消息已到达或者消息有问题等
            response("message arrived");
        }

        @Override
        public void deliveryComplete(IMqttDeliveryToken token) {

        }
    };
    /**
     * 响应 （收到其他客户端的消息后，响应给对方告知消息已到达或者消息有问题等）
     *
     * @param message 消息
     */
    public void response(String message) {
        String topic = RESPONSE_TOPIC;
        Integer qos = 2;
        Boolean retained = false;
        try {
            //参数分别为：主题、消息的字节数组、服务质量、是否在服务器保留断开连接后的最后一条消息
            mqttAndroidClient.publish(topic, message.getBytes(), qos.intValue(), retained.booleanValue());
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    /**
     * 发布 （模拟其他客户端发布消息）
     *
     * @param message 消息
     */
    public static void publish(String message) {
        String topic = PUBLISH_TOPIC;
        Integer qos = 2;
        Boolean retained = false;
        try {
            //参数分别为：主题、消息的字节数组、服务质量、是否在服务器保留断开连接后的最后一条消息
            mqttAndroidClient.publish(topic, message.getBytes(), qos.intValue(), retained.booleanValue());
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onDestroy() {
        try {
            mqttAndroidClient.disconnect(); //断开连接
        } catch (MqttException e) {
            e.printStackTrace();
        }
        super.onDestroy();
    }
}
