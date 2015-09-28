package com.example.android.SpotifyStreamerWithBeaconSupport;

import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.android.SpotifyStreamerWithBeaconSupport.models.LocalArtist;
import com.example.android.SpotifyStreamerWithBeaconSupport.models.LocalImage;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;

import kaaes.spotify.webapi.android.SpotifyApi;
import kaaes.spotify.webapi.android.SpotifyService;
import kaaes.spotify.webapi.android.models.Artist;
import kaaes.spotify.webapi.android.models.ArtistsPager;
import kaaes.spotify.webapi.android.models.Image;


/**
 * A placeholder fragment containing a simple view.
 */
public class MainActivityFragment extends Fragment {

    private OnArtistSelectedListener mCallback;

    private final String LOG_TAG = MainActivityFragment.class.getSimpleName();

//    private ArrayAdapter<String> artistAdapter;
    private ArrayList<LocalArtist> artistsArrayList;
    private int mPosition = -1;
    private String mArtistId = "";
    private ListView listView;
    private String mQueryString = "";

    public interface OnArtistSelectedListener {
        void onArtistSelected(String queryString, LocalArtist localArtist);
        void onArtistSearchChanged();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelableArrayList("artistsArrayList", artistsArrayList);
        outState.putInt("position", mPosition);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        // This makes sure that the container activity has implemented
        // the callback interface. If not, it throws an exception
        try {
            mCallback = (OnArtistSelectedListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnArtistSelectedListener");
        }
    }

    public MainActivityFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_main, container, false);

        listView = (ListView) rootView.findViewById(R.id.listview_artist);

        return rootView;

    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        artistsArrayList = new ArrayList<LocalArtist>();

        if ( savedInstanceState != null ) {
            artistsArrayList = savedInstanceState.getParcelableArrayList("artistsArrayList");
            mPosition = savedInstanceState.getInt("position", -1);

            ArtistsAdapter adapter = new ArtistsAdapter(getActivity(), artistsArrayList);

            listView.setAdapter(adapter);

        }

        final EditText artistQuery = (EditText) getView().findViewById(R.id.edittext_artist_query);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                LocalArtist selectedArtist;

                // TODO this should be artistAdapter.getItem..see lesson 3 - 5th session
                selectedArtist = artistsArrayList.get(position);
                mPosition = position;

                mCallback.onArtistSelected(mQueryString, selectedArtist);

            }
        });

    }

    public void setValues(String artistQueryString, String artistId) {
        final EditText artistQuery = (EditText) getView().findViewById(R.id.edittext_artist_query);
        artistQuery.setText(artistQueryString);

        mQueryString = artistQueryString;
        mArtistId = artistId;

        if ( !mQueryString.isEmpty() ) {
            new DownloadArtists().execute(mQueryString);
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        listView.post(new Runnable() {
            @Override
            public void run() {
                listView.smoothScrollToPositionFromTop(mPosition, 0);
            }
        });

        final EditText artistQuery = (EditText) getView().findViewById(R.id.edittext_artist_query);

        artistQuery.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                mCallback.onArtistSearchChanged();
            }

            @Override
            public void afterTextChanged(Editable s) {
                mQueryString = artistQuery.getText().toString();
                if (!mQueryString.isEmpty()) {
                    new DownloadArtists().execute(mQueryString);
                } else {

                    artistsArrayList.clear();
                    ArtistsAdapter adapter = new ArtistsAdapter(getActivity(), artistsArrayList);

                    listView.setAdapter(adapter);
                }
            }
        });

    }

    public class DownloadArtists extends AsyncTask<String, Void, ArtistsPager> {

        @Override
        protected ArtistsPager doInBackground(String... params) {

            ArtistsPager results = null;

            try {
                SpotifyApi api = new SpotifyApi();
                SpotifyService spotify = api.getService();
                if ( params[0] != null && !params[0].equals("") ) {
                    results = spotify.searchArtists(params[0]);
                }
            } catch (Exception e) {
                Log.e(LOG_TAG, "Spotify exception - " + e.getMessage());
            }

            return results;
        }

        @Override
        protected void onPostExecute(ArtistsPager results) {

            if ( results == null ) {
                Toast.makeText(getActivity(), getString(R.string.message_problem_artist), Toast.LENGTH_SHORT).show();
                return;
            }

            artistsArrayList = new ArrayList<LocalArtist>();

            int position = -1;

            if ( results != null) {

                for(Artist a : results.artists.items) {

                    ArrayList<LocalImage> images = new ArrayList<LocalImage>();

                    for(Image i : a.images) {
                        images.add(new LocalImage(i.url, i.width, i.height));
                    }

                    LocalArtist oneArtist = new LocalArtist(a.id, a.name, images);
                    artistsArrayList.add(oneArtist);
                }

            }

            for(int i = 0; i < artistsArrayList.size(); i++) {
                LocalArtist artist = artistsArrayList.get(i);
                if ( artist.getId().equals(mArtistId) ) {
                    position = i;
                    break;
                }
            }

            if ( getActivity() != null ) {

                final EditText artistQuery = (EditText) getView().findViewById(R.id.edittext_artist_query);

                if ( !artistQuery.getText().toString().equals("") ) {
                    ArtistsAdapter adapter = new ArtistsAdapter(getActivity(), artistsArrayList);
                    listView.setAdapter(adapter);

                    if ( position != -1 ) {
                        listView.setSelection(position);
                    }

                } else {
                    listView.setAdapter(null);
                }

                if ( artistsArrayList.size() == 0 ) {
                    Toast.makeText(getActivity(), getString(R.string.message_no_artists_found),
                            Toast.LENGTH_SHORT).show();
                }

            }

        }
    }

    static class ViewHolder {
        ArtistLinearLayout artistLinearLayout;
        TextView textViewArtistName;
        ImageView imageViewArtistImage;
    }

    public class ArtistsAdapter extends ArrayAdapter<LocalArtist> {

        public ArtistsAdapter(Context context, ArrayList<LocalArtist> artists) {
            super(context, 0, artists);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {

            LocalArtist artist = getItem(position);

            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.list_item_artist, parent, false);

                ViewHolder viewHolder = new ViewHolder();
                viewHolder.artistLinearLayout = (ArtistLinearLayout) convertView.findViewById(R.id.artistlinearlayout);
                viewHolder.textViewArtistName = (TextView) convertView.findViewById(R.id.textView_artistName);
                viewHolder.imageViewArtistImage = (ImageView) convertView.findViewById(R.id.imageView_artistImage);

                convertView.setTag(viewHolder);
            }

            ViewHolder viewHolder = (ViewHolder) convertView.getTag();

            TextView tvArtistName = viewHolder.textViewArtistName;
            ImageView ivArtistImage = viewHolder.imageViewArtistImage;
            ArtistLinearLayout artistLinearLayout = viewHolder.artistLinearLayout;

            tvArtistName.setText(artist.getName());

            if ( artist.getArtistImages().size() > 0 ) {
//                Picasso.with(getContext()).setIndicatorsEnabled(true);
                Picasso.with(getContext()).load(artist.getThumbnailUrl())
                        .resize(200, 200)
                        .centerInside()
                        .placeholder(R.drawable.icon_square)
                        .into(ivArtistImage);

                Picasso.with(getContext()).load(artist.getThumbnailUrl())
                        .into(artistLinearLayout);

            } else {
//                Picasso.with(getContext()).setIndicatorsEnabled(true);
                Picasso.with(getContext()).load(R.drawable.no_album)
                        .resize(200, 200)
                        .centerInside()
                        .into(ivArtistImage);

                Picasso.with(getContext()).load(R.drawable.no_album)
                        .into(artistLinearLayout);

            }

            return convertView;
        }
    }
}


