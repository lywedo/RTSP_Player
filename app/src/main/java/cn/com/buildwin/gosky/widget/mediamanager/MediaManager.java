package cn.com.buildwin.gosky.widget.mediamanager;

import android.content.Context;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.util.Log;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import buildwin.common.BWCommonCallbacks.BWCompletionCallback;
import buildwin.common.BWCommonCallbacks.BWCompletionCallbackWith;
import buildwin.common.BWError;
import buildwin.common.Utilities;
import cn.com.buildwin.gosky.widget.bwsocket.BWSocketWrapper;
import cn.com.buildwin.gosky.widget.mediamanager.ftpmanager.FTPManager;
import cn.com.buildwin.gosky.widget.mediamanager.ftpmanager.FTPManagerDataTransferListener;
import it.sauronsoftware.ftp4j.FTPFile;

public class MediaManager {

    private static final String TAG = MediaManager.class.getSimpleName();

    /* Members */

    public static final String VIDEO_SUFFIX = ".avi";
    public static final String IMAGE_SUFFIX = ".jpg";

    private long transferredLength;

    /* Singleton */

    private static final MediaManager ourInstance = new MediaManager();

    public static MediaManager getInstance() {
        return ourInstance;
    }

    private MediaManager() {
    }

    /* Methods */

    /**
     * 获取远程文件列表
     */
    private void getRemoteFileList(@NonNull final BWCompletionCallbackWith<List<RemoteFile>> completionCallbackWith) {
        getRemoteFileList(null, completionCallbackWith);
    }

    /**
     * 获取远程文件列表，带过滤
     */
    private void getRemoteFileList(final String suffix, @NonNull final BWCompletionCallbackWith<List<RemoteFile>> completionCallbackWith) {

        FTPManager.getInstance().listFiles(suffix, new BWCompletionCallbackWith<List<FTPFile>>() {
            @Override
            public void onSuccess(List<FTPFile> files) {
                FTPManager.getInstance().disconnect(true, null);

                List<RemoteFile> remoteFiles = new ArrayList<>();
                for (int i = 0; i < files.size(); i++) {
                    FTPFile ftpFile = files.get(i);

                    if (ftpFile.getSize() == 0) continue;   // 过滤掉长度为0的文件

                    RemoteFile remoteFile = new RemoteFile();

                    // 因为RemoteFile和FTPFile的类型兼容，所以直接赋值
                    remoteFile.setType(ftpFile.getType());
                    remoteFile.setName(ftpFile.getName());
                    remoteFile.setModifiedDate(ftpFile.getModifiedDate());
                    remoteFile.setSize(ftpFile.getSize());

                    remoteFiles.add(remoteFile);
                }
                completionCallbackWith.onSuccess(remoteFiles);
            }

            @Override
            public void onFailure(BWError error) {
                FTPManager.getInstance().disconnect(true, null);
                completionCallbackWith.onFailure(error);
            }
        });
    }

    public void getVideoFileList(@NonNull final BWCompletionCallbackWith<List<RemoteFile>> completionCallbackWith) {

        BWSocketWrapper.getInstance().stopRecord(new BWCompletionCallback() {
            @Override
            public void onResult(BWError error) {
                // 停止录制错误
                if (error != null) {
                    completionCallbackWith.onFailure(error);
                } else {    // 停止录制OK
                    // 开始连接
                    FTPManager.getInstance().connect(new BWCompletionCallback() {
                        @Override
                        public void onResult(BWError error) {
                            if (error != null) {
                                completionCallbackWith.onFailure(error);
                            } else {
                                // 改变当前目录到视频目录
                                FTPManager.getInstance().changeToVideoDirectory(new BWCompletionCallback() {
                                    @Override
                                    public void onResult(BWError error) {
                                        if (error != null) {
                                            completionCallbackWith.onFailure(error);
                                        } else {
                                            // 获取远程文件列表
                                            getRemoteFileList(VIDEO_SUFFIX, completionCallbackWith);
                                        }
                                    }
                                });
                            }
                        }
                    });
                }
            }
        });
    }

    public void getImageFileList(@NonNull final BWCompletionCallbackWith<List<RemoteFile>> completionCallbackWith) {

        BWSocketWrapper.getInstance().stopRecord(new BWCompletionCallback() {
            @Override
            public void onResult(BWError error) {
                // 停止录制错误
                if (error != null) {
                    completionCallbackWith.onFailure(error);
                } else {    // 停止录制OK
                    // 开始连接
                    FTPManager.getInstance().connect(new BWCompletionCallback() {
                        @Override
                        public void onResult(BWError error) {
                            if (error != null) {
                                completionCallbackWith.onFailure(error);
                            } else {
                                // 改变当前目录到图像目录
                                FTPManager.getInstance().changeToImageDirectory(new BWCompletionCallback() {
                                    @Override
                                    public void onResult(BWError error) {
                                        if (error != null) {
                                            completionCallbackWith.onFailure(error);
                                        } else {
                                            // 获取远程文件列表
                                            getRemoteFileList(IMAGE_SUFFIX, completionCallbackWith);
                                        }
                                    }
                                });
                            }
                        }
                    });
                }
            }
        });
    }

    /**
     * 下载文件
     * @param remoteFiles 要下载的远程文件列表
     * @param listener  回调
     */
    public void downloadRemoteFiles(final List<RemoteFile> remoteFiles, @NonNull final MediaDownloadListener listener) {
        BWSocketWrapper.getInstance().stopRecord(new BWCompletionCallback() {
            @Override
            public void onResult(BWError error) {
                // 停止录制错误
                if (error != null) {
                    listener.failed();
                } else {    // 停止录制OK
                    // 开始连接
                    FTPManager.getInstance().connect(new BWCompletionCallback() {
                        @Override
                        public void onResult(BWError error) {
                            if (error != null) {
                                listener.failed();
                            } else {
                                FTPManager.getInstance().changeToVideoDirectory(new BWCompletionCallback() {
                                    @Override
                                    public void onResult(BWError error) {
                                        if (error != null) {
                                            listener.failed();
                                        } else {
                                            downloadRemoteFiles_internal(remoteFiles, listener);
                                        }
                                    }
                                });
                            }
                        }
                    });
                }
            }
        });
    }

    private void downloadRemoteFiles_internal(final List<RemoteFile> remoteFiles, @NonNull final MediaDownloadListener listener) {

        if (remoteFiles.size() == 0) {
            FTPManager.getInstance().disconnect(true, null);
            listener.completed();
            return; // 数量为0时，递归返回
        }

        final RemoteFile remoteFile = remoteFiles.get(0);
        remoteFiles.remove(0);

        final String fileName = remoteFile.getName();
        final long fileSize = remoteFile.getSize();

        final File outputFile = new File(Utilities.getCardMediaVideoPath(), fileName);
        final File tempFile = new File(Utilities.getCardMediaVideoPath(), fileName + "~");    // 下载临时文件后是"~"
        long restartAt = remoteFile.isResumeDownload() ? remoteFile.getLocalSize() : 0;

        // If downloaded
        if (remoteFile.isDownloaded()) {
            outputFile.delete();    // Delete local file
            MediaManagerHelper.getInstance().removeDownloadedFileSizeItem(remoteFile.getName());
        }
        // If overwrite
        if (!remoteFile.isResumeDownload())
            tempFile.delete();      // Delete temp file

        // Save file size to settings
        MediaManagerHelper.getInstance().addDownloadingFileSizeItem(remoteFile);

        transferredLength = restartAt;
        FTPManager.getInstance().downloadFile(fileName, tempFile, restartAt, new FTPManagerDataTransferListener() {
            @Override
            public void started() {
                listener.started(fileName, fileSize);
            }

            @Override
            public void transferred(int length) {
                transferredLength += length;
                listener.transferred(transferredLength, fileSize);
            }

            @Override
            public void completed() {
                // Rename to remove "~"
                if (tempFile.renameTo(outputFile)) {
                    // Scan file to MediaLibrary
                    listener.singleCompleted(outputFile);
                } else {
//                    listener.failed();  // 如果是重命名失败，再想想怎么处理，暂时不用这种方式结束
                }

                // Remove it from downloading settings
                MediaManagerHelper.getInstance().removeDownloadingFileSizeItem(remoteFile.getName());

                // Add it to downloaded settings
                MediaManagerHelper.getInstance().addDownloadedFileSizeItem(remoteFile);

                // 这里递归调用
                downloadRemoteFiles_internal(remoteFiles, listener);
            }

            @Override
            public void aborted() {
                FTPManager.getInstance().disconnect(true, null);
                listener.aborted();
            }

            @Override
            public void failed() {
                FTPManager.getInstance().disconnect(true, null);
                listener.failed();
            }
        });
    }

    public void cancelDownload(BWCompletionCallback completionCallback) {
        FTPManager.getInstance().cancelDownload(completionCallback);
    }

    /* Local file manager */

    public List<File> getAllLocalVideoFiles() {
        return getAllLocalFiles(MediaManager.VIDEO_SUFFIX);
    }

    public List<File> getAllLocalImageFiles() {
        return getAllLocalFiles(MediaManager.IMAGE_SUFFIX);
    }

    private List<File> getAllLocalFiles(final String extName) {

        File cardMediaPath =  new File(Utilities.getCardMediaVideoPath());
        File[] files = null;

        if (cardMediaPath.exists()) {
            files = cardMediaPath.listFiles(new FilenameFilter() {
                @Override
                public boolean accept(File dir, String name) {
                    return extName == null || name.toLowerCase().endsWith(extName.toLowerCase());
                }
            });
        }

        return files != null ? Arrays.asList(files) : null;
    }

    /**
     * 扫描添加媒体文件到系统媒体库
     * @param file  媒体文件
     */
    public static void mediaScan(Context context, File file) {
        MediaScannerConnection.scanFile(context,
                new String[] { file.getAbsolutePath() }, null,
                new MediaScannerConnection.OnScanCompletedListener() {
                    @Override
                    public void onScanCompleted(String path, Uri uri) {
                        Log.v("MediaScanWork", "file " + path
                                + " was scanned seccessfully: " + uri);
                    }
                });
    }

}
