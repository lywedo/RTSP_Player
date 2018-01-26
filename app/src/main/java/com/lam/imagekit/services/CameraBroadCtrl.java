package com.lam.imagekit.services;

import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;

import com.ftr.message.CameraBroadMessageProtos;
import com.ftr.message.MediaDeviceMessageProtos;
import com.ftr.utils.FTRCallback;
import com.google.protobuf.InvalidProtocolBufferException;
import com.lam.imagekit.AppContext;
import com.lam.imagekit.application.Constants;
import com.lam.imagekit.data.CameraParam;
import com.lam.imagekit.data.StreamParam;
import com.lam.imagekit.services.utils.MediaDeviceParse;
import com.lam.imagekit.utils.CameraBroadCtrlHelper;
import com.lam.imagekit.utils.ConnectUtils;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import static com.ftr.message.CameraBroadMessageProtos.CameraBroadMessage.MsgType.MSG_TYPE_CUSTOM_CMD_REQ;
import static com.ftr.message.CameraBroadMessageProtos.CameraBroadMessage.MsgType.MSG_TYPE_CUSTOM_CMD_RESP;
import static com.ftr.message.CameraBroadMessageProtos.CameraBroadMessage.MsgType.MSG_TYPE_NOTIFY_GPIOPARAM;
import static com.ftr.message.CameraBroadMessageProtos.CameraBroadMessage.parseFrom;
import static com.lam.imagekit.application.Constants.CAMERA_BROAD_CTRL_PORT2;
import static com.lam.imagekit.application.Constants.SERVER_ADDRESS;
import static com.lam.imagekit.utils.CameraBroadCtrlHelper.DATA_TYPE_MEDIADEVICE;

/**
 * Created by sknown on 2017/12/14.
 */

public class CameraBroadCtrl {
    public static final int MSG_CAMERABROADCTRL_TAKEPHOTOS = 1;
    public static final int MSG_CAMERABROADCTRL_START_APP = 2;
    public static final int MSG_CAMERABROADCTRL_AD_VALUE = 3;
    public static final int MSG_CAMERABROADCTRL_ZOOMIN = 4;
    public static final int MSG_CAMERABROADCTRL_ZOOMOUT = 5;

    public static final int MSG_BTN_PARAM_LONG_PRESS = 2;
    public static final int MSG_BTN_PARAM_SHORT_PRESS = 1;

    private static final boolean USE_JNI = true;
    static {
        System.loadLibrary("broadctrl_jni");
    }
    private native int init(String ip, int port);
    private native int send(byte[] buffer, int buf_len);

    //call from jni
    private void recvBuffer(byte[] buffer, int buf_len){
        parse(buffer, buf_len);
    }

    private UdpThread m_thread;
    private HandlerThread m_handlerThread;
    private Handler m_handler;

    private boolean m_startRecv;
    public CameraBroadCtrl(){
        if(!USE_JNI) {
            m_thread = new UdpThread(10000);
            m_thread.start();
        }

        m_handlerThread = new HandlerThread("CameraBroadCtrlHandler");
        m_handlerThread.start();
        m_handler = new Handler(m_handlerThread.getLooper());

        if(USE_JNI){
            init(SERVER_ADDRESS, CAMERA_BROAD_CTRL_PORT2);
        }
    }

    public void hello(){
        if(USE_JNI) {
            byte[] helloByte = CameraBroadCtrlHelper.cmdReqBuf("hello", 1, 1);
            send(helloByte, helloByte.length);
        }else {
            m_handler.post(new Runnable() {
                @Override
                public void run() {
                    byte[] helloByte = CameraBroadCtrlHelper.cmdReqBuf("hello", 1, 1);
                    try {
                        m_thread.send(helloByte);
                        m_startRecv = true;
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
        }

    }

    public void getuvc(){
        if(USE_JNI) {
            byte[] getuvcByte = CameraBroadCtrlHelper.cmdReqBuf("getuvc", 1, 1);
            send(getuvcByte, getuvcByte.length);
        }else {
            m_handler.post(new Runnable() {
                @Override
                public void run() {
                    try {
                        m_thread.send(CameraBroadCtrlHelper.cmdReqBuf("getuvc", 1, 1));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
        }
    }

    Comparator comp = new Comparator() {
        public int compare(Object o1, Object o2) {
            StreamParam p1 = (StreamParam) o1;
            StreamParam p2 = (StreamParam) o2;

            int s1 = p1.width*p1.height;
            int s2 = p2.width*p2.height;

            if (s1 < s2)
                return 1;
            else if (s1 == s2)
                return 0;
            else if (s1 > s2)
                return -1;
            return 0;
        }
    };

    private boolean tooLargeResolution(int width, int height){
        if(height>720){
            return true;
        }

        return false;
    }
    private StreamParam getRealResolution(int width, int height){
        float rotio = (float) width/(float)height;
        if(tooLargeResolution(width, height)){
            CameraParam cameraParam = AppContext.getInstance().getCameraParam();
            int currentIndex = 0;
            CameraParam cparam = AppContext.getInstance().getCameraParam();
            int sourceIndex = CameraParam.SOURCE_USB;
            CameraParam.Source source = cparam.getSource(sourceIndex);
            if(source.size() == 0){
                source = cparam.getSource(CameraParam.SOURCE_VI);
                sourceIndex = CameraParam.SOURCE_VI;
            }

            int i=0;
            int picformat = CameraParam.FORMAT_MJPEG;
            CameraParam.StreamParamList sparamList = cparam.getFormat(sourceIndex, picformat);
            if (sparamList != null){
                ArrayList<StreamParam> list = new ArrayList<>();

                for(StreamParam streamParam : sparamList){
                    list.add(streamParam);
                }
                Collections.sort(list, comp);

                for(StreamParam streamParam: list){
                    float rotio2 = (float) streamParam.width/(float)streamParam.height;
                    if(!tooLargeResolution(streamParam.width, streamParam.height) && rotio == rotio2){
                        return streamParam;
                    }
                }
            }
        }

        StreamParam streamParam = new StreamParam();
        streamParam.width = width;
        streamParam.height = height;
        streamParam.fps = 25;

        return streamParam;

    }

    public void setuvc(final int width, final int height){
//        StreamParam streamParam = getRealResolution(width, height);
        if(USE_JNI) {
            byte[] setuvcByte = CameraBroadCtrlHelper.cmdReqBuf("setuvc", width, height);
            send(setuvcByte, setuvcByte.length);
        }else {
            m_handler.post(new Runnable() {
                @Override
                public void run() {
                    try {
                        m_thread.send(CameraBroadCtrlHelper.cmdReqBuf("setuvc", width, height));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
        }
    }

    class UdpThread extends Thread{
        private DatagramSocket mDatagramSocket = null;
        private byte[] bytes = new byte[2048];
        public UdpThread(int timeout) {
            try {
                mDatagramSocket = new DatagramSocket();

                mDatagramSocket.setSoTimeout(timeout);
            } catch (SocketException e) {
                e.printStackTrace();
            }
        }

        // 向指定的服务端发送数据信息. 参数介绍： host 服务器主机地址 port 服务端端口 bytes 发送的数据信息
        public final synchronized void send(final byte[] bytes) throws IOException {
            DatagramPacket dp = new DatagramPacket(bytes, bytes.length,
                    InetAddress.getByName(SERVER_ADDRESS), Constants.CAMERA_BROAD_CTRL_PORT);
            mDatagramSocket.send(dp);
        }

        // 接收从指定的服务端发回的数据. hostName 服务端主机 hostPort 服务端端口 return 服务端发回的数据.
        public final synchronized int receive() {
            DatagramPacket dp = new DatagramPacket(bytes, bytes.length);
            try {
                mDatagramSocket.receive(dp);
            } catch (Exception e) {
                e.printStackTrace();
                return 0;
            }
            return dp.getLength();
        }
        @Override
        public void run() {
            for (;;) {
                if(!m_startRecv){
                    try {
                        sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                int recv_len = receive();
                if(recv_len>0){
                    parse(bytes, recv_len);
                }

            }
        }
    }
    private void parse(byte[]buffer, int buf_len){
        int type = buffer[0] + (buffer[1]<<8) + (buffer[2]<<16) + (buffer[3]<<24);
        byte[] newbuffer = new byte[buf_len-4];
        System.arraycopy(buffer, 4, newbuffer, 0, buf_len-4);
        if(type == DATA_TYPE_MEDIADEVICE) {
            MediaDeviceMessageProtos.MediaDeviceMessage message;
            try {
                message = MediaDeviceMessageProtos.MediaDeviceMessage.parseFrom(newbuffer);
                parseMediaDevice(message);
            } catch (InvalidProtocolBufferException e) {

            }
        }else{
            CameraBroadMessageProtos.CameraBroadMessage cameraBroadMessage;
            try {
                cameraBroadMessage = parseFrom(newbuffer);
                parseBroadCtrl(cameraBroadMessage);
            } catch (InvalidProtocolBufferException e) {

            }
        }
    }
    private MediaDeviceParse m_mediaDeviceParse = new MediaDeviceParse();
    private void parseMediaDevice(MediaDeviceMessageProtos.MediaDeviceMessage message){
        if (message != null) {

            m_mediaDeviceParse.broadType = message.getBroadcode().getValueDescriptor().getName();
            String ip = message.getIp().getData();

            m_mediaDeviceParse.fillCameraParam(message);

            AppContext appContext = AppContext.getInstance();
            if(appContext != null){
                appContext.setDeviceOnline(true);
                FTRCallback<Boolean> callback = appContext.getScanResultCallback();
                try {
                    if (callback != null)
                        callback.process(message, 0, 0, 0);
                } catch (Exception e) {

                }
            }
        }
    }

    private void parseBroadCtrl(CameraBroadMessageProtos.CameraBroadMessage message){
        if (message != null) {
            if(message.getType().equals(MSG_TYPE_NOTIFY_GPIOPARAM)) {
                CameraBroadMessageProtos.CameraBroadMessage.GPIOCtrl gpioCtrl = message.getGpioctrl();
                if (gpioCtrl != null) {
                    m_mediaDeviceParse.processGPIOParam(gpioCtrl);
                }
            }
        }
    }

    public interface CameraBroadCtrlCallback {
        int process(int what, int param1, int parma2);
    }

    public void setCameraBroadCtrlCallback(CameraBroadCtrlCallback callback){
        m_mediaDeviceParse.setCameraBroadCtrlCallback(callback);
    }
}
