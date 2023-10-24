package com.capacitorjs.plugins.pushnotifications.ack;

import android.content.Context;
import android.os.Build;
import android.provider.Settings;

public class DeviceInfoExtractor {

    Context context;
    public DeviceInfoExtractor(Context context) {
        this.context = context;
    }
    public String getDeviceId() {
        return Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
    }

    public String getOrigin() {
        return "Native Android " + Build.VERSION.RELEASE + " " + Build.MODEL;
    }
}
