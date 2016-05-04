package com.avoice.uam.model;

/**
 * Stores song info:
 *      <br>{@link #title},
 *      <br>{@link #artist},
 *      <br>{@link #coverUrl cover URL},
 *      <br>{@link #timeLeft time left}.
 */
public class Track {
    private String title;
    private String artist;
    private String coverUrl;
    private String timeLeft;

    public Track(String title, String artist, String coverUrl, String timeLeft) {
        this.title = title;
        this.artist = artist;
        this.coverUrl = coverUrl;
        this.timeLeft = timeLeft;
    }

    public Track(Track track) {
        this.title = track.getTitle();
        this.artist = track.getArtist();
        this.coverUrl = track.getCoverUrl();
        this.timeLeft = track.getTimeLeft();
    }

    public String getTitle() {
        return title == null ? "" : title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getArtist() {
        return artist == null ? "" : artist;
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }

    public String getCoverUrl() {
        return coverUrl == null ? "" : coverUrl;
    }

    public void setCoverUrl(String coverUrl) {
        this.coverUrl = coverUrl;
    }

    public String getTimeLeft() {
        return timeLeft == null ? " " : timeLeft;
    }

    public void setTimeLeft(String timeLeft) {
        this.timeLeft = timeLeft;
    }

    @Override
    public String toString() {
        return getArtist() + " - " + getTitle();
    }
}
