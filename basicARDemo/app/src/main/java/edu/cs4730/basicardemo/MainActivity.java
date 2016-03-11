package edu.cs4730.basicardemo;

import android.app.Activity;
import android.content.Context;
import android.hardware.Camera;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.Window;
import android.widget.TextView;

/*
 * A very simple/basic AR example. 
 * 
 * It turns on the camera preview and displays the sensor for Orientation, accelerometer and gps location over the camera preview.
 * 
 * This code is based on code from 
 * https://github.com/JimSeker/sensors
 * https://github.com/JimSeker/gps
 * https://github.com/JimSeker/video  piccapture demos.  for the camera preview.
 *
 * Yes, some many things are deprecated in 22... camera, orientation... I know.
 * The camera permissions are also not included.
 * 
 */


public class MainActivity extends Activity implements SensorEventListener, LocationListener, SurfaceHolder.Callback {


    TextView tv_alt, tv_lat, tv_long; //gps
    TextView tv_head, tv_pitch, tv_roll; //orientation
    TextView tv_x, tv_y, tv_z;  //ACCELEROMETER

    //sensor and gps
    private LocationManager myL;
    private SensorManager mgr;
    private Sensor accel, orient;

    //camera and preview
    SurfaceView cameraPreview;
    SurfaceHolder previewHolder;
    Camera camera;
    boolean inPreview;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //display setup stuff
        tv_alt = (TextView) findViewById(R.id.altitudeValue);
        tv_lat = (TextView) findViewById(R.id.latitudeValue);
        tv_long = (TextView) findViewById(R.id.longitudeValue);

        tv_head = (TextView) findViewById(R.id.headingValue);
        tv_pitch = (TextView) findViewById(R.id.pitchValue);
        tv_roll = (TextView) findViewById(R.id.rollValue);

        tv_x = (TextView) findViewById(R.id.xAxisValue);
        tv_y = (TextView) findViewById(R.id.yAxisValue);
        tv_z = (TextView) findViewById(R.id.zAxisValue);

        //we need the sensor manager and the gps manager, the
        //registration is all in onpause and onresume;
        mgr = (SensorManager) this.getSystemService(SENSOR_SERVICE);
        //orientation
        orient = mgr.getDefaultSensor(Sensor.TYPE_ORIENTATION);
        //acceleraometer
        accel = mgr.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        //gps location information
        myL = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        //setup the location listener.


        //all the camera preivew information
        inPreview = false;
        cameraPreview = (SurfaceView) findViewById(R.id.cameraPreview);
        previewHolder = cameraPreview.getHolder();
        previewHolder.addCallback(this);
        previewHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

    }

    @Override
    protected void onResume() {
        mgr.registerListener(this, accel, SensorManager.SENSOR_DELAY_NORMAL);
        mgr.registerListener(this, orient, SensorManager.SENSOR_DELAY_NORMAL);
        myL.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
        super.onResume();
        camera = Camera.open();
    }

    @Override
    protected void onPause() {
        mgr.unregisterListener(this, accel);
        mgr.unregisterListener(this, orient);
        myL.removeUpdates(this);
        super.onPause();
        camera.release();
        camera = null;
        inPreview = false;
    }

    //this are for the Sensor events of orientation and accelerometer
    @Override
    public void onSensorChanged(SensorEvent event) {

        if (event.sensor.getType() == Sensor.TYPE_ORIENTATION) {
            tv_head.setText(String.valueOf(event.values[0]));
            tv_pitch.setText(String.valueOf(event.values[1]));
            tv_roll.setText(String.valueOf(event.values[2]));
        } else if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            tv_x.setText(String.valueOf(event.values[0]));
            tv_y.setText(String.valueOf(event.values[1]));
            tv_z.setText(String.valueOf(event.values[2]));
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // ignoring!
    }

    //this are for the gps.
    @Override
    public void onLocationChanged(Location location) {
        //if we have location information, update the screen here. just lat and lot, others
        //are shown if you may need them.
        if (location != null) {
            tv_alt.setText("Altitude: " + (location.getAltitude() * 3.2808399) + " ft");  //1 meter is 3.2808399 feet
            tv_lat.setText("Latitude: " + location.getLatitude());
            tv_long.setText("Longitude: " + location.getLongitude());
        }
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        // ignoring!
    }

    @Override
    public void onProviderEnabled(String provider) {
        // ignoring!
    }

    @Override
    public void onProviderDisabled(String provider) {
        // ignoring!
    }


    //finally the following are for the Camera preview.
    private Camera.Size getBestPreviewSize(int width, int height, Camera.Parameters parameters) {
        Camera.Size result = null;

        for (Camera.Size size : parameters.getSupportedPreviewSizes()) {
            if (size.width <= width && size.height <= height) {
                if (result == null) {
                    result = size;
                } else {
                    int resultArea = result.width * result.height;
                    int newArea = size.width * size.height;

                    if (newArea > resultArea) {
                        result = size;
                    }
                }
            }
        }

        return (result);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        try {
            camera.setPreviewDisplay(previewHolder);
        } catch (Throwable t) {
            Log.e("Camera", "Exception in setPreviewDisplay()", t);
        }

    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        Camera.Parameters parameters = camera.getParameters();
        Camera.Size size = getBestPreviewSize(width, height, parameters);

        if (size != null) {
            parameters.setPreviewSize(size.width, size.height);
            camera.setParameters(parameters);
            camera.startPreview();
            inPreview = true;
        }

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        // TODO Auto-generated method stub

    }

}
