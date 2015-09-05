package com.example.android.project1.models;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;

/**
 * Created by John on 7/3/2015.
 */
public class LocalTrack implements Parcelable {
    public String albumName;
    public String trackName;
    public ArrayList<LocalImage> albumImages;
    public String preview_url;

    private Integer smallestImageIndex;
    private Integer largestImageIndex;

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(albumName);
        dest.writeString(trackName);
        dest.writeTypedList(albumImages);
        dest.writeString(preview_url);
        dest.writeInt(smallestImageIndex);
        dest.writeInt(largestImageIndex);
    }

    private LocalTrack(Parcel in) {
        albumName = in.readString();
        trackName = in.readString();
        albumImages = in.createTypedArrayList(LocalImage.CREATOR);
        preview_url = in.readString();
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

    public LocalTrack(String albumName, String trackName, ArrayList<LocalImage> albumImages, String preview_url) {
        this.albumName = albumName;
        this.trackName = trackName;
        this.albumImages = albumImages;
        this.preview_url = preview_url;

        calculateSmallestAndLargestImage();
    }

    private void calculateSmallestAndLargestImage() {

        Integer currentSmallestIndex;
        Integer currentSmallestWidth;

        Integer currentLargestIndex;
        Integer currentLargestWidth;

        if ( this.albumImages.size() > 0 ) {

            currentSmallestIndex = 0;
            currentSmallestWidth = this.albumImages.get(0).width;

            currentLargestIndex = 0;
            currentLargestWidth = this.albumImages.get(0).width;

            for(int i = 0; i < this.albumImages.size(); i++) {

                LocalImage currentImage = this.albumImages.get(i);

                // Determine smallest image
                if ( currentImage.width < currentSmallestWidth ) {
                    currentSmallestIndex = i;
                    currentSmallestWidth = this.albumImages.get(i).width;
                }

                // Determine largest image
                if ( currentImage.width > currentLargestWidth ) {
                    currentLargestIndex = i;
                    currentLargestWidth = this.albumImages.get(i).width;
                }
            }

            smallestImageIndex = currentSmallestIndex;
            largestImageIndex = currentLargestIndex;
        }


    }

    public String getThumbnailUrl() {
        return this.albumImages.get(smallestImageIndex).url;
    }

    public String getLargestImageUrl() {
        return this.albumImages.get(largestImageIndex).url;
    }

}