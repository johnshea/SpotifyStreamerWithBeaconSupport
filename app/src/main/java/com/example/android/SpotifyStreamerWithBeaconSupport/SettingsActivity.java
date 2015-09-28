package com.example.android.SpotifyStreamerWithBeaconSupport;

import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;

/**
 * Created by John on 9/7/2015.
 */
public class SettingsActivity extends ActionBarActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        getFragmentManager().beginTransaction()
                .replace(R.id.settings_container, new SettingsFragment())
                .commit();
    }
}
