package huadou.bleclientlib;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.content.Context;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import huadou.bleclientlib.model.BluetoothDeviceInfo;

/**
 * Created by jinliang on 16-9-27.
 */
public class BLEClientManager {

     private final static String TAG = BLEClientManager.class.getName();
    /**
     *  ble client manager 单例模型
     */
    public static BLEClientManager  bleClientManager ;
    private Context mContext;


    private BluetoothAdapter bluetoothAdapter;
    private BluetoothDevice  currentBluetoothDevice; // 当前链接的蓝牙设备信息

    // 当期那链接的bluetooth 设备
    private BluetoothDeviceInfo mBluetoothDeviceInfo;

    /**
     * 搜索的设备集合的内容
     */
    private List<BluetoothDeviceInfo> deviceInfoList = new ArrayList<>();


    /**
     *  获取单例操作
     * @return
     */
    public static BLEClientManager getInstance(Context context){
        if(bleClientManager==null){
            synchronized (BLEClientManager.class){
               if(bleClientManager==null){
                   bleClientManager = new BLEClientManager(context);
               }
            }
        }
        return bleClientManager;
    }

    /**
     * @param context
     */
    public BLEClientManager(Context context) {
        this.mContext =context;
    }


    /**
     * 测试整个流程的方法
     */
    private void test(){

        checkBluetooth();


    }

    /**
     * 检查蓝牙是否打开
     */
    private void checkBluetooth(){
         bluetoothAdapter=BluetoothAdapter.getDefaultAdapter();
        if(!bluetoothAdapter.isEnabled()){
           bluetoothAdapter.enable();
        }
    }


    /**
     * 开始搜索蓝牙
     */
    private void startScan(final String  macAddress ){

        if(macAddress==null){
            return ;
        }
        deviceInfoList.clear();
        // 搜索
        bluetoothAdapter.startLeScan(new BluetoothAdapter.LeScanCallback() {
            @Override
            public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
                if(device!=null){
                    // 将搜索的设备添加到缓存集合中
                    deviceInfoList.add(new BluetoothDeviceInfo(device,rssi,scanRecord));
                    if(macAddress!=null && macAddress.equalsIgnoreCase(device.getAddress())){
                        Log.i(TAG, " find macAddress: "+ macAddress +"的设备信息");
                        currentBluetoothDevice = device;
                    }
                }
            }
        });

    }

    /**
     * 链接设备信息
     */
    private boolean  connect(Context context){
        if(currentBluetoothDevice==null){
            return false ; // 设备为空
        }
        currentBluetoothDevice.connectGatt(context,false ,myBluetoothGattCallBack);
        return false;
    }

    private BluetoothGattCallback myBluetoothGattCallBack = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicRead(gatt, characteristic, status);
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);
        }

        @Override
        public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorRead(gatt, descriptor, status);
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorWrite(gatt, descriptor, status);
        }

        @Override
        public void onReliableWriteCompleted(BluetoothGatt gatt, int status) {
            super.onReliableWriteCompleted(gatt, status);
        }

        @Override
        public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
            super.onReadRemoteRssi(gatt, rssi, status);
        }

        @Override
        public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
            super.onMtuChanged(gatt, mtu, status);
        }
    };


}
