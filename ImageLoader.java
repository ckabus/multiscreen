/**
 * Lade Bitmaps in Array.
 *
 * OBSOLETE: Performancegewinn ist nur marginal gegenüber Speicherverbrauch
 * KLASSE WIRD NICHT EINGESETZT
 *
 */

package com.coernel.tf_multiscreen;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;

import java.io.File;

// Lade Bilder von Dateipfad herunter und dekodiere sie für späteren Gebrauch
//
public class ImageLoader implements Runnable {

    File sdDir;
    String path;
    Bitmap[] bmImage = new Bitmap[30];



    public ImageLoader (MainActivity activity){

        this.path=activity.str_DataDir+"1_Bilder/"; // Hardcodiertes Userverzeichnis:
    }


    @Override
    public void run() {
        //sdDir   = Environment.getExternalStorageDirectory();
        //path    = sdDir.getAbsolutePath() + "/1_Bilder/";
        bmImage[0]=BitmapFactory.decodeFile(path+"0.jpg");  // TEST ONLY: Preload 0.jpg
        bmImage[1]=BitmapFactory.decodeFile(path+"1.jpg");  // TEST ONLY: Preload 1.jpg


    }
}
