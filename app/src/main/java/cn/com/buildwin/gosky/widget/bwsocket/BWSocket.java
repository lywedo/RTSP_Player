package cn.com.buildwin.gosky.widget.bwsocket;

/**
 * 使用的第三方Socket库，在测试时，设置超时后，不论设置是手动接收还是自动接收，
 * 在接收了一次数据之后，如果在超时时间内没有收到任何数据，则会自动取消接收然后
 * 断开连接。
 * 这个问题可能是bug，也可能是设计如此，不论如何，现在先做成，发送一次请求后，
 * 立即断开连接，需要发送请求之前再重新连接。
 * 本来是要做成先connect，然后在发送各种请求，最后disconnect。
 * 暂时找不到比较好用的Android第三方Socket库，先使用AndroidSocketClient。
 */

import android.support.annotation.NonNull;
import android.util.Log;

import com.vilyever.socketclient.SocketClient;
import com.vilyever.socketclient.helper.SocketClientDelegate;
import com.vilyever.socketclient.helper.SocketPacketHelper;
import com.vilyever.socketclient.helper.SocketResponsePacket;
import com.vilyever.socketclient.util.CharsetUtil;

import java.util.HashMap;

import cn.com.buildwin.gosky.application.Constants;

public class BWSocket {

    private static final String TAG = BWSocket.class.getSimpleName();

    // Key status
    public static final String kKeyProtocol             = "protocol";
    public static final String kKeyProtocolVersion      = "protocolVersion";
    public static final String kKeyStatusCode           = "statusCode";
    public static final String kKeyStatus               = "status";
    // Status code
    public static final String kStatusCodeOK            = "200";
    // Key info
    public static final String kKeyMethod               = "METHOD";
    public static final String kKeySSID                 = "SSID";
    public static final String kKeyChip                 = "CHIP";
    public static final String kKeyVendor               = "VENDOR";
    public static final String kKeyVersion              = "VERSION";
    // Method
    public static final String kCommandUndefined        = "Undefined";
    public static final String kCommandGetInfo          = "GETINFO";
    public static final String kCommandSetSSID          = "SETSSID";
    public static final String kCommandSetPassword      = "SETPW";         ///////////// TODO: modify it
    public static final String kCommandResetNet         = "RESETNET";
    public static final String kCommandRotateImage      = "ROTATEIMG";

    public static final String kCommandRecordStart      = "RECSTART";
    public static final String kCommandRecordStop       = "RECSTOP";
    // Others...
    private static final String kCommandPath            = "/webcam";
    private static final String kCommandProtocol        = "APPO";
    private static final String kCommandProtocolVersion = "1.0";

    private String host;
    private int port;
    private static final int TIMEOUT = 3000;

    private SocketClient client;
    private BWSocketCallback mCallback;

    private enum BWSocketRequest {
        BW_SOCKET_REQUEST_IDLE,
        BW_SOCKET_REQUEST_GET_INFO,
        BW_SOCKET_REQUEST_SET_SSID,
        BW_SOCKET_REQUEST_SET_PASSWORD,
        BW_SOCKET_REQUEST_RESET_NET,
        BW_SOCKET_REQUEST_ROTATE_IMAGE,
        BW_SOCKET_REQUEST_REC_START,
        BW_SOCKET_REQUEST_REC_STOP,
    }
    private BWSocketRequest request = BWSocketRequest.BW_SOCKET_REQUEST_IDLE;
    private String strRequest = null;

//    public enum BWSocketStatus {
//        BW_SOCKET_STATUS_DISCONNECTED,
//        BW_SOCKET_STATUS_CONNECTING,
//        BW_SOCKET_STATUS_CONNECTED,
//        BW_SOCKET_STATUS_GET_SSID,
//        BW_SOCKET_STATUS_SET_SSID,
//        BW_SOCKET_STATUS_SET_PASSWORD,
//        BW_SOCKET_STATUS_RESET,
//    }
//    BWSocketStatus status = BWSocketStatus.BW_SOCKET_STATUS_DISCONNECTED;

    private static BWSocket ourInstance
            = new BWSocket(Constants.SERVER_ADDRESS, Constants.SERVER_PORT);

    public static BWSocket getInstance() {
        return ourInstance;
    }

    private BWSocket(String host, int port) {
        this.host = host;
        this.port = port;
        setup();
    }

    private void setup() {
        client = new SocketClient();

        client.getAddress().setRemoteIP(host);
        client.getAddress().setRemotePortWithInteger(port);
        client.getAddress().setConnectionTimeout(TIMEOUT);

        client.setCharsetName(CharsetUtil.UTF_8);

        client.registerSocketClientDelegate(new SocketClientDelegate() {
            @Override
            public void onConnected(SocketClient client) {
                Log.d(TAG, "onConnected");

                // Recall sendRequest when connected
                switch (request) {
                    case BW_SOCKET_REQUEST_GET_INFO:
                    case BW_SOCKET_REQUEST_SET_SSID:
                    case BW_SOCKET_REQUEST_SET_PASSWORD:
                    case BW_SOCKET_REQUEST_RESET_NET:
                    case BW_SOCKET_REQUEST_ROTATE_IMAGE:
                    case BW_SOCKET_REQUEST_REC_START:
                    case BW_SOCKET_REQUEST_REC_STOP:
                        sendRequest(strRequest);
                        break;
                }

                // Callback didConnectToHost
                if (mCallback != null) {
                    mCallback.didConnectToHost(host, port);
                }
            }

            @Override
            public void onDisconnected(SocketClient client) {
                Log.d(TAG, "onDisconnected");

                // Callback didConnectToHost
                if (mCallback != null) {
                    mCallback.didDisconnectFromHost();
                }
            }

            @Override
            public void onResponse(SocketClient client, @NonNull SocketResponsePacket responsePacket) {
                if (responsePacket != null) {   // NonNull, but sometimes it's null, add if
                    String responseString = responsePacket.getMessage();
                    Log.d(TAG, "onResponse: " + responseString);

                    request = BWSocketRequest.BW_SOCKET_REQUEST_IDLE;
                    client.disconnect();

                    HashMap<String, String> map = parseResponseString(responseString);  // Callback
                    if (mCallback != null) {
                        mCallback.didGetInformation(map);
                    }
                }
            }
        });

//        client.getSocketPacketHelper().setReadStrategy(SocketPacketHelper.ReadStrategy.AutoReadToTrailer);
//        client.getSocketPacketHelper().setReceiveTrailerData(new byte[]{'\r', '\n', '\r', '\n'});
        client.getSocketPacketHelper().setReadStrategy(SocketPacketHelper.ReadStrategy.Manually);

        client.getSocketPacketHelper().setSendTimeout(TIMEOUT);
        client.getSocketPacketHelper().setSendTimeoutEnabled(true);
        client.getSocketPacketHelper().setReceiveTimeout(TIMEOUT);
        client.getSocketPacketHelper().setReceiveTimeoutEnabled(true);

//        client.connect();
    }

    public void connect() {
        if (!client.isConnected()) {
            client.connect();
        }
    }

    public void disconnect() {
        if (client.isConnected()) {
            client.disconnect();
        }
    }

    /**
     * Request
     */

    private void sendRequest(String req) {
        if (!client.isConnected()) {
            client.connect();
        }
        else {
            try {
                client.sendString(req);
                // 设备那边如果因为某个问题没法回复，会出现异常
                client.readDataToData(new byte[]{'\r', '\n', '\r', '\n'}, true);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    // ---------------------------------------------

    private String baseCommandWithRequest(BWSocketRequest request) {
        String strCommand;
        String strPath = kCommandPath;
        String strProtocol = kCommandProtocol + "/" + kCommandProtocolVersion;

        switch (request) {
            case BW_SOCKET_REQUEST_GET_INFO:
                strCommand = kCommandGetInfo;
                break;
            case BW_SOCKET_REQUEST_SET_SSID:
                strCommand = kCommandSetSSID;
                break;
            case BW_SOCKET_REQUEST_SET_PASSWORD:
                strCommand = kCommandSetPassword;
                break;
            case BW_SOCKET_REQUEST_RESET_NET:
                strCommand = kCommandResetNet;
                break;
            case BW_SOCKET_REQUEST_ROTATE_IMAGE:
                strCommand = kCommandRotateImage;
                break;
            case BW_SOCKET_REQUEST_REC_START:
                strCommand = kCommandRecordStart;
                break;
            case BW_SOCKET_REQUEST_REC_STOP:
                strCommand = kCommandRecordStop;
                break;

            default:
                strCommand = kCommandUndefined;
                break;
        }

        return strCommand + " " + strPath + " " + strProtocol;
    }

    private String appendingTerminalSign(String string) {
        return string + "\r\n\r\n";
    }

    private void makeRequestAndSend(BWSocketRequest request) {
        strRequest = baseCommandWithRequest(request);
        strRequest = appendingTerminalSign(strRequest);
        sendRequest(strRequest);
    }

    // ----------------- Command -----------------------

    public void getInfo() {
        request = BWSocketRequest.BW_SOCKET_REQUEST_GET_INFO;
        makeRequestAndSend(request);
    }

    public void setSSID(String ssid) {
        request = BWSocketRequest.BW_SOCKET_REQUEST_SET_SSID;
        strRequest = baseCommandWithRequest(request);
        strRequest += ("\r\nssid:" + ssid);
        strRequest = appendingTerminalSign(strRequest);
        sendRequest(strRequest);
    }

    public void setPassword(String password) {
        request = BWSocketRequest.BW_SOCKET_REQUEST_SET_PASSWORD;
        //
    }

    public void resetNet() {
        request = BWSocketRequest.BW_SOCKET_REQUEST_RESET_NET;
        makeRequestAndSend(request);
    }

    public void rotateImage() {
        request = BWSocketRequest.BW_SOCKET_REQUEST_ROTATE_IMAGE;
        makeRequestAndSend(request);
    }

    public void recordStart() {
        request = BWSocketRequest.BW_SOCKET_REQUEST_REC_START;
        makeRequestAndSend(request);
    }

    public void recordStop() {
        request = BWSocketRequest.BW_SOCKET_REQUEST_REC_STOP;
        makeRequestAndSend(request);
    }

    /**
     * Parse response
     * @param responseString
     * @return
     */
    private HashMap<String, String> parseResponseString(String responseString) {
        HashMap<String, String> map = new HashMap<String, String>();
        if (responseString != null) {
            String[] stringArray = responseString.split("\\r\\n");  // regex

            // Parse status
            if (stringArray.length >= 1) {
                String statusHeaderString = stringArray[0];
                String[] statusArray = statusHeaderString.split(" ");
                if (statusArray.length >= 3) {
                    String protocolVersioString = statusArray[0];
                    String statusCodeString = statusArray[1];

                    // Parse protocol version
                    String[] protocolVersionArray = protocolVersioString.split("/");
                    if (protocolVersionArray.length == 2) {
                        map.put("protocol", protocolVersionArray[0]);
                        map.put("protocolVersion", protocolVersionArray[1]);
                    }

                    map.put("statusCode", statusCodeString);

                    String statusString = "";
                    for (int i = 2; i < statusArray.length; i++) {
                        if (i == statusArray.length - 1)
                            statusString += statusArray[i];
                        else
                            statusString += (statusArray[i] + " ");
                    }
                    map.put("status", statusString);
                }
            }

            if (stringArray.length >= 2) {
                String methodString = stringArray[1];

                // Parse Method
                methodString = methodString.replace(" ", "");
                String[] methodArray = methodString.split(":");
                map.put(methodArray[0], methodArray[1]);
            }

            for (int i = 2; i < stringArray.length; i++) {
                String infoString = stringArray[i];

                // Parse Info
                infoString = infoString.replace(" ", "");
                String[] infoArray = infoString.split(":");
                map.put(infoArray[0], infoArray[1]);
            }
        }

        return map;
    }

    public interface BWSocketCallback {
        void didConnectToHost(String host, int port);
        void didDisconnectFromHost();
        void didGetInformation(HashMap<String, String> map);
    }

    public void setCallback(BWSocketCallback callback) {
        mCallback = callback;
    }
}
