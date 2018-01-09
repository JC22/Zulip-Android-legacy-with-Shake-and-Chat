package com.zulip.android.networking;

import com.zulip.android.ZulipApp;


/**
 * Created by User on 2018/1/6.
 */

public class AsyncScoreSending extends ZulipAsyncPushTask{

    /**
     * Declares a new HumbugAsyncPushTask, passing the activity as context.
     *
     * @param app the zulip app god object
     */

    private String strangerMail;
    private String senderMail;
    private String score;
    private String comment;



    private static final String SENDEREMAIL = "sender_mail";
    private static final String STRANGERMAIL = "stranger_mail";
    private static final String SCORE = "score";
    private static final String COMMENT = "comment";


    public AsyncScoreSending(ZulipApp app,String strangerMail,String score,String comment) {
        super(app);
        this.strangerMail = strangerMail;
        this.score = score;
        this.comment = comment;
        this.senderMail = ZulipApp.get().getYou().getEmail();

    }

    public final void execute(){
        setProperty(SCORE, this.score);
        setProperty(COMMENT, this.comment);
        setProperty(SENDEREMAIL, senderMail);
        setProperty(STRANGERMAIL, strangerMail);

        execute("POST", "score/");
    }
}
