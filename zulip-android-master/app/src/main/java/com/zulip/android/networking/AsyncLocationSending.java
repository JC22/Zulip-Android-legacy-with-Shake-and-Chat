package com.zulip.android.networking;

import android.location.Location;

import com.zulip.android.ZulipApp;
import com.zulip.android.activities.ZulipLocationManager;

/**
 * Created by Halley on 2017/12/28.
 */

public class AsyncLocationSending extends ZulipAsyncPushTask {

    private static final String TIMESTAMP = "timestamp";
    private static final String LONGITUDE = "longitude";
    private static final String LATITUDE = "latitude";
    private static final String EMAIL = "sender_email";

    private String myMail = null;
    private String timeStamp = null;
    private ZulipLocationManager locationManager;
    /**
     * Declares a new HumbugAsyncPushTask, passing the activity as context.
     *
     * @param app the zulip app god object
     */
    public AsyncLocationSending(ZulipApp app)
    {
        super(app);
        locationManager = ZulipLocationManager.getInstance();
        myMail = ZulipApp.get().getYou().getEmail();
    }

    public final void execute()
    {
        // timestamp fixed
        // mail

        Location location = locationManager.getCurrentLocation();
        String longitude;
        String latitude;
        if (location == null){
            longitude = "0";
            latitude = "0";
        }
        else{
            longitude = Double.toString(location.getLongitude());
            latitude = Double.toString(location.getLatitude());
        }

        setProperty(TIMESTAMP, timeStamp);
        setProperty(LONGITUDE, longitude);
        setProperty(LATITUDE, latitude);
        setProperty(EMAIL, myMail);

        execute("POST", "geo_chat/");
    }

    public void setTimeStamp(String timeStamp)
    {
        this.timeStamp = timeStamp;
    }
}
