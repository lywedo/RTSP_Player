package com.lam.imagekit.activities;

import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.lam.imagekit.AppContext;
import com.lam.imagekit.R;

import java.lang.ref.WeakReference;

/**
 * Created by Lam on 2018/1/6.
 */

public class BoxCameraActivity extends CameraActivity {
    private SplashBoxFragment mSplashBoxFragment;

    @Override
    public void initSplash() {
        if (AppContext.getInstance().splashed){
            return;
        }

        mSplashBoxFragment = new SplashBoxFragment();
        mSplashBoxFragmentWeak = new WeakReference<SplashBoxFragment>(mSplashBoxFragment);
        getSupportFragmentManager().beginTransaction().add(R.id.rl_main, mSplashBoxFragment).commitAllowingStateLoss();

        splashHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                getSupportFragmentManager().beginTransaction().remove(mSplashBoxFragmentWeak.get()).commitAllowingStateLoss();
            }
        },3000 );
    }
    private WeakReference<SplashBoxFragment> mSplashBoxFragmentWeak;

    Handler splashHandler = new Handler();
    public static class SplashBoxFragment extends Fragment{

        @Nullable
        @Override
        public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
            AppContext.getInstance().splashed = true;
            View view = inflater.inflate(R.layout.activity_splash, container, false);
            checkLogo(view, this.getResources().getConfiguration().orientation);
            return view;
        }

        @Override
        public void onConfigurationChanged(Configuration newConfig) {
            super.onConfigurationChanged(newConfig);
            checkLogo(this.getView(), newConfig.orientation);

        }

        private void checkLogo(View view, int orientation){
            ImageView imageView = view.findViewById(R.id.iv_sp);
            if(orientation == Configuration.ORIENTATION_LANDSCAPE){
                imageView.setImageResource(R.mipmap.splash_land);
            }else{
                imageView.setImageResource(R.mipmap.splash);
            }
        }
    }

}
