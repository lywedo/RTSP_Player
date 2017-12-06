package cn.com.buildwin.gosky.widget.flycontroller;

import java.util.Timer;
import java.util.TimerTask;

import cn.com.buildwin.gosky.application.Constants;

public class FlyController {

    private static final String TAG = FlyController.class.getSimpleName();

    private int controlByteAIL;  // 副翼
    private int controlByteELE;  // 升降舵

    private int controlByteTHR;  // 油门
    private int controlByteRUDD; // 方向舵
    private int trimByteAIL;
    private int trimByteELE;
    private int trimByteRUDD;

    private boolean enableLimitHigh = false;

    private int limitSpeedValue = 30;
    private float limitSpeedValuef = 0.3f;

    private boolean headlessMode = false;
    private boolean gyroCalibrateMode = false;
    private boolean flyupMode = false;
    private boolean flydownMode = false;
    private boolean returnMode = false;
    private boolean rotateMode = false;
    private boolean fixedDirectionRollMode = false;
    private boolean emergencyDownMode = false;
    private boolean rollMode = false;       // 按键状态，不代表触发状态
    private boolean triggeredRoll = false;  // 触发状态
    private boolean trackMode = false;
    private boolean lightOn = false;

    private Timer mFlyControlTimer;

    private FlyControllerDelegate mDelegate;

    /* Constructor */

    public FlyController() {

    }

    /* Setter and getter */

    public void setControlByteAIL(int controlByteAIL) {
        this.controlByteAIL = controlByteAIL;
    }

    public void setControlByteELE(int controlByteELE) {
        this.controlByteELE = controlByteELE;
    }

    public int getControlByteTHR() {
        return controlByteTHR;
    }

    public void setControlByteTHR(int controlByteTHR) {
        this.controlByteTHR = controlByteTHR;
    }

    public void setControlByteRUDD(int controlByteRUDD) {
        this.controlByteRUDD = controlByteRUDD;
    }

    public void setTrimByteAIL(int trimByteAIL) {
        this.trimByteAIL = trimByteAIL;
    }

    public void setTrimByteELE(int trimByteELE) {
        this.trimByteELE = trimByteELE;
    }

    public void setTrimByteRUDD(int trimByteRUDD) {
        this.trimByteRUDD = trimByteRUDD;
    }

    public boolean isEnableLimitHigh() {
        return enableLimitHigh;
    }

    public void setEnableLimitHigh(boolean enableLimitHigh) {
        this.enableLimitHigh = enableLimitHigh;
    }

    public void setLimitSpeedValue(int limitSpeedValue) {
        this.limitSpeedValue = limitSpeedValue;
        this.limitSpeedValuef = limitSpeedValue / 100f;
    }

    public boolean isHeadlessMode() {
        return headlessMode;
    }

    public void setHeadlessMode(boolean headlessMode) {
        this.headlessMode = headlessMode;
    }

    public boolean isGyroCalibrateMode() {
        return gyroCalibrateMode;
    }

    public void setGyroCalibrateMode(boolean gyroCalibrateMode) {
        this.gyroCalibrateMode = gyroCalibrateMode;
    }

    public boolean isFlyupMode() {
        return flyupMode;
    }

    public void setFlyupMode(boolean flyupMode) {
        this.flyupMode = flyupMode;
    }

    public boolean isFlydownMode() {
        return flydownMode;
    }

    public void setFlydownMode(boolean flydownMode) {
        this.flydownMode = flydownMode;
    }

    public boolean isReturnMode() {
        return returnMode;
    }

    public void setReturnMode(boolean returnMode) {
        this.returnMode = returnMode;
    }

    public boolean isRotateMode() {
        return rotateMode;
    }

    public void setRotateMode(boolean rotateMode) {
        this.rotateMode = rotateMode;
    }

    public boolean isFixedDirectionRollMode() {
        return fixedDirectionRollMode;
    }

    public void setFixedDirectionRollMode(boolean fixedDirectionRollMode) {
        this.fixedDirectionRollMode = fixedDirectionRollMode;
    }

    public boolean isEmergencyDownMode() {
        return emergencyDownMode;
    }

    public void setEmergencyDownMode(boolean emergencyDownMode) {
        this.emergencyDownMode = emergencyDownMode;
    }

    public boolean isRollMode() {
        return rollMode;
    }

    public void setRollMode(boolean rollMode) {
        this.rollMode = rollMode;
    }

    public boolean isTriggeredRoll() {
        return triggeredRoll;
    }

    public void setTriggeredRoll(boolean triggeredRoll) {
        this.triggeredRoll = triggeredRoll;
    }

    public boolean isTrackMode() {
        return trackMode;
    }

    public void setTrackMode(boolean trackMode) {
        this.trackMode = trackMode;
    }

    public boolean isLightOn() {
        return lightOn;
    }

    public void setLightOn(boolean lightOn) {
        this.lightOn = lightOn;
    }

    // Delegate

    public FlyControllerDelegate getDelegate() {
        return mDelegate;
    }

    public void setDelegate(FlyControllerDelegate delegate) {
        mDelegate = delegate;
    }

    private void sendFlyControllerData(int[] data) {
        if (getDelegate() != null) {
            getDelegate().sendFlyControllerData(data);
        }
    }

    /* Public method */

    /**
     * 发送飞控命令开关
     */
    public void sendFlyControllerData(boolean on) {
        if (on) {
            // Set Send Command Timer
            if (mFlyControlTimer == null) {
                mFlyControlTimer = new Timer();
                mFlyControlTimer.schedule(
                        new FlyControlTimerTask(),
                        0,
                        Constants.SEND_COMMAND_INTERVAL
                );
            }
        } else {
            // Close Send Command Timer
            if (mFlyControlTimer != null) {
                mFlyControlTimer.cancel();
                mFlyControlTimer = null;
            }
        }
    }

    /* Private method */

    /**
     * 发送飞控命令数据TimerTask
     */
    private class FlyControlTimerTask extends TimerTask {
        @Override
        public void run() {
            int[] data = generateFlyControllerData();
            // Output for debug
//            Log.d(TAG, "" + Arrays.toString(data));
            sendFlyControllerData(data);
        }
    }

    private static final int COMMAND_LENGTH = 11;

    /**
     * 生成飞控命令
     */
    private int[] generateFlyControllerData() {
        int controlBytes[] = new int[COMMAND_LENGTH];

        int controlByteAIL_l = controlByteAIL;  // 副翼
        int controlByteELE_l = controlByteELE;  // 升降舵
        int controlByteTHR_l = controlByteTHR;  // 油门
        int controlByteRUDD_l = controlByteRUDD; // 方向舵
        int trimByteAIL_l = trimByteAIL;
        int trimByteELE_l = trimByteELE;
        int trimByteRUDD_l = trimByteRUDD;

        if (!triggeredRoll) {
            // 速度限制（30%、60%、100%）
            // 按线性限定
            // AIL
            if (controlByteAIL_l < 0x80) {
                int deltaAIL = 0x80 - controlByteAIL_l;
                deltaAIL *= limitSpeedValuef;
                controlByteAIL_l = 0x80 - deltaAIL;
            } else if (controlByteAIL_l > 0x80) {
                int deltaAIL = controlByteAIL_l - 0x80;
                deltaAIL *= limitSpeedValuef;
                controlByteAIL_l = 0x80 + deltaAIL;
            }
            // ELE
            if (controlByteELE_l < 0x80) {
                int deltaELE = 0x80 - controlByteELE_l;
                deltaELE *= limitSpeedValuef;
                controlByteELE_l = 0x80 - deltaELE;
            } else if (controlByteELE_l > 0x80) {
                int deltaELE = controlByteELE_l - 0x80;
                deltaELE *= limitSpeedValuef;
                controlByteELE_l = 0x80 + deltaELE;
            }
        }

        // 如果是限高模式,则油门值始终为0x80
//        if (enableLimitHigh) controlByteTHR_l = 0x80;   // 客户要求限高还是可以调油门，松手后回中点，所以注释掉

        // 定高
        int bitAltitudeHold = enableLimitHigh ? 1 : 0;

        // 一键起飞
        int bitFlyup = flyupMode ? 1 : 0;
        // 一键下降
        int bitFlydown = flydownMode ? 1 : 0;
        // 一键返回
        int bitReturnMode = returnMode ? 1 : 0;
        // 一键旋转
        int bitRotate = rotateMode ? 1 : 0;
        // 一键固定方向翻转
        int bitFixedDirectionRotate = fixedDirectionRollMode ? 1 : 0;
        // 无头模式
        int bitHeadless = headlessMode ? 1 : 0;
        // 一键翻转
//            int bitRoll = rollMode ? 1 : 0;
        int bitRoll = triggeredRoll ? 1 : 0;
        // 紧急降落
        int bitEmergencyDown = emergencyDownMode ? 1 : 0;

        // 校正陀螺仪
        int bitGyroCalibrate = gyroCalibrateMode ? 1 : 0;

        // 灯光控制
        int bitLightOn = lightOn ? 1 : 0;

//        controlBytes[0] = 0x66;
//        controlBytes[1] = controlByteAIL_l;
//        controlBytes[2] = controlByteELE_l;
//        controlBytes[3] = controlByteTHR_l;
//        controlBytes[4] = controlByteRUDD_l;
//        controlBytes[5] = bitAltitudeHold           // bit0 = Altitude Hold
//                | (limitSpeedValue << 1);   // bit1-7 = Limited Speed
//        controlBytes[6] = controlBytes[1]
//                ^ controlBytes[2]
//                ^ controlBytes[3]
//                ^ controlBytes[4]
//                ^ controlBytes[5];
//        controlBytes[7] = 0x99;
//        controlBytes[8] = trimByteELE_l;
//        controlBytes[9] = trimByteAIL_l;
//        controlBytes[10] = trimByteRUDD_l;
//        controlBytes[11] = bitFlyup                     // bit0 = Flyup
//                | (bitFlydown << 1)             // bit1 = Flydown
//                | (bitReturnMode << 2)          // bit2 = Return Mode
//                | (bitFixedDirectionRotate << 3)  // bit3 = Fixed Direction Rotate Mode
//                | (bitHeadless << 4)            // bit4 = Headless Mode
//                | (bitRotate << 5)              // bit5 = Rotate Mode
//                | (bitEmergencyDown << 6)       // bit6 = Emergency Down Mode
//                | (bitGyroCalibrate << 7)       // bit7 = Gyro Calibrate Mode
//        ;
//        controlBytes[12] = bitRoll                      // bit0 = Roll Mode
//                | (bitLightOn << 1);

        controlBytes[0] = 0x66;
        controlBytes[1] = controlByteAIL_l;
        controlBytes[2] = controlByteELE_l;
        controlBytes[3] = controlByteTHR_l;
        controlBytes[4] = controlByteRUDD_l;
        controlBytes[5] = trimByteAIL_l;
        controlBytes[6] = trimByteELE_l;
        controlBytes[7] = trimByteRUDD_l;
        controlBytes[8] = bitFlyup         // bit0 = Flyup
                | (bitFlydown << 1)         // bit1 = Flydown
//                | (bitReturnMode << 2)      // bit2 = Return Mode
                | (bitEmergencyDown << 2)   // bit2 = Emergency stop
                | (bitRoll << 3)          // bit3 = Roll Mode
                | (bitHeadless << 4)        // bit4 = Headless Mode
                | (bitRotate << 5)            // bit5 = Rotate Mode
                | (bitLightOn << 6)         // bit6 = Light Control
                | (bitGyroCalibrate << 7)   // bit7 = Gyro Calibrate Mode
        ;
        controlBytes[9] = controlBytes[1]
                ^ controlBytes[2]
                ^ controlBytes[3]
                ^ controlBytes[4]
                ^ controlBytes[5]
                ^ controlBytes[6]
                ^ controlBytes[7]
                ^ controlBytes[8];
        controlBytes[10] = 0x99;

        return controlBytes;
    }

}
