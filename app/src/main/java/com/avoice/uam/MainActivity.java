package com.avoice.uam;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.IBinder;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.avoice.uam.listener.OnPlayerStateChangedListener;
import com.avoice.uam.util.Config;
import com.avoice.uam.util.Constants;

public class MainActivity extends AppCompatActivity implements OnPlayerStateChangedListener {
    private final String LOGTAG = "MainActivity";

    private FloatingActionButton btnPlay;
    private ProgressBar progressBar;
    private TextView infoTextView;
    private Animation playClickAnimation;

    private ForegroundService mAudioService;
    private boolean isServiceBound;
    private boolean showPlayButton;//to decide which icon to show on play button (not to wait until service start actual playing/pausing)

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        isServiceBound = false;
        showPlayButton = true;
        doBindService();

        /*Init the UI*/
        infoTextView = (TextView) findViewById(R.id.tv_info);
        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        progressBar.setVisibility(View.GONE);
        btnPlay = (FloatingActionButton) findViewById(R.id.btn_start_playing);
        btnPlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mAudioService.executeAction(Constants.Action.ACTION_PLAY);
                if(isNetworkAvailable()) {
                    if(mAudioService != null) {
                        mAudioService.doStartForeground();
                        btnPlay.startAnimation(playClickAnimation);
                        btnPlay.setImageResource(showPlayButton ? android.R.drawable.ic_media_play
                                                                : android.R.drawable.ic_media_pause);
                        showPlayButton = !showPlayButton;
                    }
                } else {
                    Toast.makeText(MainActivity.this, getString(R.string.no_network), Toast.LENGTH_SHORT).show();
                }
            }
        });
        playClickAnimation = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.click_play);
        initUI();
    }

    @Override
    protected void onDestroy() {
        doUnbindService();
        Log.d(LOGTAG, "onDestroy()");
        super.onDestroy();
    }

    private ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            mAudioService = ((ForegroundService.MusicServiceBinder) iBinder).getService();
            isServiceBound = true;
            mAudioService.setOnStateChangedListener(MainActivity.this);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mAudioService = null;
            isServiceBound = false;
        }
    };

    private void doBindService() {
        Intent serviceIntent = new Intent(MainActivity.this, ForegroundService.class);
        startService(serviceIntent);
        bindService(serviceIntent, mServiceConnection, Context.BIND_AUTO_CREATE);
    }

    private void doUnbindService() {
        if(isServiceBound) {
            mAudioService.setOnStateChangedListener(null);
            unbindService(mServiceConnection);
        }
    }

    private void initUI() {
        if(mAudioService != null) {
            onPlayerStateChanged(mAudioService.getCurrentState());
        }
    }

    @Override
    public void onPlayerStateChanged(Config.State newState) {
        switch (newState) {
            case PAUSED:
                infoTextView.setText(getString(R.string.paused));
                //btnPlay.setText(getString(R.string.start_playing));
                btnPlay.setImageResource(android.R.drawable.ic_media_play);
                progressBar.setVisibility(View.GONE);
                break;
            case PLAYING:
                infoTextView.setText(getString(R.string.playing));
                //btnPlay.setText(getString(R.string.pause_playing));
                btnPlay.setImageResource(android.R.drawable.ic_media_pause);
                progressBar.setVisibility(View.GONE);
                break;
            case PREPARING:
            case RESTART:
                infoTextView.setText(getString(R.string.preparing));
                //btnPlay.setText(getString(R.string.pause_playing));
                btnPlay.setImageResource(android.R.drawable.ic_media_pause);
                progressBar.setVisibility(View.VISIBLE);
                break;
            case STOPPED:
            default:
                infoTextView.setText("");
                //btnPlay.setText(getString(R.string.start_playing));
                btnPlay.setImageResource(android.R.drawable.ic_media_play);
                progressBar.setVisibility(View.GONE);
                break;
        }
    }

    @Override
    public void onPlayerBufferingPercentChanged(int percent) {
        infoTextView.setText(getString(R.string.buffering, percent));
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onResume() {
        /*if(mAudioService != null) {
            mAudioService.doStopForeground();
        }*/
        super.onResume();
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    //TODO Add BroadcastReceiver to track internet connection changes.
}