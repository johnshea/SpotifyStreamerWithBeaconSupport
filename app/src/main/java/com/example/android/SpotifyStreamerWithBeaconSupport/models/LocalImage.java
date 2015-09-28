package com.example.android.SpotifyStreamerWithBeaconSupport.models;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by John on 7/3/2015.
 */
public class LocalImage implements Parcelable {
    private String url;
    private Integer height;
    private Integer width;

    public LocalImage(String url, Integer width, Integer height) {
        this.setUrl(url);
        this.setWidth(width);
        this.setHeight(height);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(getUrl());
        dest.writeInt(getWidth());
        dest.writeInt(getHeight());
    }

    private LocalImage(Parcel in) {
        setUrl(in.readString());
        setWidth(in.readInt());
        setHeight(in.readInt());
    }

    public static final Parcelable.Creator<LocalImage> CREATOR =
            new Parcelable.Creator<LocalImage>() {
                @Override
                public LocalImage createFromParcel(Parcel source) {
                    return new LocalImage(source);
                }

                @Override
                public LocalImage[] newArray(int size) {
                    return new LocalImage[size];
                }
            };

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public Integer getHeight() {
        return height;
    }

    public void setHeight(Integer height) {
        this.height = height;
    }

    public Integer getWidth() {
        return width;
    }

    public void setWidth(Integer width) {
        this.width = width;
    }
}
