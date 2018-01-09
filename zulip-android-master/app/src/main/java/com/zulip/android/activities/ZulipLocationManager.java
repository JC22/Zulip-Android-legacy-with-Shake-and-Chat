package com.zulip.android.activities;


import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;

/**
 * Created by Halley on 2018/1/2.
 */

public class ZulipLocationManager implements LocationListener {

    private static ZulipLocationManager instance;
    private Location currentLocation;

    public synchronized static ZulipLocationManager getInstance()
    {
        if (instance == null)
            instance = new ZulipLocationManager();
        return instance;
    }

    private ZulipLocationManager()
    {
        currentLocation = new Location("DEFAULT");
        currentLocation.setLongitude(0);
        currentLocation.setLatitude(0);
    }

    public Location getCurrentLocation()
    {
        return currentLocation;
    }

    @Override
    public void onLocationChanged(Location location)
    {
        this.currentLocation = location;
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {}

    @Override
    public void onProviderEnabled(String provider) {}

    @Override
    public void onProviderDisabled(String provider) {}

    public void setCurrentLocation(Location currentLocation)
    {
        this.currentLocation = currentLocation;
    }
}
