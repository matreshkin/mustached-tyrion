package com.testapp.vknews;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.app.Fragment;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.testapp.vknews.work.NewsItem;

import java.util.List;


public class NewsDetailFragment extends Fragment implements View.OnClickListener {

    private static final String STATE_SCROLL_X = "STATE_SCROLL_X";
    private static final String STATE_SCROLL_Y = "STATE_SCROLL_Y";
    private static final String STATE_PAGER_POS = "STATE_PAGER_POS";

    public static final String ARG_ITEM = "ARG_ITEM";
    static final String FRAGMENT_TAG = "news_details";

    private NewsItem mItem = null;

    private ImageLoader mImageLoader = ImageLoader.getInstance();
    private DisplayImageOptions mImageLoaderOptions = new DisplayImageOptions.Builder()
            .showImageOnLoading(R.drawable.ic_launcher)
            .cacheInMemory(true)
            .cacheOnDisc(true)
            .bitmapConfig(Bitmap.Config.RGB_565)
            .build();
    private int mScrollX = -1;
    private int mScrollY = -1;
    private ScrollView mScrollView;
    private int mPagerPos = -1;
    private ViewPager mViewPager;

    public static NewsDetailFragment newInstance(NewsItem item) {
        NewsDetailFragment fragment = new NewsDetailFragment();
        Bundle args = new Bundle();
        args.putSerializable(ARG_ITEM, item);
        fragment.setArguments(args);
        return fragment;
    }

    public NewsDetailFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            Object o = getArguments().getSerializable(ARG_ITEM);
            if (o != null && o instanceof NewsItem)
                mItem = (NewsItem) o;
        }
    }

    private void loadDetails() {
        if (mItem.history != null) {
            if (mItem.text == null || mItem.text.trim().length() == 0) {
                for (NewsItem i : mItem.history) {
                    if (i.text != null && i.text.trim().length() > 0) {
                        mItem.text = i.text;
                        break;
                    }
                }
            }
            if (mItem.images == null || mItem.images.size() == 0) {
                for (NewsItem i : mItem.history) {
                    if (i.images != null && i.images.size() > 0) {
                        mItem.images = i.images;
                        break;
                    }
                }
            }
        }
        // TODO
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_news_detail, container, false);
        mViewPager = (ViewPager) view.findViewById(R.id.images);
        if (mItem != null) {
            loadDetails();
            fillView(view);
        }
        mScrollView = (ScrollView) view.findViewById(R.id.scroll);
        if (savedInstanceState != null) {
            mScrollX = savedInstanceState.getInt(STATE_SCROLL_X);
            mScrollY = savedInstanceState.getInt(STATE_SCROLL_Y);
            if (mScrollX  >= 0)
                mScrollView.post(new Runnable() {
                    public void run() {
                        mScrollView.scrollTo(mScrollX, mScrollY);
                        mScrollX = -1;
                        mScrollY = -1;
                    }
                });
            mPagerPos = savedInstanceState.getInt(STATE_PAGER_POS);
            if (mPagerPos >= 0) {
                mViewPager.post(new Runnable() {
                    @Override
                    public void run() {
                        if (mViewPager.getAdapter().getCount() > mPagerPos)
                            mViewPager.setCurrentItem(mPagerPos);
                        mPagerPos = -1;
                    }
                });
            }
        }
        return view;
    }

    @Override
    public void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);
        mScrollX =  mScrollView.getScrollX();
        mScrollY =  mScrollView.getScrollY();
        out.putInt(STATE_SCROLL_X, mScrollX);
        out.putInt(STATE_SCROLL_Y, mScrollY);
        out.putInt(STATE_PAGER_POS, mViewPager.getCurrentItem());
    }

    public View fillView(View view) {
        NewsItem item = mItem;
        int historyCount = 0;
        int imageCount = 0;
        if (item.history != null) historyCount = item.history.size();
        if (item.images != null) imageCount = item.images.size();
        view.findViewById(R.id.sender1).setVisibility(View.GONE);
        view.findViewById(R.id.sender2).setVisibility(View.GONE);
        view.findViewById(R.id.images).setVisibility(View.GONE);
        if (historyCount == 1) {
            View p1View = view.findViewById(R.id.sender1);
            p1View.setVisibility(View.VISIBLE);
            fillSender(p1View, item.history.get(0));
        }
        if (historyCount == 2) {
            View p2View = view.findViewById(R.id.sender2);
            p2View.setVisibility(View.VISIBLE);
            fillSender(p2View, item.history.get(1));
        }
        View sender = view.findViewById(R.id.sender);
        fillSender(sender, item);
        if (imageCount > 0) {
            view.findViewById(R.id.images).setVisibility(View.VISIBLE);
            ViewPager pager = (ViewPager) view.findViewById(R.id.images);
            pager.setAdapter(new PreviewAdapter(pager, item.images));
        }
        TextView text = (TextView) view.findViewById(R.id.text_full);
        if (item.text != null && item.text.trim().length() > 0) {
            text.setVisibility(View.VISIBLE);
            text.setText(item.text);
        } else {
            text.setVisibility(View.GONE);
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
        TextView text =  (TextView) view.findViewById(R.id.text);
        text.setVisibility(View.GONE);
        mImageLoader.displayImage(item.senderPhotoMini, photo, mImageLoaderOptions);
        title.setText(item.senderName == null ? "" : item.senderName);
    }

    @Override
    public void onClick(View view) {
        // TODO
    }

    class PreviewAdapter extends PagerAdapter {

        private ViewPager mViewPager;
        List<String> mImages;

        public PreviewAdapter(ViewPager viewPager, List<String> images) {
            mViewPager = viewPager;
            mImages = images;
        }

        @Override
        public Object instantiateItem(ViewGroup collection, int position) {
            Activity a = getActivity();
            if (a == null) return null;
            ImageView imageView = new ImageView(a);
            imageView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
            mImageLoader.displayImage(mImages.get(position), imageView, mImageLoaderOptions);
            collection.addView(imageView, 0);
            return imageView;
        }

        @Override
        public void destroyItem(ViewGroup collection, int position, Object view) {
            collection.removeView((View) view);
        }

        @Override
        public int getCount() {
            return mImages.size();
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view.equals(object);
        }

        @Override
        public void finishUpdate(ViewGroup arg0) {
        }

        @Override
        public void restoreState(Parcelable arg0, ClassLoader arg1) {
        }

        @Override
        public Parcelable saveState() {
            return null;
        }
    }

}
