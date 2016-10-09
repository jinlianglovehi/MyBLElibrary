package com.ingenic.ancsclient;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCallback;
import android.content.Context;

public class AncsConsumer {
	private boolean mDebug;
	private Context mContext;
	private AncsStateMachine AncsSM;

	public AncsConsumer(Context content){
		mContext = content;
	}
	public void start(boolean autoConnect) {
		start(autoConnect, false);
	}
	public void start(boolean autoConnect, boolean isStartedByIwds) {
		if(AncsSM == null){
			AncsLog.d("AncsConsumer started. Verson=T160427");
			AncsSM = new AncsStateMachine(mContext, autoConnect);
		}else{
			AncsLog.w("AncsConsumer already started.");
		}
	}
	public int connectDevice(BluetoothDevice device) {
		if(AncsSM == null){
			return AncsStateMachine.CONN_FAILED_STATE_ERR;
		}else{
			return AncsSM.connectDevice(device);
		}
	}
	public void stop() {
		if(AncsSM != null){
			AncsSM.destroy();
			AncsSM = null;
		}
	}
	public void setAncsSettings(AncsSender sender, String bleName, boolean debug) {
		AncsStateMachine.setAncsSender(sender);
		AncsLog.Enable(debug);
	}
	public void setAncsCallback(AncsCallback callback) {
		AncsStateMachine.setAncsCallback(callback);
	}
	public void setDebug(boolean debug) {
		AncsLog.Enable(debug);
	}
	public void AncsPerform(int uid, int action) {
		if(AncsSM != null){
			AncsSM.AncsPerform(uid, action);
		}
	}
	public void setCustomGattClientCallback(BluetoothGattCallback callback) {
		AncsGattClient.setCustomCallback(callback);
	}
}
