package com.lam.imagekit.utils;

import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Looper;
import android.support.v7.app.AlertDialog;
import android.widget.Toast;

import com.lam.imagekit.AppContext;
import com.lam.imagekit.BuildConfig;
import com.lam.imagekit.R;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;

import static android.content.Context.MODE_PRIVATE;

/**
 * Created by Lam on 2017/7/22.
 */

public class CrashHandler implements Thread.UncaughtExceptionHandler {
    public static final String TAG = "CrashHandler";
    public static final String GETCRASH = "crash";

    private Thread.UncaughtExceptionHandler mDefaultHandler;

    private static CrashHandler INSTANCE = new CrashHandler();
    private Context mContext;

    private CrashHandler() {
    }

    public static CrashHandler getInstance() {
        return INSTANCE;
    }

    public void init(Context context) {
        mContext = context;
        mDefaultHandler = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(this);
    }


    @Override
    public void uncaughtException(Thread thread, Throwable ex) {
        if (!handleException(ex) && mDefaultHandler != null) {
            mDefaultHandler.uncaughtException(thread, ex);
        } else {
            if (!BuildConfig.DEBUG) {

                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                }
                //退出程序
                android.os.Process.killProcess(android.os.Process.myPid());
                System.exit(1);
            }
        }
    }

    private boolean handleException(final Throwable ex) {
        if (ex == null) {
            return false;
        }
        //使用Toast来显示异常信息
        new Thread() {
            @Override
            public void run() {
                Looper.prepare();
                if (BuildConfig.DEBUG){
				    Writer result = new StringWriter();
			        PrintWriter printWriter = new PrintWriter(result);
			        ex.printStackTrace(printWriter);
			        printWriter.close();
                    Toast.makeText(mContext, result.toString(), Toast.LENGTH_LONG).show();
//                    Intent intent = new Intent(AppContext.getInstance(),DebugCrashActivity.class);
//                    intent.putExtra("err",result.toString());
//                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//                    AppContext.getInstance().startActivity(intent);
                }else{
                    Toast.makeText(mContext, AppContext.getInstance().getString(R.string.something_err), Toast.LENGTH_LONG).show();

                }
                SharedPreferences sharedPreferences = AppContext.getInstance().getSharedPreferences(GETCRASH, MODE_PRIVATE);
                int crashTick = sharedPreferences.getInt(GETCRASH, 0) + 1;
                sharedPreferences.edit().putInt(GETCRASH, crashTick).commit();
                Looper.loop();
            }
        }.start();

        return true;
    }

    public int getCrashTick(Context context){
         SharedPreferences preferences = context.getSharedPreferences(GETCRASH,MODE_PRIVATE);
        int tick = preferences.getInt(GETCRASH,0);

        return tick;
    }

    public int checkCrash(Context context){
        final SharedPreferences preferences = context.getSharedPreferences(GETCRASH,MODE_PRIVATE);
        int tick = preferences.getInt(GETCRASH,0);
        if (tick>0){
            final AlertDialog.Builder builder = new AlertDialog.Builder(context)
                    .setTitle(context.getString(R.string.Reminder)).setMessage(context.getString(R.string.send_err))
                    .setNegativeButton(context.getString(R.string.send), new DialogInterface.OnClickListener() {

                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            AppContext.getInstance().setBugly();
                            preferences.edit().putInt(GETCRASH,0).commit();
                            dialogInterface.dismiss();
                        }
                    }).setPositiveButton(context.getString(R.string.no_send), new DialogInterface.OnClickListener() {

                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            AppContext.getInstance().deleteDatabase("db_bugly");
                            AppContext.getInstance().setBugly();
                            preferences.edit().putInt(GETCRASH,0).commit();
                            dialogInterface.dismiss();
                        }
                    });
            builder.create().show();
//            final SweetAlertDialog dialog = new SweetAlertDialog(context);
//            dialog.setTitleText(context.getString(R.string.Reminder));
//            dialog.setContentText(context.getString(R.string.send_err));
//            dialog.setConfirmText(context.getString(R.string.send));
//            dialog.setConfirmClickListener(new SweetAlertDialog.OnSweetClickListener() {
//                @Override
//                public void onClick(SweetAlertDialog sweetAlertDialog) {
//                    AppContext.getInstance().setBugly();
//                    preferences.edit().putInt(GETCRASH,0).commit();
//                    dialog.dismiss();
//                }
//            });
//            dialog.setCancelText(context.getString(R.string.no_send));
//            dialog.setCancelClickListener(new SweetAlertDialog.OnSweetClickListener() {
//                @Override
//                public void onClick(SweetAlertDialog sweetAlertDialog) {
//                    AppContext.getInstance().deleteDatabase("db_bugly");
//                    AppContext.getInstance().setBugly();
//                    preferences.edit().putInt(GETCRASH,0).commit();
//                    dialog.dismiss();
//                }
//            });
//            dialog.show();
        }

        return tick;
    }
}
