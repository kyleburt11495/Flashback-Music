package com.example.chadlohrli.myapplication;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.Environment;
import android.provider.ContactsContract;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class VibeActivity extends AppCompatActivity {
    private Location location;
    private ArrayList<String> vibeList = new ArrayList<String>();
    private ArrayList<String> vibeListURLs = new ArrayList<String>();

    private ArrayList<SongData> vibeSongs = new ArrayList<SongData>();
    private Set<SongData> setSong;
    private ArrayList<SongData> vibeFinalPlaylist;

    /*
    private Set<String> set;
    private ArrayList<String> finalRec;

    private Set<String> seturl;
    private ArrayList<String> finalRecURL;*/

    FirebaseDatabase database = FirebaseDatabase.getInstance();
    DatabaseReference myRef = database.getReference();
    private FirebaseAuth mAuth;
    private ImageButton playFB;

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    public int matchWeek(String songTimestamp) {
        Date curtime = Calendar.getInstance().getTime();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss");
        Date songTime = null;

        try {
            songTime = sdf.parse(songTimestamp);
        } catch (ParseException e){
            e.printStackTrace();
        }

        if((curtime.getTime() - songTime.getTime()) < 604800000) {
            return 2;
        }
        return 0;
    }

    public double matchLocation(Location songLoc) {
        double distance = songLoc.distanceTo(location);
        double locRating = 0;
        if (distance <= 304.8) {
            locRating += 2;
        }
        return locRating;
    }

    public Location getLoc(){
        LocationManager lm = (LocationManager) getApplicationContext().getSystemService(Context.LOCATION_SERVICE);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[] {Manifest.permission.ACCESS_FINE_LOCATION}, 100);
        }

        Location loc = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);

        if (location != null) {
            return loc;
        } else {
            Toast.makeText(getApplicationContext(), "Cannot Get Location", Toast.LENGTH_LONG).show();
            onSupportNavigateUp();
            return null;
        }
    }
    @Override
    protected void onStart() {
        super.onStart();
        setContentView(R.layout.flashback);
        LocationHelper.getLatLong(getApplicationContext());
        vibe();
    }

    protected void vibe(){
        location = getLoc();
        mAuth = FirebaseAuth.getInstance();
        vibeSongs = createDownloadedSongs();
        myRef.child("songs").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    String lp = snapshot.child("lastPlayed").getValue(String.class);
                    //int newR = snapshot.child("rating").getValue(int.class) + matchWeek(lp);
                    //snapshot.child("rating").getRef().setValue(newR);
                    int wR = matchWeek(lp);
                    SharedPrefs.updateRating(VibeActivity.this.getApplicationContext(), snapshot.getKey(), (float)wR);
                    SharedPrefs.updateLastPlayedWeek(VibeActivity.this.getApplicationContext(), snapshot.getKey(), 2);

                    for(DataSnapshot locs: snapshot.child("location").getChildren()){
                        double lat = locs.child("lat").getValue(double.class);
                        double lngt = locs.child("lngt").getValue(double.class);
                        Location playLoc = new Location("any");
                        playLoc.setLatitude(lat);
                        playLoc.setLongitude(lngt);
                        if(matchLocation(playLoc) == 2){
                            //double rat = snapshot.child("rating").getValue(int.class) + matchLocation(playLoc);
                            //snapshot.child("rating").getRef().setValue(rat);
                            SharedPreferences pref = getSharedPreferences(snapshot.getKey(), MODE_PRIVATE);
                            int curRate = pref.getInt("Rating", 0);
                            double locR = 2;
                            SharedPrefs.updateRating(VibeActivity.this.getApplicationContext(),
                                    snapshot.getKey(), (float)curRate + (float)locR);
                            SharedPrefs.updateLocPlay(VibeActivity.this.getApplicationContext(), snapshot.getKey(), 2);
                            break;
                        }
                    }
                    SharedPreferences pref = getSharedPreferences(snapshot.getKey(), MODE_PRIVATE);
                    int curRate = pref.getInt("Rating", 0);

                    if(curRate > 0) {
                        SongData song = new SongData(snapshot.getKey(), null, null, null,
                                null, null, snapshot.child("url").getValue(String.class));
                        boolean state = pref.getBoolean("downloaded", false);
                        if (state == true) {
                            song = createDownloadedSongData(song);
                        }
                        else {
                            song.setIfDownloaded("False");
                        }
                        vibeSongs.add(song);

                        /**
                        if(!state) {
                            vibeSongs.add(song);
                        }
                         */
                        //vibeList.add(snapshot.getKey());
                        //vibeListURLs.add(snapshot.child("url").getValue(String.class));
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });

        //get CURRENT USER ID HERE
        FirebaseUser currentUser = mAuth.getCurrentUser();
        final String curId = currentUser.getUid();
        myRef.child("users").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot snapshot : dataSnapshot.child(curId).child("friends").getChildren()) {
                    String friendid = snapshot.getKey();
                    for(DataSnapshot fSongs: dataSnapshot.child(friendid).child("songs").getChildren()){
                        // GET ORIGINAL RATING IN NEWR
                        // int newR = myRef.child("songs").child(snapshot.getKey()).child("rating")
                        // CHANGE RATINGS BY ADDING 2
                        SharedPreferences pref = getSharedPreferences(fSongs.getKey(), MODE_PRIVATE);
                        int curRate = pref.getInt("Rating", 0);
                        SharedPrefs.updateRating(VibeActivity.this.getApplicationContext(),
                                snapshot.getKey(), (float)curRate + 2);
                        SharedPrefs.updateFriendPlayed(VibeActivity.this.getApplicationContext(), snapshot.getKey(), 2);
                        SongData song = new SongData(snapshot.getKey(), null, null, null,
                                null, null, snapshot.child("url").getValue(String.class));
                        boolean state = pref.getBoolean("downloaded", false);
                        if (state == true) {
                            song = createDownloadedSongData(song);
                        }
                        else {
                            song.setIfDownloaded("False");
                        }
                        vibeSongs.add(song);
                        /**
                        if(!state) {
                            vibeSongs.add(song);
                        }
                         */
                        //vibeList.add(snapshot.getKey());
                        //vibeListURLs.add(snapshot.child("url").getValue(String.class));
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });

        // Get unique song ids only
        /*set = new HashSet<String>(vibeList);
        finalRec = new ArrayList<String>(set);

        seturl = new HashSet<String>(vibeListURLs);
        finalRecURL = new ArrayList<String>(seturl);*/

        setSong = new HashSet<SongData>(vibeSongs);
        vibeFinalPlaylist = new ArrayList<SongData>(setSong);

        Collections.sort(vibeFinalPlaylist, new VibeSongSorter(getApplicationContext()));

        for(int i = 1; i <= vibeFinalPlaylist.size(); i++){
            vibeFinalPlaylist.get(i - 1).setPriority(i);
        }

        //PASS finalRecURL to Download Service and start downloads
        playFB = (ImageButton) findViewById(R.id.playfb);
        playFB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (vibeFinalPlaylist.size() == 0) {
                    Toast toast = Toast.makeText(getApplicationContext(), "Play Songs First Before Using Flashback!", Toast.LENGTH_LONG);
                    toast.show();
                    onSupportNavigateUp();
                }

                Intent intent = new Intent(VibeActivity.this, MusicPlayer.class);
                intent.putExtra("SONGS", vibeFinalPlaylist);
                intent.putExtra("CUR", 0);
                intent.putExtra("caller", "VibeActivity");
                VibeActivity.this.startActivity(intent);
                finish();
            }
        });

    }
    public ArrayList<SongData> createDownloadedSongs() {
        File musicDirectory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC);
        File[] fields = musicDirectory.listFiles();
        ArrayList<SongData> songs = new ArrayList<SongData>();

        for (int count = 0; count < fields.length; count++) {
            String path  = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC).getAbsolutePath();
            String Id = fields[count].getName();

            SongData song = SongParser.parseSong(path, Id, getApplicationContext());
            songs.add(song);
        }

        //sort songs
        Collections.sort(songs, new Comparator<SongData>() {
            @Override
            public int compare(SongData a, SongData b) {
                return a.getTitle().compareTo(b.getTitle());
            }
        });
        return songs;
    }

    public SongData createDownloadedSongData(SongData songData) {
        String songId = songData.getID();
        File musicDirectory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC);
        File[] fields = musicDirectory.listFiles();
        String path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC).getAbsolutePath();
        songData =  SongParser.parseSong(path, songId, getApplicationContext());
        songData.setIfDownloaded("True");
        return songData;
    }
}
