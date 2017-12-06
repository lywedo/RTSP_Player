package buildwin.common;

import android.os.Handler;
import android.os.Looper;

public class CallbackHandler {

    private static boolean runInMainThread = false;
    private static boolean runInNewThread = false;
    private static Handler mainThreadHandler = new Handler(Looper.getMainLooper());
    private static Handler backgroundThreadHandler = new Handler(BackgroundThread.getLooper());

    public CallbackHandler() {
    }

    public static void setRunInMainThread(boolean b) {
        runInMainThread = b;
    }

    public static boolean getRunInMainThread() {
        return runInMainThread;
    }

    public static void post(Runnable r) {

        if(!runInNewThread) {
            if(runInMainThread) {
                mainThreadHandler.post(r);
            } else {
                backgroundThreadHandler.post(r);
            }
        } else {
            Thread thread = new Thread(r);
            thread.start();
        }
    }

    public static void setRunInNewThread(boolean b) {
        runInNewThread = b;
    }

    public static void postDelayed(Runnable r, long delayMillis) {

        if(runInMainThread) {
            mainThreadHandler.post(r);
        } else {
            backgroundThreadHandler.postDelayed(r, delayMillis);
        }
    }

}
