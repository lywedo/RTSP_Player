package cn.com.buildwin.gosky.widget.rudderview;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.TypedArray;
import android.graphics.PointF;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.RelativeLayout;

import cn.com.buildwin.gosky.R;
import cn.com.buildwin.gosky.application.Settings;

// TODO: 在onMeasure和onLayout中处理Padding

public class RudderView extends RelativeLayout implements SensorEventListener {

    // TODO: 设置横向和纵向刻度数
    public static final int H_SCALE_NUM = 24;
    public static final int V_SCALE_NUM = 24;

    // TODO: 重力感应的阈值(重力感应所能达到的最大值)
    private static final float GRAVITY_THRESHOLD = 0.75f;

    // TODO: HTrim宽度和VTrim高度的缩放比例(太长不好看)
    private static final float HTRIM_WIDTH_SCALE = 1.f;
    private static final float VTRIM_HEIGHT_SCALE = 1.f;

    // 控件类型
    public enum RudderStyle {
        RudderStylePower,
        RudderStyleRanger
    }
    private RudderStyle mRudderStyle;

    // 控件
    private JoyStickView mJoyStickView;
    private HTrimView mHTrimView;
    private VTrimView mVTrimView;

    // 监听接口
    private OnValueChangedListener mOnValueChangedListener;

    // 特殊功能
    private boolean rightHandMode = false;
    private boolean enableGravityControl = false;

    // 重力感应控制
    private SensorManager mSensorManager;
    private Sensor mSensor;

    private Context mContext;

    // 构造方法
    public RudderView(Context context) {
        super(context);
        mContext = context;
        init(null, 0);
    }
    // 构造方法
    public RudderView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        init(attrs, 0);
    }
    // 构造方法
    public RudderView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mContext = context;
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
                attrs, R.styleable.RudderView, defStyle, 0);

        // From attr to enum
        int rudderStyle = 0;
        if (a.hasValue(R.styleable.RudderView_RudderStyle)) {
            rudderStyle = a.getInt(R.styleable.RudderView_RudderStyle, 0);
        }
        mRudderStyle = RudderStyle.values()[rudderStyle];

        a.recycle();

        // 添加摇杆控件
        mJoyStickView = new JoyStickView(getContext());
        setRudderStyle(mRudderStyle);
        addView(mJoyStickView);
        mJoyStickView.setOnStickMovedListener(new JoyStickView.OnStickMovedListener() {
            @Override
            public void onStickMoved(PointF point) {
                if (mOnValueChangedListener != null) {
                    mOnValueChangedListener.onBasePointMoved(point);
                }
            }
        });
        mJoyStickView.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                // 重力感应控制时禁能触摸,返回true;平时使用false
                return enableGravityControl;
            }
        });

        // 添加横向微调控件
        mHTrimView = new HTrimView(getContext());
        mHTrimView.setScaleNum(H_SCALE_NUM);
        addView(mHTrimView);
        mHTrimView.setOnHTrimChangedListener(new HTrimView.OnHTrimChangedListener() {
            @Override
            public void onHTrimChanged(float value) {
                if (mOnValueChangedListener != null) {
                    mOnValueChangedListener.onHTrimValueChanged(value);
                }

                // Save Setting
                boolean autosave = Settings.getInstance(getContext()).getParameterForAutosave();
                if (autosave) {
                    if (mRudderStyle == RudderStyle.RudderStylePower) {
                        Settings.getInstance(getContext()).saveParameterForTrimRUDD(mHTrimView.getScaleValue());
                    } else {
                        Settings.getInstance(getContext()).saveParameterForTrimAIL(mHTrimView.getScaleValue());
                    }
                }
            }
        });

        // 添加纵向微调控件
        mVTrimView = new VTrimView(getContext());
        mVTrimView.setScaleNum(V_SCALE_NUM);
        addView(mVTrimView);
        mVTrimView.setOnVTrimChangedListener(new VTrimView.OnVTrimChangedListener() {
            @Override
            public void onVTrimChanged(float value) {
                if (mOnValueChangedListener != null) {
                    mOnValueChangedListener.onVTrimValueChanged(value);
                }

                // Save Setting
                boolean autosave = Settings.getInstance(getContext()).getParameterForAutosave();
                if (autosave) {
                    if (mRudderStyle == RudderStyle.RudderStyleRanger) {
                        Settings.getInstance(getContext()).saveParameterForTrimELE(mVTrimView.getScaleValue());
                    }
                }
            }
        });

        // Init Trim Value
        if (mRudderStyle == RudderStyle.RudderStylePower) {
            int trimRUDD = Settings.getInstance(getContext()).getParameterForTrimRUDD();
            mHTrimView.setScaleValue(trimRUDD);
        } else {
            int trimELE = Settings.getInstance(getContext()).getParameterForTrimELE();
            int trimAIL = Settings.getInstance(getContext()).getParameterForTrimAIL();
            mHTrimView.setScaleValue(trimAIL);
            mVTrimView.setScaleValue(trimELE);
        }
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);

        int width = getMeasuredWidth();
        int height = getMeasuredHeight();
        int centerX = width / 2;
        int centerY = height / 2;

//        int paddingLeft = getPaddingLeft();
//        int paddingTop = getPaddingTop();
//        int paddingRight = getPaddingRight();
//        int paddingBottom = getPaddingBottom();

        // Layout JoyStickView
        int widthJoyStickView = mJoyStickView.getMeasuredWidth();
        int heightJoyStickView = mJoyStickView.getMeasuredHeight();
        mJoyStickView.layout(
                centerX - widthJoyStickView / 2,
                centerY - heightJoyStickView / 2,
                centerX + widthJoyStickView / 2,
                centerY + heightJoyStickView / 2
        );

        // Layout HTrimView
        int widthHTrimView = mHTrimView.getMeasuredWidth();
        int heightHTrimView = mHTrimView.getMeasuredHeight();
        int narrowWidthHTrimView = (int)(widthHTrimView * (1 - HTRIM_WIDTH_SCALE) / 2);
        mHTrimView.layout(
                centerX - widthJoyStickView / 2 + narrowWidthHTrimView,
                centerY + heightJoyStickView / 2,
                centerX + widthJoyStickView / 2 - narrowWidthHTrimView,
                centerY + heightJoyStickView / 2 + heightHTrimView
        );

        // Layout VTrimView
        int widthVTrimView = mVTrimView.getMeasuredWidth();
        int heightVTrimView = mVTrimView.getMeasuredHeight();
        int narrowHeightVTrimView = (int)(heightVTrimView * (1 - VTRIM_HEIGHT_SCALE) / 2);
        if (rightHandMode) {
            mVTrimView.layout(
                    centerX - widthJoyStickView / 2 - widthVTrimView,
                    centerY - heightJoyStickView / 2  + narrowHeightVTrimView,
                    centerX - widthJoyStickView / 2,
                    centerY + heightJoyStickView / 2 - narrowHeightVTrimView
            );
        } else {
            mVTrimView.layout(
                    centerX + widthJoyStickView / 2,
                    centerY - heightJoyStickView / 2 + narrowHeightVTrimView,
                    centerX + widthJoyStickView / 2 + widthVTrimView,
                    centerY + heightJoyStickView / 2 - narrowHeightVTrimView
            );
        }

        // 如果是Power Rudder,则隐藏纵向微调控件
        // 如果不使用动态设置的话,使用GONE应该也没问题
        if (mRudderStyle == RudderStyle.RudderStylePower) {
            mVTrimView.setVisibility(INVISIBLE);
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        int widthMeasureSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightMeasureSize = MeasureSpec.getSize(heightMeasureSpec);
        int widthWithoutPadding = widthMeasureSize - getPaddingLeft() - getPaddingRight();
        int heightWithoutPadding = heightMeasureSize - getPaddingTop() - getPaddingBottom();

        int minEdge = Math.min(widthMeasureSize, heightMeasureSize);
        int longEdge = minEdge * 2 / 3;
        int shortEdge = (minEdge - longEdge) / 2;

        measureChild(mJoyStickView,
                MeasureSpec.makeMeasureSpec(longEdge, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(longEdge, MeasureSpec.EXACTLY));
        measureChild(mHTrimView,
                MeasureSpec.makeMeasureSpec(longEdge, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(shortEdge, MeasureSpec.EXACTLY));
        measureChild(mVTrimView,
                MeasureSpec.makeMeasureSpec(shortEdge, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(longEdge, MeasureSpec.EXACTLY));
    }

    /**
     * 设置Rudder样式,并载入不同的背景图
     * @param rudderStyle   样式
     */
    public void setRudderStyle(RudderStyle rudderStyle) {
        mRudderStyle = rudderStyle;
        switch (mRudderStyle) {
            case RudderStylePower:
                mJoyStickView.setJoyStickStyle(JoyStickView.JoyStickStyle.JoyStickStylePower);
                break;
            case RudderStyleRanger:
                mJoyStickView.setJoyStickStyle(JoyStickView.JoyStickStyle.JoyStickStyleRanger);
                break;
        }
    }

    /**
     * 设置回调接口
     * @param onValueChangedListener    回调接口
     */
    public void setOnValueChangedListener(OnValueChangedListener onValueChangedListener) {
        mOnValueChangedListener = onValueChangedListener;
    }

    /**
     * 回调接口
     */
    public interface OnValueChangedListener {
        void onBasePointMoved(PointF point);
        void onHTrimValueChanged(float value);
        void onVTrimValueChanged(float value);
    }

    /**
     * 传感器
     */
    @Override
    public void onSensorChanged(SensorEvent event) {
        float gravityX = event.values[1] / SensorManager.GRAVITY_EARTH;   // 相对手机坐标系取Y
        float gravityY = -event.values[0] / SensorManager.GRAVITY_EARTH;   // 相对手机坐标系取X
        // Set the max abs(value) is 0.75
        gravityX /= GRAVITY_THRESHOLD;
        gravityY /= GRAVITY_THRESHOLD;
        double r = Math.sqrt(gravityX * gravityX + gravityY * gravityY);
        // 解决越界
        if (r > 1.0) {
            double scale = r / 1.0;
            gravityX /= scale;
            gravityY /= scale;
        }

        mJoyStickView.moveStickTo(gravityX, gravityY);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    /**
     * Move stick to location
     * @param deltaToCenter [x, y] = [[-1, 1], [-1, 1]]
     */
    public void moveStickTo(PointF deltaToCenter) {
        float x = deltaToCenter.x;
        float y = deltaToCenter.y;
        if (x >= -1.0 && x <= 1.0
                && y >= -1.0 && y <= 1.0) {
            mJoyStickView.moveStickTo(x, y);
        }
    }

    /**
     * Move stick to location
     * @param x [-1, 1]
     * @param y [-1, 1]
     */
    public void moveStickTo(float x, float y) {
        if (x >= -1.0 && x <= 1.0
                && y >= -1.0 && y <= 1.0) {
            mJoyStickView.moveStickTo(x, y);
        }
    }

    /**
     * 设置右手模式
     * @param rightHandMode 右手模式开关
     */
    public void setRightHandMode(boolean rightHandMode) {
        this.rightHandMode = rightHandMode;
        requestLayout();
    }

    /**
     * 设置定高模式
     * @param alititudeHoldMode   定高模式开关
     */
    public void setAlititudeHoldMode(boolean alititudeHoldMode) {
        // RudderStylePower才可以设置定高
        if (mRudderStyle == RudderStyle.RudderStylePower) {
            mJoyStickView.setAlititudeHoldMode(alititudeHoldMode);
        }
    }

    /**
     * 是否支持重力传感器
     * @return  是否支持重力传感器
     */
    private boolean isSupportGrivitySensor() {
        SensorManager sensorManager = (SensorManager)getContext().getSystemService(Context.SENSOR_SERVICE);
        return mContext.getPackageManager().hasSystemFeature(PackageManager.FEATURE_SENSOR_GYROSCOPE)
                && sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY) != null;
    }

    /**
     * 是否支持加速度传感器
     * @return  是否支持加速度传感器
     */
    private boolean isSupportAccelerometerSensor() {
        SensorManager sensorManager = (SensorManager)getContext().getSystemService(Context.SENSOR_SERVICE);
        return mContext.getPackageManager().hasSystemFeature(PackageManager.FEATURE_SENSOR_ACCELEROMETER)
                && sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null;
    }

    /**
     * 是否支持重力控制。因为涉及Context，所以不作为静态方法使用。
     * @return  是否支持重力控制
     */
    public boolean isSupportGravityControl() {
        return isSupportGrivitySensor() || isSupportAccelerometerSensor();
    }

    /**
     * 使能/禁能重力控制，仅供Ranger Rudder使用，对Power Rudder无效，对不支持重力控制的设备无效
     * @param enableGravityControl  重力控制开关
     * @return  是否成功配置了重力控制
     */
    public boolean setEnableGravityControl(boolean enableGravityControl) {
        boolean s = false;
        if (isSupportGravityControl()
                && mRudderStyle == RudderStyle.RudderStyleRanger) {
            this.enableGravityControl = enableGravityControl;
            if (this.enableGravityControl) {
                // 重力感应控制
                mSensorManager = (SensorManager)getContext().getSystemService(Context.SENSOR_SERVICE);
                if (isSupportGrivitySensor())
                    mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY);
                else
//                if (isSupportAccelerometerSensor())
                    mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
                if (mSensor != null) {
                    mSensorManager.registerListener(this, mSensor, SensorManager.SENSOR_DELAY_GAME);
                    s = true;
                } else {
                    mSensorManager = null;
                }
            } else {
                if (mSensor != null && mSensorManager != null) {
                    mSensorManager.unregisterListener(this, mSensor);
                    mSensor = null;
                    mSensorManager = null;
                    mJoyStickView.moveStickTo(0, 0);
                    s = true;
                }
            }
        }
        return s;
    }

    /**
     * 获取横向微调值
     * @return  微调值
     */
    public float getHTrimValue() {
        return mHTrimView.getTrimValue();
    }

    /**
     * 获取纵向微调值
     * @return  微调值
     */
    public float getVTrimValue() {
        return mVTrimView.getTrimValue();
    }

}
