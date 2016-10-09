package com.ingenic.ancsclient;
import android.util.Log;

public class AncsLog {
    private final static String TAG = "AncsConsumer";
    private static boolean sEnable = false;
	static public void Enable(boolean enable) {
		sEnable = enable;
	}
	static public void v(String msg) {
		if(sEnable)
			Log.v(TAG, msg);
	}
	static public void w(String msg) {
		if(sEnable)
			Log.w(TAG, msg);
	}
	static public void e(String msg) {
		if(sEnable)
			Log.e(TAG, msg);
	}

	static public void d(String msg) {
		if (sEnable)
			Log.d(TAG, msg);
	}
}
