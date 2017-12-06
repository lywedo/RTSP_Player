package cn.com.buildwin.gosky.widget.freespacemonitor;

import android.util.Log;

import java.util.Timer;
import java.util.TimerTask;

import buildwin.common.Utilities;

public class FreeSpaceMonitor {

    private Timer mTimer;

    private static final long DEFAULT_THRESHOLD = 10 * 1024 * 1024;
    private static final long MIN_THRESHOLD = 10 * 1024 * 1024;
    private static final long DEFAULT_PERIOD = 1000;
    private static final long MIN_PERIOD = 500;

    private long mThreshold = DEFAULT_THRESHOLD;
    private long mPeriod = DEFAULT_PERIOD;

    private FreeSpaceCheckerListener mListener;

    // Constructor

    public FreeSpaceMonitor() {
    }

    public FreeSpaceMonitor(long threshold, long period) {
        mThreshold = threshold;
        mPeriod = period;
    }

    // public

    public void start() {
        stop();

        TimerTask task = new FreeSpaceCheckerTimerTask();
        mTimer = new Timer();
        mTimer.schedule(task, 0, mPeriod);
    }

    public void stop() {
        if (mTimer != null) {
            mTimer.cancel();
            mTimer = null;
        }
    }

    public boolean checkFreeSpace() {
//        Log.w("@@@", "" + Utilities.getFreeDiskSpace());
        return Utilities.getFreeDiskSpace() > mThreshold;
    }

    // Inner class

    private class FreeSpaceCheckerTimerTask extends TimerTask {
        @Override
        public void run() {
            if (!checkFreeSpace()) {
                if (mListener != null)
                    mListener.onExceed();
            }
        }
    }

    // Listener

    public interface FreeSpaceCheckerListener {
        void onExceed();
    }

    // Getter and setter

    public long getThreshold() {
        return mThreshold;
    }

    public void setThreshold(long threshold) {
        if (threshold < MIN_THRESHOLD)
            mThreshold = MIN_THRESHOLD;
        else
            mThreshold = threshold;
    }

    public long getPeriod() {
        return mPeriod;
    }

    public void setPeriod(long period) {
        if (period < MIN_PERIOD)
            mPeriod = MIN_PERIOD;
        else
            mPeriod = period;
    }

    public void setListener(FreeSpaceCheckerListener listener) {
        mListener = listener;
    }
}
