package com.example.chadlohrli.myapplication;

import android.app.DownloadManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Binder;
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
    private final IBinder downloadBinder = new DownloadBinder();

    //TODO move broadcast reciever into either vibe mode or music player
    /**
    BroadcastReceiver onDownloadComplete = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            long referenceId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
            //directory that song has been stored in
            String path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC).getAbsolutePath();


            DownloadManager.Query query = new DownloadManager.Query();
            query.setFilterById(referenceId);
            Cursor cursor = downloadManager.query(query);

            if(cursor.moveToFirst()); {
                //get description of download which contains position of downloaded song in song ArrayList passed in
                String downloadDescription = cursor.getString(cursor.getColumnIndex(DownloadManager.COLUMN_DESCRIPTION));
                //convert downloadDescription to int
                int songPosition = Integer.parseInt(downloadDescription);
                Log.d("songPosition", Integer.toString(songPosition));

                //get title of column which is the id of the song
                String songId = cursor.getString(cursor.getColumnIndex(DownloadManager.COLUMN_TITLE));
                Log.d("songId", songId);

                //TODO use songPosition to mark song as playable and remove progress bar in fragment
                SongData song = songList.get(songPosition);

                //parse song data into song
                song = SongParser.parseSong(path, songId, getApplicationContext());


            }
        }
    };
    */
    public DownloadService() {
        //TODO move this into music player/vibe mode
        //registerReceiver(onDownloadComplete, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
    }

    @Override
    public void onCreate() {
        super.onCreate();
        downloadManager = (DownloadManager)getSystemService(DOWNLOAD_SERVICE);

    }



    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        return downloadBinder;
    }

    public void setSongList(ArrayList<SongData> songList) {
        this.songList = songList;
    }

    public void downloadVibeSongsPlaylist() {
        Iterator<SongData> it = songList.iterator();

        //position of song in arraylist, passed into downloadSong and then the description of download
        //to be able to tell which song was just downloaded
        int position = 0;
        while (it.hasNext()) {
            SongData song = it.next();
            String songId = song.getID();
            SharedPreferences pref = getSharedPreferences(songId, MODE_PRIVATE);
            boolean isDownloaded = pref.getBoolean("downloaded", false);

            //Map<String, ?> map = SharedPrefs.getSongData(getApplicationContext(), songId);
            //if map size is 0, then song has not yet been downloade
            if(isDownloaded == false)
                downloadSong(song, position);
            position++;

        }
    }

    public void downloadSong(SongData song, int position) {
        //Log.d("Logging downloadSong id", song.getID());
        //Log.d("Logging downloadSong path", song.getPath());
        //Log.d("Logging downloadSong title", song.getTitle());
        Uri uri = Uri.parse(song.getUrl());
        //Uri uri = Uri.parse(song.getUrl);

        DownloadManager.Request request = new DownloadManager.Request(uri);

        String songId = song.getID();

        //description will be position of song in list so that broa
        // dcast reciever knows which song was downloaded
        request.setTitle(songId);
        request.setDescription(Integer.toString(position));

        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_MUSIC, songId);


        request.allowScanningByMediaScanner();
        request.setMimeType("audio/MP3");

        downloadManager.enqueue(request);


        //TODO update shared prefs
        SharedPrefs.updateURL(this,songId,uri.toString());

    }

    public DownloadManager getDownloadManager() {return downloadManager;}

    public class DownloadBinder extends Binder {
        public DownloadService getService() {
            return DownloadService.this;
        }
    }




}
