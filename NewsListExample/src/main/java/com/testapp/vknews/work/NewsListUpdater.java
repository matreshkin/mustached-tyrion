package com.testapp.vknews.work;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import com.testapp.vknews.tools.ClientException;
import com.testapp.vknews.tools.Log;
import com.testapp.vknews.tools.NetworkException;
import com.testapp.vknews.tools.ServerException;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Created by Vasiliy P.
 */
public class NewsListUpdater {

    private static final String SCHEMA = "content://";
    private static final String AUTHORITY = NewsListUpdater.class.getPackage().getName();
    public static final Uri URI_NEWS = Uri.parse(SCHEMA + AUTHORITY + "/" + Database.Tables.NEWS_VIEW);

    private static final String TAG = NewsListUpdater.class.getSimpleName();
    public static final String NEWS_LIST_UPDATER_START_FROM = "NewsListUpdater:StartFrom";
    public static final String EXTRA_ERROR = "error";
    public static final String ACTION_ERROR = NewsListUpdater.class.getName() + ".action.ACTION_ERROR";
    private static NewsListUpdater sInstance = null;


    public static synchronized NewsListUpdater getInstance(Context context) {
        if (sInstance == null) sInstance = new NewsListUpdater(context);
        return sInstance;
    }


    private Context mContext;
    private ThreadPoolExecutor mExecutor = new ThreadPoolExecutor(
            1, 2, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>());
    private Handler mHandler = new Handler();
    private BaseUpdateTask mFullRefreshTask = null;
    private BaseUpdateTask mUpdateOlderTask = null;

    private Database mDatabase;

    private NewsListUpdater(Context context) {
        mContext = context;
        mDatabase = Database.getInstance(context);
    }

    public boolean updateFull() {
        if (mFullRefreshTask != null) return false;
        if (mUpdateOlderTask != null) {
            mUpdateOlderTask.cancel();
            mUpdateOlderTask = null;
        }
        new UpdateTask(true).start();
        return true;
    }

    public boolean updateOlder() {
        if (mFullRefreshTask != null) return false;
        if (mUpdateOlderTask != null) return false;
        new UpdateTask(false).start();
        return true;
    }

    public void clearData() {
        if (mFullRefreshTask != null) mFullRefreshTask.cancel();
        if (mUpdateOlderTask != null) mUpdateOlderTask.cancel();
        new ClearTask().start();
    }

    private class ClearTask extends BaseUpdateTask {

        @Override
        public void beforeExecute() {
        }
        @Override
        public void execute() throws ClientException, NetworkException, ServerException {
            mDatabase.clear();
        }
        @Override
        public void afterExecute() {
        }
    }


    private class UpdateTask extends BaseUpdateTask {

        final boolean fullRefresh;

        NewsListParser mParser = new NewsListParser(mContext);

        UpdateTask(boolean fullRefresh) {
            this.fullRefresh = fullRefresh;
        }

        @Override
        public void beforeExecute() {
            if (fullRefresh) mFullRefreshTask = this;
            else mUpdateOlderTask = this;
        }

        @Override
        public void execute()
                throws ClientException,
                NetworkException,
                ServerException {

            String startFrom = null;

            if (!fullRefresh) {
                Cursor c = mDatabase.query(Database.Tables.PREFS,
                        new String[]{Database.Tables.Prefs.VALUE},
                        Database.Tables.Prefs.KEY + "='" + NEWS_LIST_UPDATER_START_FROM + "'",
                        null, null);
                if (c != null && c.moveToFirst()) {
                    startFrom = c.getString(0);
                }
                if (c != null && !c.isClosed()) c.close();
            }

            JSONObject obj = VkApi.getInstance(mContext).getNewsList(startFrom, 5);
            AuthUtils.Error error = mParser.parseError(obj);
            if (error != null) {
                Intent i = new Intent(ACTION_ERROR);
                i.putExtra(EXTRA_ERROR, error);
                mContext.sendBroadcast(i);
                return;
            }
            JSONArray items = null;
            JSONArray profiles = null;
            JSONArray groups = null;
            obj = obj.optJSONObject("response");
            startFrom = obj.optString("next_from");
            if (obj == null) return; // WTF
            items = obj.optJSONArray("items");
            profiles = obj.optJSONArray("profiles");
            groups = obj.optJSONArray("groups");
            ArrayList<NewsItem> parsedItems = mParser.parseItems(
                    items, mParser.parseProfiles(profiles), mParser.parseGroups(groups));
            if (isCancelled()) return;
            mDatabase.beginSimpleTransaction();
            try {
                if (fullRefresh) mDatabase.clearTable(Database.Tables.NEWS);
                ContentValues val = new ContentValues();
                int count = 0;
                for (NewsItem item : parsedItems) {
                    Long[] parentIds = new Long[] {null, null};
                    if (item.history != null) {
                        for (int n = 0, c = item.history.size(); n < c; n++) {
                            if (n < c - 2) continue; // only the last two :)
                            NewsItem copied = item.history.get(n);
                            parentIds[n - (c - 2)] = insertItem(val, copied, true, null);
                        }
                    }
                    long id = insertItem(val, item, false, parentIds);
                    count++;
                }
                putValue(val, Database.Tables.Prefs.KEY, NEWS_LIST_UPDATER_START_FROM);
                putValue(val, Database.Tables.Prefs.VALUE, startFrom);
                mDatabase.insertOrReplace(Database.Tables.PREFS, val);
                if (isCancelled()) return;
                mDatabase.setTransactionSuccessful();
                Log.d(TAG, "Inserted: " + count);
            } finally {
                mDatabase.endSimpleTransaction();
            }
        }

        private long insertItem(ContentValues val, NewsItem item, boolean fromHistory, Long[] parentIds) {
            putValue(val, Database.Tables.News.DATE, item.date);
            putValue(val, Database.Tables.News.TYPE, item.type);
            putValue(val, Database.Tables.News.PROFILE_ID, item.profileId);
            putValue(val, Database.Tables.News.GROUP_ID, item.groupId);
            putValue(val, Database.Tables.News.POST_ID, item.postId);
            putValue(val, Database.Tables.News.POST_TYPE, item.postType);
            putValue(val, Database.Tables.News.TEXT, item.text);
            putValue(val, Database.Tables.News.COMMENTS_COUNT, item.commentsCount);
            putValue(val, Database.Tables.News.LIKES_COUNT, item.likesCount);
            putValue(val, Database.Tables.News.USER_LIKES, item.userLikes);
            putValue(val, Database.Tables.News.CAN_LIKE, item.canLike);
            putValue(val, Database.Tables.News.REPOSTS_COUNT, item.repostsCount);
            String images = null;
            if (item.images != null && item.images.size() > 0) {
                StringBuilder _images = new StringBuilder();
                for (String img : item.images) {
                    _images.append(img);
                    _images.append('\n');
                }
                images = _images.toString();
            }
            putValue(val, Database.Tables.News.IMAGE_URL, images);
            putValue(val, Database.Tables.News.SENDER_NAME, item.senderName);
            putValue(val, Database.Tables.News.SENDER_PHOTO_MINI, item.senderPhotoMini);
            putValue(val, Database.Tables.News.FROM_HISTORY, fromHistory);
            putValue(val, Database.Tables.News.PARENT_ID1, parentIds == null ? null : parentIds[0]);
            putValue(val, Database.Tables.News.PARENT_ID2, parentIds == null ? null : parentIds[1]);
            long id = mDatabase.insert(Database.Tables.NEWS, val);
            val.clear();
            return id;
        }

        private  void putValue(ContentValues values, String column, String val) {
            if (val == null) values.putNull(column);
            else values.put(column, val);
        }
        private  void putValue(ContentValues values, String column, Long val) {
            if (val == null) values.putNull(column);
            else values.put(column, val);
        }
        private  void putValue(ContentValues values, String column, Integer val) {
            if (val == null) values.putNull(column);
            else values.put(column, val);
        }
        private  void putValue(ContentValues values, String column, Boolean val) {
            if (val == null) values.putNull(column);
            else values.put(column, val ? 1 : 0);
        }


        @Override
        public void afterExecute() {
            if (fullRefresh) mFullRefreshTask = null;
            else mUpdateOlderTask = null;
            mContext.getContentResolver().notifyChange(URI_NEWS, null);
        }
    }

    private abstract class BaseUpdateTask implements Runnable {

        public abstract void beforeExecute();
        public abstract void execute() throws ClientException, NetworkException, ServerException;
        public abstract void afterExecute();

        volatile Future<?> futureTask = null;

        synchronized BaseUpdateTask start() {
            futureTask = mExecutor.submit(this);
            beforeExecute();
            return this;
        }

        @Override
        public final synchronized void run() {
            if (isCancelled()) return;
            try {
                execute();
            } catch (ClientException e) {
                Log.e(e);
            } catch (NetworkException e) {
                Log.e(e);
            } catch (ServerException e) {
                Log.e(e);
            } catch (Exception e) {
                Log.e(e);
            }
            if (isCancelled()) return;
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (isCancelled()) return;
                    afterExecute();
                }
            });
        }

        public final void cancel() {
            futureTask.cancel(false);
        }

        public final boolean isCancelled() {
            return futureTask.isCancelled();
        }
        public final boolean isDone() {
            return futureTask.isDone();
        }
    }

}
