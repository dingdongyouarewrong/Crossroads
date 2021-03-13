package com.example.crossroads.service;


import android.Manifest;
import android.app.*;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.*;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.app.JobIntentService;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import com.example.crossroads.R;
import com.example.crossroads.retrofit_classes.Api;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;


import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import java.lang.reflect.Type;
import java.util.*;


public class GoogleService extends JobIntentService implements LocationListener {

    Map<Double, Double> coordinates = new HashMap<>();

    boolean isGPSEnable = false;
    boolean isNetworkEnable = false;
    double latitude, longitude;
    LocationManager locationManager;
    Location location;
    private Handler mHandler = new Handler();
    long timerNotifyInterval = 2000;
    Retrofit retrofit;
    Api api;
    String city;
    int timeBetweenNotifications = 300000;
    double critical_distance = 0.0003;
    public static String strReceiver = "crossroads.service.receiver";
    Intent intent;
    double currentDistance, kilometersDistance = 0;
    boolean danger = false;
    private static String CHANNEL_ID = "Crossroads channel";
    long previousNotificationTime;
    NotificationCompat.Builder builder;

    public GoogleService() {

    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    protected void onHandleWork(@NonNull @org.jetbrains.annotations.NotNull Intent intent) {

    }


    @Override
    public void onCreate() {
        super.onCreate();
        retrofit = new Retrofit.Builder()
                .baseUrl("http://evening-dusk-59162.herokuapp.com") //Базовая часть адреса
                .addConverterFactory(GsonConverterFactory.create()) //Конвертер, необходимый для преобразования JSON'а в объекты
                .build();
        settingUpNotifications();

        previousNotificationTime = 0;


        api = retrofit.create(Api.class);
//        Timer mTimer = new Timer();
//        TimerTaskToGetLocation timerTask = new TimerTaskToGetLocation();
//
//        mTimer.schedule(timerTask, 5, timerNotifyInterval);
        intent = new Intent(strReceiver);
        city = getCurrentCity();
        coordinates = getCoordinates(city);
//        getLocation();
        locationManager = (LocationManager) getApplicationContext().getSystemService(LOCATION_SERVICE);
        isGPSEnable = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        isNetworkEnable = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            return;
        }
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 2000, 0, this);
//

    }

    private void settingUpNotifications() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, CHANNEL_ID, importance);
            channel.setDescription("Уведомления о том, что вы рядом с перекрестком");
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }

        builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.cast_ic_expanded_controller_stop)
                .setContentTitle("Осторожно!")
                .setContentText("Вы рядом с перекрестком")
                .setPriority(NotificationCompat.PRIORITY_MAX);
    }

    public String getCurrentCity() {
        SharedPreferences sharedPreferences = getSharedPreferences("Crossroads", MODE_PRIVATE);
        return sharedPreferences.getString("current_city", "Minsk");
    }

    @Override
    public void onLocationChanged(final Location location) {
        if (location!=null){
            Log.i("latitude",location.getLatitude()+"");
            Log.i("longitude",location.getLongitude()+"");
            latitude = location.getLatitude();
            longitude = location.getLongitude();
                if (coordinates!=null) {
                if (isDanger(latitude, longitude)) {
                    danger = true;
                } else {
                    danger = false;
                }

            }
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    fnUpdate(location);
                }
            });


        }
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


    private Map<Double, Double> getCoordinates(String city) {
        SharedPreferences sharedPreferences = getSharedPreferences("Crossroads", MODE_PRIVATE);
        Type mapType = new TypeToken<Map<Double, Double >>(){}.getType();
        String jsonCoordinates = sharedPreferences.getString(city,"{0.1:0.1}");
            if (jsonCoordinates!=null) {
                return new Gson().fromJson(jsonCoordinates, mapType);
            }
        return null;
    }

    private void fnUpdate(Location location){

        intent.putExtra("latitude",location.getLatitude());
        intent.putExtra("longitude",location.getLongitude());
        intent.putExtra("distance", kilometersDistance);
        intent.putExtra("danger",danger);
        sendBroadcast(intent);
    }

    private boolean isDanger(double latitude, double longitude) {
        ArrayList<Double> distances = new ArrayList<>();
        for (Map.Entry<Double, Double> entry : coordinates.entrySet()) {
            getDistance(latitude, longitude, entry.getValue(), entry.getKey());
            distances.add(currentDistance);
            kilometersDistance = degreeToKilometers(latitude, longitude,entry.getValue(), entry.getKey());
            if (currentDistance <critical_distance) {
                Log.i("return", "true");
                Log.i("minimal", String.valueOf(Collections.min(distances)));
                checkTimeAndNotify();
                return true;

            }

        }
        Log.i("miminimal distance is", String.valueOf(distances.indexOf(Collections.min(distances))));
        kilometersDistance = currentDistance;
        return false;
    }

    private void getDistance(double latitude1, double longitude1, double latitude2, double longitude2) {
        currentDistance = Math.hypot(latitude1-latitude2, longitude1-longitude2);
    }


    private double degreeToKilometers(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371;
        // Radious of the earth
        double latDistance = toRad(lat2-lat1);
        double lonDistance = toRad(lon2-lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2) +
                Math.cos(toRad(lat1)) * Math.cos(toRad(lat2)) *
                        Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
        return R * c;
    }

    private void checkTimeAndNotify() {
        long curentTime = System.currentTimeMillis();
        if ((curentTime-previousNotificationTime)> timeBetweenNotifications) {
            previousNotificationTime = curentTime;
            dangerNotify();
        }
    }

    private void dangerNotify() {
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        Log.i("Notification","NOW");
        notificationManager.notify(1, builder.build());



    }

    private static Double toRad(Double value) {
        return value * Math.PI / 180;
    }

    @Override
    public void onDestroy() {
        Log.d("SERVICE", "onDestroy");
        locationManager.removeUpdates(this);
    }
}
