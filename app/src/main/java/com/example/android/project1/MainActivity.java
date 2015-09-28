package com.example.android.project1;

import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.ShareActionProvider;
import android.util.Base64;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.example.android.project1.models.LocalArtist;
import com.example.android.project1.models.LocalTrack;
import com.example.android.project1.service.TrackPlayerService;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.ErrorDialogFragment;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.nearby.Nearby;
import com.google.android.gms.nearby.messages.Message;
import com.google.android.gms.nearby.messages.MessageFilter;
import com.google.android.gms.nearby.messages.MessageListener;
import com.google.android.gms.nearby.messages.Strategy;
import com.squareup.picasso.Picasso;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;


public class MainActivity extends ActionBarActivity
        implements MainActivityFragment.OnArtistSelectedListener, TrackActivityFragment.OnTrackSelectedListener
        , TrackPlayerActivityFragment.TrackPlayerActivityListener
, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener{

    GoogleApiClient mGoogleApiClient;

    private final String LOG_TAG = MainActivity.class.getSimpleName();

    private boolean mDualPane;
    private LocalArtist mSelectedArtist;
    private String mArtistName;
    private String mArtistQueryString = "";
    ServiceStatusReceiver mServiceStatusReceiver;

    private TrackPlayerService mTrackPlayerService;
    private Boolean mBound = false;
    private Boolean mNoNetworkConnectivity = false;
    private MenuItem mIsPlayingMenuItem;
    private Boolean mShowNowPlayingButton = false;

    private ShareActionProvider mShareActionProvider;
    private MenuItem mShareMenuItem;

    // Google Play Services
    // Request code to use when launching the resolution activity
    private static final int REQUEST_RESOLVE_ERROR = 1001;
    // Unique tag for the error dialog fragment
    private static final String DIALOG_ERROR = "dialog_error";
    // Bool to track whether the app is already resolving an error
    private boolean mResolvingError = false;

    @Override
    public void onConnected(Bundle bundle) {
        Log.d(LOG_TAG, "onConnected");
        publishAndSubscribe();
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.d(LOG_TAG, "onConnectionSuspended");
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        if (mResolvingError) {
            // Already attempting to resolve an error.
            return;
        } else if (connectionResult.hasResolution()) {
            try {
                mResolvingError = true;
                connectionResult.startResolutionForResult(this, REQUEST_RESOLVE_ERROR);
            } catch (IntentSender.SendIntentException e) {
                // There was an error with the resolution intent. Try again.
                mGoogleApiClient.connect();
            }
        } else {
            // Show dialog using GoogleApiAvailability.getErrorDialog()
            showErrorDialog(connectionResult.getErrorCode());
            mResolvingError = true;
        }
    }

    /* Creates a dialog for an error message */
    private void showErrorDialog(int errorCode) {
        // Create a fragment for the error dialog
        ErrorDialogFragment dialogFragment = new ErrorDialogFragment();
        // Pass the error that should be displayed
        Bundle args = new Bundle();
        args.putInt(DIALOG_ERROR, errorCode);
        dialogFragment.setArguments(args);
        dialogFragment.show(getFragmentManager(), "errordialog");
    }

    private void publishAndSubscribe() {

        MessageFilter.Builder messageFilterBuilder = new MessageFilter.Builder()
                .includeAllMyTypes();

        Nearby.Messages.subscribe(mGoogleApiClient, messageListener, Strategy.BLE_ONLY,
                messageFilterBuilder.build())
                .setResultCallback(new ErrorCheckingCallback("subscribe()"));

    }

    /**
     * A simple ResultCallback that displays a toast when errors occur.
     * It also displays the Nearby opt-in dialog when necessary.
     */
    private class ErrorCheckingCallback implements ResultCallback<Status> {
        private final String method;
        private final Runnable runOnSuccess;

        private ErrorCheckingCallback(String method) {
            this(method, null);
        }

        private ErrorCheckingCallback(String method, @Nullable Runnable runOnSuccess) {
            this.method = method;
            this.runOnSuccess = runOnSuccess;
        }

        @Override
        public void onResult(@NonNull Status status) {
            if (status.isSuccess()) {
                Log.i(LOG_TAG, method + " succeeded.");
                if (runOnSuccess != null) {
                    runOnSuccess.run();
                }
            } else {
                // Currently, the only resolvable error is that the device is not opted
                // in to Nearby. Starting the resolution displays an opt-in dialog.
                if (status.hasResolution()) {
                    if (!mResolvingError) {
                        try {
                            status.startResolutionForResult(MainActivity.this,
                                    REQUEST_RESOLVE_ERROR);
                            mResolvingError = true;
                        } catch (IntentSender.SendIntentException e) {
                            showToastAndLog(Log.ERROR, method + " failed with exception: " + e);
                        }
                    } else {
                        // This will be encountered on initial startup because we do
                        // both publish and subscribe together.  So having a toast while
                        // resolving dialog is in progress is confusing, so just log it.
                        Log.i(LOG_TAG, method + " failed with status: " + status
                                + " while resolving error.");
                    }
                } else {
                    showToastAndLog(Log.ERROR, method + " failed with : " + status
                            + " resolving error: " + mResolvingError);
                }
            }
        }
    }

    public void showToastAndLog(int errorLevel, String msg) {
        switch ( errorLevel ) {
            case Log.ERROR:
                Log.e(LOG_TAG, msg);
            default:
                Log.d(LOG_TAG, msg);
        }
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    // Create an instance of MessageListener
    MessageListener messageListener = new MessageListener() {
        // Called each time a new message is discovered nearby.
        @Override
        public void onFound(Message message) {
            Log.i(LOG_TAG, "Found message: " + message);

            String nearbyMessageNamespace = message.getNamespace();
            String nearbyMessageType = message.getType();
            String nearbyMessageString = new String(message.getContent());
            byte[] nearbyMessageByte = message.getContent();

            Log.i(LOG_TAG, "Message string: " + nearbyMessageString);
            Log.i(LOG_TAG, "Message string (decoded): " + new String(Base64.decode(message.getContent(), Base64.DEFAULT)));
//            Log.i(TAG, "Message string (nearbyMessageByte): " + Base64.decode(nearbyMessageByte, Base64.DEFAULT));
//            Log.i(TAG, "Message string (new String nearbyMessageByte): " + new String(Base64.decode(nearbyMessageByte, Base64.DEFAULT)));
            Log.i(LOG_TAG, "Message namespaced type: " + nearbyMessageNamespace +
                    "/" + nearbyMessageType);

            if ( nearbyMessageType.equals("event_id") ) {
                String event_id = new String(Base64.decode(message.getContent(), Base64.DEFAULT));
                Log.d(LOG_TAG, "Event id = " + event_id);
                new BuildBeaconNotificationAsyncTask().execute(event_id);
            }

        }

        // Called when a message is no longer nearby.
        @Override
        public void onLost(Message message) {
            Log.i(LOG_TAG, "Lost message: " + message);

            String nearbyMessageNamespace = message.getNamespace();
            String nearbyMessageType = message.getType();
            String nearbyMessageString = new String(message.getContent());

            Log.i(LOG_TAG, "Message string: " + nearbyMessageString);
            Log.i(LOG_TAG, "Message string (decoded): " + Base64.decode(nearbyMessageString, Base64.DEFAULT));
            Log.i(LOG_TAG, "Message namespaced type: " + nearbyMessageNamespace +
                    "/" + nearbyMessageType);
        }
    };

    @Override
    public void onStart() {
        super.onStart();

        if (!mGoogleApiClient.isConnected()) {
            mGoogleApiClient.connect();
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

        // Display alert dialog about needing network connectivity
        if ( mNoNetworkConnectivity ) {

            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
            alertDialogBuilder.setTitle(getString(R.string.network_missing_title));
            alertDialogBuilder.setMessage(getString(R.string.network_missing_message));

            alertDialogBuilder.setPositiveButton(getString(R.string.button_message_positive), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {

                }
            });

            AlertDialog dialog = alertDialogBuilder.create();
            dialog.show();

            // TODO Shutdown app because network connectivity is missing

        }

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
    public void showActionBarPlayingButton(boolean showButton) {

        final boolean isButtonVisible = showButton;

        final ActionBar actionBar = this.getSupportActionBar();
        if (actionBar != null && mIsPlayingMenuItem != null) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (mIsPlayingMenuItem != null && actionBar != null) {
                        mIsPlayingMenuItem.setVisible(isButtonVisible);
                        mShareMenuItem.setVisible(isButtonVisible);
                        actionBar.invalidateOptionsMenu();
                    }
                }
            });
        }

    }

    @Override
    public void onArtistSearchChanged() {

        if (mDualPane) {
            ActionBar actionBar = this.getSupportActionBar();
            if (actionBar != null) {
                actionBar.setSubtitle("");
            }

            TrackActivityFragment tracksActivityFragment;

            FragmentManager fm = getSupportFragmentManager();
            tracksActivityFragment = (TrackActivityFragment) fm.findFragmentByTag("track");

            FragmentTransaction fragmentTransaction = fm.beginTransaction();

            if (tracksActivityFragment != null) {
                fragmentTransaction.remove(tracksActivityFragment);
                fragmentTransaction.commit();
            }

            TrackFrameLayout trackFrameLayout = (TrackFrameLayout) findViewById(R.id.track_list_container);

            // TODO Need to use correct color
//            trackFrameLayout.setBackgroundColor(Color.LTGRAY);

            Picasso.with(this).load(R.drawable.no_album)
                    .into(trackFrameLayout);
        }

    }

    @Override
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Check for network connectivity
        ConnectivityManager cm =
                (ConnectivityManager)this.getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        boolean isConnected = activeNetwork != null &&
                activeNetwork.isConnectedOrConnecting();

        if ( !isConnected ) {

            mNoNetworkConnectivity = true;

        } else {

            mNoNetworkConnectivity = false;

            if (findViewById(R.id.track_list_container) != null) {
                mDualPane = true;
            } else {
                mDualPane = false;
            }

            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addApi(Nearby.MESSAGES_API)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .build();

            mGoogleApiClient.connect();

            if (savedInstanceState != null) {

                if (mDualPane) {

                    mArtistQueryString = savedInstanceState.getString("artistQueryString");
                    mSelectedArtist = (LocalArtist) savedInstanceState.getParcelable("mSelectedArtist");

                }

                mShowNowPlayingButton = savedInstanceState.getBoolean("showNowPlayingActionBarButton", false);

            }

            // Start up service
            // bind - so we can call its methods
            // startService - so it stays around indefinitely
            Intent intent = new Intent(this, TrackPlayerService.class);
            bindService(intent, mConnection, Context.BIND_AUTO_CREATE);

            startService(intent);

            intent = getIntent();

            if (intent != null & intent.hasExtra("artist") & savedInstanceState == null) {

                String artistQueryString = intent.getStringExtra("artistQueryString");
                LocalArtist artist = (LocalArtist) intent.getParcelableExtra("artist");
                LocalTrack track = (LocalTrack) intent.getParcelableExtra("track");

                MainActivityFragment mainActivityFragment;
                mainActivityFragment = (MainActivityFragment) getSupportFragmentManager().findFragmentById(R.id.fragment);
                mainActivityFragment.setValues(artistQueryString, artist.getId());

                onArtistSelected(artistQueryString, artist);

                FragmentManager fragmentManager = getSupportFragmentManager();
                TrackPlayerActivityFragment trackPlayerActivityFragment = new TrackPlayerActivityFragment();

                // TODO fix this - unneeded parameters(?)
                ArrayList<LocalTrack> tracks = new ArrayList<>();
                tracks.add(track);
                trackPlayerActivityFragment.setValues(mArtistName, tracks, 0);

                trackPlayerActivityFragment.show(fragmentManager, "dialog");

            }

        }
    }

    public void onArtistSelected(String queryString, LocalArtist localArtist) {

        mArtistQueryString = queryString;
        mSelectedArtist = localArtist;

        if (mDualPane) {

            ActionBar actionBar = this.getSupportActionBar();
            if ( actionBar != null ) {
                actionBar.setSubtitle(localArtist.getName());
            }

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

            if ( mSelectedArtist.getId() != null && mSelectedArtist.getName() != null ) {
                tracksActivityFragment.setValues(mSelectedArtist.getId(), mSelectedArtist.getName(), prefCountryCode);
                TrackFrameLayout trackFrameLayout = (TrackFrameLayout) findViewById(R.id.track_list_container);

                if (localArtist.getArtistImages().size() > 0) {
                    String imageUrl = localArtist.getThumbnailUrl();
                    Picasso.with(this).load(imageUrl)
                            .into(trackFrameLayout);

                } else {
                    Picasso.with(this).load(R.drawable.no_album)
                            .into(trackFrameLayout);
                }

            }
        } else {

            Intent intent = new Intent(this, TrackActivity.class);
            intent.putExtra("id", mSelectedArtist.getId());
            intent.putExtra("artist", mSelectedArtist);
            // TODO Need to pull this extra (artistQueryString) in phone activity
            intent.putExtra("artistQueryString", mArtistQueryString);
            intent.putExtra("showButton", mShowNowPlayingButton);
            intent.putExtra("image", mSelectedArtist.getLargestImageUrl());

            startActivityForResult(intent, 1);

        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if ( requestCode == 1 ) {
            if ( resultCode == RESULT_OK ) {
                mShowNowPlayingButton = data.getBooleanExtra("showButton", false);
            }
        }

    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putString("artistQueryString", mArtistQueryString);
        outState.putParcelable("mSelectedArtist", mSelectedArtist);
        if ( mIsPlayingMenuItem != null ) {
            outState.putBoolean("showNowPlayingActionBarButton", mIsPlayingMenuItem.isVisible());
        } else {
            outState.putBoolean("showNowPlayingActionBarButton", false);
        }

   }

    private void setShareIntent(Intent shareIntent) {
        if ( mShareActionProvider != null ) {
            mShareActionProvider.setShareIntent(shareIntent);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
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
            case R.id.action_settings:

                Intent intent = new Intent(this, SettingsActivity.class);
                startActivity(intent);

                return true;

            case R.id.action_play:

                FragmentManager fragmentManager = getSupportFragmentManager();
                TrackPlayerActivityFragment trackPlayerActivityFragment = new TrackPlayerActivityFragment();

                LocalTrack track = mTrackPlayerService.getCurrentTrack();

                ArrayList<LocalTrack> tracks = new ArrayList<>();
                tracks.add(track);

                trackPlayerActivityFragment.setValues("", tracks, 0);

                trackPlayerActivityFragment.show(fragmentManager, "dialog");

                mTrackPlayerService.requestUiUpdate();

                mIsPlayingMenuItem.setVisible(false);
                mShareMenuItem.setVisible(false);

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

    @Override
    protected void onStop() {
        super.onStop();

        if (mBound) {
            unbindService(mConnection);
            mBound = false;
        }

        if (mGoogleApiClient.isConnected()) {
            Nearby.Messages.unsubscribe(mGoogleApiClient, messageListener)
                    .setResultCallback(new ErrorCheckingCallback("unsubscribe()"));
        }

        mGoogleApiClient.disconnect();

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

                    // Setup shareintent
                    Intent shareIntent = new Intent();
                    shareIntent.setAction(Intent.ACTION_SEND);
                    shareIntent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.share_subject));

                    String shareMessage = String.format(getString(R.string.share_message), localTrack.getTrackName(), artistName, localTrack.getPreview_url());
                    shareIntent.putExtra(Intent.EXTRA_TEXT, shareMessage);

                    shareIntent.setType("text/plain");
                    setShareIntent(shareIntent);

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

    public class BuildBeaconNotificationAsyncTask extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... params) {

            String webPage = params[0];

            String site = "http://jscruffy.fatcow.com/" + webPage + ".json";

            InputStream is = null;

            StringBuilder stringBuilder = new StringBuilder();

            try {
                URL url = new URL(site);

                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setReadTimeout(10000);
                conn.setConnectTimeout(15000);
                conn.setRequestMethod("GET");

                conn.connect();
                int response = conn.getResponseCode();
                Log.d(LOG_TAG, "Response code = " + response);

                is = conn.getInputStream();

                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(is, "UTF-8"));

                String line;
                while ( (line = bufferedReader.readLine()) != null ) {
                    stringBuilder.append(line + "\n");
                }
                bufferedReader.close();

            } catch (Exception e) {
                Log.e(LOG_TAG, "Bad URL");
            } finally {
                if ( is != null ) {
                    try {
                        is.close();
                    } catch (Exception e) {
                        Log.e(LOG_TAG, "Unable to close input stream");
                    }
                }
            }

            return stringBuilder.toString();
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);

            NotificationManager mNotificationManager;

            Intent intent = new Intent(MainActivity.this, EventActivity.class);
            intent.putExtra("event_info", result);

            PendingIntent pendingIntent = PendingIntent.getActivity(MainActivity.this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

            final Notification notification = new Notification.Builder(MainActivity.this)
                    .setContentTitle("Upcoming Spotify Events (Real)")
                    .setContentText("Your local events")
                    .setSmallIcon(R.drawable.icon_square)
                    .setContentIntent(pendingIntent)
                    .build();

            mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

            mNotificationManager.notify(999, notification);

        }
    }
}
