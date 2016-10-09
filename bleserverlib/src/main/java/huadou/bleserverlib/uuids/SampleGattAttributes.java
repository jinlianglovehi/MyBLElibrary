package huadou.bleserverlib.uuids;

import android.bluetooth.le.AdvertiseSettings;

/**
 * Created by jinliang on 16-9-13.
 * <p>
 * Gatt uuids 属性的定义
 */
public class SampleGattAttributes {


    //  serverid
    public static final String GATT_SERVICE_UUID = "";
    //  特征uuid
    public static final String GATT_CHARACTERISTICS_UUID = "";


    // 特征订阅的描述uuid
    public static String UUID_Descriptor = "00002902-0000-1000-8000-00805f9b34fb";

    /**
     * 广播设置 power频度值
     */
    public static int mADVERTISE_TX_POWER = AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM;
    public static int mADVERTISE_MODE = AdvertiseSettings.ADVERTISE_MODE_LOW_POWER;


}
