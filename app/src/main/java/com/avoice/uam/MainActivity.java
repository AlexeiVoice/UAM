package com.avoice.uam;

import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.PowerManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;

import com.avoice.uam.util.Config;

import java.io.IOException;

public class MainActivity extends AppCompatActivity {
    private final String LOGTAG = "MainActivity";

    private Button btnPlay;
    private ProgressBar progressBar;

    private boolean isPlaying;
    private MediaPlayer mPlayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        isPlaying = false;

        progressBar = (ProgressBar)findViewById(R.id.progressBar);
        progressBar.setVisibility(View.GONE);
        btnPlay = (Button)findViewById(R.id.btn_start_playing);
        btnPlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (isPlaying)
                    stopPlaying();
                else
                    play(Config.RADIO_URL);
            }
        });
    }

    @Override
    protected void onDestroy() {
        if (mPlayer != null) mPlayer.release();
        super.onDestroy();
    }

    public void setIsPlaying(boolean isPlaying) {
        this.isPlaying = isPlaying;
        if(isPlaying)
            btnPlay.setText(getString(R.string.stop_playing));
        else
            btnPlay.setText(getString(R.string.start_playing));
    }

    public void play(String Url) {
        Log.d(LOGTAG, "play()");
        mPlayer = new MediaPlayer();
        setIsPlaying(true);
        try {
            mPlayer.setDataSource(Url);
            mPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            mPlayer.setVolume(Config.AUDIO_VOLUME, Config.AUDIO_VOLUME);
            mPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mediaPlayer) {
                    Log.d(LOGTAG, "onPrepared");

                    mediaPlayer.start();
                    progressBar.setVisibility(View.GONE);
                }
            });
            mPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mediaPlayer) {
                    Log.d(LOGTAG, "onCompletion()");
                    setIsPlaying(false);
                    mediaPlayer.reset();
                }
            });
            mPlayer.prepareAsync();
            progressBar.setVisibility(View.VISIBLE);

        } catch (IOException | IllegalStateException | IllegalArgumentException |SecurityException
                e ) {
            Log.e(LOGTAG, "playSound() error: " + e.toString());
        }
    }

    public boolean stopPlaying(){
        Log.d(LOGTAG, "stopPlaying()");
        progressBar.setVisibility(View.GONE);
        setIsPlaying(false);
        if (mPlayer == null) {
            return false;
        } else {
            try {
                if(mPlayer.isPlaying()) {
                    mPlayer.stop();
                }
                mPlayer.reset();
                mPlayer.release();
            } catch (IllegalStateException e) {
                Log.e(LOGTAG, "stopPlaying: " + e.toString());
                mPlayer.release();
                return false;
            }
            return true;
        }
    }
}