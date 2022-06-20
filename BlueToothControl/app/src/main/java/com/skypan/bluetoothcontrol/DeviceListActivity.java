package com.skypan.bluetoothcontrol;
import android.app.Activity;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.List;


public class DeviceListActivity extends Activity {

    public static String EXTRA_DEVICE_ADDRESS = "设备地址";     //存放设备地址的String
    private BluetoothAdapter mBtAdapter;
    private List<String> mExist = new ArrayList<>();            //动态数组
    private ArrayAdapter<String> mPairedDevicesArrayAdapter;    //已配对列表
    private ArrayAdapter<String> mNewDevicesArrayAdapter;       //新设备列表
    /*private ProgressDialog progressDialog;              //ProgressDialog是圆圈进度动画Dialog*/
    private static final long SCAN_TIME = 8000;       // 自定义蓝牙扫描时间scanning time
    private static final long POINT_TIME = 2000;       // 自定义提示时间


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);    //创建并显示窗口
        setContentView(R.layout.activity_device_list);                  //找到Activity
        DeviceListActivity.this.setFinishOnTouchOutside(false);         //点击空白处，窗体不自动finish
        setResult(Activity.RESULT_CANCELED);                            //设定默认返回值为取消

        // 初使化设备存储数组
        mPairedDevicesArrayAdapter = new ArrayAdapter<>(this, R.layout.device_name);
        mNewDevicesArrayAdapter = new ArrayAdapter<>(this, R.layout.device_name);

        // 得到本地蓝牙句柄
        mBtAdapter = BluetoothAdapter.getDefaultAdapter();
        ListView pairedListView = findViewById(R.id.paired_devices);    //找到 显示已配对设备 的 ListView控件
        pairedListView.setAdapter(mPairedDevicesArrayAdapter);
        pairedListView.setOnItemClickListener(mDeviceClickListener);      // 设置已配队设备列表 添加点击事件

        // 查找 按钮 响应事件
        ImageButton scanButton = findViewById(R.id.button_scan);
        scanButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                againDoDiscovery();                    //开始服务和设备查找 函数
                ListView newDevicesListView = findViewById(R.id.new_devices);    //找到 显示新设备 的 ListView控件
                newDevicesListView.setAdapter(mNewDevicesArrayAdapter);
                newDevicesListView.setOnItemClickListener(mDeviceClickListener);     // 设置新设备列表 添加点击事件
            }
        });

        this.registerReceiver(mReceiver, new IntentFilter(BluetoothDevice.ACTION_FOUND));   // 注册接收查找到设备action接收器
        this.registerReceiver(mReceiver, new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)); // 注册查找结束action接收器
        this.registerReceiver(mReceiver, new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_STARTED));
    }


    /**********销毁发现 函数**********/
    @Override
    protected void onDestroy() { super.onDestroy();
        // 关闭服务查找
        if (mBtAdapter != null) {
            mBtAdapter.cancelDiscovery();
        }
        // 注销action接收器
        this.unregisterReceiver(mReceiver);
    }

    /************开始服务和设备查找  againDoDiscovery函数**********/
    private void againDoDiscovery() {
        //setProgressBarIndeterminateVisibility(true);     //label显示转圈圈样式
        //progressDialog = ProgressDialog.show(DeviceListActivity.this,"查找蓝牙设备中...", "请等待...", true, false);
        //progressDialog.setCanceledOnTouchOutside(true);     //显示圆圈动画Dialog,设置点击对话框外取消弹窗true，无效false
        //setTitle("查找蓝牙设备中");                           //设置标题栏显示文字
        TextView textView  =  findViewById(R.id.blue_status);       //找到blue_status控件，自定义显示文字
        textView.setText("查找蓝牙设备中");
        findViewById(R.id.myProgressBar).setVisibility(View.VISIBLE);        // 找到圆圈控件 并显示可见
        findViewById(R.id.new_devices).setVisibility(View.VISIBLE);          // 显示其它设备（未配对设备）列表

       /* // 再次点击 则 关闭正在进行的查找服务
        if (mBtAdapter.isDiscovering()) {
            mBtAdapter.cancelDiscovery();
        }*/
        mBtAdapter.startDiscovery();    // 开始扫描.并在预定义的时间后 停止扫描

       new Handler().postDelayed(new Runnable(){
            @Override
            public void run() {
                mBtAdapter.cancelDiscovery(); }
            },SCAN_TIME);
    }

    /***********************点击扫描到的设备 设置响应函数**********************/
    private OnItemClickListener mDeviceClickListener = new OnItemClickListener() {
        public void onItemClick(AdapterView<?> av, View v, int arg2, long arg3) {
            mBtAdapter.cancelDiscovery(); // 点击后准备连接设备，并关闭服务查找

            String info = ((TextView) v).getText().toString();
            String address = info.substring(info.length() - 17);    // 得到mac地址

            Intent intent = new Intent();
            intent.putExtra(EXTRA_DEVICE_ADDRESS, address);         // 设置返回数据

            setResult(Activity.RESULT_OK, intent);                   // 设置返回值并结束程序
            finish();
        }
    };

    /*****************查找到设备和搜索完成action监听器*********************/
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
                // 查找到设备action  得到蓝牙设备
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                // 如果是已配对的则略过，已得到显示，其余的在添加到列表中进行显示
                if (device.getBondState() != BluetoothDevice.BOND_BONDED) {
                    mNewDevicesArrayAdapter.add(device.getName() + "\n" + device.getAddress());
                }
                else{  //添加到已配对设备列表
                    if(!mExist.contains(device.getAddress()))// 防止重复添加
                    {
                        mExist.add(device.getAddress());
                        mPairedDevicesArrayAdapter.add(device.getName() + "\n" + device.getAddress());
                    }
                }
                //当查找结束 ACTION_DISCOVERY_FINISHED
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                //setTitle("选取蓝牙设备");       //标题栏文字
                //setProgressBarIndeterminateVisibility(false);     //标题栏处的转圈圈显示关闭
                // progressDialog.dismiss(); //转圈圈弹窗关闭

                // View.INVISIBLE 转圈圈控件不可见，但这个View仍然会占用在xml文件中所分配的布局空间，不重新layout
                // View.GONE  不可见，但这个View不再占用位置，会重新layout，后面的view就会取代他的位置
                findViewById(R.id.myProgressBar).setVisibility(View.INVISIBLE);
                TextView textView  =  findViewById(R.id.blue_status);       //找到blue_status控件并改变文字
                textView.setText("请选择蓝牙设备");
                //如果没有找到蓝牙设备
                if (mNewDevicesArrayAdapter.getCount() == 0 && mPairedDevicesArrayAdapter.getCount()==0 ) {
                   // String noDevices = "未找到到蓝牙设备！";
                   // mNewDevicesArrayAdapter.add(noDevices);
                    showToast();
                    new Handler().postDelayed(new Runnable(){
                        @Override
                        public void run() {
                            finish();
                        }},POINT_TIME);         //在指定的时间关闭Activity
                }
            }
        }
    };

    /********* 自定义Toast函数 ********/
    void showToast(){
        Toast.makeText(DeviceListActivity.this, "未找到到蓝牙设备！", Toast.LENGTH_SHORT).show();
    }


    /********* 点击退出按钮 销毁Activity ********/
    public void OnCancel(View v){
        finish();
    }

}
