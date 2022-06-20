package com.skypan.bluetoothcontrol;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.SpannedString;
import android.text.style.AbsoluteSizeSpan;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Objects;
import java.util.UUID;


public class MainActivity extends Activity {
    private final static int REQUEST_CONNECT_DEVICE = 1;    //宏定义查询设备句柄
    private final static String MY_UUID = "00001101-0000-1000-8000-00805F9B34FB";   //SPP服务UUID号
    private BluetoothAdapter _bluetooth = BluetoothAdapter.getDefaultAdapter();    //获取本地蓝牙适配器
    private InputStream is;             //输入流，用来接收蓝牙数据
    private OutputStream os;            //输出流，用来发送蓝牙数据
    private EditText edit0;             //发送数据输入句柄
    private TextView tv_in;             //接收数据显示句柄
    private ScrollView sv;              //翻页句柄
    private String smsg = "";           //显示用数据缓存
    //boolean mode_flag = false ;         //自定义模式标识符
    private static int mode_flag = 0;
    //private int flag=0 ;
    BluetoothDevice _device = null;     //蓝牙设备
    BluetoothSocket _socket = null;     //蓝牙通信socket
    boolean bRun = true;
    boolean flag = true ;
    boolean bThread = false;            //布尔型 线程标签
    private static Toast toast;



    /***************************************在第一次创建活动时调用**************************************
     ***************************************在第一次创建活动时调用**************************************
     ***************************************在第一次创建活动时调用**************************************/
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);   //设置画面为主画面 main.xml
        edit0 = findViewById(R.id.Edit0);           // 得到输入框句柄

        SpannableString ss = new SpannableString(" 请输入命令......");//定义hint的值
        AbsoluteSizeSpan ass = new AbsoluteSizeSpan(15, true);//设置字体大小 true表示单位是sp
        ss.setSpan(ass, 0, ss.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        edit0 .setHint(new SpannedString(ss));
        sv = findViewById(R.id.ScrollView01);  //得到翻页句柄
        tv_in = findViewById(R.id.in);      //得到数据显示句柄
    }



    /******************************重写 onTouchEvent***************************/
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        closeKeyBoard();
        return super.onTouchEvent(event);
    }

    public void closeKeyBoard() {
        if (getCurrentFocus() != null && getCurrentFocus().getWindowToken() != null) {
            View v = getCurrentFocus();
            closeSoftInput(this, v);
        }
    }
    /******************************关闭键盘输入法*********************************/
    public static void closeSoftInput(Context context, View v) {
        if (v != null) {
            InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
        }
    }



    /**************************解决Toast重复弹出 长时间不消失的问题*************************/

    @SuppressLint("ShowToast")
    public static void showToast(Context context, String message){
        if (toast==null){
            toast = Toast.makeText(context,message,Toast.LENGTH_SHORT);
        }else {
            toast.setText(message);
        }
        toast.setGravity(Gravity.BOTTOM,0,457);
        toast.show();           //设置新的消息提示
    }


    /**************************************发送按键响应**************************************/
    public void onSendButtonClicked(View v){
        int i;
        int n=0;
        if(_socket==null){
            showToast(this,"请先连接蓝牙设备");
            return;
        }

        if(edit0.getText().length()==0){
            showToast(this,"请输入控制指令");
            return;
        }

        if(mode_flag!=1) {
            showToast(this,"请开启遥控模式");
        }
        else {
            try {         //蓝牙连接输出流 OutputStream os
                os = _socket.getOutputStream();
                byte[] bos = edit0.getText().toString().getBytes("GB2312");

                for (i = 0; i < bos.length; i++) {
                    if (bos[i] == 0x0a) n++;
                }
                byte[] bos_new = new byte[bos.length + n];
                n = 0;
                for (i = 0; i < bos.length; i++) { //手机中换行为0a,将其改为0d 0a后再发送
                    if (bos[i] == 0x0a) {
                        bos_new[n] = 0x0d;
                        n++;
                        bos_new[n] = 0x0a;
                    } else {
                        bos_new[n] = bos[i];
                    }
                    n++;
                }
                os.write(bos_new);
/*              if(bos_new.equals("0"))
                    showToast(this, "发送成功");*/
            } catch (IOException ignored) {
            }
        }
    }

    /***************接收活动结果，响应startActivityForResult()***************/
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CONNECT_DEVICE) {     //连接结果，由DeviceListActivity设置返回
            // 响应返回结果
            if (resultCode == Activity.RESULT_OK) {     //连接成功，由DeviceListActivity设置返回
                // MAC地址，由DeviceListActivity设置返回
                String address = data.getExtras().getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
                // 得到蓝牙设备句柄
                _device = _bluetooth.getRemoteDevice(address);

                // 用服务号得到socket
                try {
                    _socket = _device.createRfcommSocketToServiceRecord(UUID.fromString(MY_UUID));
                } catch (IOException e) {
                    showToast(this, "连接失败");
                }
                //连接socket
                ImageButton btn = findViewById(R.id.BtnConnect);
                try {
                    _socket.connect();
                    showToast(this, "连接" + _device.getName() + "成功");
                    // 已连接  Button显示断开图标
                    //btn.setBackgroundResource(R.drawable.cancel);
                    btn.setImageResource(R.drawable.cancel);
                } catch (IOException e) {
                    try {
                        showToast(this, "连接失败");
                        _socket.close();
                        _socket = null;
                    } catch (IOException ee) {
                        showToast(this, "连接失败");
                    }
                    return;
                }
                //打开接收线程
                try {
                    is = _socket.getInputStream();   //得到蓝牙数据输入流 InputStream is
                } catch (IOException e) {
                    showToast(this, "接收数据失败");
                    return;
                }
                if (!bThread) {
                    initListenerThread();
                    bThread = true;
                    bRun = true;
                } else {
                    bRun = true;
                }
            }
        }
    }


    /************************************接收数据线程************************************/
    Thread mListenerThread;
    private void initListenerThread(){
        mListenerThread = new Thread(new Runnable() {
            @Override
            public void run() {
                byte[] bytes = new byte[1024];
                byte[] dataByte = new byte[2048];
                String str;
                while (bRun){
                    if (is != null){
                        try {
                            if (is.available()!=0){
                                do {
                                    is.read(bytes);
                                    dataByte = addByteArray(dataByte, bytes);
                                    clearArray(bytes);
                                    Thread.sleep(100);//短时间内没数据才退出
                                } while (is.available() != 0);
                                str = new String(dataByte, 0, dataByte.length, "GB2312");//GB2312编码
                                smsg+=str;
                                handler.sendMessage(handler.obtainMessage());
                                bytes[0] = 0;
                                dataByte[0] = 0;
                            }
                        }catch (Exception e){ e.printStackTrace();
                        }
                    }
                }
            }
        });
        mListenerThread.start();
    }

    //消息处理队列
    Handler handler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage( Message msg) {
            tv_in.setText(smsg);   //显示数据
            sv.scrollTo(0,tv_in.getMeasuredHeight()); //跳至数据最后一页
            return false;
        }
    });

    //关闭程序 调用处理部分
    public void onDestroy(){ super.onDestroy();
        if(_socket!=null)  //关闭连接socket
            try{
                _socket.close();
                mListenerThread.interrupt();
                mListenerThread = null;
            }catch(Exception ignored){}
        //	_bluetooth.disable();  //关闭蓝牙服务
    }

     /************************************连接 按键响应函数********************************************/
    public void onConnectButtonClicked(View v){
        _bluetooth.enable();

        //如果蓝牙服务不可用则提示
        if(!_bluetooth.isEnabled()){
            showToast(this,"请开启蓝牙");
            return;
        }
        //  GPS权限提示
        if(!isOpenGPS(this)){
            AlertDialog.Builder builder = new AlertDialog.Builder(this,AlertDialog.THEME_DEVICE_DEFAULT_LIGHT);
            builder.setTitle("提示")
                    .setMessage("请打开手机定位权限！")
                    //.setCancelable(false)       //默认不可关闭
                    .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                            startActivityForResult(intent, 10);
                        }
                    }).show();
            return;
        }
        //如未连接设备则打开DeviceListActivity进行设备搜索
        ImageButton btn = findViewById(R.id.BtnConnect);
        if(_socket==null){
            Intent serverIntent = new Intent(this, DeviceListActivity.class); //跳转程序设置
            startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);  //设置返回宏定义
        }
        //若已连接, 点击此按键 则关闭socket连接
        else{
            try{
                os = _socket.getOutputStream();
                String str = "A";                //回到自动模式
                os.write(str.getBytes("GB2312"));

                bRun = false;
                mListenerThread.interrupt();
                mListenerThread = null;
                Thread.sleep(1000);
                is.close();
                _socket.close();
                _socket = null;
                bThread = false;
                showToast(this,"蓝牙连接已断开");
                //btn.setBackgroundResource(R.drawable.bluetooth);           //Button更换图标
                btn.setImageResource(R.drawable.bluetooth);                  //Button更换图标
            }catch(IOException ignored){}
            catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    /*********************************清除按键 响应函数********************************/
    public void onClearButtonClicked(View v){
        if(Objects.equals(smsg, ""))
        {
            showToast(this,"没有接收数据");
        }
        else{
            smsg="";
            tv_in.setText(smsg);
            showToast(this,"接收数据已清空");
        }
    }

    /*********************************按键控制打开窗帘*******************************/

    public void turn_up(View view) throws IOException {
        if(_socket==null ){
            showToast(this,"请先连接蓝牙设备");
        }
        else{
            if(mode_flag!=1) {
                showToast(this,"请开启遥控模式");
            }
            else {
                showToast(this,"正在打开窗帘");
                os = _socket.getOutputStream();
                String str = "O";
                os.write(str.getBytes("GB2312"));
            }
        }
    }

    /*********************************按键控制关闭窗帘*******************************/
    public void turn_down(View view) throws IOException {

        if(_socket==null ){
            showToast(this,"请先连接蓝牙设备");
        }
        else{
            if(mode_flag!=1) {
                showToast(this,"请开启遥控模式");
            }
            else {
                showToast(this,"正在关闭窗帘");
                os = _socket.getOutputStream();
                String str = "C";
                os.write(str.getBytes("GB2312"));
            }
        }
    }

    /*******************************按键控制模式更改*********************************/
    public void open(View view) throws IOException{
        if(_socket==null ){
            showToast(this,"请先连接蓝牙设备");
        }
        else{
            mode_flag++;
            if(mode_flag==4)
                mode_flag=1;
            if(mode_flag==1) {
                showToast(this,"遥控模式已开启");
                os = _socket.getOutputStream();
                String str = "M";
                os.write(str.getBytes("GB2312"));
            }
            if(mode_flag==2) {
                showToast(this,"定时模式已开启");
                os = _socket.getOutputStream();
                String str = "T";
                os.write(str.getBytes("GB2312"));
            }
            if(mode_flag==3) {
                showToast(this,"自动模式已开启");
                os = _socket.getOutputStream();
                String str = "A";
                os.write(str.getBytes("GB2312"));
            }
        }
    }


    /******************************按键控制停止窗帘动作*********************************/
    public void pause(View view) throws IOException {
        if(_socket==null){
            showToast(this,"请先连接蓝牙设备");
        }
        else{
            if(mode_flag!=1) {
                showToast(this,"请开启遥控模式");
            }
            else {
                showToast(this,"停止");
                os = _socket.getOutputStream();
                String str = "S";
                os.write(str.getBytes("GB2312"));
            }
        }
    }

    /******************************退出按键响应函数******************************/
    public void onQuitButtonClicked(View v){

        //---安全关闭蓝牙连接再退出，避免报异常----//
        if(_socket!=null){
            //关闭连接socket
            try{
                os = _socket.getOutputStream();
                String str = "A";
                os.write(str.getBytes("GB2312"));
                bRun = false;
                Thread.sleep(1000);
                is.close();
                _socket.close();
                _socket = null;
            }catch(IOException e){}
            catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        finish();
    }


    private byte[] clearArray(byte[] as){
        int i;
        for ( i=0;i<as.length;i++){
            as[i] = 0;
        }
        return as;
    }

    private byte[] addByteArray(byte[] byte_1, byte[] byte_2){
        byte[] byte_3 = new byte[byte_1.length];
        int number = 0;
        for (byte b : byte_1) {
            if (b==0){
                break;
            }
            ++number;
        }
        System.arraycopy(byte_1, 0, byte_3, 0, byte_1.length);
        System.arraycopy(byte_2, 0, byte_3, number, byte_2.length);
        return byte_3;
    }


    /***************判断GPS是否开启，GPS或者AGPS开启一个就认为是开启的***************/
    public static boolean isOpenGPS(final Context context) {
        LocationManager locationManager
                = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        // GPS定位
        boolean gps = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        // 网络服务定位
        boolean network = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        return gps || network;
    }

    /***************主界面 EditText 旁边的设置键***************/
    public void onSetEditText(View view) {
        if(flag) {
            edit0.setVisibility(View.VISIBLE);
        }
        else {
            edit0.setVisibility(View.INVISIBLE);
        }
        flag = !flag;
    }
}