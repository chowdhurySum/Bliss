package com.example.sumitra.myapplication;

import android.app.Service;
import android.content.Intent;
import android.media.MediaPlayer;
import java.util.ArrayList;
import android.content.ContentUris;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;
import android.os.PowerManager;
import android.provider.MediaStore;
import android.util.Log;
import java.util.Random;
import android.app.Notification;
import android.app.PendingIntent;
import android.widget.Toast;

/**
 * Created by sumitra on 15/10/16.
 */
public class MusicService extends Service implements MediaPlayer.OnPreparedListener,MediaPlayer.OnErrorListener,MediaPlayer.OnCompletionListener{
    //media player
    private MediaPlayer player;
    //song list
    private ArrayList<Song> songs;
    //current position
    private int songPosn;
    //variables for Song title and notification id
    private String songTitle="";
    private static final int NOTIFY_ID=1;
    //for shuffleing
    private boolean shuffle=false;
    private Random rand;

    private final IBinder musicBind=new MusicBinder();
    public void onCreate()
    {
        //create the service
        super.onCreate();
        //instantiate random number generator
        rand=new Random();
        //initialise the position
        songPosn=0;
        //create player
        player=new MediaPlayer();
        //create audio manager
        //AudioManager manager=new AudioManager();
        //manager.requestAudioFocus();
        initMusicPlayer();
        //set player properties
    }
    public void initMusicPlayer()
    {
        //set player properties
        player.setWakeMode(getApplicationContext(),PowerManager.PARTIAL_WAKE_LOCK);
        player.setAudioStreamType(AudioManager.STREAM_MUSIC);
        player.setOnPreparedListener(this);
        player.setOnCompletionListener(this);
        player.setOnErrorListener(this);
    }
    public void setList(ArrayList<Song> theSongs)
    {
        songs=theSongs;
    }

    public class MusicBinder extends Binder{
        MusicService getService()
        {
            return MusicService.this;
        }
    }
    @Override
    public IBinder onBind(Intent intent){
        return musicBind;
    }
    @Override
    public boolean onUnbind(Intent intent){
        player.stop();
        player.release();
        return false;
    }
    public void playSong(){
        //play a song
        player.reset();
        //get song
        Song playSong=songs.get(songPosn);
        songTitle=playSong.getTitle();
        //get id
        long currSong=playSong.getId();
        //set uri
        Uri trackUri=ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,currSong);
        try{
            player.setDataSource(getApplicationContext(),trackUri);
        }
        catch(Exception e){
            Log.e("MUSIC SERVICE","Error setting data source",e);
        }
        player.prepareAsync();
    }
    @Override
    public void onPrepared(MediaPlayer mp)
    {
        //start playback
        mp.start();
        //Toast.makeText(this,"PLAYER STARTED",Toast.LENGTH_LONG).show();
        Intent noIntent=new Intent(this,MainActivity.class);
        noIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendInt=PendingIntent.getActivity(this,0,noIntent,PendingIntent.FLAG_UPDATE_CURRENT);
        Notification.Builder builder=new Notification.Builder(this);
        builder.setContentIntent(pendInt).setSmallIcon(R.drawable.play).setTicker(songTitle).setOngoing(true).setContentTitle("Playing").setContentText(songTitle);
        Notification notification= builder.build();

        startForeground(NOTIFY_ID,notification);
    }
    @Override
    public boolean onError(MediaPlayer mp,int what,int extra)
    {
        mp.reset();
        return false;
    }
    @Override
    public void onCompletion(MediaPlayer mp)
    {
        if(player.getCurrentPosition()>0)
        {
            mp.reset();
            playNext();
        }
    }
    public void setSong(int songIndex)
    {
        songPosn=songIndex;
    }
    public int getPosn(){
        return player.getCurrentPosition();
    }

    public int getDur(){
        return player.getDuration();
    }

    public boolean isPng(){
        return player.isPlaying();
    }

    public void pausePlayer(){
        player.pause();
    }

    public void seek(int posn){
        player.seekTo(posn);
    }

    public void go(){
        player.start();
        Toast.makeText(this,"music service go() method called",Toast.LENGTH_LONG).show();
    }
    //skip to previous
    public void playPrev(){
        songPosn--;
        if(songPosn<0)
            songPosn=songs.size()-1;
        playSong();
    }
    //skips to next
    public void playNext(){
        if(shuffle)
        {
            int newSong=songPosn;
            while(newSong==songPosn)
                newSong=rand.nextInt(songs.size());
            songPosn=newSong;
        }
        else
        {
            songPosn = (songPosn + 1) % songs.size();
        }
        playSong();
    }
    @Override
    public void onDestroy()
    {
        stopForeground(true);
    }
    public void setShuffle() {
        if (shuffle)
            shuffle = false;
        else
            shuffle = true;
    }


}
