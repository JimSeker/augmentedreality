package edu.cs4730.facetrackerdemo;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.OnInitListener;
import android.support.v7.app.AppCompatActivity;
import android.view.SurfaceHolder;

import android.util.Log;
import android.view.SurfaceView;
import android.widget.TextView;

import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.MultiProcessor;
import com.google.android.gms.vision.Tracker;
import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.FaceDetector;
import com.google.android.gms.vision.face.LargestFaceFocusingProcessor;

import java.io.IOException;


public class MainActivity extends AppCompatActivity implements SurfaceHolder.Callback, OnInitListener {

    String TAG = "MainActivity";
    CameraSource mCameraSource;
    SurfaceView mPreview;
    TextView mLogger;
    private boolean mSurfaceAvailable;

    //handler, since the facetracker is on another thread.
    protected Handler handler;

    //speech variables.
    private static final int REQ_TTS_STATUS_CHECK = 0;
    private TextToSpeech mTts;
    private  String myUtteranceId = "txt2spk";
    private boolean canspeak;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //get the views first.
        mPreview = (SurfaceView) findViewById(R.id.CameraView);
        //finally, setup the preview pieces
        mPreview.getHolder().addCallback(this);

        mLogger = (TextView) findViewById(R.id.mylogger);

        //Setup the FaceDetector
        Context context = getApplicationContext();
        FaceDetector detector = new FaceDetector.Builder(context)
                .setProminentFaceOnly(true)   //track only one face... makes it faster.
                .setClassificationType(FaceDetector.ALL_CLASSIFICATIONS)  //allows for eye and smile detection!
                .build();

        detector.setProcessor(
                new LargestFaceFocusingProcessor(detector,new FaceTracker()));


        if (!detector.isOperational()) {
            // Note: The first time that an app using face API is installed on a device, GMS will
            // download a native library to the device in order to do detection.  Usually this
            // completes before the app is run for the first time.  But if that download has not yet
            // completed, then the above call will not detect any faces.
            //
            // isOperational() can be used to check if the required native library is currently
            // available.  The detector will automatically become operational once the library
            // download completes on device.
            Log.w(TAG, "Face detector dependencies are not yet available.");
        }

        mCameraSource = new CameraSource.Builder(context, detector)
                .setRequestedPreviewSize(640, 480)
                .setFacing(CameraSource.CAMERA_FACING_FRONT)
                .setRequestedFps(30.0f)
                .build();

        //message handler for textivew.
        handler = new Handler(new Handler.Callback() {
            @Override
            public boolean handleMessage(Message msg) {

                Bundle stuff = msg.getData();
                mLogger.setText(stuff.getString("logthis"));
                mLogger.invalidate();  //should not need this...
                return true;
            }
        });

        // Check to be sure that TTS exists and is okay to use
        Intent checkIntent = new Intent();
        checkIntent.setAction(TextToSpeech.Engine.ACTION_CHECK_TTS_DATA);
        //The result will come back in onActivityResult with our REQ_TTS_STATUS_CHECK number
        startActivityForResult(checkIntent, REQ_TTS_STATUS_CHECK);

    }

    void startPreview() {
        if (mSurfaceAvailable  && mCameraSource != null) {
            try {
                mCameraSource.start(mPreview.getHolder());
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            Log.v(TAG, "preview failed.");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        startPreview();
    }

    /**
     * Stops the camera.
     */
    @Override
    protected void onPause() {
        super.onPause();
        if (mCameraSource != null)
            mCameraSource.stop();
        //if we lose focus, stop talking.
        if( mTts != null)
            mTts.stop();
    }

    /**
     * Releases the resources associated with the camera source, the associated detector, and the
     * rest of the processing pipeline.
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        mCameraSource.release();
        mTts.shutdown();
    }


    /*
    *  methods needed for the surfaceView callback methods.
     */

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        mSurfaceAvailable = true;
        startPreview();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            //should not be called, app is locked in portrait mode.
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        mSurfaceAvailable = false;
    }

    /*
    *  FaceDetector code
     */
    class FaceTracker extends Tracker<Face> {
        boolean RightEye, LeftEye, Smile;
        boolean AskRight, AskLeft, AskSmile;
        String TAG = " Tracker";

        public void onNewItem(int id, Face face) {
            Log.i(TAG, "Awesome person detected.  Hello!");
            //mLogger.setText("New Face");
            sendmessage("New Face");
        }

        public void onUpdate(Detector.Detections<Face> detections, Face face) {


            //Is the left Eye open?
            LeftEye = face.getIsLeftEyeOpenProbability() > 0.75;
            // Log.i(TAG, "LeftEye Open is " + LeftEye);

            //Is the Right Eye open?
            RightEye = face.getIsRightEyeOpenProbability() > 0.75;
            //  Log.i(TAG, "RightEye Open is " + RightEye);

            //Is the face smiling?
            Smile = face.getIsSmilingProbability() > 0.75;
            //  Log.i(TAG, "Smile is " + Smile);
            //mLogger.setText("Smile: " + Smile + " Left: " + LeftEye + " Right:" +RightEye);
            if (!mTts.isSpeaking()) {  //If not speaking.
                if (!LeftEye) {  //checking left eye first.
                    if (!AskLeft) { //have I already asked?
                        Speech("Please Open your Left Eye");
                        AskLeft = true;
                        AskRight = false;
                        AskSmile = false;
                    }
                } else if (!RightEye) {
                    if (!AskRight) {
                        Speech("Please Open your Right Eye");
                        AskLeft = false;
                        AskRight = true;
                        AskSmile = false;
                    }
                } else if (!Smile) {
                    if (!AskSmile)
                    Speech("Please Smile");
                    AskLeft = false;
                    AskRight = false;
                    AskSmile = true;
                } else if (AskSmile) {
                    AskLeft = false;
                    AskRight = false;
                    AskSmile = false;
                    Speech("Perfect!");
                }
            }
            sendmessage("Smile: " + Smile + " Left: " + LeftEye + " Right:" +RightEye);



        }

        public void onDone() {
            Log.i(TAG, "Elvis has left the building.");
           //mLogger.setText("No Face dectected");
            sendmessage("No Face detected.");
        }
    }

    public void Speech(String text) {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            //not sure what an utteranceId is supposed to be... we maybe able to setup a
            //listener for "utterances" and check to see if they completed or something.
            mTts.speak(text, TextToSpeech.QUEUE_ADD, null, myUtteranceId);
        } else {  //below lollipop and use this method instead.
            mTts.speak(text, TextToSpeech.QUEUE_ADD, null);
        }

    }

    public void sendmessage(String logthis) {
        Bundle b = new Bundle();
        b.putString("logthis", logthis);
        Message msg = handler.obtainMessage();
        msg.setData(b);
        msg.arg1 = 1;

        msg.what = 1;  //so the empty message is not used!
       // System.out.println("About to Send message"+ logthis);
        handler.sendMessage(msg);
       // System.out.println("Sent message"+ logthis);
    }

    /*
    *  for the speech part of this code.
     */
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQ_TTS_STATUS_CHECK) {
            switch (resultCode) {
                case TextToSpeech.Engine.CHECK_VOICE_DATA_PASS:
                    // TTS is up and running
                    mTts = new TextToSpeech(this, this);
                    Log.v(TAG, "Pico is installed okay");
                    break;
                case TextToSpeech.Engine.CHECK_VOICE_DATA_FAIL:
                default:
                    Log.e(TAG, "Got a failure. TTS apparently not available");
            }
        }
        else {
            // Got something else
        }
    }
    @Override
    public void onInit(int status) {
        // Now that the TTS engine is ready, we enable the button
        if( status == TextToSpeech.SUCCESS) {
            canspeak =true;
        }
    }
}
