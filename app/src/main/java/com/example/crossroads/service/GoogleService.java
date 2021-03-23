package com.example.crossroads.service;


import android.Manifest;
import android.app.*;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.*;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.*;
import android.util.Log;
import android.widget.RemoteViews;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import com.example.crossroads.MainActivity;
import com.example.crossroads.R;
import com.example.crossroads.retrofit_classes.Api;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;


import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import java.lang.reflect.Type;
import java.util.*;



public class GoogleService extends Service implements LocationListener {

    HashMap<Double, Double> coordinates = new HashMap<>();
    NotificationManagerCompat notificationManager;
    boolean isGPSEnable = false;
    boolean isNetworkEnable = false;
    double latitude, longitude;
    LocationManager locationManager;
    Retrofit retrofit;
    Api api;
    String city;
    int timeBetweenNotifications = 180000;
    double critical_distance = 0.0003;
    public String actionName = "SendCoordinatesToActivity";
    Intent intent;
    Bundle bundle;
    double currentDistance = 0;
    boolean danger = false;
    long previousNotificationTime;
    NotificationCompat.Builder builder;

    public GoogleService() {

    }



    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Intent mainActivityIntent = new Intent(getApplicationContext(), MainActivity.class);
        PendingIntent notificationIntent = PendingIntent.getActivity(getApplicationContext(),0, mainActivityIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        Notification notification = new NotificationCompat.Builder(this, "Crossroads channel")
                .setSmallIcon(R.drawable.cast_ic_expanded_controller_stop)
                .setContentTitle("Сервис работает")
                .setContentIntent(notificationIntent)
                .build();
        startForeground(1, notification);
        return super.onStartCommand(intent, flags, startId);
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

        intent = new Intent(actionName);
        bundle = new Bundle();
        city = getCurrentCity();
        coordinates = getCoordinates(city);
        locationManager = (LocationManager) getApplicationContext().getSystemService(LOCATION_SERVICE);
        isGPSEnable = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        isNetworkEnable = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        if (androidx.core.content.ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && androidx.core.content.ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 2000, 1, this);
        Intent mainActivityIntent = new Intent(getApplicationContext(), MainActivity.class);
        PendingIntent notificationIntent = PendingIntent.getActivity(getApplicationContext(),0, mainActivityIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        Notification notification = new NotificationCompat.Builder(this, "Crossroads channel")
                .setSmallIcon(R.drawable.cast_ic_expanded_controller_stop)
                .setContentTitle("Сервис работает")
                .setContentIntent(notificationIntent)
                .build();
        startForeground(1, notification);
    }

    private void settingUpNotifications() {
        String CHANNEL_ID = "Crossroads channel";
        RemoteViews notificationLayout = new RemoteViews(getPackageName(), R.layout.notification_layout);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, CHANNEL_ID, importance);
            channel.setDescription("Уведомления о том, что вы рядом с перекрестком");
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            channel.setVibrationPattern(new long[]{500, 400});
            channel.enableVibration(false);
            channel.enableLights(true);
            notificationManager = NotificationManagerCompat.from(this);
            notificationManager.createNotificationChannel(channel);
        }
        Uri alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.cast_ic_expanded_controller_stop)
                .setContentTitle("Осторожно!")
                .setContentText("Вы рядом с перекрестком")
                .setVibrate(new long[]{500, 400})
                .setSound(alarmSound)
                .setContent(notificationLayout)
                .setPriority(NotificationCompat.PRIORITY_MAX);

    }

    public String getCurrentCity() {
        SharedPreferences sharedPreferences = getSharedPreferences(getString(R.string.SharedPreferencesStoreName), MODE_PRIVATE);
        return sharedPreferences.getString(getString(R.string.currentCityPreferenceName), "minsk");
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
            updateActivity(location);


        }
    }

    private void updateActivity(Location location) {
        bundle.putDouble("latitude", location.getLatitude());
        bundle.putDouble("longitude", location.getLongitude());
        bundle.putSerializable("coordinates",coordinates);
        bundle.putBoolean("danger", danger);
        intent.putExtras(bundle);
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);

    }


    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }


    private HashMap<Double, Double> getCoordinates(String city) {
        SharedPreferences sharedPreferences = getSharedPreferences(getString(R.string.SharedPreferencesStoreName), MODE_PRIVATE);
        Type mapType = new TypeToken<Map<Double, Double >>(){}.getType();
        String jsonCoordinates = sharedPreferences.getString(city,"{0.1:0.1}");
            if (jsonCoordinates!=null) {
                return new Gson().fromJson(jsonCoordinates, mapType);
            }
        return null;
    }

    private boolean isDanger(double latitude, double longitude) {
        for (Map.Entry<Double, Double> entry : coordinates.entrySet()) {
            getDistance(latitude, longitude, entry.getValue(), entry.getKey());
            if (currentDistance <critical_distance) {
                Log.i("return", "true");
                checkTimeAndNotify();
                return true;

            }

        }
        return false;
    }

    private void getDistance(double latitude1, double longitude1, double latitude2, double longitude2) {
        currentDistance = Math.hypot(latitude1-latitude2, longitude1-longitude2);
    }

    private void checkTimeAndNotify() {
        long curentTime = System.currentTimeMillis();
        if ((curentTime-previousNotificationTime)> timeBetweenNotifications) {
            previousNotificationTime = curentTime;
            dangerNotify();
        }
    }

    private void dangerNotify() {
        Vibrator v = (Vibrator) getApplicationContext().getSystemService(Context.VIBRATOR_SERVICE);
        // Vibrate for 2000 milliseconds
        v.vibrate(VibrationEffect.createOneShot(2000, VibrationEffect.DEFAULT_AMPLITUDE));

        Uri alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        Ringtone r = RingtoneManager.getRingtone(getApplicationContext(), alarmSound);
        r.play();
        notificationManager = NotificationManagerCompat.from(this);
        Log.i("Notification","NOW");
        notificationManager.notify(2, builder.build());



    }

    @Override
    public void onDestroy() {
        Log.d("SERVICE", "onDestroy");
        notificationManager.cancel(1);
        notificationManager.cancel(2);
        locationManager.removeUpdates(this);
    }
}
