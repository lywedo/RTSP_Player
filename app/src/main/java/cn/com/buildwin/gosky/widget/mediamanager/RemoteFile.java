package cn.com.buildwin.gosky.widget.mediamanager;

import java.util.Date;

public class RemoteFile {

    public static final int TYPE_FILE = 0;
    public static final int TYPE_DIRECTORY = 1;


    private String name = null;
    private Date modifiedDate = null;
    private long size = -1;
    private int type;

    private boolean tempExist = false;
    private boolean sizeMismatch = false;
    private long localSize = 0;
    private boolean resumeDownload = false; // true to resume download, false to overwrite
    private boolean downloaded = false;


    public Date getModifiedDate() {
        return modifiedDate;
    }

    public void setModifiedDate(Date modifiedDate) {
        this.modifiedDate = modifiedDate;
    }


    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }


    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }


    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public boolean isTempExist() {
        return tempExist;
    }

    public void setTempExist(boolean tempExist) {
        this.tempExist = tempExist;
    }

    public boolean isSizeMismatch() {
        return sizeMismatch;
    }

    public void setSizeMismatch(boolean sizeMismatch) {
        this.sizeMismatch = sizeMismatch;
    }

    public long getLocalSize() {
        return localSize;
    }

    public void setLocalSize(long localSize) {
        this.localSize = localSize;
    }

    public boolean isResumeDownload() {
        return resumeDownload;
    }

    public void setResumeDownload(boolean resumeDownload) {
        this.resumeDownload = resumeDownload;
    }

    public boolean isDownloaded() {
        return downloaded;
    }

    public void setDownloaded(boolean downloaded) {
        this.downloaded = downloaded;
    }


    public String toString() {
        StringBuilder buffer = new StringBuilder();
        buffer.append(getClass().getName());
        buffer.append(" [name=");
        buffer.append(name);
        buffer.append(", type=");
        if (type == TYPE_FILE) {
            buffer.append("FILE");
        } else if (type == TYPE_DIRECTORY) {
            buffer.append("DIRECTORY");
        } else {
            buffer.append("UNKNOWN");
        }
        buffer.append(", size=");
        buffer.append(size);
        buffer.append(", modifiedDate=");
        buffer.append(modifiedDate);
        buffer.append("]");
        return buffer.toString();
    }

}
