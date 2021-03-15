package com.example.crossroads;


import android.Manifest;
import android.app.*;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Paint;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.preference.PreferenceManager;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import com.example.crossroads.retrofit_classes.Api;
import com.example.crossroads.service.GoogleService;
import org.osmdroid.api.IGeoPoint;
import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;
import org.osmdroid.views.overlay.simplefastpoint.LabelledGeoPoint;
import org.osmdroid.views.overlay.simplefastpoint.SimpleFastPointOverlay;
import org.osmdroid.views.overlay.simplefastpoint.SimpleFastPointOverlayOptions;
import org.osmdroid.views.overlay.simplefastpoint.SimplePointTheme;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import java.util.*;

import static android.Manifest.permission.*;

public class MainActivity extends Activity {
    private static final int REQUEST_PERMISSIONS = 100;
    boolean boolean_permission;
    TextView tv_latitude, tv_longitude, distanceTextView, dangerTextView,tv_locality;
    SharedPreferences mPref;
    SharedPreferences.Editor medit;
    Double latitude,longitude;
    Geocoder geocoder;
    Retrofit retrofit;
    Api api;
    private MapView map_view;
    private MyBroadcastReceiver broadcastReceiver;
    IMapController mapController;
    GeoPoint currentPoint;
    private static final String CHANNEL_ID = "Crossroads service channel";
    NotificationCompat.Builder builder;

    public class MyBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {

            Bundle b = intent.getExtras();
            latitude = b.getDouble("latitude");
            longitude = b.getDouble("longitude");
            HashMap<Double, Double> coordinates = (HashMap<Double, Double>) b.getSerializable("coordinates");
            updateMapWithCoordinates(latitude, longitude, coordinates);
        }
    }

    private void updateMapWithCoordinates(double latitude, double longitude, HashMap<Double, Double> coordinates) {
        if (map_view!=null && mapController!=null) {
            currentPoint.setLatitude(latitude);
            currentPoint.setLongitude(longitude);
            mapController.setCenter(currentPoint);
            putCrossroadsOnMap(coordinates);
        }
    }

    private void putCrossroadsOnMap(HashMap<Double, Double> coordinates) {
        List<IGeoPoint> points = new ArrayList<>();
        for (Map.Entry<Double, Double> entry : coordinates.entrySet()) {
            points.add(new LabelledGeoPoint(entry.getValue(), entry.getKey()));
        }
        SimplePointTheme pt = new SimplePointTheme(points, false);
        Paint textStyle = new Paint();
        textStyle.setStyle(Paint.Style.FILL);
        textStyle.setColor(Color.parseColor("#0000ff"));
        // set some visual options for the overlay
        // we use here MAXIMUM_OPTIMIZATION algorithm, which works well with >100k points
        SimpleFastPointOverlayOptions opt = SimpleFastPointOverlayOptions.getDefaultStyle()
                .setAlgorithm(SimpleFastPointOverlayOptions.RenderingAlgorithm.MAXIMUM_OPTIMIZATION)
                .setRadius(30).setIsClickable(false).setCellSize(30);
        // create the overlay with the theme
        final SimpleFastPointOverlay sfpo = new SimpleFastPointOverlay(pt, opt);
        map_view.getOverlays().add(sfpo);

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (androidx.core.content.ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
            androidx.core.content.ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
            androidx.core.content.ContextCompat.checkSelfPermission(this, Manifest.permission.FOREGROUND_SERVICE) != PackageManager.PERMISSION_GRANTED ||
            androidx.core.content.ContextCompat.checkSelfPermission(this, Manifest.permission.VIBRATE) != PackageManager.PERMISSION_GRANTED ||
            androidx.core.content.ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ) {

            ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.READ_EXTERNAL_STORAGE,Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.FOREGROUND_SERVICE, Manifest.permission.VIBRATE },1);

        }


        setContentView(R.layout.activity_main);
        distanceTextView = (TextView) findViewById(R.id.distanceTextView);
        tv_latitude = (TextView) findViewById(R.id.tv_latitude);
        tv_longitude = (TextView) findViewById(R.id.tv_longitude);
        dangerTextView = (TextView)findViewById(R.id.tv_danger);
        tv_locality = (TextView)findViewById(R.id.tv_locality);
        retrofit = new Retrofit.Builder()
                .baseUrl("http://evening-dusk-59162.herokuapp.com") //Базовая часть адреса
                .addConverterFactory(GsonConverterFactory.create()) //Конвертер, необходимый для преобразования JSON'а в объекты
                .build();


        api = retrofit.create(Api.class);

        geocoder = new Geocoder(this, Locale.getDefault());
        mPref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        medit = mPref.edit();
        enableServiceIfNeeded();
        settingUpNotifications();
        settingUpMap();
    }

    private void enableServiceIfNeeded() {
        boolean serviceEnabled = isMyServiceRunning(GoogleService.class);
        if (!serviceEnabled) {

            Intent intent = new Intent(this, GoogleService.class);
            startForegroundService(intent);

        } else {
            Toast.makeText(getApplicationContext(), "Сервис запущен", Toast.LENGTH_SHORT).show();
        }
    }

    private void settingUpNotifications() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, CHANNEL_ID, importance);
            channel.setDescription("Уведомления о том, что сервис запущен");
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
        Intent mainActiovityIntent = new Intent(getApplicationContext(), MainActivity.class);
        PendingIntent notificationIntent = PendingIntent.getActivity(getApplicationContext(),0, mainActiovityIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.cast_ic_expanded_controller_stop)
                .setContentTitle("Сервис запущен")
                .setContentText("Вы рядом с перекрестком")
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setContentIntent(notificationIntent)
                .setOngoing(true);

    }

    private void settingUpMap() {
        map_view = (MapView) findViewById(R.id.map_view);
        Configuration.getInstance().setUserAgentValue(getPackageName());
        map_view.setMultiTouchControls(false);
        map_view.setBuiltInZoomControls(false);

        map_view.setEnabled(false);
        map_view.setMinZoomLevel(12d);
        map_view.setTileSource(TileSourceFactory.MAPNIK);
        mapController = map_view.getController();
        mapController.setZoom(19d);
        double[] current_location = getLocation();

        currentPoint = new GeoPoint(current_location[0], current_location[1]);
        mapController.setCenter(currentPoint);

        GpsMyLocationProvider prov= new GpsMyLocationProvider(getApplicationContext());
        prov.addLocationSource(LocationManager.NETWORK_PROVIDER);
        MyLocationNewOverlay locationOverlay = new MyLocationNewOverlay(prov, map_view);
        map_view.getOverlayManager().add(locationOverlay);

    }



    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode) {
            case 1: {
                settingUpMap();
                enableServiceIfNeeded();
            }
        }
    }



    @Override
    protected void onResume() {
        super.onResume();
        map_view.onResume();
        broadcastReceiver = new MyBroadcastReceiver();
        final IntentFilter intentFilter = new IntentFilter("SendCoordinatesToActivity");
        LocalBroadcastManager.getInstance(this).registerReceiver(broadcastReceiver, intentFilter);
    }

    public boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    public void onPause() {
        super.onPause();
        map_view.onPause();
        if(broadcastReceiver != null)
            LocalBroadcastManager.getInstance(this).unregisterReceiver(broadcastReceiver);
        broadcastReceiver = null;
//        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
//        notificationManager.notify(1, builder.build());
    }

    @Override
    public void onStop() {
        super.onStop();

    }

    public void startSettings(View view) {
        startActivity(new Intent(this, SettingsActivity.class));

    }

    private double[] getLocation() {
        LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        List<String> providers = lm.getProviders(true);

        /* Loop over the array backwards, and if you get an accurate location, then break                 out the loop*/
        Location l = null;

        for (int i=providers.size()-1; i>=0; i--) {
            l = lm.getLastKnownLocation(providers.get(i));
            if (l != null) break;
        }

        double[] gps = new double[2];
        if (l != null) {
            gps[0] = l.getLatitude();
            gps[1] = l.getLongitude();
        }
        return gps;
    }

}