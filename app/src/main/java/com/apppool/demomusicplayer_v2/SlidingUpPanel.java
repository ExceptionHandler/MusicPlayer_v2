package com.apppool.demomusicplayer_v2;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SeekBar;
import com.sothree.slidinguppanel.SlidingUpPanelLayout;

public class SlidingUpPanel extends AppCompatActivity
{
    Button play, pause;
    SeekBar seekBar;
    ImageView imageView;
    SlidingUpPanelLayout slidingUpPanelLayout;


    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.slidinguppanel);

        imageView = (ImageView) findViewById(R.id.albumart);
        seekBar = (SeekBar) findViewById(R.id.seekbar);
        slidingUpPanelLayout = (SlidingUpPanelLayout) findViewById(R.id.sliding_layout);


    }
}
