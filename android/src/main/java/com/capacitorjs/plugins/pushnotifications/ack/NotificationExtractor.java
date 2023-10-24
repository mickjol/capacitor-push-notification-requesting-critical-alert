package com.capacitorjs.plugins.pushnotifications.ack;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;

import com.google.firebase.messaging.Constants;
import com.google.firebase.messaging.NotificationParams;

public class NotificationExtractor {
    static final String ACTION_REMOTE_INTENT = "com.google.android.c2dm.intent.RECEIVE";

    private Intent intent;
    public NotificationExtractor(Intent intent) {
        this.intent = intent;
    }

    public Bundle getNotificationData() {
        String action = intent.getAction();
        if (ACTION_REMOTE_INTENT.equals(action)) {
            Log.i("NotificationExtractor", "Action is remote intent");
            String messageId = intent.getStringExtra(Constants.MessagePayloadKeys.MSGID);
            String messageType = intent.getStringExtra(Constants.MessagePayloadKeys.MESSAGE_TYPE);
            if (messageType == null || messageType == Constants.MessageTypes.MESSAGE) {
                Log.i("NotificationExtractor", "is message type messageId=" + messageId);
                Bundle data = intent.getExtras();
                if (data != null) {
                    Log.i("NotificationExtractor", "bundle found messageId=" + messageId);
                    if (NotificationParams.isNotification(data)) {
                        Log.i("NotificationExtractor", "is a notification messageId=" + messageId);
                        return data;
                    }
                }
            }
        }
        return null;
    }
}
