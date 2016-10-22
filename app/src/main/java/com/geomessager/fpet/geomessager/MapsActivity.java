package com.geomessager.fpet.geomessager;


import android.Manifest;

import android.content.pm.PackageManager;
import android.location.Location;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.geomessager.fpet.geomessager.mqtt.MyMqttClient;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;


import java.text.DateFormat;
import java.util.Date;


public class MapsActivity extends AppCompatActivity
        implements
        ActivityCompat.OnRequestPermissionsResultCallback,
        ConnectionCallbacks,
        OnConnectionFailedListener,
        LocationListener{

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;

    private boolean mPermissionDenied = false;

    private GoogleApiClient mGoogleApiClient;
    protected LocationRequest mLocationRequest;

    protected Location mCurrentLocation;

    protected String mLastUpdateTime;

    protected MyMqttClient mqttClient;

    private Button button;

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.event_activity);

        buildGoogleApiClient();
        createLocationRequest();

        mqttClient = new MyMqttClient(this);

        button = (Button)findViewById(R.id.start_button);
    }

    @Override
    protected void onStart(){
        super.onStart();
        mGoogleApiClient.connect();
        mqttClient.Connect();
    }

    @Override
    protected void onStop(){
        super.onStop();
        if(mGoogleApiClient.isConnected()){
            mGoogleApiClient.disconnect();
        }
        if(mqttClient.isConnected()){
            mqttClient.Disconnect();
        }
    }

    protected synchronized void buildGoogleApiClient(){
        if(mGoogleApiClient == null){
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
        }
    }

    protected void createLocationRequest(){
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(10000);
        mLocationRequest.setFastestInterval(5000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    @Override
    public void onConnected(Bundle connectionHint){
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            if(mCurrentLocation == null) {
                mCurrentLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
                mLastUpdateTime = DateFormat.getTimeInstance().format(new Date());
            }
        }

        if(mCurrentLocation != null){
            //Print shit about location
            Log.d("Location", "Last Location: " + mCurrentLocation.toString());
        }
        else{
            Log.d("Location", "No known location");
        }
        startLocationUpdates();
    }

    @Override
    public void onConnectionFailed(ConnectionResult result){
      Log.i("Connection", "Connection Failed: ConnectionResult.getErrorCode() = " + result.getErrorCode());
    }

    @Override
    public void onConnectionSuspended(int cause){
        Log.i("Connection", "Connection Suspended");
        mGoogleApiClient.connect();
    }

    protected void startLocationUpdates(){
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
        }
    }

    @Override
    public void onLocationChanged(Location location){
        mCurrentLocation = location;
        mLastUpdateTime = DateFormat.getTimeInstance().format(new Date());
        Log.d("Location", "Current Time: " + mLastUpdateTime + "Current Location: " + mCurrentLocation.toString());

        mqttClient.Publish("fpet/coords", "Location: " + location.toString() + "Timestamp: " + mLastUpdateTime.toString());
    }

    private void enableMyLocation(){
        if(ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED){
            PermissionUtils.requestPermission(this, LOCATION_PERMISSION_REQUEST_CODE,
                    android.Manifest.permission.ACCESS_FINE_LOCATION, true);
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults){
        if(requestCode != LOCATION_PERMISSION_REQUEST_CODE){
            return;
        }

        if(PermissionUtils.isPermissionGranted(permissions, grantResults, android.Manifest.permission.ACCESS_FINE_LOCATION)){
            enableMyLocation();
        } else {
            mPermissionDenied = true;
        }
    }

    @Override
    protected void onResumeFragments(){
        super.onResumeFragments();
        if(mPermissionDenied){
            showMissingPermissionError();
            mPermissionDenied = false;
        }
    }

    private void showMissingPermissionError(){
        PermissionUtils.PermissionDeniedDialog.newInstance(true).show(getSupportFragmentManager(), "dialog");
    }

    public void startButtonOnClickHandler(View view){
        if(mqttClient.isConnected()){
            mqttClient.Subscribe("fpet/coords");
        }
    }


}
