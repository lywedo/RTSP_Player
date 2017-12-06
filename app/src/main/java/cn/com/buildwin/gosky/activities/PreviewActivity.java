/*
 * Copyright (C) 2015 Bilibili
 * Copyright (C) 2015 Zhang Rui <bbcallen@gmail.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cn.com.buildwin.gosky.activities;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ProgressBar;
import android.widget.TableLayout;
import android.widget.TextView;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;

import cn.com.buildwin.gosky.R;
import cn.com.buildwin.gosky.content.RecentMediaStorage;
import cn.com.buildwin.gosky.fragments.TracksFragment;
import cn.com.buildwin.gosky.widget.media.AndroidMediaController;
import cn.com.buildwin.gosky.widget.media.IjkVideoView;
import cn.com.buildwin.gosky.widget.media.MeasureHelper;
import tv.danmaku.ijk.media.player.IMediaPlayer;
import tv.danmaku.ijk.media.player.IjkMediaPlayer;
import tv.danmaku.ijk.media.player.misc.ITrackInfo;
import tv.danmaku.ijk.media.player.pragma.DebugLog;

import static cn.com.buildwin.gosky.widget.media.IRenderView.AR_MATCH_PARENT;
import static cn.com.buildwin.gosky.widget.media.IjkVideoView.RENDER_SURFACE_VIEW;
import static cn.com.buildwin.gosky.widget.media.IjkVideoView.RENDER_TEXTURE_VIEW;
import static cn.com.buildwin.gosky.widget.media.IjkVideoView.RTP_JPEG_PARSE_PACKET_METHOD_FILL;

public class PreviewActivity extends AppCompatActivity implements TracksFragment.ITrackHolder {
    private static final String TAG = "PreviewActivity";

    private static final int VIDEO_VIEW_RENDER = RENDER_TEXTURE_VIEW;
    private static final int VIDEO_VIEW_ASPECT = AR_MATCH_PARENT;
    private static final int RTP_JPEG_PARSE_PACKET_METHOD = RTP_JPEG_PARSE_PACKET_METHOD_FILL;
    private static final int RECONNECT_INTERVAL = 500;  // release ijkplayer delay 200ms

    private String mVideoPath;

    private AndroidMediaController mMediaController;
    private IjkVideoView mVideoView;
    private TextView mToastTextView;
    private TableLayout mHudView;
    private DrawerLayout mDrawerLayout;
    private ViewGroup mRightDrawer;

    private boolean mBackPressed;

    private ProgressBar mProgressBar;

    public static Intent newIntent(Context context, String videoPath, String videoTitle) {
        Intent intent = new Intent(context, PreviewActivity.class);
        intent.putExtra("videoPath", videoPath);
        intent.putExtra("videoTitle", videoTitle);
        return intent;
    }

    public static void intentTo(Context context, String videoPath, String videoTitle) {
        context.startActivity(newIntent(context, videoPath, videoTitle));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_preview);

        // handle arguments
        mVideoPath = getIntent().getStringExtra("videoPath");

        // init UI
        mToastTextView = (TextView) findViewById(R.id.toast_text_view);
        mHudView = (TableLayout) findViewById(R.id.hud_view);
//        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
//        mRightDrawer = (ViewGroup) findViewById(R.id.right_drawer);

//        mHudView.setVisibility(View.GONE);
//        mDrawerLayout.setScrimColor(Color.TRANSPARENT);

        mProgressBar = (ProgressBar)findViewById(R.id.progress_bar);

        // init player
        IjkMediaPlayer.loadLibrariesOnce(null);
        IjkMediaPlayer.native_profileBegin("libijkplayer.so");

        mVideoView = (IjkVideoView) findViewById(R.id.video_view);
        mVideoView.setRtpJpegParsePacketMethod(RTP_JPEG_PARSE_PACKET_METHOD);
        mVideoView.setRender(VIDEO_VIEW_RENDER);
        mVideoView.setAspectRatio(VIDEO_VIEW_ASPECT);
        mVideoView.setHudView(mHudView);

        mVideoView.setOnPreparedListener(mOnPreparedListener);
        mVideoView.setOnErrorListener(mOnErrorListener);
        mVideoView.setOnReceivedRtcpSrDataListener(mOnReceivedRtcpSrDataListener);
        mVideoView.setOnTookPictureListener(mOnTookPictureListener);

        // prefer mVideoPath
        if (mVideoPath != null)
            mVideoView.setVideoPath(mVideoPath);
        else {
            Log.e(TAG, "Null Data Source\n");
            finish();
            return;
        }
        mVideoView.start();
    }

    private IMediaPlayer.OnPreparedListener mOnPreparedListener = new IMediaPlayer.OnPreparedListener() {
        @Override
        public void onPrepared(IMediaPlayer mp) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            mProgressBar.setVisibility(View.GONE);
            mVideoView.setVisibility(View.VISIBLE);
        }
    };

    private IMediaPlayer.OnErrorListener mOnErrorListener = new IMediaPlayer.OnErrorListener() {
        @Override
        public boolean onError(IMediaPlayer mp, int what, int extra) {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            mProgressBar.setVisibility(View.VISIBLE);
            mVideoView.setVisibility(View.GONE);

            mVideoView.post(new Runnable() {
                @Override
                public void run() {
                    mVideoView.stopPlayback();
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
            return true;
        }
    };

    private IMediaPlayer.OnReceivedRtcpSrDataListener mOnReceivedRtcpSrDataListener = new IMediaPlayer.OnReceivedRtcpSrDataListener() {
        @Override
        public void onReceivedRtcpSrData(IMediaPlayer mp, byte[] data) {
//            DebugLog.e(TAG, ">>>>>>: " + Arrays.toString(data) + "\n");

            // Send RR while received SR
//            byte[] bytes = new byte[] {(byte)0x66, (byte)0x11, (byte)0x22, (byte)0x33, (byte)0x44, (byte)0x55, (byte)(0x11^0x22^0x33^0x44^0x55), (byte)0x99};
//            mVideoView.sendRtcpRrData(bytes);


//            Date date = new Date();
//            SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd_HHmmsss", Locale.getDefault());
//            String dateString = format.format(date);
//            try {
//                String extStoragePath = Environment.getExternalStorageDirectory().getCanonicalPath();
//                String homePath = new File(extStoragePath, "111111").getCanonicalPath();
//                mVideoView.takePicture(homePath, dateString, -1, -1, 1);
//            }
//            catch (Exception e) {
//                e.printStackTrace();
//            }
        }
    };

    private IMediaPlayer.OnTookPictureListener mOnTookPictureListener = new IMediaPlayer.OnTookPictureListener() {
        @Override
        public void onTookPicture(IMediaPlayer mp, int resultCode, String fileName) {
            DebugLog.e(TAG, "TTTTTT: " + resultCode + ", " + fileName);
        }
    };

    @Override
    public void onBackPressed() {
        mBackPressed = true;

        super.onBackPressed();
    }

    @Override
    protected void onStop() {
        super.onStop();

        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        if (mBackPressed || !mVideoView.isBackgroundPlayEnabled()) {
            mVideoView.stopPlayback();
            mVideoView.release(true);
            mVideoView.stopBackgroundPlay();
        } else {
            mVideoView.enterBackground();
        }
        IjkMediaPlayer.native_profileEnd();
    }

    @Override
    public ITrackInfo[] getTrackInfo() {
        if (mVideoView == null)
            return null;

        return mVideoView.getTrackInfo();
    }

    @Override
    public void selectTrack(int stream) {
        mVideoView.selectTrack(stream);
    }

    @Override
    public void deselectTrack(int stream) {
        mVideoView.deselectTrack(stream);
    }

    @Override
    public int getSelectedTrack(int trackType) {
        if (mVideoView == null)
            return -1;

        return mVideoView.getSelectedTrack(trackType);
    }
}
