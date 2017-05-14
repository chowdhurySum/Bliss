package com.example.sumitra.myapplication;

import android.content.DialogInterface;
import android.provider.MediaStore;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import android.net.Uri;
import android.content.ContentResolver;
import android.database.Cursor;
import android.support.v7.widget.Toolbar;
import android.view.KeyEvent;
import android.view.Menu;
import android.widget.ListView;
import android.os.IBinder;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.view.MenuItem;
import android.view.View;
import android.util.Log;
import com.example.sumitra.myapplication.MusicService.MusicBinder;
import android.widget.MediaController.MediaPlayerControl;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity implements MediaPlayerControl {

    private ArrayList<Song> songList;
    private ListView songView;
    private MusicService musicSrv;
    private Intent playIntent;
    private boolean musicBound = false;
    private MusicController controller;
    //to check pause
    private boolean paused=false;
    private boolean playbackPaused=false;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //Log.d("Bliss_test", "onCreate invoked");
        setContentView(R.layout.activity_main);
        Toolbar myToolbar = (Toolbar) findViewById(R.id.my_toolbar);
        setSupportActionBar(myToolbar);
        songView = (ListView)findViewById(R.id.song_list);

        songList = new ArrayList<Song>();
        getSongList();
        Collections.sort(songList, new Comparator<Song>() {
            @Override
            public int compare(Song a, Song b) {
                return a.getTitle().compareTo(b.getTitle());
            }
        });
        SongAdapter songAdt = new SongAdapter(this, songList);
        songView.setAdapter(songAdt);
        setController();

    }

    //connect to the service
    private ServiceConnection musicConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MusicBinder binder = (MusicBinder) service;
            //get service
            musicSrv = binder.getService();
            //pass list
            musicSrv.setList(songList);
            musicBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            musicBound = false;
        }
    };

    public void getSongList() {
        //retrieve song info
        //Log.d("Bliss_test", "getSongList invoked ");
        ContentResolver musicResolver = getContentResolver();
        Uri musicUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        Cursor musicCursor = musicResolver.query(musicUri, null, null, null, null);
        if (musicCursor != null && musicCursor.moveToFirst()) {
            //get coloumns
            int titleColoumn = musicCursor.getColumnIndex(MediaStore.Audio.Media.TITLE);
            int idColoumn = musicCursor.getColumnIndex(MediaStore.Audio.Media._ID);
            int artistColoumn = musicCursor.getColumnIndex(MediaStore.Audio.Media.ARTIST);
            //add songs to list
            do {
                long thisID = musicCursor.getLong(idColoumn);
                String thisTitle = musicCursor.getString(titleColoumn);
                String thisArtist = musicCursor.getString(artistColoumn);
                songList.add(new Song(thisID, thisTitle, thisArtist));
            }
            while (musicCursor.moveToNext());
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        if (playIntent == null) {
            playIntent = new Intent(this, MusicService.class);
            bindService(playIntent, musicConnection, Context.BIND_AUTO_CREATE);
            startService(playIntent);
        }
    }

    public void songPicked(View view)
    {
        musicSrv.setSong(Integer.parseInt(view.getTag().toString()) );
        musicSrv.playSong();
        if(playbackPaused)
        {
            setController();
            playbackPaused=false;
        }
        controller.show(0);
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        //menu item selected
        switch (item.getItemId()) {
            case R.id.action_shuffle:
                musicSrv.setShuffle();
                break;
            case R.id.action_end:
                stopService(playIntent);
                musicSrv=null;
                System.exit(0);
                break;
        }
        return super.onOptionsItemSelected(item);
    }
    @Override
    protected void onDestroy() {
        stopService(playIntent);
        musicSrv=null;
        super.onDestroy();
    }
    //to set the controller up
    private void setController()
    {
        //set the controller up
        controller=new MusicController(this);
        controller.setPrevNextListeners(new View.OnClickListener() {
            @Override
            public void onClick (View v)
            {
                playNext();
            }
        },
                new View.OnClickListener(){
            @Override
            public void onClick(View v)
            {
                playPrev();
            }
        });
        controller.setMediaPlayer(this);
        controller.setAnchorView(findViewById(R.id.song_list));
        controller.setEnabled(true);
    }
    private void playNext()
    {
        musicSrv.playNext();
        if(playbackPaused)
        {
            setController();
            playbackPaused=false;
        }
        controller.show(0);
    }
    private void playPrev()
    {
        musicSrv.playPrev();
        if(playbackPaused)
        {
            setController();
            playbackPaused=false;
        }
        controller.show(0);
    }
    @Override
    public void start() {
        Toast.makeText(this,"media player start method() called",Toast.LENGTH_LONG).show();
        musicSrv.go();
    }

    @Override
    public void pause() {
        playbackPaused=true;
        musicSrv.pausePlayer();

    }

    @Override
    public int getDuration()
    {
        if(musicSrv!=null && musicBound && musicSrv.isPng())
            return musicSrv.getDur();
        else
            return 0;
    }

    @Override
    public int getCurrentPosition()
    {
        if(musicSrv!=null && musicBound && musicSrv.isPng())
            return musicSrv.getPosn();
        else
            return 0;
    }

    @Override
    public void seekTo(int pos) {
        musicSrv.seek(pos);
    }

    @Override
    public boolean isPlaying()
    {
        if(musicSrv!=null && musicBound)
            return musicSrv.isPng();
        return false;
    }

    @Override
    public int getBufferPercentage() {
        return 0;
    }

    @Override
    public boolean canPause() {
        return true;
    }

    @Override
    public boolean canSeekBackward() {
        return true;
    }

    @Override
    public boolean canSeekForward() {
        return true;
    }

    @Override
    public int getAudioSessionId() {
        return 0;
    }
    @Override
    protected void onPause()
    {
        super.onPause();
        paused=true;
    }
    @Override
    protected void onResume()
    {
        super.onResume();
        if(paused) {
            setController();
            paused = false;
        }
    }
    @Override
    protected  void onStop()
    {
        controller.hide();
        super.onStop();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        Toast.makeText(this,"BACK BUTTON IS PRESSED",Toast.LENGTH_LONG).show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflates the menu;
        // this adds items to the action bar if it is present.
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

}
