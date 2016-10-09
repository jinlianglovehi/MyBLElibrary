package com.ingenic.ancsclient;

public interface AncsSender {
	public static final int STATE_DISCONNECTED = 1;
	public static final int STATE_CONNECTING   = 2;
	public static final int STATE_CONNECTED    = 3;

	public void broadcastStaAddr(String addr);
	public void broadcast(int eventUid, int eventFlags, int categoryId,
						  int categoryCount, int notifUid);
	public void broadcast(int uid, String appName, String title, String message, String date,
						  String subtitle, String messageSize, boolean isEmail);
	public void broadcast(String devName, String addr, int connState);
}
