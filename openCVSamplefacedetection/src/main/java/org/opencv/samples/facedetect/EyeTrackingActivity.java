package org.opencv.samples.facedetect;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.media.MediaScannerConnection;
import android.net.Uri;
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
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;

import wseemann.media.FFmpegMediaMetadataRetriever;


public class EyeTrackingActivity extends Activity{

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


	private float                 	mRelativeFaceSize   = 0.5f;//0.5f
	private int                     mAbsoluteFaceSize   = 0;
    private boolean                 kSmoothFaceImage = false;
    private float                   kSmoothFaceFactor = 0.005f;

	double xCenter = -1;
	double yCenter = -1;

	int leftEyePoint [] = new int[2];
	int rightEyePoint [] = new int[2];





	Point[] calibrationArray = new Point[4];

	int screen_width, screen_height;
	static double scale_factor;
	Point leftPupil, rightPupil;
    static double d;
    static double angle,angle1,angle2,angle3,angle4;
    static double time;


    private Button button,savechart;

    //GraphView
    private LineChart mChart;
    //Text file

    private int count = 0;
    private int cnt1 =0;
    String typeNyst="";

    public ArrayList<Double> Delta_x = new ArrayList<Double>();
    public ArrayList<Double> Delta_y = new ArrayList<Double>();
    //double[] Delta_x = new double[500];
    //double[] Delta_y = new double[500];


    String datatoCollect="Init";


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

    public EyeTrackingActivity() {
      Log.i(TAG, "Instantiated new " + this.getClass());
  }

    public void loadFrames() {
        File sdcard = Environment.getExternalStorageDirectory();
        File  videoFile = new File(sdcard,"/my_Vid/myvideo.mp4");
        //File videoFile = new File("/sdcard/my_Vid/LeftBeatingSlowSop_L.mp4");
        if(videoFile.exists())Log.v("videolog", "exists the file");
       // new Decoder().execute(videoFile);
        //File videoFile=new File(Environment.getExternalStorageDirectory().getAbsolutePath()+"/videos","sample_mpeg4.mp4");

        Uri videoFileUri= Uri.parse(videoFile.toString());

        FFmpegMediaMetadataRetriever retriever = new FFmpegMediaMetadataRetriever();
        retriever.setDataSource(videoFile.getAbsolutePath());
        ArrayList<Bitmap> rev=new ArrayList<Bitmap>();
        String frameRate = retriever.extractMetadata(FFmpegMediaMetadataRetriever.METADATA_KEY_FRAMERATE);
        String time = retriever.extractMetadata(FFmpegMediaMetadataRetriever.METADATA_KEY_DURATION);
        int FRate = Integer.parseInt(frameRate);
        Log.v("FRate",""+FRate);
        int videoDuration = Integer.parseInt(time);
        Log.v("VD",""+videoDuration);
        //Create a new Media Player
        /*MediaPlayer mp = MediaPlayer.create(getBaseContext(), videoFileUri);

        //we are using directly a question from stackoverflow
        //apply the answer on it
        //http://stackoverflow.com/questions/12772547/mediametadataretriever-getframeattime-returns-only-first-frame
        long millis = mp.getDuration();
        Log.v("milliseconds","milliseconds got"+millis);*/


        for(long i=0;i<videoDuration;i+=(1000/FRate)){



           Bitmap bitmap = retriever.getFrameAtTime(i*1000, FFmpegMediaMetadataRetriever.OPTION_CLOSEST);

            //Log.v("bitmap size",":"+bitmap.describeContents());
            //rev.add(bitmap);
            //Log.v("some", "message: ");
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
            /*Utils.matToBitmap(mGray,bitmap);
            rev.add(bitmap);*/
            //Toast.makeText(EyeTrackingActivity.this, datatoCollect, Toast.LENGTH_LONG).show();
            Log.v("Imageframe","Image frame generated "+i);
            //mChart.saveToGallery("Result.png",85);


        }
        //retriever.release();
       /*try {
            saveFrames(rev);
        } catch (IOException e) {
            Log.v("saveerror", "saveerror");
            e.printStackTrace();
        }*/
        Toast.makeText(EyeTrackingActivity.this, "Graph Implemented!", Toast.LENGTH_LONG).show();

        typeNyst = typeDetect(Delta_x,Delta_y);

    }



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

        savechart = (Button) findViewById(R.id.saveChart);

        savechart.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {

                LineChart saveChart = (LineChart) findViewById(R.id.linechart);
                saveChart.saveToGallery("chart",85);

            }

        });

    }
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
        //mChart.animateXY(2000, 2000);
        //mChart.saveToGallery("Result.jpg",85);


        Legend l = mChart.getLegend();


        l.setForm(Legend.LegendForm.LINE);
        l.setPosition(Legend.LegendPosition.BELOW_CHART_LEFT);
        l.setTextColor(Color.WHITE);
        l.setEnabled(true);

        XAxis x1 = mChart.getXAxis();
        x1.setTextColor(Color.WHITE);
        x1.setDrawGridLines(false);
        /*x1.setAxisMinValue(0f);
        x1.setAxisMaxValue(1000f);*/
        //x1.setSpaceBetweenLabels(10);


        x1.setAvoidFirstLastClipping(true);

        YAxis y1 = mChart.getAxisLeft();
        y1.setTextColor(Color.WHITE);
        /*y1.setAxisMinValue(-25f);
        y1.setAxisMaxValue(25f);*/
        y1.setDrawGridLines(true);

        YAxis y12 = mChart.getAxisRight();
        y12.setEnabled(false);

        Intent intent = getIntent();
        datatoCollect = intent.getStringExtra("Eye");
        Log.v("eyeSelect0",datatoCollect);
        //Toast.makeText(EyeTrackingActivity.this,datatoCollect, Toast.LENGTH_SHORT).show();

        addListenerOnButton();






        /*mOpenCvCameraView = (CustomizableCameraView) findViewById(R.id.fd_activity_surface_view);

        // Change the resolution (best resolution 352x288)
//        mOpenCvCameraView.setMaxFrameSize(1280, 720);
        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        mOpenCvCameraView.setCameraIndex(JavaCameraView.CAMERA_ID_FRONT);
        mOpenCvCameraView.setMaxFrameSize(640,480);
        mOpenCvCameraView.enableFpsMeter();
        mOpenCvCameraView.setPreviewFPS(7,30);
        mOpenCvCameraView.setCvCameraViewListener(this);*/

        //GraphView


        //time variable

	}




  /*public void saveFrames(ArrayList<Bitmap> saveBitmapList) throws IOException{
        //Random r = new Random();
        //int folder_id = r.nextInt(1000) + 1;
        Log.v("Saving", "Start saving");
        String folder = "/sdcard/theframes/";
        File saveFolder=new File(folder);
        if(!saveFolder.exists()){
            saveFolder.mkdirs();

        }
        Log.v("Size of bitmap",""+saveBitmapList.size());

        int i=1;
        for (Bitmap b : saveBitmapList){
            b = Bitmap.createScaledBitmap(b, 100, 100, true);
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            b.compress(Bitmap.CompressFormat.JPEG, 80, bytes);

            File f = new File(saveFolder,(i+"frame"+".jpg"));

            f.createNewFile();

            FileOutputStream fo = new FileOutputStream(f);
            fo.write(bytes.toByteArray());
            b.recycle();
            fo.flush();
            fo.close();
            Log.v("FileSaved","FileName"+i);
            i++;
        }
        //Toast.makeText(getApplicationContext(),"Folder id : "+folder_id, Toast.LENGTH_LONG).show();

    }*/


    public void writeTo(double dX,double dY,double t)
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
    }
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


            //data.addXValue(String.valueOf(time));
            Log.v("XValue",""+(float) Time);
            data.addEntry(new Entry((float)Time,(float)dataPoints0),0);
            data.addEntry(new Entry((float)Time,(float)dataPoints1),1);
            data.notifyDataChanged();
            //data.addEntry(new Entry((float)yPoint,(int)xPoint), 0);
            mChart.notifyDataSetChanged();
           /* mChart.setVisibleXRange(0,100);
            mChart.moveViewToX(data.getXValCount() - 101);*/

        }
        Log.v("Graph Generated","At Delta_x "+dataPoints0+" At time"+Time);
        Log.v("Graph Generated","At Delta_y "+dataPoints1+" At time"+Time);
    }

    private LineDataSet createSet(int flag){
        //LineDataSet set = new LineDataSet(new Date().getTime(),"Time");

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
        //set.setCircleColor(ColorTemplate.getHoloBlue());
        set.setLineWidth(0.2f);
        //set.setCircleSize(4f);
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
        /*if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();*/
    }

    @Override
    public void onResume()
    {
        super.onResume();
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_9, this, mLoaderCallback);
        /*new Thread(new Runnable() {
            @Override
            public void run() {
                for(int i = 0; i<1000; i++){
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            addEntry();

                        }
                    });

                    try {
                        Thread.sleep(600);
                    } catch (InterruptedException e) {

                    }
                }
            }
        }).start();*/

    }



    public void onDestroy() {
        super.onDestroy();
        /*if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();*/
    }

    /*public void onCameraViewStarted(int width, int height) {
    	mRgba = new Mat();
    	mGray = new Mat();
        mGrayNew = new Mat();
        scaledMatrix = new Mat();
    	tempMatrix = new Mat();
    	invertcolormatrix= new Mat();

        // Displayes the support FPS range in Logcat ?. Check there
//        mOpenCvCameraView.getSupportedPreviewFpsRange();
       mOpenCvCameraView.setPreviewFPS(7,30);
//    	mretVal = new Mat();
    }*/

    /*public void onCameraViewStopped() {
        mGray.release();
        mGrayNew.release();
        scaledMatrix.release();
        tempMatrix.release();
        invertcolormatrix.release();
    }*/


    //public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
    public Mat processFrames(Mat frame, long time) {
        Log.v("Process1","Processing of frame started");

    	mGray = frame;


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
        Log.v("Template","Template matched");
        Rect[] facesArray = faces.toArray();
        if (facesArray.length!=1) {
            Log.v("Nullvalue", "NullValue returned"+time);
            return null;
        }

        for (Rect aFacesArray : facesArray) {
            //Log.v("Nullvalue","at time"+facesArray.length+""+time);
            Core.rectangle(mGray, aFacesArray.tl(), aFacesArray.br(), FACE_RECT_COLOR, 3);
            /*xCenter = (aFacesArray.x + aFacesArray.width + aFacesArray.x) / 2;
            yCenter = (aFacesArray.y + aFacesArray.y + aFacesArray.height) / 3;
            Point center = new Point(xCenter, yCenter);
            Log.v("Center","Center located"+String.valueOf(center));
            Core.circle(mGray, center, 10, new Scalar(255, 255, 255, 255), 3);

            Core.putText(mGray, "[" + center.x + "," + center.y + "]", new Point(center.x + 20, center.y + 20), Core.FONT_HERSHEY_SIMPLEX, 0.7, new Scalar(255, 0, 0, 255));
*/
            /*scale_factor = screen_width/(double)facesArray[0].width;

            facesArray[0].height = (int) (screen_height/scale_factor);

            facesArray[0].y += 50;


            scaledMatrix = mGray.submat(facesArray[0]);

            Imgproc.resize(scaledMatrix, tempMatrix, new Size(screen_width,screen_height));
            Rect qwer = new Rect(0,0,tempMatrix.width(), tempMatrix.height());*/

            findEyes(mGray, aFacesArray,time);


            /*addEntry();


            writeTo();*/
        }
        Log.v("Process2","Processing of frames finished");

        return mGray;
    }

    private Mat findEyes(Mat frame_gray, Rect face, long time) {


        /*org.opencv.core.Size s = new Size(3,3);
        Imgproc.GaussianBlur(frame_gray,frame_gray,s,2);
        Log.v("Gaussian","gaussian blur done");*/

    	Mat faceROI = frame_gray.submat(face);

        int eye_region_width = (int) (face.width * 0.75);
        int eye_region_height = (int) (face.width * 0.45);
        int eye_region_top = (int) (face.height * 0.40);
        int leftEyeRegion_x = (int) (face.width * 0.13);
        Rect leftEyeRegion = new Rect(leftEyeRegion_x,eye_region_top,eye_region_width,eye_region_height);
        int [] leftEyeArray = {leftEyeRegion_x,eye_region_top,eye_region_width,eye_region_height};
        //Corner detection algorithm

        /*xCenter = leftEyeRegion.x+face.x;
        yCenter = leftEyeRegion.y+face.y;
        Point center = new Point(xCenter, yCenter);
        Log.v("Center","Center located");
        Core.circle(mGray, center, 3, new Scalar(255, 255, 255, 255), 3);*/


        // TODO: error when loading the native function
        leftEyePoint = findEyeCenter(faceROI.getNativeObjAddr(), leftEyeArray);
        //rightEyePoint = findEyeCenter(faceROI.getNativeObjAddr(), rightEyeArray);
        leftPupil = new Point(leftEyePoint[0], leftEyePoint[1]);
        //rightPupil = new Point(rightEyePoint[0], rightEyePoint[1]);
        //-- Find Eye Centers

        //rightPupil.x += Math.round(rightEyeRegion.x + face.x);
        //rightPupil.y += Math.round(rightEyeRegion.y + face.y) ;
        leftPupil.x += (leftEyeRegion.x+ face.x);
        leftPupil.y += (leftEyeRegion.y+ face.y);

        /*if (time == 0){
            xCenter = leftEyeRegion.x+eye_region_width/2+face.x;
            yCenter = leftEyeRegion.y+eye_region_height/2+face.y;
            Point center = new Point(xCenter, yCenter);
            Log.v("Center","Center located");
            Core.circle(mGray, center, 3, new Scalar(255, 255, 255, 255), 3);

        }*/
        Point center = new Point(eye_region_width/2, eye_region_height/2);
        center.x += (leftEyeRegion_x+face.x);
        center.y += (eye_region_top+face.y);
       /* xCenter = leftEyeRegion.x+eye_region_width/2+face.x;
        yCenter = leftEyeRegion.y+eye_region_height/2+face.y;
        *//*xCenter = leftEyeRegion.x+eye_region_width/2+face.x;
        yCenter = leftEyeRegion.y+eye_region_height/2+face.y;*//*
        Point center = new Point(xCenter, yCenter);*/
        Log.v("Center","Center located");
        Core.circle(mGray, center, 3, new Scalar(255, 255, 255, 255), 3);

        //rightPupil = Math.round(rightPupil);
        //leftPupil = unscalePoint(leftPupil);
        /*Delta_x = leftPupil.x - center.x;//totally correct subtraction
        Delta_y = leftPupil.y - center.y;*/
        Point Delta = new Point(leftPupil.x - center.x, leftPupil.y - center.y);
        Delta.x += (leftEyeRegion_x+face.x);
        Delta.y += (eye_region_top+face.y);

        Log.i(TAG,"xCenter"+String.valueOf(center.x));
        Log.i(TAG,"left_x"+String.valueOf(leftPupil.x));
        Log.i(TAG,"Delta_x"+String.valueOf(Delta.x));
        Log.i(TAG,"yCenter"+String.valueOf(center.y));
        Log.v("left_y","left_y0"+String.valueOf(leftPupil.y));
        Log.i(TAG,"Delta_y"+String.valueOf(Delta.y));
        writeTo(Delta.x,Delta.y,time);

        Delta_x.add(Delta.x);
        Delta_y.add(Delta.y);


        // draw eye centers
        //Core.circle(mGray, rightPupil, 3, FACE_RECT_COLOR);
        Core.circle(frame_gray, leftPupil, 3, FACE_RECT_COLOR);


        //d = Math.sqrt( (leftPupil.x-=xCenter)*leftPupil.x + (leftPupil.y-=yCenter)*leftPupil.y);



        //angle1 = Math.toDegrees(Math.atan(Delta_y / Delta_x));


        Log.v("left_y","left_y1"+leftPupil.y);
        //Log.v("eye center","detected"+d);
        addEntry(Delta.x,Delta.y,time);


        return frame_gray;
    }
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

    /*private class Decoder extends AsyncTask<File, Integer, Integer> {
        private static final String TAG = "DECODER";

        protected Integer doInBackground(File... params) {
            FileChannelWrapper ch = null;
            try {
                ch = NIOUtils.readableFileChannel(params[0]);
                FrameGrab frameGrab = new FrameGrab(ch);
                org.jcodec.api.FrameGrab.MediaInfo mi = frameGrab.getMediaInfo();
                Bitmap frame = Bitmap.createBitmap(mi.getDim().getWidth(), mi.getDim().getHeight(), Bitmap.Config.ARGB_8888);


                for (int i = 0; i<200; i++) {

                    frameGrab.getFrame(frame);
                    if (frame == null)
                        break;
                    OutputStream os = null;
                    try {
                        os = new BufferedOutputStream(new FileOutputStream(new File(params[0].getParentFile(), String.format("img%08d.jpg", i))));
                        frame.compress(Bitmap.CompressFormat.JPEG, 90, os);
                        Log.v("framegenerated",""+i);
                        Mat imgToProcess = new Mat();
                        Mat imgToDest = new Mat();
                        Utils.bitmapToMat(frame, imgToProcess);
                        Imgproc.cvtColor(imgToProcess, imgToDest, Imgproc.COLOR_BGR2GRAY);
                        Log.v("process", ""+i);
                        //use imgToDest
                        processFrames(imgToDest);
                        Log.v("ImageProcess","Process done "+i);

                    } finally {
                        if (os != null)
                            os.close();
                    }
                    publishProgress(i);

                }
            } catch (IOException e) {
                Log.e(TAG, "IO", e);
            } catch (JCodecException e) {
                Log.e(TAG, "JCodec", e);
            } finally {
                NIOUtils.closeQuietly(ch);
            }
            return 0;
        }

       *//* @Override
        protected void onProgressUpdate(Integer... values) {

            //progress.setText(String.valueOf(values[0]));
        }*//*
    }*/
}
