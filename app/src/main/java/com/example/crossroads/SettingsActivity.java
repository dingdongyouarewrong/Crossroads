package com.example.crossroads;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.CompoundButton;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;
import com.example.crossroads.retrofit_classes.Api;
import com.example.crossroads.service.GoogleService;
import com.google.gson.JsonObject;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class SettingsActivity extends Activity implements CompoundButton.OnCheckedChangeListener {
    RadioGroup chooseCityRadioGroup;
    Retrofit retrofit;
    Api api;
    RadioButton gomelRadioButton,
                minskRadioButton,
                grodnoRadioButton,
                mogilevRadioButton,
                brestRadioButton,
                vitebskRadioButton;
    RadioButton lastCheckedRadioButton;
    String current_city;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        current_city = getCurrentCity();
        retrofit = new Retrofit.Builder()
                .baseUrl("http://evening-dusk-59162.herokuapp.com") //Базовая часть адреса
                .addConverterFactory(GsonConverterFactory.create()) //Конвертер, необходимый для преобразования JSON'а в объекты
                .build();
        api = retrofit.create(Api.class);

        chooseCityRadioGroup = findViewById(R.id.chooseCityRadioGroup);
        gomelRadioButton =  findViewById(R.id.gomelRadioButton);
        gomelRadioButton.setOnCheckedChangeListener(this);

        minskRadioButton =  findViewById(R.id.minskRadioButton);
        minskRadioButton.setOnCheckedChangeListener(this);

        grodnoRadioButton =  findViewById(R.id.grodnoRadioButton);
        grodnoRadioButton.setOnCheckedChangeListener(this);

        mogilevRadioButton =  findViewById(R.id.mogilevRadioButton);
        mogilevRadioButton.setOnCheckedChangeListener(this);

        brestRadioButton =  findViewById(R.id.brestRadioButton);
        brestRadioButton.setOnCheckedChangeListener(this);

        vitebskRadioButton =  findViewById(R.id.vitebskRadioButton);
        vitebskRadioButton.setOnCheckedChangeListener(this);

//        Button downloadCoordinatesButton = findViewById(R.id.download_data_button);

        switch (current_city) {
            case "gomel": {
                gomelRadioButton.toggle();
                lastCheckedRadioButton = gomelRadioButton;
                break;
            }
            case "minsk": {
                minskRadioButton.toggle();
                lastCheckedRadioButton = minskRadioButton;

                break;
            }
            case "grodno": {
                grodnoRadioButton.toggle();
                lastCheckedRadioButton = grodnoRadioButton;

                break;
            }
            case "mogilev": {
                mogilevRadioButton.toggle();
                lastCheckedRadioButton = mogilevRadioButton;

                break;
            }
            case "brest": {
                brestRadioButton.toggle();
                lastCheckedRadioButton = brestRadioButton;

                break;
            }
            case "vitebsk": {
                vitebskRadioButton.toggle();
                lastCheckedRadioButton = vitebskRadioButton;

                break;
            }
            default: {
                Toast.makeText(getApplicationContext(), "Пожалуйста, задайте свой город", Toast.LENGTH_LONG).show();
            }

        }
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
        }

    }

    private void downloadCoordinatesIfNecessary(String city) {
        SharedPreferences sharedPreferences = getSharedPreferences("Crossroads", MODE_PRIVATE);
        String coord = sharedPreferences.getString(city,null);
        setCurrentCity(city);

        if (coord == null) {
            downloadDataDialog();
        } else {
            restartService();
        }


    }

    private void setCurrentCity( String city) {
        current_city = city;
        SharedPreferences prefs = getApplicationContext().getSharedPreferences(
                "Crossroads", Context.MODE_PRIVATE);
        SharedPreferences.Editor prefsEditor = prefs.edit();
        prefsEditor.putString("current_city", city);
        prefsEditor.commit();
    }

    public String getCurrentCity() {
        SharedPreferences sharedPreferences = getSharedPreferences("Crossroads", MODE_PRIVATE);
        return sharedPreferences.getString("current_city", "not found");
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

        Call<JsonObject> call = api.getCoordinatesArrayFromServerByCity(current_city);
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
                "Crossroads", Context.MODE_PRIVATE);
        SharedPreferences.Editor prefsEditor = prefs.edit();
        prefsEditor.putString(current_city, json);
        prefsEditor.putString("current_city",current_city);
        prefsEditor.commit();
        Toast.makeText(this, "Данные сохранены", Toast.LENGTH_SHORT).show();
        restartService();

    }

    public void restartService() {
        stopService(new Intent(this, GoogleService.class));
        Intent startServiceIntent = new Intent(this, GoogleService.class);
        startService(startServiceIntent);
    }
    


}
