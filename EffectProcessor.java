package com.coernel.tf_multiscreen;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.constraint.Constraints;
import android.view.SurfaceHolder;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.Toast;

import java.io.IOException;
import java.net.BindException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.util.concurrent.atomic.AtomicBoolean;



public class EffectProcessor extends Thread {
    final AtomicBoolean running = new AtomicBoolean(true);

    MainActivity activity;
    Handler handler;
    static Handler fx_handler=null;


    public String   cmd_target_id;  // WER: soll was machen
    public String   cmd_action_id;  // WAS soll gemacht werden
    public String   cmd_param_1;    // WOMIT soll es gemacht werden
    public String   cmd_param_2;    // WOMIT soll es gemacht werden
    public String   cmd_data;       // WANN soll es ausgeführt werden (Zufällig oder Sequentiell)
    //public String   packet_tid;     // Paket Sequenznummer
    public int      myGroupOrder=0; // Wenn Gruppennachrichten erhalten wurden, an welcher Position stehe ich
    public int      myGroupDelay=0; // Millisekunden bevor ich meine Aktion starte
    private int     curr_tid = 0,
                    prev_tid = 0,
                    packet_count=0;

    private volatile int mW=-1;
    private volatile int mH=-1;

    String mystr;
    String myID;

    static volatile String strCmdLine;

    SoundPlayer sp=null;
    Bitmap bmImage;
    private int actionId=StateConstants.ST_CLEAR_ALL;
    private volatile int i=0;
    private volatile int j=0;
    private int tid=0;


    public EffectProcessor (MainActivity activity, Handler handler) {
        this.handler        = handler;
        this.activity       = activity;
        this.myID           = activity.str_myID;
        this.sp             = new SoundPlayer(activity);
    }

    public void run() {

        Looper.prepare();
        fx_handler = new  Handler(){
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                fx_handler.obtainMessage();
                //data = msg.getData().getFloatArray("data");
                //doSomethingWithData(data);
               process_fx_state(msg.obj.toString());

            };
        };
        Looper.loop();
    }



    private void process_fx_state(final String strCmdLine) {

        //
        //
        // DEBUG ONLY
        this.handler.post(new Runnable() {
            @Override
            public void run() {
                activity.textConsole.setText(strCmdLine+" Packets: "+Integer.toString(packet_count++));
            }
        });
        // END OF DEBUG

        if (parse_incoming_data(strCmdLine)) {

            //if (cmd_target_id.equals(this.myID) || cmd_target_id.equals("ALL")){  // Die Nachricht ist für uns

            try {
                actionId = Integer.parseInt(cmd_action_id);
            } catch (  NumberFormatException nfe ){
                actionId=StateConstants.ST_CLEAR_ALL;
                mystr=nfe.getMessage();
                this.handler.post(new Runnable() {
                    @Override
                    public void run() {
                        activity.textConsole.setText(mystr);
                    }
                });
            }

            switch (actionId) {
                case StateConstants.ST_SOUND_PLAY:
                    this.handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            //sleep(1000);
                            sp.playSound(cmd_param_1);
                        }
                    },myGroupDelay*myGroupOrder);
                    break;

                case StateConstants.ST_SOUND_STOP:
                    this.handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            sp.stopSound();
                        }
                    },myGroupDelay*myGroupOrder);
                    break;

                case StateConstants.ST_SPLASHIMAGE_HIDE:
                    this.handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            sp.stopSound();
                        }
                    },myGroupDelay*myGroupOrder);
                    // no break; here, Fallthrough to next state
                case StateConstants.ST_IMAGE_HIDE:
                    this.handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            activity.imageView.setVisibility(View.GONE);
                        }
                    },myGroupDelay*myGroupOrder);
                    break;

                case StateConstants.ST_SPLASHIMAGE_SHOW_ANIM_1:
                    this.handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            //sleep(1000);
                            activity.animation.start();
                        }
                    },myGroupDelay*myGroupOrder);
                    // no break; here, fallthrough to next state!
                case StateConstants.ST_SPLASHIMAGE_SHOW:
                    this.handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            //sleep(1000);
                            sp.playSound(cmd_param_2);
                        }
                    },myGroupDelay*myGroupOrder);
                    // no break; here, fallthrough to next state!
                case StateConstants.ST_IMAGE_SHOW:
                    /*
                    try {
                        i = Integer.parseInt(cmd_param_1);
                    } catch (  NumberFormatException nfe ){
                        break;
                    }*/
                    bmImage= BitmapFactory.decodeFile(activity.str_DataDir+"1_Bilder/"+cmd_param_1);  //

                    this.handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            // VideoView verbergen
                            activity.videoView.setVisibility(View.GONE);
                            if (activity.videoView.isPlaying()) {
                                activity.videoView.stopPlayback();
                            }

                            activity.imageView.setVisibility(View.VISIBLE);
                            activity.imageView.setImageBitmap(bmImage);
                            //activity.animation.start();
                        }
                    },myGroupDelay*myGroupOrder);
                    break;

                case StateConstants.ST_VIDEO_PLAY:

                    this.handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {

                            activity.imageView.setVisibility(View.GONE);

                            activity.videoView.setVisibility(View.VISIBLE);
                            activity.videoView.setVideoPath(activity.str_DataDir+"1_Video/"+cmd_param_1);
                            activity.videoView.seekTo(0);
                            //activity.videoView.setRotation(90.0f);
                            //activity.videoView.requestFocus();
                            activity.videoView.start();
                        }
                    },myGroupDelay*myGroupOrder);


                    this.handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {

                            int width = activity.videoView.getMeasuredWidth();
                            int height = activity.videoView.getMeasuredHeight();
                            //Toast.makeText(activity,"width = "+width+", height = "+height, Toast.LENGTH_LONG).show();
                            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(mW, mH);
                            params.topMargin=j; params.leftMargin=i;
                            activity.videoView.setLayoutParams(params);
                        }
                    },(myGroupDelay*myGroupOrder)+150);

                    break;

                case StateConstants.ST_VIDEO_STOP:
                    this.handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            activity.videoView.setVisibility(View.GONE);
                            if (activity.videoView.isPlaying()) {
                                activity.videoView.stopPlayback();
                            }
                            //activity.videoView.setVideoPath(activity.str_DataDir+"1_Video/"+cmd_param_1);
                            //activity.videoView.seekTo(0);
                            //activity.videoView.setVisibility(View.GONE);
                        }
                    },myGroupDelay*myGroupOrder);
                    break;

                case StateConstants.ST_FLASHLIGHT_ON:
                    this.handler.postDelayed(new Runnable() {
                        //this.handler.post(new Runnable() {
                        @Override
                        public void run() {
                            activity.turnOnFlash();
                        }
                    },myGroupDelay*myGroupOrder);
                    break;

                case StateConstants.ST_FLASHLIGHT_OFF:
                    this.handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            activity.turnOffFlash();
                        }
                    },myGroupDelay*myGroupOrder);
                    break;
//myGroupOrder
                case StateConstants.ST_CLEAR_ALL:       // ALLES stoppen und schwarzen Bildschirm machen
                    this.handler.post(new Runnable() {
                        @Override
                        public void run() {

                            // VideoView verbergen
                            activity.videoView.setVisibility(View.GONE);
                            if (activity.videoView.isPlaying()) {
                                activity.videoView.stopPlayback();
                            }

                            // ImageView verbergen
                            activity.imageView.setVisibility(View.GONE);

                            // Sound AUS
                            sp.stopSound();

                            // Fotolampe AUS
                            activity.turnOffFlash();

                            // YET TO DO:
                            // Stoppe alle Animationen

                        }
                    });
                    break;


                case StateConstants.ST_SET_SCREEN_SIZE:     // Setze Bildschirmgröße (Default -1,-1)

                    try {
                        mW = Integer.parseInt(cmd_param_1);
                    } catch (  NumberFormatException nfe ){
                        break;
                    }

                    try {
                        mH = Integer.parseInt(cmd_param_2);
                    } catch (  NumberFormatException nfe ){
                        break;
                    }

                    break;

                case StateConstants.ST_SET_SCREEN_OFFSET:   // Verschiebung des Bildausschnittes (Default 0,0)

                    try {
                        i = Integer.parseInt(cmd_param_1);
                    } catch (  NumberFormatException nfe ){
                        break;
                    }

                    try {
                        j = Integer.parseInt(cmd_param_2);
                    } catch (  NumberFormatException nfe ){
                        break;
                    }
                    break;

                    case StateConstants.ST_RESET_SCREEN:
                   /*
                    this.handler.post(new Runnable() {
                        @Override
                        public void run() {

                            activity.videoView.getHolder().setFixedSize(-1,-1);
                            activity.mRootParam.width=-1;
                            activity.mRootParam.height=-1;
                            activity.mRootParam.leftMargin=0;
                            activity.mRootParam.topMargin=0;

                        }
                    });
                    */
                    break;


                case StateConstants.ST_VIBRATOR_ON_DURATION:    // YET TO DO... Die Lännge des Vibrators implementieren
                    this.handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            activity.vibrator.vibrate(100);
                        }
                    },myGroupDelay*myGroupOrder);
                    break;
                case StateConstants.ST_VIBRATOR_OFF:
                    break;

            }

            //}
        }// */

    }


    // Parse den eingetroffenen String und übertrage die Werte in globale Variablen
    private boolean parse_incoming_data(String inData) {
        myGroupOrder = 0;
        myGroupDelay = 0;

        if (inData.isEmpty()) {
            return false;
        } else {
            String[] splitStr = inData.split("\\:");    // Suche nach allen Feldern, die durch ':' getrennt sind
            if (splitStr.length != 6) {                       // Wir suchen genau 6 Felder
                //outputErrorToConsole("Error: Please enter an IP Address in this format: xxx.xxx.xxx.xxx\n" +
                //        "Each 'xxx' segment may range from 0 to 255.");
                return false;
            }

            // Hole die Transaktionsnummer des Datenpaketes aus letzten Feld
            //
            try {
                curr_tid = Integer.parseInt(splitStr[5]);
                if (curr_tid==prev_tid) {       // Wenn die SequenzID die gleiche ist, wie vorher, dann haben wir das Paket schon
                    return false;
                } else {
                    prev_tid=curr_tid;
                }
            } catch (  NumberFormatException nfe ){
                return false;
            }


            // Extrahiere den Adressaten
            //
            cmd_target_id = splitStr[0];
            if (cmd_target_id.isEmpty()) {
                return false;
            }

            cmd_action_id = splitStr[1];
            if (cmd_action_id.isEmpty())
                return false;

            cmd_param_1     = splitStr[2];
            if (cmd_param_1.isEmpty())
                return false;

            cmd_param_2     = splitStr[3];
            if (cmd_param_2.isEmpty())
                return false;

            cmd_data      = splitStr[4];
            if (cmd_data.isEmpty())
                return false;
        }


        // Wenn unser Name oder 'ALL' im Feld steht, dann weiter...
        //
        if (cmd_target_id.equals("ALL")) return true;
        if (cmd_target_id.equals(this.myID)) return true;

        // Der vorletzte Parameter gibt mit +Wert die Sequenzverzögerung und mit -Wert die Zufallsverzögerung in Millisekunden an
        //
        try {
            myGroupDelay = Integer.parseInt(cmd_data);
            if (myGroupDelay<0) {
                int j=(int)(Math.random()*(-myGroupDelay));
                myGroupDelay=j;
            }
        } catch (  NumberFormatException nfe ){

            return false;   //Geht das ? -> Kein gültiger Integerwert im letzten Parameter

            //actionId=StateConstants.ST_CLEAR_ALL;
            //mystr=nfe.getMessage();
            /*this.handler.post(new Runnable() {
                @Override
                public void run() {
                    activity.textConsole.setText(mystr);
                }
            });
            */
        }

        // Wenn wir hier sind, schaue nach, ob wir in einer Gruppe vorhanden sind und extrahiere
        // ggf. unsere Position.
        // Gruppen werden z.B. in cmd_target_id=B#E#H angegeben
        String[] groupStr = cmd_target_id.split("\\#");    // Suche nach allen Feldern, die durch '#' getrennt sind
        for (int i=0;i<groupStr.length;i++) {
            if (groupStr[i].equals(this.myID)) {
                myGroupOrder=i;
                return true;
            }
        }

        // Wenn wir hier sind, ist kein gültiges Token mehr vorhanden,
        // oder wir sind nicht gemeint
        //
        return false;
    }

    void stopRunning() {
        sp.releaseSoundPlayer();
        Looper l=this.fx_handler.getLooper();
        l.quit();
        //Looper.myLooper().quit();
        this.running.set(false);
    }

}
