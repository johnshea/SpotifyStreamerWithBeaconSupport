package com.example.android.SpotifyStreamerWithBeaconSupport;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.widget.LinearLayout;

import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

/**
 * Created by John on 6/29/2015.
 */
public class ArtistLinearLayout extends LinearLayout implements Target {

    public ArtistLinearLayout(Context context) {
        super(context);
    }

    public ArtistLinearLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ArtistLinearLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public void onBitmapFailed(Drawable errorDrawable) {

    }

    @Override
    public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
        Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap, 100, 100, false);
        Drawable background = new BitmapDrawable(getResources(), scaledBitmap);
        background.setAlpha(25);
//        ScaleDrawable scaleDrawable = new ScaleDrawable(background, 0x01 | 0x10, 10.0f, 10.0f);
//        Drawable littleBackground = scaleDrawable.getDrawable();
        this.setBackground(background);
//        this.setBackgroundColor(Color.BLUE);
    }

    @Override
    public void onPrepareLoad(Drawable placeHolderDrawable) {

    }


}
