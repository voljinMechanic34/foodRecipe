package com.codingwithmitch.foodrecipes.repositories;

import android.arch.lifecycle.LiveData;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.codingwithmitch.foodrecipes.AppExecutors;
import com.codingwithmitch.foodrecipes.models.Recipe;
import com.codingwithmitch.foodrecipes.persistence.RecipeDao;
import com.codingwithmitch.foodrecipes.persistence.RecipeDatabase;
import com.codingwithmitch.foodrecipes.requests.ServiceGenerator;
import com.codingwithmitch.foodrecipes.requests.responses.ApiResponse;
import com.codingwithmitch.foodrecipes.requests.responses.RecipeResponse;
import com.codingwithmitch.foodrecipes.requests.responses.RecipeSearchResponse;
import com.codingwithmitch.foodrecipes.util.Constants;
import com.codingwithmitch.foodrecipes.util.NetworkBoundResource;
import com.codingwithmitch.foodrecipes.util.Resource;

import java.util.List;

public class RecipeRepository {
    private static final String TAG = "RecipeRepository";
    private static RecipeRepository instance;
    private RecipeDao recipeDao;

    public static RecipeRepository getInstance(Context context){
        if (instance == null){
            instance = new RecipeRepository(context);
        }
        return instance;
    }

    private RecipeRepository(Context context) {
        recipeDao = RecipeDatabase.getInstance(context).getRecipeDao();
    }

    public LiveData<Resource<List<Recipe>>> searchRecipesApi(final String   query, final int pageNumber){
        return new NetworkBoundResource<List<Recipe>, RecipeSearchResponse>(AppExecutors.getInstance()){
            @Override
            protected void saveCallResult(@NonNull RecipeSearchResponse item) {
                    if (item.getRecipes() != null){//null if api key is expired


                        Recipe[] recipes = new Recipe[item.getRecipes().size()];
                        int index = 0;
                        for (long rowid : recipeDao.insertRecipes((Recipe[])(item.getRecipes().toArray(recipes)))){
                            if (rowid == -1 ){
                                Log.d(TAG, "saveCallResult: Conflict , this recipe is already in  the cache ");
                                //if the recipe already exists .. i dont set the ingredients or timestamp
                                //they will be erased
                                recipeDao.updateRecipe(
                                        recipes[index].getRecipe_id(),
                                        recipes[index].getTitle(),
                                        recipes[index].getPublisher(),
                                        recipes[index].getImage_url(),
                                        recipes[index].getSocial_rank()
                                );
                            }
                            index++;
                        }
                    }
            }

            @Override
            protected boolean shouldFetch(@Nullable List<Recipe> data) {
                return true;
            }

            @NonNull
            @Override
            protected LiveData<List<Recipe>> loadFromDb() {
                return recipeDao.searchRecipes(query,pageNumber);
            }

            @NonNull
            @Override
            protected LiveData<ApiResponse<RecipeSearchResponse>> createCall() {
                return ServiceGenerator.getRecipeApi()
                        .searchRecipe(
                                Constants.API_KEY,
                                query,
                                String.valueOf(pageNumber)
                        );
            }
        }.getAsLiveData();
    }
    public LiveData<Resource<Recipe>> searchRecipesApi(final String recipeId){
        return new NetworkBoundResource<Recipe, RecipeResponse>(AppExecutors.getInstance()){

            @Override
            protected void saveCallResult(@NonNull RecipeResponse item) {
                if (item.getRecipe() != null ){//null if expired
                    item.getRecipe().setTimestamp((int)System.currentTimeMillis()/1000);
                    recipeDao.insertRecipe(item.getRecipe());
                }
            }

            @Override
            protected boolean shouldFetch(@Nullable Recipe data) {
                Log.d(TAG, "shouldFetch: recipe" + data.toString());
                int currentTime = (int)System.currentTimeMillis();
                Log.d(TAG, "shouldFetch: current time : " + currentTime);
                int lastRefresh = data.getTimestamp();
                Log.d(TAG, "shouldFetch: last refresh : " + lastRefresh);
                Log.d(TAG, "shouldFetch: its been " + ((currentTime-lastRefresh)/60/60/24)+"days since the refresh");
                if (currentTime-data.getTimestamp() >= Constants.RECIPE_REFRESH_TIME){
                    Log.d(TAG, "shouldFetch: should refresh recipe" + true);
                    return true;
                }
                Log.d(TAG, "shouldFetch: should refresh recipe" + false);
                return false;
            }

            @NonNull
            @Override
            protected LiveData<Recipe> loadFromDb() {

                return recipeDao.getRecipe(recipeId);
            }

            @NonNull
            @Override
            protected LiveData<ApiResponse<RecipeResponse>> createCall() {
                return ServiceGenerator.getRecipeApi().getRecipe(
                        Constants.API_KEY,
                        recipeId
                );
            }
        }.getAsLiveData();
    }
}
