package com.avoice.uam.util;

public class Config {
    public static String RADIO_URL = "http://streaming.radionomy.com/UAM-UkrainianAlternativeMusic?" +
            "lang=uk-UA%2cuk%3bq%3d0.8%2cru%3bq%3d0.6%2cen-US%3bq%3d0.4%2cen%3bq%3d0.2";
    public static float AUDIO_VOLUME = 1f;
    public enum State { PREPARING, PLAYING, PAUSED, STOPPED, RESTART };

    public static String NOTIFICATION_TITLE = "Ukrainian Alternative Music";
}
