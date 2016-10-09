package com.ingenic.ancsclient;

public interface AncsCallback {
	public static final int STATE_DISCONNECTED = 1;
	public static final int STATE_CONNECTING   = 2;
	public static final int STATE_CONNECTED    = 3;

	public void onConnectionStateChange(String addr, int connState);
	public void onRecvNotifyAttrib(int uid, String appName, String title,
								   String subtitle, String message, String date);
	public void onRecvNotifyCharacter(int eventId, int eventFlags, int categoryId,
									  int categoryCount, int notifUid);
}
