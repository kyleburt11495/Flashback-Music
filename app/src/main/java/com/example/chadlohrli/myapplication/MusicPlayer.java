package com.example.chadlohrli.myapplication;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.app.ActivityCompat;

import android.support.v4.app.FragmentManager;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

;
//int timesPlayed;

public class MusicPlayer extends AppCompatActivity {


    public static final String SONG_FINISHED = "SONG FINISHED";


    private DateHelper dateHelper;
    private boolean firstSongDownloaded = false;

    private int d = 0;
    private int psong = -1;
    //public int timesPlayed;
    private ImageView albumCover;
    private TextView locationTitle;
    private TextView songTitle;
    private TextView artistTitle;
    private ImageButton playBtn;
    private ImageButton nextBtn;
    private ImageButton prevBtn;
    private SeekBar seekBar;
    private Button favBtn;
    private TextView startTime;
    private TextView endTime;
    private TextView placeDate;


    private MediaPlayer mediaPlayer;
    private boolean isPlayingMusic = true;


    private final int MORNING = 0;
    private final int AFTERNOON = 1;
    private final int NIGHT = 2;

    private final int SUNDAY = 1;
    private final int MONDAY = 2;
    private final int TUESDAY = 3;
    private final int WEDNESDAY = 4;
    private final int THURSDAY = 5;
    private final int FRIDAY = 6;
    private final int SATURDAY = 7;

    private int timeofday = 0;
    private int day = 0;
    private double lat = 0;
    private double lng = 0;

    private ArrayList<SongData> songs;
    private int cur_song;
    //private FusedLocationProviderClient mFusedLocationClient;
    private Location lkl;

    private MusicService musicService;
    private DownloadService downloadService;
    private Intent playIntent;
    private Intent downloadIntent;
    private boolean isBound = false;
    private boolean downloadServiceIsBound = false;
    private Handler mHandler = new Handler();
    private LocalBroadcastManager bManager;
    private Location location;
    private String locationName;

    private FragmentManager fm;
    private SongProgressFragment fragment;
    private Bundle bundle;
    private String album_artist;
    private SongData songPlaying;
    private Button undislikeBtn;
    private ListView layout;

    //private enum state {NEUTRAL,DISLIKE,FAVORITE};
    private int songState;
    private int timesPlayed = 0;
    String timeStamp;
    private int fav;

    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;
    private FirebaseDatabase database;
    private DatabaseReference myRef;
    private DataSnapshot snapshot;

    private String user, lp, dpDate;
    MockTime mockTime = new MockTime();



    private BroadcastReceiver bReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(intent.getAction().equals(SONG_FINISHED)) {

                String serviceJsonString = intent.getStringExtra("hi");
                //Log.d("Broadcast", serviceJsonString);
                //Log.d("current index",String.valueOf(cur_song));

                playNextSong();

            }
        }
    };

    private BroadcastReceiver onDownloadComplete = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Toast toast = Toast.makeText(MusicPlayer.this,
                    "Music Download Complete", Toast.LENGTH_LONG);
            toast.setGravity(Gravity.TOP, 25, 400);
            toast.show();

            long referenceId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
            //directory that song has been stored in
            String path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC).getAbsolutePath();


            DownloadManager.Query query = new DownloadManager.Query();
            query.setFilterById(referenceId);
            DownloadManager downloadManager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
            Cursor cursor = downloadManager.query(query);

            d++;

            if(cursor.moveToFirst()); {
                //get description of download which contains position of downloaded song in song ArrayList passed in
                String downloadDescription = cursor.getString(cursor.getColumnIndex(DownloadManager.COLUMN_DESCRIPTION));
                //convert downloadDescription to int
                int songPosition = Integer.parseInt(downloadDescription);
                //Log.d("songPosition", Integer.toString(songPosition));

                //get title of column which is the id of the song
                String songId = cursor.getString(cursor.getColumnIndex(DownloadManager.COLUMN_TITLE));
                //Log.d("songId", songId);

                //TODO use songPosition to mark song as playable and remove progress bar in fragment
                SongData song = songs.get(songPosition);

                //parse song data into song
                song = SongParser.parseSong(path, songId, getApplicationContext());
                songs.set(songPosition, song);

                //Update the array list after downloading
                /*
                ListView toFill = (ListView) findViewById(R.id.listView);
                SongAdapter songAdapter = new SongAdapter(getApplicationContext(), songs);
                toFill.setAdapter(songAdapter);
                */

                updateFragment();
                SharedPrefs.updateDownloaded(getApplicationContext(), songId);

                if(firstSongDownloaded == false) {
                    firstSongDownloaded = true;

                }
                if(song.getPriority() < cur_song) {
                    psong = song.getPriority();
                }
                if(d == 1){
                    cur_song = song.getPriority();
                }
            }

        }
    };



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_music_player);

        //register download broadcast reciever
        registerReceiver(onDownloadComplete, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));


        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        setDateHelper(new DateHelper());
        //grab data from intent
        songs = (ArrayList<SongData>) getIntent().getSerializableExtra("SONGS");
        cur_song = getIntent().getIntExtra("CUR",0);
        //timeStamp = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss").format(new Date());

        timeStamp = mockTime.changeTime();
        Log.i("timeStamp is:", timeStamp);



        layout = findViewById(R.id.listView);
        layout.setVisibility(View.GONE);

        placeDate = (TextView) findViewById(R.id.place_date);

        //display song for aesthetics
        Toast toast = Toast.makeText(getApplicationContext(), songs.get(cur_song).getTitle(),
                Toast.LENGTH_SHORT);
        toast.show();

        initViews();
        initListeners();
        initBroadcast();
        //initLocation();

        initDB();
    }

    public void initDB(){
        database = FirebaseDatabase.getInstance();
        myRef = database.getReference();
        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();
    }

    public void songPicked(View view) {
        //if a disliked song is picked, it is no longer disliked
        SongData song = songs.get(Integer.parseInt(view.getTag().toString()));
        Map<String, ?> map = SharedPrefs.getSongData(this.getApplicationContext(), song.getID());
        if (map.get("State") != null) {
            if (((Integer) map.get("State")).intValue() == state.DISLIKE.ordinal()) {
                SharedPrefs.updateFavorite(getApplicationContext(), song.getID(), state.NEUTRAL.ordinal());
            }
        }
        setSong(Integer.parseInt(view.getTag().toString()));
        playSong();
    }

    public void dislikeAction(View view){
        Log.d("TAG",view.getTag().toString());
        SongData song = songs.get(Integer.parseInt(view.getTag().toString()));
        SharedPrefs.updateFavorite(getApplicationContext(),song.getID(),state.NEUTRAL.ordinal());
        undislikeBtn = view.findViewById(R.id.undislikeBtn);
        undislikeBtn.setVisibility(View.INVISIBLE);
        Toast toast = Toast.makeText(getApplicationContext(), "Un-Disliked!", Toast.LENGTH_SHORT);
        toast.show();
    }

    public void updateFragment() {
        songPlaying = songs.get(cur_song);
        album_artist = songPlaying.getArtist() + " \u0f36 " + songPlaying.getAlbum();

        fm = getSupportFragmentManager();
        bundle = new Bundle();
        bundle.putString("SONG", songPlaying.getTitle());
        bundle.putString("ARTIST_ALBUM", album_artist);
        bundle.putParcelableArrayList("SONGS", songs);
        fragment = new SongProgressFragment();
        fragment.setArguments(bundle);
        fm.beginTransaction().replace(R.id.mainLay, fragment).commit();
    }

    public void updatePlaceDate() {

        myRef.child("songs").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    //get all id and find matching one
                    Log.d("songid", snapshot.getKey());
                    if(snapshot.getKey().equals(songs.get(cur_song).getID())) {

                        lp = snapshot.child("lastPlayed").getValue(String.class);
                        user = snapshot.child("lastPlayedUser").getValue(String.class);

                        Log.d("time is ", lp);

                        break;
                    }

                }

                if (user != null) {
                    myRef.child("users").addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {

                            for (DataSnapshot names : dataSnapshot.getChildren()) {
                                //get all id and find matching one
                                if (names.getKey().equals(user)) {
                                    user = names.child("username").getValue().toString();
                                    Log.d("user name", user);
                                    dpDate = lp + " by " + user;
                                    placeDate.setText(dpDate);
                                    break;
                                }
                            }
                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {
                        }
                    });
                }
                else {
                    user = "No one";
                }
                if (lp == null) {
                    lp = "Last Played no where";
                }
                dpDate = lp + " by " + user;
                placeDate.setText(dpDate);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });




    }

    public void setDateHelper(DateHelper dateHelper) {
        this.dateHelper = dateHelper;
    }


    // -- inner class variables -- //
    private ServiceConnection musicConnection = new ServiceConnection(){

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MusicService.MusicBinder binder = (MusicService.MusicBinder)service;
            musicService = binder.getService();
            musicService.setSongList(songs);
            mediaPlayer = musicService.getPlayer();
            isBound = true;

            for(SongData es: songs) {
                SharedPreferences pref = getSharedPreferences(es.getID(), MODE_PRIVATE);
                boolean downloaded = pref.getBoolean("downloaded", false);
                if (downloaded == true) {
                    d++;
                }
            }
            if(d == 0){
                Toast toast = Toast.makeText(getApplicationContext(), "Downloads in progress!",
                        Toast.LENGTH_LONG);
                toast.show();
                Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        playSong();
                    }
                }, 10000);
            } else {
                playSong();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            isBound = false;
        }
    };

    private ServiceConnection downloadConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            downloadServiceIsBound = true;
            DownloadService.DownloadBinder downloadBinder = (DownloadService.DownloadBinder)iBinder;
            downloadService = downloadBinder.getService();
            downloadService.setSongList(songs);

            downloadService.downloadVibeSongsPlaylist();

        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            downloadServiceIsBound = false;
        }
    };





    // -- class specific methods -- //
    @Override
    protected void onStart() {
        super.onStart();
        if (playIntent == null) {
            playIntent = new Intent(this, MusicService.class);
            bindService(playIntent, musicConnection, Context.BIND_AUTO_CREATE);
            startService(playIntent);
            LocationHelper.getLatLong(getApplicationContext());

        }
        if (downloadIntent == null) {
            downloadIntent = new Intent(this, DownloadService.class);
            bindService(downloadIntent, downloadConnection, Context.BIND_AUTO_CREATE);
            startService(downloadIntent);
        }
    }

    @Override
    protected void onDestroy() {
        bManager.unregisterReceiver(bReceiver);
        stopService(playIntent);
        musicService = null;

        unregisterReceiver(onDownloadComplete);
        stopService(downloadIntent);
        downloadService = null;
        super.onDestroy();
        unbindService(musicConnection);
        unbindService(downloadConnection);



    }


    // -- functional methods -- //

    public void setStateButton(){

        if(songState == state.NEUTRAL.ordinal()){
            favBtn.setText("+");
            favBtn.setBackgroundColor(Color.WHITE);
            fav = 0;
        }else if(songState == state.DISLIKE.ordinal()){
            favBtn.setText("x");
            favBtn.setBackgroundColor(Color.RED);
            fav = -1;
        }else if(songState == state.FAVORITE.ordinal()){
            favBtn.setText("\u2714");
            favBtn.setBackgroundColor(Color.GREEN);
            fav = 1;
        }
    }

    public void checkSongState(SongData song){

        Map<String,?> map = SharedPrefs.getSongData(this.getApplicationContext(),song.getID());

        if(map.get("State") != null){
            songState = ((Integer) map.get("State")).intValue();
        }else{
            songState = state.NEUTRAL.ordinal();
        }

        setStateButton();

    }

    public void setSong(int songIndex){
        cur_song = songIndex;
    }

    public void playSong() {

        //TODO if current song has not been downloaded skip and play next song
        //Log.d("cur_song", songs.get(cur_song).getAlbum());

        /*if(d == 0){
            return;
        }*/

        SharedPreferences pref = getSharedPreferences(songs.get(cur_song).getID(), MODE_PRIVATE);
        boolean downloaded = pref.getBoolean("downloaded", false);
        if (downloaded == false) {
            playNextSong();
            //TODO return??
        }
        /**

        if(songs.get(cur_song).checkIfDownloaded().equals("False")) {
            playNextSong();
            return;
        }
        */


        checkSongState(songs.get(cur_song));

        //This code ensures that no disliked songs will play
        if(songState == state.DISLIKE.ordinal()){
            if(songs.size() == 1){
                onSupportNavigateUp();
            }else{
                songs.remove(cur_song);
                playNextSong();
            }
        }

        updateFragment();
        Log.d("Songs size",String.valueOf(songs.size()));
        updatePlaceDate();

        musicService.setCurrentSong(cur_song);
        musicService.playSong();
        setupPlayer(songs.get(cur_song));
        pref = getSharedPreferences("last song", MODE_PRIVATE);
        SharedPreferences.Editor edit = pref.edit();
        edit.putString("song", songs.get(cur_song).getTitle());
        edit.putFloat("Latitude", (float)lat);
        edit.putFloat("Longitude", (float)lng);
        edit.putString("Last played", timeStamp);

        edit.apply();

    }

    public void playNextSong(){

        if(songs.size() > 0){
            if (++cur_song > songs.size()-1)
                cur_song = 0;
        }else{
            onSupportNavigateUp();
        }


        Log.d("new index",String.valueOf(cur_song));

        if(psong != -1){
            cur_song = psong;
        }
        psong = -1;

        SharedPreferences pref = getSharedPreferences(songs.get(cur_song).getID(), MODE_PRIVATE);
        boolean downloaded = pref.getBoolean("downloaded", false);
        if (downloaded == false){
            Log.d("PNSWORK", "MEH");
            playNextSong();
        }
        playSong();


    }

    public void playPrevSong() {

        if(--cur_song < 0)
            cur_song = songs.size()-1;

        Log.d("new index",String.valueOf(cur_song));

        playSong();


    }

    public void trackSong() {

        MusicPlayer.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (musicService != null) {
                    int curPos = mediaPlayer.getCurrentPosition() / 1000;
                    seekBar.setProgress(curPos);
                    startTime.setText(String.format("%02d:%02d", (curPos % 36000) / 60, (curPos % 60)));
                    mHandler.postDelayed(this, 1000);
                }
            }
        });
    }

    public void saveSong(SongData song) {
        Map<String,?> map = SharedPrefs.getSongData(this.getApplicationContext(),song.getID());

        //don't save these songs!
        if(map.get("isAlbum") != null) {
            String isAlbum = map.get("isAlbum").toString();
            if(isAlbum == "true")
                return;
        }


        if(map.get("Times played") != null){
            timesPlayed = Integer.valueOf(map.get("Times played").toString()) + 1;
        } else {
            timesPlayed++;
        }

        String url = "";
        if(map.get("URL") != null){
            url = map.get("URL").toString();
        }

        SharedPrefs.saveSongData(getApplicationContext(), song.getID(), (float)lat, (float)lng, day, timeofday, 0, songState, timesPlayed, timeStamp, fav);

        //Save song object to firebase
        DatabaseReference userRef;
        DatabaseReference songRef = myRef.child("songs").child(song.getID());
        songRef.child("lastPlayed").setValue(timeStamp);
        songRef.child("url").setValue(url);

        //location
        String locUID = String.valueOf(String.valueOf(lat + lng).hashCode());
        songRef.child("location").child(locUID).child("lat").setValue((float)lat);
        songRef.child("location").child(locUID).child("long").setValue((float)lng);

        /*
        if(location != null){
            songRef.child("location").child(locUID).child(locationName).setValue(true);
        }
        */

        //save song to user
        if(currentUser!= null) {
            songRef.child("user").child(currentUser.getUid()).setValue(true);
            songRef.child("lastPlayedUser").setValue(currentUser.getUid());

            userRef = myRef.child("users").child(currentUser.getUid());
            userRef.child("songs").child(song.getID()).setValue(true);

        }else {
            songRef.child("user").child(UUID.randomUUID().toString()).setValue(true);
        }


    }


    public void setupPlayer(SongData song){

        //set up song UI
        Bitmap bp = SongParser.albumCover(song,getApplicationContext());
        if(bp != null) {
            albumCover.setImageBitmap(bp);
        }
        songTitle.setText(song.getTitle());
        artistTitle.setText(song.getArtist());

        //set up seeking UI
        final int dur = mediaPlayer.getDuration() / 1000;
        seekBar.setMax(dur);
        Log.d("DUR", String.valueOf(mediaPlayer.getDuration()));
        endTime.setText(String.format("%02d:%02d", (dur % 36000) / 60, (dur % 60)));

        trackSong(); //thread to update seek bar
        initLocation(); //refresh lat/long and display location
        initTimeDay(); //get formatted time and date

        saveSong(song); //save data to shared preference
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    public void initViews() {

        playBtn = findViewById(R.id.playBtn);
        nextBtn = findViewById(R.id.nextBtn);
        favBtn = findViewById(R.id.favBtn);
        prevBtn = findViewById(R.id.prevBtn);
        favBtn =  findViewById(R.id.favBtn);
        seekBar =  findViewById(R.id.seekBar);
        startTime =  findViewById(R.id.startTime);
        endTime =  findViewById(R.id.endTime);
        albumCover =  findViewById(R.id.albumCover);
        songTitle =  findViewById(R.id.songTitle);
        artistTitle =  findViewById(R.id.artistTitle);
        locationTitle = findViewById(R.id.locationTitle);
    }

    @SuppressWarnings("ClickableViewAccessibility")
    @SuppressLint("ClickableViewAccessibility")
    public void initListeners() {

        nextBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                    isPlayingMusic = true;
                    playBtn.setImageResource(android.R.drawable.ic_media_pause);
                    playNextSong();
            }
        });

        prevBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                    isPlayingMusic = true;
                    playBtn.setImageResource(android.R.drawable.ic_media_pause);
                    playPrevSong();

            }
        });

        playBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (isPlayingMusic) {
                    musicService.pauseSong();
                    isPlayingMusic = false;
                    playBtn.setImageResource(android.R.drawable.ic_media_play);
                }
                else {
                    musicService.resumeSong();
                    isPlayingMusic = true;
                    playBtn.setImageResource(android.R.drawable.ic_media_pause);
                }

            }
        });




        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

                if(fromUser) {
                    seekBar.setMax(mediaPlayer.getDuration() / 1000);
                    mediaPlayer.seekTo(progress * 1000);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });


        favBtn.setOnTouchListener(new View.OnTouchListener() {
            private GestureDetector gestureDetector = new GestureDetector(MusicPlayer.this, new GestureDetector.SimpleOnGestureListener() {
                @Override
                public boolean onDoubleTap(MotionEvent e) {
                    Log.d("TEST", "onDoubleTap");
                    if(songState == state.NEUTRAL.ordinal()) {
                        songState = state.DISLIKE.ordinal();
                        Toast toast = Toast.makeText(getApplicationContext(), "Disliked!", Toast.LENGTH_SHORT);
                        toast.show();
                    }
                    else if(songState == state.DISLIKE.ordinal())
                        songState = state.NEUTRAL.ordinal();

                    Log.d("STATE", String.valueOf(songState));

                    setStateButton();

                    SharedPrefs.updateFavorite(MusicPlayer.this.getApplicationContext(),songs.get(cur_song).getID(),songState);

                    /*
                    //This allows UI to update before switching songs
                    new Timer().schedule(new TimerTask() {
                        @Override
                        public void run() {
                            playNextSong();

                        }
                    }, 1000);

                    */

                    playNextSong();

                    return super.onDoubleTap(e);



                }
                @Override
                public boolean onSingleTapConfirmed(MotionEvent event) {
                    Log.d("TEST", "onSingleTap");
                    if(songState == state.NEUTRAL.ordinal()) {
                        songState = state.FAVORITE.ordinal();
                        Toast toast = Toast.makeText(getApplicationContext(), "Favorited!", Toast.LENGTH_SHORT);
                        toast.show();
                    }
                    else if(songState == state.FAVORITE.ordinal()) {
                        songState = state.NEUTRAL.ordinal();
                        Toast toast = Toast.makeText(getApplicationContext(), "Un-Favorited!", Toast.LENGTH_SHORT);
                        toast.show();
                    }

                    setStateButton();


                    Log.d("STATE", String.valueOf(songState));

                    SharedPrefs.updateFavorite(MusicPlayer.this.getApplicationContext(),songs.get(cur_song).getID(),songState);

                    return false;
                }

            });

            @Override
            public boolean onTouch(View v, MotionEvent event) {

                gestureDetector.onTouchEvent(event);
                return true;

            }
        });

    }

    public void initLocation() {

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        LocationManager lm = (LocationManager) getApplicationContext().getSystemService(Context.LOCATION_SERVICE);
        if (lm != null) {
            location = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if(location != null){
                lat = location.getLatitude();
                lng = location.getLongitude();

                Geocoder geocoder = new Geocoder(getApplicationContext(), Locale.getDefault());
                try {
                    List<Address> listAddresses = geocoder.getFromLocation(lat, lng, 1);
                    if(null!=listAddresses&&listAddresses.size()>0){
                        String loc_name = String.valueOf(listAddresses.get(0).getAddressLine(0));
                        locationName = loc_name;
                        locationTitle.setText(locationName);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }else{
                locationTitle.setText("Location not found");
            }
        }


    }


    public void initTimeDay() {
        timeofday = getTimeOfDay();
        day = getDay();
        //Log.i("time of day", String.valueOf(timeofday));
        //Log.i("day", String.valueOf(day));

    }

    public void initBroadcast() {
        bManager = LocalBroadcastManager.getInstance(this);
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(SONG_FINISHED);
        bManager.registerReceiver(bReceiver, intentFilter);
    }



    public int getTimeOfDay() {
        //int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        int hour = dateHelper.getCalendar().get(Calendar.HOUR_OF_DAY);
        if(hour > 17 || hour < 5)
            return NIGHT;
        else if (hour >= 5 && hour < 11 )
            return MORNING;
        else
            return AFTERNOON;
    }

    public int getDay() {
        //int day = Calendar.getInstance().get(Calendar.DAY_OF_WEEK);
        int day = dateHelper.getCalendar().get(Calendar.DAY_OF_WEEK);
        return day;
    }

}
