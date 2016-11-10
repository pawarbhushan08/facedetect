package org.opencv.samples.facedetect;

import android.content.Context;
import android.hardware.Camera;
import android.util.AttributeSet;
import android.util.Log;

import org.opencv.android.JavaCameraView;

import java.util.Iterator;
import java.util.List;

public class CustomizableCameraView extends JavaCameraView {

    public CustomizableCameraView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void setPreviewFPS(double min, double max){
        try {
            Camera.Parameters params = mCamera.getParameters();
            params.setPreviewFpsRange((int) (min * 1000), (int) (max * 1000));
            mCamera.setParameters(params);
            mCamera.reconnect();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void getSupportedPreviewFpsRange(){
        /****************************************************************
         * getSupportedPreviewFpsRange()- Returns specified frame rate
         * (.getSupportedPreviewFpsRange()) to log file and also displays
         * as toast message.
         ****************************************************************/
        Camera.Parameters camParameter = mCamera.getParameters();
        List<int[]> frame = camParameter.getSupportedPreviewFpsRange();
        Iterator<int[]> supportedPreviewFpsIterator = frame.iterator();
        while (supportedPreviewFpsIterator.hasNext()) {
            int[] tmpRate = supportedPreviewFpsIterator.next();
            StringBuffer sb = new StringBuffer();
            sb.append("SupportedPreviewRate: ");
            for (int i = tmpRate.length, j = 0; j < i; j++) {
                sb.append(tmpRate[j] + ", ");
            }
            Log.d("SupportedPreviewRate", "FPS6: " + sb.toString());
            //Toast.makeText(this, "FPS = "+sb.toString(), Toast.LENGTH_SHORT).show();
        }//*****************end getSupportedPreviewFpsRange()**********************
    }
}