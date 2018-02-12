package com.example.chadlohrli.myapplication;


import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;

public class gridAdapter extends ArrayAdapter<gridItem> {
    Context context;
    int layoutResourceId;
    ArrayList<gridItem> data = new ArrayList<gridItem>();

    public gridAdapter(Context context, int layoutResourceId,
                             ArrayList<gridItem> data) {
        super(context, layoutResourceId, data);
        this.layoutResourceId = layoutResourceId;
        this.context = context;
        this.data = data;
    }


    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View row = convertView;
        Album album;

        if (row == null) {
            LayoutInflater inflater = ((Activity) context).getLayoutInflater();
            row = inflater.inflate(layoutResourceId, parent, false);

            album = new Album();
            album.albumTitle = (TextView) row.findViewById(R.id.album_title_textview);
            album.albumArtist = (TextView) row.findViewById(R.id.artist_name_textview);
            album.albumArt = (ImageView) row.findViewById(R.id.album_art_imageview);
            row.setTag(album);
        } else {
            album = (Album) row.getTag();
        }

        gridItem item = data.get(position);
        ArrayList<SongData> songs = item.getSongs();
        Bitmap bp = SongParser.albumCover(songs.get(0), this.context);
        album.albumTitle.setText(item.getAlbumTitle());
        album.albumArtist.setText(item.getArtistName());
        album.albumArt.setImageBitmap(bp);
        return row;

    }

    static class Album {
        TextView albumTitle;
        TextView albumArtist;
        ImageView albumArt;
    }
}
