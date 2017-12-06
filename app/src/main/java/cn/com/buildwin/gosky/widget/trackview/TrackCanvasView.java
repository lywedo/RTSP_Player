package cn.com.buildwin.gosky.widget.trackview;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;

import cn.com.buildwin.gosky.R;

public class TrackCanvasView extends View {

    private ArrayList<TouchPoint> mTouchPoints;
    private boolean mCancelled;

    private Paint trackPathPaint;   // 轨迹路径画笔

    private OnCanvasViewEventListener mOnCanvasViewEventListener;

    public TrackCanvasView(Context context) {
        super(context);
        init(null, 0);
    }

    public TrackCanvasView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs, 0);
    }

    public TrackCanvasView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(attrs, defStyle);
    }

    /**
     * 初始化
     * @param attrs     From XML
     * @param defStyle  From XML
     */
    private void init(AttributeSet attrs, int defStyle) {

        // 设置路径属性
        trackPathPaint = new Paint();
        trackPathPaint.setAntiAlias(true);
        trackPathPaint.setStyle(Paint.Style.FILL);
        trackPathPaint.setStrokeWidth(4);
        trackPathPaint.setColor(Color.YELLOW);

        // 设置虚线边框
        setBackgroundResource(R.drawable.dash_line);
    }

    @Override
    protected void onDraw(Canvas canvas) {

        // 如果已经取消操作，则返回
        if (mCancelled)
            return;

        // draw touch points
        if (mTouchPoints != null) {
            TouchPoint p1 = null;
            TouchPoint p2 = null;
            for (TouchPoint touchPoint:mTouchPoints) {
                p2 = p1;
                p1 = touchPoint;

                if (p1 != null && p2 != null) {
                    canvas.drawLine(p1.x, p1.y, p2.x, p2.y, trackPathPaint);
                }
            }
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {

        float x = event.getX();
        float y = event.getY();

        int action = event.getAction();
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                // 通知
                onCanvasViewWillDraw();
                // 设点
                mTouchPoints = new ArrayList<>();
                mTouchPoints.add(new TouchPoint(x, y));
                // 设置cancel标志
                mCancelled = false;
                break;
            case MotionEvent.ACTION_MOVE:
                if (mCancelled)
                    return true;
                // 如果不在范围里
                if (x < 0 || y < 0
                        || x > this.getWidth()
                        || y > this.getHeight()) {
                    mCancelled = true;
                }
                // 在范围里则添加
                else {
                    mTouchPoints.add(new TouchPoint(x, y));
                }
                // redraw
                postInvalidate();
                break;
            case MotionEvent.ACTION_UP:
                // 通知
                if (!mCancelled && mTouchPoints.size() > 1) {
                    onCanvasDrawnPoints(mTouchPoints);
                }
                break;
            case MotionEvent.ACTION_CANCEL:
                // 取消
                mCancelled = true;
                postInvalidate();
                break;
        }

        return true;
    }

    /**
     * 重置
     */
    public void reset() {
        mTouchPoints = new ArrayList<>();
        postInvalidate();
    }

    /**
     * Event listener setter
     * @param onCanvasViewEventListener @
     */
    public void setOnCanvasViewEventListener(OnCanvasViewEventListener onCanvasViewEventListener) {
        mOnCanvasViewEventListener = onCanvasViewEventListener;
    }

    /**
     * 事件监听
     */
    public interface OnCanvasViewEventListener {
        void trackCanvasViewWillDraw();
        void trackCanvasViewDrawnPoints(ArrayList<TouchPoint> touchPoints);
    }

    /**
     * WillDraw事件
     */
    private void onCanvasViewWillDraw() {
        if (mOnCanvasViewEventListener != null) {
            mOnCanvasViewEventListener.trackCanvasViewWillDraw();
        }
    }

    /**
     * Drawn事件
     * @param touchPoints @
     */
    private void onCanvasDrawnPoints(ArrayList<TouchPoint> touchPoints) {
        if (mOnCanvasViewEventListener != null) {
            mOnCanvasViewEventListener.trackCanvasViewDrawnPoints(touchPoints);
        }
    }
}
