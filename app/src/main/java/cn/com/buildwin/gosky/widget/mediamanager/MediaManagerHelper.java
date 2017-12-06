package cn.com.buildwin.gosky.widget.mediamanager;

import android.content.ContextWrapper;
import android.content.SharedPreferences;

import java.io.File;
import java.util.List;

import buildwin.common.BWCommonCallbacks;
import buildwin.common.CallbackHandler;
import buildwin.common.Utilities;

public class MediaManagerHelper {

    public static final String DOWNLOADING_FILE_SIZE_SETTINGS_NAME = "downloadingfilesizesettings";
    public static final String DOWNLOADED_FILE_SIZE_SETTINGS_NAME = "downloadedfilesizesettings";

    /* Singleton */

    private static final MediaManagerHelper ourInstance = new MediaManagerHelper();

    public static MediaManagerHelper getInstance() {
        return ourInstance;
    }

    private MediaManagerHelper() {
    }

    /* Members */

    private SharedPreferences downloadingFileSizeSettings;
    private SharedPreferences downloadedFileSizeSettings;
    private SharedPreferences.Editor downloadingFileSizeSettingsEditor;
    private SharedPreferences.Editor downloadedFileSizeSettingsEditor;

    /* Methods */

    public void checkRemoteFiles(final List<RemoteFile> remoteFiles, final BWCommonCallbacks.BWCompletionCallback completion) {
        CallbackHandler.post(new Runnable() {
            @Override
            public void run() {
                for (RemoteFile remoteFile : remoteFiles) {
                    checkRemoteFile(remoteFile);
                }
                completion.onResult(null);
            }
        });
    }

    public void checkRemoteFile(RemoteFile remoteFile) {
        File localFile = new File(Utilities.getCardMediaVideoPath(), remoteFile.getName());
        File tempFile = new File(Utilities.getCardMediaVideoPath(), remoteFile.getName() + "~");    // 下载临时文件后是"~"
        if (localFile.exists()) {
            remoteFile.setDownloaded(true);
            remoteFile.setLocalSize(localFile.length());
            remoteFile.setSizeMismatch(localFile.length() != remoteFile.getSize());
        } else if (tempFile.exists()) {
            remoteFile.setTempExist(true);
            remoteFile.setLocalSize(tempFile.length());
            remoteFile.setSizeMismatch(tempFile.length() != remoteFile.getSize());
        }
    }

    /* Download file size settings */

    public void initDownloadFileSizeSettings(ContextWrapper contextWrapper) {
        openDownloadingFileSizeSettings(contextWrapper);
        openDownloadedFileSizeSettings(contextWrapper);
    }

    /* Compare downloading file size */

    private void openDownloadingFileSizeSettings(ContextWrapper contextWrapper) {
        downloadingFileSizeSettings = contextWrapper.getSharedPreferences(DOWNLOADING_FILE_SIZE_SETTINGS_NAME, 0);
        downloadingFileSizeSettingsEditor = downloadingFileSizeSettings.edit();
    }

    public void saveDownloadingFileSizeSettings() {
        downloadingFileSizeSettingsEditor.commit();
    }

    public boolean matchSizeOfDownloadingFileSize(RemoteFile remoteFile) {
        long fileSize = downloadingFileSizeSettings.getLong(remoteFile.getName(), 0);
        return fileSize == remoteFile.getSize();
    }

    public void addDownloadingFileSizeItem(RemoteFile remoteFile) {
        downloadingFileSizeSettingsEditor.putLong(remoteFile.getName(), remoteFile.getSize());
        saveDownloadingFileSizeSettings();
    }

    public void removeDownloadingFileSizeItem(String fileName) {
        downloadingFileSizeSettingsEditor.remove(fileName);
        saveDownloadingFileSizeSettings();
    }

    /* Compare downloaded file size */

    private void openDownloadedFileSizeSettings(ContextWrapper contextWrapper) {
        downloadedFileSizeSettings = contextWrapper.getSharedPreferences(DOWNLOADED_FILE_SIZE_SETTINGS_NAME, 0);
        downloadedFileSizeSettingsEditor = downloadedFileSizeSettings.edit();
    }

    public void saveDownloadedFileSizeSettings() {
        downloadedFileSizeSettingsEditor.commit();
    }

    public boolean matchSizeOfDownloadedFileSize(RemoteFile remoteFile) {
        long fileSize = downloadedFileSizeSettings.getLong(remoteFile.getName(), 0);
        return fileSize == remoteFile.getSize();
    }

    public void addDownloadedFileSizeItem(RemoteFile remoteFile) {
        downloadedFileSizeSettingsEditor.putLong(remoteFile.getName(), remoteFile.getSize());
        downloadedFileSizeSettingsEditor.commit();
    }

    public void removeDownloadedFileSizeItem(String fileName) {
        downloadedFileSizeSettingsEditor.remove(fileName);
        saveDownloadedFileSizeSettings();
    }

}
