package com.apppool.demomusicplayer_v2;

import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.Image;
import android.net.Uri;
import android.os.Handler;
import android.os.RemoteException;
import android.provider.MediaStore;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.support.v4.util.LruCache;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.transition.Fade;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.sothree.slidinguppanel.SlidingUpPanelLayout;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class MainActivity extends AppCompatActivity implements SeekBar.OnSeekBarChangeListener,
        SlidingUpPanelLayout.PanelSlideListener, MySongAdapter.ClickListener
{

    public static ArrayList<SongDetail> songList;
    private ListView songListView;
    private RecyclerView recyclerView;

    private static final int STATE_PAUSED = 0;
    private static final int STATE_PLAYING = 1;

    //Cache for Music Art Bitmap
    private static LruCache<String, Bitmap> artCache;

    private int currentState;

    private MediaBrowserCompat mediaBrowser;
   // private MusicPlayerService musicService;

    private SeekBar seekbar;
    private ImageView imageView;
    private TextView textView;
    private Button btn_play;
    private Button btn_back;
    private Button btn_next;
    Handler handler;
    SlidingUpPanel slidingUpPanel;
    private Animation animationFadeout;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //songListView = (ListView) findViewById(R.id.song_list);
        recyclerView = (RecyclerView) findViewById(R.id.recycler_view);

        songList = new ArrayList<>();

        //for sliding up panel
        textView = (TextView) findViewById(R.id.showartist);
        seekbar = (SeekBar) findViewById(R.id.seekbar);
        imageView = (ImageView) findViewById(R.id.albumart);
        btn_back = (Button) findViewById(R.id.skipback);
        btn_play = (Button) findViewById(R.id.play);
        btn_next = (Button) findViewById(R.id.skipnext);


        mediaBrowser = new MediaBrowserCompat(this, new ComponentName(this, MusicPlayerService.class),
                connectionCallback, getIntent().getExtras());

        //fetch music from device
        getSongList();

        //sort the songs list alphabetically acc to title
        Collections.sort(songList, new Comparator<SongDetail>() {
            public int compare(SongDetail a, SongDetail b) {
                return a.getTitle().compareTo(b.getTitle());
            }
        });

        System.out.println("Going inside Song Adapter");

        MySongAdapter songAdapter = new MySongAdapter(this, songList);
        songAdapter.setClickListener(this);
        System.out.println("Coming out of song Adapter Song Adapter");
        //songListView.setAdapter(songAdapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(getApplicationContext()));
        recyclerView.setAdapter(songAdapter);

        //set the Total no of songs
        TextView sizeSongListView = (TextView) findViewById(R.id.size_song_listView);
        sizeSongListView.setText(songList.size()+" Total Songs");

        /*//set the listener for listview items
        songListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l)
            {
                String mediaId = Integer.valueOf(position).toString();
                setSlidingUpPanelData(mediaId);
                getSupportMediaController().getTransportControls().playFromMediaId(mediaId, null);
                //getSupportMediaController().getTransportControls().play();
            }
        });*/

        //Cache initialisation
        final int maxMemorySize = (int) Runtime.getRuntime().maxMemory() / 1024;
        final int cacheSize = maxMemorySize / 10;
        Log.d("Cache Info", "Creating Cache with size" + cacheSize);
        artCache = new LruCache<String, Bitmap>(cacheSize)
        {
            @Override
            protected int sizeOf(String key, Bitmap value) {
                return value.getByteCount() / 1024;
            }
        };


        animationFadeout = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.fade_out);
        handler = new Handler();
        seekbar.setOnSeekBarChangeListener(this);

        btn_play.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getSupportMediaController().getTransportControls().play();
            }
        });


        btn_back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getSupportMediaController().getTransportControls().skipToPrevious();
            }
        });


        btn_next.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getSupportMediaController().getTransportControls().skipToNext();
            }
        });
    }

    //get the list of songs from device
    public void getSongList()
    {
        ContentResolver resolver = getContentResolver();
        Uri musicUri = android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        String selection = MediaStore.Audio.Media.IS_MUSIC + " != 0";

        Cursor cursor = resolver.query(musicUri, null, selection, null, null);

        if ( (cursor != null && cursor.moveToFirst()) ) {
            try {
                do {
                    //get Columns
                    int titleCol = cursor.getColumnIndex(MediaStore.Audio.Media.TITLE);
                    int idColumn = cursor.getColumnIndex(MediaStore.Audio.Media._ID);
                    int artistColumn = cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST);
                    int durationCol = cursor.getColumnIndex(MediaStore.Audio.Media.DURATION);
                    //int albumIdCol = cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID);


                    //get values
                    long id = cursor.getLong(idColumn);
                    String title = cursor.getString(titleCol);
                    String artist = cursor.getString(artistColumn);
                    long duration = cursor.getLong(durationCol);
                    //long albumId = cursor.getLong(albumIdCol);

                    //try at MediaMetaDataRetriever
                    String path = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DATA));

                    //Formatting details to fit view page correctly
                    StringBuilder titleBuild = new StringBuilder();
                    titleBuild.append(title);
                    if (titleBuild.length() > 21) {
                        titleBuild.setLength(21);
                        title = titleBuild.toString() + "...";
                    } else title = titleBuild.toString();

                    StringBuilder artistBuild = new StringBuilder();
                    artistBuild.append(artist);
                    if (artistBuild.length() > 20) {
                        artistBuild.setLength(20);
                        artist = artistBuild.toString() + "...";
                    } else artist = artistBuild.toString();

                    //Pass these value to SongDetail Class to initialise it
                    SongDetail sDetail = new SongDetail(id, title, artist, duration, path);
                    //Add to the list
                    songList.add(sDetail);

                } while (cursor.moveToNext());
            } finally
            {
               cursor.close();
            }

        }
    }

    public void setSlidingUpPanelData(String mediaId)
    {
       byte[] bytes = MusicPlayerService.getMediaArtByteArray( songList.
               get(Integer.parseInt(mediaId)).getMediaArtPath() );
       Bitmap bm = decodeBitmapFromByteArray(bytes, 330,300);
        imageView.setImageBitmap(bm);

        int mediapos = MusicPlayerService.mediaPlayer.getCurrentPosition();
        int mediaMax = MusicPlayerService.mediaPlayer.getDuration();

        seekbar.setMax(mediaMax);
        seekbar.setProgress(mediapos);

        handler.removeCallbacks(moveSeekBarThread);
    }

    //Listener for seekbar
    @Override
    public void onProgressChanged(SeekBar seekBar, int i, boolean b)
    {
        if(b)
        {
            MusicPlayerService.mediaPlayer.seekTo(i);
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        
    }

    /**Thread to move seekbar based on the current position
     * of the song
     */
    private Runnable moveSeekBarThread = new Runnable() {
        public void run() {
            if(MusicPlayerService.mediaPlayer.isPlaying()){

                int mediaPos_new = MusicPlayerService.mediaPlayer.getCurrentPosition();
                int mediaMax_new = MusicPlayerService.mediaPlayer.getDuration();
                seekbar.setMax(mediaMax_new);
                seekbar.setProgress(mediaPos_new);

                handler.postDelayed(this, 100); //Looping the thread after 0.1 second
            }

        }
    };

    @Override
    public void onStart()
    {
        super.onStart();
        mediaBrowser.connect();

        Intent intent = new Intent(this, MusicPlayerService.class);
        startService(intent);
    }

    @Override
    public void onStop()
    {
        super.onStop();
      //--------------------------------INCOMPLETE---------------------------------
        mediaBrowser.disconnect();

    }


    @Override
    protected void onDestroy() {
     //   super.onDestroy();
        Log.i("called onDestroy", "Inside onDestroy meyhod");
        if( mediaBrowser != null )
        {
            mediaBrowser.disconnect();
        }
        if(!MusicPlayerService.mediaPlayer.isPlaying())
        {
            stopService(new Intent(this, MusicPlayerService.class));
        }
    }

    private final MediaBrowserCompat.ConnectionCallback connectionCallback =
            new MediaBrowserCompat.ConnectionCallback()
            {
                @Override
                public void onConnected()
                {

                    try {

                        MediaControllerCompat mediaControllerCompat = new
                                MediaControllerCompat(MainActivity.this, mediaBrowser.getSessionToken());
                        mediaControllerCompat.registerCallback(mediaControllerCompatCallback);
                        setSupportMediaController(mediaControllerCompat);

                    } catch (RemoteException ex) {//show some message}

                        // Finish building the UI --------------INCOMPLETE----------------------
                        // buildTransportControls();

                    }


                }


            };

    private MediaControllerCompat.Callback mediaControllerCompatCallback = new MediaControllerCompat.Callback()
    {

        @Override
        public void onPlaybackStateChanged(PlaybackStateCompat state) {
            super.onPlaybackStateChanged(state);
            if( state == null ) {
                return;
            }

            switch( state.getState() ) {
                case PlaybackStateCompat.STATE_PLAYING: {
                    currentState = STATE_PLAYING;
                    break;
                }
                case PlaybackStateCompat.STATE_PAUSED: {
                    currentState = STATE_PAUSED;
                    break;
                }

                case PlaybackStateCompat.STATE_SKIPPING_TO_PREVIOUS:
                {
                    currentState = STATE_PLAYING;
                    break;
                }

                case PlaybackStateCompat.STATE_SKIPPING_TO_NEXT:
                {
                    currentState = STATE_PLAYING;
                    break;
                }
            }
        }


    };


    //Function for decoding byte[]
    public static Bitmap decodeBitmapFromByteArray(byte[] artByteArray, int reqWidth, int reqHeight)
    {
        if(artByteArray != null)
        {
            //First decode with inJustDecodeBounds = true to check dimension
            final BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            options.inSampleSize = 2;
            BitmapFactory.decodeByteArray(artByteArray, 0, artByteArray.length, options);

            //calculate inSampleSize
            options.inSampleSize = calculateInSize(options, reqWidth, reqHeight);

            //Decode Bitmap with inSampleSize set
            options.inJustDecodeBounds = false;

            return BitmapFactory.decodeByteArray(artByteArray, 0, artByteArray.length, options);
        }
        else return null;
    }

    public static int calculateInSize(BitmapFactory.Options options, int reqWidth, int reqHeight)
    {
        //Raw width and height of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if(height > reqHeight || width > reqWidth)
        {
            //calculate ratios of height and width to the requested height and width
            final int heightRatio = Math.round((float) height / (float) reqHeight);
            final int widthRatio = Math.round((float) width / (float) reqWidth);

            // Choose the smallest ratio as inSampleSize value, this will guarantee
            // a final image with both dimensions larger than or equal to the
            // requested height and width.
            inSampleSize = heightRatio < widthRatio ? heightRatio : widthRatio;
        }

        return inSampleSize;
    }

    //Function for cache
    public static Bitmap getBitmapFromCache(String key)
    {
        return artCache.get(key);
    }

    public static void setBitmapToCache(String key, Bitmap bitmap)
    {
        if(key != null && bitmap != null)
            artCache.put(key, bitmap);
        else
            Log.d("Cache Error","Key/Bitmap Null");
    }

    @Override
    public void onPanelSlide(View panel, float slideOffset)
    {
        textView.setVisibility(View.GONE);
        textView.startAnimation(animationFadeout);
    }

    @Override
    public void onPanelStateChanged(View panel, SlidingUpPanelLayout.PanelState previousState, SlidingUpPanelLayout.PanelState newState)
    {
        if(newState == SlidingUpPanelLayout.PanelState.EXPANDED)
            textView.startAnimation(animationFadeout);
    }


    @Override
    public void itemClicked(View v, int position)
    {
        String mediaId = Integer.valueOf(position).toString();
        setSlidingUpPanelData(mediaId);
        getSupportMediaController().getTransportControls().playFromMediaId(mediaId, null);
    }
}
