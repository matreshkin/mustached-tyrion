package com.testapp.vknews.work;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import com.testapp.vknews.StartActivity;

import java.io.Serializable;

/**
 * Created by Vasiliy P.
 */

public class AuthUtils {

    public static final int ERROR_AUTH = 5;

    private static AuthUtils sInstance = null;

    public static synchronized AuthUtils getInstance(Context context) {
        if (sInstance == null) sInstance = new AuthUtils(context);
        return sInstance;
    }

    private Context mContext = null;
    private SharedPreferences mPrefs = null;

    public AuthUtils(Context context) {
        mContext  = context.getApplicationContext();
        mPrefs = PreferenceManager.getDefaultSharedPreferences(mContext);
    }

    public synchronized boolean isAuthorized() {
        String token = getToken();
        return (token != null);
    }

    public synchronized void setToken(String token) {
        if (token == null) {
            mPrefs.edit().remove(KEY.AUTH_TOKEN).commit();
            return;
        }
        mPrefs.edit().putString(KEY.AUTH_TOKEN, token).commit();
    }
    public synchronized String getToken() {
        String token = mPrefs.getString(KEY.AUTH_TOKEN, null);
        return token;
    }

    public void logout(Context windowContext, boolean startLogin) {
        CookieSyncManager.createInstance(mContext);
        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.removeAllCookie();
        setToken(null);
        NewsListUpdater.getInstance(mContext).clearData();
        if (startLogin && windowContext != null) {
            Intent i = new Intent(windowContext, StartActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            windowContext.startActivity(i);
        }
    }

    public static class Error implements Serializable {
        public Integer code = null;
        public String msg = null;
    }

    private static final class KEY {
        public static final String AUTH_TOKEN = "auth_token";

    }


}
