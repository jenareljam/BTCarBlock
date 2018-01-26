package jenareljam.btcarblock;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.media.session.MediaSession;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaButtonReceiver;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;
import android.support.v4.app.NotificationCompat;

import java.io.IOException;

import static android.graphics.Bitmap.Config.ARGB_8888;
import static android.support.v4.media.MediaMetadataCompat.METADATA_KEY_ALBUM;
import static android.support.v4.media.MediaMetadataCompat.METADATA_KEY_ALBUM_ART;
import static android.support.v4.media.MediaMetadataCompat.METADATA_KEY_ARTIST;
import static android.support.v4.media.MediaMetadataCompat.METADATA_KEY_DURATION;
import static android.support.v4.media.MediaMetadataCompat.METADATA_KEY_TITLE;
import static android.support.v4.media.session.PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN;
import static android.support.v4.media.session.PlaybackStateCompat.STATE_PAUSED;
import static android.support.v4.media.session.PlaybackStateCompat.STATE_PLAYING;
import static android.support.v4.media.session.PlaybackStateCompat.STATE_STOPPED;


public class BlockerService extends Service {
    private static final String TAG = "BlockerService";
    private static final String NOTIFICATON_CHANNEL_ID = "NotificationChannel";
    private static final int NOTIFICATION_ID = 53;
    private BroadcastReceiver btreceiver;
    private MediaButtonReceiver mMediaButtonReceiver;
    private MediaPlayer mMediaPlayer;
    private MediaSessionCompat mMediaSession;
    private PlaybackStateCompat.Builder mStateBuilder;
    private MediaMetadataCompat.Builder mMediaMetadataCompat;
    private Handler teardownHandler = new Handler();
    private Runnable teardownRunnable = new Runnable() {
        @Override
        public void run() {
            Log.v(TAG, "Tearing down media stuff");
            unregisterReceiver(mMediaButtonReceiver);
            mMediaSession.release();
            mMediaPlayer.release();
        }
    };

    @Override
    public IBinder onBind(Intent intent) {
        return null;
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
                    handleBTConnection();
                }
            };
            this.registerReceiver(this.btreceiver, filter);
            Log.v(TAG, "Registered btreceiver");
        }
        makeNotification();
        Log.v(TAG, "Added Notification");

        // debug
        handleBTConnection();
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
        if (mMediaButtonReceiver != null) {
            this.unregisterReceiver(mMediaButtonReceiver);
        }
        if (mMediaSession != null) {
            mMediaSession.release();
        }
        if (mMediaPlayer != null) {
            mMediaPlayer.release();
        }
    }

    // create a mediaservice and "play" something so we can get the media button callback
    // set a 15s timer
    // at end of timer, release the mediaservice
    private void handleBTConnection() {
        Log.v(TAG, "Handling BT Connection");
        // register to handle media buttons
        mMediaButtonReceiver = new MediaButtonReceiver();
        IntentFilter filter = new IntentFilter(Intent.ACTION_MEDIA_BUTTON);
        registerReceiver(mMediaButtonReceiver, filter);
        // set up media player
        initMediaPlayer();
        initMediaSessionCompat();
        // play and pause to make our service the most-recently-used so it gets handed media events
        initPlay(); // play so we become the last media player to play
        initPause(); // pause immediately so we aren't actually playing
        // delay 15s, then do teardown
        Log.v(TAG, "Waiting 15 seconds, then tearing down media stuff");
        teardownHandler.postDelayed(teardownRunnable, 15000);
    }

    private void initMediaPlayer() {
        Log.v(TAG, "initMediaPlayer called");
        mMediaPlayer = new MediaPlayer();
        mMediaPlayer.setAudioAttributes(new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build());
        // TODO: remove the test file
        //AssetFileDescriptor afd = this.getResources().openRawResourceFd(R.raw.test);
        AssetFileDescriptor afd = this.getResources().openRawResourceFd(R.raw.silence);
        try {
            mMediaPlayer.setDataSource(afd);
            afd.close();
            mMediaPlayer.prepare();
        } catch (IOException e) {
            Log.v(TAG, "Error setting up Media Player");
            return;
        }

    }

    private void initMediaSessionCompat() {
        Log.v(TAG, "initMediaSessionCompat called");
        // Create a MediaSessionCompat
        mMediaSession = new MediaSessionCompat(this, TAG);
        // Enable callbacks from MediaButtons and TransportControls
        mMediaSession.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS | MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);
        // Set an initial PlaybackState with ACTION_PLAY, so media buttons can start the player
        mStateBuilder = new PlaybackStateCompat.Builder()
                .setActions(PlaybackStateCompat.ACTION_PLAY | PlaybackStateCompat.ACTION_PLAY_PAUSE);
                //.setState(STATE_STOPPED, 0, 0);
        mMediaSession.setPlaybackState(mStateBuilder.build());
        // Metadata
        mMediaMetadataCompat = new MediaMetadataCompat.Builder()
                .putLong(METADATA_KEY_DURATION, 5000)
                .putString(METADATA_KEY_TITLE, "Silence")
                .putString(METADATA_KEY_ARTIST, "")
                .putString(METADATA_KEY_ALBUM, "")
                .putBitmap(METADATA_KEY_ALBUM_ART, Bitmap.createBitmap(320, 320, ARGB_8888));
        mMediaSession.setMetadata(mMediaMetadataCompat.build());
        // MySessionCallback() has methods that handle callbacks from a media controller
        mMediaSession.setCallback(new MediaSessionCallback());
    }

    private void initPlay() {
        Log.v(TAG, "Called initPlay");
        mStateBuilder.setState(STATE_PLAYING, 0, 0);
        mMediaSession.setPlaybackState(mStateBuilder.build());
        Log.v(TAG, "Starting Media Player");
        mMediaPlayer.start();
        Log.v(TAG, "Started Media Player");
        if (mMediaPlayer.isPlaying()) {
            Log.v(TAG, "Media Playing");
        }
        Log.v(TAG, "Setting mediasessioncompat active");
        mMediaSession.setActive(true);
    }

    private void initPause() {
        Log.v(TAG, "Called initPause");
        mStateBuilder.setState(STATE_PAUSED, PLAYBACK_POSITION_UNKNOWN, 0);
        mMediaSession.setPlaybackState(mStateBuilder.build());
        Log.v(TAG, "Pausing Media Player");
        mMediaPlayer.pause();
        Log.v(TAG, "Paused Media Player");
    }

    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.v(TAG, "onStartCommand called");
        if(mMediaSession != null) {
            MediaButtonReceiver.handleIntent(mMediaSession, intent);
        }
        return super.onStartCommand(intent, flags, startId);
    }

    private final class MediaSessionCallback extends MediaSessionCompat.Callback {
        @Override
        public void onPlay() {
            Log.v(TAG, "Play event intercepted!");
        }
        @Override
        public void onPause() {
            Log.v(TAG, "onPause called");
        }
        @Override
        public void onStop() {
            Log.v(TAG, "onStop called");
        }
    }
}
