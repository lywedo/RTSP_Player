package cn.com.buildwin.gosky.activities;

import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.provider.Settings;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.kaopiz.kprogresshud.KProgressHUD;

import java.util.HashMap;

import cn.com.buildwin.gosky.widget.bwsocket.BWSocket;
import cn.com.buildwin.gosky.R;

public class RenameSSIDActivity extends AppCompatActivity {

    private static final String TAG = "RenameSSIDActivity";

    private Button cancelButton;
    private Button saveButton;
    private EditText currentSSIDEditText;
    private EditText newEditSSIDText;

    private BWSocket asyncSocket;

    private enum WirelessAction {
        WIRELESS_ACTION_IDLE,
        WIRELESS_ACTION_PROCESSING,
        WIRELESS_ACTION_GET_INFO,
        WIRELESS_ACTION_SET_SSID,
        WIRELESS_ACTION_RESET_NET,
    }
    private WirelessAction action = WirelessAction.WIRELESS_ACTION_IDLE;

    private KProgressHUD hud;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_rename_ssid);

        // Cancel Button
        cancelButton = (Button)findViewById(R.id.rename_ssid_cancel_button);
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        // Save Button
        saveButton = (Button)findViewById(R.id.rename_ssid_save_button);
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                hud.setStyle(KProgressHUD.Style.SPIN_INDETERMINATE)
                        .setLabel(getResources().getString(R.string.rename_hud_applying_change))
                        .show();

                action = WirelessAction.WIRELESS_ACTION_SET_SSID;
                String newSSIDString = newEditSSIDText.getText().toString();
                asyncSocket.setSSID(newSSIDString);
            }
        });
        saveButton.setEnabled(false);

        // Current SSID EditText
        currentSSIDEditText = (EditText)findViewById(R.id.rename_ssid_current_ssid_editText);
        // New SSID EditText
        newEditSSIDText = (EditText)findViewById(R.id.rename_ssid_new_ssid_editText);
        newEditSSIDText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void afterTextChanged(Editable editable) {
                String newSSIDString = editable.toString();
                if (newSSIDString.length() > 0) {
                    // 相同名称则禁用保存
                    if (newEditSSIDText.getText().toString().compareTo(currentSSIDEditText.getText().toString()) == 0 ) {
                        saveButton.setEnabled(false);
                        Toast.makeText(RenameSSIDActivity.this, R.string.rename_alert_change_another_ssid, Toast.LENGTH_SHORT).show();
                    } else {
                        saveButton.setEnabled(true);
                    }
                }
                else
                    saveButton.setEnabled(false);
            }
        });

        asyncSocket = BWSocket.getInstance();
        asyncSocket.setCallback(new BWSocket.BWSocketCallback() {
            @Override
            public void didConnectToHost(String host, int port) {
                Log.d(TAG, "Callback didConnectToHost: " + host + "(" + port + ")");

            }

            @Override
            public void didDisconnectFromHost() {
                Log.d(TAG, "Callback didDisconnectFromHost");

                switch (action) {
                    case WIRELESS_ACTION_GET_INFO:
                        Toast.makeText(RenameSSIDActivity.this, R.string.rename_error_get_info, Toast.LENGTH_SHORT).show();
                        break;
                    case WIRELESS_ACTION_SET_SSID:
                        Toast.makeText(RenameSSIDActivity.this, R.string.rename_error_set_ssid, Toast.LENGTH_SHORT).show();
                        break;
                    case WIRELESS_ACTION_RESET_NET:
                        Toast.makeText(RenameSSIDActivity.this, R.string.rename_error_reset, Toast.LENGTH_SHORT).show();
                        break;
                }
                action = WirelessAction.WIRELESS_ACTION_IDLE;
                hud.dismiss();
            }

            @Override
            public void didGetInformation(HashMap<String, String> map) {
                Log.d(TAG, "Callback didGetInformation: " + map);

                hud.dismiss();

                //
                String methodString = map.get(BWSocket.kKeyMethod);
                switch (action) {
                    case WIRELESS_ACTION_GET_INFO:
                        action = WirelessAction.WIRELESS_ACTION_PROCESSING;
                        boolean got = false;
                        if (methodString != null && methodString.compareTo(BWSocket.kCommandGetInfo) == 0) {
                            String ssidString = map.get(BWSocket.kKeySSID);
                            if (ssidString != null) {
                                currentSSIDEditText.setText(ssidString);
                                got = true;
                            }
                        }
                        if (!got) {
                            Toast.makeText(RenameSSIDActivity.this, R.string.rename_alert_get_info_fail, Toast.LENGTH_SHORT).show();
                        }
                        break;
                    case WIRELESS_ACTION_SET_SSID:
                        action = WirelessAction.WIRELESS_ACTION_PROCESSING;
                        boolean set = false;
                        if (methodString != null && methodString.compareTo(BWSocket.kCommandSetSSID) == 0) {
                            String statusCodeString = map.get(BWSocket.kKeyStatusCode);
                            String ssidString = map.get(BWSocket.kKeySSID);
                            if (statusCodeString != null && statusCodeString.compareTo(BWSocket.kStatusCodeOK) == 0) {
                                if (ssidString != null) {
                                    currentSSIDEditText.setText(ssidString);

                                    set = true;

                                    // 相同名称则禁用保存
                                    if (newEditSSIDText.getText().toString().compareTo(currentSSIDEditText.getText().toString()) == 0 ) {
                                        saveButton.setEnabled(false);
                                    }

                                    /* ---- 因为现在硬件那边Reset方案有困难，所以先使用手动Reset ---- */
                                    String title = getResources().getString(R.string.rename_message_box_notice_title);
                                    String message = getResources().getString(R.string.rename_message_box_notice_message);
                                    String positiveTitle = getResources().getString(R.string.rename_message_box_notice_confirm_title);

                                    AlertDialog.Builder builder = new AlertDialog.Builder(RenameSSIDActivity.this);
                                    builder.setMessage(message);
                                    builder.setTitle(title);
                                    builder.setPositiveButton(positiveTitle, new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialogInterface, int i) {

                                            // 如果有符合的WiFi设置界面，则弹出提示
                                            String title = null;
                                            String message = null;
                                            String positiveTitle = null;
                                            String negativeTitle = null;

                                            AlertDialog.Builder builder = new AlertDialog.Builder(RenameSSIDActivity.this);
                                            DialogInterface.OnClickListener onPositiveClickListener = null;

                                            Intent intent = new Intent(Settings.ACTION_WIFI_SETTINGS);
                                            if (intent.resolveActivity(getPackageManager()) != null) {
                                                title = getResources().getString(R.string.rename_message_box_notice2_title);
                                                message = getResources().getString(R.string.rename_message_box_notice2_message);
                                                positiveTitle = getResources().getString(R.string.rename_message_box_notice2_positive_title);
                                                negativeTitle = getResources().getString(R.string.rename_message_box_notice2_negative_title);

                                                onPositiveClickListener = new DialogInterface.OnClickListener() {
                                                    @Override
                                                    public void onClick(DialogInterface dialogInterface, int i) {
                                                        // Open WiFi setting page
                                                        Intent intent = new Intent(Settings.ACTION_WIFI_SETTINGS);
                                                        if (intent.resolveActivity(getPackageManager()) != null) {
                                                            startActivity(intent);
                                                        }
                                                    }
                                                };
                                                builder.setNegativeButton(negativeTitle, null);
                                            } else {
                                                title = getResources().getString(R.string.rename_message_box_notice3_title);
                                                message = getResources().getString(R.string.rename_message_box_notice3_message);
                                                positiveTitle = getResources().getString(R.string.rename_message_box_notice3_confirm_title);

                                                onPositiveClickListener = null;
                                            }
                                            builder.setMessage(message);
                                            builder.setTitle(title);
                                            builder.setPositiveButton(positiveTitle, onPositiveClickListener);
                                            builder.create().show();
                                        }
                                    });
                                    builder.create().show();

//                                    hud.setStyle(KProgressHUD.Style.SPIN_INDETERMINATE)
//                                            .setLabel(getResources().getString(R.string.rename_hud_resetting_board))
//                                            .show();
//
//                                    // Reset board after 200ms
//                                    new Handler().postDelayed(new Runnable() {
//                                        @Override
//                                        public void run() {
//                                            action = WirelessAction.WIRELESS_ACTION_RESET_NET;
//                                            asyncSocket.resetNet();
//                                        }
//                                    }, 200);
                                }
                            }
                        }
                        if (!set) {
                            Toast.makeText(RenameSSIDActivity.this, R.string.rename_alert_set_ssid_fail, Toast.LENGTH_SHORT).show();
                        }
                        break;
                    case WIRELESS_ACTION_RESET_NET:
                        action = WirelessAction.WIRELESS_ACTION_PROCESSING;
                        if (methodString != null && methodString.compareTo(BWSocket.kCommandResetNet) == 0) {
                            String title = getResources().getString(R.string.rename_message_box_reset_title);
                            String message = getResources().getString(R.string.rename_message_box_reset_message);
                            String positiveTitle = getResources().getString(R.string.rename_message_box_positive_title);
                            String negativeTitle = getResources().getString(R.string.rename_message_box_negative_title);

                            AlertDialog.Builder builder = new AlertDialog.Builder(RenameSSIDActivity.this);
                            builder.setMessage(message);
                            builder.setTitle(title);
                            builder.setPositiveButton(positiveTitle, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    // Open WiFi setting page
                                    Intent intent = new Intent(Intent.ACTION_MAIN, null);
                                    intent.addCategory(Intent.CATEGORY_LAUNCHER);
                                    ComponentName cn = new ComponentName("com.android.settings", "com.android.settings.wifi.WifiSettings");
                                    intent.setComponent(cn);
                                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                    startActivity(intent);
                                }
                            });
                            builder.setNegativeButton(negativeTitle, null);
                            builder.create().show();
                        } else {
                            Toast.makeText(RenameSSIDActivity.this, R.string.rename_alert_reset_fail, Toast.LENGTH_SHORT).show();
                        }
                        break;
                }
            }
        });

        hud = KProgressHUD.create(RenameSSIDActivity.this);

        hud.setStyle(KProgressHUD.Style.SPIN_INDETERMINATE)
                .setLabel(getResources().getString(R.string.rename_hud_collecting_information))
                .show();
        action = WirelessAction.WIRELESS_ACTION_GET_INFO;
        asyncSocket.getInfo();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // disconnect
    }
}
