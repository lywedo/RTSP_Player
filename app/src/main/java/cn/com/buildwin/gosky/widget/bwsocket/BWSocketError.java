package cn.com.buildwin.gosky.widget.bwsocket;

import buildwin.common.BWError;

public class BWSocketError extends BWError {

    public static final BWSocketError SOCKET_CONNECTION_FAILED = new BWSocketError("connection failed");
    public static final BWSocketError INFORMATION_FETCH_FAILED = new BWSocketError("Fetch information failed");

    private String mDescription;

    private BWSocketError() {
    }

    private BWSocketError(String description) {
        this.mDescription = description;
    }

    public String getDescription() {
        return this.mDescription;
    }

    public void setDescription(String description) {
        this.mDescription = description;
    }

}
