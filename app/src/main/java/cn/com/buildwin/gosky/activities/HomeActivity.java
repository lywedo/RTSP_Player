package cn.com.buildwin.gosky.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;

import cn.com.buildwin.gosky.R;

public class HomeActivity extends AppCompatActivity {

    private ImageButton mHelpButton;
    private ImageButton mSettingButton;
    private ImageButton mPlayButton;

    private static boolean showOnce = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        // When the compile and target version is higher than 22, please request the
        // following permissions at runtime to ensure the SDK work well
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Request if permissions not granted
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{
                                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        },
                        1);
            }
        }

        setContentView(R.layout.activity_home);

        // Help Button
        mHelpButton = (ImageButton)this.findViewById(R.id.home_help_button);
        mHelpButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 显示帮助界面
                Intent i = new Intent(HomeActivity.this, HelpActivity.class);
                startActivity(i);
            }
        });

        // Setting Button
        mSettingButton = (ImageButton)this.findViewById(R.id.home_setting_button);
        mSettingButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 显示设置界面
                Intent i = new Intent(HomeActivity.this, SettingActivity.class);
                startActivity(i);
            }
        });

        // Play Button
        mPlayButton = (ImageButton)this.findViewById(R.id.home_play_button);
        mPlayButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 显示控制面板
                Intent i = new Intent(HomeActivity.this, ControlPanelActivity.class);
                startActivity(i);
                // Activity slide from left
                overridePendingTransition(
                        android.R.anim.slide_in_left,
                        android.R.anim.slide_out_right
                );
            }
        });
    }

}
