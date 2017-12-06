package cn.com.buildwin.gosky.widget.rudderview;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.PointF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import cn.com.buildwin.gosky.R;

public class JoyStickView extends View {

    // 控件类型
    public enum JoyStickStyle {
        JoyStickStylePower,
        JoyStickStyleRanger,
    }
    private JoyStickStyle mJoyStickStyle;

    // 背景和球的图像
    private Bitmap mStickViewBase;
    private Bitmap mStickView;

    // 背景和求的坐标
    private float stickViewBaseX;
    private float stickViewBaseY;
    private float stickViewX;
    private float stickViewY;

    // Padding
    private int paddingLeft;
    private int paddingRight;
    private int paddingTop;
    private int paddingBottom;

    // The width and height of content
    private int contentWidth;
    private int contentHeight;

    // Center Point
    private PointF centerPoint;
    // Max Radius
    private float maxR;

    // Redraw flag
    private boolean needRedraw = true;

    // Alititude hold mode
    private boolean alititudeHoldMode = false;

    // 控制状态
    private boolean controlling = false;

    // The constant value of two images
    // TODO: 修改图像需要根据具体的图像属性修改
    private static final int DIFF_R_IO = 24;
    private static final int STICK_TRANSPARENT_R = 8;

    // StickMoved Listener
    private OnStickMovedListener mOnStickMovedListener;

    public JoyStickView(Context context) {
        super(context);
        init(null, 0);
    }

    public JoyStickView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs, 0);
    }

    public JoyStickView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(attrs, defStyle);
    }

    /**
     * 初始化
     * @param attrs     From XML
     * @param defStyle  From XML
     */
    private void init(AttributeSet attrs, int defStyle) {
        // Load attributes
        final TypedArray a = getContext().obtainStyledAttributes(
                attrs, R.styleable.JoyStickView, defStyle, 0);

        // From attr to enum
        int joyStickStyle = 0;
        if (a.hasValue(R.styleable.JoyStickView_JoyStickStyle)) {
            joyStickStyle = a.getInt(R.styleable.JoyStickView_JoyStickStyle, 0);
        }
        mJoyStickStyle = JoyStickStyle.values()[joyStickStyle];

        // 回收资源
        a.recycle();

        // 根据JoyStickStyle载入不同的背景图
        loadStickViewBaseImage();
        mStickView = BitmapFactory.decodeResource(getResources(), R.mipmap.bar);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // 需要重画时(包括第一次)
        if (needRedraw) {
            needRedraw = false;

            // 计算Padding
            paddingLeft = getPaddingLeft();
            paddingTop = getPaddingTop();
            paddingRight = getPaddingRight();
            paddingBottom = getPaddingBottom();

            // 计算content的宽高
            contentWidth = getWidth() - paddingLeft - paddingRight;
            contentHeight = getHeight() - paddingTop - paddingBottom;

            // Calculate
            int minEdge = contentWidth > contentHeight ? contentHeight : contentWidth;  // 最小边
            float scaleNum = (float)minEdge / (float)mStickViewBase.getWidth();         // 缩放度

            // 中点
            centerPoint = new PointF(paddingLeft + contentWidth / 2, paddingTop + contentHeight / 2);
            // 最大半径
            maxR = (mStickViewBase.getWidth() / 2
                    - (DIFF_R_IO + mStickView.getWidth() / 2 - STICK_TRANSPARENT_R)) * scaleNum;

            // 仪表盘缩放
            mStickViewBase = Bitmap.createScaledBitmap(
                    mStickViewBase,
                    (int)(mStickViewBase.getWidth() * scaleNum),
                    (int)(mStickViewBase.getHeight() * scaleNum),
                    true
            );
            // 摇杆缩放
            mStickView = Bitmap.createScaledBitmap(
                    mStickView,
                    (int)(mStickView.getWidth() * scaleNum),
                    (int)(mStickView.getHeight() * scaleNum),
                    true
            );

            // 仪表盘位置
            stickViewBaseX = paddingLeft + (contentWidth - mStickViewBase.getWidth()) / 2;
            stickViewBaseY = paddingTop + (contentHeight - mStickViewBase.getHeight()) / 2;
            // 摇杆位置
            stickViewX = paddingLeft + (contentWidth - mStickView.getWidth()) / 2;
//            stickViewY = paddingTop + (contentHeight - mStickView.getHeight()) / 2;
            if (mJoyStickStyle == JoyStickStyle.JoyStickStylePower) {
                // 计算摇杆位置
                if (alititudeHoldMode) {
                    stickViewY = centerPoint.y - mStickView.getHeight() / 2;
                } else {
                    stickViewY = centerPoint.y + maxR - mStickView.getHeight() / 2;
                }
            } else {    // JoyStickStyle.JoyStickStyleRanger
                stickViewY = paddingTop + (contentHeight - mStickView.getHeight()) / 2;
            }
        }
        // Draw
        canvas.drawBitmap(mStickViewBase, stickViewBaseX, stickViewBaseY, null);
        canvas.drawBitmap(mStickView, stickViewX, stickViewY, null);
    }

    /**
     * 触摸事件处理方法
     * @param event 触摸事件
     * @return  Handled
     */
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        PointF targetPoint = new PointF();      // targetPoint是中点,绘制时要转Left和Top

        // 计算targetPoint
        if (event.getAction() == MotionEvent.ACTION_UP) {   // 手抬起时,还原到特定位置
            if (!controlling)
                return false;
            controlling = false;

            // 根据不同的类型复位到不同的位置
            switch (mJoyStickStyle) {
                case JoyStickStylePower:
                    targetPoint.x = centerPoint.x;
                    if (alititudeHoldMode) {
                        targetPoint.y = centerPoint.y;
                    } else {
                        targetPoint.y = stickViewY + mStickView.getHeight() / 2;
                    }
                    break;
                case JoyStickStyleRanger:
                    targetPoint = centerPoint;
                    break;
            }
        } else {                                            // 其他情况根据情况计算位置
            // 触摸点
            PointF touchPoint = new PointF(event.getX(), event.getY());

            // 计算和中心点的距离
            float deltaX = touchPoint.x - centerPoint.x;
            float deltaY = touchPoint.y - centerPoint.y;
            // 计算触摸点到中点的距离
            float len = (float)Math.sqrt(deltaX * deltaX + deltaY * deltaY);

            // 如果触摸范围不在圆内，则不处理
            if (len > maxR && !controlling)
                return false;
            controlling = true;

            // 圆内外做不同的处理
            if (len > maxR) {   // 如果超过最大半径
                targetPoint.x = centerPoint.x + deltaX * maxR / len;
                targetPoint.y = centerPoint.y + deltaY * maxR / len;
            } else {            // 如果在圆内
                targetPoint = touchPoint;
            }
        }

        // 计算摇杆位置
        stickViewX = targetPoint.x - mStickView.getWidth() / 2;
        stickViewY = targetPoint.y - mStickView.getHeight() / 2;

        // Notify
        notifyStickMoved(new PointF(
                (targetPoint.x - centerPoint.x) / maxR,
                -(targetPoint.y - centerPoint.y) / maxR
        ));

        invalidate();
        // 如果是定高模式则不移动
        return true;
    }

    /**
     * 根据JoyStickStyle载入不同的背景图
     */
    private void loadStickViewBaseImage() {
        // Load image
        switch (mJoyStickStyle) {
            case JoyStickStylePower:
                mStickViewBase = BitmapFactory
                        .decodeResource(getResources(), R.mipmap.rudder_power);
                break;
            case JoyStickStyleRanger:
                mStickViewBase = BitmapFactory
                        .decodeResource(getResources(), R.mipmap.rudder_ranger);
                break;
        }
    }

    /**
     * Used for callback
     * 绘图坐标和控制坐标的Y轴是相反的,所以传入参数时point.y应取负
     * @param point [x, y] = [[-1, 1], [-1, 1]]
     */
    private void notifyStickMoved(PointF point) {
        if (mOnStickMovedListener != null) {
            mOnStickMovedListener.onStickMoved(new PointF(point.x, point.y));
        }
    }

    /**
     * Move stick to location
     * @param deltaToCenter [x, y] = [[-1, 1], [-1, 1]]
     */
    public void moveStickTo(PointF deltaToCenter) {
        // 计算摇杆位置
        stickViewX = centerPoint.x + deltaToCenter.x * maxR - mStickView.getWidth() / 2;
        stickViewY = centerPoint.y - deltaToCenter.y * maxR - mStickView.getHeight() / 2;

        // Notify
        notifyStickMoved(deltaToCenter);

        invalidate();
    }

    /**
     * Move stick to location
     * @param x [-1, 1]
     * @param y [-1, 1]
     */
    public void moveStickTo(float x, float y) {
        // 计算摇杆位置
        stickViewX = centerPoint.x + x * maxR - mStickView.getWidth() / 2;
        stickViewY = centerPoint.y - y * maxR - mStickView.getHeight() / 2;

        // Notify
        notifyStickMoved(new PointF(x, y));

        invalidate();
    }

    /**
     * 定高模式
     * @param alititudeHoldMode 定高模式开关
     */
    public void setAlititudeHoldMode(boolean alititudeHoldMode) {
        // JoyStickStylePower才有定高模式
        if (mJoyStickStyle == JoyStickStyle.JoyStickStylePower) {
            this.alititudeHoldMode = alititudeHoldMode;

            PointF point;
            if (alititudeHoldMode) {
                point = new PointF(0, 0);
            } else {
                point = new PointF(0, -1);
            }
//            if (isShown()) moveStickTo(point);

            if (!needRedraw) {
                // 计算摇杆位置
                stickViewX = centerPoint.x + point.x * maxR - mStickView.getWidth() / 2;
                stickViewY = centerPoint.y - point.y * maxR - mStickView.getHeight() / 2;
            }
            postInvalidate();

            // Notify
            notifyStickMoved(point);
        }
    }

    /**
     * Interface for callback
     */
    public interface OnStickMovedListener {
        void onStickMoved(PointF point);
    }

    // OnStickMovedListener Setter
    public void setOnStickMovedListener(OnStickMovedListener onStickMovedListener) {
        mOnStickMovedListener = onStickMovedListener;
    }

    // JoyStickStyle Setter
    public void setJoyStickStyle(JoyStickStyle joyStickStyle) {
        mJoyStickStyle = joyStickStyle;
        loadStickViewBaseImage();
        invalidate();
    }

}
