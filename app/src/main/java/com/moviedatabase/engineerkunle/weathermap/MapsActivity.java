package com.moviedatabase.engineerkunle.weathermap;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.Debug;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import zh.wang.android.apis.yweathergetter4a.WeatherInfo;
import zh.wang.android.apis.yweathergetter4a.YahooWeather;
import zh.wang.android.apis.yweathergetter4a.YahooWeatherExceptionListener;
import zh.wang.android.apis.yweathergetter4a.YahooWeatherInfoListener;


public class MapsActivity extends FragmentActivity implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener, YahooWeatherInfoListener, YahooWeatherExceptionListener {

    //app variables
    private GoogleMap mMap;

    public static final String TAG = MapsActivity.class.getSimpleName();


    private GoogleApiClient mGoogleApiClient;
    private LocationRequest mLocationRequest;

    private TextView tv;
    private TextView mediumText;


    private final static int CONNECTION_FAILURE_RESOLUTION_REQUEST = 9000;

    private YahooWeather yw = YahooWeather.getInstance(5000, 5000, true);

    //set background color
    RelativeLayout rLBackground;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        yw.setExceptionListener(this);
//        mMap.clear();
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();

        // Create the LocationRequest object
        mLocationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(10 * 1000)        // 10 seconds, in milliseconds
                .setFastestInterval(1 * 1000); // 1 second, in milliseconds

    }


    private void startMap() {
        // Do a null check to confirm that we have not already instantiated the map.

        if (mMap == null) {
            // Try to obtain the map from the SupportMapFragment.
            mMap = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map))
                    .getMap();
        }else{
            Toast.makeText(getApplicationContext(), "map is null", Toast.LENGTH_SHORT).show();
        }
    }


    private void handleNewLocation(Location location) {
        Log.d(TAG, location.toString());

        double currentLatitude = location.getLatitude();
        double currentLongitude = location.getLongitude();

        LatLng latLng = new LatLng(currentLatitude, currentLongitude);


        //mMap.addMarker(new MarkerOptions().position(new LatLng(currentLatitude, currentLongitude)).title("Current Location"));
        MarkerOptions options = new MarkerOptions()
                .position(latLng)
                .title("I am here!")
                .draggable(true); //here you can move marker
       // mMap.clear();
        mMap.addMarker(options);
        mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));

        //zoom in to add center effect
        mMap.animateCamera(CameraUpdateFactory.zoomTo(15));


        searchByLonLat(currentLatitude,currentLongitude);
        mMap.setOnMarkerDragListener(new GoogleMap.OnMarkerDragListener() {
            @Override
            public void onMarkerDragStart(Marker marker) {
            }

            @Override
            public void onMarkerDragEnd(Marker marker) {
                Log.d(TAG, "latitude : "+ marker.getPosition().latitude);

                searchByLonLat(marker.getPosition().latitude, marker.getPosition().longitude);
                mMap.animateCamera(CameraUpdateFactory.newLatLng(marker.getPosition()));


            }

            @Override
            public void onMarkerDrag(Marker marker) {
            }

        });

    }


    @Override
    public void onConnected(@Nullable Bundle bundle) {

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    Integer.parseInt(Manifest.permission.ACCESS_COARSE_LOCATION));
            // for ActivityCompat#requestPermissions for more details.
            Toast.makeText(getApplicationContext(), "make sure Access Fine Location request updated", Toast.LENGTH_SHORT).show();
        }else{

            Location location = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);

            if (location == null) {
                LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
            }
            else {
                handleNewLocation(location);
            }
        }
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onLocationChanged(Location location) {
        handleNewLocation(location);


        //Log.d(TAG, "OnLocationChanged - this has been called" );

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    protected void onResume() {
        super.onResume();

        startMap();
        mGoogleApiClient.connect();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
    }

    @Override
    public void gotWeatherInfo(WeatherInfo weatherInfo) {

        if(weatherInfo != null){

            tv = (TextView) findViewById(R.id.weatherInfo);
            rLBackground = (RelativeLayout)findViewById(R.id.backgroundSet);

            //rough estimate of weather
            int convertWeather = (weatherInfo.getCurrentTemp()-32)/2;

            tv.setText(getResources().getString(R.string.city)
                    +" "+ weatherInfo.getLocationCity()+ "\n" + " "
                    +getResources().getString(R.string.description)+" "
                    +weatherInfo.getConditionLat()
                    +", "
                    +  weatherInfo.getConditionLon() + ")"
                    + "\n" + getResources().getString(R.string.temperature)+" "
                    + convertWeather);

            //set background color
            if(convertWeather < 10){
                rLBackground.setBackgroundColor(getResources().getColor(R.color.coldBlue));
                tv.setTextColor(getResources().getColor(R.color.defaultWHite));

            }else if(convertWeather > 15){
                rLBackground.setBackgroundColor(getResources().getColor(R.color.sunnyyellow));
                tv.setTextColor(getResources().getColor(R.color.defaultWHite));

            }else{

                rLBackground.setBackgroundColor(getResources().getColor(R.color.defaultWHite));
                tv.setTextColor(getResources().getColor(R.color.colorAccent));
            }
        }

    }


    @Override
    public void onFailConnection(Exception e) {

    }

    @Override
    public void onFailParsing(Exception e) {

    }

    @Override
    public void onFailFindLocation(Exception e) {

    }


    private void searchByLonLat(Double lat, Double lng){
        yw.setNeedDownloadIcons(true);
        yw.setUnit(YahooWeather.UNIT.CELSIUS);
        yw.setSearchMode(YahooWeather.SEARCH_MODE.GPS);
        yw.queryYahooWeatherByLatLon(getApplicationContext(),lat,lng, MapsActivity.this);
    }
}
