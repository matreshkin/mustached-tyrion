package com.testapp.vknews;

import android.content.Context;
import android.support.v4.app.Fragment;
import android.widget.AbsListView;
import android.widget.ListView;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.listener.PauseOnScrollListener;
import com.testapp.vknews.tools.Utils;

/**
 * Created by Vasiliy P.
 */
public class BaseFragment extends Fragment {

    protected void setOnScrollListener(final ListView listView,
                                       final ImageLoader imageLoader,
                                       final Runnable onEndScroll) {
        AbsListView.OnScrollListener onEndListListener = null;
        if (onEndScroll != null) {
            onEndListListener = new AbsListView.OnScrollListener() {
                int mScrollFirstVisibleItem = -1;
                int mScrollVisibleItemCount = -1;
                int mScrollTotalItemCount = -1;
                @Override
                public void onScrollStateChanged(AbsListView view, int scrollState) {
                    if (scrollState == SCROLL_STATE_IDLE && mScrollFirstVisibleItem > 0) {
                        boolean needLoadMore = ((mScrollFirstVisibleItem + mScrollVisibleItemCount) >=
                                mScrollTotalItemCount - (mScrollVisibleItemCount / 2 + 1));
                        if (needLoadMore) onEndScroll.run();
                    }
                }
                @Override
                public void onScroll(AbsListView view, int firstVisibleItem,
                                     int visibleItemCount, int totalItemCount) {
                    mScrollFirstVisibleItem = firstVisibleItem;
                    mScrollVisibleItemCount = visibleItemCount;
                    mScrollTotalItemCount = totalItemCount;
                }
            };
        }

        final AbsListView.OnScrollListener l = onEndListListener;

        Context context = getActivity();
        if (context == null) return;
        boolean pauseOnScroll = !Utils.isWifiConnected(context);
        boolean pauseOnFling = true;
        AbsListView.OnScrollListener listenerWrapper = new PauseOnScrollListener(
                imageLoader, pauseOnScroll, pauseOnFling) {

            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
                super.onScrollStateChanged(view, scrollState);
                if (l != null) l.onScrollStateChanged(view, scrollState);
            }
            @Override
            public void onScroll(AbsListView view, int firstVisibleItem,
                                 int visibleItemCount, int totalItemCount) {
                super.onScroll(view, firstVisibleItem, visibleItemCount, totalItemCount);
                if (l != null){
                    l.onScroll(view, firstVisibleItem, visibleItemCount, totalItemCount);
                }
            }

        };
        listView.setOnScrollListener(listenerWrapper);
    }

}
