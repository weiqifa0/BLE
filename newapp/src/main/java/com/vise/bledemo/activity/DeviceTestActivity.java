package com.vise.bledemo.activity;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

import com.vise.baseble.ViseBle;
import com.vise.baseble.callback.IBleCallback;
import com.vise.baseble.callback.IConnectCallback;
import com.vise.baseble.callback.scan.IScanCallback;
import com.vise.baseble.callback.scan.ScanCallback;
import com.vise.baseble.callback.scan.SingleFilterScanCallback;
import com.vise.baseble.common.PropertyType;
import com.vise.baseble.core.BluetoothGattChannel;
import com.vise.baseble.core.DeviceMirror;
import com.vise.baseble.exception.BleException;
import com.vise.baseble.model.BluetoothLeDevice;
import com.vise.baseble.model.BluetoothLeDeviceStore;
import com.vise.baseble.utils.HexUtil;
import com.vise.bledemo.R;
import com.vise.bledemo.adapter.DeviceAdapter;
import com.vise.bledemo.common.BluetoothDeviceManager;
import com.vise.log.ViseLog;

import java.util.ArrayList;
import java.util.UUID;

import com.vise.bledemo.R;

public class DeviceTestActivity extends AppCompatActivity {
    private static final String FIND_BLE_NAME = "IDP_BLE_TEST";
    private static final UUID serviceUUID = UUID.fromString("00001805-0000-1000-8000-00805f9b34fb");//00001805-0000-1000-8000-00805f9b34fb
    private static final UUID characteristicUUID= UUID.fromString("00002a2b-0000-1000-8000-00805f9b34fb");//00002a2b-0000-1000-8000-00805f9b34fb
    private static final UUID descriptorUUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
    private static String MAC;

    private TextView labletextShow = null;
    private DeviceMirror mDeviceMirror = null;

    //设备扫描结果展示适配器
    private DeviceAdapter adapter;
    private BluetoothLeDeviceStore bluetoothLeDeviceStore = new BluetoothLeDeviceStore();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_test);
        init();
        ViseLog.i("");
    }

    @Override
    protected void onResume() {
        super.onResume();
        ViseLog.i("");
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

    /**
     * 断开连接
     * */
    private void disconnect(){
        if(mDeviceMirror != null) {
            ViseBle.getInstance().disconnect(mDeviceMirror.getBluetoothLeDevice());
            ViseLog.i("...");
        }
    }

    /**
     * 初始化
     */
    private void init() {
        labletextShow = (TextView) findViewById(R.id.test_show);
        labletextShow.setText("");
        startScan();
    }

    /**
     * 开始扫描
     */
    private void startScan() {
        ViseLog.i(">>> startScan");
        if (adapter != null) {
            adapter.setListAll(new ArrayList<BluetoothLeDevice>());
        }
        periodScanCallbackByName(FIND_BLE_NAME);
    }

    /**
     * 根据设备名称直接扫描并连接
     * modify by weiqifa
     * @param devicesName 设备名字
     */
    void periodScanCallbackByName(String devicesName){

        ViseBle.getInstance().connectByName(devicesName, new IConnectCallback() {
            @Override
            public void onConnectSuccess(DeviceMirror deviceMirror) {
                ViseLog.i(">>> onConnectSuccess " + deviceMirror);
                if(mDeviceMirror == null) {
                    mDeviceMirror = deviceMirror;
                }
                if((MAC == null)||(MAC!=deviceMirror.getBluetoothLeDevice().getDevice().getAddress())){
                    MAC = deviceMirror.getBluetoothLeDevice().getDevice().getAddress();
                    ViseLog.i(MAC);
                }else{
                    ViseBle.getInstance().disconnect(mDeviceMirror.getBluetoothLeDevice());
                    ViseLog.i("断开连接...");
                    testViewShowUi("该设备已经测试，断开连接~~");
                }
                bleReadData(deviceMirror);
            }

            @Override
            public void onConnectFailure(BleException exception) {
                ViseLog.i(">>> onConnectFailure " + exception);
            }

            @Override
            public void onDisconnect(boolean isActive) {
                ViseLog.i(">>> onDisconnect " + isActive);
                //startScan();
            }
        });
    }

    /**
     * 根据UUID读取数据
     * modify by weiqifa
     * @param deviceMirror
     */
    void bleReadData(DeviceMirror deviceMirror) {

        BluetoothGattChannel bluetoothGattChannel = new BluetoothGattChannel.Builder()
                .setBluetoothGatt(deviceMirror.getBluetoothGatt())
                .setPropertyType(PropertyType.PROPERTY_READ)
                .setServiceUUID(serviceUUID)
                .setCharacteristicUUID(characteristicUUID)
                .setDescriptorUUID(null)
                .builder();

        deviceMirror.bindChannel(new IBleCallback() {
            @Override
            public void onSuccess(byte[] data, BluetoothGattChannel bluetoothGattChannel, BluetoothLeDevice bluetoothLeDevice) {

                int year = byteToInt(data[1]) <<8  | byteToInt(data[0]);
                int month = byteToInt(data[2]);
                int day = byteToInt(data[3]);
                int hours = byteToInt(data[4]);
                int minutes = byteToInt(data[5]);
                int seconds = byteToInt(data[6]);
                int week = byteToInt(data[7]);
                String textShow = year + "年"+month+"月"+day+"号"+hours+"时"+minutes+"分"+seconds+"秒，星期"+week;
                ViseLog.i("-----------------------------------------------------------------------------");
                ViseLog.i(">>> " + HexUtil.encodeHexStr(data));
                ViseLog.i(">>> " + year + "年"+month+"月"+day+"号"+hours+"时"+minutes+"分"+seconds+"秒，星期"+week);
                ViseLog.i("-----------------------------------------------------------------------------");
                ViseBle.getInstance().disconnect(mDeviceMirror.getBluetoothLeDevice());
                doForTestSuccess(bluetoothLeDevice,textShow);
            }

            @Override
            public void onFailure(BleException exception) {
                /**
                 * 读取失败
                 * */
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
        testViewShowUi(Data);
        ViseBle.getInstance().disconnect(bluetoothLeDevice);
        disconnect();
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
                labletextShow.append(Data+"\n");
            }
        });
    }
}
