package com.testapp.vknews;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.FrameLayout;
import com.testapp.vknews.work.NewsItem;


public class NewsListActivity extends BaseActivity implements NewsListFragment.NewsListEventListener {

    FrameLayout mFragmentHolder = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_news_list);
        mFragmentHolder = (FrameLayout) findViewById(R.id.fragment_holder);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.news_list, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_logout) {
            logout();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }


    @Override
    public boolean onItemClicked(NewsItem item) {
        if (mFragmentHolder == null) {
            Intent intent = new Intent(this, NewsDetailActivity.class);
            intent.putExtra(NewsDetailActivity.EXTRA_ITEM, item);
            startActivity(intent);
        } else {
            Bundle args = new Bundle();
            args.putSerializable(NewsDetailFragment.ARG_ITEM, item);
            NewsDetailFragment fragment = NewsDetailFragment.newInstance(item);
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_holder, fragment, NewsDetailFragment.FRAGMENT_TAG)
                    .setCustomAnimations(R.anim.in_animation, R.anim.out_animation,
                            R.anim.in_reverse_animation, R.anim.out_reverse_animation)
                    .addToBackStack(null)
                    .commit();
        }
        return true;
    }
}
