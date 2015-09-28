package com.example.android.SpotifyStreamerWithBeaconSupport;

import android.app.Activity;
import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import com.example.android.SpotifyStreamerWithBeaconSupport.models.LocalTrack;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;

/**
 * A placeholder fragment containing a simple view.
 */
public class TrackPlayerActivityFragment extends DialogFragment implements View.OnClickListener,
SeekBar.OnSeekBarChangeListener {

    private final String LOG_TAG = TrackPlayerActivityFragment.class.getSimpleName();

    private TrackPlayerActivityListener mCallback;

    private boolean mIsPlaying = false;
    private boolean mIsSongLoaded = false;

    private String artistName;
    private LocalTrack localTrack;
    private ArrayList<LocalTrack> tracks;
    private Integer currentTrackPosition;

//    private TrackPlayerService mTrackPlayerService;
//    private Boolean mBound = false;

    private Thread mMoveSeekBarThread;
    private int mTrackDuration;

    private boolean mPausedBySeekBarMove = false;

    public String getArtistName() {
        return artistName;
    }

    public LocalTrack getLocalTrack() {
        return localTrack;
    }

    public ArrayList<LocalTrack> getTracks() {
        return tracks;
    }

    public Integer getCurrentTrackPosition() {
        return currentTrackPosition;
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onStop() {
        super.onStop();

        if ( mMoveSeekBarThread != null ) {
            mMoveSeekBarThread.interrupt();
            mMoveSeekBarThread = null;
        }

    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

        if ( mMoveSeekBarThread != null ) {
            mMoveSeekBarThread.interrupt();
            mMoveSeekBarThread = null;
        }

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {

        int currentPosition = seekBar.getProgress();

        mCallback.setCurrentTrackPosition(currentPosition);
        mCallback.onRequestUiUpdate();
    }

    public interface TrackPlayerActivityListener {
        public void onClickNextTrack();
        public void onClickPreviousTrack();
        public void onClickPlayPauseTrack();
        public void onRequestUiUpdate();
        public void setCurrentTrackPosition(int currentTrackPosition);
        public void showActionBarPlayingButton(boolean showButton);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putParcelable("localTrack", localTrack);
        outState.putString("artistName", artistName);
        outState.putParcelableArrayList("tracks", tracks);
        outState.putInt("currentTrackPosition", currentTrackPosition);

        final SeekBar seekBar = (SeekBar) getView().findViewById(R.id.seekBar);
        outState.putInt("duration", seekBar.getMax());
        outState.putInt("currentPosition", seekBar.getProgress());
        outState.putBoolean("isPlaying", mIsPlaying);

    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        // This makes sure that the container activity has implemented
        // the callback interface. If not, it throws an exception
        try {
            mCallback = (TrackPlayerActivityListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement TrackPlayerActivityListener");
        }

    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mCallback.showActionBarPlayingButton(false);

        ImageButton btnPreviousTrack = (ImageButton) getView().findViewById(R.id.btnPrevTrack);
        ImageButton btnPlayPauseTrack = (ImageButton) getView().findViewById(R.id.btnPlayPauseTrack);
        ImageButton btnNextTrack = (ImageButton) getView().findViewById(R.id.btnNextTrack);
        SeekBar seekBarTrackPosition = (SeekBar) getView().findViewById(R.id.seekBar);

        btnPreviousTrack.setOnClickListener(this);
        btnPlayPauseTrack.setOnClickListener(this);
        btnNextTrack.setOnClickListener(this);
        seekBarTrackPosition.setOnSeekBarChangeListener(this);

        if ( savedInstanceState != null ) {

            artistName = savedInstanceState.getString("artistName");
            tracks = savedInstanceState.getParcelableArrayList("tracks");
            currentTrackPosition = savedInstanceState.getInt("currentTrackPosition");

            localTrack = savedInstanceState.getParcelable("localTrack");

            final SeekBar seekBar = (SeekBar) getView().findViewById(R.id.seekBar);

            // TODO Need to improve as many rotations will get seekbar out of sync w/ track when redrawing
            int trackDuration = savedInstanceState.getInt("duration", 30);
            int currentTrackPosition = savedInstanceState.getInt("currentPosition", 0);
            boolean isPlaying = savedInstanceState.getBoolean("isPlaying");

            updateSeekbar(isPlaying, trackDuration, currentTrackPosition);

        }

        populateViews();

//        setHasOptionsMenu(true);

    }

    @Override
    public void dismiss() {
        super.dismiss();
        mCallback.showActionBarPlayingButton(true);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mCallback.showActionBarPlayingButton(true);
        mCallback.onRequestUiUpdate();
    }

    private void populateViews() {

        TextView trackPlayerArtistName = (TextView) getView().findViewById(R.id.trackPlayerArtistName);
        TextView trackPlayerAlbumName = (TextView) getView().findViewById(R.id.trackPlayerAlbumName);
        ImageView trackPlayerAlbumArtwork = (ImageView) getView().findViewById(R.id.trackPlayerAlbumArtwork);
        TextView trackPlayerTrackName = (TextView) getView().findViewById(R.id.trackPlayerTrackName);

        trackPlayerArtistName.setText(artistName);

        if ( localTrack != null ) {
            if (localTrack.getAlbumName() != null) {
                trackPlayerAlbumName.setText(localTrack.getAlbumName());
            }

            if (localTrack.getTrackName() != null) {
                trackPlayerTrackName.setText(localTrack.getTrackName());
            }

            if ( localTrack.getLargestImageUrl() != null ) {
                Picasso.with(getActivity())
                        .load(localTrack.getLargestImageUrl())
                        .resize(600, 600)
                        .centerInside()
                        .placeholder(R.drawable.no_album)
                        .into(trackPlayerAlbumArtwork);

            }
        }

    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onClick(View v) {

        final SeekBar seekBar = (SeekBar) getView().findViewById(R.id.seekBar);

        switch (v.getId()) {

            case R.id.btnPrevTrack:

                if ( mMoveSeekBarThread != null ) {
                    mMoveSeekBarThread.interrupt();
                    mMoveSeekBarThread = null;
                }

                seekBar.setProgress(0);
                seekBar.setMax(30000);
                seekBar.setProgress(0);

                mCallback.onClickPreviousTrack();

                break;

            case R.id.btnPlayPauseTrack:

                mCallback.onClickPlayPauseTrack();

                break;

            case R.id.btnNextTrack:

                if ( mMoveSeekBarThread != null ) {
                    mMoveSeekBarThread.interrupt();
                    mMoveSeekBarThread = null;
                }

                seekBar.setProgress(0);
                seekBar.setMax(30000);
                seekBar.setProgress(0);

                mCallback.onClickNextTrack();

                break;

            default:

                break;
        }

    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater){
//        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.menu_track_player, menu);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
//        return super.onCreateDialog(savedInstanceState);

        Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        return dialog;

    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }

    public TrackPlayerActivityFragment() {
    }

    public void setValues(String artistName, ArrayList<LocalTrack> tracks, Integer position) {
        this.artistName = artistName;
        this.tracks = tracks;
        this.currentTrackPosition = position;

        this.localTrack = tracks.get(position);

    }

    public void updateViews(String artistName, LocalTrack latestTrack) {

        this.artistName = artistName;
        this.localTrack = latestTrack;

        populateViews();

    }

    public void updateSeekbar(boolean isPlaying, int trackDuration, int currentTrackPosition) {
        mTrackDuration = trackDuration;
        mIsPlaying = isPlaying;

        final SeekBar seekBar = (SeekBar) getView().findViewById(R.id.seekBar);

        if ( mMoveSeekBarThread != null ) {
            mMoveSeekBarThread.interrupt();
            mMoveSeekBarThread = null;
        }

        seekBar.setProgress(0);
        seekBar.setMax(trackDuration);
        seekBar.setProgress(currentTrackPosition);

        final ImageButton btnPlayPauseTrack = (ImageButton) getView().findViewById(R.id.btnPlayPauseTrack);

        if ( isPlaying ) {

            btnPlayPauseTrack.setImageResource(android.R.drawable.ic_media_pause);

            RunnableProgress r = new RunnableProgress(seekBar, currentTrackPosition);

            mMoveSeekBarThread = new Thread(r, "Thread_mMoveSeekBarThread");
            mMoveSeekBarThread.start();

        } else {
            btnPlayPauseTrack.setImageResource(android.R.drawable.ic_media_play);
        }

    }

    public void requestUiUpdate() {
        mCallback.onRequestUiUpdate();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        return inflater.inflate(R.layout.fragment_track_player, container, false);
    }

    class RunnableProgress implements Runnable {

        SeekBar mSeekBar;
        int mCurrentSeekbarPosition = 0;

        public RunnableProgress() {

        }

        public RunnableProgress(SeekBar object, int currentTrackPosition) {
            mSeekBar = object;
            mCurrentSeekbarPosition = currentTrackPosition;
        }

        public void run() {

            int sleepTimeInMilliseconds = 50;

            while( true ) {

                if ( Thread.interrupted() ) {
                    return;
                }

                try {
                    Thread.sleep(sleepTimeInMilliseconds);
                } catch (Exception e) {
                    Log.e(LOG_TAG, "Updating seekbar");
                    return;
                }

                mCurrentSeekbarPosition += sleepTimeInMilliseconds;

                final int value = mCurrentSeekbarPosition;

                if ( getActivity() != null ) {
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mSeekBar.setProgress(value);
                        }
                    });

                }

            }
        }
    }
}
