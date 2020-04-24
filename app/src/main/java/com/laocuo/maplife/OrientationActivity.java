package com.laocuo.maplife;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;


import java.util.List;

public class OrientationActivity extends AppCompatActivity {

    private final String TAG = "OrientationActivity";

    /**
     * Called when the activity is first created.
     */
    TextView textview = null;
    TextView lontitude;
    TextView latitude;
    private SensorManager sm = null;
    private Sensor aSensor = null;
    private Sensor mSensor = null;

    float[] accelerometerValues = new float[3];
    float[] magneticFieldValues = new float[3];
    float[] values = new float[3];
    float[] RR = new float[9];

    private LocationManager mLocationManager;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_orientation);
        textview = (TextView) findViewById(R.id.orientation);
        lontitude = findViewById(R.id.lontitude);
        latitude = findViewById(R.id.latitude);
        sm = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        aSensor = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mSensor = sm.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
    }

    private String judgeProvider() {
        List<String> prodiverlist = mLocationManager.getProviders(true);
        if (prodiverlist.contains(LocationManager.NETWORK_PROVIDER)) {
            return LocationManager.NETWORK_PROVIDER;//网络定位
        } else if (prodiverlist.contains(LocationManager.GPS_PROVIDER)) {
            return LocationManager.GPS_PROVIDER;//GPS定位
        } else {
            Toast.makeText(OrientationActivity.this, "没有可用的位置提供器", Toast.LENGTH_SHORT).show();
        }
        return null;
    }

    @Override
    protected void onResume() {
        super.onResume();
        sm.registerListener(myListener, aSensor, SensorManager.SENSOR_DELAY_UI);
        sm.registerListener(myListener, mSensor, SensorManager.SENSOR_DELAY_UI);
        String provider = judgeProvider();
        Log.d(TAG, "provider = " + provider);
        if (provider == null) return;
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        mLocationManager.requestLocationUpdates(provider, 20000, 1, mLocationListener);
    }

    @Override
    //注意activity暂停的时候释放
    protected void onPause() {
        // TODO Auto-generated method stub
        super.onPause();
        sm.unregisterListener(myListener);
        mLocationManager.removeUpdates(mLocationListener);
    }

    final LocationListener mLocationListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            lontitude.setText("经度:" + location.getLongitude());
            latitude.setText("纬度:" + location.getLatitude());
        }

        @Override
        public void onStatusChanged(String s, int i, Bundle bundle) {

        }

        @Override
        public void onProviderEnabled(String s) {

        }

        @Override
        public void onProviderDisabled(String s) {

        }
    };

    final SensorEventListener myListener = new SensorEventListener() {

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
            // TODO Auto-generated method stub

        }

        @Override
        public void onSensorChanged(SensorEvent event) {
            // TODO Auto-generated method stub
            if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                accelerometerValues = event.values;
            }
            if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
                magneticFieldValues = event.values;
            }
            //调用getRotaionMatrix获得变换矩阵R[]
            SensorManager.getRotationMatrix(RR, null, accelerometerValues, magneticFieldValues);
            SensorManager.getOrientation(RR, values);
            //经过SensorManager.getOrientation(R, values);得到的values值为弧度
            //转换为角度
            values[0] = (float) Math.toDegrees(values[0]);
            textview.setText("角度:" + values[0]);
        }
    };
}