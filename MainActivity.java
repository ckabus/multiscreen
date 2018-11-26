/**
 * Multiscreen APP für Android
 * Version 1.0.0, September 2018
 * by Cornelius Kabus
 *
 * Ermöglicht die synchrone Wiedergabe von Video, Sound und Bildern auf beliebig vielen
 * Geräten.
 *
 * Anwendungen: Videowall aus Smartphones, Soundtexturen, MultiscreenPräsentationen
 *
 * Empfängt Stringtokens über Multicastadresse und gibt lokal gespeicherte Inhalte wieder.
 *
 */


package com.coernel.tf_multiscreen;

import android.animation.ObjectAnimator;
import android.app.NotificationManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.net.ConnectivityManager;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.support.constraint.Constraints;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.view.KeyEvent;
import android.view.SurfaceHolder;
import android.view.WindowManager;
import android.view.Window;
import android.view.View;
import android.view.MotionEvent;
import android.view.animation.AnticipateInterpolator;
import android.view.animation.AnticipateOvershootInterpolator;
import android.view.animation.BounceInterpolator;
import android.view.animation.CycleInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.os.Vibrator;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.widget.VideoView;

import java.io.File;


public class MainActivity extends AppCompatActivity {

    private multicast_listener_thread MulticastListenerThread;
    private EffectProcessor EffectProcessorThread;
    private AudioManager myAudioManager;

    private static final int ON_DO_NOT_DISTURB_CALLBACK_CODE = 999;

    Vibrator    vibrator 			= null;
    String      str_myID;
    String      str_MC_Addr;
    String      str_DataDir;

   // static volatile String      ipc_str =new String("ECH");        // Stringobjekt als geteiltes Datenobjekt für die Threads
    //boolean bMutex=false;

    ObjectAnimator animation;
    ObjectAnimator anim_Spinner;
    ObjectAnimator animZoom;
    //ImageLoader imLoader;

    ImageView   imageView;
    TextView    textConsole;
    VideoView   videoView;

    WifiManager     wifi;
    WakeLock        wakeLock;
    private WifiManager.MulticastLock wifiLock;

    private Camera camera;
    private boolean isFlashOn;
    private boolean hasFlash;
    Parameters params;

    public FrameLayout.LayoutParams mRootParam;

    int j=0;



    @Override
    protected void onCreate(Bundle savedInstanceState) {

        // Hole die Einstellungen  aus vorheriger SettingsApp
        //
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            str_myID = extras.getString("MY_ID");
            str_MC_Addr = extras.getString("MY_MC_ADDR");
            str_DataDir = extras.getString("MY_DATA_DIR");
        }

        super.onCreate(savedInstanceState);


        // FULLSCREEN MODUS
        //
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        //this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        setContentView(R.layout.activity_main);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);


        // Layout holen (für Videoscaling)
        //
        mRootParam = (FrameLayout.LayoutParams) ((View) findViewById(R.id.root_view)).getLayoutParams();

        videoView = (VideoView) findViewById(R.id.videoView);
        imageView = (ImageView) findViewById(R.id.imageView);
        textConsole = (TextView) findViewById(R.id.mainTextView);
        videoView.setVisibility(View.GONE);


        // ENEGRIESPARMODUS Ausschalten
        //
        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,"MyWakelockTag");
        wakeLock.acquire();

        // Vibrator installieren
        //
        vibrator =(Vibrator)getSystemService(VIBRATOR_SERVICE);
        //vibrator.vibrate(1000);

        // Telefon Stumm schalten
        //
        myAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        if(Build.VERSION.SDK_INT < 23) {
            myAudioManager.setRingerMode(AudioManager.RINGER_MODE_SILENT);
        }

        // Maximale Medienlautstärke einstellen
        //
        int maxVolume = myAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        myAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, maxVolume, 0);


        // Hole Bitmaps von SD Karte
        //
        /*
        imLoader=new ImageLoader(this);
        Thread thread = new Thread(imLoader);
        thread.start();
        */

    }

    @Override
    protected void onStart() {
        super.onStart();

/*
        videoView = (VideoView) findViewById(R.id.videoView);
        imageView = (ImageView) findViewById(R.id.imageView);
        textConsole = (TextView) findViewById(R.id.mainTextView);
        videoView.setVisibility(View.GONE);
        */

        animation = ObjectAnimator.ofFloat(imageView, "rotation", 360);//180
        animation.setInterpolator(new BounceInterpolator());//AnticipateInterpolator|BounceInterpolator|CycleInterpolator(float cycles)
        //AnticipateOvershootInterpolator
        animation.setDuration(2000);
        //animation.reverse();
        //animation.setRepeatCount(100);
        //animation.start();

        anim_Spinner = ObjectAnimator.ofFloat(textConsole, "rotation", 360);
        anim_Spinner.setDuration(1000);
        //anim_Spinner.start();


        hasFlash=checkforFlash();
        getCamera();

        /* FÜR SPÄTERE VERSIONEN NOCHMAL ANGUCKEN
        NotificationManager notificationManager =
                (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                && !notificationManager.isNotificationPolicyAccessGranted()) {

            Intent intent = new Intent(
                    android.provider.Settings
                            .ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS);

            startActivity(intent);
        }
        notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_NONE);
        */


        // Prüfe ob WIFI eingeschaltet ist und sichere WIFI Sperre
        //
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        if (connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI).isConnected()) {
            // WifiManager wifiManager = (WifiManager) activity.getSystemService(Context.WIFI_SERVICE);
            wifi = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            wifiLock = wifi.createMulticastLock("MulticastTester");
            wifiLock.acquire();

            // Listener Thread starten
            //
            start_listening();

            // Starte Effektprozessor
            //
            start_effect_processor();

            //Toast.makeText(getApplicationContext(), "Starte Service (" +  str_myID + ") auf " + str_MC_Addr, Toast.LENGTH_SHORT).show();

        } else {

            Toast.makeText(getApplicationContext(), "WIFI ist Abgeschaltet. Bitte einschalten und Service neu starten!",Toast.LENGTH_SHORT).show();

            // mystr = "WIFI ist ABGESCHALTET...";
            // helloTextView.setText(mystr.toCharArray(), 0, mystr.length());
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        // on stop release the camera
        if (camera != null) {
            camera.release();
            camera = null;
        }
        stop_listening();

    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        /*if (isListening)
            stopListening();
        */
        wakeLock.release();
        //wifiLock.release();
        //stop_listening();
    }


    @Override
    public void onBackPressed() {
        if (j++==2) {
            super.onBackPressed();
            //Toast.makeText(this,(String)"Ciao!",
            //        Toast.LENGTH_SHORT).show();
        } else {
           Toast.makeText(this,(String)"3x drücken um zu beenden",Toast.LENGTH_SHORT).show();
        }
    }


/*
    @Override
    protected void onPause() {
       super.onPause();
       startActivity(new Intent(getApplicationContext(), MainActivity.class).setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION));
        finish();
    }
*/

    private void stop_listening() {
        if (wifiLock !=null && wifiLock.isHeld()) {
            wifiLock.release();
        }
        stopThreads();
    }

    private void stopThreads() {

        // Effektprozessor stoppen
        if (this.EffectProcessorThread != null)
            this.EffectProcessorThread.stopRunning();

        // MulticastListener abschalten
        if (this.MulticastListenerThread != null)
            this.MulticastListenerThread.stopRunning();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        //Toast.makeText(getApplicationContext(),"onTouch Event!", Toast.LENGTH_LONG).show();
        /*
        vibrator.vibrate(50);
        animation.start();

        if (isFlashOn) {
            // turn off flash
            turnOffFlash();
        } else {
            // turn on flash
            turnOnFlash();
        }
        */
        return true;
    }


    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_VOLUME_UP:    // Ignoriere Lautstärkeregler
                //myAudioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC,
                //        AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI);
                //Toast.makeText(this,(String)"Ignoring KEYCODE_VOLUME_UP",Toast.LENGTH_SHORT).show();
                return true;
            case KeyEvent.KEYCODE_VOLUME_DOWN: // Ignoriere Lautstärkeregler
                //myAudioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC,
                //        AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI);
                //Toast.makeText(this,(String)"Ignoring KEYCODE_VOLUME_DOWN",Toast.LENGTH_SHORT).show();
                return true;
            //case KeyEvent.KEYCODE_BACK:
            //    Toast.makeText(this,(String)"BACK gedrückt",Toast.LENGTH_SHORT).show();
            //    return true;
            case KeyEvent.ACTION_DOWN:
                //Toast.makeText(this,(String)"Ignoring ACTION_DOWN",Toast.LENGTH_SHORT).show();
                return true;

            default:
                // return false;
                // Update based on @Rene comment below:
                //Toast.makeText(this,(String)"Catch Keyevent "+keyCode ,Toast.LENGTH_SHORT).show();
                //return true;
                return super.onKeyDown(keyCode, event);
        }
    }



    private void start_effect_processor() {
        this.EffectProcessorThread = new EffectProcessor(this,new Handler());
        EffectProcessorThread.start();
    }



    private void start_listening() {
        this.MulticastListenerThread = new multicast_listener_thread(this, new Handler(),this.str_MC_Addr,this.str_myID);
        this.MulticastListenerThread.setPriority(Thread.MAX_PRIORITY);
        MulticastListenerThread.start();
    }

    private boolean checkforFlash() {
        /*
         * First check if device is supporting flashlight or not
         */
        boolean hasFlash;
        hasFlash = getApplicationContext().getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH);

        /*
        if (hasFlash)
            Toast.makeText(this, "FOTOLICHT benutzbar!", Toast.LENGTH_LONG).show();
        else
            Toast.makeText(this, "KEIN FOTOLICHT vorhanden - Schade!", Toast.LENGTH_LONG).show();
        */
        return hasFlash;
    }

    // Get the camera
    private void getCamera() {
        if (camera == null) {
            try {
                camera = Camera.open();
                params = camera.getParameters();
            } catch (RuntimeException e) {
                Toast.makeText(this, "Failed to open Camera! " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        }
    }


    // Turning On flash
    public void turnOnFlash() {
        if (!isFlashOn) {
            if (camera == null || params == null) {
                return;
            }

            params = camera.getParameters();
            params.setFlashMode(Parameters.FLASH_MODE_TORCH);//FLASH_MODE_TORCH
            camera.setParameters(params);
            camera.startPreview();
            isFlashOn = true;

        }

    }


    // Turning Off flash
    public void turnOffFlash() {
        if (isFlashOn) {
            if (camera == null || params == null) {
                return;
            }

            params = camera.getParameters();
            params.setFlashMode(Parameters.FLASH_MODE_OFF);
            camera.setParameters(params);
            camera.stopPreview();
            isFlashOn = false;

        }
    }

    /*
    private void requestMutePhonePermsAndMutePhone() {
        try {
            if (Build.VERSION.SDK_INT < 23) {
                AudioManager audioManager = (AudioManager) getApplicationContext().getSystemService(Context.AUDIO_SERVICE);
                audioManager.setRingerMode(AudioManager.RINGER_MODE_SILENT);
            } else if( Build.VERSION.SDK_INT >= 23 ) {
                this.requestDoNotDisturbPermissionOrSetDoNotDisturbApi23AndUp();
            }
        } catch ( SecurityException e ) {

        }
    }

    private void requestDoNotDisturbPermissionOrSetDoNotDisturbApi23AndUp() {
        //TO SUPPRESS API ERROR MESSAGES IN THIS FUNCTION, since Ive no time to figrure our Android SDK suppress stuff
        if( Build.VERSION.SDK_INT < 23 ) {
            return;
        }

        NotificationManager notificationManager = (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
        if ( notificationManager.isNotificationPolicyAccessGranted()) {
            AudioManager audioManager = (AudioManager) getApplicationContext().getSystemService(Context.AUDIO_SERVICE);
            audioManager.setRingerMode(AudioManager.RINGER_MODE_SILENT);
        } else{
            // Ask the user to grant access
            Intent intent = new Intent(android.provider.Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS);
            startActivityForResult( intent, MainActivity.ON_DO_NOT_DISTURB_CALLBACK_CODE );
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Check which request we're responding to
        if (requestCode == MainActivity.ON_DO_NOT_DISTURB_CALLBACK_CODE ) {
            this.requestDoNotDisturbPermissionOrSetDoNotDisturbApi23AndUp();
        }
    }
    */
}
