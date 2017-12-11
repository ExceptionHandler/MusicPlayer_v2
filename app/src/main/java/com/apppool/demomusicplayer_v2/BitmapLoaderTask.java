package com.apppool.demomusicplayer_v2;

import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.ImageView;

import java.lang.ref.WeakReference;

public class BitmapLoaderTask extends AsyncTask<String, Integer, Bitmap>
{
    private WeakReference<ImageView> imageViewReference;
    private String concurrencyPath;

    public BitmapLoaderTask(ImageView imageView)
    {
        imageViewReference = new WeakReference<ImageView>(imageView);
    }

    @Override
    protected Bitmap doInBackground(String... params)
    {
        concurrencyPath = params[0];
        MediaMetadataRetriever metaDataArt = new MediaMetadataRetriever();
        byte[] mediaArtbyteArray;
        try
        {
            metaDataArt.setDataSource(concurrencyPath);
            mediaArtbyteArray = metaDataArt.getEmbeddedPicture();
            if(mediaArtbyteArray != null)
            {
                Bitmap musicArt;
                musicArt = MainActivity.decodeBitmapFromByteArray(mediaArtbyteArray, 50, 50);
                Log.i("Check","Setting Cache with key "+params[1]);
                MainActivity.setBitmapToCache(params[1], musicArt);
                return musicArt;
            }
        }
        catch (IllegalArgumentException e)
        {
            Log.i("Check","Exception Thrown");
        }
        finally
        {
            metaDataArt.release();
        }

        return null;
    }

    // Once complete, see if ImageView is still around and set bitmap.

    @Override
    protected void onPostExecute(Bitmap bitmap)
    {
        System.out.println("Inside onPost");

            ImageView imageView = imageViewReference.get();
            if(bitmap != null && imageView != null)
            {
                /*BitmapLoaderTask bitmapLoaderTask = SongAdapter.getBitmapLoaderTask(imageView);
                if(this == bitmapLoaderTask && imageView != null)
                {
                    imageView.setImageBitmap(bitmap);
                }*/
                imageView.setImageBitmap(bitmap);
                System.out.println("Causing Error");
            }
            else if(imageView != null)
                imageView.setImageResource(R.drawable.default_cover_art);

    }

    String getConcurencyPath()
    {
        return concurrencyPath;
    }
}
