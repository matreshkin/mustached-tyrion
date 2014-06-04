package com.testapp.vknews.work;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.MergeCursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;
import android.os.SystemClock;
import com.testapp.vknews.tools.Log;

import java.io.File;
import java.util.ArrayList;
import java.util.concurrent.Semaphore;

public class Database {
    public static final int MAX_TRANS_PERMITS = 2;
    public static final long ROOT_FOLDER_ID = 0;
    public static long UPLOADS_FOLDER_ID = 1;

    private static final int DB_VERSION = 5;
    public static final String DB_NAME = "data.db";
    private static final String TAG = Database.class.getName();
    public static final int QUERY_CURSOR_PIE = Log.DEBUG ? 2000 : 2000;

    private static Database sInstance = null;

    public static synchronized Database getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new Database(context);
        }
        return sInstance;
    }

    private SQLiteOpenHelper mOpenHelper = null;
    private SQLiteDatabase mDatabase = null;

    private Semaphore mTransactionsLocker = null;
    private Context mContext;

    private Database(Context context) {
        mContext = context;
//		File dbDir = Connection.getDatabaseDir(DB_NAME);
//		if (dbDir == null) return;
//		File dbFile = new File(dbDir, DB_NAME);
        File dbFile = mContext.getDatabasePath(DB_NAME);
        mOpenHelper = new OpenHelper(mContext, dbFile.getAbsolutePath());
        mDatabase = mOpenHelper.getWritableDatabase();
        mTransactionsLocker = new Semaphore(MAX_TRANS_PERMITS);
    }

    public void clear() {
        if (!lockerAcquire(MAX_TRANS_PERMITS)) return;
        try {
            mDatabase.execSQL(String.format(SQL_CLEAR_TABLE, Tables.NEWS));
            mDatabase.execSQL(String.format(SQL_CLEAR_TABLE, Tables.PREFS));
        } finally {
            lockerRelease(MAX_TRANS_PERMITS);
        }

    }
    /*****************************************************************/
    /**              Функции работы с транзакциями					**/

    // клиентские транзакции и транзакции сканнера
    //		взаимно исключаются

    /**
     * транзакции для "клиента" (рабочие изменения) <br />
     * транзакция стартует только после завершенной синхронизации
     * @return
     */
    public boolean beginSimpleTransaction() {
        return beginTransaction(MAX_TRANS_PERMITS);
    }
    public void endSimpleTransaction() {
        endTransaction(MAX_TRANS_PERMITS);
    }

    /**
     * транзакции синхронизации (может быть две штуки) <br />
     * блокируют клиентские транзакции
     * @return
     */
    public boolean beginSyncTransaction() {
        return beginTransaction(MAX_TRANS_PERMITS);
    }
    public void endSyncTransaction() {
        endTransaction(MAX_TRANS_PERMITS);
    }
    public void setTransactionSuccessful() {
        mDatabase.setTransactionSuccessful();
    }


    /*****************************************************************/
    /**             Функции сканнера								**/

    public void clearTable(String tableName) {
        mDatabase.execSQL(String.format(SQL_CLEAR_TABLE, tableName));
    }

    private void bindLong(SQLiteStatement s, int n, Long val) {
        if (val == null) s.bindNull(n);
        else s.bindLong(n, val);
    }

    private void bindString(SQLiteStatement s, int n, String val) {
        if (val == null) s.bindNull(n);
        else s.bindString(n, val);
    }

    private void bindDouble(SQLiteStatement s, int n, Double val) {
        if (val == null) s.bindNull(n);
        else s.bindDouble(n, val);
    }


    /*****************************************************************/
    /**              Функции клиента								**/

    public void execSQL(String sql) {
        mDatabase.execSQL(sql);
    }
    public int delete(String table, String whereClause,
                      String[] whereArgs) {
        return mDatabase.delete(table, whereClause, whereArgs);
    }
    public long insert(String table, ContentValues values) {
        return mDatabase.insert(table, null, values);
    }
    public long insertOrReplace(String table, ContentValues values) {
        return mDatabase.insertWithOnConflict(table, null, values,
                SQLiteDatabase.CONFLICT_REPLACE);
    }

    public int update(String table, ContentValues values,
                      String whereClause, String[] whereArgs) {
        return mDatabase.update(table, values, whereClause, whereArgs);
    }
    public Cursor query(String table, String[] columns,
                        String selection, String[] selectionArgs, String orderBy) {
        return query(table, columns, selection, selectionArgs, orderBy, null);
    }

    public Cursor query(String table, String[] columns,
                        String selection, String[] selectionArgs, String orderBy, String limit) {
        return query(table, columns, selection, selectionArgs, null, orderBy, limit);
    }

    public Cursor query(String table, String[] columns,
                        String selection, String[] selectionArgs, String groupBy, String orderBy,
                        String limit) {
        long time = SystemClock.uptimeMillis();
        if (!lockerAcquire(1)) return null;
        Log.b("", "CURSOR-LOCKED: time: " + (SystemClock.uptimeMillis() - time));
        try {
            if (limit != null) {
                time = SystemClock.uptimeMillis();
                Cursor c = mDatabase.query(table, columns, selection, selectionArgs,
                        groupBy, null, orderBy, limit);
                Log.b("", "CURSOR-QUERIED: time: " + (SystemClock.uptimeMillis() - time));
                return c;
            } else {
                ArrayList<Cursor> cursors = null;
                int queried = 0;
                int count = 0;
                do {
                    limit = String.valueOf(queried) + "," + QUERY_CURSOR_PIE;
                    time = SystemClock.uptimeMillis();
                    Cursor c = mDatabase.query(table, columns, selection, selectionArgs,
                            groupBy, null, orderBy, limit);
                    if (c == null) break;
                    count = c.getCount();
                    if (cursors == null) {
                        cursors = new ArrayList<Cursor>(10);
                        cursors.add(c);
                    } else {
                        if (count > 0) cursors.add(c);
                    }
                    queried += count;
                    Log.b("", "CURSOR-QUERIED: time: " + (SystemClock.uptimeMillis() - time) +
                            " count: " + count + " all: " + queried);
                } while (count == QUERY_CURSOR_PIE);
                if (cursors == null || cursors.size() == 0) return null;
                if (cursors.size() == 1) return cursors.get(0);
                return new MergeCursor(cursors.toArray(new Cursor[cursors.size()]));
            }
        } finally {
            lockerRelease(1);
        }
    }
    /*****************************************************************/
    /**              Private функции								**/

    private boolean lockerAcquire(int permits) {
        try {
            mTransactionsLocker.acquire(permits);
        } catch (InterruptedException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    private void lockerRelease(int permits) {
        mTransactionsLocker.release(permits);
    }

    private boolean beginTransaction(int permits) {
        if (!lockerAcquire(permits)) return false;
        mDatabase.beginTransaction();
        return true;
    }
    private void endTransaction(int permits) {
        mDatabase.endTransaction();
        lockerRelease(permits);
    }


    private void execPatch(String[] patch) {
        execPatch(mDatabase, patch);
    }

    private static void execPatch(SQLiteDatabase db, String[] patch) {
        for (String sql : patch) db.execSQL(sql);
    }


    private static class OpenHelper extends SQLiteOpenHelper {

        public OpenHelper(Context context, String dbFullPath) {
            super(context, dbFullPath, null, DB_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            execPatch(db, PATCH_CREATE_ALL);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            execPatch(db, PATCH_DROP_ALL);
            execPatch(db, PATCH_CREATE_ALL);
        }
    }

    @SuppressWarnings("unused")
    public static class Tables {

        public static final String NEWS = "NEWS";
        public static interface News {
            static final String ID		            = "_id";
            static final String DATE		        = "DATE";
            static final String TYPE		        = "TYPE";
            static final String PROFILE_ID		    = "PROFILE_ID";
            static final String GROUP_ID		    = "GROUP_ID";
            static final String POST_ID		        = "POST_ID";
            static final String POST_TYPE		    = "POST_TYPE";
            static final String TEXT		        = "TEXT";
            static final String COMMENTS_COUNT		= "COMMENTS_COUNT";
            static final String LIKES_COUNT		    = "LIKES_COUNT";
            static final String USER_LIKES		    = "USER_LIKES";
            static final String CAN_LIKE		    = "CAN_LIKE";
            static final String REPOSTS_COUNT		= "REPOSTS_COUNT";

            static final String IMAGE_URL   		= "IMAGE_URL";

            static final String SENDER_NAME   		= "SENDER_NAME";
            static final String SENDER_PHOTO_MINI   = "SENDER_PHOTO_MINI";

            static final String FROM_HISTORY        = "FROM_HISTORY";
            static final String PARENT_ID1          = "PARENT_ID1";
            static final String PARENT_ID2          = "PARENT_ID2";
        }

        public static final String NEWS_VIEW = "NEWS_VIEW";
        public static interface NewsParent1 {
            static final String ID		            = "p1_" + News.ID;
            static final String DATE		        = "p1_" + News.DATE;
            static final String TYPE		        = "p1_" + News.TYPE;
            static final String PROFILE_ID		    = "p1_" + News.PROFILE_ID;
            static final String GROUP_ID		    = "p1_" + News.GROUP_ID;
            static final String POST_ID		        = "p1_" + News.POST_ID;
            static final String POST_TYPE		    = "p1_" + News.POST_TYPE;
            static final String TEXT		        = "p1_" + News.TEXT;
            static final String COMMENTS_COUNT		= "p1_" + News.COMMENTS_COUNT;
            static final String LIKES_COUNT		    = "p1_" + News.LIKES_COUNT;
            static final String USER_LIKES		    = "p1_" + News.USER_LIKES;
            static final String CAN_LIKE		    = "p1_" + News.CAN_LIKE;
            static final String REPOSTS_COUNT		= "p1_" + News.REPOSTS_COUNT;

            static final String IMAGE_URL   		= "p1_" + News.IMAGE_URL;

            static final String SENDER_NAME   		= "p1_" + News.SENDER_NAME;
            static final String SENDER_PHOTO_MINI   = "p1_" + News.SENDER_PHOTO_MINI;

            static final String FROM_HISTORY        = "p1_" + News.FROM_HISTORY;
            static final String PARENT_ID1          = "p1_" + News.PARENT_ID1;
            static final String PARENT_ID2          = "p1_" + News.PARENT_ID2;
        }
        public static interface NewsParent2 {
            static final String ID		            = "p2_" + News.ID;
            static final String DATE		        = "p2_" + News.DATE;
            static final String TYPE		        = "p2_" + News.TYPE;
            static final String PROFILE_ID		    = "p2_" + News.PROFILE_ID;
            static final String GROUP_ID		    = "p2_" + News.GROUP_ID;
            static final String POST_ID		        = "p2_" + News.POST_ID;
            static final String POST_TYPE		    = "p2_" + News.POST_TYPE;
            static final String TEXT		        = "p2_" + News.TEXT;
            static final String COMMENTS_COUNT		= "p2_" + News.COMMENTS_COUNT;
            static final String LIKES_COUNT		    = "p2_" + News.LIKES_COUNT;
            static final String USER_LIKES		    = "p2_" + News.USER_LIKES;
            static final String CAN_LIKE		    = "p2_" + News.CAN_LIKE;
            static final String REPOSTS_COUNT		= "p2_" + News.REPOSTS_COUNT;

            static final String IMAGE_URL   		= "p2_" + News.IMAGE_URL;

            static final String SENDER_NAME   		= "p2_" + News.SENDER_NAME;
            static final String SENDER_PHOTO_MINI   = "p2_" + News.SENDER_PHOTO_MINI;

            static final String FROM_HISTORY        = "p2_" + News.FROM_HISTORY;
            static final String PARENT_ID1          = "p2_" + News.PARENT_ID1;
            static final String PARENT_ID2          = "p2_" + News.PARENT_ID2;
        }

        private static interface _News {
            static final String ID		            = "INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT";
            static final String DATE		        = "INTEGER NOT NULL";
            static final String TYPE		        = "TEXT DEFAULT ''";
            static final String PROFILE_ID		    = "INTEGER NULL";
            static final String GROUP_ID		    = "INTEGER NULL";
            static final String POST_ID		        = "INTEGER NOT NULL";
            static final String POST_TYPE		    = "TEXT";
            static final String TEXT		        = "TEXT";
            static final String COMMENTS_COUNT		= "INTEGER DEFAULT 0";
            static final String LIKES_COUNT		    = "INTEGER DEFAULT 0";
            static final String USER_LIKES		    = "INTEGER DEFAULT 0";
            static final String CAN_LIKE		    = "INTEGER DEFAULT 0";
            static final String REPOSTS_COUNT		= "INTEGER DEFAULT 0";

            static final String IMAGE_URL   		= "TEXT";

            static final String SENDER_NAME   		= "TEXT";
            static final String SENDER_PHOTO_MINI   = "TEXT";

            static final String FROM_HISTORY        = "INTEGER DEFAULT 0";
            static final String PARENT_ID1          = "INTEGER NULL";
            static final String PARENT_ID2          = "INTEGER NULL";

        }
        public static final String PREFS = "PREFS";
        public static interface Prefs {
            static final String ID		            = "_id";
            static final String KEY		            = "KEY";
            static final String VALUE		        = "VALUE";
        }

        private static interface _Prefs {
            static final String ID		            = "INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT";
            static final String KEY		            = "TEXT NOT NULL";
            static final String VALUE		        = "TEXT NULL";

        }
    }

    private static final String SQL_CLEAR_TABLE = "DELETE FROM %s";

    private static final String[] PATCH_DROP_ALL = new String[] {
            "DROP TABLE IF EXISTS " + Tables.NEWS,
            "DROP TABLE IF EXISTS " + Tables.PREFS,
            "DROP VIEW IF EXISTS " + Tables.NEWS_VIEW,
    };

    private static final String[] PATCH_CREATE_ALL = new String[] {

            "CREATE TABLE " + Tables.NEWS + "("
                + Tables.News.ID + " " + Tables._News.ID + ","
                + Tables.News.DATE + " " + Tables._News.DATE + ","
                + Tables.News.TYPE + " " + Tables._News.TYPE + ","
                + Tables.News.PROFILE_ID + " " + Tables._News.PROFILE_ID + ","
                + Tables.News.GROUP_ID + " " + Tables._News.GROUP_ID + ","
                + Tables.News.POST_ID + " " + Tables._News.POST_ID + ","
                + Tables.News.POST_TYPE + " " + Tables._News.POST_TYPE + ","
                + Tables.News.TEXT + " " + Tables._News.TEXT + ","
                + Tables.News.COMMENTS_COUNT + " " + Tables._News.COMMENTS_COUNT + ","
                + Tables.News.LIKES_COUNT + " " + Tables._News.LIKES_COUNT + ","
                + Tables.News.USER_LIKES + " " + Tables._News.USER_LIKES + ","
                + Tables.News.CAN_LIKE + " " + Tables._News.CAN_LIKE + ","
                + Tables.News.REPOSTS_COUNT + " " + Tables._News.REPOSTS_COUNT + ","

                + Tables.News.IMAGE_URL + " " + Tables._News.IMAGE_URL + ","

                + Tables.News.SENDER_NAME + " " + Tables._News.SENDER_NAME + ","
                + Tables.News.SENDER_PHOTO_MINI + " " + Tables._News.SENDER_PHOTO_MINI + ","

                + Tables.News.FROM_HISTORY + " " + Tables._News.FROM_HISTORY + ","
                + Tables.News.PARENT_ID1 + " " + Tables._News.PARENT_ID1 + ","
                + Tables.News.PARENT_ID2 + " " + Tables._News.PARENT_ID2 + ")",

            "CREATE TABLE IF NOT EXISTS " + Tables.PREFS + "("
                    + Tables.Prefs.ID + " " + Tables._Prefs.ID + ","
                    + Tables.Prefs.KEY + " " + Tables._Prefs.KEY + ","
                    + Tables.Prefs.VALUE + " " + Tables._Prefs.VALUE + ","
                    + "UNIQUE(" + Tables.Prefs.KEY + ") ON CONFLICT REPLACE)",

            "CREATE INDEX idx_" + Tables.NEWS + "_" + Tables.News.PROFILE_ID
                    + " ON " + Tables.NEWS + "(" + Tables.News.PROFILE_ID + ")",
            "CREATE INDEX idx_" + Tables.NEWS + "_" + Tables.News.GROUP_ID
                    + " ON " + Tables.NEWS + "(" + Tables.News.GROUP_ID + ")",
            "CREATE INDEX idx_" + Tables.NEWS + "_" + Tables.News.POST_ID
                    + " ON " + Tables.NEWS + "(" + Tables.News.POST_ID + ")",

            "CREATE VIEW IF NOT EXISTS " + Tables.NEWS_VIEW + " AS "
                    + "SELECT "
                    + "A." + Tables.News.ID + " AS " + Tables.News.ID + ", "
                    + "A." + Tables.News.DATE + " AS " + Tables.News.DATE + ", "
                    + "A." + Tables.News.TYPE + " AS " + Tables.News.TYPE + ", "
                    + "A." + Tables.News.PROFILE_ID + " AS " + Tables.News.PROFILE_ID + ", "
                    + "A." + Tables.News.GROUP_ID + " AS " + Tables.News.GROUP_ID + ", "
                    + "A." + Tables.News.POST_ID + " AS " + Tables.News.POST_ID + ", "
                    + "A." + Tables.News.POST_TYPE + " AS " + Tables.News.POST_TYPE + ", "
                    + "A." + Tables.News.TEXT + " AS " + Tables.News.TEXT + ", "
                    + "A." + Tables.News.COMMENTS_COUNT + " AS " + Tables.News.COMMENTS_COUNT + ", "
                    + "A." + Tables.News.LIKES_COUNT + " AS " + Tables.News.LIKES_COUNT + ", "
                    + "A." + Tables.News.USER_LIKES + " AS " + Tables.News.USER_LIKES + ", "
                    + "A." + Tables.News.CAN_LIKE + " AS " + Tables.News.CAN_LIKE + ", "
                    + "A." + Tables.News.REPOSTS_COUNT + " AS " + Tables.News.REPOSTS_COUNT + ", "
                    + "A." + Tables.News.IMAGE_URL + " AS " + Tables.News.IMAGE_URL + ", "
                    + "A." + Tables.News.SENDER_NAME + " AS " + Tables.News.SENDER_NAME + ", "
                    + "A." + Tables.News.SENDER_PHOTO_MINI + " AS " + Tables.News.SENDER_PHOTO_MINI + ", "

                    + "B." + Tables.News.ID + " AS " + Tables.NewsParent1.ID + ", "
                    + "B." + Tables.News.DATE + " AS " + Tables.NewsParent1.DATE + ", "
                    + "B." + Tables.News.TYPE + " AS " + Tables.NewsParent1.TYPE + ", "
                    + "B." + Tables.News.PROFILE_ID + " AS " + Tables.NewsParent1.PROFILE_ID + ", "
                    + "B." + Tables.News.GROUP_ID + " AS " + Tables.NewsParent1.GROUP_ID + ", "
                    + "B." + Tables.News.POST_ID + " AS " + Tables.NewsParent1.POST_ID + ", "
                    + "B." + Tables.News.POST_TYPE + " AS " + Tables.NewsParent1.POST_TYPE + ", "
                    + "B." + Tables.News.TEXT + " AS " + Tables.NewsParent1.TEXT + ", "
                    + "B." + Tables.News.COMMENTS_COUNT + " AS " + Tables.NewsParent1.COMMENTS_COUNT + ", "
                    + "B." + Tables.News.LIKES_COUNT + " AS " + Tables.NewsParent1.LIKES_COUNT + ", "
                    + "B." + Tables.News.USER_LIKES + " AS " + Tables.NewsParent1.USER_LIKES + ", "
                    + "B." + Tables.News.CAN_LIKE + " AS " + Tables.NewsParent1.CAN_LIKE + ", "
                    + "B." + Tables.News.REPOSTS_COUNT + " AS " + Tables.NewsParent1.REPOSTS_COUNT + ", "
                    + "B." + Tables.News.IMAGE_URL + " AS " + Tables.NewsParent1.IMAGE_URL + ", "
                    + "B." + Tables.News.SENDER_NAME + " AS " + Tables.NewsParent1.SENDER_NAME + ", "
                    + "B." + Tables.News.SENDER_PHOTO_MINI + " AS " + Tables.NewsParent1.SENDER_PHOTO_MINI + ", "

                    + "C." + Tables.News.ID + " AS " + Tables.NewsParent2.ID + ", "
                    + "C." + Tables.News.DATE + " AS " + Tables.NewsParent2.DATE + ", "
                    + "C." + Tables.News.TYPE + " AS " + Tables.NewsParent2.TYPE + ", "
                    + "C." + Tables.News.PROFILE_ID + " AS " + Tables.NewsParent2.PROFILE_ID + ", "
                    + "C." + Tables.News.GROUP_ID + " AS " + Tables.NewsParent2.GROUP_ID + ", "
                    + "C." + Tables.News.POST_ID + " AS " + Tables.NewsParent2.POST_ID + ", "
                    + "C." + Tables.News.POST_TYPE + " AS " + Tables.NewsParent2.POST_TYPE + ", "
                    + "C." + Tables.News.TEXT + " AS " + Tables.NewsParent2.TEXT + ", "
                    + "C." + Tables.News.COMMENTS_COUNT + " AS " + Tables.NewsParent2.COMMENTS_COUNT + ", "
                    + "C." + Tables.News.LIKES_COUNT + " AS " + Tables.NewsParent2.LIKES_COUNT + ", "
                    + "C." + Tables.News.USER_LIKES + " AS " + Tables.NewsParent2.USER_LIKES + ", "
                    + "C." + Tables.News.CAN_LIKE + " AS " + Tables.NewsParent2.CAN_LIKE + ", "
                    + "C." + Tables.News.REPOSTS_COUNT + " AS " + Tables.NewsParent2.REPOSTS_COUNT + ", "
                    + "C." + Tables.News.IMAGE_URL + " AS " + Tables.NewsParent2.IMAGE_URL + ", "
                    + "C." + Tables.News.SENDER_NAME + " AS " + Tables.NewsParent2.SENDER_NAME + ", "
                    + "C." + Tables.News.SENDER_PHOTO_MINI + " AS " + Tables.NewsParent2.SENDER_PHOTO_MINI + " "
                    + " FROM " + Tables.NEWS + " AS A "
                    + " LEFT JOIN " + Tables.NEWS + " AS B ON B." + Tables.News.ID + "=A." + Tables.News.PARENT_ID1
                    + " LEFT JOIN " + Tables.NEWS + " AS C ON C." + Tables.News.ID + "=A." + Tables.News.PARENT_ID2
                    + " WHERE A." + Tables.News.FROM_HISTORY + "=0"
            ,

    };


}
