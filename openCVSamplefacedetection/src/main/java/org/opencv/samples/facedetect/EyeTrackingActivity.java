package org.opencv.samples.facedetect;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Toast;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.github.mikephil.charting.utils.ColorTemplate;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;

import wseemann.media.FFmpegMediaMetadataRetriever;

/**
 * This activity is used to licalize an eye center and generate a line chart of the eye center movements.
 * This mainly includes following tasks:
 * 1. Accessing the video captured in AndroidVideoCaptureExample from the SD card.
 * 2. Segmenting the video into number of image frames.
 * 3. Process every image frame for eye center localization.
 * 4. Plot the eye center coordinates with respect to time.
 * 5. Generate an arrayList of all the eye centers located in the video. Process this arrayList for detecting the type of Nystagmus.
 */
public class EyeTrackingActivity extends Activity{

    public native int[] findEyeCenter(long mFace, int[] mEye);

    private static final Scalar    	FACE_RECT_COLOR     = new Scalar(255, 255, 255, 255);

    private Mat 					mGray;

    private File 					mCascadeFile;
    private CascadeClassifier     	face_cascade;

	private float                 	mRelativeFaceSize   = 0.5f;//0.5f
	private int                     mAbsoluteFaceSize   = 0;

   	int leftEyePoint [] = new int[2];

	int screen_width, screen_height;

	Point leftPupil;

    private Button button,savechart;

    //GraphView
    private LineChart mChart;
    //Text file

    private int count = 0;
    String typeNyst="";

    public ArrayList<Double> Delta_x = new ArrayList<Double>();
    public ArrayList<Double> Delta_y = new ArrayList<Double>();

    public String uri = "";

    String datatoCollect="Init";

    private static final String TAG = "OCVSample::NDK";

    /**
     * Default BaseLoaderCallback implementation treats application context as Activity and
     * calls Activity.finish() method to exit in case of initialization failure
     */
    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.v("before","before data collect");

                	System.loadLibrary("example");
                    Log.i(TAG, "OpenCV loaded successfully");

                    final InputStream eye;
                    FileOutputStream osEye;

                    try {
                        //-------------------------Eye classifier----------------//
                        eye = getResources().openRawResource(R.raw.haarcascade_lefteye_2splits);
                        File cascadeDirEye = getDir("cascade", Context.MODE_PRIVATE);
                        if (datatoCollect.equals("Right Eye Processing")){
                            mCascadeFile = new File(cascadeDirEye, "haarcascade_righteye_2splits.xml");
                            Log.i(TAG,"Right eye template loaded");
                        }
                        else {
                            mCascadeFile = new File(cascadeDirEye, "haarcascade_lefteye_2splits.xml");
                            Log.i(TAG,"Left eye template loaded");
                        }

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



                    Log.v("Loc12",uri);
                    loadFrames();

                    //mOpenCvCameraView.enableView();
                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };

    /**
     * Call for EyeTrackingActivity
     */
    public EyeTrackingActivity() {
      Log.i(TAG, "Instantiated new " + this.getClass());
    }

    /**
     * This method accesses the video from SD card and divides it into image frame
     * Then Every image frame is passed to localize the eye center in it.
     */
    public void loadFrames() {
        File sdcard = Environment.getExternalStorageDirectory();
        Log.v("something","something");
        Log.v("Loc1",uri);
        File videoFile = new File(uri);
        if(videoFile.exists())Log.v("videolog", "exists the file");

        FFmpegMediaMetadataRetriever retriever = new FFmpegMediaMetadataRetriever();
        retriever.setDataSource(videoFile.getAbsolutePath());
        ArrayList<Bitmap> rev=new ArrayList<Bitmap>();
        //String frameRate = retriever.extractMetadata(FFmpegMediaMetadataRetriever.METADATA_KEY_FRAMERATE);
        String time = retriever.extractMetadata(FFmpegMediaMetadataRetriever.METADATA_KEY_DURATION);
        //int FRate = Integer.parseInt(frameRate);
        int FRate = 30;
        //Log.v("FRate",frameRate);
        Log.v("FRate",""+FRate);
        int videoDuration = Integer.parseInt(time);
        Log.v("VD",""+videoDuration);

        for(long i=0;i<videoDuration;i+=(1000/FRate)){

           Bitmap bitmap = retriever.getFrameAtTime(i*1000, FFmpegMediaMetadataRetriever.OPTION_CLOSEST);

            Log.v("Time",""+i);
            Mat imgToProcess = new Mat();
            Mat imgToDest = new Mat();
            Log.v("ImageOp","Image Mat Generated"+i);
            Utils.bitmapToMat(bitmap, imgToProcess);
            Log.v("ImageOp","bmp to mat done"+i);

            Imgproc.cvtColor(imgToProcess, imgToDest, Imgproc.COLOR_BGR2GRAY);
            Log.v("process", ""+i);
            //use imgToDest
            processFrames(imgToDest,i);
            Log.v("ImageProcess","Process done "+i);
            Log.v("Imageframe","Image frame generated "+i);

        }
       Toast.makeText(EyeTrackingActivity.this, "Graph Implemented!", Toast.LENGTH_LONG).show();
       typeNyst = typeDetect(Delta_x,Delta_y);

    }

    /**
     * This method provides a button to move to the next activity.
     * While moving to the next activity, the type of Nystagmus detected is passed.
     */
    public void addListenerOnButton() {

        final Context context = this;

        button = (Button) findViewById(R.id.btnNext);

        button.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {

                Intent intent = new Intent(context, Results.class);
                intent.putExtra("Nystagmus_Type",typeNyst);
                //intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);

            }

        });

        /**
         * The button savechart is used to save the chart in an image format.
         */
        savechart = (Button) findViewById(R.id.saveChart);

        savechart.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {

                LineChart saveChart = (LineChart) findViewById(R.id.linechart);
                saveChart.saveToGallery("chart",85);

            }

        });

    }

    /**
     * The onCreate method initializes the surface view and other chart variables.
     * @param savedInstanceState
     */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Log.i(TAG, "called onCreate");
		super.onCreate(savedInstanceState);
        Log.i(TAG, "on create");

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.face_detect_surface_view);
        Display display = getWindowManager().getDefaultDisplay();
        android.graphics.Point size = new android.graphics.Point();
        display.getSize(size);
        screen_width = size.x;
        screen_height = size.y;
        Log.i(TAG, "W: " + String.valueOf(screen_width) + " - H: " + String.valueOf(screen_height));

        LineChart linechart = (LineChart) findViewById(R.id.linechart);
        mChart = new LineChart(this);
        linechart.addView(mChart);
        mChart.setDrawMarkerViews(true);
        mChart.setNoDataText("No data for the moment");
        mChart.setHighlightPerDragEnabled(true);
        mChart.setTouchEnabled(true);
        mChart.setDragEnabled(true);
        //mChart.setScaleEnabled(true);
        mChart.setDrawGridBackground(false);
        mChart.setScaleXEnabled(true);
        mChart.setScaleYEnabled(false);
        mChart.getDescription().setEnabled(true);
        //mChart.setDoubleTapToZoomEnabled(true);
        mChart.setPinchZoom(true);
        mChart.fitScreen();
        mChart.setBackgroundColor(Color.LTGRAY);
        LineData data = new LineData();
        data.setValueTextColor(Color.WHITE);
        mChart.setData(data);

        Legend l = mChart.getLegend();
        l.setForm(Legend.LegendForm.LINE);
        l.setPosition(Legend.LegendPosition.BELOW_CHART_LEFT);
        l.setTextColor(Color.WHITE);
        l.setEnabled(true);

        XAxis x1 = mChart.getXAxis();
        x1.setTextColor(Color.WHITE);
        x1.setDrawGridLines(false);
        x1.setAvoidFirstLastClipping(true);

        YAxis y1 = mChart.getAxisLeft();
        y1.setTextColor(Color.WHITE);
        y1.setDrawGridLines(true);

        YAxis y12 = mChart.getAxisRight();
        y12.setEnabled(false);

        Intent intent = getIntent();
        datatoCollect = intent.getStringExtra("Eye");
        Log.v("whicheye","after data collect");
        uri = intent.getStringExtra("VideoLoc");
        Log.v("VideoLoc",uri);
        addListenerOnButton();

	}

    /*public void writeTo(double dX,double dY,double t)
    {
        //double a = 5.2;

        String S = "dX:"+String.valueOf(dX)+"  "+"dY:"+String.valueOf(dY)+" "+"t in ms:"+String.valueOf(t)+"  "+"frame"+count++;
        Log.i(TAG,"S"+S);
        try
        {
            // Creates a trace file in the primary external storage space of the
            // current application.
            // If the file does not exists, it is created.
            File traceFile = new File(this.getExternalFilesDir(null),"myvideo.txt");

            if (!traceFile.exists())
            traceFile.createNewFile();
            // Adds a line to the trace file
            BufferedWriter writer = new BufferedWriter(new FileWriter(traceFile,true));
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
    }*/

    /**
     * This class plots the X and Y coordinates of the eye center with respect to time.
     * @param dataPoints0
     * @param dataPoints1
     * @param Time
     */
    private void addEntry(double dataPoints0,double dataPoints1,double Time) {
        LineData data = mChart.getData();

        if (data != null) {
            ILineDataSet set0 = data.getDataSetByIndex(0);
            ILineDataSet set1 = data.getDataSetByIndex(1);

           if (set0 == null || set1 ==null) {
               set0 = createSet(0);
               set1 = createSet(1);
               data.addDataSet(set0);
               data.addDataSet(set1);
            }
            data.addDataSet(set0);
            data.addDataSet(set1);

            Log.v("XValue",""+(float) Time);
            data.addEntry(new Entry((float)Time,(float)dataPoints0),0);
            data.addEntry(new Entry((float)Time,(float)dataPoints1),1);
            data.notifyDataChanged();

            mChart.notifyDataSetChanged();

        }
        Log.v("Graph Generated","At Delta_x "+dataPoints0+" At time"+Time);
        Log.v("Graph Generated","At Delta_y "+dataPoints1+" At time"+Time);
    }

    /**
     *This method creates the LineDataSet for both the line charts and assigns the special parameters to it.
     * @param flag
     * @return
     */
    private LineDataSet createSet(int flag){
        LineDataSet set = new LineDataSet(null,"Time");
        set.setDrawCircles(false);
        //set.setDrawCubic(false);//may be set true for pendel for better view
        set.setCubicIntensity(0.1f);//Sets the intensity for cubic lines (if enabled). Max = 1f = very cubic, Min = 0.05f = low cubic effect, Default: 0.2f
        set.setAxisDependency(YAxis.AxisDependency.LEFT);
        if(flag == 0) {
            set.setColor(ColorTemplate.getHoloBlue());
        }
        else
            set.setColor(Color.BLACK);
        set.setLineWidth(0.2f);
        set.setFillAlpha(65);
        set.setFillColor(ColorTemplate.getHoloBlue());
        set.setHighLightColor(Color.WHITE);
        set.setValueTextSize(10f);
        return set;
    }

    /**
     * when onPause, release Activity control in order to run other applications.
     */
	@Override
    public void onPause()
    {

        super.onPause();
    }

    /**
     * this method is called when the activity that was hidden comes back to view on the screen.
     */
    @Override
    public void onResume()
    {
        super.onResume();
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_9, this, mLoaderCallback);
       /* if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_9, this, mLoaderCallback);
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }*/

    }

    /**
     *  The final call receive before the activity is destroyed.
     *  This can happen either because the activity is finishing (someone called finish() on it,
     *  or because the system is temporarily destroying this instance of the activity to save space.
     */
    public void onDestroy() {
        super.onDestroy();

    }

    /**
     * This Method accepts the image frame from a video and passes it for eye center detection.
     * @param frame
     * @param time
     * @return
     */
    public Mat processFrames(Mat frame, long time) {
        Log.v("Process1","Processing of frame started");
    	mGray = frame;
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
        Log.v("Template","Template matched");
        Rect[] facesArray = faces.toArray();
        if (facesArray.length!=1) {
            Log.v("Nullvalue", "NullValue returned"+time);
            return null;
        }

        for (Rect aFacesArray : facesArray) {

            Core.rectangle(mGray, aFacesArray.tl(), aFacesArray.br(), FACE_RECT_COLOR, 3);

            findEyes(mGray, aFacesArray,time);

        }
        Log.v("Process2","Processing of frames finished");

        return mGray;
    }

    /**
     * The gray scale image is processed with NDK call and the eye center is returned in terms of coordinate points.
     * The difference in eye center and a fixed reference point is calculated.
     * This difference value is passed to plot a line chart.
     * @param frame_gray
     * @param face
     * @param time
     * @return
     */
    private Mat findEyes(Mat frame_gray, Rect face, long time) {

    	Mat faceROI = frame_gray.submat(face);

        int eye_region_width = (int) (face.width * 0.75);
        int eye_region_height = (int) (face.width * 0.45);
        int eye_region_top = (int) (face.height * 0.40);
        int leftEyeRegion_x = (int) (face.width * 0.13);
        Rect leftEyeRegion = new Rect(leftEyeRegion_x,eye_region_top,eye_region_width,eye_region_height);
        int [] leftEyeArray = {leftEyeRegion_x,eye_region_top,eye_region_width,eye_region_height};

        leftEyePoint = findEyeCenter(faceROI.getNativeObjAddr(), leftEyeArray);

        leftPupil = new Point(leftEyePoint[0], leftEyePoint[1]);

        leftPupil.x += (leftEyeRegion.x+ face.x);
        leftPupil.y += (leftEyeRegion.y+ face.y);

        Point center = new Point(eye_region_width/2, eye_region_height/2);
        center.x += (leftEyeRegion_x+face.x);
        center.y += (eye_region_top+face.y);
        Log.v("Center","Center located");
        Core.circle(mGray, center, 3, new Scalar(255, 255, 255, 255), 3);

        Point Delta = new Point(leftPupil.x - center.x, leftPupil.y - center.y);
        Delta.x += (leftEyeRegion_x+face.x);
        Delta.y += (eye_region_top+face.y);

        Log.i(TAG,"xCenter"+String.valueOf(center.x));
        Log.i(TAG,"left_x"+String.valueOf(leftPupil.x));
        Log.i(TAG,"Delta_x"+String.valueOf(Delta.x));
        Log.i(TAG,"yCenter"+String.valueOf(center.y));
        Log.v("left_y","left_y0"+String.valueOf(leftPupil.y));
        Log.i(TAG,"Delta_y"+String.valueOf(Delta.y));
        //writeTo(Delta.x,Delta.y,time);

        Delta_x.add(Delta.x);
        Delta_y.add(Delta.y);

        Core.circle(frame_gray, leftPupil, 3, FACE_RECT_COLOR);

        Log.v("left_y","left_y1"+leftPupil.y);
        addEntry(Delta.x,Delta.y,time);

        return frame_gray;
    }

    /**
     * This class calculates the average value of the array list
     * @param lists
     * @return
     */
    public static double calculateAverage(ArrayList<Double>lists) {
        Double sum = 0.0;
        if(!lists.isEmpty()) {
            for (Double list : lists) {
                sum += list;
            }
            return sum.doubleValue() / lists.size();
        }
        return sum;
    }

    /**
     * This method calculates the variance in an array list.
     * @param list
     * @return
     */
    public static double variance(ArrayList<Double> list) {
        double sumDiffsSquared = 0.0;
        double avg = calculateAverage(list);
        for (int i = 0; i < list.size(); i++) {
            Double value = list.get(i);
            double diff = value - avg;
            diff *= diff;
            sumDiffsSquared += diff;
        }
        return sumDiffsSquared  / (list.size()-1);
    }

    /**
     * The method returns the type of nystagmus detected by implementing few mathematical algorithms.
     * @param delta_x
     * @param delta_y
     * @return
     */
    public static String typeDetect(ArrayList<Double> delta_x, ArrayList<Double> delta_y){
        ArrayList<Integer> binary;
        String Nyst="";
        Log.v("Delta_x ",""+delta_x);
        Log.v("Delta_y ",""+delta_y);
        double varDelta_x = variance(delta_x);
        double varDelta_y = variance(delta_y);
        Log.v("Variance_x",""+varDelta_x);
        Log.v("Variance_y",""+varDelta_y);
        if(varDelta_x > varDelta_y){
            Log.v("Nystagmus","Horizontal Type");
            binary = encoder(delta_x);
            Log.v("Binary Seq",": "+binary);
            Log.v("Binary seq",": "+binary.size());
            int typeInt = findSequence(binary);
            if(typeInt==0 || typeInt==1) {
                Log.v("Nystagmus", "Left Beat Nystagmus");
                Nyst = "Horizontal-Left Beat Nystagmus";
            }
            else {
                Log.v("Nystagmus", "Right Beat Nystagmus");
                Nyst = "Horizontal-Right Beat Nystagmus";
            }
        }
        else if(varDelta_x < varDelta_y){
            Log.v("Nystagmus","Vertical Type");
            binary = encoder(delta_y);
            Log.v("Binary Seq",": "+binary);
            Log.v("Binary seq",": "+binary.size());
            int typeInt = findSequence(binary);
            if(typeInt==0 || typeInt==1) {
                Log.v("Nystagmus", "Down Beat Nystagmus");
                Nyst = "Vertical-Down Beat Nystagmus";
            }
            else {
                Log.v("Nystagmus", "Up Beat Nystagmus");
                Nyst = "Vertical-Up Beat Nystagmus";
            }
        }
        else{
            Log.v("Nystagmus","Couldn't recognize");
        }
        return Nyst;

    }

    /**
     * This method finds out the number of occurance of the bit sequences in the binary encoded array.
     * @param binary
     * @return
     */
    public static int findSequence(ArrayList<Integer> binary) {

        int[] typeInt = new int[4];
        for (int i = 2; i < binary.size(); i++) {
            int Ele0 = binary.get(i - 2);
            int Ele1 = binary.get(i - 1);
            int Ele2 = binary.get(i);
            //int Ele3 = binary.get(i);
            if (Ele0 == 1 && Ele1 == 1 && Ele2 == 0) {
                typeInt[0] += 1;
            } else if (Ele0 == 0 && Ele1 == 1 && Ele2 == 0) {
                typeInt[1] += 1;
            } else if (Ele0 == 1 && Ele1 == 0 && Ele2 == 1) {
                typeInt[2] += 1;
            } else if (Ele0 == 0 && Ele1 == 0 && Ele2 == 1) {
                typeInt[3] += 1;
            }
            Log.v("typeint", "" + Arrays.toString(typeInt));
        }
        int max = typeInt[0];

        int index = 0;
        for (int i = 0; i < typeInt.length; i++) {
            if (typeInt[i] > max) {
                max = typeInt[i];
                index = i;
            }
        }
        Log.v("index of max value", ":" + index);

        return index;
    }

    /**
     * This method encodes the Array list into a binary encoded array.
     * Every rise in the array list is represented as 1 and every decay is represented as 0.
     * @param delta
     * @return
     */
    public static ArrayList<Integer> encoder(ArrayList<Double> delta) {
        ArrayList<Integer> e = new ArrayList<>();
        for (int i = 1; i < delta.size(); i++){
            Double value1 = delta.get(i-1);
            Double value2 = delta.get(i);
            if(value2>=value1){
                e.add(1);
            }
            else
                e.add(0);
        }
     return e;

    }

}
