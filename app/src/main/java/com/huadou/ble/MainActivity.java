package com.huadou.ble;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import huadou.testresource.ResouceUtils;

public class MainActivity extends AppCompatActivity {

     private final static String TAG = MainActivity.class.getName();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

      ResouceUtils.getStringByKey(this,"string","app_name");

    }
}
