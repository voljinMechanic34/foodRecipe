package com.codingwithmitch.foodrecipes.util;

import android.arch.lifecycle.LiveData;

import com.codingwithmitch.foodrecipes.requests.responses.ApiResponse;

import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import retrofit2.CallAdapter;
import retrofit2.Retrofit;

public class LiveDataCallAdapterFactory extends CallAdapter.Factory {

    /**
     * This method performs a number of checks and then returns the Response type for the Retrofit requests
     * (@bodyType is the ResponseType. It can be RecipeResponse or RecipeSearchResponse)
     *
     * CHECK #1) returnType returns LIVEDATA
     * CHECK #2) Type LiveData<T> is of ApiResponse.class
     * CHECK #3) Make sure ApiResponse is parameeterized. AKA: ApiResponse<T> exists.
     *
     *
     * @param returnType
     * @param annotations
     * @param retrofit
     * @return
     */

    @Override
    public CallAdapter<?, ?> get(Type returnType, Annotation[] annotations, Retrofit retrofit) {
        // check 1
        // make sure the CallAdapter is returning a type of LiveData
        if (CallAdapter.Factory.getRawType(returnType) != LiveData.class){
            return null;
        }
        // check 2
        // type that livedata is wrapping
        Type observableType = CallAdapter.Factory.getParameterUpperBound(0,(ParameterizedType) returnType);
        //check if its of type ApiResponce
        Type rawObservableType = CallAdapter.Factory.getRawType(observableType);
        if(rawObservableType != ApiResponse.class){
            throw  new IllegalArgumentException("Type must be a defined resource");
        }
        // check 3
        // check if ApiResponce is parameterized . does apiresponse <t> exists?(must wrap around t)
        // T is either RecipeResponse or RecipeSearchResponse in this app. But T can be anything theoretically.
        if (!(observableType instanceof ParameterizedType)){
            throw  new IllegalArgumentException("resource must be parameterized");
        }
        Type bodyType = CallAdapter.Factory.getParameterUpperBound(0,(ParameterizedType) observableType);

        return new LiveDataCallAdapter<Type>(bodyType);
    }
}
