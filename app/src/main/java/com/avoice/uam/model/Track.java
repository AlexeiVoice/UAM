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
}
