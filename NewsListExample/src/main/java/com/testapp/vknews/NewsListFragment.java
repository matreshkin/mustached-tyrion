package com.testapp.vknews;

import android.app.Activity;
import android.content.Context;
import android.database.ContentObserver;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import com.handmark.pulltorefresh.library.PullToRefreshBase;
import com.handmark.pulltorefresh.library.PullToRefreshListView;
import com.nhaarman.listviewanimations.swinginadapters.prepared.SwingBottomInAnimationAdapter;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.testapp.vknews.work.Database;
import com.testapp.vknews.work.NewsItem;
import com.testapp.vknews.work.NewsListUpdater;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class NewsListFragment extends BaseFragment {

    private static final String STATE_LIST_INDEX = "STATE_LIST_INDEX";
    private static final String STATE_LIST_OFFSET = "STATE_LIST_OFFSET";

    private PullToRefreshListView mListViewNews;
    private NewsListAdapter mAdapter = new NewsListAdapter();
    private CursorLoaderTask mLoadTask = null;
    private ContentObserver mChangesObserver = null;
    private NewsListUpdater mUpdater = null;
    private ImageLoader mImageLoader = ImageLoader.getInstance();
    private DisplayImageOptions mImageLoaderOptions = new DisplayImageOptions.Builder()
            .showImageOnLoading(R.drawable.ic_launcher)
            .cacheInMemory(true)
            .cacheOnDisc(true)
            .bitmapConfig(Bitmap.Config.RGB_565)
            .build();

    private int mListIndex = -1;
    private int mListOffset = -1;

    public NewsListFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_news_list, container, false);
        mListViewNews = (PullToRefreshListView) rootView.findViewById(R.id.list_news);
        SwingBottomInAnimationAdapter swingBottomInAnimationAdapter = new SwingBottomInAnimationAdapter(mAdapter);
        swingBottomInAnimationAdapter.setAnimationDurationMillis(300);
        swingBottomInAnimationAdapter.setAbsListView(mListViewNews.getRefreshableView());
        mListViewNews.setAdapter(swingBottomInAnimationAdapter);
        mListViewNews.setShowIndicator(false);
        mListViewNews.setOnLastItemVisibleListener(new PullToRefreshBase.OnLastItemVisibleListener() {
            @Override
            public void onLastItemVisible() {
                if (mUpdater == null) return;
                mUpdater.updateOlder();
            }
        });
        mListViewNews.setOnRefreshListener(new PullToRefreshListView.OnRefreshListener<ListView>() {
            public void onRefresh(PullToRefreshBase<ListView> refreshView) {
                if (mUpdater == null) return;
                mUpdater.updateFull();
            }
        });
        // sets onFling/onScroll Pausing
        setOnScrollListener(mListViewNews.getRefreshableView(), mImageLoader,
                new Runnable() {
                    @Override
                    public void run() {
                        if (mUpdater == null) return;
                        mUpdater.updateOlder();
                    }
                }
        );
        mListViewNews.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Activity a = getActivity();
                if (a != null && a instanceof NewsListEventListener) {
                    NewsListEventListener controller = (NewsListEventListener) a;
                    boolean select = controller.onItemClicked(
                            (NewsItem) mListViewNews.getRefreshableView().getItemAtPosition(i));
                    if (select) {
                        // TODO
                    }
                }
            }
        });
        if (savedInstanceState != null) {
            mListIndex = savedInstanceState.getInt(STATE_LIST_INDEX);
            mListOffset = savedInstanceState.getInt(STATE_LIST_OFFSET);
        }
        return rootView;
    }

    @Override
    public void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);
        // save index and top position
        if (mListViewNews.getRefreshableView().getChildCount() > 0) {
            mListIndex = mListViewNews.getRefreshableView().getFirstVisiblePosition();
            View v = mListViewNews.getRefreshableView().getChildAt(0);
            mListOffset = (v == null) ? 0 : v.getTop();
        } else {
            mListIndex = -1;
        }
        out.putInt(STATE_LIST_INDEX, mListIndex);
        out.putInt(STATE_LIST_OFFSET, mListOffset);
    }

    @Override
    public void onPause() {
        super.onPause();
        getActivity().getBaseContext().getContentResolver().unregisterContentObserver(mChangesObserver);
        stop();
    }

    @Override
    public void onResume() {
        super.onResume();
        mChangesObserver = new ContentObserver(new Handler()) {
            @Override
            public void onChange(boolean selfChange) {
                start();
            }
        };
        getActivity().getBaseContext().getContentResolver().registerContentObserver(
                NewsListUpdater.URI_NEWS, true, mChangesObserver);
        start();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mAdapter.setCursor(null);
    }

    public void start() {
        if (mLoadTask != null) mLoadTask.cancel(false);
        (mLoadTask = new CursorLoaderTask()).execute();
    }

    public void stop() {
        if (mLoadTask != null) mLoadTask.cancel(false);
        mLoadTask = null;
    }

    public void loaded(Cursor c) {
        mLoadTask = null;
        mAdapter.setCursor(c);
        mListViewNews.onRefreshComplete();
        if (mListIndex >= 0) {
            mListViewNews.getRefreshableView().setSelectionFromTop(mListIndex, mListOffset);
            mListIndex = -1;
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mUpdater = NewsListUpdater.getInstance(activity);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mUpdater = null;
    }


    private class CursorLoaderTask extends AsyncTask<Void, Void, Cursor> {

        private Context mContext = getActivity();

        @Override
        protected void onPreExecute() {
            if (mContext == null) cancel(false);
        }

        @Override
        protected Cursor doInBackground(Void... voids) {
            if (isCancelled()) return null;
            return Database.getInstance(mContext).query(Database.Tables.NEWS_VIEW,
                    new String[]{"*"},
                    null, null, null);
        }

        @Override
        protected void onPostExecute(Cursor result) {
            if (isCancelled()) return;
            loaded(result);
        }
    }

    private static final int TYPE_1HEADER = 0;
    private static final int TYPE_2HEADERS = 1;
    private static final int TYPE_3HEADERS = 2;
    private static final int TYPE_1HEADER_IMAGE = 3;
    private static final int TYPE_2HEADERS_IMAGE = 4;
    private static final int TYPE_3HEADERS_IMAGE = 5;
    private static final int TYPE_COUNT = 6;

    private class NewsListAdapter extends BaseAdapter implements View.OnClickListener {

        private Cursor mCursor = null;
        HashMap<Integer, NewsItem> mHashItems = new HashMap<Integer, NewsItem>();

        @Override
        public int getCount() {
            if (mCursor == null) return 0;
            return mCursor.getCount();
        }

        @Override
        public int getViewTypeCount() {
            return TYPE_COUNT;
        }

        @Override
        public int getItemViewType(int i) {
            NewsItem item = getNewsItem(i);
            int historyCount = 0;
            int imageCount = 0;
            if (item.history != null) historyCount = item.history.size();
            if (item.images != null && item.images.size() > 0) imageCount = 1;
            return historyCount + 3 * imageCount; // :)
        }

        @Override
        public Object getItem(int i) {
            return getNewsItem(i);
        }


        @Override
        public long getItemId(int i) {
            return getNewsItem(i).id;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            if (view == null) view = getActivity().getLayoutInflater().inflate(R.layout.item_news, null);
            NewsItem item = getNewsItem(i);
            int historyCount = 0;
            int imageCount = 0;
            if (item.history != null) historyCount = item.history.size();
            if (item.images != null && item.images.size() > 0) imageCount = 1;
            view.findViewById(R.id.sender1).setVisibility(View.GONE);
            view.findViewById(R.id.sender2).setVisibility(View.GONE);
            view.findViewById(R.id.image).setVisibility(View.GONE);
            if (historyCount == 1) {
                View p1View = view.findViewById(R.id.sender1);
                p1View.setVisibility(View.VISIBLE);
                fillSender(p1View, item.history.get(0));
            }
            if (historyCount == 2) {
                View p2View = view.findViewById(R.id.sender1);
                p2View.setVisibility(View.VISIBLE);
                fillSender(p2View, item.history.get(1));
            }
            View sender = view.findViewById(R.id.sender);
            fillSender(sender, item);
            if (imageCount > 0) {
                view.findViewById(R.id.image).setVisibility(View.VISIBLE);
                ImageView imageView = (ImageView) view.findViewById(R.id.image);
                mImageLoader.displayImage(item.images.get(0), imageView, mImageLoaderOptions);
            }
            TextView likes = (TextView) view.findViewById(R.id.label_likes);
            TextView reposts = (TextView) view.findViewById(R.id.label_reposts);
            likes.setText(String.valueOf(item.likesCount == null ? 0 : item.likesCount));
            likes.setTextColor((item.userLikes == null || !item.userLikes) ? Color.GRAY : Color.BLUE);
            likes.setEnabled(item.canLike != null && item.canLike);
            reposts.setText(String.valueOf(item.repostsCount == null ? 0 : item.repostsCount));
            reposts.setEnabled(item.canLike != null && item.canLike);
            likes.setOnClickListener(this);
            reposts.setOnClickListener(this);
            return view;
        }

        private void fillSender(View view, NewsItem item) {
            ImageView photo = (ImageView) view.findViewById(R.id.photo);
            TextView title = (TextView) view.findViewById(R.id.name);
            TextView text = (TextView) view.findViewById(R.id.text);
            mImageLoader.displayImage(item.senderPhotoMini, photo, mImageLoaderOptions);
            title.setText(item.senderName == null ? "" : item.senderName);
            text.setText(item.text == null ? "" : item.text);
        }

        private NewsItem getNewsItem(int i) {
            if (mHashItems.containsKey(i)) return mHashItems.get(i);
            mCursor.moveToPosition(i);
            NewsItem item = new NewsItem();
            item.id = getLong(Database.Tables.News.ID);
            item.date = getLong(Database.Tables.News.DATE);
            item.type = getString(Database.Tables.News.TYPE);
            item.profileId = getLong(Database.Tables.News.PROFILE_ID);
            item.groupId = getLong(Database.Tables.News.GROUP_ID);
            item.postId = getString(Database.Tables.News.POST_ID);
            item.postType = getString(Database.Tables.News.POST_TYPE);
            item.text = getString(Database.Tables.News.TEXT);
            item.commentsCount = getInt(Database.Tables.News.COMMENTS_COUNT);
            item.likesCount = getInt(Database.Tables.News.LIKES_COUNT);
            item.userLikes = getBoolean(Database.Tables.News.USER_LIKES);
            item.canLike = getBoolean(Database.Tables.News.CAN_LIKE);
            item.repostsCount = getInt(Database.Tables.News.REPOSTS_COUNT);
            String images = getString(Database.Tables.News.IMAGE_URL);
            if (images != null) {
                List<String> list = Arrays.asList(images.split("\\n"));
                if (list.size() > 0) item.images = list;
            }
            item.senderName = getString(Database.Tables.News.SENDER_NAME);
            item.senderPhotoMini = getString(Database.Tables.News.SENDER_PHOTO_MINI);
            Long[] parents = new Long[]{
                    getLong(Database.Tables.NewsParent1.ID), getLong(Database.Tables.NewsParent2.ID)
            };
            if (parents[0] != null) {
                NewsItem p1 = new NewsItem();
                p1.id = getLong(Database.Tables.NewsParent1.ID);
                p1.date = getLong(Database.Tables.NewsParent1.DATE);
                p1.type = getString(Database.Tables.NewsParent1.TYPE);
                p1.profileId = getLong(Database.Tables.NewsParent1.PROFILE_ID);
                p1.groupId = getLong(Database.Tables.NewsParent1.GROUP_ID);
                p1.postId = getString(Database.Tables.NewsParent1.POST_ID);
                p1.postType = getString(Database.Tables.NewsParent1.POST_TYPE);
                p1.text = getString(Database.Tables.NewsParent1.TEXT);
                p1.commentsCount = getInt(Database.Tables.NewsParent1.COMMENTS_COUNT);
                p1.likesCount = getInt(Database.Tables.NewsParent1.LIKES_COUNT);
                p1.userLikes = getBoolean(Database.Tables.NewsParent1.USER_LIKES);
                p1.canLike = getBoolean(Database.Tables.NewsParent1.CAN_LIKE);
                p1.repostsCount = getInt(Database.Tables.NewsParent1.REPOSTS_COUNT);
                images = getString(Database.Tables.NewsParent1.IMAGE_URL);
                if (images != null) {
                    List<String> list = Arrays.asList(images.split("\\n"));
                    if (list.size() > 0) p1.images = list;
                }
                p1.senderName = getString(Database.Tables.NewsParent1.SENDER_NAME);
                p1.senderPhotoMini = getString(Database.Tables.NewsParent1.SENDER_PHOTO_MINI);
                if (item.history == null) item.history = new ArrayList<NewsItem>();
                item.history.add(p1);
            }
            if (parents[1] != null) {
                NewsItem p2 = new NewsItem();
                p2.id = getLong(Database.Tables.NewsParent2.ID);
                p2.date = getLong(Database.Tables.NewsParent2.DATE);
                p2.type = getString(Database.Tables.NewsParent2.TYPE);
                p2.profileId = getLong(Database.Tables.NewsParent2.PROFILE_ID);
                p2.groupId = getLong(Database.Tables.NewsParent2.GROUP_ID);
                p2.postId = getString(Database.Tables.NewsParent2.POST_ID);
                p2.postType = getString(Database.Tables.NewsParent2.POST_TYPE);
                p2.text = getString(Database.Tables.NewsParent2.TEXT);
                p2.commentsCount = getInt(Database.Tables.NewsParent2.COMMENTS_COUNT);
                p2.likesCount = getInt(Database.Tables.NewsParent2.LIKES_COUNT);
                p2.userLikes = getBoolean(Database.Tables.NewsParent2.USER_LIKES);
                p2.canLike = getBoolean(Database.Tables.NewsParent2.CAN_LIKE);
                p2.repostsCount = getInt(Database.Tables.NewsParent2.REPOSTS_COUNT);
                images = getString(Database.Tables.NewsParent2.IMAGE_URL);
                if (images != null) {
                    List<String> list = Arrays.asList(images.split("\\n"));
                    if (list.size() > 0) p2.images = list;
                }
                p2.senderName = getString(Database.Tables.NewsParent2.SENDER_NAME);
                p2.senderPhotoMini = getString(Database.Tables.NewsParent2.SENDER_PHOTO_MINI);
                if (item.history == null) item.history = new ArrayList<NewsItem>();
                item.history.add(p2);
            }
            if (item.history != null) {
                if (item.images == null || item.images.size() == 0) {
                    for (NewsItem it : item.history) {
                        if (it.images != null && it.images.size() > 0) {
                            item.images = it.images;
                            break;
                        }
                    }
                }
            }
            mHashItems.put(i, item);
            return item;
        }

        private Long getLong(String column) {
            int index = mCursor.getColumnIndex(column);
            if (mCursor.isNull(index)) return null;
            return mCursor.getLong(index);
        }

        private Integer getInt(String column) {
            int index = mCursor.getColumnIndex(column);
            if (mCursor.isNull(index)) return null;
            return mCursor.getInt(index);
        }

        private Boolean getBoolean(String column) {
            int index = mCursor.getColumnIndex(column);
            if (mCursor.isNull(index)) return null;
            return mCursor.getInt(index) > 0;
        }

        private String getString(String column) {
            int index = mCursor.getColumnIndex(column);
            if (mCursor.isNull(index)) return null;
            String s = mCursor.getString(index);
            if (s != null && s.trim().length() == 0) s = null;
            return s;
        }

        public void setCursor(Cursor c) {
            if (mCursor != null) mCursor.close();
            mCursor = c;
            mHashItems = new HashMap<Integer, NewsItem>();
            notifyDataSetChanged();
        }

        @Override
        public void onClick(View view) {
            // not implemented
        }
    }

    interface NewsListEventListener {
        boolean onItemClicked(NewsItem item);
    }


}
