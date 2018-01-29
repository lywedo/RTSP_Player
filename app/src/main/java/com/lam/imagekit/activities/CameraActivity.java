package com.lam.imagekit.activities;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.media.MediaPlayer;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Chronometer;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TableLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.lam.imagekit.AppContext;
import com.lam.imagekit.BaseActivity;
import com.lam.imagekit.BuildConfig;
import com.lam.imagekit.R;
import com.lam.imagekit.application.Constants;
import com.lam.imagekit.data.CameraParam;
import com.lam.imagekit.services.CameraBroadCtrl;
import com.lam.imagekit.utils.Utilities;
import com.lam.imagekit.widget.freespacemonitor.FreeSpaceMonitor;
import com.lam.imagekit.widget.media.IRenderView;
import com.lam.imagekit.widget.media.IjkVideoView;

import java.io.File;
import java.util.Arrays;

import tv.danmaku.ijk.media.player.IMediaPlayer;
import tv.danmaku.ijk.media.player.IjkMediaPlayer;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static com.lam.imagekit.application.Constants.CODE_WRITE_EXTERNAL_STORAGE;
import static com.lam.imagekit.services.CameraBroadCtrl.MSG_BTN_PARAM_LONG_PRESS;
import static com.lam.imagekit.services.CameraBroadCtrl.MSG_CAMERABROADCTRL_TAKEPHOTOS;
import static com.lam.imagekit.services.CameraBroadCtrl.MSG_CAMERABROADCTRL_ZOOMIN;
import static com.lam.imagekit.services.CameraBroadCtrl.MSG_CAMERABROADCTRL_ZOOMOUT;
import static com.lam.imagekit.widget.media.IRenderView.AR_ASPECT_FILL_PARENT;
import static com.lam.imagekit.widget.media.IjkVideoView.RENDER_TEXTURE_VIEW;
import static com.lam.imagekit.widget.media.IjkVideoView.RTP_JPEG_PARSE_PACKET_METHOD_FILL;

//import com.lam.imagekit.widget.multi_image_selector.MultiImageSelectorActivity;

public class CameraActivity extends BaseActivity {
    private static final String TAG = "CamereActivity";
    // 播放器
    private static final int VIDEO_VIEW_RENDER = RENDER_TEXTURE_VIEW;
    private static final int VIDEO_VIEW_ASPECT = AR_ASPECT_FILL_PARENT;
    private static final int RTP_JPEG_PARSE_PACKET_METHOD = RTP_JPEG_PARSE_PACKET_METHOD_FILL;
    private static final int RECONNECT_INTERVAL = 500;
    // 剩余空间监控
    FreeSpaceMonitor mFreeSpaceMonitor;
    ProgressBar mProgressBar;
    ImageView mCafe;
    IjkVideoView mVideoView;
    ImageButton mTakePhotoButton;
    ImageButton mTakeVideoButton;
    ImageButton mAlbumButton;
    ImageButton mSettingButton;
    LinearLayout mControlLayout;
    LinearLayout mControlRightLayout;
    TextView mPointText;
    String mVideoPath;
    TableLayout mHudView;
    ImageButton mRotateButton;
    ImageButton mFullButton;
    ImageView mResolutionButton;
    boolean recording = false;
    Chronometer mChronometer;
    boolean isButtonsVisible = true;    // 3D View中Buttons是否可见
    CountDownTimer hideButtonsTimer;    // 隐藏按键的倒计时(3D View)

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_main);
        initSplash();
        initPermission();
        initViews();
        setVideo();
    }
    SplashFragment splashFragment;
    public void initSplash() {
        if (AppContext.getInstance().splashed){
            return;
        }
        splashFragment = new SplashFragment();
        getSupportFragmentManager().beginTransaction().add(R.id.rl_main, splashFragment).commit();
        splashHandler.sendEmptyMessageDelayed(0, 3000);
    }
    Handler splashHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            getSupportFragmentManager().beginTransaction().remove(splashFragment).commit();
        }
    };

    public static class SplashFragment extends Fragment{

        @Nullable
        @Override
        public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
            AppContext.getInstance().splashed = true;
            return inflater.inflate(R.layout.activity_splash, container, false);
        }
    }

    private void initPermission() {
        if(!hasPermission("android.permission.WRITE_EXTERNAL_STORAGE")){
            requestPermission(CODE_WRITE_EXTERNAL_STORAGE, "android.permission.WRITE_EXTERNAL_STORAGE");

        }else {
//            return true;
        }
    }
    private void takePhotoSound(){
        MediaPlayer music;
        music = MediaPlayer.create(this, R.raw.shutter);
        music.start();
        music.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mediaPlayer) {
                mediaPlayer.release();
            }
        });
    }


    private Handler m_handler = new Handler();
    @Override
    protected void onResume() {
        super.onResume();
        // 开启屏幕常亮
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        mVideoView.setOnInfoListener(new IMediaPlayer.OnInfoListener() {
            @Override
            public boolean onInfo(IMediaPlayer mp, int what, int extra) {
                switch (what) {
                    case IMediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START:

                        updataRotation(rotatoin);
                        m_handler.post(new Runnable() {
                            @Override
                            public void run() {
                                AppContext.getInstance().getBroadCtrl().getuvc();
                            }
                        });
                        break;
                    default:
                }
                return false;
            }
        });
        mVideoView.setRender(VIDEO_VIEW_RENDER);
        mVideoView.setAspectRatio(IRenderView.AR_ASPECT_FILL_PARENT );
        mVideoView.setVideoPath(mVideoPath);
        mVideoView.start();
    }

    @Override
    protected void onStart() {
        super.onStart();
        updateScreen();

        AppContext.getInstance().getBroadCtrl().hello();
        AppContext.getInstance().getBroadCtrl().getuvc();
        AppContext.getInstance().getBroadCtrl().setCameraBroadCtrlCallback(new CameraBroadCtrl.CameraBroadCtrlCallback() {
            @Override
            public int process(int what, final int param1, int parma2) {
                switch (what){
                    case MSG_CAMERABROADCTRL_TAKEPHOTOS:
                        CameraActivity.this.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if(MSG_BTN_PARAM_LONG_PRESS == param1){
                                    record();
                                }else {
                                    if(recording){
                                        record();
                                    }else {
                                        takePicture();
                                    }
                                }
                            }
                        });
                        break;
                    case MSG_CAMERABROADCTRL_ZOOMIN:
                        CameraActivity.this.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                //Toast.makeText(CameraActivity.this, "zoomin", Toast.LENGTH_SHORT).show();
                                mVideoView.scaleDownView();
                            }
                        });
                        break;
                    case MSG_CAMERABROADCTRL_ZOOMOUT:
                        CameraActivity.this.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                //Toast.makeText(CameraActivity.this, "zoomout", Toast.LENGTH_SHORT).show();
                                mVideoView.scaleUpView();
                            }
                        });
                        break;
                }
                return 0;
            }
        });
    }

    @Override
    protected void onStop() {
        super.onStop();
        AppContext.getInstance().getBroadCtrl().setCameraBroadCtrlCallback(null);
    }

    private void setVideo() {
        // handle arguments
        mVideoPath = Constants.RTSP_ADDRESS;
        // init UI
        mHudView = (TableLayout) findViewById(R.id.hud_view);
        mHudView.setVisibility(View.GONE);
        // init player
        IjkMediaPlayer.loadLibrariesOnce(null);
        IjkMediaPlayer.native_profileBegin("libijkplayer.so");
        mVideoView.setRtpJpegParsePacketMethod(RTP_JPEG_PARSE_PACKET_METHOD);
        mVideoView.setRender(VIDEO_VIEW_RENDER);
        //mVideoView.setAspectRatio(IRenderView.AR_ASPECT_FIT_PARENT);
        mVideoView.setHudView(mHudView);
        mVideoView.setOnPreparedListener(new IMediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(IMediaPlayer mp) {
                onStartPlayback();
            }
        });
        mVideoView.setOnErrorListener(new IMediaPlayer.OnErrorListener() {
            @Override
            public boolean onError(IMediaPlayer mp, int what, int extra) {
                stopAndRestartPlayback();
                return true;
            }
        });
        mVideoView.setOnReceivedRtcpSrDataListener(new IMediaPlayer.OnReceivedRtcpSrDataListener() {
            @Override
            public void onReceivedRtcpSrData(IMediaPlayer mp, byte[] data) {
                Log.d(TAG, new String(data) + Arrays.toString(data));
            }
        });
        mVideoView.setOnTookPictureListener(new IMediaPlayer.OnTookPictureListener() {
            @Override
            public void onTookPicture(IMediaPlayer mp, int resultCode, String fileName) {
                String toastText = getResources().getString(R.string.control_panel_alert_save_photo_fail);
                if (!recording){
                    if (resultCode == 1) {
                        // 播放咔嚓声
                        takePhotoSound();
                    } else if (resultCode == 0 && fileName != null) {
                        File file = new File(fileName);
                        if (file.exists()) {
                            mediaScan(file);
                            // Show toast
//                            toastText = getResources().getString(R.string.control_panel_alert_save_photo_success) + fileName;
                            toastText = getResources().getString(R.string.take_photo_ok);
                        }
                        Toast toast = Toast.makeText(CameraActivity.this, toastText, Toast.LENGTH_SHORT);
                        toast.setGravity(Gravity.CENTER, 0, 0);
                        toast.show();
                    } else if (resultCode < 0) {
                        Toast toast = Toast.makeText(CameraActivity.this, toastText, Toast.LENGTH_SHORT);
                        toast.setGravity(Gravity.CENTER, 0, 0);
                        toast.show();
                    }
                }
            }
        });
        mVideoView.setOnRecordVideoListener(new IMediaPlayer.OnRecordVideoListener() {
            @Override
            public void onRecordVideo(IMediaPlayer mp, final int resultCode, final String fileName) {
                Handler handler = new Handler(getMainLooper());
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        String noteText = null;
                        if (resultCode < 0) {
                            // 停止监控剩余空间
                            if (mFreeSpaceMonitor != null)
                                mFreeSpaceMonitor.stop();

                            recording = false;
                            noteText = getResources().getString(R.string.control_panel_alert_write_video_file_error);
                            Toast.makeText(
                                    CameraActivity.this,
                                    noteText,
                                    Toast.LENGTH_SHORT
                            ).show();
                            mTakeVideoButton.setImageResource(R.mipmap.con_video);
                            // 隐藏录像计时器
                            showChronometer(false);
                        } else if (resultCode == 0) {
                            recording = true;
                            // 开启录像计时
                            showChronometer(true);
                            mTakeVideoButton.setImageResource(R.mipmap.con_video_h);
                            // 开始监控剩余空间
                            mFreeSpaceMonitor.setListener(new FreeSpaceMonitor.FreeSpaceCheckerListener() {
                                @Override
                                public void onExceed() {
                                    // 如果剩余空间低于阈值，停止录像
                                    if (recording)
                                        mVideoView.stopRecordVideo();
                                }
                            });
                            mFreeSpaceMonitor.start();
                        } else {
                            // 停止监控剩余空间
                            if (mFreeSpaceMonitor != null)
                                mFreeSpaceMonitor.stop();

                            // Scan file to media library
                            File file = new File(fileName);
                            mediaScan(file);

//                            noteText = getResources().getString(R.string.control_panel_alert_record_video_success);
//                            Toast.makeText(
//                                    CameraActivity.this,
//                                    noteText + fileName,
//                                    Toast.LENGTH_SHORT
//                            ).show();
                            Toast toast = Toast.makeText(CameraActivity.this, getResources().getString(R.string.take_video_ok), Toast.LENGTH_SHORT);
                            toast.setGravity(Gravity.CENTER, 0, 0);
                            toast.show();
                            mTakeVideoButton.setImageResource(R.mipmap.con_video);
                            // 隐藏录像计时器
                            showChronometer(false);

                            // set flag
                            recording = false;
                        }
                    }
                });
            }

        });
        mVideoView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_UP) {
                        if (isButtonsVisible) {
                            setButtonsVisible(false);
                            if (hideButtonsTimer != null) {
                                hideButtonsTimer.cancel();
                                hideButtonsTimer = null;
                            }
                        } else {
                            setButtonsVisible(true);
                            setHideButtonsTimer();
                        }
                }
                return false;
            }
        });
        mVideoView.setOnCompletionListener(new IMediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(IMediaPlayer mp) {
                stopAndRestartPlayback();
//                mVideoView.stopPlayback();
//                mVideoView.release(true);
//                mVideoView.stopBackgroundPlay();
            }
        });
        if (mVideoPath != null)
            mVideoView.setVideoPath(mVideoPath);
        else {
            Log.e(TAG, "Null Data Source\n");
            finish();
            return;
        }

        IjkMediaPlayer.getMessageBus().register(this);

        mTakePhotoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                takePicture();
            }
        });
    }

    private void takePicture(){
        if (recording){
            Toast.makeText(CameraActivity.this, getString(R.string.isRecording),Toast.LENGTH_SHORT).show();
        }else {
            // Take a photo
            String photoFilePath = Utilities.getPhotoDirPath();
            String photoFileName = Utilities.getMediaFileName();
            try {
                CameraParam cameraParam = AppContext.getInstance().getCameraParam();
                mVideoView.takePicture(photoFilePath, photoFileName, cameraParam.curWidth, cameraParam.curHeight, 1);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    private int rotatoin = 0;
    private void rotate90(){
        rotatoin += 90;
        updataRotation(rotatoin);
    }
    private void initViews() {
        /**
         * 录像计时器
         */
        mChronometer = (Chronometer)findViewById(R.id.control_panel_chronometer);
        mControlLayout = findViewById(R.id.ll_control);
        mVideoView = findViewById(R.id.video_view);
        mTakePhotoButton = findViewById(R.id.control_panel_take_photo_button);
        mTakeVideoButton = findViewById(R.id.control_panel_record_video_button);
        mAlbumButton = findViewById(R.id.control_panel_review_button);
        mSettingButton = findViewById(R.id.control_panel_setting_button);
        mResolutionButton = findViewById(R.id.control_panel_resolution_button);
        mResolutionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!recording){
                    startActivity(new Intent(CameraActivity.this, SettingActivity.class));
                }else {
                    Toast.makeText(CameraActivity.this, getString(R.string.isRecording), Toast.LENGTH_SHORT).show();
                }
                mControlRightLayout.setVisibility(GONE);
            }
        });
//        mProgressBar = findViewById(R.id.control_panel_progressBar);
        mCafe = findViewById(R.id.iv_cafe);
        ObjectAnimator animator = ObjectAnimator.ofFloat(mCafe, "translationY", 0f, 20f,0f);
        animator.setRepeatCount(ValueAnimator.INFINITE);
        animator.setDuration(1500);
        animator.start();
        mPointText = findViewById(R.id.tv_point);
        mControlRightLayout = findViewById(R.id.control_panel_right_menubar);
        mFullButton = findViewById(R.id.control_panel_full_button);
        mFullButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                fullScreenSwitch();
            }
        });
        mAlbumButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(CameraActivity.this, ReviewActivity.class));
//                startActivity(new Intent(CameraActivity.this, MultiImageSelectorActivity.class));
            }
        });
        mTakeVideoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                record();
            }
        });
        mSettingButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mControlRightLayout.getVisibility()!=VISIBLE){
                    mControlRightLayout.setVisibility(View.VISIBLE);
                }else {
                    mControlRightLayout.setVisibility(GONE);
                }
            }
        });
        if (BuildConfig.DEBUG){
            mSettingButton.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View view) {
                    if (mHudView.getVisibility() != View.VISIBLE)
                        mHudView.setVisibility(View.VISIBLE);
                    else
                        mHudView.setVisibility(View.GONE);
                    return true;
                }
            });
        }
        mRotateButton = findViewById(R.id.control_panel_rotate_screen_button);
        mRotateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                rotate90();
            }
        });
    }

    private void record(){
        if (recording) {
            mVideoView.stopRecordVideo();
        }
        else {
            mFreeSpaceMonitor = new FreeSpaceMonitor();
            if (mFreeSpaceMonitor.checkFreeSpace()) {
                String videoFilePath = Utilities.getVideoDirPath();
                String videoFileName = Utilities.getMediaFileName();
                // Start to record video
                try {
                    mVideoView.startRecordVideo(videoFilePath, videoFileName + ".avi", -1, -1);
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
            }
            else {
                // 提示剩余空间不足
                long threshold = mFreeSpaceMonitor.getThreshold();
                float megabytes = threshold / (1024 * 1024);
                String toastString = getResources().getString(R.string.control_panel_insufficient_storage_alert, megabytes);
                Toast.makeText(CameraActivity.this, toastString, Toast.LENGTH_SHORT).show();
            }
        }
    }
    /**
     * 播放开始后执行
     */
    private void onStartPlayback() {
        // 隐藏BackgroundView,ProgressBar
//        mBackgroundView.setVisibility(View.INVISIBLE);
//        mProgressBar.setVisibility(View.INVISIBLE);
        mCafe.setVisibility(View.INVISIBLE);
        mPointText.setVisibility(View.INVISIBLE);
    }

    /**
     * 关闭播放器并重新开始播放
     * 错误发生的时候调用
     */
    private void stopAndRestartPlayback() {
        // 显示BackgroundView,ProgressBar
//        mBackgroundView.setVisibility(View.VISIBLE);
//        mProgressBar.setVisibility(View.VISIBLE);
        mCafe.setVisibility(VISIBLE);
        mPointText.setVisibility(VISIBLE);

        mVideoView.post(new Runnable() {
            @Override
            public void run() {
                mVideoView.stopPlayback();
                mVideoView.release(true);
                mVideoView.stopBackgroundPlay();
            }
        });
        mVideoView.postDelayed(new Runnable() {
            @Override
            public void run() {
                mVideoView.setRender(VIDEO_VIEW_RENDER);
                //mVideoView.setAspectRatio(IRenderView.AR_4_3_FIT_PARENT);
                mVideoView.setVideoPath(mVideoPath);
                mVideoView.start();
            }
        }, RECONNECT_INTERVAL);
    }

    /**
     * 扫描添加媒体文件到系统媒体库
     *
     * @param file 媒体文件
     */
    private void mediaScan(File file) {
        MediaScannerConnection.scanFile(getApplicationContext(),
                new String[]{file.getAbsolutePath()}, null,
                new MediaScannerConnection.OnScanCompletedListener() {
                    @Override
                    public void onScanCompleted(String path, Uri uri) {
                        Log.v("MediaScanWork", "file " + path
                                + " was scanned seccessfully: " + uri);
                    }
                });
    }
    /**
     * 显示或者隐藏Chronometer
     * @param bShow 显示开关
     */
    private void showChronometer(boolean bShow) {
        if (bShow) {
            mChronometer.setVisibility(View.VISIBLE);
            mChronometer.setBase(SystemClock.elapsedRealtime());
            mChronometer.start();
        } else {
            mChronometer.stop();
            mChronometer.setVisibility(View.INVISIBLE);
            mChronometer.setText("");
        }
    }
    /**
     * 设置按键的显示
     * @param visible   显示开关
     */
    private void setButtonsVisible(boolean visible) {
        if (visible) {
            isButtonsVisible = true;
            mControlLayout.setVisibility(View.VISIBLE);
        } else {
            isButtonsVisible = false;
            mControlLayout.setVisibility(View.INVISIBLE);
            mControlRightLayout.setVisibility(View.INVISIBLE);
        }
    }
    /**
     * 设置隐藏按键的定时器
     */
    private void setHideButtonsTimer() {
        hideButtonsTimer = new CountDownTimer(3000, 3000) {
            @Override
            public void onTick(long l) {

            }

            @Override
            public void onFinish() {
//                mSettingButton.setImageResource(R.mipmap.con_extra_settings);
                setButtonsVisible(false);
            }
        }.start();
    }
    @Override
    protected void onPause() {
        super.onPause();
        // 关闭屏幕常亮
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        // 停止录像
        if (recording)
            mVideoView.stopRecordVideo();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        IjkMediaPlayer.getMessageBus().unregister(this);

//        mSoundPool.release();

        IjkMediaPlayer.native_profileEnd();
    }

    private void fullScreenSwitch(){
        if(mVideoView.getAspectRatio() == IRenderView.AR_ASPECT_FIT_PARENT) {
            mVideoView.setAspectRatio(IRenderView.AR_ASPECT_FILL_PARENT);
        }else{
            mVideoView.setAspectRatio(IRenderView.AR_ASPECT_FIT_PARENT);
        }
    }

    private int backPressedTimes = 0;
    @Override
    public void onBackPressed() {
        backPressedTimes++;
        mHandler.sendEmptyMessageDelayed(0, 2000);
        if (backPressedTimes > 1) {
            mHandler.removeMessages(0);
            android.os.Process.killProcess(android.os.Process.myPid());
//			finish();
        } else {
            Toast.makeText(this, R.string.click_back_again, Toast.LENGTH_SHORT).show();
        }
        // super.onBackPressed();
    }
    private Handler mHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 0:
                    backPressedTimes = 0;
                    break;
            }
            super.handleMessage(msg);
        }
    };

    private int updataRotation(int rotation){
        int rotationBase = 0;
        if(this.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            rotationBase = 0;
        } else if (this.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
            rotationBase = 90;
        }

        int r = rotation + rotationBase;
        r %= 360;

        mVideoView.setVideoRotation(r);

        return 0;
    }

    private void updateScreen(){

        updataRotation(rotatoin);
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        RelativeLayout.LayoutParams rightParams = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        if (this.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
            mControlLayout.setOrientation(LinearLayout.VERTICAL);
            params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
            params.addRule(RelativeLayout.CENTER_VERTICAL);
            params.rightMargin = Utilities.dip2px(this,20);
            for (int i = 0; i < mControlLayout.getChildCount(); i++) {
                LinearLayout.LayoutParams childParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                mControlLayout.getChildAt(i).setLayoutParams(childParams);
            }

            mControlRightLayout.setOrientation(LinearLayout.HORIZONTAL);
            rightParams.addRule(RelativeLayout.LEFT_OF, mControlLayout.getId());
            rightParams.addRule(RelativeLayout.ALIGN_BOTTOM, mControlLayout.getId());
        }else {
            mControlLayout.setOrientation(LinearLayout.HORIZONTAL);
            mControlLayout.setGravity(Gravity.CENTER_HORIZONTAL|Gravity.BOTTOM);
            params.rightMargin = 0;
            params.addRule(RelativeLayout.CENTER_HORIZONTAL);
            params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
            params.bottomMargin = Utilities.dip2px(this,20);
            mControlRightLayout.setOrientation(LinearLayout.VERTICAL);
            rightParams.addRule(RelativeLayout.ALIGN_RIGHT, mControlLayout.getId());
            rightParams.addRule(RelativeLayout.ABOVE, mControlLayout.getId());
        }
        mControlLayout.setLayoutParams(params);
        mControlRightLayout.setLayoutParams(rightParams);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        updateScreen();
}
}
