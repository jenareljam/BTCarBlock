package jenareljam.btcarblock;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;


public class MainActivity extends Activity {
    private static final String TAG = "MainActivity";

    // main activity only needs to register for BT connections
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.v(TAG, "Activity onCreate");

        this.startForegroundService(new Intent(this, BlockerService.class));
        //this.startForegroundService(new Intent(this, BTListenerService.class));
    }
}



