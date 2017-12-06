package cn.com.buildwin.gosky.activities;

import android.content.Intent;
import android.graphics.PointF;
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
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Chronometer;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TableLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.squareup.otto.Subscribe;

import java.io.File;
import java.util.Timer;
import java.util.TimerTask;

import buildwin.common.Utilities;
import cn.com.buildwin.gosky.widget.audiorecognizer.VoiceRecognizer;
import cn.com.buildwin.gosky.widget.flycontroller.FlyController;
import cn.com.buildwin.gosky.widget.flycontroller.FlyControllerDelegate;
import cn.com.buildwin.gosky.application.Constants;
import cn.com.buildwin.gosky.R;
import cn.com.buildwin.gosky.widget.freespacemonitor.FreeSpaceMonitor;
import cn.com.buildwin.gosky.widget.rudderview.RudderView;
import cn.com.buildwin.gosky.application.Settings;
import cn.com.buildwin.gosky.widget.trackview.TrackView;
import cn.com.buildwin.gosky.widget.media.IjkVideoView;
import tv.danmaku.ijk.media.player.IMediaPlayer;
import tv.danmaku.ijk.media.player.IjkMediaPlayer;

import static cn.com.buildwin.gosky.widget.media.IRenderView.AR_MATCH_PARENT;
import static cn.com.buildwin.gosky.widget.media.IjkVideoView.RENDER_TEXTURE_VIEW;
import static cn.com.buildwin.gosky.widget.media.IjkVideoView.RTP_JPEG_PARSE_PACKET_METHOD_FILL;

/*
    Chronometer控件在某些系统上存在bug,
    处于同一层的控件在Invisible后,Chronometer也设为Invisible,
    则其他控件在Chronometer以下的部分就会显示不出来,
    所以代码中才使用Chronometer的GONE和INVISIBLE,还有setText。
 */

public class ControlPanelActivity extends AppCompatActivity implements FlyControllerDelegate {

    private static final String TAG = "ControlPanelActivity";

    // 上排按键
    private ViewGroup mTopMenuBar;
    private ImageButton mBackButton;
    private ImageButton mTakePhotoButton;
    private ImageButton mRecordVideoButton;
    private ImageButton mReviewButton;
    private ImageButton mLimitSpeedButton;
    private ImageButton mLimitHighButton;
    private ImageButton mGravityControlButton;
    private ImageButton mSwitchButton;
    private ImageButton mSettingsButton;
    // 右侧按键
    private ViewGroup mRightMenuBar;
    private ImageButton mRotateScreenButton;
    private ImageButton mSplitScreenButton;
    private ImageButton mHeadlessButton;
    private ImageButton mGyroCalibrateButton;
    private ImageButton mLightButton;
    // 其余按键
    private ImageButton mFlyupButton;
    private ImageButton mFlydownButton;
    private ImageButton mOneKeyStopButton;
//    private ImageButton mRotateButton;
//    private ImageButton mFixedDirectionRotateButton;
//    private ImageButton mReturnButton;
    private ImageButton mRollButton;
    private ImageButton mTrackButton;
    private ImageButton mVoiceButton;

    // 播放器
    private static final int VIDEO_VIEW_RENDER = RENDER_TEXTURE_VIEW;
    private static final int VIDEO_VIEW_ASPECT = AR_MATCH_PARENT;
    private static final int RTP_JPEG_PARSE_PACKET_METHOD = RTP_JPEG_PARSE_PACKET_METHOD_FILL;
    private static final int RECONNECT_INTERVAL = 500;

    private String mVideoPath;
    private IjkVideoView mVideoView;
    private TableLayout mHudView;

    private boolean recording = false;

    private boolean mBackPressed;

    // 控制台界面
    private ViewGroup mRudderViewContainer;
    private RudderView mLeftRudderView;
    private RudderView mRightRudderView;
    private RudderView mPowerRudder;
    private RudderView mRangerRudder;
    private ImageView mBackgroundView;
    private ProgressBar mProgressBar;
    private Chronometer mChronometer;
    // 轨迹飞行
    private TrackView mTrackView;
    // 声控
    private TextView mVoiceGuideTextView;
    private Timer mVoiceControlTimer;

    // 显示帧率
    private static final boolean showFramerate = false;
    private TextView mFramerateTextView;
    private Timer mFramerateTimer;
    private int currentFramerate;
    private int lastFramerate;

    // 自动保存
    private boolean autosave;

    // 控制标志
    private boolean rightHandMode = false;
    private boolean enableControl = false;
    private boolean enableGravityControl = false;
    private int limitSpeed = 0;     // 0=30, 1=60, 2=100
    private boolean voiceMode = false;

    VoiceRecognizer mVoiceRecognizer;

    private CountDownTimer gyroCalibrateTimer;
    private CountDownTimer flyupTimer;
    private CountDownTimer flydownTimer;
    private CountDownTimer emergencyDownTimer;
    private CountDownTimer rollCountDownTimer;  // 用于360度翻转倒计时

    // 飞控(本来想用byte的,不过还是int用起来方便)
    private FlyController mFlyController;

    // 剩余空间监控
    private FreeSpaceMonitor mFreeSpaceMonitor;

    // 其他
    private static String videoFilePath = null; // 用以保存录制视频文件的路径
    private CountDownTimer hideButtonsTimer;    // 隐藏按键的倒计时(3D View)
    private boolean isButtonsVisible = true;    // 3D View中Buttons是否可见
    private Handler updateUiHanlder;            // 用于非主线程更新UI
    private SoundPool mSoundPool;

    // Debug
    // 打开右侧设置按钮，按住陀螺仪校准按钮，再长按打开右侧的设置按钮，即可打开帧数等信息。关闭方法重复操作一遍。
    private boolean touchDebug = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_control_panel);

        autosave = Settings.getInstance(this).getParameterForAutosave();

        // 获取Right hand mode设置
        rightHandMode = Settings.getInstance(this).getParameterForRightHandMode();

        /**
         * Rudder View
         * 控制杆和微调
         */
        mLeftRudderView = (RudderView)findViewById(R.id.control_panel_left_rudderView);
        mRightRudderView = (RudderView)findViewById(R.id.control_panel_right_rudderView);
        // 根据左右手模式设置不同功能对象
        if (rightHandMode) {
            mPowerRudder = mRightRudderView;
            mRangerRudder = mLeftRudderView;
        } else {
            mPowerRudder = mLeftRudderView;
            mRangerRudder = mRightRudderView;
        }
        // 设置Power Rudder
        mPowerRudder.setRudderStyle(RudderView.RudderStyle.RudderStylePower);
        mPowerRudder.setOnValueChangedListener(new RudderView.OnValueChangedListener() {
            @Override
            public void onBasePointMoved(PointF point) {
                // 使用Power Rudder的摇杆值
                float x = (point.x + 1.0f) / 2.0f;
                float y = (point.y + 1.0f) / 2.0f;
                int controlByteRUDD = floatToInt(x, 0xFF);
                int controlByteTHR = floatToInt(y, 0xFF);
                mFlyController.setControlByteRUDD(controlByteRUDD);
                mFlyController.setControlByteTHR(controlByteTHR);

//                Log.i("Power onBasePoint", controlByteRUDD + " " + controlByteTHR);

                // 方向键移动超过一半时,则一键旋转清零
                if (controlByteRUDD < 0x40 || controlByteRUDD > 0xC0) {
                    mFlyController.setRotateMode(false);
                    // 恢复旋转状态,发送消息到UI线程
                    Message message = new Message();
                    message.what = 0;
                    updateUiHanlder.sendMessage(message);
                }

                // 油门控制时，紧急停止置0
                mFlyController.setEmergencyDownMode(false);
                mOneKeyStopButton.setImageResource(R.mipmap.con_emergency_stop);
            }

            @Override
            public void onHTrimValueChanged(float value) {
                // 使用Power Rudder的横向微调值
//                float v = (value + 1.0f) / 2.0f;
                float v = value;
                mFlyController.setTrimByteRUDD(floatToInt(v, RudderView.H_SCALE_NUM));

//                Log.i("Power onHTrimValue", "" + trimByteRUDD);
            }

            @Override
            public void onVTrimValueChanged(float value) {
                // Power Rudder不使用纵向微调
            }
        });
        // 设置Ranger Rudder
        mRangerRudder.setRudderStyle(RudderView.RudderStyle.RudderStyleRanger);
        mRangerRudder.setRightHandMode(rightHandMode);
        mRangerRudder.setOnValueChangedListener(new RudderView.OnValueChangedListener() {
            @Override
            public void onBasePointMoved(PointF point) {
                // 使用Ranger Rudder的摇杆值
                float x = (point.x + 1.0f) / 2.0f;
                float y = (point.y + 1.0f) / 2.0f;
//                controlByteAIL = floatToInt(x);
//                controlByteELE = floatToInt(y);

                int intX = floatToInt(x, 0xFF);
                int intY = floatToInt(y, 0xFF);

                // 如果360度翻转开关打开
                if (mFlyController.isRollMode()) {
                    // 如果还没有触发360度翻转
                    if (!mFlyController.isTriggeredRoll()) {
                        if (intY > 0xC0) {
                            mFlyController.setTriggeredRoll(true);
                            mFlyController.setControlByteELE(0xFF);
                            mFlyController.setControlByteAIL(0x80); // 需要平衡位置？
                        } else if (intY < 0x40) {
                            mFlyController.setTriggeredRoll(true);
                            mFlyController.setControlByteELE(0x00);
                            mFlyController.setControlByteAIL(0x80); // 需要平衡位置？
                        } else {
                            mFlyController.setControlByteELE(intY);
                        }

                        if (!mFlyController.isTriggeredRoll()) {
                            if (intX > 0xC0) {
                                mFlyController.setTriggeredRoll(true);
                                mFlyController.setControlByteAIL(0xFF);
                                mFlyController.setControlByteELE(0x80); // 需要平衡位置？
                            } else if (intX < 0x40) {
                                mFlyController.setTriggeredRoll(true);
                                mFlyController.setControlByteAIL(0x00);
                                mFlyController.setControlByteELE(0x80); // 需要平衡位置？
                            } else {
                                mFlyController.setControlByteAIL(intX);
                            }
                        }

                        // 设置300ms定时器
                        if (mFlyController.isTriggeredRoll()) {
                            // 翻滚模式持续300ms
                            rollCountDownTimer = new CountDownTimer(300, 300) {
                                @Override
                                public void onTick(long l) {

                                }

                                @Override
                                public void onFinish() {
                                    mFlyController.setTriggeredRoll(false);
                                    mFlyController.setRollMode(false);
                                    // 恢复翻转状态,发送消息到UI线程
                                    Message message = new Message();
                                    message.what = 1;
                                    updateUiHanlder.sendMessage(message);

                                    mFlyController.setControlByteELE(0x80);
                                    mFlyController.setControlByteAIL(0x80);

                                    rollCountDownTimer.cancel();
                                    rollCountDownTimer = null;
                                }
                            }.start();
                        }
                    }
                }
                // 如果360度翻转开关关闭
                else {
                    mFlyController.setControlByteELE(intY);
                    mFlyController.setControlByteAIL(intX);
                }

//                Log.i("Ranger onBasePoint", controlByteAIL + " " + controlByteELE);
            }

            @Override
            public void onHTrimValueChanged(float value) {
                // 使用Ranger Rudder的横向微调值
//                float v = (value + 1.0f) / 2.0f;
                float v = value;
                mFlyController.setTrimByteAIL(floatToInt(v, RudderView.H_SCALE_NUM));

//                Log.i("Ranger onHTrimValue", "" + trimByteAIL);
            }

            @Override
            public void onVTrimValueChanged(float value) {
                // 使用Ranger Rudder的纵向微调值
//                float v = (value + 1.0f) / 2.0f;
                float v = value;
                mFlyController.setTrimByteELE(floatToInt(v, RudderView.V_SCALE_NUM));

//                Log.i("Ranger onVTrimValue", "" + trimByteELE);
            }
        });
        mRangerRudder.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mVoiceControlTimer != null) {
                    mVoiceControlTimer.purge();
                    mVoiceControlTimer.cancel();
                    mVoiceControlTimer = null;
                }
            }
        });

        /**
         * 轨迹飞行
         * !!! 因为涉及布局问题，所以需要放在RudderView后初始化 !!!
         */
        // 根据左右手模式，选择使用左右视图
        if (rightHandMode) {
            (findViewById(R.id.control_panel_right_trackView)).setVisibility(View.GONE);
            mTrackView = (TrackView)findViewById(R.id.control_panel_left_trackView);
        } else {
            (findViewById(R.id.control_panel_left_trackView)).setVisibility(View.GONE);
            mTrackView = (TrackView)findViewById(R.id.control_panel_right_trackView);
        }
        // 为了获取到布局后实际大小
        mTrackView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {

            @Override
            public void onGlobalLayout() {
                // Ensure you call it only once :
                if (Build.VERSION.SDK_INT < 16) {
                    mTrackView.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                } else {
                    mTrackView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                }

                // Here you can get the size :)
                setLimitSpeedValue(limitSpeed); // 设置限速值
            }
        });
        // 事件监听
        mTrackView.setOnTrackViewEventListener(new TrackView.OnTrackViewEventListener() {
            @Override
            public void beginOutput() {
                Log.i(TAG, "beginOutput");

                mFlyController.setControlByteELE(0x80);
                mFlyController.setControlByteAIL(0x80);
            }

            @Override
            public void outputPoint(PointF point) {
                float x = (point.x + 1.0f) / 2.0f;
                float y = (point.y + 1.0f) / 2.0f;
                int intX = floatToInt(x, 0xFF);
                int intY = floatToInt(y, 0xFF);

                mFlyController.setControlByteELE(intY);
                mFlyController.setControlByteAIL(intX);

//                Log.i(TAG, ">>> control point: x = " + controlByteAIL + ", y = " + controlByteELE);
            }

            @Override
            public void finishOutput() {
                Log.i(TAG, "finishOutput");

                mFlyController.setControlByteELE(0x80);
                mFlyController.setControlByteAIL(0x80);
            }
        });

        /**
         * Top Menu Bar
         * 上层菜单按键
         */
        mTopMenuBar = (ViewGroup)findViewById(R.id.control_panel_top_menubar);

        /**
         * Right Menu Bar
         * 右侧菜单按键
         */
        mRightMenuBar = (ViewGroup)findViewById(R.id.control_panel_right_menubar);

        /**
         * Rudder View Container
         * 控制杆容器
         */
        mRudderViewContainer = (ViewGroup)findViewById(R.id.control_panel_rudderViewContainer);

        /**
         * 背景图像
         */
        mBackgroundView = (ImageView)findViewById(R.id.control_panel_backgroundView);

        /**
         * 进度条
         */
        mProgressBar = (ProgressBar)findViewById(R.id.control_panel_progressBar);

        /**
         * 录像计时器
         */
        mChronometer = (Chronometer)findViewById(R.id.control_panel_chronometer);

        /**
         * 显示帧率的TextView
         */
        mFramerateTextView = (TextView)findViewById(R.id.control_panel_framerate_textView);
        if (!showFramerate) mFramerateTextView.setVisibility(View.GONE);

        /**
         * RTSPPlayer
         * 播放器界面
         */

        // handle arguments
        mVideoPath = Constants.RTSP_ADDRESS;

        // init UI
        mHudView = (TableLayout) findViewById(R.id.hud_view);
        mHudView.setVisibility(View.GONE);

        // init player
        IjkMediaPlayer.loadLibrariesOnce(null);
        IjkMediaPlayer.native_profileBegin("libijkplayer.so");

        mVideoView = (IjkVideoView) findViewById(R.id.video_view);
        mVideoView.setRtpJpegParsePacketMethod(RTP_JPEG_PARSE_PACKET_METHOD);
        mVideoView.setRender(VIDEO_VIEW_RENDER);
        mVideoView.setAspectRatio(VIDEO_VIEW_ASPECT);
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
//                Log.d(TAG, new String(data) + Arrays.toString(data));
            }
        });
        mVideoView.setOnTookPictureListener(new IMediaPlayer.OnTookPictureListener() {
            @Override
            public void onTookPicture(IMediaPlayer mp, int resultCode, String fileName) {
                String toastText = getResources().getString(R.string.control_panel_alert_save_photo_fail);
                if (resultCode == 1) {
                    // 播放咔嚓声
                    mSoundPool.play(1, 1, 1, 0, 0, 1);
                }
                else if (resultCode == 0 && fileName != null) {
                    File file = new File(fileName);
                    if (file.exists()) {
                        mediaScan(file);
                        // Show toast
                        toastText = getResources().getString(R.string.control_panel_alert_save_photo_success) + fileName;
                    }
                    Toast.makeText(ControlPanelActivity.this, toastText, Toast.LENGTH_SHORT).show();
                }
                else if (resultCode < 0) {
                    Toast.makeText(ControlPanelActivity.this, toastText, Toast.LENGTH_SHORT).show();
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
                                    ControlPanelActivity.this,
                                    noteText,
                                    Toast.LENGTH_SHORT
                            ).show();
                            mRecordVideoButton.setImageResource(R.mipmap.con_video);
                            // 隐藏录像计时器
                            showChronometer(false);
                        }
                        else if (resultCode == 0) {
                            recording = true;
                            // 开启录像计时
                            showChronometer(true);
                            mRecordVideoButton.setImageResource(R.mipmap.con_video_h);
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
                        }
                        else {
                            // 停止监控剩余空间
                            if (mFreeSpaceMonitor != null)
                                mFreeSpaceMonitor.stop();

                            // Scan file to media library
                            File file = new File(fileName);
                            mediaScan(file);

                            noteText = getResources().getString(R.string.control_panel_alert_record_video_success);
                            Toast.makeText(
                                    ControlPanelActivity.this,
                                    noteText + fileName,
                                    Toast.LENGTH_SHORT
                            ).show();
                            mRecordVideoButton.setImageResource(R.mipmap.con_video);
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
                    if (mVideoView.isVrMode()) {
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
        // prefer mVideoPath
        if (mVideoPath != null)
            mVideoView.setVideoPath(mVideoPath);
        else {
            Log.e(TAG, "Null Data Source\n");
            finish();
            return;
        }

        IjkMediaPlayer.getMessageBus().register(this);


        // 飞控
        mFlyController = new FlyController();
        mFlyController.setDelegate(this);


        /**
         * Back Button
         * 返回按钮
         */
        mBackButton = (ImageButton)findViewById(R.id.control_panel_back_button);
        mBackButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mBackPressed = true;
                finish();
                // Activity slide from left
                overridePendingTransition(
                        android.R.anim.slide_in_left,
                        android.R.anim.slide_out_right
                );
            }
        });

        /**
         * Take Photo Button
         * 截图按钮
         */
        mTakePhotoButton = (ImageButton)findViewById(R.id.control_panel_take_photo_button);
        mTakePhotoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Take a photo
                String photoFilePath = Utilities.getPhotoDirPath();
                String photoFileName = Utilities.getMediaFileName();
                try {
                    mVideoView.takePicture(photoFilePath, photoFileName, -1, -1, 1);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        /**
         * Record Video Button
         * 录像按钮
         */
        mRecordVideoButton = (ImageButton)findViewById(R.id.control_panel_record_video_button);
        mRecordVideoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
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
                        Toast.makeText(ControlPanelActivity.this, toastString, Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });

        /**
         * Replay Button
         * 查看按钮
         */
        mReviewButton = (ImageButton)findViewById(R.id.control_panel_review_button);
        mReviewButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Start replaying
                Intent i = new Intent(ControlPanelActivity.this, ReviewActivity.class);
                startActivity(i);
                // Activity slide from left
                overridePendingTransition(
                        android.R.anim.slide_in_left,
                        android.R.anim.slide_out_right
                );
            }
        });

        /**
         * Limit Speed Button
         * 限速按钮
         */
        mLimitSpeedButton = (ImageButton)findViewById(R.id.control_panel_limit_speed_button);
        mLimitSpeedButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Add limit speed support
                stepSpeedLimit();
            }
        });

        /**
         * Limit High Button
         * 限高按钮
         */
        boolean bAltitudeHold;
        if (autosave)
            bAltitudeHold = Settings.getInstance(this).getParameterForAltitudeHold();
        else
            bAltitudeHold = false;
        mLimitHighButton = (ImageButton)findViewById(R.id.control_panel_limit_hight_button);
        if (bAltitudeHold) {
            mFlyController.setEnableLimitHigh(true);
            mLimitHighButton.setImageResource(R.mipmap.con_altitude_hold_h);
        } else {
            mFlyController.setEnableLimitHigh(false);
            mLimitHighButton.setImageResource(R.mipmap.con_altitude_hold);
        }
        mLimitHighButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setEnableLimitHigh(!mFlyController.isEnableLimitHigh());
            }
        });

        /**
         * Gravity Control Button
         * 重力控制按钮
         */
        mGravityControlButton =
                (ImageButton)findViewById(R.id.control_panel_gravity_control_button);
        mGravityControlButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (enableControl) {
                    if (mRangerRudder.isSupportGravityControl()) {
                        setEnableGravityControl(!enableGravityControl);
                    } else {
                        Toast.makeText(getApplicationContext(),
                                R.string.control_panel_alert_not_support_gyro,
                                Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });

        /**
         * Switch Button
         * 开关按钮
         */
        mSwitchButton = (ImageButton)findViewById(R.id.control_panel_switch_button);
        mSwitchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Turn on or off control
                setEnableControl(!enableControl);
            }
        });

        /**
         * Settings Button
         * 右边设置按钮
         */
        mSettingsButton = (ImageButton)findViewById(R.id.control_panel_setting_button);
        mSettingsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // 显示隐藏右侧按键
                int buttonsVisibility = mGyroCalibrateButton.getVisibility();
                if (buttonsVisibility == View.VISIBLE) {
                    mSettingsButton.setImageResource(R.mipmap.con_extra_settings);
                    mRotateScreenButton.setVisibility(View.INVISIBLE);
                    mSplitScreenButton.setVisibility(View.INVISIBLE);
                    mHeadlessButton.setVisibility(View.INVISIBLE);
                    mGyroCalibrateButton.setVisibility(View.INVISIBLE);
                    mLightButton.setVisibility(View.INVISIBLE);
                } else {
                    mSettingsButton.setImageResource(R.mipmap.con_extra_settings_h);
                    mRotateScreenButton.setVisibility(View.VISIBLE);
                    mSplitScreenButton.setVisibility(View.VISIBLE);
                    mHeadlessButton.setVisibility(View.VISIBLE);
                    mGyroCalibrateButton.setVisibility(View.VISIBLE);
                    mLightButton.setVisibility(View.VISIBLE);
                }
            }
        });

        /**
         * Rotate Screen Button
         * 旋转屏幕按钮
         */
        mRotateScreenButton = (ImageButton)findViewById(R.id.control_panel_rotate_screen_button);
        mRotateScreenButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Rotate the screen
                mVideoView.setRotation180(!mVideoView.isRotation180());

                mSettingsButton.setImageResource(R.mipmap.con_extra_settings);
                // 显示隐藏右侧按键
                mRotateScreenButton.setVisibility(View.INVISIBLE);
                mLightButton.setVisibility(View.INVISIBLE);
                mSplitScreenButton.setVisibility(View.INVISIBLE);
                mHeadlessButton.setVisibility(View.INVISIBLE);
                mGyroCalibrateButton.setVisibility(View.INVISIBLE);
            }
        });

        /**
         * Split Screen Button
         * 3D View按钮(Split Screen)
         */
        mSplitScreenButton = (ImageButton)findViewById(R.id.control_panel_split_screen_button);
        mSplitScreenButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // 进入或退出3D模式
                mVideoView.setVrMode(!mVideoView.isVrMode());

                // 根据不同模式,设置界面控件的显示
                if (mVideoView.isVrMode()) {
                    setEnableControl(false);
                    setHideButtonsTimer();
                } else {
                    setButtonsVisible(true);
                    if (hideButtonsTimer != null) {
                        hideButtonsTimer.cancel();
                        hideButtonsTimer = null;
                    }
                }

                // Set Image
                mSplitScreenButton.setImageResource(mVideoView.isVrMode() ? R.drawable.button_flat : R.drawable.button_3d);
            }
        });

        /**
         * Headless Button
         * 无头模式
         */
        mHeadlessButton = (ImageButton)findViewById(R.id.control_panel_headless_button);
        mHeadlessButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // 启用无头或者有头模式
                mFlyController.setHeadlessMode(!mFlyController.isHeadlessMode());

                // Set Image
                int resId;
                if (mFlyController.isHeadlessMode()) {
                    resId = R.mipmap.con_headless_h;
                } else {
                    resId = R.mipmap.con_headless;
                }
                mHeadlessButton.setImageResource(resId);
            }
        });

        /**
         * Gyro Calibrate Button
         * Gyro传感器校准
         */
        mGyroCalibrateButton = (ImageButton)findViewById(R.id.control_panel_gyro_calibrate_button);
        mGyroCalibrateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // 校准Gyro传感器
                if (!mFlyController.isGyroCalibrateMode()) {
                    mFlyController.setGyroCalibrateMode(true);
                    if (gyroCalibrateTimer == null) {
                        gyroCalibrateTimer = new CountDownTimer(1000, 1000) {
                            @Override
                            public void onTick(long l) {

                            }

                            @Override
                            public void onFinish() {
                                mFlyController.setGyroCalibrateMode(false);
                                gyroCalibrateTimer.cancel();
                                gyroCalibrateTimer = null;
                            }
                        }.start();
                    }
                }
            }
        });

        /**
         * 灯光控制
         */
        mLightButton = (ImageButton)findViewById(R.id.control_panel_light_button);
        mLightButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mFlyController.setLightOn(!mFlyController.isLightOn());
                // Set Image
                int resId = mFlyController.isLightOn() ? R.mipmap.con_light_h : R.mipmap.con_light;
                mLightButton.setImageResource(resId);
            }
        });

        /**
         * 一键旋转
         */
//        mRotateButton = (ImageButton)findViewById(R.id.control_panel_rotate_button);
//        mRotateButton.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                // 设置一键旋转
//                rotateMode = !rotateMode;
//                // Set Image
//                int resId;
//                if (rotateMode) {
//                    resId = R.mipmap.con_rotate_h;
//                } else {
//                    resId = R.mipmap.con_rotate;
//                }
//                mRotateButton.setImageResource(resId);
//            }
//        });

//        /**
//         * 一键固定方向旋转
//         */
//        mFixedDirectionRotateButton = (ImageButton)findViewById(R.id.control_panel_fixed_direction_rotate_button);
//        mFixedDirectionRotateButton.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                // 设置一键固定方向旋转
//                fixedDirectionRollMode = !fixedDirectionRollMode;
//                // Set Image
//                int resId;
//                if (fixedDirectionRollMode) {
//                    resId = R.mipmap.con_rotate_direction_h;
//                } else {
//                    resId = R.mipmap.con_rotate_direction;
//                }
//                mFixedDirectionRotateButton.setImageResource(resId);
//            }
//        });

        /**
         * Flyup Button
         * 一键起飞
         */
        mFlyupButton = (ImageButton)findViewById(R.id.control_panel_fly_up_button);
        mFlyupButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // 设置一键起飞
                if (!mFlyController.isFlyupMode()) {
                    mFlyController.setFlyupMode(true);
                    if (flyupTimer == null) {
                        flyupTimer = new CountDownTimer(1000, 1000) {
                            @Override
                            public void onTick(long l) {

                            }

                            @Override
                            public void onFinish() {
                                mFlyController.setFlyupMode(false);
                                flyupTimer.cancel();
                                flyupTimer = null;
                            }
                        }.start();
                    }
                }
            }
        });

        /**
         * Flydown Button
         * 一键降落
         */
        mFlydownButton = (ImageButton)findViewById(R.id.control_panel_fly_down_button);
        mFlydownButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // 设置一键降落
                if (!mFlyController.isFlydownMode()) {
                    mFlyController.setFlydownMode(true);
                    if (flydownTimer == null) {
                        flydownTimer = new CountDownTimer(1000, 1000) {
                            @Override
                            public void onTick(long l) {

                            }

                            @Override
                            public void onFinish() {
                                mFlyController.setFlydownMode(false);
                                flydownTimer.cancel();
                                flydownTimer = null;
                            }
                        }.start();
                    }
                }
                // 按一键下降后，紧急停止置0
                mFlyController.setEmergencyDownMode(false);
                mOneKeyStopButton.setImageResource(R.mipmap.con_emergency_stop);
            }
        });

        /**
         * One Key Stop Button
         * 紧急降落
         */
        mOneKeyStopButton = (ImageButton)findViewById(R.id.control_panel_one_key_stop_button);
        mOneKeyStopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // 设置紧急降落
                if (!mFlyController.isEmergencyDownMode()) {
                    float fTHR = (float)mFlyController.getControlByteTHR() / 255.0f;
                    if (fTHR > 0.4) {
                        mFlyController.setEmergencyDownMode(true);
                        mOneKeyStopButton.setImageResource(R.mipmap.con_emergency_stop_h);
                        // 设置关闭功能定时器
                        if (emergencyDownTimer == null) {
                            emergencyDownTimer = new CountDownTimer(1000, 1000) {
                                @Override
                                public void onTick(long l) {

                                }

                                @Override
                                public void onFinish() {
                                    mFlyController.setEmergencyDownMode(false);
                                    emergencyDownTimer.cancel();
                                    emergencyDownTimer = null;
                                    mOneKeyStopButton.setImageResource(R.mipmap.con_emergency_stop);
                                }
                            }.start();
                        }
                    }
                } else {
                    mFlyController.setEmergencyDownMode(false);
                    mOneKeyStopButton.setImageResource(R.mipmap.con_emergency_stop);
                    // 关闭定时器
                    if (emergencyDownTimer != null) {
                        emergencyDownTimer.cancel();
                        emergencyDownTimer = null;
                    }
                }
            }
        });

        /**
         * 一键返回
         */
//        mReturnButton = (ImageButton)findViewById(R.id.control_panel_return_button);
//        mReturnButton.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                if (!(rollMode || trackMode)) {
//                    // 设置一键返回
//                    returnMode = !returnMode;
//                    // Set Image
//                    int resId;
//                    if (returnMode) {
//                        resId = R.mipmap.con_go_home_h;
//                    } else {
//                        resId = R.mipmap.con_go_home;
//                    }
//                    mReturnButton.setImageResource(resId);
//                }
//            }
//        });

        /**
         * Roll Button
         * 360度翻转
         */
        mRollButton = (ImageButton)findViewById(R.id.control_panel_roll_button);
        mRollButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!(mFlyController.isReturnMode() || mFlyController.isTrackMode())) {
                    // 设置360度翻转
                    if (!mFlyController.isTriggeredRoll()) {
                        mFlyController.setRollMode(!mFlyController.isRollMode());

                        // Set Image
                        int resId;
                        if (mFlyController.isRollMode()) {
                            resId = R.mipmap.con_roll_h;
                        } else {
                            resId = R.mipmap.con_roll;
                        }
                        mRollButton.setImageResource(resId);
                    }
                }
            }
        });

        /**
         * Track Button
         * 轨迹飞行
         */
        mTrackButton = (ImageButton)findViewById(R.id.control_panel_track_button);
        mTrackButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!(mFlyController.isReturnMode() || mFlyController.isRollMode())) {
                    // 设置轨迹飞行
                    mFlyController.setTrackMode(!mFlyController.isTrackMode());
                    // set image
                    mTrackButton.setImageResource(mFlyController.isTrackMode() ? R.mipmap.con_track_h : R.mipmap.con_track);
                    // set rudders' visibility
                    if (mFlyController.isTrackMode()) {
                        mRangerRudder.setVisibility(View.INVISIBLE);
                        mTrackView.setVisibility(View.VISIBLE);
                        // 关掉重力感应控制
                        setEnableGravityControl(false);
                        // 退出语音控制
                        voiceMode = false;
                        mVoiceButton.setImageResource(R.mipmap.con_voice);
                        mVoiceRecognizer.stopListening();
                    } else {
                        mTrackView.setVisibility(View.INVISIBLE);
                        mRangerRudder.setVisibility(View.VISIBLE);
                        // 结束当前轨迹
                        mTrackView.reset();
                    }
                }
            }
        });

        /**
         * 语音控制
         */
        mVoiceButton = (ImageButton)findViewById(R.id.control_panel_voice_button);
        mVoiceButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                voiceMode = !voiceMode;
                mVoiceButton.setImageResource(voiceMode ? R.mipmap.con_voice_h : R.mipmap.con_voice);

                if (voiceMode) {
                    // 语音控制开始监听
                    mVoiceRecognizer.startListening();
                    // 关掉重力感应控制
                    setEnableGravityControl(false);
                    // 结束当前轨迹
                    if (mFlyController.isTrackMode()) {
                        mFlyController.setTrackMode(false);
                        mTrackButton.setImageResource(R.mipmap.con_track);

                        mTrackView.setVisibility(View.INVISIBLE);
                        mRangerRudder.setVisibility(View.VISIBLE);
                        // 结束当前轨迹
                        mTrackView.reset();
                    }
                } else {
                    // 语音控制停止监听
                    mVoiceRecognizer.stopListening();
                }
            }
        });

        mVoiceGuideTextView = (TextView)findViewById(R.id.control_panel_voice_guide_textView);
        resetDefaultVoiceGuide();
        mVoiceGuideTextView.setVisibility(View.INVISIBLE);

        mVoiceRecognizer = new VoiceRecognizer(this);
        mVoiceRecognizer.setVoiceRecognitionListener(new VoiceRecognizer.VoiceRecognitionListener() {
            @Override
            public void onListen() {
                Log.d(TAG, "onListen");
                mVoiceGuideTextView.setVisibility(View.VISIBLE);
            }

            @Override
            public void onPause() {
                Log.d(TAG, "onPause");
                mVoiceGuideTextView.setVisibility(View.INVISIBLE);
            }

            @Override
            public void onResult(VoiceRecognizer.Action action, String text) {
                if (voiceMode) {
                    Log.d(TAG, "onResult: " + action);

                    mVoiceGuideTextView.setText(text);

                    TimerTask voiceControlTask = null;

                    switch (action) {
                        case FORWARD:
                            mRangerRudder.moveStickTo(0, 1.0f);
                            voiceControlTask = new TimerTask() {
                                @Override
                                public void run() {
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            mRangerRudder.moveStickTo(0, 0);
                                            resetDefaultVoiceGuide();
                                        }
                                    });
                                }
                            };
                            break;
                        case BACKWARD:
                            mRangerRudder.moveStickTo(0, -1.0f);
                            voiceControlTask = new TimerTask() {
                                @Override
                                public void run() {
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            mRangerRudder.moveStickTo(0, 0);
                                            resetDefaultVoiceGuide();
                                        }
                                    });
                                }
                            };
                            break;
                        case LEFT:
                            mRangerRudder.moveStickTo(-1.0f, 0);
                            voiceControlTask = new TimerTask() {
                                @Override
                                public void run() {
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            mRangerRudder.moveStickTo(0, 0);
                                            resetDefaultVoiceGuide();
                                        }
                                    });
                                }
                            };
                            break;
                        case RIGHT:
                            mRangerRudder.moveStickTo(1.0f, 0);
                            voiceControlTask = new TimerTask() {
                                @Override
                                public void run() {
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            mRangerRudder.moveStickTo(0, 0);
                                            resetDefaultVoiceGuide();
                                        }
                                    });
                                }
                            };
                            break;
                        case TAKEOFF:
                            mFlyController.setFlyupMode(true);
                            if (flyupTimer == null) {
                                flyupTimer = new CountDownTimer(1000, 1000) {
                                    @Override
                                    public void onTick(long l) {

                                    }

                                    @Override
                                    public void onFinish() {
                                        mFlyController.setFlyupMode(false);
                                        flyupTimer.cancel();
                                        flyupTimer = null;
                                        resetDefaultVoiceGuide();
                                    }
                                }.start();
                            }
                            break;
                        case LANDING:
                            mFlyController.setFlydownMode(true);
                            if (flydownTimer == null) {
                                flydownTimer = new CountDownTimer(1000, 1000) {
                                    @Override
                                    public void onTick(long l) {

                                    }

                                    @Override
                                    public void onFinish() {
                                        mFlyController.setFlydownMode(false);
                                        flydownTimer.cancel();
                                        flydownTimer = null;
                                        resetDefaultVoiceGuide();
                                    }
                                }.start();
                            }
                            break;
                    }
                    if (voiceControlTask != null) {
                        if (mVoiceControlTimer != null) {
                            mVoiceControlTimer.purge();
                            mVoiceControlTimer.cancel();
                            mVoiceControlTimer = null;
                        }
                        mVoiceControlTimer = new Timer("voice control");
                        mVoiceControlTimer.schedule(voiceControlTask, 3000, 3000);
                    }
                }
            }

            @Override
            public void onError(String error) {
                Log.d(TAG, "onError: " + error);
            }
        });

        /**
         * 处理UI刷新
         */
        updateUiHanlder = new Handler(new Handler.Callback() {
            @Override
            public boolean handleMessage(Message message) {
                int what = message.what;
                switch (what) {
                    // 更新mRotateButton图像
                    case 0:
                        mFlyController.setRotateMode(false);
//                        mRotateButton.setImageResource(R.mipmap.con_rotate);
                        break;
                    // 更新mRollButton图像
                    case 1:
                        mFlyController.setRollMode(false);
                        mRollButton.setImageResource(R.mipmap.con_roll);
                        break;
                    // 更新帧率显示
                    case 1000:
                        final float s = 0.8f;
                        String framerateToBeShown = "" + (int)(currentFramerate * (1 - s) + lastFramerate * s);
                        mFramerateTextView.setText(framerateToBeShown);
                        lastFramerate = currentFramerate;
                        currentFramerate = 0;
                        break;
                }

                return true;
            }
        });

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

        /**
         * 初始化控件显示
         */
        mRudderViewContainer.setVisibility(View.INVISIBLE);
        mChronometer.setVisibility(View.GONE);
        mRotateScreenButton.setVisibility(View.INVISIBLE);
        mSplitScreenButton.setVisibility(View.INVISIBLE);
        mHeadlessButton.setVisibility(View.INVISIBLE);
        mGyroCalibrateButton.setVisibility(View.INVISIBLE);
        mRollButton.setVisibility(View.INVISIBLE);
        mFlyupButton.setVisibility(View.INVISIBLE);
        mFlydownButton.setVisibility(View.INVISIBLE);
        mOneKeyStopButton.setVisibility(View.INVISIBLE);
        mTrackButton.setVisibility(View.INVISIBLE);
        mTrackView.setVisibility(View.INVISIBLE);
        mLightButton.setVisibility(View.INVISIBLE);
        mVoiceButton.setVisibility(View.INVISIBLE);

        /**
         * 初始化控制
         */
        if (autosave)
            mFlyController.setEnableLimitHigh(Settings.getInstance(this).getParameterForAltitudeHold());
        else
            mFlyController.setEnableLimitHigh(false);
        mPowerRudder.setAlititudeHoldMode(mFlyController.isEnableLimitHigh());
        setEnableControl(enableControl);
        setEnableGravityControl(enableGravityControl);

        if (autosave)
            limitSpeed = Settings.getInstance(this).getParameterForSpeedLimit();
        else
            limitSpeed = 0;
        setLimitSpeedValue(limitSpeed); // 设置限速值
        setLimitSpeedIcon(limitSpeed);  // 设置图标

        /**
         * 初始化命令
         */
        mFlyController.setControlByteAIL(0x80); // 副翼
        mFlyController.setControlByteELE(0x80); // 升降舵
        if (mFlyController.isEnableLimitHigh())
            mFlyController.setControlByteTHR(0x80); // 油门
        else
            mFlyController.setControlByteTHR(0x80);
        mFlyController.setControlByteRUDD(0x80);    // 方向舵
        // 微调
        mFlyController.setTrimByteAIL(floatToInt(mRangerRudder.getHTrimValue(), RudderView.H_SCALE_NUM));
        mFlyController.setTrimByteELE(floatToInt(mRangerRudder.getVTrimValue(), RudderView.V_SCALE_NUM));
        mFlyController.setTrimByteRUDD(floatToInt(mPowerRudder.getHTrimValue(), RudderView.H_SCALE_NUM));

        // for debug, show framerate, etc
        mGyroCalibrateButton.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN)
                    touchDebug = true;
                else if (event.getAction() == MotionEvent.ACTION_UP)
                    touchDebug = false;
                return false;
            }
        });
        mSettingsButton.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if (touchDebug) {
                    if (mHudView.getVisibility() != View.VISIBLE)
                        mHudView.setVisibility(View.VISIBLE);
                    else
                        mHudView.setVisibility(View.GONE);
                }
                return false;
            }
        });
    }

    @Override
    public void onBackPressed() {
        mBackPressed = true;
        super.onBackPressed();
        finish();
        // Activity slide from left
        overridePendingTransition(
                android.R.anim.slide_in_left,
                android.R.anim.slide_out_right
        );
    }

    @Override
    protected void onStop() {
        super.onStop();

//        if (mBackPressed || !mVideoView.isBackgroundPlayEnabled()) {
        if (!mVideoView.isBackgroundPlayEnabled()) {
            mVideoView.stopPlayback();
            mVideoView.release(true);
            mVideoView.stopBackgroundPlay();
        } else {
            mVideoView.enterBackground();
        }
        mBackgroundView.setVisibility(View.VISIBLE);
        mProgressBar.setVisibility(View.VISIBLE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 开启屏幕常亮
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        mVideoView.setRender(VIDEO_VIEW_RENDER);
        mVideoView.setAspectRatio(VIDEO_VIEW_ASPECT);
        mVideoView.setVideoPath(mVideoPath);
        mVideoView.start();
    }

    @Override
    protected void onPause() {
        super.onPause();
        // 关闭屏幕常亮
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        // 停止录像
        if (recording)
            mVideoView.stopRecordVideo();
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        IjkMediaPlayer.getMessageBus().unregister(this);

        mSoundPool.release();
        // Stop sending fly controller data
        mFlyController.sendFlyControllerData(false);
        mVoiceRecognizer.shutdown();

        IjkMediaPlayer.native_profileEnd();
    }

    /**
     * 播放开始后执行
     */
    private void onStartPlayback() {
        // 隐藏BackgroundView,ProgressBar
        mBackgroundView.setVisibility(View.INVISIBLE);
        mProgressBar.setVisibility(View.INVISIBLE);
    }

    /**
     * 关闭播放器并重新开始播放
     * 错误发生的时候调用
     */
    private void stopAndRestartPlayback() {
        // 显示BackgroundView,ProgressBar
        mBackgroundView.setVisibility(View.VISIBLE);
        mProgressBar.setVisibility(View.VISIBLE);

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
                mVideoView.setAspectRatio(VIDEO_VIEW_ASPECT);
                mVideoView.setVideoPath(mVideoPath);
                mVideoView.start();
            }
        }, RECONNECT_INTERVAL);
    }

    @Subscribe
    public void unhandledMessage(Message msg) {
        // 一个暂定的解决方案
//        private static final int MEDIA_ERROR = 100;
        if (msg.what == 100) {
            stopAndRestartPlayback();
        }
    }

    /**
     * 扫描添加媒体文件到系统媒体库
     * @param file  媒体文件
     */
    private void mediaScan(File file) {
        MediaScannerConnection.scanFile(getApplicationContext(),
                new String[] { file.getAbsolutePath() }, null,
                new MediaScannerConnection.OnScanCompletedListener() {
                    @Override
                    public void onScanCompleted(String path, Uri uri) {
                        Log.v("MediaScanWork", "file " + path
                                + " was scanned seccessfully: " + uri);
                    }
                });
    }

    private void stepSpeedLimit() {
        limitSpeed = ++limitSpeed % 3;
        setLimitSpeedValue(limitSpeed); // 设置限速值
        setLimitSpeedIcon(limitSpeed);  // 设置图标
    }

    /**
     * 设置限速值
     * @param limitSpeed    限速值
     */
    private void setLimitSpeedValue(int limitSpeed) {
        if (autosave)
            Settings.getInstance(this).saveParameterForSpeedLimit(limitSpeed);

        switch (limitSpeed) {
            case 0:
                mFlyController.setLimitSpeedValue(30);
                break;
            case 1:
                mFlyController.setLimitSpeedValue(60);
                break;
            case 2:
                mFlyController.setLimitSpeedValue(100);
                break;
            default:
                mFlyController.setLimitSpeedValue(30);
        }

        // 轨迹飞行设置速度级
        mTrackView.setSpeedLevel(limitSpeed);
    }

    private void setLimitSpeedIcon(int speedLimit) {
        int resId;
        switch (speedLimit) {
            case 0:
                resId = R.drawable.button_speed_30;
                break;
            case 1:
                resId = R.drawable.button_speed_60;
                break;
            case 2:
                resId = R.drawable.button_speed_100;
                break;
            default:
                resId = R.drawable.button_speed_30;
        }
        mLimitSpeedButton.setImageResource(resId);
    }

    /**
     * 开启/关闭限高开关
     * @param enableLimitHigh   限高开关
     */
    private void setEnableLimitHigh(boolean enableLimitHigh) {
        mFlyController.setEnableLimitHigh(enableLimitHigh);

        if (autosave)
            Settings.getInstance(this).saveParameterForAltitudeHold(enableLimitHigh);
        mPowerRudder.setAlititudeHoldMode(enableLimitHigh);

        // 启用控制时,才可开启定高模式
        if (enableControl) {
            mFlyupButton.setVisibility(View.VISIBLE);
            mFlydownButton.setVisibility(View.VISIBLE);
            mOneKeyStopButton.setVisibility(View.VISIBLE);
        }

        if (!enableLimitHigh) {
            mFlyupButton.setVisibility(View.INVISIBLE);
            mFlydownButton.setVisibility(View.INVISIBLE);
            mOneKeyStopButton.setVisibility(View.INVISIBLE);
        }

        // 设置图像
        int resId;
        if (enableLimitHigh) {
            resId = R.mipmap.con_altitude_hold_h;
        } else {
            resId = R.mipmap.con_altitude_hold;
        }
        mLimitHighButton.setImageResource(resId);
    }

    /**
     * 使能/禁能重力控制
     * @param enableGravityControl  重力开关
     */
    private void setEnableGravityControl(boolean enableGravityControl) {
        if (!enableGravityControl
                || (enableGravityControl && !mFlyController.isTrackMode() && !voiceMode)) {
            this.enableGravityControl = enableGravityControl;
            mRangerRudder.setEnableGravityControl(this.enableGravityControl);

            int resId;
            if (this.enableGravityControl) {
                resId = R.mipmap.con_gravity_control_h;
            } else {
                resId = R.mipmap.con_gravity_control;
            }
            mGravityControlButton.setImageResource(resId);
        }
    }

    /**
     * 开启/关闭控制开关
     * @param enableControl 控制开关
     */
    private void setEnableControl(boolean enableControl) {
        this.enableControl = enableControl;

        // 设置图像和Visibility
        int resId;
        if (this.enableControl) {
            boolean isShownChronometer = mChronometer.isShown();
            if (isShownChronometer)
                mChronometer.setVisibility(View.GONE);

            // UI
            mRudderViewContainer.setVisibility(View.VISIBLE);
//            mRotateButton.setVisibility(View.VISIBLE);
//            mFixedDirectionRotateButton.setVisibility(View.VISIBLE);
//            mReturnButton.setVisibility(View.VISIBLE);
            mRollButton.setVisibility(View.VISIBLE);
            mTrackButton.setVisibility(View.VISIBLE);
            mVoiceButton.setVisibility(View.VISIBLE);
//            if (trackMode)
//                mTrackView.setVisibility(View.VISIBLE);
//            else
//                mTrackView.setVisibility(View.INVISIBLE);
            if (mFlyController.isEnableLimitHigh()) {
                mFlyupButton.setVisibility(View.VISIBLE);
                mFlydownButton.setVisibility(View.VISIBLE);
                mOneKeyStopButton.setVisibility(View.VISIBLE);
            }
            resId = R.mipmap.con_on;

            // Start sending fly controller data
            mFlyController.sendFlyControllerData(true);

            if (isShownChronometer)
                mChronometer.setVisibility(View.VISIBLE);
        } else {
            // UI
            mRudderViewContainer.setVisibility(View.INVISIBLE);
            mFlyupButton.setVisibility(View.INVISIBLE);
            mFlydownButton.setVisibility(View.INVISIBLE);
            mOneKeyStopButton.setVisibility(View.INVISIBLE);
//            mRotateButton.setVisibility(View.INVISIBLE);
//            mFixedDirectionRotateButton.setVisibility(View.INVISIBLE);
//            mReturnButton.setVisibility(View.INVISIBLE);
            mRollButton.setVisibility(View.INVISIBLE);
            mTrackButton.setVisibility(View.INVISIBLE);
            mVoiceButton.setVisibility(View.INVISIBLE);
            mVoiceGuideTextView.setVisibility(View.INVISIBLE);
            if (mFlyController.isTrackMode())
                mTrackView.reset();
            resId = R.mipmap.con_off;

            if (voiceMode) {
                voiceMode = false;
                mVoiceRecognizer.stopListening();

                mVoiceButton.setImageResource(R.mipmap.con_voice);
            }

            // Stop sending fly controller data
            mFlyController.sendFlyControllerData(false);
        }
        mSwitchButton.setImageResource(resId);

        // 关闭控制时,关掉重力控制
        if (!this.enableControl) {
            // 关闭开关时关闭重力感应控制
            setEnableGravityControl(false);
        }
    }

    @Override
    public void sendFlyControllerData(int[] data) {
        byte[] bytes = new byte[data.length];
        for (int i=0; i<data.length; i++) {
            bytes[i] = (byte)data[i];
        }
        // Send
        try {
            mVideoView.sendRtcpRrData(bytes);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

//    /**
//     * 转换摇杆值到整型([0, 1] -> [0, 0xFF])
//     * 为保证中间值为0x80,使用此转换方法
//     * @param v 浮点值
//     * @return  整型值
//     */
//    private int floatToInt(float v) {
//        int intV = (int)(v * 0x100);
//        if (intV < 0) intV = 0;
//        if (intV > 0xFF) intV = 0xFF;
//        return intV;
//    }

    private int floatToInt(float f, int maxValue) {
        return Math.round(f * maxValue);
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
            mBackButton.setVisibility(View.VISIBLE);
            mTopMenuBar.setVisibility(View.VISIBLE);
            mRightMenuBar.setVisibility(View.VISIBLE);
        } else {
            isButtonsVisible = false;
            mBackButton.setVisibility(View.INVISIBLE);
            mTopMenuBar.setVisibility(View.INVISIBLE);
            mRightMenuBar.setVisibility(View.INVISIBLE);
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
                mSettingsButton.setImageResource(R.mipmap.con_extra_settings);
                setButtonsVisible(false);
            }
        }.start();
    }

    /**
     * 设置Voice Guide为默认的Guide文本
     */
    private void resetDefaultVoiceGuide() {
        mVoiceGuideTextView.setText(R.string.control_panel_voice_guide);
    }

}
