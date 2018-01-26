package jenareljam.btcarblock;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

public class BTListenerService extends Service {
    private static final String TAG = "BTListenerService";
    private static final String NOTIFICATON_CHANNEL_ID = "NotificationChannel";
    private static final int NOTIFICATION_ID = 53;
    private BroadcastReceiver btreceiver;
    private Intent mBlockerServiceIntent;
    private Handler teardownHandler = new Handler();
    private Runnable teardownRunnable = new Runnable() {
        @Override
        public void run() {
            Log.v(TAG, "Tearing down media stuff");
            if (mBlockerServiceIntent != null) {
                stopService(mBlockerServiceIntent);
            }
        }
    };

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onCreate() {
        super.onCreate();

        Log.v(TAG, "Creating service");
        if (this.btreceiver == null) {
            IntentFilter filter = new IntentFilter();
            filter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
            this.btreceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    if (mBlockerServiceIntent == null) {
                        mBlockerServiceIntent = new Intent(context, BlockerService.class);
                        startService(mBlockerServiceIntent);
                        Log.v(TAG, "Waiting 15 seconds, then tearing down media stuff");
                        teardownHandler.postDelayed(teardownRunnable, 15000);
                    }
                }
            };
            this.registerReceiver(this.btreceiver, filter);
            Log.v(TAG, "Registered btreceiver");
        }
        makeNotification();
        Log.v(TAG, "Added Notification");

        // debug
        if (mBlockerServiceIntent == null) {
            mBlockerServiceIntent = new Intent(this, BlockerService.class);
            startService(mBlockerServiceIntent);
            Log.v(TAG, "Waiting 15 seconds, then tearing down media stuff");
            teardownHandler.postDelayed(teardownRunnable, 15000);
        }
    }

    private void makeNotification() {
        NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationChannel mChannel = new NotificationChannel(NOTIFICATON_CHANNEL_ID, "BTCarBlockChannel", NotificationManager.IMPORTANCE_LOW);
        mChannel.setDescription("Channel for Notifications for the BTCarBlock App");
        mChannel.enableLights(false);
        mChannel.enableVibration(false);
        mNotificationManager.createNotificationChannel(mChannel);
        // make notification
        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this, NOTIFICATON_CHANNEL_ID)
                .setSmallIcon(android.R.drawable.btn_star)
                .setContentTitle("BTCarBlock")
                .setContentText("Waiting for events to block...");
        // Notify!
        startForeground(NOTIFICATION_ID, mBuilder.build());

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (btreceiver != null) {
            this.unregisterReceiver(btreceiver);
        }
    }
}
