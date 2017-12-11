package com.apppool.demomusicplayer_v2;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.media.MediaMetadata;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.media.session.MediaController;
import android.media.session.PlaybackState;
import android.net.Uri;
import android.os.Bundle;
import android.os.PowerManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaBrowserServiceCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaButtonReceiver;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.support.v4.media.session.PlaybackStateCompat.Builder;
import android.support.v7.app.NotificationCompat;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;


public class MusicPlayerService extends MediaBrowserServiceCompat implements
        MediaPlayer.OnCompletionListener, AudioManager.OnAudioFocusChangeListener
{

    private MediaSessionCompat mediaSessionCompat;

    //song list
    private ArrayList<SongDetail> songs;
    //current position
    private int songPosn;

    private String songTitle = "";

    public  static MediaPlayer mediaPlayer;
    private PlaybackStateCompat.Builder playbackstateBuilder;

    public static final String ACTION_NEXT = "action_next";
    public static final String ACTION_PREVIOUS = "action_previous";
    MediaControllerCompat mediaControllerCompat;


    @Override
    public void onCreate()
    {
        super.onCreate();

        initMediaPlayer();
        initMediaSession();
        initNoisyReceiver();

    }

    @Nullable
    @Override
    public BrowserRoot onGetRoot(@NonNull String clientPackageName, int clientUid, @Nullable Bundle rootHints) {
        if(TextUtils.equals(clientPackageName, getPackageName())) {
            return new BrowserRoot(getString(R.string.app_name), null);
        }

        return null;
    }

    @Override
    public void onLoadChildren(@NonNull String parentId, @NonNull Result<List<MediaBrowserCompat.MediaItem>> result) {
        result.sendResult(null);

    }

    @Override
    public void onLoadChildren(@NonNull String parentId, @NonNull Result<List<MediaBrowserCompat.MediaItem>> result, @NonNull Bundle options) {
        super.onLoadChildren(parentId, result, options);
    }

    //It is the entry point to ur service
   @Override
    public int onStartCommand(Intent intent, int flags, int startId)
   {
       Log.i("called startCommand", "inside onStartCommand");
        handleIntent(mediaSessionCompat, intent);
        return super.onStartCommand(intent, flags, startId);
    }



    public void handleIntent(MediaSessionCompat mediaSessionCompat, Intent intent)
    {
        if(mediaSessionCompat != null && intent != null &&
                "android.intent.action.MEDIA_BUTTON".equals(intent.getAction()) &&
                intent.hasExtra("android.intent.extra.KEY_EVENT"))
        {
            Log.i("Check handle Intent", "Called handle intent ");
            KeyEvent ke = intent.getParcelableExtra("android.intent.extra.KEY_EVENT");
            MediaControllerCompat mediaControllerCompat = mediaSessionCompat.getController();
            mediaControllerCompat.dispatchMediaButtonEvent(ke);
        }
    }

    //Broadcast Receiver that listens for headphone state change
    private BroadcastReceiver headPhoneReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(mediaPlayer != null && mediaPlayer.isPlaying())
                mediaPlayer.pause();
        }
    };

    //initialise media Player
    private void initMediaPlayer()
    {
        mediaPlayer = new MediaPlayer();
        mediaPlayer.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);
        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        mediaPlayer.setVolume(1.0f, 1.0f);
    }

    //initialise Media Session
    private void initMediaSession()
    {
        ComponentName mediaButtonReceiver = new ComponentName(getApplicationContext(), MediaButtonReceiver.class);
        //Initialise Media Session
        mediaSessionCompat = new MediaSessionCompat(getApplicationContext(), "Tag", mediaButtonReceiver, null);
        // MySessionCallback() has methods that handle callbacks from a media controller
        mediaSessionCompat.setCallback(mediaSessionCallback);
        //Enable callbacks from MediaButtons and TransportControl
        mediaSessionCompat.setFlags(
              MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS |
                      MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS
        );

       /* Intent mediaButtonIntent = new Intent(Intent.ACTION_MEDIA_BUTTON);
        mediaButtonIntent.setClass(this, MediaButtonReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 1, mediaButtonIntent, 0);
        mediaSessionCompat.setMediaButtonReceiver(pendingIntent);*/

        // Set an initial PlaybackState with ACTION_PLAY, so media buttons can start the player
        playbackstateBuilder = new PlaybackStateCompat.Builder();
        playbackstateBuilder.setActions( PlaybackStateCompat.ACTION_PLAY | PlaybackStateCompat.ACTION_PLAY_PAUSE |
                PlaybackStateCompat.ACTION_PAUSE | PlaybackStateCompat.ACTION_SKIP_TO_NEXT |
                PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS );
        setMediaPlaybackState(PlaybackStateCompat.STATE_PLAYING);

        // Set the session's token so that client activities can communicate with it.
        setSessionToken(mediaSessionCompat.getSessionToken());

        // Set's the MediaSession to Active.
        mediaSessionCompat.setActive(true);
    }

    //initialise Noisy Receiver
    private void initNoisyReceiver()
    {
        //Handles headphones coming unplugged. cannot be done through a manifest receiver
        IntentFilter filter = new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY);
        registerReceiver(headPhoneReceiver, filter);
    }

    //set Callbacks for mediaSession Compat
    private MediaSessionCompat.Callback mediaSessionCallback = new MediaSessionCompat.Callback()
    {


        @Override
        public boolean onMediaButtonEvent(Intent mediaButtonEvent)
        {
            Log.i("Check onMediaButonEvent", "Called Onmediabuttonevent");
            System.out.println("Check " + mediaButtonEvent);
           KeyEvent ke = mediaButtonEvent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
            //ke.getKeyCode();
           long action =  ke.getAction();
            if(action == PlaybackState.ACTION_PLAY)
                onPlay();
            if(action == PlaybackState.ACTION_PAUSE)
            {
                Log.i("Check onMediaButonEvent", "going to call func onPause");
                onPause();
            }

            if(action == PlaybackState.ACTION_SKIP_TO_NEXT)
            {
                Log.i("Check onMediaButonEvent", "going to call func onskip to next");
                onSkipToNext();
            }
            return super.onMediaButtonEvent(mediaButtonEvent);
        }

        @Override
        public void onPlay()
        {
            super.onPlay();

            if( !successfullyRetrievedAudioFocus())
                return;
            mediaPlayer.start();
            setMediaPlaybackState(PlaybackStateCompat.STATE_PLAYING);
            showPlayingNotification();
            mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener()
            {
                @Override
                public void onCompletion(MediaPlayer mp)
                {
                    Log.i("Check","onCompletion");
                    mediaPlayer.stop();
                    setMediaPlaybackState(PlaybackStateCompat.STATE_STOPPED);
                    stopForeground(true);
                    showPausedNotification();
                }
            });

        }

        @Override
        public void onPause() {
            Log.i("Check pause", "on pause func called");
            super.onPause();
            if(mediaPlayer.isPlaying())
                mediaPlayer.pause();
            setMediaPlaybackState(PlaybackStateCompat.STATE_PAUSED);
            //stopForeground(true);
            showPausedNotification();
        }

        @Override
        public void onSkipToNext()
        {
            Log.i("Check skip to next", "on skip to next func called");
            setMediaPlaybackState(PlaybackStateCompat.STATE_SKIPPING_TO_PREVIOUS);
            if(++songPosn >= MainActivity.songList.size())
                songPosn = 0;
            String mediaId = String.valueOf(songPosn);
            initMediaSessionMetadata(mediaId);
            mediaPlayer.reset();
            SongDetail playSong = MainActivity.songList.get(songPosn);
            long playSongId = playSong.getId();
            Uri playSongUri = ContentUris.withAppendedId(
                    android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, playSongId);
            mediaPlayer = MediaPlayer.create( getApplicationContext(), playSongUri);
            onPlay();
            super.onSkipToNext();
        }

        @Override
        public void onSkipToPrevious()
        {
            setMediaPlaybackState(PlaybackStateCompat.STATE_SKIPPING_TO_PREVIOUS);
            if(--songPosn < 0)
                songPosn = (MainActivity.songList.size() - 1);
            String mediaId = String.valueOf(songPosn);
            initMediaSessionMetadata(mediaId);
            mediaPlayer.reset();
            SongDetail playSong = MainActivity.songList.get(songPosn);
            long playSongId = playSong.getId();
            Uri playSongUri = ContentUris.withAppendedId(
                    android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, playSongId);
            mediaPlayer = MediaPlayer.create( getApplicationContext(), playSongUri);
            onPlay();
            super.onSkipToPrevious();
        }

        @Override
        public void onPlayFromMediaId(String mediaId, Bundle extras)
        {
            super.onPlayFromMediaId(mediaId, extras);

            initMediaSessionMetadata(mediaId);

            songPosn = Integer.parseInt(mediaId);
            mediaPlayer.reset();
            //get song
            SongDetail playSong = MainActivity.songList.get(songPosn);
            //get id
            long currSongId = playSong.getId();
            //set uri
            Uri trackUri = ContentUris.withAppendedId(
                    android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, currSongId);

            mediaPlayer = MediaPlayer.create(getApplicationContext(), trackUri);

            onPlay();
        }
    };

    private void showPlayingNotification()
    {
        NotificationCompat.Builder builder = MediaStyleHelper.from(MusicPlayerService.this, mediaSessionCompat, MainActivity.class);
        if( builder == null ) {
            return;
        }


        builder.addAction(new NotificationCompat.Action(android.R.drawable.ic_media_previous, "Previous",
                //this will broadcast a pending state which then received by pending intent inside initMediaSessionCompat
                MediaButtonReceiver.buildMediaButtonPendingIntent(this, PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS)));
        builder.addAction(new NotificationCompat.Action(android.R.drawable.ic_media_pause, "Pause",
                //this will broadcast a pending state which then received by pending intent inside initMediaSessionCompat
                MediaButtonReceiver.buildMediaButtonPendingIntent(this, PlaybackStateCompat.ACTION_PAUSE)));
        builder.addAction(new NotificationCompat.Action(android.R.drawable.ic_media_next, "Next",
                //this will broadcast a pending state which then received by pending intent inside initMediaSessionCompat
                MediaButtonReceiver.buildMediaButtonPendingIntent(this, PlaybackStateCompat.ACTION_SKIP_TO_NEXT)));

        builder.setSmallIcon(R.mipmap.ic_launcher);
        //Take advantage of MediaStyle feature
        builder.setStyle(new NotificationCompat.MediaStyle().
                setShowActionsInCompactView(1).
                setMediaSession(mediaSessionCompat.getSessionToken()));

        NotificationManagerCompat.from(MusicPlayerService.this).notify(1, builder.build());
        startForeground(1, builder.build());
    }

    private void showPausedNotification()
    {
        NotificationCompat.Builder builder = MediaStyleHelper.from(this, mediaSessionCompat, MainActivity.class);
        if( builder == null ) {
            return;
        }

        builder.addAction(new NotificationCompat.Action(android.R.drawable.ic_media_previous, "Previous",
                //this will broadcast a pending state which then received by pending intent inside initMediaSessionCompat
                MediaButtonReceiver.buildMediaButtonPendingIntent(this, PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS)));

        builder.addAction(new NotificationCompat.Action(android.R.drawable.ic_media_play, "Play",
                MediaButtonReceiver.buildMediaButtonPendingIntent(this, PlaybackStateCompat.ACTION_PLAY)));
        builder.addAction(new NotificationCompat.Action(android.R.drawable.ic_media_next, "Next",
                //this will broadcast a pending state which then received by pending intent inside initMediaSessionCompat
                MediaButtonReceiver.buildMediaButtonPendingIntent(this, PlaybackStateCompat.ACTION_SKIP_TO_NEXT)));
        builder.setStyle(new NotificationCompat.MediaStyle().
                setShowActionsInCompactView(1).
                setMediaSession(mediaSessionCompat.getSessionToken()));
        builder.setSmallIcon(R.mipmap.ic_launcher);

        //Property for Sticky Notification & priority of the Notification.
        builder.setPriority(Notification.PRIORITY_HIGH);

        NotificationManagerCompat.from(this).notify(1, builder.build());
    }

    private void setMediaPlaybackState(int state)
    {
        /*PlaybackStateCompat.Builder playbackstateBuilder = new PlaybackStateCompat.Builder();
        if( state == PlaybackStateCompat.STATE_PLAYING ) {
            playbackstateBuilder.setActions(PlaybackStateCompat.ACTION_PLAY_PAUSE | PlaybackStateCompat.ACTION_PAUSE);
        } else if(state == PlaybackStateCompat.STATE_PAUSED) {
            playbackstateBuilder.setActions(PlaybackStateCompat.ACTION_PLAY_PAUSE | PlaybackStateCompat.ACTION_PLAY);
        } else if(state == PlaybackStateCompat.STATE_SKIPPING_TO_NEXT)
        {
            playbackstateBuilder.setActions(PlaybackStateCompat.ACTION_SKIP_TO_NEXT);
        }
        else if(state == PlaybackStateCompat.STATE_SKIPPING_TO_PREVIOUS)
        {
            playbackstateBuilder.setActions(PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS);
        }*/
        playbackstateBuilder.setState(state, PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN, 1);
        mediaSessionCompat.setPlaybackState(playbackstateBuilder.build());
    }

    private void initMediaSessionMetadata(String mediaId)
    {

        MediaMetadataCompat.Builder metadataBuilder = new MediaMetadataCompat.Builder();
        byte[] mediabyteArray = getMediaArtByteArray(MainActivity.songList.
                get(Integer.parseInt(mediaId)).getMediaArtPath());
        if(mediabyteArray != null)
        {
            //Notification icon in card.
            metadataBuilder.putBitmap(MediaMetadataCompat.METADATA_KEY_DISPLAY_ICON, MainActivity.
                    decodeBitmapFromByteArray(mediabyteArray ,SongDetail.TARGET_IMAGE_THUMBNAIL_WIDTH,
                            SongDetail.TARGET_IMAGE_THUMBNAIL_HEIGHT));
            metadataBuilder.putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, MainActivity.
                    decodeBitmapFromByteArray(mediabyteArray,SongDetail.TARGET_IMAGE_THUMBNAIL_WIDTH,
                            SongDetail.TARGET_IMAGE_THUMBNAIL_HEIGHT));

            //lock screen icon for pre lollipop
            metadataBuilder.putBitmap(MediaMetadataCompat.METADATA_KEY_ART, MainActivity.
                    decodeBitmapFromByteArray(mediabyteArray,SongDetail.TARGET_IMAGE_LOCKSCREEN_WIDTH,
                            SongDetail.TARGET_IMAGE_LOCKSCREEN_HEIGHT));
        }
        else
        {
            //Notification icon in card.
            metadataBuilder.putBitmap(MediaMetadataCompat.METADATA_KEY_DISPLAY_ICON, BitmapFactory.decodeResource
                    (getResources(),R.drawable.default_cover_art));
            metadataBuilder.putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, BitmapFactory.decodeResource
                    (getResources(),R.drawable.default_cover_art));

            //lock screen icon for pre lollipop
            metadataBuilder.putBitmap(MediaMetadataCompat.METADATA_KEY_ART, BitmapFactory.decodeResource
                    (getResources(),R.drawable.default_cover_art));
        }
        metadataBuilder.putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_TITLE,
                MainActivity.songList.get(Integer.parseInt(mediaId)).getTitle());
        metadataBuilder.putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_SUBTITLE,
                MainActivity.songList.get(Integer.parseInt(mediaId)).getArtist());

        mediaSessionCompat.setMetadata(metadataBuilder.build());
    }

    private boolean successfullyRetrievedAudioFocus() {
        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

        int result = audioManager.requestAudioFocus(this,
                AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);

        return result == AudioManager.AUDIOFOCUS_GAIN;
    }


    @Override
    public void onAudioFocusChange(int i)
    {
        switch( i ) {
            case AudioManager.AUDIOFOCUS_LOSS: {
                if( mediaPlayer.isPlaying() ) {
                    mediaPlayer.stop();
                }
                break;
            }
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT: {
                mediaPlayer.pause();
                break;
            }
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK: {
                if( mediaPlayer != null ) {
                    mediaPlayer.setVolume(0.3f, 0.3f);
                }
                break;
            }
            case AudioManager.AUDIOFOCUS_GAIN: {
                if( mediaPlayer != null ) {
                    if( !mediaPlayer.isPlaying() ) {
                        mediaPlayer.start();
                    }
                    mediaPlayer.setVolume(1.0f, 1.0f);
                }
                break;
            }
        }
    }

    @Override
    public void onCompletion(MediaPlayer mediaPlayer)
    {
        if( mediaPlayer != null )
        {
            mediaPlayer.release();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        audioManager.abandonAudioFocus(this);
        unregisterReceiver(headPhoneReceiver);
        mediaSessionCompat.release();
        mediaPlayer.release();
        NotificationManagerCompat.from(this).cancel(1);
    }

    public static byte[] getMediaArtByteArray(String path)
    {
        MediaMetadataRetriever metaDataArt = new MediaMetadataRetriever();
        try
        {
            metaDataArt.setDataSource(path);
        }
        catch (IllegalArgumentException e)
        {
            Log.i("Check","Exception Thrown");
        }
        return metaDataArt.getEmbeddedPicture();
    }

}
