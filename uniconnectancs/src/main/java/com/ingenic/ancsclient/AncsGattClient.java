package com.ingenic.ancsclient;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.content.Context;

import java.util.UUID;

abstract class AncsGattClient {
	private BluetoothManager mBluetoothManager;
	private Context mContext;
	private BluetoothGatt mClient;
	private BluetoothGattCallback sGattCallback;
	private static BluetoothGattCallback sCustomGattClientCallback;

	abstract void ConnStateChanged(BluetoothGatt gatt, int status, int newState);
	abstract void ServicesDiscovered(BluetoothGatt gatt, int status);
	abstract void DescWrite(BluetoothGatt gatt, BluetoothGattDescriptor desc, int status);
	abstract void ChacChanged(BluetoothGatt gatt, BluetoothGattCharacteristic chac);

	public AncsGattClient(Context context, BluetoothManager mBluetoothManager) {
		BluetoothAdapter mBluetoothAdapter = mBluetoothManager.getAdapter();
		mContext = context;
	}
	public static void setCustomCallback(BluetoothGattCallback callback) {
		sCustomGattClientCallback = callback;
	}
	public BluetoothGatt getGatt() {
		return mClient;
	}
	public void Connect(BluetoothDevice device) {
		if(sGattCallback == null){
			sGattCallback = new MyGattClientCallback();
		}
		mClient = device.connectGatt(mContext, false, sGattCallback);
	}
	public void Disconnect() {
		if(sGattCallback != null){
			sGattCallback = null;
		}
		if(mClient != null){
			mClient.disconnect();
			mClient.close();
			mClient = null;
		}
	}

	/**
	 * 设置请求的权限等级级别
	 * @param isSleep
     */
	public void requestConnectionPriority(boolean isSleep) {
		AncsLog.d("Update ble connection priority for " + (isSleep ? "sleep" : "wake"));

		// mClient != null && mClient.getDevice().isConnected()
		if (mClient != null) {
			mClient.requestConnectionPriority(
					isSleep ? BluetoothGatt.CONNECTION_PRIORITY_LOW_POWER : BluetoothGatt.CONNECTION_PRIORITY_BALANCED);
		}
	}

	/**
	 *  设置订阅特征
	 * @param gatt
	 * @param chac
     */
	public void subjectToNotification(BluetoothGatt gatt, BluetoothGattCharacteristic chac) {
		int charaProp = chac.getProperties();
		if ((charaProp & BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0) {
			gatt.setCharacteristicNotification(chac, true);
			BluetoothGattDescriptor descriptor = chac.getDescriptor(AncsUUID.CHAC_CONFIG);
			if (descriptor != null) {
				descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
				Boolean res = gatt.writeDescriptor(descriptor);
				AncsLog.d("writeDescriptor:"+res+",UUID="+chac.getUuid());
			} else {
				AncsLog.e("There is no descriptor of characteristic: "+chac.getUuid());
			}
		}
	}

	/**
	 *  control point 请求ancs 内容通知
	 * @param gatt
	 * @param req
     * @return
     */
    public boolean requestAttrs(BluetoothGatt gatt, byte[] req) {
            BluetoothGattService gattService;
            BluetoothGattCharacteristic charac;
            if (gatt == null) return false;
            gattService =  gatt.getService(AncsUUID.SERVICE);
			if (gattService == null) return false;
            charac = gattService.getCharacteristic(AncsUUID.CONTROL_POINT);
			if (charac == null) return false;
            charac.setValue(req);
			return gatt.writeCharacteristic(charac);
    }
	private boolean isAncsUuid(UUID uuid) {
		return uuid.equals(AncsUUID.SERVICE)
			|| uuid.equals(AncsUUID.SOURCE)
			|| uuid.equals(AncsUUID.CONTROL_POINT)
			|| uuid.equals(AncsUUID.NOTIFY);
	}
	final private class MyGattClientCallback extends BluetoothGattCallback {
		@Override
		public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
			AncsLog.d("GattClient onConnectionStateChange"
					+" device:" + gatt.getDevice());
			AncsLog.d("continue.."
					+" status:" + status
					+" newState:"+GetString.BtProfileState(newState));
			ConnStateChanged(gatt, status, newState);
			if(sCustomGattClientCallback != null){
				sCustomGattClientCallback.onConnectionStateChange(gatt, status, newState);
			}
		}
		@Override
		public void onServicesDiscovered(BluetoothGatt gatt, int status) {
			ServicesDiscovered(gatt, status);
			if(sCustomGattClientCallback != null){
				sCustomGattClientCallback.onServicesDiscovered(gatt, status);
			}
		}
		@Override
		public void onCharacteristicRead(BluetoothGatt gatt,
					BluetoothGattCharacteristic characteristic, int status) {
			if(sCustomGattClientCallback != null && !isAncsUuid(characteristic.getUuid())){
				sCustomGattClientCallback.onCharacteristicRead(gatt, characteristic, status);
			}
		}
		@Override
		public void onCharacteristicChanged(BluetoothGatt gatt,
				BluetoothGattCharacteristic characteristic) {
			UUID uuid = characteristic.getUuid();
			if(isAncsUuid(characteristic.getUuid())){
				ChacChanged(gatt, characteristic);
			}else{
				if(sCustomGattClientCallback != null){
					sCustomGattClientCallback.onCharacteristicChanged(gatt, characteristic);
				}
			}
		}
		@Override
		public void onCharacteristicWrite(BluetoothGatt gatt,
				BluetoothGattCharacteristic characteristic, int status) {
			if(sCustomGattClientCallback != null && !isAncsUuid(characteristic.getUuid())){
				sCustomGattClientCallback.onCharacteristicWrite(gatt, characteristic, status);
			}
		}
		@Override
		public void onDescriptorWrite(BluetoothGatt gatt,
				BluetoothGattDescriptor descriptor, int status) {
			/*status is defined in the file hcidefs.h gatt_api.h
			 * 0x84 GATT_BUSY
			 * 0x85 GATT_ERROR :such as to write when disconnected.
			 * 0xe GATT_ERR_UNLIKELY
			 * 0x8 HCI_ERR_CONNECTION_TOUT
			 * 0x13 HCI_ERR_PEER_USER
			 * 0x5 GATT_INSUF_AUTHENTICATION
			 */
			DescWrite(gatt, descriptor, status);
			if(sCustomGattClientCallback != null &&
					!isAncsUuid(descriptor.getCharacteristic().getUuid())){
				sCustomGattClientCallback.onDescriptorWrite(gatt, descriptor, status);
			}
		}
	};
}
