package com.capacitorjs.plugins.pushnotifications;

import static java.lang.System.currentTimeMillis;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class AckStorage {
    private ContentResolver contentResolver;

    public AckStorage(ContentResolver contentResolver) {
        this.contentResolver = contentResolver;
    }

    public void save(String deviceId, String notificationLogId, String interventionId, String origin) {
        try {
            String key = Long.toString(currentTimeMillis());

            contentResolver.insert(NotificationContentProviderDefinition.CONTENT_URI, createContentValues(key, new NotificationLogItem(key, deviceId, notificationLogId, interventionId, origin)));
        } catch (Exception ex) {
            Log.e("AckStorage", "logNotification : " + ex.getMessage());
        }
    }

    public void remove(String timeStamp) {
        String[] selectionArgs = {timeStamp};
        contentResolver.delete(NotificationContentProviderDefinition.CONTENT_URI, NotificationContentProviderDefinition.COLUMN_KEY + " = ?", selectionArgs);
    }

    public List<NotificationLogItem> getNotificationLogItems(int maxValues) {
        List<NotificationLogItem> values = new ArrayList<>();
        String[] projection = {
                NotificationContentProviderDefinition.COLUMN_KEY,
                NotificationContentProviderDefinition.COLUMN_DEVICEID,
                NotificationContentProviderDefinition.COLUMN_NOTIFICATIONLOGID,
                NotificationContentProviderDefinition.COLUMN_INTERVENTIONID,
                NotificationContentProviderDefinition.COLUMN_ORIGIN};
        Cursor cursor = contentResolver.query(NotificationContentProviderDefinition.CONTENT_URI, projection, null, null, NotificationContentProviderDefinition.COLUMN_KEY + " DESC");
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                while (!cursor.isAfterLast()) {
                    NotificationLogItem logItem = new NotificationLogItem(
                            cursor.getString(0),
                            cursor.getString(1),
                            cursor.getString(2),
                            cursor.getString(3),
                            cursor.getString(4));
                    values.add(logItem);
                    if (values.size() >= maxValues) {
                        break;
                    }
                    cursor.moveToNext();
                }
            }
            cursor.close();
        }
        return values;
    }

    private static ContentValues createContentValues(String key, NotificationLogItem logItem) {
        ContentValues values = new ContentValues();
        values.put(NotificationContentProviderDefinition.COLUMN_KEY, key);
        values.put(NotificationContentProviderDefinition.COLUMN_DEVICEID, logItem.deviceId);
        values.put(NotificationContentProviderDefinition.COLUMN_NOTIFICATIONLOGID, logItem.notificationLogId);
        values.put(NotificationContentProviderDefinition.COLUMN_INTERVENTIONID, logItem.interventionId);
        values.put(NotificationContentProviderDefinition.COLUMN_ORIGIN, logItem.origin);
        return values;
    }
}
