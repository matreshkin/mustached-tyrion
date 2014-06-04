package com.testapp.vknews;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.ContentObserver;
import android.os.Bundle;
import android.os.Handler;
import com.testapp.vknews.work.AuthUtils;
import com.testapp.vknews.work.NewsListUpdater;

public class StartActivity extends BaseActivity {

    public static final String EXTRA_LOGOUT = "EXTRA_LOGOUT";

    private SocialLoginDialog mAuthDialog = null;

    private boolean mNeedLogout = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSupportActionBar().hide();
        setContentView(R.layout.activity_start);
        if (getIntent() != null && getIntent().getExtras() != null) {
            mNeedLogout = getIntent().getExtras().getBoolean(EXTRA_LOGOUT, false);
        }
        auth();
    }

    @Override
    public void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    private void auth() {
        final AuthUtils auth = AuthUtils.getInstance(getBaseContext());
        if (auth.isAuthorized()) {
            start(auth);
        } else {
            if (mAuthDialog == null) {
                mAuthDialog = new SocialLoginDialog(this);
                mAuthDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialogInterface) {
                        mAuthDialog = null;
                        if (auth.isAuthorized()) {
                            start(auth);
                        } else {
                            error();
                        }
                    }
                });
                mAuthDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialogInterface) {
                        finish();
                    }
                });
                mAuthDialog.show();
            } else {
                finish();
            }
        }
    }

    private void error() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.title_activity_start)
                .setMessage(R.string.label_error)
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        auth();
                    }
                })
                .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                    }
                })
                .setIcon(R.drawable.ic_launcher)
                .show();
    }

    private void start(AuthUtils auth) {
        NewsListUpdater.getInstance(this).updateFull();
        getContentResolver().registerContentObserver(NewsListUpdater.URI_NEWS, true,
                new ContentObserver(new Handler()) {
                    @Override
                    public void onChange(boolean selfChange) {
                        startActivity(new Intent(StartActivity.this, NewsListActivity.class));
                        finish();
                        getContentResolver().unregisterContentObserver(this);
                    }
                }
        );
    }

    @Override
    public void onPause() {
        super.onPause();
//        if (mAuthDialog != null) {
//            mAuthDialog.dismiss();
//            mAuthDialog = null;
//        }
    }

}
