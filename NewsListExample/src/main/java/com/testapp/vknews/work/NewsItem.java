package com.testapp.vknews.work;

import java.io.Serializable;
import java.util.List;

/**
 * Created by Vasiliy P.
 */
public class NewsItem implements Serializable {
    public Long id = null; // internal ID
    public Long date = null;
    public String type = null;
    public Long profileId = null;
    public Long groupId = null;
    public String postId = null;
    public String postType = null;
    public String text = null;
    public Integer commentsCount = null;
    public Integer likesCount = null;
    public Boolean userLikes = null;
    public Boolean canLike = null;
    public Integer repostsCount = null;

    public String senderName = null;
    public String senderPhotoMini = null;

    public List<NewsItem> history = null;

    public List<String> images = null;
}
