package com.lam.imagekit.services.utils;

import android.content.Intent;

import com.ftr.message.CameraBroadMessageProtos;
import com.ftr.message.MediaDeviceMessageProtos;
import com.lam.imagekit.AppContext;
import com.lam.imagekit.data.CameraParam;
import com.lam.imagekit.services.CameraBroadCtrl;

import static com.lam.imagekit.services.CameraBroadCtrl.MSG_CAMERABROADCTRL_TAKEPHOTOS;

/**
 * Created by sknown on 2017/12/16.
 */

public class MediaDeviceParse {
    public String broadType;

    public void fillCameraParam(MediaDeviceMessageProtos.MediaDeviceMessage message) {
        boolean heigth_width_chanaged = false;
        CameraParam cparam = AppContext.getInstance().getCameraParam();

        MediaDeviceMessageProtos.MediaDeviceMessage.BroadCode broadCode = message.getBroadcode();
        MediaDeviceMessageProtos.MediaDeviceMessage.BroadVersion broadVersion = message.getBroadversion();
        MediaDeviceMessageProtos.MediaDeviceMessage.CustomerINfo customerINfo = message.getCustomerINfo();

        if(cparam.broadCode == null || !broadCode.equals(cparam.broadCode)){
            cparam.broadCode = broadCode;
            cparam.updateBroadCode();
        }

        String customerId = customerINfo.getCustomerId();
        if(cparam.getCustomId() == null || !customerId.equals(cparam.getCustomId())){
            cparam.setCustomId(customerINfo.getCustomerId());
        }

        if(message.getParam().getWifichannel() != cparam.wifiChannel){
            cparam.wifiChannel = message.getParam().getWifichannel();
        }

        if (broadVersion != null){
            if(cparam.broadVersionCode != message.getBroadversion().getVersionCode()){
                cparam.broadVersionCode = message.getBroadversion().getVersionCode();
            }
            if(cparam.broadVersionName == null || !cparam.broadVersionName.equals(message.getBroadversion().getVersionName())){
                cparam.broadVersionName = message.getBroadversion().getVersionName();
            }
        }

        int sourceCount = message.getInputSourceCount();
        int i = 0;
        int j = 0;
        int z = 0;

        //format
        int newFormat = CameraParam.FORMAT_MJPEG;
        MediaDeviceMessageProtos.MediaDeviceMessage.Picture pic = message.getPic();
        if(pic.getFormat()== MediaDeviceMessageProtos.MediaDeviceMessage.PicFormat.PIC_FORMAT_MJPEG){
            newFormat = CameraParam.FORMAT_MJPEG;
        }else {
            newFormat = CameraParam.FORMAT_YUV;
        }
        if(newFormat != cparam.curFormat){
            cparam.curFormat = newFormat;
        }

        cparam.interlace = message.getPic().getInterlace();
        if(cparam.curFps != pic.getFps())
            cparam.curFps = pic.getFps();
        if(cparam.curHeight != pic.getHeight()) {
            cparam.curHeight = pic.getHeight();
            heigth_width_chanaged = true;
        }
        if(cparam.curWidth != pic.getWidth()) {
            cparam.curWidth = pic.getWidth();
            heigth_width_chanaged = true;
        }

        //source
        int newSource = CameraParam.SOURCE_USB;
        if(pic.getSource() == MediaDeviceMessageProtos.MediaDeviceMessage.PicSource.PIC_SOURCE_IPC){
            newSource = CameraParam.SOURCE_VI;
        }else{
            newSource = CameraParam.SOURCE_USB;
        }
        if(newSource != cparam.curSource){
            cparam.curSource = newSource;
        }

        cparam.clearSource();
        for(i=0; i<sourceCount; i++){
            MediaDeviceMessageProtos.MediaDeviceMessage.InputSource inputSource = message.getInputSource(i);

            int formatCount = inputSource.getIformatCount();
            for(j=0; j<formatCount; j++){
                MediaDeviceMessageProtos.MediaDeviceMessage.InputFormat inputFormat = inputSource.getIformat(j);
                int resolutionCount = inputFormat.getResolutionCount();
                for(z=0; z<resolutionCount; z++){
                    MediaDeviceMessageProtos.MediaDeviceMessage.PicResolution picResolution = inputFormat.getResolution(z);

                    int width = picResolution.getWidth();
                    int height = picResolution.getHeight();
                    int fps = picResolution.getFps();

                    int source = inputSource.getSource()== MediaDeviceMessageProtos.MediaDeviceMessage.PicSource.PIC_SOURCE_IPC
                            ? CameraParam.SOURCE_VI:CameraParam.SOURCE_USB;
                    int format = (inputFormat.getFormat() == MediaDeviceMessageProtos.MediaDeviceMessage.PicFormat.PIC_FORMAT_MJPEG)?
                            CameraParam.FORMAT_MJPEG:CameraParam.FORMAT_YUV;
                    if(format == CameraParam.FORMAT_YUV && (height==480 || height == 240)){
//                        if (height==480){
//                            cparam.addStreamParam(source, format, 1280, 720, 10);
//                        }
                        cparam.addStreamParam(source, format, width, height, fps);
                    }else if(format == CameraParam.FORMAT_MJPEG){
                        cparam.addStreamParam(source, format, width, height, fps);
                    }

                }
            }
        }

    }

    public  void processGPIOParam(CameraBroadMessageProtos.CameraBroadMessage.GPIOCtrl gpioCtrl){
        int val = 0;

        CameraBroadMessageProtos.CameraBroadMessage.GPIOParam gpioParam = gpioCtrl.getParam(0);
        if (gpioParam != null){
            val = gpioParam.getVal();
            String name = gpioParam.getName();
            int msg = MSG_CAMERABROADCTRL_TAKEPHOTOS;
            if (mCameraBroadCtrlCallback != null){
                mCameraBroadCtrlCallback.process(msg, val, 0);
            }
        }
    }

    private CameraBroadCtrl.CameraBroadCtrlCallback mCameraBroadCtrlCallback;
    public void setCameraBroadCtrlCallback(CameraBroadCtrl.CameraBroadCtrlCallback mCameraBroadCtrlCallback) {
        this.mCameraBroadCtrlCallback = mCameraBroadCtrlCallback;
    }

}
