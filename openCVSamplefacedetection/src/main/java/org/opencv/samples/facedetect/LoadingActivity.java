package org.opencv.samples.facedetect;

import android.app.Activity;
import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.util.ArrayList;

public class LoadingActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_loading);




        File videoFile = new File("/sdcard/myvideo.mp4");
        //File videoFile=new File(Environment.getExternalStorageDirectory().getAbsolutePath()+"/videos","sample_mpeg4.mp4");

        Uri videoFileUri= Uri.parse(videoFile.toString());

        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        retriever.setDataSource(videoFile.getAbsolutePath());
        ArrayList<Bitmap> rev=new ArrayList<Bitmap>();

        //Create a new Media Player
        MediaPlayer mp = MediaPlayer.create(getBaseContext(), videoFileUri);

        //we are using directly a question from stackoverflow
        //apply the answer on it
        //http://stackoverflow.com/questions/12772547/mediametadataretriever-getframeattime-returns-only-first-frame
        int millis = mp.getDuration();
        for(int i=0;i<millis;i+=100){
            Bitmap bitmap=retriever.getFrameAtTime(i,MediaMetadataRetriever.OPTION_CLOSEST_SYNC);
            rev.add(bitmap);
            //Log.v("some", "message: ");
            Mat imgToProcess = new Mat();
            Mat imgToDest = new Mat();
            org.opencv.android.Utils.bitmapToMat(bitmap, imgToProcess);
            Imgproc.cvtColor(imgToProcess, imgToDest, Imgproc.COLOR_BGR2GRAY);
            //use imgToDest
        }

        //System.out.println(""+rev.size());
        TextView tv = (TextView) findViewById(R.id.editText);
        tv.setText(""+rev.size());
    }
}
