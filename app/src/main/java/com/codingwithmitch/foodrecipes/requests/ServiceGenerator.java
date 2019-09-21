package com.codingwithmitch.foodrecipes.requests;

import com.codingwithmitch.foodrecipes.util.Constants;
import com.codingwithmitch.foodrecipes.util.LiveDataCallAdapter;
import com.codingwithmitch.foodrecipes.util.LiveDataCallAdapterFactory;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ServiceGenerator {

    private static OkHttpClient client = new OkHttpClient.Builder()
            //estabilish connection to server
            .connectTimeout(Constants.CONNECTION_TIMEOUT, TimeUnit.SECONDS)
            //time between each byte read from server
            .readTimeout(Constants.READ_TIMEOUT,TimeUnit.SECONDS)
            //time between each byte send to server
            .writeTimeout(Constants.WRITE_TIMEOUT,TimeUnit.SECONDS)
            .retryOnConnectionFailure(false)
            .build();
    private static Retrofit.Builder retrofitBuilder =
            new Retrofit.Builder()
                    .client(client)
                    .baseUrl(Constants.BASE_URL)
                    .addCallAdapterFactory(new LiveDataCallAdapterFactory())
                    .addConverterFactory(GsonConverterFactory.create());

    private static Retrofit retrofit = retrofitBuilder.build();

    private static RecipeApi recipeApi = retrofit.create(RecipeApi.class);

    public static RecipeApi getRecipeApi(){
        return recipeApi;
    }

}
