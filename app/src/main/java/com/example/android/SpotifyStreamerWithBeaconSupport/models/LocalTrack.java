package com.example.android.SpotifyStreamerWithBeaconSupport.models;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;

/**
 * Created by John on 7/3/2015.
 */
public class LocalTrack implements Parcelable {
    private String albumName;
    private String trackName;
    private ArrayList<LocalImage> albumImages;
    private String preview_url;

    private Integer smallestImageIndex;
    private Integer largestImageIndex;

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(getAlbumName());
        dest.writeString(getTrackName());
        dest.writeTypedList(getAlbumImages());
        dest.writeString(getPreview_url());
        dest.writeInt(smallestImageIndex);
        dest.writeInt(largestImageIndex);
    }

    private LocalTrack(Parcel in) {
        setAlbumName(in.readString());
        setTrackName(in.readString());
        setAlbumImages(in.createTypedArrayList(LocalImage.CREATOR));
        setPreview_url(in.readString());
        smallestImageIndex = in.readInt();
        largestImageIndex = in.readInt();
    }

    public static final Parcelable.Creator<LocalTrack> CREATOR =
            new Parcelable.Creator<LocalTrack>() {
                @Override
                public LocalTrack createFromParcel(Parcel source) {
                    return new LocalTrack(source);
                }

                @Override
                public LocalTrack[] newArray(int size) {
                    return new LocalTrack[size];
                }
            };

    public LocalTrack() {

    }

    public LocalTrack(String albumName, String trackName, ArrayList<LocalImage> albumImages, String preview_url) {
        this.setAlbumName(albumName);
        this.setTrackName(trackName);
        this.setAlbumImages(albumImages);
        this.setPreview_url(preview_url);

        calculateSmallestAndLargestImage();
    }

    private void calculateSmallestAndLargestImage() {

        Integer currentSmallestIndex;
        Integer currentSmallestWidth;

        Integer currentLargestIndex;
        Integer currentLargestWidth;

        if ( this.getAlbumImages().size() > 0 ) {

            currentSmallestIndex = 0;
            currentSmallestWidth = this.getAlbumImages().get(0).getWidth();

            currentLargestIndex = 0;
            currentLargestWidth = this.getAlbumImages().get(0).getWidth();

            for(int i = 0; i < this.getAlbumImages().size(); i++) {

                LocalImage currentImage = this.getAlbumImages().get(i);

                // Determine smallest image
                if ( currentImage.getWidth() < currentSmallestWidth ) {
                    currentSmallestIndex = i;
                    currentSmallestWidth = this.getAlbumImages().get(i).getWidth();
                }

                // Determine largest image
                if ( currentImage.getWidth() > currentLargestWidth ) {
                    currentLargestIndex = i;
                    currentLargestWidth = this.getAlbumImages().get(i).getWidth();
                }
            }

            smallestImageIndex = currentSmallestIndex;
            largestImageIndex = currentLargestIndex;
        }


    }

    public String getThumbnailUrl() {
        return this.getAlbumImages().get(smallestImageIndex).getUrl();
    }

    public String getLargestImageUrl() {
        return this.getAlbumImages().get(largestImageIndex).getUrl();
    }

    public String getAlbumName() {
        return albumName;
    }

    public void setAlbumName(String albumName) {
        this.albumName = albumName;
    }

    public String getTrackName() {
        return trackName;
    }

    public void setTrackName(String trackName) {
        this.trackName = trackName;
    }

    public ArrayList<LocalImage> getAlbumImages() {
        return albumImages;
    }

    public void setAlbumImages(ArrayList<LocalImage> albumImages) {
        this.albumImages = albumImages;
    }

    public String getPreview_url() {
        return preview_url;
    }

    public void setPreview_url(String preview_url) {
        this.preview_url = preview_url;
    }
}
