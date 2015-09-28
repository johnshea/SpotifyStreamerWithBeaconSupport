package com.example.android.SpotifyStreamerWithBeaconSupport.service;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import com.example.android.SpotifyStreamerWithBeaconSupport.EventActivity;
import com.example.android.SpotifyStreamerWithBeaconSupport.R;

public class BeaconScanningService extends Service {

    private NotificationManager mNotificationManager;
    private boolean useRealBeacon = false;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
//        return super.onStartCommand(intent, flags, startId);

        if ( intent.hasExtra("beaconType") ) {
            String beaconType = intent.getStringExtra("beaconType");

            if ( beaconType.equals("real") ) {
                useRealBeacon = true;
            } else {
                useRealBeacon = false;
            }
        }

        return Service.START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        if ( !useRealBeacon ) {

            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        Thread.sleep(5000);
                    } catch (Exception e) {

                    } finally {
                        showSimulatedNotification();
                    }

                }
            }).start();

        }

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    private void showSimulatedNotification() {

        Intent intent = new Intent(this, EventActivity.class);
        intent.putExtra("artist_name", "Rush");
        intent.putExtra("event_type", "CONCERT");
        intent.putExtra("event_info", "{\n" +
                "    \"beacon_location\": \"Grand Central Terminal\",\n" +
                "    \"events\": [\n" +
                "        {\n" +
                "            \"event_date\": \"2015-09-28T20:00:00.000Z\",\n" +
                "            \"event_type\": \"CONCERT\",\n" +
                "            \"event_location\": \"Madison Square Garden\",\n" +
                "            \"event_artist_id\": \"4gzpq5DPGxSnKTe4SA8HAU\",\n" +
                "            \"event_artist_name\": \"Coldplay\",\n" +
                "            \"event_artist_image\": \"https://i.scdn.co/image/68e20f364ba16a4386d8f55ca6bed5fb8da3136d\"\n" +
                "        },\n" +
                "        {\n" +
                "            \"event_date\": \"2015-09-29T00:00:00.000Z\",\n" +
                "            \"event_type\": \"NEWALBUM\",\n" +
                "            \"event_location\": \"\",\n" +
                "            \"event_artist_id\": \"2Hkut4rAAyrQxRdof7FVJq\",\n" +
                "            \"event_artist_name\": \"Rush\",\n" +
                "            \"event_artist_image\": \"https://i.scdn.co/image/e73da0511bf079227f1c83e03b439966095a2417\"\n" +
                "        }\n" +
                "    ]\n" +
                "}");

        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        final Notification notification = new Notification.Builder(this)
                .setContentTitle("Upcoming Spotify Events (Simulated)")
                .setContentText("1 Concert, 1 New Album")
                .setSmallIcon(R.drawable.icon_square)
                .setContentIntent(pendingIntent)
                .build();

        mNotificationManager.notify(999, notification);

    }
}
