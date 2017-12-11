package com.apppool.demomusicplayer_v2;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;
import java.util.Locale;


public class MySongAdapter extends RecyclerView.Adapter<MySongAdapter.SongViewHolder>
{
    private LayoutInflater inflater;
    private List<SongDetail> songList;
    public ClickListener clickListener;
    public MySongAdapter(Context c, List<SongDetail> song)
    {
        Log.i("Called songAdapter cons","Inside SongAdapter Constructor");
        inflater = LayoutInflater.from(c);
        songList = song;
    }

    public void setClickListener(ClickListener clickListener)
    {
        this.clickListener = clickListener;
    }

    @Override
    public MySongAdapter.SongViewHolder onCreateViewHolder(ViewGroup parent, int viewType)
    {
        Log.i("Caled onCreateVewholder", "Inside onCreateViewHolder method");
        View view = inflater.inflate(R.layout.song, parent, false);
        SongViewHolder viewHolder = new SongViewHolder(view);
        return viewHolder;
    }

    @Override
    public int getItemCount() {
        return songList.size();
    }

    @Override
    public void onBindViewHolder(SongViewHolder holder, int position)
    {
        Log.i("Called onBindViewholder", "Inside onBindViewHolder method");
        SongDetail currSong = songList.get(position);
        //get title and artist Strings
        holder.titleView.setText(currSong.getTitle());
        holder.artistView.setText(currSong.getArtist());
        BitmapLoaderTask loaderTask = new BitmapLoaderTask(holder.trackArt);
        loaderTask.execute(currSong.getMediaArtPath(),Long.toString(currSong.getId()));

        //set the duration of song
        long milliSeconds =  currSong.getDuration();
        int minutesLength = ( (int) milliSeconds/1000 ) / 60;
        int secondsLength = ( (int) milliSeconds/1000 ) % 60;

        if(secondsLength < 10)
        {
            holder.durationView.setText(minutesLength + ":" +
                    String.format(Locale.getDefault(), "%02d", secondsLength ));
        }
        else holder.durationView.setText(minutesLength+":"+secondsLength);
        System.out.println("Setted the value of duration TextView");
        //System.out.println("Value of counter......"+(++counter));
        System.out.println("Value of duration is" + minutesLength + ":" + secondsLength);

        Log.i("Check","Looking For cache with key "+Long.toString(currSong.getId()));
        System.out.println("Sending imageview");

      /*  Bitmap musicArtBitmap = MainActivity.getBitmapFromCache(Long.toString(currSong.getId()));
        if(musicArtBitmap != null)
        {
            Log.i("Check","Populating Cached Bitmap");
            holder.trackArt.setImageBitmap(musicArtBitmap);
        }*/
       /* else
        {
            System.out.println("Executing else");
            BitmapLoaderTask loaderTask = new BitmapLoaderTask(holder.trackArt);
            loaderTask.execute(currSong.getMediaArtPath(),Long.toString(currSong.getId()));
           *//* Bitmap mArtBitmap = MainActivity.getBitmapFromCache(Long.toString(currSong.getId()));
            holder.trackArt.setImageBitmap(mArtBitmap);*//*
        }*/
       /* else if(checkBitmapLoaderTask(currSong.getMediaArtPath(),holder.trackArt))
        {
            BitmapLoaderTask loaderTask = new BitmapLoaderTask(holder.trackArt);
            AsyncArtLoader artLoader = new AsyncArtLoader(holder.trackArt.getResources(),
                    placeHolderBitmap, loaderTask, currSong.getMediaArtPath(), currSong.getId());
            holder.trackArt.setImageDrawable(artLoader);
        }*/
    }

    class SongViewHolder extends RecyclerView.ViewHolder
    {
        TextView titleView;
        TextView artistView;
        TextView durationView;
        ImageView trackArt;

        public SongViewHolder(View itemView)
        {
            super(itemView);
            Log.i("Called SongViewHolder", "Inside SongViewHolder");
            //map to the textViews
            titleView = (TextView) itemView.findViewById(R.id.song_title);
            artistView = (TextView) itemView.findViewById(R.id.song_artist);
            durationView = (TextView) itemView.findViewById(R.id.song_duration);
            trackArt = (ImageView) itemView.findViewById(R.id.imgview);
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    clickListener.itemClicked(view, getLayoutPosition());
                }
            });
        }
    }

    //helper for listen for onClick Listener for  recycler view items
    interface ClickListener
    {
        void itemClicked(View v, int position);
    }
}
