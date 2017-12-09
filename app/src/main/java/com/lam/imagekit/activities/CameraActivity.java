package com.lam.imagekit.activities;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaScannerConnection;
import android.media.SoundPool;
import android.net.Uri;
import android.os.Build;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Chronometer;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TableLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.lam.imagekit.BaseActivity;
import com.lam.imagekit.R;
import com.lam.imagekit.application.Constants;
import com.lam.imagekit.utils.AppManager;
import com.lam.imagekit.utils.CameraBroadCtrlHelper;
import com.lam.imagekit.utils.ConnectUtils;
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
import static com.lam.imagekit.widget.media.IRenderView.AR_ASPECT_FILL_PARENT;
import static com.lam.imagekit.widget.media.IjkVideoView.RENDER_TEXTURE_VIEW;
import static com.lam.imagekit.widget.media.IjkVideoView.RTP_JPEG_PARSE_PACKET_METHOD_FILL;

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
    ImageButton mPhoto;
    ImageButton mVideo;
    ImageButton mAlbum;
    ImageButton mSetting;
    LinearLayout mControl;
    LinearLayout mControlRight;
    TextView mPointText;
    String mVideoPath;
    TableLayout mHudView;
    SoundPool mSoundPool;
    ImageButton mRotate;
    ImageButton mFull;
    boolean recording = false;
    Chronometer mChronometer;
    boolean isButtonsVisible = true;    // 3D View中Buttons是否可见
    CountDownTimer hideButtonsTimer;    // 隐藏按键的倒计时(3D View)
    float mScaleX = -1;
    float mScaleY = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_main);
        initPermission();
        initSound();
        initViews();
        setVideo();
        sendUDP(CameraBroadCtrlHelper.cmdReqBuf(AppManager.getAppName(this),1,1).toString());
    }

    private void initDisplayMetrics() {
        DisplayMetrics dm = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dm);

        mScaleX = ((float) dm.heightPixels/(float) mVideoView.getMeasuredWidth());
        mScaleY = ((float) dm.widthPixels/(float) mVideoView.getMeasuredHeight());
        Log.d("full", "dm.heightPixels:"+dm.heightPixels+"mVideoView.getWidth()"+mVideoView.getWidth());
        Log.d("full", "x"+mScaleX+"Y"+mScaleY);

    }

    private void initPermission() {
        if(!hasPermission("android.permission.WRITE_EXTERNAL_STORAGE")){
            requestPermission(CODE_WRITE_EXTERNAL_STORAGE, "android.permission.WRITE_EXTERNAL_STORAGE");

        }else {
//            return true;
        }
    }

    private void initSound() {
        // 载入声音资源
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            SoundPool.Builder builder = new SoundPool.Builder();
            builder.setMaxStreams(1);

            AudioAttributes.Builder attrBuilder = new AudioAttributes.Builder();
            attrBuilder.setLegacyStreamType(AudioManager.STREAM_SYSTEM);

            builder.setAudioAttributes(attrBuilder.build());
            mSoundPool = builder.build();
        } else {
            mSoundPool = new SoundPool(1, AudioManager.STREAM_SYSTEM, 5);
        }
        mSoundPool.load(this, R.raw.shutter, 1);
    }

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
                        rotate90();
                        break;
                    default:
                }
                return false;
            }
        });
        mVideoView.setRender(VIDEO_VIEW_RENDER);
        mVideoView.setAspectRatio(IRenderView.AR_ASPECT_FIT_PARENT );
        isFull = false;
        mVideoView.setVideoPath(mVideoPath);
        mVideoView.start();
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
                if (resultCode == 1) {
                    // 播放咔嚓声
                    mSoundPool.play(1, 1, 1, 0, 0, 1);
                } else if (resultCode == 0 && fileName != null) {
                    File file = new File(fileName);
                    if (file.exists()) {
                        mediaScan(file);
                        // Show toast
                        toastText = getResources().getString(R.string.control_panel_alert_save_photo_success) + fileName;
                    }
                    Toast.makeText(CameraActivity.this, toastText, Toast.LENGTH_SHORT).show();
                } else if (resultCode < 0) {
                    Toast.makeText(CameraActivity.this, toastText, Toast.LENGTH_SHORT).show();
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
                            mVideo.setImageResource(R.mipmap.con_video);
                            // 隐藏录像计时器
                            showChronometer(false);
                        } else if (resultCode == 0) {
                            recording = true;
                            // 开启录像计时
                            showChronometer(true);
                            mVideo.setImageResource(R.mipmap.con_video_h);
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

                            noteText = getResources().getString(R.string.control_panel_alert_record_video_success);
                            Toast.makeText(
                                    CameraActivity.this,
                                    noteText + fileName,
                                    Toast.LENGTH_SHORT
                            ).show();
                            mVideo.setImageResource(R.mipmap.con_video);
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
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
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
                mVideoView.stopPlayback();
                mVideoView.release(true);
                mVideoView.stopBackgroundPlay();
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

        mPhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (recording){
                    Toast.makeText(CameraActivity.this, "正在录像",Toast.LENGTH_SHORT).show();
                }else {
                    // Take a photo
                    String photoFilePath = Utilities.getPhotoDirPath();
                    String photoFileName = Utilities.getMediaFileName();
                    try {
                        mVideoView.takePicture(photoFilePath, photoFileName, -1, -1, 1);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                }
        });
    }
    boolean isFull = false;
    private int rotatoin = 0;
    private void rotate90(){
        rotatoin += 90;
        rotatoin %= 360;
        mVideoView.setVideoRotation(rotatoin);
    }
    private void initViews() {
        /**
         * 录像计时器
         */
        mChronometer = (Chronometer)findViewById(R.id.control_panel_chronometer);
        mControl = findViewById(R.id.ll_control);
        mVideoView = findViewById(R.id.video_view);
        mPhoto = findViewById(R.id.control_panel_take_photo_button);
        mVideo = findViewById(R.id.control_panel_record_video_button);
        mAlbum = findViewById(R.id.control_panel_review_button);
        mSetting = findViewById(R.id.control_panel_setting_button);
//        mProgressBar = findViewById(R.id.control_panel_progressBar);
        mCafe = findViewById(R.id.iv_cafe);
        ObjectAnimator animator = ObjectAnimator.ofFloat(mCafe, "translationY", 0f, 20f,0f);
        animator.setRepeatCount(ValueAnimator.INFINITE);
        animator.setDuration(1500);
        animator.start();
        mPointText = findViewById(R.id.tv_point);
        mControlRight = findViewById(R.id.control_panel_right_menubar);
        mFull = findViewById(R.id.control_panel_full_button);
        mFull.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                isFull = !isFull;
                fullScreen(isFull);
            }
        });
        mAlbum.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(CameraActivity.this, ReviewActivity.class));
            }
        });
        mVideo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
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
        });
        mSetting.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mControlRight.getVisibility()!=VISIBLE){
                    mControlRight.setVisibility(View.VISIBLE);
                }else {
                    mControlRight.setVisibility(GONE);
                }
            }
        });
        mRotate = findViewById(R.id.control_panel_rotate_screen_button);
        mRotate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                rotate90();
            }
        });
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
            mControl.setVisibility(View.VISIBLE);
        } else {
            isButtonsVisible = false;
            mControl.setVisibility(View.INVISIBLE);
            mControlRight.setVisibility(View.INVISIBLE);
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
                mSetting.setImageResource(R.mipmap.con_extra_settings);
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

        mSoundPool.release();

        IjkMediaPlayer.native_profileEnd();
    }

    private void fullScreen(boolean isFull){
        if(isFull) {
            mVideoView.setAspectRatio(IRenderView.AR_ASPECT_FILL_PARENT);
        }else{
            mVideoView.setAspectRatio(IRenderView.AR_ASPECT_FIT_PARENT);
        }
//        if (mScaleY < 0 && mScaleX < 0){
//            initDisplayMetrics();
//        }
//        if (!isFull){
//            ObjectAnimator animatorx = ObjectAnimator.ofFloat(mVideoView, "scaleX", 1f, mScaleX);
//            animatorx.start();
//            ObjectAnimator animatory = ObjectAnimator.ofFloat(mVideoView, "scaleY", 1f, mScaleY);
//            animatory.start();
//            mVideoView.setScaleX(mScaleX);
//            mVideoView.setScaleY(mScaleY);
//            this.isFull = true;
//        }else {
//            ObjectAnimator animatorx = ObjectAnimator.ofFloat(mVideoView, "scaleX", mScaleX, 1f);
//            animatorx.start();
//            ObjectAnimator animatory = ObjectAnimator.ofFloat(mVideoView, "scaleY", mScaleY, 1f);
//            animatory.start();
//            mVideoView.setScaleX(mScaleX);
//            mVideoView.setScaleY(mScaleY);
//            this.isFull = false;
//        }
    }

    private void sendUDP(final String message){
        new Thread(new Runnable() {
            @Override
            public void run() {
                for (;;) {
                    ConnectUtils utils = null;
                    try {
                        utils = ConnectUtils.getInstance();
                        utils.setTimeOut(5000);// 设置超时为5s
                        // 向服务器发数据
                        utils.send(Constants.SERVER_ADDRESS, Constants.SERVER_PORT, message.getBytes());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
//                try {
//                    ConnectUtils.getInstance().close();
//                } catch (Exception e) {
//                    e.printStackTrace();
//                }

            }
        }).start();
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
            Toast.makeText(this, "再按一次退出程序", Toast.LENGTH_SHORT).show();
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
}
