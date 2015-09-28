package com.example.android.SpotifyStreamerWithBeaconSupport;

import android.app.Activity;
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
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.ShareActionProvider;
import android.view.Menu;
import android.view.MenuItem;

import com.example.android.SpotifyStreamerWithBeaconSupport.models.LocalArtist;
import com.example.android.SpotifyStreamerWithBeaconSupport.models.LocalTrack;
import com.example.android.SpotifyStreamerWithBeaconSupport.service.TrackPlayerService;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;


public class TrackActivity extends ActionBarActivity implements TrackActivityFragment.OnTrackSelectedListener
        , TrackPlayerActivityFragment.TrackPlayerActivityListener {

    private String mArtistName;
    private LocalArtist mSelectedArtist;
    private String mArtistQueryString = "";

    ServiceStatusReceiver mServiceStatusReceiver;
    private TrackPlayerService mTrackPlayerService;
    private Boolean mBound = false;
    private MenuItem mIsPlayingMenuItem;
    private Boolean mShowNowPlayingButton = false;

    private ShareActionProvider mShareActionProvider;
    private MenuItem mShareMenuItem;

    private void setShareIntent(Intent shareIntent) {
        if ( mShareActionProvider != null ) {
            mShareActionProvider.setShareIntent(shareIntent);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putParcelable("mSelectedArtist", mSelectedArtist);
        outState.putBoolean("showButton", mShowNowPlayingButton);

        TrackActivityFragment tracksActivityFragment;

        FragmentManager fm = getSupportFragmentManager();
        tracksActivityFragment = (TrackActivityFragment) fm.findFragmentByTag("track");

        if ( tracksActivityFragment != null ) {
            fm.putFragment(outState, "tracksActivityFragment", tracksActivityFragment);
        }

        TrackPlayerActivityFragment trackPlayerActivityFragment;

        trackPlayerActivityFragment = (TrackPlayerActivityFragment) fm.findFragmentByTag("dialog");

        if ( trackPlayerActivityFragment != null ) {
            fm.putFragment(outState, "trackPlayerActivityFragment", trackPlayerActivityFragment);
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
    protected void onPause() {
        super.onPause();

        if ( mServiceStatusReceiver != null ) {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(mServiceStatusReceiver);
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

        final ActionBar actionBar = this.getSupportActionBar();
        if ( actionBar != null && mSelectedArtist != null ) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    actionBar.setSubtitle(mSelectedArtist.getName());
                }
            });
        }

    }

    @Override
    public void OnTrackSelected(ArrayList<LocalTrack> tracks, Integer position) {

        mTrackPlayerService.loadTracks(mArtistQueryString, mSelectedArtist, tracks);
        mTrackPlayerService.setCurrentTrackPosition(position);
        mTrackPlayerService.unloadTrack();
        mTrackPlayerService.playPauseTrack();

//        Toast.makeText(this, "(TrackActivity) Track selected: " + localTrack.trackName.toString(), Toast.LENGTH_SHORT).show();

        TrackPlayerActivityFragment trackPlayerActivityFragment = new TrackPlayerActivityFragment();

//        trackPlayerActivityFragment.setValues(mArtistName, localTrack);
        trackPlayerActivityFragment.setValues(mSelectedArtist.getName(), tracks, position);

        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        fragmentTransaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
        fragmentTransaction.replace(R.id.track_list_container, trackPlayerActivityFragment, "dialog")
                .addToBackStack(null)
                .commit();

    }

    @Override
    public void showActionBarPlayingButton(boolean showButton) {

        mShowNowPlayingButton = showButton;

        final boolean isButtonVisible = showButton;

        final ActionBar actionBar = this.getSupportActionBar();
        if ( actionBar != null && mSelectedArtist != null ) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if ( mIsPlayingMenuItem != null && actionBar != null) {
                        mIsPlayingMenuItem.setVisible(isButtonVisible);
                        mShareMenuItem.setVisible(isButtonVisible);
                        actionBar.invalidateOptionsMenu();
                    }
                }
            });
        }

    }

    private TrackActivityFragment tracksActivityFragment;

    private String id;
    private String artist;

    public TrackActivity() {
        super();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_track);

        final ActionBar actionBar = this.getSupportActionBar();

        getSupportFragmentManager().addOnBackStackChangedListener(new FragmentManager.OnBackStackChangedListener() {
            @Override
            public void onBackStackChanged() {

                if ( actionBar != null && mSelectedArtist != null ) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            actionBar.setSubtitle(mSelectedArtist.getName());
                        }
                    });
                }

            }
        });

        // Start up service
        // bind - so we can call its methods
        // startService - so it stays around indefinitely
        Intent serviIntent = new Intent(this, TrackPlayerService.class);
        bindService(serviIntent, mConnection, Context.BIND_AUTO_CREATE);

        startService(serviIntent);

        if ( savedInstanceState != null ) {

            mSelectedArtist = (LocalArtist) savedInstanceState.getParcelable("mSelectedArtist");
            mShowNowPlayingButton = savedInstanceState.getBoolean("showButton");

            FragmentManager fm = getSupportFragmentManager();

            TrackActivityFragment tracksActivityFragment;

            tracksActivityFragment = (TrackActivityFragment) fm.getFragment(savedInstanceState, "tracksActivityFragment");

            if ( tracksActivityFragment != null ) {
                FragmentTransaction fragmentTransaction = fm.beginTransaction();
                fragmentTransaction.replace(R.id.track_list_container, tracksActivityFragment, "track");
                fragmentTransaction.commit();
            }

            TrackPlayerActivityFragment trackPlayerActivityFragment;

            trackPlayerActivityFragment = (TrackPlayerActivityFragment) fm.getFragment(savedInstanceState, "trackPlayerActivityFragment");

            if ( trackPlayerActivityFragment != null ) {
                FragmentTransaction fragmentTransaction = fm.beginTransaction();
                fragmentTransaction.replace(R.id.track_list_container, trackPlayerActivityFragment, "dialog");
                fragmentTransaction.commit();
            }

        }

        FragmentManager fm = getSupportFragmentManager();
        tracksActivityFragment = (TrackActivityFragment) fm.findFragmentByTag("track");

        FragmentTransaction fragmentTransaction = fm.beginTransaction();

        if ( tracksActivityFragment == null ) {
            tracksActivityFragment = new TrackActivityFragment();
            fragmentTransaction.add(R.id.track_list_container, tracksActivityFragment, "track");
            fragmentTransaction.commit();
        }

        Intent intent = getIntent();

        if ( intent != null && savedInstanceState == null) {
            String id = intent.getStringExtra("id");
            mSelectedArtist = intent.getParcelableExtra("artist");
            String imageUrl = intent.getStringExtra("image");
            mArtistQueryString = intent.getStringExtra("artistQueryString");
            mShowNowPlayingButton = intent.getBooleanExtra("showButton", false);

            if ( mSelectedArtist.getId() != null && mSelectedArtist.getName() != null ) {

                SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
                String prefCountryCode = sharedPref.getString("pref_country_code", "US");

                tracksActivityFragment.setValues(mSelectedArtist.getId(), mSelectedArtist.getName(), prefCountryCode);
                TrackFrameLayout trackFrameLayout = (TrackFrameLayout)findViewById(R.id.track_list_container);

                if ( !imageUrl.equals("") ) {
                    Picasso.with(this).load(imageUrl)
                            .into(trackFrameLayout);
                }

            }

        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_track, menu);
        mIsPlayingMenuItem = menu.findItem(R.id.action_play);
        mIsPlayingMenuItem.setVisible(mShowNowPlayingButton);

        // Optional Component - Sharing Functionality
        mShareMenuItem = menu.findItem(R.id.menu_item_share);
        mShareActionProvider = (ShareActionProvider) MenuItemCompat.getActionProvider(mShareMenuItem);

        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_SEND);
        intent.putExtra(Intent.EXTRA_TEXT, getString(R.string.share_message_temp));
        intent.setType("text/plain");
        setShareIntent(intent);

        mShareMenuItem.setVisible(mShowNowPlayingButton);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        switch ( id ) {
            case android.R.id.home:
                int count = getSupportFragmentManager().getBackStackEntryCount();
                if ( getSupportFragmentManager().getBackStackEntryCount() > 0 ) {
                    getSupportFragmentManager().popBackStack();
                    return true;
                } else {
                    Intent intent = new Intent();
                    boolean showButton = mIsPlayingMenuItem.isVisible();
                    intent.putExtra("showButton", showButton);
                    setResult(Activity.RESULT_OK, intent);
                    finish();
                }
                break;

            case R.id.action_play:
                FragmentManager fragmentManager = getSupportFragmentManager();
                TrackPlayerActivityFragment trackPlayerActivityFragment = new TrackPlayerActivityFragment();

                LocalTrack track = mTrackPlayerService.getCurrentTrack();

                ArrayList<LocalTrack> tracks = new ArrayList<>();
                tracks.add(track);

                trackPlayerActivityFragment.setValues(mSelectedArtist.getName(), tracks, 0);

                FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
                fragmentTransaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
                fragmentTransaction.replace(R.id.track_list_container, trackPlayerActivityFragment, "dialog")
                        .addToBackStack(null)
                        .commit();

                mTrackPlayerService.requestUiUpdate();

                mIsPlayingMenuItem.setVisible(false);
                mShareMenuItem.setVisible(false);

                return true;

            case R.id.action_settings:

                Intent intent = new Intent(this, SettingsActivity.class);
                startActivity(intent);

                return true;
        }

        return super.onOptionsItemSelected(item);
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

    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            TrackPlayerService.LocalBinder binder = (TrackPlayerService.LocalBinder) service;
            mTrackPlayerService = binder.getService();
            mBound = true;

            mTrackPlayerService.requestUiUpdate();

//            mTrackPlayerService.loadTracks(tracks);
//            mTrackPlayerService.setCurrentTrackPosition(currentTrackPosition);

//            if ( mTrackPlayerService.isTrackLoaded() ) {
//                final SeekBar seekBar = (SeekBar) getView().findViewById(R.id.seekBar);
//                seekBar.setMax(mTrackPlayerService.getDuration());
//
//                RunnableProgress r = new RunnableProgress(seekBar);
//
//                mMoveSeekBarThread = new Thread(r, "Thread_mMoveSeekBarThread");
//                mMoveSeekBarThread.start();
//            }
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
//            Toast.makeText(context, "move next - Track image changed", Toast.LENGTH_SHORT).show();
            FragmentManager fragmentManager = getSupportFragmentManager();
            TrackPlayerActivityFragment trackPlayerActivityFragment = (TrackPlayerActivityFragment) fragmentManager.findFragmentByTag("dialog");

            String action = intent.getAction();
            int trackDuration;

            switch ( action ) {
                case Constants.BROADCAST_ACTION:

                    String artistName = intent.getStringExtra(Constants.EXTENDED_DATA_STATUS_ARTIST_NAME);
                    LocalTrack localTrack = (LocalTrack) intent.getParcelableExtra(Constants.EXTENDED_DATA_STATUS);

                    // Setup shareintent
                    Intent shareIntent = new Intent();
                    shareIntent.setAction(Intent.ACTION_SEND);
                    shareIntent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.share_subject));

                    String shareMessage = String.format(getString(R.string.share_message), localTrack.getTrackName(), artistName, localTrack.getPreview_url());
                    shareIntent.putExtra(Intent.EXTRA_TEXT, shareMessage);

                    shareIntent.setType("text/plain");
                    setShareIntent(shareIntent);

                    // If there no trackPlayerActivytFragment -> nothing to update
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

//                case Constants.BROADCAST_ACTION_TRACK_STARTED:
//
//                    trackDuration = intent.getIntExtra(Constants.EXTENDED_DATA_TRACK_DURATION, 30);
//                    trackPlayerActivityFragment.updateSeekbar(trackDuration);
//
//                    break;

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

}
