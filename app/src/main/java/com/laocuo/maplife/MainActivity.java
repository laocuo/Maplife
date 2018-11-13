package com.laocuo.maplife;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
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

import java.util.List;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {
    private final String TAG = "maplife";
    private Context mContext;

    private MapView mapView;
    private MapboxMap mapboxMap;
    private BuildingPlugin buildingPlugin;

    private LocationManager mLocationManager;
    private LocationListener mLocationListener;

    private final String MAP_KEY = "pk.eyJ1IjoibGFvY3VvIiwiYSI6ImNqa3RnZzV0dzA1MjAzdmxrbXM1eDhma2cifQ.woU4FxsUqUVOQAmsQWhS5w";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = this;
        Mapbox.getInstance(this, MAP_KEY);
        setContentView(R.layout.activity_main);
        mapView = (MapView) findViewById(R.id.mapView);
        mapView.onCreate(savedInstanceState);
        Log.d(TAG, "getMapAsync");
        mapView.getMapAsync(this);

        mLocationManager = (LocationManager) mContext.getSystemService(Context.LOCATION_SERVICE);
        mLocationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                Log.d(TAG, "onLocationChanged");
                mapboxMap.animateCamera(CameraUpdateFactory.newLatLng(new LatLng(location)), 4000);
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
    }

    @SuppressLint("MissingPermission")
    private void setCurrentPosition() {
        String provider = judgeProvider();
        Log.d(TAG, "provider = " + provider);
        Location l = mLocationManager.getLastKnownLocation(provider);
        if (l != null) {
            Log.d(TAG, "getLastKnownLocation");
            mapboxMap.animateCamera(CameraUpdateFactory.newLatLng(new LatLng(l)), 4000);
        } else {
            mLocationManager.requestLocationUpdates(provider, 20000, 1, mLocationListener);
        }
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
    public void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        mapView.onResume();
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
    }
}
