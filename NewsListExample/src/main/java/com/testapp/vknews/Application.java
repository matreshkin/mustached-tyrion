package com.testapp.vknews;

import android.content.Context;
import com.nostra13.universalimageloader.cache.disc.naming.Md5FileNameGenerator;
import com.nostra13.universalimageloader.cache.memory.impl.WeakMemoryCache;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.nostra13.universalimageloader.core.assist.QueueProcessingType;
import com.nostra13.universalimageloader.core.download.BaseImageDownloader;

/**
 * Created by Vasiliy P.
 */
public class Application extends android.app.Application {

    private static final int IMAGES_DISC_CACHE_MAX_SIZE = 100 * 1024 * 1024; // 100 mb
    private static boolean sIsTabletDevice;

    @Override
    public void onCreate() {
        super.onCreate();
        sIsTabletDevice = getResources().getBoolean(R.bool.is_tablet);
        initImageLoader(getApplicationContext());
    }

    public static boolean isTablet() {
        return sIsTabletDevice;
    }

    public static void initImageLoader(Context context) {

        ImageLoaderConfiguration config = new ImageLoaderConfiguration.Builder(context)
                .threadPriority(Thread.MIN_PRIORITY)
                .denyCacheImageMultipleSizesInMemory()
                .discCacheFileNameGenerator(new Md5FileNameGenerator())
                .tasksProcessingOrder(QueueProcessingType.LIFO)
                .imageDownloader(new BaseImageDownloader(context))
                .discCacheSize(IMAGES_DISC_CACHE_MAX_SIZE)
                .memoryCache(new WeakMemoryCache())
                .build();
        ImageLoader.getInstance().init(config);
    }
}
