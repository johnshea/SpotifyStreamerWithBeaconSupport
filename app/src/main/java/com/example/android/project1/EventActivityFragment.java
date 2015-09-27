package com.example.android.project1;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.example.android.project1.models.LocalEvent;
import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;

public class EventActivityFragment extends Fragment {

    private final String LOG_TAG = EventActivityFragment.class.getSimpleName();

    private OnEventSelectedListener mCallback;

    JSONObject eventsObject;

    private ArrayList<LocalEvent> eventsArrayList;

    EventsAdapter eventsAdapter;

    public interface OnEventSelectedListener {
        void onEventSelected(String artist_id, String artist_name, String artist_image);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        // This makes sure that the container activity has implemented
        // the callback interface. If not, it throws an exception
        try {
            mCallback = (OnEventSelectedListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnEventSelectedListener");
        }
    }

    public EventActivityFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_event, container, false);
    }

    public void setValues(String event_info) {
        try {
            eventsObject = new JSONObject(event_info);
        } catch (Exception e) {
            Log.e(LOG_TAG, "Unable to parse JSON passed in");
        }
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        if ( eventsObject != null ) {
            eventsArrayList = new ArrayList<>();

            TextView tvEventLocation = (TextView) getView().findViewById(R.id.beaconLocationTextView);
            try {
                tvEventLocation.setText("Upcoming Events Near " + eventsObject.getString("beacon_location"));
            } catch (Exception e) {
                Log.e(LOG_TAG, "Unable to parse beacon location from JSON");
                tvEventLocation.setText("Upcoming Events Near Your Location");
            }

            try {
                JSONArray jsonArray = eventsObject.getJSONArray("events");
                for (int i = 0; i < jsonArray.length(); i++) {

                    JSONObject oneEvent = jsonArray.getJSONObject(i);

                    LocalEvent ev = new LocalEvent();

                    ev.event_date = oneEvent.get("event_date").toString();
                    ev.event_type = oneEvent.get("event_type").toString();
                    ev.event_location = oneEvent.get("event_location").toString();
                    ev.event_artist_id = oneEvent.get("event_artist_id").toString();
                    ev.event_artist_name = oneEvent.get("event_artist_name").toString();
                    ev.event_artist_image = oneEvent.get("event_artist_image").toString();

                    eventsArrayList.add(ev);
                }
            } catch (Exception e) {
                Log.e(LOG_TAG, "Unable to parse JSON Array");
            }

            ListView eventsListView = (ListView) getView().findViewById(R.id.eventsListView);

            eventsAdapter = new EventsAdapter(getActivity(), eventsArrayList);

            eventsListView.setAdapter(eventsAdapter);

            eventsListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                    LocalEvent selectedEvent;
                    selectedEvent = eventsAdapter.getItem(position);
                    mCallback.onEventSelected(selectedEvent.event_artist_id, selectedEvent.event_artist_name, selectedEvent.event_artist_image);
                }
            });

        }
    }

    static class ViewHolder {
        ArtistLinearLayout artistLinearLayout;
        TextView textViewArtistName;
        TextView textViewEventType;
        TextView textViewEventDate;
        ImageView imageViewArtistImage;
    }

    public class EventsAdapter extends ArrayAdapter<LocalEvent> {

        public EventsAdapter(Context context, ArrayList<LocalEvent> localEvents) {
            super(context, 0, localEvents);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {

            LocalEvent event = getItem(position);

            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.list_item_event, parent, false);

                ViewHolder viewHolder = new ViewHolder();
                viewHolder.artistLinearLayout = (ArtistLinearLayout) convertView.findViewById(R.id.artistlinearlayout);
                viewHolder.textViewArtistName = (TextView) convertView.findViewById(R.id.textView_artistName);
                viewHolder.imageViewArtistImage = (ImageView) convertView.findViewById(R.id.imageView_artistImage);
                viewHolder.textViewEventDate = (TextView) convertView.findViewById(R.id.eventDateTextView);
                viewHolder.textViewEventType = (TextView) convertView.findViewById(R.id.eventTypeTextView);

                convertView.setTag(viewHolder);
            }

            ViewHolder viewHolder = (ViewHolder) convertView.getTag();

            TextView tvArtistName = viewHolder.textViewArtistName;
            TextView tvEventDate = viewHolder.textViewEventDate;
            TextView tvEventType = viewHolder.textViewEventType;
            ImageView ivArtistImage = viewHolder.imageViewArtistImage;
            ArtistLinearLayout artistLinearLayout = viewHolder.artistLinearLayout;

            tvArtistName.setText(event.event_artist_name);

            switch ( event.event_type ) {
                case "CONCERT":
                    tvEventType.setText("Concert @ " + event.event_location);
                    break;
                case "NEWALBUM":
                    tvEventType.setText("New Album Launch");
                    break;
                default:
                    tvArtistName.setText("Unknown Event");
            }


            DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");

            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("cccc, MMMM d");
            try {
                tvEventDate.setText(simpleDateFormat.format(dateFormat.parse(event.event_date)));
            } catch (Exception e) {
                tvEventDate.setText(event.event_date);
            }

            if ( event.event_artist_image != null && !event.event_artist_image.equals("") ) {
                Picasso.with(getContext()).load(event.event_artist_image)
                        .resize(200, 200)
                        .centerInside()
                        .placeholder(R.drawable.icon_square)
                        .into(ivArtistImage);

                Picasso.with(getContext()).load(event.event_artist_image)
                        .into(artistLinearLayout);

            } else {
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
