package com.example.android.project1;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;

import com.example.android.project1.models.LocalArtist;
import com.example.android.project1.models.LocalImage;
import com.example.android.project1.models.LocalTrack;
import com.example.android.project1.service.TrackPlayerService;

import java.util.ArrayList;

public class EventActivity extends ActionBarActivity
        implements EventActivityFragment.OnEventSelectedListener,
        TrackActivityFragment.OnTrackSelectedListener, TrackPlayerActivityFragment.TrackPlayerActivityListener {

    private boolean mDualPane;

    private TrackPlayerService mTrackPlayerService;
    private Boolean mBound = false;

    private LocalArtist mSelectedArtist;
    private String mArtistQueryString = "";
    ServiceStatusReceiver mServiceStatusReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event);

        if (findViewById(R.id.track_list_container) != null) {
            mDualPane = true;
        } else {
            mDualPane = false;
        }

        // Start up service
        // bind - so we can call its methods
        // startService - so it stays around indefinitely
        Intent intent = new Intent(this, TrackPlayerService.class);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);

        startService(intent);

        /*Intent*/ intent = getIntent();

        if ( intent != null && intent.hasExtra("event_info") ) {
            String event_info = intent.getStringExtra("event_info");

            FragmentManager fragmentManager = getSupportFragmentManager();
            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();

            EventActivityFragment fragment = new EventActivityFragment();
            fragment.setValues(event_info);
            fragmentTransaction.add(R.id.events_container, fragment);

            fragmentTransaction.commit();

        }

    }

    @Override
    protected void onResume() {
        super.onResume();

        // Set-up receiver
        IntentFilter mStatusIntentFilter = new IntentFilter(
                Constants.BROADCAST_ACTION);

        mServiceStatusReceiver = new ServiceStatusReceiver();

        LocalBroadcastManager.getInstance(this).registerReceiver(
                mServiceStatusReceiver,
                mStatusIntentFilter);

        mStatusIntentFilter = new IntentFilter(
                Constants.BROADCAST_ACTION_TRACK_UPDATE);

        LocalBroadcastManager.getInstance(this).registerReceiver(
                mServiceStatusReceiver,
                mStatusIntentFilter);

    }

    @Override
    protected void onPause() {
        super.onPause();

        if ( mServiceStatusReceiver != null ) {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(mServiceStatusReceiver);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();

        if (mBound) {
            unbindService(mConnection);
            mBound = false;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_event, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void onEventSelected(String artist_id, String artist_name, String artist_image) {

        TrackActivityFragment tracksActivityFragment;

        FragmentManager fm = getSupportFragmentManager();
        tracksActivityFragment = (TrackActivityFragment) fm.findFragmentByTag("track");

        FragmentTransaction fragmentTransaction = fm.beginTransaction();

        if ( tracksActivityFragment == null ) {
            tracksActivityFragment = new TrackActivityFragment();
            fragmentTransaction.add(R.id.track_list_container, tracksActivityFragment, "track");
            fragmentTransaction.commit();
        }

        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        String prefCountryCode = sharedPref.getString("pref_country_code", "US");

        if ( artist_id != null && artist_name != null ) {

            LocalImage localImage = new LocalImage(artist_image, 200, 200);

            ArrayList<LocalImage> localImages = new ArrayList<>();
            localImages.add(localImage);
            mSelectedArtist = new LocalArtist(artist_id, artist_name, localImages);

            tracksActivityFragment.setValues(artist_id, artist_name, prefCountryCode);

        }

    }

    public void OnTrackSelected(ArrayList<LocalTrack> tracks, Integer position) {

        mTrackPlayerService.loadTracks(mArtistQueryString, mSelectedArtist, tracks);
        mTrackPlayerService.setCurrentTrackPosition(position);
        mTrackPlayerService.unloadTrack();
        mTrackPlayerService.playPauseTrack();

        FragmentManager fragmentManager = getSupportFragmentManager();
        TrackPlayerActivityFragment trackPlayerActivityFragment = (TrackPlayerActivityFragment) fragmentManager.findFragmentByTag("dialog");

        if ( trackPlayerActivityFragment == null ) {
            trackPlayerActivityFragment = new TrackPlayerActivityFragment();
        }

        trackPlayerActivityFragment.setValues(mSelectedArtist.getName(), tracks, position);

        if( mDualPane ) {
            trackPlayerActivityFragment.show(fragmentManager, "dialog");
        }
    }

    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            TrackPlayerService.LocalBinder binder = (TrackPlayerService.LocalBinder) service;
            mTrackPlayerService = binder.getService();
            mBound = true;

            mTrackPlayerService.requestUiUpdate();

        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mBound = false;
        }
    };

    private class ServiceStatusReceiver extends BroadcastReceiver {

        private ServiceStatusReceiver() {

        }

        @Override
        public void onReceive(Context context, Intent intent) {
            FragmentManager fragmentManager = getSupportFragmentManager();
            TrackPlayerActivityFragment trackPlayerActivityFragment = (TrackPlayerActivityFragment) fragmentManager.findFragmentByTag("dialog");

            String action = intent.getAction();
            int trackDuration;

            switch ( action ) {
                case Constants.BROADCAST_ACTION:

                    String artistName = intent.getStringExtra(Constants.EXTENDED_DATA_STATUS_ARTIST_NAME);
                    LocalTrack localTrack = (LocalTrack) intent.getParcelableExtra(Constants.EXTENDED_DATA_STATUS);

                    // If there is no trackPlayerActivityFragment -> nothing to update
                    if ( trackPlayerActivityFragment == null ) {
                        return;
                    }

                    trackPlayerActivityFragment.updateViews(artistName, localTrack);

                    TrackActivityFragment tracksActivityFragment;

                    tracksActivityFragment = (TrackActivityFragment) fragmentManager.findFragmentByTag("track");

                    if ( tracksActivityFragment != null ) {
                        int currentTrack = intent.getIntExtra(Constants.EXTENDED_DATA_TRACK_CURRENT,0);
                        tracksActivityFragment.setTrackPosition(currentTrack);
                    }

                    break;

                case Constants.BROADCAST_ACTION_TRACK_UPDATE:

                    boolean isPlaying = intent.getBooleanExtra(Constants.EXTENDED_DATA_TRACK_IS_PLAYING, false);
                    trackDuration = intent.getIntExtra(Constants.EXTENDED_DATA_TRACK_DURATION, 30);
                    int currentTrackPosition = intent.getIntExtra(Constants.EXTENDED_DATA_TRACK_CURRENT_POSITION, 0);

                    // If there is no trackPlayerActivityFragment -> nothing to update
                    if ( trackPlayerActivityFragment == null ) {
                        return;
                    }

                    trackPlayerActivityFragment.updateSeekbar(isPlaying, trackDuration, currentTrackPosition);

                    break;
            }
        }
    }

    @Override
    public void onClickNextTrack() {
        mTrackPlayerService.nextTrack();
    }

    @Override
    public void onClickPreviousTrack() {
        mTrackPlayerService.previousTrack();
    }

    @Override
    public void onClickPlayPauseTrack() {
        mTrackPlayerService.playPauseTrack();
    }

    @Override
    public void onRequestUiUpdate() {
        mTrackPlayerService.requestUiUpdate();
    }

    @Override
    public void setCurrentTrackPosition(int currentTrackPosition) {
        mTrackPlayerService.seekTo(currentTrackPosition);
    }

    @Override
    public void showActionBarPlayingButton(boolean showButton) {

    }

}
