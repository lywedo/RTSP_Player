package cn.com.buildwin.gosky.widget.mediamanager;

import java.io.File;

public interface MediaDownloadListener {

    void started(final String fileName, final long total);

    void transferred(final long length, final long total);

    void singleCompleted(File file);

    void completed();

    void aborted();

    void failed();
}
