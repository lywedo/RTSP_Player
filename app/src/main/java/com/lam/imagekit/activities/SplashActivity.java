package com.lam.imagekit.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import com.lam.imagekit.BuildConfig;



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
//            getWindow().getDecorView().setBackgroundResource(R.mipmap.splash);
//            setContentView(R.layout.activity_splash);
//            handler.sendMessageDelayed(new Message(), 2000);
            try {
                startActivity(new Intent(this, Class.forName("com.lam.imagekit.activities.BoxCameraActivity")));
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        }else {
            startActivity(new Intent(this, CameraActivity.class));
        }
            finish();
    }

}
