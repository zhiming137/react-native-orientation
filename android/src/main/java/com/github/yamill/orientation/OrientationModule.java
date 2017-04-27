package com.github.yamill.orientation;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.media.ExifInterface;
import android.util.Log;
import android.view.OrientationEventListener;
import android.view.Surface;
import android.view.WindowManager;

import com.facebook.common.logging.FLog;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.LifecycleEventListener;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.common.ReactConstants;
import com.facebook.react.modules.core.DeviceEventManagerModule;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nullable;

class OrientationModule extends ReactContextBaseJavaModule implements LifecycleEventListener{
    private static final String TAG = "OrientationModule";
    private OrientationEventListener mOrientationListener; // 屏幕方向改变监听器

    private int screenOrientation;

    public OrientationModule(ReactApplicationContext reactContext) {
        super(reactContext);
        final ReactApplicationContext ctx = reactContext;


        mOrientationListener = new OrientationEventListener(ctx) {
            @Override
            public void onOrientationChanged(int i) {
                // i的范围是0～359
                // 屏幕左边在顶部的时候 i = 90;
                // 屏幕顶部在底部的时候 i = 180;
                // 屏幕右边在底部的时候 i = 270;
                // 正常情况默认i = 0;

                int mScreenExifOrientation;
                if (45 <= i && i < 135) {
                    mScreenExifOrientation = ExifInterface.ORIENTATION_ROTATE_90;
                } else if (135 <= i && i < 225) {
                    mScreenExifOrientation = ExifInterface.ORIENTATION_ROTATE_180;
                } else if (225 <= i && i < 315) {
                    mScreenExifOrientation = ExifInterface.ORIENTATION_ROTATE_270;
                } else {
                    mScreenExifOrientation = ExifInterface.ORIENTATION_NORMAL;
                }

                if (OrientationModule.this.screenOrientation != mScreenExifOrientation) {
                    OrientationModule.this.screenOrientation = mScreenExifOrientation;

                    String orientation = OrientationModule.this.getOrientationString();
                    String specificOrientation = OrientationModule.this.getSpecificOrientationString();

                    WritableMap params = Arguments.createMap();
                    params.putString("orientation", orientation);

                    WritableMap specificParams = Arguments.createMap();
                    specificParams.putString("specificOrientation", specificOrientation);

                    ctx.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                            .emit("orientationDidChange", params);
                    ctx.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                            .emit("specificOrientationDidChange", specificParams);
                }
            }
        };
        ctx.addLifecycleEventListener(this);
    }

    @Override
    public String getName() {
        return "Orientation";
    }

    @ReactMethod
    public void getOrientation(Callback callback) {

        String orientation = this.getOrientationString();

        if (orientation == "null") {
            callback.invoke(null, null);
        } else {
            callback.invoke(null, orientation);
        }
    }

    @ReactMethod
    public void getSpecificOrientation(Callback callback) {

        String orientation = this.getSpecificOrientationString();

        if (orientation == "null") {
            callback.invoke(null, null);
        } else {
            callback.invoke(null, orientation);
        }
    }

    @ReactMethod
    public void lockToPortrait() {
        final Activity activity = getCurrentActivity();
        if (activity == null) {
            return;
        }
        activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
    }

    @ReactMethod
    public void lockToLandscape() {
        final Activity activity = getCurrentActivity();
        if (activity == null) {
            return;
        }
        activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
    }

    @ReactMethod
    public void lockToLandscapeLeft() {
        final Activity activity = getCurrentActivity();
        if (activity == null) {
            return;
        }
        activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
    }

    @ReactMethod
    public void lockToLandscapeRight() {
        final Activity activity = getCurrentActivity();
        if (activity == null) {
            return;
        }
        activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE);
    }

    @ReactMethod
    public void unlockAllOrientations() {
        final Activity activity = getCurrentActivity();
        if (activity == null) {
            return;
        }
        activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
    }

    @Override
    public @Nullable Map<String, Object> getConstants() {
        HashMap<String, Object> constants = new HashMap<String, Object>();

        String orientation = this.getOrientationString();
        if (orientation == "null") {
            constants.put("initialOrientation", null);
        } else {
            constants.put("initialOrientation", orientation);
        }

        return constants;
    }

    private String getOrientationString() {
        String orientationString = this.getSpecificOrientationString();
        if (orientationString == "LANDSCAPE-RIGHT" || orientationString == "LANDSCAPE-LEFT") {
            return "LANDSCAPE";
        }
        return orientationString;
    }

    private String getSpecificOrientationString() {
        if (screenOrientation == ExifInterface.ORIENTATION_NORMAL) {
            return "PORTRAIT";
        } else if (screenOrientation == ExifInterface.ORIENTATION_ROTATE_90) {
            return "LANDSCAPE-RIGHT";
        } else if (screenOrientation == ExifInterface.ORIENTATION_ROTATE_180) {
            return "PORTRAITUPSIDEDOWN";
        } else if (screenOrientation == ExifInterface.ORIENTATION_ROTATE_270) {
            return "LANDSCAPE-LEFT";
        } else {
            return "UNKNOWN";
        }
    }

    @Override
    public void onHostResume() {
        final Activity activity = getCurrentActivity();
        Log.d(TAG, "onHostResume: ");

        assert activity != null;
        mOrientationListener.enable();
    }
    @Override
    public void onHostPause() {
        final Activity activity = getCurrentActivity();
        if (activity == null) return;
        try
        {
            Log.d(TAG, "onHostPause: ");
            mOrientationListener.disable();
        }
        catch (java.lang.IllegalArgumentException e) {
            FLog.e(ReactConstants.TAG, "receiver already unregistered", e);
        }
    }

    @Override
    public void onHostDestroy() {
        final Activity activity = getCurrentActivity();
        if (activity == null) return;
        try
        {
            Log.d(TAG, "onHostDestroy: ");
            mOrientationListener.disable();
        }
        catch (java.lang.IllegalArgumentException e) {
            FLog.e(ReactConstants.TAG, "receiver already unregistered", e);
        }
	}
}

