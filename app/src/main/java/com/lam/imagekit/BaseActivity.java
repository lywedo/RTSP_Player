package com.lam.imagekit;

import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

import com.lam.imagekit.application.Constants;
import com.lam.imagekit.utils.CrashHandler;

/**
 * Created by Lam on 2017/11/23.
 */

public class BaseActivity extends AppCompatActivity {
    private int m_crashTick;
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState, @Nullable PersistableBundle persistentState) {
        super.onCreate(savedInstanceState, persistentState);
    }

    @Override
    protected void onStart() {
        super.onStart();
        m_crashTick = CrashHandler.getInstance().checkCrash(this);
    }

    protected boolean isBuglyShowing(){
        m_crashTick = CrashHandler.getInstance().getCrashTick(AppContext.getInstance());
        return (m_crashTick>0);
    }
    /**
     * 判断是否拥有权限
     *
     * @param permissions
     * @return
     */
    public boolean hasPermission(String... permissions) {
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED)
                return false;
        }
        return true;
    }

    /**
     * 请求权限
     */
    protected void requestPermission(int code, String... permissions) {
        ActivityCompat.requestPermissions(this, permissions, code);
    }

    /**
     * 请求权限的回调
     *
     * @param requestCode
     * @param permissions
     * @param grantResults
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
//            case Constants.CODE_CAMERA:
//                //例子：请求相机的回调
//                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
//                    // 这里写你需要的业务逻辑
//                } else {
//                }
//                break;
            case Constants.CODE_WRITE_EXTERNAL_STORAGE:
                //另一个权限的回调
                if (!(grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    Toast.makeText(this,"未授予储存读写权限", Toast.LENGTH_SHORT).show();
                }
                break;
        }
    }

    public void initSplash(){

    }
}
