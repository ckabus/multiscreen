package com.coernel.tf_multiscreen;
import android.app.Activity;
import android.content.Context;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;

    public class SoundPlayer {

        MediaPlayer     mp;// = null;
        File            sdDir;
        String          path;
        Context         context;
        boolean         isPlaying=false;

        public SoundPlayer(MainActivity activity) {

            this.context=activity.getApplicationContext();
            mp = new MediaPlayer();
            this.path = activity.str_DataDir+"1_Audio/";
        }

        public void playSound(String filename) {
            // Codesnippet um Pfadnamen zu finden
            //sdDir = Environment.getExternalStorageDirectory();
            //path = sdDir.getAbsolutePath() + "/1_Audio/";

            if (isPlaying==true) {
                stopSoundPlayer();
                isPlaying=false;
            }


            //Toast.makeText(this.context, "Playing:" + path + filename, Toast.LENGTH_LONG).show();

            try {
                mp.setDataSource(path + filename);
                mp.prepare();
                mp.setLooping(false);
                mp.setVolume(100.0f,100.0f);
                mp.start();
                isPlaying=true;
            } catch (IOException e) {
                Toast.makeText(this.context, "ERROR Playing:" + path + filename, Toast.LENGTH_LONG).show();
            } catch(IllegalArgumentException e) {
                Toast.makeText(this.context, "IllegalArgumentException" + e.getMessage(), Toast.LENGTH_LONG).show();
            } catch (IllegalStateException e) {
                Toast.makeText(this.context, "IllegalStateException" + e.getMessage(), Toast.LENGTH_LONG).show();
            }

        }

        public void pauseSound() {
        }

        public void stopSound() {
            mp.stop();

        }

        public void releaseSoundPlayer() {
            stopSoundPlayer();
            mp.release();
            //mp=null;

        }

        public void stopSoundPlayer(){
            try {
                mp.stop();
                mp.reset();
            } catch (IllegalStateException e) {
                Toast.makeText(this.context, "IllegalStateException" + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        }
}

