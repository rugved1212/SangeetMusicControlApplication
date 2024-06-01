package com.example.serversidemusiccontrol;

import android.widget.ImageView;

public class MusicFile {

    private String music_ID;
    private String music_name;
    private String artist_name;
    private String musicURL;
    private String musicType;
    private String storageFile;
    private String imgURL;
    private String movieName;
//    private long timeSTAMP;

    public MusicFile(){}

    public MusicFile(String music_ID, String music_name, String artist_name, String musicURL, String musicType, String storageFile, String imgURL, String movieName) {
        this.music_ID = music_ID;
        this.music_name = music_name;
        this.artist_name = artist_name;
        this.musicURL = musicURL;
        this.musicType = musicType;
        this.storageFile = storageFile;
        this.imgURL = imgURL;
        this.movieName = movieName;
//        this.timeSTAMP = timeSTAMP;
    }

    public String getMusic_ID() {
        return music_ID;
    }
    public String getMusic_name() {
        return music_name;
    }

    public String getMusicURL() {
        return musicURL;
    }

    public String getMusicType() {
        return musicType;
    }

    public String getStorageFile() {
        return storageFile;
    }
    public String getImgURL() {
        return imgURL;
    }

    public String getArtist_name() {
        return artist_name;
    }
    public String getMovieName() {
        return movieName;
    }
}
