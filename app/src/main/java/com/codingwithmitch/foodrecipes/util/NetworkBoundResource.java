package com.codingwithmitch.foodrecipes.util;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MediatorLiveData;
import android.arch.lifecycle.Observer;
import android.support.annotation.MainThread;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.WorkerThread;
import android.util.Log;

import com.codingwithmitch.foodrecipes.AppExecutors;
import com.codingwithmitch.foodrecipes.requests.responses.ApiResponse;

public abstract class NetworkBoundResource<CacheObject, RequestObject> {
    private MediatorLiveData<Resource<CacheObject>> results = new MediatorLiveData<>();
    private AppExecutors appExecutors;
    private static final String TAG = "NetworkBoundResource";
    public NetworkBoundResource( AppExecutors appExecutors) {

        this.appExecutors = appExecutors;
        init();
    }
    private void init(){
        //update livedata for loading status
        results.setValue((Resource<CacheObject>)Resource.loading(null));
        //observe LiveData  source from local db
        final LiveData<CacheObject> dbSource = loadFromDb();
        results.addSource(dbSource, new Observer<CacheObject>() {
            @Override
            public void onChanged(@Nullable CacheObject cacheObject) {
                results.removeSource(dbSource);
                if (shouldFetch(cacheObject)){
                    //get data from network
                    fetchFromNetwork(dbSource);
                } else{
                    results.addSource(dbSource, new Observer<CacheObject>() {
                        @Override
                        public void onChanged(@Nullable CacheObject cacheObject) {
                            setValue(Resource.success(cacheObject));
                        }
                    });
                }
            }
        });
    }
    //observer the local db
    // if condition query the network
    // stop observe db
    // insert new data in db
    // begin observing db again to see the  refreshed data from network
    private  void fetchFromNetwork(final LiveData<CacheObject> dbSource){
        Log.d(TAG, "fetchFromNetwork: called");
        //updata livedata from loading status
        results.addSource(dbSource, new Observer<CacheObject>() {
            @Override
            public void onChanged(@Nullable CacheObject cacheObject) {
                setValue(Resource.loading(cacheObject));
            }
        });
        final LiveData<ApiResponse<RequestObject>> apiResponse = createCall();
        results.addSource(apiResponse, new Observer<ApiResponse<RequestObject>>() {
            @Override
            public void onChanged(@Nullable final ApiResponse<RequestObject> requestObjectApiResponse) {
                results.removeSource(dbSource);
                results.removeSource(apiResponse);

                if (requestObjectApiResponse instanceof ApiResponse.ApiSuccessResponse){
                    Log.d(TAG, "onChanged: ApiSuccess");
                    appExecutors.diskIO().execute(new Runnable() {
                        @Override
                        public void run() {
                            // save the response to local db
                            saveCallResult((RequestObject) processResponce((ApiResponse.ApiSuccessResponse) requestObjectApiResponse));
                            appExecutors.mainThread().execute(new Runnable() {
                                @Override
                                public void run() {
                                    results.addSource(loadFromDb(), new Observer<CacheObject>() {
                                        @Override
                                        public void onChanged(@Nullable CacheObject cacheObject) {
                                            setValue(Resource.success(cacheObject));
                                        }
                                    });
                                }
                            });
                        }
                    });
                } else  if (requestObjectApiResponse instanceof ApiResponse.ApiEmptyResponse){

                    Log.d(TAG, "onChanged: ApiEmptyResponce");
                    appExecutors.mainThread().execute(new Runnable() {
                        @Override
                        public void run() {
                            results.addSource(loadFromDb(), new Observer<CacheObject>() {
                                @Override
                                public void onChanged(@Nullable CacheObject cacheObject) {
                                    setValue(Resource.success(cacheObject));
                                }
                            });
                        }
                    });

                } else if (requestObjectApiResponse instanceof ApiResponse.ApiErrorResponse){
                    Log.d(TAG, "onChanged: Error");
                    results.addSource(dbSource, new Observer<CacheObject>() {
                        @Override
                        public void onChanged(@Nullable CacheObject cacheObject) {
                            setValue(Resource.error(
                                    ((ApiResponse.ApiErrorResponse) requestObjectApiResponse).getErrorMessage(),
                                        cacheObject)
                            );
                        }
                    });
                }
            }
        });
    }
    private CacheObject processResponce(ApiResponse.ApiSuccessResponse response){
        return (CacheObject) response.getBody();
    }
    private void setValue(Resource<CacheObject> newValue){

        if (results.getValue() != newValue){
            results.setValue(newValue);
        }
    }
    // Called to save the result of the API response into the database.
    @WorkerThread
    protected abstract void saveCallResult(@NonNull RequestObject item);

    // Called with the data in the database to decide whether to fetch
    // potentially updated data from the network.
    @MainThread
    protected abstract boolean shouldFetch(@Nullable CacheObject data);

    // Called to get the cached data from the database.
    @NonNull @MainThread
    protected abstract LiveData<CacheObject> loadFromDb();

    // Called to create the API call.
    @NonNull @MainThread
    protected abstract LiveData<ApiResponse<RequestObject>> createCall();

    // Returns a LiveData object that represents the resource that's implemented
    // in the base class.
    public final LiveData<Resource<CacheObject>> getAsLiveData(){
        return results;
    };
}
