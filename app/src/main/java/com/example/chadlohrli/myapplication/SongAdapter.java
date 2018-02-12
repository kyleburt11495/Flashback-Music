package com.example.chadlohrli.myapplication;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.chadlohrli.myapplication.R;
import com.example.chadlohrli.myapplication.SongData;

import java.util.ArrayList;

/**
 * Created by chadlohrli on 2/11/18.
 */

public class SongAdapter extends BaseAdapter {

    private ArrayList<SongData> songs;
    private LayoutInflater songInf;

    public SongAdapter(Context c, ArrayList<SongData> songs){
        this.songs = songs;
        songInf = LayoutInflater.from(c);
    }

    @Override
    public int getCount() {
        return songs.size();
    }

    @Override
    public Object getItem(int i) {
        return null;
    }

    @Override
    public long getItemId(int i) {
        return 0;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        //map to song layout
        LinearLayout songLay = (LinearLayout)songInf.inflate
                (R.layout.song, viewGroup, false);
        //get title and artist views
        TextView songView = (TextView)songLay.findViewById(R.id.song_title);
        TextView artistView = (TextView)songLay.findViewById(R.id.song_artist);
        //get song using position
        SongData currSong = songs.get(i);
        //get title and artist strings
        songView.setText(currSong.getTitle());
        artistView.setText(currSong.getArtist());
        //set position as tag
        songLay.setTag(i);
        return songLay;
    }
}
