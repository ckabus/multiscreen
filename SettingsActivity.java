package com.coernel.tf_multiscreen;

import android.Manifest;
import android.app.Activity;
import android.app.NotificationManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.PowerManager;
import android.os.Vibrator;
import android.support.v4.provider.DocumentFile;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View.OnClickListener;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;

public class SettingsActivity extends AppCompatActivity implements OnClickListener{
    Vibrator vibrator 			= null;
    Button start_button;
    EditText edit_myID;
    EditText edit_myMC_Addr;
    EditText edit_myDataDir;

    private SharedPreferences mPrefs;   // Zum Sichern der GeräteID und der Multicast IP Adresse


    File sdDir;
    String path;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        //this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        setContentView(R.layout.activity_settings);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        //requestPermissions( new String[] {Manifest.permission.READ_EXTERNAL_STORAGE},99);

        //PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        //PowerManager.WakeLock wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,"MyWakelockTag");
        //wakeLock.acquire();

        edit_myID        =  (EditText) findViewById(R.id.edit_myID);
        edit_myMC_Addr   =  (EditText) findViewById(R.id.edit_MC_Addr);
        edit_myDataDir   =  (EditText) findViewById(R.id.editDataDir);


        start_button = (Button)findViewById(R.id.button_start);
        start_button.setOnClickListener(this);
        findViewById(R.id.button_start).setOnClickListener(this);



        // Vibrator installieren
        //
        vibrator =(Vibrator)getSystemService(VIBRATOR_SERVICE);
        //vibrator.vibrate(500);

        // Stelle die Editfelder aus den gespeicherten Einstellungen wieder her
        SharedPreferences mPrefs = getSharedPreferences("tf_settings",MODE_PRIVATE);
        edit_myID.setText(mPrefs.getString("MY_ID","A"));
        edit_myMC_Addr.setText(mPrefs.getString("MY_MC_ADDR", "224.1.1.1"));
        edit_myDataDir.setText(mPrefs.getString("MY_DATA_DIR", "/storage/emulated/0/"));

        //sp=new SoundPlayer(this);
    }

    protected void onStart() {
        super.onStart();
        if(Build.VERSION.SDK_INT >= 23) {
            requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.CAMERA, Manifest.permission.VIBRATE,Manifest.permission.ACCESS_NOTIFICATION_POLICY}, 99);
        }

    }

    protected void onStop() {
        super.onStop();


        // Einstellungen fürs nächste mal speichern
        SharedPreferences mPrefs = getSharedPreferences("tf_settings", MODE_PRIVATE);
        SharedPreferences.Editor editor = mPrefs.edit();
        editor.putString("MY_ID", edit_myID.getText().toString());
        editor.putString("MY_MC_ADDR", edit_myMC_Addr.getText().toString());
        editor.putString("MY_DATA_DIR", edit_myDataDir.getText().toString());
        editor.commit();



        /*
        if (sp!=null) {
            sp.releaseSoundPlayer();
            sp=null;
        }*/

    }

    public void onClick(View view) {
        if (view.getId() == R.id.button_start) {
            //vibrator.vibrate(500);

            //sp.playSound("AAA.mp3");

            /* Funzt!
            sdDir = Environment.getExternalStorageDirectory();
            path = sdDir.getAbsolutePath() + "/1_Audio/";
            Toast.makeText(getApplicationContext(), "Playing:"+ path + "AAA.mp3",Toast.LENGTH_LONG).show();
            //MediaPlayer mp = MediaPlayer.create(this, Uri.parse(path + "AAA.mp3"));//getPath()
            try {
                mp.setDataSource(path + "AAA.mp3");
                mp.prepare();
                mp.setLooping(false);
                mp.start();
            } catch (IOException e) {
                Toast.makeText(getApplicationContext(), "ERROR Playing:"+ path + "AAA.mp3",Toast.LENGTH_LONG).show();
            }
            */



            //mp.setDataSource(filename);
            //mp.prepare();
            //mp.setMax(mp.getDuration());
            //mp.seekTo(0);
            //mp.start();



            final Context context = this;
            Intent intent = new Intent(context, MainActivity.class);

            // Geräte ID, Multicastadresse und SD-Card Pfad an die neue Activity übergeben
            intent.putExtra("MY_ID", edit_myID.getText().toString());
            intent.putExtra("MY_MC_ADDR", edit_myMC_Addr.getText().toString());
            intent.putExtra("MY_DATA_DIR", edit_myDataDir.getText().toString());
            startActivity(intent);



        } /*else if (view.getId() == R.id.button_getDir){
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
            startActivityForResult(intent, 42);
        }*/


    }


    public void onActivityResult(int requestCode, int resultCode, Intent resultData) {
        if (resultCode == RESULT_OK) {
            Uri treeUri = resultData.getData();
            ContentResolver cs;
            DocumentFile pickedDir = DocumentFile.fromTreeUri(this, treeUri);


            //edit_myDataDir.setText( pickedDir.getName().ge);



            // List all existing files inside picked directory
            /*
            for (DocumentFile file : pickedDir.listFiles()) {

                Log.d("LENNOX", "Found file " + file.getName() + " with size " + file.length());

            }
            */

            // Create a new file and write into it
            /*
            DocumentFile newFile = pickedDir.createFile("text/plain", "My Novel");
            OutputStream out = getContentResolver().openOutputStream(newFile.getUri());
            out.write("A long time ago...".getBytes());
            out.close();
            */
        }
    }
}