package com.example.administrator.remotecamera;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.support.v7.app.AppCompatActivity;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;


@SuppressLint("NewApi")
public class MainActivity extends AppCompatActivity {

    //定义变量
    private TextView wifiInfoText;
    private TextView myNum;

    private TextView led01stateText;
    private TextView led02stateText;
    private TextView led03stateText;

    private TextView iotDateText;

    private ImageButton goButton;
    private ImageButton quitButton;

    //imagebtn
    private ImageButton led01Button;  //灯1按键
    private ImageButton led02Button;  //灯2按键
    private ImageButton led03Button;  //灯3按键

    long my_num = 0;					//执行次数记时间
    long getDataCount = 0;
    long getErrCon = 0;
    String LEDCTRL;

    //定义线程
    private Thread myThread;
    private Thread downThread;

    private EditText displayText = null;

    //标志位
    private boolean ledChange1 = false;   		 //判断灯1是否变化
    private boolean ledState1 = false;           //灯1的状态
    private boolean ledChange2 = false;   		 //判断灯2是否变化
    private boolean ledState2 = false;           //灯2的状态
    private boolean ledChange3 = false;   	     //判断灯3是否变化
    private boolean ledState3 = false;           //灯3的状态

    private boolean StartOrStop = false;         //启动监听标志位

    //显示图片
    private ImageView image;                     //not used
    private int picNum = 0;

    String fileName = Environment.getExternalStorageDirectory() + "/"
            + "qq2357481431.txt";	//存放文件路径

    public final static int TIMEOUT_CONNETCT = 500;
    public final static int TIMEOUT_READ = 300;
    private final int UPDATE_UI = 1;

    private final String TAG = "WifiSoftAP";
    public WifiManager wifiManager;
    public static final String WIFI_AP_STATE_CHANGED_ACTION = "android.net.wifi.WIFI_AP_STATE_CHANGED";

    public static final int WIFI_AP_STATE_DISABLING = 0;
    public static final int WIFI_AP_STATE_DISABLED = 1;
    public static final int WIFI_AP_STATE_ENABLING = 2;
    public static final int WIFI_AP_STATE_ENABLED = 3;
    public static final int WIFI_AP_STATE_FAILED = 4;

    public SocketAddress sourceAddr = null;
    Socket mySocket = null;
    private InetSocketAddress mySocketAddr;
    private int picSize = 0;


    private int mode = 1; // 模式选择 0：STA, 1：AP
    private boolean debug = false; // 是否调试输出

    /*handle队列, UI用*/
    Handler myHandler = new Handler() {
        public void handleMessage(Message msg) { // 处理消息
            switch (msg.what) {
                case UPDATE_UI: {
                    break;
                }
                default: {
                    break;
                }
            }
            super.handleMessage(msg);
        }

    };


    /**
     * onCreate
     * @param savedInstanceState
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        //引用并初始化控件
        initUI();

        if (mode == 0) { // 路由器模式
            if (!isApEnabled()) {
                setWifiApEnabled(true); // 打开热点
            }
            if (myThread == null) {
                myThread = new Thread(udpRunnable);
                myThread.start();
            }
        } else { 		// AP模式
            if (myThread == null) {
                myThread = new Thread(downloadRunnable);
                myThread.start();
            }
        }

        Toast toast = Toast.makeText(MainActivity.this, "Start up...",
                Toast.LENGTH_SHORT);
        toast.show();

        goButton.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                StartOrStop = !StartOrStop;
                if (StartOrStop == true) {	 			//开始获取数据时候为停止图标
                    goButton.setImageResource(R.drawable.stop);
                } else {
                    goButton.setImageResource(R.drawable.go1);
                }
            }
        });

        led01Button.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                ledChange1 = true;									//按键变化了 灯1
                ledState1 = !ledState1;
                if (ledState1 == true) {
                    led01Button.setImageResource(R.drawable.ledoff);
                } else {
                    led01Button.setImageResource(R.drawable.ledon);
                }
            }
        });

        led02Button.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                ledChange2 = true;									//按键变化了 灯2
                ledState2 = !ledState2;
                if (ledState2 == true) {
                    led02Button.setImageResource(R.drawable.ledoff);
                } else {
                    led02Button.setImageResource(R.drawable.ledon);
                }
            }
        });

        led03Button.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                ledChange3 = true;									//按键变化了 灯3
                ledState3 = !ledState3;
                if (ledState3 == true) {
                    led03Button.setImageResource(R.drawable.ledoff);
                } else {
                    led03Button.setImageResource(R.drawable.ledon);
                }
            }
        });

        quitButton.setOnClickListener(new Button.OnClickListener() {	//退出按键
            public void onClick(View v) {
                dialog();
            }
        });

    }

    /**
     * 引用并初始化控件
     */
    public void initUI(){
        wifiInfoText = findViewById(R.id.wifiInfoText);			//顶部文字信息
        myNum = findViewById(R.id.numCountText);                  //底部计数(收发字节)

        led01stateText = findViewById(R.id.led01stateText);	//灯1的文字提示
        led02stateText = findViewById(R.id.led02stateText);	//灯2的文字提示
        led03stateText = findViewById(R.id.led03stateText);	//灯3的文字提示

        iotDateText = findViewById(R.id.iotDateText);   //温湿度显示

        image = findViewById(R.id.imageView);               //显示图片用
        goButton =  findViewById(R.id.goButton);			//监听启动按键

        led01Button =  findViewById(R.id.led01Button);		//灯1按键
        led02Button =  findViewById(R.id.led02Button);  	//灯2按键
        led03Button = findViewById(R.id.led03Button);	    //灯3按键
        quitButton =  findViewById(R.id.quitButton);		//退出按键
        displayText = findViewById(R.id.logInfoText);         //显示日志信息

        //显示后台打印信息
        displayText.setMovementMethod(ScrollingMovementMethod.getInstance());
        displayText.setSelection(displayText.getText().length(), displayText.getText().length());
        displayText.getText().append("务必先将手机WIFI连接到板子热点WIFIBOARD,然后点击右下三角箭头连接到板子服务器，IP地址和PORT端口在eclipse里面修改");
        displayText.setEnabled(false);

        //显示连接提示
        wifiInfoText.setText("CSIC TCPIP PROTOCOL OF WIFI\r\nConnect to board server ip:192.168.1.8  port:1001\r\n");
        wifiInfoText.setTextColor(0xffff0000);	//显示提示信息
        //show.append(fileName);

        //温湿度的显示
        myNum.setTextColor(0xFFFFFFFF);
        iotDateText.setTextColor(0xFFFFFFFF);
        iotDateText.setText("\r\n创思通信\r\n");
        iotDateText.setBackgroundColor(android.graphics.Color.BLACK);
        iotDateText.getBackground().setAlpha(70);
        iotDateText.setGravity(Gravity.CENTER);
        iotDateText.setVisibility(View.GONE);

    }


    /**
     * 设置AP
     * @param enabled
     * @return
     */
    public boolean setWifiApEnabled(boolean enabled) {
        if (enabled) {
            wifiManager.setWifiEnabled(false);
        }
        try {
            WifiConfiguration apConfig = new WifiConfiguration();

            apConfig.allowedAuthAlgorithms.clear();
            apConfig.allowedGroupCiphers.clear();
            apConfig.allowedKeyManagement.clear();
            apConfig.allowedPairwiseCiphers.clear();
            apConfig.allowedProtocols.clear();

            apConfig.SSID = "WIFIBOARD"; // 手机热点的名字
            apConfig.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);

            Method method = wifiManager.getClass().getMethod(
                    "setWifiApEnabled", WifiConfiguration.class, Boolean.TYPE);

            return (Boolean) method.invoke(wifiManager, apConfig, enabled);
        } catch (Exception e) {
            Log.e(TAG, "Cannot set WiFi AP state", e);
            return false;
        }
    }

    /**
     * 获取WiFi信息
     * @return
     */
    public int getWifiApState() {
        try {
            Method method = wifiManager.getClass().getMethod("getWifiApState");
            return (Integer) method.invoke(wifiManager);
        } catch (Exception e) {
            Log.e(TAG, "Cannot get WiFi AP state", e);
            return WIFI_AP_STATE_FAILED;
        }
    }

    /**
     * AP状态
     * @return
     */
    public boolean isApEnabled() {
        int state = getWifiApState();
        return WIFI_AP_STATE_ENABLING == state
                || WIFI_AP_STATE_ENABLED == state;
    }

    /**
     * 启动UDP服务
     */
    public void udpServer() {
        int tmp = 0;
        if (debug) {
            Log.v("system", "enter udpServer");
        }
        try {
            DatagramSocket serSocket = new DatagramSocket(8080);
            byte data[] = new byte[1024];
            DatagramPacket pack = new DatagramPacket(data, data.length);
            try {
                do {
                    serSocket.receive(pack);
                    sourceAddr = pack.getSocketAddress();
                    tmp = pack.getLength();
                    String recvData = new String(data, 0, tmp);
                    Log.v("system", "received(" + sourceAddr + "): " + recvData);
                    if (downThread == null) {
                        downThread = new Thread(downloadRunnable);
                        downThread.start();
                    }
                } while (tmp > 0);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (SocketException e) {
            e.printStackTrace();
        }
    }

    /**
     * 启动TCP服务(Client)
     * @return
     */
    private int loadTCPIP() {

        if (mySocket == null || mySocket.isClosed()) { // 创建套接字

            displayText.getText().append("#newsocket#");
            mySocket = new Socket();
            if (sourceAddr != null) {
                displayText.getText().append("#infra#");
                mySocketAddr = new InetSocketAddress( 	// STA
                        ((InetSocketAddress) sourceAddr).getAddress()
                                .getHostAddress(), 5000);
            } else {
                mySocketAddr = new InetSocketAddress("192.168.1.8", 1001); // /AP

            }

            try { // 连接
                mySocket.connect(mySocketAddr, TIMEOUT_CONNETCT);
            } catch (IOException e2) {
                getErrCon++;
                if(getErrCon > 20)			//连接错误次数超过20次
                {
                    Toast toast1 = Toast.makeText(MainActivity.this, "请连WIFI无线接到板子，并且开启好板子为服务器模式，否则无法正常通过TCPIP连接到板子",
                            Toast.LENGTH_LONG);
                    toast1.show();
                }
                try {
                    mySocket.close();
                    return 0;
                } catch (IOException e) {
                    e.printStackTrace();
                }
                e2.printStackTrace();
            }

        }
        getErrCon = 0;  					//已经连接成功，错误归零
        OutputStream outputstream = null; 	// 打开输出流
        try {
            outputstream = mySocket.getOutputStream();
        } catch (IOException e) {
            Log.e("TCP", "! getOutputStream");
            e.printStackTrace();
        }

        if(getDataCount<5) 	//发送5条的开关灯命令
        {

            if(ledState1 == true)
            {
                LEDCTRL="io_ctrl=CLOSELED1#";
            }
            else
            {
                LEDCTRL="io_ctrl=OPENLED1#";
            }



            if(ledState2 == true)
            {
                LEDCTRL+="=CLOSELED2#";
            }
            else
            {
                LEDCTRL+="=OPENLED2#";
            }


            if(ledState3 == true)
            {
                LEDCTRL+="=CLOSELED3#";
            }
            else
            {
                LEDCTRL+="=OPENLED3#";
            }

            try {
                outputstream.write(LEDCTRL.getBytes()); // 发送命令：开关灯
                displayText.getText().append(LEDCTRL);	//手机APP显示信息
                LEDCTRL="NULL";
                return 0;
            } catch (IOException e) {
                Log.e("TCP", "outputstream.write");
                try {
                    mySocket.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
                e.printStackTrace();
            }
        }

        if(getDataCount==5)
        {
            try {
                outputstream.write("=csic.taobao.com#".getBytes()); // 发送命令：发送淘宝广告
                displayText.setText("=csic.taobao.com#");	//手机APP显示信息
                return 0;
            } catch (IOException e) {
                Log.e("TCP", "outputstream.write");
                try {
                    mySocket.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
                e.printStackTrace();
            }
        }

        if(getDataCount==6)		//获取数据
        {
            getDataCount = 0;
            try {
                outputstream.write("GETDATA\n".getBytes()); // 发送需要获取板子信息的命令
                //displayText.setText("send command to get data from board\n");	//显示发送需要获取命令
            } catch (IOException e) {
                Log.e("TCP", "outputstream.write");
                try {
                    mySocket.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
                e.printStackTrace();
            }


            InputStream inputstream = null;		//TCP输入流
            try {
                inputstream = mySocket.getInputStream();
            } catch (IOException e) {
                Log.v("TCP", "! getInputStream");
                e.printStackTrace();
            }

            try { 								// 设置接收超时
                mySocket.setSoTimeout(TIMEOUT_READ);
            } catch (SocketException e1) {
                Log.v("TCP", "! setSoTimeout");
                e1.printStackTrace();
            }

            byte bufferSize[] = new byte[50];

            try {
                int tmp0 = inputstream.read(bufferSize);
                if (tmp0 > 0)	//如果获取到了数据
                {
                    String Msg = new String(bufferSize, 0, tmp0, "gb2312");
                    displayText.setText("GETDATA:");	//显示获取到的数据
                    displayText.getText().append(Msg);

                    String spStr[] = Msg.split("#");
                }
                else
                {
                    displayText.getText().append("#nodata#");
                    return 0;
                }
            } 	catch (IOException e2)	{
                Log.e("TCP", "inputStream_read  time_out");
                e2.printStackTrace();
                return 0;
            }
        }
        return 1;

    }



    /**
     * 获取pic线程
     */
    Runnable downloadRunnable = new Runnable() {
        public void run() {
            myHandler.postDelayed(this, 1000); // 间隔一段时间，再请求下一帧
            if(StartOrStop)	//开启了监听按钮
            {
                int sucess = loadTCPIP();
                if (sucess == 1) {
                    if (debug) {
                        Log.v("TCP", "load success");
                    }
                    //myHandler.obtainMessage(UPDATE_UI).sendToTarget(); // 发送更新ui的消息
                }

                my_num++;
                if(my_num > 10000)
                    my_num = 0;
                myNum.setText(Long.toString(my_num));
                myNum.append(":");

                if(displayText.getText().length()>60)
                {
                    displayText.getText().clear();
                }

                KeyguardManager mKeyguardManager = (KeyguardManager) getSystemService(KEYGUARD_SERVICE);
                if (mKeyguardManager.inKeyguardRestrictedInputMode()) {	//检测到锁频，不刷新
                    // keyguard on
                    getDataCount=0;
                }

                PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
                if( !pm.isScreenOn() )  									//检测到屏幕关闭，不刷新
                {
                    getDataCount=0;
                }

                getDataCount++;
                myNum.append(Long.toString(getDataCount));
            }
            else
            {
                if(mySocket!=null && mySocket.isConnected())
                {
                    try {
                        mySocket.close();
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }
                }

            }
        }
    };

    /**
     * UDP广播
     */
    Runnable udpRunnable = new Runnable() {
        public void run() {
            udpServer();
        }
    };


    /**
     * 退出对话框
     */
    public void dialog() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(
                MainActivity.this);
        builder.setMessage("亲,真的要退出吗？")
                .setPositiveButton("退出", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface arg0, int arg1) {
                        if (isApEnabled()) {
                            setWifiApEnabled(false);
                        }
                        System.exit(0);
                    }
                })
                .setNegativeButton("不是", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface arg0, int arg1) {
                    }
                });
        AlertDialog ad = builder.create();
        ad.show();
    }


    /**
     * KeyBack
     * @param keyCode
     * @param event
     * @return
     */
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) { // 返回按键
        if (keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0) {
            dialog();
        }
        return super.onKeyDown(keyCode, event);
    }

    /**
     * 创建menu,再加功能
     * @param menu
     * @return
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_main, menu);
        return true;
    }


}



