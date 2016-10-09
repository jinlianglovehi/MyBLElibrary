package com.ingenic.ancsclient;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;

import java.util.List;

abstract class ConnectionCheck {

	abstract void onConnected(BluetoothDevice device);

	private static final int INTERVAL_CHECK_MS          = 1000;
	private static final int CMD_CHECK_CONNECTED_DEVICE = 0;
	private BluetoothManager mBluetoothManager;
    private HandlerThread mHandlerThread;
    private Handler mHandler;
	private Context mContext;
	public ConnectionCheck(Context content) {
		mContext = content;
		mBluetoothManager = (BluetoothManager) mContext.getSystemService(Context.BLUETOOTH_SERVICE);
	}
	public void start() {
        if (mHandler == null) {
            mHandlerThread = new HandlerThread("ConnectionCheck");
            mHandlerThread.start();
            mHandler = new LoopHandler(mHandlerThread.getLooper());
        }
		mHandler.sendEmptyMessageDelayed(CMD_CHECK_CONNECTED_DEVICE, INTERVAL_CHECK_MS);
	}
	public void stop() {
		if(mHandler != null && mHandlerThread != null){
			mHandler.removeCallbacks(mHandlerThread);
			mHandler = null;
			mHandlerThread.quit();
			mHandlerThread = null;
		}
	}
	public boolean isConnected(BluetoothDevice device) {
		List<BluetoothDevice> devices;
		devices = mBluetoothManager.getConnectedDevices(BluetoothProfile.GATT);
		for (BluetoothDevice dev: devices) {
			if(device.getAddress().equals(dev.getAddress())){
				return true;
			}
		}
		return false;
	}
    private class LoopHandler extends Handler {
        private LoopHandler(Looper looper) {
            super(looper);
        }
        @Override
        public void handleMessage(Message msg) {
			BluetoothDevice device;
			List<BluetoothDevice> devices;
            switch (msg.what) {
                case CMD_CHECK_CONNECTED_DEVICE:
					devices = mBluetoothManager.getConnectedDevices(BluetoothProfile.GATT);
					for (BluetoothDevice dev: devices) {
						onConnected(dev);
						return;
					}
					sendEmptyMessageDelayed(CMD_CHECK_CONNECTED_DEVICE, INTERVAL_CHECK_MS);
                    break;
                default:
                    break;
            }
        }
    }

}
