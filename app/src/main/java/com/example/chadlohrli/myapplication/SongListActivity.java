package com.example.chadlohrli.myapplication;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

public class SongListActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.songlist);

        Album album = SongParser.createAlbum("app/src/main/assets/music/albums/iwillnotbeafraid/");


    }
}
