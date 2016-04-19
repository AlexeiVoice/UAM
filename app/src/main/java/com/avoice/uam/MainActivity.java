package com.avoice.uam;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.avoice.uam.interfaces.OnPlayerStateChangedListener;
import com.avoice.uam.util.Config;
import com.avoice.uam.util.Constants;

import java.io.IOException;

public class MainActivity extends AppCompatActivity implements OnPlayerStateChangedListener {
    private final String LOGTAG = "MainActivity";

    private Button btnPlay;
    private ProgressBar progressBar;
    private TextView infoTextView;

    private ForegroundService mAudioService;
    private boolean isServiceBound;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        isServiceBound = false;
        doBindService();

        /*Init the UI*/
        infoTextView = (TextView) findViewById(R.id.tv_info);
        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        progressBar.setVisibility(View.GONE);
        btnPlay = (Button) findViewById(R.id.btn_start_playing);
        btnPlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(isNetworkAvailable()) {
                    if(mAudioService != null) {
                        mAudioService.doStartForeground();
                        mAudioService.executeAction(Constants.Action.ACTION_PLAY);
                    }
                } else {
                    Toast.makeText(MainActivity.this, getString(R.string.no_network), Toast.LENGTH_SHORT).show();
                }
            }
        });
        initUI();
    }

    @Override
    protected void onDestroy() {
        //mAudioService.doStartForeground();
        doUnbindService();
        Log.d(LOGTAG, "onDestroy()");
        super.onDestroy();
    }

    private ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            mAudioService = ((ForegroundService.MusicServiceBinder) iBinder).getService();
            isServiceBound = true;
            mAudioService.setOnStateChangeListener(MainActivity.this);
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
            mAudioService.setOnStateChangeListener(null);
            unbindService(mServiceConnection);
        }
    }

    private void initUI() {
        if(mAudioService != null) {
            onPlayerStateChange(mAudioService.getCurrentState());
        }
    }

    @Override
    public void onPlayerStateChange(Config.State newState) {
        switch (newState) {
            case PAUSED:
                infoTextView.setText(getString(R.string.paused));
                btnPlay.setText(getString(R.string.start_playing));
                progressBar.setVisibility(View.GONE);
                break;
            case PLAYING:
                infoTextView.setText(getString(R.string.playing));
                btnPlay.setText(getString(R.string.pause_playing));
                progressBar.setVisibility(View.GONE);
                break;
            case PREPARING:
            case RESTART:
                infoTextView.setText(getString(R.string.preparing));
                btnPlay.setText(getString(R.string.pause_playing));
                progressBar.setVisibility(View.VISIBLE);
                break;
            case STOPPED:
            default:
                infoTextView.setText("");
                btnPlay.setText(getString(R.string.start_playing));
                progressBar.setVisibility(View.GONE);
                break;
        }
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
}