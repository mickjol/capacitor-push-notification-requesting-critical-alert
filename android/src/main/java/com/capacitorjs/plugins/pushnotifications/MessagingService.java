package com.capacitorjs.plugins.pushnotifications;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.provider.Settings;
import android.util.Log;

import androidx.annotation.NonNull;

import com.capacitorjs.plugins.pushnotifications.ack.DeviceInfoExtractor;
import com.capacitorjs.plugins.pushnotifications.ack.NotificationAckPublisher;
import com.capacitorjs.plugins.pushnotifications.ack.NotificationAckService;
import com.capacitorjs.plugins.pushnotifications.ack.storage.NotificationAckStorage;
import com.capacitorjs.plugins.pushnotifications.ack.NotificationExtractor;
import com.capacitorjs.plugins.pushnotifications.ack.NotificationLogItem;
import com.getcapacitor.CapConfig;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.List;

public class MessagingService extends FirebaseMessagingService {
    NotificationAckService notificationAckService = new NotificationAckService();

    @Override
    public void handleIntent(Intent intent) {
        Log.i("MessagingService", "intent received");
        notificationAckService.initContext(this);
        notificationAckService.saveNotification(intent);
        super.handleIntent(intent);
        notificationAckService.PostNotificationAck();
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
