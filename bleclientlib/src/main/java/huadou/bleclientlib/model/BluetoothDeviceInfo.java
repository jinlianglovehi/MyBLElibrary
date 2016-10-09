package huadou.bleclientlib.model;

import android.bluetooth.BluetoothDevice;

/**
 * Created by jinliang on 16-9-27.
 *
 *  自己封装的bluetooth Device Info
 */
public class BluetoothDeviceInfo {

    BluetoothDevice device ; // 设备信息
    int rssi ; // 信号强度
    byte[] scanRecord ; // 扫描包
    public BluetoothDeviceInfo(BluetoothDevice device, int rssi, byte[] scanRecord) {
        this.device = device;
        this.rssi = rssi;
        this.scanRecord = scanRecord;
    }
}
