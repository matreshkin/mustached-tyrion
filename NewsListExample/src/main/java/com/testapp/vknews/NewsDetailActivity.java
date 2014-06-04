package com.testapp.vknews;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import com.testapp.vknews.work.NewsItem;


public class NewsDetailActivity extends ActionBarActivity {

    public static final String EXTRA_ITEM = "EXTRA_ITEM";

    private NewsItem mItem;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getIntent().getExtras() != null) {
            Object o = getIntent().getExtras().getSerializable(EXTRA_ITEM);
            if (o != null && o instanceof NewsItem) {
                mItem = (NewsItem) o;
            }
        }
        if (mItem == null) {
            finish();
            return;
        }
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        setContentView(R.layout.activity_news_detail);
    }

    @Override
    public void onPostResume() {
        super.onPostResume();
        Fragment fragment = getSupportFragmentManager().findFragmentByTag(NewsDetailFragment.FRAGMENT_TAG);
        if (fragment == null) {
            Bundle args = new Bundle();
            args.putSerializable(NewsDetailFragment.ARG_ITEM, mItem);
            fragment = NewsDetailFragment.newInstance(mItem);
            getSupportFragmentManager().beginTransaction().add(R.id.fragment_holder,
                    fragment, NewsDetailFragment.FRAGMENT_TAG).commit();
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return false;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

}
