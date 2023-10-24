package com.capacitorjs.plugins.pushnotifications;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.provider.Settings;
import android.util.Log;

import androidx.annotation.NonNull;

import com.getcapacitor.CapConfig;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.List;

public class MessagingService extends FirebaseMessagingService {

    private static int MAX_LOGS_IN_STORAGE = 5;
    private AckStorage ackStorage;
    private String apiUrl;

    @Override
    public void handleIntent(Intent intent) {
        Log.i("MessagingService", "intent received");
        saveNotification(intent);
        KeepMaxLogs();
        super.handleIntent(intent);
        tryPost();
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

    private String getDeviceId() {
        return Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
    }

    private String getOrigin() {
        return "Native Android " + Build.VERSION.RELEASE + " " + Build.MODEL;
    }

    private void saveNotification(Intent intent) {
        try {
            if (ackStorage == null) {
                ackStorage = new AckStorage(getContentResolver());
            }

            NotificationExtractor NotificationExtractor = new NotificationExtractor(intent);

            Bundle data = NotificationExtractor.getNotificationData();
            if (data != null) {
                String notificationLogId = data.getString("notificationLogId");
                String interventionId = data.getString("interventionId");
                Log.i("MessagingService", "receive notification id=" + notificationLogId);

                ackStorage.save(getDeviceId(), notificationLogId, interventionId, getOrigin());
                Log.i("MessagingService", "save notification id=" + notificationLogId);
            }
        } catch (Exception ex) {
            Log.e("MessagingService", "ackNotification exception : " + ex.getMessage());
        }
    }

    private void KeepMaxLogs() {
        List<NotificationLogItem> logItems = ackStorage.getNotificationLogItems(MAX_LOGS_IN_STORAGE * 2);
        for (int i = MAX_LOGS_IN_STORAGE; i < logItems.size(); i++) {
            Log.i("MessagingService", "Notification not sent " + logItems.get(i).notificationLogId);
            ackStorage.remove(logItems.get(i).timeStamp);
        }
    }

    private void tryPost() {
        try {
            if (apiUrl == null) {
                CapConfig capConfig = CapConfig.loadDefault(getApplicationContext());
                apiUrl = capConfig.getString("plugins.PushNotifications.apiUrl", "localhost:5555");
            }
            AckPublisher ackPublisher = new AckPublisher(apiUrl);
            List<NotificationLogItem> logItems = ackStorage.getNotificationLogItems(MAX_LOGS_IN_STORAGE);
            int allowedTimeMs = 3000;
            long startTime = SystemClock.elapsedRealtime();
            for (NotificationLogItem logItem : logItems) {
                var result = ackPublisher.post(logItem, allowedTimeMs);
                if (result) {
                    ackStorage.remove(logItem.timeStamp);
                }
                long endTime = SystemClock.elapsedRealtime();
                long elapsedMillisec = endTime - startTime;
                allowedTimeMs -= elapsedMillisec;
                Log.i("MessagingService", "post notification " + logItem.notificationLogId + " with result " + result + " in " + elapsedMillisec + " ms" + " remaining " + allowedTimeMs + " ms");
                if (allowedTimeMs <= 100) {
                    break;
                }
                startTime = endTime;
            }
        } catch (Exception ex) {
            Log.e("MessagingService", "tryPost : " + ex.getMessage());
        }
    }

}
