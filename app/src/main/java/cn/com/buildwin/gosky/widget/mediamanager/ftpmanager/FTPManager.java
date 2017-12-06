package cn.com.buildwin.gosky.widget.mediamanager.ftpmanager;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Process;
import android.support.annotation.NonNull;
import android.util.Log;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import buildwin.common.BWCommonCallbacks;
import buildwin.common.CallbackHandler;
import it.sauronsoftware.ftp4j.FTPClient;
import it.sauronsoftware.ftp4j.FTPDataTransferListener;
import it.sauronsoftware.ftp4j.FTPFile;

public class FTPManager {

    private static final String TAG = FTPManager.class.getSimpleName();

    /* FTP information */

    private static final String HOST = "192.168.1.1";
    private static final String USERNAME = "ftp";
    private static final String PASSWORD = "ftp";

    private static final String VIDEO_PATH = "VIDEO";
    private static final String IMAGE_PATH = "IMAGE";

    /* Singleton */

    private static final FTPManager ourInstance = new FTPManager();

    public static FTPManager getInstance() {
        return ourInstance;
    }

    private FTPManager() {
        mFTPClient = new FTPClient();
    }

    /* Members */

    private FTPClient mFTPClient;

    /* Setter & Getter */

    /* Methods */

    /**
     * 连接并准备
     */
    public void connect(@NonNull final BWCommonCallbacks.BWCompletionCallback completionCallback) {
        CallbackHandler.post(new Runnable() {
            @Override
            public void run() {
                // Disconnect first, for safe
                // 有时候设备断开后再重连，FTPClient以为自己还是处于连接状态，再执行下一步操作的时候就会异常
                try {
                    mFTPClient.disconnect(false);
                } catch (Exception e) {
                }

                // Connect
                try {
                    mFTPClient.connect(HOST);
                }
                catch (Exception e) {
                    e.printStackTrace();
                    if (!(e instanceof IllegalStateException)) {
                        completionCallback.onResult(BWFTPManagerError.FTP_MANAGER_NOT_SUPPORT);
                        return;
                    }
                }

                // Login and prepare
                try {
                    mFTPClient.login(USERNAME, PASSWORD);

                    mFTPClient.setMLSDPolicy(FTPClient.MLSD_NEVER);
                    mFTPClient.setPassive(true);

                    // Adapt to Firmware
                    mFTPClient.changeDirectory("/");
                }
                catch (Exception e) {
                    e.printStackTrace();
                    completionCallback.onResult(BWFTPManagerError.FTP_MANAGER_RESULT_FAILED);
                    return;
                }

                completionCallback.onResult(null);
            }
        });
    }

    /**
     * 断开连接
     */
    public void disconnect(boolean sendQuitCommand, final BWCommonCallbacks.BWCompletionCallback completionCallback) {
        CallbackHandler.post(new Runnable() {
            @Override
            public void run() {
                // Disconnect
                try {
                    mFTPClient.disconnect(true);
                }
                catch (Exception e) {
                    if (completionCallback != null)
                        completionCallback.onResult(BWFTPManagerError.FTP_MANAGER_RESULT_FAILED);
                    return;
                }
                if (completionCallback != null)
                    completionCallback.onResult(null);
            }
        });
    }

    /**
     * 获取当前目录的文件列表
     */
    public void listFiles(@NonNull final BWCommonCallbacks.BWCompletionCallbackWith<List<FTPFile>> completionCallbackWith) {
        listFiles(null, completionCallbackWith);
    }

    /**
     * 获取当前目录的文件列表，带过滤器
     */
    public void listFiles(final String suffix, @NonNull final BWCommonCallbacks.BWCompletionCallbackWith<List<FTPFile>> completionCallbackWith) {

        CallbackHandler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    FTPFile[] files = mFTPClient.list();
                    List<FTPFile> filteredFiles = new ArrayList<>();

                    if (suffix != null) {
                        for (FTPFile file : files) {
                            String fileName = file.getName().toLowerCase();
                            if (fileName.endsWith(suffix.toLowerCase())) {
                                filteredFiles.add(file);
                            }
                        }
                    } else {
                        filteredFiles = Arrays.asList(files);
                    }

                    completionCallbackWith.onSuccess(filteredFiles);
                }
                catch (Exception e) {
                    e.printStackTrace();
                    completionCallbackWith.onFailure(BWFTPManagerError.FTP_MANAGER_RESULT_FAILED);
                }
            }
        });
    }

    /**
     * 切换到视频目录
     */
    public void changeToVideoDirectory(@NonNull final BWCommonCallbacks.BWCompletionCallback completionCallback) {

        CallbackHandler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    mFTPClient.changeDirectory("/");
                    mFTPClient.changeDirectory(VIDEO_PATH);

                    completionCallback.onResult(null);
                }
                catch (Exception e) {
                    e.printStackTrace();
                    completionCallback.onResult(BWFTPManagerError.FTP_MANAGER_RESULT_FAILED);
                }
            }
        });
    }

    /**
     * 切换到图像目录
     */
    public void changeToImageDirectory(@NonNull final BWCommonCallbacks.BWCompletionCallback completionCallback) {

        CallbackHandler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    mFTPClient.changeDirectory("/");
                    mFTPClient.changeDirectory(IMAGE_PATH);

                    completionCallback.onResult(null);
                }
                catch (Exception e) {
                    e.printStackTrace();
                    completionCallback.onResult(BWFTPManagerError.FTP_MANAGER_RESULT_FAILED);
                }
            }
        });
    }

    public void downloadFile(final String remoteFileName, final File localFile,
                             final long restartAt, @NonNull final FTPManagerDataTransferListener listener) {

        CallbackHandler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    mFTPClient.download(remoteFileName, localFile, restartAt, new FTPDataTransferListener() {
                        @Override
                        public void started() {
                            listener.started();
                        }

                        @Override
                        public void transferred(int length) {
                            listener.transferred(length);
                        }

                        @Override
                        public void completed() {
                            listener.completed();
                        }

                        @Override
                        public void aborted() {
                            listener.aborted();
                        }

                        @Override
                        public void failed() {
                            listener.failed();
                        }
                    });
                }
                catch (Exception e) {
                    Log.e(TAG, "FTPClient.download aborted or failed");
                    e.printStackTrace();
                }
            }
        });
    }

    public void cancelDownload(final BWCommonCallbacks.BWCompletionCallback completionCallback) {

        HandlerThread handlerThread = new HandlerThread("cn.com.buildwin.ftpmanager.abort_download", Process.THREAD_PRIORITY_MORE_FAVORABLE);
        handlerThread.start();
        Handler handler = new Handler(handlerThread.getLooper());

        handler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    Log.e(TAG, "abortCurrentDataTransfer");
                    mFTPClient.abortCurrentDataTransfer(false);

                    if (completionCallback != null) {
                        completionCallback.onResult(null);
                    }
                } catch (Exception e) {
                    e.printStackTrace();

                    if (completionCallback != null) {
                        completionCallback.onResult(BWFTPManagerError.FTP_MANAGER_RESULT_FAILED);
                    }
                }
            }
        });
    }

}
