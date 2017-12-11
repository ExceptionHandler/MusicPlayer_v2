package com.apppool.demomusicplayer_v2;

import android.graphics.Bitmap;

import java.io.Serializable;

public class SongDetail implements Serializable
{
    static int TARGET_IMAGE_THUMBNAIL_WIDTH = 150;
    static int TARGET_IMAGE_THUMBNAIL_HEIGHT = 150;
    static int TARGET_IMAGE_LOCKSCREEN_WIDTH = 200;
    static int TARGET_IMAGE_LOCKSCREEN_HEIGHT = 300;

    private long id;
    private String title;
    private String artist;
    private long duration;
    private String mediaArtPath;


    public SongDetail(long id, String title, String artist, long duration, String mediaArtPath) {

        this.artist = artist;
        this.duration = duration;
        this.id = id;
        this.mediaArtPath = mediaArtPath;
        this.title = title;
    }


    public long getDuration() {
        return duration;
    }

    public long getId()
    {
        return id;
    }

    public String getTitle()
    {
        return title;
    }

    public String getArtist()
    {
        return artist;
    }

    public String getMediaArtPath() {
        return mediaArtPath;
    }
}
