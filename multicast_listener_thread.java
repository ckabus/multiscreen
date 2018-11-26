package com.coernel.tf_multiscreen;


import android.content.Context;
import android.graphics.BitmapFactory;
import android.media.MediaPlayer;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.net.BindException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;

public class multicast_listener_thread extends Thread {
    final AtomicBoolean running = new AtomicBoolean(true);
    final String mcIPStr;      //= "224.1.1.1";    //230.
    final String myID;
    final int mcPort          = 12345;
    final MainActivity activity;
    final Handler handler;

    InetAddress mcIPAddress;
    MulticastSocket  mcSocket;

    //private WifiManager.MulticastLock wifiLock;
    private InetAddress inetAddress;

    //static volatile String ipc_str;

    WifiManager wifi;
    WifiInfo wifiInfo;

    int tid=0;


    public multicast_listener_thread (MainActivity activity, Handler handler, String mcIPStr, String myID) {
        this.handler        = handler;
        this.activity       = activity;
        this.mcIPStr        = mcIPStr;
        this.myID           = myID;
    }


    public void run() {

        try {
            wifi = (WifiManager) activity.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            wifiInfo = wifi.getConnectionInfo();
            int wifiIPInt = wifiInfo.getIpAddress();

            byte[] wifiIPByte = new byte[]{
                    (byte) (wifiIPInt & 0xff),
                    (byte) (wifiIPInt >> 8 & 0xff),
                    (byte) (wifiIPInt >> 16 & 0xff),
                    (byte) (wifiIPInt >> 24 & 0xff)};
            inetAddress = InetAddress.getByAddress(wifiIPByte);
            NetworkInterface networkInterface = NetworkInterface.getByInetAddress(inetAddress);

            mcSocket = new MulticastSocket(mcPort);
            mcSocket.setNetworkInterface(networkInterface);
            mcSocket.joinGroup(InetAddress.getByName(mcIPStr));
            mcSocket.setSoTimeout(100);// 0 <- mal UNENDLICH Warten ausprobieren
            mcSocket.setTimeToLive(3);// 1 <- mal nur EINEN Routerhop ausprobieren
        } catch (BindException e) {
            //activity.helloTextView.setText(e.getMessage());
            e.printStackTrace();
        } catch (IOException e) {
            //activity.helloTextView.setText(e.getMessage());
            e.printStackTrace();
        }

        DatagramPacket packet = new DatagramPacket(new byte[128], 128);

        while (running.get()) {

            packet.setData(new byte[128]);

            try {
                if (mcSocket != null) {
                    mcSocket.receive(packet);

                    //tid++;

                    String data = "";
                    data = new String(packet.getData()).trim();

                    Message msg = new Message();
                    msg.obj = data;//"Ali send message";

                    if (EffectProcessor.fx_handler!= null)
                        EffectProcessor.fx_handler.sendMessage(msg);
                }
                else
                    break;
            } catch (IOException ignored) {
                continue;
            }

         }

        if (mcSocket != null)
            this.mcSocket.close();
    }



    void stopRunning() {
        //sp.releaseSoundPlayer();
        this.running.set(false);
    }

    String getLocalIP() {
        return this.inetAddress.getHostAddress();
    }
}
