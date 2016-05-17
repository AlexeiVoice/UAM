package com.avoice.uam;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.v7.app.NotificationCompat;
import android.util.Log;

import com.avoice.uam.listener.OnPlayerStateChangedListener;
import com.avoice.uam.listener.OnTrackChangedListener;
import com.avoice.uam.model.Track;
import com.avoice.uam.util.Config;
import com.avoice.uam.util.Constants;
import com.avoice.uam.util.RadioXmlParser;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Timer;

public class ForegroundService extends Service {
    //region FIELDS
    private final String TAG = "ForegroundService";
    private final String WIFILOCK = "UAM_lock";

    private final String RADIOUID = "48ae349f-4283-46cb-9777-f28664875942";
    private final String APIKEY = "c784ff98-1f17-4d1d-91c1-6337438474a7";

    private MediaPlayer mPlayer;
    private Config.State currentState;
    private WifiManager.WifiLock wifiLock;

    private final IBinder mBiinder = new MusicServiceBinder();
    private OnPlayerStateChangedListener stateChangedListener;
    private OnTrackChangedListener trackChangedListener;

    private PendingIntent startingActivityIntent, pPlayIntent, pQuitIntent;
    private NotificationCompat.Builder notificationBuilder;
    private NotificationManager notificationManager;

    private boolean notificationShown;

    private Track currentTrack;
    /** For downloading track cover using Picasso lib*/
    private Target target;
    //endregion

    public ForegroundService() {
        currentState = Config.State.STOPPED;
    }

    /**
     * Class for clients to access.
     */
    public class MusicServiceBinder extends Binder {
        public ForegroundService getService() {
            return ForegroundService.this;
        }
    }

    //region > OVERRIDE METHODS
    @Override
    public void onCreate() {
        //notificationBuilder = new NotificationCompat.Builder(getApplicationContext());
        notificationManager = (NotificationManager) getSystemService(this.NOTIFICATION_SERVICE);

        wifiLock = ((WifiManager) getSystemService(Context.WIFI_SERVICE))
                .createWifiLock(WifiManager.WIFI_MODE_FULL, WIFILOCK);
        notificationShown = false;
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        if(intent != null){
            String action = intent.getAction();
        if(action != null) {
            switch (action) {
            /*case Constants.Action.START_FOREGROUND:
                doStartForeground();
                break;
            case Constants.Action.STOP_FOREGROUND:
                stopForeground(true);
                break;*/
                case Constants.Action.ACTION_PLAY:
                    executeAction(action);
                    break;
                case Constants.Action.ACTION_QUIT:
                    stopPlaying();
                    doStopForeground();
                    doStopSelf();
                    break;
                default:
                    break;
            }
        }
        }
        return START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBiinder;
    }

    @Override
    public void onLowMemory() {
        if (mPlayer != null) {
            mPlayer.release();
            mPlayer = null;
        }
        if(wifiLock.isHeld()) {
            wifiLock.release();
        }
    }

    @Override
    public void onDestroy() {
        cleanUp();
        Log.d(TAG, "onDestroy()");
        super.onDestroy();
    }
    //endregion

    public void executeAction(String action) {

        if(action.equals(Constants.Action.ACTION_PLAY)){
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
                    Log.i(TAG, "executeAction(): unknown action");
                    //stopPlaying();
                    break;
            }
        }
    }

    private void notifyStateChanged() {
        Log.d(TAG, currentState.toString());
        if(stateChangedListener != null) {
            stateChangedListener.onPlayerStateChanged(currentState);
        }
        updateState();
    }

    /**
     * Notify if buffering percentage changed (hence works only in PREPARING state).
     * @param bufferingPercentage
     */
    private void notifyStateChanged(int bufferingPercentage) {
        if(currentState == Config.State.PREPARING && stateChangedListener != null) {
            stateChangedListener.onPlayerBufferingPercentChanged(bufferingPercentage);
        }
    }

    private void notifyTrackChanged(Track newTrack) {
        Log.d(TAG, newTrack.getArtist() + " - " + newTrack.getTitle() + ".\nCover URL: "
                + newTrack.getCoverUrl() + " Time left: " + newTrack.getTimeLeft());
        updateTrackInfo(newTrack);
        if (trackChangedListener != null) {
            trackChangedListener.onTrackChanged(newTrack);
        }
        //delay (if some troubles - delay 60sec)
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                getSongInfo();
            }
        }, Integer.getInteger(newTrack.getTimeLeft(), 60000));

    }

    private void updateState(){
        String notifyText;
        switch (currentState) {
            case PLAYING:
                if (currentTrack != null) {
                    notifyText = getString(R.string.playing, currentTrack.toString());
                } else {
                    notifyText = getString(R.string.playing, "");
                }
                break;
            case RESTART:
            case PREPARING:
                notifyText = getString(R.string.preparing);
                break;
            case PAUSED:
                notifyText = getString(R.string.paused);
                break;
            case STOPPED:
            default: notifyText = "stopped";
                break;
        }
        updateNotification(notifyText);
    }

    private void updateTrackInfo(Track newTrack) {
        //persist current track
        currentTrack = new Track(newTrack);
        //update notification
        String notifyText = getString(R.string.playing, currentTrack.toString());
        if (newTrack.getCoverUrl() != "") {
            updateNotification(notifyText, newTrack.getCoverUrl());
        } else {
            updateNotification(notifyText);
        }
    }

    //region > PLAYBACK METHODS
    private void play(String url) {
        Log.d(TAG, "play()");
        //final Uri playUrl = Uri.parse(url);
        if(currentState == Config.State.STOPPED) {
            mPlayer = new MediaPlayer();
            currentState = Config.State.PREPARING;
            notifyStateChanged();
            try {
                mPlayer.setDataSource(url);
                mPlayer.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);
                mPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
                mPlayer.setVolume(Config.AUDIO_VOLUME, Config.AUDIO_VOLUME);
                mPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                    @Override
                    public void onPrepared(final MediaPlayer mediaPlayer) {
                        Log.d(TAG, "onPrepared");
                        currentState = Config.State.PLAYING;
                        notifyStateChanged();
                        /*new Thread(new Runnable() {
                            @Override
                            public void run() {
                                final MediaPlayer player = mediaPlayer;
                                player.start();
                            }
                        });*/
                        mediaPlayer.start();
                    }
                });
                mPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                    @Override
                    public void onCompletion(MediaPlayer mediaPlayer) {
                        Log.d(TAG, "onCompletion()");
                        //TODO Deal with this case: after unpausing player only plays part that happened to be downloaded before pause.
                        /*currentState = Config.State.PAUSED;
                        notifyStateChanged();*/
                        currentState = Config.State.PAUSED;
                        notifyStateChanged();
                    }
                });
                mPlayer.setOnBufferingUpdateListener(new MediaPlayer.OnBufferingUpdateListener() {
                    @Override
                    public void onBufferingUpdate(MediaPlayer mediaPlayer, int i) {
                        notifyStateChanged(i);
                    }
                });
                mPlayer.prepareAsync();

            } catch (IOException | IllegalStateException | IllegalArgumentException |SecurityException
                    e ) {
                Log.e(TAG, "playSound() error: " + e.toString());
                return;
            }
        } else if(currentState == Config.State.PAUSED) {
            //if player was just paused (and not stopped) we can continue playing by "unpausing" it
            mPlayer.start();
            currentState = Config.State.PLAYING;
            notifyStateChanged();
        }
        if(!wifiLock.isHeld()){
            wifiLock.acquire();
        }
    }

    private void pause() {
        Log.d(TAG, "pause()");
        if (mPlayer != null) {
            try {
                if(mPlayer.isPlaying()) {
                    mPlayer.pause();
                    currentState = Config.State.PAUSED;
                    notifyStateChanged();
                }
            } catch (IllegalStateException e) {
                Log.e(TAG, "pause(): " + e.toString());
                mPlayer.release();
                currentState = Config.State.STOPPED;
                notifyStateChanged();
                if(wifiLock.isHeld()) {
                    wifiLock.release();
                }
            }
        } else {
            currentState = Config.State.STOPPED;
            notifyStateChanged();
        }
        if(wifiLock.isHeld()) {
            wifiLock.release();
        }
    }

    private boolean stopPlaying() {
        currentState = Config.State.STOPPED;
        notifyStateChanged();
        Log.d(TAG, "stopPlaying()");
        if (mPlayer == null) {
            if(wifiLock.isHeld()) {
                wifiLock.release();
            }
            return false;
        } else {
            try {
                if(mPlayer.isPlaying()) {
                    mPlayer.stop();
                }
                mPlayer.release();
                mPlayer = null;
            } catch (IllegalStateException e) {
                Log.e(TAG, "stopPlaying: " + e.toString());
                mPlayer.release();
                mPlayer = null;
                if(wifiLock.isHeld()) {
                    wifiLock.release();
                }
                return false;
            }
            if(wifiLock.isHeld()) {
                wifiLock.release();
            }
            return true;
        }
    }
    //endregion

    public void setOnStateChangedListener(OnPlayerStateChangedListener listener) {
        this.stateChangedListener = listener;
    }
    public void setOnTrackChangedListener(OnTrackChangedListener listener) {
        this.trackChangedListener = listener;
    }

    private void initActionIntents() {
        //Showing MainActivity intent
        Intent notificationIntent = new Intent(this, MainActivity.class);
        notificationIntent.setAction(Constants.Action.ACTION_PLAY);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startingActivityIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

        //Play/Pause intent
        Intent playIntent = new Intent(this, ForegroundService.class);
        playIntent.setAction(Constants.Action.ACTION_PLAY);
        pPlayIntent = PendingIntent.getService(this, 0, playIntent, 0);

        //Quit the app intent
        Intent quitIntent = new Intent(this, ForegroundService.class);
        quitIntent.setAction(Constants.Action.ACTION_QUIT);
        pQuitIntent = PendingIntent.getService(this, 0, quitIntent, 0);
    }

    private Notification generateNotification(){
        notificationBuilder = new NotificationCompat.Builder(getApplicationContext());
        //if(builder != null) {
            Bitmap icon = BitmapFactory.decodeResource(getResources(),
                    R.mipmap.ic_launcher);

        notificationBuilder.setStyle(new NotificationCompat.MediaStyle()
                            .setShowActionsInCompactView(0, 1)
                            .setCancelButtonIntent(pQuitIntent)
                            .setShowCancelButton(true))
                    .setCategory(NotificationCompat.CATEGORY_TRANSPORT)
                    .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setContentTitle(Config.NOTIFICATION_TITLE)
                    .setTicker(Config.NOTIFICATION_TITLE)
                    .setContentText("Radio")
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setLargeIcon(icon)
                    .setContentIntent(startingActivityIntent)
                    .setDeleteIntent(pQuitIntent)
                    .setOngoing(true)
                    .setOnlyAlertOnce(true)
                    .setAutoCancel(true)
                    .addAction(currentState == Config.State.PLAYING ? android.R.drawable.ic_media_pause
                                    : android.R.drawable.ic_media_play,
                            currentState == Config.State.PLAYING ? "Pause"
                                    : "Play", pPlayIntent)
                    .addAction(android.R.drawable.ic_delete, "Quit", pQuitIntent);
            return notificationBuilder.build();
        //} else {
        //    return null;
        //}
    }

    private void updateNotification(String text) {
        if(notificationShown){
            notificationBuilder.mActions.get(0).icon = currentState == Config.State.PLAYING ?
                    android.R.drawable.ic_media_pause : android.R.drawable.ic_media_play;
            notificationBuilder.setContentText(text);
            notificationManager.notify(Constants.SERVICE_ID, notificationBuilder.build());
        }
    }

    private void updateNotification(final String text, final String coverUrl) {
        if(notificationShown){
            target = new Target() {
                @Override
                public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
                    notificationBuilder
                            .setContentText(text)
                            .setLargeIcon(bitmap);
                    notificationManager.notify(Constants.SERVICE_ID, notificationBuilder.build());
                }

                @Override
                public void onBitmapFailed(Drawable errorDrawable) {

                }

                @Override
                public void onPrepareLoad(Drawable placeHolderDrawable) {

                }
            };
            Picasso.with(getApplicationContext()).load(coverUrl).into(target);
        }
    }

    //region > ACTIVITY INTERACTION METHODS
    public Config.State getCurrentState() {
        return currentState;
    }

    public void doStartForeground() {
        initActionIntents();
        startForeground(Constants.SERVICE_ID, generateNotification());
        notificationShown = true;
    }

    public void doStopForeground() {
        Log.d(TAG, "doStopForeground()");
        stopForeground(true);
    }
    //endregion

    public void doStopSelf() {
        cleanUp();
        stopSelf();
    }

    /**
     * Stops mediaplayer, releases locks, cancels requests.
     */
    public void cleanUp() {
        if (mPlayer != null) {
            mPlayer.release();
            mPlayer = null;
        }
        if(wifiLock.isHeld()) {
            wifiLock.release();
        }
        if (target != null) {
            Picasso.with(getApplicationContext()).cancelRequest(target);
        }
    }
    public class FetchRadioInfoTask extends AsyncTask<Constants.ApiCalls, Void, Track[]> {
        @Override
        protected Track[] doInBackground(Constants.ApiCalls... params) {
            HttpURLConnection urlConnection;
            RadioXmlParser radioXmlParser;
            InputStream inputStream;

                    String type = "xml";
            String callmeback = "yes";
            String cover = "yes";
            String previous = "yes";

            if (params[0] == Constants.ApiCalls.PREVIOUS_TRACKS) {
                //TODO Implement "previous tracks" request
                return new Track[0];
            }

            final String BASE_URL = "http://api.radionomy.com/";
            final String REQUEST_PARAM  = params[0] == Constants.ApiCalls.CURRENT_TRACK ?
                    "currentsong.cfm" : "tracklist.cfm";
            final String RADIOUID_PARAM = "radiouid";
            final String APIKEY_PARAM = "apikey";
            final String CALLMEBACK_PARAM = "callmeback";
            final String TYPE_PARAM = "type";
            final String COVER_PARAM = "cover";
            final String PREVIOUS_PARAM = "previous";

            Uri builtUri = Uri.parse(BASE_URL + REQUEST_PARAM + "?").buildUpon()
                    .appendQueryParameter(RADIOUID_PARAM, RADIOUID)
                    .appendQueryParameter(APIKEY_PARAM, APIKEY)
                    .appendQueryParameter(CALLMEBACK_PARAM, callmeback)
                    .appendQueryParameter(TYPE_PARAM, type)
                    .appendQueryParameter(COVER_PARAM, cover)
                    .appendQueryParameter(PREVIOUS_PARAM, previous)
                    .build();
            URL url = null;
            try {
                url = new URL(builtUri.toString());
            } catch (MalformedURLException e) {
                e.printStackTrace();
                Log.e(TAG, "FetchRadioInfoTask. Error: " + e.toString());
            }

            try {
                //create a request and connect
                urlConnection = (HttpURLConnection)url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.connect();
                //now let's read the stream
                inputStream = urlConnection.getInputStream();
                if (inputStream == null) {
                    return null;
                } else {
                    radioXmlParser = new RadioXmlParser();
                    Track track = radioXmlParser.parse(inputStream);
                    return new Track[]{track};
                }
            } catch (IOException | XmlPullParserException e) {
                e.printStackTrace();
                Log.e(TAG, "FetchRadioInfoTask. Error: " + e.toString());
            }
            return null;
        }

        @Override
        protected void onPostExecute(Track[] tracks) {
            notifyTrackChanged(tracks[0]);
            super.onPostExecute(tracks);
        }
    }

    /*DEBUG*/
    public void getSongInfo() {
        new FetchRadioInfoTask().execute(Constants.ApiCalls.CURRENT_TRACK);
    }
    /*ENDOFDEBUG*/
}
