package com.capacitorjs.plugins.pushnotifications;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import java.util.List;

import androidx.annotation.NonNull;
import com.getcapacitor.JSObject;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import com.capacitorjs.plugins.pushnotifications.acknowledge.AcknowledgeService;

public class MessagingService extends FirebaseMessagingService {
    static final String PREFS_NAME = "PushNotificationsPending";
    static final String PREFS_KEY = "pendingNotification";

    private final AcknowledgeService acknowledgeService = new AcknowledgeService();

    public void handleIntent(Intent intent) {
        Log.i("MessagingService", "intent received");
        storePendingNotificationFromIntent(intent);
        super.handleIntent(intent);
        this.acknowledgeService.initContent(this);
        this.acknowledgeService.newNotification(intent);
    }

    private boolean isAppInForeground() {
        ActivityManager activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        if (activityManager == null) return false;
        String packageName = getPackageName();
        List<ActivityManager.RunningAppProcessInfo> processes = activityManager.getRunningAppProcesses();
        if (processes == null) return false;
        for (ActivityManager.RunningAppProcessInfo process : processes) {
            if (process.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND
                    && process.processName.equals(packageName)) {
                return true;
            }
        }
        return false;
    }

    private void storePendingNotificationFromIntent(Intent intent) {
        if (isAppInForeground()) return;

        Bundle extras = intent.getExtras();
        if (extras == null) {
            Log.w("MessagingService", "storePending: extras are null");
            return;
        }

        JSObject notificationJson = new JSObject();
        JSObject dataObject = new JSObject();
        for (String key : extras.keySet()) {
            if (key.equals("google.message_id")) {
                notificationJson.put("id", extras.getString(key));
            } else {
                dataObject.put(key, extras.get(key));
            }
        }
        notificationJson.put("data", dataObject);

        String title = dataObject.getString("notificationTitle");
        if (title == null || title.isEmpty()) return;

        getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(PREFS_KEY, notificationJson.toString())
            .apply();
        Log.i("MessagingService", "storePending: saved to SharedPreferences = " + notificationJson);
    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);
        PushNotificationsPlugin.sendRemoteMessage(remoteMessage);
    }

    @Override
    public void onNewToken(@NonNull String s) {
        super.onNewToken(s);
        PushNotificationsPlugin.onNewToken(s);
    }
}
