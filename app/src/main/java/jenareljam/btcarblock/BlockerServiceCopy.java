package jenareljam.btcarblock;

import android.app.Service;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.os.IBinder;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaButtonReceiver;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;

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


public class BlockerServiceCopy extends Service {
    private static final String TAG = "BlockerService";
    private MediaPlayer mMediaPlayer;
    private MediaSessionCompat mMediaSession;
    private PlaybackStateCompat.Builder mStateBuilder;
    private MediaMetadataCompat.Builder mMediaMetadataCompat;


    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        // create a mediaservice and "play" something so we can get the media button callback
        // set a 15s timer
        // at end of timer, release the mediaservice
        Log.v(TAG, "Handling BT Connection");
        // set up media player
        initMediaPlayer();
        initMediaSessionCompat();
        // play and pause to make our service the most-recently-used so it gets handed media events
        initPlay(); // play so we become the last media player to play
        initPause(); // pause immediately so we aren't actually playing
        // this service will be torn down (calling onDestroy) 15 seconds after its created

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mMediaSession != null) {
            mMediaSession.release();
        }
        if (mMediaPlayer != null) {
            mMediaPlayer.release();
        }
        Log.v(TAG, "BlockerService Destroyed");
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
        //Log.v(TAG, "Setting mediasessioncompat active");
        //mMediaSession.setActive(true);
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
