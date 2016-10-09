//package huadou.bleserverlib;
//
//import android.bluetooth.BluetoothAdapter;
//import android.bluetooth.BluetoothGattCharacteristic;
//import android.bluetooth.BluetoothGattDescriptor;
//import android.bluetooth.BluetoothGattService;
//import android.bluetooth.BluetoothManager;
//import android.bluetooth.le.AdvertiseData;
//import android.bluetooth.le.AdvertiseSettings;
//import android.content.Context;
//import android.os.ParcelUuid;
//import android.util.Log;
//
//import java.util.UUID;
//
//import huadou.bleserverlib.listener.BLEServerListener;
//import huadou.bleserverlib.uuids.SampleGattAttributes;
//
///**
// * Created by jinliang on 16-9-12.
// */
//public class BLEServerManager  implements BLEServerListener{
//
//    // ################ 变量的区域 start ##########################
//    public static BLEServerManager instance ;
//    private Context mContex;
//    private BluetoothManager mBluetoothManager;
//    private BluetoothAdapter mBluetoothAdapter;
//
//    private BluetoothGattService  bluetoothGattService;
//    // ################ 变量区域  end ############################
//    /**
//     * 获取 单例模型
//     * @param context
//     * @return
//     */
//    public static BLEServerManager getInstance (Context context){
//           if(instance==null){
//               synchronized (BLEServerManager.class){
//                   if(instance==null){
//                       instance = new BLEServerManager(context);
//                   }
//               }
//           }
//        return instance;
//    }
//
//    /**
//     * 构造函数
//     */
//    public BLEServerManager(Context context) {
//        this.mContex = context;
//    }
//
//
//    /**
//     * 打开蓝牙的开关
//     */
//    @Override
//    public void openBluetooth() {
//
//        mBluetoothManager = (BluetoothManager) mContex.getSystemService(Context.BLUETOOTH_SERVICE);
//        mBluetoothAdapter = mBluetoothManager.getAdapter();
//
//        if(!mBluetoothAdapter.enable()){ // 如果蓝牙没有打开
//            mBluetoothAdapter.enable(); // 打开蓝牙
//        }
//
//        // 现在暂时为一个gattService
//         bluetoothGattService = new BluetoothGattService(
//                UUID.fromString(SampleGattAttributes.GATT_SERVICE_UUID),
//                BluetoothGattService.SERVICE_TYPE_PRIMARY);
//
//    }
//
//    /**
//     * 添加服务和特征
//     */
//    @Override
//    public void addCharacteristics(String characteristicUUID ) {
//
//        final int properties = BluetoothGattCharacteristic.PROPERTY_BROADCAST
//                | BluetoothGattCharacteristic.PROPERTY_READ
//                | BluetoothGattCharacteristic.PROPERTY_NOTIFY
//                | BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE
//                | BluetoothGattCharacteristic.PROPERTY_WRITE;
//        final int permissions = BluetoothGattCharacteristic.PERMISSION_READ | BluetoothGattCharacteristic.PERMISSION_WRITE;
//        final int descPermissions = BluetoothGattDescriptor.PERMISSION_READ
//                //for password
//                //| BluetoothGattDescriptor.PERMISSION_WRITE_SIGNED
//                | BluetoothGattDescriptor.PERMISSION_WRITE;
//        String descStr = new String("communicate demo, can read,WRITE");
//
//        BluetoothGattCharacteristic gattChar = new BluetoothGattCharacteristic(
//                UUID.fromString(characteristicUUID), properties, permissions);
//        BluetoothGattDescriptor gattDesc = new BluetoothGattDescriptor(
//                UUID.fromString(SampleGattAttributes.UUID_Descriptor),
//                descPermissions);
//        gattDesc.setValue(descStr.getBytes());
//        gattChar.addDescriptor(gattDesc);
//
//        // 添加特征的内容
//        bluetoothGattService.addCharacteristic(gattChar);
//
//    }
//
//    /**
//     * 开始进行广播
//     */
//    @Override
//    public void startAdvertic() {
//
//            AdvertiseSettings.Builder SettingBuilder = new AdvertiseSettings.Builder();
//            SettingBuilder.setTxPowerLevel(SampleGattAttributes.mADVERTISE_TX_POWER);
//            SettingBuilder.setAdvertiseMode(SampleGattAttributes.mADVERTISE_MODE);
//            SettingBuilder.setTimeout(0);
//            SettingBuilder.setConnectable(true);
//
//            AdvertiseData.Builder advertiseBuilder = new AdvertiseData.Builder();
//            advertiseBuilder.setIncludeDeviceName(true);
//            try {
//                String oldblueAddress = BluetoothAdapter.getDefaultAdapter().getAddress();
//                String blueAddress = oldblueAddress.replace(":", "");
//                mBluetoothAdapter.setName(PRODUCT_NAME + blueAddress.substring(blueAddress.length() - 4));
//                Log.d(TAG_SELF, "mBluetoothAdapter.getName success = " + mBluetoothAdapter.getName());
//
//                // include step count
//                    int stepCount = 0;
//                    byte[] stepDate = new byte[4];
//                    Utils.parseIntToBytes(stepCount, stepDate, 0, 4, false);
//                    advertiseBuilder.addServiceData(ParcelUuid.fromString(SampleGattAttributes.GATT_SERVICE_UUID), stepDate);
//            } catch (Exception e) {
//                Log.w(TAG_SELF, "advertiseData obtain error: " + e.getMessage());
//            }
//
//            // scanResponse obtain
//            AdvertiseData.Builder responseBuilder = new AdvertiseData.Builder();
//            try {
//                responseBuilder.addServiceUuid(ParcelUuid.fromString(SampleGattAttributes.GATT_SERVICE_UUID));
//                String macAddress = BluetoothAdapter.getDefaultAdapter().getAddress();
//                byte[] macData = new byte[7];
//                boolean success = Utils.getByteFromMac(macAddress, macData, 1);
//                if (success) {
//                    macData[0] = 0;
//                    responseBuilder.addManufacturerData(MANUFACTURER_ID, macData);
//                }
//            } catch (Exception e) {
//            }
//            sAdvertiser.startAdvertising(SettingBuilder.build(), advertiseBuilder.build(),
//                    responseBuilder.build(), myAdvertiseCallback);
//
//
//
//
//
//    }
//
//}
