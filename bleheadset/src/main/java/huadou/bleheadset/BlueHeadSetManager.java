package huadou.bleheadset;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaRecorder;
import android.os.Environment;
import android.util.Log;

import java.io.File;

/**
 * Created by jinliang on 16-9-27.
 */
public class BlueHeadSetManager {

    private final static String TAG = BlueHeadSetManager.class.getName();
    private AudioManager _audioManager ;

    private MediaRecorder _recorder;

    private void init(Context context){
        _audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        _audioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
    }

    private void start()
    {
        BroadcastReceiver scoReceiver = new BroadcastReceiver() {

            public void onReceive(Context context, Intent intent) {

                int scoState = intent.getIntExtra(
                        AudioManager.EXTRA_SCO_AUDIO_STATE, -1);

                switch (scoState) {
                    case -1:
                        Log.d("D","SCO State: Error\n");
                        break;
                    case 0:
                        Log.d("D","SCO State: Disconnected\n");
                        break;
                    case 1:
                        Log.d("D","SCO State: Connected\n");
                        break;
                    case 2:
                        Log.d("D","SCO State: Connecting\n");
                        break;
                }
                if (scoState == AudioManager.SCO_AUDIO_STATE_CONNECTED) {
                    Log.d("D","Startrecording\n");
                    //record audio
                    File path = new File(Environment.getExternalStorageDirectory() + "/VoiceRecord");
                    if (!path.exists())
                        path.mkdirs();

//                    Log.w("BluetoothReceiver.java | startRecord", "|" + path.toString() + "|");

                    File file = null;
                    try
                    {
                        file = File.createTempFile("voice_", ".m4a", path);
                    }
                    catch (Exception e)
                    {
                        e.printStackTrace();
                    }
//                    Log.w("BluetoothReceiver.java | startRecord", "|" + file.toString() + "|");
                    //_text1.setText(file.toString());

                    try
                    {

                        _audioManager.startBluetoothSco();
                        _recorder = new MediaRecorder();
                        _recorder.setAudioSource(MediaRecorder.AudioSource.DEFAULT);
                        _recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
                        _recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
                        _recorder.setOutputFile(file.toString());
                        _recorder.prepare();
                        _recorder.start();

                        Log.i(TAG, "onReceive: 正在录音操作");
                    }
                    catch (Exception e)
                    {
                        e.printStackTrace();
                    }

                }
            }
        };

    }

    private void stop()
    {
        try
        {
            _recorder.stop();
            _recorder.release();
            _audioManager.stopBluetoothSco();
            Log.i(TAG, "stop: 停止结束");
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }
}
