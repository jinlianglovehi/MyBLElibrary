package com.ingenic.ancsclient;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.os.Message;

import com.android.internal.util.State;
import com.android.internal.util.StateMachine;
import com.ingenic.ancsclient.PowerWorker.PowerWorkerCallback;

import java.util.ArrayList;
import java.util.Date;
import java.util.UUID;

public class AncsStateMachine extends StateMachine {
	public static final int CONN_DEV_SUCCESS               =  0;
	public static final int CONN_FAILED_NO_SUCH_DEV        = -1;
	public static final int CONN_FAILED_STATE_ERR          = -2;

	private static final int TYPE_ATTR_REQUEST_CHARACTER   = 1;
	private static final int TYPE_ATTR_REQUEST_PERFORM     = 2;

	private static final int TOUT_CONNECTING_MS            = 10000;
	private static final int TOUT_BONDING_MS               = 30000;
	private static final int TOUT_BONDING_CHECK_MS         = 1000;
	private static final int TOUT_SERVICES_DISCOVER_MS     = 30000;
	private static final int TOUT_SUBJECT_NOTIFY_MS        = 5000;
	private static final int TOUT_REQUEST_ATTRS_MS         = 2000;
	private static final int TDELAY_SUBJECT_MS             = 5;
	private static final int TDELAY_REQUEST_ATTRS_MS       = 500;
	private static final int TDELAY_REQUEST_PERFORM_MS     = 250;
	private static final int TDELAY_GATT_MESSAGE_MS        = 1;
	private static final int TDELAY_DEVICE_CONNECTED_MS    = 1;

	private static final int CMD_INIT                      = -2;
	private static final int CMD_RESET                     = 0x00;
	private static final int CMD_STARTUP                   = 0x01;
	private static final int CMD_STOP                      = 0x02;

	private static final int CMD_BT_SCAN_MODE_CHANGED      = 0x10;
	private static final int CMD_BT_BOND_STATE_CHANGED     = 0x11;
	private static final int CMD_BT_DEVICE_CONNECTED       = 0x12;

	private static final int CMD_EVENT_FOR_BONDING         = 0x31;
	private static final int CMD_BONDING_CHECK             = 0x32;
	private static final int CMD_CONNECTING                = 0x33;
	private static final int CMD_CONNECTING_TIMEOUT        = 0x34;
	private static final int CMD_BONDING_TIMEOUT           = 0x35;

	private static final int CMD_GATTC_CONNECTED           = 0x40;
	private static final int CMD_GATTC_DISCONNECTED        = 0x41;
	private static final int CMD_GATTC_SERVICES_DISCOVER   = 0x42;
	private static final int CMD_GATTC_SERV_DISC_TIMEOUT   = 0x43;
	private static final int CMD_GATTC_SERV_DISC_ERR       = 0x44;
	private static final int CMD_GATTC_SERVICES_DISCOVERED = 0x45;
	private static final int CMD_GATTC_ANCS_SERV_NOT_FOUND = 0x46;

	private static final int CMD_SUBJECTING                = 0x50;
	private static final int CMD_SUBJECT_NOTIFY_TIMEOUT    = 0x51;
	private static final int CMD_SUBJECT_NOTIFY_SUCCESS    = 0x52;
	private static final int CMD_SUBJECT_SOURCE_SUCCESS    = 0x53;
	private static final int CMD_CHAC_CHANGED              = 0x54;
	private static final int CMD_REQUEST_ATTRS             = 0x55;
	private static final int CMD_ANCS_PERFORM              = 0x56;
	private static final int CMD_ANCS_PERFORM_DELAY_DONE   = 0x57;


	private static AncsSender sAncsSender;//compatible with earlier Ancs versions
	private static AncsCallback  sAncsCallback;
	private BluetoothManager     mBluetoothManager;
	private GattClient           mGattClient;
	private Context              mContext;
	private BluetoothDevice      mConnectedDevice;
	private boolean              mAutoConnect;
	private MyConnectionCheck    mConnectionCheck;
	private PowerWorker          mPowerWorker;
	private State                mCurrentState;
	private SM_ClientReset       mSM_ClientReset         = new SM_ClientReset();
	private SM_ClientUnconnected mSM_ClientUnconnected   = new SM_ClientUnconnected();
	private SM_ClientConnecting  mSM_ClientConnecting    = new SM_ClientConnecting();
	private SM_ClientConnected   mSM_ClientConnected     = new SM_ClientConnected();
	private boolean              mIsConnectedState = false;
	private Object               mLock = new Object();

	AncsStateMachine (Context context, boolean autoConnect) {
		this(context, autoConnect, false);
	}

	AncsStateMachine (Context context, boolean autoConnect, boolean isStartedByIwds) {
		super("AncsStateMachine");
		mContext           = context;
		mAutoConnect       = autoConnect;
		mBluetoothManager  = (BluetoothManager)mContext.getSystemService(Context.BLUETOOTH_SERVICE);
		mConnectionCheck   = new MyConnectionCheck(mContext);

		if (isStartedByIwds) {
			mPowerWorker = new PowerWorker(null, new PowerWorkerCallback() {
				public void suspend(Object arg) {
					requestConnectionPriority(true);
				}

				public void resume(Object arg) {
					requestConnectionPriority(false);
				}
			}, 0);
		}

		addState(mSM_ClientReset);
		addState(mSM_ClientUnconnected);
		addState(mSM_ClientConnecting);
		addState(mSM_ClientConnected);
		setInitialState(mSM_ClientReset);
		start();
		sendMessage(CMD_INIT);
	}
	private void requestConnectionPriority(boolean isSleep) {
		synchronized (mLock) {
			if (mIsConnectedState && mGattClient != null) {
				mGattClient.requestConnectionPriority(isSleep);
			}
		}
	}
	public void destroy() {
		sendMessage(CMD_STOP);
	}
	private void onDestroy() {
		AncsLog.d("AncsStateMachine onDestroy");
		removeAllTimeoutMessages();
		quit();
		mCurrentState         = null;
		mSM_ClientReset       = null;
		mSM_ClientUnconnected = null;
		mSM_ClientConnecting  = null;
		mSM_ClientConnected   = null;
		mBluetoothManager     = null;
		sAncsSender           = null;
		sAncsCallback         = null;
		mContext              = null;
		mConnectedDevice      = null;
		if(mGattClient != null){
			mGattClient.Disconnect();
			mGattClient = null;
		}
		if(mConnectionCheck != null){
			mConnectionCheck.stop();
			mConnectionCheck = null;
		}
		if (mPowerWorker != null) {
			mPowerWorker.quit();
			mPowerWorker = null;
		}
	}
	public int connectDevice(BluetoothDevice device) {
		if(mCurrentState == mSM_ClientUnconnected){
			if(mConnectionCheck.isConnected(device)){
				sendMessageDelayed(CMD_BT_DEVICE_CONNECTED, device, TDELAY_DEVICE_CONNECTED_MS);
				return CONN_DEV_SUCCESS;
			}
			return CONN_FAILED_NO_SUCH_DEV;
		}
		return CONN_FAILED_STATE_ERR;
	}
	public void AncsPerform(int uid, int action) {
		Message message  =new Message();
		message.what = CMD_ANCS_PERFORM;
		message.arg1 =uid ;
		message.arg2 =action ;
		sendMessageDelayed(message, TDELAY_GATT_MESSAGE_MS);
	}
	public static void setAncsCallback(AncsCallback callback) {
		sAncsCallback = callback;
	}
	public static void setAncsSender(AncsSender sender) {
		sAncsSender = sender;
	}
	final private class SM_ClientReset extends State{
		@Override public void enter() {
			SM_Debug.enter("SM_Reset");
			mCurrentState = mSM_ClientReset;
			if(mConnectedDevice != null){
				if(sAncsSender != null){
					sAncsSender.broadcast(null,
							mConnectedDevice.getAddress(), AncsSender.STATE_DISCONNECTED);
				}
				if(sAncsCallback != null){
					sAncsCallback.onConnectionStateChange(
							mConnectedDevice.getAddress(), AncsSender.STATE_DISCONNECTED);
				}
				mConnectedDevice = null;
			}
		}
		@Override public void exit() { SM_Debug.exit("SM_Reset"); }
		@Override public boolean processMessage(Message message) {
			switch (message.what) {
			case CMD_STARTUP:
				if(mGattClient != null){
					mGattClient.Disconnect();
					mGattClient = null;
				}
				if(mGattClient == null){
					mGattClient = new GattClient(mContext, mBluetoothManager);
				}
				if(mAutoConnect){
					mConnectionCheck.start();
				}
				transitionTo(mSM_ClientUnconnected);
				break;
			case CMD_STOP:
				onDestroy();
				break;
			default:
				removeAllTimeoutMessages();
				sendMessage(CMD_STARTUP);
				break;
			}
			return HANDLED;
		}
	}
	private void removeAllTimeoutMessages() {
		// remove all timeout messages
		removeMessages(CMD_CONNECTING_TIMEOUT);
		removeMessages(CMD_BONDING_TIMEOUT);
		removeMessages(CMD_GATTC_SERV_DISC_TIMEOUT);
		removeMessages(CMD_SUBJECT_NOTIFY_TIMEOUT);
		removeMessages(CMD_SUBJECTING);
		removeMessages(CMD_REQUEST_ATTRS);
		removeMessages(CMD_BT_DEVICE_CONNECTED);
		removeMessages(CMD_STARTUP);
	}
	final private class SM_ClientUnconnected extends State{
		@Override public void enter() {
			SM_Debug.enter("SM_ClientUnconnected");
			mCurrentState = mSM_ClientUnconnected;
		}
		@Override public void exit() { SM_Debug.exit("SM_ClientUnconnected"); }
		@Override public boolean processMessage(Message message) {
			BluetoothDevice device;
			switch (message.what){
				case CMD_BT_DEVICE_CONNECTED:
					device = (BluetoothDevice)message.obj;
					AncsLog.d("Device Connected:"+device);
					sendMessageDelayed(CMD_EVENT_FOR_BONDING, device, TDELAY_GATT_MESSAGE_MS);
					mConnectedDevice = device;
					transitionTo(mSM_ClientConnecting);
					return HANDLED;
				case CMD_STOP:
					onDestroy();
					return HANDLED;
				default:
					return NOT_HANDLED;
			}
		}
	}
	final private class SM_ClientConnecting extends State{
		@Override public void enter() {
			SM_Debug.enter("SM_ClientConnecting");
			mCurrentState = mSM_ClientConnecting;
			if(mConnectedDevice != null){
				if(sAncsSender != null){
					sAncsSender.broadcast(null,
							mConnectedDevice.getAddress(), AncsSender.STATE_CONNECTING);
				}
				if(sAncsCallback != null){
					sAncsCallback.onConnectionStateChange(
							mConnectedDevice.getAddress(), AncsSender.STATE_CONNECTING);
				}
			}
		}
		@Override public void exit() { SM_Debug.exit("SM_ClientConnecting"); }
		@Override public boolean processMessage(Message message) {
			BluetoothGatt gatt;
			BluetoothDevice device;
			switch (message.what){
				case CMD_EVENT_FOR_BONDING:
					device = (BluetoothDevice)message.obj;
					device.createBond(BluetoothDevice.TRANSPORT_LE);
					sendMessageDelayed(CMD_BONDING_TIMEOUT, TOUT_BONDING_MS);
					sendMessageDelayed(CMD_BONDING_CHECK, device, TDELAY_GATT_MESSAGE_MS);
					return HANDLED;
				case CMD_BONDING_CHECK://Connecting device if encrypted,regardless paring in process
					AncsLog.d("CMD_BONDING_CHECK...");
					device = (BluetoothDevice)message.obj;
					boolean isConnEncryped = device.isEncrypted();
					if(isConnEncryped){
						removeMessages(CMD_BONDING_TIMEOUT);
						sendMessageDelayed(CMD_CONNECTING, device, TDELAY_GATT_MESSAGE_MS);
						AncsLog.d("CMD_BONDING_CHECK...OK! Connecting");
					}else{
						sendMessageDelayed(CMD_BONDING_CHECK, device, TOUT_BONDING_CHECK_MS);
					}
					return HANDLED;
				case CMD_BONDING_TIMEOUT:
					AncsLog.d("bonding timeout");
					removeMessages(CMD_BONDING_CHECK);
					sendMessage(CMD_RESET);
					transitionTo(mSM_ClientReset);
					return HANDLED;
				case CMD_CONNECTING:
					//connect to iphone as client.
					device = (BluetoothDevice)message.obj;
					sendMessageDelayed(CMD_CONNECTING_TIMEOUT, TOUT_CONNECTING_MS);
					mGattClient.Connect(device);
					return HANDLED;
				case CMD_CONNECTING_TIMEOUT:
					AncsLog.d("connecting timeout");
					sendMessage(CMD_RESET);
					transitionTo(mSM_ClientReset);
					return HANDLED;
				case CMD_GATTC_CONNECTED:
					removeMessages(CMD_CONNECTING_TIMEOUT);
					gatt = (BluetoothGatt)message.obj;
					sendMessage(CMD_GATTC_SERVICES_DISCOVER, gatt);
					transitionTo(mSM_ClientConnected);
					return HANDLED;
				case CMD_STOP:
					onDestroy();
					return HANDLED;
				default:
					return NOT_HANDLED;
			}
		}
	}
	final private class SM_ClientConnected extends State{
		//use for request attrib
		long attrLastTime;
		boolean attrReqPendingFlag;
		ArrayList<AttrRequest> attrReqArray;
		@Override public void enter() {
			SM_Debug.enter("SM_ClientConnected");
			synchronized (mLock) {
				mIsConnectedState = true;
			}
			mCurrentState = mSM_ClientConnected;
			if(mConnectedDevice != null){
				if(sAncsSender != null){
					sAncsSender.broadcast(mConnectedDevice.getName(),
							mConnectedDevice.getAddress(), AncsSender.STATE_CONNECTED);
				}
				if(sAncsCallback != null){
					sAncsCallback.onConnectionStateChange(
							mConnectedDevice.getAddress(), AncsSender.STATE_CONNECTED);
				}
			}
			attrReqArray = new ArrayList();
		}
		@Override public void exit() {
			attrReqArray = null;
			synchronized (mLock) {
				mIsConnectedState = false;
			}
			SM_Debug.exit("SM_ClientConnected");
		}
		@Override public boolean processMessage(Message message) {
			BluetoothGatt gatt;
			BluetoothGattService gattAncsService;
			switch (message.what){
				case CMD_GATTC_DISCONNECTED:
					sendMessage(CMD_RESET);
					transitionTo(mSM_ClientReset);
					AncsLog.d("gatt client disconnected");
					return HANDLED;
				case CMD_GATTC_ANCS_SERV_NOT_FOUND:
					sendMessage(CMD_RESET);
					transitionTo(mSM_ClientReset);
					AncsLog.d("Ancs Service not found");
					return HANDLED;
				case CMD_GATTC_SERV_DISC_TIMEOUT:
					sendMessage(CMD_RESET);
					transitionTo(mSM_ClientReset);
					AncsLog.d("Ancs Service discovery timeout");
					return HANDLED;
				case CMD_GATTC_SERVICES_DISCOVER:
					gatt = (BluetoothGatt)message.obj;
					sendMessageDelayed(CMD_GATTC_SERV_DISC_TIMEOUT, TOUT_SERVICES_DISCOVER_MS);
					gatt.refresh();
					gatt.discoverServices();
					return HANDLED;
				case CMD_GATTC_SERV_DISC_ERR:
					AncsLog.d("CMD_GATTC_SERV_DISC_ERR:" + message.arg1);
					sendMessage(CMD_RESET);
					transitionTo(mSM_ClientReset);
					return HANDLED;
				case CMD_GATTC_SERVICES_DISCOVERED:
					removeMessages(CMD_GATTC_SERV_DISC_TIMEOUT);
					gatt = (BluetoothGatt)message.obj;
					gattAncsService = gatt.getService(AncsUUID.SERVICE);
					if(gattAncsService == null
							|| gattAncsService.getCharacteristic(AncsUUID.SOURCE) == null
							|| gattAncsService.getCharacteristic(AncsUUID.NOTIFY) == null){
						sendMessage(CMD_GATTC_ANCS_SERV_NOT_FOUND);
					}else{
						//AncsLog.d("ANCS services found, begin subject notification");
						sendMessageDelayed(CMD_SUBJECTING, gatt, TDELAY_SUBJECT_MS);
					}
					return HANDLED;
				case CMD_SUBJECTING:
					sendMessageDelayed(CMD_SUBJECT_NOTIFY_TIMEOUT, TOUT_SUBJECT_NOTIFY_MS);
					gatt = (BluetoothGatt)message.obj;
					gattAncsService = gatt.getService(AncsUUID.SERVICE);
					mGattClient.subjectToNotification(gatt,
							gattAncsService.getCharacteristic(AncsUUID.SOURCE));
					return HANDLED;
				case CMD_SUBJECT_SOURCE_SUCCESS:
					gatt = (BluetoothGatt)message.obj;
					gattAncsService = gatt.getService(AncsUUID.SERVICE);
					mGattClient.subjectToNotification(gatt,
							gattAncsService.getCharacteristic(AncsUUID.NOTIFY));
					return HANDLED;
				case CMD_SUBJECT_NOTIFY_SUCCESS:
					removeMessages(CMD_SUBJECT_NOTIFY_TIMEOUT);
					//waiting for messages
					return HANDLED;
				case CMD_SUBJECT_NOTIFY_TIMEOUT:
					AncsLog.e("Subject notify timeout");
					sendMessage(CMD_RESET);
					transitionTo(mSM_ClientReset);
					return HANDLED;
				case CMD_REQUEST_ATTRS:
					removeMessages(CMD_REQUEST_ATTRS);
					gatt = (BluetoothGatt)message.obj;
					if(!attrReqPendingFlag && !attrReqArray.isEmpty()){
						AttrRequest attrRequest = attrReqArray.get(0);
						attrReqPendingFlag = true;
						mGattClient.requestAttrs(gatt, attrRequest.mData);
						if(attrRequest.mType == TYPE_ATTR_REQUEST_PERFORM){
							sendMessageDelayed(CMD_ANCS_PERFORM_DELAY_DONE, TDELAY_REQUEST_PERFORM_MS);
						}
						attrReqArray.remove(0);
					}
					return HANDLED;
				case CMD_CHAC_CHANGED:
					ChacChangedData data = (ChacChangedData)message.obj;
					gatt = data.mGatt;
					byte[] value = data.mValue;
					UUID uuid = data.mUuid;
					if(uuid.equals(AncsUUID.NOTIFY)){
						Date nowDate = new Date();
						AncsParser.Notification notify;
						notify = AncsParser.ParseNotification(value);
						if(sAncsSender != null){
							sAncsSender.broadcast(
									notify.EventID,
									notify.EventFlags,
									notify.CategoryID,
									notify.CategoryCount,
									notify.NotificationID);
						}
						if(sAncsCallback != null){
							sAncsCallback.onRecvNotifyCharacter(
									notify.EventID,
									notify.EventFlags,
									notify.CategoryID,
									notify.CategoryCount,
									notify.NotificationID);
						}
						AncsLog.d("requestAttrs:"
								+notify.EventID+"-"+
								+notify.EventFlags+"-"+
								+notify.CategoryID+"-"+
								+notify.CategoryCount+"-"+
								+notify.NotificationID);
						byte[] requestData;
						if(notify.EventID == AncsParser.EVENTID_NOTIFICATION_ADDED
								/*&& (notify.EventFlags & AncsParser.EVENT_FLAG_PRE_EXISTING) == 0*/){
							requestData = AncsParser.GetRequsetData(notify);
							if(System.currentTimeMillis()-attrLastTime > TOUT_REQUEST_ATTRS_MS){
								attrReqArray.clear();
								attrReqPendingFlag = false;
							}
							attrReqArray.add(new AttrRequest(TYPE_ATTR_REQUEST_CHARACTER, requestData));
							attrLastTime = System.currentTimeMillis();
							removeMessages(CMD_REQUEST_ATTRS);
							sendMessageDelayed(CMD_REQUEST_ATTRS, gatt, TDELAY_REQUEST_ATTRS_MS);
						}
					}else if(uuid.equals(AncsUUID.SOURCE)){
						AncsParser.Detail detail;
						detail = AncsParser.ParseData(value);
						attrLastTime = System.currentTimeMillis();
						if(detail.isValid){
							if(sAncsSender != null){
								sAncsSender.broadcast(
										detail.NotificationID,
										detail.appId,
										detail.title,
										detail.message,
										detail.date,
										detail.subtitle,
										null,
										false);
							}
							if(sAncsCallback != null){
								sAncsCallback.onRecvNotifyAttrib(
										detail.NotificationID,
										detail.appId,
										detail.title,
										detail.subtitle,
										detail.message,
										detail.date);
							}
							AncsLog.d(":"+detail.NotificationID);
							AncsLog.d(":"+detail.message);
							AncsLog.d(":"+detail.date);
							attrReqPendingFlag = false;
							if(!attrReqArray.isEmpty()){
								sendMessageDelayed(CMD_REQUEST_ATTRS, gatt, TDELAY_GATT_MESSAGE_MS);
							}
						}else{
						}
					}else{
						AncsLog.d("Characteristic unknown:"+uuid);
					}
					return HANDLED;
				case CMD_ANCS_PERFORM_DELAY_DONE:
					attrReqPendingFlag = false;
					sendMessageDelayed(CMD_REQUEST_ATTRS,
							mGattClient.getGatt(), TDELAY_GATT_MESSAGE_MS);
					return HANDLED;
				case CMD_ANCS_PERFORM:
					int uid = message.arg1;
					int action = message.arg2;
					byte[] requestData = AncsParser.GetPerformData(uid, (byte)action);
					if(System.currentTimeMillis()-attrLastTime > TOUT_REQUEST_ATTRS_MS){
						attrReqArray.clear();
						attrReqPendingFlag = false;
					}
					attrReqArray.add(new AttrRequest(TYPE_ATTR_REQUEST_PERFORM, requestData));
					attrLastTime = System.currentTimeMillis();
					removeMessages(CMD_REQUEST_ATTRS);
					sendMessageDelayed(CMD_REQUEST_ATTRS,
							mGattClient.getGatt(), TDELAY_REQUEST_ATTRS_MS);
					return HANDLED;
				case CMD_STOP:
					onDestroy();
					return HANDLED;
				default:
					return NOT_HANDLED;
			}
		}
	}
	final private class GattClient extends AncsGattClient {
		public GattClient(Context mContext, BluetoothManager mBluetoothManager) {
			super(mContext, mBluetoothManager);
		}
		public void ConnStateChanged(BluetoothGatt gatt, int status, int newState) {
			if(status == BluetoothGatt.GATT_SUCCESS){
				if(newState == BluetoothProfile.STATE_CONNECTED){
					sendMessageDelayed(CMD_GATTC_CONNECTED, gatt, TDELAY_GATT_MESSAGE_MS);
				}
			}else{
				if(newState == BluetoothProfile.STATE_DISCONNECTED){
					sendMessageDelayed(CMD_GATTC_DISCONNECTED, gatt, TDELAY_GATT_MESSAGE_MS);
				}
			}
		}
		public void ServicesDiscovered(BluetoothGatt gatt, int status) {
			if(status == BluetoothGatt.GATT_SUCCESS){
				sendMessageDelayed(CMD_GATTC_SERVICES_DISCOVERED, gatt, TDELAY_GATT_MESSAGE_MS);
			}else{
				sendMessageDelayed(CMD_GATTC_SERV_DISC_ERR, status, TDELAY_GATT_MESSAGE_MS);
			}
		}
		public void DescWrite(BluetoothGatt gatt, BluetoothGattDescriptor desc, int status) {
			if(status == BluetoothGatt.GATT_SUCCESS){
				if(desc.getCharacteristic().getUuid().equals(AncsUUID.SOURCE)){
					AncsLog.d("Subject source success");
					sendMessageDelayed(CMD_SUBJECT_SOURCE_SUCCESS, gatt, TDELAY_GATT_MESSAGE_MS);
				}else if(desc.getCharacteristic().getUuid().equals(AncsUUID.NOTIFY)){
					AncsLog.d("Subject notify success");
					sendMessageDelayed(CMD_SUBJECT_NOTIFY_SUCCESS, TDELAY_GATT_MESSAGE_MS);
				}
			}else{
				AncsLog.e("Descriptor("+desc.getCharacteristic().getUuid()+") subject failed");
			}
		}
		public void ChacChanged(BluetoothGatt gatt, BluetoothGattCharacteristic chac) {
			sendMessageDelayed(CMD_CHAC_CHANGED,
					new ChacChangedData(gatt, chac.getUuid(), chac.getValue()), TDELAY_GATT_MESSAGE_MS);
		}
	}
	final private class ChacChangedData {
		public BluetoothGatt mGatt;
		public UUID mUuid;
		public byte[] mValue;
		public ChacChangedData(BluetoothGatt gatt, UUID uuid,  byte[] value){
			mValue = new byte[value.length];
			mGatt = gatt;
			mUuid = UUID.fromString(uuid.toString());
			System.arraycopy(value,0,mValue,0,value.length);
		}
	}
	final private class AttrRequest {
		int mType;
		byte[] mData;
		public AttrRequest(int type, byte[] data) {
			mType = type;
			mData = data;
		}
	}
	final private class MyConnectionCheck extends ConnectionCheck {
		public MyConnectionCheck(Context mContext) {
			super(mContext);
		}
		@Override
		public void onConnected(BluetoothDevice device) {
			sendMessageDelayed(CMD_BT_DEVICE_CONNECTED, device, TDELAY_DEVICE_CONNECTED_MS);
		}
	}
	private static class SM_Debug{
		private static String lastState;
		public static void enter(String newState) {
			AncsLog.d("StateTran: ["+lastState+"] -> ["+newState+"]");
			lastState = newState;
		}
		public static void exit(String state) {
			//AncsLog.d("StateTran: State exit:"+state);
		}
	}
}
