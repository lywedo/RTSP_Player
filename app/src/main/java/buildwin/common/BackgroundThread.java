package buildwin.common;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;

public class BackgroundThread {

    private static BackgroundThread sThread;
    public HandlerThread mHandlerThread = new HandlerThread("buildwin_background_thread");
    public Handler mHandler;

    public static BackgroundThread getThread() {

        if(sThread == null) {
            sThread = new BackgroundThread();
        }
        return sThread;
    }

    private BackgroundThread() {
        this.mHandlerThread.start();
        this.mHandler = new Handler(this.mHandlerThread.getLooper());
    }

    public static Looper getLooper() {
        return getThread().mHandlerThread.getLooper();
    }

    public static void post(Runnable r) {
        getThread().mHandler.post(r);
    }

    public static void postDelayed(Runnable r, long delayMillis) {
        getThread().mHandler.postDelayed(r, delayMillis);
    }

    public static void removeCallbacks(Runnable r) {
        getThread().mHandler.removeCallbacks(r);
    }

}
