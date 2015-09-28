package com.example.android.SpotifyStreamerWithBeaconSupport;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;

import com.example.android.SpotifyStreamerWithBeaconSupport.service.BeaconScanningService;

/**
 * Created by John on 9/7/2015.
 */
public class SettingsFragment extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener {

    SharedPreferences sharedPref;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);

        sharedPref = PreferenceManager.getDefaultSharedPreferences(getActivity());
        sharedPref.registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onDetach() {
        sharedPref.unregisterOnSharedPreferenceChangeListener(this);
        super.onDetach();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {

        if ( key.equals("pref_enable_beacon_scanning") ) {

            Intent intent = new Intent(getActivity(), BeaconScanningService.class);

            if ( sharedPreferences.getBoolean("pref_enable_beacon_scanning", false) ) {

                String beaconType = sharedPreferences.getString("pref_beacon_type", "simulated");

                intent.putExtra("beaconType", beaconType);

                getActivity().startService(intent);
            } else {
                getActivity().stopService(intent);
            }
        }
    }

}
