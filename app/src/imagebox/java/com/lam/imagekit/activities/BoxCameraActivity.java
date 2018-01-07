package com.lam.imagekit.activities;

import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.lam.imagekit.AppContext;
import com.lam.imagekit.R;

/**
 * Created by Lam on 2018/1/6.
 */

public class BoxCameraActivity extends CameraActivity {
    @Override
    public void initSplash() {
        if (AppContext.getInstance().splashed){
            return;
        }
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        new Thread(new Runnable() {
            @Override
            public void run() {
                SplashFragment splashFragment = new SplashFragment();

                getSupportFragmentManager().beginTransaction().add(R.id.rl_main, splashFragment).commitAllowingStateLoss();
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                getSupportFragmentManager().beginTransaction().remove(splashFragment).commitAllowingStateLoss();
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
            }
        }).start();
    }
    public static class SplashFragment extends Fragment{

        @Nullable
        @Override
        public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
            AppContext.getInstance().splashed = true;
            return inflater.inflate(R.layout.activity_splash, container, false);
        }
    }

}
