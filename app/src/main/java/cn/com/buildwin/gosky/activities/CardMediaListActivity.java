package cn.com.buildwin.gosky.activities;

import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.IdRes;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.kaopiz.kprogresshud.KProgressHUD;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import buildwin.common.BWCommonCallbacks;
import buildwin.common.BWError;
import buildwin.common.Utilities;
import butterknife.BindColor;
import butterknife.BindString;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnItemClick;
import cn.com.buildwin.gosky.widget.bwsocket.BWSocketWrapper;
import cn.com.buildwin.gosky.R;
import cn.com.buildwin.gosky.widget.mediamanager.MediaDownloadListener;
import cn.com.buildwin.gosky.widget.mediamanager.MediaManager;
import cn.com.buildwin.gosky.widget.mediamanager.MediaManagerHelper;
import cn.com.buildwin.gosky.widget.mediamanager.RemoteFile;
import info.hoang8f.android.segmented.SegmentedGroup;

public class CardMediaListActivity extends AppCompatActivity
        implements RadioGroup.OnCheckedChangeListener, SwipeRefreshLayout.OnRefreshListener {

    private static final String TAG = CardMediaListActivity.class.getSimpleName();

    @BindView(R.id.card_media_back_button)
    ImageButton mBackButton;
    @BindView(R.id.card_media_download_button)
    ImageButton mDownloadButton;
    @BindView(R.id.card_media_delete_button)
    ImageButton mDeleteButton;
    @BindView(R.id.card_media_select_button)
    ImageButton mSelectButton;

    @BindView(R.id.card_media_segmentedGroup)
    SegmentedGroup mSegmentedGroup;
    @BindView(R.id.card_media_device_files_radioButton)
    RadioButton mDeviceRadioButton;
    @BindView(R.id.card_media_local_files_radioButton)
    RadioButton mLocalRadioButton;

    @BindView(R.id.card_media_list_listView)
    ListView mListView;
    @BindView(R.id.card_media_list_empty_textView)
    TextView listEmptyTextView;

    @BindView(R.id.card_media_list_swipeRefreshLayout)
    SwipeRefreshLayout mSwipeRefreshLayout;

    // ListView color
    @BindColor(R.color.list_view_default_color)
    int listViewDefaultColor;
    @BindColor(R.color.list_view_checked_color)
    int listViewCheckedColor;
    // List text color
    @BindColor(R.color.title_text_color)
    int titleTextColor;
    @BindColor(R.color.detail_text_color)
    int detailTextColor;

    // SegmentedGroup color
    @BindColor(R.color.main_theme_color)
    int mainThemeColor;
    @BindColor(R.color.main_background_color)
    int mainBackgroundColor;

    private List<RemoteFile> mRemoteMediaList;
    private List<File> mLocalMediaList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_card_media_list);
        ButterKnife.bind(this);

        // Init segmentedGroup
        mSegmentedGroup.setOnCheckedChangeListener(this);
        mSegmentedGroup.setTintColor(mainBackgroundColor, mainThemeColor);
//        mSegmentedGroup.check(R.id.card_media_device_files_radioButton);  // Bug, called two times
        mDeviceRadioButton.setChecked(true);    // Do this instead. Trigger getting remote files.

        // ListView settings
        mListView.setAdapter(getListAdapter());
        mListView.setEmptyView(listEmptyTextView);

        // Pull to refresh
        mSwipeRefreshLayout.setOnRefreshListener(this);

        // Init download file size settings
        MediaManagerHelper.getInstance().initDownloadFileSizeSettings(this);
    }

    /**
     * 拦截后退键，先做关闭前处理
     */
    @Override
    public void onBackPressed() {
        doExit();
    }

    /**
     * 执行退出动作（先发出重新录卡命令，再退出）
     */
    private void doExit() {
        doStartRemoteRecord();
    }

    /**
     * 屏幕常亮的开关
     * @param on 开关
     */
    private void keepScreenOn(final boolean on) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (on) {
                    // 开启屏幕常亮
                    getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                } else {
                    // 关闭屏幕常亮
                    getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                }
            }
        });
    }

    /**
     * 获取并载入远程视频文件列表
     */
    private void loadRemoteVideoList() {
        // 下载时屏幕常亮
        keepScreenOn(true);

        // 显示HUD
        final KProgressHUD hud = createIndeterminateProgressHUD("Fetching video list...", null).show();

        MediaManager.getInstance().getVideoFileList(new BWCommonCallbacks.BWCompletionCallbackWith<List<RemoteFile>>() {
            @Override
            public void onSuccess(List<RemoteFile> fileList) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        // 关闭屏幕常亮
                        keepScreenOn(false);
                        // 关闭HUD
                        hud.dismiss();
                        // 停止下拉刷新状态
                        mSwipeRefreshLayout.setRefreshing(false);
                    }
                });

                // 重新载入列表
                mRemoteMediaList = fileList;
                reloadListView();

                // Refresh file status
                refreshFileStatus(fileList);
            }

            @Override
            public void onFailure(BWError error) {
                Log.e(TAG, "getVideoFileList error: " + error.getDescription());

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        // 关闭屏幕常亮
                        keepScreenOn(false);
                        // 关闭HUD
                        hud.dismiss();
                        // 停止下拉刷新状态
                        mSwipeRefreshLayout.setRefreshing(false);
                    }
                });
                // 提示下载失败，设备不支持卡，或者是连接错误
                showAlertDialog(CardMediaListActivity.this, "Connection Failed", "Device doesn't support card or error occurred while connecting to device");
            }
        });
    }

    /**
     * 载入本地视频文件列表（本地文件处理速度很快，不需要异步处理）
     */
    private void loadLocalVideoList() {
        mLocalMediaList = MediaManager.getInstance().getAllLocalVideoFiles();
        reloadListView();
        mSwipeRefreshLayout.setRefreshing(false);
    }

    /**
     * 获取已选择的RemoteFile列表
     * @return  RemoteFile列表
     */
    private List<RemoteFile> getSelectedRemoteFiles() {
        List<RemoteFile> selectedList = new ArrayList<>();
        SparseBooleanArray selectedItemPositions = mListView.getCheckedItemPositions();
        for (int i=0; i<mListView.getCount(); i++) {
            boolean selected = selectedItemPositions.get(i);
            if (selected) {
                selectedList.add(mRemoteMediaList.get(i));
            }
        }
        return selectedList;
    }

    /**
     * 获取已选择的File列表
     * @return  File列表
     */
    private List<File> getSelectedLocalFiles() {
        List<File> selectedList = new ArrayList<>();
        SparseBooleanArray selectedItemPositions = mListView.getCheckedItemPositions();
        for (int i=0; i<mListView.getCount(); i++) {
            boolean selected = selectedItemPositions.get(i);
            if (selected) {
                selectedList.add(mLocalMediaList.get(i));
            }
        }
        return selectedList;
    }

    /**
     * 下载文件
     * @param fileList RemoteFile文件列表
     */
    private void downloadFiles(List<RemoteFile> fileList) {
        // 下载时屏幕常亮
        keepScreenOn(true);

        // 显示HUD
        final KProgressHUD hud = createDeterminateProgressHUD(null, "Preparing to download " + fileList.size() + " files", 0);

        // 处理取消下载
        hud.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                final KProgressHUD hud = createIndeterminateProgressHUD(null, "Cancelling download...").show();
                MediaManager.getInstance().cancelDownload(new BWCommonCallbacks.BWCompletionCallback() {
                    @Override
                    public void onResult(BWError error) {
                        Log.e(">>>>>>", "cancelDownload " + error);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                hud.dismiss();
                            }
                        });
                    }
                });
            }
        });

        // Start downloading
        MediaManager.getInstance().downloadRemoteFiles(fileList, new MediaDownloadListener() {
            @Override
            public void started(final String fileName, final long total) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        hud.setLabel(fileName);
                        hud.setMaxProgress(total);
                        hud.setProgress(0);
                        hud.show();
                    }
                });
            }

            @Override
            public void transferred(final long length, final long total) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        hud.setProgress((int)length);
                        hud.setDetailsLabel("Downloaded: " + Utilities.memoryFormatter(length) + "/" + Utilities.memoryFormatter(total));
                    }
                });
            }

            @Override
            public void singleCompleted(File file) {
                // Scan file to MediaLibrary
                MediaManager.mediaScan(getApplicationContext(), file);
            }

            @Override
            public void completed() {
                Log.d(TAG, "download completed");
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        // 关闭屏幕常亮
                        keepScreenOn(false);
                        // 关闭选择模式
                        openChoiceMode(false);
                        // 刷新文件状态
                        refreshFileStatus(mRemoteMediaList);
                        // 关闭HUD
                        hud.dismiss();
                    }
                });
            }

            @Override
            public void aborted() {
                Log.d(TAG, "download aborted");
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        // 关闭屏幕常亮
                        keepScreenOn(false);
                        // 关闭选择模式
                        openChoiceMode(false);
                        // 刷新文件状态
                        refreshFileStatus(mRemoteMediaList);
                        // 关闭HUD
                        hud.dismiss();
                        // 提示下载已中止
                        showAlertDialog(CardMediaListActivity.this, null, "Download aborted");
                    }
                });
            }

            @Override
            public void failed() {
                Log.d(TAG, "download failed");
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        // 关闭屏幕常亮
                        keepScreenOn(false);
                        // 关闭选择模式
                        openChoiceMode(false);
                        // 刷新文件状态
                        refreshFileStatus(mRemoteMediaList);
                        // 关闭HUD
                        hud.dismiss();
                        // 提示下载已失败
                        showAlertDialog(CardMediaListActivity.this, null, "Download failed");
                    }
                });
            }
        });
    }

    /**
     * SwipeRefreshLayout开始刷新
     */
    @Override
    public void onRefresh() {
        if (isDeviceMode()) {
            loadRemoteVideoList();
        } else {
            loadLocalVideoList();
        }
    }

    /**
     * 点击选择RadioGroup (Device's or Local Files)
     */
    public void onCheckedChanged(RadioGroup group, @IdRes int checkedId) {
        mRemoteMediaList = null;
        mLocalMediaList = null;
        reloadListView();

        switch (checkedId) {
            case R.id.card_media_device_files_radioButton:
                loadRemoteVideoList();
                break;
            case R.id.card_media_local_files_radioButton:
                loadLocalVideoList();
                break;
        }
    }

    /**
     * 是否显示的是设备，否则是本地
     */
    private boolean isDeviceMode () {
        return mSegmentedGroup.getCheckedRadioButtonId() != R.id.card_media_local_files_radioButton;
    }

    /**
     * 点击列表
     */
    @OnItemClick(R.id.card_media_list_listView)
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
        if (isListChoiceMode()) {
            reloadListView();

            final ListView lv = (ListView)adapterView;
            SparseBooleanArray checkedItems = lv.getCheckedItemPositions();
            boolean checked = checkedItems.get(i);

            if (isDeviceMode()) {
                final RemoteFile remoteFile = mRemoteMediaList.get(i);
                final int position = i;

                if (checked) {
                    // File exists
                    if (remoteFile.isDownloaded()) {
                        boolean matchFileSize = MediaManagerHelper.getInstance().matchSizeOfDownloadedFileSize(remoteFile);

                        String message = null;
                        if (matchFileSize)
                            message = "Local file exists, with size " + Utilities.memoryFormatter(remoteFile.getLocalSize()) + ", overwrite?";
                        else
                            message = "Local file exists, overwrite?";

                        AlertDialog.Builder builder = new AlertDialog.Builder(CardMediaListActivity.this);
                        builder.setTitle("Overwrite confirm")
                                .setMessage(message)
                                .setPositiveButton("Overwrite", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        remoteFile.setResumeDownload(false);
                                    }
                                })
                                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        lv.setItemChecked(position, false);
                                    }
                                })
                                .setCancelable(false).create().show();
                    }
                    else if (remoteFile.isTempExist()) {
                        boolean matchFileSize = MediaManagerHelper.getInstance().matchSizeOfDownloadingFileSize(remoteFile);

                        // If downloading the same file
                        if (matchFileSize) {
                            // File size mismatch
                            if (remoteFile.isSizeMismatch()) {
                                if (remoteFile.getLocalSize() < remoteFile.getSize()) {

                                    String message = "Temp file exists, with size " + Utilities.memoryFormatter(remoteFile.getLocalSize()) + ", resume download or overwrite?";

                                    AlertDialog.Builder builder = new AlertDialog.Builder(CardMediaListActivity.this);
                                    builder.setTitle("Resuming download confirm")
                                            .setMessage(message)
                                            .setPositiveButton("Resume", new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialog, int which) {
                                                    remoteFile.setResumeDownload(true);
                                                }
                                            })
                                            .setNegativeButton("Overwrite", new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialog, int which) {
                                                    remoteFile.setResumeDownload(false);
                                                }
                                            })
                                            .setNeutralButton("Cancel", new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialog, int which) {
                                                    lv.setItemChecked(position, false);
                                                }
                                            })
                                            .setCancelable(false).create().show();
                                }
                                // 这个应该不会发生了
                                else if (remoteFile.getLocalSize() > remoteFile.getSize()) {

                                    AlertDialog.Builder builder = new AlertDialog.Builder(CardMediaListActivity.this);
                                    builder.setTitle("Overwrite confirm")
                                            .setMessage("Temp file exists, but the file size larger than the file in device, download again?")
                                            .setPositiveButton("YES", new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialog, int which) {
                                                    remoteFile.setResumeDownload(false);
                                                }
                                            })
                                            .setNegativeButton("NO", new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialog, int which) {
                                                    lv.setItemChecked(position, false);
                                                }
                                            })
                                            .setCancelable(false).create().show();
                                }
                            }
                            // 这个应该不会发生了
                            else {
                                // 是临时文件，大小又相同，麻烦
                                AlertDialog.Builder builder = new AlertDialog.Builder(CardMediaListActivity.this);
                                builder.setTitle("Overwrite confirm")
                                        .setMessage("Temp file exists, overwrite?")
                                        .setPositiveButton("YES", new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                                remoteFile.setResumeDownload(false);
                                            }
                                        })
                                        .setNegativeButton("NO", new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                                lv.setItemChecked(position, false);
                                            }
                                        })
                                        .setCancelable(false).create().show();
                            }
                        }
                        // Not the same file
                        else {
                            AlertDialog.Builder builder = new AlertDialog.Builder(CardMediaListActivity.this);
                            String message = "Temp file exists, with size " + Utilities.memoryFormatter(remoteFile.getLocalSize()) + ", but isn't from the file will be downloaded, overwrite?";
                            builder.setTitle("Overwrite confirm")
                                    .setMessage(message)
                                    .setPositiveButton("YES", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            remoteFile.setResumeDownload(false);
                                        }
                                    })
                                    .setNegativeButton("NO", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            lv.setItemChecked(position, false);
                                        }
                                    })
                                    .setCancelable(false).create().show();
                        }
                    }
                } else {
                    remoteFile.setResumeDownload(false);
                }
            }
        } else {
            if (isDeviceMode()) {
                // do something
            } else {
                File file = mLocalMediaList.get(i);
                String videoFilePath = file.getAbsolutePath();

                String strPath = videoFilePath;
                String strFileName = strPath.substring(strPath.lastIndexOf("/") + 1);

                VideoActivity.intentTo(this, videoFilePath, strFileName);
            }
        }
    }

    /**
     * 点击返回按钮
     */
    @OnClick(R.id.card_media_back_button)
    public void onBackButtonClicked(View view) {
        doExit();
    }

    /**
     * 点击下载按钮
     */
    @OnClick(R.id.card_media_download_button)
    public void onDownloadClicked(View view) {
        if (isListChoiceMode()) {
            List<RemoteFile> remoteFiles = getSelectedRemoteFiles();
            if (remoteFiles.size() > 0)
                downloadFiles(remoteFiles);
            else
                showAlertDialog(this, null, "Select at least one file");
        }
    }

    /**
     * 点击删除按钮
     */
    @OnClick(R.id.card_media_delete_button)
    public void onDeleteButtonClicked(View view) {
        if (isListChoiceMode()) {
            if (isDeviceMode()) {
                // TODO: delete remote files
                Log.w(TAG, "Not support DELETE yet");
            } else {
                List<File> files = getSelectedLocalFiles();
                if (files.size() > 0) {
                    for (File file : files) {
                        if (!file.delete()) {
                            Log.e(TAG, "Delete " + file.getAbsolutePath() + "Failed");
                        }
                        // Remove from settings
                        MediaManagerHelper.getInstance().removeDownloadedFileSizeItem(file.getName());
                    }
                    mListView.clearChoices();
                    loadLocalVideoList();
                } else {
                    showAlertDialog(this, null, "Select at least one file");
                }
            }
        }
    }

    /**
     * 点击选择按钮
     */
    @OnClick(R.id.card_media_select_button)
    public void onSelectButtonClicked(View view) {
        openChoiceMode(!isListChoiceMode());
        reloadListView();
    }

    /**
     * 设置ListView的选择模式
     */
    private void openChoiceMode(final boolean choiceMode) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (choiceMode) {
                    mListView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);

                    mSelectButton.setImageResource(R.mipmap.media_cancel);
//                    mSelectButton.setText("Cancel");
                    mDownloadButton.setVisibility(isDeviceMode() ? View.VISIBLE : View.GONE);
                    mDeleteButton.setVisibility(isDeviceMode() ? View.GONE : View.VISIBLE);

                    mSegmentedGroup.setVisibility(View.INVISIBLE);
                } else {
                    mListView.clearChoices();
                    mListView.setChoiceMode(ListView.CHOICE_MODE_NONE);

                    mSelectButton.setImageResource(R.mipmap.media_select);
//                    mSelectButton.setText("Select");
                    mDownloadButton.setVisibility(View.INVISIBLE);
                    mDeleteButton.setVisibility(View.INVISIBLE);

                    mSegmentedGroup.setVisibility(View.VISIBLE);
                }
            }
        });
    }

    /**
     * 列表是否处于选择模式
     * @return  选择模式
     */
    private boolean isListChoiceMode() {
        return mListView.getChoiceMode() != ListView.CHOICE_MODE_NONE;
    }

    /**
     * 重新载入列表
     */
    private void reloadListView() {
        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(new Runnable() {
            @Override
            public void run() {
                ((BaseAdapter) mListView.getAdapter()).notifyDataSetChanged();
            }
        });
    }

    /**
     * 刷新远程文件的对应的本地文件状态
     */
    private void refreshFileStatus(List<RemoteFile> remoteFiles) {
        MediaManagerHelper.getInstance().checkRemoteFiles(remoteFiles, new BWCommonCallbacks.BWCompletionCallback() {
            @Override
            public void onResult(BWError error) {
                reloadListView();
            }
        });
    }

    /**
     * 返回ListAdapter
     * @return  ListAdapter
     */
    protected ListAdapter getListAdapter() {
        return new BaseAdapter() {
            @Override
            public int getCount() {
                return isDeviceMode() ?
                        mRemoteMediaList != null ? mRemoteMediaList.size() : 0 :
                        mLocalMediaList != null ? mLocalMediaList.size() : 0;
            }

            @Override
            public Object getItem(int position) {
                return isDeviceMode() ? mRemoteMediaList.get(position) : mLocalMediaList.get(position);
            }

            @Override
            public long getItemId(int position) {
                return position;
            }

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                if (convertView == null) {
                    convertView = getLayoutInflater().inflate(R.layout.list_item_card_video, null);
                }

                SparseBooleanArray checkedItemsPositions =  mListView.getCheckedItemPositions();
                // Set background color
                convertView.setBackgroundColor(
                        isListChoiceMode() && checkedItemsPositions.get(position) ? listViewCheckedColor : listViewDefaultColor
                );

                ImageView downloadedImageView = (ImageView) convertView
                        .findViewById(R.id.list_item_card_video_file_downloaded_imageView);

                if (isDeviceMode()) {
                    // 显示远程文件
                    RemoteFile remoteFile = (RemoteFile) this.getItem(position);

                    String videoFileName = remoteFile.getName();
                    long videoFileSize = remoteFile.getSize();

                    // Set title
                    TextView nameTextView = (TextView) convertView
                            .findViewById(R.id.list_item_card_video_file_name_textView);
                    nameTextView.setText(videoFileName);
                    // Set detail
                    TextView sizeTextView = (TextView) convertView
                            .findViewById(R.id.list_item_card_video_file_size_textView);
                    sizeTextView.setText(Utilities.memoryFormatter(videoFileSize));

                    boolean downloaded = remoteFile.isDownloaded();
                    boolean matchFileSize = MediaManagerHelper.getInstance().matchSizeOfDownloadedFileSize(remoteFile);
//                    boolean sizeMismatch = remoteFile.isSizeMismatch();
//                    long localSize = remoteFile.getLocalSize();
                    // File tempExist flag
                    downloadedImageView.setVisibility(downloaded && matchFileSize ? View.VISIBLE : View.INVISIBLE);
                    // File size mismatch
//                    if (sizeMismatch)
//                        sizeTextView.setText(sizeTextView.getText() + " (" + Utilities.memoryFormatter(localSize) + ")");
                } else {
                    // 显示本地文件
                    File file = (File) this.getItem(position);

                    String videoFileName = file.getName();
                    long videoFileSize = file.length();

                    // Set title
                    TextView nameTextView = (TextView) convertView
                            .findViewById(R.id.list_item_card_video_file_name_textView);
                    nameTextView.setText(videoFileName);
                    // Set detail
                    TextView sizeTextView = (TextView) convertView
                            .findViewById(R.id.list_item_card_video_file_size_textView);
                    sizeTextView.setText(Utilities.memoryFormatter(videoFileSize));

                    // Hide downloaded image
                    downloadedImageView.setVisibility(View.GONE);
                }

                return convertView;
            }
        };
    }

    /**
     * 创建无进度的ProgressHUD
     * @param label         Title
     * @param detailsLabel  Detail
     * @return  KProgressHUD
     */
    private KProgressHUD createIndeterminateProgressHUD(String label, String detailsLabel) {
        return KProgressHUD.create(this)
                .setStyle(KProgressHUD.Style.SPIN_INDETERMINATE)
                .setLabel(label, titleTextColor)
                .setDetailsLabel(detailsLabel, detailTextColor)
                .setDimAmount(0.5f)
                .setBackgroundColor(mainBackgroundColor);
    }

    /**
     * 创建有进度的ProgressHUD
     * @param label         Title
     * @param detailsLabel  Detail
     * @param maxProgress   The max progress
     * @return  KprogressHUD
     */
    private KProgressHUD createDeterminateProgressHUD(String label, String detailsLabel, long maxProgress) {
        return KProgressHUD.create(this)
                .setStyle(KProgressHUD.Style.BAR_DETERMINATE)
                .setLabel(label, titleTextColor)
                .setDetailsLabel(detailsLabel, detailTextColor)
                .setMaxProgress(maxProgress)
                .setCancellable(true)
                .setDimAmount(0.5f)
                .setBackgroundColor(mainBackgroundColor);
    }

    /**
     * Show AlertDialog
     */
    private void showAlertDialog(Context context, String title, String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(title)
                .setMessage(message)
                .setPositiveButton("OK", null)
                .create().show();
    }

    /* BWSocket */

//    private void doStopRemoteRecord() {
//    }

    private void doStartRemoteRecord() {

        final KProgressHUD hud = createIndeterminateProgressHUD("Exiting remote file manager...", null).show();

        BWSocketWrapper.getInstance().startRecord(new BWCommonCallbacks.BWCompletionCallback() {
            @Override
            public void onResult(BWError error) {
                Log.e(TAG, "doStartRemoteRecord " + error);

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        hud.dismiss();
                        // 退出
                        finish();
                        // Activity slide from left
                        overridePendingTransition(
                                android.R.anim.slide_in_left,
                                android.R.anim.slide_out_right
                        );
                    }
                });
            }
        });
    }

}
