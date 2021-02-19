package com.example.crossroads.retrofit_classes;

import com.google.gson.JsonObject;
import retrofit2.Call;
import retrofit2.http.*;


public interface Api {





    @Headers("Content-Type: application/json")
    @GET("/get_data")
    Call<JsonObject> getCoordinatesArrayFromServer(
    );







}

