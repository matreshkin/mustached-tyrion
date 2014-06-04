package com.testapp.vknews;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.webkit.WebView;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;
import com.testapp.vknews.tools.Log;
import com.testapp.vknews.work.AuthUtils;
import com.testapp.vknews.work.VkApi;


public class SocialLoginDialog extends Dialog implements VkApi.AuthWebViewListener {
    private static final FrameLayout.LayoutParams FILL = new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT);

    private View mProgressView;
    private WebView mBrowser;
    private FrameLayout mContent;
    private VkApi mApi;

    public SocialLoginDialog(Context context) {
        super(context, android.R.style.Theme_Translucent_NoTitleBar);
        mApi = VkApi.getInstance(context);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        mContent = new FrameLayout(getContext());
        setUpWebView(30);
        addContentView(mContent, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
    }

    @Override
    public void show() {
        super.show();
        mBrowser.setVisibility(View.INVISIBLE);
        mProgressView.setVisibility(View.VISIBLE);
        mApi.authorize(mBrowser, this);
    }

    @Override
    public void cancel() {
        super.cancel();
        free();
    }
    @Override
    public void dismiss() {
        try {
            super.dismiss();
        } catch (Exception e) {
            // http://stackoverflow.com/questions/2745061/java-lang-illegalargumentexception-view-not-attached-to-window-manager
            Log.e(e);
        }
        free();
    }

    private void free() {
        try {
            if (mBrowser != null) {
                Log.d("SocialLoginDialog", "Destroy browser");
                if (Build.VERSION.SDK_INT < 19) //noinspection deprecation
                    mBrowser.freeMemory();
                mBrowser.destroy();
            }
        } catch (Exception e) {
            Log.e(e);
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void setUpWebView(int margin) {
        LinearLayout webViewContainer = new LinearLayout(getContext());
        mBrowser = new WebView(getContext());
        mBrowser.setVerticalScrollBarEnabled(false);
        mBrowser.setHorizontalScrollBarEnabled(false);
        mBrowser.getSettings().setJavaScriptEnabled(true);
        mBrowser.setLayoutParams(FILL);
        webViewContainer.setPadding(margin, margin, margin, margin);
        webViewContainer.addView(mBrowser);
        mContent.addView(webViewContainer);
        mProgressView = createProgressView();
        mContent.addView(mProgressView);
    }

    private View createProgressView() {
        FrameLayout layout = new FrameLayout(getContext());
        layout.setLayoutParams(new FrameLayout.LayoutParams(
                LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        ProgressBar spinner = new ProgressBar(getContext());
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, Gravity.CENTER);
        layout.addView(spinner, lp);
        // to avoid panic clicking
        layout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // just do nothing
                Log.d("is everything all right?", "enjoy the day!");
            }
        });
        return layout;
    }

    @Override
    public void onLoading() {
        mProgressView.setVisibility(View.VISIBLE);
    }

    @Override
    public void onLoaded() {
        mProgressView.setVisibility(View.GONE);
        mBrowser.setVisibility(View.VISIBLE);
    }

    @Override
    public void onFailed(String error, String message) {
        Toast.makeText(getContext(), message, Toast.LENGTH_LONG);
        dismiss();
    }

    @Override
    public void onSucceeded(String token) {
        mBrowser.clearCache(true);
        mBrowser.clearFormData();
        mBrowser.clearFormData();
        AuthUtils auth = AuthUtils.getInstance(getContext());
        auth.setToken(token);
        dismiss();
    }
}

