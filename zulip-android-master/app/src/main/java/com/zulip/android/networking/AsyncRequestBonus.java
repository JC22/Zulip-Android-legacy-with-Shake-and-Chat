package com.zulip.android.networking;

import android.location.Location;

import com.zulip.android.ZulipApp;
import com.zulip.android.activities.ZulipLocationManager;
import com.zulip.android.models.Person;

/**
 * Created by victordark on 2018/1/5.
 */

public class AsyncRequestBonus extends ZulipAsyncPushTask {

    /**
     * Declares a new HumbugAsyncPushTask, passing the activity as context.
     *
     * @param app the zulip app god object
     */

    private String strangerMail;
    private static final String SENDEREMAIL = "sender_mail";
    private static final String STRANGERMAIL = "stranger_mail";
    private static final String LONGITUDE = "longitude";
    private static final String LATITUDE = "latitude";


    public AsyncRequestBonus(ZulipApp app , String strangerMail) {
        super(app);
        this.strangerMail = strangerMail;
    }

    public final void execute(){
        Location location = ZulipLocationManager.getInstance().getCurrentLocation();
        double longitude = location.getLongitude();
        double latitude = location.getLatitude();
        String senderMail = ZulipApp.get().getYou().getEmail();

        setProperty(LONGITUDE, Double.toString(longitude));
        setProperty(LATITUDE, Double.toString(latitude));
        setProperty(SENDEREMAIL, senderMail);
        setProperty(STRANGERMAIL, strangerMail);



        //TODO: send json to backend to calculate distance
        execute("POST", "meet_check/");

    }
}
