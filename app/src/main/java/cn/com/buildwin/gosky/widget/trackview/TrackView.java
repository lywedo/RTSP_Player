package cn.com.buildwin.gosky.widget.trackview;

import android.content.Context;
import android.graphics.PointF;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import cn.com.buildwin.gosky.BuildConfig;
import cn.com.buildwin.gosky.application.Constants;
import cn.com.buildwin.gosky.R;

public class TrackView extends RelativeLayout
        implements TrackCanvasView.OnCanvasViewEventListener, TouchPointPU.OnProcessPointEventListener {

    private static final String TAG = TrackView.class.getName();

    // 飞机轨迹更新间隔，现在使用和飞控指令一样的间隔
    private static final int TRACK_UPDATE_INTERVAL = Constants.SEND_COMMAND_INTERVAL;

    private TrackCanvasView mCanvasView;
    private ImageView mAircraftView;
    private TouchPointPU mTouchPointPU;

    private float rLeft;
    private float rTop;

    private Timer mTaskTimer;
    private Handler uiUpdateHandler;

    public int mSpeedLevel;

    public void setSpeedLevel(int speedLevel) {
//        assert(speedLevel >= 0 && speedLevel <= 2);
        if (BuildConfig.DEBUG && !(speedLevel >= 0 && speedLevel <= 2)) {
            throw new AssertionError();
        }

        mSpeedLevel = speedLevel;

        int flyTime;
        switch (mSpeedLevel) {
            case 0: // 30%
                flyTime = 10;
                break;
            case 1: // 60%
                flyTime = 6;
                break;
            case 2: // 100%
                flyTime = 4;
                break;

            default:
                flyTime = 10;
        }

        float perUnitLength = mCanvasView.getMeasuredWidth() / flyTime * (TRACK_UPDATE_INTERVAL / 1000.f);
        mTouchPointPU.setPerUnitLength(perUnitLength);
    }

    private OnTrackViewEventListener mOnTrackViewEventListener;

    public void setOnTrackViewEventListener(OnTrackViewEventListener onTrackViewEventListener) {
        mOnTrackViewEventListener = onTrackViewEventListener;
    }

    /**
     * 构造方法
     * @param context @
     */
    public TrackView(Context context) {
        super(context);
        init(null, 0);
    }

    public TrackView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs, 0);
    }

    public TrackView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(attrs, defStyle);
    }

    /**
     * 初始化
     * @param attrs     From XML
     * @param defStyle  From XML
     */
    private void init(AttributeSet attrs, int defStyle) {

        // 轨迹画布
        mCanvasView = new TrackCanvasView(getContext());
        mCanvasView.setOnCanvasViewEventListener(this);
        addView(mCanvasView);

        // 飞行器
        mAircraftView = new ImageView(getContext());
        mAircraftView.setImageResource(R.drawable.circle);
        addView(mAircraftView);
        mAircraftView.setVisibility(INVISIBLE);

        // 触摸点处理
        mTouchPointPU = new TouchPointPU();
        mTouchPointPU.setOnProcessPointEventListener(this);

        // UI更新处理
        uiUpdateHandler = new Handler(Looper.getMainLooper());
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {

        super.onLayout(changed, l, t, r, b);

        int width = getMeasuredWidth();
        int height = getMeasuredHeight();
        int centerX = width / 2;
        int centerY = height / 2;

        // Layout mCanvasView
        int widthCanvasView = mCanvasView.getMeasuredWidth();
        int heightCanvasView = mCanvasView.getMeasuredHeight();
        mCanvasView.layout(
                centerX - widthCanvasView / 2,
                centerY - heightCanvasView / 2,
                centerX + widthCanvasView / 2,
                centerY + heightCanvasView / 2
        );

        // Relative left and top (point center), for aircraft view
        rLeft = centerX - widthCanvasView / 2 - mAircraftView.getWidth() / 2;
        rTop = centerY - heightCanvasView / 2 - mAircraftView.getHeight() / 2;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {

        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        int widthMeasureSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightMeasureSize = MeasureSpec.getSize(heightMeasureSpec);
        int minEdge = Math.min(widthMeasureSize, heightMeasureSize);

        measureChild(mCanvasView,
                MeasureSpec.makeMeasureSpec(minEdge, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(minEdge, MeasureSpec.EXACTLY));
    }

    public void reset() {

        if (mTaskTimer != null) {
            mTaskTimer.cancel();
            mTaskTimer = null;
        }
        // 更新UI使用主线程
        uiUpdateHandler.post(new Runnable() {
            @Override
            public void run() {
                mAircraftView.setVisibility(INVISIBLE);
            }
        });
        // 重置画板
        mCanvasView.reset();
        // 代理通知，完成输出
        onFinishOutput();
    }

    /**
     * TrackCanvasView事件接口
     */

    @Override
    public void trackCanvasViewWillDraw() {

        // 准备开始新的轨迹，清除当前轨迹
        reset();
    }

    @Override
    public void trackCanvasViewDrawnPoints(ArrayList<TouchPoint> touchPoints) {

        mTouchPointPU.processTouchPoints(touchPoints);
    }

    /**
     * TouchPointPU事件接口
     */

    @Override
    public void beginProcessingPoint() {

        // 代理通知，准备输出
        onBeginOutput();

        // 先放在一个看不见的地方
        mAircraftView.setX(-20.f);
        mAircraftView.setY(-20.f);
        mAircraftView.setVisibility(VISIBLE);

        // 输出定时器
        mTaskTimer = new Timer();
        mTaskTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                final TrackPoint currentPoint = mTouchPointPU.dequeueTrackPoint();
                if (currentPoint != null) {
//                    Log.i(TAG, "" + currentPoint.x + " " + currentPoint.y + " "
//                            + currentPoint.ux + " " + currentPoint.uy);
                    // 使用主线程更新UI
                    uiUpdateHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            float lx = rLeft + currentPoint.x;
                            float ly = rTop + currentPoint.y;
                            mAircraftView.setX(lx);
                            mAircraftView.setY(ly);
                        }
                    });

                    onOutputPoint(new PointF(currentPoint.ux, currentPoint.uy));
                }
                else {
                    reset();
                }
            }
        }, 0, Constants.SEND_COMMAND_INTERVAL);
    }

    /**
     * TrackView事件接口
     */
    public interface OnTrackViewEventListener {
        void beginOutput();
        void outputPoint(PointF point);
        void finishOutput();
    }

    private void onBeginOutput() {
        if (mOnTrackViewEventListener != null) {
            mOnTrackViewEventListener.beginOutput();
        }
    }

    private void onOutputPoint(PointF point) {
        if (mOnTrackViewEventListener != null) {
            mOnTrackViewEventListener.outputPoint(point);
        }
    }

    private void onFinishOutput() {
        if (mOnTrackViewEventListener != null) {
            mOnTrackViewEventListener.finishOutput();
        }
    }

}
