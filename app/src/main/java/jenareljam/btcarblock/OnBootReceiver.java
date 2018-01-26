package jenareljam.btcarblock;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class OnBootReceiver extends BroadcastReceiver {
    private static final String TAG = "OnBootReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.v(TAG, "Callback on boot");

        context.startForegroundService(new Intent(context, BlockerService.class));
        //context.startForegroundService(new Intent(context, BTListenerService.class));
    }
}