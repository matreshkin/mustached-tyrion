package com.testapp.vknews.work;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import com.testapp.vknews.tools.*;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;

/**
 * Created by Vasiliy P.
 */
public class VkApi {

    private static final String TAG = VkApi.class.getSimpleName();

    private static final String API_PUBLIC_KEY = "4393263";
    //private static final String API_SECRET_KEY = "gzJYHBtD6VXmLTVB4CU1";
    private static final String API_SECRET_KEY = "";
    // wall + friends
    private static final String API_PERMISSIONS = "wall,friends";
    private static volatile VkApi sInstance = null;

    //////////// singleton
    //
    public static VkApi getInstance(Context c) {
        if (sInstance != null) return sInstance;
        return createInstance(c);
    }
    private static synchronized VkApi createInstance(Context c) {
        if (sInstance != null) return sInstance;
        return sInstance = new VkApi(c, API_PUBLIC_KEY, API_SECRET_KEY, API_PERMISSIONS);
    }
    //
    ///////////

    private final Context mContext;
    private final String mPublicKey;
    private final String mSecretKey;
    private final String mPermissions;

    private VkApi(Context context, String apiPublicKey, String apiSecretKey, String apiPermissions) {
        mContext = context;
        mPublicKey = apiPublicKey;
        mSecretKey = apiSecretKey;
        mPermissions = apiPermissions;
    }

    public void authorize(WebView webView, AuthWebViewListener l) {
        webView.setWebViewClient(new WebViewLoginClient(CONST.AUTH_REDIRECT_URL, l));
        webView.getSettings().setJavaScriptEnabled(true);
        HashMap<String, String> params = new HashMap<String, String>();
        params.put(PARAM.APP_ID, mPublicKey);
        params.put(PARAM.PERMISSIONS, mPermissions);
        params.put(PARAM.DISPLAY, CONST.AUTH_DISPLAY);
        params.put(PARAM.REDIRECT_URI, CONST.AUTH_REDIRECT_URL);
        params.put(PARAM.RESPONSE_TYPE, CONST.AUTH_TYPE);
        webView.loadUrl(getOauthUrl(params));
    }

    public JSONObject getNewsList(String startFrom, Integer count)
            throws ClientException, NetworkException, ServerException {
        HashMap<String, String> params = new HashMap<String, String>();
        params.put(PARAM.NEWS_FILTER, CONST.NEWS_FILTER_POST);
        if (count != null) {
            if (count < 1) count = 1;
            else if (count > CONST.COUNT_MAX) count = 100;
            params.put(PARAM.COUNT, String.valueOf(count));
        }
        if (startFrom != null) params.put(PARAM.NEWS_START_FROM, startFrom);
        String url = getUrl(COMMANDS.NEWS_GET, params);
        String str = Network.getStr(url, null);
        JSONObject obj;
        try {
            obj = new JSONObject(str);
        } catch (JSONException e) {
            throw new AnswerException("Invalid answer format", e);
        }
        return obj;
    }

    private String getOauthUrl(HashMap<String, String> params) {
        params.put(PARAM.API_VERSION, CONST.API_VERSION);
        return API_SCHEME + "://" + OAUTH_SERVER_ + "/" + COMMANDS.AUTH + "?" + Utils.joinQueryList(params);

    }
    private String getUrl(String cmd, HashMap<String, String> params) {
        params.put(PARAM.API_VERSION, CONST.API_VERSION);
        String token = AuthUtils.getInstance(mContext).getToken();
        if (token == null)  return null;
        params.put(PARAM.AUTH_TOKEN, token);
        return API_SCHEME + "://" + API_SERVER + "/" + cmd + "?" + Utils.joinQueryList(params);
    }

    public interface AuthWebViewListener {
        void onLoading();
        void onLoaded();
        void onFailed(String error, String massage);
        void onSucceeded(String token);
    }

    private class WebViewLoginClient extends WebViewClient {

        private AuthWebViewListener mListener;
        private String mResultRedirectUrl;

        public WebViewLoginClient(String redirectUrl, AuthWebViewListener l) {
            mResultRedirectUrl = redirectUrl;
            mListener = l;
        }
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            Log.d(TAG, "Redirect url: " + url);
            return false;
        }
        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            Log.d(TAG, "onPageStarted " + url);
            if (catchRedirect(url)) return;
            super.onPageStarted(view, url, favicon);
            if (mListener != null) mListener.onLoading();
        }
        private boolean catchRedirect(String url) {
            Log.d("catchRedirect", "url " + url);
            if (mResultRedirectUrl != null && url.contains(mResultRedirectUrl)) {
                Uri uri = Uri.parse(url);
                Uri uri2 = null;
                String fragment = uri.getFragment();
                if (fragment != null) {
                    uri2 = Uri.parse(mResultRedirectUrl + "?" + fragment);
                }
                String error = uri.getQueryParameter(RESULT.ERROR);
                if (error == null && uri2 != null) error = uri2.getQueryParameter(RESULT.ERROR);
                if (error != null) {
                    String msg = uri.getQueryParameter(RESULT.ERROR_MSG);
                    if (mListener != null) mListener.onFailed(error, msg);
                    return true;
                }
                String token = uri.getQueryParameter(RESULT.AUTH_TOKEN);
                if (token == null && uri2 != null) token = uri2.getQueryParameter(RESULT.AUTH_TOKEN);
                if (token != null) {
                    if (mListener != null) mListener.onSucceeded(token);
                    return true;
                }
            }
            return false;
        }
        @Override
        public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
            super.onReceivedError(view, errorCode, description, failingUrl);
            if (mListener != null) mListener.onFailed(String.valueOf(errorCode), description);
        }
        @Override
        public void onPageFinished(WebView view, String url) {
            Log.d(TAG, "onPageFinished " + url);
            super.onPageFinished(view, url);
            if (mListener != null) mListener.onLoaded();
        }
    }
    private static final String API_SCHEME = "https";
    private static final String OAUTH_SERVER_ = "oauth.vk.com";
    private static final String API_SERVER = "api.vk.com";
    private static class RESULT {
        static final String ERROR = "error";
        static final String ERROR_MSG = "error_description";
        static final String AUTH_TOKEN = "access_token";
    }
    private static class PARAM {
        static final String APP_ID = "client_id";
        static final String PERMISSIONS = "scope";
        static final String REDIRECT_URI = "redirect_uri";
        static final String DISPLAY = "display";
        static final String API_VERSION = "v";
        static final String RESPONSE_TYPE = "response_type";
        static final String AUTH_TOKEN = "access_token";

        static final String NEWS_FILTER = "filters";
        static final String NEWS_START_FROM = "start_from";
        static final String COUNT = "count";
    }
    private static class CONST {
        static final String API_VERSION = "5.21";
        static final String AUTH_REDIRECT_URL = "https://oauth.vk.com/blank.html";
        static final String AUTH_DISPLAY = "mobile";
        static final String AUTH_TYPE = "token";

        static final String NEWS_FILTER_POST = "post";
        public static final int COUNT_MAX = 100 ;
    }
    private static class COMMANDS {
        static final String AUTH = "authorize";
        static final String NEWS_GET = "method/newsfeed.get";
    }

}
