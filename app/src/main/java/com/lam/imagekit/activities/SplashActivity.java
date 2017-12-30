package com.lam.imagekit.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.lam.imagekit.BuildConfig;
import com.lam.imagekit.R;


/**
 * Created by Lam on 2017/12/29.
 */

public class SplashActivity extends Activity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (BuildConfig.FLAVOR.equals("imagebox")){
//            requestWindowFeature(Window.FEATURE_NO_TITLE); //设置无标题
//            getWindow().setFlags(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT);  //设置全屏
            setContentView(R.layout.activity_splash);
            handler.sendMessageDelayed(new Message(), 2000);
        }else {
            startActivity(new Intent(this, CameraActivity.class));
            finish();
        }
    }

    Handler handler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            startActivity(new Intent(SplashActivity.this, CameraActivity.class));
            finish();
        }
    };
}
