package com.gsu.crossroads;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Paint;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;

import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import com.gsu.crossroads.retrofit_classes.Api;
import com.gsu.crossroads.service.GoogleService;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.osmdroid.api.IGeoPoint;
import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.CustomZoomButtonsController;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;
import org.osmdroid.views.overlay.simplefastpoint.LabelledGeoPoint;
import org.osmdroid.views.overlay.simplefastpoint.SimpleFastPointOverlay;
import org.osmdroid.views.overlay.simplefastpoint.SimpleFastPointOverlayOptions;
import org.osmdroid.views.overlay.simplefastpoint.SimplePointTheme;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import java.lang.reflect.Type;
import java.util.*;

public class MainActivity extends Activity {

    Double latitude,longitude;
    Geocoder geocoder;
    Retrofit retrofit;
    Api api;
    private MapView mapView;
    private MyBroadcastReceiver broadcastReceiver;
    IMapController mapController;
    GeoPoint currentPoint;
    Boolean danger;
    private static final String CHANNEL_ID = "Crossroads service channel";
    NotificationCompat.Builder builder;
    Button dangerIndicator, notDangerIndicator;

    public class MyBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            Bundle b = intent.getExtras();
            latitude = b.getDouble("latitude");
            longitude = b.getDouble("longitude");
            danger = b.getBoolean("danger");
            HashMap<Double, Double> coordinates = (HashMap<Double, Double>) b.getSerializable("coordinates");
            updateMapWithCoordinates(latitude, longitude, coordinates);

        }

        private void updateMapWithCoordinates(double latitude, double longitude, HashMap<Double, Double> coordinates) {
            if (mapView !=null && mapController!=null) {
                currentPoint.setLatitude(latitude);
                currentPoint.setLongitude(longitude);
                mapController.setCenter(currentPoint);
                if (dangerIndicator!=null) {
                    if (Boolean.TRUE.equals(danger)) {
                        dangerIndicator.setVisibility(View.VISIBLE);
                        notDangerIndicator.setVisibility(View.INVISIBLE);
                    }
                    else {
                        dangerIndicator.setVisibility(View.INVISIBLE);
                        notDangerIndicator.setVisibility(View.VISIBLE);                }
                }
                putCrossroadsOnMap(coordinates);
            }
        }

    }


    private void putCrossroadsOnMap(HashMap<Double, Double> coordinates) {
        if (coordinates!=null) {
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
            mapView.getOverlays().add(sfpo);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (androidx.core.content.ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
            androidx.core.content.ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
            androidx.core.content.ContextCompat.checkSelfPermission(this, Manifest.permission.FOREGROUND_SERVICE) != PackageManager.PERMISSION_GRANTED ||
            androidx.core.content.ContextCompat.checkSelfPermission(this, Manifest.permission.VIBRATE) != PackageManager.PERMISSION_GRANTED ||
            androidx.core.content.ContextCompat.checkSelfPermission(this, Manifest.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS) != PackageManager.PERMISSION_GRANTED ||
            androidx.core.content.ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ) {

            ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.READ_EXTERNAL_STORAGE,Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.FOREGROUND_SERVICE, Manifest.permission.VIBRATE, Manifest.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS },1);

        }


        setContentView(R.layout.activity_main);
        licenseConfirmation();
        retrofit = new Retrofit.Builder()
                .baseUrl("http://evening-dusk-59162.herokuapp.com") //Базовая часть адреса
                .addConverterFactory(GsonConverterFactory.create()) //Конвертер, необходимый для преобразования JSON'а в объекты
                .build();

        notDangerIndicator = findViewById(R.id.notDangerIndicatiorButton);
        dangerIndicator = findViewById(R.id.dangerIndicatiorButton);
        api = retrofit.create(Api.class);
        geocoder = new Geocoder(this, Locale.getDefault());
        enableServiceIfNeeded();
        settingUpNotifications();
        settingUpMap();
    }

    private void licenseConfirmation() {
        SharedPreferences sharedPreferences = getSharedPreferences(getString(R.string.SharedPreferencesStoreName), MODE_PRIVATE);
        if (!sharedPreferences.getBoolean("license_confirmed", false)) {
            licenseAlerDialog();
        }

    }

    private void enableServiceIfNeeded() {
        boolean serviceEnabled = isMyServiceRunning(GoogleService.class);

        if (!serviceEnabled && serviceSetUpToStart()) {

            Intent intent = new Intent(this, GoogleService.class);
            startForegroundService(intent);
        }
    }

    private boolean serviceSetUpToStart() {
        SharedPreferences sharedPreferences = getSharedPreferences(getString(R.string.SharedPreferencesStoreName), MODE_PRIVATE);
        return sharedPreferences.getBoolean("start_service", true);
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
        mapView = findViewById(R.id.map_view);
        Configuration.getInstance().setUserAgentValue(getPackageName());
        mapView.setMultiTouchControls(false);
        mapView.getZoomController().setVisibility(CustomZoomButtonsController
                .Visibility.NEVER);
        mapView.setMaxZoomLevel(19d);

        mapView.setEnabled(false);
        mapView.setMinZoomLevel(19d);

        mapView.setTileSource(TileSourceFactory.MAPNIK);
        mapController = mapView.getController();
        mapController.setZoom(19d);

        GpsMyLocationProvider prov= new GpsMyLocationProvider(getApplicationContext());
        prov.addLocationSource(LocationManager.NETWORK_PROVIDER);
        MyLocationNewOverlay locationOverlay = new MyLocationNewOverlay(prov, mapView);
        mapView.getOverlayManager().add(locationOverlay);
        double[] currentLocation = getLocation();
        currentPoint = new GeoPoint(currentLocation[0], currentLocation[1]);
        mapController.setCenter(currentPoint);
        settingUpCrossroadsIfExists();
    }

    private void settingUpCrossroadsIfExists() {
        String city = getCurrentCity();
        if (city!=null) {
            HashMap<Double, Double> coordinates = getCoordinates(city);
            putCrossroadsOnMap(coordinates);
        }
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

    public String getCurrentCity() {
        SharedPreferences sharedPreferences = getSharedPreferences(getString(R.string.SharedPreferencesStoreName), MODE_PRIVATE);
        return sharedPreferences.getString(getString(R.string.currentCityPreferenceName), "Minsk");
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode==1) {
                settingUpMap();
                enableServiceIfNeeded();
            }
        else {
            Toast.makeText(getApplicationContext(), "Приложение не может работать без соответсвующих разрешений", Toast.LENGTH_SHORT);
        }
    }



    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
        settingUpMap();
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
    public void onPause() {
        super.onPause();
        mapView.onPause();
        if(broadcastReceiver != null)
            LocalBroadcastManager.getInstance(this).unregisterReceiver(broadcastReceiver);
        broadcastReceiver = null;
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

    public void licenseAlerDialog() {

        final AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder
                .setTitle(R.string.alert_title)
                .setMessage(R.string.license)
                .setPositiveButton("Да", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        disableBatteryRestrictionsDialog();
                    }
                })
                .setNegativeButton("Нет", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        MainActivity.this.finish();
                        System.exit(0);
                        Toast.makeText(getApplicationContext(), "Соглашение не принято", Toast.LENGTH_LONG).show();
                    }
                })

                .setCancelable(false)
                .create()
                .show();
    }

    private void disableBatteryRestrictionsDialog() {
                final AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setTitle("Выполните действия перед началом работы")
                        .setMessage("Для корректной работы приложения нужно сказать телефону не выключать его через несколько минут работы")
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                Toast.makeText(getApplicationContext(), "Соглашение принято", Toast.LENGTH_LONG).show();
                                SharedPreferences sharedPreferences = getSharedPreferences(getString(R.string.SharedPreferencesStoreName), MODE_PRIVATE);
                                SharedPreferences.Editor prefsEditor = sharedPreferences.edit();
                                prefsEditor.putBoolean("license_confirmed", true);
                                prefsEditor.commit();
                                disableBatteryRestrictions();

                            }
                        })
                        .setView(findViewById(R.layout.activity_main))
                        .setCancelable(false)
                        .create()
                        .show();


    }

    private void disableBatteryRestrictions() {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Intent intent = new Intent();
            String packageName = getPackageName();
            PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
            if (!pm.isIgnoringBatteryOptimizations(packageName)) {
                intent.setAction(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                intent.setData(Uri.parse("package:" + packageName));
                startActivity(intent);
            }
        }
    }


}