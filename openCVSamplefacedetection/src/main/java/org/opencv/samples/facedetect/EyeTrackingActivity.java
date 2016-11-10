package org.opencv.samples.facedetect;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.android.FpsMeter;
import org.opencv.android.JavaCameraView;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.*;
import org.opencv.objdetect.CascadeClassifier;
import org.opencv.imgproc.Imgproc;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.security.Policy;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Random;

import android.hardware.Camera;
import android.media.MediaScannerConnection;
import android.os.AsyncTask;
import android.os.Bundle;
import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.os.Environment;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Display;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.DataSet;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

import com.github.mikephil.charting.utils.ColorTemplate;


public class EyeTrackingActivity extends Activity implements CvCameraViewListener2{

    private Context context;

    public native int[] findEyeCenter(long mFace, int[] mEye);
//	private static native long nativeCreateObject(String fileName);

    private static final Scalar    	FACE_RECT_COLOR     = new Scalar(255, 255, 255, 255);
    private Mat 				 	mRgba;
    private Mat 					mGray;
    private Mat 					mGrayNew;
    private Mat 					mretVal;
    private Mat 					scaledMatrix;
    private Mat 					tempMatrix;
    private Mat 					invertcolormatrix;

    private File 					mCascadeFile;
    private CascadeClassifier     	face_cascade;
    private CustomizableCameraView 	mOpenCvCameraView;

	private float                 	mRelativeFaceSize   = 0.5f;
	private int                     mAbsoluteFaceSize   = 0;

	double xCenter = -1;
	double yCenter = -1;

	int leftEyePoint [] = new int[2];
	int rightEyePoint [] = new int[2];

	Point[] calibrationArray = new Point[4];

	int screen_width, screen_height;
	static double scale_factor;
	Point leftPupil, rightPupil;
    static double d;
    static double Delta_x,Delta_y;

    //GraphView
    private LineChart mChart;
    private int lastX = 0;

    private static final String TAG = "OCVSample::NDK";

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                	System.loadLibrary("example");
                    Log.i(TAG, "OpenCV loaded successfully");

                    final InputStream is;
                    final InputStream eye;
                    FileOutputStream os;
                    FileOutputStream osEye;

                    try {
                        /*is = getResources().openRawResource(R.raw.haarcascade_frontalface_alt);
                        File cascadeDir = getDir("cascade", Context.MODE_PRIVATE);
                        mCascadeFile = new File(cascadeDir, "haarcascade_frontalface_alt.xml");

                        os = new FileOutputStream(mCascadeFile);

                        byte[] buffer = new byte[4096];
                        int bytesRead;
                        while ((bytesRead = is.read(buffer)) != -1) {
                            os.write(buffer, 0, bytesRead);
                        }

                        is.close();
                        os.close();*/
                        //-------------------------Eye classifier----------------//
                        eye = getResources().openRawResource(R.raw.haarcascade_lefteye_2splits);
                        File cascadeDirEye = getDir("cascade", Context.MODE_PRIVATE);
                        mCascadeFile = new File(cascadeDirEye, "haarcascade_lefteye_2splits.xml");

                        osEye = new FileOutputStream(mCascadeFile);

                        byte[] bufferEye = new byte[4096];
                        int bytesReadEye;
                        while ((bytesReadEye = eye.read(bufferEye)) != -1) {
                            osEye.write(bufferEye, 0, bytesReadEye);
                        }

                        eye.close();
                        osEye.close();
                        face_cascade = new CascadeClassifier(mCascadeFile.getAbsolutePath());
                        if (face_cascade.empty()) {
                            Log.e(TAG, "Failed to load cascade classifier");
                            face_cascade = null;
                        } else
                            Log.i(TAG, "Loaded cascade classifier from " + mCascadeFile.getAbsolutePath());

                        cascadeDirEye.delete();
                    } catch (IOException e) {
                        Log.i(TAG, "face cascade not found");
                    }

                    mOpenCvCameraView.enableView();
                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };

    public EyeTrackingActivity() {
      Log.i(TAG, "Instantiated new " + this.getClass());
  }


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Log.i(TAG, "called onCreate");
		super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.face_detect_surface_view);
        Display display = getWindowManager().getDefaultDisplay();
        android.graphics.Point size = new android.graphics.Point();
        display.getSize(size);
        screen_width = size.x;
        screen_height = size.y;
        Log.i(TAG, "W: " + String.valueOf(screen_width) + " - H: " + String.valueOf(screen_height));

        mOpenCvCameraView = (CustomizableCameraView) findViewById(R.id.fd_activity_surface_view);

        // Change the resolution (best resolution 352x288)
//        mOpenCvCameraView.setMaxFrameSize(1280, 720);
        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        mOpenCvCameraView.setCameraIndex(JavaCameraView.CAMERA_ID_FRONT);
        mOpenCvCameraView.setMaxFrameSize(640,480);
        mOpenCvCameraView.enableFpsMeter();
        mOpenCvCameraView.setCvCameraViewListener(this);

        //GraphView
        LineChart linechart = (LineChart) findViewById(R.id.linechart);
        mChart = new LineChart(this);
        linechart.addView(mChart);
        mChart.setDrawMarkerViews(false);
        mChart.setDescription("");
        mChart.setNoDataTextDescription("No data for the moment");
        mChart.setHighlightPerDragEnabled(true);
        mChart.setTouchEnabled(true);
        mChart.setDragEnabled(true);
        mChart.setScaleEnabled(true);
        mChart.setDrawGridBackground(false);
        mChart.setPinchZoom(true);
        mChart.setBackgroundColor(Color.LTGRAY);
        LineData data = new LineData();
        data.setValueTextColor(Color.WHITE);
        mChart.setData(data);
        mChart.animateXY(2000, 2000);


        Legend l = mChart.getLegend();

        l.setForm(Legend.LegendForm.LINE);
        l.setTextColor(Color.WHITE);

        XAxis x1 = mChart.getXAxis();
        x1.setTextColor(Color.WHITE);
        x1.setDrawGridLines(false);
        x1.setAvoidFirstLastClipping(true);

        YAxis y1 = mChart.getAxisLeft();
        y1.setTextColor(Color.WHITE);
        y1.setAxisMinValue(60f);
        y1.setAxisMaxValue(180f);
        y1.setDrawGridLines(true);

        YAxis y12 = mChart.getAxisRight();
        y12.setEnabled(false);

        //text file
	}


    public void writeTo()
    {
        //double a = 5.2;
        String S = "dX:"+String.valueOf(Delta_x)+"  "+"dY:"+String.valueOf(Delta_y);
        try
        {
            // Creates a trace file in the primary external storage space of the
            // current application.
            // If the file does not exists, it is created.
            File traceFile = new File(this.getExternalFilesDir(null),"TraceFile.txt");
            if (!traceFile.exists())
                traceFile.createNewFile();
            // Adds a line to the trace file
            BufferedWriter writer = new BufferedWriter(new FileWriter(traceFile, true /*append*/));
            writer.write(S);
            writer.write("\n");
            writer.close();
            // Refresh the data so it can seen when the device is plugged in a
            // computer. You may have to unplug and replug the device to see the
            // latest changes. This is not necessary if the user should not modify
            // the files.
            MediaScannerConnection.scanFile(this,
                    new String[] { traceFile.toString() },
                    null,
                    null);

        }
        catch (IOException e) {
            Log.e("facedetect","Unable to write to the TraceFile.txt file.");
        }
    }

    private void addEntry() {
        LineData data = mChart.getData();

        LineDataSet set = (LineDataSet) data.getDataSetByIndex(0);
        if (set == null) {
            set = createSet();
            data.addDataSet(set);
        }

        Entry entry = new Entry((float)d, set.getEntryCount());
        data.addEntry(entry, 0);

        mChart.notifyDataSetChanged();
        mChart.setVisibleXRange(0,6);
        mChart.moveViewToX(data.getXValCount() - 7);
    }

    private LineDataSet createSet(){
        Date date =new Date();
        String timeFrame = new SimpleDateFormat("mm:ss.SSSSSS").format(date);
        LineDataSet set = new LineDataSet(null,timeFrame);

        set.setDrawCubic(true);
        set.setCubicIntensity(0.2f);
        set.setAxisDependency(YAxis.AxisDependency.LEFT);
        set.setColor(ColorTemplate.getHoloBlue());
        set.setCircleColor(ColorTemplate.getHoloBlue());
        set.setLineWidth(1f);
        set.setCircleSize(4f);
        set.setFillAlpha(65);
        set.setFillColor(ColorTemplate.getHoloBlue());
        set.setHighLightColor(Color.WHITE);
        set.setValueTextSize(10f);
        return set;
    }

	@Override
    public void onPause()
    {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    public void onResume()
    {
        super.onResume();
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_9, this, mLoaderCallback);
    }

    public void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    public void onCameraViewStarted(int width, int height) {
    	mRgba = new Mat();
    	mGray = new Mat();
        mGrayNew = new Mat();
        scaledMatrix = new Mat();
    	tempMatrix = new Mat();
    	invertcolormatrix= new Mat();

        // Displayes the support FPS range in Logcat ?. Check there
        mOpenCvCameraView.getSupportedPreviewFpsRange();
        mOpenCvCameraView.setPreviewFPS(15, 30);
//    	mretVal = new Mat();
    }

    public void onCameraViewStopped() {
        mGray.release();
        mGrayNew.release();
        scaledMatrix.release();
        tempMatrix.release();
        invertcolormatrix.release();
    }


    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
    	mGray = inputFrame.gray();


    	MatOfPoint pointsMat = new MatOfPoint();

    	if (mAbsoluteFaceSize == 0) {
            int height = mGray.rows();
            if (Math.round(height * mRelativeFaceSize) > 0) {
                mAbsoluteFaceSize = Math.round(height * mRelativeFaceSize);
            }
        }

        MatOfRect faces = new MatOfRect();

        if (face_cascade != null) {
            face_cascade.detectMultiScale(mGray, faces, 1.1, 2, 2, //TODO: objdetect.CV_HAAR_SCALE_IMAGE
                    new Size(mAbsoluteFaceSize, mAbsoluteFaceSize), new Size());
        }

        Rect[] facesArray = faces.toArray();
        if (facesArray.length<1)
        	return null;
        for (Rect aFacesArray : facesArray) {
            Core.rectangle(mGray, aFacesArray.tl(), aFacesArray.br(), FACE_RECT_COLOR, 3);
            xCenter = (aFacesArray.x + aFacesArray.width + aFacesArray.x) / 2;
            yCenter = (aFacesArray.y + aFacesArray.y + aFacesArray.height) / 4;
            Point center = new Point(xCenter, yCenter);

            Core.circle(mGray, center, 10, new Scalar(255, 255, 255, 255), 3);

            Core.putText(mGray, "[" + center.x + "," + center.y + "]", new Point(center.x + 20, center.y + 20), Core.FONT_HERSHEY_SIMPLEX, 0.7, new Scalar(255, 0, 0, 255));

            /*scale_factor = screen_width/(double)facesArray[0].width;

            facesArray[0].height = (int) (screen_height/scale_factor);

            facesArray[0].y += 50;


            scaledMatrix = mGray.submat(facesArray[0]);

            Imgproc.resize(scaledMatrix, tempMatrix, new Size(screen_width,screen_height));
            Rect qwer = new Rect(0,0,tempMatrix.width(), tempMatrix.height());*/

            findEyes(mGray, aFacesArray);

            addEntry();
            writeTo();
        }
        return mGray;
    }

    private Mat findEyes(Mat frame_gray, Rect face) {

    	Mat faceROI = frame_gray.submat(face);

        int eye_region_width = (int) (face.width * 0.55);
        int eye_region_height = (int) (face.width * 0.45);
        int eye_region_top = (int) (face.height * 0.40);
        int leftEyeRegion_x = (int) (face.width * 0.28);
        Rect leftEyeRegion = new Rect(leftEyeRegion_x,eye_region_top,eye_region_width,eye_region_height);
        int [] leftEyeArray = {leftEyeRegion_x,eye_region_top,eye_region_width,eye_region_height};
      /*Rect rightEyeRegion = new Rect(face.width - eye_region_width - leftEyeRegion_x,
              eye_region_top,eye_region_width,eye_region_height);
      int [] rightEyeArray = {face.width - eye_region_width - leftEyeRegion_x,
              eye_region_top,eye_region_width,eye_region_height};*/


        // TODO: error when loading the native function
        leftEyePoint = findEyeCenter(faceROI.getNativeObjAddr(), leftEyeArray);
        //rightEyePoint = findEyeCenter(faceROI.getNativeObjAddr(), rightEyeArray);
        leftPupil = new Point(leftEyePoint[0], leftEyePoint[1]);
        //rightPupil = new Point(rightEyePoint[0], rightEyePoint[1]);
        //-- Find Eye Centers

        //rightPupil.x += Math.round(rightEyeRegion.x + face.x);
        //rightPupil.y += Math.round(rightEyeRegion.y + face.y) ;
        leftPupil.x += Math.round(leftEyeRegion.x + face.x);
        leftPupil.y += Math.round(leftEyeRegion.y + face.y);


        //rightPupil = Math.round(rightPupil);
        //leftPupil = unscalePoint(leftPupil);
        Delta_x = xCenter - leftPupil.x;
        Delta_y = yCenter - leftPupil.y;


        // draw eye centers
        //Core.circle(mGray, rightPupil, 3, FACE_RECT_COLOR);
        Core.circle(mGray, leftPupil, 3, FACE_RECT_COLOR);

        d = Math.sqrt( (leftPupil.x-=xCenter)*leftPupil.x + (leftPupil.y-=yCenter)*leftPupil.y);

        return mGray;
    }
}
