package cn.com.buildwin.gosky.widget.bwsocket;

import android.util.Log;

import java.util.HashMap;

import buildwin.common.BWCommonCallbacks;

public class BWSocketWrapper implements BWSocket.BWSocketCallback {

    private static final String TAG = BWSocketWrapper.class.getSimpleName();

    /* Enums */

    private static final int REQUEST_START_RECORD   = 1;
    private static final int REQUEST_STOP_RECORD    = 2;

    /* Members */

    private BWSocket mBWSocket;
    private boolean mConnected;

    private int mRequestCode;
    private boolean mRequestOK;

    private BWCommonCallbacks.BWCompletionCallback mCompletionCallback;

    /* Singleton */

    private static final BWSocketWrapper ourInstance = new BWSocketWrapper();

    public static BWSocketWrapper getInstance() {
        return ourInstance;
    }

    private BWSocketWrapper() {
        mBWSocket = BWSocket.getInstance();
        mBWSocket.setCallback(this);
    }

    /* Methods */

    /**
     * 开始录像
     */
    public void startRecord(BWCommonCallbacks.BWCompletionCallback completionCallback) {
        doRequest(REQUEST_START_RECORD, completionCallback);
    }

    /**
     * 停止录像
     */
    public void stopRecord(BWCommonCallbacks.BWCompletionCallback completionCallback) {
        doRequest(REQUEST_STOP_RECORD, completionCallback);
    }

    /**
     * 执行请求
     */
    private void doRequest(int requestCode, BWCommonCallbacks.BWCompletionCallback completionCallback) {
        Log.d(TAG, "doRequest: " + requestCode);

        mCompletionCallback = completionCallback;
        mRequestCode = requestCode;

        mBWSocket.connect();
    }

    /* Callback */

    @Override
    public void didConnectToHost(String host, int port) {
        mConnected = true;

        // 执行相应的动作
        switch (mRequestCode) {
            case REQUEST_START_RECORD:
                Log.d(TAG, "didConnectToHost: Do remote start record");
                mBWSocket.recordStart();
                break;
            case REQUEST_STOP_RECORD:
                Log.d(TAG, "didConnectToHost: Do remote stop record");
                mBWSocket.recordStop();
                break;

            default:
                // 断开连接
                Log.d(TAG, "didConnectToHost: No request, disconnect");
                mBWSocket.disconnect();
        }
    }

    @Override
    public void didDisconnectFromHost() {
        Log.d(TAG, "didDisconnectFromHost: connected(" + mConnected + "), requestOK(" + mRequestOK + ")");

        if (mConnected) {
            // 网络已经连接，判断动作有没有执行成功
            if (mRequestOK) {
                // 动作执行成功
                if (mCompletionCallback != null)
                    mCompletionCallback.onResult(null);
            } else {
                // 动作执行失败
                if (mCompletionCallback != null)
                    mCompletionCallback.onResult(BWSocketError.INFORMATION_FETCH_FAILED);
            }
        } else {
            // 如果没有成功Connected就进入到这里，说明网络没有连接上
            if (mCompletionCallback != null)
                mCompletionCallback.onResult(BWSocketError.SOCKET_CONNECTION_FAILED);
        }

        // 重置状态
        mConnected = false;
        mRequestOK = false;
        mCompletionCallback = null;
    }

    @Override
    public void didGetInformation(HashMap<String, String> map) {
        Log.d(TAG, "didGetInformation: " + map);

        String statusCode = map.get(BWSocket.kKeyStatusCode);
        String method = map.get(BWSocket.kKeyMethod);
        String desiredMethod = null;

        // 如果状态码存在，且等于200
        if (statusCode != null && statusCode.equalsIgnoreCase(BWSocket.kStatusCodeOK)) {

            switch (mRequestCode) {
                case REQUEST_START_RECORD:
                    desiredMethod = BWSocket.kCommandRecordStart;
                    break;
                case REQUEST_STOP_RECORD:
                    desiredMethod = BWSocket.kCommandRecordStop;
                    break;
            }

            // 如果返回符合预期
            if (method.equalsIgnoreCase(desiredMethod))
                mRequestOK = true;
        }

        // 断开连接
        mBWSocket.disconnect();
    }

}
