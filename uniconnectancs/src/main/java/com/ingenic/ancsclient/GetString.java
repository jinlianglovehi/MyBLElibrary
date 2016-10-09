package com.ingenic.ancsclient;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.AdvertiseCallback;

public class GetString {
	public static String scanMode(int mode) {
		switch(mode) {
			case BluetoothAdapter.SCAN_MODE_NONE :
				return "SCAN_MODE_NONE(20)";
			case BluetoothAdapter.SCAN_MODE_CONNECTABLE :
				return "SCAN_MODE_CONNECTABLE(21)";
			case BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE :
				return "SCAN_MODE_CONNECTABLE_DISCOVERABLE(23)";
			default :
				return "UNKNOWN("+mode+")";
		}
	}
	public static String bondState(int state) {
		switch(state) {
			case BluetoothDevice.BOND_NONE:
				return "BOND_NONE(10)";
			case BluetoothDevice.BOND_BONDING:
				return "BOND_BONDING(11)";
			case BluetoothDevice.BOND_BONDED:
				return "BOND_BONDED(12)";
			default :
				return "UNKNOWN("+state+")";
		}
	}
	public static String BtConnState(int state) {
		switch(state) {
			case BluetoothProfile.STATE_CONNECTING:
				return "STATE_CONNECTING(1)";
			case BluetoothProfile.STATE_DISCONNECTING:
				return "STATE_CONNECTING(0)";
			default :
				return "UNKNOWN("+state+")";
		}
	}
	public static String BtProfileState(int state) {
		switch(state) {
			case BluetoothProfile.STATE_DISCONNECTED :
				return "STATE_DISCONNECTED(0)";
			case BluetoothProfile.STATE_CONNECTING :
				return "STATE_CONNECTING(1)";
			case BluetoothProfile.STATE_CONNECTED :
				return "STATE_CONNECTED(2)";
			case BluetoothProfile.STATE_DISCONNECTING :
				return "STATE_DISCONNECTING(3)";
			default :
				return "UNKNOWN("+state+")";
		}
	}
	public static String StartAdvErr(int state) {
		switch(state) {
			case AdvertiseCallback.ADVERTISE_FAILED_DATA_TOO_LARGE:
				return "ADVERTISE_FAILED_DATA_TOO_LARGE(1)";
			case AdvertiseCallback.ADVERTISE_FAILED_TOO_MANY_ADVERTISERS:
				return "ADVERTISE_FAILED_TOO_MANY_ADVERTISERS(2)";
			case AdvertiseCallback.ADVERTISE_FAILED_ALREADY_STARTED:
				return "ADVERTISE_FAILED_ALREADY_STARTED(3)";
			case AdvertiseCallback.ADVERTISE_FAILED_INTERNAL_ERROR:
				return "ADVERTISE_FAILED_INTERNAL_ERROR(4)";
			case AdvertiseCallback.ADVERTISE_FAILED_FEATURE_UNSUPPORTED:
				return "ADVERTISE_FAILED_FEATURE_UNSUPPORTED(5)";
			default :
				return "UNKNOWN("+state+")";
		}
	}
}
