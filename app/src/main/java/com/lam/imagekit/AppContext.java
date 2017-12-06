package com.lam.imagekit;

import android.app.Application;
import android.os.Build;
import android.os.StrictMode;

/**
 * Created by Lam on 2017/11/22.
 */

public class AppContext extends Application {
    protected static AppContext instance;
    @Override
    public void onCreate() {
        super.onCreate();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
            StrictMode.setVmPolicy(builder.build());
        }
        instance = this;
    }

    public static AppContext getInstance(){
        return instance;
    }
}
