package cn.com.buildwin.gosky.application;

import android.content.Context;
import android.content.SharedPreferences;

public class Settings {

    private static final String PARAMETER_SETTING_PARAMETERS_AUTO_SAVE =
            "cn.com.buildwin.gosky.parameters_auto_save";
    private static final String PARAMETER_SETTING_RIGHT_HAND_MODE =
            "cn.com.buildwin.gosky.right_hand_mode";
    private static final String PARAMETER_SETTING_TRIM_RUDD =
            "cn.com.buildwin.gosky.trim_rudd";
    private static final String PARAMETER_SETTING_TRIM_ELE =
            "cn.com.buildwin.gosky.trim_ele";
    private static final String PARAMETER_SETTING_TRIM_AIL =
            "cn.com.buildwin.gosky.trim_ail";
    private static final String PARAMETER_SETTING_ALTITUDE_HOLD =
            "cn.com.buildwin.gosky.altitude_hold";
    private static final String PARAMETER_SETTING_SPEED_LIMIT =
            "cn.com.buildwin.gosky.speed_limit";

    static private Settings instance = null;
    static private Context mContext;

    private Settings (Context context) {
        super();
        mContext = context;
    }

    static public Settings getInstance(Context context) {
        synchronized (Settings.class) {
            if (instance == null) {
                instance = new Settings(context);
            }
        }
        return instance;
    }

    /**
     * 重置设置
     */
    public void resetSettings() {
        SharedPreferences settings = mContext.getSharedPreferences(Constants.PREFS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
//        editor.putBoolean(PARAMETER_SETTING_PARAMETERS_AUTO_SAVE, true);
//        editor.putBoolean(PARAMETER_SETTING_RIGHT_HAND_MODE, false);
        editor.putInt(PARAMETER_SETTING_TRIM_RUDD, 0);
        editor.putInt(PARAMETER_SETTING_TRIM_ELE, 0);
        editor.putInt(PARAMETER_SETTING_TRIM_AIL, 0);
        editor.putBoolean(PARAMETER_SETTING_ALTITUDE_HOLD, false);
        editor.putInt(PARAMETER_SETTING_SPEED_LIMIT, 0);
//        editor.apply();
        editor.commit();
    }

    // ---------------- Set ----------------

    private void putBoolean(String s, boolean b) {
        SharedPreferences settings = mContext.getSharedPreferences(Constants.PREFS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putBoolean(s, b);
//        editor.apply();
        editor.commit();
    }

    private void putInt(String s, int i) {
        SharedPreferences settings = mContext.getSharedPreferences(Constants.PREFS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putInt(s, i);
//        editor.apply();
        editor.commit();
    }

    // ----------------

    public void saveParameterForAutosave(boolean autosave) {
        putBoolean(PARAMETER_SETTING_PARAMETERS_AUTO_SAVE, autosave);
    }

    public void saveParameterForRightHandMode(boolean rightHandMode) {
        putBoolean(PARAMETER_SETTING_RIGHT_HAND_MODE, rightHandMode);
    }

    public void saveParameterForTrimRUDD(int trimValue) {
        putInt(PARAMETER_SETTING_TRIM_RUDD, trimValue);
    }

    public void saveParameterForTrimELE(int trimValue) {
        putInt(PARAMETER_SETTING_TRIM_ELE, trimValue);
    }

    public void saveParameterForTrimAIL(int trimValue) {
        putInt(PARAMETER_SETTING_TRIM_AIL, trimValue);
    }

    public void saveParameterForAltitudeHold(boolean altitudeHold) {
        putBoolean(PARAMETER_SETTING_ALTITUDE_HOLD, altitudeHold);
    }

    public void saveParameterForSpeedLimit(int speedLimit) {
        putInt(PARAMETER_SETTING_SPEED_LIMIT, speedLimit);
    }

    // ---------------- Get ----------------

//    private boolean getBoolean(String s) {
//        SharedPreferences settings = mContext.getSharedPreferences(Constants.PREFS_NAME, 0);
//        return settings.getBoolean(s, false);
//    }

    private int getInt(String s) {
        SharedPreferences settings = mContext.getSharedPreferences(Constants.PREFS_NAME, 0);
        return settings.getInt(s, 0);
    }

    // ----------------

    public boolean getParameterForAutosave() {
        SharedPreferences settings = mContext.getSharedPreferences(Constants.PREFS_NAME, 0);
//        return settings.getBoolean(PARAMETER_SETTING_PARAMETERS_AUTO_SAVE, true); // Default is true
        return settings.getBoolean(PARAMETER_SETTING_PARAMETERS_AUTO_SAVE, false); // Default is false
    }

    public boolean getParameterForRightHandMode() {
        SharedPreferences settings = mContext.getSharedPreferences(Constants.PREFS_NAME, 0);
        return settings.getBoolean(PARAMETER_SETTING_RIGHT_HAND_MODE, false); // Default is false
    }

    public int getParameterForTrimRUDD() {
        return getInt(PARAMETER_SETTING_TRIM_RUDD);
    }

    public int getParameterForTrimELE() {
        return getInt(PARAMETER_SETTING_TRIM_ELE);
    }

    public int getParameterForTrimAIL() {
        return getInt(PARAMETER_SETTING_TRIM_AIL);
    }

    public boolean getParameterForAltitudeHold() {
        SharedPreferences settings = mContext.getSharedPreferences(Constants.PREFS_NAME, 0);
        return settings.getBoolean(PARAMETER_SETTING_ALTITUDE_HOLD, false); // Default is false
    }

    public int getParameterForSpeedLimit() {
        return getInt(PARAMETER_SETTING_SPEED_LIMIT);
    }

}
