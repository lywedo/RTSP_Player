package com.lam.imagekit.data;



import com.ftr.message.MediaDeviceMessageProtos;
import com.lam.imagekit.utils.StringUtils;
import com.lam.imagekit.utils.to;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by sknown on 2016/9/24.
 */

public class CameraParam {
    public final static int SOURCE_VI = 1;
    public final static int SOURCE_USB = 2;

    public final static int FORMAT_MJPEG = 1;
    public final static int FORMAT_YUV = 2;

    public final static int DEFUALT_WIDTH = 352;
    public final static int DEFAULT_HEIGHT = 288;

    public int curWidth;
    public int curHeight;
    public int curFps;
    public int curFormat;
    public int curSource;
    public int interlace;

    private String ip;

    public MediaDeviceMessageProtos.MediaDeviceMessage.BroadCode broadCode;

    public int broadVersionCode;
    public String broadVersionName;

    public int wifiChannel;

    private HashMap<Integer, Source> m_sourceList = new HashMap<>();
    private String customId;

    public HashMap<Integer, Source> getSourceList(){
        return m_sourceList;
    }

    public String getBroadIdMd5(){
        if(customId==null || broadCode==null || customId.equals("") || broadCode.equals("")){
            return null;
        }

        String stripid = customId.contains("\n")?customId.split("\n")[0]:customId;
        stripid = StringUtils.getString(stripid, true);
        String brodCode = broadCode.getValueDescriptor().getName();

        String broadIdMd5 = to.stringToMD5(stripid + "-" + brodCode);

        return broadIdMd5;
    }

    public void setCustomId(String id){
        customId = id;

        String stripid = id.contains("\n")?id.split("\n")[0]:id;
        stripid = StringUtils.getString(stripid, true);

        //UpdateManager.getInstance().saveCustomId(stripid);
    }

    public String getCustomId(){
        return customId;
    }

//    public void updateBroadCode(){
//        String brodCode = broadCode.getValueDescriptor().getName();
//        UpdateManager.getInstance().saveBroadCode(brodCode);
//    }

    private Source addSource(int source){
        Source s = m_sourceList.containsKey(source)?m_sourceList.get(source):null;
        if(s == null) {
            s = new Source();
            m_sourceList.put(source, s);
        }

        return s;
    }

    public Source getSource(int source){
        Source s;
        synchronized (this) {
            s = (Source) addSource(source).clone();
        }

        return s;
    }
    public void clearSource(){
        synchronized (this) {
            m_sourceList.clear();
        }
    }

    private StreamParamList addFormat(int source, int format){
        Source s = addSource(source);


        StreamParamList list = s.containsKey(format)?s.get(format):null;
        if(list == null){
            list = new StreamParamList();
            s.put(format, list);
        }

        return list;
    }

    public StreamParamList getFormat(int source, int format){
        return addFormat(source, format);
    }

    public void addStreamParam(int source, int format, int width, int height, int fps){
        StreamParamList slist = getFormat(source, format);

        for(StreamParam param : slist){
            if((param.width == width) && (param.height == height)){
                param.fps =fps;
                return;
            }
        }

        StreamParam newStremParam = new StreamParam();
        newStremParam.width = width;
        newStremParam.height = height;
        newStremParam.fps = fps;

        slist.add(newStremParam);
    }

    public void updateBroadCode() {

    }

    public class StreamParamList extends ArrayList<StreamParam> {

    };

    public class Source extends HashMap<Integer, StreamParamList> {

    }
}
