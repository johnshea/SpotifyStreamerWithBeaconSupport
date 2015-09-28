package com.example.android.SpotifyStreamerWithBeaconSupport.service;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

import com.example.android.SpotifyStreamerWithBeaconSupport.Constants;
import com.example.android.SpotifyStreamerWithBeaconSupport.MainActivity;
import com.example.android.SpotifyStreamerWithBeaconSupport.R;
import com.example.android.SpotifyStreamerWithBeaconSupport.models.LocalArtist;
import com.example.android.SpotifyStreamerWithBeaconSupport.models.LocalTrack;
import com.squareup.picasso.Picasso;

import java.io.IOException;
import java.util.ArrayList;

/**
 * Created by John on 8/2/2015.
 */
public class TrackPlayerService extends Service implements MediaPlayer.OnPreparedListener
, MediaPlayer.OnCompletionListener {

    private final String LOG_TAG = TrackPlayerService.class.getSimpleName();

    private final IBinder mBinder = new LocalBinder();
    private MediaPlayer mMediaPlayer = null;
    private boolean mIsPlaying = false;

    private String mArtistQueryString;
    private LocalArtist mSelectedArtist;

    private ArrayList<LocalTrack> tracks;
    private Integer mCurrentTrackPosition;

    boolean mIsTrackLoaded;
    boolean mIsTrackPlaying;
    int mDuration;
    int mCurrentPosition;
    boolean mIsMediaPlayerPrepared;
    boolean mIsPaused;
    private boolean mJustLoadTrack = false;
    private boolean mCompletedPlaying = false;
    private boolean mIsPreparingMediaPlayer = false;

    public class LocalBinder extends Binder {
        public TrackPlayerService getService() {
            return TrackPlayerService.this;
        }

    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public void onPrepared(final MediaPlayer mediaPlayer) {

        if ( mJustLoadTrack ) {

            mIsPlaying = false;
            mIsTrackPlaying = false;
            mIsPaused = true;
            mIsMediaPlayerPrepared = true;
            mDuration = mediaPlayer.getDuration();
            mCompletedPlaying = false;

            mJustLoadTrack = false;

        } else {

            new Thread(new Runnable() {
                @Override
                public void run() {
                    mediaPlayer.start();
                }
            }, "Thread_MediaPlayer").start();

            mIsPlaying = true;
            mIsTrackPlaying = true;
            mIsPaused = false;
            mIsMediaPlayerPrepared = true;
            mDuration = mediaPlayer.getDuration();
            mCompletedPlaying = false;

            buildNotification("PLAY");

            Intent localIntent = new Intent(Constants.BROADCAST_ACTION_TRACK_UPDATE)
                    .putExtra(Constants.EXTENDED_DATA_TRACK_IS_PLAYING, true)
                    .putExtra(Constants.EXTENDED_DATA_TRACK_DURATION, mediaPlayer.getDuration())
                    .putExtra(Constants.EXTENDED_DATA_TRACK_CURRENT_POSITION, mediaPlayer.getCurrentPosition());

            LocalBroadcastManager.getInstance(this).sendBroadcast(localIntent);

        }

        mIsPreparingMediaPlayer = false;
    }

    public void requestUiUpdate() {

        if ( tracks != null ) {

            Intent localIntent = new Intent(Constants.BROADCAST_ACTION)
                    .putExtra(Constants.EXTENDED_DATA_STATUS, tracks.get(mCurrentTrackPosition))
                    .putExtra(Constants.EXTENDED_DATA_TRACK_CURRENT, mCurrentTrackPosition)
                    .putExtra(Constants.EXTENDED_DATA_STATUS_ARTIST_NAME, mSelectedArtist.getName());

            LocalBroadcastManager.getInstance(this).sendBroadcast(localIntent);

            int temporaryCurrentPosition;

            if ( !mIsPreparingMediaPlayer ) {
                // There is a known mediaPlayer bug where the currentPosition returned
                // is not accurate.  This is a temporary workaround for when the track
                // finishes playing.
                if ( mCompletedPlaying ) {
                    temporaryCurrentPosition = mMediaPlayer.getDuration();
                } else {
                    temporaryCurrentPosition = mMediaPlayer.getCurrentPosition();
                }

                localIntent = new Intent(Constants.BROADCAST_ACTION_TRACK_UPDATE)
                        .putExtra(Constants.EXTENDED_DATA_TRACK_IS_PLAYING, mMediaPlayer.isPlaying())
                        .putExtra(Constants.EXTENDED_DATA_TRACK_DURATION, mMediaPlayer.getDuration())
                        .putExtra(Constants.EXTENDED_DATA_TRACK_CURRENT_POSITION, temporaryCurrentPosition)
                        .putExtra(Constants.EXTENDED_DATA_STATUS, tracks.get(mCurrentTrackPosition))
                        .putExtra(Constants.EXTENDED_DATA_STATUS_ARTIST_NAME, mSelectedArtist.getName());;

                LocalBroadcastManager.getInstance(this).sendBroadcast(localIntent);

            }

        }

    }

    private boolean isNetworkConnected() {
        // Check for network connectivity
        ConnectivityManager cm =
                (ConnectivityManager)this.getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        boolean isConnected = activeNetwork != null &&
                activeNetwork.isConnectedOrConnecting();

        return isConnected;
    }

    public void previousTrack() {

        unloadTrack();

        if ( mCurrentTrackPosition > 0 ) {
            mCurrentTrackPosition--;
        }

        if ( !isTrackLoaded() ) {
            loadTrack(tracks.get(mCurrentTrackPosition).getPreview_url());

//            mJustLoadTrack = true;
            if ( !mIsPreparingMediaPlayer ) {
                if ( isNetworkConnected() ) {
                    mIsPreparingMediaPlayer = true;
                    mMediaPlayer.prepareAsync();
                } else {
                    Toast.makeText(this, getString(R.string.message_check_for_network_connectivity), Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, getString(R.string.message_load_track_please_be_patient), Toast.LENGTH_SHORT).show();
            };

        }

        buildNotification("PREV");

        Intent localIntent = new Intent(Constants.BROADCAST_ACTION)
                .putExtra(Constants.EXTENDED_DATA_STATUS, tracks.get(mCurrentTrackPosition))
                .putExtra(Constants.EXTENDED_DATA_TRACK_CURRENT, mCurrentTrackPosition)
                .putExtra(Constants.EXTENDED_DATA_STATUS_ARTIST_NAME, mSelectedArtist.getName());

        LocalBroadcastManager.getInstance(this).sendBroadcast(localIntent);
    }

    public void nextTrack() {

        unloadTrack();

        if ( mCurrentTrackPosition < (tracks.size() - 1) ) {
            mCurrentTrackPosition++;
        }

        if ( !isTrackLoaded() ) {
            loadTrack(tracks.get(mCurrentTrackPosition).getPreview_url());

//            mJustLoadTrack = true;
            if ( !mIsPreparingMediaPlayer ) {
                if ( isNetworkConnected() ) {
                    mIsPreparingMediaPlayer = true;
                    mMediaPlayer.prepareAsync();
                } else {
                    Toast.makeText(this, getString(R.string.message_check_for_network_connectivity), Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, getString(R.string.message_load_track_please_be_patient), Toast.LENGTH_SHORT).show();
            };

        }

        buildNotification("NEXT");

        Intent localIntent = new Intent(Constants.BROADCAST_ACTION)
                .putExtra(Constants.EXTENDED_DATA_STATUS, tracks.get(mCurrentTrackPosition))
                .putExtra(Constants.EXTENDED_DATA_TRACK_CURRENT, mCurrentTrackPosition)
                .putExtra(Constants.EXTENDED_DATA_STATUS_ARTIST_NAME, mSelectedArtist.getName());

        LocalBroadcastManager.getInstance(this).sendBroadcast(localIntent);
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        mIsPlaying = false;
        mIsPaused = true;
        mIsTrackPlaying = false;
        mCompletedPlaying = true;

        Intent localIntent = new Intent(Constants.BROADCAST_ACTION_TRACK_UPDATE)
                .putExtra(Constants.EXTENDED_DATA_TRACK_IS_PLAYING, mp.isPlaying())
                .putExtra(Constants.EXTENDED_DATA_TRACK_DURATION, mp.getDuration())
                .putExtra(Constants.EXTENDED_DATA_TRACK_CURRENT_POSITION, mp.getDuration());

        LocalBroadcastManager.getInstance(this).sendBroadcast(localIntent);

    }

    public void loadTracks(String artistQueryString, LocalArtist selectedArtist, ArrayList<LocalTrack> tracks) {
        try {
            this.mArtistQueryString = artistQueryString;
            this.mSelectedArtist = selectedArtist;
            this.tracks = tracks;
        }
        catch (Exception e) {
            Log.e(LOG_TAG, "Unable to loadTracks in Service");
        }
    }

    public void setCurrentTrackPosition(Integer mCurrentTrackPosition) {
        this.mCurrentTrackPosition = mCurrentTrackPosition;
    }

    public LocalTrack getCurrentTrack() {
        return tracks.get(mCurrentPosition);
    }

    public void loadTrack(String url) {

        try {
            mMediaPlayer.reset();
            mMediaPlayer.setDataSource(url);

            mIsTrackLoaded = true;
            mIsMediaPlayerPrepared = false;
            mIsTrackPlaying = false;
            mIsPaused = false;

            mDuration = 0;
            mCurrentPosition = 0;
        }
        catch (IOException e) {
            Log.e(LOG_TAG, "Unable to playSong");
        }

    }

    public void unloadTrack() {

        if ( mIsTrackPlaying ) {
            mMediaPlayer.stop();
            mIsTrackPlaying = false;
        }

        if ( mIsTrackLoaded ) {
            mMediaPlayer.reset();
            mIsTrackPlaying = false;
            mIsPaused = false;
            mIsTrackLoaded = false;
            mIsMediaPlayerPrepared = false;
            mCompletedPlaying = false;
        }

        Intent localIntent = new Intent(Constants.BROADCAST_ACTION_TRACK_UPDATE)
                .putExtra(Constants.EXTENDED_DATA_TRACK_IS_PLAYING, false)
                .putExtra(Constants.EXTENDED_DATA_TRACK_DURATION, 30000)
                .putExtra(Constants.EXTENDED_DATA_TRACK_CURRENT_POSITION, 0);

        LocalBroadcastManager.getInstance(this).sendBroadcast(localIntent);

    }

    public void playPauseTrack() {

        if ( !isTrackLoaded() ) {
            loadTrack(tracks.get(mCurrentTrackPosition).getPreview_url());
        }

        if ( mIsTrackLoaded && !mIsMediaPlayerPrepared && !mJustLoadTrack) {
            if ( !mIsPreparingMediaPlayer ) {
                if ( isNetworkConnected() ) {
                    mIsPreparingMediaPlayer = true;
                    mMediaPlayer.prepareAsync();
                } else {
                    Toast.makeText(this, getString(R.string.message_check_for_network_connectivity), Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, getString(R.string.message_load_track_please_be_patient), Toast.LENGTH_SHORT).show();
                return;
            };
        }

        if ( mIsMediaPlayerPrepared && mIsTrackPlaying && !mIsPaused ) {
            mMediaPlayer.pause();
            mIsTrackPlaying = false;
            mIsPaused = true;

            buildNotification("PAUSE");

            Intent localIntent = new Intent(Constants.BROADCAST_ACTION_TRACK_UPDATE)
                    .putExtra(Constants.EXTENDED_DATA_TRACK_IS_PLAYING, mMediaPlayer.isPlaying())
                    .putExtra(Constants.EXTENDED_DATA_TRACK_DURATION, mMediaPlayer.getDuration())
                    .putExtra(Constants.EXTENDED_DATA_TRACK_CURRENT_POSITION, mMediaPlayer.getCurrentPosition());

            LocalBroadcastManager.getInstance(this).sendBroadcast(localIntent);

        } else if (mIsMediaPlayerPrepared && !mIsTrackPlaying && mIsPaused) {
            mMediaPlayer.start();
            mIsTrackPlaying = true;
            mIsPaused = false;

            buildNotification("PLAY");

            Intent localIntent = new Intent(Constants.BROADCAST_ACTION_TRACK_UPDATE)
                    .putExtra(Constants.EXTENDED_DATA_TRACK_IS_PLAYING, mMediaPlayer.isPlaying())
                    .putExtra(Constants.EXTENDED_DATA_TRACK_DURATION, mMediaPlayer.getDuration())
                    .putExtra(Constants.EXTENDED_DATA_TRACK_CURRENT_POSITION, mMediaPlayer.getCurrentPosition());

            LocalBroadcastManager.getInstance(this).sendBroadcast(localIntent);

        }
    }

    private void buildNotification(String action) {

        // Optional component - Notifications
        LoadImageAsyncTaskParams loadImageAsyncTaskParams = new LoadImageAsyncTaskParams(mSelectedArtist.getThumbnailUrl(), action);
        new LoadImageAsyncTask().execute(loadImageAsyncTaskParams);

    }

    public boolean isTrackLoaded() {
        return mIsTrackLoaded;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        if ( intent != null ) {
            Bundle bundle = intent.getExtras();

            if ( bundle != null ) {
                String action = bundle.getString("action");

                switch ( action ) {
                    case "PLAYPAUSE":
                        playPauseTrack();
                        break;
                    case "PREVIOUS":
                        previousTrack();
                        break;
                    case "NEXT":
                        nextTrack();
                        break;
                }
            }

        }

        return START_STICKY;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        mMediaPlayer = new MediaPlayer();
        mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);

        mMediaPlayer.setOnPreparedListener(this);
        mMediaPlayer.setOnCompletionListener(this);
        mMediaPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
            @Override
            public boolean onError(MediaPlayer mp, int what, int extra) {
                Log.e(LOG_TAG, "onError called in mediaPlayer");
                Toast.makeText(getApplicationContext(), getString(R.string.message_mediaPlayer_onError), Toast.LENGTH_LONG).show();
                return true;
            }
        });

        mIsTrackLoaded = false;
        mIsTrackPlaying = false;
        mIsPaused = false;
        mIsMediaPlayerPrepared = false;

        mDuration = 0;
        mCurrentPosition = 0;

    }

    public void seekTo(int newPosition) {
        mCompletedPlaying = false;
        mMediaPlayer.seekTo(newPosition);
    }

    private static class LoadImageAsyncTaskParams {
        String bitmapUrl;
        String action;

        LoadImageAsyncTaskParams(String bitmapUrl, String action) {
            this.bitmapUrl = bitmapUrl;
            this.action = action;
        }
    }

    private class LoadImageAsyncTask extends AsyncTask<LoadImageAsyncTaskParams, Void, Bitmap> {

        String bitmapUrl;
        String action;

        @Override
        protected Bitmap doInBackground(LoadImageAsyncTaskParams... params) {
            bitmapUrl = params[0].bitmapUrl;
            action = params[0].action;

            Bitmap b = null;

            try {
                b = Picasso.with(getApplicationContext()).load(bitmapUrl).get();
            } catch (Exception e) {
                Log.e("TrackPlayerService", "Unable to load large bitmap");
            }

            return b;
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {

            Intent prevIntent = new Intent(getApplicationContext(), TrackPlayerService.class);
            prevIntent.putExtra("action", "PREVIOUS");

            Intent playPauseIntent = new Intent(getApplicationContext(), TrackPlayerService.class);
            playPauseIntent.putExtra("action", "PLAYPAUSE");

            Intent nextIntent = new Intent(getApplicationContext(), TrackPlayerService.class);
            nextIntent.putExtra("action", "NEXT");

            PendingIntent piPrevIntent = PendingIntent.getService(getApplicationContext(), 0, prevIntent, 0);
            PendingIntent piPlayPauseIntent = PendingIntent.getService(getApplicationContext(), 1, playPauseIntent, 0);
            PendingIntent piNextIntent = PendingIntent.getService(getApplicationContext(), 2, nextIntent, 0);

            SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
            Boolean prefShowonLockScreen = sharedPref.getBoolean("pref_show_on_lock_screen", false);

            int visibilityLevel;

            if ( prefShowonLockScreen ) {
                visibilityLevel = Notification.VISIBILITY_PUBLIC;
            } else {
                visibilityLevel = Notification.VISIBILITY_PRIVATE;
            }

            String iconWording;
            int iconId;

            switch ( action ) {
                case "PAUSE":
                    iconWording = "PLAY";
                    iconId = android.R.drawable.ic_media_play;
                    break;

                case "PLAY":
                    iconWording = "PAUSE";
                    iconId = android.R.drawable.ic_media_pause;
                    break;

                default:
                    iconWording = "ZZZ";
                    iconId = android.R.drawable.ic_media_play;
                    break;
            }

            Intent startActivityIntent = new Intent(getApplicationContext(), MainActivity.class)
                    .putExtra("artistQueryString", mArtistQueryString)
                    .putExtra("artist", mSelectedArtist)
                    .putExtra("track", tracks.get(mCurrentTrackPosition));
            PendingIntent pendingStartActivityIntent = PendingIntent.getActivity(getApplicationContext(), 101, startActivityIntent, PendingIntent.FLAG_UPDATE_CURRENT);

            Notification.Builder mPublicBuilder = null;
            Notification.Builder mPrivateBuilder = null;

            mPublicBuilder =
                    new Notification.Builder(getApplicationContext())
                            .setSmallIcon(android.R.drawable.ic_media_play)
                            .setLargeIcon(bitmap)
                            .setContentTitle("\"" + tracks.get(mCurrentTrackPosition).getTrackName() + "\"")
                            .setContentText(mSelectedArtist.getName())
                            .setVisibility(Notification.VISIBILITY_PUBLIC)
                            .setContentTitle(tracks.get(mCurrentTrackPosition).getTrackName())
                            .setStyle(new Notification.MediaStyle())
                            .setContentIntent(pendingStartActivityIntent);

            Notification publicNotification = mPublicBuilder.build();

            mPrivateBuilder =
                    new Notification.Builder(getApplicationContext())
                            .setSmallIcon(android.R.drawable.ic_media_play)
                            .setLargeIcon(bitmap)
                            .setContentTitle("\"" + tracks.get(mCurrentTrackPosition).getTrackName() + "\"")
                            .setContentText(mSelectedArtist.getName())
                            .setVisibility(visibilityLevel)
                            .addAction(android.R.drawable.ic_media_previous, "prev", piPrevIntent)
                            .addAction(iconId, iconWording, piPlayPauseIntent)
                            .addAction(android.R.drawable.ic_media_next, "next", piNextIntent)
                            .setStyle(new Notification.MediaStyle()
                                    .setShowActionsInCompactView(0, 1, 2))
                            .setContentIntent(pendingStartActivityIntent)
                            .setPublicVersion(publicNotification);

            Notification n = mPrivateBuilder.build();

            startForeground(888, n);

        }
    }

}
