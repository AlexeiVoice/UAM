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
import android.widget.TextView;

import com.avoice.uam.util.Config;

import java.io.IOException;

public class MainActivity extends AppCompatActivity {
    public enum State { PREPARING, PLAYING, PAUSED, STOPPED, RESTART };
    private final String LOGTAG = "MainActivity";

    private Button btnPlay;
    private ProgressBar progressBar;
    private TextView infoTextView;

    private MediaPlayer mPlayer;

    private State currentState;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        currentState = State.STOPPED;

        infoTextView = (TextView)findViewById(R.id.tv_info);
        progressBar = (ProgressBar)findViewById(R.id.progressBar);
        progressBar.setVisibility(View.GONE);
        btnPlay = (Button)findViewById(R.id.btn_start_playing);
        btnPlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                switch (currentState) {
                    case STOPPED:
                    case PAUSED:
                        play(Config.RADIO_URL);
                        break;
                    case PREPARING:
                    case RESTART:
                        stopPlaying();
                        break;
                    case PLAYING:
                        pause();
                        break;
                    default:
                        stopPlaying();
                        break;

                }
            }
        });
    }

    @Override
    protected void onDestroy() {
        if (mPlayer != null) mPlayer.release();
        super.onDestroy();
    }

    public void notifyStateChanged() {
        Log.d(LOGTAG, currentState.toString());
        switch (currentState) {
            case PAUSED:
                infoTextView.setText(getString(R.string.paused));
                btnPlay.setText(getString(R.string.start_playing));
                break;
            case PLAYING:
                infoTextView.setText(getString(R.string.playing));
                btnPlay.setText(getString(R.string.pause_playing));
                break;
            case PREPARING:
            case RESTART:
                infoTextView.setText(getString(R.string.preparing));
                btnPlay.setText(getString(R.string.pause_playing));
                break;
            case STOPPED:
            default:
                infoTextView.setText("");
                btnPlay.setText(getString(R.string.start_playing));
                break;
        }
    }

    public void play(String Url) {
        Log.d(LOGTAG, "play()");
        if(currentState == State.STOPPED) {
            mPlayer = new MediaPlayer();
            currentState = State.PREPARING;
            notifyStateChanged();
            try {
                mPlayer.setDataSource(Url);
                mPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
                mPlayer.setVolume(Config.AUDIO_VOLUME, Config.AUDIO_VOLUME);
                mPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                    @Override
                    public void onPrepared(MediaPlayer mediaPlayer) {
                        Log.d(LOGTAG, "onPrepared");
                        currentState = State.PLAYING;
                        notifyStateChanged();
                        mediaPlayer.start();
                        progressBar.setVisibility(View.GONE);
                    }
                });
                mPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                    @Override
                    public void onCompletion(MediaPlayer mediaPlayer) {
                        Log.d(LOGTAG, "onCompletion()");
                        mediaPlayer.pause();
                        //TODO Deal with this case: after unpausing player only plays part that happened to be downloaded before pause.
                        /*currentState = State.PAUSED;
                        notifyStateChanged();*/
                        currentState = State.RESTART;
                        notifyStateChanged();
                        play(Config.RADIO_URL);
                    }
                });
                mPlayer.prepareAsync();
                progressBar.setVisibility(View.VISIBLE);

            } catch (IOException | IllegalStateException | IllegalArgumentException |SecurityException
                    e ) {
                Log.e(LOGTAG, "playSound() error: " + e.toString());
            }
        } else if(currentState == State.PAUSED) {
            //if player was just paused (and not stopped) we can continue playing by "unpausing" it
            mPlayer.start();
            currentState = State.PLAYING;
            notifyStateChanged();
        }
    }

    public void pause() {
        Log.d(LOGTAG, "pause()");
        progressBar.setVisibility(View.GONE);
        if (mPlayer != null) {
            try {
                if(mPlayer.isPlaying()) {
                    mPlayer.pause();
                    currentState = State.PAUSED;
                    notifyStateChanged();
                }
            } catch (IllegalStateException e) {
                Log.e(LOGTAG, "pause(): " + e.toString());
                mPlayer.release();
                currentState = State.STOPPED;
                notifyStateChanged();
            }
        } else {
            currentState = State.STOPPED;
            notifyStateChanged();
        }
    }

    public boolean stopPlaying(){
        Log.d(LOGTAG, "stopPlaying()");
        progressBar.setVisibility(View.GONE);
        if (mPlayer == null) {
            currentState = State.STOPPED;
            notifyStateChanged();
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
                currentState = State.STOPPED;
                notifyStateChanged();
                return false;
            }
            currentState = State.STOPPED;
            notifyStateChanged();
            return true;
        }
    }

    public State getCurrentState() {
        return currentState;
    }
}