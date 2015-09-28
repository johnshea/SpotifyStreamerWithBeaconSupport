package com.example.android.SpotifyStreamerWithBeaconSupport;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.android.SpotifyStreamerWithBeaconSupport.models.LocalTrack;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;

/**
 * Created by John on 9/9/2015.
 */

public class TracksAdapter extends ArrayAdapter<LocalTrack> {

    static class ViewHolder {
        TextView textViewAlbumName;
        TextView textViewTrackName;
        ImageView imageViewAlbumImage;
    }

    public TracksAdapter(Context context, ArrayList<LocalTrack> tracks) {
        super(context, 0, tracks);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        LocalTrack localTrack = getItem(position);

        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.list_item_track, parent, false);

            ViewHolder viewHolder = new ViewHolder();
            viewHolder.textViewAlbumName = (TextView) convertView.findViewById(R.id.textView_albumName);
            viewHolder.textViewTrackName = (TextView) convertView.findViewById(R.id.textView_trackName);
            viewHolder.imageViewAlbumImage = (ImageView) convertView.findViewById(R.id.imageView_albumImage);

            convertView.setTag(viewHolder);
        }

        ViewHolder viewHolder = (ViewHolder) convertView.getTag();

        TextView tvAlbumName = viewHolder.textViewAlbumName;
        TextView tvTrackName = viewHolder.textViewTrackName;
        ImageView ivAlbumImage = viewHolder.imageViewAlbumImage;

        tvAlbumName.setText(localTrack.getAlbumName());
        tvTrackName.setText(localTrack.getTrackName());

        if ( localTrack.getAlbumImages().size() > 0 ) {
            Picasso.with(getContext()).load(localTrack.getThumbnailUrl())
                    .resize(200, 200)
                    .centerInside()
                    .placeholder(R.drawable.icon_square)
                    .into(ivAlbumImage);

        } else {
            Picasso.with(getContext()).load(R.drawable.no_album)
                    .resize(200, 200)
                    .centerInside()
                    .into(ivAlbumImage);
        }

        return convertView;
    }

}
