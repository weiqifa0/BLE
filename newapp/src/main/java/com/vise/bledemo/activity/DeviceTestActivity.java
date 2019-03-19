package com.vise.bledemo.activity;

import android.os.Handler;
import android.os.SystemClock;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.widget.TextView;

import com.vise.baseble.ViseBle;
import com.vise.baseble.callback.IBleCallback;
import com.vise.baseble.callback.IConnectCallback;
import com.vise.baseble.callback.scan.IScanCallback;
import com.vise.baseble.callback.scan.SingleFilterScanCallback;
import com.vise.baseble.common.PropertyType;
import com.vise.baseble.core.BluetoothGattChannel;
import com.vise.baseble.core.DeviceMirror;
import com.vise.baseble.exception.BleException;
import com.vise.baseble.model.BluetoothLeDevice;
import com.vise.baseble.model.BluetoothLeDeviceStore;
import com.vise.baseble.utils.HexUtil;
import com.vise.bledemo.R;
import com.vise.log.ViseLog;

import java.text.SimpleDateFormat;
import java.util.UUID;

import com.vise.xsnow.event.BusManager;

public class DeviceTestActivity extends AppCompatActivity {
    private static final String FIND_BLE_NAME = "IDP_BLE_TEST";
    private static final UUID serviceUUID = UUID.fromString("00001805-0000-1000-8000-00805f9b34fb");//00001805-0000-1000-8000-00805f9b34fb
    private static final UUID characteristicUUID= UUID.fromString("00002a2b-0000-1000-8000-00805f9b34fb");//00002a2b-0000-1000-8000-00805f9b34fb
    private static final UUID descriptorUUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
    private static String MAC;

    private TextView labletextShow = null;
    private DeviceMirror mDeviceMirror = null;
    private Handler handler = new Handler();
    private int intConnectCount = 0;
    /**
     * 线程实体
     */
    Runnable BleScanThread = new Runnable() {
        public void run() {
            SystemClock.sleep(1000);
            ViseLog.i("-------------------------->BleScanThread");
            handler.post(new Runnable() {
                @Override public void run() {
                    ViseLog.i("-------------------------->BleScanThread-handler");
                    periodScanCallbackByName(FIND_BLE_NAME);
                }
            });
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_test);
        init();
        testViewShowUi("开始测试...");

        ViseLog.i("-------------------------->onCreate");
    }

    @Override
    protected void onResume() {
        super.onResume();
        ViseLog.i("-------------------------->onResume");
        startScan();
    }

    @Override
    protected void onPause() {
        super.onPause();
        uninit();
        ViseLog.i(".");
    }
    /*
    * 退出的时候调用
    * */
    private void uninit(){
        disconnect();
    }
    @Override
    protected void onDestroy() {
        ViseBle.getInstance().clear();
        BusManager.getBus().unregister(this);
        super.onDestroy();
    }
    /**
     * 断开连接
     * */
    private void disconnect(){
        if(mDeviceMirror != null) {
            mDeviceMirror = null;
            ViseBle.getInstance().disconnect(mDeviceMirror.getBluetoothLeDevice());
            ViseLog.i("...");
            testViewShowUi("主设备断开蓝牙连接~~~");
        }
    }
    Runnable BleDisconnect = new Runnable() {
        public void run() {
            SystemClock.sleep(100);
            ViseLog.i("-------------------------->BleDisconnect");
            handler.post(new Runnable() {
                @Override public void run() {
                    ViseLog.i("-------------------------->BleDisconnect-handler");
                    if(mDeviceMirror != null) {
                        mDeviceMirror = null;
                        ViseBle.getInstance().disconnect(mDeviceMirror.getBluetoothLeDevice());
                        ViseLog.i("...");
                        testViewShowUi("主设备断开蓝牙连接~~~");
                    }
                }
            });
        }
    };

    /**
     * 初始化
     */
    private void init() {
        labletextShow = (TextView) findViewById(R.id.test_show);
        labletextShow.setMovementMethod(new ScrollingMovementMethod());
        labletextShow.setText("");
        startScan();
    }

    /**
     * 开始扫描
     */
    private void startScan() {
        ViseLog.i("-------------------------->startScan");
        testViewShowUi("b查找蓝牙广播...");
        //启动线程
        new Thread(BleScanThread).start();
    }

    public static String getCurDate(String pattern){
        SimpleDateFormat sDateFormat = new SimpleDateFormat(pattern);
        return sDateFormat.format(new java.util.Date());
    }

    private void connectBleByBluetoothDevice(BluetoothLeDevice bluetoothLeDevice){
        ViseBle.getInstance().connect(bluetoothLeDevice, new IConnectCallback() {
            @Override
            public void onConnectSuccess(DeviceMirror deviceMirror) {
                ViseLog.i("---------------------------------->>> onConnectSuccess " );
                //ViseBle.getInstance().disconnect(deviceMirror.getBluetoothLeDevice());
                testViewShowUi("连接蓝牙广播成功~~~");
                if(mDeviceMirror == null) {
                    mDeviceMirror = deviceMirror;
                }
                /*
                if((MAC == null)||(MAC!=deviceMirror.getBluetoothLeDevice().getDevice().getAddress())){
                    MAC = deviceMirror.getBluetoothLeDevice().getDevice().getAddress();
                    ViseLog.i(MAC);
                }else{
                    ViseBle.getInstance().disconnect(mDeviceMirror.getBluetoothLeDevice());
                    ViseLog.i("断开连接...");
                    testViewShowUi("该设备已经测试，断开连接~~");
                    return;
                }*/
                bleReadData(deviceMirror);
            }

            @Override
            public void onConnectFailure(BleException exception) {
                ViseLog.i("---------------------------------->>> onConnectFailure " );
                testViewShowUi("连接蓝牙广播失败~~~");
            }

            @Override
            public void onDisconnect(boolean isActive) {
                ViseLog.i("---------------------------------->>> onDisconnect " );
                intConnectCount+=1;
                testViewShowUi("["+intConnectCount+"]"+"断开蓝牙~~~e\n");
                startScan();
            }
        });
    }
    /**
     * 根据设备名称直接扫描并连接
     * modify by weiqifa
     * @param devicesName 设备名字
     */
    void periodScanCallbackByName(String devicesName){
        //该方式是扫到指定设备就停止扫描
        ViseBle.getInstance().startScan(new SingleFilterScanCallback(new IScanCallback() {
            @Override
            public void onDeviceFound(BluetoothLeDevice bluetoothLeDevice) {
                ViseLog.i("---------------------------------->>> onDeviceFound " );
                connectBleByBluetoothDevice(bluetoothLeDevice);
                testViewShowUi("找到蓝牙设备~~~");
            }

            @Override
            public void onScanFinish(BluetoothLeDeviceStore bluetoothLeDeviceStore) {
                ViseLog.i("---------------------------------->>> onScanFinish " );
                testViewShowUi("广播扫描完成~~~");
            }

            @Override
            public void onScanTimeout() {
                ViseLog.i("---------------------------------->>> onScanTimeout " );
                //testViewShowUi("广播扫描超时...");
            }
        }).setDeviceName(devicesName));
    }

    /**
     * 根据UUID读取数据
     * modify by weiqifa
     * @param deviceMirror
     */
    void bleReadData(final DeviceMirror deviceMirror) {

        if(deviceMirror == null){
            ViseLog.i("指针为空~");
            return;
        }
        BluetoothGattChannel bluetoothGattChannel = new BluetoothGattChannel.Builder()
                .setBluetoothGatt(deviceMirror.getBluetoothGatt())
                .setPropertyType(PropertyType.PROPERTY_READ)
                .setServiceUUID(serviceUUID)
                .setCharacteristicUUID(characteristicUUID)
                .setDescriptorUUID(null)
                .builder();

        deviceMirror.bindChannel(new IBleCallback() {
            @Override
            public void onSuccess(final byte[] data, final BluetoothGattChannel bluetoothGattChannel, final BluetoothLeDevice bluetoothLeDevice) {

                int year = byteToInt(data[1]) <<8  | byteToInt(data[0]);
                int month = byteToInt(data[2]);
                int day = byteToInt(data[3]);
                int hours = byteToInt(data[4]);
                int minutes = byteToInt(data[5]);
                int seconds = byteToInt(data[6]);
                int week = byteToInt(data[7]);
                final String textShow = year + "年"+month+"月"+day+"号"+hours+"时"+minutes+"分"+seconds+"秒，星期"+week;
                ViseLog.i("-----------------------------------------------------------------------------");
                ViseLog.i(">>> " + HexUtil.encodeHexStr(data));
                ViseLog.i(">>> " + year + "年"+month+"月"+day+"号"+hours+"时"+minutes+"分"+seconds+"秒，星期"+week);
                ViseLog.i("-----------------------------------------------------------------------------");

                doForTestSuccess(bluetoothLeDevice,textShow);
                ViseBle.getInstance().disconnect(deviceMirror.getBluetoothLeDevice());
            }

            @Override
            public void onFailure(BleException exception) {
                /**
                 * 读取失败
                 * */
                //labletextShow.setText("");
                testViewShowUi("读取数据失败~~重读~");
                runOnUiThread(new Runnable() {
                      @Override
                      public void run() {
                          deviceMirror.readData();
                      }
                });
                ViseLog.i(">>> "+exception);

            }
        }, bluetoothGattChannel);
        deviceMirror.readData();
    }
    /*
     * byte 与 int 的相互转换
     */
    private  byte intToByte(int x) {
        return (byte) x;
    }

    private  int byteToInt(byte b) {
        //Java 总是把 byte 当做有符处理；我们可以通过将其和 0xFF 进行二进制与得到它的无符值
        return b & 0xFF;
    }

    /**
     * 停止扫描
     */
    private void stopScan() {
        ViseLog.i(".");
    }

    /**
     * 成功获取数据后的处理
     * */
    private void doForTestSuccess(BluetoothLeDevice bluetoothLeDevice,String Data){
        testViewShowUi("收到数据:"+Data);
        //disconnect();
        //new Thread(BleDisconnect).start();
        ViseLog.i(".");
        testViewShowUi("测试完成断开连接~~~");
    }
    /**
     * UI显示 要用线程来显示，要不然会出现问题
     */
    private void testViewShowUi(final String Data){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ViseLog.i(".");
                labletextShow.append(getCurDate("yyyy-MM-dd HH:mm:ss"));
                labletextShow.append(":"+Data+"\n");
            }
        });
    }
}
