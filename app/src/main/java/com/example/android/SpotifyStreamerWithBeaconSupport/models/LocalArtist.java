package com.example.android.SpotifyStreamerWithBeaconSupport.models;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;

/**
 * Created by John on 7/3/2015.
 */
public class LocalArtist implements Parcelable {
    private String id;
    private String name;
    private String url;
    private ArrayList<LocalImage> artistImages;
    private Integer smallestImageIndex;
    private Integer largestImageIndex;

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(getId());
        dest.writeString(getName());
        dest.writeString(getUrl());
        dest.writeTypedList(getArtistImages());
        dest.writeInt(smallestImageIndex);
        dest.writeInt(largestImageIndex);
    }

    private LocalArtist(Parcel in) {
        setId(in.readString());
        setName(in.readString());
        setUrl(in.readString());
        setArtistImages(in.createTypedArrayList(LocalImage.CREATOR));
        //in.readTypedList(artistImages, LocalImage.CREATOR);
        smallestImageIndex = in.readInt();
        largestImageIndex = in.readInt();
    }

    public static final Parcelable.Creator<LocalArtist> CREATOR =
            new Parcelable.Creator<LocalArtist>() {

                @Override
                public LocalArtist createFromParcel(Parcel source) {
                    return new LocalArtist(source);
                }

                @Override
                public LocalArtist[] newArray(int size) {
                    return new LocalArtist[size];
                }
            };

    public LocalArtist(String id, String name, ArrayList<LocalImage> artistImages) {
        this.setId(id);
        this.setName(name);
        this.setArtistImages(artistImages);

        smallestImageIndex = 0;
        largestImageIndex = 0;

        calculateSmallestAndLargestImage();

    }

    private void calculateSmallestAndLargestImage() {

        Integer currentSmallestIndex;
        Integer currentSmallestWidth;

        Integer currentLargestIndex;
        Integer currentLargestWidth;

        if ( this.getArtistImages().size() > 0 ) {

            currentSmallestIndex = 0;
            currentSmallestWidth = this.getArtistImages().get(0).getWidth();

            currentLargestIndex = 0;
            currentLargestWidth = this.getArtistImages().get(0).getWidth();

            for(int i = 0; i < this.getArtistImages().size(); i++) {

                LocalImage currentImage = this.getArtistImages().get(i);

                // Determine smallest image
                if ( currentImage.getWidth() < currentSmallestWidth ) {
                    currentSmallestIndex = i;
                    currentSmallestWidth = this.getArtistImages().get(i).getWidth();
                }

                // Determine largest image
                if ( currentImage.getWidth() > currentLargestWidth ) {
                    currentLargestIndex = i;
                    currentLargestWidth = this.getArtistImages().get(i).getWidth();
                }
            }

            smallestImageIndex = currentSmallestIndex;
            largestImageIndex = currentLargestIndex;
        }

    }

    public String getThumbnailUrl() {
        return this.getArtistImages().get(smallestImageIndex).getUrl();
    }

    public String getLargestImageUrl() {
        String url = "";

        if (getArtistImages().size() == 0) {
            url = "";
        } else {
            url = this.getArtistImages().get(largestImageIndex).getUrl();
        }

        return url;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public ArrayList<LocalImage> getArtistImages() {
        return artistImages;
    }

    public void setArtistImages(ArrayList<LocalImage> artistImages) {
        this.artistImages = artistImages;
    }
}
