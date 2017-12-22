package com.lam.imagekit;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.StrictMode;
import android.provider.Settings;

import com.ftr.utils.FTRCallback;
import com.lam.imagekit.data.CameraParam;
import com.lam.imagekit.services.CameraBroadCtrl;
import com.lam.imagekit.utils.CrashHandler;
import com.tencent.bugly.crashreport.CrashReport;

import static com.lam.imagekit.utils.CrashHandler.GETCRASH;

/**
 * Created by Lam on 2017/11/22.
 */

public class AppContext extends Application {
    private static final String BUGLY_ID = "d2ec6c5fae";
    protected static AppContext instance;
    private CameraParam cameraParam = new CameraParam();
    public int mResolutionIndex = 3;
    private CameraBroadCtrl m_broadCtrl;
    private boolean deviceOnline;

    @Override
    public void onCreate() {
        super.onCreate();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
            StrictMode.setVmPolicy(builder.build());
        }
        instance = this;
        final SharedPreferences preferences = this.getSharedPreferences(GETCRASH,MODE_PRIVATE);
        CrashHandler.getInstance().init(this);
        if (preferences.getInt(GETCRASH,0) == 0
                || preferences.getInt(GETCRASH, 0)>3){
            AppContext.getInstance().setBugly();
        }

        m_broadCtrl = new CameraBroadCtrl();

        WifiManager manager = (WifiManager) this
                .getSystemService(Context.WIFI_SERVICE);
        multicastLock= manager.createMulticastLock("test wifi");
        multicastLock.acquire();
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
        multicastLock.release();
    }

    private WifiManager.MulticastLock multicastLock;

    public CameraBroadCtrl getBroadCtrl(){
        return m_broadCtrl;
    }
    public void setResulotionIndex(int index){
        mResolutionIndex = index;
    }

    public int getResolutionIndex(){
        return mResolutionIndex;
    }

    public static AppContext getInstance(){
        return instance;
    }
    private boolean m_alreadySetBugly;
    public void setBugly() {
        if(m_alreadySetBugly){
            return;
        }

        m_alreadySetBugly = true;
        String channel = BuildConfig.FLAVOR;
        CrashReport.UserStrategy strategy = new CrashReport.UserStrategy(this);
        if (channel!=null){
            strategy.setAppChannel(channel+ (BuildConfig.DEBUG?"-debug":"-release"));
        }
        String androidId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
        CrashReport.initCrashReport(this, BUGLY_ID, false, strategy);
        CrashReport.setUserId(androidId);
    }

    public CameraParam getCameraParam() {
        return cameraParam;
    }

    public void setDeviceOnline(boolean deviceOnline) {
        this.deviceOnline = deviceOnline;
    }

    public boolean isDeviceOnline() {
        return deviceOnline;
    }

    private FTRCallback<Boolean> m_scanResult;
    public void setScanResultCallback(FTRCallback callback){
        synchronized (this) {
            m_scanResult = callback;
        }
    }
    public FTRCallback<Boolean> getScanResultCallback(){
        synchronized (this) {
            return m_scanResult;
        }
    }
}
