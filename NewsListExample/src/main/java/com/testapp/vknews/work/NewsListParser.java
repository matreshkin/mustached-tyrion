package com.testapp.vknews.work;

import android.content.Context;
import android.graphics.Point;
import android.os.Build;
import android.view.Display;
import android.view.WindowManager;
import org.json.JSONArray;
import org.json.JSONObject;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by Vasiliy P.
 */
public class NewsListParser {

    private Context mContext;
    private int mBaseImageSize;
    private int mMiniImageSize;

    public NewsListParser(Context c) {
        mContext = c;
        mBaseImageSize = getBaseSize();
        mMiniImageSize = mBaseImageSize / 4;
    }

    public AuthUtils.Error parseError(JSONObject obj) {
        AuthUtils.Error e = new AuthUtils.Error();
        if (obj == null) return e;
        JSONObject error = obj.optJSONObject("error");
        if (error == null) return null;
        e.code = error.optInt("error_code");
        e.msg = error.optString("error_msg");
        return e;
    }

    public HashMap<Long, NewsProfile> parseProfiles(JSONArray profiles) {
        HashMap<Long, NewsProfile> res = new HashMap<Long, NewsProfile>();
        for (int n = 0, c = profiles.length(); n < c; n++) {
            NewsProfile profile = parseProfile(profiles.optJSONObject(n));
            if (profile == null || profile.id == null) continue;
            res.put(profile.id, profile);
        }
        return res;
    }

    public HashMap<Long, NewsGroup> parseGroups(JSONArray groups) {
        HashMap<Long, NewsGroup> res = new HashMap<Long, NewsGroup>();
        for (int n = 0, c = groups.length(); n < c; n++) {
            NewsGroup group = parseGroup(groups.optJSONObject(n));
            if (group == null || group.id == null) continue;
            res.put(group.id, group);
        }
        return res;
    }

    public ArrayList<NewsItem> parseItems(JSONArray items, HashMap<Long, NewsProfile> profiles,
                                          HashMap<Long, NewsGroup> groups) {
        ArrayList<NewsItem> res = new ArrayList<NewsItem>();
        for (int n = 0, c = items.length(); n < c; n++) {
            NewsItem item = parseItem(items.optJSONObject(n));
            if (item == null) continue;
            if (!linkSender(profiles, groups, item)) continue;
            if (item.history != null) {
                boolean ok = true;
                for (NewsItem copied : item.history)
                    if (!linkSender(profiles, groups, copied)) ok = false;
                if (!ok) continue;
            }
            res.add(item);
        }
        return res;
    }

    private boolean linkSender(HashMap<Long, NewsProfile> profiles, HashMap<Long, NewsGroup> groups, NewsItem item) {
        if (item.profileId != null) {
            if (profiles.containsKey(item.profileId)) {
                item.senderName = profiles.get(item.profileId).name;
                item.senderPhotoMini = profiles.get(item.profileId).photoMini;
                return true;
            }
        } else if (item.groupId != null) {
            if (groups.containsKey(item.groupId)) {
                item.senderName = groups.get(item.groupId).name;
                item.senderPhotoMini = groups.get(item.groupId).photoMini;
                return true;
            }
        }
        return false;
    }

    @SuppressWarnings("deprecation")
    private int getBaseSize() {
        WindowManager wm = (WindowManager) mContext.getSystemService(
                Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        int width;
        int height;
        if (Build.VERSION.SDK_INT >= 17) {
            Point size = new Point();
            display.getRealSize(size);
            width = size.x;
            height = size.y;
        } else if (Build.VERSION.SDK_INT >= 13) {
            try {
                Method getRawH = Display.class.getMethod("getRawHeight");
                Method getRawW = Display.class.getMethod("getRawWidth");
                width = (Integer) getRawH.invoke(display);
                height = (Integer) getRawW.invoke(display);
            } catch (Exception e) {
                Point size = new Point();
                display.getSize(size);
                width = size.x;
                height = size.y;
            }
        } else {
            width = display.getWidth();
            height = display.getHeight();
        }
        return Math.min(width, height);
    }

    private  NewsProfile parseProfile(JSONObject profile) {
        if (profile == null) return null;
        NewsProfile res = new NewsProfile();
        res.id = profile.optLong("id");
        res.name = profile.optString("first_name", "") + " " +
                profile.optString("last_name", "");
        res.name = res.name.trim();
        if (mMiniImageSize <= 50) res.photoMini = profile.optString("photo_50");
        else res.photoMini = profile.optString("photo_100");
        return res;
    }

    private NewsGroup parseGroup(JSONObject group) {
        if (group == null) return null;
        NewsGroup res = new NewsGroup();
        res.id = group.optLong("id");
        res.name = group.optString("name", "");
        if (mMiniImageSize <= 50) res.photoMini = group.optString("photo_50");
        else if (mMiniImageSize <= 100) res.photoMini = group.optString("photo_100");
        else res.photoMini = group.optString("photo_200");
        return res;
    }

    private NewsItem parseItem(JSONObject item) {
        if (item == null) return null;
        Long senderId = item.optLong("source_id", 0);
        if (senderId.equals(0)) return null;
        NewsItem res = new NewsItem();
        res.date = item.optLong("date");
        res.type = item.optString("type");
        if (senderId > 0) res.profileId = senderId;
        else res.groupId = -senderId;
        res.postId = item.optString("post_id");
        res.postType = item.optString("post_type");
        res.text = item.optString("text");
        JSONObject comments = item.optJSONObject("comments");
        if (comments != null) res.commentsCount = comments.optInt("count");
        JSONObject likes = item.optJSONObject("likes");
        if (likes != null) {
            res.likesCount = likes.optInt("count");
            res.userLikes = likes.optBoolean("user_likes");
            res.canLike = likes.optBoolean("can_like");
        }
        JSONObject reposts = item.optJSONObject("reposts");
        if (reposts != null) res.repostsCount = reposts.optInt("count");
        parseAttachments(item, res);
        parseHistory(item, res);
        return res;
    }

    private void parseHistory(JSONObject item, NewsItem res) {
        Long senderId;
        JSONArray history = item.optJSONArray("copy_history");
        if (history != null) {
            for (int n = 0, c = history.length(); n < c; n++) {
                JSONObject post = history.optJSONObject(n);
                if (post == null) continue;
                senderId = post.optLong("owner_id", 0);
                if (senderId.equals(0)) continue;
                NewsItem copiedPost = new NewsItem();
                copiedPost.date = post.optLong("date");
                copiedPost.type = post.optString("type");
                if (senderId > 0) copiedPost.profileId = senderId;
                else copiedPost.groupId = -senderId;
                copiedPost.postId = post.optString("id");
                copiedPost.postType = post.optString("post_type");
                copiedPost.text = post.optString("text");
                parseAttachments(post, copiedPost);
                if (res.history == null) res.history = new ArrayList<NewsItem>();
                res.history.add(copiedPost);
            }
        }
    }

    private void parseAttachments(JSONObject item, NewsItem res) {
        JSONArray attachments = item.optJSONArray("attachments");
        if (attachments != null) {
            for (int n = 0, c = attachments.length(); n < c; n++) {
                JSONObject attachment = attachments.optJSONObject(n);
                if (attachment == null) continue;
                String attType = attachment.optString("type");
                if (attType != null && attType.equals("photo")) {
                    JSONObject photo = attachment.optJSONObject("photo");
                    if (photo != null) {
                        String image = null;
                        if (mBaseImageSize <= 75) image = photo.optString("photo_75");
                        if (image == null && mBaseImageSize <= 130) image = photo.optString("photo_130");
                        if (image == null && mBaseImageSize <= 604) image = photo.optString("photo_604");
                        if (image == null) image = photo.optString("photo_807");
                        if (image != null && image.trim().length() == 0) image = null;
                        if (image != null) {
                            if (res.images == null) res.images = new ArrayList<String>();
                            res.images.add(image);
                        }
                    }
                }
            }
        }
    }

    static class NewsProfile {
        public Long id = null;
        public String name = null;
        public String photoMini = null;
    }

    static class NewsGroup {
        public Long id = null;
        public String name = null;
        public String photoMini = null;
    }
}
