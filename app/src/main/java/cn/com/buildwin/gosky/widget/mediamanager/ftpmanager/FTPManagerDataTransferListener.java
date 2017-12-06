package cn.com.buildwin.gosky.widget.mediamanager.ftpmanager;

public interface FTPManagerDataTransferListener {

    void started();

    void transferred(int length);

    void completed();

    void aborted();

    void failed();
}
