package com.example.crossroads;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.*;
import androidx.core.app.NotificationCompat;
import com.example.crossroads.retrofit_classes.Api;
import com.example.crossroads.service.GoogleService;
import com.google.gson.JsonObject;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class SettingsActivity extends Activity implements CompoundButton.OnCheckedChangeListener {
    Retrofit retrofit;
    Api api;
    RadioButton gomelRadioButton,
                minskRadioButton,
                grodnoRadioButton,
                mogilevRadioButton,
                brestRadioButton,
                vitebskRadioButton;
    TextView gomelDataDownloadedState,
            minskDataDownloadedState,
            grodnoDataDownloadedState,
            mogilevDataDownloadedState,
            brestDataDownloadedState,
            vitebskDataDownloadedState;

    Switch serviceWorksSwitch;
    String currentCity;
    private static final String CHANNEL_ID = "Crossroads service channel";
    NotificationCompat.Builder builder;
    private final String[] supportedCities = new String[] {"gomel", "minsk", "grodno", "vitebsk", "mogilev", "brest"};


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        currentCity = getCurrentCity();
        retrofit = new Retrofit.Builder()
                .baseUrl("http://evening-dusk-59162.herokuapp.com") //Базовая часть адреса
                .addConverterFactory(GsonConverterFactory.create()) //Конвертер, необходимый для преобразования JSON'а в объекты
                .build();
        api = retrofit.create(Api.class);

        gomelRadioButton =  findViewById(R.id.gomelRadioButton);
        gomelRadioButton.setOnCheckedChangeListener(this);
        gomelDataDownloadedState = findViewById(R.id.gomelDataState);

        minskRadioButton =  findViewById(R.id.minskRadioButton);
        minskRadioButton.setOnCheckedChangeListener(this);
        minskDataDownloadedState = findViewById(R.id.minskDataState);

        grodnoRadioButton =  findViewById(R.id.grodnoRadioButton);
        grodnoRadioButton.setOnCheckedChangeListener(this);
        grodnoDataDownloadedState = findViewById(R.id.grodnoDataState);

        mogilevRadioButton =  findViewById(R.id.mogilevRadioButton);
        mogilevRadioButton.setOnCheckedChangeListener(this);
        mogilevDataDownloadedState = findViewById(R.id.mogilevDataState);

        brestRadioButton =  findViewById(R.id.brestRadioButton);
        brestRadioButton.setOnCheckedChangeListener(this);
        brestDataDownloadedState = findViewById(R.id.brestDataState);

        vitebskRadioButton =  findViewById(R.id.vitebskRadioButton);
        vitebskRadioButton.setOnCheckedChangeListener(this);
        vitebskDataDownloadedState = findViewById(R.id.vitebskDataState);

        serviceWorksSwitch = findViewById(R.id.serviceWorksSwitch);
        serviceWorksSwitch.setOnCheckedChangeListener(this);
        setServiceWorksSwitch();
        toggleCheckboxes();
        setDataDownloadedTextViews();
        settingUpNotifications();
    }

    private void setServiceWorksSwitch() {
        SharedPreferences sharedPreferences = getSharedPreferences(getString(R.string.SharedPreferencesStoreName), MODE_PRIVATE);
        if (sharedPreferences.getBoolean("start_service", true)) {
            serviceWorksSwitch.setChecked(true);
        } else {
            serviceWorksSwitch.setChecked(false);
        }

    }

    private void toggleCheckboxes() {
        switch (currentCity) {
            case "gomel": {
                gomelRadioButton.toggle();
                break;
            }
            case "minsk": {
                minskRadioButton.toggle();
                break;
            }
            case "grodno": {
                grodnoRadioButton.toggle();
                break;
            }
            case "mogilev": {
                mogilevRadioButton.toggle();
                break;
            }
            case "brest": {
                brestRadioButton.toggle();
                break;
            }
            case "vitebsk": {
                vitebskRadioButton.toggle();
                break;
            }
            default: {
                Toast.makeText(getApplicationContext(), "Пожалуйста, задайте свой город", Toast.LENGTH_LONG).show();
            }

        }

    }

    private void setDataDownloadedTextViews() {
        SharedPreferences sharedPreferences = getSharedPreferences(getString(R.string.SharedPreferencesStoreName), MODE_PRIVATE);
        String coord;
        for (String city:supportedCities) {
            coord = sharedPreferences.getString(city,null);

            switch (city) {
                case "gomel": {
                    setDownloadedTextStatus(coord!=null, gomelDataDownloadedState);
                    break;
                }
                case "minsk": {
                    setDownloadedTextStatus(coord!=null, minskDataDownloadedState);
                    break;
                }
                case "grodno": {
                    setDownloadedTextStatus(coord!=null, grodnoDataDownloadedState);
                    break;
                }
                case "mogilev": {
                    setDownloadedTextStatus(coord!=null, mogilevDataDownloadedState);
                    break;
                }
                case "brest": {
                    setDownloadedTextStatus(coord!=null, brestDataDownloadedState);
                    break;
                }
                case "vitebsk": {
                    setDownloadedTextStatus(coord!=null, vitebskDataDownloadedState);
                    break;
                }
                default:
                    Log.e("unexpected","Unexpected value: " + currentCity);
            }

        }
    }

    private void setDownloadedTextStatus(boolean dataDownloaded, TextView view) {
        if (dataDownloaded) {
            view.setText("Данные сохранены");
        } else {
            view.setText("Данные не сохранены");
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

        builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.cast_ic_expanded_controller_stop)
                .setContentTitle("Сервис запущен")
                .setContentText("Вы рядом с перекрестком")
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_MAX);
    }

    @Override
    public void onPause() {
        super.onPause();
    }


    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

        switch (buttonView.getId()){
            case R.id.gomelRadioButton: {

                if(isChecked) {
                    downloadCoordinatesIfNecessary("gomel");
                    minskRadioButton.setChecked(false);
                    grodnoRadioButton.setChecked(false);
                    mogilevRadioButton.setChecked(false);
                    brestRadioButton.setChecked(false);
                    vitebskRadioButton.setChecked(false);

                }
                break;
            }
            case R.id.minskRadioButton: {

                if (isChecked) {
                    downloadCoordinatesIfNecessary("minsk");
                    gomelRadioButton.setChecked(false);
                    grodnoRadioButton.setChecked(false);
                    mogilevRadioButton.setChecked(false);
                    brestRadioButton.setChecked(false);
                    vitebskRadioButton.setChecked(false);

                }

                break;
            }
            case R.id.grodnoRadioButton: {

                if (isChecked) {
                    downloadCoordinatesIfNecessary("grodno");
                    gomelRadioButton.setChecked(false);
                    minskRadioButton.setChecked(false);
                    mogilevRadioButton.setChecked(false);
                    brestRadioButton.setChecked(false);
                    vitebskRadioButton.setChecked(false);

                }

                break;
            }
            case R.id.mogilevRadioButton: {

                if (isChecked) {
                    downloadCoordinatesIfNecessary("mogilev");
                    gomelRadioButton.setChecked(false);
                    minskRadioButton.setChecked(false);
                    grodnoRadioButton.setChecked(false);
                    brestRadioButton.setChecked(false);
                    vitebskRadioButton.setChecked(false);

                }
                break;
            }
            case R.id.brestRadioButton: {

                if (isChecked) {
                    downloadCoordinatesIfNecessary("brest");
                    gomelRadioButton.setChecked(false);
                    minskRadioButton.setChecked(false);
                    grodnoRadioButton.setChecked(false);
                    mogilevRadioButton.setChecked(false);
                    vitebskRadioButton.setChecked(false);

                }

                break;
            }
            case R.id.vitebskRadioButton: {

                if (isChecked) {
                    downloadCoordinatesIfNecessary("vitebsk");
                    gomelRadioButton.setChecked(false);
                    minskRadioButton.setChecked(false);
                    grodnoRadioButton.setChecked(false);
                    mogilevRadioButton.setChecked(false);
                    brestRadioButton.setChecked(false);

                }

                break;
            }
            case R.id.serviceWorksSwitch: {
                SharedPreferences prefs = getApplicationContext().getSharedPreferences(
                        getString(R.string.SharedPreferencesStoreName), Context.MODE_PRIVATE);
                SharedPreferences.Editor prefsEditor = prefs.edit();
                if (isChecked) {
                    prefsEditor.putBoolean(getString(R.string.currentCityPreferenceName), true);
                } else {
                    prefsEditor.putBoolean(getString(R.string.currentCityPreferenceName), false);
                    stopService();
                }
                prefsEditor.commit();

            }
        }

    }

    private void downloadCoordinatesIfNecessary(String city) {
        SharedPreferences sharedPreferences = getSharedPreferences(getString(R.string.SharedPreferencesStoreName), MODE_PRIVATE);
        String coord = sharedPreferences.getString(city,null);
        setCurrentCity(city);

        if (coord == null) {
            downloadDataDialog();
        } else {
            restartService();
        }


    }

    private void setCurrentCity( String city) {
        currentCity = city;
        SharedPreferences prefs = getApplicationContext().getSharedPreferences(
                getString(R.string.SharedPreferencesStoreName), Context.MODE_PRIVATE);
        SharedPreferences.Editor prefsEditor = prefs.edit();
        prefsEditor.putString(getString(R.string.currentCityPreferenceName), city);
        prefsEditor.commit();
    }

    public String getCurrentCity() {
        SharedPreferences sharedPreferences = getSharedPreferences(getString(R.string.SharedPreferencesStoreName), MODE_PRIVATE);
        return sharedPreferences.getString(getString(R.string.currentCityPreferenceName), "not found");
    }

    public void downloadDataDialog() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(SettingsActivity.this);
        builder
                .setTitle("Скачать данные для выбранного города?")
                .setPositiveButton("Да", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        downloadCoordinates();
                    }
                })
                .setCancelable(true)
                .create()
                .show();
    }


    public void downloadCoordinates() {
        Toast.makeText(getApplicationContext(), "Cкачиваю данные", Toast.LENGTH_SHORT).show();

        Call<JsonObject> call = api.getCoordinatesArrayFromServerByCity(currentCity);
        call.enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (response.body() != null) {
                    JsonObject jsonObj = response.body().getAsJsonObject();
                    saveCordinates(jsonObj.toString());
                    Log.i("RESPONSE", String.valueOf(jsonObj));
                } else {
                    Log.i("NULL", "NULL");
                }
            }
            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {

                Log.i("TAG", "fail");
            }
        });


    }

    private void saveCordinates(String json) {
        SharedPreferences prefs = getApplicationContext().getSharedPreferences(
                getString(R.string.SharedPreferencesStoreName), Context.MODE_PRIVATE);
        SharedPreferences.Editor prefsEditor = prefs.edit();
        prefsEditor.putString(currentCity, json);
        prefsEditor.putString(getString(R.string.currentCityPreferenceName), currentCity);
        prefsEditor.commit();
        Toast.makeText(this, "Данные сохранены", Toast.LENGTH_SHORT).show();
        setDataDownloadedTextViews();
        restartService();

    }

    public void restartService() {
        stopService(new Intent(this, GoogleService.class));
        Intent startServiceIntent = new Intent(this, GoogleService.class);
        startForegroundService(startServiceIntent);
    }


    public void stopService() {
        stopService(new Intent(this, GoogleService.class));
    }
}
