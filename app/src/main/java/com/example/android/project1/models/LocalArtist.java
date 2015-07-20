package com.example.android.project1.models;

import java.util.ArrayList;

/**
 * Created by John on 7/3/2015.
 */
public class LocalArtist {
    public String id;
    public String name;
    public String url;
    public ArrayList<LocalImage> artistImages;

    private Integer smallestImageIndex;
    private Integer largestImageIndex;

    public LocalArtist(String id, String name, ArrayList<LocalImage> artistImages) {
        this.id = id;
        this.name = name;
        this.artistImages = artistImages;

        smallestImageIndex = 0;
        largestImageIndex = 0;

        calculateSmallestAndLargestImage();

    }

    private void calculateSmallestAndLargestImage() {

        Integer currentSmallestIndex;
        Integer currentSmallestWidth;

        Integer currentLargestIndex;
        Integer currentLargestWidth;

        if ( this.artistImages.size() > 0 ) {

            currentSmallestIndex = 0;
            currentSmallestWidth = this.artistImages.get(0).width;

            currentLargestIndex = 0;
            currentLargestWidth = this.artistImages.get(0).width;

            for(int i = 0; i < this.artistImages.size(); i++) {

                LocalImage currentImage = this.artistImages.get(i);

                // Determine smallest image
                if ( currentImage.width < currentSmallestWidth ) {
                    currentSmallestIndex = i;
                    currentSmallestWidth = this.artistImages.get(i).width;
                }

                // Determine largest image
                if ( currentImage.width > currentLargestWidth ) {
                    currentLargestIndex = i;
                    currentLargestWidth = this.artistImages.get(i).width;
                }
            }

            smallestImageIndex = currentSmallestIndex;
            largestImageIndex = currentLargestIndex;
        }

    }

    public String getThumbnailUrl() {
        return this.artistImages.get(smallestImageIndex).url;
    }

    public String getLargestImageUrl() {
        String url = "";

        if (artistImages.size() == 0) {
            url = "";
        } else {
            url =  this.artistImages.get(largestImageIndex).url;
        }

        return url;
    }

}
