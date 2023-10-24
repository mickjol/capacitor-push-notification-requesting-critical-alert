package com.capacitorjs.plugins.pushnotifications;

import android.util.Log;

import java.io.DataOutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class AckPublisher {

    private final String apiUrl;
    private final String acknoledgePath = "Intervention/Acknowledge";

    public AckPublisher(String apiUrl) {
        this.apiUrl = apiUrl;
    }

    public boolean post(NotificationLogItem logItem, int timeoutMillis) {
        String targetURL = apiUrl + acknoledgePath;
        boolean sent = false;
        try {
            if (logItem != null) {
                String requestBody = logItem.toJson();

                Log.w("AckPublisher", "posting " + requestBody + " to " + targetURL + " ...");

                URL url = new URL(targetURL);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();

                connection.setRequestMethod("POST");
                connection.setDoOutput(true);
                connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
                connection.setConnectTimeout(timeoutMillis);
                connection.setReadTimeout(timeoutMillis);

                DataOutputStream wr = new DataOutputStream(connection.getOutputStream());
                wr.writeBytes(requestBody);
                wr.flush();
                wr.close();

                int responseCode = connection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK || responseCode == HttpURLConnection.HTTP_NO_CONTENT) {
                    sent = true;
                } else {
                    Log.w("AckPublisher", "postNotification ack bad response=" + responseCode);
                }
                connection.disconnect();
            }
        } catch (Exception ex) {
            Log.e("AckPublisher", "postAcknowledge : " + ex.getMessage());
        }
        if (sent) {
            Log.w("AckPublisher", "postNotification ack successfull");
        } else {
            Log.w("AckPublisher", "postNotification not sent");
        }
        return sent;
    }
}
