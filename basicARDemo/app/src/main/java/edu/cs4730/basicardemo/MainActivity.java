package edu.cs4730.basicardemo;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;

import androidx.core.app.ActivityCompat;

import androidx.appcompat.app.AppCompatActivity;

import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.Window;
import android.widget.FrameLayout;
import android.widget.TextView;

/**
 * A very simple/basic AR example.
 * <p>
 * It turns on the camera preview and displays the sensor for Orientation, accelerometer and gps location over the camera preview.
 * <p>
 * This code is based on code from
 * https://github.com/JimSeker/sensors
 * https://github.com/JimSeker/gps
 * https://github.com/JimSeker/video  CameraPreview demos.  for the camera preview.
 * <p>
 * Yes, some many things are deprecated in 22... orientation... I know.
 * <p>
 * NOTE: the first time, the camera is blank after getting permissions.  I didn't peruse a fix.
 */


public class MainActivity extends AppCompatActivity implements SensorEventListener, LocationListener {

    static public String TAG = "MainActivity";


    TextView tv_alt, tv_lat, tv_long; //gps
    TextView tv_head, tv_pitch, tv_roll; //orientation
    TextView tv_x, tv_y, tv_z;  //ACCELEROMETER

    //sensor and gps
    public static final int REQUEST_FINE_ACCESS = 0;
    private LocationManager myL;
    private SensorManager mgr;
    private Sensor accel, orient;

    //camera and preview
    Camera2Preview mPreview;
    FrameLayout preview;

    boolean inPreview;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //display setup stuff
        tv_alt = findViewById(R.id.altitudeValue);
        tv_lat = findViewById(R.id.latitudeValue);
        tv_long = findViewById(R.id.longitudeValue);

        tv_head = findViewById(R.id.headingValue);
        tv_pitch = findViewById(R.id.pitchValue);
        tv_roll = findViewById(R.id.rollValue);

        tv_x = findViewById(R.id.xAxisValue);
        tv_y = findViewById(R.id.yAxisValue);
        tv_z = findViewById(R.id.zAxisValue);

        //we need the sensor manager and the gps manager, the
        //registration is all in onpause and onresume;
        mgr = (SensorManager) this.getSystemService(SENSOR_SERVICE);
        //orientation
        orient = mgr.getDefaultSensor(Sensor.TYPE_ORIENTATION);
        //accelerometer
        accel = mgr.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        //gps location information
        myL = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        //setup the location listener.

        preview = findViewById(R.id.cameraPreview);

    }

    @Override
    protected void onResume() {
        super.onResume();
        startDemo();
    }

    @Override
    protected void onPause() {
        mgr.unregisterListener(this, accel);
        mgr.unregisterListener(this, orient);
        myL.removeUpdates(this);
        super.onPause();
        inPreview = false;
    }

    public void startDemo() {
        if ((ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) ||
            (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED)) {
            //I'm on not explaining why, just asking for permission.
            Log.v(TAG, "asking for permissions");
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.CAMERA},
                MainActivity.REQUEST_FINE_ACCESS);

        } else {
            //gps
            mgr.registerListener(this, accel, SensorManager.SENSOR_DELAY_NORMAL);
            mgr.registerListener(this, orient, SensorManager.SENSOR_DELAY_NORMAL);
            myL.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
            //camera
            //we have to pass the camera id that we want to use to the surfaceview
            CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);

            try {
                String cameraId = manager.getCameraIdList()[0];
                CameraCharacteristics cc = manager.getCameraCharacteristics(cameraId);
                int[] map = cc.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES);
                //its 3 on a pixel and I can't find what that actually means....
                Log.e("CameraDepth value", "Value is " + map[CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_DEPTH_OUTPUT]);
                mPreview = new Camera2Preview(this, cameraId);
                preview.addView(mPreview);

            } catch (CameraAccessException e) {
                Log.v(TAG, "Failed to get a camera ID!");
                e.printStackTrace();
            }

        }
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


    /**
     * Callback received when a permissions request has been completed.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        Log.v(TAG, "onRequest result called.");
        switch (requestCode) {
            case REQUEST_FINE_ACCESS:
                //received result for GPS access
                Log.v(TAG, "Received response for gps permission request.");
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted
                    Log.v(TAG, permissions[0] + " permission has now been granted. Showing preview.");
                    startDemo();  //call the method again, so the gps demo will start up.
                } else {
                    // permission denied,    Disable this feature or close the app.
                    Log.v(TAG, "GPS permission was NOT granted.");
                }

                return;
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

}
