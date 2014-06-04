package com.testapp.vknews;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.widget.Toast;
import com.testapp.vknews.work.AuthUtils;
import com.testapp.vknews.work.NewsListUpdater;

/**
 * Created by Vasiliy P.
 */
public class BaseActivity extends ActionBarActivity {

    private BroadcastReceiver mErrorReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getExtras() == null) return;
            Object o = intent.getExtras().get(NewsListUpdater.EXTRA_ERROR);
            if (o != null && o instanceof AuthUtils.Error) {
                AuthUtils.Error e = (AuthUtils.Error)o;
                if (e.code == AuthUtils.ERROR_AUTH) {
                    logout();
                } else {
                    Toast.makeText(BaseActivity.this, e.msg, Toast.LENGTH_LONG).show();
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Application.isTablet()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
            } else {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            }
        }
    }

    protected void logout() {
        AuthUtils auth = AuthUtils.getInstance(this);
        auth.logout(this, true);
        finish();
    }

    @Override
    public void onResume() {
        super.onResume();
        registerReceiver(mErrorReceiver, new IntentFilter(NewsListUpdater.ACTION_ERROR));
    }

    @Override
    public void onPause() {
        super.onPause();;
        unregisterReceiver(mErrorReceiver);
    }
}
