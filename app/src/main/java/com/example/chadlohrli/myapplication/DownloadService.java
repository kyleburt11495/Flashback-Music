package com.example.chadlohrli.myapplication;

import android.app.DownloadManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.os.IBinder;
import android.util.Log;
import android.view.Gravity;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

public class DownloadService extends Service {
    private ArrayList<SongData> songList;
    private DownloadManager downloadManager;

    //TODO move broadcast reciever into either vibe mode or music player
    BroadcastReceiver onComplete = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            long referenceId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);

            DownloadManager.Query query = new DownloadManager.Query();
            query.setFilterById(referenceId);
            Cursor cursor = downloadManager.query(query);
            cursor.moveToFirst();
            //get description of download which contains position of downloaded song in song ArrayList passed in
            String downloadDescription = cursor.getString(cursor.getColumnIndex(DownloadManager.COLUMN_DESCRIPTION));
            //convert downloadDescription to int
            int songPosition = Integer.parseInt(downloadDescription);

            //TODO use songPosition to mark song as playable and remove progress bar in fragment

        }
    };

    public DownloadService() {
        downloadManager = (DownloadManager)getSystemService(DOWNLOAD_SERVICE);
        //TODO move this into music player/vibe mode
        registerReceiver(onComplete, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    public void setSongList(ArrayList<SongData> songList) {
        this.songList = songList;
    }

    public void downloadVibeSongsPlaylist(ArrayList<SongData> songList) {
        Iterator<SongData> it = songList.iterator();

        //position of song in arraylist
        int position = 0;
        while (it.hasNext()) {
            SongData song = it.next();
            String songId = song.getID();

            Map<String, ?> map = SharedPrefs.getSongData(getApplicationContext(), songId);
            //if map size is 0, then song has not yet been downloaded
            if(map.size() == 0)
                downloadSong(song, position);
            position++;

        }
    }

    public void downloadSong(SongData song, int position) {
        Uri uri = Uri.parse("REPLACE WITH REAL URL");
        //Uri uri = Uri.parse(song.getUrl);

        DownloadManager.Request request = new DownloadManager.Request(uri);

        String songId = song.getID();

        //description will be position of song in list so that broadcast reciever knows which song was downloaded
        request.setTitle(songId);
        request.setDescription(Integer.toString(position));

        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_MUSIC, songId+".mp3");


        request.allowScanningByMediaScanner();
        request.setMimeType("audio/MP3");

        downloadManager.enqueue(request);


        //TODO update shared prefs
    }


}
