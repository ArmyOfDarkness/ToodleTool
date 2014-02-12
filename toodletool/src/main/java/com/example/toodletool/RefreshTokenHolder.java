package com.example.toodletool;

import android.content.Context;
import android.content.SharedPreferences;
import org.scribe.exceptions.OAuthException;
import org.scribe.model.Token;
import org.scribe.utils.OAuthEncoder;

import java.sql.Ref;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by brendan on 2/3/14.
 */
public class RefreshTokenHolder {
    private String refreshTokenSecret;
    private boolean useAccess = false;
    private boolean useRefresh = false;

    private static RefreshTokenHolder INSTANCE = new RefreshTokenHolder();



    public static RefreshTokenHolder getInstance() {
        return INSTANCE;
    }

    public void setAccessStatus(boolean b) {
        useAccess = b;
    }

    public boolean getAccessStatus() {
        return useAccess;
    }

    public void setRefreshStatus(boolean b) {
        useRefresh = b;
    }
    public boolean getRefreshStatus() {
        return useRefresh;
    }

    public String getSecret() {
        return refreshTokenSecret;
    }

    public void extractToken(String response) {
        Matcher matcher = Pattern.compile("\"refresh_token\":\"([^&\"]+)\"").matcher(response);
        if (matcher.find()) {
            refreshTokenSecret = OAuthEncoder.decode(matcher.group(1));
            //MainActivity.editor.putString("SHARED_REFRESH_SECRET", refreshTokenSecret).commit();
        } else {
            throw new OAuthException("Response body is incorrect. Can't extract a token from this: '" + response + "'", null);
        }
    }

}
