package com.lam.imagekit.utils;


import com.ftr.message.CameraBroadMessageProtos;

import static com.ftr.message.CameraBroadMessageProtos.CameraBroadMessage.MsgType.MSG_TYPE_CUSTOM_CMD_REQ;

/**
 * Created by sknown on 2017/8/22.
 */

public class CameraBroadCtrlHelper {
    public final static int DATA_TYPE_BROADCTRL = 0x10101010;
    public final static int DATA_TYPE_MEDIADEVICE = 0x20202020;
    public static byte[] cmdReqBuf(String name, int param1, int param2){
        CameraBroadMessageProtos.CameraBroadMessage.Builder builder = CameraBroadMessageProtos.CameraBroadMessage.newBuilder();
        builder.setType(MSG_TYPE_CUSTOM_CMD_REQ);
        CameraBroadMessageProtos.CameraBroadMessage.CustomCmdParam.Builder cmdParam = CameraBroadMessageProtos.CameraBroadMessage.CustomCmdParam.newBuilder();
        cmdParam.setName(name);
        cmdParam.setParam1(param1);
        cmdParam.setParam2(param2);

        builder.setCmdParam(cmdParam);
        byte[] buf = builder.build().toByteArray();

        return buf;
    }
}
