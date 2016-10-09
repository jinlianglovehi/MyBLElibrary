package huadou.bleclientlib.listener;

import android.bluetooth.BluetoothDevice;

import java.util.List;

/**
 * Created by jinliang on 16-9-27.
 */


public interface BLEClientListener {

    /**
     * 直接开放链接接口
     * @param macAddress
     * @param callBack
     */
    void startScanAndConnectWithMac(String macAddress , BLEClientCallBack callBack);
}
