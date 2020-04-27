package com.laocuo.maplife;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.mapbox.android.core.permissions.PermissionsManager;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.camera.CameraPosition;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.mapboxsdk.plugins.building.BuildingPlugin;
import com.mapbox.mapboxsdk.style.layers.LineLayer;
import com.mapbox.mapboxsdk.style.layers.PropertyFactory;
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {
    private final String TAG = "maplife";
    private Context mContext;

    private TextView lontitude;
    private TextView latitude;
    private TextView orientation;

    private MapView mapView;
    private MapboxMap mapboxMap;
    private BuildingPlugin buildingPlugin;

    private LocationManager mLocationManager;
    private LocationListener mLocationListener;

    private SensorManager mSensorManager;
    private SensorEventListener mSensorEventListener;
    private Sensor aSensor = null;
    private Sensor mSensor = null;

    float[] accelerometerValues = new float[3];
    float[] magneticFieldValues = new float[3];
    float[] values = new float[3];
    float[] RR = new float[9];

    private boolean isPermissionOk;

    private String provider;

    private boolean isListening;

    private final String MAP_KEY = "pk.eyJ1IjoibGFvY3VvIiwiYSI6ImNqa3RnZzV0dzA1MjAzdmxrbXM1eDhma2cifQ.woU4FxsUqUVOQAmsQWhS5w";

    private LatLng latlng = new LatLng(31.20027131, 121.66745533);

    private double bearing = 0;

    private double zoom = 15;

    private double tilt = 45;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = this;
        Mapbox.getInstance(this, MAP_KEY);
        setContentView(R.layout.activity_main);
        lontitude = findViewById(R.id.lontitude);
        latitude = findViewById(R.id.latitude);
        orientation = findViewById(R.id.orientation);
        mapView = findViewById(R.id.mapView);
        mapView.onCreate(savedInstanceState);
        Log.d(TAG, "getMapAsync");
        mapView.getMapAsync(this);

        mLocationManager = (LocationManager) mContext.getSystemService(Context.LOCATION_SERVICE);
        mLocationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                Log.d(TAG, "onLocationChanged");
                lontitude.setText("经度:" + location.getLongitude());
                latitude.setText("纬度:" + location.getLatitude());
                changeCamera(new LatLng(location));
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

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        aSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        mSensorEventListener = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent event) {
                // TODO Auto-generated method stub
//                Log.d(TAG, "onSensorChanged");
                if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                    accelerometerValues = event.values;
                }
                if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
                    magneticFieldValues = event.values;
                }
                //调用getRotaionMatrix获得变换矩阵R[]
                SensorManager.getRotationMatrix(RR, null, accelerometerValues, magneticFieldValues);
                //经过SensorManager.getOrientation(R, values);得到的values值为弧度
                SensorManager.getOrientation(RR, values);
                //转换为角度
                values[0] = (float) Math.toDegrees(values[0]);
                orientation.setText("角度:" + values[0]);
                changeCamera(values[0]);
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {

            }
        };
    }

    public void changeCamera(double bearing) {
        this.bearing = bearing;
        changeCamera();
    }

    public void changeCamera(LatLng target) {
        latlng.setLongitude(target.getLongitude());
        latlng.setLatitude(target.getLatitude());
        changeCamera();
    }

    private void changeCamera() {
        changeCamera(latlng, zoom, bearing, tilt, 2000);
    }

    /**
     * 切换camera视角
     *
     * @return
     */
    private void changeCamera(LatLng target, double zoom, double bearing, double tilt, int during) {
        CameraPosition cameraPosition = new CameraPosition.Builder()
                .target(target)
                .zoom(zoom)//放大尺度 从0开始，0即最大比例尺，最大未知，17左右即为街道层级
                .bearing(bearing)//地图旋转，但并不是每次点击都旋转180度，而是相对于正方向180度，即如果已经为相对正方向180度了，就不会进行旋转
                .tilt(tilt)//地图倾斜角度，同上，相对于初始状态（平面）成30度
                .build();//创建CameraPosition对象
        mapboxMap.easeCamera(CameraUpdateFactory.newCameraPosition(cameraPosition), during);
    }

    @SuppressLint("MissingPermission")
    private void setCurrentPosition() {
        isPermissionOk = true;
        listen(true);
    }

    @SuppressLint("MissingPermission")
    private void listen(boolean listen) {
        if (!isPermissionOk) return;
        if (isListening == listen) return;
        if (provider == null) {
            provider = judgeProvider();
            Log.d(TAG, "provider = " + provider);
            if (provider == null) {
                return;
            }
        }
        if (listen) {
            mLocationManager.requestLocationUpdates(provider, 1000, 0.1f, mLocationListener);
            mSensorManager.registerListener(mSensorEventListener, aSensor, SensorManager.SENSOR_DELAY_NORMAL);
            mSensorManager.registerListener(mSensorEventListener, mSensor, SensorManager.SENSOR_DELAY_NORMAL);
        } else {
            mSensorManager.unregisterListener(mSensorEventListener);
            mLocationManager.removeUpdates(mLocationListener);
        }
        isListening = listen;
        Log.d(TAG, "isListening = " + isListening);
    }

    private String judgeProvider() {
        List<String> prodiverlist = mLocationManager.getProviders(true);
        if (prodiverlist.contains(LocationManager.NETWORK_PROVIDER)) {
            return LocationManager.NETWORK_PROVIDER;//网络定位
        } else if (prodiverlist.contains(LocationManager.GPS_PROVIDER)) {
            return LocationManager.GPS_PROVIDER;//GPS定位
        } else {
            Toast.makeText(mContext, "没有可用的位置提供器", Toast.LENGTH_SHORT).show();
        }
        return null;
    }

    @Override
    public void onStart() {
        super.onStart();
        mapView.onStart();
    }

    @Override
    public void onResume() {
        super.onResume();
        mapView.onResume();
        Log.d(TAG, "onResume");
        listen(true);
    }

    @Override
    public void onPause() {
        super.onPause();
        mapView.onPause();
        Log.d(TAG, "onPause");
        listen(false);
    }

    @Override
    public void onStop() {
        super.onStop();
        mapView.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mLocationManager.removeUpdates(mLocationListener);
        mapView.onDestroy();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == 1 && permissions[0].equalsIgnoreCase(Manifest.permission.ACCESS_FINE_LOCATION)) {
            int result = grantResults[0];
            if (result == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(mContext, "权限授予", Toast.LENGTH_SHORT).show();
                setCurrentPosition();
            }
        }
    }

    @Override
    public void onMapReady(MapboxMap mapboxMap) {
        Log.d(TAG, "onMapReady");
        MainActivity.this.mapboxMap = mapboxMap;
        buildingPlugin = new BuildingPlugin(mapView, mapboxMap);
        buildingPlugin.setVisibility(true);
        if (false == PermissionsManager.areLocationPermissionsGranted(this)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION}, 1);
            }
        } else {
            setCurrentPosition();
        }
        addField();
    }

    private void addField() {
        try {
            GeoJsonSource geoJsonSource = new GeoJsonSource("geojson-source", loadJsonFromAsset("yihui_fieldmodel.geojson"));
            mapboxMap.addSource(geoJsonSource);
            //添加图层
            LineLayer lineLayer = new LineLayer("linelayer", "geojson-source");
            lineLayer.setProperties(
                    PropertyFactory.lineWidth(1f),
                    PropertyFactory.lineColor(Color.parseColor("#ffda6600"))
            );
            mapboxMap.addLayer(lineLayer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 加载GeoJSON数据源
     */
    private String loadJsonFromAsset(String nameOfLocalFile) throws IOException {
        InputStream is = getResources().getAssets().open(nameOfLocalFile);
        int size = is.available();
        byte[] buffer = new byte[size];
        is.read(buffer);
        is.close();
        return new String(buffer, "UTF-8");
    }
}
