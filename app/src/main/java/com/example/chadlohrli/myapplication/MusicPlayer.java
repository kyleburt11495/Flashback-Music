package com.example.chadlohrli.myapplication;

import android.Manifest;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.location.Location;
import android.location.LocationManager;
import android.media.Image;
import android.media.MediaPlayer;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Map;

public class MusicPlayer extends AppCompatActivity {

    private Button backBtn;
    private ImageView albumCover;
    private TextView locationTitle;
    private TextView songTitle;
    private TextView artistTitle;
    private ImageButton playBtn;
    private ImageButton nextBtn;
    private ImageButton prevBtn;
    private ImageButton seekBar;
    private Button favBtn;

    private MediaPlayer mediaPlayer;
    private boolean isPlayingMusic = true;

    private final int MORNING = 0;
    private final int AFTERNOON = 1;
    private final int NIGHT = 2;

    private final int MONDAY = 0;
    private final int TUESDAY = 1;
    private final int WEDNESDAY = 2;
    private final int THURSDAY = 3;
    private final int FRIDAY = 4;
    private final int SATURDAY = 5;
    private final int SUNDAY = 6;

    private ArrayList<SongData> songs;
    private int cur_song;
    private FusedLocationProviderClient mFusedLocationClient;
    private Location lkl;

    public void setSong(int songIndex){
        cur_song = songIndex;
    }

    public void setupPlayer(SongData song){

        albumCover = (ImageView) findViewById(R.id.albumCover);
        songTitle = (TextView) findViewById(R.id.songTitle);
        artistTitle = (TextView) findViewById(R.id.artistTitle);

        Bitmap bp = SongParser.albumCover(song,getApplicationContext());
        if(bp != null) {
            albumCover.setImageBitmap(bp);
        }
        songTitle.setText(song.getTitle());
        artistTitle.setText(song.getArtist());

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_music_player);

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        backBtn = (Button) findViewById(R.id.backBtn);
        playBtn = (ImageButton) findViewById(R.id.playBtn);
        nextBtn = (ImageButton) findViewById(R.id.nextBtn);
        favBtn = (Button) findViewById(R.id.favBtn);
        prevBtn = (ImageButton) findViewById(R.id.prevBtn);

        //grab data from intent
        songs = (ArrayList<SongData>) getIntent().getSerializableExtra("SONGS");
        cur_song = getIntent().getIntExtra("CUR",0);

        //display song for now to ensure data has correctly been passed
        Toast toast = Toast.makeText(getApplicationContext(), songs.get(cur_song).getTitle(), Toast.LENGTH_SHORT);
        toast.show();

        setupPlayer(songs.get(cur_song));

        backBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MusicPlayer.this, SongListActivity.class);
                MusicPlayer.this.startActivity(intent);
                finish();
            }
        });


        playBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (isPlayingMusic = true) {
                    mediaPlayer.stop();
                    isPlayingMusic = false;
                    playBtn.setImageResource(android.R.drawable.ic_media_pause);
                }
                else {
                    mediaPlayer.start();
                    isPlayingMusic = true;
                    playBtn.setImageResource(android.R.drawable.ic_media_play);
                }

            }
        });



        //double check later to see if this is correct if
        loadMedia(R.raw.gottagetoveryou);
        //automatically start playing music





        //save data in shared preferences
        Location currentLocation = getCurrentLocation();
        int timeOfDay = getTimeOfDay();
        int day = getDay();

        saveSongData(currentLocation, timeOfDay, day);



    }


    /* TODO:
    1) get location, time of day, and day of week
    2) save song data in shared preferences
    3) add all music player functionality (play,stop,seek,next)
    4) tap to favorite/dislike (changes button state and sharedPreferences
    5) handle next song and play in background (optional) ?




     */

}
