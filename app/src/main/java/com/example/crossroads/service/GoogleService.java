package com.example.crossroads.service;


import android.Manifest;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.*;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import androidx.core.app.ActivityCompat;
import com.example.crossroads.retrofit_classes.Api;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.*;


public class GoogleService extends Service implements LocationListener {

    boolean isGPSEnable = false;
    boolean isNetworkEnable = false;
    double latitude, longitude;
    LocationManager locationManager;
    Location location;
    private Handler mHandler = new Handler();
    private Timer mTimer = null;
    long notify_interval = 1000;
    Retrofit retrofit;
    Api api;
    Geocoder geocoder;
    Map<Double, Double> coordinates;
    double critical_distance = 0.0003;
    public static String strReceiver = "servicetutorial.service.receiver";
    Intent intent;
    double current_distance,kilometers_distance = 0;
    boolean danger = false;

    public GoogleService() {

    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        retrofit = new Retrofit.Builder()
                .baseUrl("http://evening-dusk-59162.herokuapp.com") //Базовая часть адреса
                .addConverterFactory(GsonConverterFactory.create()) //Конвертер, необходимый для преобразования JSON'а в объекты
                .build();

        api = retrofit.create(Api.class);
        mTimer = new Timer();
        mTimer.schedule(new TimerTaskToGetLocation(), 5, notify_interval);
        intent = new Intent(strReceiver);
        callBackend();
        fn_getlocation();

    }

    @Override
    public void onLocationChanged(Location location) {

    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }

    private void fn_getlocation() {
        locationManager = (LocationManager) getApplicationContext().getSystemService(LOCATION_SERVICE);
        isGPSEnable = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        isNetworkEnable = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

        if (!isGPSEnable && !isNetworkEnable) {

        } else {

            if (isGPSEnable) {
                location = null;
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
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 0, this);
                if (locationManager!=null){
                    location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                    if (location!=null){
                        Log.e("latitude",location.getLatitude()+"");
                        Log.e("longitude",location.getLongitude()+"");
                        latitude = location.getLatitude();
                        longitude = location.getLongitude();
                        fnUpdate(location);

//                        try {
//                            List<Address> addresses = geocoder.getFromLocation(latitude, longitude, 1);
//                            String cityName = addresses.get(0).getLocality();
//
//                        } catch (IOException e) {
//                            e.printStackTrace();
//                        }
                        if (coordinates!=null) {
                            if (isDanger(latitude, longitude)) {
                                danger = true;
                            } else {
                                danger = false;
//                                Log.e("distance", String.valueOf(current_distance));
                            }
                        }

                    }
                }
            }


        }

    }

    private class TimerTaskToGetLocation extends TimerTask{
        @Override
        public void run() {

            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    fn_getlocation();
                }
            });

        }
    }

    String regionName = "awd";
    private void callBackend() {


        JsonObject obj = new JsonObject();
        obj.addProperty("crc","aas22");



        Call<JsonObject> call = api.getCoordinatesArrayFromServer();

        call.enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (response.body() != null) {
                    JsonObject jsonObj = response.body().getAsJsonObject();
                    Type mapType = new TypeToken<Map<Double, Double >>(){}.getType();
                    coordinates = new Gson().fromJson(jsonObj, mapType);
                    saveCordinates(jsonObj.getAsString());


                    Log.e("RESPONSE", String.valueOf(jsonObj));
                } else {
                    Log.e("NULL", "NULL");
                }
            }
            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {

                Log.e("TAG", "fail");
            }
        });


    }

    private void fnUpdate(Location location){

        intent.putExtra("latitude",location.getLatitude());
        intent.putExtra("longitude",location.getLongitude());
        intent.putExtra("distance", kilometers_distance);
        intent.putExtra("danger",danger);
        sendBroadcast(intent);
    }

    private boolean isDanger(double latitude, double longitude) {
        ArrayList<Double> distances = new ArrayList<>();
        for (Map.Entry<Double, Double> entry : coordinates.entrySet()) {
            getDistance(latitude, longitude, entry.getValue(), entry.getKey());
            distances.add(current_distance);
            kilometers_distance = degreeToKilometers(latitude, longitude,entry.getValue(), entry.getKey());
            if (current_distance<critical_distance) {
                Log.e("return", "true");
                Log.e("minimal", String.valueOf(Collections.min(distances)));

                return true;

            }

        }
        Log.e("miminimal distance is", String.valueOf(distances.indexOf(Collections.min(distances))));
        kilometers_distance = current_distance;
        return false;
    }
    private void getDistance(double latitude1, double longitude1, double latitude2, double longitude2) {

        current_distance = Math.hypot(latitude1-latitude2, longitude1-longitude2);
    }


    private double degreeToKilometers(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371; // Radious of the earth

        double latDistance = toRad(lat2-lat1);
        double lonDistance = toRad(lon2-lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2) +
                Math.cos(toRad(lat1)) * Math.cos(toRad(lat2)) *
                        Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
        return R * c;
    }

    private static Double toRad(Double value) {
        return value * Math.PI / 180;
    }

    private void saveCordinates(String json) {
        SharedPreferences prefs = getApplicationContext().getSharedPreferences(
                "Coordinates", Context.MODE_PRIVATE);
        SharedPreferences.Editor prefsEditor = prefs.edit();
        prefsEditor.putString("myStringSet", json);
        prefsEditor.apply();
    }




}
