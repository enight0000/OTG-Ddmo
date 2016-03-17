package com.x.otg_ddmo;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.tencent.bugly.crashreport.BuglyLog;
import com.tencent.bugly.crashreport.CrashReport;
import com.x.otg_ddmo.chooser.FileActivity;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;

public class MainActivity extends Activity{
    private static final String TAG = "MainActivity";   //记录标识
    private Button btfind, btsend, btclean, btfile;      //按钮
    private EditText edit;
    // private ArrayList<String> recList = new ArrayList<>();
    private BaseStringAdapter adapter;
    private ListView lsv1;         //显示USB信息的
    private TextView text, info;
    private UsbDeviceConnection mDeviceConnection;
    private UsbManager manager;   //USB管理器
    private UsbInterface mInterface;
    private boolean threadRun = true;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        CrashReport.initCrashReport(getApplicationContext(), "900021743", true);

        btfind = (Button) findViewById(R.id.button);
        this.btsend = (Button) findViewById(R.id.button2);
        btclean = (Button) findViewById(R.id.button3);
        btfile = (Button) findViewById(R.id.button4);
        edit = (EditText) findViewById(R.id.editText);
        lsv1 = (ListView) findViewById(R.id.listView);
        text = (TextView) findViewById(R.id.deviceText);
        info = (TextView) findViewById(R.id.textView2);
        adapter = new BaseStringAdapter(this);
        lsv1.setAdapter(adapter);
        btfind.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                deviceFind();
            }
        });
        btsend.setOnClickListener(this.btsendListener);
        btclean.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                adapter.clear();
                info.setText("接收信息列表已清理");
            }
        });
        btfile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setClass(MainActivity.this, FileActivity.class);
                startActivityForResult(intent, FileActivity.RESULTCODE);
            }
        });
        manager = (UsbManager) getSystemService(Context.USB_SERVICE);
        BuglyLog.i(TAG, "启动成功！");
        //deviceFind();

    }
    private Thread waitThread = null;
    private Thread fileThread = null;
    @Override
    protected void onResume() {
        BuglyLog.i(TAG, "onResume");
        super.onResume();
        this.threadRun = true;
        if(waitThread==null || !waitThread.isAlive()) {
            waitThread = new Thread(ListenerDeviceRunnable);
            waitThread.start();
        }
    }
    @Override
    protected void onPause(){
        this.threadRun = false;
        super.onPause();
    }
    @Override
    protected void onStop(){
        BuglyLog.i(TAG,"onStop");
        this.threadRun = false;
        super.onStop();
    }

    private UsbDevice mUsbDevice;  //找到的USB设备

    public void deviceFind() {
        // 获取USB设备

        if (manager == null) {
            return;
        } else {
            info.setText("usb设备：" + String.valueOf(manager.toString()));
        }
        HashMap<String, UsbDevice> deviceList = manager.getDeviceList();
        if (deviceList.size() == 0) {
            mUsbDevice = null;
        }
        //BuglyLog.i(TAG, "usb设备：" + String.valueOf(deviceList.size()));
        Iterator<UsbDevice> deviceIterator = deviceList.values().iterator();
        while (deviceIterator.hasNext()) {
            mUsbDevice = deviceIterator.next();
            //Toast.makeText(MainActivity.this, "找到设备"+mUsbDevice.getDeviceName(), Toast.LENGTH_SHORT).show();
        }
        if (mUsbDevice != null) {
            text.setText("连接设备：" + mUsbDevice.getDeviceId());
            btsend.setEnabled(true);
            btfile.setEnabled(true);
            sendPrepare();
        } else {
            text.setText("无连接设备");
            info.setText("");
            btsend.setEnabled(false);
            btfile.setEnabled(true);

        }
    }

    private void sendPrepare() {
        info.setText("查找接口中....");
        for (int i = 0; i < mUsbDevice.getInterfaceCount(); ) {
            UsbInterface intf = mUsbDevice.getInterface(i);
            BuglyLog.d(TAG, i + " " + intf);
            mInterface = intf;
            break;
        }
        if (mInterface != null) {
            info.setText("发现接口:" + mInterface.getId());
            UsbDeviceConnection connection = null;
            // 判断是否有权限
            if (manager.hasPermission(mUsbDevice)) {
                info.setText("已获取连接授权");
                // 打开设备，获取 UsbDeviceConnection 对象，连接设备，用于后面的通讯
                connection = manager.openDevice(mUsbDevice);
                if (connection == null) {
                    info.setText("连接建立失败：connection == null");
                    return;
                }
                if (connection.claimInterface(mInterface, true)) {
                    info.setText("连接建立成功：" + connection.getSerial());
                    mDeviceConnection = connection;
                    //用UsbDeviceConnection 与 UsbInterface 进行端点设置和通讯
                    getEndpoint(mDeviceConnection, mInterface);
                } else {
                    info.setText("连接建立失败：非原设备！");
                    connection.close();
                }
            } else {
                info.setText("权限不足，无法连接！");
            }
        } else {
            info.setText("没有找到接口!");
        }
    }


    private UsbEndpoint epOut;
    private UsbEndpoint epIn;

    //用UsbDeviceConnection 与 UsbInterface 进行端点设置和通讯
    private void getEndpoint(UsbDeviceConnection connection, UsbInterface intf) {
        if (intf != null) { //这一句不加的话 很容易报错  导致很多人在各大论坛问:为什么报错呀
            for (int i = 0; i < intf.getEndpointCount(); i++) {
                UsbEndpoint ep = intf.getEndpoint(i);
                if (ep.getType() == UsbConstants.USB_ENDPOINT_XFER_INT) {
                    if (ep.getDirection() == UsbConstants.USB_DIR_OUT) {
                        epOut = ep;
                    } else {
                        epIn = ep;
                    }
                }
            }
        }
        info.setText("端点设置成功，In端口：" + (epIn == null ? epIn : epIn.getAddress()) + "  Out端口：" + (epOut == null ? epOut : epOut.getAddress()));
    }

    private View.OnClickListener btsendListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            String send = edit.getText().toString();
            if (send.length() == 0) {
                info.setText("待发送命令为空！");
                return;
            }
            byte[] sendCache = send.getBytes();
            sendMessageToPoint(sendCache);
            /*
            try {
                byte[] receive = receiveMessageFromPoint();
                if (receive != null) {
                    String r = new String(receive);
                    adapter.add(r);
                }
            }catch (Exception e){}*/
        }
    };

    // 从设备接收数据bulkIn
    private byte[] receiveMessageFromPoint() {
        if (epIn == null) {
            BuglyLog.i(TAG, "接收接口为空，无法接受！");
            return null;
        }
        byte[] buffer = new byte[15];
        if (mDeviceConnection.bulkTransfer(epIn, buffer, buffer.length,
                2000) < 0) {
            BuglyLog.i(TAG, "bulkIn返回输出为  负数");
            return null;
        } else {
            BuglyLog.i(TAG, "Receive Message Succese！");
            return buffer;
        }

    }

    // 发送数据
    private boolean sendMessageToPoint(byte[] buffer) {
        if (epOut == null) {
            info.setText("未找到发送接口，无法发送命令！");
            return false;
        }
        // bulkOut传输
        if (mDeviceConnection
                .bulkTransfer(epOut, buffer, buffer.length, 0) < 0) {
            BuglyLog.i(TAG, "bulkOut返回输出为  负数");
            return false;
        } else {
            info.setText("命令发送成功，长度:" + buffer.length);
            return true;
        }

    }

    // 显示提示的函数，这样可以省事，哈哈
    public void DisplayToast(CharSequence str) {
        Toast toast = Toast.makeText(this, str, Toast.LENGTH_LONG);
        // 设置Toast显示的位置
        toast.setGravity(Gravity.TOP, 0, 200);
        // 显示Toast
        toast.show();
    }

    Runnable ListenerDeviceRunnable = new Runnable() {
        @Override
        public void run() {
            while (threadRun) {
                BuglyLog.i(TAG,"tick");
                if (mUsbDevice == null) {
                    Message msg = new Message();
                    msg.what = CHECK;
                    handler.sendMessage(msg);
                } else {
                    HashMap<String, UsbDevice> deviceList = manager.getDeviceList();
                    if (deviceList.size() == 0) {
                        Message msg = new Message();
                        msg.what = TONONE;
                        handler.sendMessage(msg);
                    } else {
                        Message msg = new Message();
                        msg.what = KEEP;
                        handler.sendMessage(msg);
                    }
                }
                try {
                    Thread.currentThread().sleep(3000);
                } catch (InterruptedException e) {
                    BuglyLog.e(TAG, e.getMessage());
                }
            }
        }
    };

    private static final int CHECK = 0;
    private static final int KEEP = 1;
    private static final int TONONE = 2;
    private static final int FILESTART = 10;
    private static final int FILESUCCESS = 11;
    private static final int FILEFAIL = 12;
    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == CHECK) {
                deviceFind();
            } else if (msg.what == KEEP) {
                while (true) {
                    byte[] receive = receiveMessageFromPoint();
                    if (receive != null) {
                        //adapter.add(receive.toString());
                        adapter.add(bytes2HexString(receive).toString());
                    } else {
                        break;
                    }
                }
                //info.setText("receive == null");
            } else if (msg.what == TONONE) {
                text.setText("无连接设备");
                info.setText("");
                btsend.setEnabled(false);
            } else if (msg.what == FILESTART) {
                info.setText("正在传输文件中....");
            } else if (msg.what == FILESUCCESS) {
                info.setText("传输成功！");
            } else if (msg.what == FILEFAIL) {
                info.setText("传输失败：" + msg.getData().getString("reason"));
            }

        }
    };

    public static String bytes2HexString(byte[] b) {
        String ret = "";
        for (int i = 0; i < b.length; i++) {
            String hex = Integer.toHexString(b[i] & 0xFF);
            if (hex.length() == 1) {
                hex = '0' + hex;
            }
            ret += hex.toUpperCase();
        }
        return ret;
    }




    private void uploadFile(String path) {
        BuglyLog.d(TAG, "uploadFile path:" + path);

        final File file = new File(path);
        BuglyLog.i(TAG, "File:" + file.exists());
        if (mUsbDevice == null) {
            info.setText("设备已断开连接，传输失败！");
            return;
        }


        info.setText("发送命令->UpdateFile " + file.length());
        String title = "UpdateFile " + file.length();
        boolean flag = sendMessageToPoint(title.getBytes());

        BuglyLog.d(TAG, "uploadFile thread start");
        final Runnable runnable = new Runnable() {
            @Override
            public void run() {
                BuglyLog.d(TAG, "uploadFile wait 1500");
                try {
                    Thread.currentThread().sleep(1500);
                } catch (InterruptedException e) {
                    BuglyLog.e(TAG, "wait error! " + e.getMessage());
                }
                BuglyLog.d(TAG, "uploadFile start to send file");
                Message msg = new Message();
                msg.what = FILESTART;
                handler.sendMessage(msg);
                BufferedInputStream inStream = null;
                try {
                    inStream = new BufferedInputStream(new FileInputStream(file));
                    byte[] tmp = new byte[4096];
                    int len;
                    while ((len = inStream.read(tmp)) != -1 && mUsbDevice != null) {
                        if (!sendMessageToPoint(tmp)) {
                            msg = new Message();
                            msg.what = FILEFAIL;
                            Bundle bundle = new Bundle();
                            bundle.putString("reason", "传输失败：传输长度小于0");
                            msg.setData(bundle);
                            handler.sendMessage(msg);
                            return;
                        }
                    }
                    msg = new Message();
                    msg.what = FILESUCCESS;
                    handler.sendMessage(msg);

                } catch (FileNotFoundException e) {
                    BuglyLog.e(TAG, e.getMessage());
                    msg = new Message();
                    msg.what = FILEFAIL;
                    Bundle bundle = new Bundle();
                    bundle.putString("reason", "文件读取失败:" + e.getMessage());
                    msg.setData(bundle);
                    handler.sendMessage(msg);
                } catch (IOException e) {
                    BuglyLog.e(TAG, e.getMessage());
                    msg = new Message();
                    msg.what = FILEFAIL;
                    Bundle bundle = new Bundle();
                    bundle.putString("reason", "传输失败：" + e.getMessage());
                    msg.setData(bundle);
                    handler.sendMessage(msg);
                } finally {
                    try {
                        if (inStream != null)
                            inStream.close();
                    } catch (IOException e) {
                        BuglyLog.e(TAG, e.getMessage());
                    }
                }
            }
        };
        //handler.postDelayed(runnable,1500);
        fileThread = new Thread(runnable);
        fileThread.start();

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == FileActivity.RESULTCODE) {
            BuglyLog.d(TAG, "file selected");
            if(data!=null) {
                Bundle bundle = data.getExtras();
                String path = bundle.getString("PATH");
                BuglyLog.d(TAG, "file path:" + path);
                if (path != null && !path.equals(""))
                    uploadFile(path);
            }
        }else{
            super.onActivityResult(requestCode, resultCode, data);
        }

    }

}