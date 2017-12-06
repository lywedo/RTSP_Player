package cn.com.buildwin.gosky.widget.rudderview;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Build;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import cn.com.buildwin.gosky.R;

public class HTrimView extends View {

    // 图像
    private Bitmap mTrimBackgroundView;
    private Bitmap mTrimSignView;
    // 图像属性
    // TODO: 修改图像需要根据具体的图像属性修改
//    private static final float BACKGROUND_WIDTH = 196;
//    private static final float BACKGROUND_HEIGHT = 23;
//    private static final float SIGN_WIDTH = 16;
//    private static final float SIGN_HEIGHT = 34;
//    private static final float ORIGINAL_WIDTH = BACKGROUND_WIDTH;
//    private static final float ORIGINAL_HEIGHT = BACKGROUND_HEIGHT;
//    private static final float BACKGROUND_SCALE = 1f;

    // 刻度界面属性
    private static final int DEFAULT_SCALE_NUM = 24;    // 默认左右分别的刻度值
    private int scaleNum;
    private int scaleValue;
    private float widthPerScale;
    private float backgroundX;
    private float backgroundY;
    private float signX;
    private float signY;
    private float centerX;

    // 重画标志
    private boolean needRedraw = true;

    // 声音
    private SoundPool mSoundPool;

    // HTrim Changed Listener
    private OnHTrimChangedListener mOnHTrimChangedListener;
    // 设置微调值监听器
    public void setOnHTrimChangedListener(OnHTrimChangedListener onHTrimChangedListener) {
        mOnHTrimChangedListener = onHTrimChangedListener;
    }

    public HTrimView(Context context) {
        super(context);
        init(null, 0);
    }

    public HTrimView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs, 0);
    }

    public HTrimView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(attrs, defStyle);
    }

    private void init(AttributeSet attrs, int defStyle) {
        // 设置刻度数量(这是左右分别的数量)
        scaleNum = DEFAULT_SCALE_NUM;
        // 初始位置(0=中间)
//        scaleValue = 0;
        // 载入图像资源
        mTrimBackgroundView = BitmapFactory.decodeResource(getResources(), R.mipmap.hslider);
        mTrimSignView = BitmapFactory.decodeResource(getResources(), R.mipmap.bar);

        // 载入声音资源
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            SoundPool.Builder builder = new SoundPool.Builder();
            builder.setMaxStreams(2);

            AudioAttributes.Builder attrBuilder = new AudioAttributes.Builder();
            attrBuilder.setLegacyStreamType(AudioManager.STREAM_SYSTEM);

            builder.setAudioAttributes(attrBuilder.build());
            mSoundPool = builder.build();
        } else {
            mSoundPool = new SoundPool(2, AudioManager.STREAM_SYSTEM, 5);
        }
        mSoundPool.load(getContext(), R.raw.btn_turn, 1);
        mSoundPool.load(getContext(), R.raw.btn_middle, 2);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mSoundPool.release();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // 需要重画时(包括第一次)
        if (needRedraw) {
            needRedraw = false;

            // View尺寸
            int viewWidth = getWidth();
            int viewHeight = getHeight();
            // 获取Padding
            int paddingLeft = getPaddingLeft();
            int paddingTop = getPaddingTop();
            int paddingRight = getPaddingRight();
            int paddingBottom = getPaddingBottom();

            // 计算Content的宽高
            int contentWidth = viewWidth - paddingLeft - paddingRight;
            int contentHeight = viewHeight - paddingTop - paddingBottom;

            // 根据background和bar确定算出最大的尺寸，用来计算缩放
            int backgroundWidth = mTrimBackgroundView.getWidth();
            int backgroundHeight = mTrimBackgroundView.getHeight();
            int barWidth = mTrimSignView.getWidth();
            int barHeight = mTrimSignView.getHeight();
            int maxWidth = backgroundWidth + barWidth;
            int maxHeight = backgroundHeight > barHeight ? backgroundHeight : barHeight;

            // 缩放
            float widthScale = (float)contentWidth / maxWidth;
            float heightScale = (float)contentHeight / maxHeight;
            float scale = widthScale < heightScale ? widthScale : heightScale;
            // 缩放后的background和bar尺寸
            backgroundWidth = (int)(mTrimBackgroundView.getWidth() * scale);
            backgroundHeight = (int)(mTrimBackgroundView.getHeight() * scale);
            barWidth = (int)(mTrimSignView.getWidth() * scale);
            barHeight = (int)(mTrimSignView.getHeight() * scale);
            // 缩放图像
            mTrimBackgroundView = Bitmap.createScaledBitmap(
                    mTrimBackgroundView,
                    backgroundWidth,
                    backgroundHeight,
                    true
            );
            mTrimSignView = Bitmap.createScaledBitmap(
                    mTrimSignView,
                    barWidth,
                    barHeight,
                    true
            );

            // 背景留出的空间，bar在边缘的时候占用
            int margin = barWidth / 2;

            // 计算每个刻度的宽度
            widthPerScale = backgroundWidth / (scaleNum * 2f);
            // 计算中点的X坐标
            centerX = paddingLeft + (backgroundWidth - barWidth) / 2f + margin;

            // 设置初始值
            backgroundX = paddingLeft + (contentWidth - backgroundWidth) / 2f;
            backgroundY = paddingTop + (contentHeight - backgroundHeight) / 2f;
            signX = paddingLeft + centerX + widthPerScale * scaleValue;
            signY = paddingTop + (contentHeight - barHeight) / 2f;
        }
        // Draw
        canvas.drawBitmap(mTrimBackgroundView, backgroundX, backgroundY, null);
        canvas.drawBitmap(mTrimSignView, signX, signY, null);
    }

    static float touchDownX;    // 记录TouchDown的X坐标

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {         // 处理TouchDown事件
            touchDownX = event.getX();
        } else if (event.getAction() == MotionEvent.ACTION_UP) {    // 处理TouchUp事件
            float x = event.getX();
            float y = event.getY();
            float width = getWidth();
            float height = getHeight();

            // 如果在布局范围内Up
            if (x > 0 && x < width && y > 0 && y < height) {
                // 按照点击时的X坐标判断
                if (touchDownX < centerX) {
                    trimLeft();
                } else {
                    trimRight();
                }
                // Callback
                if (mOnHTrimChangedListener != null) {
                    mOnHTrimChangedListener.onHTrimChanged(getTrimValue());
                }
            }
        }

        return true;
    }

    // 向左微调
    public void trimLeft() {
        if (scaleValue > -scaleNum) {
            scaleValue--;
            updateUI();
            // 播放声音
            if (scaleValue == 0) {
                mSoundPool.play(2, 1, 1, 0, 0, 1);
            } else {
                mSoundPool.play(1, 1, 1, 0, 0, 1);
            }
        }
    }

    // 向右微调
    public void trimRight() {
        if (scaleValue < scaleNum) {
            scaleValue++;
            updateUI();
            // 播放声音
            if (scaleValue == 0) {
                mSoundPool.play(2, 1, 1, 0, 0, 1);
            } else {
                mSoundPool.play(1, 1, 1, 0, 0, 1);
            }
        }
    }

    /**
     * 更新UI
     */
    private void updateUI() {
        signX = centerX + widthPerScale * scaleValue;
        invalidate();
    }

    /**
     * 设置刻度数
     * @param scaleNum  刻度数(左右分别的值)
     */
    public void setScaleNum(int scaleNum) {
        this.scaleNum = scaleNum;
        needRedraw = true;
        invalidate();
    }

    // 设置微调刻度值
    public void setScaleValue(int scaleValue) {
        this.scaleValue = scaleValue;

        if (scaleValue < -scaleNum) this.scaleValue = -scaleNum;
        if (scaleValue > scaleNum) this.scaleValue = scaleNum;

        updateUI();
    }

    // 获取微调刻度值
    public int getScaleValue() {
        return scaleValue;
    }

    // 获取微调值
    public float getTrimValue() {
        return (float)scaleValue / (float)scaleNum;
    }

    /**
     * Interface for callback
     */
    public interface OnHTrimChangedListener {
        void onHTrimChanged(float value);
    }

}
