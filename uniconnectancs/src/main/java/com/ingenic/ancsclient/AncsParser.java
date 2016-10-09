package com.ingenic.ancsclient;
import java.util.Date;

public class AncsParser {
	private static final int COMMANDID_GETNOTIFICATION_ATTRIBUITES          = 0;
	private static final int COMMANDID_GETAPP_ATTRIBUITES                   = 1;
	private static final int COMMANDID_PERFORM_NOTIFICATION_ACTION          = 2;

	private static final int NOTIFICATION_ATTRIBUITE_ID_APPIDENTIFIER       = 0;
	private static final int NOTIFICATION_ATTRIBUITE_ID_TITLE               = 1;
	private static final int NOTIFICATION_ATTRIBUITE_ID_SUBTITLE            = 2;
	private static final int NOTIFICATION_ATTRIBUITE_ID_MESSAGE             = 3;
	private static final int NOTIFICATION_ATTRIBUITE_ID_MESSAGESIZE         = 4;
	private static final int NOTIFICATION_ATTRIBUITE_ID_DATE                = 5;
	private static final int NOTIFICATION_ATTRIBUITE_ID_POSITIVEACTIONLABLE = 6;
	private static final int NOTIFICATION_ATTRIBUITE_ID_NEGATIVEACTIONLABLE = 7;

	public static final int EVENTID_NOTIFICATION_ADDED    = 0;
	public static final int EVENTID_NOTIFICATION_MODIFIED = 1;
	public static final int EVENTID_NOTIFICATION_REMOVED  = 2;

	public static final byte EVENT_FLAG_SILENT             = (1 << 0);
	public static final byte EVENT_FLAG_IMPORTANT          = (1 << 1);
	public static final byte EVENT_FLAG_PRE_EXISTING       = (1 << 2);
	public static final byte EVENT_FLAG_POSITIVE_ACTION    = (1 << 3);
	public static final byte EVENT_FLAG_NEGATIVE_ACTION    = (1 << 4);

	private static final int APPID_CAP      = 31;
	private static final int TITLE_CAP      = 31;
	private static final int SUBTITLE_CAP   = 31;
	private static final int MSG_CAP        = 511;
	private static final int MSG_SIZE_CAP   = 7;
	private static final int DATE_CAP       = 31;

	public static final int STATE_DATA_CONTINUE = 0;
	public static final int STATE_DATA_DONE     = 1;
	public static final int STATE_DATA_ERROR    = 2;

	private static final int DATA_BUF_BUF_SIZE   = 4096;
	private static final int MAX_PARTS_TOUT_MS   = 1000;

	private static byte[] sRequestData;
	private static Notification sNotify;
	private static Detail sDetail = new Detail();

	private static byte[] sDataBuf;
	private static int BufLen;
	private static boolean isDataContinue;

	private static Date last_tv = new Date();

	public static Notification ParseNotification(byte[] notify) {
		sNotify = new Notification();
		sNotify.EventID        = notify[0];
		sNotify.EventFlags     = notify[1];
		sNotify.CategoryID     = notify[2];
		sNotify.CategoryCount  = notify[3];
		sNotify.NotificationID = ((notify[4]<< 0)&0x000000FF) +
                                 ((notify[5]<< 8)&0x0000FF00) +
                                 ((notify[6]<<16)&0x00FF0000) +
                                 ((notify[7]<<24)&0xFF000000) ;
		return sNotify;
	}
	public static byte[] GetPerformData(int uid, byte action) {
		byte[] data = new byte[32];
		data[0] = COMMANDID_PERFORM_NOTIFICATION_ACTION;
		int2array(data, 1, uid);
		data[5] = action;
		return data;
	}

	public static byte[] GetRequsetData(Notification notify) {
		byte[] data = new byte[32];
		int len = 0;
		data[len] = NOTIFICATION_ATTRIBUITE_ID_APPIDENTIFIER;
		len += 1;
		len += int2array(data, len, notify.NotificationID);
		data[len] = NOTIFICATION_ATTRIBUITE_ID_TITLE;
		len += 1;
		len += short2array(data, len, TITLE_CAP);
		data[len] = NOTIFICATION_ATTRIBUITE_ID_SUBTITLE;
		len += 1;
		len += short2array(data, len, SUBTITLE_CAP);
		data[len] = NOTIFICATION_ATTRIBUITE_ID_DATE;
		len += 1;
		len += short2array(data, len, DATE_CAP);
		data[len] = NOTIFICATION_ATTRIBUITE_ID_MESSAGE;
		len += 1;
		len += short2array(data, len, MSG_CAP);
		sRequestData = new byte[len];
		System.arraycopy(data, 0, sRequestData, 0, len);
		return sRequestData;
	}
	public static Detail ParseData(byte[] data) {
		int attrLen, idx, res;
		byte commId;
		byte attrId;
		Date new_tv = new Date();
		sDetail.isValid = false;
		if(!isDataContinue || (new_tv.getTime()-last_tv.getTime())>MAX_PARTS_TOUT_MS){
			sDataBuf = new byte[DATA_BUF_BUF_SIZE];
			BufLen = 0;
		}
		last_tv = new_tv;
		if(BufLen+data.length > DATA_BUF_BUF_SIZE){
			AncsLog.w("Parser:Message structure error");
			return sDetail; /* invalid */
		}
		System.arraycopy(data, 0, sDataBuf, BufLen, data.length);
		BufLen += data.length;

		res = checkIntegral(sDataBuf, BufLen);
		if(res == STATE_DATA_CONTINUE){
			//AncsLog.v("Parser: checkIntegral - STATE_DATA_CONTINUE");
			isDataContinue = true;
			return sDetail; /* invalid */
		}else if(res == STATE_DATA_ERROR){
			AncsLog.w("Parser:Message checkIntegral error");
			return sDetail; /* invalid */
		}

		idx = 0;
		commId = sDataBuf[idx];
		idx++;
		if(commId != COMMANDID_GETNOTIFICATION_ATTRIBUITES){
			AncsLog.w("Parser:Message structure error");
			return sDetail; /* invalid */
		}
		sDetail.NotificationID = array2int(sDataBuf, idx);
		idx += 4;
		while(true){
			if(BufLen-idx > 0){
				attrId = sDataBuf[idx];
				idx += 1;
				attrLen = array2short(sDataBuf, idx);
				idx += 2;
				switch(attrId){
					case NOTIFICATION_ATTRIBUITE_ID_APPIDENTIFIER :
						if(attrLen > APPID_CAP){
							return sDetail; /* invalid */
						}
						sDetail.appId = byte2string(sDataBuf, idx, attrLen);
						break;
					case NOTIFICATION_ATTRIBUITE_ID_TITLE :
						if(attrLen > TITLE_CAP){
							return sDetail; /* invalid */
						}
						sDetail.title = byte2string(sDataBuf, idx, attrLen);
						break;
					case NOTIFICATION_ATTRIBUITE_ID_SUBTITLE :
						if(attrLen > SUBTITLE_CAP){
							return sDetail; /* invalid */
						}
						sDetail.subtitle = byte2string(sDataBuf, idx, attrLen);
						break;
					case NOTIFICATION_ATTRIBUITE_ID_DATE :
						if(attrLen > DATE_CAP){
							return sDetail; /* invalid */
						}
						sDetail.date = byte2string(sDataBuf, idx, attrLen);
						break;
					case NOTIFICATION_ATTRIBUITE_ID_MESSAGE :
						if(attrLen > MSG_CAP){
							return sDetail; /* invalid */
						}
						sDetail.message = byte2string(sDataBuf, idx, attrLen);
						break;
					default :
						idx += attrLen;
						break;
				}
				idx += attrLen;
			}else if(BufLen-idx == 0){
				sDetail.isValid = true;
				isDataContinue = false;
				return sDetail;
			}else{
				AncsLog.w("Parser:Message structure error:");
				return sDetail; /* invalid */
			}
		}
	}
	public static class Detail {
		boolean isValid;
		int NotificationID;
		String appId;
		String title;
		String subtitle;
		String message;
		String date;
	}
	public static class Notification {
		public byte EventID;
		public byte EventFlags;
		public byte CategoryID;
		public byte CategoryCount;
		public int  NotificationID;
	}
	private static String byte2string(byte[] source, int pos, int len) {
		byte[] strByte = new byte[len];
		System.arraycopy(source, pos, strByte, 0, len);
		return new String(strByte);
	}
	private static int array2short(byte[] source, int pos) {
		return    ((source[pos+0]<< 0)&0x000000FF) +
                  ((source[pos+1]<< 8)&0x0000FF00) ;
	}
	private static int array2int(byte[] source, int pos) {
		return    ((source[pos+0]<< 0)&0x000000FF) +
                  ((source[pos+1]<< 8)&0x0000FF00) +
                  ((source[pos+2]<<16)&0x00FF0000) +
                  ((source[pos+3]<<24)&0xFF000000) ;
	}
	private static int short2array(byte[] source, int pos, int shortNum) {
		source[pos]   = (byte)(shortNum >> 0);
		source[pos+1] = (byte)(shortNum >> 8);
		return 2;
	}
	private static int int2array(byte[] source, int pos, int intNum) {
		source[pos]   = (byte)(intNum >> 0);
		source[pos+1] = (byte)(intNum >> 8);
		source[pos+2] = (byte)(intNum >>16);
		source[pos+3] = (byte)(intNum >>24);
		return 4;
	}
	private static int checkIntegral(byte[] data, int len) {
		int idx = 0;
		int attrLen;
		byte attrId;
		if(len-idx< 1+4 || len > DATA_BUF_BUF_SIZE){
			return STATE_DATA_ERROR;
		}
		if(data[0] != COMMANDID_GETNOTIFICATION_ATTRIBUITES){
			return STATE_DATA_ERROR;
		}
		idx += 5;
		while(true){
			if(len-idx < 1+2){
				return STATE_DATA_CONTINUE;
			}
			attrId = data[idx];
			idx += 1;
			attrLen = array2short(data, idx);
			idx += 2;
			switch(attrId){
				case NOTIFICATION_ATTRIBUITE_ID_APPIDENTIFIER :
					if(attrLen > APPID_CAP){
						return STATE_DATA_ERROR;
					}
					if(attrLen > len-idx){
						return STATE_DATA_CONTINUE;
					}
					idx += attrLen;
					break;
				case NOTIFICATION_ATTRIBUITE_ID_TITLE :
					if(attrLen > TITLE_CAP){
						return STATE_DATA_ERROR;
					}
					if(attrLen > len-idx){
						return STATE_DATA_CONTINUE;
					}
					idx += attrLen;
					break;
				case NOTIFICATION_ATTRIBUITE_ID_SUBTITLE :
					if(attrLen > SUBTITLE_CAP){
						return STATE_DATA_ERROR;
					}
					if(attrLen > len-idx){
						return STATE_DATA_CONTINUE;
					}
					idx += attrLen;
					break;
				case NOTIFICATION_ATTRIBUITE_ID_DATE :
					if(attrLen > DATE_CAP){
						return STATE_DATA_ERROR;
					}
					if(attrLen > len-idx){
						return STATE_DATA_CONTINUE;
					}
					idx += attrLen;
					break;
				case NOTIFICATION_ATTRIBUITE_ID_MESSAGESIZE :
					if(attrLen > MSG_SIZE_CAP){
						return STATE_DATA_ERROR;
					}
					if(attrLen > len-idx){
						return STATE_DATA_CONTINUE;
					}
					idx += attrLen;
					break;
				case NOTIFICATION_ATTRIBUITE_ID_MESSAGE :
					if(attrLen > MSG_CAP){
						return STATE_DATA_ERROR;
					}
					if(attrLen > len-idx){
						return STATE_DATA_CONTINUE;
					}
					idx += attrLen;
					if(len-idx== 0){
						return STATE_DATA_DONE;
					}else if(len-idx> 0){
						return STATE_DATA_CONTINUE;
					}else{
						return STATE_DATA_ERROR;
					}
				default :
					idx += attrLen;
					break;
			}
		}
	}
}
