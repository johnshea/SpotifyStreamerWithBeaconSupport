package com.example.android.SpotifyStreamerWithBeaconSupport;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import com.example.android.SpotifyStreamerWithBeaconSupport.models.LocalImage;
import com.example.android.SpotifyStreamerWithBeaconSupport.models.LocalTrack;

import java.util.ArrayList;
import java.util.HashMap;

import kaaes.spotify.webapi.android.SpotifyApi;
import kaaes.spotify.webapi.android.SpotifyService;
import kaaes.spotify.webapi.android.models.Image;
import kaaes.spotify.webapi.android.models.Track;
import kaaes.spotify.webapi.android.models.Tracks;


/**
 * A placeholder fragment containing a simple view.
 */
public class TrackActivityFragment extends Fragment {

    private OnTrackSelectedListener mCallback;

    private final String LOG_TAG = TrackActivityFragment.class.getSimpleName();

    private ListView tracksListView;
    private ArrayList<LocalTrack> tracksArrayList;

    private String id;
    private String artist;

    private int intCurrentTrack;
    private String prefCountryCode;

    public interface OnTrackSelectedListener {
        void OnTrackSelected(ArrayList<LocalTrack> tracks, Integer position);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelableArrayList("tracksArrayList", tracksArrayList);
        outState.putInt("position", intCurrentTrack);
    }

    @Override
    public void onResume() {
        super.onResume();

        tracksListView.post(new Runnable() {
            @Override
            public void run() {
                tracksListView.smoothScrollToPositionFromTop(intCurrentTrack, 0);
            }
        });
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        // This makes sure that the container activity has implemented
        // the callback interface. If not, it throws an exception
        try {
            mCallback = (OnTrackSelectedListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnTrackSelected");
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getActivity());
        prefCountryCode = sharedPref.getString("pref_country_code", "US");

        if ( savedInstanceState != null ) {
            tracksArrayList = savedInstanceState.getParcelableArrayList("tracksArrayList");
            intCurrentTrack = savedInstanceState.getInt("position", 0);
        }

        if ( tracksArrayList == null ){
            DownloadTracksParam downloadTracksParam = new DownloadTracksParam(id, prefCountryCode);
            new DownloadTracks().execute(downloadTracksParam);
        } else {
            TracksAdapter tracksAdapter = new TracksAdapter(getActivity(), tracksArrayList);

            tracksListView.setAdapter(tracksAdapter);
        }

    }

    public TrackActivityFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    public void setData(ArrayList<LocalTrack> data) {
        this.tracksArrayList = data;
    }

    public void setTrackPosition(int currentTrackPosition) {

        final int position = currentTrackPosition;

        intCurrentTrack = position;

        tracksListView.post(new Runnable() {
            @Override
            public void run() {
                tracksListView.setItemChecked(position, true);
            }
        });
    }

    public void setValues(String id, String artist, String countryCode) {

        Boolean artistIdChanged = false;

        if ( this.id != id ) {
            artistIdChanged = true;
        } else {
            artistIdChanged = false;
        }

        this.id = id;
        this.artist = artist;

        if ( artistIdChanged ) {
            DownloadTracksParam downloadTracksParam = new DownloadTracksParam(id, countryCode);
            new DownloadTracks().execute(downloadTracksParam);
        }

    }

    public ArrayList<LocalTrack> getData() {
        return this.tracksArrayList;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_track, container, false);

        tracksListView = (ListView) rootView.findViewById(R.id.listview_tracks);

        tracksListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                intCurrentTrack = position;

                mCallback.OnTrackSelected(tracksArrayList, intCurrentTrack);
            }
        });

        ActionBar actionBar = ((ActionBarActivity)getActivity()).getSupportActionBar();
        if ( actionBar != null ) {
            actionBar.setSubtitle(artist);
        }

        return rootView;
    }

    public LocalTrack getNextTrack() {
        if ( intCurrentTrack < (tracksArrayList.size() - 1) ) {
            intCurrentTrack++;
        }
        return tracksArrayList.get(intCurrentTrack);
    }

    public LocalTrack getPreviousTrack() {
        if ( intCurrentTrack > 0 ) {
            intCurrentTrack--;
        }
        return tracksArrayList.get(intCurrentTrack);
    }

    private static class DownloadTracksParam {
        String artistId;
        String countryCode;

        DownloadTracksParam(String artistId, String countryCode) {
            this.artistId = artistId;
            this.countryCode = countryCode;
        }
    }

    public class DownloadTracks extends AsyncTask<DownloadTracksParam, Void, Tracks> {
        @Override
        protected Tracks doInBackground(DownloadTracksParam... params) {
            String artistId = params[0].artistId;
            String countryCode = params[0].countryCode;

            HashMap<String, Object> optionsMap = new HashMap<>();
            optionsMap.put("country", countryCode);

            Tracks results = null;

            SpotifyApi api = new SpotifyApi();
            SpotifyService spotify = api.getService();

            try {
                results = spotify.getArtistTopTrack(artistId, optionsMap);
            } catch (Exception e) {
                Log.e(LOG_TAG, "Spotify exception - " + e.getMessage());
            }

            return results;
        }

        @Override
        protected void onPostExecute(Tracks results) {

            if ( results == null ) {
                Toast.makeText(getActivity(), getString(R.string.message_problem_top_10_tracks), Toast.LENGTH_SHORT).show();
                return;
            }

            tracksArrayList = new ArrayList<LocalTrack>();

            for(Track track : results.tracks) {

                ArrayList<LocalImage> images = new ArrayList<LocalImage>();

                for(Image i : track.album.images) {
                    images.add(new LocalImage(i.url, i.width, i.height));
                }

                LocalTrack localTrack = new LocalTrack(track.album.name, track.name, images, track.preview_url);
                tracksArrayList.add(localTrack);
            }

            TracksAdapter tracksAdapter = new TracksAdapter(getActivity(), tracksArrayList);

            tracksListView.setAdapter(tracksAdapter);

            if ( tracksArrayList.size() == 0 ) {
                Toast.makeText(getActivity(), getString(R.string.message_no_top_10_tracks), Toast.LENGTH_SHORT).show();
            }
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }
    }
}
