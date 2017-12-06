package cn.com.buildwin.gosky.widget.mediamanager.ftpmanager;

import buildwin.common.BWError;

public class BWFTPManagerError extends BWError {

    public static final BWFTPManagerError FTP_MANAGER_NOT_SUPPORT = new BWFTPManagerError("Device doesn't support card or error occurred while connecting to device");
    public static final BWFTPManagerError FTP_MANAGER_RESULT_FAILED = new BWFTPManagerError("failed");

    private String mDescription;

    private BWFTPManagerError() {
    }

    private BWFTPManagerError(String description) {
        this.mDescription = description;
    }

    public String getDescription() {
        return this.mDescription;
    }

    public void setDescription(String description) {
        this.mDescription = description;
    }

}
