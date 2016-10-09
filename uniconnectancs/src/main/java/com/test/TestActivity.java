package com.test;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.os.Bundle;

import com.ingenic.ancsclient.AncsCallback;
import com.ingenic.ancsclient.AncsConsumer;

/**
 * Created by jinliang on 16-10-8.
 */
public class TestActivity extends Activity{

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        AncsConsumer  ancsConsumer = new AncsConsumer(this);
        BluetoothDevice connectDevice = null ;

        // 设置链接
        ancsConsumer.start(true);
        ancsConsumer.connectDevice(connectDevice);
        ancsConsumer.setDebug(true);
        ancsConsumer.setAncsCallback(new AncsCallback() {
            @Override
            public void onConnectionStateChange(String addr, int connState) {

            }

            @Override
            public void onRecvNotifyAttrib(int uid, String appName, String title, String subtitle, String message, String date) {

            }

            @Override
            public void onRecvNotifyCharacter(int eventId, int eventFlags, int categoryId, int categoryCount, int notifUid) {

            }
        });

    }
}
